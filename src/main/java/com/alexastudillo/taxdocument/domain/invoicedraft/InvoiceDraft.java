package com.alexastudillo.taxdocument.domain.invoicedraft;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Complete validated and calculated USD Invoice Draft business aggregate. */
@NullMarked
public record InvoiceDraft(
    UUID id,
    CompanyId companyId,
    UUID emissionPointId,
    LocalDate emissionDate,
    Buyer buyer,
    List<@NonNull InvoiceLine> lines,
    List<@NonNull TaxTotal> taxTotals,
    List<@NonNull Payment> payments,
    List<@NonNull AdditionalInformation> additionalInformation,
    BigDecimal subtotalBeforeTaxes,
    BigDecimal totalDiscount,
    BigDecimal grandTotal) {
  public static final String STATUS = "DRAFT";
  public static final String CURRENCY = "USD";

  public InvoiceDraft(
      UUID id,
      CompanyId companyId,
      UUID emissionPointId,
      LocalDate emissionDate,
      Buyer buyer,
      List<@NonNull InvoiceLine> lines,
      List<@NonNull TaxTotal> taxTotals,
      List<@NonNull Payment> payments,
      List<@NonNull AdditionalInformation> additionalInformation,
      BigDecimal subtotalBeforeTaxes,
      BigDecimal totalDiscount,
      BigDecimal grandTotal) {
    this.id = Objects.requireNonNull(id, "id");
    this.companyId = Objects.requireNonNull(companyId, "companyId");
    this.emissionPointId = Objects.requireNonNull(emissionPointId, "emissionPointId");
    this.emissionDate = Objects.requireNonNull(emissionDate, "emissionDate");
    this.buyer = Objects.requireNonNull(buyer, "buyer");
    this.lines = Objects.requireNonNull(List.copyOf(lines), "lines");
    this.taxTotals = Objects.requireNonNull(List.copyOf(taxTotals), "taxTotals");
    this.payments = Objects.requireNonNull(List.copyOf(payments), "payments");
    this.additionalInformation =
        Objects.requireNonNull(List.copyOf(additionalInformation), "additionalInformation");
    this.subtotalBeforeTaxes = Objects.requireNonNull(subtotalBeforeTaxes, "subtotalBeforeTaxes");
    this.totalDiscount = Objects.requireNonNull(totalDiscount, "totalDiscount");
    this.grandTotal = Objects.requireNonNull(grandTotal, "grandTotal");
    if (emissionPointId.equals(new UUID(0L, 0L))) {
      throw invalid("emissionPointId", "Emission point must not be nil");
    }
    if (this.lines.isEmpty()
        || this.lines.size() > 500
        || this.payments.isEmpty()
        || this.payments.size() > 8) {
      throw invalid("invoiceDraft", "Aggregate collection cardinality is invalid");
    }
    if (this.additionalInformation.size() > 15) {
      throw invalid("additionalInformation", "Too many additional-information entries");
    }
    requireUniquePositions(this.lines);
    requireUniquePaymentMethods(this.payments);
    requireUniqueCanonicalNames(this.additionalInformation);
    if (this.lines.stream()
        .anyMatch(line -> line.grossAmount() == null || line.lineTotal() == null)) {
      throw invalid("lines", "Every line must be calculated");
    }
  }

  private static void requireUniquePositions(List<@NonNull InvoiceLine> values) {
    Set<@NonNull Integer> positions = new HashSet<>();
    for (InvoiceLine value : values) {
      if (!positions.add(value.position())) {
        throw invalid("lines", "Line positions must be unique");
      }
    }
  }

  private static void requireUniquePaymentMethods(List<@NonNull Payment> values) {
    Set<@NonNull UUID> methods = new HashSet<>();
    for (Payment value : values) {
      if (!methods.add(value.paymentMethodId())) {
        throw invalid("payments", "Payment methods must be unique");
      }
    }
  }

  private static void requireUniqueCanonicalNames(List<@NonNull AdditionalInformation> values) {
    Set<@Nullable String> names = new HashSet<>();
    for (AdditionalInformation value : values) {
      if (!names.add(value.canonicalName())) {
        throw invalid("additionalInformation", "Canonical names must be unique");
      }
    }
  }

  private static DraftValidationException invalid(String field, String message) {
    return new DraftValidationException("BUSINESS_VALIDATION_FAILED", field, message);
  }
}
