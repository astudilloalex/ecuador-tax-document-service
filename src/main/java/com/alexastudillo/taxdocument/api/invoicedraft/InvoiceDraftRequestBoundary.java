package com.alexastudillo.taxdocument.api.invoicedraft;

import com.alexastudillo.taxdocument.api.invoicedraft.telemetry.InvoiceDraftTelemetryPort;
import com.alexastudillo.taxdocument.api.problem.ProblemDetails;
import com.alexastudillo.taxdocument.api.requestcontext.CorrelationHeader;
import com.alexastudillo.taxdocument.application.requestcontext.RequestClock;
import com.alexastudillo.taxdocument.application.requestcontext.RequestDeadline;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.vertx.http.runtime.RouteConstants;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Captures immutable request time, deadline, and safe correlation before body consumption. */
@NullMarked
@ApplicationScoped
public final class InvoiceDraftRequestBoundary {
  private static final String PATH = "/api/v1/invoice-drafts";
  private static final String STATE_KEY = InvoiceDraftRequestBoundary.class.getName() + ".state";
  private static final String PROBLEM_JSON = "application/problem+json";

  private final RequestClock clock;
  private final CorrelationHeader correlationHeader;
  private final ObjectMapper objectMapper;
  private final InvoiceDraftTelemetryPort telemetry;
  private final Duration requestDeadline;

  public InvoiceDraftRequestBoundary(
      RequestClock clock,
      CorrelationHeader correlationHeader,
      ObjectMapper objectMapper,
      InvoiceDraftTelemetryPort telemetry) {
    this.clock = clock;
    this.correlationHeader = correlationHeader;
    this.objectMapper = objectMapper;
    this.telemetry = telemetry;
    requestDeadline =
        ConfigProvider.getConfig().getValue("invoice-draft.request-deadline", Duration.class);
  }

  void install(@Observes Router router) {
    router
        .route(HttpMethod.POST, PATH)
        .order(RouteConstants.ROUTE_ORDER_BODY_HANDLER - 1)
        .handler(
            context -> {
              BoundaryState state = capture(context);
              context.put(STATE_KEY, state);
              long timerId =
                  context
                      .vertx()
                      .setTimer(
                          Math.max(1L, requestDeadline.toMillis()),
                          ignored -> deadlineReached(context, state));
              state.timerId(timerId);
              context.addEndHandler(ignored -> context.vertx().cancelTimer(state.timerId()));
              context.next();
            });
  }

  BoundaryState state(RoutingContext context) {
    BoundaryState state = context.get(STATE_KEY);
    if (state == null) {
      state = capture(context);
      context.put(STATE_KEY, state);
    }
    return state;
  }

  private BoundaryState capture(RoutingContext context) {
    long startedNanos = System.nanoTime();
    Instant requestInstant = clock.requestTime();
    RequestDeadline deadline = RequestDeadline.start(requestDeadline);
    CorrelationHeader.Classification correlation =
        correlationHeader.classify(context.request().headers().getAll("X-Correlation-Id"));
    return new BoundaryState(requestInstant, deadline, correlation, startedNanos);
  }

  private void deadlineReached(RoutingContext context, BoundaryState state) {
    if (context.response().ended()) {
      return;
    }
    if (context.response().headWritten()) {
      state.acceptTerminal();
      if (state.requestState() != null) {
        telemetry.lateOutcome(state.requestState(), "discarded");
      }
      return;
    }
    if (!state.acceptTerminal()) {
      return;
    }
    ProblemDetails problem =
        new ProblemDetails(
            URI.create("urn:ecuador-tax-document-service:problem:request_timeout"),
            "Request timeout",
            504,
            "REQUEST_TIMEOUT",
            "The Invoice Draft request exceeded its deadline",
            URI.create(PATH),
            state.correlation().safeValue(),
            null);
    try {
      Buffer body = Buffer.buffer(objectMapper.writeValueAsBytes(problem));
      context
          .response()
          .setStatusCode(504)
          .putHeader(HttpHeaders.CONTENT_TYPE, PROBLEM_JSON)
          .putHeader(HttpHeaders.CONNECTION, "close")
          .putHeader("X-Correlation-Id", state.correlation().safeValue())
          .end(body)
          .onComplete(ignored -> context.request().connection().close());
    } catch (JsonProcessingException exception) {
      context.fail(exception);
    }
  }

  static final class BoundaryState {
    private final Instant requestInstant;
    private final RequestDeadline deadline;
    private final CorrelationHeader.Classification correlation;
    private final long startedNanos;
    private final AtomicBoolean terminalAccepted = new AtomicBoolean();
    private long timerId = -1L;
    private @Nullable InvoiceDraftRequestState requestState;

    private BoundaryState(
        Instant requestInstant,
        RequestDeadline deadline,
        CorrelationHeader.Classification correlation,
        long startedNanos) {
      this.requestInstant = requestInstant;
      this.deadline = deadline;
      this.correlation = correlation;
      this.startedNanos = startedNanos;
    }

    Instant requestInstant() {
      return requestInstant;
    }

    RequestDeadline deadline() {
      return deadline;
    }

    CorrelationHeader.Classification correlation() {
      return correlation;
    }

    long startedNanos() {
      return startedNanos;
    }

    AtomicBoolean terminalAccepted() {
      return terminalAccepted;
    }

    boolean acceptTerminal() {
      return terminalAccepted.compareAndSet(false, true);
    }

    long timerId() {
      return timerId;
    }

    void timerId(long value) {
      timerId = value;
    }

    @Nullable InvoiceDraftRequestState requestState() {
      return requestState;
    }

    void requestState(InvoiceDraftRequestState value) {
      requestState = value;
    }
  }
}
