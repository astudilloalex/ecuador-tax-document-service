package com.alexastudillo.taxdocument.api.invoicedraft;

import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftApplicationException;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftFailure;
import com.alexastudillo.taxdocument.application.invoicedraft.RequestClock;
import com.alexastudillo.taxdocument.application.invoicedraft.RequestDeadline;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.eclipse.microprofile.config.ConfigProvider;

/** Exclusive API request-time, deadline-race, and terminal-outcome owner. */
@ApplicationScoped
public final class InvoiceDraftRequestDeadlineHandler {
  private final RequestClock clock;
  private final InvoiceDraftTelemetryPort telemetry;
  private final Duration requestDeadline;

  public InvoiceDraftRequestDeadlineHandler(
      RequestClock clock, InvoiceDraftTelemetryPort telemetry) {
    this.clock = clock;
    this.telemetry = telemetry;
    requestDeadline =
        ConfigProvider.getConfig().getValue("invoice-draft.request-deadline", Duration.class);
  }

  public void initialize(InvoiceDraftRequestState state, String safeCorrelationId) {
    long started = System.nanoTime();
    Instant requestInstant = clock.requestTime();
    state.initialize(
        requestInstant, RequestDeadline.start(requestDeadline), safeCorrelationId, started);
  }

  public <T> Uni<T> race(Uni<T> application, InvoiceDraftRequestState state) {
    Uni<T> timeout =
        Uni.createFrom()
            .voidItem()
            .onItem()
            .delayIt()
            .by(state.deadline().remaining())
            .onItem()
            .transformToUni(ignored -> Uni.createFrom().failure(timeoutFailure()));
    return Uni.join()
        .first(application, timeout)
        .toTerminate()
        .onItemOrFailure()
        .transformToUni(
            (item, failure) -> {
              if (!state.acceptTerminal()) {
                telemetry.lateOutcome(state, "discarded");
                return Uni.createFrom().nothing();
              }
              if (failure != null) {
                return Uni.createFrom().failure(failure);
              }
              return Uni.createFrom().item(item);
            });
  }

  private static InvoiceDraftApplicationException timeoutFailure() {
    return new InvoiceDraftApplicationException(
        new InvoiceDraftFailure(
            InvoiceDraftFailure.Code.REQUEST_TIMEOUT,
            "The Invoice Draft request exceeded its deadline",
            true,
            List.of()));
  }
}
