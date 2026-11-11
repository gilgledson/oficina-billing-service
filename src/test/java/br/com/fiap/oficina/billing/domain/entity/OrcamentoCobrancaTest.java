package br.com.fiap.oficina.billing.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;

class OrcamentoCobrancaTest {

    private final Orcamento orcamento = new Orcamento(UUID.randomUUID(), new BigDecimal("185.00"), null);

    @Test
    @DisplayName("Registra a cobrança sem mudar o status do orçamento")
    void deveRegistrarCobranca() {
        orcamento.registrarCobranca("pref-1", "https://mp/pagar");

        assertEquals("pref-1", orcamento.getPagamentoExternoId());
        assertEquals("https://mp/pagar", orcamento.getLinkPagamento());
        assertEquals(StatusOrcamento.PENDENTE, orcamento.getStatus());
    }

    @Test
    @DisplayName("Exige o identificador e o link da cobrança")
    void deveExigirIdentificadorELink() {
        assertThrows(IllegalArgumentException.class, () -> orcamento.registrarCobranca(null, "https://mp/pagar"));
        assertThrows(IllegalArgumentException.class, () -> orcamento.registrarCobranca(" ", "https://mp/pagar"));
        assertThrows(IllegalArgumentException.class, () -> orcamento.registrarCobranca("pref-1", null));
        assertThrows(IllegalArgumentException.class, () -> orcamento.registrarCobranca("pref-1", " "));
    }
}
