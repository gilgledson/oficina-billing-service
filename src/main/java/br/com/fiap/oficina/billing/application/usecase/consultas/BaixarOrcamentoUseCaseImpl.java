package br.com.fiap.oficina.billing.application.usecase.consultas;

import br.com.fiap.oficina.billing.application.dto.ClienteOrcamentoDTO;
import br.com.fiap.oficina.billing.application.dto.OrdemServicoOrcamentoDTO;
import br.com.fiap.oficina.billing.application.dto.VeiculoOrcamentoDTO;
import br.com.fiap.oficina.billing.application.gateway.AtendimentoGateway;
import br.com.fiap.oficina.billing.application.gateway.OperacionalGateway;
import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import io.quarkus.qute.Template;
import io.quarkus.qute.Location;
import jakarta.enterprise.context.ApplicationScoped;

import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

@ApplicationScoped
public class BaixarOrcamentoUseCaseImpl implements BaixarOrcamentoUseCase {

        private final OrcamentoRepository repository;
        private final AtendimentoGateway atendimentoGateway;
        private final OperacionalGateway operacionalGateway;
        private final Template templateFatura;

        public BaixarOrcamentoUseCaseImpl(
                        OrcamentoRepository repository,
                        AtendimentoGateway atendimentoGateway,
                        OperacionalGateway operacionalGateway,
                        @Location("orcamento/orcamento") Template templateFatura) {
                this.repository = repository;
                this.atendimentoGateway = atendimentoGateway;
                this.operacionalGateway = operacionalGateway;
                this.templateFatura = templateFatura;
        }

        @Override
        public byte[] executar(UUID id) {
                Orcamento orcamento = repository.buscarPorId(id)
                                .orElseThrow(() -> new RuntimeException("Orcamento nao encontrado"));

                OrdemServicoOrcamentoDTO os = operacionalGateway
                                .buscarOrdemDeServicoPorId(orcamento.getOrdemServicoId())
                                .orElseThrow(() -> new RuntimeException("Ordem de servico nao encontrada"));

                ClienteOrcamentoDTO cliente = atendimentoGateway.buscarClientePorId(os.getClienteId())
                                .orElseThrow(() -> new RuntimeException("Cliente nao encontrado"));

                VeiculoOrcamentoDTO veiculo = atendimentoGateway.buscarVeiculoPorId(os.getVeiculoId())
                                .orElseThrow(() -> new RuntimeException("Veiculo nao encontrado"));

                String htmlRenderizado = templateFatura
                                .data("fatura", orcamento)
                                .data("os", os)
                                .data("cliente", cliente)
                                .data("veiculo", veiculo)
                                .render();

                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        ITextRenderer renderer = new ITextRenderer();
                        renderer.setDocumentFromString(htmlRenderizado);
                        renderer.layout();
                        renderer.createPDF(outputStream);
                        return outputStream.toByteArray();
                } catch (Exception e) {
                        throw new RuntimeException("Erro ao gerar PDF do orcamento", e);
                }
        }
}
