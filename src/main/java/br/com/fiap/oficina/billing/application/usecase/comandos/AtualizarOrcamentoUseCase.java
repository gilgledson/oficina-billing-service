package br.com.fiap.oficina.billing.application.usecase.comandos;

import java.math.BigDecimal;
import java.util.UUID;

import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class AtualizarOrcamentoUseCase {

    private final OrcamentoRepository repository;

    public AtualizarOrcamentoUseCase(OrcamentoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void executar(UUID ordemServicoId, BigDecimal novoValor) {
        Orcamento orcamento = repository.buscarPorOrdemServicoId(ordemServicoId)
                .orElseThrow(() -> new NotFoundException("Orçamento não encontrado para a OS " + ordemServicoId));
        orcamento.setValorTotal(novoValor);
        repository.atualizar(orcamento);
    }
}
