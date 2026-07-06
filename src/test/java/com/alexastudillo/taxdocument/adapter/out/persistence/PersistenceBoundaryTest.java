package com.alexastudillo.taxdocument.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PersistenceBoundaryTest {
    private static final Path MAIN_SOURCE = Path.of("src/main/java/com/alexastudillo/taxdocument");
    private static final List<String> FORBIDDEN_DOMAIN_APPLICATION_TERMS = List.of(
            "jakarta.persistence",
            "org.hibernate",
            "io.quarkus",
            "Panache",
            "java.sql",
            "javax.sql",
            "Flyway",
            "PostgreSQL",
            "adapter.out.persistence");
    private static final List<String> FORBIDDEN_SCOPE_PATHS = List.of(
            "adapter/in/rest",
            "adapter/out/sri",
            "adapter/out/storage",
            "adapter/out/queue",
            "adapter/out/webhook",
            "bootstrap");

    @Test
    void domainAndApplicationDoNotImportPersistenceFrameworksOrAdapterTypes() throws IOException {
        List<Path> files = sourceFiles(Path.of("src/main/java/com/alexastudillo/taxdocument/domain"));
        files.addAll(sourceFiles(Path.of("src/main/java/com/alexastudillo/taxdocument/application")));

        for (Path file : files) {
            String content = Files.readString(file);
            for (String forbidden : FORBIDDEN_DOMAIN_APPLICATION_TERMS) {
                assertFalse(content.contains(forbidden), file + " contains " + forbidden);
            }
        }
    }

    @Test
    void featureDoesNotCreateForbiddenRuntimePackages() throws IOException {
        for (Path file : sourceFiles(MAIN_SOURCE)) {
            String normalized = file.toString().replace('\\', '/');
            for (String forbiddenPath : FORBIDDEN_SCOPE_PATHS) {
                assertFalse(normalized.contains(forbiddenPath), normalized);
            }
        }
    }

    private static List<Path> sourceFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return new java.util.ArrayList<>();
        }
        try (var paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        }
    }
}
