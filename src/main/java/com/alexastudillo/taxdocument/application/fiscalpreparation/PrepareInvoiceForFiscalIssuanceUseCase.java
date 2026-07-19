package com.alexastudillo.taxdocument.application.fiscalpreparation;

import io.smallrye.mutiny.Uni;

/** Directly observable Fiscal Preparation use case. */
@FunctionalInterface
public interface PrepareInvoiceForFiscalIssuanceUseCase {
  Uni<PrepareInvoiceForFiscalIssuanceResult> prepare(
      PrepareInvoiceForFiscalIssuanceCommand command);
}
