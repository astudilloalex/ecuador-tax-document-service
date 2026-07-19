package com.alexastudillo.taxdocument.support.fiscalpreparation;

import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationCommitIntent;
import com.alexastudillo.taxdocument.application.fiscalpreparation.InvoiceDraftPreparationView;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalContextSnapshot;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalDesignation;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalSourceEvidence;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public final class FiscalPreparationTestFixtures {
  public static final UUID COMPANY_UUID = UUID.fromString("11111111-1111-4111-8111-111111111111");
  public static final CompanyId COMPANY = new CompanyId(COMPANY_UUID);
  public static final UUID DRAFT = UUID.fromString("22222222-2222-4222-8222-222222222222");
  public static final UUID EMISSION_POINT = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  public static final UUID BASELINE = UUID.fromString("44444444-4444-4444-8444-444444444444");
  public static final LocalDate DATE = LocalDate.of(2026, 7, 18);
  public static final Instant CREATED_AT = Instant.parse("2026-07-18T12:00:00Z");

  private FiscalPreparationTestFixtures() {}

  public static InvoiceDraftPreparationView draft() {
    return new InvoiceDraftPreparationView(DRAFT, COMPANY, EMISSION_POINT, DATE, "DRAFT");
  }

  public static FiscalPreparationCommitIntent intent() {
    return new FiscalPreparationCommitIntent(draft(), snapshot());
  }

  public static FiscalContextSnapshot snapshot() {
    return snapshot(EMISSION_POINT, "issuer-1", "establishment-1", "001", "001");
  }

  public static FiscalContextSnapshot snapshot(
      UUID emissionPointId,
      String issuerReference,
      String establishmentReference,
      String establishmentCode,
      String emissionPointCode) {
    return new FiscalContextSnapshot(
        issuerReference,
        "1792146739001",
        "Issuer S.A.",
        Optional.of("Issuer"),
        "Head Office",
        true,
        Optional.empty(),
        Optional.empty(),
        FiscalDesignation.RimpeClassification.NONE,
        Optional.empty(),
        establishmentReference,
        establishmentCode,
        "Establishment Address",
        emissionPointId,
        emissionPointCode,
        "1",
        "01",
        "1",
        new FiscalSourceEvidence(
            "SRI",
            "revision-1",
            LocalDate.of(2026, 7, 1),
            Optional.empty(),
            Instant.parse("2026-07-18T11:59:00Z")),
        "SRI-OFFLINE-2.33",
        LocalDate.of(2026, 7, 13),
        "SECURE_RANDOM_8_V1");
  }
}
