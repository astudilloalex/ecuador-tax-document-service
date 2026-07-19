package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Bounded JSON representations for authoritative-fiscal-context contract 1.0.0. */
public final class AuthoritativeFiscalContextDto {
  private AuthoritativeFiscalContextDto() {}

  public record Selection(UUID emissionPointId, LocalDate emissionDate, String documentTypeCode) {}

  @JsonIgnoreProperties(ignoreUnknown = false)
  public record Context(
      @Nullable String issuerReference,
      @Nullable String issuerRuc,
      @Nullable String legalName,
      @Nullable String commercialName,
      @Nullable String headOfficeAddress,
      @Nullable Boolean accountingRequired,
      @Nullable ResolutionDesignation specialTaxpayer,
      @Nullable ResolutionDesignation withholdingAgent,
      @Nullable String rimpeClassification,
      @Nullable LargeContributorDesignation largeContributor,
      @Nullable String establishmentReference,
      @Nullable String establishmentCode,
      @Nullable String establishmentAddress,
      @Nullable UUID emissionPointId,
      @Nullable String emissionPointCode,
      @Nullable String environmentCode,
      @Nullable String documentTypeCode,
      @Nullable String emissionTypeCode,
      @Nullable Boolean invoiceIssuanceEligible,
      @Nullable SourceEvidence sourceEvidence) {}

  @JsonIgnoreProperties(ignoreUnknown = false)
  public record ResolutionDesignation(@Nullable String resolutionIdentifier) {}

  @JsonIgnoreProperties(ignoreUnknown = false)
  public record LargeContributorDesignation(
      @Nullable String resolutionIdentifier, @Nullable String requiredLegend) {}

  @JsonIgnoreProperties(ignoreUnknown = false)
  public record SourceEvidence(
      @Nullable String authority,
      @Nullable String revision,
      @Nullable LocalDate effectiveFrom,
      @Nullable LocalDate effectiveThrough,
      @Nullable Instant observedAt) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ProviderProblem(@Nullable String code) {}
}
