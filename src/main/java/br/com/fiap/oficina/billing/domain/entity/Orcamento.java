package br.com.fiap.oficina.billing.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Orcamento {
    private UUID id;
    private LocalDate dataEmissao;
    private UUID ordemServicoId;
    private LocalDate dataVencimento;
    private MetodoPagamento metodoPagamento;
    private BigDecimal valorTotal;
    private StatusOrcamento status;
    private String assinaturaUrl;

    public static Orcamento reconstruir(UUID id, LocalDate dataEmissao, UUID ordemServicoId, LocalDate dataVencimento,
            BigDecimal valorTotal, StatusOrcamento status, MetodoPagamento metodoPagamento, String assinaturaUrl) {
        Orcamento orcamento = new Orcamento(id, dataEmissao, ordemServicoId, dataVencimento, valorTotal, status,
                metodoPagamento);
        orcamento.setAssinaturaUrl(assinaturaUrl);
        return orcamento;
    }

    public Orcamento(UUID id, LocalDate dataEmissao, UUID ordemServicoId, LocalDate dataVencimento,
            BigDecimal valorTotal, StatusOrcamento status, MetodoPagamento metodoPagamento) {
        this.id = id;
        this.dataEmissao = dataEmissao;
        this.ordemServicoId = ordemServicoId;
        this.dataVencimento = dataVencimento;
        this.valorTotal = valorTotal;
        this.status = status;
        this.metodoPagamento = metodoPagamento;
    }

    public Orcamento(UUID ordemServicoId, BigDecimal valorTotal, MetodoPagamento metodoPagamento) {
        this.id = UUID.randomUUID();
        this.dataEmissao = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        this.ordemServicoId = ordemServicoId;
        this.dataVencimento = this.dataEmissao.plusDays(30);
        this.valorTotal = valorTotal;
        this.status = StatusOrcamento.PENDENTE;
        this.metodoPagamento = metodoPagamento;
    }

    public void validar() {
        if (ordemServicoId == null) {
            throw new IllegalArgumentException("Ordem de serviço é obrigatória.");
        }
        if (dataEmissao == null) {
            throw new IllegalArgumentException("Data de emissão é obrigatória.");
        }
        if (dataVencimento == null) {
            throw new IllegalArgumentException("Data de vencimento é obrigatória.");
        }
        if (valorTotal == null) {
            throw new IllegalArgumentException("Valor total é obrigatório.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status é obrigatório.");
        }
    }

    public void atualizar(UUID ordemServicoId, LocalDate dataEmissao, LocalDate dataVencimento, BigDecimal valorTotal,
            StatusOrcamento status, MetodoPagamento metodoPagamento) {
        this.ordemServicoId = ordemServicoId;
        this.dataEmissao = dataEmissao;
        this.dataVencimento = dataVencimento;
        this.valorTotal = valorTotal;
        this.status = status;
        this.metodoPagamento = metodoPagamento;
    }

    public void cancelar() {
        this.status = StatusOrcamento.CANCELADA;
    }

    public void pagar(MetodoPagamento metodoPagamento) {
        if (metodoPagamento == null) {
            throw new IllegalArgumentException("Método de pagamento é obrigatório para confirmar o pagamento.");
        }
        this.status = StatusOrcamento.PAGO;
        this.metodoPagamento = metodoPagamento;
    }

    public void pendente() {
        this.status = StatusOrcamento.PENDENTE;
    }

    public void registrarAceiteManual(String assinaturaUrl) {
        if (assinaturaUrl == null || assinaturaUrl.isBlank()) {
            throw new IllegalArgumentException("A URL da assinatura é obrigatória para o aceite manual.");
        }
        this.assinaturaUrl = assinaturaUrl;
        // Ao aceitar o orçamento, o Or�amento continua PENDENTE até o pagamento,
        // mas o status da OS mudará via evento.
    }
}

