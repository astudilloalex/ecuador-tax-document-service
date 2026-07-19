package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalPreparation;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/** Synchronous observable preparation result. */
@NullMarked
public record PrepareInvoiceForFiscalIssuanceResult(
    FiscalPreparation preparation, boolean replayed) {
  public PrepareInvoiceForFiscalIssuanceResult {
    Objects.requireNonNull(preparation, "preparation");
  }
}
