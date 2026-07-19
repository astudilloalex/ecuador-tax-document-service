package com.alexastudillo.taxdocument.api.fiscalpreparation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Exact sensitive success representation; Company context is intentionally absent. */
public record FiscalPreparationResponse(
    UUID fiscalPreparationId,
    UUID invoiceDraftId,
    LocalDate emissionDate,
    FiscalContextSnapshotResponse fiscalContextSnapshot,
    String officialSequentialNumber,
    String numericCode,
    String accessKey,
    Instant createdAt) {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record FiscalContextSnapshotResponse(
      String issuerReference,
      String issuerRuc,
      String legalName,
      @Nullable String commercialName,
      String headOfficeAddress,
      boolean accountingRequired,
      @Nullable ResolutionDesignationResponse specialTaxpayer,
      @Nullable ResolutionDesignationResponse withholdingAgent,
      String rimpeClassification,
      @Nullable LargeContributorDesignationResponse largeContributor,
      String establishmentReference,
      String establishmentCode,
      String establishmentAddress,
      UUID emissionPointId,
      String emissionPointCode,
      String environmentCode,
      String documentTypeCode,
      String emissionTypeCode,
      String technicalRuleId,
      LocalDate technicalRuleModifiedOn,
      String numericCodePolicyId,
      String sourceAuthority,
      String sourceRevision,
      LocalDate sourceEffectiveFrom,
      @Nullable LocalDate sourceEffectiveThrough,
      Instant sourceObservedAt) {}

  public record ResolutionDesignationResponse(String resolutionIdentifier) {}

  public record LargeContributorDesignationResponse(
      String resolutionIdentifier, String requiredLegend) {}
}
