package com.alexastudillo.taxdocument.api.invoicedraft;

import com.alexastudillo.taxdocument.application.invoicedraft.BusinessTextNormalizer;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/** Always-safe correlation classifier. */
@ApplicationScoped
public final class CorrelationHeader {
  private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{0,63}$");

  public Classification classify(List<String> values) {
    if (values == null || values.isEmpty()) {
      return new Classification(UUID.randomUUID().toString(), true);
    }
    if (values.size() == 1) {
      String normalized = BusinessTextNormalizer.trimAsciiSpaceAndTab(values.getFirst());
      if (VALID.matcher(normalized).matches()) {
        return new Classification(normalized, true);
      }
    }
    return new Classification(UUID.randomUUID().toString(), false);
  }

  public record Classification(String safeValue, boolean validInput) {}
}
