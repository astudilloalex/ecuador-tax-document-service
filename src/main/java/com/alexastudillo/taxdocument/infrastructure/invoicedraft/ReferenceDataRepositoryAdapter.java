package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftApplicationException;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftFailure;
import com.alexastudillo.taxdocument.application.invoicedraft.ReferenceDataPort;
import com.alexastudillo.taxdocument.domain.invoicedraft.DraftValidationException;
import com.alexastudillo.taxdocument.domain.invoicedraft.TaxSelection;
import com.alexastudillo.taxdocument.infrastructure.persistence.ReactiveOperationBudget;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Reactive unscoped access to immutable global SRI reference catalogs. */
@NullMarked
@ApplicationScoped
public final class ReferenceDataRepositoryAdapter implements ReferenceDataPort {
  private final Duration operationTimeout;

  public ReferenceDataRepositoryAdapter() {
    @Nullable Duration configuredOperationTimeout =
        ConfigProvider.getConfig()
            .getValue("invoice-draft.persistence.operation-timeout", Duration.class);
    this.operationTimeout =
        requireHydrated(configuredOperationTimeout, "reference-data operation timeout");
  }

  @Override
  public Uni<@NonNull BuyerIdentificationRule> buyerIdentificationRule(
      String officialCode, Duration remaining) {
    if (remaining.isZero() || remaining.isNegative()) {
      return requireUni(Uni.createFrom().failure(deadlineExhausted()), "deadline failure");
    }
    Uni<@NonNull BuyerIdentificationTypeEntity> query =
        Panache.withSession(
            () ->
                BuyerIdentificationTypeEntity.find(
                        "officialCode = ?1 and active = true", officialCode)
                    .firstResult());
    return requireUni(
        bounded(requireHydrated(query, "buyer identification query"), remaining)
            .onItem()
            .ifNull()
            .failWith(
                () ->
                    new DraftValidationException(
                        "BUSINESS_VALIDATION_FAILED",
                        "buyer.identificationType",
                        "Buyer identification type is not approved"))
            .onItem()
            .<@NonNull BuyerIdentificationRule>transform(
                value ->
                    new BuyerIdentificationRule(
                        requireHydrated(value.officialCode, "buyerIdentification.officialCode"),
                        requireHydrated(
                            value.catalogVersion, "buyerIdentification.catalogVersion"))),
        "buyer identification rule");
  }

  @Override
  public Uni<@NonNull TaxSelection> ivaRule(
      UUID taxRuleId, LocalDate emissionDate, Duration remaining) {
    if (remaining.isZero() || remaining.isNegative()) {
      return requireUni(Uni.createFrom().failure(deadlineExhausted()), "deadline failure");
    }
    Uni<@NonNull IvaTaxRuleEntity> query =
        Panache.withSession(
            () ->
                IvaTaxRuleEntity.find(
                        "id = ?1 and active = true and targetValidFrom <= ?2 "
                            + "and (targetValidTo is null or targetValidTo >= ?2)",
                        taxRuleId,
                        emissionDate)
                    .firstResult());
    return requireUni(
        bounded(requireHydrated(query, "IVA rule query"), remaining)
            .onItem()
            .ifNull()
            .failWith(() -> invalidReference("lines[].taxRuleId", "IVA rule is not effective"))
            .onItem()
            .<@NonNull TaxSelection>transform(
                value ->
                    new TaxSelection(
                        requireHydrated(value.id, "ivaRule.id"),
                        requireHydrated(value.family, "ivaRule.family"),
                        TaxSelection.Treatment.valueOf(
                            Objects.requireNonNull(
                                requireHydrated(value.treatment, "ivaRule.treatment"),
                                "ivaRule.treatment")),
                        requireHydrated(value.officialTaxCode, "ivaRule.officialTaxCode"),
                        requireHydrated(
                            value.officialPercentageCode, "ivaRule.officialPercentageCode"),
                        requireHydrated(value.rate, "ivaRule.rate"),
                        requireHydrated(value.catalogVersion, "ivaRule.catalogVersion"),
                        value.active,
                        requireHydrated(value.targetValidFrom, "ivaRule.targetValidFrom"),
                        value.targetValidTo)),
        "IVA rule");
  }

  @Override
  public Uni<@NonNull PaymentMethod> paymentMethod(
      UUID paymentMethodId, LocalDate emissionDate, Duration remaining) {
    if (remaining.isZero() || remaining.isNegative()) {
      return requireUni(Uni.createFrom().failure(deadlineExhausted()), "deadline failure");
    }
    Uni<@NonNull PaymentMethodEntity> query =
        Panache.withSession(
            () ->
                PaymentMethodEntity.find(
                        "id = ?1 and active = true and targetValidFrom <= ?2 "
                            + "and (targetValidTo is null or targetValidTo >= ?2)",
                        paymentMethodId,
                        emissionDate)
                    .firstResult());
    return requireUni(
        bounded(requireHydrated(query, "payment method query"), remaining)
            .onItem()
            .ifNull()
            .failWith(
                () ->
                    invalidReference(
                        "payments[].paymentMethodId", "Payment method is not effective"))
            .onItem()
            .<@NonNull PaymentMethod>transform(
                value ->
                    new PaymentMethod(
                        requireHydrated(value.id, "paymentMethod.id"),
                        requireHydrated(value.officialCode, "paymentMethod.officialCode"),
                        requireHydrated(value.displayName, "paymentMethod.displayName"),
                        requireHydrated(value.catalogVersion, "paymentMethod.catalogVersion"))),
        "payment method");
  }

  private <T extends @NonNull Object> Uni<@NonNull T> bounded(
      Uni<@NonNull T> operation, Duration remaining) {
    if (remaining.isZero() || remaining.isNegative()) {
      return requireUni(Uni.createFrom().failure(deadlineExhausted()), "deadline failure");
    }
    ReactiveOperationBudget budget = ReactiveOperationBudget.clamp(remaining, operationTimeout);
    return requireUni(
        operation
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
                                emptyViolations())))
            .onFailure(
                throwable ->
                    !(throwable instanceof InvoiceDraftApplicationException)
                        && !(throwable instanceof DraftValidationException))
            .transform(
                _ ->
                    new InvoiceDraftApplicationException(
                        new InvoiceDraftFailure(
                            InvoiceDraftFailure.Code.PERSISTENCE_UNAVAILABLE,
                            "Reference data is temporarily unavailable",
                            true,
                            emptyViolations()))),
        "bounded reference-data operation");
  }

  private InvoiceDraftApplicationException deadlineExhausted() {
    return new InvoiceDraftApplicationException(
        new InvoiceDraftFailure(
            InvoiceDraftFailure.Code.REQUEST_TIMEOUT,
            "The remaining request budget is exhausted",
            true,
            emptyViolations()));
  }

  private static DraftValidationException invalidReference(String field, String message) {
    return new DraftValidationException("BUSINESS_VALIDATION_FAILED", field, message);
  }

  private static List<InvoiceDraftFailure.@NonNull Violation> emptyViolations() {
    return requireHydrated(List.of(), "empty violations");
  }

  private static <T> @NonNull T requireHydrated(@Nullable T value, String field) {
    return Objects.requireNonNull(value, field);
  }

  private static <T extends @NonNull Object> Uni<@NonNull T> requireUni(
      @Nullable Uni<@NonNull T> value, String field) {
    return Objects.requireNonNull(value, field);
  }
}
