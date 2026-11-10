package br.com.fiap.oficina.billing.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "orcamento")
@Getter
@Setter
public class OrcamentoJpaEntity {
    public OrcamentoJpaEntity() {
    }

    public OrcamentoJpaEntity(UUID id, UUID ordemServicoId, LocalDate dataEmissao, LocalDate dataVencimento,
            BigDecimal valorTotal, StatusOrcamento status, MetodoPagamento metodoPagamento) {
        this.id = id;
        this.ordemServicoId = ordemServicoId;
        this.dataEmissao = dataEmissao;
        this.dataVencimento = dataVencimento;
        this.valorTotal = valorTotal;
        this.status = status;
        this.metodoPagamento = metodoPagamento;
    }

    @Id
    private UUID id;

    @Column(name = "ordem_servico_id", nullable = false)
    private UUID ordemServicoId;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "valor_total", nullable = false)
    private BigDecimal valorTotal;

    @Column(name = "metodo_pagamento", nullable = true)
    @Enumerated(EnumType.STRING)
    private MetodoPagamento metodoPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusOrcamento status;

    @Column(name = "assinatura_cliente")
    private String assinaturaCliente;

    public Orcamento toEntity() {
        return Orcamento.reconstruir(
                id,
                dataEmissao,
                ordemServicoId,
                dataVencimento,
                valorTotal,
                status,
                metodoPagamento,
                assinaturaCliente);
    }

    public static OrcamentoJpaEntity fromEntity(Orcamento orcamento) {
        OrcamentoJpaEntity entity = new OrcamentoJpaEntity(
                orcamento.getId(),
                orcamento.getOrdemServicoId(),
                orcamento.getDataEmissao(),
                orcamento.getDataVencimento(),
                orcamento.getValorTotal(),
                orcamento.getStatus(),
                orcamento.getMetodoPagamento());
        entity.setAssinaturaCliente(orcamento.getAssinaturaUrl());
        return entity;
    }

}







