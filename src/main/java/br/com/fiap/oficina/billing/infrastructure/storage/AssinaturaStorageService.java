package br.com.fiap.oficina.billing.infrastructure.storage;

public interface AssinaturaStorageService {

    String store(byte[] content, String originalFilename);

    String storeBase64(String base64Content, String filenamePrefix);

    byte[] baixar(String filename);
}
