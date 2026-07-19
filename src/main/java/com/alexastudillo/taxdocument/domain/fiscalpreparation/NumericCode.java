package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import java.util.Locale;
import java.util.regex.Pattern;

/** Immutable system-generated eight-digit SRI Access Key component. */
public record NumericCode(String value) {
  private static final Pattern REPRESENTATION = Pattern.compile("^[0-9]{8}$");

  public NumericCode {
    if (value == null || !REPRESENTATION.matcher(value).matches()) {
      throw new IllegalArgumentException("Numeric Code must contain exactly eight ASCII digits");
    }
  }

  public static NumericCode of(int number) {
    if (number < 0 || number > 99_999_999) {
      throw new IllegalArgumentException("Numeric Code is outside its valid range");
    }
    return new NumericCode(String.format(Locale.ROOT, "%08d", number));
  }

  public static NumericCode parse(String value) {
    return new NumericCode(value);
  }

  public int number() {
    return Integer.parseInt(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
