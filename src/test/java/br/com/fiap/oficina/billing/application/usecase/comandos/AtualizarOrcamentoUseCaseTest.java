package br.com.fiap.oficina.billing.application.usecase.comandos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.oficina.billing.application.repository.OrcamentoRepository;
import br.com.fiap.oficina.billing.domain.entity.Orcamento;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class AtualizarOrcamentoUseCaseTest {

    @Mock
    private OrcamentoRepository repository;

    @InjectMocks
    private AtualizarOrcamentoUseCase useCase;

    @Test
    @DisplayName("Deve atualizar o valor do orçamento da OS")
    void deveAtualizarValor() {
        UUID osId = UUID.randomUUID();
        Orcamento orcamento = new Orcamento(osId, new BigDecimal("100.00"), null);
        when(repository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.of(orcamento));

        useCase.executar(osId, new BigDecimal("180.50"));

        assertEquals(new BigDecimal("180.50"), orcamento.getValorTotal());
        verify(repository).atualizar(orcamento);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando a OS não tem orçamento")
    void deveLancarExcecaoQuandoNaoExisteOrcamento() {
        UUID osId = UUID.randomUUID();
        when(repository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.executar(osId, BigDecimal.TEN));

        verify(repository, never()).atualizar(any());
    }
}
