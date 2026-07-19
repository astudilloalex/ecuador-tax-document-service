package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalDesignation;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalSourceEvidence;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

/**
 * Transport-neutral authoritative response before consumer-side validation and snapshot mapping.
 */
@NullMarked
public record FiscalContextResolution(
    String issuerReference,
    String issuerRuc,
    String legalName,
    Optional<@NonNull String> commercialName,
    String headOfficeAddress,
    boolean accountingRequired,
    Optional<FiscalDesignation.@NonNull SpecialTaxpayer> specialTaxpayer,
    Optional<FiscalDesignation.@NonNull WithholdingAgent> withholdingAgent,
    FiscalDesignation.RimpeClassification rimpeClassification,
    Optional<FiscalDesignation.@NonNull LargeContributor> largeContributor,
    String establishmentReference,
    String establishmentCode,
    String establishmentAddress,
    UUID emissionPointId,
    String emissionPointCode,
    String environmentCode,
    String documentTypeCode,
    String emissionTypeCode,
    boolean invoiceIssuanceEligible,
    FiscalSourceEvidence sourceEvidence) {
  public FiscalContextResolution(
      String issuerReference,
      String issuerRuc,
      String legalName,
      Optional<@NonNull String> commercialName,
      String headOfficeAddress,
      boolean accountingRequired,
      Optional<FiscalDesignation.@NonNull SpecialTaxpayer> specialTaxpayer,
      Optional<FiscalDesignation.@NonNull WithholdingAgent> withholdingAgent,
      FiscalDesignation.RimpeClassification rimpeClassification,
      Optional<FiscalDesignation.@NonNull LargeContributor> largeContributor,
      String establishmentReference,
      String establishmentCode,
      String establishmentAddress,
      UUID emissionPointId,
      String emissionPointCode,
      String environmentCode,
      String documentTypeCode,
      String emissionTypeCode,
      boolean invoiceIssuanceEligible,
      FiscalSourceEvidence sourceEvidence) {
    this.issuerReference = Objects.requireNonNull(issuerReference, "issuerReference");
    this.issuerRuc = Objects.requireNonNull(issuerRuc, "issuerRuc");
    this.legalName = Objects.requireNonNull(legalName, "legalName");
    this.commercialName = Objects.requireNonNull(commercialName, "commercialName");
    this.headOfficeAddress = Objects.requireNonNull(headOfficeAddress, "headOfficeAddress");
    this.accountingRequired = accountingRequired;
    this.specialTaxpayer = Objects.requireNonNull(specialTaxpayer, "specialTaxpayer");
    this.withholdingAgent = Objects.requireNonNull(withholdingAgent, "withholdingAgent");
    this.rimpeClassification = Objects.requireNonNull(rimpeClassification, "rimpeClassification");
    this.largeContributor = Objects.requireNonNull(largeContributor, "largeContributor");
    this.establishmentReference =
        Objects.requireNonNull(establishmentReference, "establishmentReference");
    this.establishmentCode = Objects.requireNonNull(establishmentCode, "establishmentCode");
    this.establishmentAddress =
        Objects.requireNonNull(establishmentAddress, "establishmentAddress");
    this.emissionPointId = Objects.requireNonNull(emissionPointId, "emissionPointId");
    this.emissionPointCode = Objects.requireNonNull(emissionPointCode, "emissionPointCode");
    this.environmentCode = Objects.requireNonNull(environmentCode, "environmentCode");
    this.documentTypeCode = Objects.requireNonNull(documentTypeCode, "documentTypeCode");
    this.emissionTypeCode = Objects.requireNonNull(emissionTypeCode, "emissionTypeCode");
    this.invoiceIssuanceEligible = invoiceIssuanceEligible;
    this.sourceEvidence = Objects.requireNonNull(sourceEvidence, "sourceEvidence");
  }

  public FiscalContextResolution withEmissionPointCode(String value) {
    return new FiscalContextResolution(
        issuerReference,
        issuerRuc,
        legalName,
        commercialName,
        headOfficeAddress,
        accountingRequired,
        specialTaxpayer,
        withholdingAgent,
        rimpeClassification,
        largeContributor,
        establishmentReference,
        establishmentCode,
        establishmentAddress,
        emissionPointId,
        value,
        environmentCode,
        documentTypeCode,
        emissionTypeCode,
        invoiceIssuanceEligible,
        sourceEvidence);
  }
}
