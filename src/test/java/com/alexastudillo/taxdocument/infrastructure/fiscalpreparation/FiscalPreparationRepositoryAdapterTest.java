package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationApplicationException;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationCommitResult;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationFailure;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationLookup;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationStore;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import com.alexastudillo.taxdocument.support.FixedRequestClock;
import com.alexastudillo.taxdocument.support.fiscalpreparation.FiscalPreparationPostgreSqlSupport;
import com.alexastudillo.taxdocument.support.fiscalpreparation.FiscalPreparationTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.sqlclient.Pool;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@NullMarked
class FiscalPreparationRepositoryAdapterTest {
  @Inject FiscalPreparationStore store;
  @Inject FiscalPreparationPostgreSqlSupport database;
  @Inject FixedRequestClock clock;
  @Inject Pool pool;

  @BeforeEach
  void reset() {
    database.resetSchema();
    clock.reset(FiscalPreparationTestFixtures.CREATED_AT, FiscalPreparationTestFixtures.CREATED_AT);
    database.insertControlledDraft(
        FiscalPreparationTestFixtures.COMPANY_UUID,
        FiscalPreparationTestFixtures.DRAFT,
        FiscalPreparationTestFixtures.EMISSION_POINT,
        FiscalPreparationTestFixtures.DATE,
        beforeCreatedAt());
  }

