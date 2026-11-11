package br.com.fiap.oficina.billing.infrastructure.pagamento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import br.com.fiap.oficina.billing.application.gateway.PagamentoGateway;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import br.com.fiap.oficina.billing.infrastructure.pagamento.MercadoPagoClient.Item;
import br.com.fiap.oficina.billing.infrastructure.pagamento.MercadoPagoClient.PagamentoResponse;
import br.com.fiap.oficina.billing.infrastructure.pagamento.MercadoPagoClient.PreferenciaRequest;
import br.com.fiap.oficina.billing.infrastructure.pagamento.MercadoPagoClient.PreferenciaResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

/**
 * Adaptador do Mercado Pago (Checkout Pro). A cobrança é uma preferência de
 * pagamento cuja referência externa é o ID do orçamento; é por ela que o
 * pagamento notificado depois é ligado ao orçamento.
 */
@ApplicationScoped
public class MercadoPagoPagamentoGateway implements PagamentoGateway {

    private static final String MOEDA = "BRL";

    private final MercadoPagoClient client;
    private final Optional<String> accessToken;
    private final Optional<String> notificationUrl;

    public MercadoPagoPagamentoGateway(
            @RestClient MercadoPagoClient client,
            @ConfigProperty(name = "oficina.mercadopago.access-token") Optional<String> accessToken,
            @ConfigProperty(name = "oficina.mercadopago.notification-url") Optional<String> notificationUrl) {
        this.client = client;
        this.accessToken = accessToken.filter(valor -> !valor.isBlank());
        this.notificationUrl = notificationUrl.filter(valor -> !valor.isBlank());
    }

    @Override
    public boolean habilitado() {
        return accessToken.isPresent();
    }

    @Override
    public Cobranca criarCobranca(Orcamento orcamento) {
        PreferenciaRequest request = new PreferenciaRequest(
                List.of(new Item(
                        orcamento.getId().toString(),
                        "Ordem de serviço " + orcamento.getOrdemServicoId(),
                        1,
                        MOEDA,
                        orcamento.getValorTotal())),
                orcamento.getId().toString(),
                notificationUrl.orElse(null));

        PreferenciaResponse response = client.criarPreferencia(autorizacao(), request);
        return new Cobranca(response.id(), response.initPoint());
    }

    @Override
    public Optional<Pagamento> consultarPagamento(String pagamentoId) {
        PagamentoResponse response;
        try {
            response = client.buscarPagamento(autorizacao(), pagamentoId);
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                return Optional.empty();
            }
            throw e;
        }
        return Optional.of(new Pagamento(
                response.id(),
                status(response.status()),
                referencia(response.externalReference()),
                metodo(response.paymentTypeId()),
                response.transactionAmount(),
                response.statusDetail()));
    }

    private String autorizacao() {
        return "Bearer " + accessToken
                .orElseThrow(() -> new IllegalStateException("Access token do Mercado Pago não configurado."));
    }

    static StatusPagamento status(String status) {
        if (status == null) {
            return StatusPagamento.PENDENTE;
        }
        return switch (status) {
            case "approved" -> StatusPagamento.APROVADO;
            case "rejected", "cancelled" -> StatusPagamento.RECUSADO;
            // pending, in_process, authorized, in_mediation, refunded, charged_back
            default -> StatusPagamento.PENDENTE;
        };
    }

    static MetodoPagamento metodo(String paymentTypeId) {
        if (paymentTypeId == null) {
            return MetodoPagamento.SALDO_EM_CONTA;
        }
        return switch (paymentTypeId) {
            case "credit_card" -> MetodoPagamento.CARTAO_CREDITO;
            case "debit_card", "prepaid_card" -> MetodoPagamento.CARTAO_DEBITO;
            case "bank_transfer" -> MetodoPagamento.PIX;
            case "ticket", "atm" -> MetodoPagamento.BOLETO;
            // account_money e demais meios internos do Mercado Pago
            default -> MetodoPagamento.SALDO_EM_CONTA;
        };
    }

    static UUID referencia(String externalReference) {
        if (externalReference == null) {
            return null;
        }
        try {
            return UUID.fromString(externalReference);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
