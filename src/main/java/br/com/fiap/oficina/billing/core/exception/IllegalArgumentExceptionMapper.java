package br.com.fiap.oficina.billing.core.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.LocalDateTime;

@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        ErroResponse erroResponse = new ErroResponse(
                Response.Status.BAD_REQUEST.getStatusCode(),
                "BAD REQUEST",
                exception.getMessage(),
                LocalDateTime.now()
        );

        // Devolve um HTTP 400 em vez do feio 500
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(erroResponse)
                .build();
    }
}






