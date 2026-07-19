package com.alexastudillo.taxdocument.domain.invoicedraft;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Immutable selected IVA rule and its versioned evidence. */
public record TaxSelection(
    UUID taxRuleId,
    String family,
    Treatment treatment,
    String officialTaxCode,
    String officialPercentageCode,
    BigDecimal rate,
    String catalogVersion,
    boolean active,
    LocalDate effectiveFrom,
    LocalDate effectiveTo) {
  public enum Treatment {
    PERCENTAGE_RATE,
    ZERO_RATE,
    NOT_SUBJECT,
    EXEMPT
  }

  public TaxSelection {
    Objects.requireNonNull(taxRuleId, "taxRuleId");
    Objects.requireNonNull(family, "family");
    Objects.requireNonNull(treatment, "treatment");
    Objects.requireNonNull(officialTaxCode, "officialTaxCode");
    Objects.requireNonNull(officialPercentageCode, "officialPercentageCode");
    Objects.requireNonNull(rate, "rate");
    Objects.requireNonNull(catalogVersion, "catalogVersion");
    Objects.requireNonNull(effectiveFrom, "effectiveFrom");
    if (!"IVA".equals(family)
        || rate.scale() > 2
        || rate.signum() < 0
        || rate.compareTo(new BigDecimal("100.00")) > 0
        || (treatment == Treatment.PERCENTAGE_RATE && rate.signum() == 0)
        || (treatment != Treatment.PERCENTAGE_RATE && rate.signum() != 0)) {
      throw new DraftValidationException(
          "BUSINESS_VALIDATION_FAILED", "lines[].taxRuleId", "IVA rule is invalid");
    }
  }

  public void requireEffectiveOn(LocalDate emissionDate) {
    if (!active
        || emissionDate.isBefore(effectiveFrom)
        || (effectiveTo != null && emissionDate.isAfter(effectiveTo))) {
      throw new DraftValidationException(
          "BUSINESS_VALIDATION_FAILED", "lines[].taxRuleId", "IVA rule is not effective");
    }
  }
}
