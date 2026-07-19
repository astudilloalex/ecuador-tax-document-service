package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.AccessKey;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalContextSnapshot;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalDesignation;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalPreparation;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalSourceEvidence;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.NumericCode;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.OfficialSequentialNumber;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Explicit lossless mapping between flattened persistence rows and the immutable domain. */
@ApplicationScoped
@NullMarked
public final class FiscalPreparationPersistenceMapper {
  public FiscalPreparation fromRow(Row row) {
    Objects.requireNonNull(row, "row");
    FiscalContextSnapshot snapshot =
        new FiscalContextSnapshot(
            require(row.getString("issuer_reference"), "issuerReference"),
            require(row.getString("issuer_ruc"), "issuerRuc"),
            require(row.getString("legal_name"), "legalName"),
            optional(row.getString("commercial_name"), "commercialName optional"),
            require(row.getString("head_office_address"), "headOfficeAddress"),
            require(row.getBoolean("accounting_required"), "accountingRequired"),
            specialTaxpayer(row.getString("special_taxpayer_resolution")),
            withholdingAgent(row.getString("withholding_agent_resolution")),
            FiscalDesignation.RimpeClassification.valueOf(
                Objects.requireNonNull(
                    require(row.getString("rimpe_classification"), "rimpeClassification"),
                    "rimpeClassification")),
            largeContributor(
                row.getString("large_contributor_resolution"),
                row.getString("large_contributor_legend")),
            require(row.getString("establishment_reference"), "establishmentReference"),
            require(row.getString("establishment_code"), "establishmentCode"),
            require(row.getString("establishment_address"), "establishmentAddress"),
            require(row.getUUID("emission_point_id"), "emissionPointId"),
            require(row.getString("emission_point_code"), "emissionPointCode"),
            require(row.getString("environment_code"), "environmentCode"),
            require(row.getString("document_type_code"), "documentTypeCode"),
            require(row.getString("emission_type_code"), "emissionTypeCode"),
            new FiscalSourceEvidence(
                require(row.getString("source_authority"), "sourceAuthority"),
                require(row.getString("source_revision"), "sourceRevision"),
                require(row.getLocalDate("source_effective_from"), "sourceEffectiveFrom"),
                optional(
                    row.getLocalDate("source_effective_through"),
                    "sourceEffectiveThrough optional"),
                require(
                    require(row.getOffsetDateTime("source_observed_at"), "sourceObservedAt")
                        .toInstant(),
                    "sourceObservedAt instant")),
            require(row.getString("technical_rule_id"), "technicalRuleId"),
            require(row.getLocalDate("technical_rule_modified_on"), "technicalRuleModifiedOn"),
            require(row.getString("numeric_code_policy_id"), "numericCodePolicyId"));
    return new FiscalPreparation(
        require(row.getUUID("id"), "id"),
        new CompanyId(require(row.getUUID("company_id"), "companyId")),
        require(row.getUUID("invoice_draft_id"), "invoiceDraftId"),
        require(row.getUUID("official_sequence_baseline_id"), "officialSequenceBaselineId"),
        require(row.getLocalDate("emission_date"), "emissionDate"),
        snapshot,
        OfficialSequentialNumber.parse(
            require(row.getString("official_sequential_number"), "officialSequentialNumber")),
        NumericCode.parse(require(row.getString("numeric_code"), "numericCode")),
        AccessKey.parse(require(row.getString("access_key"), "accessKey")),
        require(
            require(row.getOffsetDateTime("created_at"), "createdAt").toInstant(),
            "createdAt instant"));
  }

  public Tuple toInsertParameters(FiscalPreparation preparation) {
    Objects.requireNonNull(preparation, "preparation");
    FiscalContextSnapshot snapshot = preparation.fiscalContextSnapshot();
    FiscalSourceEvidence source = snapshot.sourceEvidence();
    Tuple values = Tuple.tuple();
    values.addUUID(preparation.id());
    values.addUUID(preparation.companyId().value());
    values.addUUID(preparation.invoiceDraftId());
    values.addUUID(preparation.officialSequenceBaselineId());
    values.addLocalDate(preparation.emissionDate());
    values.addString(snapshot.issuerReference());
    values.addString(snapshot.issuerRuc());
    values.addString(snapshot.legalName());
    values.addValue(optionalString(snapshot.commercialName(), "commercialName"));
    values.addString(snapshot.headOfficeAddress());
    values.addBoolean(snapshot.accountingRequired());
    values.addValue(specialTaxpayerResolution(snapshot.specialTaxpayer()));
    values.addValue(withholdingAgentResolution(snapshot.withholdingAgent()));
    values.addString(snapshot.rimpeClassification().name());
    values.addValue(largeContributorResolution(snapshot.largeContributor()));
    values.addValue(largeContributorLegend(snapshot.largeContributor()));
    values.addString(snapshot.establishmentReference());
    values.addString(snapshot.establishmentCode());
    values.addString(snapshot.establishmentAddress());
    values.addUUID(snapshot.emissionPointId());
    values.addString(snapshot.emissionPointCode());
    values.addString(snapshot.environmentCode());
    values.addString(snapshot.documentTypeCode());
    values.addString(snapshot.emissionTypeCode());
    values.addString(source.authority());
    values.addString(source.revision());
    values.addLocalDate(source.effectiveFrom());
    values.addValue(optionalDate(source.effectiveThrough(), "sourceEffectiveThrough"));
    values.addOffsetDateTime(OffsetDateTime.ofInstant(source.observedAt(), ZoneOffset.UTC));
    values.addString(snapshot.sriTechnicalRuleIdentifier());
    values.addLocalDate(snapshot.sriTechnicalRuleDate());
    values.addString(snapshot.numericCodePolicyVersion());
    values.addString(preparation.officialSequentialNumber().value());
    values.addString(preparation.numericCode().value());
    values.addString(preparation.accessKey().value());
    values.addOffsetDateTime(OffsetDateTime.ofInstant(preparation.createdAt(), ZoneOffset.UTC));
    return values;
  }

