package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import java.time.Duration;

/** Exact remaining-request/configured-timeout minimum used by reactive database subscriptions. */
record ReactiveOperationBudget(Duration timeout, TimeoutOwner timeoutOwner) {
  static ReactiveOperationBudget clamp(Duration remaining, Duration configuredTimeout) {
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

  enum TimeoutOwner {
    REQUEST_DEADLINE,
    CONFIGURED_OPERATION
  }
}
