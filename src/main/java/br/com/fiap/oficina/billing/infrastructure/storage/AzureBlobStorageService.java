package br.com.fiap.oficina.billing.infrastructure.storage;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Armazena as assinaturas de aceite de orçamento no Azure Blob Storage.
 * Alternativa a LocalFileStorageService (disco local do pod, que é efêmero:
 * some a cada restart/redeploy e não é compartilhado entre réplicas do HPA).
 * A escolha entre as duas é feita em runtime via oficina.storage.type
 * (StorageServiceProducer). O container de blobs já é provisionado via
 * Terraform (infra/storage.tf) — este serviço só faz upload/download.
 */
@Azure
@ApplicationScoped
public class AzureBlobStorageService implements AssinaturaStorageService {

    private final BlobContainerClient containerClient;

    public AzureBlobStorageService(
            @ConfigProperty(name = "oficina.storage.azure.connection-string") String connectionString,
            @ConfigProperty(name = "oficina.storage.azure.container") String containerName) {
        this.containerClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient()
                .getBlobContainerClient(containerName);
    }

    @Override
    public String store(byte[] content, String originalFilename) {
        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".png";
        String blobName = UUID.randomUUID() + extension;

        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.upload(new ByteArrayInputStream(content), content.length, true);

        return blobName;
    }

    @Override
    public String storeBase64(String base64Content, String filenamePrefix) {
        String pureBase64 = base64Content.contains(",")
                ? base64Content.split(",")[1]
                : base64Content;

        byte[] decodedBytes = Base64.getDecoder().decode(pureBase64);
        return store(decodedBytes, filenamePrefix + ".png");
    }

    @Override
    public byte[] baixar(String blobName) {
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        if (!blobClient.exists()) {
            return null;
        }
        return blobClient.downloadContent().toBytes();
    }
}
