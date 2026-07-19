package com.alexastudillo.taxdocument.domain.invoicedraft;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;

/** Validated payment reference and amount. */
@NullMarked
public record Payment(
    UUID id,
    UUID paymentMethodId,
    String officialCode,
    String name,
    BigDecimal amount,
    String catalogVersion) {
  private static final BigDecimal MAX_MONEY = new BigDecimal("999999999999999.99");

  public Payment {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(paymentMethodId, "paymentMethodId");
    Objects.requireNonNull(officialCode, "officialCode");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(catalogVersion, "catalogVersion");
    if (amount.scale() > 2 || amount.signum() < 0 || amount.compareTo(MAX_MONEY) > 0) {
      throw new DraftValidationException(
          "MONETARY_RANGE_EXCEEDED", "payments[].amount", "Payment amount is invalid");
    }
  }
}
