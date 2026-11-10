package br.com.fiap.oficina.billing.shared.web;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.logmanager.MDC;

import java.util.UUID;

/**
 * Propaga um ID de correlação entre requisições: reaproveita o header
 * {@code X-Correlation-Id} se o cliente/serviço upstream já mandou um, ou
 * gera um novo. Coloca no MDC (aparece automaticamente em todo log JSON
 * emitido durante a requisição, via quarkus-logging-json) e devolve no
 * header de resposta, para permitir correlacionar logs entre múltiplos
 * serviços na mesma cadeia de chamadas.
 *
 * Também loga uma linha estruturada por requisição (método, path, status,
 * duração) — como o log de acesso nativo do Quarkus roda na camada Vert.x
 * (fora do escopo do MDC desta requisição) e a aplicação não tinha nenhum
 * log de requisição na camada de negócio, sem isso o correlationId nunca
 * apareceria em log nenhum.
 */
@Provider
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(CorrelationIdFilter.class);

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    private static final String START_TIME_PROPERTY = "requestStartTimeMillis";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String correlationId = requestContext.getHeaderString(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        requestContext.setProperty(MDC_KEY, correlationId);
        requestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object correlationId = requestContext.getProperty(MDC_KEY);
        if (correlationId != null) {
            responseContext.getHeaders().add(HEADER, correlationId.toString());
        }

        Object startTime = requestContext.getProperty(START_TIME_PROPERTY);
        long durationMs = startTime instanceof Long ? System.currentTimeMillis() - (Long) startTime : -1;

        LOG.infof("%s %s -> %d (%dms)",
                requestContext.getMethod(),
                requestContext.getUriInfo().getPath(),
                responseContext.getStatus(),
                durationMs);

        MDC.remove(MDC_KEY);
    }
}
