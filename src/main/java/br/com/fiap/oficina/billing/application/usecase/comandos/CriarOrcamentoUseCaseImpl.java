package br.com.fiap.oficina.billing.application.usecase.comandos;

import java.util.Optional;

import br.com.fiap.oficina.billing.application.event.OrcamentoGeradoEvent;
import br.com.fiap.oficina.billing.application.gateway.EventPublisher;
import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class CriarOrcamentoUseCaseImpl implements CriarOrcamentoUseCase {

    private final OrcamentoRepository orcamentoRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public void execute(Orcamento orcamento) {
        orcamento.validar();

        Optional<Orcamento> existente = orcamentoRepository.buscarPorOrdemServicoId(orcamento.getOrdemServicoId());
        if (existente.isPresent()) {
            Orcamento atual = existente.get();
            if (atual.getStatus() == StatusOrcamento.PENDENTE) {
                atual.setValorTotal(orcamento.getValorTotal());
                orcamentoRepository.atualizar(atual);
            }
            return;
        }

        orcamentoRepository.salvar(orcamento);
        eventPublisher.publicar(new OrcamentoGeradoEvent(orcamento.getId(), orcamento.getOrdemServicoId(),
                orcamento.getValorTotal()));
    }
}
