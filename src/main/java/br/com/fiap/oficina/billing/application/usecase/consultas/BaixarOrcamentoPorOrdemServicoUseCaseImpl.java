package br.com.fiap.oficina.billing.application.usecase.consultas;

import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class BaixarOrcamentoPorOrdemServicoUseCaseImpl implements BaixarOrcamentoPorOrdemServicoUseCase {

    private final OrcamentoRepository orcamentoRepository;

    private final BaixarOrcamentoUseCase baixarOrcamentoUseCase;

    @Override
    public byte[] executar(UUID ordemServicoId) {
        Orcamento orcamento = buscarPorOrdemServicoId(ordemServicoId);
        return baixarOrcamentoUseCase.executar(orcamento.getId());
    }

    @Override
    public Orcamento buscarPorOrdemServicoId(UUID ordemServicoId) {
        return orcamentoRepository.buscarPorOrdemServicoId(ordemServicoId)
                .orElseThrow(
                        () -> new NotFoundException(
                                "Orcamento não encontrada para a ordem de serviço " + ordemServicoId));
    }
}
