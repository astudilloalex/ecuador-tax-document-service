package com.alexastudillo.taxdocument.application.invoicedraft;

import com.alexastudillo.taxdocument.domain.invoicedraft.TaxSelection;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

/** Transport-neutral access to immutable global SRI reference catalogs. */
@NullMarked
public interface ReferenceDataPort {
  Uni<@NonNull BuyerIdentificationRule> buyerIdentificationRule(
      String officialCode, Duration remaining);

  Uni<@NonNull TaxSelection> ivaRule(UUID taxRuleId, LocalDate emissionDate, Duration remaining);

  Uni<@NonNull PaymentMethod> paymentMethod(
      UUID paymentMethodId, LocalDate emissionDate, Duration remaining);

  record BuyerIdentificationRule(String officialCode, String catalogVersion) {}

  record PaymentMethod(UUID id, String officialCode, String name, String catalogVersion) {}
}
