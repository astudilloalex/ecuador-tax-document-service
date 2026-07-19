package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalContextSnapshot;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/** Minimal Company-scoped Invoice Draft projection needed for fiscal preparation. */
@NullMarked
public record InvoiceDraftPreparationView(
    UUID invoiceDraftId,
    CompanyId companyId,
    UUID emissionPointId,
    LocalDate emissionDate,
    String status) {
  public InvoiceDraftPreparationView {
    FiscalContextSnapshot.requireNonNil(invoiceDraftId, "invoiceDraftId");
    Objects.requireNonNull(companyId, "companyId");
    FiscalContextSnapshot.requireNonNil(emissionPointId, "emissionPointId");
    Objects.requireNonNull(emissionDate, "emissionDate");
    if (!"DRAFT".equals(status)) {
      throw new IllegalArgumentException("Preparation view must represent an eligible DRAFT");
    }
  }
}
