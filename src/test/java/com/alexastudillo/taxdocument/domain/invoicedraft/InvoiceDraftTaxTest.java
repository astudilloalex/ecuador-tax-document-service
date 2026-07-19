package com.alexastudillo.taxdocument.domain.invoicedraft;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class InvoiceDraftTaxTest {
  @Test
  void ivaRuleUsesInclusiveEffectiveBoundaries() {
    TaxSelection selection =
        new TaxSelection(
            requireNonNull(java.util.UUID.randomUUID()),
            "IVA",
            TaxSelection.Treatment.PERCENTAGE_RATE,
            "2",
            "4",
            new java.math.BigDecimal("15.00"),
            DomainTestFixtures.VERSION,
            true,
            date(7, 1),
            date(7, 31));
    assertDoesNotThrow(() -> selection.requireEffectiveOn(date(7, 1)));
    assertDoesNotThrow(() -> selection.requireEffectiveOn(date(7, 31)));
    assertThrows(DraftValidationException.class, () -> selection.requireEffectiveOn(date(8, 1)));
  }

  private static LocalDate date(int month, int day) {
    return requireNonNull(LocalDate.of(2026, month, day));
  }
}
