package com.alexastudillo.taxdocument.application.invoicedraft;

import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceDraft;
import java.time.Instant;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;

/** Transport-neutral new-or-replayed result. */
@NullMarked
public record CreateInvoiceDraftResult(
    InvoiceDraft draft, Instant createdAt, Instant updatedAt, boolean replayed) {
  public CreateInvoiceDraftResult {
    Objects.requireNonNull(draft, "draft");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
  }

  public static CreateInvoiceDraftResult newResult(PersistedInvoiceDraft persisted) {
    return from(persisted, false);
  }

  public static CreateInvoiceDraftResult replay(PersistedInvoiceDraft persisted) {
    return from(persisted, true);
  }

  private static CreateInvoiceDraftResult from(PersistedInvoiceDraft persisted, boolean replayed) {
    return new CreateInvoiceDraftResult(
        persisted.draft(), persisted.createdAt(), persisted.updatedAt(), replayed);
  }
}
