package com.alexastudillo.taxdocument.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ReactiveOperationBudgetTest {
  @Test
  void clampsToTheRemainingBudgetWithAnExplicitTimeoutOwner() {
    ReactiveOperationBudget requestBound =
        ReactiveOperationBudget.clamp(Duration.ofSeconds(2), Duration.ofSeconds(5));
    assertEquals(Duration.ofSeconds(2), requestBound.timeout());
    assertEquals(
        ReactiveOperationBudget.TimeoutOwner.REQUEST_DEADLINE, requestBound.timeoutOwner());

    ReactiveOperationBudget operationBound =
        ReactiveOperationBudget.clamp(Duration.ofSeconds(8), Duration.ofSeconds(5));
    assertEquals(Duration.ofSeconds(5), operationBound.timeout());
    assertEquals(
        ReactiveOperationBudget.TimeoutOwner.CONFIGURED_OPERATION, operationBound.timeoutOwner());
  }

  @Test
  void refusesExhaustedOrInvalidBudgetsBeforeStartingPersistenceWork() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ReactiveOperationBudget.clamp(Duration.ZERO, Duration.ofSeconds(5)));
    assertThrows(
        IllegalArgumentException.class,
        () -> ReactiveOperationBudget.clamp(Duration.ofSeconds(1), Duration.ZERO));
  }
}
