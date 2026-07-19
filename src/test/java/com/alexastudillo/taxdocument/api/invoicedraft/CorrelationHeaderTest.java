package com.alexastudillo.taxdocument.api.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.api.problem.ProblemDetails;
import com.alexastudillo.taxdocument.api.requestcontext.CorrelationHeader;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@NullMarked
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
    for (List<String> values :
        List.of(
            List.of("   "),
            List.of("unsafe value"),
            List.of("unsafe/value"),
            List.of("ñ"),
            List.of("x".repeat(65)),
            List.of("first", "second"))) {
      CorrelationHeader.Classification invalid = header.classify(values);
      assertFalse(invalid.validInput());
      UUID.fromString(invalid.safeValue());
      assertFalse(invalid.safeValue().contains("unsafe"));
    }
  }

  @Test
  void idempotencyHeaderRejectsMissingInvalidAndMultipleDeterministically() {
    IdempotencyKeyHeader keys = new IdempotencyKeyHeader();
    assertEquals("Key Case", keys.parse(List.of("\tKey Case ")));
    assertEquals("IDEMPOTENCY_KEY_REQUIRED", keyFailure(keys, null).code());
    assertEquals("IDEMPOTENCY_KEY_INVALID", keyFailure(keys, List.of("   ")).code());
    assertEquals("IDEMPOTENCY_KEY_INVALID", keyFailure(keys, List.of("x".repeat(129))).code());
    assertEquals("IDEMPOTENCY_KEY_INVALID", keyFailure(keys, List.of("key\u0001value")).code());
    assertEquals("IDEMPOTENCY_KEY_INVALID", keyFailure(keys, List.of("clé")).code());
    assertEquals("IDEMPOTENCY_KEY_MULTIPLE", keyFailure(keys, List.of("a,b")).code());
    assertEquals("IDEMPOTENCY_KEY_MULTIPLE", keyFailure(keys, List.of("a", "b")).code());
  }

  private static ProblemDetails.ApiException keyFailure(
      IdempotencyKeyHeader header, @Nullable List<String> values) {
    try {
      header.parse(values);
      throw new AssertionError("Expected failure");
    } catch (ProblemDetails.ApiException exception) {
      return exception;
    }
  }
}
