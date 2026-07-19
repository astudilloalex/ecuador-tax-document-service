package com.alexastudillo.taxdocument.api.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalContextSnapshot;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalDesignation;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalPreparation;
import jakarta.enterprise.context.ApplicationScoped;

/** Copies a committed immutable domain result into the exact API success representation. */
@ApplicationScoped
public final class FiscalPreparationApiMapper {
  public FiscalPreparationResponse toResponse(FiscalPreparation preparation) {
    FiscalContextSnapshot snapshot = preparation.fiscalContextSnapshot();
    return new FiscalPreparationResponse(
        preparation.id(),
        preparation.invoiceDraftId(),
        preparation.emissionDate(),
        new FiscalPreparationResponse.FiscalContextSnapshotResponse(
            snapshot.issuerReference(),
            snapshot.issuerRuc(),
            snapshot.legalName(),
            snapshot.commercialName().orElse(null),
            snapshot.headOfficeAddress(),
            snapshot.accountingRequired(),
            snapshot.specialTaxpayer().map(FiscalPreparationApiMapper::designation).orElse(null),
            snapshot.withholdingAgent().map(FiscalPreparationApiMapper::designation).orElse(null),
            snapshot.rimpeClassification().name(),
            snapshot
                .largeContributor()
                .map(FiscalPreparationApiMapper::largeContributor)
                .orElse(null),
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
            snapshot.sourceEvidence().effectiveThrough().orElse(null),
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
