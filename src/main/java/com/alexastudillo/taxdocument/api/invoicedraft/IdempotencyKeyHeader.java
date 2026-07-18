package com.alexastudillo.taxdocument.api.invoicedraft;

import com.alexastudillo.taxdocument.application.invoicedraft.BusinessTextNormalizer;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.regex.Pattern;

/** Mandatory exactly-one Idempotency-Key parser. */
@ApplicationScoped
public final class IdempotencyKeyHeader {
  private static final Pattern VALID =
      Pattern.compile(
          "^[\\x21-\\x2B\\x2D-\\x7E](?:[\\x20-\\x2B\\x2D-\\x7E]{0,126}[\\x21-\\x2B\\x2D-\\x7E])?$");

  public String parse(List<String> values) {
    if (values == null || values.isEmpty()) {
      throw new ProblemDetails.ApiException(
          400, "IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key is required");
    }
    if (values.size() != 1 || values.stream().anyMatch(value -> value.indexOf(',') >= 0)) {
      throw new ProblemDetails.ApiException(
          400, "IDEMPOTENCY_KEY_MULTIPLE", "Idempotency-Key must contain exactly one value");
    }
    String normalized = BusinessTextNormalizer.trimAsciiSpaceAndTab(values.getFirst());
    if (!VALID.matcher(normalized).matches()) {
      throw new ProblemDetails.ApiException(
          400, "IDEMPOTENCY_KEY_INVALID", "Idempotency-Key is invalid");
    }
    return normalized;
  }
}
