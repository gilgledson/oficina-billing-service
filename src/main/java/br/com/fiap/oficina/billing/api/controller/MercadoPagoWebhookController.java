package br.com.fiap.oficina.billing.api.controller;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import br.com.fiap.oficina.billing.application.usecase.comandos.ProcessarNotificacaoPagamentoUseCase;
import br.com.fiap.oficina.billing.infrastructure.pagamento.MercadoPagoAssinatura;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

/**
 * Recebe as notificações (webhooks) do Mercado Pago. É a única rota pública do
 * Billing: quem chama é o Mercado Pago, sem JWT. A autenticidade vem da
 * assinatura {@code x-signature} e da consulta do pagamento na API deles.
 */
@Path("/api/orcamentos/webhooks/mercadopago")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.WILDCARD)
@Tag(name = "pagamento", description = "Integração com o Mercado Pago")
@RequiredArgsConstructor
public class MercadoPagoWebhookController {

    private static final Logger LOG = Logger.getLogger(MercadoPagoWebhookController.class);
    private static final String TIPO_PAGAMENTO = "payment";

    private final ProcessarNotificacaoPagamentoUseCase processarNotificacao;
    private final MercadoPagoAssinatura assinatura;

    @POST
    @PermitAll
    @Operation(summary = "Notificação de pagamento do Mercado Pago")
    public Response receber(
            @HeaderParam("x-signature") String xSignature,
            @HeaderParam("x-request-id") String xRequestId,
            @QueryParam("data.id") String dataIdQuery,
            @QueryParam("type") String typeQuery,
            @QueryParam("topic") String topicQuery,
            @QueryParam("id") String idQuery,
            String corpo) {

        JsonObject json = lerCorpo(corpo);
        String tipo = primeiro(json.getString("type"), typeQuery, topicQuery);
        JsonObject data = json.getJsonObject("data");
        String pagamentoId = primeiro(
                dataIdQuery,
                data != null && data.getValue("id") != null ? data.getValue("id").toString() : null,
                idQuery);

        if (!assinatura.valida(xSignature, xRequestId, pagamentoId)) {
            LOG.warnf("Notificação do Mercado Pago com assinatura inválida (pagamento %s)", pagamentoId);
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        // O Mercado Pago notifica outros assuntos (merchant_order etc.); só pagamento interessa.
        if (!TIPO_PAGAMENTO.equals(tipo) || pagamentoId == null) {
            return Response.ok().build();
        }

        // Se o processamento falhar, o erro vira 500 e o Mercado Pago reenvia a notificação.
        processarNotificacao.executar(pagamentoId);
        return Response.ok().build();
    }

    private static JsonObject lerCorpo(String corpo) {
        if (corpo == null || corpo.isBlank()) {
            return new JsonObject();
        }
        try {
            return new JsonObject(corpo);
        } catch (RuntimeException e) {
            return new JsonObject();
        }
    }

    private static String primeiro(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }
}
