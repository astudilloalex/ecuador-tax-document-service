package com.alexastudillo.taxdocument.application.invoicedraft;

import com.alexastudillo.taxdocument.domain.invoicedraft.AdditionalInformation;
import com.alexastudillo.taxdocument.domain.invoicedraft.Buyer;
import com.alexastudillo.taxdocument.domain.invoicedraft.DraftValidationException;
import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceDraft;
import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceDraftCalculator;
import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceLine;
import com.alexastudillo.taxdocument.domain.invoicedraft.Payment;
import com.alexastudillo.taxdocument.domain.invoicedraft.TaxSelection;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Transport-neutral ordered Invoice Draft creation orchestration. */
@NullMarked
@ApplicationScoped
public final class CreateInvoiceDraftService implements CreateInvoiceDraftUseCase {
  private static final ZoneId ECUADOR = Objects.requireNonNull(ZoneId.of("America/Guayaquil"));
  private static final UUID NIL = new UUID(0L, 0L);
  private static final Pattern UUID_TEXT =
      Objects.requireNonNull(
          Pattern.compile(
              "^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$"));

  private final InvoiceDraftRepository repository;
  private final ReferenceDataPort referenceData;
  private final DraftIdentifierGenerator identifiers;
  private final BusinessTextNormalizer textNormalizer = new BusinessTextNormalizer();
  private final IdempotencyFingerprint fingerprint = new IdempotencyFingerprint();
  private final InvoiceDraftCalculator calculator = new InvoiceDraftCalculator();

  public CreateInvoiceDraftService(
      InvoiceDraftRepository repository,
      ReferenceDataPort referenceData,
      DraftIdentifierGenerator identifiers) {
    this.repository = repository;
    this.referenceData = referenceData;
    this.identifiers = identifiers;
  }

  @Override
  public Uni<@NonNull CreateInvoiceDraftResult> create(CreateInvoiceDraftCommand command) {
    return Objects.requireNonNull(
        Uni.createFrom()
            .item(() -> normalize(command))
            .onItem()
            .transformToUni(this::createNormalized)
            .onFailure(DraftValidationException.class)
            .transform(this::mapValidationFailure));
  }

  private Uni<@NonNull CreateInvoiceDraftResult> createNormalized(
      CreateInvoiceDraftCommand normalized) {
    requireBudget(normalized);
    byte[] keyHash = fingerprint.keyHash(normalized.idempotencyKey());
    byte[] requestFingerprint = fingerprint.requestFingerprint(normalized);
    return Objects.requireNonNull(
        repository
            .findByIdempotency(
                normalized.companyId(),
                keyHash,
                requestFingerprint,
                normalized.deadline().remaining())
            .onItem()
            .transformToUni(
                lookup -> {
                  if (lookup
                      instanceof InvoiceDraftRepository.IdempotencyLookup.Equivalent equivalent) {
                    return Objects.requireNonNull(
                        Uni.createFrom()
                            .item(CreateInvoiceDraftResult.replay(equivalent.persisted())));
                  }
                  if (lookup instanceof InvoiceDraftRepository.IdempotencyLookup.Conflict) {
                    return Objects.requireNonNull(
                        Uni.createFrom()
                            .failure(
                                new InvoiceDraftApplicationException(
                                    new InvoiceDraftFailure(
                                        InvoiceDraftFailure.Code.IDEMPOTENCY_CONFLICT,
                                        "The idempotency key is already bound to different content",
                                        false,
                                        emptyViolations()))));
                  }
                  Uni<? extends @NonNull CreateInvoiceDraftResult> prepared =
                      prepareAndPersist(normalized, keyHash, requestFingerprint);
                  return Objects.requireNonNull(prepared);
                }));
  }

