package com.alexastudillo.taxdocument.api.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.application.requestcontext.RequestDeadline;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InvoiceDraftRequestValidationTest {
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  private final InvoiceDraftRequestPropertyClassifier classifier =
      new InvoiceDraftRequestPropertyClassifier();

  @Test
  void calculatedPathWinsRegardlessOfValueTypeOrOtherProperties() throws Exception {
    JsonNode root =
        mapper.readTree("{\"unknown\":true,\"taxTotals\":null,\"lines\":[{\"tax\":\"same\"}]}");
    assertTrue(classifier.containsCalculatedProperty(root));
    assertTrue(
        classifier.containsCalculatedProperty(
            mapper.readTree("{\"lines\":[{\"grossAmount\":0}]}")));
    assertFalse(
        classifier.containsCalculatedProperty(mapper.readTree("{\"lines\":[{\"Tax\":0}]}")));
  }

  @Test
  void apiMapperForwardsDecodedBusinessAndEmissionPointTextUnchanged() throws Exception {
    String rawEmission = "\t123E4567-E89B-12D3-A456-426614174000\t";
    String rawName = " Cafe\u0301 Buyer ";
    JsonNode json = mapper.readTree(validJson(rawEmission, rawName));
    CreateInvoiceDraftRequest request =
        Objects.requireNonNull(mapper.treeToValue(json, CreateInvoiceDraftRequest.class));
    InvoiceDraftRequestState state = new InvoiceDraftRequestState();
    state.initialize(
        Instant.parse("2026-07-17T12:00:00Z"),
        RequestDeadline.start(Duration.ofSeconds(10)),
        "corr",
        System.nanoTime());
    state.companyId(new CompanyId(UUID.fromString("11111111-1111-4111-8111-111111111111")));
    state.idempotencyKey("key");
    var command = new InvoiceDraftApiMapper().toCommand(request, state);
    assertEquals(rawEmission, command.emissionPointId());
    assertEquals(rawName, command.buyer().legalName());
    assertEquals(" SKU1 ", command.lines().getFirst().productCode());
  }

  static String validJson(String emissionPoint, String buyerName) {
    return "{\"emissionPointId\":"
        + quote(emissionPoint)
        + ",\"emissionDate\":\"2026-07-17\",\"buyer\":{\"identificationType\":\"06\","
        + "\"identification\":\" ABC123 \",\"legalName\":"
        + quote(buyerName)
        + ",\"email\":\"Buyer@Example.COM\"},\"lines\":[{\"productCode\":\" SKU1 \","
        + "\"description\":\" Service \",\"quantity\":\"1\",\"unitPrice\":\"10.000000\","
        + "\"discount\":\"0.00\",\"taxRuleId\":\"5b34b038-931c-50e3-a84c-10af272fdcd4\"}],"
        + "\"payments\":[{\"paymentMethodId\":\"639f2b7e-10a3-5d92-a1a3-28223896f5b5\","
        + "\"amount\":\"11.50\"}],\"additionalInformation\":[]}";
  }

  private static String quote(String value) {
    try {
      return new ObjectMapper().writeValueAsString(value);
    } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
