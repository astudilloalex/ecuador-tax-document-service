package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.AccessKeyGenerator;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalContextSnapshot;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Replay-first orchestration with complete external validation before the local transaction. */
@ApplicationScoped
@NullMarked
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
  public Uni<@NonNull PrepareInvoiceForFiscalIssuanceResult> prepare(
      PrepareInvoiceForFiscalIssuanceCommand command) {
    Objects.requireNonNull(command, "command");
    Duration remaining = requireRemaining(command);
    return requireResult(
        store
            .lookup(command.companyId(), command.invoiceDraftId(), remaining)
            .onItem()
            .transformToUni(
                lookup ->
                    afterLookup(
                        command, Objects.requireNonNull(lookup, "fiscal preparation lookup"))),
        "fiscal preparation lookup result");
  }

  private Uni<@NonNull PrepareInvoiceForFiscalIssuanceResult> afterLookup(
      PrepareInvoiceForFiscalIssuanceCommand command, FiscalPreparationLookup lookup) {
    if (lookup instanceof FiscalPreparationLookup.Existing existing) {
      return requireResult(
          Uni.createFrom()
              .item(new PrepareInvoiceForFiscalIssuanceResult(existing.preparation(), true)),
          "existing preparation result");
    }
    if (lookup instanceof FiscalPreparationLookup.NotFound) {
      return requireResult(
          Uni.createFrom().failure(failure(FiscalPreparationFailure.Code.INVOICE_DRAFT_NOT_FOUND)),
          "draft not found result");
    }
    if (lookup instanceof FiscalPreparationLookup.NotPreparable) {
      return requireResult(
          Uni.createFrom()
              .failure(failure(FiscalPreparationFailure.Code.INVOICE_DRAFT_NOT_PREPARABLE)),
          "draft not preparable result");
    }
    FiscalPreparationLookup.EligibleDraft eligible = (FiscalPreparationLookup.EligibleDraft) lookup;
    return prepareFirst(command, eligible.draft());
  }

  private Uni<@NonNull PrepareInvoiceForFiscalIssuanceResult> prepareFirst(
      PrepareInvoiceForFiscalIssuanceCommand command, InvoiceDraftPreparationView draft) {
    if (!draft.companyId().equals(command.companyId())
        || !draft.invoiceDraftId().equals(command.invoiceDraftId())) {
      return requireResult(
          Uni.createFrom().failure(failure(FiscalPreparationFailure.Code.INVOICE_DRAFT_NOT_FOUND)),
          "draft not found failure");
    }
    if (!draft.emissionDate().equals(command.requestContext().ecuadorDate())) {
      return requireResult(
          Uni.createFrom().failure(failure(FiscalPreparationFailure.Code.EMISSION_DATE_STALE)),
          "stale emission date failure");
    }
    FiscalContextPort.Request request =
        new FiscalContextPort.Request(
            command.companyId(),
            draft.emissionPointId(),
            draft.emissionDate(),
            AccessKeyGenerator.INVOICE_DOCUMENT_TYPE,
            command.safeCorrelationId(),
            requireRemaining(command));
    return requireResult(
        fiscalContext
            .resolve(request)
            .onItem()
            .transform(
                resolution ->
                    validator.validate(
                        Objects.requireNonNull(resolution, "fiscal context resolution"),
                        draft.emissionPointId(),
                        draft.emissionDate()))
            .onItem()
            .transformToUni(
                snapshot ->
                    commit(
                        command,
                        draft,
                        Objects.requireNonNull(snapshot, "fiscal context snapshot"))),
        "fiscal preparation result");
  }

  private Uni<@NonNull PrepareInvoiceForFiscalIssuanceResult> commit(
      PrepareInvoiceForFiscalIssuanceCommand command,
      InvoiceDraftPreparationView draft,
      FiscalContextSnapshot snapshot) {
    return requireResult(
        store
            .commit(
                new FiscalPreparationCommitIntent(draft, snapshot),
                requireRemaining(command),
                command.commitTracker())
            .onItem()
            .transform(
                result -> {
                  FiscalPreparationCommitResult required =
                      Objects.requireNonNull(result, "commit result");
                  if (required instanceof FiscalPreparationCommitResult.Created created) {
                    return new PrepareInvoiceForFiscalIssuanceResult(created.preparation(), false);
                  }
                  FiscalPreparationCommitResult.Replay replay =
                      (FiscalPreparationCommitResult.Replay) required;
                  return new PrepareInvoiceForFiscalIssuanceResult(replay.preparation(), true);
                }),
        "commit result");
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

  private static <T> @NonNull T requireResult(@Nullable T value, String field) {
    return Objects.requireNonNull(value, field);
  }
}
