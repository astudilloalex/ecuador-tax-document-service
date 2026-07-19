package com.alexastudillo.taxdocument.api.invoicedraft;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Complete externally visible Invoice Draft representation. */
@NullMarked
public record InvoiceDraftResponse(
    UUID id,
    UUID companyId,
    UUID emissionPointId,
    LocalDate emissionDate,
    String status,
    String currency,
    BuyerResponse buyer,
    List<@NonNull LineResponse> lines,
    List<@NonNull TaxResponse> taxTotals,
    List<@NonNull PaymentResponse> payments,
    List<@NonNull AdditionalInformationResponse> additionalInformation,
    @JsonFormat(shape = JsonFormat.Shape.STRING) @Nullable BigDecimal subtotalBeforeTaxes,
    @JsonFormat(shape = JsonFormat.Shape.STRING) @Nullable BigDecimal totalDiscount,
    @JsonFormat(shape = JsonFormat.Shape.STRING) @Nullable BigDecimal grandTotal,
    Instant createdAt,
    Instant updatedAt) {
  public record BuyerResponse(
      String identificationType,
      String identification,
      String legalName,
      @Nullable String address,
      @Nullable String email,
      @Nullable String telephone) {}

  public record LineResponse(
      int position,
      String productCode,
      String description,
      @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal quantity,
      @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal unitPrice,
      @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal discount,
      @JsonFormat(shape = JsonFormat.Shape.STRING) @Nullable BigDecimal grossAmount,
      @JsonFormat(shape = JsonFormat.Shape.STRING) @Nullable BigDecimal netAmount,
      LineTaxResponse tax,
      @JsonFormat(shape = JsonFormat.Shape.STRING) @Nullable BigDecimal lineTotal) {}

  public record LineTaxResponse(
      UUID taxRuleId,
      String family,
      String treatment,
      String officialTaxCode,
      String officialPercentageCode,
      @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal rate,
      @JsonFormat(shape = JsonFormat.Shape.STRING) @Nullable BigDecimal base,
      @JsonFormat(shape = JsonFormat.Shape.STRING) @Nullable BigDecimal amount,
      String catalogVersion) {}

  public record TaxResponse(
      String family,
      String treatment,
      String officialTaxCode,
      String officialPercentageCode,
      @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal rate,
      @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal base,
      @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
      String catalogVersion) {}

  public record PaymentResponse(
      UUID paymentMethodId,
      String officialCode,
      String name,
      @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
      String catalogVersion) {}

  public record AdditionalInformationResponse(String name, String value) {}
}
