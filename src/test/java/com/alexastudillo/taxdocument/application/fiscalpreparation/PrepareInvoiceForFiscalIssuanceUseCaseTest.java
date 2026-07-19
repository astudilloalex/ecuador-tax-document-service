package com.alexastudillo.taxdocument.application.fiscalpreparation;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alexastudillo.taxdocument.application.requestcontext.RequestContext;
import com.alexastudillo.taxdocument.application.requestcontext.RequestDeadline;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.AccessKeyGenerator;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalContextSnapshot;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalDesignation;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalPreparation;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalSourceEvidence;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.NumericCode;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.OfficialSequentialNumber;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class PrepareInvoiceForFiscalIssuanceUseCaseTest {
  private static final CompanyId COMPANY =
      new CompanyId(uuid("11111111-1111-4111-8111-111111111111"));
  private static final UUID DRAFT = uuid("22222222-2222-4222-8222-222222222222");
  private static final UUID EMISSION_POINT = uuid("123e4567-e89b-12d3-a456-426614174000");
  private static final LocalDate TODAY = date(2026, 7, 18);

  @Test
  void replayReturnsTheCommittedWinnerBeforeDateProviderStoreCommitOrIdentityWork() {
    FiscalPreparation winner = preparation();
    FakeStore store = new FakeStore(new FiscalPreparationLookup.Existing(winner));
    FakeFiscalContextPort provider = new FakeFiscalContextPort(validResolution());
    PrepareInvoiceForFiscalIssuanceService service = service(store, provider);

    PrepareInvoiceForFiscalIssuanceResult result =
        service.prepare(command(date(2030, 1, 1))).await().indefinitely();

    assertSame(winner, result.preparation());
    assertEquals(true, result.replayed());
    assertEquals(1, store.lookupCalls.get());
    assertEquals(0, store.commitCalls.get());
    assertEquals(0, provider.calls.get());
  }

  @Test
  void firstPreparationFixesDateValidatesProviderCompletelyThenStartsOneStoreCommit() {
    InvoiceDraftPreparationView draft =
        new InvoiceDraftPreparationView(DRAFT, COMPANY, EMISSION_POINT, TODAY, "DRAFT");
    FakeStore store = new FakeStore(new FiscalPreparationLookup.EligibleDraft(draft));
    store.commitResult.set(new FiscalPreparationCommitResult.Created(preparation()));
    FakeFiscalContextPort provider = new FakeFiscalContextPort(validResolution());
    PrepareInvoiceForFiscalIssuanceResult result =
        service(store, provider).prepare(command(TODAY)).await().indefinitely();

    assertEquals(false, result.replayed());
    assertEquals(1, provider.calls.get());
    assertEquals(1, store.commitCalls.get());
    assertSame(draft, store.intent.get().draft());
    assertEquals("revision-1", store.intent.get().snapshot().sourceEvidence().revision());
  }

  @Test
  void nonPreparableAndStaleDraftsFailBeforeProviderAndCommitWithStableCodes() {
    for (FiscalPreparationLookup candidateLookup :
        new FiscalPreparationLookup[] {
          new FiscalPreparationLookup.NotFound(),
          new FiscalPreparationLookup.NotPreparable(
              FiscalPreparationLookup.NotPreparableReason.NON_DRAFT)
        }) {
      FiscalPreparationLookup lookup = requireNonNull(candidateLookup);
      FakeStore store = new FakeStore(lookup);
      FakeFiscalContextPort provider = new FakeFiscalContextPort(validResolution());
      FiscalPreparationApplicationException failure =
          assertThrows(
              FiscalPreparationApplicationException.class,
              () -> service(store, provider).prepare(command(TODAY)).await().indefinitely());
      assertEquals(0, provider.calls.get());
      assertEquals(0, store.commitCalls.get());
      assertEquals(
          lookup instanceof FiscalPreparationLookup.NotFound
              ? FiscalPreparationFailure.Code.INVOICE_DRAFT_NOT_FOUND
              : FiscalPreparationFailure.Code.INVOICE_DRAFT_NOT_PREPARABLE,
          failure.failure().code());
    }

    InvoiceDraftPreparationView stale =
        new InvoiceDraftPreparationView(
            DRAFT, COMPANY, EMISSION_POINT, requireNonNull(TODAY.minusDays(1)), "DRAFT");
    FakeStore store = new FakeStore(new FiscalPreparationLookup.EligibleDraft(stale));
    FakeFiscalContextPort provider = new FakeFiscalContextPort(validResolution());
    FiscalPreparationApplicationException failure =
        assertThrows(
            FiscalPreparationApplicationException.class,
            () -> service(store, provider).prepare(command(TODAY)).await().indefinitely());
    assertEquals(FiscalPreparationFailure.Code.EMISSION_DATE_STALE, failure.failure().code());
    assertEquals(0, provider.calls.get());
    assertEquals(0, store.commitCalls.get());
  }

  @Test
  void unavailableIncompleteUnsupportedOrInconsistentContextNeverCallsTheStoreCommit() {
    InvoiceDraftPreparationView draft =
        new InvoiceDraftPreparationView(DRAFT, COMPANY, EMISSION_POINT, TODAY, "DRAFT");
    FakeStore store = new FakeStore(new FiscalPreparationLookup.EligibleDraft(draft));
    FakeFiscalContextPort unavailable =
        new FakeFiscalContextPort(
            new FiscalPreparationApplicationException(
                FiscalPreparationFailure.of(
                    FiscalPreparationFailure.Code.FISCAL_CONTEXT_UNAVAILABLE)));
    FiscalPreparationApplicationException providerFailure =
        assertThrows(
            FiscalPreparationApplicationException.class,
            () -> service(store, unavailable).prepare(command(TODAY)).await().indefinitely());
    assertEquals(
        FiscalPreparationFailure.Code.FISCAL_CONTEXT_UNAVAILABLE, providerFailure.failure().code());
    assertEquals(0, store.commitCalls.get());

    FiscalContextResolution inconsistent = validResolution().withEmissionPointCode("1");
    FakeFiscalContextPort invalid = new FakeFiscalContextPort(inconsistent);
    FiscalPreparationApplicationException validationFailure =
        assertThrows(
            FiscalPreparationApplicationException.class,
            () -> service(store, invalid).prepare(command(TODAY)).await().indefinitely());
    assertEquals(
        FiscalPreparationFailure.Code.FISCAL_CONTEXT_INVALID, validationFailure.failure().code());
    assertEquals(0, store.commitCalls.get());
  }

  private static PrepareInvoiceForFiscalIssuanceService service(
      FakeStore store, FakeFiscalContextPort provider) {
    return new PrepareInvoiceForFiscalIssuanceService(
        store, provider, new FiscalContextValidator());
  }

  private static PrepareInvoiceForFiscalIssuanceCommand command(LocalDate requestDate) {
    Instant entry =
        requireNonNull(requestDate.atStartOfDay(RequestContext.ECUADOR_TIME_ZONE).toInstant());
    return new PrepareInvoiceForFiscalIssuanceCommand(
        COMPANY,
        DRAFT,
        "corr-1",
        new RequestContext(
            entry, requestDate, RequestDeadline.start(requireNonNull(Duration.ofSeconds(10)))),
        new FiscalPreparationCommitTracker());
  }

  static FiscalContextResolution validResolution() {
    return new FiscalContextResolution(
        "issuer-1",
        "1792146739001",
        "Issuer S.A.",
        optional("Issuer"),
        "Head Office",
        true,
        emptyOptional(),
        emptyOptional(),
        FiscalDesignation.RimpeClassification.NONE,
        emptyOptional(),
        "establishment-1",
        "001",
        "Establishment Address",
        EMISSION_POINT,
        "002",
        "1",
        "01",
        "1",
        true,
        new FiscalSourceEvidence(
            "SRI",
            "revision-1",
            date(2026, 7, 1),
            emptyOptional(),
            instant("2026-07-18T11:59:00Z")));
  }

  private static FiscalPreparation preparation() {
    FiscalContextSnapshot snapshot =
        new FiscalContextValidator().validate(validResolution(), EMISSION_POINT, TODAY);
    return new FiscalPreparation(
        uuid("33333333-3333-4333-8333-333333333333"),
        COMPANY,
        DRAFT,
        uuid("44444444-4444-4444-8444-444444444444"),
        TODAY,
        snapshot,
        OfficialSequentialNumber.of(1),
        NumericCode.of(1),
        new AccessKeyGenerator()
            .generate(
                TODAY,
                snapshot.issuerRuc(),
                snapshot.environmentCode(),
                snapshot.establishmentCode(),
                snapshot.emissionPointCode(),
                OfficialSequentialNumber.of(1),
                NumericCode.of(1)),
        instant("2026-07-18T12:00:00Z"));
  }

  private static final class FakeStore implements FiscalPreparationStore {
    private final FiscalPreparationLookup lookup;
    private final AtomicInteger lookupCalls = new AtomicInteger();
    private final AtomicInteger commitCalls = new AtomicInteger();
    private final AtomicReference<FiscalPreparationCommitIntent> intent = new AtomicReference<>();
    private final AtomicReference<FiscalPreparationCommitResult> commitResult =
        new AtomicReference<>();

    private FakeStore(FiscalPreparationLookup lookup) {
      this.lookup = lookup;
    }

    @Override
    public Uni<FiscalPreparationLookup> lookup(
        CompanyId companyId, UUID invoiceDraftId, Duration remaining) {
      lookupCalls.incrementAndGet();
      return uniItem(lookup);
    }

    @Override
    public Uni<FiscalPreparationCommitResult> commit(
        FiscalPreparationCommitIntent value, Duration remaining) {
      commitCalls.incrementAndGet();
      intent.set(value);
      return uniItem(requireNonNull(commitResult.get(), "commit result"));
    }
  }

  private static final class FakeFiscalContextPort implements FiscalContextPort {
    private final AtomicInteger calls = new AtomicInteger();
    private final Optional<FiscalContextResolution> resolution;
    private final Optional<RuntimeException> failure;

    private FakeFiscalContextPort(FiscalContextResolution resolution) {
      this.resolution = Optional.of(resolution);
      this.failure = emptyOptional();
    }

    private FakeFiscalContextPort(RuntimeException failure) {
      this.resolution = emptyOptional();
      this.failure = Optional.of(failure);
    }

    @Override
    public Uni<FiscalContextResolution> resolve(Request request) {
      calls.incrementAndGet();
      if (failure.isPresent()) {
        return requireNonNull(Uni.createFrom().failure(failure.orElseThrow()));
      }
      return uniItem(resolution.orElseThrow());
    }
  }

  private static UUID uuid(String value) {
    return requireNonNull(UUID.fromString(value));
  }

  private static LocalDate date(int year, int month, int day) {
    return requireNonNull(LocalDate.of(year, month, day));
  }

  private static Instant instant(String value) {
    return requireNonNull(Instant.parse(value));
  }

  private static <T> Optional<T> optional(T value) {
    return requireNonNull(Optional.of(value));
  }

  private static <T> Optional<T> emptyOptional() {
    return requireNonNull(Optional.empty());
  }

  private static <T> Uni<T> uniItem(T value) {
    return requireNonNull(Uni.createFrom().item(value));
  }
}
