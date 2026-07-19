package com.alexastudillo.taxdocument.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
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
    ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
    JsonNode canonical =
        yaml.readTree(
            Path.of("specs/001-create-invoice-draft/contracts/invoice-draft-api.openapi.yaml")
                .toFile());
    JsonNode runtime = yaml.readTree(Path.of("src/main/resources/META-INF/openapi.yaml").toFile());
    assertEquals(
        canonical.required("paths").required("/invoice-drafts"),
        runtime.required("paths").required("/invoice-drafts"));
    for (Map.Entry<String, JsonNode> group : canonical.required("components").properties()) {
      for (Map.Entry<String, JsonNode> entry : group.getValue().properties()) {
        assertEquals(
            entry.getValue(),
            runtime.required("components").required(group.getKey()).get(entry.getKey()));
      }
    }
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
