package com.alexastudillo.taxdocument.application.invoicedraft;

import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceDraft;
import java.time.Instant;
import java.util.Objects;

/** Committed persistence result. */
public record PersistedInvoiceDraft(InvoiceDraft draft, Instant createdAt, Instant updatedAt) {
  public PersistedInvoiceDraft {
    Objects.requireNonNull(draft, "draft");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
  }
}
