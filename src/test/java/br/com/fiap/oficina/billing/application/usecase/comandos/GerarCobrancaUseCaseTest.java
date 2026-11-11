package br.com.fiap.oficina.billing.application.usecase.comandos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.oficina.billing.application.gateway.PagamentoGateway;
import br.com.fiap.oficina.billing.application.gateway.PagamentoGateway.Cobranca;
import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class GerarCobrancaUseCaseTest {

    @Mock
    private OrcamentoRepository repository;

    @Mock
    private PagamentoGateway pagamentoGateway;

    @InjectMocks
    private GerarCobrancaUseCase useCase;

    private static Orcamento orcamento(StatusOrcamento status) {
        return new Orcamento(UUID.randomUUID(), null, UUID.randomUUID(), null, new BigDecimal("185.00"), status,
                null);
    }

    @Test
    @DisplayName("Ao fim da execução, cria a cobrança e guarda o link no orçamento")
    void deveGerarCobrancaParaOrdemDeServico() {
        Orcamento orcamento = orcamento(StatusOrcamento.PENDENTE);
        when(pagamentoGateway.habilitado()).thenReturn(true);
        when(repository.buscarPorOrdemServicoId(orcamento.getOrdemServicoId())).thenReturn(Optional.of(orcamento));
        when(pagamentoGateway.criarCobranca(orcamento)).thenReturn(new Cobranca("pref-1", "https://mp/pagar"));
        when(repository.atualizar(orcamento)).thenReturn(orcamento);

        Optional<Orcamento> resultado = useCase.paraOrdemDeServico(orcamento.getOrdemServicoId());

        assertTrue(resultado.isPresent());
        assertEquals("pref-1", orcamento.getPagamentoExternoId());
        assertEquals("https://mp/pagar", orcamento.getLinkPagamento());
        verify(repository).atualizar(orcamento);
    }

    @Test
    @DisplayName("Sem provedor configurado, não cobra e nem consulta o orçamento")
    void naoDeveCobrarSemProvedor() {
        when(pagamentoGateway.habilitado()).thenReturn(false);

        assertTrue(useCase.paraOrdemDeServico(UUID.randomUUID()).isEmpty());

        verifyNoInteractions(repository);
        verify(pagamentoGateway, never()).criarCobranca(any());
    }

    @Test
    @DisplayName("Não cobra orçamento que já foi pago ou cancelado")
    void naoDeveCobrarOrcamentoEncerrado() {
        Orcamento orcamento = orcamento(StatusOrcamento.CANCELADA);
        when(pagamentoGateway.habilitado()).thenReturn(true);
        when(repository.buscarPorOrdemServicoId(orcamento.getOrdemServicoId())).thenReturn(Optional.of(orcamento));

        assertTrue(useCase.paraOrdemDeServico(orcamento.getOrdemServicoId()).isEmpty());

        verify(pagamentoGateway, never()).criarCobranca(any());
    }

    @Test
    @DisplayName("Falha quando a OS não tem orçamento")
    void deveFalharSemOrcamentoDaOrdem() {
        UUID osId = UUID.randomUUID();
        when(pagamentoGateway.habilitado()).thenReturn(true);
        when(repository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.paraOrdemDeServico(osId));
    }

    @Test
    @DisplayName("O atendente gera o link pelo ID do orçamento")
    void deveGerarCobrancaParaOrcamento() {
        Orcamento orcamento = orcamento(StatusOrcamento.PENDENTE);
        when(pagamentoGateway.habilitado()).thenReturn(true);
        when(repository.buscarPorId(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(pagamentoGateway.criarCobranca(orcamento)).thenReturn(new Cobranca("pref-2", "https://mp/pagar2"));
        when(repository.atualizar(orcamento)).thenReturn(orcamento);

        Orcamento resultado = useCase.paraOrcamento(orcamento.getId());

        assertEquals("https://mp/pagar2", resultado.getLinkPagamento());
    }

    @Test
    @DisplayName("O atendente recebe erro claro quando o provedor não está configurado")
    void deveRecusarPedidoManualSemProvedor() {
        when(pagamentoGateway.habilitado()).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> useCase.paraOrcamento(UUID.randomUUID()));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("O atendente não consegue cobrar orçamento inexistente ou já pago")
    void deveRecusarPedidoManualInvalido() {
        when(pagamentoGateway.habilitado()).thenReturn(true);
        UUID inexistente = UUID.randomUUID();
        when(repository.buscarPorId(inexistente)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> useCase.paraOrcamento(inexistente));

        Orcamento pago = orcamento(StatusOrcamento.PAGO);
        when(repository.buscarPorId(pago.getId())).thenReturn(Optional.of(pago));
        assertThrows(IllegalArgumentException.class, () -> useCase.paraOrcamento(pago.getId()));

        verify(pagamentoGateway, never()).criarCobranca(any());
    }
}
