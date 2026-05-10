package com.circleguard.file.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;
    private final Path testRoot = Paths.get("uploads");

    @BeforeEach
    void setUp() throws IOException {
        fileStorageService = new FileStorageService();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(testRoot)) {
            Files.walk(testRoot)
                 .sorted(Comparator.reverseOrder())
                 .forEach(path -> {
                     try {
                         Files.delete(path);
                     } catch (IOException e) {
                         // ignore
                     }
                 });
        }
    }

    @Test
    void saveFile_StoresFileCorrectly() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hello.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        String savedFilename = fileStorageService.saveFile(file);

        assertNotNull(savedFilename);
        assertTrue(savedFilename.endsWith("_hello.txt"));
        assertTrue(Files.exists(testRoot.resolve(savedFilename)));
    }
}
