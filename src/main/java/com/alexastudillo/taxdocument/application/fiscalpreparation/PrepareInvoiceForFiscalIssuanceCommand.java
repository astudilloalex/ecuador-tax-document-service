package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.application.requestcontext.RequestContext;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalContextSnapshot;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import java.util.Objects;
import java.util.UUID;

/** Bodyless natural-identity preparation command. */
public record PrepareInvoiceForFiscalIssuanceCommand(
    CompanyId companyId,
    UUID invoiceDraftId,
    String safeCorrelationId,
    RequestContext requestContext,
    FiscalPreparationCommitTracker commitTracker) {
  public PrepareInvoiceForFiscalIssuanceCommand {
    Objects.requireNonNull(companyId, "companyId");
    FiscalContextSnapshot.requireNonNil(invoiceDraftId, "invoiceDraftId");
    Objects.requireNonNull(safeCorrelationId, "safeCorrelationId");
    Objects.requireNonNull(requestContext, "requestContext");
    Objects.requireNonNull(commitTracker, "commitTracker");
  }
}
