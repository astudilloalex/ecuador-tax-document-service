package com.alexastudillo.taxdocument.support.fiscalpreparation;

import static java.util.Objects.requireNonNull;

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
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class FiscalPreparationTestFixtures {
  public static final UUID COMPANY_UUID = uuid("11111111-1111-4111-8111-111111111111");
  public static final CompanyId COMPANY = new CompanyId(COMPANY_UUID);
  public static final UUID DRAFT = uuid("22222222-2222-4222-8222-222222222222");
  public static final UUID EMISSION_POINT = uuid("123e4567-e89b-12d3-a456-426614174000");
  public static final UUID BASELINE = uuid("44444444-4444-4444-8444-444444444444");
  public static final LocalDate DATE = date(2026, 7, 18);
  public static final Instant CREATED_AT = instant("2026-07-18T12:00:00Z");

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
        requireNonNull(Optional.of("Issuer")),
        "Head Office",
        true,
        emptyOptional(),
        emptyOptional(),
        FiscalDesignation.RimpeClassification.NONE,
        emptyOptional(),
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
            date(2026, 7, 1),
            emptyOptional(),
            instant("2026-07-18T11:59:00Z")),
        "SRI-OFFLINE-2.33",
        date(2026, 7, 13),
        "SECURE_RANDOM_8_V1");
  }

  private static UUID uuid(String value) {
    return requireNonNull(UUID.fromString(value));
  }

  private static LocalDate date(int year, int month, int day) {
    return requireNonNull(LocalDate.of(year, month, day));
  }

  private static Instant instant(String value) {
    return requireNonNull(Instant.parse(value));
  }

  private static <T> Optional<T> emptyOptional() {
    return requireNonNull(Optional.empty());
  }
}
