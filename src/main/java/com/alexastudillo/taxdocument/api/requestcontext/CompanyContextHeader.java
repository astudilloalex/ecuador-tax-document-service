package com.alexastudillo.taxdocument.api.requestcontext;

import com.alexastudillo.taxdocument.api.problem.ProblemDetails;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/** Exactly-one authoritative X-Company-Id transport parser shared by Company-owned operations. */
@ApplicationScoped
public final class CompanyContextHeader {
  private static final Pattern UUID_TEXT =
      Pattern.compile(
          "^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$");

  public CompanyId parse(@Nullable List<String> values) {
    if (values == null || values.isEmpty()) {
      throw new ProblemDetails.ApiException(
          400, "COMPANY_CONTEXT_REQUIRED", "X-Company-Id is required");
    }
    if (values.size() != 1) {
      throw invalid();
    }
    String normalized = trimAsciiSpaceAndTab(values.getFirst());
    if (!UUID_TEXT.matcher(normalized).matches()) {
      throw invalid();
    }
    try {
      return new CompanyId(UUID.fromString(normalized));
    } catch (RuntimeException exception) {
      throw invalid();
    }
  }

  private static String trimAsciiSpaceAndTab(String value) {
    int start = 0;
    int end = value.length();
    while (start < end && isTransportWhitespace(value.charAt(start))) {
      start++;
    }
    while (end > start && isTransportWhitespace(value.charAt(end - 1))) {
      end--;
    }
    return value.substring(start, end);
  }

  private static boolean isTransportWhitespace(char value) {
    return value == ' ' || value == '\t';
  }

  private static ProblemDetails.ApiException invalid() {
    return new ProblemDetails.ApiException(
        400, "COMPANY_CONTEXT_INVALID", "X-Company-Id must contain exactly one non-nil UUID");
  }
}
