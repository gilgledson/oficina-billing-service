package br.com.fiap.oficina.billing.application.usecase.comandos;

import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import java.util.UUID;

public interface ConfirmarPagamentoOrcamentoUseCase {
    /** Pagamento no balcão, registrado pelo atendente. */
    void execute(UUID orcamentoId, MetodoPagamento metodoPagamento);

    /** Pagamento aprovado no provedor de pagamentos. */
    void execute(UUID orcamentoId, MetodoPagamento metodoPagamento, String pagamentoId);
}






