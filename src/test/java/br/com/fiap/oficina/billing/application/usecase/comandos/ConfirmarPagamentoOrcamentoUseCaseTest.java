package br.com.fiap.oficina.billing.application.usecase.comandos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import br.com.fiap.oficina.billing.application.event.OrcamentoPagoEvent;
import br.com.fiap.oficina.billing.application.gateway.EventPublisher;
import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class ConfirmarPagamentoOrcamentoUseCaseTest {

    @Mock
    private OrcamentoRepository repository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ConfirmarPagamentoOrcamentoUseCaseImpl useCase;

    @Test
    @DisplayName("Deve confirmar pagamento, atualizar repositório e publicar evento")
    void deveConfirmarPagamento() {
        UUID orcamentoId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        Orcamento orcamento = new Orcamento(orcamentoId, null, osId, null,
                new BigDecimal("400.00"), StatusOrcamento.PENDENTE, null);
        when(repository.buscarPorId(orcamentoId)).thenReturn(Optional.of(orcamento));

        useCase.execute(orcamentoId, MetodoPagamento.PIX);

        assertEquals(StatusOrcamento.PAGO, orcamento.getStatus());
        assertEquals(MetodoPagamento.PIX, orcamento.getMetodoPagamento());
        verify(repository).atualizar(orcamento);
        verify(eventPublisher).publicar(new OrcamentoPagoEvent(orcamentoId, osId));
    }

    @Test
    @DisplayName("Pagamento vindo do provedor guarda o ID do pagamento no orçamento")
    void deveGuardarIdDoPagamentoDoProvedor() {
        UUID orcamentoId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        Orcamento orcamento = new Orcamento(orcamentoId, null, osId, null,
                new BigDecimal("400.00"), StatusOrcamento.PENDENTE, null);
        when(repository.buscarPorId(orcamentoId)).thenReturn(Optional.of(orcamento));

        useCase.execute(orcamentoId, MetodoPagamento.CARTAO_CREDITO, "pag-99");

        assertEquals(StatusOrcamento.PAGO, orcamento.getStatus());
        assertEquals("pag-99", orcamento.getPagamentoId());
        verify(eventPublisher).publicar(new OrcamentoPagoEvent(orcamentoId, osId));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando Orcamento não for encontrada")
    void deveLancarExcecaoQuandoFaturaNaoEncontrada() {
        UUID orcamentoId = UUID.randomUUID();
        when(repository.buscarPorId(orcamentoId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> useCase.execute(orcamentoId, MetodoPagamento.DINHEIRO));

        verify(repository, never()).atualizar(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Deve lançar exceção quando método de pagamento for nulo")
    void deveLancarExcecaoQuandoMetodoPagamentoNulo() {
        UUID orcamentoId = UUID.randomUUID();
        Orcamento orcamento = new Orcamento(orcamentoId, null, UUID.randomUUID(), null,
                new BigDecimal("400.00"), StatusOrcamento.PENDENTE, null);
        when(repository.buscarPorId(orcamentoId)).thenReturn(Optional.of(orcamento));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(orcamentoId, null));

        verify(repository, never()).atualizar(any());
        verifyNoInteractions(eventPublisher);
    }
}