  private Uni<@NonNull CreateInvoiceDraftResult> prepareAndPersist(
      CreateInvoiceDraftCommand command, byte[] keyHash, byte[] requestFingerprint) {
    requireBudget(command);
    return Objects.requireNonNull(
        referenceData
            .buyerIdentificationRule(
                command.buyer().identificationType(), command.deadline().remaining())
            .onItem()
            .transformToUni(
                buyerRule ->
                    resolveTaxes(command, 0, new ArrayList<>())
                        .onItem()
                        .transformToUni(
                            taxes ->
                                resolvePayments(command, 0, new ArrayList<>())
                                    .onItem()
                                    .transformToUni(
                                        paymentMethods ->
                                            persistPrepared(
                                                command,
                                                Objects.requireNonNull(
                                                    buyerRule, "buyer identification rule"),
                                                Objects.requireNonNull(taxes, "resolved taxes"),
                                                Objects.requireNonNull(
                                                    paymentMethods, "resolved payment methods"),
                                                keyHash,
                                                requestFingerprint)))));
  }

  private Uni<@NonNull CreateInvoiceDraftResult> persistPrepared(
      CreateInvoiceDraftCommand command,
      ReferenceDataPort.BuyerIdentificationRule buyerRule,
      List<@NonNull TaxSelection> taxes,
      List<ReferenceDataPort.@NonNull PaymentMethod> paymentMethods,
      byte[] keyHash,
      byte[] requestFingerprint) {
    return Objects.requireNonNull(
        Uni.createFrom()
            .item(
                () -> {
                  Buyer buyer =
                      new Buyer(
                          command.buyer().identificationType(),
                          command.buyer().identification(),
                          command.buyer().legalName(),
                          command.buyer().address(),
                          command.buyer().email(),
                          command.buyer().telephone(),
                          buyerRule.catalogVersion());
                  List<@NonNull InvoiceLine> lineInputs = new ArrayList<>(command.lines().size());
                  for (int index = 0; index < command.lines().size(); index++) {
                    CreateInvoiceDraftCommand.LineInput input =
                        Objects.requireNonNull(command.lines().get(index));
                    lineInputs.add(
                        new InvoiceLine(
                            identifiers.nextIdentifier(),
                            index + 1,
                            input.productCode(),
                            input.description(),
                            input.quantity(),
                            input.unitPrice(),
                            input.discount(),
                            Objects.requireNonNull(taxes.get(index), "resolved tax"),
                            null,
                            null,
                            null,
                            null,
                            null));
                  }
                  List<@NonNull Payment> payments = new ArrayList<>(command.payments().size());
                  for (int index = 0; index < command.payments().size(); index++) {
                    CreateInvoiceDraftCommand.PaymentInput input =
                        Objects.requireNonNull(command.payments().get(index));
                    ReferenceDataPort.PaymentMethod method =
                        Objects.requireNonNull(paymentMethods.get(index));
                    payments.add(
                        new Payment(
                            identifiers.nextIdentifier(),
                            method.id(),
                            method.officialCode(),
                            method.name(),
                            input.amount(),
                            method.catalogVersion()));
                  }
                  List<@NonNull AdditionalInformation> additional = new ArrayList<>();
                  for (int index = 0; index < command.additionalInformation().size(); index++) {
                    CreateInvoiceDraftCommand.AdditionalInformationInput input =
                        Objects.requireNonNull(command.additionalInformation().get(index));
                    additional.add(
                        new AdditionalInformation(
                            identifiers.nextIdentifier(),
                            index + 1,
                            input.name(),
                            input.canonicalName(),
                            input.value()));
                  }
                  InvoiceDraftCalculator.Calculation calculation =
                      calculator.calculate(buyer, lineInputs, payments);
                  InvoiceDraft draft =
                      new InvoiceDraft(
                          identifiers.nextIdentifier(),
                          command.companyId(),
                          Objects.requireNonNull(UUID.fromString(command.emissionPointId())),
                          command.emissionDate(),
                          buyer,
                          calculation.lines(),
                          calculation.taxTotals(),
                          payments,
                          additional,
                          calculation.subtotalBeforeTaxes(),
                          calculation.totalDiscount(),
                          calculation.grandTotal());
                  Map<@NonNull UUID, @NonNull UUID> lineTaxIdentifiers = new LinkedHashMap<>();
                  calculation
                      .lines()
                      .forEach(
                          line -> lineTaxIdentifiers.put(line.id(), identifiers.nextIdentifier()));
                  Map<@NonNull String, @NonNull UUID> taxTotalIdentifiers = new LinkedHashMap<>();
                  calculation
                      .taxTotals()
                      .forEach(
                          total ->
                              taxTotalIdentifiers.put(
                                  total.groupKey(), identifiers.nextIdentifier()));
                  return new InvoiceDraftCandidate(
                      draft,
                      lineTaxIdentifiers,
                      taxTotalIdentifiers,
                      keyHash,
                      requestFingerprint,
                      IdempotencyFingerprint.NORMALIZATION_VERSION);
                })
            .onItem()
            .transformToUni(
                candidate ->
                    repository
                        .persist(candidate, command.deadline().remaining())
                        .map(
                            persisted ->
                                candidate.draft().id().equals(persisted.draft().id())
                                    ? CreateInvoiceDraftResult.newResult(persisted)
                                    : CreateInvoiceDraftResult.replay(persisted))));
  }

