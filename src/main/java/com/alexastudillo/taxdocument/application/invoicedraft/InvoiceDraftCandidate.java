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
    HashValue idempotencyKeyHashValue,
    HashValue requestFingerprintValue,
    short normalizationVersion) {
  public InvoiceDraftCandidate {
    Objects.requireNonNull(draft, "draft");
    lineTaxIdentifiers = Map.copyOf(lineTaxIdentifiers);
    taxTotalIdentifiers = Map.copyOf(taxTotalIdentifiers);
    Objects.requireNonNull(idempotencyKeyHashValue, "idempotencyKeyHashValue");
    Objects.requireNonNull(requestFingerprintValue, "requestFingerprintValue");
    if (normalizationVersion < 1) {
      throw new IllegalArgumentException("normalizationVersion must be positive");
    }
  }

  public InvoiceDraftCandidate(
      InvoiceDraft draft,
      Map<UUID, UUID> lineTaxIdentifiers,
      Map<String, UUID> taxTotalIdentifiers,
      byte[] idempotencyKeyHash,
      byte[] requestFingerprint,
      short normalizationVersion) {
    this(
        draft,
        lineTaxIdentifiers,
        taxTotalIdentifiers,
        new HashValue(idempotencyKeyHash, "idempotencyKeyHash"),
        new HashValue(requestFingerprint, "requestFingerprint"),
        normalizationVersion);
  }

  public byte[] idempotencyKeyHash() {
    return idempotencyKeyHashValue.bytes();
  }

  public byte[] requestFingerprint() {
    return requestFingerprintValue.bytes();
  }

  /** Immutable defensive wrapper keeps arrays out of record components and equality semantics. */
  public static final class HashValue {
    private final byte[] bytes;

    private HashValue(byte[] value, String name) {
      Objects.requireNonNull(value, name);
      if (value.length != 32) {
        throw new IllegalArgumentException(name + " must contain 32 bytes");
      }
      bytes = Arrays.copyOf(value, value.length);
    }

    public byte[] bytes() {
      return Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof HashValue value && Arrays.equals(bytes, value.bytes);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(bytes);
    }
  }
}
