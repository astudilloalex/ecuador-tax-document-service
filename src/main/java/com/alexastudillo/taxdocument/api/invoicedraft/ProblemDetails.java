package com.alexastudillo.taxdocument.api.invoicedraft;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.net.URI;
import java.util.List;

/** Safe RFC 9457-style problem representation with stable English codes. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetails(
    URI type,
    String title,
    int status,
    String code,
    String detail,
    URI instance,
    String correlationId,
    List<Violation> violations) {
  public record Violation(
      String code, String field, String validationStage, Integer maximum, String countingUnit)
      implements Serializable {
    private static final long serialVersionUID = 1L;
  }

  /** Internal API-boundary exception; it never crosses into Application or Domain. */
  public static final class ApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int status;
    private final String code;
    private final transient List<Violation> violations;

    public ApiException(int status, String code, String message) {
      this(status, code, message, List.of());
    }

    public ApiException(int status, String code, String message, List<Violation> violations) {
      super(message);
      this.status = status;
      this.code = code;
      this.violations = List.copyOf(violations);
    }

    public int status() {
      return status;
    }

    public String code() {
      return code;
    }

    public List<Violation> violations() {
      return violations;
    }
  }
}
