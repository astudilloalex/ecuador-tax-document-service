package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class SensitiveDataExposureTest {
  @Test
  void telemetryContainsOnlyApprovedSafeLateDeadlineFields() throws Exception {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/telemetry/InvoiceDraftTelemetry.java"));
    assertTrue(source.contains("request_deadline_exceeded_after_response_commit"));
    assertTrue(source.contains("correlationId"));
    assertTrue(source.contains("draftId"));
    assertFalse(source.contains("idempotencyKey()"));
    assertFalse(source.contains("buyer()."));
    assertFalse(source.contains("request body"));
    assertFalse(source.contains("token="));
  }

  @Test
  void persistedBindingStoresHashesAndNeverTheRawKey() throws Exception {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftIdempotencyEntity.java"));
    assertTrue(source.contains("idempotencyKeyHash"));
    assertTrue(source.contains("requestFingerprint"));
    assertFalse(source.contains("rawKey"));
  }
}
