package com.alexastudillo.taxdocument.api.requestcontext;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Always-safe correlation classifier shared by request boundaries. */
@ApplicationScoped
@NullMarked
public final class CorrelationHeader {
  private static final Pattern VALID =
      Objects.requireNonNull(Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{0,63}$"));

  public Classification classify(@Nullable List<String> values) {
    if (values == null || values.isEmpty()) {
      return new Classification(Objects.requireNonNull(UUID.randomUUID().toString()), true);
    }
    if (values.size() == 1) {
      String normalized =
          trimAsciiSpaceAndTab(Objects.requireNonNull(values.getFirst(), "correlation value"));
      if (VALID.matcher(normalized).matches()) {
        return new Classification(normalized, true);
      }
    }
    return new Classification(Objects.requireNonNull(UUID.randomUUID().toString()), false);
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
    return Objects.requireNonNull(value.substring(start, end));
  }

  private static boolean isTransportWhitespace(char value) {
    return value == ' ' || value == '\t';
  }

  public record Classification(String safeValue, boolean validInput) {
    public Classification {
      Objects.requireNonNull(safeValue, "safeValue");
    }
  }
}
