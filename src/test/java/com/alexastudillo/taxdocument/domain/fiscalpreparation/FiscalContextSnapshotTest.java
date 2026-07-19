package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class FiscalContextSnapshotTest {
  @Test
  void minimalSnapshotRetainsOnlyAuthoritativeIssuanceFactsAndVersionEvidence() {
    FiscalContextSnapshot snapshot = snapshot(emptyOptional(), emptyOptional(), emptyOptional());

    assertEquals("SRI-OFFLINE-2.33", snapshot.sriTechnicalRuleIdentifier());
    assertEquals(LocalDate.of(2026, 7, 13), snapshot.sriTechnicalRuleDate());
    assertEquals("SECURE_RANDOM_8_V1", snapshot.numericCodePolicyVersion());
    assertTrue(snapshot.commercialName().isEmpty());
    assertTrue(snapshot.specialTaxpayer().isEmpty());
    assertTrue(snapshot.withholdingAgent().isEmpty());
    assertTrue(snapshot.largeContributor().isEmpty());
    assertEquals(FiscalDesignation.RimpeClassification.NONE, snapshot.rimpeClassification());
  }

  @Test
  void applicableDesignationsPersistTheirExactRequiredEvidenceAsCompleteValues() {
    FiscalContextSnapshot snapshot =
        snapshot(
            Optional.of(new FiscalDesignation.SpecialTaxpayer("NAC-001")),
            Optional.of(new FiscalDesignation.WithholdingAgent("42")),
            Optional.of(new FiscalDesignation.LargeContributor("NAC-002", "LARGE CONTRIBUTOR")));

    assertEquals("NAC-001", snapshot.specialTaxpayer().orElseThrow().resolutionIdentifier());
    assertEquals("42", snapshot.withholdingAgent().orElseThrow().resolutionIdentifier());
    assertEquals("LARGE CONTRIBUTOR", snapshot.largeContributor().orElseThrow().requiredLegend());
  }

  @Test
  void rejectsInvalidCodesEffectiveEvidenceAndPartialOrInventedDesignationData() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new FiscalDesignation.LargeContributor("NAC-002", " "));
    assertThrows(
        IllegalArgumentException.class, () -> new FiscalDesignation.WithholdingAgent("00042"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new FiscalSourceEvidence(
                "SRI",
                "revision-1",
                date(2026, 7, 19),
                requireNonNull(Optional.of(date(2026, 7, 18))),
                instant("2026-07-18T12:00:00Z")));
  }

  static FiscalContextSnapshot snapshot(
      Optional<FiscalDesignation.SpecialTaxpayer> specialTaxpayer,
      Optional<FiscalDesignation.WithholdingAgent> withholdingAgent,
      Optional<FiscalDesignation.LargeContributor> largeContributor) {
    return new FiscalContextSnapshot(
        "issuer-1",
        "1792146739001",
        "Issuer S.A.",
        emptyOptional(),
        "Head Office",
        true,
        specialTaxpayer,
        withholdingAgent,
        FiscalDesignation.RimpeClassification.NONE,
        largeContributor,
        "establishment-1",
        "001",
        "Establishment Address",
        requireNonNull(UUID.fromString("123e4567-e89b-12d3-a456-426614174000")),
        "002",
        "1",
        "01",
        "1",
        new FiscalSourceEvidence(
            "SRI",
            "revision-1",
            date(2026, 7, 1),
            emptyOptional(),
            instant("2026-07-18T12:00:00Z")),
        "SRI-OFFLINE-2.33",
        date(2026, 7, 13),
        "SECURE_RANDOM_8_V1");
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
