package com.alexastudillo.taxdocument.domain.invoicedraft;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class InvoiceDraftTest {
  @Test
  void aggregateIsUsdDraftWithImmutableCompanyAndChildren() {
    InvoiceDraftCalculator.Calculation calculation =
        new InvoiceDraftCalculator()
            .calculate(
                DomainTestFixtures.buyer(),
                List.of(
                    DomainTestFixtures.line(
                        1,
                        "1.000000",
                        "0.000000",
                        "0.00",
                        DomainTestFixtures.tax("0.00", TaxSelection.Treatment.ZERO_RATE))),
                List.of(DomainTestFixtures.payment("0.00")));
    InvoiceDraft draft =
        new InvoiceDraft(
            requireNonNull(UUID.randomUUID()),
            new CompanyId(uuid("11111111-1111-1111-1111-111111111111")),
            uuid("123e4567-e89b-12d3-a456-426614174000"),
            requireNonNull(LocalDate.of(2026, 7, 17)),
            DomainTestFixtures.buyer(),
            calculation.lines(),
            calculation.taxTotals(),
            List.of(DomainTestFixtures.payment("0.00")),
            requireNonNull(List.of()),
            calculation.subtotalBeforeTaxes(),
            calculation.totalDiscount(),
            calculation.grandTotal());
    assertEquals("DRAFT", InvoiceDraft.STATUS);
    assertEquals("USD", InvoiceDraft.CURRENCY);
    assertThrows(UnsupportedOperationException.class, () -> draft.lines().clear());
  }

  private static UUID uuid(String value) {
    return requireNonNull(UUID.fromString(value));
  }
}
