package br.com.fiap.oficina.billing.application.usecase.consultas;

import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import br.com.fiap.oficina.billing.domain.valueObject.StatusOrcamento;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class baixarOrcamentoPorOrdemServicoUseCaseTest {

    @Mock
    private OrcamentoRepository OrcamentoRepository;

    @Mock
    private BaixarOrcamentoUseCase BaixarOrcamentoUseCase;

    @InjectMocks
    private BaixarOrcamentoPorOrdemServicoUseCaseImpl useCase;

    @Test
    @DisplayName("Deve retornar Orcamento ao buscar por ordem de serviço existente")
    void devebuscarOrcamentoPorOrdemServicoComSucesso() {
        UUID osId = UUID.randomUUID();
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), null, osId, null,
                new BigDecimal("300.00"), StatusOrcamento.PENDENTE, MetodoPagamento.PIX);

        when(OrcamentoRepository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.of(orcamento));

        Orcamento resultado = useCase.buscarPorOrdemServicoId(osId);

        assertEquals(orcamento, resultado);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException ao buscar por ordem de serviço inexistente")
    void deveLancarExcecaoQuandoOrdemServicoNaoTemFatura() {
        UUID osId = UUID.randomUUID();
        when(OrcamentoRepository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.buscarPorOrdemServicoId(osId));
    }

    @Test
    @DisplayName("Deve executar download chamando BaixarOrcamentoUseCase com id do Or�amento")
    void deveExecutarDownloadDaFatura() {
        UUID osId = UUID.randomUUID();
        UUID orcamentoId = UUID.randomUUID();
        Orcamento orcamento = new Orcamento(orcamentoId, null, osId, null,
                new BigDecimal("300.00"), StatusOrcamento.PENDENTE, MetodoPagamento.PIX);
        byte[] pdf = new byte[] { 1, 2, 3 };

        when(OrcamentoRepository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.of(orcamento));
        when(BaixarOrcamentoUseCase.executar(orcamentoId)).thenReturn(pdf);

        byte[] resultado = useCase.executar(osId);

        assertArrayEquals(pdf, resultado);
        verify(BaixarOrcamentoUseCase).executar(orcamentoId);
    }
}




