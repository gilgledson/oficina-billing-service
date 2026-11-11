package br.com.fiap.oficina.billing.infrastructure.pagamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MercadoPagoAssinaturaTest {

    private static final String SEGREDO = "segredo-do-webhook";
    private static final String TS = "1704908010";

    private final MercadoPagoAssinatura assinatura = new MercadoPagoAssinatura(Optional.of(SEGREDO));

    private static String header(String manifesto) {
        return "ts=" + TS + ",v1=" + MercadoPagoAssinatura.hmac(manifesto, SEGREDO);
    }

    @Test
    @DisplayName("HMAC-SHA256 confere com o vetor de teste da RFC 4231")
    void deveCalcularHmac() {
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
                MercadoPagoAssinatura.hmac("what do ya want for nothing?", "Jefe"));
    }

    @Test
    @DisplayName("Aceita assinatura calculada sobre id, request-id e ts")
    void deveAceitarAssinaturaValida() {
        String header = header("id:123456;request-id:req-1;ts:" + TS + ";");

        assertTrue(assinatura.valida(header, "req-1", "123456"));
        assertTrue(assinatura.valida(header.replace(",", " , "), "req-1", "123456"));
    }

    @Test
    @DisplayName("O id entra em minúsculas e o request-id é omitido quando não vem")
    void deveMontarManifestoComoODoMercadoPago() {
        assertTrue(assinatura.valida(header("id:abc123;ts:" + TS + ";"), null, "ABC123"));
    }

    @Test
    @DisplayName("Recusa assinatura de outro pagamento, adulterada ou incompleta")
    void deveRecusarAssinaturaInvalida() {
        String header = header("id:123456;request-id:req-1;ts:" + TS + ";");

        assertFalse(assinatura.valida(header, "req-1", "999999"));
        assertFalse(assinatura.valida(header, "req-2", "123456"));
        assertFalse(assinatura.valida("ts=" + TS + ",v1=00ff", "req-1", "123456"));
        assertFalse(assinatura.valida("ts=" + TS, "req-1", "123456"));
        assertFalse(assinatura.valida("lixo", "req-1", "123456"));
        assertFalse(assinatura.valida(null, "req-1", "123456"));
        assertFalse(assinatura.valida(header, "req-1", null));
    }

    @Test
    @DisplayName("Sem chave configurada, a conferência é dispensada")
    void deveDispensarConferenciaSemChave() {
        assertTrue(new MercadoPagoAssinatura(Optional.empty()).valida(null, null, null));
        assertTrue(new MercadoPagoAssinatura(Optional.of(" ")).valida("qualquer", null, "1"));
    }
}
