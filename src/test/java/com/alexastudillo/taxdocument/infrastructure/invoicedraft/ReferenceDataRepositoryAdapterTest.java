package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alexastudillo.taxdocument.application.invoicedraft.ApplicationTestFixtures;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftApplicationException;
import com.alexastudillo.taxdocument.application.invoicedraft.ReferenceDataPort;
import com.alexastudillo.taxdocument.domain.invoicedraft.DraftValidationException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@RunOnVertxContext
class ReferenceDataRepositoryAdapterTest {
  @Inject PostgreSqlTestResource database;
  @Inject ReferenceDataPort references;

  @BeforeEach
  void migrate() {
    database.resetSchema();
  }

  @Test
  void paymentEffectivityUsesInclusiveEmissionDateAndNotCurrentDate(UniAsserter asserter) {
    asserter.assertThat(
        () ->
            references.paymentMethod(
                ApplicationTestFixtures.PAYMENT_METHOD,
                LocalDate.of(2026, 7, 12),
                Duration.ofSeconds(2)),
        method -> assertEquals("01", method.officialCode()));
  }

  @Test
  void exhaustedBudgetStartsNoLookup(UniAsserter asserter) {
    asserter.assertFailedWith(
        () ->
            references.paymentMethod(
                ApplicationTestFixtures.PAYMENT_METHOD, LocalDate.of(2026, 7, 17), Duration.ZERO),
        InvoiceDraftApplicationException.class);
  }

  @Test
  void unsupportedReferenceRemainsAValueFreeBusinessFailure(UniAsserter asserter) {
    asserter.assertFailedWith(
        () ->
            references.paymentMethod(
                UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                LocalDate.of(2026, 7, 17),
                Duration.ofSeconds(2)),
        DraftValidationException.class);
  }
}
