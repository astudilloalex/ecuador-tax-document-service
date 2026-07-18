package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftCandidate;
import com.alexastudillo.taxdocument.application.invoicedraft.PersistedInvoiceDraft;
import com.alexastudillo.taxdocument.domain.invoicedraft.AdditionalInformation;
import com.alexastudillo.taxdocument.domain.invoicedraft.Buyer;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceDraft;
import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceLine;
import com.alexastudillo.taxdocument.domain.invoicedraft.Payment;
import com.alexastudillo.taxdocument.domain.invoicedraft.TaxSelection;
import com.alexastudillo.taxdocument.domain.invoicedraft.TaxTotal;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Explicit persistence mapping with no identifier, text, timestamp, or HTTP policy. */
@ApplicationScoped
public final class InvoiceDraftPersistenceMapper {
  public MappedAggregate toEntities(InvoiceDraftCandidate candidate, Instant createdAt) {
    InvoiceDraft draft = candidate.draft();
    InvoiceDraftEntity root = new InvoiceDraftEntity();
    root.id = draft.id();
    root.companyId = draft.companyId().value();
    root.emissionPointId = draft.emissionPointId();
    root.emissionDate = draft.emissionDate();
    root.buyerIdentificationTypeCode = draft.buyer().identificationType();
    root.buyerIdentificationCatalogVersion = draft.buyer().catalogVersion();
    root.buyerIdentification = draft.buyer().identification();
    root.buyerLegalName = draft.buyer().legalName();
    root.buyerAddress = draft.buyer().address();
    root.buyerEmail = draft.buyer().email();
    root.buyerTelephone = draft.buyer().telephone();
    root.status = InvoiceDraft.STATUS;
    root.currency = InvoiceDraft.CURRENCY;
    root.subtotalBeforeTaxes = draft.subtotalBeforeTaxes();
    root.totalDiscount = draft.totalDiscount();
    root.grandTotal = draft.grandTotal();
    root.createdAt = createdAt;
    root.updatedAt = createdAt;

    List<InvoiceLineEntity> lines =
        draft.lines().stream().map(line -> lineEntity(draft.id(), line)).toList();
    List<InvoiceLineTaxEntity> lineTaxes =
        draft.lines().stream()
            .map(
                line ->
                    lineTaxEntity(candidate.lineTaxIdentifiers().get(line.id()), line.id(), line))
            .toList();
    List<InvoiceTaxTotalEntity> totals =
        draft.taxTotals().stream()
            .map(
                total ->
                    taxTotalEntity(
                        candidate.taxTotalIdentifiers().get(total.groupKey()), draft.id(), total))
            .toList();
    List<InvoicePaymentEntity> payments =
        draft.payments().stream().map(payment -> paymentEntity(draft.id(), payment)).toList();
    List<InvoiceAdditionalInformationEntity> additional =
        draft.additionalInformation().stream()
            .map(value -> additionalEntity(draft.id(), value))
            .toList();
    return new MappedAggregate(root, lines, lineTaxes, totals, payments, additional);
  }

  public PersistedInvoiceDraft toPersisted(
      InvoiceDraftEntity root,
      List<InvoiceLineEntity> lines,
      List<InvoiceLineTaxEntity> lineTaxes,
      List<InvoiceTaxTotalEntity> totals,
      List<InvoicePaymentEntity> payments,
      List<InvoiceAdditionalInformationEntity> additional) {
    Map<UUID, InvoiceLineTaxEntity> taxByLine =
        lineTaxes.stream()
            .collect(
                Collectors.toUnmodifiableMap(value -> value.invoiceLineId, Function.identity()));
    Buyer buyer =
        new Buyer(
            root.buyerIdentificationTypeCode,
            root.buyerIdentification,
            root.buyerLegalName,
            root.buyerAddress,
            root.buyerEmail,
            root.buyerTelephone,
            root.buyerIdentificationCatalogVersion);
    List<InvoiceLine> domainLines =
        lines.stream()
            .sorted(Comparator.comparingInt(value -> value.position))
            .map(line -> domainLine(line, taxByLine.get(line.id)))
            .toList();
    List<TaxTotal> domainTotals = totals.stream().map(this::domainTaxTotal).toList();
    List<Payment> domainPayments = payments.stream().map(this::domainPayment).toList();
    List<AdditionalInformation> domainAdditional =
        additional.stream()
            .sorted(Comparator.comparingInt(value -> value.position))
            .<AdditionalInformation>map(
                value ->
                    new AdditionalInformation(
                        value.id, value.position, value.name, value.canonicalName, value.value))
            .toList();
    InvoiceDraft draft =
        new InvoiceDraft(
            root.id,
            new CompanyId(root.companyId),
            root.emissionPointId,
            root.emissionDate,
            buyer,
            domainLines,
            domainTotals,
            domainPayments,
            domainAdditional,
            root.subtotalBeforeTaxes,
            root.totalDiscount,
            root.grandTotal);
    return new PersistedInvoiceDraft(draft, root.createdAt, root.updatedAt);
  }

