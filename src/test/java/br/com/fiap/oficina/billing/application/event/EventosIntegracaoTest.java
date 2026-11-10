package br.com.fiap.oficina.billing.application.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/** Os nomes das routing keys e dos campos são o contrato com os outros serviços. */
class EventosIntegracaoTest {

    private final UUID orcamentoId = UUID.randomUUID();
    private final UUID osId = UUID.randomUUID();

    @Test
    @DisplayName("OrcamentoGeradoEvent")
    void orcamentoGerado() {
        OrcamentoGeradoEvent evento = new OrcamentoGeradoEvent(orcamentoId, osId, new BigDecimal("120.50"));

        JsonObject json = evento.toJson();

        assertEquals("billing.orcamento-gerado", evento.routingKey());
        assertEquals(orcamentoId.toString(), json.getString("orcamentoId"));
        assertEquals(osId.toString(), json.getString("ordemServicoId"));
        assertEquals("120.50", json.getString("valorTotal"));
    }

    @Test
    @DisplayName("OrcamentoAceitoEvent")
    void orcamentoAceito() {
        UUID aceito = UUID.randomUUID();
        UUID rejeitado = UUID.randomUUID();
        OrcamentoAceitoEvent evento = new OrcamentoAceitoEvent(orcamentoId, osId, List.of(aceito),
                List.of(rejeitado));

        JsonObject json = evento.toJson();

        assertEquals("billing.orcamento-aceito", evento.routingKey());
        assertEquals(orcamentoId.toString(), json.getString("orcamentoId"));
        assertEquals(osId.toString(), json.getString("ordemServicoId"));
        assertEquals(new JsonArray().add(aceito.toString()), json.getJsonArray("servicosAceitos"));
        assertEquals(new JsonArray().add(rejeitado.toString()), json.getJsonArray("servicosRejeitados"));
    }

    @Test
    @DisplayName("OrcamentoRejeitadoEvent")
    void orcamentoRejeitado() {
        OrcamentoRejeitadoEvent evento = new OrcamentoRejeitadoEvent(orcamentoId, osId);

        assertEquals("billing.orcamento-rejeitado", evento.routingKey());
        assertEquals(orcamentoId.toString(), evento.toJson().getString("orcamentoId"));
        assertEquals(osId.toString(), evento.toJson().getString("ordemServicoId"));
    }

    @Test
    @DisplayName("OrcamentoPagoEvent")
    void orcamentoPago() {
        OrcamentoPagoEvent evento = new OrcamentoPagoEvent(orcamentoId, osId);

        assertEquals("billing.pagamento-confirmado", evento.routingKey());
        assertEquals(orcamentoId.toString(), evento.toJson().getString("orcamentoId"));
        assertEquals(osId.toString(), evento.toJson().getString("ordemServicoId"));
    }

    @Test
    @DisplayName("OrcamentoFalhouEvent")
    void orcamentoFalhou() {
        OrcamentoFalhouEvent evento = new OrcamentoFalhouEvent(osId, "motivo");

        assertEquals("billing.orcamento-falhou", evento.routingKey());
        assertEquals(osId.toString(), evento.toJson().getString("ordemServicoId"));
        assertEquals("motivo", evento.toJson().getString("motivo"));
    }

    @Test
    @DisplayName("OrcamentoCanceladoEvent")
    void orcamentoCancelado() {
        OrcamentoCanceladoEvent comEstorno = new OrcamentoCanceladoEvent(orcamentoId, osId, true);
        OrcamentoCanceladoEvent semEstorno = new OrcamentoCanceladoEvent(orcamentoId, osId, false);

        assertEquals("billing.orcamento-cancelado", comEstorno.routingKey());
        assertEquals(orcamentoId.toString(), comEstorno.toJson().getString("orcamentoId"));
        assertEquals(osId.toString(), comEstorno.toJson().getString("ordemServicoId"));
        assertTrue(comEstorno.toJson().getBoolean("estornoNecessario"));
        assertFalse(semEstorno.toJson().getBoolean("estornoNecessario"));
    }
}
