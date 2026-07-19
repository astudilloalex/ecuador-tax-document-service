package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/** Immutable complete 49-digit SRI v2.33 Access Key. */
@NullMarked
public record AccessKey(String value) {
  private static final Pattern REPRESENTATION =
      Objects.requireNonNull(Pattern.compile("^[0-9]{49}$"));

  public AccessKey {
    if (value == null || !REPRESENTATION.matcher(value).matches()) {
      throw new IllegalArgumentException("Access Key must contain exactly 49 ASCII digits");
    }
    int expected = AccessKeyGenerator.verificationDigit(value.substring(0, 48));
    int supplied = value.charAt(48) - '0';
    if (supplied != expected) {
      throw new IllegalArgumentException("Access Key Verification Digit is invalid");
    }
  }

  public static AccessKey parse(String value) {
    return new AccessKey(value);
  }

  public int verificationDigit() {
    return value.charAt(48) - '0';
  }

  @Override
  public String toString() {
    return value;
  }
}
