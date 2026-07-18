package com.alexastudillo.taxdocument.application.invoicedraft;

import java.util.Objects;

/** Exception carrier for a safe transport-neutral application failure. */
public final class InvoiceDraftApplicationException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final InvoiceDraftFailure failure;

  public InvoiceDraftApplicationException(InvoiceDraftFailure failure) {
    super(failure.detail());
    this.failure = Objects.requireNonNull(failure, "failure");
  }

  public InvoiceDraftApplicationException(InvoiceDraftFailure failure, Throwable cause) {
    super(failure.detail(), cause);
    this.failure = Objects.requireNonNull(failure, "failure");
  }

  public InvoiceDraftFailure failure() {
    return failure;
  }
}
