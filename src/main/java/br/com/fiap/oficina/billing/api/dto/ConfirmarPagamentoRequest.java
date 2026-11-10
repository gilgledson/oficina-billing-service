package br.com.fiap.oficina.billing.api.dto;

import br.com.fiap.oficina.billing.domain.valueObject.MetodoPagamento;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ConfirmarPagamentoRequest(
                @NotNull(message = "Método de pagamento é obrigatório") @Schema(description = "Método de pagamento", defaultValue = "PIX") MetodoPagamento metodoPagamento) {
}






