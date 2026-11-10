package br.com.fiap.oficina.billing.shared.security;

/**
 * Perfis (claim {@code groups}) dos JWTs emitidos pelo OS Service. O Billing
 * só valida o token; quem emite e mantém os usuários é o OS Service.
 */
public final class Perfis {

    public static final String ADMIN = "ADMIN";
    public static final String MECANICO = "MECANICO";
    public static final String ATENDENTE = "ATENDENTE";
    public static final String CLIENTE = "CLIENTE";

    private Perfis() {
    }
}
