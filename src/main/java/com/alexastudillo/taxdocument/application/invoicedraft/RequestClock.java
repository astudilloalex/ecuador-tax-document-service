package com.alexastudillo.taxdocument.application.invoicedraft;

import java.time.Instant;

/** Separate request-boundary and transactional-persistence clock operations. */
public interface RequestClock {
  Instant requestTime();

  /** Returns an instant at PostgreSQL {@code timestamptz} microsecond precision. */
  Instant persistenceTime();
}
