# Oficina — Billing Service

Microsserviço de **orçamento e pagamento** da oficina (Tech Challenge SOAT, Fase 4).
Foi extraído do módulo `orcamento` do monólito
[`fiat-tech-challenge-ddd`](https://github.com/gilgledson/fiat-tech-challenge-ddd).

Responsabilidades:

- Gerar o orçamento de uma OS quando o diagnóstico é concluído.
- Registrar o aceite (com assinatura) ou a rejeição do cliente.
- Registrar o pagamento e avisar os outros serviços.
- Gerar o PDF do orçamento.

## Tecnologias

| Item | Escolha |
|---|---|
| Linguagem / framework | Java 17, Quarkus 3.15 |
| Banco de dados | PostgreSQL próprio (database `billing`), migrações com Flyway |
| Mensageria | RabbitMQ (SmallRye Reactive Messaging) |
| Comunicação síncrona | REST Client para o OS Service |
| Pagamentos | Mercado Pago (Checkout Pro), via REST Client e webhook |
| Autenticação | JWT RS256 emitido pelo OS Service; aqui só é validado |
| Observabilidade | New Relic (Java Agent), logs JSON com `correlationId` |
| Testes e qualidade | JUnit 5, Mockito, JaCoCo (mínimo de 80%), SonarCloud |

## Arquitetura

```mermaid
flowchart LR
    GW[API Gateway<br/>Traefik] -->|/api/orcamentos| B[Billing Service]
    B --> DB[(PostgreSQL<br/>billing)]
    B -->|REST: dados da OS,<br/>cliente e veículo| OS[OS Service]
    OS -->|os.*| MQ{{RabbitMQ<br/>exchange oficina.events}}
    MQ -->|fila billing.os-events| B
    B -->|billing.*| MQ
    B --> BLOB[(Azure Blob<br/>assinaturas)]
    B -->|REST: cria cobrança,<br/>consulta pagamento| MP[Mercado Pago]
    MP -->|webhook de pagamento| GW
```

O código segue a mesma organização do monólito:

- `domain` — entidade `Orcamento` e regras de negócio.
- `application` — casos de uso, eventos e portas (`gateway`, `repository`).
- `infrastructure` — JPA, outbox, RabbitMQ, cliente REST e armazenamento.
- `api` — endpoints REST.

Nenhum outro serviço acessa o banco do Billing, e o Billing não acessa o banco
de ninguém: a tabela `orcamento` guarda só o identificador da OS, sem chave
estrangeira.

## Saga Pattern: coreografia

O fluxo da OS (abrir → orçamento → aprovação → execução → pagamento) é uma
**Saga coreografada**: não há orquestrador central; cada serviço reage aos
eventos dos outros e publica os seus.

Por que coreografia:

- O monólito já funcionava assim. Os módulos conversavam por eventos em
  memória, então trocar o barramento por um broker preservou o desenho e os
  testes.
- São só três serviços e um fluxo linear; um orquestrador seria mais um
  componente para implantar e manter no ar.
- Cada serviço continua dono das próprias regras, sem um ponto central que
  conheça o fluxo inteiro.

O custo é que o fluxo completo não está escrito em um único lugar. As tabelas
abaixo são essa documentação, e o `correlationId` permite seguir uma OS pelos
logs e traces dos três serviços.

### Eventos consumidos

Fila `billing.os-events`, ligada à exchange `oficina.events` (tipo `topic`).

| Routing key | Payload | O que o Billing faz |
|---|---|---|
| `os.diagnostico-concluido` | `ordemServicoId`, `valorTotal`, `metodoPagamento` (opcional) | Gera o orçamento e publica `billing.orcamento-gerado` |
| `os.execucao-concluida` | `ordemServicoId`, `valorTotal` | Atualiza o orçamento com o valor final e cria a cobrança no Mercado Pago |
| `os.cancelada`, `os.rejeitada` | `ordemServicoId` | **Compensação:** cancela o orçamento |

### Eventos publicados

| Routing key | Payload | Quando |
|---|---|---|
| `billing.orcamento-gerado` | `orcamentoId`, `ordemServicoId`, `valorTotal` | Orçamento criado, aguardando o cliente |
| `billing.orcamento-aceito` | `orcamentoId`, `ordemServicoId`, `servicosAceitos`, `servicosRejeitados` | Cliente aceitou ao menos um serviço |
| `billing.orcamento-rejeitado` | `orcamentoId`, `ordemServicoId` | **Compensação:** cliente rejeitou tudo |
| `billing.orcamento-falhou` | `ordemServicoId`, `motivo` | **Compensação:** não foi possível gerar o orçamento |
| `billing.orcamento-cancelado` | `orcamentoId`, `ordemServicoId`, `estornoNecessario` | Orçamento cancelado porque a OS foi cancelada |
| `billing.pagamento-confirmado` | `orcamentoId`, `ordemServicoId` | Pagamento aprovado no Mercado Pago ou registrado pelo atendente |
| `billing.pagamento-recusado` | `orcamentoId`, `ordemServicoId`, `pagamentoId`, `motivo` | O Mercado Pago recusou uma tentativa de pagamento |

### Consistência

- **Transactional Outbox:** o evento é gravado na tabela `outbox_evento` na
  mesma transação do orçamento. O `OutboxRelay` entrega os pendentes ao
  RabbitMQ a cada 2 segundos e só marca como publicado depois da confirmação
  do broker.
- **Consumo idempotente:** a entrega é "pelo menos uma vez". Um evento repetido
  não duplica orçamento (há restrição de unicidade por OS) nem repete o
  cancelamento.
- **Fila de mensagens mortas:** mensagem que falha no processamento vai para
  `billing.os-events.dlq`.

## Pagamento com Mercado Pago

A cobrança usa o **Checkout Pro**: o Billing cria uma preferência de pagamento
e o cliente paga no link do Mercado Pago, com o meio que preferir.

```mermaid
sequenceDiagram
    participant OS as OS Service
    participant B as Billing
    participant MP as Mercado Pago
    participant C as Cliente
    OS-->>B: os.execucao-concluida (valor final)
    B->>MP: POST /checkout/preferences (external_reference = ID do orçamento)
    MP-->>B: ID da preferência e link de pagamento
    C->>MP: paga no link
    MP->>B: POST /api/orcamentos/webhooks/mercadopago (ID do pagamento)
    B->>MP: GET /v1/payments/{id}
    MP-->>B: status, valor e external_reference
    B-->>OS: billing.pagamento-confirmado
```

- **Quando a cobrança é criada:** ao fim da execução, quando o valor final da
  OS é conhecido. O link fica no campo `linkPagamento` do orçamento. Se o
  Mercado Pago estiver fora do ar nesse momento, o evento não é rejeitado; o
  atendente gera o link depois em `POST /api/orcamentos/{id}/pagamento`.
- **Notificação não é verdade:** o webhook só traz o ID do pagamento. O
  Billing consulta o pagamento na API do Mercado Pago e só confirma se ele
  estiver aprovado, referenciar o orçamento e cobrir o valor total.
- **Assinatura:** com `MERCADOPAGO_WEBHOOK_SECRET` configurado, o header
  `x-signature` é conferido (HMAC-SHA256) e notificação com assinatura
  inválida recebe `401`.
- **Idempotência:** o Mercado Pago reenvia a mesma notificação; orçamento já
  pago não é confirmado de novo.
- **Pagamento recusado:** o orçamento continua `PENDENTE` e a OS continua
  aguardando pagamento, porque o cliente pode tentar de novo no mesmo link. O
  Billing publica `billing.pagamento-recusado`.
- **Pagamento de orçamento cancelado:** não é confirmado; fica registrado em
  log de erro para estorno manual.
- **Sem credenciais:** sem `MERCADOPAGO_ACCESS_TOKEN` a integração fica
  desligada e o pagamento é registrado pelo atendente em `/confirmar-pagamento`
  (rota que continua valendo para pagamento no balcão).

Para configurar: crie uma aplicação em
[Suas integrações](https://www.mercadopago.com.br/developers/panel/app), copie
o access token de teste para `MERCADOPAGO_ACCESS_TOKEN`, cadastre o webhook de
pagamentos apontando para `/api/orcamentos/webhooks/mercadopago` e copie a
chave secreta dele para `MERCADOPAGO_WEBHOOK_SECRET`.

## API

As rotas exigem JWT com perfil `ADMIN` ou `ATENDENTE`, menos o webhook, que é
chamado pelo Mercado Pago.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/orcamentos/{id}/aceitar` | Aceite do orçamento com assinatura em Base64 |
| `POST` | `/api/orcamentos/{id}/pagamento` | Gera (ou gera de novo) o link de pagamento no Mercado Pago |
| `POST` | `/api/orcamentos/{id}/confirmar-pagamento` | Registra um pagamento feito no balcão |
| `POST` | `/api/orcamentos/webhooks/mercadopago` | Webhook de pagamento do Mercado Pago (sem JWT) |
| `GET` | `/api/orcamentos/{id}` | PDF do orçamento |
| `GET` | `/api/orcamentos/ordem-servico/{id}` | PDF do orçamento de uma OS |
| `GET` | `/api/orcamentos/ordem-servico/{id}/json` | Orçamento de uma OS em JSON |
| `GET` | `/api/orcamentos/assinaturas/{filename}` | Imagem da assinatura |

Swagger: `/q/swagger-ui`. Healthchecks: `/q/health`.

Para montar o PDF, o Billing chama o OS Service repassando o JWT de quem fez a
requisição:

- `GET /api/integracao/ordens/{id}/orcamento`
- `GET /api/integracao/clientes/{id}`
- `GET /api/integracao/veiculos/{id}`

## Como executar

Requer JDK 17 e Docker.

```bash
./mvnw clean package -DskipTests
```

```bash
docker compose up --build
```

A API sobe em `http://localhost:8384`, o Postgres na porta `25433` e o painel
do RabbitMQ em `http://localhost:15672` (`oficina`/`oficina`).

Testes e cobertura (falha abaixo de 80% de linhas; relatório em
`target/site/jacoco/index.html`):

```bash
./mvnw clean verify
```

### Variáveis de ambiente

| Variável | Descrição | Padrão |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Banco do Billing | — |
| `RABBITMQ_HOST`, `RABBITMQ_PORT` | Broker | `localhost`, `5672` |
| `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | Credenciais do broker | `guest` |
| `OS_SERVICE_URL` | URL base do OS Service | `http://localhost:8383` |
| `STORAGE_TYPE` | `local` ou `azure` | `local` |
| `AZURE_STORAGE_CONNECTION_STRING`, `AZURE_STORAGE_CONTAINER` | Blob das assinaturas | emulador Azurite |
| `MERCADOPAGO_ACCESS_TOKEN` | Access token da aplicação no Mercado Pago | vazio (integração desligada) |
| `MERCADOPAGO_WEBHOOK_SECRET` | Chave secreta do webhook | vazio (assinatura não conferida) |
| `MERCADOPAGO_NOTIFICATION_URL` | URL pública do webhook, enviada em cada cobrança | vazio (vale a do painel) |
| `MERCADOPAGO_API_URL` | Endereço da API do Mercado Pago | `https://api.mercadopago.com` |

## CI/CD

O pipeline em [`.github/workflows/ci.yml`](.github/workflows/ci.yml) tem dois jobs:

1. **Build, testes e qualidade** — em todo PR e push na `main`: compila, roda
   os testes e exige 80% de cobertura. Se o secret `SONAR_TOKEN` estiver
   configurado, envia também a análise ao SonarCloud.
2. **Deploy** — só em push na `main`: publica a imagem no Docker Hub e aplica
   os manifestos de [`k8s/`](k8s) no AKS.

Configuração necessária no repositório:

- Secrets: `SONAR_TOKEN` (opcional), `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`,
  `AZURE_CREDENTIALS`, `BILLING_DB_PASSWORD`, `RABBITMQ_USERNAME`,
  `RABBITMQ_PASSWORD`, `AZURE_STORAGE_CONNECTION_STRING`, `NEW_RELIC_LICENSE_KEY`,
  `MERCADOPAGO_ACCESS_TOKEN`, `MERCADOPAGO_WEBHOOK_SECRET`.
- Variables: `SONAR_ORGANIZATION`, `SONAR_PROJECT_KEY`.

## Teste integrado

Validado de ponta a ponta com Postgres e RabbitMQ reais, junto com os outros dois serviços, pelo ambiente integrado do OS Service ([e2e](https://github.com/gilgledson/fiat-tech-challenge-ddd/tree/main/e2e)).

## Pendências

- Mercado Pago: a integração foi validada com testes unitários e, no ambiente
  integrado, contra um Mercado Pago simulado. Falta rodar com credenciais de
  teste reais e cadastrar o webhook no painel.
- Estorno automático quando chega pagamento de um orçamento cancelado.
- O OS Service ainda não consome `billing.pagamento-recusado`.
- Evidência de cobertura e link do SonarCloud, depois da primeira execução do
  pipeline.
