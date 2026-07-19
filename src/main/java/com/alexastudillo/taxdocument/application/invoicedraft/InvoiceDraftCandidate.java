package com.alexastudillo.taxdocument.application.invoicedraft;

import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceDraft;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Complete timestamp-free aggregate handoff to persistence. */
@NullMarked
public record InvoiceDraftCandidate(
    InvoiceDraft draft,
    Map<@NonNull UUID, @NonNull UUID> lineTaxIdentifiers,
    Map<@NonNull String, @NonNull UUID> taxTotalIdentifiers,
    HashValue idempotencyKeyHashValue,
    HashValue requestFingerprintValue,
    short normalizationVersion) {
  public InvoiceDraftCandidate {
    Objects.requireNonNull(draft, "draft");
    lineTaxIdentifiers = Objects.requireNonNull(Map.copyOf(lineTaxIdentifiers));
    taxTotalIdentifiers = Objects.requireNonNull(Map.copyOf(taxTotalIdentifiers));
    Objects.requireNonNull(idempotencyKeyHashValue, "idempotencyKeyHashValue");
    Objects.requireNonNull(requestFingerprintValue, "requestFingerprintValue");
    if (normalizationVersion < 1) {
      throw new IllegalArgumentException("normalizationVersion must be positive");
    }
  }

  public InvoiceDraftCandidate(
      InvoiceDraft draft,
      Map<@NonNull UUID, @NonNull UUID> lineTaxIdentifiers,
      Map<@NonNull String, @NonNull UUID> taxTotalIdentifiers,
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
      bytes = Objects.requireNonNull(Arrays.copyOf(value, value.length));
    }

    public byte[] bytes() {
      return Objects.requireNonNull(Arrays.copyOf(bytes, bytes.length));
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return other instanceof HashValue value && Arrays.equals(bytes, value.bytes);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(bytes);
    }
  }
}
