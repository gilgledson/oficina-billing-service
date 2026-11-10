package br.com.fiap.oficina.billing.application.usecase.comandos;

import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import java.util.UUID;

public interface ConfirmarPagamentoOrcamentoUseCase {
    void execute(UUID orcamentoId, MetodoPagamento metodoPagamento);
}






