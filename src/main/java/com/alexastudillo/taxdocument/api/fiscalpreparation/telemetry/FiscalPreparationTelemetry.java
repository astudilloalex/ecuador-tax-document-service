package com.alexastudillo.taxdocument.api.fiscalpreparation.telemetry;

import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationFailure;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Objects;
import org.jboss.logging.Logger;

/** Metrics implementation restricted to stable bounded outcome and commit-knowledge labels. */
@ApplicationScoped
public final class FiscalPreparationTelemetry implements FiscalPreparationTelemetryPort {
  private static final Logger LOG = Logger.getLogger(FiscalPreparationTelemetry.class);
  private final MeterRegistry registry;

  public FiscalPreparationTelemetry(MeterRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  @Override
  public void completed(String safeCorrelationId, boolean replayed) {
    Objects.requireNonNull(safeCorrelationId, "safeCorrelationId");
    String outcome = replayed ? "replayed" : "committed";
    registry.counter("fiscal_preparation_requests_total", "outcome", outcome).increment();
    Span.current().addEvent("fiscal_preparation." + outcome);
    LOG.infof(
        "fiscal_preparation_audit correlationId=%s outcome=%s rule=SRI-OFFLINE-2.33 policy=SECURE_RANDOM_8_V1",
        safeCorrelationId, outcome);
  }

  @Override
  public void failed(String safeCorrelationId, FiscalPreparationFailure failure) {
    Objects.requireNonNull(safeCorrelationId, "safeCorrelationId");
    Objects.requireNonNull(failure, "failure");
    registry
        .counter(
            "fiscal_preparation_requests_total",
            "outcome",
            outcome(failure.code()),
            "commit_knowledge",
            failure.commitKnowledge().name().toLowerCase(java.util.Locale.ROOT))
        .increment();
    Span.current().addEvent("fiscal_preparation." + outcome(failure.code()));
    LOG.infof(
        "fiscal_preparation_audit correlationId=%s outcome=%s commitKnowledge=%s",
        safeCorrelationId, outcome(failure.code()), failure.commitKnowledge().name());
  }

  private static String outcome(FiscalPreparationFailure.Code code) {
    return switch (code) {
      case OFFICIAL_SEQUENCE_BASELINE_MISSING,
          OFFICIAL_SEQUENCE_BASELINE_INVALID,
          OFFICIAL_SEQUENCE_EXHAUSTED ->
          "baseline_failure";
      case REQUEST_TIMEOUT -> "timeout";
      case PREPARATION_OUTCOME_UNKNOWN -> "unknown";
      case PERSISTENCE_FAILURE -> "rollback";
      default -> "rejected";
    };
  }
}
