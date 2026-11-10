package br.com.fiap.oficina.billing.infrastructure.persistence.outbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Evento de integração aguardando entrega ao broker (Transactional Outbox).
 * É gravado na mesma transação do dado de negócio, então ou os dois existem
 * ou nenhum existe.
 */
@Entity
@Table(name = "outbox_evento")
@Getter
@Setter
public class OutboxEventoJpaEntity {

    @Id
    private UUID id;

    @Column(name = "routing_key", nullable = false)
    private String routingKey;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "publicado_em")
    private Instant publicadoEm;
}
