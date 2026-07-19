package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SystemRequestClockTest {
  @Test
  void persistenceTimeUsesPostgreSqlRepresentableMicrosecondPrecision() {
    Instant nanosecondInstant = Instant.parse("2026-07-18T23:04:00.612623045Z");
    SystemRequestClock clock =
        new SystemRequestClock(Clock.fixed(nanosecondInstant, ZoneOffset.UTC));

    assertEquals(nanosecondInstant, clock.requestTime());
    assertEquals(Instant.parse("2026-07-18T23:04:00.612623Z"), clock.persistenceTime());
  }
}
