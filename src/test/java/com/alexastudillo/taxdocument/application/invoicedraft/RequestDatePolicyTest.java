package com.alexastudillo.taxdocument.application.invoicedraft;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alexastudillo.taxdocument.application.requestcontext.RequestDeadline;
import com.alexastudillo.taxdocument.support.FixedRequestClock;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class RequestDatePolicyTest {
  @Test
  void requestBudgetUsesMonotonicTimeWithoutChangingCapturedCivilDate() {
    AtomicLong ticker = new AtomicLong(100L);
    RequestDeadline deadline =
        RequestDeadline.start(requireNonNull(Duration.ofNanos(20)), ticker::get);
    ticker.set(119L);
    assertFalse(deadline.expired());
    assertEquals(Duration.ofNanos(1), deadline.remaining());
  }

  @Test
  void clockOperationsAreDistinctAndResettable() {
    FixedRequestClock clock = new FixedRequestClock();
    Instant request = requireNonNull(Instant.parse("2026-07-18T04:59:59Z"));
    Instant persistence = requireNonNull(Instant.parse("2026-07-18T05:00:01Z"));
    clock.reset(request, persistence);
    assertEquals(request, clock.requestTime());
    assertEquals(persistence, clock.persistenceTime());
    assertEquals(1, clock.requestCalls());
    assertEquals(1, clock.persistenceCalls());
  }

  @Test
  void persistedResultRequiresBothRealTimestamps() {
    InvocationTargetException failure =
        assertThrows(
            InvocationTargetException.class,
            () ->
                PersistedInvoiceDraft.class
                    .getDeclaredConstructor(
                        com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceDraft.class,
                        Instant.class,
                        Instant.class)
                    .newInstance(null, Instant.EPOCH, Instant.EPOCH));
    assertInstanceOf(NullPointerException.class, failure.getCause());
  }
}
