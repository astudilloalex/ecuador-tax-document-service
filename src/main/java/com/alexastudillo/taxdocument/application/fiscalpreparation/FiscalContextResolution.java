package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalDesignation;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalSourceEvidence;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Transport-neutral authoritative response before consumer-side validation and snapshot mapping.
 */
public record FiscalContextResolution(
    String issuerReference,
    String issuerRuc,
    String legalName,
    Optional<String> commercialName,
    String headOfficeAddress,
    boolean accountingRequired,
    Optional<FiscalDesignation.SpecialTaxpayer> specialTaxpayer,
    Optional<FiscalDesignation.WithholdingAgent> withholdingAgent,
    FiscalDesignation.RimpeClassification rimpeClassification,
    Optional<FiscalDesignation.LargeContributor> largeContributor,
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
  public FiscalContextResolution {
    Objects.requireNonNull(issuerReference, "issuerReference");
    Objects.requireNonNull(issuerRuc, "issuerRuc");
    Objects.requireNonNull(legalName, "legalName");
    Objects.requireNonNull(commercialName, "commercialName");
    Objects.requireNonNull(headOfficeAddress, "headOfficeAddress");
    Objects.requireNonNull(specialTaxpayer, "specialTaxpayer");
    Objects.requireNonNull(withholdingAgent, "withholdingAgent");
    Objects.requireNonNull(rimpeClassification, "rimpeClassification");
    Objects.requireNonNull(largeContributor, "largeContributor");
    Objects.requireNonNull(establishmentReference, "establishmentReference");
    Objects.requireNonNull(establishmentCode, "establishmentCode");
    Objects.requireNonNull(establishmentAddress, "establishmentAddress");
    Objects.requireNonNull(emissionPointId, "emissionPointId");
    Objects.requireNonNull(emissionPointCode, "emissionPointCode");
    Objects.requireNonNull(environmentCode, "environmentCode");
    Objects.requireNonNull(documentTypeCode, "documentTypeCode");
    Objects.requireNonNull(emissionTypeCode, "emissionTypeCode");
    Objects.requireNonNull(sourceEvidence, "sourceEvidence");
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
