package br.com.fiap.oficina.billing.application.usecase.comandos;

import java.util.UUID;

import br.com.fiap.oficina.billing.application.event.OrcamentoPagoEvent;
import br.com.fiap.oficina.billing.application.gateway.EventPublisher;
import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class ConfirmarPagamentoOrcamentoUseCaseImpl implements ConfirmarPagamentoOrcamentoUseCase {

    private final OrcamentoRepository repository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public void execute(UUID orcamentoId, MetodoPagamento metodoPagamento) {
        Orcamento orcamento = repository.buscarPorId(orcamentoId)
                .orElseThrow(() -> new NotFoundException("Orcamento não encontrada"));

        orcamento.pagar(metodoPagamento);
        repository.atualizar(orcamento);

        eventPublisher.publicar(new OrcamentoPagoEvent(orcamento.getId(), orcamento.getOrdemServicoId()));
    }
}
