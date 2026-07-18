package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.application.invoicedraft.RequestDeadline;
import com.alexastudillo.taxdocument.domain.invoicedraft.Buyer;
import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceDraftCalculator;
import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceLine;
import com.alexastudillo.taxdocument.domain.invoicedraft.Payment;
import com.alexastudillo.taxdocument.domain.invoicedraft.TaxSelection;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class InvoiceDraftPerformanceTest {
  @Test
  void maximumLineCalculationIsDeterministicAndCompletesWithinRequestBudget() {
    TaxSelection tax =
        new TaxSelection(
            UUID.randomUUID(),
            "IVA",
            TaxSelection.Treatment.PERCENTAGE_RATE,
            "2",
            "4",
            new BigDecimal("15.00"),
            "SRI-OFFLINE-2.32-TARGET-1",
            true,
            LocalDate.of(2026, 7, 12),
            null);
    List<InvoiceLine> lines = new ArrayList<>();
    for (int position = 1; position <= 500; position++) {
      lines.add(
          new InvoiceLine(
              UUID.randomUUID(),
              position,
              "SKU" + position,
              "Service",
              BigDecimal.ONE,
              new BigDecimal("1.000000"),
              new BigDecimal("0.00"),
              tax,
              null,
              null,
              null,
              null,
              null));
    }
    Payment payment =
        new Payment(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "01",
            "Cash",
            new BigDecimal("575.00"),
            "SRI-OFFLINE-2.32-TARGET-1");
    Buyer buyer = new Buyer("06", "P123", "Buyer", null, null, null, "SRI-OFFLINE-2.32-TARGET-1");
    long start = System.nanoTime();
    var calculation = new InvoiceDraftCalculator().calculate(buyer, lines, List.of(payment));
    assertEquals(new BigDecimal("575.00"), calculation.grandTotal());
    assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofSeconds(10)) < 0);
  }

  @Test
  void remainingBudgetClampsToZeroWithoutSleeping() {
    AtomicLong ticker = new AtomicLong(0L);
    RequestDeadline deadline = RequestDeadline.start(Duration.ofNanos(10), ticker::get);
    ticker.set(10L);
    assertTrue(deadline.expired());
    assertEquals(Duration.ZERO, deadline.remaining());
  }
}
