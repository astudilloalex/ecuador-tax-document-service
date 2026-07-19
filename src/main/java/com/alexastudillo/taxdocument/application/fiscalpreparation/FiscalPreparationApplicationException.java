package com.alexastudillo.taxdocument.application.fiscalpreparation;

import java.util.Objects;

/** Failure carrier that exposes only the stable transport-neutral fiscal failure. */
public final class FiscalPreparationApplicationException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final FiscalPreparationFailure failure;

  public FiscalPreparationApplicationException(FiscalPreparationFailure failure) {
    super(Objects.requireNonNull(failure, "failure").detail());
    this.failure = failure;
  }

  public FiscalPreparationApplicationException(
      FiscalPreparationFailure failure, Throwable internalCause) {
    super(Objects.requireNonNull(failure, "failure").detail(), internalCause);
    this.failure = failure;
  }

  public FiscalPreparationFailure failure() {
    return failure;
  }
}
