package br.com.fiap.oficina.billing.application.event;

import java.util.UUID;

import io.vertx.core.json.JsonObject;

public record OrcamentoRejeitadoEvent(UUID orcamentoId, UUID ordemServicoId) implements EventoIntegracao {

    public static final String ROUTING_KEY = "billing.orcamento-rejeitado";

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
