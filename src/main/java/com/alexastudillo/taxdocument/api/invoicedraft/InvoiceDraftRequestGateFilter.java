package com.alexastudillo.taxdocument.api.invoicedraft;

import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/** Non-blocking header gate executed before request entity consumption. */
@ApplicationScoped
public final class InvoiceDraftRequestGateFilter {
  private final InvoiceDraftRequestDeadlineHandler deadlineHandler;
  private final InvoiceDraftRequestBoundary requestBoundary;
  private final CompanyContextHeader companyHeader;
  private final IdempotencyKeyHeader idempotencyHeader;
  private final InvoiceDraftRequestState state;
  private final RoutingContext routingContext;

  public InvoiceDraftRequestGateFilter(
      InvoiceDraftRequestDeadlineHandler deadlineHandler,
      InvoiceDraftRequestBoundary requestBoundary,
      CompanyContextHeader companyHeader,
      IdempotencyKeyHeader idempotencyHeader,
      InvoiceDraftRequestState state,
      RoutingContext routingContext) {
    this.deadlineHandler = deadlineHandler;
    this.requestBoundary = requestBoundary;
    this.companyHeader = companyHeader;
    this.idempotencyHeader = idempotencyHeader;
    this.state = state;
    this.routingContext = routingContext;
  }

  @InvoiceDraftRequestGate
  @ServerRequestFilter(priority = Integer.MIN_VALUE, nonBlocking = true)
  public void filter(ContainerRequestContext context) {
    InvoiceDraftRequestBoundary.BoundaryState boundary = requestBoundary.state(routingContext);
    CorrelationHeader.Classification correlation = boundary.correlation();
    deadlineHandler.initialize(state, boundary);
    state.companyId(companyHeader.parse(context.getHeaders().get("X-Company-Id")));
    if (!correlation.validInput()) {
      throw new ProblemDetails.ApiException(400, "INVALID_REQUEST", "X-Correlation-Id is invalid");
    }
    state.idempotencyKey(idempotencyHeader.parse(context.getHeaders().get("Idempotency-Key")));
  }
}
