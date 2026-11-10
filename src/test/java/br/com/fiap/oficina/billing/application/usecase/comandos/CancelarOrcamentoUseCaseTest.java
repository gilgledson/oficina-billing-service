package br.com.fiap.oficina.billing.application.usecase.comandos;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import br.com.fiap.oficina.billing.application.event.OrcamentoCanceladoEvent;
import br.com.fiap.oficina.billing.application.gateway.EventPublisher;
import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;

@ExtendWith(MockitoExtension.class)
class CancelarOrcamentoUseCaseTest {

    @Mock
    private OrcamentoRepository repository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CancelarOrcamentoUseCase useCase;

    private Orcamento orcamento(UUID osId, StatusOrcamento status) {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), null, osId, null, new BigDecimal("200.00"), status,
                MetodoPagamento.PIX);
        when(repository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.of(orcamento));
        return orcamento;
    }

    @Test
    @DisplayName("Deve cancelar orçamento pendente sem pedir estorno")
    void deveCancelarOrcamentoPendente() {
        UUID osId = UUID.randomUUID();
        Orcamento orcamento = orcamento(osId, StatusOrcamento.PENDENTE);

        useCase.executar(osId);

        assertEquals(StatusOrcamento.CANCELADA, orcamento.getStatus());
        verify(repository).atualizar(orcamento);
        verify(eventPublisher).publicar(new OrcamentoCanceladoEvent(orcamento.getId(), osId, false));
    }

    @Test
    @DisplayName("Deve cancelar orçamento pago sinalizando que o estorno é necessário")
    void deveCancelarOrcamentoPagoComEstorno() {
        UUID osId = UUID.randomUUID();
        Orcamento orcamento = orcamento(osId, StatusOrcamento.PAGO);

        useCase.executar(osId);

        assertEquals(StatusOrcamento.CANCELADA, orcamento.getStatus());
        verify(eventPublisher).publicar(new OrcamentoCanceladoEvent(orcamento.getId(), osId, true));
    }

    @Test
    @DisplayName("Evento repetido: orçamento já cancelado não deve gerar novo evento")
    void deveIgnorarOrcamentoJaCancelado() {
        UUID osId = UUID.randomUUID();
        orcamento(osId, StatusOrcamento.CANCELADA);

        useCase.executar(osId);

        verify(repository, never()).atualizar(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("OS sem orçamento não tem o que compensar")
    void deveIgnorarOsSemOrcamento() {
        UUID osId = UUID.randomUUID();
        when(repository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.empty());

        useCase.executar(osId);

        verify(repository, never()).atualizar(any());
        verifyNoInteractions(eventPublisher);
    }
}
