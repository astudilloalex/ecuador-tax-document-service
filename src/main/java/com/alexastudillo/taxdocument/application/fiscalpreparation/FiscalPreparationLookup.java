package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalPreparation;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/** Explicit preflight outcomes with no nullable or cross-Company distinction. */
@NullMarked
public sealed interface FiscalPreparationLookup {
  record Existing(FiscalPreparation preparation) implements FiscalPreparationLookup {
    public Existing {
      Objects.requireNonNull(preparation, "preparation");
    }
  }

  record EligibleDraft(InvoiceDraftPreparationView draft) implements FiscalPreparationLookup {
    public EligibleDraft {
      Objects.requireNonNull(draft, "draft");
    }
  }

  record NotFound() implements FiscalPreparationLookup {}

  record NotPreparable(NotPreparableReason reason) implements FiscalPreparationLookup {
    public NotPreparable {
      Objects.requireNonNull(reason, "reason");
    }
  }

  enum NotPreparableReason {
    NON_DRAFT,
    ORPHAN_FISCAL_IDENTITY,
    DUPLICATE_PREPARATION,
    INCONSISTENT_LINK
  }
}
