package br.com.fiap.oficina.billing.infrastructure.pagamento;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import br.com.fiap.oficina.billing.application.gateway.PagamentoGateway;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import br.com.fiap.oficina.billing.infrastructure.pagamento.MercadoPagoClient.BackUrls;
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
 *
 * <p>Cada orçamento tem uma única preferência: cobrar de novo atualiza a que
 * já existe, para não deixar um link antigo pagável com outro valor.
 */
@ApplicationScoped
public class MercadoPagoPagamentoGateway implements PagamentoGateway {

    private static final String MOEDA = "BRL";
    private static final String CATEGORIA = "services";
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATA_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private final MercadoPagoClient client;
    private final Optional<String> accessToken;
    private final Optional<String> notificationUrl;
    private final Optional<String> backUrl;
    private final String statementDescriptor;

    public MercadoPagoPagamentoGateway(
            @RestClient MercadoPagoClient client,
            @ConfigProperty(name = "oficina.mercadopago.access-token") Optional<String> accessToken,
            @ConfigProperty(name = "oficina.mercadopago.notification-url") Optional<String> notificationUrl,
            @ConfigProperty(name = "oficina.mercadopago.back-url") Optional<String> backUrl,
            @ConfigProperty(name = "oficina.mercadopago.statement-descriptor", defaultValue = "OFICINA")
            String statementDescriptor) {
        this.client = client;
        this.accessToken = accessToken.filter(valor -> !valor.isBlank());
        this.notificationUrl = notificationUrl.filter(valor -> !valor.isBlank());
        this.backUrl = backUrl.filter(valor -> !valor.isBlank());
        this.statementDescriptor = statementDescriptor;
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
                        "Serviços e peças da ordem de serviço " + orcamento.getOrdemServicoId(),
                        CATEGORIA,
                        1,
                        MOEDA,
                        orcamento.getValorTotal())),
                orcamento.getId().toString(),
                notificationUrl.orElse(null),
                statementDescriptor,
                backUrl.map(url -> new BackUrls(url, url, url)).orElse(null),
                // Volta sozinho para o site da oficina quando o pagamento é aprovado.
                backUrl.map(url -> "approved").orElse(null),
                vencimento(orcamento) != null ? Boolean.TRUE : null,
                vencimento(orcamento));

        String preferenciaExistente = orcamento.getPagamentoExternoId();
        PreferenciaResponse response = preferenciaExistente == null || preferenciaExistente.isBlank()
                ? client.criarPreferencia(autorizacao(), request)
                : client.atualizarPreferencia(autorizacao(), preferenciaExistente, request);
        return new Cobranca(response.id(), response.initPoint());
    }

    /** O link deixa de aceitar pagamento no fim do dia de vencimento do orçamento. */
    private static String vencimento(Orcamento orcamento) {
        if (orcamento.getDataVencimento() == null) {
            return null;
        }
        return orcamento.getDataVencimento().atTime(23, 59, 59).atZone(FUSO).format(DATA_ISO);
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
