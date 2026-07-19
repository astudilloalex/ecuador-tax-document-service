package com.alexastudillo.taxdocument.api.fiscalpreparation.telemetry;

import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationFailure;
import org.jspecify.annotations.NullMarked;

/** Sanitized bounded telemetry boundary with no fiscal payload labels. */
@NullMarked
public interface FiscalPreparationTelemetryPort {
  void completed(String safeCorrelationId, boolean replayed);

  void failed(String safeCorrelationId, FiscalPreparationFailure failure);
}
