package br.com.fiap.oficina.billing.application.gateway;

import br.com.fiap.oficina.billing.application.event.EventoIntegracao;

/**
 * Porta de saída para publicar eventos de integração. Deve ser chamada dentro
 * da transação do caso de uso: a implementação grava o evento junto com o
 * dado de negócio (outbox) e a entrega ao broker acontece depois.
 */
public interface EventPublisher {

    void publicar(EventoIntegracao evento);
}
