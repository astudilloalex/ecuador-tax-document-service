package com.alexastudillo.taxdocument.api.invoicedraft;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alexastudillo.taxdocument.api.invoicedraft.telemetry.InvoiceDraftTelemetry;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftApplicationException;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftFailure;
import com.alexastudillo.taxdocument.application.requestcontext.RequestDeadline;
import com.alexastudillo.taxdocument.support.FixedRequestClock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class InvoiceDraftRequestDeadlineHandlerTest {
  @Test
  void firstConclusiveApplicationOrDeadlineOutcomeWinsExactlyOnce() {
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    InvoiceDraftTelemetry telemetry = new InvoiceDraftTelemetry(meters);
    InvoiceDraftRequestDeadlineHandler handler =
        new InvoiceDraftRequestDeadlineHandler(new FixedRequestClock(), telemetry);

    AtomicLong applicationTicker = new AtomicLong();
    InvoiceDraftRequestState applicationState = new InvoiceDraftRequestState();
    applicationState.initialize(
        requireNonNull(Instant.EPOCH),
        RequestDeadline.start(requireNonNull(Duration.ofNanos(10)), applicationTicker::get),
        "application-first",
        System.nanoTime());
    assertEquals(
        "accepted", handler.race(item("accepted"), applicationState).await().indefinitely());
    applicationTicker.set(10L);
    assertFalse(applicationState.acceptTerminal());

    AtomicLong deadlineTicker = new AtomicLong();
    InvoiceDraftRequestState deadlineState = new InvoiceDraftRequestState();
    deadlineState.initialize(
        requireNonNull(Instant.EPOCH),
        RequestDeadline.start(requireNonNull(Duration.ofNanos(1)), deadlineTicker::get),
        "deadline-first",
        System.nanoTime());
    deadlineTicker.set(1L);
    InvoiceDraftApplicationException failure =
        assertThrows(
            InvoiceDraftApplicationException.class,
            () ->
                handler
                    .race(requireNonNull(Uni.createFrom().nothing()), deadlineState)
                    .await()
                    .atMost(Duration.ofSeconds(1)));
    assertEquals(InvoiceDraftFailure.Code.REQUEST_TIMEOUT, failure.failure().code());
    assertFalse(deadlineState.acceptTerminal());
  }

  private static <T> Uni<T> item(T value) {
    return requireNonNull(Uni.createFrom().item(value));
  }
}
