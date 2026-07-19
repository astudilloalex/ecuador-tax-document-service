package com.alexastudillo.taxdocument.application.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alexastudillo.taxdocument.application.requestcontext.RequestDeadline;
import com.alexastudillo.taxdocument.support.FixedRequestClock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RequestDatePolicyTest {
  @Test
  void requestBudgetUsesMonotonicTimeWithoutChangingCapturedCivilDate() {
    AtomicLong ticker = new AtomicLong(100L);
    RequestDeadline deadline = RequestDeadline.start(Duration.ofNanos(20), ticker::get);
    ticker.set(119L);
    assertFalse(deadline.expired());
    assertEquals(Duration.ofNanos(1), deadline.remaining());
  }

  @Test
  void clockOperationsAreDistinctAndResettable() {
    FixedRequestClock clock = new FixedRequestClock();
    Instant request = Instant.parse("2026-07-18T04:59:59Z");
    Instant persistence = Instant.parse("2026-07-18T05:00:01Z");
    clock.reset(request, persistence);
    assertEquals(request, clock.requestTime());
    assertEquals(persistence, clock.persistenceTime());
    assertEquals(1, clock.requestCalls());
    assertEquals(1, clock.persistenceCalls());
  }

  @Test
  void persistedResultRequiresBothRealTimestamps() {
    assertThrows(
        NullPointerException.class,
        () -> new PersistedInvoiceDraft(null, Instant.EPOCH, Instant.EPOCH));
  }
}
