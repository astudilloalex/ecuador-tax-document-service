package com.alexastudillo.taxdocument.application.invoicedraft;

import static java.util.Objects.requireNonNull;

import com.alexastudillo.taxdocument.application.requestcontext.RequestDeadline;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import com.alexastudillo.taxdocument.domain.invoicedraft.TaxSelection;
import io.smallrye.mutiny.Uni;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ApplicationTestFixtures {
  public static final UUID COMPANY = uuid("11111111-1111-4111-8111-111111111111");
  public static final UUID EMISSION_POINT = uuid("123e4567-e89b-12d3-a456-426614174000");
  public static final UUID TAX_RULE = uuid("5b34b038-931c-50e3-a84c-10af272fdcd4");
  public static final UUID PAYMENT_METHOD = uuid("639f2b7e-10a3-5d92-a1a3-28223896f5b5");
  public static final String VERSION = "SRI-OFFLINE-2.32-TARGET-1";

  private ApplicationTestFixtures() {}

  public static CreateInvoiceDraftCommand command() {
    return new CreateInvoiceDraftCommand(
        new CompanyId(COMPANY),
        requireNonNull(Instant.parse("2026-07-17T12:00:00Z")),
        RequestDeadline.start(requireNonNull(Duration.ofSeconds(30))),
        "draft-key-1",
        "corr-1",
        "\t123E4567-E89B-12D3-A456-426614174000\t",
        requireNonNull(LocalDate.of(2026, 7, 17)),
        new CreateInvoiceDraftCommand.BuyerInput(
            "06", " ABC123 ", " Cafe\u0301 Buyer ", null, "Buyer@Example.COM", null),
        requireNonNull(
            List.<CreateInvoiceDraftCommand.LineInput>of(
                new CreateInvoiceDraftCommand.LineInput(
                    " SKU1 ",
                    " Service 😀 ",
                    requireNonNull(BigDecimal.ONE),
                    new BigDecimal("10.000000"),
                    new BigDecimal("0.00"),
                    TAX_RULE))),
        requireNonNull(
            List.<CreateInvoiceDraftCommand.PaymentInput>of(
                new CreateInvoiceDraftCommand.PaymentInput(
                    PAYMENT_METHOD, new BigDecimal("11.50")))),
        requireNonNull(
            List.<CreateInvoiceDraftCommand.AdditionalInformationInput>of(
                new CreateInvoiceDraftCommand.AdditionalInformationInput(
                    " Client  Ref ", " A-1 "))));
  }

  public static ReferenceDataPort references() {
    return new ReferenceDataPort() {
      @Override
      public Uni<BuyerIdentificationRule> buyerIdentificationRule(
          String officialCode, Duration remaining) {
        return requireNonNull(
            Uni.createFrom()
                .<BuyerIdentificationRule>item(new BuyerIdentificationRule(officialCode, VERSION)));
      }

      @Override
      public Uni<TaxSelection> ivaRule(UUID taxRuleId, LocalDate emissionDate, Duration remaining) {
        return requireNonNull(
            Uni.createFrom()
                .<TaxSelection>item(
                    new TaxSelection(
                        taxRuleId,
                        "IVA",
                        TaxSelection.Treatment.PERCENTAGE_RATE,
                        "2",
                        "4",
                        new BigDecimal("15.00"),
                        VERSION,
                        true,
                        requireNonNull(LocalDate.of(2026, 7, 12)),
                        null)));
      }

      @Override
      public Uni<PaymentMethod> paymentMethod(
          UUID paymentMethodId, LocalDate emissionDate, Duration remaining) {
        return requireNonNull(
            Uni.createFrom()
                .<PaymentMethod>item(
                    new PaymentMethod(paymentMethodId, "01", "Without financial system", VERSION)));
      }
    };
  }

  public static DraftIdentifierGenerator identifiers() {
    AtomicLong sequence = new AtomicLong(1L);
    return () -> new UUID(0x123456789abcdef0L, sequence.getAndIncrement());
  }

  private static UUID uuid(String value) {
    return requireNonNull(UUID.fromString(value));
  }
}
