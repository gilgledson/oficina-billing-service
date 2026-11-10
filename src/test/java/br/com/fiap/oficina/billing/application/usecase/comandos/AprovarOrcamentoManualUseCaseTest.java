package br.com.fiap.oficina.billing.application.usecase.comandos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.oficina.billing.application.event.OrcamentoAceitoEvent;
import br.com.fiap.oficina.billing.application.event.OrcamentoRejeitadoEvent;
import br.com.fiap.oficina.billing.application.gateway.EventPublisher;
import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class AprovarOrcamentoManualUseCaseTest {

    private static final String ASSINATURA_URL = "http://storage.com/assinatura.png";

    @Mock
    private OrcamentoRepository repository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private AprovarOrcamentoManualUseCaseImpl useCase;

    private Orcamento orcamentoPendente(UUID orcamentoId, UUID osId) {
        Orcamento orcamento = new Orcamento(orcamentoId, null, osId, null,
                new BigDecimal("500.00"), StatusOrcamento.PENDENTE, null);
        when(repository.buscarPorId(orcamentoId)).thenReturn(Optional.of(orcamento));
        return orcamento;
    }

    @Test
    @DisplayName("Deve registrar aceite manual, atualizar repositório e publicar evento de aceite")
    void deveRegistrarAceiteManual() {
        UUID orcamentoId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        List<UUID> aceitos = List.of(UUID.randomUUID());
        List<UUID> rejeitados = List.of(UUID.randomUUID());
        Orcamento orcamento = orcamentoPendente(orcamentoId, osId);

        useCase.executar(orcamentoId, ASSINATURA_URL, aceitos, rejeitados);

        assertEquals(ASSINATURA_URL, orcamento.getAssinaturaUrl());
        assertEquals(StatusOrcamento.PENDENTE, orcamento.getStatus());
        verify(repository).atualizar(orcamento);
        verify(eventPublisher).publicar(new OrcamentoAceitoEvent(orcamentoId, osId, aceitos, rejeitados));
    }

    @Test
    @DisplayName("Sem serviços aceitos: deve cancelar o orçamento e publicar a rejeição (compensação)")
    void deveRejeitarQuandoNenhumServicoAceito() {
        UUID orcamentoId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        Orcamento orcamento = orcamentoPendente(orcamentoId, osId);

        useCase.executar(orcamentoId, ASSINATURA_URL, List.of(), List.of(UUID.randomUUID()));

        assertEquals(StatusOrcamento.CANCELADA, orcamento.getStatus());
        verify(repository).atualizar(orcamento);
        verify(eventPublisher).publicar(new OrcamentoRejeitadoEvent(orcamentoId, osId));
    }

    @Test
    @DisplayName("Listas nulas devem ser tratadas como vazias (rejeição)")
    void deveTratarListasNulasComoVazias() {
        UUID orcamentoId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        orcamentoPendente(orcamentoId, osId);

        useCase.executar(orcamentoId, ASSINATURA_URL, null, null);

        verify(eventPublisher).publicar(new OrcamentoRejeitadoEvent(orcamentoId, osId));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando orçamento não for encontrado")
    void deveLancarExcecaoQuandoOrcamentoNaoEncontrado() {
        UUID orcamentoId = UUID.randomUUID();
        when(repository.buscarPorId(orcamentoId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> useCase.executar(orcamentoId, "url", List.of(), List.of()));

        verify(repository, never()).atualizar(any());
        verifyNoInteractions(eventPublisher);
    }
}
