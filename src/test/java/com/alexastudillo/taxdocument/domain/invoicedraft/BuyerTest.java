package com.alexastudillo.taxdocument.domain.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class BuyerTest {
  @Test
  void productionIdentificationRuleConsumesApplicationNormalizedAsciiVectors() throws Exception {
    JsonNode root =
        new ObjectMapper()
            .readTree(
                Path.of("src/test/resources/invoicedraft/ascii-validation-vectors.json").toFile());
    for (JsonNode vector : root.required("requestPipelineVectors")) {
      if (!"buyer.identification".equals(vector.required("field").textValue())) {
        continue;
      }
      boolean expected =
          "ACCEPTED".equals(vector.required("expectedApplicationOutcome").textValue());
      assertTrue(
          expected
              == Buyer.identificationIsValid(
                  vector.required("identificationType").textValue(),
                  vector.required("applicationNormalizedValue").textValue()),
          vector.required("id").textValue());
    }
  }

  @Test
  void emailProfileIsAsciiCasePreservingAndDotAtomOnly() {
    assertTrue(Buyer.emailIsValid("Buyer+tax@Example.COM"));
    assertFalse(Buyer.emailIsValid("buyer..name@example.com"));
    assertFalse(Buyer.emailIsValid("búyer@example.com"));
  }

  @Test
  void finalConsumerLimitUsesTheCalculatedGrandTotal() {
    Buyer buyer =
        new Buyer(
            "07",
            "9999999999999",
            "CONSUMIDOR FINAL",
            null,
            null,
            null,
            DomainTestFixtures.VERSION);
    buyer.validateCalculatedTotal(new BigDecimal("50.00"));
    assertThrows(
        DraftValidationException.class,
        () -> buyer.validateCalculatedTotal(new BigDecimal("50.01")));
  }
}
