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
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Explicit persistence mapping with no identifier, text, timestamp, or HTTP policy. */
@NullMarked
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

    List<@NonNull InvoiceLineEntity> lines =
        draft.lines().stream()
            .map(line -> lineEntity(draft.id(), requireHydrated(line, "draft.lines element")))
            .toList();
    List<@NonNull InvoiceLineTaxEntity> lineTaxes =
        draft.lines().stream()
            .map(
                line ->
                    lineTaxEntity(
                        requireHydrated(
                            candidate.lineTaxIdentifiers().get(line.id()),
                            "candidate.lineTaxIdentifiers"),
                        line.id(),
                        line))
            .toList();
    List<@NonNull InvoiceTaxTotalEntity> totals =
        draft.taxTotals().stream()
            .map(
                total ->
                    taxTotalEntity(
                        requireHydrated(
                            candidate.taxTotalIdentifiers().get(total.groupKey()),
                            "candidate.taxTotalIdentifiers"),
                        draft.id(),
                        total))
            .toList();
    List<@NonNull InvoicePaymentEntity> payments =
        draft.payments().stream()
            .map(
                payment ->
                    paymentEntity(draft.id(), requireHydrated(payment, "draft.payments element")))
            .toList();
    List<@NonNull InvoiceAdditionalInformationEntity> additional =
        draft.additionalInformation().stream()
            .map(
                value ->
                    additionalEntity(
                        draft.id(), requireHydrated(value, "draft.additionalInformation element")))
            .toList();
    return new MappedAggregate(
        root,
        Objects.requireNonNull(lines, "lines"),
        Objects.requireNonNull(lineTaxes, "lineTaxes"),
        Objects.requireNonNull(totals, "totals"),
        Objects.requireNonNull(payments, "payments"),
        Objects.requireNonNull(additional, "additional"));
  }

  public PersistedInvoiceDraft toPersisted(
      InvoiceDraftEntity root,
      List<@NonNull InvoiceLineEntity> lines,
      List<@NonNull InvoiceLineTaxEntity> lineTaxes,
      List<@NonNull InvoiceTaxTotalEntity> totals,
      List<@NonNull InvoicePaymentEntity> payments,
      List<@NonNull InvoiceAdditionalInformationEntity> additional) {
    Map<@NonNull UUID, @NonNull InvoiceLineTaxEntity> taxByLine =
        lineTaxes.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    value -> requireHydrated(value.invoiceLineId, "lineTax.invoiceLineId"),
                    Function.identity()));
    Buyer buyer =
        new Buyer(
            requireHydrated(root.buyerIdentificationTypeCode, "buyerIdentificationTypeCode"),
            requireHydrated(root.buyerIdentification, "buyerIdentification"),
            requireHydrated(root.buyerLegalName, "buyerLegalName"),
            root.buyerAddress,
            root.buyerEmail,
            root.buyerTelephone,
            requireHydrated(
                root.buyerIdentificationCatalogVersion, "buyerIdentificationCatalogVersion"));
    List<@NonNull InvoiceLine> domainLines =
        lines.stream()
            .sorted(Comparator.comparingInt(value -> value.position))
            .map(
                line ->
                    domainLine(line, requireHydrated(taxByLine.get(line.id), "persisted line tax")))
            .toList();
    List<@NonNull TaxTotal> domainTotals =
        totals.stream()
            .map(value -> domainTaxTotal(requireHydrated(value, "taxTotals element")))
            .toList();
    List<@NonNull Payment> domainPayments =
        payments.stream()
            .map(value -> domainPayment(requireHydrated(value, "payments element")))
            .toList();
    List<@NonNull AdditionalInformation> domainAdditional =
        additional.stream()
            .sorted(Comparator.comparingInt(value -> value.position))
            .<@NonNull AdditionalInformation>map(
                value ->
                    new AdditionalInformation(
                        requireHydrated(value.id, "additionalInformation.id"),
                        value.position,
                        requireHydrated(value.name, "additionalInformation.name"),
                        requireHydrated(value.canonicalName, "additionalInformation.canonicalName"),
                        requireHydrated(value.value, "additionalInformation.value")))
            .toList();
    InvoiceDraft draft =
        new InvoiceDraft(
            requireHydrated(root.id, "invoiceDraft.id"),
            new CompanyId(requireHydrated(root.companyId, "invoiceDraft.companyId")),
            requireHydrated(root.emissionPointId, "invoiceDraft.emissionPointId"),
            requireHydrated(root.emissionDate, "invoiceDraft.emissionDate"),
            buyer,
            Objects.requireNonNull(domainLines, "domainLines"),
            Objects.requireNonNull(domainTotals, "domainTotals"),
            Objects.requireNonNull(domainPayments, "domainPayments"),
            Objects.requireNonNull(domainAdditional, "domainAdditional"),
            requireHydrated(root.subtotalBeforeTaxes, "invoiceDraft.subtotalBeforeTaxes"),
            requireHydrated(root.totalDiscount, "invoiceDraft.totalDiscount"),
            requireHydrated(root.grandTotal, "invoiceDraft.grandTotal"));
    return new PersistedInvoiceDraft(
        draft,
        requireHydrated(root.createdAt, "invoiceDraft.createdAt"),
        requireHydrated(root.updatedAt, "invoiceDraft.updatedAt"));
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
    TaxSelection selection =
        new TaxSelection(
            requireHydrated(tax.taxRuleId, "lineTax.taxRuleId"),
            requireHydrated(tax.family, "lineTax.family"),
            TaxSelection.Treatment.valueOf(
                Objects.requireNonNull(
                    requireHydrated(tax.treatment, "lineTax.treatment"), "lineTax.treatment")),
            requireHydrated(tax.officialTaxCode, "lineTax.officialTaxCode"),
            requireHydrated(tax.officialPercentageCode, "lineTax.officialPercentageCode"),
            requireHydrated(tax.rate, "lineTax.rate"),
            requireHydrated(tax.catalogVersion, "lineTax.catalogVersion"),
            true,
            Objects.requireNonNull(LocalDate.MIN, "LocalDate.MIN"),
            null);
    return new InvoiceLine(
        requireHydrated(line.id, "line.id"),
        line.position,
        requireHydrated(line.productCode, "line.productCode"),
        requireHydrated(line.description, "line.description"),
        requireHydrated(line.quantity, "line.quantity"),
        requireHydrated(line.unitPrice, "line.unitPrice"),
        requireHydrated(line.discount, "line.discount"),
        selection,
        requireHydrated(line.grossAmount, "line.grossAmount"),
        requireHydrated(line.netAmount, "line.netAmount"),
        requireHydrated(tax.taxBase, "lineTax.taxBase"),
        requireHydrated(tax.taxAmount, "lineTax.taxAmount"),
        requireHydrated(line.lineTotal, "line.lineTotal"));
  }

  private TaxTotal domainTaxTotal(InvoiceTaxTotalEntity value) {
    return new TaxTotal(
        requireHydrated(value.family, "taxTotal.family"),
        TaxSelection.Treatment.valueOf(requireHydrated(value.treatment, "taxTotal.treatment")),
        requireHydrated(value.officialTaxCode, "taxTotal.officialTaxCode"),
        requireHydrated(value.officialPercentageCode, "taxTotal.officialPercentageCode"),
        requireHydrated(value.rate, "taxTotal.rate"),
        requireHydrated(value.taxBase, "taxTotal.taxBase"),
        requireHydrated(value.taxAmount, "taxTotal.taxAmount"),
        requireHydrated(value.catalogVersion, "taxTotal.catalogVersion"));
  }

  private Payment domainPayment(InvoicePaymentEntity value) {
    return new Payment(
        requireHydrated(value.id, "payment.id"),
        requireHydrated(value.paymentMethodId, "payment.paymentMethodId"),
        requireHydrated(value.officialCode, "payment.officialCode"),
        requireHydrated(value.name, "payment.name"),
        requireHydrated(value.amount, "payment.amount"),
        requireHydrated(value.catalogVersion, "payment.catalogVersion"));
  }

  private static <T> @NonNull T requireHydrated(@Nullable T value, String field) {
    return Objects.requireNonNull(value, field);
  }

  public record MappedAggregate(
      InvoiceDraftEntity root,
      List<@NonNull InvoiceLineEntity> lines,
      List<@NonNull InvoiceLineTaxEntity> lineTaxes,
      List<@NonNull InvoiceTaxTotalEntity> taxTotals,
      List<@NonNull InvoicePaymentEntity> payments,
      List<@NonNull InvoiceAdditionalInformationEntity> additionalInformation) {}
}
