package com.alexastudillo.taxdocument.domain.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class InvoiceDraftCalculatorTest {
  private final InvoiceDraftCalculator calculator = new InvoiceDraftCalculator();

  @Test
  void calculatesTheApprovedFifteenPercentVectorWithHalfUpMoney() {
    var calculation =
        calculator.calculate(
            DomainTestFixtures.buyer(),
            List.of(
                DomainTestFixtures.line(
                    1,
                    "2.000000",
                    "10.000000",
                    "5.00",
                    DomainTestFixtures.tax("15.00", TaxSelection.Treatment.PERCENTAGE_RATE))),
            List.of(DomainTestFixtures.payment("17.25")));

    assertEquals(
        "20.00",
        Objects.requireNonNull(calculation.lines().getFirst().grossAmount()).toPlainString());
    assertEquals("15.00", calculation.subtotalBeforeTaxes().toPlainString());
    assertEquals("2.25", calculation.taxTotals().getFirst().amount().toPlainString());
    assertEquals("17.25", calculation.grandTotal().toPlainString());
  }

  @Test
  void stage11bSelectsDiscountBeforePaymentMismatch() {
    DraftValidationException failure =
        assertThrows(
            DraftValidationException.class,
            () ->
                calculator.calculate(
                    DomainTestFixtures.buyer(),
                    List.of(
                        DomainTestFixtures.line(
                            1,
                            "1.000000",
                            "1.000000",
                            "1.01",
                            DomainTestFixtures.tax("0.00", TaxSelection.Treatment.ZERO_RATE))),
                    List.of(DomainTestFixtures.payment("999.00"))));
    assertEquals("DISCOUNT_EXCEEDS_GROSS", failure.code());
  }
}
