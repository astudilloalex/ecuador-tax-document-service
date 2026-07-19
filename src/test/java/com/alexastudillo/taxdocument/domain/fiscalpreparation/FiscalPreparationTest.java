package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FiscalPreparationTest {
  @Test
  void aggregateIsOneImmutableCompanyDraftFiscalIdentityWithOneUnchangedDateAndInstant() {
    CompanyId company = new CompanyId(UUID.fromString("11111111-1111-4111-8111-111111111111"));
    UUID draft = UUID.fromString("22222222-2222-4222-8222-222222222222");
    LocalDate emissionDate = LocalDate.of(2011, 10, 21);
    FiscalContextSnapshot snapshot =
        FiscalContextSnapshotTest.snapshot(Optional.empty(), Optional.empty(), Optional.empty());
    OfficialSequentialNumber sequential = OfficialSequentialNumber.of(1);
    NumericCode numericCode = NumericCode.parse("12345678");
    AccessKey accessKey =
        new AccessKeyGenerator()
            .generate(
                emissionDate,
                snapshot.issuerRuc(),
                snapshot.environmentCode(),
                snapshot.establishmentCode(),
                snapshot.emissionPointCode(),
                sequential,
                numericCode);
    Instant createdAt = Instant.parse("2026-07-18T12:00:00Z");
    FiscalPreparation preparation =
        new FiscalPreparation(
            UUID.fromString("33333333-3333-4333-8333-333333333333"),
            company,
            draft,
            UUID.fromString("44444444-4444-4444-8444-444444444444"),
            emissionDate,
            snapshot,
            sequential,
            numericCode,
            accessKey,
            createdAt);

    assertEquals(company, preparation.companyId());
    assertEquals(draft, preparation.invoiceDraftId());
    assertEquals(emissionDate, preparation.emissionDate());
    assertEquals(createdAt, preparation.createdAt());
    assertEquals(accessKey, preparation.accessKey());
    assertFalse(
        Arrays.stream(FiscalPreparation.class.getMethods())
            .map(Method::getName)
            .anyMatch(
                name ->
                    name.startsWith("set")
                        || name.equals("update")
                        || name.equals("delete")
                        || name.equals("cancel")
                        || name.equals("reverse")
                        || name.equals("reuse")));
  }
}
