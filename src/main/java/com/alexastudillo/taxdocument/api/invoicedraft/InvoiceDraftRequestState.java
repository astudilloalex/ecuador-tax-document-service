package com.alexastudillo.taxdocument.api.invoicedraft;

import com.alexastudillo.taxdocument.application.invoicedraft.RequestDeadline;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import jakarta.enterprise.context.RequestScoped;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/** Request-local API state populated before entity consumption. */
@RequestScoped
public class InvoiceDraftRequestState {
  private final AtomicBoolean terminalAccepted = new AtomicBoolean();
  private Instant requestCreationInstant;
  private RequestDeadline deadline;
  private String correlationId;
  private CompanyId companyId;
  private String idempotencyKey;
  private long startedNanos;
  private Integer acceptedStatus;

  public void initialize(
      Instant instant, RequestDeadline requestDeadline, String safeCorrelationId, long startNanos) {
    requestCreationInstant = instant;
    deadline = requestDeadline;
    correlationId = safeCorrelationId;
    startedNanos = startNanos;
  }

  public boolean acceptTerminal() {
    return terminalAccepted.compareAndSet(false, true);
  }

  public Instant requestCreationInstant() {
    return requestCreationInstant;
  }

  public RequestDeadline deadline() {
    return deadline;
  }

  public String correlationId() {
    return correlationId;
  }

  public CompanyId companyId() {
    return companyId;
  }

  public void companyId(CompanyId value) {
    companyId = value;
  }

  public String idempotencyKey() {
    return idempotencyKey;
  }

  public void idempotencyKey(String value) {
    idempotencyKey = value;
  }

  public long startedNanos() {
    return startedNanos;
  }

  public Integer acceptedStatus() {
    return acceptedStatus;
  }

  public void acceptedStatus(int value) {
    acceptedStatus = value;
  }
}
