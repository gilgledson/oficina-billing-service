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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.oficina.billing.application.event.EventoIntegracao;
import br.com.fiap.oficina.billing.application.event.OrcamentoGeradoEvent;
import br.com.fiap.oficina.billing.application.gateway.EventPublisher;
import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;

@ExtendWith(MockitoExtension.class)
class CriarOrcamentoUseCaseTest {

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CriarOrcamentoUseCaseImpl useCase;

    @Test
    @DisplayName("Deve salvar o orçamento e publicar o evento de orçamento gerado")
    void deveSalvarOrcamentoEPublicarEvento() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), new BigDecimal("500.00"), MetodoPagamento.PIX);
        when(orcamentoRepository.buscarPorOrdemServicoId(orcamento.getOrdemServicoId())).thenReturn(Optional.empty());

        useCase.execute(orcamento);

        verify(orcamentoRepository).salvar(orcamento);
        ArgumentCaptor<EventoIntegracao> captor = ArgumentCaptor.forClass(EventoIntegracao.class);
        verify(eventPublisher).publicar(captor.capture());
        OrcamentoGeradoEvent evento = (OrcamentoGeradoEvent) captor.getValue();
        assertEquals(orcamento.getId(), evento.orcamentoId());
        assertEquals(orcamento.getOrdemServicoId(), evento.ordemServicoId());
        assertEquals(new BigDecimal("500.00"), evento.valorTotal());
    }

    @Test
    @DisplayName("Evento repetido: não deve duplicar o orçamento, só atualizar o valor do pendente")
    void deveSerIdempotenteQuandoOrcamentoJaExiste() {
        UUID osId = UUID.randomUUID();
        Orcamento existente = new Orcamento(osId, new BigDecimal("300.00"), null);
        when(orcamentoRepository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.of(existente));

        useCase.execute(new Orcamento(osId, new BigDecimal("450.00"), null));

        assertEquals(new BigDecimal("450.00"), existente.getValorTotal());
        verify(orcamentoRepository).atualizar(existente);
        verify(orcamentoRepository, never()).salvar(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Evento repetido de orçamento já pago não deve alterar nada")
    void naoDeveAlterarOrcamentoJaPago() {
        UUID osId = UUID.randomUUID();
        Orcamento pago = new Orcamento(UUID.randomUUID(), null, osId, null, new BigDecimal("300.00"),
                StatusOrcamento.PAGO, MetodoPagamento.PIX);
        when(orcamentoRepository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.of(pago));

        useCase.execute(new Orcamento(osId, new BigDecimal("450.00"), null));

        assertEquals(new BigDecimal("300.00"), pago.getValorTotal());
        verify(orcamentoRepository, never()).atualizar(any());
        verify(orcamentoRepository, never()).salvar(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Deve rejeitar orçamento inválido antes de gravar")
    void deveValidarOrcamento() {
        Orcamento semValor = new Orcamento(UUID.randomUUID(), null, null);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(semValor));

        verifyNoInteractions(orcamentoRepository, eventPublisher);
    }
}
