package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalPreparation;
import java.util.Objects;

/** Atomic store result distinguishing a new commit from a natural replay. */
public sealed interface FiscalPreparationCommitResult {
  FiscalPreparation preparation();

  record Created(FiscalPreparation preparation) implements FiscalPreparationCommitResult {
    public Created {
      Objects.requireNonNull(preparation, "preparation");
    }
  }

  record Replay(FiscalPreparation preparation) implements FiscalPreparationCommitResult {
    public Replay {
      Objects.requireNonNull(preparation, "preparation");
    }
  }
}
