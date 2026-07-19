package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import com.alexastudillo.taxdocument.application.invoicedraft.RequestClock;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Default UTC system-clock adapter with distinct request and persistence operations. */
@ApplicationScoped
public final class SystemRequestClock implements RequestClock {
  private final Clock clock;

  public SystemRequestClock() {
    this(Clock.systemUTC());
  }

  SystemRequestClock(Clock clock) {
    this.clock = clock;
  }

  @Override
  public Instant requestTime() {
    return clock.instant();
  }

  @Override
  public Instant persistenceTime() {
    return clock.instant().truncatedTo(ChronoUnit.MICROS);
  }
}
