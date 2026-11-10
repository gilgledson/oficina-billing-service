-- Banco próprio do Billing Service. Diferente do monólito, não há chave
-- estrangeira para ORDEM_DE_SERVICO: a OS vive no banco do OS Service e aqui
-- só guardamos o identificador dela.
CREATE TABLE orcamento (
    id UUID PRIMARY KEY,
    ordem_servico_id UUID NOT NULL,
    valor_total NUMERIC(10, 2) NOT NULL,
    metodo_pagamento VARCHAR(50),
    data_emissao DATE NOT NULL,
    data_vencimento DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    assinatura_cliente VARCHAR(500),
    CONSTRAINT check_orcamento_metodo_pagamento CHECK (metodo_pagamento IN ('DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'PIX')),
    CONSTRAINT check_orcamento_status CHECK (status IN ('PENDENTE', 'PAGO', 'CANCELADA')),
    -- Um orçamento por OS: garante a idempotência do consumo de eventos.
    CONSTRAINT uk_orcamento_ordem_servico UNIQUE (ordem_servico_id)
);

-- Transactional Outbox: eventos de integração gravados junto com o dado de
-- negócio e entregues ao RabbitMQ pelo OutboxRelay.
CREATE TABLE outbox_evento (
    id UUID PRIMARY KEY,
    routing_key VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    correlation_id VARCHAR(100),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    publicado_em TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_evento_pendentes ON outbox_evento (criado_em) WHERE publicado_em IS NULL;
