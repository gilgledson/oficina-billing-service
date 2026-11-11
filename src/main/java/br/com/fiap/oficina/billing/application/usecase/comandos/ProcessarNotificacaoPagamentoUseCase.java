package br.com.fiap.oficina.billing.application.usecase.comandos;

import java.util.Optional;

import org.jboss.logging.Logger;

import br.com.fiap.oficina.billing.application.event.PagamentoRecusadoEvent;
import br.com.fiap.oficina.billing.application.gateway.EventPublisher;
import br.com.fiap.oficina.billing.application.gateway.PagamentoGateway;
import br.com.fiap.oficina.billing.application.gateway.PagamentoGateway.Pagamento;
import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

/**
 * Trata a notificação de pagamento do provedor. A notificação só traz o
 * identificador do pagamento; o estado é sempre consultado no provedor, então
 * uma notificação forjada não confirma nada.
 *
 * <p>É idempotente: o provedor reenvia a mesma notificação várias vezes.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class ProcessarNotificacaoPagamentoUseCase {

    private static final Logger LOG = Logger.getLogger(ProcessarNotificacaoPagamentoUseCase.class);

    private final PagamentoGateway pagamentoGateway;
    private final OrcamentoRepository repository;
    private final ConfirmarPagamentoOrcamentoUseCase confirmarPagamento;
    private final EventPublisher eventPublisher;

    public void executar(String pagamentoId) {
        Optional<Pagamento> consulta = pagamentoGateway.consultarPagamento(pagamentoId);
        if (consulta.isEmpty()) {
            LOG.warnf("Pagamento %s não encontrado no provedor; notificação ignorada", pagamentoId);
            return;
        }
        Pagamento pagamento = consulta.get();
        if (pagamento.orcamentoId() == null) {
            LOG.warnf("Pagamento %s não referencia um orçamento; notificação ignorada", pagamentoId);
            return;
        }
        Orcamento orcamento = repository.buscarPorId(pagamento.orcamentoId()).orElse(null);
        if (orcamento == null) {
            LOG.warnf("Pagamento %s referencia o orçamento %s, que não existe", pagamentoId, pagamento.orcamentoId());
            return;
        }

        switch (pagamento.status()) {
            case APROVADO -> aprovar(orcamento, pagamento);
            case RECUSADO -> recusar(orcamento, pagamento);
            case PENDENTE -> LOG.infof("Pagamento %s do orçamento %s ainda em processamento", pagamentoId,
                    orcamento.getId());
        }
    }

    private void aprovar(Orcamento orcamento, Pagamento pagamento) {
        if (orcamento.getStatus() == StatusOrcamento.PAGO) {
            return;
        }
        if (orcamento.getStatus() == StatusOrcamento.CANCELADA) {
            LOG.errorf("Pagamento %s aprovado para o orçamento %s, que está cancelado: é preciso estornar",
                    pagamento.id(), orcamento.getId());
            return;
        }
        if (pagamento.valor() == null || pagamento.valor().compareTo(orcamento.getValorTotal()) < 0) {
            LOG.errorf("Pagamento %s de %s não cobre o orçamento %s de %s; não confirmado", pagamento.id(),
                    pagamento.valor(), orcamento.getId(), orcamento.getValorTotal());
            return;
        }
        confirmarPagamento.execute(orcamento.getId(), pagamento.metodoPagamento());
    }

    private void recusar(Orcamento orcamento, Pagamento pagamento) {
        if (orcamento.getStatus() != StatusOrcamento.PENDENTE) {
            return;
        }
        LOG.warnf("Pagamento %s do orçamento %s recusado: %s", pagamento.id(), orcamento.getId(),
                pagamento.detalhe());
        eventPublisher.publicar(new PagamentoRecusadoEvent(orcamento.getId(), orcamento.getOrdemServicoId(),
                pagamento.id(), pagamento.detalhe()));
    }
}
