package com.alexastudillo.taxdocument.api.invoicedraft;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Path;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class InvoiceDraftOpenApiContractTest {
  private static final Path CANONICAL =
      requireNonNull(
          Path.of("specs/001-create-invoice-draft/contracts/invoice-draft-api.openapi.yaml"));
  private static final Path RUNTIME =
      requireNonNull(Path.of("src/main/resources/META-INF/openapi.yaml"));
  private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

  @Test
  void runtimeSemanticallyPreservesEveryFeatureOneOperationComponentAndSecurityAbsence()
      throws Exception {
    JsonNode canonical = YAML.readTree(CANONICAL.toFile());
    JsonNode runtime = YAML.readTree(RUNTIME.toFile());

    assertEquals(
        canonical.required("paths").required("/invoice-drafts"),
        runtime.required("paths").required("/invoice-drafts"));
    assertCanonicalEntriesPreserved(
        requireNonNull(canonical.required("components")),
        requireNonNull(runtime.required("components")));
    assertFalse(runtime.has("security"));
    assertFalse(
        runtime.required("paths").required("/invoice-drafts").required("post").has("security"));

    String contract = canonical.toString();
    assertTrue(contract.contains("x-application-stage-6"));
    assertTrue(contract.contains("PROHIBITED_CALCULATED_FIELD"));
    assertTrue(contract.contains("EMISSION_POINT_INVALID"));
    assertFalse(contract.contains("\"401\""));
    assertFalse(contract.contains("\"403\""));
  }

  @Test
  void sharedAsciiFixtureUsesStageAppropriateRawNormalizedAndStoredValues() throws Exception {
    JsonNode fixture =
        new ObjectMapper()
            .readTree(
                Path.of("src/test/resources/invoicedraft/ascii-validation-vectors.json").toFile());
    for (JsonNode vector : fixture.required("requestPipelineVectors")) {
      assertTrue(vector.has("rawValue"));
      assertTrue(vector.has("applicationNormalizedValue"));
      assertTrue(vector.has("expectedStoredValue"));
      assertTrue(vector.has("failureStage"));
    }
  }

  private static void assertCanonicalEntriesPreserved(JsonNode canonical, JsonNode runtime) {
    for (Map.Entry<String, JsonNode> group : canonical.properties()) {
      JsonNode runtimeGroup = runtime.get(group.getKey());
      assertNotNull(runtimeGroup, "Missing component group " + group.getKey());
      for (Map.Entry<String, JsonNode> entry : group.getValue().properties()) {
        assertEquals(
            entry.getValue(),
            runtimeGroup.get(entry.getKey()),
            "Feature 001 component drift: " + group.getKey() + "/" + entry.getKey());
      }
    }
  }
}
