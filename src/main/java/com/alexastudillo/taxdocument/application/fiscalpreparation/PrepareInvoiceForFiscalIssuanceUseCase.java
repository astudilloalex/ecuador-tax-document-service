package com.alexastudillo.taxdocument.application.fiscalpreparation;

import io.smallrye.mutiny.Uni;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

/** Directly observable Fiscal Preparation use case. */
@FunctionalInterface
@NullMarked
public interface PrepareInvoiceForFiscalIssuanceUseCase {
  Uni<@NonNull PrepareInvoiceForFiscalIssuanceResult> prepare(
      PrepareInvoiceForFiscalIssuanceCommand command);
}
