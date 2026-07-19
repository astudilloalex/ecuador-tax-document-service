package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Minimal immutable authoritative fiscal facts and source evidence for one preparation. */
public record FiscalContextSnapshot(
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
    FiscalSourceEvidence sourceEvidence,
    String sriTechnicalRuleIdentifier,
    LocalDate sriTechnicalRuleDate,
    String numericCodePolicyVersion) {
  public static final String SRI_TECHNICAL_RULE_IDENTIFIER = "SRI-OFFLINE-2.33";
  public static final LocalDate SRI_TECHNICAL_RULE_DATE = LocalDate.of(2026, 7, 13);
  public static final String NUMERIC_CODE_POLICY_VERSION = "SECURE_RANDOM_8_V1";
  private static final Pattern RUC = Pattern.compile("^[0-9]{13}$");
  private static final Pattern THREE_DIGITS = Pattern.compile("^[0-9]{3}$");

  public FiscalContextSnapshot {
    FiscalSourceEvidence.requireText(issuerReference, 128, "issuerReference");
    if (issuerRuc == null || !RUC.matcher(issuerRuc).matches()) {
      throw new IllegalArgumentException("Issuer RUC is invalid");
    }
    FiscalSourceEvidence.requireText(legalName, 300, "legalName");
    Objects.requireNonNull(commercialName, "commercialName");
    commercialName.ifPresent(
        value -> FiscalSourceEvidence.requireText(value, 300, "commercialName"));
    FiscalSourceEvidence.requireText(headOfficeAddress, 300, "headOfficeAddress");
    Objects.requireNonNull(specialTaxpayer, "specialTaxpayer");
    Objects.requireNonNull(withholdingAgent, "withholdingAgent");
    Objects.requireNonNull(rimpeClassification, "rimpeClassification");
    Objects.requireNonNull(largeContributor, "largeContributor");
    FiscalSourceEvidence.requireText(establishmentReference, 128, "establishmentReference");
    requireThreeDigits(establishmentCode, "Establishment Code");
    FiscalSourceEvidence.requireText(establishmentAddress, 300, "establishmentAddress");
    requireNonNil(emissionPointId, "emissionPointId");
    requireThreeDigits(emissionPointCode, "Emission Point Code");
    if (!"1".equals(environmentCode) && !"2".equals(environmentCode)) {
      throw new IllegalArgumentException("Environment Code is unsupported");
    }
    if (!AccessKeyGenerator.INVOICE_DOCUMENT_TYPE.equals(documentTypeCode)) {
      throw new IllegalArgumentException("Document Type Code must identify an invoice");
    }
    if (!AccessKeyGenerator.NORMAL_EMISSION_TYPE.equals(emissionTypeCode)) {
      throw new IllegalArgumentException("Emission Type Code is unsupported");
    }
    Objects.requireNonNull(sourceEvidence, "sourceEvidence");
    if (!SRI_TECHNICAL_RULE_IDENTIFIER.equals(sriTechnicalRuleIdentifier)
        || !SRI_TECHNICAL_RULE_DATE.equals(sriTechnicalRuleDate)) {
      throw new IllegalArgumentException("SRI technical rule evidence is invalid");
    }
    if (!NUMERIC_CODE_POLICY_VERSION.equals(numericCodePolicyVersion)) {
      throw new IllegalArgumentException("Numeric Code policy evidence is invalid");
    }
  }

  private static void requireThreeDigits(String value, String field) {
    if (value == null || !THREE_DIGITS.matcher(value).matches()) {
      throw new IllegalArgumentException(field + " is invalid");
    }
  }

  public static void requireNonNil(UUID value, String field) {
    Objects.requireNonNull(value, field);
    if (value.equals(new UUID(0L, 0L))) {
      throw new IllegalArgumentException(field + " must be non-nil");
    }
  }
}
