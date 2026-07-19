package com.alexastudillo.taxdocument.application.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class IdempotencyFingerprintTest {
  @Test
  void keyHashIsVersionedDeterministicAndPrivacyMinimal() {
    IdempotencyFingerprint fingerprint = new IdempotencyFingerprint();
    byte[] first = fingerprint.keyHash("Case Sensitive Key");
    assertArrayEquals(first, fingerprint.keyHash("Case Sensitive Key"));
    assertFalse(Arrays.equals(first, fingerprint.keyHash("case sensitive key")));
    assertEquals(32, first.length);
    assertEquals(1, IdempotencyFingerprint.NORMALIZATION_VERSION);
  }

  @Test
  void correlationAndCompanyScopeDoNotAlterBusinessFingerprint() {
    IdempotencyFingerprint fingerprint = new IdempotencyFingerprint();
    CreateInvoiceDraftCommand first = ApplicationTestFixtures.command();
    CreateInvoiceDraftCommand second =
        new CreateInvoiceDraftCommand(
            first.companyId(),
            first.requestCreationInstant(),
            first.deadline(),
            first.idempotencyKey(),
            "different-correlation",
            first.emissionPointId(),
            first.emissionDate(),
            first.buyer(),
            first.lines(),
            first.payments(),
            first.additionalInformation());
    assertArrayEquals(
        fingerprint.requestFingerprint(first), fingerprint.requestFingerprint(second));
  }
}
