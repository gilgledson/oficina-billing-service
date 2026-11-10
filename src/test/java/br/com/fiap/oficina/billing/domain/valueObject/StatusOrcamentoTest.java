package br.com.fiap.oficina.billing.domain.valueObject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusOrcamentoTest {

    @Test
    @DisplayName("fromId(1) deve retornar PENDENTE")
    void deveRetornarPendenteParaId1() {
        assertEquals(StatusOrcamento.PENDENTE, StatusOrcamento.fromId(1));
    }

    @Test
    @DisplayName("fromId(2) deve retornar PAGO")
    void deveRetornarPagoParaId2() {
        assertEquals(StatusOrcamento.PAGO, StatusOrcamento.fromId(2));
    }

    @Test
    @DisplayName("fromId(3) deve retornar CANCELADA")
    void deveRetornarCanceladaParaId3() {
        assertEquals(StatusOrcamento.CANCELADA, StatusOrcamento.fromId(3));
    }

    @Test
    @DisplayName("fromId com ID inválido deve lançar IllegalArgumentException")
    void deveLancarExcecaoParaIdInvalido() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StatusOrcamento.fromId(99));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    @DisplayName("Deve retornar nome e id corretos para cada status")
    void deveRetornarNomeEIdCorretos() {
        assertEquals(1, StatusOrcamento.PENDENTE.getId());
        assertEquals("Pendente", StatusOrcamento.PENDENTE.getNome());

        assertEquals(2, StatusOrcamento.PAGO.getId());
        assertEquals("Pago", StatusOrcamento.PAGO.getNome());

        assertEquals(3, StatusOrcamento.CANCELADA.getId());
        assertEquals("Cancelada", StatusOrcamento.CANCELADA.getNome());
    }
}









