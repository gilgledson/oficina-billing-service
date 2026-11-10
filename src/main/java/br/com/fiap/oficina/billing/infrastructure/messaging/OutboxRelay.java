package br.com.fiap.oficina.billing.infrastructure.messaging;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.jboss.logging.Logger;

import br.com.fiap.oficina.billing.infrastructure.persistence.outbox.OutboxEventoJpaEntity;
import br.com.fiap.oficina.billing.infrastructure.persistence.outbox.OutboxEventoRepository;
import br.com.fiap.oficina.billing.shared.web.CorrelationIdFilter;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Entrega ao RabbitMQ os eventos pendentes da outbox. Um evento só é marcado
 * como publicado depois que o broker confirma o recebimento; se a entrega
 * falhar ele continua pendente e é reenviado no próximo ciclo. A garantia é
 * "pelo menos uma vez", por isso os consumidores precisam ser idempotentes.
 */
@ApplicationScoped
public class OutboxRelay {

    private static final Logger LOG = Logger.getLogger(OutboxRelay.class);
    private static final int LOTE = 50;
    private static final long TIMEOUT_CONFIRMACAO_SEGUNDOS = 5;

    private final OutboxEventoRepository repository;
    private final Emitter<JsonObject> emitter;

    public OutboxRelay(OutboxEventoRepository repository, @Channel("billing-events") Emitter<JsonObject> emitter) {
        this.repository = repository;
        this.emitter = emitter;
    }

    @Scheduled(every = "${oficina.outbox.intervalo:2s}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void publicarPendentes() {
        List<OutboxEventoJpaEntity> pendentes = repository.buscarPendentes(LOTE);
        for (OutboxEventoJpaEntity evento : pendentes) {
            if (!publicar(evento)) {
                // Preserva a ordem dos eventos: se um falhou, tenta de novo no
                // próximo ciclo antes de mandar os seguintes.
                return;
            }
        }
    }

    private boolean publicar(OutboxEventoJpaEntity evento) {
        CompletableFuture<Void> confirmacao = new CompletableFuture<>();

        OutgoingRabbitMQMetadata.Builder metadata = OutgoingRabbitMQMetadata.builder()
                .withRoutingKey(evento.getRoutingKey())
                .withMessageId(evento.getId().toString());
        if (evento.getCorrelationId() != null) {
            metadata.withCorrelationId(evento.getCorrelationId())
                    .withHeader(CorrelationIdFilter.HEADER, evento.getCorrelationId());
        }

        Message<JsonObject> mensagem = Message.of(new JsonObject(evento.getPayload()), Metadata.of(metadata.build()),
                () -> {
                    confirmacao.complete(null);
                    return CompletableFuture.completedFuture(null);
                },
                falha -> {
                    confirmacao.completeExceptionally(falha);
                    return CompletableFuture.completedFuture(null);
                });

        try {
            emitter.send(mensagem);
            confirmacao.get(TIMEOUT_CONFIRMACAO_SEGUNDOS, TimeUnit.SECONDS);
            repository.marcarPublicado(evento.getId());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            LOG.warnf("Evento %s (%s) não entregue ao broker; será reenviado: %s", evento.getId(),
                    evento.getRoutingKey(), e.getMessage());
            return false;
        }
    }
}
