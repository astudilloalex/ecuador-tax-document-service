package com.alexastudillo.taxdocument.domain.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@NullMarked
class InvoiceDraftCalculatorTest {
  private final InvoiceDraftCalculator calculator = new InvoiceDraftCalculator();

  @Test
  void calculatesTheApprovedFifteenPercentVectorWithHalfUpMoney() {
    var calculation =
        calculator.calculate(
            DomainTestFixtures.buyer(),
            Objects.requireNonNull(
                List.of(
                    DomainTestFixtures.line(
                        1,
                        "2.000000",
                        "10.000000",
                        "5.00",
                        DomainTestFixtures.tax("15.00", TaxSelection.Treatment.PERCENTAGE_RATE)))),
            Objects.requireNonNull(List.of(DomainTestFixtures.payment("17.25"))));

    @Nullable InvoiceLine nullableLine = calculation.lines().getFirst();
    InvoiceLine firstLine = Objects.requireNonNull(nullableLine, "first calculated line");
    assertEquals("20.00", Objects.requireNonNull(firstLine.grossAmount()).toPlainString());
    assertEquals("15.00", calculation.subtotalBeforeTaxes().toPlainString());
    @Nullable TaxTotal nullableTaxTotal = calculation.taxTotals().getFirst();
    TaxTotal firstTaxTotal = Objects.requireNonNull(nullableTaxTotal, "first tax total");
    assertEquals("2.25", firstTaxTotal.amount().toPlainString());
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
                    Objects.requireNonNull(
                        List.of(
                            DomainTestFixtures.line(
                                1,
                                "1.000000",
                                "1.000000",
                                "1.01",
                                DomainTestFixtures.tax("0.00", TaxSelection.Treatment.ZERO_RATE)))),
                    Objects.requireNonNull(List.of(DomainTestFixtures.payment("999.00")))));
    assertEquals("DISCOUNT_EXCEEDS_GROSS", failure.code());
  }
}
