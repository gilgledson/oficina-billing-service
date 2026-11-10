package br.com.fiap.oficina.billing.application.event;

import java.util.UUID;

import io.vertx.core.json.JsonObject;

/**
 * Compensação da Saga: o orçamento foi cancelado porque a OS foi cancelada.
 * {@code estornoNecessario} indica que o orçamento já estava pago e o valor
 * precisa ser devolvido ao cliente.
 */
public record OrcamentoCanceladoEvent(UUID orcamentoId, UUID ordemServicoId, boolean estornoNecessario)
        implements EventoIntegracao {

    public static final String ROUTING_KEY = "billing.orcamento-cancelado";

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject()
                .put("orcamentoId", orcamentoId.toString())
                .put("ordemServicoId", ordemServicoId.toString())
                .put("estornoNecessario", estornoNecessario);
    }
}
