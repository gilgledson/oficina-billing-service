package br.com.fiap.oficina.billing.application.event;

import java.util.UUID;

import io.vertx.core.json.JsonObject;

/**
 * Compensação da Saga: o Billing não conseguiu gerar o orçamento da OS. Os
 * outros serviços devem desfazer o que já fizeram (liberar estoque reservado,
 * tirar a OS da fila, cancelar a OS).
 */
public record OrcamentoFalhouEvent(UUID ordemServicoId, String motivo) implements EventoIntegracao {

    public static final String ROUTING_KEY = "billing.orcamento-falhou";

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put("ordemServicoId", ordemServicoId.toString())
                .put("motivo", motivo);
    }
}
