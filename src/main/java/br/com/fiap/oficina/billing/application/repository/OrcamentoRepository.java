package br.com.fiap.oficina.billing.application.repository;

import java.util.Optional;
import java.util.UUID;

import br.com.fiap.oficina.billing.domain.entity.Orcamento;

public interface OrcamentoRepository {

    Orcamento salvar(Orcamento orcamento);

    Optional<Orcamento> buscarPorId(UUID id);

    Optional<Orcamento> buscarPorOrdemServicoId(UUID ordemServicoId);

    Orcamento atualizar(Orcamento orcamento);
}
