package com.alexastudillo.taxdocument.infrastructure.persistence;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class ReactiveOperationBudgetTest {
  @Test
  void clampsToTheRemainingBudgetWithAnExplicitTimeoutOwner() {
    ReactiveOperationBudget requestBound = ReactiveOperationBudget.clamp(seconds(2), seconds(5));
    assertEquals(Duration.ofSeconds(2), requestBound.timeout());
    assertEquals(
        ReactiveOperationBudget.TimeoutOwner.REQUEST_DEADLINE, requestBound.timeoutOwner());

    ReactiveOperationBudget operationBound = ReactiveOperationBudget.clamp(seconds(8), seconds(5));
    assertEquals(Duration.ofSeconds(5), operationBound.timeout());
    assertEquals(
        ReactiveOperationBudget.TimeoutOwner.CONFIGURED_OPERATION, operationBound.timeoutOwner());
  }

  @Test
  void refusesExhaustedOrInvalidBudgetsBeforeStartingPersistenceWork() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ReactiveOperationBudget.clamp(requireNonNull(Duration.ZERO), seconds(5)));
    assertThrows(
        IllegalArgumentException.class,
        () -> ReactiveOperationBudget.clamp(seconds(1), requireNonNull(Duration.ZERO)));
  }

  private static Duration seconds(long value) {
    return requireNonNull(Duration.ofSeconds(value));
  }
}
