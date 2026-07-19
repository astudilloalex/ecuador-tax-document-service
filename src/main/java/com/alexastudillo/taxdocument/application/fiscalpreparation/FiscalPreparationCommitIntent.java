package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalContextSnapshot;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * Fully prevalidated facts passed into the atomic persistence operation without proposed identity.
 */
@NullMarked
public record FiscalPreparationCommitIntent(
    InvoiceDraftPreparationView draft, FiscalContextSnapshot snapshot) {
  public FiscalPreparationCommitIntent {
    Objects.requireNonNull(draft, "draft");
    Objects.requireNonNull(snapshot, "snapshot");
    if (!draft.emissionPointId().equals(snapshot.emissionPointId())) {
      throw new IllegalArgumentException("Fiscal context does not match the Invoice Draft");
    }
  }
}
