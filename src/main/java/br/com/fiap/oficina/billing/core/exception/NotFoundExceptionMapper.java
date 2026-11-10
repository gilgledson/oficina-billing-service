package br.com.fiap.oficina.billing.core.exception;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.LocalDateTime;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
    @Override
    public Response toResponse(NotFoundException exception) {
        ErroResponse erroResponse = new ErroResponse(
                Response.Status.NOT_FOUND.getStatusCode(),
                "NOT FOUND",
                exception.getMessage(),
                LocalDateTime.now()
        );
        return Response.status(Response.Status.NOT_FOUND)
                .entity(erroResponse)
                .build();
    }
}






