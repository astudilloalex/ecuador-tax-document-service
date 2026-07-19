package com.alexastudillo.taxdocument.infrastructure.persistence;

import java.time.Duration;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/** Exact remaining-request/configured-timeout minimum used by reactive database subscriptions. */
@NullMarked
public record ReactiveOperationBudget(Duration timeout, TimeoutOwner timeoutOwner) {
  public ReactiveOperationBudget {
    Objects.requireNonNull(timeout, "timeout");
    Objects.requireNonNull(timeoutOwner, "timeoutOwner");
  }

  public static ReactiveOperationBudget clamp(Duration remaining, Duration configuredTimeout) {
    Objects.requireNonNull(remaining, "remaining");
    Objects.requireNonNull(configuredTimeout, "configuredTimeout");
    if (remaining.isZero() || remaining.isNegative()) {
      throw new IllegalArgumentException("remaining must be positive");
    }
    if (configuredTimeout.isZero() || configuredTimeout.isNegative()) {
      throw new IllegalArgumentException("configuredTimeout must be positive");
    }
    return remaining.compareTo(configuredTimeout) < 0
        ? new ReactiveOperationBudget(remaining, TimeoutOwner.REQUEST_DEADLINE)
        : new ReactiveOperationBudget(configuredTimeout, TimeoutOwner.CONFIGURED_OPERATION);
  }

  public enum TimeoutOwner {
    REQUEST_DEADLINE,
    CONFIGURED_OPERATION
  }
}
