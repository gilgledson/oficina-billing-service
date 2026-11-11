package br.com.fiap.oficina.billing.infrastructure.pagamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.oficina.billing.application.gateway.PagamentoGateway.Cobranca;
import br.com.fiap.oficina.billing.application.gateway.PagamentoGateway.Pagamento;
import br.com.fiap.oficina.billing.application.gateway.PagamentoGateway.StatusPagamento;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;
import br.com.fiap.oficina.billing.infrastructure.pagamento.MercadoPagoClient.PagamentoResponse;
import br.com.fiap.oficina.billing.infrastructure.pagamento.MercadoPagoClient.PreferenciaRequest;
import br.com.fiap.oficina.billing.infrastructure.pagamento.MercadoPagoClient.PreferenciaResponse;
import jakarta.ws.rs.WebApplicationException;

@ExtendWith(MockitoExtension.class)
class MercadoPagoPagamentoGatewayTest {

    private static final String TOKEN = "TEST-token";

    @Mock
    private MercadoPagoClient client;

    private MercadoPagoPagamentoGateway gateway(String token, String notificationUrl) {
        return gateway(token, notificationUrl, null);
    }

    private MercadoPagoPagamentoGateway gateway(String token, String notificationUrl, String backUrl) {
        return new MercadoPagoPagamentoGateway(client, Optional.ofNullable(token),
                Optional.ofNullable(notificationUrl), Optional.ofNullable(backUrl), "OFICINA");
    }

    @Test
    @DisplayName("Só fica habilitado com access token")
    void deveIndicarSeEstaHabilitado() {
        assertTrue(gateway(TOKEN, null).habilitado());
        assertFalse(gateway(null, null).habilitado());
        assertFalse(gateway("  ", null).habilitado());
    }

