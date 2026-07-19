package com.alexastudillo.taxdocument.domain.invoicedraft;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

final class DomainTestFixtures {
  static final String VERSION = "SRI-OFFLINE-2.32-TARGET-1";

  private DomainTestFixtures() {}

  static Buyer buyer() {
    return new Buyer("06", "A123", "Buyer", null, "buyer@example.com", null, VERSION);
  }

  static TaxSelection tax(String rate, TaxSelection.Treatment treatment) {
    return new TaxSelection(
        UUID.fromString("5b34b038-931c-50e3-a84c-10af272fdcd4"),
        "IVA",
        treatment,
        "2",
        treatment == TaxSelection.Treatment.PERCENTAGE_RATE ? "4" : "0",
        new BigDecimal(rate),
        VERSION,
        true,
        LocalDate.of(2026, 1, 1),
        null);
  }

  static InvoiceLine line(
      int position, String quantity, String price, String discount, TaxSelection tax) {
    return new InvoiceLine(
        UUID.nameUUIDFromBytes(
            ("line-" + position).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
        position,
        "SKU" + position,
        "Product",
        new BigDecimal(quantity),
        new BigDecimal(price),
        new BigDecimal(discount),
        tax,
        null,
        null,
        null,
        null,
        null);
  }

  static Payment payment(String amount) {
    return new Payment(
        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        UUID.fromString("639f2b7e-10a3-5d92-a1a3-28223896f5b5"),
        "01",
        "Without use of the financial system",
        new BigDecimal(amount),
        VERSION);
  }
}
