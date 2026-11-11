package br.com.fiap.oficina.billing.application.usecase.comandos;

import java.util.Optional;
import java.util.UUID;

import br.com.fiap.oficina.billing.application.gateway.PagamentoGateway;
import br.com.fiap.oficina.billing.application.gateway.PagamentoGateway.Cobranca;
import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Cria a cobrança do orçamento no provedor de pagamentos e guarda o link que o
 * cliente usa para pagar.
 *
 * <p>Não é transacional de propósito: a chamada ao provedor fica fora de
 * transação de banco, e só o registro do link é gravado. Por isso os métodos
 * ativam o contexto de requisição: quando a chamada vem do consumidor de
 * eventos não há requisição HTTP, e sem contexto a leitura do orçamento falha.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class GerarCobrancaUseCase {

    private final OrcamentoRepository repository;
    private final PagamentoGateway pagamentoGateway;

    /**
     * Chamado quando a execução da OS termina e o valor final é conhecido. Sem
     * provedor configurado não faz nada: o pagamento segue manual.
     */
    @ActivateRequestContext
    public Optional<Orcamento> paraOrdemDeServico(UUID ordemServicoId) {
        if (!pagamentoGateway.habilitado()) {
            return Optional.empty();
        }
        Orcamento orcamento = repository.buscarPorOrdemServicoId(ordemServicoId)
                .orElseThrow(() -> new NotFoundException("Orçamento não encontrado para a OS " + ordemServicoId));
        if (orcamento.getStatus() != StatusOrcamento.PENDENTE) {
            return Optional.empty();
        }
        return Optional.of(gerar(orcamento));
    }

    /** Chamado pelo atendente, para gerar o link ou gerar de novo depois de uma falha. */
    @ActivateRequestContext
    public Orcamento paraOrcamento(UUID orcamentoId) {
        if (!pagamentoGateway.habilitado()) {
            throw new IllegalArgumentException("A integração com o Mercado Pago não está configurada.");
        }
        Orcamento orcamento = repository.buscarPorId(orcamentoId)
                .orElseThrow(() -> new NotFoundException("Orçamento não encontrado"));
        if (orcamento.getStatus() != StatusOrcamento.PENDENTE) {
            throw new IllegalArgumentException(
                    "Só é possível cobrar um orçamento pendente. Status atual: " + orcamento.getStatus());
        }
        return gerar(orcamento);
    }

    private Orcamento gerar(Orcamento orcamento) {
        Cobranca cobranca = pagamentoGateway.criarCobranca(orcamento);
        orcamento.registrarCobranca(cobranca.id(), cobranca.link());
        return repository.atualizar(orcamento);
    }
}
