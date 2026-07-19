package com.alexastudillo.taxdocument.domain.invoicedraft;

import java.math.BigDecimal;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/** Grouped IVA total whose key keeps zero treatments distinct. */
@NullMarked
public record TaxTotal(
    String family,
    TaxSelection.Treatment treatment,
    String officialTaxCode,
    String officialPercentageCode,
    BigDecimal rate,
    BigDecimal base,
    BigDecimal amount,
    String catalogVersion) {
  public TaxTotal {
    Objects.requireNonNull(family, "family");
    Objects.requireNonNull(treatment, "treatment");
    Objects.requireNonNull(officialTaxCode, "officialTaxCode");
    Objects.requireNonNull(officialPercentageCode, "officialPercentageCode");
    Objects.requireNonNull(rate, "rate");
    Objects.requireNonNull(base, "base");
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(catalogVersion, "catalogVersion");
    if (!"IVA".equals(family)) {
      throw new DraftValidationException("BUSINESS_VALIDATION_FAILED", "taxTotals", "Tax family");
    }
  }

  public String groupKey() {
    return treatment
        + "|"
        + officialTaxCode
        + "|"
        + officialPercentageCode
        + "|"
        + rate.toPlainString()
        + "|"
        + catalogVersion;
  }
}
