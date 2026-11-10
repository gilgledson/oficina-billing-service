package br.com.fiap.oficina.billing.core.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

class ExceptionMappersTest {

    @Test
    @DisplayName("NotFoundException deve virar 404 com a mensagem do erro")
    void deveMapearNotFound() {
        Response response = new NotFoundExceptionMapper().toResponse(new NotFoundException("Orçamento não encontrado"));

        ErroResponse erro = (ErroResponse) response.getEntity();
        assertEquals(404, response.getStatus());
        assertEquals(404, erro.status());
        assertEquals("NOT FOUND", erro.erro());
        assertEquals("Orçamento não encontrado", erro.mensagem());
        assertNotNull(erro.timestamp());
    }

    @Test
    @DisplayName("IllegalArgumentException deve virar 400 com a mensagem do erro")
    void deveMapearIllegalArgument() {
        Response response = new IllegalArgumentExceptionMapper()
                .toResponse(new IllegalArgumentException("Valor total é obrigatório."));

        ErroResponse erro = (ErroResponse) response.getEntity();
        assertEquals(400, response.getStatus());
        assertEquals(400, erro.status());
        assertEquals("BAD REQUEST", erro.erro());
        assertEquals("Valor total é obrigatório.", erro.mensagem());
        assertNotNull(erro.timestamp());
    }
}
