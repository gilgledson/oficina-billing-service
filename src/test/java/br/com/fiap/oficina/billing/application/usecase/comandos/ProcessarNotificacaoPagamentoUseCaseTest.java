package br.com.fiap.oficina.billing.application.usecase.comandos;

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

import br.com.fiap.oficina.billing.application.event.PagamentoRecusadoEvent;
import br.com.fiap.oficina.billing.application.gateway.EventPublisher;
import br.com.fiap.oficina.billing.application.gateway.PagamentoGateway;
import br.com.fiap.oficina.billing.application.gateway.PagamentoGateway.Pagamento;
import br.com.fiap.oficina.billing.application.gateway.PagamentoGateway.StatusPagamento;
import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;

@ExtendWith(MockitoExtension.class)
class ProcessarNotificacaoPagamentoUseCaseTest {

    private static final String PAGAMENTO_ID = "123456";

    @Mock
    private PagamentoGateway pagamentoGateway;

    @Mock
    private OrcamentoRepository repository;

    @Mock
    private ConfirmarPagamentoOrcamentoUseCase confirmarPagamento;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ProcessarNotificacaoPagamentoUseCase useCase;

    private Orcamento orcamento(StatusOrcamento status) {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), null, UUID.randomUUID(), null,
                new BigDecimal("185.00"), status, null);
        when(repository.buscarPorId(orcamento.getId())).thenReturn(Optional.of(orcamento));
        return orcamento;
    }

    private void provedorResponde(UUID orcamentoId, StatusPagamento status, String valor) {
        when(pagamentoGateway.consultarPagamento(PAGAMENTO_ID)).thenReturn(Optional.of(new Pagamento(
                PAGAMENTO_ID, status, orcamentoId, MetodoPagamento.PIX,
                valor == null ? null : new BigDecimal(valor), "detalhe")));
    }

    @Test
    @DisplayName("Pagamento aprovado confirma o orçamento com o meio de pagamento usado")
    void deveConfirmarPagamentoAprovado() {
        Orcamento orcamento = orcamento(StatusOrcamento.PENDENTE);
        provedorResponde(orcamento.getId(), StatusPagamento.APROVADO, "185.00");

        useCase.executar(PAGAMENTO_ID);

        verify(confirmarPagamento).execute(orcamento.getId(), MetodoPagamento.PIX, PAGAMENTO_ID);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Notificação repetida de orçamento já pago não confirma de novo")
    void deveIgnorarNotificacaoRepetida() {
        Orcamento orcamento = orcamento(StatusOrcamento.PAGO);
        orcamento.setPagamentoId(PAGAMENTO_ID);
        provedorResponde(orcamento.getId(), StatusPagamento.APROVADO, "185.00");

        useCase.executar(PAGAMENTO_ID);

        verifyNoInteractions(confirmarPagamento, eventPublisher);
    }

    @Test
    @DisplayName("Segundo pagamento de orçamento já pago não confirma de novo")
    void naoDeveConfirmarSegundoPagamento() {
        Orcamento orcamento = orcamento(StatusOrcamento.PAGO);
        orcamento.setPagamentoId("outro-pagamento");
        provedorResponde(orcamento.getId(), StatusPagamento.APROVADO, "185.00");

        useCase.executar(PAGAMENTO_ID);

        verifyNoInteractions(confirmarPagamento, eventPublisher);
    }

    @Test
    @DisplayName("Pagamento aprovado de orçamento cancelado não confirma")
    void naoDeveConfirmarOrcamentoCancelado() {
        Orcamento orcamento = orcamento(StatusOrcamento.CANCELADA);
        provedorResponde(orcamento.getId(), StatusPagamento.APROVADO, "185.00");

        useCase.executar(PAGAMENTO_ID);

        verifyNoInteractions(confirmarPagamento, eventPublisher);
    }

    @Test
    @DisplayName("Pagamento de valor menor que o orçamento não confirma")
    void naoDeveConfirmarValorMenor() {
        Orcamento orcamento = orcamento(StatusOrcamento.PENDENTE);
        provedorResponde(orcamento.getId(), StatusPagamento.APROVADO, "100.00");
        useCase.executar(PAGAMENTO_ID);

        provedorResponde(orcamento.getId(), StatusPagamento.APROVADO, null);
        useCase.executar(PAGAMENTO_ID);

        verifyNoInteractions(confirmarPagamento, eventPublisher);
    }

    @Test
    @DisplayName("Pagamento recusado publica evento e mantém o orçamento pendente")
    void devePublicarPagamentoRecusado() {
        Orcamento orcamento = orcamento(StatusOrcamento.PENDENTE);
        provedorResponde(orcamento.getId(), StatusPagamento.RECUSADO, "185.00");

        useCase.executar(PAGAMENTO_ID);

        verify(eventPublisher).publicar(new PagamentoRecusadoEvent(orcamento.getId(), orcamento.getOrdemServicoId(),
                PAGAMENTO_ID, "detalhe"));
        verifyNoInteractions(confirmarPagamento);
    }

    @Test
    @DisplayName("Recusa de orçamento que não está pendente é ignorada")
    void deveIgnorarRecusaDeOrcamentoEncerrado() {
        Orcamento orcamento = orcamento(StatusOrcamento.PAGO);
        provedorResponde(orcamento.getId(), StatusPagamento.RECUSADO, "185.00");

        useCase.executar(PAGAMENTO_ID);

        verifyNoInteractions(confirmarPagamento, eventPublisher);
    }

    @Test
    @DisplayName("Pagamento ainda em processamento não muda nada")
    void deveAguardarPagamentoPendente() {
        Orcamento orcamento = orcamento(StatusOrcamento.PENDENTE);
        provedorResponde(orcamento.getId(), StatusPagamento.PENDENTE, "185.00");

        useCase.executar(PAGAMENTO_ID);

        verifyNoInteractions(confirmarPagamento, eventPublisher);
    }

    @Test
    @DisplayName("Notificação de pagamento desconhecido, sem referência ou de orçamento inexistente é ignorada")
    void deveIgnorarNotificacaoSemOrcamento() {
        when(pagamentoGateway.consultarPagamento(PAGAMENTO_ID)).thenReturn(Optional.empty());
        useCase.executar(PAGAMENTO_ID);

        provedorResponde(null, StatusPagamento.APROVADO, "185.00");
        useCase.executar(PAGAMENTO_ID);

        UUID inexistente = UUID.randomUUID();
        when(repository.buscarPorId(inexistente)).thenReturn(Optional.empty());
        provedorResponde(inexistente, StatusPagamento.APROVADO, "185.00");
        useCase.executar(PAGAMENTO_ID);

        verify(confirmarPagamento, never()).execute(any(), any(), any());
        verifyNoInteractions(eventPublisher);
    }
}
