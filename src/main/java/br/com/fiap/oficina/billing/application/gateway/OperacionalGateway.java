package br.com.fiap.oficina.billing.application.gateway;

import br.com.fiap.oficina.billing.application.dto.OrdemServicoOrcamentoDTO;
import java.util.Optional;
import java.util.UUID;

public interface OperacionalGateway {
    Optional<OrdemServicoOrcamentoDTO> buscarOrdemDeServicoPorId(UUID id);
}






