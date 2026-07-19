package com.alexastudillo.taxdocument.domain.invoicedraft;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Deterministic Stage 11A calculation followed by ordered Stage 11B validation. */
public final class InvoiceDraftCalculator {
  public static final BigDecimal MAX_MONEY = new BigDecimal("999999999999999.99");
  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private static final int MONEY_SCALE = 2;

  public Calculation calculate(Buyer buyer, List<InvoiceLine> inputs, List<Payment> payments) {
    Objects.requireNonNull(buyer, "buyer");
    Objects.requireNonNull(inputs, "inputs");
    Objects.requireNonNull(payments, "payments");
    if (inputs.isEmpty() || inputs.size() > 500) {
      throw violation("LINE_CARDINALITY_INVALID", "lines", "Line count is invalid");
    }

    List<InvoiceLine> lines = new ArrayList<>(inputs.size());
    Map<String, MutableTaxTotal> grouped = new LinkedHashMap<>();
    BigDecimal subtotal = zero();
    BigDecimal totalDiscount = zero();
    for (InvoiceLine input :
        inputs.stream().sorted(Comparator.comparingInt(line -> line.position())).toList()) {
      BigDecimal gross = money(input.quantity().multiply(input.unitPrice()));
      BigDecimal discount = money(input.discount());
      BigDecimal net = money(gross.subtract(discount));
      BigDecimal base = net;
      BigDecimal tax =
          input.taxSelection().treatment() == TaxSelection.Treatment.PERCENTAGE_RATE
              ? money(base.multiply(input.taxSelection().rate()).divide(HUNDRED))
              : zero();
      BigDecimal lineTotal = money(net.add(tax));
      requireMaximum(gross, input.position(), "grossAmount");
      requireMaximum(discount, input.position(), "discount");
      requireMaximum(base.abs(), input.position(), "taxBase");
      requireMaximum(tax.abs(), input.position(), "taxAmount");
      requireMaximum(lineTotal.abs(), input.position(), "lineTotal");
      InvoiceLine calculated = input.calculated(gross, net, base, tax, lineTotal);
      lines.add(calculated);
      subtotal = subtotal.add(net);
      totalDiscount = totalDiscount.add(discount);
      String key = groupKey(input.taxSelection());
      grouped
          .computeIfAbsent(key, ignored -> new MutableTaxTotal(input.taxSelection()))
          .add(base, tax);
    }

    subtotal = money(subtotal);
    totalDiscount = money(totalDiscount);
    requireMaximum(subtotal.abs(), Integer.MAX_VALUE, "subtotalBeforeTaxes");
    requireMaximum(totalDiscount, Integer.MAX_VALUE, "totalDiscount");

    List<TaxTotal> taxTotals =
        grouped.values().stream()
            .sorted(Comparator.comparing(total -> total.key()))
            .<TaxTotal>map(total -> total.toTaxTotal())
            .toList();
    BigDecimal totalTax =
        money(
            taxTotals.stream()
                .map(total -> total.amount())
                .reduce(BigDecimal.ZERO, (acc, val) -> acc.add(val)));
    requireMaximum(totalTax, Integer.MAX_VALUE, "taxTotals");
    BigDecimal grandTotal = money(subtotal.add(totalTax));
    requireMaximum(grandTotal.abs(), Integer.MAX_VALUE, "grandTotal");

    for (InvoiceLine line : lines) {
      if (line.discount().compareTo(line.grossAmount()) > 0) {
        throw violation(
            "DISCOUNT_EXCEEDS_GROSS",
            "lines[" + (line.position() - 1) + "].discount",
            "Discount exceeds line gross amount");
      }
    }
    buyer.validateCalculatedTotal(grandTotal);
    validatePayments(grandTotal, payments);
    return new Calculation(
        List.copyOf(lines),
        taxTotals,
        money(subtotal),
        money(totalDiscount),
        grandTotal,
        grandTotal);
  }

  private static void validatePayments(BigDecimal grandTotal, List<Payment> payments) {
    if (payments.isEmpty() || payments.size() > 8) {
      throw violation("PAYMENT_CARDINALITY_INVALID", "payments", "Payment count is invalid");
    }
    Set<java.util.UUID> methods =
        payments.stream()
            .map(payment -> payment.paymentMethodId())
            .collect(Collectors.toUnmodifiableSet());
    if (methods.size() != payments.size()) {
      throw violation("DUPLICATE_PAYMENT_METHOD", "payments", "Payment method is duplicated");
    }
    if (grandTotal.signum() == 0) {
      if (payments.size() != 1 || payments.getFirst().amount().signum() != 0) {
        throw violation("PAYMENT_SHAPE_INVALID", "payments", "Zero total needs one zero payment");
      }
    } else if (payments.stream().anyMatch(payment -> payment.amount().signum() <= 0)) {
      throw violation("PAYMENT_SHAPE_INVALID", "payments", "Payments must be positive");
    }
    BigDecimal sum =
        money(
            payments.stream()
                .map(payment -> payment.amount())
                .reduce(BigDecimal.ZERO, (acc, val) -> acc.add(val)));
    if (sum.compareTo(grandTotal) != 0) {
      throw violation("PAYMENT_TOTAL_MISMATCH", "payments", "Payments do not reconcile");
    }
  }

  private static String groupKey(TaxSelection selection) {
    return selection.treatment()
        + "|"
        + selection.officialTaxCode()
        + "|"
        + selection.officialPercentageCode()
        + "|"
        + selection.rate().toPlainString()
        + "|"
        + selection.catalogVersion();
  }

  private static void requireMaximum(BigDecimal value, int line, String field) {
    if (value.compareTo(MAX_MONEY) > 0) {
      String path = line == Integer.MAX_VALUE ? field : "lines[" + (line - 1) + "]." + field;
      throw violation("MONETARY_RANGE_EXCEEDED", path, "Calculated monetary value overflowed");
    }
  }

  private static BigDecimal money(BigDecimal value) {
    return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
  }

  private static BigDecimal zero() {
    return BigDecimal.ZERO.setScale(MONEY_SCALE);
  }

  private static DraftValidationException violation(String code, String field, String message) {
    return new DraftValidationException(code, field, message);
  }

  public record Calculation(
      List<InvoiceLine> lines,
      List<TaxTotal> taxTotals,
      BigDecimal subtotalBeforeTaxes,
      BigDecimal totalDiscount,
      BigDecimal grandTotal,
      BigDecimal paymentReferenceTotal) {}

  private static final class MutableTaxTotal {
    private final TaxSelection selection;
    private BigDecimal base = zero();
    private BigDecimal amount = zero();

    private MutableTaxTotal(TaxSelection selection) {
      this.selection = selection;
    }

    private void add(BigDecimal addedBase, BigDecimal addedAmount) {
      base = money(base.add(addedBase));
      amount = money(amount.add(addedAmount));
      requireMaximum(base.abs(), Integer.MAX_VALUE, "taxTotals");
      requireMaximum(amount.abs(), Integer.MAX_VALUE, "taxTotals");
    }

    private String key() {
      return groupKey(selection);
    }

    private TaxTotal toTaxTotal() {
      return new TaxTotal(
          selection.family(),
          selection.treatment(),
          selection.officialTaxCode(),
          selection.officialPercentageCode(),
          selection.rate(),
          base,
          amount,
          selection.catalogVersion());
    }
  }
}
