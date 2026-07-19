package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalPreparation;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/** Atomic store result distinguishing a new commit from a natural replay. */
@NullMarked
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
