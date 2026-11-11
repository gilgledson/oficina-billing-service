package br.com.fiap.oficina.billing.domain.valueObject;

public enum MetodoPagamento {
    DINHEIRO,
    CARTAO_CREDITO,
    CARTAO_DEBITO,
    PIX,
    BOLETO,
    /** Saldo da conta do cliente no provedor de pagamentos. */
    SALDO_EM_CONTA
}






