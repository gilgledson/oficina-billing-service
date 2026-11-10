package br.com.fiap.oficina.billing.infrastructure.gateway;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.logmanager.MDC;

import br.com.fiap.oficina.billing.shared.web.CorrelationIdFilter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Repassa ao OS Service o JWT de quem chamou o Billing (o OS Service aplica
 * as próprias regras de acesso) e o ID de correlação da requisição.
 */
@ApplicationScoped
public class PropagacaoHeadersFactory implements ClientHeadersFactory {

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        String authorization = incomingHeaders.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null) {
            headers.putSingle(HttpHeaders.AUTHORIZATION, authorization);
        }

        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            headers.putSingle(CorrelationIdFilter.HEADER, correlationId);
        }
        return headers;
    }
}
