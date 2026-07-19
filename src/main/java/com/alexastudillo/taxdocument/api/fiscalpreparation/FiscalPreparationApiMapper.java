package com.alexastudillo.taxdocument.api.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalContextSnapshot;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalDesignation;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalPreparation;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;

/**
 * Copies a committed immutable domain result into the exact API success
 * representation.
 */
@ApplicationScoped
public final class FiscalPreparationApiMapper {
  public FiscalPreparationResponse toResponse(FiscalPreparation preparation) {
    FiscalContextSnapshot snapshot = preparation.fiscalContextSnapshot();
    String commercialName = snapshot.commercialName().orElse(null);
    FiscalDesignation.SpecialTaxpayer specialTaxpayer = snapshot.specialTaxpayer().orElse(null);
    FiscalPreparationResponse.ResolutionDesignationResponse specialTaxpayerResponse =
        specialTaxpayer != null ? designation(specialTaxpayer) : null;
    FiscalDesignation.WithholdingAgent withholdingAgent = snapshot.withholdingAgent().orElse(null);
    FiscalPreparationResponse.ResolutionDesignationResponse withholdingAgentResponse =
        withholdingAgent != null ? designation(withholdingAgent) : null;
    FiscalDesignation.LargeContributor largeContributor = snapshot.largeContributor().orElse(null);
    FiscalPreparationResponse.LargeContributorDesignationResponse largeContributorResponse =
        largeContributor != null ? largeContributor(largeContributor) : null;
    LocalDate effectiveThrough = snapshot.sourceEvidence().effectiveThrough().orElse(null);

    return new FiscalPreparationResponse(
        preparation.id(),
        preparation.invoiceDraftId(),
        preparation.emissionDate(),
        new FiscalPreparationResponse.FiscalContextSnapshotResponse(
            snapshot.issuerReference(),
            snapshot.issuerRuc(),
            snapshot.legalName(),
            commercialName,
            snapshot.headOfficeAddress(),
            snapshot.accountingRequired(),
            specialTaxpayerResponse,
            withholdingAgentResponse,
            snapshot.rimpeClassification().name(),
            largeContributorResponse,
            snapshot.establishmentReference(),
            snapshot.establishmentCode(),
            snapshot.establishmentAddress(),
            snapshot.emissionPointId(),
            snapshot.emissionPointCode(),
            snapshot.environmentCode(),
            snapshot.documentTypeCode(),
            snapshot.emissionTypeCode(),
            snapshot.sriTechnicalRuleIdentifier(),
            snapshot.sriTechnicalRuleDate(),
            snapshot.numericCodePolicyVersion(),
            snapshot.sourceEvidence().authority(),
            snapshot.sourceEvidence().revision(),
            snapshot.sourceEvidence().effectiveFrom(),
            effectiveThrough,
            snapshot.sourceEvidence().observedAt()),
        preparation.officialSequentialNumber().value(),
        preparation.numericCode().value(),
        preparation.accessKey().value(),
        preparation.createdAt());
  }

  private static FiscalPreparationResponse.ResolutionDesignationResponse designation(
      FiscalDesignation.SpecialTaxpayer value) {
    return new FiscalPreparationResponse.ResolutionDesignationResponse(
        value.resolutionIdentifier());
  }

  private static FiscalPreparationResponse.ResolutionDesignationResponse designation(
      FiscalDesignation.WithholdingAgent value) {
    return new FiscalPreparationResponse.ResolutionDesignationResponse(
        value.resolutionIdentifier());
  }

  private static FiscalPreparationResponse.LargeContributorDesignationResponse largeContributor(
      FiscalDesignation.LargeContributor value) {
    return new FiscalPreparationResponse.LargeContributorDesignationResponse(
        value.resolutionIdentifier(), value.requiredLegend());
  }
}
