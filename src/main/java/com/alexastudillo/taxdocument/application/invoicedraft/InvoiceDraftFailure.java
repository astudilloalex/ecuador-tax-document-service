package com.alexastudillo.taxdocument.application.invoicedraft;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Safe transport-neutral application failure. */
public record InvoiceDraftFailure(
    Code code, String detail, boolean retryable, List<Violation> violations)
    implements Serializable {
  private static final long serialVersionUID = 1L;

  public InvoiceDraftFailure {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(detail, "detail");
    violations = violations == null ? List.<Violation>of() : List.copyOf(violations);
  }

  public enum Code {
    BUSINESS_VALIDATION_FAILED,
    IDEMPOTENCY_CONFLICT,
    PERSISTENCE_UNAVAILABLE,
    REQUEST_TIMEOUT,
    INTERNAL_ERROR
  }

  public record Violation(
      String code,
      String field,
      String validationStage,
      @Nullable Integer maximum,
      @Nullable String countingUnit)
      implements Serializable {
    private static final long serialVersionUID = 1L;
  }

  public static InvoiceDraftFailure validation(String code, String field, String stage) {
    return new InvoiceDraftFailure(
        Code.BUSINESS_VALIDATION_FAILED,
        "The Invoice Draft request violates an approved business rule",
        false,
        List.<Violation>of(new Violation(code, field, stage, null, null)));
  }
}
