package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.jspecify.annotations.Nullable;

/** Infrastructure-only flattened immutable Fiscal Preparation row. */
@Entity
@Immutable
@Table(name = "fiscal_preparation")
public class FiscalPreparationEntity extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "company_id", nullable = false)
  public UUID companyId;

  @Column(name = "invoice_draft_id", nullable = false)
  public UUID invoiceDraftId;

  @Column(name = "official_sequence_baseline_id", nullable = false)
  public UUID officialSequenceBaselineId;

  @Column(name = "emission_date", nullable = false)
  public LocalDate emissionDate;

  @Column(name = "issuer_reference", length = 128, nullable = false)
  public String issuerReference;

  @Column(name = "issuer_ruc", length = 13, nullable = false)
  public String issuerRuc;

  @Column(name = "legal_name", length = 300, nullable = false)
  public String legalName;

  @Nullable
  @Column(name = "commercial_name", length = 300)
  public String commercialName;

  @Column(name = "head_office_address", length = 300, nullable = false)
  public String headOfficeAddress;

  @Column(name = "accounting_required", nullable = false)
  public boolean accountingRequired;

  @Nullable
  @Column(name = "special_taxpayer_resolution", length = 64)
  public String specialTaxpayerResolution;

  @Nullable
  @Column(name = "withholding_agent_resolution", length = 8)
  public String withholdingAgentResolution;

  @Column(name = "rimpe_classification", length = 32, nullable = false)
  public String rimpeClassification;

  @Nullable
  @Column(name = "large_contributor_resolution", length = 64)
  public String largeContributorResolution;

  @Nullable
  @Column(name = "large_contributor_legend", length = 300)
  public String largeContributorLegend;

  @Column(name = "establishment_reference", length = 128, nullable = false)
  public String establishmentReference;

  @Column(name = "establishment_code", length = 3, nullable = false)
  public String establishmentCode;

  @Column(name = "establishment_address", length = 300, nullable = false)
  public String establishmentAddress;

  @Column(name = "emission_point_id", nullable = false)
  public UUID emissionPointId;

  @Column(name = "emission_point_code", length = 3, nullable = false)
  public String emissionPointCode;

  @Column(name = "environment_code", length = 1, nullable = false)
  public String environmentCode;

  @Column(name = "document_type_code", length = 2, nullable = false)
  public String documentTypeCode;

  @Column(name = "emission_type_code", length = 1, nullable = false)
  public String emissionTypeCode;

  @Column(name = "source_authority", length = 128, nullable = false)
  public String sourceAuthority;

  @Column(name = "source_revision", length = 128, nullable = false)
  public String sourceRevision;

  @Column(name = "source_effective_from", nullable = false)
  public LocalDate sourceEffectiveFrom;

  @Nullable
  @Column(name = "source_effective_through")
  public LocalDate sourceEffectiveThrough;

  @Column(name = "source_observed_at", nullable = false)
  public Instant sourceObservedAt;

  @Column(name = "technical_rule_id", length = 64, nullable = false)
  public String technicalRuleId;

  @Column(name = "technical_rule_modified_on", nullable = false)
  public LocalDate technicalRuleModifiedOn;

  @Column(name = "numeric_code_policy_id", length = 64, nullable = false)
  public String numericCodePolicyId;

  @Column(name = "official_sequential_number", length = 9, nullable = false)
  public String officialSequentialNumber;

  @Column(name = "numeric_code", length = 8, nullable = false)
  public String numericCode;

  @Column(name = "access_key", length = 49, nullable = false)
  public String accessKey;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
