package br.com.fiap.oficina.billing.application.event;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonObject;

public record OrcamentoAceitoEvent(UUID orcamentoId, UUID ordemServicoId, List<UUID> servicosAceitos,
        List<UUID> servicosRejeitados) implements EventoIntegracao {

    public static final String ROUTING_KEY = "billing.orcamento-aceito";

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put("orcamentoId", orcamentoId.toString())
                .put("ordemServicoId", ordemServicoId.toString())
                .put("servicosAceitos", servicosAceitos.stream().map(UUID::toString).collect(Collectors.toList()))
                .put("servicosRejeitados",
                        servicosRejeitados.stream().map(UUID::toString).collect(Collectors.toList()));
    }
}
