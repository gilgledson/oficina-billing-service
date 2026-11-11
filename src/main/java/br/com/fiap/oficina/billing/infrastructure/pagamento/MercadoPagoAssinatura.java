package br.com.fiap.oficina.billing.infrastructure.pagamento;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Confere a assinatura que o Mercado Pago envia no header {@code x-signature}
 * das notificações ({@code ts=<timestamp>,v1=<hmac>}). O HMAC-SHA256 é
 * calculado, com a chave secreta do webhook, sobre
 * {@code id:<data.id>;request-id:<x-request-id>;ts:<ts>;}.
 *
 * <p>Sem chave configurada nenhuma notificação é aceita: um webhook público
 * sem conferência de origem deixaria qualquer um disparar consultas ao
 * Mercado Pago em nome da oficina.
 *
 * <p>O {@code ts} não tem prazo de validade de propósito. O Mercado Pago reenvia
 * a mesma notificação por até um dia, e repetir uma notificação antiga não
 * causa dano: o pagamento é sempre consultado na API e a confirmação é
 * idempotente.
 */
@ApplicationScoped
public class MercadoPagoAssinatura {

    private static final String ALGORITMO = "HmacSHA256";

    private final Optional<String> segredo;

    public MercadoPagoAssinatura(
            @ConfigProperty(name = "oficina.mercadopago.webhook-secret") Optional<String> segredo) {
        this.segredo = segredo.filter(valor -> !valor.isBlank());
    }

    public boolean valida(String xSignature, String xRequestId, String dataId) {
        if (segredo.isEmpty() || xSignature == null || dataId == null) {
            return false;
        }
        String ts = null;
        String v1 = null;
        for (String parte : xSignature.split(",")) {
            String[] chaveValor = parte.split("=", 2);
            if (chaveValor.length != 2) {
                continue;
            }
            switch (chaveValor[0].trim()) {
                case "ts" -> ts = chaveValor[1].trim();
                case "v1" -> v1 = chaveValor[1].trim();
                default -> { }
            }
        }
        if (ts == null || v1 == null) {
            return false;
        }

        StringBuilder manifesto = new StringBuilder("id:").append(dataId.toLowerCase(Locale.ROOT)).append(';');
        if (xRequestId != null && !xRequestId.isBlank()) {
            manifesto.append("request-id:").append(xRequestId).append(';');
        }
        manifesto.append("ts:").append(ts).append(';');

        String esperado = hmac(manifesto.toString(), segredo.get());
        return MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8),
                v1.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    static String hmac(String mensagem, String chave) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(chave.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            return HexFormat.of().formatHex(mac.doFinal(mensagem.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível calcular o HMAC da notificação.", e);
        }
    }
}