  private static Optional<FiscalDesignation.@NonNull SpecialTaxpayer> specialTaxpayer(
      @Nullable String resolution) {
    if (resolution == null) {
      return require(Optional.empty(), "empty special taxpayer");
    }
    return require(
        Optional.of(new FiscalDesignation.SpecialTaxpayer(resolution)), "special taxpayer");
  }

  private static Optional<FiscalDesignation.@NonNull WithholdingAgent> withholdingAgent(
      @Nullable String resolution) {
    if (resolution == null) {
      return require(Optional.empty(), "empty withholding agent");
    }
    return require(
        Optional.of(new FiscalDesignation.WithholdingAgent(resolution)), "withholding agent");
  }

  private static Optional<FiscalDesignation.@NonNull LargeContributor> largeContributor(
      @Nullable String resolution, @Nullable String legend) {
    if (resolution == null && legend == null) {
      return require(Optional.empty(), "empty large contributor");
    }
    return require(
        Optional.of(
            new FiscalDesignation.LargeContributor(
                Objects.requireNonNull(resolution, "largeContributorResolution"),
                Objects.requireNonNull(legend, "largeContributorLegend"))),
        "large contributor");
  }

  private static <T> @NonNull T require(@Nullable T value, String field) {
    return Objects.requireNonNull(value, field);
  }

  private static <T extends @NonNull Object> Optional<@NonNull T> optional(
      @Nullable T value, String field) {
    return value == null ? require(Optional.empty(), field) : require(Optional.of(value), field);
  }

  private static @Nullable String optionalString(Optional<@NonNull String> value, String field) {
    Optional<@NonNull String> optional = require(value, field);
    return optional.isPresent() ? require(optional.get(), field) : null;
  }

  private static @Nullable LocalDate optionalDate(
      Optional<@NonNull LocalDate> value, String field) {
    Optional<@NonNull LocalDate> optional = require(value, field);
    return optional.isPresent() ? require(optional.get(), field) : null;
  }

  private static @Nullable String specialTaxpayerResolution(
      Optional<FiscalDesignation.@NonNull SpecialTaxpayer> value) {
    Optional<FiscalDesignation.@NonNull SpecialTaxpayer> optional =
        require(value, "specialTaxpayer");
    if (optional.isEmpty()) {
      return null;
    }
    FiscalDesignation.SpecialTaxpayer designation =
        require(optional.get(), "specialTaxpayer value");
    return require(designation.resolutionIdentifier(), "specialTaxpayer resolution");
  }

  private static @Nullable String withholdingAgentResolution(
      Optional<FiscalDesignation.@NonNull WithholdingAgent> value) {
    Optional<FiscalDesignation.@NonNull WithholdingAgent> optional =
        require(value, "withholdingAgent");
    if (optional.isEmpty()) {
      return null;
    }
    FiscalDesignation.WithholdingAgent designation =
        require(optional.get(), "withholdingAgent value");
    return require(designation.resolutionIdentifier(), "withholdingAgent resolution");
  }

  private static @Nullable String largeContributorResolution(
      Optional<FiscalDesignation.@NonNull LargeContributor> value) {
    Optional<FiscalDesignation.@NonNull LargeContributor> optional =
        require(value, "largeContributor");
    if (optional.isEmpty()) {
      return null;
    }
    FiscalDesignation.LargeContributor designation =
        require(optional.get(), "largeContributor value");
    return require(designation.resolutionIdentifier(), "largeContributor resolution");
  }

  private static @Nullable String largeContributorLegend(
      Optional<FiscalDesignation.@NonNull LargeContributor> value) {
    Optional<FiscalDesignation.@NonNull LargeContributor> optional =
        require(value, "largeContributor");
    if (optional.isEmpty()) {
      return null;
    }
    FiscalDesignation.LargeContributor designation =
        require(optional.get(), "largeContributor value");
    return require(designation.requiredLegend(), "largeContributor legend");
  }
}
