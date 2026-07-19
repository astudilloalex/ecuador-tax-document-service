package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Pure SRI Offline Technical Sheet v2.33 Access Key construction and component validation. */
@NullMarked
public final class AccessKeyGenerator {
  public static final String INVOICE_DOCUMENT_TYPE = "01";
  public static final String NORMAL_EMISSION_TYPE = "1";
  private static final DateTimeFormatter DATE =
      Objects.requireNonNull(DateTimeFormatter.ofPattern("ddMMyyyy", Locale.ROOT));
  private static final Pattern RUC = Objects.requireNonNull(Pattern.compile("^[0-9]{13}$"));
  private static final Pattern THREE_DIGITS = Objects.requireNonNull(Pattern.compile("^[0-9]{3}$"));
  private static final Pattern DIGITS = Objects.requireNonNull(Pattern.compile("^[0-9]+$"));

  public AccessKey generate(
      LocalDate emissionDate,
      String issuerRuc,
      String environmentCode,
      String establishmentCode,
      String emissionPointCode,
      OfficialSequentialNumber sequentialNumber,
      NumericCode numericCode) {
    Objects.requireNonNull(emissionDate, "emissionDate");
    requireMatch(issuerRuc, RUC, "Issuer RUC");
    if (!environmentCode.equals("1") && !environmentCode.equals("2")) {
      throw new IllegalArgumentException("Environment Code must be 1 or 2");
    }
    requireMatch(establishmentCode, THREE_DIGITS, "Establishment Code");
    requireMatch(emissionPointCode, THREE_DIGITS, "Emission Point Code");
    Objects.requireNonNull(sequentialNumber, "sequentialNumber");
    Objects.requireNonNull(numericCode, "numericCode");
    String base =
        DATE.format(emissionDate)
            + INVOICE_DOCUMENT_TYPE
            + issuerRuc
            + environmentCode
            + establishmentCode
            + emissionPointCode
            + sequentialNumber.value()
            + numericCode.value()
            + NORMAL_EMISSION_TYPE;
    if (base.length() != 48) {
      throw new IllegalArgumentException("Access Key base must contain exactly 48 digits");
    }
    return AccessKey.parse(base + verificationDigit(base));
  }

  public void validateMatches(
      AccessKey accessKey,
      LocalDate emissionDate,
      String issuerRuc,
      String environmentCode,
      String establishmentCode,
      String emissionPointCode,
      OfficialSequentialNumber sequentialNumber,
      NumericCode numericCode) {
    Objects.requireNonNull(accessKey, "accessKey");
    AccessKey expected =
        generate(
            emissionDate,
            issuerRuc,
            environmentCode,
            establishmentCode,
            emissionPointCode,
            sequentialNumber,
            numericCode);
    if (!expected.equals(accessKey)) {
      throw new IllegalArgumentException("Access Key components do not match the fiscal context");
    }
  }

  public static int verificationDigit(@Nullable String digits) {
    if (digits == null || digits.isEmpty() || !DIGITS.matcher(digits).matches()) {
      throw new IllegalArgumentException("Modulo 11 input must contain only ASCII digits");
    }
    int sum = 0;
    int weight = 2;
    for (int index = digits.length() - 1; index >= 0; index--) {
      sum += (digits.charAt(index) - '0') * weight;
      weight = weight == 7 ? 2 : weight + 1;
    }
    int raw = 11 - (sum % 11);
    return switch (raw) {
      case 11 -> 0;
      case 10 -> 1;
      default -> raw;
    };
  }

  private static void requireMatch(@Nullable String value, Pattern pattern, String field) {
    if (value == null || !pattern.matcher(value).matches()) {
      throw new IllegalArgumentException(field + " is invalid");
    }
  }
}
