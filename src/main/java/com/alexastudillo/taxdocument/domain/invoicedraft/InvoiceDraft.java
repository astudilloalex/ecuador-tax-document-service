package com.alexastudillo.taxdocument.domain.invoicedraft;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/** Complete validated and calculated USD Invoice Draft business aggregate. */
@NullMarked
public record InvoiceDraft(
    UUID id,
    CompanyId companyId,
    UUID emissionPointId,
    LocalDate emissionDate,
    Buyer buyer,
    List<InvoiceLine> lines,
    List<TaxTotal> taxTotals,
    List<Payment> payments,
    List<AdditionalInformation> additionalInformation,
    BigDecimal subtotalBeforeTaxes,
    BigDecimal totalDiscount,
    BigDecimal grandTotal) {
  public static final String STATUS = "DRAFT";
  public static final String CURRENCY = "USD";

  public InvoiceDraft {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(companyId, "companyId");
    Objects.requireNonNull(emissionPointId, "emissionPointId");
    Objects.requireNonNull(emissionDate, "emissionDate");
    Objects.requireNonNull(buyer, "buyer");
    Objects.requireNonNull(lines, "lines");
    Objects.requireNonNull(taxTotals, "taxTotals");
    Objects.requireNonNull(payments, "payments");
    Objects.requireNonNull(additionalInformation, "additionalInformation");
    Objects.requireNonNull(subtotalBeforeTaxes, "subtotalBeforeTaxes");
    Objects.requireNonNull(totalDiscount, "totalDiscount");
    Objects.requireNonNull(grandTotal, "grandTotal");
    lines = Objects.requireNonNull(List.copyOf(lines), "lines");
    taxTotals = Objects.requireNonNull(List.copyOf(taxTotals), "taxTotals");
    payments = Objects.requireNonNull(List.copyOf(payments), "payments");
    additionalInformation =
        Objects.requireNonNull(List.copyOf(additionalInformation), "additionalInformation");
    if (emissionPointId.equals(new UUID(0L, 0L))) {
      throw invalid("emissionPointId", "Emission point must not be nil");
    }
    if (lines.isEmpty() || lines.size() > 500 || payments.isEmpty() || payments.size() > 8) {
      throw invalid("invoiceDraft", "Aggregate collection cardinality is invalid");
    }
    if (additionalInformation.size() > 15) {
      throw invalid("additionalInformation", "Too many additional-information entries");
    }
    requireUniquePositions(lines);
    requireUniquePaymentMethods(payments);
    requireUniqueCanonicalNames(additionalInformation);
    if (lines.stream().anyMatch(line -> line.grossAmount() == null || line.lineTotal() == null)) {
      throw invalid("lines", "Every line must be calculated");
    }
  }

  private static void requireUniquePositions(List<InvoiceLine> values) {
    Set<Integer> positions = new HashSet<>();
    for (InvoiceLine value : values) {
      if (!positions.add(value.position())) {
        throw invalid("lines", "Line positions must be unique");
      }
    }
  }

  private static void requireUniquePaymentMethods(List<Payment> values) {
    Set<UUID> methods = new HashSet<>();
    for (Payment value : values) {
      if (!methods.add(value.paymentMethodId())) {
        throw invalid("payments", "Payment methods must be unique");
      }
    }
  }

  private static void requireUniqueCanonicalNames(List<AdditionalInformation> values) {
    Set<String> names = new HashSet<>();
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
