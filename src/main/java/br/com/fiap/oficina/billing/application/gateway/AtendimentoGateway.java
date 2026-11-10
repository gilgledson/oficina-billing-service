package br.com.fiap.oficina.billing.application.gateway;

import br.com.fiap.oficina.billing.application.dto.ClienteOrcamentoDTO;
import br.com.fiap.oficina.billing.application.dto.VeiculoOrcamentoDTO;
import java.util.Optional;
import java.util.UUID;

public interface AtendimentoGateway {
    Optional<ClienteOrcamentoDTO> buscarClientePorId(UUID id);
    Optional<VeiculoOrcamentoDTO> buscarVeiculoPorId(UUID id);
}






