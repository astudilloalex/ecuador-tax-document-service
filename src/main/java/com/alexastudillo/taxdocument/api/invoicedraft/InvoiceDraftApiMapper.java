package com.alexastudillo.taxdocument.api.invoicedraft;

import com.alexastudillo.taxdocument.api.problem.ProblemDetails;
import com.alexastudillo.taxdocument.application.invoicedraft.CreateInvoiceDraftCommand;
import com.alexastudillo.taxdocument.application.invoicedraft.CreateInvoiceDraftResult;
import com.alexastudillo.taxdocument.domain.invoicedraft.AdditionalInformation;
import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceDraft;
import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceLine;
import com.alexastudillo.taxdocument.domain.invoicedraft.Payment;
import com.alexastudillo.taxdocument.domain.invoicedraft.TaxTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/** Explicit raw-transport-to-command and persisted-result-to-response mapping. */
@NullMarked
@ApplicationScoped
public final class InvoiceDraftApiMapper {
  public CreateInvoiceDraftCommand toCommand(
      CreateInvoiceDraftRequest request, InvoiceDraftRequestState state) {
    try {
      List<CreateInvoiceDraftCommand.LineInput> lines =
          request.lines().stream().map(line -> lineInput(line)).toList();
      List<CreateInvoiceDraftCommand.PaymentInput> payments =
          request.payments().stream().map(payment -> paymentInput(payment)).toList();
      List<CreateInvoiceDraftCommand.AdditionalInformationInput> additionalInformation =
          additionalInformationInputs(request);

      return new CreateInvoiceDraftCommand(
          state.companyId(),
          state.requestCreationInstant(),
          state.deadline(),
          state.idempotencyKey(),
          state.correlationId(),
          request.emissionPointId(),
          request.emissionDate(),
          new CreateInvoiceDraftCommand.BuyerInput(
              request.buyer().identificationType(),
              request.buyer().identification(),
              request.buyer().legalName(),
              request.buyer().address(),
              request.buyer().email(),
              request.buyer().telephone()),
          lines,
          payments,
          additionalInformation);
    } catch (NullPointerException | NumberFormatException exception) {
      throw new ProblemDetails.ApiException(
          400, "INVALID_REQUEST", "The request representation is invalid");
    }
  }

  public InvoiceDraftResponse toResponse(CreateInvoiceDraftResult result) {
    InvoiceDraft draft = result.draft();
    if (!result.replayed() && !result.createdAt().equals(result.updatedAt())) {
      throw new IllegalStateException("Initial Invoice Draft timestamps must be identical");
    }
    List<InvoiceDraftResponse.LineResponse> lines =
        draft.lines().stream().map(line -> lineResponse(line)).toList();
    List<InvoiceDraftResponse.TaxResponse> taxTotals =
        draft.taxTotals().stream().map(tax -> taxResponse(tax)).toList();
    List<InvoiceDraftResponse.PaymentResponse> payments =
        draft.payments().stream().map(payment -> paymentResponse(payment)).toList();
    List<InvoiceDraftResponse.AdditionalInformationResponse> additionalInformation =
        draft.additionalInformation().stream().map(info -> additionalInformationResponse(info)).toList();

    return new InvoiceDraftResponse(
        draft.id(),
        draft.companyId().value(),
        draft.emissionPointId(),
        draft.emissionDate(),
        InvoiceDraft.STATUS,
        InvoiceDraft.CURRENCY,
        new InvoiceDraftResponse.BuyerResponse(
            draft.buyer().identificationType(),
            draft.buyer().identification(),
            draft.buyer().legalName(),
            draft.buyer().address(),
            draft.buyer().email(),
            draft.buyer().telephone()),
        lines,
        taxTotals,
        payments,
        additionalInformation,
        draft.subtotalBeforeTaxes(),
        draft.totalDiscount(),
        draft.grandTotal(),
        result.createdAt(),
        result.updatedAt());
  }

  private CreateInvoiceDraftCommand.LineInput lineInput(
      CreateInvoiceDraftRequest.LineRequest line) {
    return new CreateInvoiceDraftCommand.LineInput(
        line.productCode(),
        line.description(),
        new BigDecimal(line.quantity()),
        new BigDecimal(line.unitPrice()),
        new BigDecimal(line.discount()),
        line.taxRuleId());
  }

  private CreateInvoiceDraftCommand.PaymentInput paymentInput(
      CreateInvoiceDraftRequest.PaymentRequest payment) {
    return new CreateInvoiceDraftCommand.PaymentInput(
        payment.paymentMethodId(), new BigDecimal(payment.amount()));
  }

  private List<CreateInvoiceDraftCommand.AdditionalInformationInput> additionalInformationInputs(
      CreateInvoiceDraftRequest request) {
    List<CreateInvoiceDraftRequest.AdditionalInformationRequest> infoList = request.additionalInformation();
    if (infoList == null) {
      return List.of();
    }
    return infoList.stream().map(info -> additionalInformationInput(info)).toList();
  }

  private CreateInvoiceDraftCommand.AdditionalInformationInput additionalInformationInput(
      CreateInvoiceDraftRequest.AdditionalInformationRequest value) {
    return new CreateInvoiceDraftCommand.AdditionalInformationInput(value.name(), value.value());
  }

  private InvoiceDraftResponse.LineResponse lineResponse(InvoiceLine line) {
    return new InvoiceDraftResponse.LineResponse(
        line.position(),
        line.productCode(),
        line.description(),
        line.quantity(),
        line.unitPrice(),
        line.discount(),
        line.grossAmount(),
        line.netAmount(),
        new InvoiceDraftResponse.LineTaxResponse(
            line.taxSelection().taxRuleId(),
            line.taxSelection().family(),
            line.taxSelection().treatment().name(),
            line.taxSelection().officialTaxCode(),
            line.taxSelection().officialPercentageCode(),
            line.taxSelection().rate(),
            line.taxBase(),
            line.taxAmount(),
            line.taxSelection().catalogVersion()),
        line.lineTotal());
  }

  private InvoiceDraftResponse.TaxResponse taxResponse(TaxTotal total) {
    return new InvoiceDraftResponse.TaxResponse(
        total.family(),
        total.treatment().name(),
        total.officialTaxCode(),
        total.officialPercentageCode(),
        total.rate(),
        total.base(),
        total.amount(),
        total.catalogVersion());
  }

  private InvoiceDraftResponse.PaymentResponse paymentResponse(Payment payment) {
    return new InvoiceDraftResponse.PaymentResponse(
        payment.paymentMethodId(),
        payment.officialCode(),
        payment.name(),
        payment.amount(),
        payment.catalogVersion());
  }

  private InvoiceDraftResponse.AdditionalInformationResponse additionalInformationResponse(
      AdditionalInformation value) {
    return new InvoiceDraftResponse.AdditionalInformationResponse(value.name(), value.value());
  }
}
