package com.alexastudillo.taxdocument.api.invoicedraft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Strict client-controlled request representation; Company context is intentionally absent. */
@NullMarked
@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateInvoiceDraftRequest(
    String emissionPointId,
    LocalDate emissionDate,
    BuyerRequest buyer,
    List<LineRequest> lines,
    List<PaymentRequest> payments,
    @Nullable List<AdditionalInformationRequest> additionalInformation) {
  @JsonIgnoreProperties(ignoreUnknown = false)
  public record BuyerRequest(
      String identificationType,
      String identification,
      String legalName,
      @Nullable String address,
      @Nullable String email,
      @Nullable String telephone) {}

  @JsonIgnoreProperties(ignoreUnknown = false)
  public record LineRequest(
      String productCode,
      String description,
      String quantity,
      String unitPrice,
      String discount,
      String taxRuleId) {}

  @JsonIgnoreProperties(ignoreUnknown = false)
  public record PaymentRequest(String paymentMethodId, String amount) {}

  @JsonIgnoreProperties(ignoreUnknown = false)
  public record AdditionalInformationRequest(String name, String value) {}
}
