package br.com.fiap.oficina.billing.infrastructure.gateway;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import br.com.fiap.oficina.billing.application.dto.ClienteOrcamentoDTO;
import br.com.fiap.oficina.billing.application.dto.OrdemServicoOrcamentoDTO;
import br.com.fiap.oficina.billing.application.dto.VeiculoOrcamentoDTO;
import br.com.fiap.oficina.billing.application.gateway.AtendimentoGateway;
import br.com.fiap.oficina.billing.application.gateway.OperacionalGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Implementa os gateways de OS, cliente e veículo chamando a API REST do OS
 * Service — no monólito eram chamadas diretas aos repositórios dos outros
 * módulos.
 */
@ApplicationScoped
public class OsServiceRestGateway implements AtendimentoGateway, OperacionalGateway {

    private final OsServiceClient client;

    public OsServiceRestGateway(@RestClient OsServiceClient client) {
        this.client = client;
    }

    @Override
    public Optional<OrdemServicoOrcamentoDTO> buscarOrdemDeServicoPorId(UUID id) {
        return buscar(() -> client.buscarOrdemDeServico(id)).map(OsServiceRestGateway::paraDto);
    }

    @Override
    public Optional<ClienteOrcamentoDTO> buscarClientePorId(UUID id) {
        return buscar(() -> client.buscarCliente(id))
                .map(c -> new ClienteOrcamentoDTO(c.id(), c.nome(), c.email(), c.cpfCnpj(), c.telefone(),
                        c.enderecoCompleto()));
    }

    @Override
    public Optional<VeiculoOrcamentoDTO> buscarVeiculoPorId(UUID id) {
        return buscar(() -> client.buscarVeiculo(id))
                .map(v -> new VeiculoOrcamentoDTO(v.id(), v.placa(), v.marca(), v.modelo(), v.ano()));
    }

    private static <T> Optional<T> buscar(Supplier<T> chamada) {
        try {
            return Optional.ofNullable(chamada.get());
        } catch (WebApplicationException e) {
            if (e.getResponse() != null
                    && e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                return Optional.empty();
            }
            throw e;
        }
    }

    private static OrdemServicoOrcamentoDTO paraDto(OsServiceClient.OrdemServicoResponse os) {
        List<OrdemServicoOrcamentoDTO.ItemServicoDTO> servicos = lista(os.servicos()).stream()
                .map(s -> new OrdemServicoOrcamentoDTO.ItemServicoDTO(s.nome(), s.quantidade(), s.precoUnitario(),
                        s.total(),
                        lista(s.produtos()).stream()
                                .map(p -> new OrdemServicoOrcamentoDTO.ItemProdutoDTO(p.nome(), p.quantidade(),
                                        p.precoUnitario(), p.total()))
                                .toList()))
                .toList();
        return new OrdemServicoOrcamentoDTO(os.id(), os.clienteId(), os.veiculoId(), servicos, os.valorTotal());
    }

    private static <T> List<T> lista(List<T> itens) {
        return itens == null ? List.of() : itens;
    }
}
