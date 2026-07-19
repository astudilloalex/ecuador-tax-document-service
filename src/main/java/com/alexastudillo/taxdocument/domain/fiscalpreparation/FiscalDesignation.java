package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/** Explicit authoritative conditional fiscal designations and their required paired evidence. */
@NullMarked
public final class FiscalDesignation {
  private static final Pattern WITHHOLDING_RESOLUTION =
      Objects.requireNonNull(Pattern.compile("^(0|[1-9][0-9]{0,7})$"));

  private FiscalDesignation() {}

  public enum RimpeClassification {
    NONE,
    RIMPE_CONTRIBUTOR,
    POPULAR_BUSINESS
  }

  public record SpecialTaxpayer(String resolutionIdentifier) {
    public SpecialTaxpayer {
      FiscalSourceEvidence.requireText(resolutionIdentifier, 64, "specialTaxpayerResolution");
    }
  }

  public record WithholdingAgent(String resolutionIdentifier) {
    public WithholdingAgent {
      if (resolutionIdentifier == null
          || !WITHHOLDING_RESOLUTION.matcher(resolutionIdentifier).matches()) {
        throw new IllegalArgumentException("Withholding Agent resolution is invalid");
      }
    }
  }

  public record LargeContributor(String resolutionIdentifier, String requiredLegend) {
    public LargeContributor {
      FiscalSourceEvidence.requireText(resolutionIdentifier, 64, "largeContributorResolution");
      FiscalSourceEvidence.requireText(requiredLegend, 300, "largeContributorLegend");
    }
  }
}
