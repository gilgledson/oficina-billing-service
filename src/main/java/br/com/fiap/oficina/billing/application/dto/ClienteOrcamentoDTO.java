package br.com.fiap.oficina.billing.application.dto;


import java.util.UUID;

public class ClienteOrcamentoDTO {
    public ClienteOrcamentoDTO(UUID id, String nome, String email, String cpfCnpj, String telefone,
            String enderecoCompleto) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.cpfCnpj = cpfCnpj;
        this.telefone = telefone;
        this.enderecoCompleto = enderecoCompleto;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getCpfCnpj() { return cpfCnpj; }
    public String getTelefone() { return telefone; }
    public String getEnderecoCompleto() { return enderecoCompleto; }

    private UUID id;
    private String nome;
    private String email;
    private String cpfCnpj;
    private String telefone;
    private String enderecoCompleto;
}






