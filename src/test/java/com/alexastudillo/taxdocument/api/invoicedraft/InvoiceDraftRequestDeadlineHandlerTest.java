package com.alexastudillo.taxdocument.api.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alexastudillo.taxdocument.api.invoicedraft.telemetry.InvoiceDraftTelemetry;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftApplicationException;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftFailure;
import com.alexastudillo.taxdocument.application.invoicedraft.RequestDeadline;
import com.alexastudillo.taxdocument.support.FixedRequestClock;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

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
        Instant.EPOCH,
        RequestDeadline.start(Duration.ofNanos(10), applicationTicker::get),
        "application-first",
        System.nanoTime());
    assertEquals(
        "accepted",
        handler.race(Uni.createFrom().item("accepted"), applicationState).await().indefinitely());
    applicationTicker.set(10L);
    assertFalse(applicationState.acceptTerminal());

    AtomicLong deadlineTicker = new AtomicLong();
    InvoiceDraftRequestState deadlineState = new InvoiceDraftRequestState();
    deadlineState.initialize(
        Instant.EPOCH,
        RequestDeadline.start(Duration.ofNanos(1), deadlineTicker::get),
        "deadline-first",
        System.nanoTime());
    deadlineTicker.set(1L);
    InvoiceDraftApplicationException failure =
        assertThrows(
            InvoiceDraftApplicationException.class,
            () ->
                handler
                    .race(Uni.createFrom().nothing(), deadlineState)
                    .await()
                    .atMost(Duration.ofSeconds(1)));
    assertEquals(InvoiceDraftFailure.Code.REQUEST_TIMEOUT, failure.failure().code());
    assertFalse(deadlineState.acceptTerminal());
  }
}
