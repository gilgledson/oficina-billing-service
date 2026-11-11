package br.com.fiap.oficina.billing.application.event;

import java.util.UUID;

import io.vertx.core.json.JsonObject;

/**
 * O provedor recusou uma tentativa de pagamento. O orçamento continua pendente
 * e a OS continua aguardando pagamento: o cliente pode tentar de novo no mesmo
 * link.
 */
public record PagamentoRecusadoEvent(UUID orcamentoId, UUID ordemServicoId, String pagamentoId, String motivo)
        implements EventoIntegracao {

    public static final String ROUTING_KEY = "billing.pagamento-recusado";

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put("orcamentoId", orcamentoId.toString())
                .put("ordemServicoId", ordemServicoId.toString())
                .put("pagamentoId", pagamentoId)
                .put("motivo", motivo);
    }
}
