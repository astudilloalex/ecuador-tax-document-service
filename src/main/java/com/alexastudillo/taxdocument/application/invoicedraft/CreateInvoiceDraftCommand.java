package com.alexastudillo.taxdocument.application.invoicedraft;

import com.alexastudillo.taxdocument.application.requestcontext.RequestDeadline;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Decoded request data passed from the API boundary without business-text normalization. */
@NullMarked
public record CreateInvoiceDraftCommand(
    CompanyId companyId,
    Instant requestCreationInstant,
    RequestDeadline deadline,
    String idempotencyKey,
    String correlationId,
    String emissionPointId,
    LocalDate emissionDate,
    BuyerInput buyer,
    List<@NonNull LineInput> lines,
    List<@NonNull PaymentInput> payments,
    List<@NonNull AdditionalInformationInput> additionalInformation) {
  public CreateInvoiceDraftCommand {
    Objects.requireNonNull(companyId, "companyId");
    Objects.requireNonNull(requestCreationInstant, "requestCreationInstant");
    Objects.requireNonNull(deadline, "deadline");
    Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    Objects.requireNonNull(correlationId, "correlationId");
    Objects.requireNonNull(emissionPointId, "emissionPointId");
    Objects.requireNonNull(emissionDate, "emissionDate");
    Objects.requireNonNull(buyer, "buyer");
    lines = Objects.requireNonNull(List.copyOf(lines));
    payments = Objects.requireNonNull(List.copyOf(payments));
    additionalInformation =
        additionalInformation == null
            ? Objects.requireNonNull(List.<@NonNull AdditionalInformationInput>of())
            : Objects.requireNonNull(List.copyOf(additionalInformation));
  }

  public record BuyerInput(
      String identificationType,
      String identification,
      String legalName,
      @Nullable String address,
      @Nullable String email,
      @Nullable String telephone) {}

  public record LineInput(
      String productCode,
      String description,
      BigDecimal quantity,
      BigDecimal unitPrice,
      BigDecimal discount,
      UUID taxRuleId) {}

  public record PaymentInput(UUID paymentMethodId, BigDecimal amount) {}

  public record AdditionalInformationInput(
      String name, @Nullable String canonicalName, String value) {
    public AdditionalInformationInput(String name, String value) {
      this(name, null, value);
    }
  }
}
