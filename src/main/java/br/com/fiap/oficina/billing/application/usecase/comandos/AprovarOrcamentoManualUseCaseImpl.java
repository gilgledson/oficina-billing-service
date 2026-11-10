package br.com.fiap.oficina.billing.application.usecase.comandos;

import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina.billing.application.event.OrcamentoAceitoEvent;
import br.com.fiap.oficina.billing.application.event.OrcamentoRejeitadoEvent;
import br.com.fiap.oficina.billing.application.gateway.EventPublisher;
import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class AprovarOrcamentoManualUseCaseImpl implements AprovarOrcamentoManualUseCase {

    private final OrcamentoRepository repository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public void executar(UUID orcamentoId, String assinaturaUrl, List<UUID> servicosAceitos,
            List<UUID> servicosRejeitados) {
        Orcamento orcamento = repository.buscarPorId(orcamentoId)
                .orElseThrow(() -> new NotFoundException("Orcamento não encontrada"));

        List<UUID> aceitos = servicosAceitos == null ? List.of() : servicosAceitos;
        List<UUID> rejeitados = servicosRejeitados == null ? List.of() : servicosRejeitados;

        orcamento.registrarAceiteManual(assinaturaUrl);

        if (aceitos.isEmpty()) {
            // Nenhum serviço aceito = orçamento rejeitado. Dispara a compensação
            // da Saga: o OS Service libera o estoque reservado e rejeita a OS.
            orcamento.cancelar();
            repository.atualizar(orcamento);
            eventPublisher.publicar(new OrcamentoRejeitadoEvent(orcamento.getId(), orcamento.getOrdemServicoId()));
            return;
        }

        repository.atualizar(orcamento);
        eventPublisher.publicar(new OrcamentoAceitoEvent(orcamento.getId(), orcamento.getOrdemServicoId(),
                aceitos, rejeitados));
    }
}
