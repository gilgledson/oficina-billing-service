package br.com.fiap.oficina.billing.application.event;

import java.util.UUID;

import io.vertx.core.json.JsonObject;

public record OrcamentoPagoEvent(UUID orcamentoId, UUID ordemServicoId) implements EventoIntegracao {

    public static final String ROUTING_KEY = "billing.pagamento-confirmado";

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put("orcamentoId", orcamentoId.toString())
                .put("ordemServicoId", ordemServicoId.toString());
    }
}
