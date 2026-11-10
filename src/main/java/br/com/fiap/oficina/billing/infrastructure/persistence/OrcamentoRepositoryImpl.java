package br.com.fiap.oficina.billing.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OrcamentoRepositoryImpl implements OrcamentoRepository, PanacheRepositoryBase<OrcamentoJpaEntity, UUID> {

    @Override
    @Transactional
    public Orcamento salvar(Orcamento orcamento) {
        OrcamentoJpaEntity entity = OrcamentoJpaEntity.fromEntity(orcamento);
        persist(entity);
        return entity.toEntity();
    }

    @Override
    public Optional<Orcamento> buscarPorId(UUID id) {
        return findByIdOptional(id).map(OrcamentoJpaEntity::toEntity);
    }

    @Override
    public Optional<Orcamento> buscarPorOrdemServicoId(UUID ordemServicoId) {
        return find("ordemServicoId", ordemServicoId).firstResultOptional()
                .map(OrcamentoJpaEntity::toEntity);
    }

    @Override
    @Transactional
    public Orcamento atualizar(Orcamento orcamento) {
        OrcamentoJpaEntity entity = OrcamentoJpaEntity.fromEntity(orcamento);
        getEntityManager().merge(entity);
        return entity.toEntity();
    }
}
