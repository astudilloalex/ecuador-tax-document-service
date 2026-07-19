package com.alexastudillo.taxdocument.api.invoicedraft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Strict client-controlled request representation; Company context is intentionally absent. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateInvoiceDraftRequest(
    String emissionPointId,
    LocalDate emissionDate,
    BuyerRequest buyer,
    List<LineRequest> lines,
    List<PaymentRequest> payments,
    List<AdditionalInformationRequest> additionalInformation) {
  @JsonIgnoreProperties(ignoreUnknown = false)
  public record BuyerRequest(
      String identificationType,
      String identification,
      String legalName,
      String address,
      String email,
      String telephone) {}

  @JsonIgnoreProperties(ignoreUnknown = false)
  public record LineRequest(
      String productCode,
      String description,
      String quantity,
      String unitPrice,
      String discount,
      UUID taxRuleId) {}

  @JsonIgnoreProperties(ignoreUnknown = false)
  public record PaymentRequest(UUID paymentMethodId, String amount) {}

  @JsonIgnoreProperties(ignoreUnknown = false)
  public record AdditionalInformationRequest(String name, String value) {}
}
