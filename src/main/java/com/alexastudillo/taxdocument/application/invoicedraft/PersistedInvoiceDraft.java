package com.alexastudillo.taxdocument.application.invoicedraft;

import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceDraft;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/** Committed persistence result. */
@NullMarked
public record PersistedInvoiceDraft(InvoiceDraft draft, Instant createdAt, Instant updatedAt) {
  public PersistedInvoiceDraft {
    Objects.requireNonNull(draft, "draft");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
  }
}
