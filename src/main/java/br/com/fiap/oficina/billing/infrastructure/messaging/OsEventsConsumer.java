package br.com.fiap.oficina.billing.infrastructure.messaging;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;
import org.jboss.logmanager.MDC;

import br.com.fiap.oficina.billing.application.listener.GerarOrcamentoOnOrdemFinalizadaListener;
import br.com.fiap.oficina.billing.shared.web.CorrelationIdFilter;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

/**
 * Consome da fila do Billing os eventos publicados pelo OS Service e entrega
 * ao listener da aplicação conforme a routing key.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class OsEventsConsumer {

    public static final String DIAGNOSTICO_CONCLUIDO = "os.diagnostico-concluido";
    public static final String EXECUCAO_CONCLUIDA = "os.execucao-concluida";
    public static final String ORDEM_CANCELADA = "os.cancelada";
    public static final String ORDEM_REJEITADA = "os.rejeitada";

    private static final Logger LOG = Logger.getLogger(OsEventsConsumer.class);

    private final GerarOrcamentoOnOrdemFinalizadaListener listener;

    @Incoming("os-events")
    @Blocking
    public CompletionStage<Void> consumir(Message<JsonObject> mensagem) {
        Optional<IncomingRabbitMQMetadata> metadata = mensagem.getMetadata(IncomingRabbitMQMetadata.class);
        String routingKey = metadata.map(IncomingRabbitMQMetadata::getRoutingKey).orElse("");
        String correlationId = metadata
                .flatMap(m -> m.getHeader(CorrelationIdFilter.HEADER, String.class))
                .orElse(null);

        if (correlationId != null) {
            MDC.put(CorrelationIdFilter.MDC_KEY, correlationId);
        }
        try {
            despachar(routingKey, mensagem.getPayload());
            return mensagem.ack();
        } catch (RuntimeException e) {
            // nack: a mensagem vai para a fila de mensagens mortas (DLQ) em vez
            // de ser perdida ou reprocessada em loop.
            LOG.errorf(e, "Falha ao processar evento %s", routingKey);
            return mensagem.nack(e);
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }

    void despachar(String routingKey, JsonObject payload) {
        switch (routingKey) {
            case DIAGNOSTICO_CONCLUIDO -> listener.onDiagnosticoConcluido(payload);
            case EXECUCAO_CONCLUIDA -> listener.onExecucaoConcluida(payload);
            // OS cancelada ou rejeitada direto no OS Service: o orçamento deixa de valer.
            case ORDEM_CANCELADA, ORDEM_REJEITADA -> listener.onOrdemCancelada(payload);
            default -> LOG.debugf("Evento %s ignorado pelo Billing", routingKey);
        }
    }
}
