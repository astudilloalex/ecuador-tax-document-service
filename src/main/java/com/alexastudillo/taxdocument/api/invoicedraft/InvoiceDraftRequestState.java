package com.alexastudillo.taxdocument.api.invoicedraft;

import com.alexastudillo.taxdocument.application.requestcontext.RequestDeadline;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import jakarta.enterprise.context.RequestScoped;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Request-local API state populated before entity consumption. */
@NullMarked
@RequestScoped
public class InvoiceDraftRequestState {
  private AtomicBoolean terminalAccepted = new AtomicBoolean();
  private @Nullable Instant requestCreationInstant;
  private @Nullable RequestDeadline deadline;
  private @Nullable String correlationId;
  private @Nullable CompanyId companyId;
  private @Nullable String idempotencyKey;
  private long startedNanos;
  private @Nullable Integer acceptedStatus;

  public void initialize(
      Instant instant, RequestDeadline requestDeadline, String safeCorrelationId, long startNanos) {
    initialize(instant, requestDeadline, safeCorrelationId, startNanos, new AtomicBoolean());
  }

  void initialize(
      Instant instant,
      RequestDeadline requestDeadline,
      String safeCorrelationId,
      long startNanos,
      AtomicBoolean sharedTerminal) {
    requestCreationInstant = instant;
    deadline = requestDeadline;
    correlationId = safeCorrelationId;
    startedNanos = startNanos;
    terminalAccepted = Objects.requireNonNull(sharedTerminal, "sharedTerminal");
  }

  public boolean acceptTerminal() {
    return terminalAccepted.compareAndSet(false, true);
  }

  public Instant requestCreationInstant() {
    return Objects.requireNonNull(requestCreationInstant, "requestCreationInstant");
  }

  public RequestDeadline deadline() {
    return Objects.requireNonNull(deadline, "deadline");
  }

  public String correlationId() {
    return Objects.requireNonNull(correlationId, "correlationId");
  }

  public @Nullable String correlationIdOrNull() {
    return correlationId;
  }

  public CompanyId companyId() {
    return Objects.requireNonNull(companyId, "companyId");
  }

  public @Nullable CompanyId companyIdOrNull() {
    return companyId;
  }

  public void companyId(CompanyId value) {
    companyId = value;
  }

  public String idempotencyKey() {
    return Objects.requireNonNull(idempotencyKey, "idempotencyKey");
  }

  public void idempotencyKey(String value) {
    idempotencyKey = value;
  }

  public long startedNanos() {
    return startedNanos;
  }

  public @Nullable Integer acceptedStatus() {
    return acceptedStatus;
  }

  public void acceptedStatus(int value) {
    acceptedStatus = value;
  }
}
