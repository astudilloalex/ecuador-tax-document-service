package com.alexastudillo.taxdocument.application.invoicedraft;

import io.smallrye.mutiny.Uni;

/** Input port for synchronous Invoice Draft creation. */
public interface CreateInvoiceDraftUseCase {
  Uni<CreateInvoiceDraftResult> create(CreateInvoiceDraftCommand command);
}
