package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.AccessKeyGenerator;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalContextSnapshot;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Complete consumer-side authoritative response validation before transaction start. */
@ApplicationScoped
public final class FiscalContextValidator {
  public FiscalContextSnapshot validate(
      FiscalContextResolution resolution, UUID selectedEmissionPointId, LocalDate emissionDate) {
    Objects.requireNonNull(resolution, "resolution");
    Objects.requireNonNull(selectedEmissionPointId, "selectedEmissionPointId");
    Objects.requireNonNull(emissionDate, "emissionDate");
    if (!resolution.emissionPointId().equals(selectedEmissionPointId)
        || !AccessKeyGenerator.INVOICE_DOCUMENT_TYPE.equals(resolution.documentTypeCode())) {
      throw failure(FiscalPreparationFailure.Code.FISCAL_CONTEXT_INCONSISTENT);
    }
    if (!resolution.invoiceIssuanceEligible()) {
      throw failure(FiscalPreparationFailure.Code.FISCAL_CONTEXT_INVALID);
    }
    if (!resolution.sourceEvidence().effectiveOn(emissionDate)) {
      throw failure(FiscalPreparationFailure.Code.FISCAL_CONTEXT_INVALID);
    }
    if ((!"1".equals(resolution.environmentCode()) && !"2".equals(resolution.environmentCode()))
        || !AccessKeyGenerator.NORMAL_EMISSION_TYPE.equals(resolution.emissionTypeCode())) {
      throw failure(FiscalPreparationFailure.Code.FISCAL_CONTEXT_UNSUPPORTED);
    }
    try {
      return new FiscalContextSnapshot(
          resolution.issuerReference(),
          resolution.issuerRuc(),
          resolution.legalName(),
          resolution.commercialName(),
          resolution.headOfficeAddress(),
          resolution.accountingRequired(),
          resolution.specialTaxpayer(),
          resolution.withholdingAgent(),
          resolution.rimpeClassification(),
          resolution.largeContributor(),
          resolution.establishmentReference(),
          resolution.establishmentCode(),
          resolution.establishmentAddress(),
          resolution.emissionPointId(),
          resolution.emissionPointCode(),
          resolution.environmentCode(),
          resolution.documentTypeCode(),
          resolution.emissionTypeCode(),
          resolution.sourceEvidence(),
          FiscalContextSnapshot.SRI_TECHNICAL_RULE_IDENTIFIER,
          FiscalContextSnapshot.SRI_TECHNICAL_RULE_DATE,
          FiscalContextSnapshot.NUMERIC_CODE_POLICY_VERSION);
    } catch (IllegalArgumentException exception) {
      throw failure(FiscalPreparationFailure.Code.FISCAL_CONTEXT_INVALID);
    }
  }

  private static FiscalPreparationApplicationException failure(FiscalPreparationFailure.Code code) {
    return new FiscalPreparationApplicationException(FiscalPreparationFailure.of(code));
  }
}
