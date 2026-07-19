package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftApplicationException;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftCandidate;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftRepository;
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
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.IntStream;
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
            () ->
                Uni.join()
                    .all(
                        IntStream.range(0, 50)
                            .mapToObj(
                                ignored ->
                                    inIndependentContext(
                                        () -> repository.persist(candidate, timeout())))
                            .toList())
                    .andCollectFailures(),
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
              return Uni.join()
                  .all(
                      IntStream.range(0, 50)
                          .mapToObj(
                              ignored ->
                                  inIndependentContext(
                                      () ->
                                          repository.findByIdempotency(
                                              candidate.draft().companyId(),
                                              requireNonNull(candidate.idempotencyKeyHash()),
                                              requireNonNull(candidate.requestFingerprint()),
                                              timeout())))
                          .toList())
                  .andCollectFailures()
                  .invoke(ignored -> assertEquals(callsBeforeReplay, clock.persistenceCalls()));
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

  private <T> Uni<T> inIndependentContext(Supplier<Uni<T>> operation) {
    return requireNonNull(
        Uni.createFrom()
            .emitter(
                emitter -> {
                  Context context =
                      Objects.requireNonNull(
                          VertxContext.createNewDuplicatedContext(), "duplicated context");
                  VertxContextSafetyToggle.setContextSafe(context, true);
                  context.runOnContext(
                      ignored -> {
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
}
