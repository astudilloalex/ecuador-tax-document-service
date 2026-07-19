package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class AuthoritativeFiscalContextContractTest {
  @Test
  void approvedContractMetadataAndNonRoutablePlanningPlaceholderAreExplicit() throws Exception {
    JsonNode contract =
        new ObjectMapper(new YAMLFactory())
            .readTree(
                Path.of(
                        "specs/002-prepare-invoice-issuance/contracts/authoritative-fiscal-context.openapi.yaml")
                    .toFile());
    JsonNode info = contract.required("info");
    assertEquals("1.0.0", info.required("version").asText());
    assertEquals("authoritative-fiscal-context", info.required("x-capability-id").asText());
    assertEquals("Fiscal Context Provider Owner", info.required("x-accountable-role").asText());
    assertEquals("approved-contract-first", info.required("x-dependency-status").asText());
    String placeholder = contract.required("servers").get(0).required("url").asText();
    assertTrue(placeholder.endsWith(".invalid"));
    assertFalse(
        java.nio.file.Files.readString(Path.of("src/main/resources/application.properties"))
            .contains(placeholder));
  }
}
