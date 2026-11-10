package br.com.fiap.oficina.billing.infrastructure.gateway;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * API REST do OS Service usada pelo Billing para montar o PDF do orçamento.
 * É a única comunicação síncrona entre os dois; o restante é por eventos.
 */
@Path("/api/integracao")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "os-service")
@RegisterClientHeaders(PropagacaoHeadersFactory.class)
public interface OsServiceClient {

    @GET
    @Path("/ordens/{id}/orcamento")
    OrdemServicoResponse buscarOrdemDeServico(@PathParam("id") UUID id);

    @GET
    @Path("/clientes/{id}")
    ClienteResponse buscarCliente(@PathParam("id") UUID id);

    @GET
    @Path("/veiculos/{id}")
    VeiculoResponse buscarVeiculo(@PathParam("id") UUID id);

    record OrdemServicoResponse(UUID id, UUID clienteId, UUID veiculoId, List<ItemServicoResponse> servicos,
            BigDecimal valorTotal) {
    }

    record ItemServicoResponse(String nome, int quantidade, BigDecimal precoUnitario, BigDecimal total,
            List<ItemProdutoResponse> produtos) {
    }

    record ItemProdutoResponse(String nome, BigDecimal quantidade, BigDecimal precoUnitario, BigDecimal total) {
    }

    record ClienteResponse(UUID id, String nome, String email, String cpfCnpj, String telefone,
            String enderecoCompleto) {
    }

    record VeiculoResponse(UUID id, String placa, String marca, String modelo, int ano) {
    }
}
