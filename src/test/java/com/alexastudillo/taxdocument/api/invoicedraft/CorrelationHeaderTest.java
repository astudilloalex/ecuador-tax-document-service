package com.alexastudillo.taxdocument.api.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CorrelationHeaderTest {
  private final CorrelationHeader header = new CorrelationHeader();

  @Test
  void preservesOneSafeValueAfterOneTransportTrim() {
    CorrelationHeader.Classification result = header.classify(List.of(" corr:42 "));
    assertTrue(result.validInput());
    assertEquals("corr:42", result.safeValue());
  }

  @Test
  void absentGeneratesSafeUuidAndInvalidNeverEchoesInput() {
    assertTrue(header.classify(null).validInput());
    CorrelationHeader.Classification invalid = header.classify(List.of("unsafe value"));
    assertFalse(invalid.validInput());
    UUID.fromString(invalid.safeValue());
    assertFalse(invalid.safeValue().contains("unsafe"));
  }

  @Test
  void idempotencyHeaderRejectsMissingInvalidAndMultipleDeterministically() {
    IdempotencyKeyHeader keys = new IdempotencyKeyHeader();
    assertEquals("Key Case", keys.parse(List.of("\tKey Case ")));
    assertEquals("IDEMPOTENCY_KEY_REQUIRED", keyFailure(keys, null).code());
    assertEquals("IDEMPOTENCY_KEY_INVALID", keyFailure(keys, List.of("   ")).code());
    assertEquals("IDEMPOTENCY_KEY_MULTIPLE", keyFailure(keys, List.of("a,b")).code());
    assertEquals("IDEMPOTENCY_KEY_MULTIPLE", keyFailure(keys, List.of("a", "b")).code());
  }

  private static ProblemDetails.ApiException keyFailure(
      IdempotencyKeyHeader header, List<String> values) {
    try {
      header.parse(values);
      throw new AssertionError("Expected failure");
    } catch (ProblemDetails.ApiException exception) {
      return exception;
    }
  }
}
