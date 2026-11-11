package br.com.fiap.oficina.billing.application.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

class PagamentoRecusadoEventTest {

    @Test
    @DisplayName("Evento de pagamento recusado leva o orçamento, a OS, o pagamento e o motivo")
    void deveSerializar() {
        UUID orcamentoId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();

        PagamentoRecusadoEvent evento = new PagamentoRecusadoEvent(orcamentoId, osId, "99", "cc_rejected_other_reason");

        assertEquals("billing.pagamento-recusado", evento.routingKey());
        JsonObject json = evento.toJson();
        assertEquals(orcamentoId.toString(), json.getString("orcamentoId"));
        assertEquals(osId.toString(), json.getString("ordemServicoId"));
        assertEquals("99", json.getString("pagamentoId"));
        assertEquals("cc_rejected_other_reason", json.getString("motivo"));
    }
}
