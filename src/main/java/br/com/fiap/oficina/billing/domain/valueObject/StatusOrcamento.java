package br.com.fiap.oficina.billing.domain.valueObject;

public enum StatusOrcamento {
    PENDENTE(1, "Pendente"),
    PAGO(2, "Pago"),
    CANCELADA(3, "Cancelada");

    private final int id;
    private final String nome;

    StatusOrcamento(int id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public static StatusOrcamento fromId(int id) {
        for (StatusOrcamento status : values()) {
            if (status.id == id) {
                return status;
            }
        }
        throw new IllegalArgumentException("ID de status de Orcamento inválido: " + id);
    }
}






