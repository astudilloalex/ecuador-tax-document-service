package com.alexastudillo.taxdocument.api.fiscalpreparation;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationApplicationException;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationCommitTracker;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationFailure;
import com.alexastudillo.taxdocument.application.requestcontext.RequestContext;
import com.alexastudillo.taxdocument.application.requestcontext.RequestDeadline;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class FiscalPreparationRequestDeadlineHandlerTest {
  @Test
  void conclusiveResultBeforeExpiryWinsExactlyOnceAndLateDeadlineCannotReplaceIt() {
    AtomicLong ticker = new AtomicLong();
    FiscalPreparationRequestState state = state(ticker, 10L);
    FiscalPreparationRequestDeadlineHandler handler = new FiscalPreparationRequestDeadlineHandler();

    assertEquals("winner", handler.race(item("winner"), state).await().indefinitely());
    ticker.set(10L);
    assertFalse(state.acceptTerminal());
  }

  @Test
  void expiryReturnsStableTimeoutWithKnowledgeAccurateRecoveryGuidance() {
    AtomicLong ticker = new AtomicLong();
    FiscalPreparationRequestState state = state(ticker, 1L);
    ticker.set(1L);
    FiscalPreparationApplicationException failure =
        assertThrows(
            FiscalPreparationApplicationException.class,
            () ->
                new FiscalPreparationRequestDeadlineHandler()
                    .race(nothing(), state)
                    .await()
                    .indefinitely());
    assertEquals(FiscalPreparationFailure.Code.REQUEST_TIMEOUT, failure.failure().code());
    assertEquals(
        FiscalPreparationFailure.CommitKnowledge.NOT_STARTED, failure.failure().commitKnowledge());
  }

  @Test
  void expiryAfterPossibleCommitMakesNoZeroStateClaimAndDirectsNaturalReplay() {
    AtomicLong ticker = new AtomicLong();
    FiscalPreparationRequestState state = state(ticker, 1L);
    state.commitTracker().possibleCommit();
    ticker.set(1L);

    FiscalPreparationApplicationException failure =
        assertThrows(
            FiscalPreparationApplicationException.class,
            () ->
                new FiscalPreparationRequestDeadlineHandler()
                    .race(nothing(), state)
                    .await()
                    .indefinitely());

    assertEquals(FiscalPreparationFailure.Code.REQUEST_TIMEOUT, failure.failure().code());
    assertEquals(
        FiscalPreparationFailure.CommitKnowledge.POSSIBLE_COMMIT,
        failure.failure().commitKnowledge());
    assertEquals(true, failure.failure().detail().contains("same Company and Invoice Draft"));
  }

  @Test
  void expiryAfterConfirmedRollbackRetainsTheConclusiveZeroStateKnowledge() {
    AtomicLong ticker = new AtomicLong();
    FiscalPreparationRequestState state = state(ticker, 1L);
    state.commitTracker().possibleCommit();
    state.commitTracker().confirmedRollback();
    ticker.set(1L);

    FiscalPreparationApplicationException failure =
        assertThrows(
            FiscalPreparationApplicationException.class,
            () ->
                new FiscalPreparationRequestDeadlineHandler()
                    .race(nothing(), state)
                    .await()
                    .indefinitely());
    assertEquals(
        FiscalPreparationFailure.CommitKnowledge.CONFIRMED_ROLLBACK,
        failure.failure().commitKnowledge());
  }

  @Test
  void requestEntryDateRemainsFixedAcrossEcuadorMidnightAndBudgetsOnlyDecrease() {
    AtomicLong ticker = new AtomicLong(100L);
    RequestContext context =
        new RequestContext(
            instant("2026-07-19T04:59:59.999Z"),
            date(),
            RequestDeadline.start(requireNonNull(Duration.ofNanos(20)), ticker::get));
    ticker.set(119L);
    assertEquals(LocalDate.of(2026, 7, 18), context.ecuadorDate());
    assertEquals(Duration.ofNanos(1), context.deadline().remaining());
  }

  private static FiscalPreparationRequestState state(AtomicLong ticker, long budgetNanos) {
    FiscalPreparationRequestState state = new FiscalPreparationRequestState();
    state.initialize(
        new RequestContext(
            instant("2026-07-18T12:00:00Z"),
            date(),
            RequestDeadline.start(requireNonNull(Duration.ofNanos(budgetNanos)), ticker::get)),
        "corr-1",
        new FiscalPreparationCommitTracker());
    return state;
  }

  private static <T> Uni<T> item(T value) {
    return requireNonNull(Uni.createFrom().item(value));
  }

  private static Uni<Object> nothing() {
    return requireNonNull(Uni.createFrom().nothing());
  }

  private static Instant instant(String value) {
    return requireNonNull(Instant.parse(value));
  }

  private static LocalDate date() {
    return requireNonNull(LocalDate.of(2026, 7, 18));
  }
}
