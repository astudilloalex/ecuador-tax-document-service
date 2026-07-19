package com.alexastudillo.taxdocument.api.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Safe RFC 9457-style problem representation with stable English codes. */
@NullMarked
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetails(
    URI type,
    String title,
    int status,
    String code,
    String detail,
    URI instance,
    String correlationId,
    @Nullable List<@NonNull Violation> violations) {
  public ProblemDetails {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(detail, "detail");
    Objects.requireNonNull(instance, "instance");
    Objects.requireNonNull(correlationId, "correlationId");
    violations = violations == null ? null : Objects.requireNonNull(List.<@NonNull Violation>copyOf(violations));
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

  /** Internal API-boundary exception; it never crosses into Application or Domain. */
  public static final class ApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int status;
    private final String code;
    private final transient List<@NonNull Violation> violations;

    public ApiException(int status, String code, String message) {
      this(status, code, message, Objects.requireNonNull(List.<@NonNull Violation>of()));
    }

    public ApiException(int status, String code, String message, List<@NonNull Violation> violations) {
      super(message);
      this.status = status;
      this.code = Objects.requireNonNull(code, "code");
      this.violations = Objects.requireNonNull(List.<@NonNull Violation>copyOf(Objects.requireNonNull(violations, "violations")));
    }

    public int status() {
      return status;
    }

    public String code() {
      return code;
    }

    public List<@NonNull Violation> violations() {
      return violations;
    }
  }
}
