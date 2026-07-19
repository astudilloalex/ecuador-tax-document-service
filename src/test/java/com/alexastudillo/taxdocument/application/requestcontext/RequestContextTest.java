package com.alexastudillo.taxdocument.application.requestcontext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RequestContextTest {
  @Test
  void fixesRequestEntryInstantAndEcuadorCivilDateWhileDeadlineRemainsMonotonic() {
    Instant entry = Instant.parse("2026-07-19T04:59:59.999999999Z");
    AtomicLong ticker = new AtomicLong(100L);
    RequestDeadline deadline = RequestDeadline.start(Duration.ofNanos(20), ticker::get);
    RequestClock clock =
        new RequestClock() {
          @Override
          public Instant requestTime() {
            return entry;
          }

          @Override
          public Instant persistenceTime() {
            return entry.plusNanos(1).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
          }
        };

    RequestContext context = RequestContext.capture(clock, deadline);
    assertEquals(entry, context.requestEntryInstant());
    assertEquals(LocalDate.of(2026, 7, 18), context.ecuadorDate());
    assertEquals(Duration.ofNanos(20), context.deadline().remaining());
    ticker.addAndGet(7L);
    assertEquals(Duration.ofNanos(13), context.deadline().remaining());
    assertFalse(context.deadline().expired());
    ticker.addAndGet(13L);
    assertTrue(context.deadline().expired());
  }
}
