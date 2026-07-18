package com.alexastudillo.taxdocument.api.invoicedraft;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/** Non-blocking header gate executed before request entity consumption. */
@ApplicationScoped
public final class InvoiceDraftRequestGateFilter {
  private final InvoiceDraftRequestDeadlineHandler deadlineHandler;
  private final CompanyContextHeader companyHeader;
  private final IdempotencyKeyHeader idempotencyHeader;
  private final CorrelationHeader correlationHeader;
  private final InvoiceDraftRequestState state;

  public InvoiceDraftRequestGateFilter(
      InvoiceDraftRequestDeadlineHandler deadlineHandler,
      CompanyContextHeader companyHeader,
      IdempotencyKeyHeader idempotencyHeader,
      CorrelationHeader correlationHeader,
      InvoiceDraftRequestState state) {
    this.deadlineHandler = deadlineHandler;
    this.companyHeader = companyHeader;
    this.idempotencyHeader = idempotencyHeader;
    this.correlationHeader = correlationHeader;
    this.state = state;
  }

  @InvoiceDraftRequestGate
  @ServerRequestFilter(priority = Integer.MIN_VALUE, nonBlocking = true)
  public void filter(ContainerRequestContext context) {
    CorrelationHeader.Classification correlation =
        correlationHeader.classify(context.getHeaders().get("X-Correlation-Id"));
    deadlineHandler.initialize(state, correlation.safeValue());
    state.companyId(companyHeader.parse(context.getHeaders().get("X-Company-Id")));
    if (!correlation.validInput()) {
      throw new ProblemDetails.ApiException(400, "INVALID_REQUEST", "X-Correlation-Id is invalid");
    }
    state.idempotencyKey(idempotencyHeader.parse(context.getHeaders().get("Idempotency-Key")));
  }
}
