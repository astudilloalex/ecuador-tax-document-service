package com.alexastudillo.taxdocument.domain.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InvoiceDraftTaxTest {
  @Test
  void ivaRuleUsesInclusiveEffectiveBoundaries() {
    TaxSelection selection =
        new TaxSelection(
            java.util.UUID.randomUUID(),
            "IVA",
            TaxSelection.Treatment.PERCENTAGE_RATE,
            "2",
            "4",
            new java.math.BigDecimal("15.00"),
            DomainTestFixtures.VERSION,
            true,
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 31));
    assertDoesNotThrow(() -> selection.requireEffectiveOn(LocalDate.of(2026, 7, 1)));
    assertDoesNotThrow(() -> selection.requireEffectiveOn(LocalDate.of(2026, 7, 31)));
    assertThrows(
        DraftValidationException.class,
        () -> selection.requireEffectiveOn(LocalDate.of(2026, 8, 1)));
  }
}
