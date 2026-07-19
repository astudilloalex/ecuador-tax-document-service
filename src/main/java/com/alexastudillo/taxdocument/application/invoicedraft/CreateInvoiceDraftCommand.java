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
  public CreateInvoiceDraftCommand(
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
      @Nullable List<@NonNull AdditionalInformationInput> additionalInformation) {
    this.companyId = Objects.requireNonNull(companyId, "companyId");
    this.requestCreationInstant =
        Objects.requireNonNull(requestCreationInstant, "requestCreationInstant");
    this.deadline = Objects.requireNonNull(deadline, "deadline");
    this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
    this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
    this.emissionPointId = Objects.requireNonNull(emissionPointId, "emissionPointId");
    this.emissionDate = Objects.requireNonNull(emissionDate, "emissionDate");
    this.buyer = Objects.requireNonNull(buyer, "buyer");
    this.lines = Objects.requireNonNull(List.copyOf(lines));
    this.payments = Objects.requireNonNull(List.copyOf(payments));
    this.additionalInformation =
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
