package br.com.fiap.oficina.billing.api.controller;

import br.com.fiap.oficina.billing.application.usecase.consultas.BaixarOrcamentoPorOrdemServicoUseCase;
import br.com.fiap.oficina.billing.application.usecase.consultas.BaixarOrcamentoUseCase;
import br.com.fiap.oficina.billing.api.dto.AceitarOrcamentoRequest;
import br.com.fiap.oficina.billing.api.dto.ConfirmarPagamentoRequest;
import br.com.fiap.oficina.billing.application.usecase.comandos.ConfirmarPagamentoOrcamentoUseCase;
import br.com.fiap.oficina.billing.application.usecase.comandos.GerarCobrancaUseCase;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.shared.security.Perfis;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import br.com.fiap.oficina.billing.application.usecase.comandos.AprovarOrcamentoManualUseCase;
import br.com.fiap.oficina.billing.infrastructure.storage.AssinaturaStorageService;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import java.util.UUID;

@Path("/api/orcamentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "orcamento", description = "Gestão de faturas e pagamentos")
@RequiredArgsConstructor
public class OrcamentoController {

    private final ConfirmarPagamentoOrcamentoUseCase confirmarPagamentoOrcamentoUseCase;

    private final BaixarOrcamentoUseCase baixarOrcamentoUseCase;

    private final BaixarOrcamentoPorOrdemServicoUseCase BaixarOrcamentoPorOrdemServicoUseCase;

    private final AprovarOrcamentoManualUseCase aprovarOrcamentoManualUseCase;

    private final AssinaturaStorageService storageService;

    private final GerarCobrancaUseCase gerarCobrancaUseCase;

    @Context
    UriInfo uriInfo;

    @POST
    @Path("/{id}/confirmar-pagamento")
    @RolesAllowed({ Perfis.ADMIN, Perfis.ATENDENTE })
    @Operation(summary = "Confirmar pagamento de umo Oramento")
    public Response confirmarPagamento(@PathParam("id") UUID id, @Valid ConfirmarPagamentoRequest request) {
        confirmarPagamentoOrcamentoUseCase.execute(id, request.metodoPagamento());
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/pagamento")
    @RolesAllowed({ Perfis.ADMIN, Perfis.ATENDENTE })
    @Operation(summary = "Gerar (ou gerar de novo) o link de pagamento do orçamento no Mercado Pago")
    public Response gerarPagamento(@PathParam("id") UUID id) {
        Orcamento orcamento = gerarCobrancaUseCase.paraOrcamento(id);
        return Response.ok(new JsonObject()
                .put("pagamentoExternoId", orcamento.getPagamentoExternoId())
                .put("linkPagamento", orcamento.getLinkPagamento())).build();
    }

    @POST
    @Path("/{id}/aceitar")
    @RolesAllowed({ Perfis.ADMIN, Perfis.ATENDENTE })
    @Operation(summary = "Aceitar orçamento manualmente (assinatura Base64 e lista de serviços)")
    public Response aceitarOrcamento(
            @PathParam("id") UUID id,
            @Valid AceitarOrcamentoRequest request) {

        if (request.assinaturaBase64() == null || request.assinaturaBase64().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new JsonObject().put("mensagem", "Assinatura Base64 é obrigatória"))
                    .build();
        }

        String filename = storageService.storeBase64(request.assinaturaBase64(), "assinatura_" + id);

        String assinaturaUrl = uriInfo.getBaseUriBuilder()
                .path(OrcamentoController.class)
                .path("assinaturas")
                .path(filename)
                .build()
                .toString();

        aprovarOrcamentoManualUseCase.executar(id, assinaturaUrl, request.servicosAceitos(), request.servicosRejeitados());
        return Response.ok(new JsonObject().put("assinaturaUrl", assinaturaUrl)).build();
    }

    @GET
    @Path("/assinaturas/{filename}")
    @RolesAllowed({ Perfis.ADMIN, Perfis.ATENDENTE })
    @Produces("image/png")
    @Operation(summary = "Obter imagem da assinatura")
    public Response obterAssinatura(@PathParam("filename") String filename) {
        byte[] conteudo = storageService.baixar(filename);
        if (conteudo == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(conteudo).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ Perfis.ADMIN, Perfis.ATENDENTE })
    @Operation(summary = "Obter Orcamento por ID")
    @Produces("application/pdf")
    public Response buscarOrcamentoPorId(@PathParam("id") UUID id) {
        byte[] orcamento = baixarOrcamentoUseCase.executar(id);
        return Response.ok(orcamento).build();
    }

    @GET
    @Path("/ordem-servico/{id}")
    @RolesAllowed({ Perfis.ADMIN, Perfis.ATENDENTE })
    @Operation(summary = "Obter Orcamento por ID da Ordem de Serviço")
    @Produces("application/pdf")
    public Response buscarOrcamentoPorOrdemServicoId(@PathParam("id") UUID id) {
        byte[] orcamento = BaixarOrcamentoPorOrdemServicoUseCase.executar(id);
        return Response.ok(orcamento).build();
    }

    @GET
    @Path("/ordem-servico/{id}/json")
    @RolesAllowed({ Perfis.ADMIN, Perfis.ATENDENTE })
    @Operation(summary = "Obter Orcamento por ID da Ordem de Serviço (JSON)")
    @Produces(MediaType.APPLICATION_JSON)
    public Response buscarOrcamentoPorOrdemServicoIdJson(@PathParam("id") UUID id) {
        Orcamento orcamento = BaixarOrcamentoPorOrdemServicoUseCase.buscarPorOrdemServicoId(id);
        return Response.ok(orcamento).build();
    }
}
