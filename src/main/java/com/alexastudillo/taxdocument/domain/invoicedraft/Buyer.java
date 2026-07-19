package com.alexastudillo.taxdocument.domain.invoicedraft;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Buyer data captured by an Invoice Draft after Application normalization. */
@NullMarked
public record Buyer(
    String identificationType,
    String identification,
    String legalName,
    @Nullable String address,
    @Nullable String email,
    @Nullable String telephone,
    String catalogVersion) {
  private static final Pattern NUMERIC_13 = Pattern.compile("^[0-9]{13}$");
  private static final Pattern NUMERIC_10 = Pattern.compile("^[0-9]{10}$");
  private static final Pattern ASCII_1_20 = Pattern.compile("^[A-Za-z0-9]{1,20}$");
  private static final String EMAIL_ATOM = "A-Za-z0-9!#$%&'*+/=?^_\\u0060{|}~-";
  private static final Pattern EMAIL =
      Pattern.compile(
          "^(?=.{1,254}$)(?=[^@]{1,64}@)["
              + EMAIL_ATOM
              + "]+(?:\\.["
              + EMAIL_ATOM
              + "]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
              + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");
  private static final Pattern TELEPHONE =
      Pattern.compile("^(?=(?:\\D*\\d){7,15}\\D*$)[0-9+() -]{7,20}$");
  private static final BigDecimal FINAL_CONSUMER_LIMIT = new BigDecimal("50.00");

  public Buyer {
    Objects.requireNonNull(identificationType, "identificationType");
    Objects.requireNonNull(identification, "identification");
    Objects.requireNonNull(legalName, "legalName");
    Objects.requireNonNull(catalogVersion, "catalogVersion");
    if (legalName.isEmpty() || legalName.codePointCount(0, legalName.length()) > 300) {
      throw invalid("buyer.legalName", "Buyer legal name is outside the approved length");
    }
    if (!identificationIsValid(identificationType, identification)) {
      throw invalid("buyer.identification", "Buyer identification is invalid");
    }
    if (email != null && !emailIsValid(email)) {
      throw new DraftValidationException("EMAIL_INVALID", "buyer.email", "Buyer email is invalid");
    }
    if (telephone != null && !TELEPHONE.matcher(telephone).matches()) {
      throw invalid("buyer.telephone", "Buyer telephone is invalid");
    }
    validateOptionalText(address, "buyer.address");
  }

  public boolean finalConsumer() {
    return "07".equals(identificationType);
  }

  public void validateCalculatedTotal(BigDecimal grandTotal) {
    Objects.requireNonNull(grandTotal, "grandTotal");
    if (finalConsumer()
        && (!"9999999999999".equals(identification)
            || !"CONSUMIDOR FINAL".equals(legalName)
            || grandTotal.compareTo(FINAL_CONSUMER_LIMIT) > 0)) {
      throw invalid("buyer", "Final-consumer requirements are not satisfied");
    }
  }

  public static boolean identificationIsValid(String type, String value) {
    if (type == null || value == null) {
      return false;
    }
    return switch (type) {
      case "04" -> NUMERIC_13.matcher(value).matches();
      case "05" -> NUMERIC_10.matcher(value).matches();
      case "06", "08" -> ASCII_1_20.matcher(value).matches();
      case "07" -> "9999999999999".equals(value);
      default -> false;
    };
  }

  public static boolean emailIsValid(String value) {
    return value != null && EMAIL.matcher(value).matches();
  }

  private static void validateOptionalText(@Nullable String value, String field) {
    if (value != null && (value.isEmpty() || value.codePointCount(0, value.length()) > 300)) {
      throw invalid(field, "Optional buyer text is outside the approved length");
    }
  }

  private static DraftValidationException invalid(String field, String message) {
    return new DraftValidationException("BUSINESS_VALIDATION_FAILED", field, message);
  }
}
