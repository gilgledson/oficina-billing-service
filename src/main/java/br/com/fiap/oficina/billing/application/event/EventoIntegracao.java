package br.com.fiap.oficina.billing.application.event;

import io.vertx.core.json.JsonObject;

/**
 * Evento publicado pelo Billing Service no broker para os outros
 * microsserviços. A routing key identifica o tipo do evento na exchange
 * compartilhada e faz parte do contrato da Saga.
 */
public interface EventoIntegracao {

    String routingKey();

    JsonObject toJson();
}
