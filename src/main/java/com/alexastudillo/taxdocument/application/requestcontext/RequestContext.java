package com.alexastudillo.taxdocument.application.requestcontext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/** Immutable request-entry time, Ecuador civil date, and monotonic deadline. */
@NullMarked
public record RequestContext(
    Instant requestEntryInstant, LocalDate ecuadorDate, RequestDeadline deadline) {
  public static final ZoneId ECUADOR_TIME_ZONE =
      Objects.requireNonNull(ZoneId.of("America/Guayaquil"));

  public RequestContext {
    Objects.requireNonNull(requestEntryInstant, "requestEntryInstant");
    Objects.requireNonNull(ecuadorDate, "ecuadorDate");
    Objects.requireNonNull(deadline, "deadline");
  }

  public static RequestContext capture(RequestClock clock, RequestDeadline deadline) {
    Objects.requireNonNull(clock, "clock");
    Objects.requireNonNull(deadline, "deadline");
    Instant entry = clock.requestTime();
    return new RequestContext(
        entry, Objects.requireNonNull(entry.atZone(ECUADOR_TIME_ZONE).toLocalDate()), deadline);
  }
}
