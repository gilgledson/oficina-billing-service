package br.com.fiap.oficina.billing.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.jboss.logmanager.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.oficina.billing.application.event.OrcamentoPagoEvent;
import br.com.fiap.oficina.billing.infrastructure.persistence.outbox.OutboxEventoJpaEntity;
import br.com.fiap.oficina.billing.infrastructure.persistence.outbox.OutboxEventoRepository;
import br.com.fiap.oficina.billing.shared.web.CorrelationIdFilter;
import io.vertx.core.json.JsonObject;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventoRepository repository;

    @InjectMocks
    private OutboxEventPublisher publisher;

    @AfterEach
    void limparMdc() {
        MDC.remove(CorrelationIdFilter.MDC_KEY);
    }

    @Test
    @DisplayName("Deve gravar o evento na outbox como pendente, com o correlationId da requisição")
    void deveGravarEventoNaOutbox() {
        UUID orcamentoId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        MDC.put(CorrelationIdFilter.MDC_KEY, "corr-123");

        publisher.publicar(new OrcamentoPagoEvent(orcamentoId, osId));

        ArgumentCaptor<OutboxEventoJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventoJpaEntity.class);
        verify(repository).persist(captor.capture());
        OutboxEventoJpaEntity gravado = captor.getValue();
        assertNotNull(gravado.getId());
        assertNotNull(gravado.getCriadoEm());
        assertNull(gravado.getPublicadoEm());
        assertEquals("billing.pagamento-confirmado", gravado.getRoutingKey());
        assertEquals("corr-123", gravado.getCorrelationId());
        JsonObject payload = new JsonObject(gravado.getPayload());
        assertEquals(orcamentoId.toString(), payload.getString("orcamentoId"));
        assertEquals(osId.toString(), payload.getString("ordemServicoId"));
    }

    @Test
    @DisplayName("Sem requisição em andamento o evento é gravado sem correlationId")
    void deveGravarSemCorrelationId() {
        publisher.publicar(new OrcamentoPagoEvent(UUID.randomUUID(), UUID.randomUUID()));

        ArgumentCaptor<OutboxEventoJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventoJpaEntity.class);
        verify(repository).persist(captor.capture());
        assertNull(captor.getValue().getCorrelationId());
    }
}
