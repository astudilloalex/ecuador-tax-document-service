package com.alexastudillo.taxdocument.api.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class InvoiceDraftOpenApiContractTest {
  private static final Path CANONICAL =
      Path.of("specs/001-create-invoice-draft/contracts/invoice-draft-api.openapi.yaml");
  private static final Path RUNTIME = Path.of("src/main/resources/META-INF/openapi.yaml");

  @Test
  void runtimeContractIsByteForByteCanonicalAndHasNoSecurityOutcomes() throws Exception {
    assertArrayEquals(Files.readAllBytes(CANONICAL), Files.readAllBytes(RUNTIME));
    String contract = Files.readString(CANONICAL);
    assertTrue(
        contract.contains(
            "required: true\n      description: >-\n        Mandatory single-valued"));
    assertTrue(contract.contains("x-application-stage-6:"));
    assertTrue(contract.contains("PROHIBITED_CALCULATED_FIELD"));
    assertTrue(contract.contains("EMISSION_POINT_INVALID"));
    assertFalse(contract.contains("'401':"));
    assertFalse(contract.contains("'403':"));
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
}
