package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftApplicationException;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftCandidate;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftRepository;
import com.alexastudillo.taxdocument.application.invoicedraft.PersistedInvoiceDraft;
import com.alexastudillo.taxdocument.support.FixedRequestClock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@RunOnVertxContext
@NullMarked
class InvoiceDraftIdempotencyConcurrencyTest {
  @Inject PostgreSqlTestResource database;
  @Inject InvoiceDraftRepository repository;
  @Inject FixedRequestClock clock;

  @BeforeEach
  void prepare() {
    database.resetSchema();
    clock.reset(instant("2026-07-17T12:00:00Z"), instant("2026-07-17T12:00:01Z"));
  }

  @Test
  void fiftyEquivalentContendersLoadTheSingleWinnerWithoutMoreClockCalls(UniAsserter asserter) {
    InvoiceDraftCandidate candidate = InfrastructureTestFixtures.candidate();
    asserter
        .assertThat(
            () -> joinPersistOperations(candidate),
            values ->
                assertTrue(
                    values.stream()
                        .allMatch(
                            value ->
                                value.draft().id().equals(candidate.draft().id())
                                    && value.createdAt().equals(value.updatedAt()))))
        .assertThat(
            () -> {
              int callsBeforeReplay = clock.persistenceCalls();
              return joinLookupOperations(candidate)
                  .invoke(_ -> assertEquals(callsBeforeReplay, clock.persistenceCalls()));
            },
            values ->
                assertTrue(
                    values.stream()
                        .allMatch(
                            value ->
                                value
                                    instanceof
                                    InvoiceDraftRepository.IdempotencyLookup.Equivalent)));
  }

  @Test
  void differentFingerprintProducesConflictWithinTheSameCompanyScope(UniAsserter asserter) {
    InvoiceDraftCandidate candidate = InfrastructureTestFixtures.candidate();
    byte[] different = candidate.requestFingerprint();
    different[0] ^= (byte) 0xff;
    InvoiceDraftCandidate conflicting =
        new InvoiceDraftCandidate(
            candidate.draft(),
            candidate.lineTaxIdentifiers(),
            candidate.taxTotalIdentifiers(),
            candidate.idempotencyKeyHash(),
            different,
            candidate.normalizationVersion());
    asserter
        .execute(() -> repository.persist(candidate, timeout()))
        .assertFailedWith(
            () -> repository.persist(conflicting, timeout()),
            InvoiceDraftApplicationException.class);
  }

  private Uni<@NonNull List<@NonNull PersistedInvoiceDraft>> joinPersistOperations(
      InvoiceDraftCandidate candidate) {
    var builder = Uni.join().<@NonNull PersistedInvoiceDraft>builder();
    for (int index = 0; index < 50; index++) {
      builder =
          requireNonNull(
              builder.add(inIndependentContext(() -> repository.persist(candidate, timeout()))));
    }
    return requireNonNull(
        builder
            .joinAll()
            .andCollectFailures()
            .map(values -> requireNonNull(values, "persist-operation results")),
        "persist-operation join");
  }

  private Uni<@NonNull List<InvoiceDraftRepository.@NonNull IdempotencyLookup>>
      joinLookupOperations(InvoiceDraftCandidate candidate) {
    var builder = Uni.join().<InvoiceDraftRepository.@NonNull IdempotencyLookup>builder();
    for (int index = 0; index < 50; index++) {
      builder =
          requireNonNull(
              builder.add(
                  inIndependentContext(
                      () ->
                          repository.findByIdempotency(
                              candidate.draft().companyId(),
                              requireNonNull(candidate.idempotencyKeyHash()),
                              requireNonNull(candidate.requestFingerprint()),
                              timeout()))));
    }
    return requireNonNull(
        builder
            .joinAll()
            .andCollectFailures()
            .map(values -> requireNonNull(values, "lookup-operation results")),
        "lookup-operation join");
  }

  private <T extends @NonNull Object> Uni<@NonNull T> inIndependentContext(
      UniOperation<T> operation) {
    return requireNonNull(
        Uni.createFrom()
            .emitter(
                emitter -> {
                  Context context =
                      Objects.requireNonNull(
                          VertxContext.createNewDuplicatedContext(), "duplicated context");
                  VertxContextSafetyToggle.setContextSafe(context, true);
                  context.runOnContext(
                      _ -> {
                        try {
                          operation.get().subscribe().with(emitter::complete, emitter::fail);
                        } catch (RuntimeException failure) {
                          emitter.fail(failure);
                        }
                      });
                }));
  }

  private static Instant instant(String value) {
    return requireNonNull(Instant.parse(value));
  }

  private static Duration timeout() {
    return requireNonNull(Duration.ofSeconds(5));
  }

  @FunctionalInterface
  private interface UniOperation<T extends @NonNull Object> {
    Uni<@NonNull T> get();
  }
}
