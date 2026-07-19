package com.alexastudillo.taxdocument.api.fiscalpreparation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FiscalPreparationOpenApiContractTest {
  private static final String PATH = "/invoice-drafts/{invoiceDraftId}/fiscal-preparation";
  private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
  private static final Map<String, String> MERGED_NAMES =
      Map.ofEntries(
          Map.entry("parameters/InvoiceDraftId", "FiscalPreparationInvoiceDraftId"),
          Map.entry("parameters/CompanyContext", "FiscalPreparationCompanyContext"),
          Map.entry("parameters/CorrelationId", "FiscalPreparationCorrelationId"),
          Map.entry("headers/CorrelationId", "FiscalPreparationCorrelationId"),
          Map.entry("headers/NoStore", "FiscalPreparationNoStore"),
          Map.entry("schemas/NonNilUuid", "FiscalPreparationNonNilUuid"),
          Map.entry("schemas/OpaqueReference", "FiscalPreparationOpaqueReference"),
          Map.entry("schemas/FiscalText", "FiscalPreparationFiscalText"),
          Map.entry("schemas/ResolutionDesignation", "FiscalPreparationResolutionDesignation"),
          Map.entry(
              "schemas/WithholdingAgentDesignation",
              "FiscalPreparationWithholdingAgentDesignation"),
          Map.entry(
              "schemas/LargeContributorDesignation",
              "FiscalPreparationLargeContributorDesignation"),
          Map.entry("schemas/ProblemDetails", "FiscalPreparationProblemDetails"),
          Map.entry("responses/BadRequest", "FiscalPreparationBadRequest"),
          Map.entry("responses/DraftNotFound", "FiscalPreparationDraftNotFound"),
          Map.entry("responses/PreparationConflict", "FiscalPreparationConflict"),
          Map.entry("responses/UnprocessablePreparation", "FiscalPreparationUnprocessable"),
          Map.entry("responses/InternalPreparationFailure", "FiscalPreparationInternalFailure"),
          Map.entry("responses/PreparationUnavailable", "FiscalPreparationUnavailable"),
          Map.entry("responses/PreparationTimeout", "FiscalPreparationTimeout"));

  @Test
  void runtimeExactlyPublishesTheBodylessFeatureTwoOperationAndAllItsComponents() throws Exception {
    JsonNode canonical =
        YAML.readTree(
            Path.of(
                    "specs/002-prepare-invoice-issuance/contracts/fiscal-preparation-api.openapi.yaml")
                .toFile());
    JsonNode runtime = YAML.readTree(Path.of("src/main/resources/META-INF/openapi.yaml").toFile());
    JsonNode mergedCanonical = rewriteReferences(canonical);
    assertEquals(
        mergedCanonical.required("paths").required(PATH), runtime.required("paths").required(PATH));
    for (Map.Entry<String, JsonNode> group : canonical.required("components").properties()) {
      JsonNode runtimeGroup = runtime.required("components").get(group.getKey());
      assertNotNull(runtimeGroup, group.getKey());
      for (Map.Entry<String, JsonNode> entry : group.getValue().properties()) {
        String runtimeName = mergedName(group.getKey(), entry.getKey());
        assertEquals(
            mergedCanonical.required("components").required(group.getKey()).get(entry.getKey()),
            runtimeGroup.get(runtimeName),
            entry.getKey());
      }
    }
  }

  @Test
  void operationHasOneCompanyHeaderNoBodySecurityIdempotencyOrExcludedSurface() throws Exception {
    JsonNode runtime = YAML.readTree(Path.of("src/main/resources/META-INF/openapi.yaml").toFile());
    JsonNode operation = runtime.required("paths").required(PATH).required("post");
    assertFalse(operation.has("requestBody"));
    assertFalse(operation.has("security"));
    String contract = operation.toString();
    assertFalse(contract.contains("Idempotency-Key"));
    assertFalse(contract.contains("Authorization"));
    assertFalse(contract.contains("\"401\""));
    assertFalse(contract.contains("\"403\""));
    assertFalse(contract.contains("companyId"));
    for (String path : runtime.required("paths").propertyStream().map(Map.Entry::getKey).toList()) {
      assertFalse(
          path.matches(
              ".*(xml|sign|certificate|sri|ride|pdf|email|webhook|queue|cancel|baseline).*"));
    }
  }

  private static JsonNode rewriteReferences(JsonNode canonical) throws Exception {
    String rewritten = canonical.toString();
    for (Map.Entry<String, String> entry : MERGED_NAMES.entrySet()) {
      rewritten =
          rewritten.replace(
              "#/components/" + entry.getKey(),
              "#/components/"
                  + entry.getKey().substring(0, entry.getKey().indexOf('/') + 1)
                  + entry.getValue());
    }
    return YAML.readTree(rewritten);
  }

  private static String mergedName(String group, String name) {
    return MERGED_NAMES.getOrDefault(group + "/" + name, name);
  }
}
