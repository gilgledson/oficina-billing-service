package br.com.fiap.oficina.billing.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalFileStorageServiceTest {

    private final LocalFileStorageService storage = new LocalFileStorageService();
    private String salvo;

    @AfterEach
    void apagarArquivoDeTeste() throws IOException {
        if (salvo != null) {
            Files.deleteIfExists(Paths.get("uploads/assinaturas", salvo));
        }
    }

    @Test
    @DisplayName("Devolve o arquivo que foi salvo")
    void deveBaixarArquivoSalvo() {
        byte[] conteudo = { 1, 2, 3 };
        salvo = storage.store(conteudo, "assinatura.png");

        assertArrayEquals(conteudo, storage.baixar(salvo));
    }

    @Test
    @DisplayName("Arquivo inexistente devolve nulo")
    void deveDevolverNuloParaArquivoInexistente() {
        assertNull(storage.baixar("nao-existe.png"));
    }

    @Test
    @DisplayName("Recusa nomes que sairiam da pasta de uploads")
    void deveRecusarPathTraversal() throws IOException {
        // Um arquivo real fora da pasta de uploads, para provar que ele não é lido.
        Path fora = Files.createTempFile(Paths.get("target"), "segredo", ".txt");
        try {
            assertNull(storage.baixar("../../" + fora));
            assertNull(storage.baixar("../../target/" + fora.getFileName()));
            assertNull(storage.baixar(fora.toAbsolutePath().toString()));
            assertNull(storage.baixar(".."));
            assertNull(storage.baixar(".oculto"));
            assertNull(storage.baixar("pasta/arquivo.png"));
            assertNull(storage.baixar(""));
            assertNull(storage.baixar(null));
        } finally {
            Files.deleteIfExists(fora);
        }
    }
}
