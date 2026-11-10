package br.com.fiap.oficina.billing.application.usecase.comandos;

import java.util.List;
import java.util.UUID;

public interface AprovarOrcamentoManualUseCase {
    void executar(UUID orcamentoId, String assinaturaUrl, List<UUID> servicosAceitos, List<UUID> servicosRejeitados);
}






