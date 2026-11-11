package br.com.fiap.oficina.billing.application.listener;

import java.math.BigDecimal;
import java.util.UUID;

import org.jboss.logging.Logger;

import br.com.fiap.oficina.billing.application.event.OrcamentoFalhouEvent;
import br.com.fiap.oficina.billing.application.gateway.EventPublisher;
import br.com.fiap.oficina.billing.application.usecase.comandos.AtualizarOrcamentoUseCase;
import br.com.fiap.oficina.billing.application.usecase.comandos.CancelarOrcamentoUseCase;
import br.com.fiap.oficina.billing.application.usecase.comandos.CriarOrcamentoUseCase;
import br.com.fiap.oficina.billing.application.usecase.comandos.GerarCobrancaUseCase;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

/**
 * Reage aos eventos do OS Service que interessam ao Billing. Quem entrega as
 * mensagens é o consumidor do broker
 * ({@code infrastructure.messaging.OsEventsConsumer}).
 */
@ApplicationScoped
@RequiredArgsConstructor
public class GerarOrcamentoOnOrdemFinalizadaListener {

    private static final Logger LOG = Logger.getLogger(GerarOrcamentoOnOrdemFinalizadaListener.class);

    private final CriarOrcamentoUseCase criarOrcamentoUseCase;
    private final AtualizarOrcamentoUseCase atualizarOrcamentoUseCase;
    private final CancelarOrcamentoUseCase cancelarOrcamentoUseCase;
    private final GerarCobrancaUseCase gerarCobrancaUseCase;
    private final EventPublisher eventPublisher;

    /** Diagnóstico concluído: gera o orçamento que o cliente vai aprovar. */
    public void onDiagnosticoConcluido(JsonObject json) {
        UUID ordemServicoId = UUID.fromString(json.getString("ordemServicoId"));
        try {
            BigDecimal valorTotal = lerValor(json);
            String metodo = json.getString("metodoPagamento");
            MetodoPagamento metodoPagamento = metodo != null ? MetodoPagamento.valueOf(metodo) : null;

            criarOrcamentoUseCase.execute(new Orcamento(ordemServicoId, valorTotal, metodoPagamento));
        } catch (RuntimeException e) {
            // Compensação: sem orçamento a OS não pode seguir. Avisa os outros
            // serviços para desfazerem o que já fizeram.
            LOG.errorf(e, "Falha ao gerar orçamento da OS %s", ordemServicoId);
            eventPublisher.publicar(new OrcamentoFalhouEvent(ordemServicoId, e.getMessage()));
        }
    }

    /**
     * Execução concluída: o orçamento passa a refletir o valor final da OS e a
     * cobrança é criada no provedor de pagamentos.
     */
    public void onExecucaoConcluida(JsonObject json) {
        UUID ordemServicoId = UUID.fromString(json.getString("ordemServicoId"));
        atualizarOrcamentoUseCase.executar(ordemServicoId, lerValor(json));
        try {
            gerarCobrancaUseCase.paraOrdemDeServico(ordemServicoId);
        } catch (RuntimeException e) {
            // O valor final já foi gravado; rejeitar o evento não ajudaria. O
            // atendente gera o link de novo em POST /api/orcamentos/{id}/pagamento.
            LOG.errorf(e, "Falha ao criar a cobrança da OS %s no provedor de pagamentos", ordemServicoId);
        }
    }

    /** OS cancelada: compensa cancelando o orçamento. */
    public void onOrdemCancelada(JsonObject json) {
        cancelarOrcamentoUseCase.executar(UUID.fromString(json.getString("ordemServicoId")));
    }

    private static BigDecimal lerValor(JsonObject json) {
        Object valor = json.getValue("valorTotal");
        if (valor == null) {
            throw new IllegalArgumentException("valorTotal é obrigatório no evento.");
        }
        return new BigDecimal(valor.toString());
    }
}
