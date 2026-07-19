package com.alexastudillo.taxdocument.support;

import com.alexastudillo.taxdocument.application.invoicedraft.RequestClock;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/** Resettable deterministic test clock with separate invocation counters. */
@Alternative
@Priority(1)
@Singleton
public final class FixedRequestClock implements RequestClock {
  private final AtomicInteger requestCalls = new AtomicInteger();
  private final AtomicInteger persistenceCalls = new AtomicInteger();
  private volatile Instant requestInstant = Instant.parse("2026-07-17T12:00:00Z");
  private volatile Instant persistenceInstant = Instant.parse("2026-07-17T12:00:01Z");

  @Override
  public Instant requestTime() {
    requestCalls.incrementAndGet();
    return requestInstant;
  }

  @Override
  public Instant persistenceTime() {
    persistenceCalls.incrementAndGet();
    return persistenceInstant;
  }

  public void reset(Instant requestValue, Instant persistenceValue) {
    requestInstant = requestValue;
    persistenceInstant = persistenceValue;
    requestCalls.set(0);
    persistenceCalls.set(0);
  }

  public int requestCalls() {
    return requestCalls.get();
  }

  public int persistenceCalls() {
    return persistenceCalls.get();
  }
}
