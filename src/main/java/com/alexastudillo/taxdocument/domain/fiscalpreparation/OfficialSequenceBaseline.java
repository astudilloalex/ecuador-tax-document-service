package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/** Existing controlled mutable baseline state represented immutably at the domain boundary. */
@NullMarked
public record OfficialSequenceBaseline(
    UUID id,
    CompanyId companyId,
    OfficialSequenceScope scope,
    int lastAllocated,
    Instant createdAt,
    Instant updatedAt) {
  public OfficialSequenceBaseline {
    FiscalContextSnapshot.requireNonNil(id, "baselineId");
    Objects.requireNonNull(companyId, "companyId");
    Objects.requireNonNull(scope, "scope");
    if (lastAllocated < 0 || lastAllocated > 999_999_999) {
      throw new IllegalArgumentException("Official Sequence Baseline is invalid");
    }
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
    if (updatedAt.isBefore(createdAt)) {
      throw new IllegalArgumentException("Official Sequence Baseline timestamps are invalid");
    }
  }

  public AllocationDecision allocationDecision() {
    return lastAllocated == 999_999_999
        ? new AllocationDecision.Exhausted()
        : new AllocationDecision.Next(OfficialSequentialNumber.of(lastAllocated + 1));
  }

  public sealed interface AllocationDecision {
    record Next(OfficialSequentialNumber sequentialNumber) implements AllocationDecision {
      public Next {
        Objects.requireNonNull(sequentialNumber, "sequentialNumber");
      }
    }

    record Exhausted() implements AllocationDecision {}
  }
}
