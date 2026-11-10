package br.com.fiap.oficina.billing.infrastructure.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Escolhe em runtime, via oficina.storage.type (STORAGE_TYPE), qual
 * implementação de AssinaturaStorageService é usada: "local" (disco do pod,
 * padrão de dev) ou "azure" (Azure Blob Storage, padrão de produção).
 */
@ApplicationScoped
public class StorageServiceProducer {

    @Produces
    @ApplicationScoped
    public AssinaturaStorageService storageService(
            @ConfigProperty(name = "oficina.storage.type", defaultValue = "local") String tipo,
            @Local AssinaturaStorageService local,
            @Azure AssinaturaStorageService azure) {
        return "azure".equalsIgnoreCase(tipo) ? azure : local;
    }
}
