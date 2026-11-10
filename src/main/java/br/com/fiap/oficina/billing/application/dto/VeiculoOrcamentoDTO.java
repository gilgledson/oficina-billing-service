package br.com.fiap.oficina.billing.application.dto;


import java.util.UUID;

public class VeiculoOrcamentoDTO {
    public VeiculoOrcamentoDTO(UUID id, String placa, String marca, String modelo, int ano) {
        this.id = id;
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
    }

    public UUID getId() { return id; }
    public String getPlaca() { return placa; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public int getAno() { return ano; }

    private UUID id;
    private String placa;
    private String marca;
    private String modelo;
    private int ano;
}






