package br.com.fiap.oficina.billing.application.usecase.comandos;

import java.util.UUID;

import br.com.fiap.oficina.billing.application.event.OrcamentoCanceladoEvent;
import br.com.fiap.oficina.billing.application.gateway.EventPublisher;
import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Compensação da Saga no Billing: a OS foi cancelada (pelo atendente ou por
 * falha na execução), então o orçamento dela deixa de valer.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class CancelarOrcamentoUseCase {

    private final OrcamentoRepository repository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void executar(UUID ordemServicoId) {
        Orcamento orcamento = repository.buscarPorOrdemServicoId(ordemServicoId).orElse(null);
        // OS cancelada antes do diagnóstico não tem orçamento; evento repetido
        // encontra o orçamento já cancelado. Nos dois casos não há o que desfazer.
        if (orcamento == null || orcamento.getStatus() == StatusOrcamento.CANCELADA) {
            return;
        }

        boolean estornoNecessario = orcamento.getStatus() == StatusOrcamento.PAGO;
        orcamento.cancelar();
        repository.atualizar(orcamento);

        eventPublisher.publicar(new OrcamentoCanceladoEvent(orcamento.getId(), orcamento.getOrdemServicoId(),
                estornoNecessario));
    }
}
