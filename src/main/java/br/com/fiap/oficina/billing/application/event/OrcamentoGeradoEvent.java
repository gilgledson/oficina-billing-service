package br.com.fiap.oficina.billing.application.event;

import java.math.BigDecimal;
import java.util.UUID;

import io.vertx.core.json.JsonObject;

/** Passo da Saga: o orçamento da OS foi gerado e aguarda aprovação do cliente. */
public record OrcamentoGeradoEvent(UUID orcamentoId, UUID ordemServicoId, BigDecimal valorTotal)
        implements EventoIntegracao {

    public static final String ROUTING_KEY = "billing.orcamento-gerado";

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put("orcamentoId", orcamentoId.toString())
                .put("ordemServicoId", ordemServicoId.toString())
                .put("valorTotal", valorTotal.toPlainString());
    }
}
