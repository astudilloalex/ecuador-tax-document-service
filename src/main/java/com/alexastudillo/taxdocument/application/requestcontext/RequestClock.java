package com.alexastudillo.taxdocument.application.requestcontext;

import java.time.Instant;
import org.jspecify.annotations.NullMarked;

/** Separate request-boundary and transactional-persistence clock operations. */
@NullMarked
public interface RequestClock {
  Instant requestTime();

  /** Returns an instant at PostgreSQL {@code timestamptz} microsecond precision. */
  Instant persistenceTime();
}
