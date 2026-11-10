package br.com.fiap.oficina.billing.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

import org.jboss.logmanager.MDC;

import br.com.fiap.oficina.billing.application.event.EventoIntegracao;
import br.com.fiap.oficina.billing.application.gateway.EventPublisher;
import br.com.fiap.oficina.billing.infrastructure.persistence.outbox.OutboxEventoJpaEntity;
import br.com.fiap.oficina.billing.infrastructure.persistence.outbox.OutboxEventoRepository;
import br.com.fiap.oficina.billing.shared.web.CorrelationIdFilter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Grava o evento na tabela de outbox, dentro da transação de quem chamou. A
 * entrega ao RabbitMQ é feita depois pelo {@link OutboxRelay}.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class OutboxEventPublisher implements EventPublisher {

    private final OutboxEventoRepository repository;

    @Override
    @Transactional
    public void publicar(EventoIntegracao evento) {
        OutboxEventoJpaEntity entity = new OutboxEventoJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setRoutingKey(evento.routingKey());
        entity.setPayload(evento.toJson().encode());
        entity.setCorrelationId(MDC.get(CorrelationIdFilter.MDC_KEY));
        entity.setCriadoEm(Instant.now());
        repository.persist(entity);
    }
}
