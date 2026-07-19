package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import static java.util.Objects.requireNonNull;

import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftApplicationException;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftCandidate;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftRepository;
import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceLine;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@RunOnVertxContext
@NullMarked
class InvoiceDraftRollbackTest {
  @Inject PostgreSqlTestResource database;
  @Inject InvoiceDraftRepository repository;

  @BeforeEach
  void migrate() {
    database.resetSchema();
  }

  @Test
  void constraintFailureAfterEarlierWritesRollsBackEveryAggregatePhase(UniAsserter asserter) {
    InvoiceDraftCandidate valid = InfrastructureTestFixtures.candidate();
    @Nullable InvoiceLine nullableLine = valid.draft().lines().getFirst();
    UUID lineId = requireNonNull(nullableLine, "first invoice line").id();
    Map<@NonNull UUID, @NonNull UUID> lineTaxIdentifiers =
        requireNonNull(Map.of(lineId, new UUID(0L, 0L)));
    InvoiceDraftCandidate invalid =
        new InvoiceDraftCandidate(
            valid.draft(),
            lineTaxIdentifiers,
            valid.taxTotalIdentifiers(),
            valid.idempotencyKeyHash(),
            valid.requestFingerprint(),
            valid.normalizationVersion());
    asserter
        .assertFailedWith(
            () -> repository.persist(invalid, requireNonNull(Duration.ofSeconds(5))),
            InvoiceDraftApplicationException.class)
        .assertEquals(
            () -> Panache.withSession(() -> InvoiceDraftEntity.count("id", invalid.draft().id())),
            0L)
        .assertEquals(
            () ->
                Panache.withSession(
                    () -> InvoiceLineEntity.count("invoiceDraftId", invalid.draft().id())),
            0L)
        .assertEquals(
            () ->
                Panache.withSession(
                    () ->
                        InvoiceDraftIdempotencyEntity.count(
                            "companyId = ?1 and idempotencyKeyHash = ?2",
                            invalid.draft().companyId().value(),
                            invalid.idempotencyKeyHash())),
            0L);
  }

  @Test
  void exhaustedBudgetDoesNotBeginTheTransaction(UniAsserter asserter) {
    InvoiceDraftCandidate candidate = InfrastructureTestFixtures.candidate();
    asserter
        .assertFailedWith(
            () -> repository.persist(candidate, requireNonNull(Duration.ZERO)),
            InvoiceDraftApplicationException.class)
        .assertEquals(
            () -> Panache.withSession(() -> InvoiceDraftEntity.count("id", candidate.draft().id())),
            0L);
  }
}
