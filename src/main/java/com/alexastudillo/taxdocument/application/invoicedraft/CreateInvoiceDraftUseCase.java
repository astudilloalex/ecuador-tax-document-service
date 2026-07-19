package com.alexastudillo.taxdocument.application.invoicedraft;

import io.smallrye.mutiny.Uni;
import org.jspecify.annotations.NullMarked;

/** Input port for synchronous Invoice Draft creation. */
@NullMarked
public interface CreateInvoiceDraftUseCase {
  Uni<CreateInvoiceDraftResult> create(CreateInvoiceDraftCommand command);
}
