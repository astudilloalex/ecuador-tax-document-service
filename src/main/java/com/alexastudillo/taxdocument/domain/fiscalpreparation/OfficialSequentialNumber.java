package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import java.util.Locale;
import java.util.regex.Pattern;

/** Official immutable nine-digit invoice sequential in the range 000000001..999999999. */
public record OfficialSequentialNumber(String value) {
  private static final Pattern REPRESENTATION = Pattern.compile("^[0-9]{9}$");

  public OfficialSequentialNumber {
    if (value == null || !REPRESENTATION.matcher(value).matches() || value.equals("000000000")) {
      throw new IllegalArgumentException(
          "Official Sequential Number must contain nine ASCII digits and be positive");
    }
  }

  public static OfficialSequentialNumber of(int number) {
    if (number < 1 || number > 999_999_999) {
      throw new IllegalArgumentException("Official Sequential Number is outside its valid range");
    }
    return new OfficialSequentialNumber(String.format(Locale.ROOT, "%09d", number));
  }

  public static OfficialSequentialNumber parse(String value) {
    return new OfficialSequentialNumber(value);
  }

  public int number() {
    return Integer.parseInt(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
