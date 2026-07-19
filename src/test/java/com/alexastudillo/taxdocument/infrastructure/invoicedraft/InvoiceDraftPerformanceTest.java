package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.application.requestcontext.RequestDeadline;
import com.alexastudillo.taxdocument.domain.invoicedraft.Buyer;
import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceDraftCalculator;
import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceLine;
import com.alexastudillo.taxdocument.domain.invoicedraft.Payment;
import com.alexastudillo.taxdocument.domain.invoicedraft.TaxSelection;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.sqlclient.Pool;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@QuarkusTest
@NullMarked
class InvoiceDraftPerformanceTest {
  @Inject Pool pool;

  @Test
  void maximumLineCalculationIsDeterministicAndCompletesWithinRequestBudget() {
    TaxSelection tax =
        new TaxSelection(
            randomUuid(),
            "IVA",
            TaxSelection.Treatment.PERCENTAGE_RATE,
            "2",
            "4",
            new BigDecimal("15.00"),
            "SRI-OFFLINE-2.32-TARGET-1",
            true,
            requireNonNull(LocalDate.of(2026, 7, 12)),
            null);
    List<InvoiceLine> lines = new ArrayList<>();
    for (int position = 1; position <= 500; position++) {
      lines.add(
          new InvoiceLine(
              randomUuid(),
              position,
              "SKU" + position,
              "Service",
              requireNonNull(BigDecimal.ONE),
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
            randomUuid(),
            randomUuid(),
            "01",
            "Cash",
            new BigDecimal("575.00"),
            "SRI-OFFLINE-2.32-TARGET-1");
    Buyer buyer = new Buyer("06", "P123", "Buyer", null, null, null, "SRI-OFFLINE-2.32-TARGET-1");
    long start = System.nanoTime();
    var calculation =
        new InvoiceDraftCalculator().calculate(buyer, lines, requireNonNull(List.of(payment)));
    assertEquals(new BigDecimal("575.00"), calculation.grandTotal());
    assertTrue(Duration.ofNanos(System.nanoTime() - start).compareTo(Duration.ofSeconds(10)) < 0);
  }

  @Test
  void remainingBudgetClampsToZeroWithoutSleeping() {
    AtomicLong ticker = new AtomicLong(0L);
    RequestDeadline deadline =
        RequestDeadline.start(requireNonNull(Duration.ofNanos(10)), ticker::get);
    ticker.set(10L);
    assertTrue(deadline.expired());
    assertEquals(Duration.ZERO, deadline.remaining());
  }

  @Test
  void recordsTheLocalPostgreSqlRoundTripBaseline() {
    for (int sample = 0; sample < 20; sample++) {
      selectOne();
    }
    long[] samples = new long[100];
    for (int sample = 0; sample < samples.length; sample++) {
      long started = System.nanoTime();
      selectOne();
      samples[sample] = System.nanoTime() - started;
    }
    Arrays.sort(samples);
    System.out.printf(
        Locale.ROOT,
        "POSTGRESQL_BASELINE_EVIDENCE samples=100 p50Ms=%.3f p95Ms=%.3f p99Ms=%.3f "
            + "maxMs=%.3f%n",
        percentileMillis(samples, 0.50),
        percentileMillis(samples, 0.95),
        percentileMillis(samples, 0.99),
        samples[samples.length - 1] / 1_000_000.0);
  }

  private void selectOne() {
    long value =
        pool.query("SELECT 1")
            .execute()
            .map(rows -> rows.iterator().next().getLong(0))
            .await()
            .atMost(Duration.ofSeconds(5));
    assertEquals(1L, value);
  }

  private static UUID randomUuid() {
    return requireNonNull(UUID.randomUUID());
  }

  private static double percentileMillis(long[] sortedNanos, double percentile) {
    int index = Math.max(0, (int) Math.ceil(percentile * sortedNanos.length) - 1);
    return sortedNanos[index] / 1_000_000.0;
  }
}
