package br.com.fiap.oficina.billing.infrastructure.storage;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Local
@ApplicationScoped
public class LocalFileStorageService implements AssinaturaStorageService {

    private static final String UPLOAD_DIR = "uploads/assinaturas";

    @Override
    public String store(byte[] content, String originalFilename) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String extension = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".png";
            String filename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(filename);

            Files.write(filePath, content);

            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar arquivo", e);
        }
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
    public byte[] baixar(String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR, filename);
            if (!Files.exists(filePath)) {
                return null;
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler arquivo", e);
        }
    }
}
