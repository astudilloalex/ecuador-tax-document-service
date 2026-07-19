package com.alexastudillo.taxdocument.application.requestcontext;

import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Transport-neutral monotonic request budget. */
public final class RequestDeadline {
  private final long expiresAtNanos;
  private final LongSupplier ticker;

  private RequestDeadline(long expiresAtNanos, LongSupplier ticker) {
    this.expiresAtNanos = expiresAtNanos;
    this.ticker = ticker;
  }

  public static RequestDeadline start(Duration budget) {
    return start(budget, System::nanoTime);
  }

  public static RequestDeadline start(Duration budget, LongSupplier ticker) {
    Objects.requireNonNull(budget, "budget");
    Objects.requireNonNull(ticker, "ticker");
    if (budget.isNegative()) {
      throw new IllegalArgumentException("budget must not be negative");
    }
    long now = ticker.getAsLong();
    long nanos = budget.toNanos();
    long expiry = Long.MAX_VALUE - now < nanos ? Long.MAX_VALUE : now + nanos;
    return new RequestDeadline(expiry, ticker);
  }

  public Duration remaining() {
    return Duration.ofNanos(Math.max(0L, expiresAtNanos - ticker.getAsLong()));
  }

  public boolean expired() {
    return remaining().isZero();
  }
}
