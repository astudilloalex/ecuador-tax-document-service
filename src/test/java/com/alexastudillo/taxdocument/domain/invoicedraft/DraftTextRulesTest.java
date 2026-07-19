package com.alexastudillo.taxdocument.domain.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DraftTextRulesTest {
  @Test
  void productionProductRuleConsumesApplicationNormalizedAsciiVectors() throws Exception {
    JsonNode root =
        new ObjectMapper()
            .readTree(
                Path.of("src/test/resources/invoicedraft/ascii-validation-vectors.json").toFile());
    for (JsonNode vector : root.required("requestPipelineVectors")) {
      if (!"lines[].productCode".equals(vector.required("field").textValue())) {
        continue;
      }
      boolean expected =
          "ACCEPTED".equals(vector.required("expectedApplicationOutcome").textValue());
      assertTrue(
          expected
              == InvoiceLine.productCodeIsValid(
                  vector.required("applicationNormalizedValue").textValue()),
          vector.required("id").textValue());
    }
  }

  @Test
  void domainAcceptsAlreadyDerivedCanonicalNameWithoutNormalizingIt() {
    AdditionalInformation information =
        new AdditionalInformation(
            UUID.randomUUID(), 1, "Display   Name", "display name", "Value 😀");
    assertEquals("display name", information.canonicalName());
  }

  @Test
  void domainConsumesOnlyStage6AcceptedUnicodeFixtureValues() throws Exception {
    JsonNode root =
        new ObjectMapper()
            .readTree(
                Path.of("src/test/resources/invoicedraft/unicode-text-validation-vectors.json")
                    .toFile());
    for (JsonNode vector : root.required("vectors")) {
      if (!"ACCEPTED".equals(vector.required("expectedStage6Outcome").textValue())) {
        assertTrue(
            vector.required("expectedDomainInput").isNull(), vector.required("id").textValue());
        continue;
      }
      String domainInput = vector.required("expectedDomainInput").textValue();
      if ("BUYER_EMAIL".equals(vector.required("fieldCategory").textValue())) {
        boolean expected =
            "ACCEPTED".equals(vector.required("expectedBusinessValidationOutcome").textValue());
        assertTrue(expected == Buyer.emailIsValid(domainInput), vector.required("id").textValue());
      } else {
        assertTrue(domainInput.codePointCount(0, domainInput.length()) <= 300);
      }
    }
  }
}
