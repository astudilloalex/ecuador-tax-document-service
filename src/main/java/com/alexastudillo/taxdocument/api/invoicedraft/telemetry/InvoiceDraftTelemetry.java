package com.alexastudillo.taxdocument.api.invoicedraft.telemetry;

import com.alexastudillo.taxdocument.api.invoicedraft.InvoiceDraftRequestState;
import com.alexastudillo.taxdocument.application.invoicedraft.CreateInvoiceDraftResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.Objects;
import org.jboss.logging.Logger;

/** Bounded low-cardinality metrics and safe operational evidence. */
@ApplicationScoped
public final class InvoiceDraftTelemetry implements InvoiceDraftTelemetryPort {
  private static final Logger LOG = Logger.getLogger(InvoiceDraftTelemetry.class);
  private final MeterRegistry meters;

  public InvoiceDraftTelemetry(MeterRegistry meters) {
    this.meters = Objects.requireNonNull(meters, "meters");
  }

  @Override
  public void completed(
      InvoiceDraftRequestState state, CreateInvoiceDraftResult result, int selectedStatus) {
    String outcome = result.replayed() ? "replay" : "new";
    long elapsed = Math.max(0L, System.nanoTime() - state.startedNanos());
    Timer.builder("invoice_draft_request_duration")
        .tag("outcome", outcome)
        .register(Objects.requireNonNull(meters))
        .record(Objects.requireNonNull(Duration.ofNanos(elapsed)));
    meters.counter("invoice_draft_requests", "outcome", outcome).increment();
    Span.current().setAttribute("invoice.draft.outcome", outcome);
    Span.current().setAttribute("http.response.status_code", selectedStatus);
    LOG.infov(
        "invoice_draft_completed correlationId={0} outcome={1} status={2} draftId={3}",
        state.correlationId(), outcome, selectedStatus, result.draft().id());
  }

  @Override
  public void lateOutcome(InvoiceDraftRequestState state, String outcome) {
    long elapsed = Math.max(0L, System.nanoTime() - state.startedNanos());
    LOG.warnv(
        "request_deadline_exceeded_after_response_commit correlationId={0} operation={1} "
            + "status={2} elapsedNanos={3} companyId={4}",
        state.correlationId(),
        "create_invoice_draft",
        state.acceptedStatus(),
        elapsed,
        state.companyId() == null ? null : state.companyId().value());
    meters
        .counter("invoice_draft_late_outcomes", "outcome", Objects.requireNonNull(outcome))
        .increment();
    meters.counter("request_deadline_exceeded_after_response_commit").increment();
  }

  @Override
  public void failed(InvoiceDraftRequestState state, String outcome, int selectedStatus) {
    long elapsed = elapsed(state);
    Timer.builder("invoice_draft_request_duration")
        .tag("outcome", Objects.requireNonNull(outcome))
        .register(Objects.requireNonNull(meters))
        .record(Objects.requireNonNull(Duration.ofNanos(elapsed)));
    meters.counter("invoice_draft_requests", "outcome", outcome).increment();
    Span.current().setAttribute("invoice.draft.outcome", outcome);
    Span.current().setAttribute("http.response.status_code", selectedStatus);
    LOG.warnv(
        "invoice_draft_failed correlationId={0} outcome={1} status={2} elapsedNanos={3}",
        state.correlationId(), outcome, selectedStatus, elapsed);
  }

  private static long elapsed(InvoiceDraftRequestState state) {
    long started = state.startedNanos();
    return started <= 0L ? 0L : Math.max(0L, System.nanoTime() - started);
  }
}
