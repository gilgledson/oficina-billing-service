package br.com.fiap.oficina.billing.application.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.oficina.billing.application.event.OrcamentoFalhouEvent;
import br.com.fiap.oficina.billing.application.gateway.EventPublisher;
import br.com.fiap.oficina.billing.application.usecase.comandos.AtualizarOrcamentoUseCase;
import br.com.fiap.oficina.billing.application.usecase.comandos.CancelarOrcamentoUseCase;
import br.com.fiap.oficina.billing.application.usecase.comandos.CriarOrcamentoUseCase;
import br.com.fiap.oficina.billing.application.usecase.comandos.GerarCobrancaUseCase;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import io.vertx.core.json.JsonObject;

@ExtendWith(MockitoExtension.class)
class GerarOrcamentoOnOrdemFinalizadaListenerTest {

    @Mock
    private CriarOrcamentoUseCase criarOrcamentoUseCase;

    @Mock
    private AtualizarOrcamentoUseCase atualizarOrcamentoUseCase;

    @Mock
    private CancelarOrcamentoUseCase cancelarOrcamentoUseCase;

    @Mock
    private GerarCobrancaUseCase gerarCobrancaUseCase;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private GerarOrcamentoOnOrdemFinalizadaListener listener;

    @Test
    @DisplayName("Deve gerar orçamento ao receber diagnóstico concluído sem método de pagamento")
    void deveGerarOrcamentoSemMetodoPagamento() {
        UUID osId = UUID.randomUUID();
        JsonObject json = new JsonObject()
                .put("ordemServicoId", osId.toString())
                .put("valorTotal", 500.0);

        listener.onDiagnosticoConcluido(json);

        ArgumentCaptor<Orcamento> captor = ArgumentCaptor.forClass(Orcamento.class);
        verify(criarOrcamentoUseCase).execute(captor.capture());
        Orcamento orcamento = captor.getValue();
        assertEquals(osId, orcamento.getOrdemServicoId());
        assertEquals(0, new BigDecimal("500").compareTo(orcamento.getValorTotal()));
        assertNull(orcamento.getMetodoPagamento());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Deve gerar orçamento ao receber evento com método de pagamento e valor em texto")
    void deveGerarOrcamentoComMetodoPagamento() {
        UUID osId = UUID.randomUUID();
        JsonObject json = new JsonObject()
                .put("ordemServicoId", osId.toString())
                .put("valorTotal", "300.45")
                .put("metodoPagamento", "PIX");

        listener.onDiagnosticoConcluido(json);

        ArgumentCaptor<Orcamento> captor = ArgumentCaptor.forClass(Orcamento.class);
        verify(criarOrcamentoUseCase).execute(captor.capture());
        assertEquals(MetodoPagamento.PIX, captor.getValue().getMetodoPagamento());
        assertEquals(new BigDecimal("300.45"), captor.getValue().getValorTotal());
    }

    @Test
    @DisplayName("Falha ao gerar orçamento deve publicar evento de compensação")
    void devePublicarCompensacaoQuandoGeracaoFalha() {
        UUID osId = UUID.randomUUID();
        JsonObject json = new JsonObject()
                .put("ordemServicoId", osId.toString())
                .put("valorTotal", 500.0);
        doThrow(new IllegalStateException("banco fora do ar")).when(criarOrcamentoUseCase).execute(any());

        listener.onDiagnosticoConcluido(json);

        verify(eventPublisher).publicar(new OrcamentoFalhouEvent(osId, "banco fora do ar"));
    }

    @Test
    @DisplayName("Evento sem valor total deve publicar evento de compensação")
    void devePublicarCompensacaoQuandoEventoSemValor() {
        UUID osId = UUID.randomUUID();
        JsonObject json = new JsonObject().put("ordemServicoId", osId.toString());

        listener.onDiagnosticoConcluido(json);

        verifyNoInteractions(criarOrcamentoUseCase);
        verify(eventPublisher).publicar(new OrcamentoFalhouEvent(osId, "valorTotal é obrigatório no evento."));
    }

    @Test
    @DisplayName("Deve atualizar o valor do orçamento quando a execução for concluída")
    void deveAtualizarOrcamentoNaExecucaoConcluida() {
        UUID osId = UUID.randomUUID();
        JsonObject json = new JsonObject()
                .put("ordemServicoId", osId.toString())
                .put("valorTotal", "750.90");

        listener.onExecucaoConcluida(json);

        verify(atualizarOrcamentoUseCase).executar(osId, new BigDecimal("750.90"));
    }

    @Test
    @DisplayName("Execução concluída sem valor total deve falhar")
    void deveFalharNaExecucaoConcluidaSemValor() {
        JsonObject json = new JsonObject().put("ordemServicoId", UUID.randomUUID().toString());

        assertThrows(IllegalArgumentException.class, () -> listener.onExecucaoConcluida(json));

        verifyNoInteractions(atualizarOrcamentoUseCase);
    }

    @Test
    @DisplayName("Deve cancelar o orçamento quando a OS for cancelada")
    void deveCancelarOrcamentoQuandoOsCancelada() {
        UUID osId = UUID.randomUUID();

        listener.onOrdemCancelada(new JsonObject().put("ordemServicoId", osId.toString()));

        verify(cancelarOrcamentoUseCase).executar(osId);
    }

    @Test
    @DisplayName("Execução concluída atualiza o valor e cria a cobrança no provedor")
    void deveGerarCobrancaAoConcluirExecucao() {
        UUID osId = UUID.randomUUID();

        listener.onExecucaoConcluida(new JsonObject().put("ordemServicoId", osId.toString()).put("valorTotal", 755.0));

        verify(atualizarOrcamentoUseCase).executar(osId, new BigDecimal("755.0"));
        verify(gerarCobrancaUseCase).paraOrdemDeServico(osId);
    }

    @Test
    @DisplayName("Falha ao criar a cobrança não rejeita o evento de execução concluída")
    void naoDeveRejeitarEventoQuandoCobrancaFalha() {
        UUID osId = UUID.randomUUID();
        doThrow(new IllegalStateException("Mercado Pago fora do ar")).when(gerarCobrancaUseCase)
                .paraOrdemDeServico(osId);

        listener.onExecucaoConcluida(new JsonObject().put("ordemServicoId", osId.toString()).put("valorTotal", 10));

        verify(atualizarOrcamentoUseCase).executar(osId, new BigDecimal("10"));
    }
}
