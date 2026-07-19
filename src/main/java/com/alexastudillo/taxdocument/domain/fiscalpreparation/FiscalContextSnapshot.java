package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Minimal immutable authoritative fiscal facts and source evidence for one preparation. */
@NullMarked
public record FiscalContextSnapshot(
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
    FiscalSourceEvidence sourceEvidence,
    String sriTechnicalRuleIdentifier,
    LocalDate sriTechnicalRuleDate,
    String numericCodePolicyVersion) {
  public static final String SRI_TECHNICAL_RULE_IDENTIFIER = "SRI-OFFLINE-2.33";
  public static final LocalDate SRI_TECHNICAL_RULE_DATE =
      Objects.requireNonNull(LocalDate.of(2026, 7, 13));
  public static final String NUMERIC_CODE_POLICY_VERSION = "SECURE_RANDOM_8_V1";
  private static final Pattern RUC = Objects.requireNonNull(Pattern.compile("^[0-9]{13}$"));
  private static final Pattern THREE_DIGITS = Objects.requireNonNull(Pattern.compile("^[0-9]{3}$"));

  public FiscalContextSnapshot(
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
      FiscalSourceEvidence sourceEvidence,
      String sriTechnicalRuleIdentifier,
      LocalDate sriTechnicalRuleDate,
      String numericCodePolicyVersion) {
    FiscalSourceEvidence.requireText(issuerReference, 128, "issuerReference");
    requireRuc(issuerRuc);
    FiscalSourceEvidence.requireText(legalName, 300, "legalName");
    Objects.requireNonNull(commercialName, "commercialName");
    commercialName.ifPresent(
        value ->
            FiscalSourceEvidence.requireText(
                Objects.requireNonNull(value, "commercialName"), 300, "commercialName"));
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
    this.issuerReference = issuerReference;
    this.issuerRuc = issuerRuc;
    this.legalName = legalName;
    this.commercialName = commercialName;
    this.headOfficeAddress = headOfficeAddress;
    this.accountingRequired = accountingRequired;
    this.specialTaxpayer = specialTaxpayer;
    this.withholdingAgent = withholdingAgent;
    this.rimpeClassification = rimpeClassification;
    this.largeContributor = largeContributor;
    this.establishmentReference = establishmentReference;
    this.establishmentCode = establishmentCode;
    this.establishmentAddress = establishmentAddress;
    this.emissionPointId = emissionPointId;
    this.emissionPointCode = emissionPointCode;
    this.environmentCode = environmentCode;
    this.documentTypeCode = documentTypeCode;
    this.emissionTypeCode = emissionTypeCode;
    this.sourceEvidence = sourceEvidence;
    this.sriTechnicalRuleIdentifier = sriTechnicalRuleIdentifier;
    this.sriTechnicalRuleDate = sriTechnicalRuleDate;
    this.numericCodePolicyVersion = numericCodePolicyVersion;
  }

  private static void requireRuc(@Nullable String value) {
    if (value == null || !RUC.matcher(value).matches()) {
      throw new IllegalArgumentException("Issuer RUC is invalid");
    }
  }

  private static void requireThreeDigits(@Nullable String value, String field) {
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
