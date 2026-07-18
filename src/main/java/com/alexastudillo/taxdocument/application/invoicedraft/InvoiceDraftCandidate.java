package com.alexastudillo.taxdocument.application.invoicedraft;

import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceDraft;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Complete timestamp-free aggregate handoff to persistence. */
public record InvoiceDraftCandidate(
    InvoiceDraft draft,
    Map<UUID, UUID> lineTaxIdentifiers,
    Map<String, UUID> taxTotalIdentifiers,
    byte[] idempotencyKeyHash,
    byte[] requestFingerprint,
    short normalizationVersion) {
  public InvoiceDraftCandidate {
    Objects.requireNonNull(draft, "draft");
    lineTaxIdentifiers = Map.copyOf(lineTaxIdentifiers);
    taxTotalIdentifiers = Map.copyOf(taxTotalIdentifiers);
    idempotencyKeyHash = requireHash(idempotencyKeyHash, "idempotencyKeyHash");
    requestFingerprint = requireHash(requestFingerprint, "requestFingerprint");
    if (normalizationVersion < 1) {
      throw new IllegalArgumentException("normalizationVersion must be positive");
    }
  }

  @Override
  public byte[] idempotencyKeyHash() {
    return Arrays.copyOf(idempotencyKeyHash, idempotencyKeyHash.length);
  }

  @Override
  public byte[] requestFingerprint() {
    return Arrays.copyOf(requestFingerprint, requestFingerprint.length);
  }

  private static byte[] requireHash(byte[] value, String name) {
    Objects.requireNonNull(value, name);
    if (value.length != 32) {
      throw new IllegalArgumentException(name + " must contain 32 bytes");
    }
    return Arrays.copyOf(value, value.length);
  }
}
