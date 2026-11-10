package br.com.fiap.oficina.billing.domain.entity;

import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrcamentoTest {

    @Test
    @DisplayName("Deve criar Orcamento com construtor simples com valores padrão")
    void deveCriarFaturaComValoresPadrao() {
        UUID osId = UUID.randomUUID();
        BigDecimal valor = new BigDecimal("500.00");

        Orcamento orcamento = new Orcamento(osId, valor, MetodoPagamento.PIX);

        assertNotNull(orcamento.getId());
        assertEquals(osId, orcamento.getOrdemServicoId());
        assertEquals(valor, orcamento.getValorTotal());
        assertEquals(MetodoPagamento.PIX, orcamento.getMetodoPagamento());
        assertEquals(StatusOrcamento.PENDENTE, orcamento.getStatus());
        assertNotNull(orcamento.getDataEmissao());
        assertNotNull(orcamento.getDataVencimento());
        assertEquals(orcamento.getDataEmissao().plusDays(30), orcamento.getDataVencimento());
    }

    @Test
    @DisplayName("Deve criar Orcamento via construtor completo preservando todos os campos")
    void deveCriarFaturaComConstrutorCompleto() {
        UUID id = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        LocalDate emissao = LocalDate.of(2025, 1, 1);
        LocalDate vencimento = LocalDate.of(2025, 1, 31);
        BigDecimal valor = new BigDecimal("1000.00");

        Orcamento orcamento = new Orcamento(id, emissao, osId, vencimento, valor, StatusOrcamento.PAGO,
                MetodoPagamento.CARTAO_CREDITO);

        assertEquals(id, orcamento.getId());
        assertEquals(osId, orcamento.getOrdemServicoId());
        assertEquals(emissao, orcamento.getDataEmissao());
        assertEquals(vencimento, orcamento.getDataVencimento());
        assertEquals(valor, orcamento.getValorTotal());
        assertEquals(StatusOrcamento.PAGO, orcamento.getStatus());
        assertEquals(MetodoPagamento.CARTAO_CREDITO, orcamento.getMetodoPagamento());
    }

    @Test
    @DisplayName("Deve criar Orcamento via reconstruir com todos os campos")
    void deveCriarFaturaViaReconstruir() {
        UUID id = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        LocalDate emissao = LocalDate.now();
        LocalDate vencimento = emissao.plusDays(30);
        BigDecimal valor = new BigDecimal("750.00");

        Orcamento orcamento = Orcamento.reconstruir(id, emissao, osId, vencimento, valor, StatusOrcamento.PENDENTE,
                MetodoPagamento.DINHEIRO, null);

        assertEquals(id, orcamento.getId());
        assertEquals(osId, orcamento.getOrdemServicoId());
        assertEquals(StatusOrcamento.PENDENTE, orcamento.getStatus());
        assertEquals(MetodoPagamento.DINHEIRO, orcamento.getMetodoPagamento());
    }

    @Test
    @DisplayName("Deve validar Orcamento sem erros quando todos os campos estão preenchidos")
    void deveValidarFaturaSemErros() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), new BigDecimal("200.00"), MetodoPagamento.PIX);
        assertDoesNotThrow(orcamento::validar);
    }

    @Test
    @DisplayName("Deve lançar exceção na validação quando ordemServicoId for nulo")
    void deveLancarExcecaoQuandoOrdemServicoIdNulo() {
        Orcamento orcamento = new Orcamento((UUID) null, new BigDecimal("200.00"), MetodoPagamento.PIX);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, orcamento::validar);
        assertTrue(ex.getMessage().contains("Ordem de serviço"));
    }

    @Test
    @DisplayName("Deve lançar exceção na validação quando dataEmissao for nula")
    void deveLancarExcecaoQuandoDataEmissaoNula() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), new BigDecimal("200.00"), MetodoPagamento.PIX);
        orcamento.setDataEmissao(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, orcamento::validar);
        assertTrue(ex.getMessage().contains("Data de emissão"));
    }

    @Test
    @DisplayName("Deve lançar exceção na validação quando dataVencimento for nula")
    void deveLancarExcecaoQuandoDataVencimentoNula() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), new BigDecimal("200.00"), MetodoPagamento.PIX);
        orcamento.setDataVencimento(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, orcamento::validar);
        assertTrue(ex.getMessage().contains("Data de vencimento"));
    }

    @Test
    @DisplayName("Deve lançar exceção na validação quando valorTotal for nulo")
    void deveLancarExcecaoQuandoValorTotalNulo() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), new BigDecimal("200.00"), MetodoPagamento.PIX);
        orcamento.setValorTotal(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, orcamento::validar);
        assertTrue(ex.getMessage().contains("Valor total"));
    }

    @Test
    @DisplayName("Deve lançar exceção na validação quando status for nulo")
    void deveLancarExcecaoQuandoStatusNulo() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), new BigDecimal("200.00"), MetodoPagamento.PIX);
        orcamento.setStatus(null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, orcamento::validar);
        assertTrue(ex.getMessage().contains("Status"));
    }

    @Test
    @DisplayName("Deve cancelar Orcamento alterando status para CANCELADA")
    void deveCancelarFatura() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), new BigDecimal("300.00"), MetodoPagamento.DINHEIRO);
        orcamento.cancelar();
        assertEquals(StatusOrcamento.CANCELADA, orcamento.getStatus());
    }

    @Test
    @DisplayName("Deve pagar Orcamento alterando status para PAGO e gravando método de pagamento")
    void devePagarFatura() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), new BigDecimal("300.00"), MetodoPagamento.DINHEIRO);
        orcamento.pagar(MetodoPagamento.CARTAO_DEBITO);
        assertEquals(StatusOrcamento.PAGO, orcamento.getStatus());
        assertEquals(MetodoPagamento.CARTAO_DEBITO, orcamento.getMetodoPagamento());
    }

    @Test
    @DisplayName("Deve lançar exceção ao pagar Orcamento com método de pagamento nulo")
    void deveLancarExcecaoAoPagarComMetodoNulo() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), new BigDecimal("300.00"), MetodoPagamento.DINHEIRO);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> orcamento.pagar(null));
        assertTrue(ex.getMessage().contains("Método de pagamento"));
    }

    @Test
    @DisplayName("Deve marcar Orcamento como pendente")
    void deveMarcarFaturaComoPendente() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), new BigDecimal("300.00"), MetodoPagamento.PIX);
        orcamento.cancelar();
        assertEquals(StatusOrcamento.CANCELADA, orcamento.getStatus());
        orcamento.pendente();
        assertEquals(StatusOrcamento.PENDENTE, orcamento.getStatus());
    }

    @Test
    @DisplayName("Deve atualizar todos os campos do Orcamento")
    void deveAtualizaOrcamento() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), new BigDecimal("300.00"), MetodoPagamento.PIX);
        UUID novoOsId = UUID.randomUUID();
        LocalDate novaEmissao = LocalDate.of(2025, 3, 1);
        LocalDate novoVencimento = LocalDate.of(2025, 3, 31);
        BigDecimal novoValor = new BigDecimal("999.99");

        orcamento.atualizar(novoOsId, novaEmissao, novoVencimento, novoValor, StatusOrcamento.PAGO,
                MetodoPagamento.CARTAO_CREDITO);

        assertEquals(novoOsId, orcamento.getOrdemServicoId());
        assertEquals(novaEmissao, orcamento.getDataEmissao());
        assertEquals(novoVencimento, orcamento.getDataVencimento());
        assertEquals(novoValor, orcamento.getValorTotal());
        assertEquals(StatusOrcamento.PAGO, orcamento.getStatus());
        assertEquals(MetodoPagamento.CARTAO_CREDITO, orcamento.getMetodoPagamento());
    }
}

