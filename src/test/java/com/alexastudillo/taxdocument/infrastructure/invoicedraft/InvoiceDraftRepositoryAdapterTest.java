package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftCandidate;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftRepository;
import com.alexastudillo.taxdocument.support.FixedRequestClock;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@NullMarked
class InvoiceDraftRepositoryAdapterTest {
  @Inject PostgreSqlTestResource database;
  @Inject InvoiceDraftRepository repository;
  @Inject FixedRequestClock clock;

  @BeforeEach
  void migrate() {
    database.resetSchema();
    clock.reset(instant("2026-07-17T12:00:00Z"), instant("2026-07-17T12:00:01Z"));
  }

  @Test
  @RunOnVertxContext
  void persistsCompleteCompanyScopedAggregateWithOneEqualTimestamp(UniAsserter asserter) {
    InvoiceDraftCandidate candidate = InfrastructureTestFixtures.candidate();
    asserter
        .assertThat(
            () -> repository.persist(candidate, timeout()),
            persisted -> {
              assertEquals(candidate.draft().id(), persisted.draft().id());
              assertEquals(persisted.createdAt(), persisted.updatedAt());
              assertEquals(1, clock.persistenceCalls());
            })
        .assertEquals(() -> Panache.withSession(() -> InvoiceDraftEntity.count()), 1L)
        .assertEquals(() -> Panache.withSession(() -> InvoiceDraftIdempotencyEntity.count()), 1L)
        .assertThat(
            () ->
                repository.findByIdempotency(
                    candidate.draft().companyId(),
                    requireNonNull(candidate.idempotencyKeyHash()),
                    requireNonNull(candidate.requestFingerprint()),
                    timeout()),
            replay -> {
              assertTrue(replay instanceof InvoiceDraftRepository.IdempotencyLookup.Equivalent);
              assertEquals(1, clock.persistenceCalls());
            });
  }

  @Test
  void candidateHasNoTimestampAndGlobalCatalogsHaveNoCompanyColumn() {
    assertFalse(
        java.util.Arrays.stream(InvoiceDraftCandidate.class.getRecordComponents())
            .anyMatch(component -> component.getName().endsWith("At")));
    assertEquals(
        0,
        database.scalarLong(
            "SELECT count(*) FROM information_schema.columns WHERE table_schema='public' "
                + "AND table_name IN ('buyer_identification_type_catalog','iva_tax_rule_catalog',"
                + "'payment_method_catalog') AND column_name='company_id'"));
    InvoiceDraftCandidate candidate = InfrastructureTestFixtures.candidate();
    assertArrayEquals(candidate.requestFingerprint(), candidate.requestFingerprint());
  }

  private static Instant instant(String value) {
    return requireNonNull(Instant.parse(value));
  }

  private static Duration timeout() {
    return requireNonNull(Duration.ofSeconds(5));
  }
}
