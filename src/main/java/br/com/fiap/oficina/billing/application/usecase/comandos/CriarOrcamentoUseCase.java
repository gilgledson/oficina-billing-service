package br.com.fiap.oficina.billing.application.usecase.comandos;

import br.com.fiap.oficina.billing.domain.entity.Orcamento;

public interface CriarOrcamentoUseCase {

    /**
     * Gera o orçamento de uma OS. É idempotente: se a OS já tem orçamento (o
     * broker entregou o mesmo evento duas vezes), só atualiza o valor.
     */
    void execute(Orcamento orcamento);
}
