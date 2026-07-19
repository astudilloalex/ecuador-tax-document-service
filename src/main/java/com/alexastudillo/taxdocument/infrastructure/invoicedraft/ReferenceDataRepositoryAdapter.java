package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftApplicationException;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftFailure;
import com.alexastudillo.taxdocument.application.invoicedraft.ReferenceDataPort;
import com.alexastudillo.taxdocument.domain.invoicedraft.DraftValidationException;
import com.alexastudillo.taxdocument.domain.invoicedraft.TaxSelection;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.config.ConfigProvider;

/** Reactive unscoped access to immutable global SRI reference catalogs. */
@ApplicationScoped
public final class ReferenceDataRepositoryAdapter implements ReferenceDataPort {
  private final Duration operationTimeout;

  public ReferenceDataRepositoryAdapter() {
    this.operationTimeout =
        ConfigProvider.getConfig()
            .getValue("invoice-draft.persistence.operation-timeout", Duration.class);
  }

  @Override
  public Uni<BuyerIdentificationRule> buyerIdentificationRule(
      String officialCode, Duration remaining) {
    if (remaining.isZero() || remaining.isNegative()) {
      return Uni.createFrom().failure(deadlineExhausted());
    }
    Uni<BuyerIdentificationTypeEntity> query =
        Panache.withSession(
            () ->
                BuyerIdentificationTypeEntity.find(
                        "officialCode = ?1 and active = true", officialCode)
                    .firstResult());
    return bounded(query, remaining)
        .onItem()
        .ifNull()
        .failWith(
            () ->
                new DraftValidationException(
                    "BUSINESS_VALIDATION_FAILED",
                    "buyer.identificationType",
                    "Buyer identification type is not approved"))
        .onItem()
        .<BuyerIdentificationRule>transform(
            value -> new BuyerIdentificationRule(value.officialCode, value.catalogVersion));
  }

  @Override
  public Uni<TaxSelection> ivaRule(UUID taxRuleId, LocalDate emissionDate, Duration remaining) {
    if (remaining.isZero() || remaining.isNegative()) {
      return Uni.createFrom().failure(deadlineExhausted());
    }
    Uni<IvaTaxRuleEntity> query =
        Panache.withSession(
            () ->
                IvaTaxRuleEntity.find(
                        "id = ?1 and active = true and targetValidFrom <= ?2 "
                            + "and (targetValidTo is null or targetValidTo >= ?2)",
                        taxRuleId,
                        emissionDate)
                    .firstResult());
    return bounded(query, remaining)
        .onItem()
        .ifNull()
        .failWith(() -> invalidReference("lines[].taxRuleId", "IVA rule is not effective"))
        .onItem()
        .<TaxSelection>transform(
            value ->
                new TaxSelection(
                    value.id,
                    value.family,
                    TaxSelection.Treatment.valueOf(value.treatment),
                    value.officialTaxCode,
                    value.officialPercentageCode,
                    value.rate,
                    value.catalogVersion,
                    value.active,
                    value.targetValidFrom,
                    value.targetValidTo));
  }

  @Override
  public Uni<PaymentMethod> paymentMethod(
      UUID paymentMethodId, LocalDate emissionDate, Duration remaining) {
    if (remaining.isZero() || remaining.isNegative()) {
      return Uni.createFrom().failure(deadlineExhausted());
    }
    Uni<PaymentMethodEntity> query =
        Panache.withSession(
            () ->
                PaymentMethodEntity.find(
                        "id = ?1 and active = true and targetValidFrom <= ?2 "
                            + "and (targetValidTo is null or targetValidTo >= ?2)",
                        paymentMethodId,
                        emissionDate)
                    .firstResult());
    return bounded(query, remaining)
        .onItem()
        .ifNull()
        .failWith(
            () -> invalidReference("payments[].paymentMethodId", "Payment method is not effective"))
        .onItem()
        .<PaymentMethod>transform(
            value ->
                new PaymentMethod(
                    value.id, value.officialCode, value.displayName, value.catalogVersion));
  }

  private <T> Uni<T> bounded(Uni<T> operation, Duration remaining) {
    if (remaining.isZero() || remaining.isNegative()) {
      return Uni.createFrom().failure(deadlineExhausted());
    }
    ReactiveOperationBudget budget = ReactiveOperationBudget.clamp(remaining, operationTimeout);
    return operation
        .ifNoItem()
        .after(budget.timeout())
        .failWith(
            budget.timeoutOwner() == ReactiveOperationBudget.TimeoutOwner.REQUEST_DEADLINE
                ? this::deadlineExhausted
                : () ->
                    new InvoiceDraftApplicationException(
                        new InvoiceDraftFailure(
                            InvoiceDraftFailure.Code.PERSISTENCE_UNAVAILABLE,
                            "The reference-data operation timed out",
                            true,
                            List.of())))
        .onFailure(
            throwable ->
                !(throwable instanceof InvoiceDraftApplicationException)
                    && !(throwable instanceof DraftValidationException))
        .transform(
            throwable ->
                new InvoiceDraftApplicationException(
                    new InvoiceDraftFailure(
                        InvoiceDraftFailure.Code.PERSISTENCE_UNAVAILABLE,
                        "Reference data is temporarily unavailable",
                        true,
                        List.of())));
  }

  private InvoiceDraftApplicationException deadlineExhausted() {
    return new InvoiceDraftApplicationException(
        new InvoiceDraftFailure(
            InvoiceDraftFailure.Code.REQUEST_TIMEOUT,
            "The remaining request budget is exhausted",
            true,
            List.of()));
  }

  private static DraftValidationException invalidReference(String field, String message) {
    return new DraftValidationException("BUSINESS_VALIDATION_FAILED", field, message);
  }
}