  private static InvoiceLineEntity lineEntity(UUID draftId, InvoiceLine line) {
    InvoiceLineEntity entity = new InvoiceLineEntity();
    entity.id = line.id();
    entity.invoiceDraftId = draftId;
    entity.position = line.position();
    entity.productCode = line.productCode();
    entity.description = line.description();
    entity.quantity = line.quantity();
    entity.unitPrice = line.unitPrice();
    entity.discount = line.discount();
    entity.grossAmount = line.grossAmount();
    entity.netAmount = line.netAmount();
    entity.lineTotal = line.lineTotal();
    return entity;
  }

  private static InvoiceLineTaxEntity lineTaxEntity(UUID id, UUID lineId, InvoiceLine line) {
    if (id == null) {
      throw new IllegalArgumentException("A final line-tax identifier is required");
    }
    TaxSelection tax = line.taxSelection();
    InvoiceLineTaxEntity entity = new InvoiceLineTaxEntity();
    entity.id = id;
    entity.invoiceLineId = lineId;
    entity.taxRuleId = tax.taxRuleId();
    entity.family = tax.family();
    entity.treatment = tax.treatment().name();
    entity.officialTaxCode = tax.officialTaxCode();
    entity.officialPercentageCode = tax.officialPercentageCode();
    entity.rate = tax.rate();
    entity.taxBase = line.taxBase();
    entity.taxAmount = line.taxAmount();
    entity.catalogVersion = tax.catalogVersion();
    return entity;
  }

  private static InvoiceTaxTotalEntity taxTotalEntity(UUID id, UUID draftId, TaxTotal total) {
    if (id == null) {
      throw new IllegalArgumentException("A final tax-total identifier is required");
    }
    InvoiceTaxTotalEntity entity = new InvoiceTaxTotalEntity();
    entity.id = id;
    entity.invoiceDraftId = draftId;
    entity.family = total.family();
    entity.treatment = total.treatment().name();
    entity.officialTaxCode = total.officialTaxCode();
    entity.officialPercentageCode = total.officialPercentageCode();
    entity.rate = total.rate();
    entity.taxBase = total.base();
    entity.taxAmount = total.amount();
    entity.catalogVersion = total.catalogVersion();
    return entity;
  }

  private static InvoicePaymentEntity paymentEntity(UUID draftId, Payment payment) {
    InvoicePaymentEntity entity = new InvoicePaymentEntity();
    entity.id = payment.id();
    entity.invoiceDraftId = draftId;
    entity.paymentMethodId = payment.paymentMethodId();
    entity.officialCode = payment.officialCode();
    entity.name = payment.name();
    entity.amount = payment.amount();
    entity.catalogVersion = payment.catalogVersion();
    return entity;
  }

  private static InvoiceAdditionalInformationEntity additionalEntity(
      UUID draftId, AdditionalInformation value) {
    InvoiceAdditionalInformationEntity entity = new InvoiceAdditionalInformationEntity();
    entity.id = value.id();
    entity.invoiceDraftId = draftId;
    entity.position = value.position();
    entity.name = value.name();
    entity.canonicalName = value.canonicalName();
    entity.value = value.value();
    return entity;
  }

  private InvoiceLine domainLine(InvoiceLineEntity line, InvoiceLineTaxEntity tax) {
    if (tax == null) {
      throw new IllegalStateException("Persisted line tax is missing");
    }
    TaxSelection selection =
        new TaxSelection(
            tax.taxRuleId,
            tax.family,
            TaxSelection.Treatment.valueOf(tax.treatment),
            tax.officialTaxCode,
            tax.officialPercentageCode,
            tax.rate,
            tax.catalogVersion,
            true,
            LocalDate.MIN,
            null);
    return new InvoiceLine(
        line.id,
        line.position,
        line.productCode,
        line.description,
        line.quantity,
        line.unitPrice,
        line.discount,
        selection,
        line.grossAmount,
        line.netAmount,
        tax.taxBase,
        tax.taxAmount,
        line.lineTotal);
  }

  private TaxTotal domainTaxTotal(InvoiceTaxTotalEntity value) {
    return new TaxTotal(
        value.family,
        TaxSelection.Treatment.valueOf(value.treatment),
        value.officialTaxCode,
        value.officialPercentageCode,
        value.rate,
        value.taxBase,
        value.taxAmount,
        value.catalogVersion);
  }

  private Payment domainPayment(InvoicePaymentEntity value) {
    return new Payment(
        value.id,
        value.paymentMethodId,
        value.officialCode,
        value.name,
        value.amount,
        value.catalogVersion);
  }

  public record MappedAggregate(
      InvoiceDraftEntity root,
      List<InvoiceLineEntity> lines,
      List<InvoiceLineTaxEntity> lineTaxes,
      List<InvoiceTaxTotalEntity> taxTotals,
      List<InvoicePaymentEntity> payments,
      List<InvoiceAdditionalInformationEntity> additionalInformation) {}
}
