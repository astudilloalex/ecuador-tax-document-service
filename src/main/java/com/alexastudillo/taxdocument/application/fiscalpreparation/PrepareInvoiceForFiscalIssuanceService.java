package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.AccessKeyGenerator;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalContextSnapshot;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.Objects;

/** Replay-first orchestration with complete external validation before the local transaction. */
@ApplicationScoped
public final class PrepareInvoiceForFiscalIssuanceService
    implements PrepareInvoiceForFiscalIssuanceUseCase {
  private final FiscalPreparationStore store;
  private final FiscalContextPort fiscalContext;
  private final FiscalContextValidator validator;

  public PrepareInvoiceForFiscalIssuanceService(
      FiscalPreparationStore store,
      FiscalContextPort fiscalContext,
      FiscalContextValidator validator) {
    this.store = Objects.requireNonNull(store, "store");
    this.fiscalContext = Objects.requireNonNull(fiscalContext, "fiscalContext");
    this.validator = Objects.requireNonNull(validator, "validator");
  }

  @Override
  public Uni<PrepareInvoiceForFiscalIssuanceResult> prepare(
      PrepareInvoiceForFiscalIssuanceCommand command) {
    Objects.requireNonNull(command, "command");
    Duration remaining = requireRemaining(command);
    return store
        .lookup(command.companyId(), command.invoiceDraftId(), remaining)
        .onItem()
        .transformToUni(lookup -> afterLookup(command, lookup));
  }

  private Uni<PrepareInvoiceForFiscalIssuanceResult> afterLookup(
      PrepareInvoiceForFiscalIssuanceCommand command, FiscalPreparationLookup lookup) {
    return switch (lookup) {
      case FiscalPreparationLookup.Existing existing ->
          Uni.createFrom()
              .item(new PrepareInvoiceForFiscalIssuanceResult(existing.preparation(), true));
      case FiscalPreparationLookup.NotFound ignored ->
          Uni.createFrom().failure(failure(FiscalPreparationFailure.Code.INVOICE_DRAFT_NOT_FOUND));
      case FiscalPreparationLookup.NotPreparable ignored ->
          Uni.createFrom()
              .failure(failure(FiscalPreparationFailure.Code.INVOICE_DRAFT_NOT_PREPARABLE));
      case FiscalPreparationLookup.EligibleDraft eligible ->
          prepareFirst(command, eligible.draft());
    };
  }

  private Uni<PrepareInvoiceForFiscalIssuanceResult> prepareFirst(
      PrepareInvoiceForFiscalIssuanceCommand command, InvoiceDraftPreparationView draft) {
    if (!draft.companyId().equals(command.companyId())
        || !draft.invoiceDraftId().equals(command.invoiceDraftId())) {
      return Uni.createFrom()
          .failure(failure(FiscalPreparationFailure.Code.INVOICE_DRAFT_NOT_FOUND));
    }
    if (!draft.emissionDate().equals(command.requestContext().ecuadorDate())) {
      return Uni.createFrom().failure(failure(FiscalPreparationFailure.Code.EMISSION_DATE_STALE));
    }
    FiscalContextPort.Request request =
        new FiscalContextPort.Request(
            command.companyId(),
            draft.emissionPointId(),
            draft.emissionDate(),
            AccessKeyGenerator.INVOICE_DOCUMENT_TYPE,
            command.safeCorrelationId(),
            requireRemaining(command));
    return fiscalContext
        .resolve(request)
        .onItem()
        .transform(
            resolution ->
                validator.validate(resolution, draft.emissionPointId(), draft.emissionDate()))
        .onItem()
        .transformToUni(snapshot -> commit(command, draft, snapshot));
  }

  private Uni<PrepareInvoiceForFiscalIssuanceResult> commit(
      PrepareInvoiceForFiscalIssuanceCommand command,
      InvoiceDraftPreparationView draft,
      FiscalContextSnapshot snapshot) {
    return store
        .commit(
            new FiscalPreparationCommitIntent(draft, snapshot),
            requireRemaining(command),
            command.commitTracker())
        .onItem()
        .transform(
            result ->
                switch (result) {
                  case FiscalPreparationCommitResult.Created created ->
                      new PrepareInvoiceForFiscalIssuanceResult(created.preparation(), false);
                  case FiscalPreparationCommitResult.Replay replay ->
                      new PrepareInvoiceForFiscalIssuanceResult(replay.preparation(), true);
                });
  }

  private static Duration requireRemaining(PrepareInvoiceForFiscalIssuanceCommand command) {
    Duration remaining = command.requestContext().deadline().remaining();
    if (remaining.isZero() || remaining.isNegative()) {
      throw failure(FiscalPreparationFailure.Code.REQUEST_TIMEOUT);
    }
    return remaining;
  }

  private static FiscalPreparationApplicationException failure(FiscalPreparationFailure.Code code) {
    return new FiscalPreparationApplicationException(FiscalPreparationFailure.of(code));
  }
}
