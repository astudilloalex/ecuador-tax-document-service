package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalContextSnapshot;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Read-only authoritative fiscal-context boundary. */
public interface FiscalContextPort {
  Uni<FiscalContextResolution> resolve(Request request);

  record Request(
      CompanyId companyId,
      UUID emissionPointId,
      LocalDate emissionDate,
      String documentTypeCode,
      String safeCorrelationId,
      Duration remaining) {
    public Request {
      Objects.requireNonNull(companyId, "companyId");
      FiscalContextSnapshot.requireNonNil(emissionPointId, "emissionPointId");
      Objects.requireNonNull(emissionDate, "emissionDate");
      if (!"01".equals(documentTypeCode)) {
        throw new IllegalArgumentException("Fiscal context request supports invoices only");
      }
      Objects.requireNonNull(safeCorrelationId, "safeCorrelationId");
      Objects.requireNonNull(remaining, "remaining");
      if (remaining.isNegative() || remaining.isZero()) {
        throw new IllegalArgumentException("Fiscal context request budget is exhausted");
      }
    }
  }
}
