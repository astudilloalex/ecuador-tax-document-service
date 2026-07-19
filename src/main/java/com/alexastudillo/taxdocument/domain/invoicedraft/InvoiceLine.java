package com.alexastudillo.taxdocument.domain.invoicedraft;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/** Ordered commercial line with one selected IVA rule and optional calculated values. */
public record InvoiceLine(
    UUID id,
    int position,
    String productCode,
    String description,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal discount,
    TaxSelection taxSelection,
    @Nullable BigDecimal grossAmount,
    @Nullable BigDecimal netAmount,
    @Nullable BigDecimal taxBase,
    @Nullable BigDecimal taxAmount,
    @Nullable BigDecimal lineTotal) {
  private static final Pattern PRODUCT_CODE = Pattern.compile("^[A-Za-z0-9]{1,25}$");
  private static final BigDecimal MAX_QUANTITY = new BigDecimal("999999.999999");

  public InvoiceLine {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(productCode, "productCode");
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(quantity, "quantity");
    Objects.requireNonNull(unitPrice, "unitPrice");
    Objects.requireNonNull(discount, "discount");
    Objects.requireNonNull(taxSelection, "taxSelection");
    if (position < 1
        || position > 500
        || !PRODUCT_CODE.matcher(productCode).matches()
        || description.isEmpty()
        || description.codePointCount(0, description.length()) > 300) {
      throw invalid("lines[" + Math.max(0, position - 1) + "]", "Invoice line text is invalid");
    }
    requireSixScale(quantity, true, "quantity");
    requireSixScale(unitPrice, false, "unitPrice");
    if (discount.scale() > 2
        || discount.signum() < 0
        || discount.compareTo(InvoiceDraftCalculator.MAX_MONEY) > 0) {
      throw range("discount");
    }
  }

  public static boolean productCodeIsValid(String value) {
    return value != null && PRODUCT_CODE.matcher(value).matches();
  }

  public InvoiceLine calculated(
      BigDecimal gross, BigDecimal net, BigDecimal base, BigDecimal tax, BigDecimal total) {
    return new InvoiceLine(
        id,
        position,
        productCode,
        description,
        quantity,
        unitPrice,
        discount,
        taxSelection,
        gross,
        net,
        base,
        tax,
        total);
  }

  private static void requireSixScale(BigDecimal value, boolean positive, String field) {
    if (value.scale() > 6
        || value.compareTo(MAX_QUANTITY) > 0
        || (positive ? value.signum() <= 0 : value.signum() < 0)) {
      throw range(field);
    }
  }

  private static DraftValidationException range(String field) {
    return new DraftValidationException(
        "MONETARY_RANGE_EXCEEDED", "lines[]." + field, "Numeric input is outside its range");
  }

  private static DraftValidationException invalid(String field, String message) {
    return new DraftValidationException("BUSINESS_VALIDATION_FAILED", field, message);
  }
}
