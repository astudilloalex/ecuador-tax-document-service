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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Infrastructure-only flattened immutable Fiscal Preparation row. */
@Entity
@Immutable
@Table(name = "fiscal_preparation")
@NullMarked
public class FiscalPreparationEntity extends PanacheEntityBase {
  @Id public @Nullable UUID id;

  @Column(name = "company_id", nullable = false)
  public @Nullable UUID companyId;

  @Column(name = "invoice_draft_id", nullable = false)
  public @Nullable UUID invoiceDraftId;

  @Column(name = "official_sequence_baseline_id", nullable = false)
  public @Nullable UUID officialSequenceBaselineId;

  @Column(name = "emission_date", nullable = false)
  public @Nullable LocalDate emissionDate;

  @Column(name = "issuer_reference", length = 128, nullable = false)
  public @Nullable String issuerReference;

  @Column(name = "issuer_ruc", length = 13, nullable = false)
  public @Nullable String issuerRuc;

  @Column(name = "legal_name", length = 300, nullable = false)
  public @Nullable String legalName;

  @Nullable
  @Column(name = "commercial_name", length = 300)
  public String commercialName;

  @Column(name = "head_office_address", length = 300, nullable = false)
  public @Nullable String headOfficeAddress;

  @Column(name = "accounting_required", nullable = false)
  public boolean accountingRequired;

  @Nullable
  @Column(name = "special_taxpayer_resolution", length = 64)
  public String specialTaxpayerResolution;

  @Nullable
  @Column(name = "withholding_agent_resolution", length = 8)
  public String withholdingAgentResolution;

  @Column(name = "rimpe_classification", length = 32, nullable = false)
  public @Nullable String rimpeClassification;

  @Nullable
  @Column(name = "large_contributor_resolution", length = 64)
  public String largeContributorResolution;

  @Nullable
  @Column(name = "large_contributor_legend", length = 300)
  public String largeContributorLegend;

  @Column(name = "establishment_reference", length = 128, nullable = false)
  public @Nullable String establishmentReference;

  @Column(name = "establishment_code", length = 3, nullable = false)
  public @Nullable String establishmentCode;

  @Column(name = "establishment_address", length = 300, nullable = false)
  public @Nullable String establishmentAddress;

  @Column(name = "emission_point_id", nullable = false)
  public @Nullable UUID emissionPointId;

  @Column(name = "emission_point_code", length = 3, nullable = false)
  public @Nullable String emissionPointCode;

  @Column(name = "environment_code", length = 1, nullable = false)
  public @Nullable String environmentCode;

  @Column(name = "document_type_code", length = 2, nullable = false)
  public @Nullable String documentTypeCode;

  @Column(name = "emission_type_code", length = 1, nullable = false)
  public @Nullable String emissionTypeCode;

  @Column(name = "source_authority", length = 128, nullable = false)
  public @Nullable String sourceAuthority;

  @Column(name = "source_revision", length = 128, nullable = false)
  public @Nullable String sourceRevision;

  @Column(name = "source_effective_from", nullable = false)
  public @Nullable LocalDate sourceEffectiveFrom;

  @Nullable
  @Column(name = "source_effective_through")
  public LocalDate sourceEffectiveThrough;

  @Column(name = "source_observed_at", nullable = false)
  public @Nullable Instant sourceObservedAt;

  @Column(name = "technical_rule_id", length = 64, nullable = false)
  public @Nullable String technicalRuleId;

  @Column(name = "technical_rule_modified_on", nullable = false)
  public @Nullable LocalDate technicalRuleModifiedOn;

  @Column(name = "numeric_code_policy_id", length = 64, nullable = false)
  public @Nullable String numericCodePolicyId;

  @Column(name = "official_sequential_number", length = 9, nullable = false)
  public @Nullable String officialSequentialNumber;

  @Column(name = "numeric_code", length = 8, nullable = false)
  public @Nullable String numericCode;

  @Column(name = "access_key", length = 49, nullable = false)
  public @Nullable String accessKey;

  @Column(name = "created_at", nullable = false)
  public @Nullable Instant createdAt;
}
