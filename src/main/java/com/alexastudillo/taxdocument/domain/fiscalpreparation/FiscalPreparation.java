package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Indivisible immutable fiscal identity assigned to exactly one Company-owned Invoice Draft. */
public record FiscalPreparation(
    UUID id,
    CompanyId companyId,
    UUID invoiceDraftId,
    UUID officialSequenceBaselineId,
    LocalDate emissionDate,
    FiscalContextSnapshot fiscalContextSnapshot,
    OfficialSequentialNumber officialSequentialNumber,
    NumericCode numericCode,
    AccessKey accessKey,
    Instant createdAt) {
  public FiscalPreparation {
    FiscalContextSnapshot.requireNonNil(id, "fiscalPreparationId");
    Objects.requireNonNull(companyId, "companyId");
    FiscalContextSnapshot.requireNonNil(invoiceDraftId, "invoiceDraftId");
    FiscalContextSnapshot.requireNonNil(officialSequenceBaselineId, "officialSequenceBaselineId");
    Objects.requireNonNull(emissionDate, "emissionDate");
    Objects.requireNonNull(fiscalContextSnapshot, "fiscalContextSnapshot");
    Objects.requireNonNull(officialSequentialNumber, "officialSequentialNumber");
    Objects.requireNonNull(numericCode, "numericCode");
    Objects.requireNonNull(accessKey, "accessKey");
    Objects.requireNonNull(createdAt, "createdAt");
    new AccessKeyGenerator()
        .validateMatches(
            accessKey,
            emissionDate,
            fiscalContextSnapshot.issuerRuc(),
            fiscalContextSnapshot.environmentCode(),
            fiscalContextSnapshot.establishmentCode(),
            fiscalContextSnapshot.emissionPointCode(),
            officialSequentialNumber,
            numericCode);
  }
}
