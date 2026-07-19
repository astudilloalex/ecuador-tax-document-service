package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.UUID;

/** Transaction-shaped Company-scoped preparation persistence boundary. */
public interface FiscalPreparationStore {
  Uni<FiscalPreparationLookup> lookup(CompanyId companyId, UUID invoiceDraftId, Duration remaining);

  Uni<FiscalPreparationCommitResult> commit(
      FiscalPreparationCommitIntent intent, Duration remaining);

  default Uni<FiscalPreparationCommitResult> commit(
      FiscalPreparationCommitIntent intent,
      Duration remaining,
      FiscalPreparationCommitTracker commitTracker) {
    return commit(intent, remaining)
        .invoke(ignored -> commitTracker.committed())
        .onFailure()
        .invoke(ignored -> commitTracker.confirmedRollback());
  }
}
