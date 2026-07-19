package com.alexastudillo.taxdocument.api.fiscalpreparation;

import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationCommitTracker;
import com.alexastudillo.taxdocument.application.requestcontext.RequestContext;
import jakarta.enterprise.context.RequestScoped;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Request-local fixed context and exclusive terminal-outcome state. */
@NullMarked
@RequestScoped
public class FiscalPreparationRequestState {
  private final AtomicBoolean terminalAccepted = new AtomicBoolean();
  private @Nullable RequestContext requestContext;
  private @Nullable String correlationId;
  private @Nullable FiscalPreparationCommitTracker commitTracker;

  public void initialize(
      RequestContext context,
      String safeCorrelationId,
      FiscalPreparationCommitTracker requestCommitTracker) {
    requestContext = Objects.requireNonNull(context, "context");
    correlationId = Objects.requireNonNull(safeCorrelationId, "safeCorrelationId");
    commitTracker = Objects.requireNonNull(requestCommitTracker, "requestCommitTracker");
  }

  public RequestContext requestContext() {
    return Objects.requireNonNull(requestContext, "requestContext");
  }

  public String correlationId() {
    return Objects.requireNonNull(correlationId, "correlationId");
  }

  public FiscalPreparationCommitTracker commitTracker() {
    return Objects.requireNonNull(commitTracker, "commitTracker");
  }

  public String safeCorrelationOrGenerated() {
    String cid = correlationId;
    return cid == null ? Objects.requireNonNull(java.util.UUID.randomUUID().toString()) : cid;
  }

  public boolean acceptTerminal() {
    return terminalAccepted.compareAndSet(false, true);
  }
}
