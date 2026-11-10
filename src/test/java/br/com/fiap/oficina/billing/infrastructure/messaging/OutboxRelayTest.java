package br.com.fiap.oficina.billing.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.oficina.billing.infrastructure.persistence.outbox.OutboxEventoJpaEntity;
import br.com.fiap.oficina.billing.infrastructure.persistence.outbox.OutboxEventoRepository;
import br.com.fiap.oficina.billing.shared.web.CorrelationIdFilter;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventoRepository repository;

    @Mock
    private Emitter<JsonObject> emitter;

    private OutboxRelay relay;
    private final List<Message<JsonObject>> enviadas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(repository, emitter);
    }

    private static OutboxEventoJpaEntity evento(String routingKey, String correlationId) {
        OutboxEventoJpaEntity evento = new OutboxEventoJpaEntity();
        evento.setId(UUID.randomUUID());
        evento.setRoutingKey(routingKey);
        evento.setPayload(new JsonObject().put("ordemServicoId", "abc").encode());
        evento.setCorrelationId(correlationId);
        evento.setCriadoEm(Instant.now());
        return evento;
    }

    /** Simula o broker: confirma (ack) ou recusa (nack) cada mensagem enviada. */
    @SuppressWarnings("unchecked")
    private void brokerResponde(boolean confirma) {
        doAnswer(invocation -> {
            Message<JsonObject> mensagem = invocation.getArgument(0);
            enviadas.add(mensagem);
            if (confirma) {
                mensagem.ack();
            } else {
                mensagem.nack(new IllegalStateException("broker indisponível"));
            }
            return null;
        }).when(emitter).send(any(Message.class));
    }

    @Test
    @DisplayName("Deve enviar o evento com routing key, id e correlationId e marcar como publicado")
    void devePublicarEventoPendente() {
        OutboxEventoJpaEntity evento = evento("billing.orcamento-gerado", "corr-123");
        when(repository.buscarPendentes(anyInt())).thenReturn(List.of(evento));
        brokerResponde(true);

        relay.publicarPendentes();

        assertEquals(1, enviadas.size());
        Message<JsonObject> mensagem = enviadas.get(0);
        assertEquals("abc", mensagem.getPayload().getString("ordemServicoId"));
        OutgoingRabbitMQMetadata metadata = mensagem.getMetadata(OutgoingRabbitMQMetadata.class).orElseThrow();
        assertEquals("billing.orcamento-gerado", metadata.getRoutingKey());
        assertEquals(evento.getId().toString(), metadata.getMessageId());
        assertEquals("corr-123", metadata.getCorrelationId());
        assertEquals("corr-123", metadata.getHeaders().get(CorrelationIdFilter.HEADER));
        verify(repository).marcarPublicado(evento.getId());
    }

    @Test
    @DisplayName("Evento sem correlationId deve ser enviado sem o cabeçalho")
    void devePublicarEventoSemCorrelationId() {
        OutboxEventoJpaEntity evento = evento("billing.orcamento-aceito", null);
        when(repository.buscarPendentes(anyInt())).thenReturn(List.of(evento));
        brokerResponde(true);

        relay.publicarPendentes();

        OutgoingRabbitMQMetadata metadata = enviadas.get(0).getMetadata(OutgoingRabbitMQMetadata.class).orElseThrow();
        assertEquals(null, metadata.getCorrelationId());
        verify(repository).marcarPublicado(evento.getId());
    }

    @Test
    @DisplayName("Broker recusou: o evento continua pendente e os seguintes esperam o próximo ciclo")
    void deveManterPendenteQuandoBrokerRecusa() {
        OutboxEventoJpaEntity primeiro = evento("billing.orcamento-gerado", null);
        OutboxEventoJpaEntity segundo = evento("billing.orcamento-aceito", null);
        when(repository.buscarPendentes(anyInt())).thenReturn(List.of(primeiro, segundo));
        brokerResponde(false);

        relay.publicarPendentes();

        assertEquals(1, enviadas.size());
        verify(repository, never()).marcarPublicado(any());
    }

    @Test
    @DisplayName("Falha ao enviar (emitter) também mantém o evento pendente")
    @SuppressWarnings("unchecked")
    void deveManterPendenteQuandoEnvioFalha() {
        OutboxEventoJpaEntity evento = evento("billing.orcamento-gerado", null);
        when(repository.buscarPendentes(anyInt())).thenReturn(List.of(evento));
        org.mockito.Mockito.doThrow(new IllegalStateException("sem conexão")).when(emitter).send(any(Message.class));

        relay.publicarPendentes();

        verify(repository, never()).marcarPublicado(any());
    }

    @Test
    @DisplayName("Deve publicar todos os pendentes, na ordem")
    void devePublicarTodosNaOrdem() {
        OutboxEventoJpaEntity primeiro = evento("billing.orcamento-gerado", null);
        OutboxEventoJpaEntity segundo = evento("billing.orcamento-aceito", null);
        when(repository.buscarPendentes(anyInt())).thenReturn(List.of(primeiro, segundo));
        brokerResponde(true);

        relay.publicarPendentes();

        assertEquals(2, enviadas.size());
        verify(repository, times(1)).marcarPublicado(primeiro.getId());
        verify(repository, times(1)).marcarPublicado(segundo.getId());
    }
}
