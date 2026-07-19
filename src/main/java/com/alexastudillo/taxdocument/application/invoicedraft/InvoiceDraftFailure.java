package com.alexastudillo.taxdocument.application.invoicedraft;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Safe transport-neutral application failure. */
@NullMarked
public record InvoiceDraftFailure(
    Code code, String detail, boolean retryable, List<@NonNull Violation> violations)
    implements Serializable {
  private static final long serialVersionUID = 1L;

  public InvoiceDraftFailure {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(detail, "detail");
    violations =
        violations == null
            ? Objects.requireNonNull(List.<@NonNull Violation>of())
            : Objects.requireNonNull(List.<@NonNull Violation>copyOf(violations));
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
        Objects.requireNonNull(
            List.<@NonNull Violation>of(new Violation(code, field, stage, null, null))));
  }
}
