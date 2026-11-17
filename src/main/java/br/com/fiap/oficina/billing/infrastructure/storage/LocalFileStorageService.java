package br.com.fiap.oficina.billing.infrastructure.storage;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

@Local
@ApplicationScoped
public class LocalFileStorageService implements AssinaturaStorageService {

    private static final String UPLOAD_DIR = "uploads/assinaturas";
    /** Letras, números, ponto, hífen e sublinhado; não pode começar com ponto. */
    private static final Pattern NOME_SEGURO = Pattern.compile("^[A-Za-z0-9_-][A-Za-z0-9._-]{0,254}$");

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

    /**
     * O nome vem da URL, então é tratado como entrada não confiável: só são
     * aceitos nomes simples, e o caminho resolvido precisa continuar dentro da
     * pasta de uploads. Sem isso, um nome como "../../etc/passwd" leria
     * qualquer arquivo do servidor.
     */
    @Override
    public byte[] baixar(String filename) {
        if (filename == null || !NOME_SEGURO.matcher(filename).matches()) {
            return null;
        }
        try {
            Path base = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Path filePath = base.resolve(filename).normalize();
            if (!filePath.startsWith(base) || !Files.isRegularFile(filePath)) {
                return null;
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler arquivo", e);
        }
    }
}
