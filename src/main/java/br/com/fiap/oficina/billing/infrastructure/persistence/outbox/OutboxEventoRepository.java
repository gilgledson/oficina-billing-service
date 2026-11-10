package br.com.fiap.oficina.billing.infrastructure.persistence.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OutboxEventoRepository implements PanacheRepositoryBase<OutboxEventoJpaEntity, UUID> {

    public List<OutboxEventoJpaEntity> buscarPendentes(int limite) {
        return find("publicadoEm is null", Sort.by("criadoEm")).page(0, limite).list();
    }

    @Transactional
    public void marcarPublicado(UUID id) {
        update("publicadoEm = ?1 where id = ?2", Instant.now(), id);
    }
}
