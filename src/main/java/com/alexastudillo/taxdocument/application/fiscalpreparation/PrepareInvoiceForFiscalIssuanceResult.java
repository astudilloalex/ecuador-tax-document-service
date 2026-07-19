package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalPreparation;
import java.util.Objects;

/** Synchronous observable preparation result. */
public record PrepareInvoiceForFiscalIssuanceResult(
    FiscalPreparation preparation, boolean replayed) {
  public PrepareInvoiceForFiscalIssuanceResult {
    Objects.requireNonNull(preparation, "preparation");
  }
}