  private Uni<@NonNull List<@NonNull TaxSelection>> resolveTaxes(
      CreateInvoiceDraftCommand command, int index, List<@NonNull TaxSelection> values) {
    if (index == command.lines().size()) {
      return Objects.requireNonNull(
          Uni.createFrom().item(Objects.requireNonNull(List.copyOf(values))));
    }
    return Objects.requireNonNull(
        referenceData
            .ivaRule(
                Objects.requireNonNull(command.lines().get(index)).taxRuleId(),
                command.emissionDate(),
                command.deadline().remaining())
            .onItem()
            .transformToUni(
                value -> {
                  value.requireEffectiveOn(command.emissionDate());
                  values.add(value);
                  return resolveTaxes(command, index + 1, values);
                }));
  }

  private Uni<@NonNull List<ReferenceDataPort.@NonNull PaymentMethod>> resolvePayments(
      CreateInvoiceDraftCommand command,
      int index,
      List<ReferenceDataPort.@NonNull PaymentMethod> values) {
    if (index == command.payments().size()) {
      return Objects.requireNonNull(
          Uni.createFrom().item(Objects.requireNonNull(List.copyOf(values))));
    }
    return Objects.requireNonNull(
        referenceData
            .paymentMethod(
                Objects.requireNonNull(command.payments().get(index)).paymentMethodId(),
                command.emissionDate(),
                command.deadline().remaining())
            .onItem()
            .transformToUni(
                value -> {
                  values.add(value);
                  return resolvePayments(command, index + 1, values);
                }));
  }

