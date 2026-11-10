package br.com.fiap.oficina.billing.shared.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jboss.logmanager.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    private ContainerRequestContext request;

    @Mock
    private ContainerResponseContext response;

    @Mock
    private UriInfo uriInfo;

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void limparMdc() {
        MDC.remove(CorrelationIdFilter.MDC_KEY);
    }

    @Test
    @DisplayName("Deve reaproveitar o correlationId recebido no cabeçalho")
    void deveReaproveitarCorrelationIdRecebido() {
        when(request.getHeaderString(CorrelationIdFilter.HEADER)).thenReturn("corr-123");

        filter.filter(request);

        assertEquals("corr-123", MDC.get(CorrelationIdFilter.MDC_KEY));
        verify(request).setProperty(CorrelationIdFilter.MDC_KEY, "corr-123");
    }

    @Test
    @DisplayName("Deve gerar um correlationId quando a requisição não traz um")
    void deveGerarCorrelationId() {
        when(request.getHeaderString(CorrelationIdFilter.HEADER)).thenReturn(" ");

        filter.filter(request);

        String gerado = MDC.get(CorrelationIdFilter.MDC_KEY);
        assertNotNull(gerado);
        assertTrue(gerado.length() >= 32);
        verify(request).setProperty(eq(CorrelationIdFilter.MDC_KEY), any());
    }

    @Test
    @DisplayName("Deve devolver o correlationId na resposta e limpar o MDC")
    void deveDevolverCorrelationIdNaResposta() {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        MDC.put(CorrelationIdFilter.MDC_KEY, "corr-123");
        when(request.getProperty(CorrelationIdFilter.MDC_KEY)).thenReturn("corr-123");
        when(request.getProperty("requestStartTimeMillis")).thenReturn(System.currentTimeMillis());
        when(request.getMethod()).thenReturn("GET");
        when(request.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/orcamentos/1");
        when(response.getHeaders()).thenReturn(headers);
        when(response.getStatus()).thenReturn(200);

        filter.filter(request, response);

        assertEquals("corr-123", headers.getFirst(CorrelationIdFilter.HEADER));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
