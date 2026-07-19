package com.alexastudillo.taxdocument.application.invoicedraft;

import com.alexastudillo.taxdocument.domain.invoicedraft.DraftValidationException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** The sole business-text normalization policy owner. */
@NullMarked
public final class BusinessTextNormalizer {
  public NormalizedText normalizeDisplay(String field, String raw, int maximumCodePoints) {
    Objects.requireNonNull(field, "field");
    Objects.requireNonNull(raw, "raw");
    String value = trimAsciiSpace(Normalizer.normalize(raw, Normalizer.Form.NFC));
    validateCodePoints(field, value);
    int length = value.codePointCount(0, value.length());
    if (length < 1 || length > maximumCodePoints) {
      throw new DraftValidationException(
          "BUSINESS_VALIDATION_FAILED", field, "Normalized text length is invalid");
    }
    return new NormalizedText(value, null);
  }

  public NormalizedText normalizeWithCanonicalName(String field, String raw) {
    NormalizedText display = normalizeDisplay(field, raw, 300);
    String canonical = collapseAsciiSpaces(display.displayValue()).toLowerCase(Locale.ROOT);
    int length = canonical.codePointCount(0, canonical.length());
    if (length > 300) {
      throw new DraftValidationException(
          "CANONICAL_NAME_TOO_LONG", field, "Canonical name exceeds 300 Unicode code points");
    }
    return new NormalizedText(display.displayValue(), canonical);
  }

  public static String trimAsciiSpaceAndTab(String value) {
    Objects.requireNonNull(value, "value");
    int start = 0;
    int end = value.length();
    while (start < end && (value.charAt(start) == ' ' || value.charAt(start) == '\t')) {
      start++;
    }
    while (end > start && (value.charAt(end - 1) == ' ' || value.charAt(end - 1) == '\t')) {
      end--;
    }
    return value.substring(start, end);
  }

  private static String trimAsciiSpace(String value) {
    int start = 0;
    int end = value.length();
    while (start < end && value.charAt(start) == ' ') {
      start++;
    }
    while (end > start && value.charAt(end - 1) == ' ') {
      end--;
    }
    return value.substring(start, end);
  }

  private static void validateCodePoints(String field, String value) {
    for (int offset = 0; offset < value.length(); ) {
      int codePoint = value.codePointAt(offset);
      int type = Character.getType(codePoint);
      boolean prohibitedCategory =
          type == Character.CONTROL
              || type == Character.FORMAT
              || type == Character.SURROGATE
              || type == Character.PRIVATE_USE
              || type == Character.UNASSIGNED;
      boolean prohibitedSeparator =
          codePoint == 0x2028
              || codePoint == 0x2029
              || (Character.isWhitespace(codePoint) && codePoint != 0x20)
              || (Character.isSpaceChar(codePoint) && codePoint != 0x20);
      if (prohibitedCategory || prohibitedSeparator) {
        throw new DraftValidationException(
            "BUSINESS_VALIDATION_FAILED", field, "Business text contains a prohibited code point");
      }
      offset += Character.charCount(codePoint);
    }
  }

  private static String collapseAsciiSpaces(String value) {
    StringBuilder result = new StringBuilder(value.length());
    boolean previousSpace = false;
    for (int offset = 0; offset < value.length(); ) {
      int codePoint = value.codePointAt(offset);
      if (codePoint != 0x20 || !previousSpace) {
        result.appendCodePoint(codePoint);
      }
      previousSpace = codePoint == 0x20;
      offset += Character.charCount(codePoint);
    }
    return result.toString();
  }

  public record NormalizedText(String displayValue, @Nullable String canonicalValue) {}
}
