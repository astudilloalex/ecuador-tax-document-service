package com.alexastudillo.taxdocument.infrastructure.requestcontext;

import com.alexastudillo.taxdocument.application.requestcontext.RequestClock;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Default UTC system-clock adapter with distinct request and persistence operations. */
@ApplicationScoped
public final class SystemRequestClock implements RequestClock {
  private final Clock clock;

  public SystemRequestClock() {
    this(Clock.systemUTC());
  }

  public SystemRequestClock(Clock clock) {
    this.clock = Objects.requireNonNull(clock, "clock");
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
