package br.com.fiap.oficina.billing.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrcamentoDTOTest {

    @Test
    @DisplayName("ClienteOrcamentoDTO deve armazenar todos os campos corretamente")
    void deveArmazenarClienteorcamentoDTO() {
        UUID id = UUID.randomUUID();
        ClienteOrcamentoDTO dto = new ClienteOrcamentoDTO(
                id, "João Silva", "joao@test.com", "12345678901", "(11)99999-9999", "Rua A, 100, SP");

        assertEquals(id, dto.getId());
        assertEquals("João Silva", dto.getNome());
        assertEquals("joao@test.com", dto.getEmail());
        assertEquals("12345678901", dto.getCpfCnpj());
        assertEquals("(11)99999-9999", dto.getTelefone());
        assertEquals("Rua A, 100, SP", dto.getEnderecoCompleto());
    }

    @Test
    @DisplayName("VeiculoOrcamentoDTO deve armazenar todos os campos corretamente")
    void deveArmazenarVeiculoorcamentoDTO() {
        UUID id = UUID.randomUUID();
        VeiculoOrcamentoDTO dto = new VeiculoOrcamentoDTO(id, "ABC-1234", "Honda", "Civic", 2022);

        assertEquals(id, dto.getId());
        assertEquals("ABC-1234", dto.getPlaca());
        assertEquals("Honda", dto.getMarca());
        assertEquals("Civic", dto.getModelo());
        assertEquals(2022, dto.getAno());
    }

    @Test
    @DisplayName("OrdemServicoOrcamentoDTO deve armazenar campos e inner classes")
    void deveArmazenarOrdemServicoorcamentoDTO() {
        UUID id = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        BigDecimal total = new BigDecimal("750.00");

        OrdemServicoOrcamentoDTO.ItemProdutoDTO produto = new OrdemServicoOrcamentoDTO.ItemProdutoDTO(
                "Óleo 5W30", new BigDecimal("2"), new BigDecimal("50.00"), new BigDecimal("100.00"));

        OrdemServicoOrcamentoDTO.ItemServicoDTO servico = new OrdemServicoOrcamentoDTO.ItemServicoDTO(
                "Troca de óleo", 1, new BigDecimal("150.00"), new BigDecimal("250.00"), List.of(produto));

        OrdemServicoOrcamentoDTO dto = new OrdemServicoOrcamentoDTO(
                id, clienteId, veiculoId, List.of(servico), total);

        assertEquals(id, dto.getId());
        assertEquals(clienteId, dto.getClienteId());
        assertEquals(veiculoId, dto.getVeiculoId());
        assertEquals(total, dto.getValorTotal());
        assertEquals(1, dto.getServicos().size());

        OrdemServicoOrcamentoDTO.ItemServicoDTO s = dto.getServicos().get(0);
        assertEquals("Troca de óleo", s.getNome());
        assertEquals(1, s.getQuantidade());
        assertEquals(new BigDecimal("150.00"), s.getPrecoUnitario());
        assertEquals(new BigDecimal("250.00"), s.getTotal());
        assertEquals(1, s.getProdutos().size());

        OrdemServicoOrcamentoDTO.ItemProdutoDTO p = s.getProdutos().get(0);
        assertEquals("Óleo 5W30", p.getNome());
        assertEquals(new BigDecimal("2"), p.getQuantidade());
        assertEquals(new BigDecimal("50.00"), p.getPrecoUnitario());
        assertEquals(new BigDecimal("100.00"), p.getTotal());
    }
}









