package com.alexastudillo.taxdocument.api.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalContextSnapshot;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalDesignation;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalPreparation;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Copies a committed immutable domain result into the exact API success representation. */
@ApplicationScoped
@NullMarked
public final class FiscalPreparationApiMapper {
  public FiscalPreparationResponse toResponse(FiscalPreparation preparation) {
    FiscalContextSnapshot snapshot = preparation.fiscalContextSnapshot();
    @Nullable String commercialName = optionalValue(snapshot.commercialName());
    FiscalDesignation.@Nullable SpecialTaxpayer specialTaxpayer =
        optionalValue(snapshot.specialTaxpayer());
    FiscalPreparationResponse.@Nullable ResolutionDesignationResponse specialTaxpayerResponse =
        specialTaxpayer != null ? designation(specialTaxpayer) : null;
    FiscalDesignation.@Nullable WithholdingAgent withholdingAgent =
        optionalValue(snapshot.withholdingAgent());
    FiscalPreparationResponse.@Nullable ResolutionDesignationResponse withholdingAgentResponse =
        withholdingAgent != null ? designation(withholdingAgent) : null;
    FiscalDesignation.@Nullable LargeContributor largeContributor =
        optionalValue(snapshot.largeContributor());
    FiscalPreparationResponse.@Nullable LargeContributorDesignationResponse
        largeContributorResponse =
            largeContributor != null ? largeContributor(largeContributor) : null;
    @Nullable LocalDate effectiveThrough =
        optionalValue(snapshot.sourceEvidence().effectiveThrough());

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
            Objects.requireNonNull(snapshot.rimpeClassification().name()),
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

  private static <T extends @NonNull Object> @Nullable T optionalValue(
      Optional<@NonNull T> optional) {
    if (optional.isEmpty()) {
      return null;
    }
    @Nullable T value = optional.get();
    return value;
  }
}
