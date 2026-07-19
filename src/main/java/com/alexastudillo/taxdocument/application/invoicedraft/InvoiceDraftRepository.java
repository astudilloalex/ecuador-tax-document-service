package com.alexastudillo.taxdocument.application.invoicedraft;

import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

/** Company-scoped aggregate and idempotency persistence boundary. */
public interface InvoiceDraftRepository {
  Uni<IdempotencyLookup> findByIdempotency(
      CompanyId companyId, byte[] keyHash, byte[] requestFingerprint, Duration remaining);

  Uni<PersistedInvoiceDraft> persist(InvoiceDraftCandidate candidate, Duration remaining);

  sealed interface IdempotencyLookup {
    record Missing() implements IdempotencyLookup {}

    record Equivalent(PersistedInvoiceDraft persisted) implements IdempotencyLookup {
      public Equivalent {
        Objects.requireNonNull(persisted, "persisted");
      }
    }

    final class Conflict implements IdempotencyLookup {
      private final byte[] storedFingerprint;

      public Conflict(byte[] storedFingerprint) {
        this.storedFingerprint = Arrays.copyOf(storedFingerprint, storedFingerprint.length);
      }

      public byte[] storedFingerprint() {
        return Arrays.copyOf(storedFingerprint, storedFingerprint.length);
      }
    }
  }
}