  private CreateInvoiceDraftCommand normalize(CreateInvoiceDraftCommand command) {
    requireBudget(command);
    String emissionPoint = normalizeEmissionPoint(command.emissionPointId());
    LocalDate expectedDate =
        Objects.requireNonNull(command.requestCreationInstant().atZone(ECUADOR).toLocalDate());
    if (!expectedDate.equals(command.emissionDate())) {
      throw new DraftValidationException(
          "BUSINESS_VALIDATION_FAILED",
          "emissionDate",
          "Emission date does not match request date");
    }
    CreateInvoiceDraftCommand.BuyerInput inputBuyer = command.buyer();
    String identification =
        BusinessTextNormalizer.trimAsciiSpaceAndTab(inputBuyer.identification());
    CreateInvoiceDraftCommand.BuyerInput buyer =
        new CreateInvoiceDraftCommand.BuyerInput(
            inputBuyer.identificationType(),
            identification,
            textNormalizer
                .normalizeDisplay("buyer.legalName", inputBuyer.legalName(), 300)
                .displayValue(),
            normalizeOptional("buyer.address", inputBuyer.address(), 300),
            normalizeOptional("buyer.email", inputBuyer.email(), 254),
            normalizeOptional("buyer.telephone", inputBuyer.telephone(), 20));
    List<CreateInvoiceDraftCommand.@NonNull LineInput> lines = new ArrayList<>();
    for (int index = 0; index < command.lines().size(); index++) {
      CreateInvoiceDraftCommand.LineInput line = Objects.requireNonNull(command.lines().get(index));
      lines.add(
          new CreateInvoiceDraftCommand.LineInput(
              BusinessTextNormalizer.trimAsciiSpaceAndTab(line.productCode()),
              textNormalizer
                  .normalizeDisplay("lines[" + index + "].description", line.description(), 300)
                  .displayValue(),
              line.quantity(),
              line.unitPrice(),
              line.discount(),
              line.taxRuleId()));
    }
    List<CreateInvoiceDraftCommand.@NonNull AdditionalInformationInput> additional =
        new ArrayList<>();
    for (int index = 0; index < command.additionalInformation().size(); index++) {
      CreateInvoiceDraftCommand.AdditionalInformationInput value =
          Objects.requireNonNull(command.additionalInformation().get(index));
      BusinessTextNormalizer.NormalizedText name =
          textNormalizer.normalizeWithCanonicalName(
              "additionalInformation[" + index + "].name", value.name());
      additional.add(
          new CreateInvoiceDraftCommand.AdditionalInformationInput(
              name.displayValue(),
              name.canonicalValue(),
              textNormalizer
                  .normalizeDisplay(
                      "additionalInformation[" + index + "].value", value.value(), 300)
                  .displayValue()));
    }
    return new CreateInvoiceDraftCommand(
        command.companyId(),
        command.requestCreationInstant(),
        command.deadline(),
        command.idempotencyKey(),
        command.correlationId(),
        emissionPoint,
        command.emissionDate(),
        buyer,
        lines,
        command.payments(),
        additional);
  }

  private @Nullable String normalizeOptional(String field, @Nullable String value, int maximum) {
    return value == null
        ? null
        : textNormalizer.normalizeDisplay(field, value, maximum).displayValue();
  }

  private static String normalizeEmissionPoint(String raw) {
    String normalized = BusinessTextNormalizer.trimAsciiSpaceAndTab(raw);
    if (!UUID_TEXT.matcher(normalized).matches()) {
      throw emissionPointFailure();
    }
    UUID value = Objects.requireNonNull(UUID.fromString(normalized));
    if (NIL.equals(value)) {
      throw emissionPointFailure();
    }
    return Objects.requireNonNull(value.toString());
  }

  private static DraftValidationException emissionPointFailure() {
    return new DraftValidationException(
        "EMISSION_POINT_INVALID", "emissionPointId", "Emission point is invalid");
  }

  private static void requireBudget(CreateInvoiceDraftCommand command) {
    if (command.deadline().expired()) {
      throw new InvoiceDraftApplicationException(
          new InvoiceDraftFailure(
              InvoiceDraftFailure.Code.REQUEST_TIMEOUT,
              "The request deadline expired",
              true,
              emptyViolations()));
    }
  }

  private Throwable mapValidationFailure(Throwable throwable) {
    DraftValidationException validation = (DraftValidationException) throwable;
    String stage =
        "EMISSION_POINT_INVALID".equals(validation.code())
            ? "NORMALIZATION"
            : "BUSINESS_VALIDATION";
    return new InvoiceDraftApplicationException(
        InvoiceDraftFailure.validation(
            Objects.requireNonNull(validation.code()),
            Objects.requireNonNull(validation.field()),
            stage));
  }

  private static List<InvoiceDraftFailure.@NonNull Violation> emptyViolations() {
    return Objects.requireNonNull(List.of(), "empty violations");
  }
}
