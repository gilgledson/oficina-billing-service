package br.com.fiap.oficina.billing.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.jboss.logmanager.MDC;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.oficina.billing.application.listener.GerarOrcamentoOnOrdemFinalizadaListener;
import br.com.fiap.oficina.billing.shared.web.CorrelationIdFilter;
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;

@ExtendWith(MockitoExtension.class)
class OsEventsConsumerTest {

    @Mock
    private GerarOrcamentoOnOrdemFinalizadaListener listener;

    @InjectMocks
    private OsEventsConsumer consumer;

    private final JsonObject payload = new JsonObject().put("ordemServicoId", UUID.randomUUID().toString());
    private final AtomicBoolean ack = new AtomicBoolean();
    private final AtomicBoolean nack = new AtomicBoolean();

    private Message<JsonObject> mensagem(String routingKey, String correlationId) {
        IncomingRabbitMQMetadata metadata = mock(IncomingRabbitMQMetadata.class);
        when(metadata.getRoutingKey()).thenReturn(routingKey);
        when(metadata.getHeader(CorrelationIdFilter.HEADER, String.class))
                .thenReturn(Optional.ofNullable(correlationId));
        return Message.of(payload, Metadata.of(metadata),
                () -> {
                    ack.set(true);
                    return CompletableFuture.completedFuture(null);
                },
                falha -> {
                    nack.set(true);
                    return CompletableFuture.completedFuture(null);
                });
    }

    @Test
    @DisplayName("Diagnóstico concluído deve gerar orçamento e confirmar a mensagem")
    void deveDespacharDiagnosticoConcluido() {
        consumer.consumir(mensagem(OsEventsConsumer.DIAGNOSTICO_CONCLUIDO, null));

        verify(listener).onDiagnosticoConcluido(payload);
        assertTrue(ack.get());
        assertFalse(nack.get());
    }

    @Test
    @DisplayName("Execução concluída deve atualizar o orçamento")
    void deveDespacharExecucaoConcluida() {
        consumer.consumir(mensagem(OsEventsConsumer.EXECUCAO_CONCLUIDA, null));

        verify(listener).onExecucaoConcluida(payload);
        assertTrue(ack.get());
    }

    @Test
    @DisplayName("OS cancelada deve disparar a compensação")
    void deveDespacharOrdemCancelada() {
        consumer.consumir(mensagem(OsEventsConsumer.ORDEM_CANCELADA, null));

        verify(listener).onOrdemCancelada(payload);
        assertTrue(ack.get());
    }

    @Test
    @DisplayName("OS rejeitada direto no OS Service também cancela o orçamento")
    void deveDespacharOrdemRejeitada() {
        consumer.consumir(mensagem(OsEventsConsumer.ORDEM_REJEITADA, null));

        verify(listener).onOrdemCancelada(payload);
        assertTrue(ack.get());
    }

    @Test
    @DisplayName("Evento desconhecido deve ser confirmado e ignorado")
    void deveIgnorarEventoDesconhecido() {
        consumer.consumir(mensagem("os.aberta", null));

        verifyNoInteractions(listener);
        assertTrue(ack.get());
    }

    @Test
    @DisplayName("Falha no processamento deve rejeitar a mensagem (DLQ)")
    void deveRejeitarMensagemQuandoProcessamentoFalha() {
        doThrow(new IllegalStateException("falha")).when(listener).onExecucaoConcluida(any());

        consumer.consumir(mensagem(OsEventsConsumer.EXECUCAO_CONCLUIDA, null));

        assertTrue(nack.get());
        assertFalse(ack.get());
    }

    @Test
    @DisplayName("Deve propagar o correlationId do cabeçalho para o log e limpar ao final")
    void devePropagarCorrelationId() {
        AtomicReference<String> correlationIdDuranteProcessamento = new AtomicReference<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            correlationIdDuranteProcessamento.set(MDC.get(CorrelationIdFilter.MDC_KEY));
            return null;
        }).when(listener).onOrdemCancelada(any());

        consumer.consumir(mensagem(OsEventsConsumer.ORDEM_CANCELADA, "corr-123"));

        assertEquals("corr-123", correlationIdDuranteProcessamento.get());
        assertEquals(null, MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    @DisplayName("Mensagem sem metadados do RabbitMQ deve ser confirmada e ignorada")
    void deveIgnorarMensagemSemMetadados() {
        consumer.consumir(Message.of(payload));

        verifyNoInteractions(listener);
    }
}
