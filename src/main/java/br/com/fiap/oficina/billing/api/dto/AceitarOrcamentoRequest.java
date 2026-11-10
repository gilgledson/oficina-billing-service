package br.com.fiap.oficina.billing.api.dto;

import java.util.List;
import java.util.UUID;

public record AceitarOrcamentoRequest(
    String assinaturaBase64,
    List<UUID> servicosAceitos,
    List<UUID> servicosRejeitados
) {}






