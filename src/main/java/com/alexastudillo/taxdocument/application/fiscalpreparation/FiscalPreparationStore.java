package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

/** Transaction-shaped Company-scoped preparation persistence boundary. */
@NullMarked
public interface FiscalPreparationStore {
  Uni<@NonNull FiscalPreparationLookup> lookup(
      CompanyId companyId, UUID invoiceDraftId, Duration remaining);

  Uni<@NonNull FiscalPreparationCommitResult> commit(
      FiscalPreparationCommitIntent intent, Duration remaining);

  default Uni<@NonNull FiscalPreparationCommitResult> commit(
      FiscalPreparationCommitIntent intent,
      Duration remaining,
      FiscalPreparationCommitTracker commitTracker) {
    return Objects.requireNonNull(
        commit(intent, remaining)
            .invoke(_ -> commitTracker.committed())
            .onFailure()
            .invoke(_ -> commitTracker.confirmedRollback()),
        "tracked commit");
  }
}
