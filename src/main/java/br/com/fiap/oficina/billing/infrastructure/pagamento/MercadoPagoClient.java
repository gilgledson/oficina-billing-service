package br.com.fiap.oficina.billing.infrastructure.pagamento;

import java.math.BigDecimal;
import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

/** API REST do Mercado Pago: Checkout Pro (preferências) e consulta de pagamentos. */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "mercadopago")
public interface MercadoPagoClient {

    @POST
    @Path("checkout/preferences")
    PreferenciaResponse criarPreferencia(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            PreferenciaRequest request);

    @PUT
    @Path("checkout/preferences/{id}")
    PreferenciaResponse atualizarPreferencia(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            @PathParam("id") String id, PreferenciaRequest request);

    @GET
    @Path("v1/payments/{id}")
    PagamentoResponse buscarPagamento(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            @PathParam("id") String id);

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PreferenciaRequest(
            List<Item> items,
            @JsonProperty("external_reference") String externalReference,
            @JsonProperty("notification_url") String notificationUrl,
            @JsonProperty("statement_descriptor") String statementDescriptor,
            @JsonProperty("back_urls") BackUrls backUrls,
            @JsonProperty("auto_return") String autoReturn,
            Boolean expires,
            @JsonProperty("expiration_date_to") String expirationDateTo) {
    }

    record BackUrls(String success, String pending, String failure) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Item(
            String id,
            String title,
            String description,
            @JsonProperty("category_id") String categoryId,
            int quantity,
            @JsonProperty("currency_id") String currencyId,
            @JsonProperty("unit_price") BigDecimal unitPrice) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PreferenciaResponse(
            String id,
            @JsonProperty("init_point") String initPoint) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PagamentoResponse(
            String id,
            String status,
            @JsonProperty("status_detail") String statusDetail,
            @JsonProperty("external_reference") String externalReference,
            @JsonProperty("payment_type_id") String paymentTypeId,
            @JsonProperty("transaction_amount") BigDecimal transactionAmount) {
    }
}
