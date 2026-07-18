package com.alexastudillo.taxdocument.application.invoicedraft;

import java.time.Instant;

/** Separate request-boundary and transactional-persistence clock operations. */
public interface RequestClock {
  Instant requestTime();

  Instant persistenceTime();
}