    @Test
    @DisplayName("Cria a preferência com o valor do orçamento e o ID dele como referência externa")
    void deveCriarCobranca() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), null, UUID.randomUUID(), null,
                new BigDecimal("185.00"), StatusOrcamento.PENDENTE, null);
        when(client.criarPreferencia(eq("Bearer " + TOKEN), any()))
                .thenReturn(new PreferenciaResponse("pref-1", "https://mp/pagar"));

        Cobranca cobranca = gateway(TOKEN, "https://oficina/webhook").criarCobranca(orcamento);

        assertEquals(new Cobranca("pref-1", "https://mp/pagar"), cobranca);
        ArgumentCaptor<PreferenciaRequest> captor = ArgumentCaptor.forClass(PreferenciaRequest.class);
        verify(client).criarPreferencia(eq("Bearer " + TOKEN), captor.capture());
        PreferenciaRequest request = captor.getValue();
        assertEquals(orcamento.getId().toString(), request.externalReference());
        assertEquals("https://oficina/webhook", request.notificationUrl());
        assertEquals(1, request.items().size());
        assertEquals(new BigDecimal("185.00"), request.items().get(0).unitPrice());
        assertEquals("BRL", request.items().get(0).currencyId());
        assertEquals(1, request.items().get(0).quantity());
        assertEquals("services", request.items().get(0).categoryId());
        assertTrue(request.items().get(0).description().contains(orcamento.getOrdemServicoId().toString()));
        assertEquals("OFICINA", request.statementDescriptor());
        // Sem data de vencimento e sem página de retorno, esses campos não vão.
        assertNull(request.expires());
        assertNull(request.expirationDateTo());
        assertNull(request.backUrls());
        assertNull(request.autoReturn());
    }

    @Test
    @DisplayName("O link expira no fim do dia de vencimento e volta para a página da oficina")
    void deveEnviarVencimentoEPaginaDeRetorno() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), java.time.LocalDate.of(2026, 11, 11),
                UUID.randomUUID(), java.time.LocalDate.of(2026, 12, 11), new BigDecimal("185.00"),
                StatusOrcamento.PENDENTE, null);
        when(client.criarPreferencia(any(), any())).thenReturn(new PreferenciaResponse("pref-3", "https://mp/3"));

        gateway(TOKEN, null, "https://oficina/pagamento").criarCobranca(orcamento);

        ArgumentCaptor<PreferenciaRequest> captor = ArgumentCaptor.forClass(PreferenciaRequest.class);
        verify(client).criarPreferencia(any(), captor.capture());
        PreferenciaRequest request = captor.getValue();
        assertEquals(Boolean.TRUE, request.expires());
        assertEquals("2026-12-11T23:59:59.000-03:00", request.expirationDateTo());
        assertEquals("https://oficina/pagamento", request.backUrls().success());
        assertEquals("https://oficina/pagamento", request.backUrls().pending());
        assertEquals("https://oficina/pagamento", request.backUrls().failure());
        assertEquals("approved", request.autoReturn());
    }

    @Test
    @DisplayName("Cobrar de novo atualiza a preferência que já existe em vez de criar outra")
    void deveAtualizarPreferenciaExistente() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), new BigDecimal("300.00"), null);
        orcamento.registrarCobranca("pref-1", "https://mp/pagar");
        when(client.atualizarPreferencia(eq("Bearer " + TOKEN), eq("pref-1"), any()))
                .thenReturn(new PreferenciaResponse("pref-1", "https://mp/pagar"));

        Cobranca cobranca = gateway(TOKEN, null).criarCobranca(orcamento);

        assertEquals(new Cobranca("pref-1", "https://mp/pagar"), cobranca);
        verify(client, org.mockito.Mockito.never()).criarPreferencia(any(), any());
    }

    @Test
    @DisplayName("Sem URL de notificação configurada, a preferência vai sem ela")
    void deveCriarCobrancaSemNotificationUrl() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), new BigDecimal("10.00"), null);
        when(client.criarPreferencia(any(), any())).thenReturn(new PreferenciaResponse("pref-2", "https://mp/2"));

        gateway(TOKEN, " ").criarCobranca(orcamento);

        ArgumentCaptor<PreferenciaRequest> captor = ArgumentCaptor.forClass(PreferenciaRequest.class);
        verify(client).criarPreferencia(any(), captor.capture());
        assertNull(captor.getValue().notificationUrl());
    }

    @Test
    @DisplayName("Sem access token, não chama o Mercado Pago")
    void deveFalharSemToken() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), new BigDecimal("10.00"), null);

        assertThrows(IllegalStateException.class, () -> gateway(null, null).criarCobranca(orcamento));
    }

    @Test
    @DisplayName("Traduz o pagamento consultado para o modelo da aplicação")
    void deveConsultarPagamento() {
        UUID orcamentoId = UUID.randomUUID();
        when(client.buscarPagamento("Bearer " + TOKEN, "99")).thenReturn(new PagamentoResponse(
                "99", "approved", "accredited", orcamentoId.toString(), "bank_transfer", new BigDecimal("185.00")));

        Optional<Pagamento> pagamento = gateway(TOKEN, null).consultarPagamento("99");

        assertEquals(Optional.of(new Pagamento("99", StatusPagamento.APROVADO, orcamentoId, MetodoPagamento.PIX,
                new BigDecimal("185.00"), "accredited")), pagamento);
    }

    @Test
    @DisplayName("Pagamento inexistente (404) vira vazio; outros erros sobem")
    void deveTratarErrosDaConsulta() {
        when(client.buscarPagamento(any(), eq("404"))).thenThrow(new WebApplicationException(404));
        when(client.buscarPagamento(any(), eq("500"))).thenThrow(new WebApplicationException(500));
        MercadoPagoPagamentoGateway gateway = gateway(TOKEN, null);

        assertTrue(gateway.consultarPagamento("404").isEmpty());
        assertThrows(WebApplicationException.class, () -> gateway.consultarPagamento("500"));
    }

    @Test
    @DisplayName("Mapeia os status do Mercado Pago")
    void deveMapearStatus() {
        assertEquals(StatusPagamento.APROVADO, MercadoPagoPagamentoGateway.status("approved"));
        assertEquals(StatusPagamento.RECUSADO, MercadoPagoPagamentoGateway.status("rejected"));
        assertEquals(StatusPagamento.RECUSADO, MercadoPagoPagamentoGateway.status("cancelled"));
        assertEquals(StatusPagamento.PENDENTE, MercadoPagoPagamentoGateway.status("in_process"));
        assertEquals(StatusPagamento.PENDENTE, MercadoPagoPagamentoGateway.status(null));
    }

    @Test
    @DisplayName("Mapeia os meios de pagamento do Mercado Pago")
    void deveMapearMeioDePagamento() {
        assertEquals(MetodoPagamento.CARTAO_CREDITO, MercadoPagoPagamentoGateway.metodo("credit_card"));
        assertEquals(MetodoPagamento.CARTAO_DEBITO, MercadoPagoPagamentoGateway.metodo("debit_card"));
        assertEquals(MetodoPagamento.PIX, MercadoPagoPagamentoGateway.metodo("bank_transfer"));
        assertEquals(MetodoPagamento.BOLETO, MercadoPagoPagamentoGateway.metodo("ticket"));
        assertEquals(MetodoPagamento.SALDO_EM_CONTA, MercadoPagoPagamentoGateway.metodo("account_money"));
        assertEquals(MetodoPagamento.SALDO_EM_CONTA, MercadoPagoPagamentoGateway.metodo(null));
    }

    @Test
    @DisplayName("Referência externa que não é um UUID é tratada como ausente")
    void deveLerReferencia() {
        UUID id = UUID.randomUUID();
        assertEquals(id, MercadoPagoPagamentoGateway.referencia(id.toString()));
        assertNull(MercadoPagoPagamentoGateway.referencia("pedido-123"));
        assertNull(MercadoPagoPagamentoGateway.referencia(null));
    }
}
