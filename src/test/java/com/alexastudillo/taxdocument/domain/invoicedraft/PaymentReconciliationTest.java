package com.alexastudillo.taxdocument.domain.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentReconciliationTest {
  @Test
  void zeroTotalRequiresExactlyOneZeroPayment() {
    var result =
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
    assertEquals("0.00", result.grandTotal().toPlainString());
  }

  @Test
  void paymentSumMustReconcileExactly() {
    DraftValidationException failure =
        assertThrows(
            DraftValidationException.class,
            () ->
                new InvoiceDraftCalculator()
                    .calculate(
                        DomainTestFixtures.buyer(),
                        List.of(
                            DomainTestFixtures.line(
                                1,
                                "1.000000",
                                "10.000000",
                                "0.00",
                                DomainTestFixtures.tax("0.00", TaxSelection.Treatment.ZERO_RATE))),
                        List.of(DomainTestFixtures.payment("9.99"))));
    assertEquals("PAYMENT_TOTAL_MISMATCH", failure.code());
  }
}
