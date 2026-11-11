package br.com.fiap.oficina.billing.application.gateway;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;

/**
 * Porta de saída para o provedor de pagamentos. A aplicação não conhece o
 * Mercado Pago: só pede uma cobrança e consulta um pagamento.
 */
public interface PagamentoGateway {

    /** Falso quando o provedor não está configurado; o pagamento segue manual. */
    boolean habilitado();

    /** Cria no provedor a cobrança do valor atual do orçamento. */
    Cobranca criarCobranca(Orcamento orcamento);

    /** Consulta um pagamento pelo identificador que o provedor informou na notificação. */
    Optional<Pagamento> consultarPagamento(String pagamentoId);

    record Cobranca(String id, String link) {
    }

    record Pagamento(String id, StatusPagamento status, UUID orcamentoId, MetodoPagamento metodoPagamento,
            BigDecimal valor, String detalhe) {
    }

    enum StatusPagamento {
        APROVADO,
        RECUSADO,
        PENDENTE
    }
}
