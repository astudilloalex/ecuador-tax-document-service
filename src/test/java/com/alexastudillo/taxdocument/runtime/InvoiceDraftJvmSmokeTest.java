package com.alexastudillo.taxdocument.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class InvoiceDraftJvmSmokeTest {
  @Test
  void mandatoryJvmRuntimeAndCanonicalOpenApiAreConfigured() throws Exception {
    Properties gradle = new Properties();
    try (var input = Files.newInputStream(Path.of("gradle.properties"))) {
      gradle.load(input);
    }
    assertEquals("3.33.2.1", gradle.getProperty("quarkusPluginVersion"));
    assertEquals("3.33.2.1", gradle.getProperty("quarkusPlatformVersion"));
    assertTrue(
        Files.readString(Path.of("build.gradle.kts")).contains("JavaLanguageVersion.of(25)"));
    assertArrayEquals(
        Files.readAllBytes(
            Path.of("specs/001-create-invoice-draft/contracts/invoice-draft-api.openapi.yaml")),
        Files.readAllBytes(Path.of("src/main/resources/META-INF/openapi.yaml")));
    String openapi = Files.readString(Path.of("src/main/resources/META-INF/openapi.yaml"));
    assertFalse(openapi.contains("'401':"));
    assertFalse(openapi.contains("'403':"));
  }

  @Test
  void runtimeConfigurationKeepsReactivePersistenceAndSchemaValidation() throws Exception {
    String properties = Files.readString(Path.of("src/main/resources/application.properties"));
    assertTrue(properties.contains("quarkus.datasource.reactive=true"));
    assertTrue(properties.contains("quarkus.flyway.migrate-at-start=true"));
    assertTrue(properties.contains("quarkus.hibernate-orm.schema-management.strategy=validate"));
    assertTrue(properties.contains("mp.openapi.scan.disable=true"));
  }
}