  @Test
  void everyLookupIsCompanyScopedAndCrossCompanyIsIndistinguishableFromAbsence() {
    @Nullable FiscalPreparationLookup nullableOwn =
        store
            .lookup(
                FiscalPreparationTestFixtures.COMPANY,
                FiscalPreparationTestFixtures.DRAFT,
                timeout())
            .await()
            .indefinitely();
    FiscalPreparationLookup own = requireNonNull(nullableOwn, "own-company lookup");
    assertInstanceOf(FiscalPreparationLookup.EligibleDraft.class, own);

    @Nullable FiscalPreparationLookup nullableOther =
        store
            .lookup(
                new CompanyId(
                    requireNonNull(UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"))),
                FiscalPreparationTestFixtures.DRAFT,
                timeout())
            .await()
            .indefinitely();
    FiscalPreparationLookup other = requireNonNull(nullableOther, "cross-company lookup");
    assertInstanceOf(FiscalPreparationLookup.NotFound.class, other);
  }

  @Test
  void locksDraftThenExactBaselineAndAtomicallyCommitsOneIdentityAndOneStep() {
    database.insertControlledBaseline(
        FiscalPreparationTestFixtures.BASELINE,
        FiscalPreparationTestFixtures.COMPANY_UUID,
        "issuer-1",
        "establishment-1",
        FiscalPreparationTestFixtures.EMISSION_POINT,
        "001",
        "001",
        122,
        beforeCreatedAt());
    FiscalPreparationPostgreSqlSupport.DraftSnapshot before =
        database.draftSnapshot(
            FiscalPreparationTestFixtures.COMPANY_UUID, FiscalPreparationTestFixtures.DRAFT);

    @Nullable FiscalPreparationCommitResult nullableResult =
        store.commit(FiscalPreparationTestFixtures.intent(), timeout()).await().indefinitely();
    FiscalPreparationCommitResult result = requireNonNull(nullableResult, "commit result");
    FiscalPreparationCommitResult.Created created =
        assertInstanceOf(FiscalPreparationCommitResult.Created.class, result);
    assertEquals("000000123", created.preparation().officialSequentialNumber().value());
    assertEquals(49, created.preparation().accessKey().value().length());
    assertEquals(FiscalPreparationTestFixtures.CREATED_AT, created.preparation().createdAt());
    assertEquals(
        123,
        database.lastAllocated(
            FiscalPreparationTestFixtures.COMPANY_UUID, FiscalPreparationTestFixtures.BASELINE));
    assertEquals(
        1L,
        database.fiscalPreparationCount(
            FiscalPreparationTestFixtures.COMPANY_UUID, FiscalPreparationTestFixtures.DRAFT));
    assertEquals(
        before,
        database.draftSnapshot(
            FiscalPreparationTestFixtures.COMPANY_UUID, FiscalPreparationTestFixtures.DRAFT));

    @Nullable FiscalPreparationCommitResult nullableReplay =
        store.commit(FiscalPreparationTestFixtures.intent(), timeout()).await().indefinitely();
    FiscalPreparationCommitResult replay = requireNonNull(nullableReplay, "commit replay");
    assertEquals(
        created.preparation(),
        assertInstanceOf(FiscalPreparationCommitResult.Replay.class, replay).preparation());
    assertEquals(
        123,
        database.lastAllocated(
            FiscalPreparationTestFixtures.COMPANY_UUID, FiscalPreparationTestFixtures.BASELINE));
  }

  @Test
  void missingBaselineFailsClosedBeforeIdentityOrPersistenceAndConsumesNothing() {
    FiscalPreparationApplicationException failure =
        assertThrows(
            FiscalPreparationApplicationException.class,
            () ->
                store
                    .commit(FiscalPreparationTestFixtures.intent(), timeout())
                    .await()
                    .indefinitely());
    assertEquals(
        FiscalPreparationFailure.Code.OFFICIAL_SEQUENCE_BASELINE_MISSING, failure.failure().code());
    assertEquals(0L, database.rowCount("official_sequence_baseline"));
    assertEquals(0L, database.rowCount("fiscal_preparation"));
  }

  @Test
  void exhaustedBaselineFailsClosedWithoutAdvancementOrPreparation() {
    database.insertControlledBaseline(
        FiscalPreparationTestFixtures.BASELINE,
        FiscalPreparationTestFixtures.COMPANY_UUID,
        "issuer-1",
        "establishment-1",
        FiscalPreparationTestFixtures.EMISSION_POINT,
        "001",
        "001",
        999_999_999,
        beforeCreatedAt());

    FiscalPreparationApplicationException failure =
        assertThrows(
            FiscalPreparationApplicationException.class,
            () ->
                store
                    .commit(FiscalPreparationTestFixtures.intent(), timeout())
                    .await()
                    .indefinitely());
    assertEquals(
        FiscalPreparationFailure.Code.OFFICIAL_SEQUENCE_EXHAUSTED, failure.failure().code());
    assertEquals(
        999_999_999,
        database.lastAllocated(
            FiscalPreparationTestFixtures.COMPANY_UUID, FiscalPreparationTestFixtures.BASELINE));
    assertEquals(0L, database.rowCount("fiscal_preparation"));
  }

  @Test
  void defensivelyRejectsAnInvalidExistingBaselineWithoutAllocating() {
    pool.query(
            "ALTER TABLE official_sequence_baseline "
                + "DROP CONSTRAINT ck_official_sequence_baseline_value")
        .execute()
        .await()
        .indefinitely();
    database.insertControlledBaseline(
        FiscalPreparationTestFixtures.BASELINE,
        FiscalPreparationTestFixtures.COMPANY_UUID,
        "issuer-1",
        "establishment-1",
        FiscalPreparationTestFixtures.EMISSION_POINT,
        "001",
        "001",
        -1,
        beforeCreatedAt());

    FiscalPreparationApplicationException failure =
        assertThrows(
            FiscalPreparationApplicationException.class,
            () ->
                store
                    .commit(FiscalPreparationTestFixtures.intent(), timeout())
                    .await()
                    .indefinitely());
    assertEquals(
        FiscalPreparationFailure.Code.OFFICIAL_SEQUENCE_BASELINE_INVALID, failure.failure().code());
    assertEquals(
        -1,
        database.lastAllocated(
            FiscalPreparationTestFixtures.COMPANY_UUID, FiscalPreparationTestFixtures.BASELINE));
    assertEquals(0L, database.rowCount("fiscal_preparation"));
  }

  private static Duration timeout() {
    return requireNonNull(Duration.ofSeconds(5));
  }

  private static java.time.Instant beforeCreatedAt() {
    return requireNonNull(FiscalPreparationTestFixtures.CREATED_AT.minusSeconds(60));
  }
}
