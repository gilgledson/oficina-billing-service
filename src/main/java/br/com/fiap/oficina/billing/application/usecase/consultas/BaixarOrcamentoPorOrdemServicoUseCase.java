package br.com.fiap.oficina.billing.application.usecase.consultas;

import java.util.UUID;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;

public interface BaixarOrcamentoPorOrdemServicoUseCase {
    byte[] executar(UUID ordemServicoId);
    Orcamento buscarPorOrdemServicoId(UUID ordemServicoId);
}






