package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationApplicationException;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationCommitResult;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationFailure;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationIdentifierGenerator;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationStore;
import com.alexastudillo.taxdocument.application.fiscalpreparation.NumericCodeGenerator;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.AccessKeyGenerator;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.NumericCode;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.OfficialSequentialNumber;
import com.alexastudillo.taxdocument.support.FixedRequestClock;
import com.alexastudillo.taxdocument.support.fiscalpreparation.FiscalPreparationPostgreSqlSupport;
import com.alexastudillo.taxdocument.support.fiscalpreparation.FiscalPreparationTestFixtures;
import com.alexastudillo.taxdocument.support.fiscalpreparation.PostgreSqlCommitFaultProxy;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.pgclient.PgBuilder;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import jakarta.inject.Inject;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@NullMarked
class FiscalPreparationRollbackTest {
  @Inject FiscalPreparationPostgreSqlSupport database;
  @Inject Pool pool;
  @Inject Vertx vertx;
  @Inject FixedRequestClock clock;
  @Inject FiscalPreparationPersistenceMapper mapper;
  @Inject FiscalPreparationIdentifierGenerator identifierGenerator;
  @Inject NumericCodeGenerator numericCodeGenerator;
  @Inject FiscalPreparationCommitReconciler reconciler;
  @Inject FiscalPreparationStore store;

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
    database.insertControlledBaseline(
        FiscalPreparationTestFixtures.BASELINE,
        FiscalPreparationTestFixtures.COMPANY_UUID,
        "issuer-1",
        "establishment-1",
        FiscalPreparationTestFixtures.EMISSION_POINT,
        "001",
        "001",
        0,
        beforeCreatedAt());
  }

  @Test
  void confirmedFailureAfterBaselineWriteRollsBackWithoutConsumingASequential() {
    String accessKey =
        new AccessKeyGenerator()
            .generate(
                FiscalPreparationTestFixtures.DATE,
                FiscalPreparationTestFixtures.snapshot().issuerRuc(),
                "1",
                "001",
                "001",
                OfficialSequentialNumber.of(1),
                NumericCode.of(0))
            .value();
    assertThrows(
        IllegalStateException.class,
        () ->
            pool.withTransaction(
                    connection ->
                        connection
                            .preparedQuery(INSERT_PREPARATION)
                            .execute(
                                requireNonNull(
                                    Tuple.from(
                                        requireNonNull(
                                            List.<@NonNull Object>of(
                                                requireNonNull(
                                                    UUID.fromString(
                                                        "55555555-5555-4555-8555-555555555555")),
                                                FiscalPreparationTestFixtures.COMPANY_UUID,
                                                FiscalPreparationTestFixtures.DRAFT,
                                                FiscalPreparationTestFixtures.BASELINE,
                                                FiscalPreparationTestFixtures.EMISSION_POINT,
                                                accessKey,
                                                requireNonNull(
                                                    OffsetDateTime.ofInstant(
                                                        FiscalPreparationTestFixtures.CREATED_AT,
                                                        ZoneOffset.UTC)))))))
                            .chain(
                                () ->
                                    connection
                                        .preparedQuery(
                                            "UPDATE official_sequence_baseline "
                                                + "SET last_allocated = 1, updated_at = $2 "
                                                + "WHERE id = $1")
                                        .execute(
                                            Tuple.of(
                                                FiscalPreparationTestFixtures.BASELINE,
                                                OffsetDateTime.ofInstant(
                                                    FiscalPreparationTestFixtures.CREATED_AT,
                                                    ZoneOffset.UTC))))
                            .replaceWith(
                                Uni.createFrom().failure(new IllegalStateException("fault"))))
                .await()
                .indefinitely());
    assertEquals(
        0,
        database.lastAllocated(
            FiscalPreparationTestFixtures.COMPANY_UUID, FiscalPreparationTestFixtures.BASELINE));
    assertEquals(0L, database.rowCount("fiscal_preparation"));
  }

  @Test
  void lostCommitAcknowledgementNeverReallocatesAndRetryConvergesOnDatabaseTruth() {
    Row endpoint =
        Objects.requireNonNull(
            pool.query(
                    "SELECT host(inet_server_addr()) AS host, inet_server_port() AS port, "
                        + "current_database() AS database, current_user AS username")
                .execute()
                .await()
                .atMost(Duration.ofSeconds(5))
                .iterator()
                .next(),
            "endpoint");
    PgConnectOptions configured =
        new PgConnectOptions()
            .setHost(Objects.requireNonNull(endpoint.getString("host"), "host"))
            .setPort(Objects.requireNonNull(endpoint.getInteger("port"), "port"))
            .setDatabase(Objects.requireNonNull(endpoint.getString("database"), "database"))
            .setUser(Objects.requireNonNull(endpoint.getString("username"), "username"))
            .setPassword(
                ConfigProvider.getConfig().getValue("quarkus.datasource.password", String.class));
    try (PostgreSqlCommitFaultProxy proxy =
        PostgreSqlCommitFaultProxy.start(
            requireNonNull(configured.getHost()), configured.getPort())) {
      PgConnectOptions proxied =
          new PgConnectOptions(configured).setHost("127.0.0.1").setPort(proxy.port());
      Pool faultPool =
          PgBuilder.pool()
              .using(vertx)
              .connectingTo(proxied)
              .with(new PoolOptions().setMaxSize(1))
              .build();
      try {
        FiscalPreparationRepositoryAdapter faultStore =
            new FiscalPreparationRepositoryAdapter(
                requireNonNull(faultPool),
                clock,
                mapper,
                identifierGenerator,
                numericCodeGenerator,
                reconciler);
        proxy.interruptNextCommitAcknowledgement();
        try {
          faultStore
              .commit(FiscalPreparationTestFixtures.intent(), timeout())
              .await()
              .indefinitely();
        } catch (FiscalPreparationApplicationException failure) {
          assertEquals(
              FiscalPreparationFailure.Code.PREPARATION_OUTCOME_UNKNOWN, failure.failure().code());
        }
        assertTrue(proxy.commitWasObserved());
      } finally {
        faultPool.close().await().atMost(Duration.ofSeconds(5));
      }
    }

    FiscalPreparationCommitResult retry =
        store.commit(FiscalPreparationTestFixtures.intent(), timeout()).await().indefinitely();
    assertTrue(
        retry instanceof FiscalPreparationCommitResult.Created
            || retry instanceof FiscalPreparationCommitResult.Replay);
    assertEquals(1L, database.rowCount("fiscal_preparation"));
    assertEquals(
        1,
        database.lastAllocated(
            FiscalPreparationTestFixtures.COMPANY_UUID, FiscalPreparationTestFixtures.BASELINE));
  }

  private static Duration timeout() {
    return requireNonNull(Duration.ofSeconds(5));
  }

  private static java.time.Instant beforeCreatedAt() {
    return requireNonNull(FiscalPreparationTestFixtures.CREATED_AT.minusSeconds(60));
  }

  @Test
  void commitKnowledgeClassificationNeverTreatsAConnectionLossAsConfirmedZeroState() {
    PostgreSqlCommitOutcomeClassifier classifier = new PostgreSqlCommitOutcomeClassifier();
    assertEquals(
        PostgreSqlCommitOutcomeClassifier.Knowledge.CONFIRMED_ROLLBACK,
        classifier.classify(
            new SQLException("serialization", "40001"),
            PostgreSqlCommitOutcomeClassifier.CommitPhase.COMMIT_INITIATED,
            false));
    for (String state : List.of("23502", "23503", "23505", "23514", "40P01")) {
      assertEquals(
          PostgreSqlCommitOutcomeClassifier.Knowledge.CONFIRMED_ROLLBACK,
          classifier.classify(
              new SQLException("conclusive", state),
              PostgreSqlCommitOutcomeClassifier.CommitPhase.COMMIT_INITIATED,
              false));
    }
    assertEquals(
        PostgreSqlCommitOutcomeClassifier.Knowledge.UNKNOWN,
        classifier.classify(
            new SQLException("connection", "08007"),
            PostgreSqlCommitOutcomeClassifier.CommitPhase.COMMIT_INITIATED,
            false));
    assertEquals(
        PostgreSqlCommitOutcomeClassifier.Knowledge.UNKNOWN,
        classifier.classify(
            new SQLException("outcome", "40003"),
            PostgreSqlCommitOutcomeClassifier.CommitPhase.COMMIT_INITIATED,
            false));
    for (String state : List.of("57014", "57P01", "57P02", "57P03")) {
      assertEquals(
          PostgreSqlCommitOutcomeClassifier.Knowledge.UNKNOWN,
          classifier.classify(
              new SQLException("server", state),
              PostgreSqlCommitOutcomeClassifier.CommitPhase.COMMIT_INITIATED,
              false));
    }
  }

  private static final String INSERT_PREPARATION =
      """
      INSERT INTO fiscal_preparation (
        id, company_id, invoice_draft_id, official_sequence_baseline_id, emission_date,
        issuer_reference, issuer_ruc, legal_name, commercial_name, head_office_address,
        accounting_required, special_taxpayer_resolution, withholding_agent_resolution,
        rimpe_classification, large_contributor_resolution, large_contributor_legend,
        establishment_reference, establishment_code, establishment_address,
        emission_point_id, emission_point_code, environment_code, document_type_code,
        emission_type_code, source_authority, source_revision, source_effective_from,
        source_effective_through, source_observed_at, technical_rule_id,
        technical_rule_modified_on, numeric_code_policy_id, official_sequential_number,
        numeric_code, access_key, created_at
      ) VALUES (
        $1, $2, $3, $4, DATE '2026-07-18',
        'issuer-1', '1792146739001', 'Issuer S.A.', 'Issuer', 'Head Office',
        true, NULL, NULL, 'NONE', NULL, NULL,
        'establishment-1', '001', 'Establishment Address',
        $5, '001', '1', '01', '1', 'SRI', 'revision-1', DATE '2026-07-01',
        NULL, TIMESTAMPTZ '2026-07-18 11:59:00Z', 'SRI-OFFLINE-2.33',
        DATE '2026-07-13', 'SECURE_RANDOM_8_V1', '000000001',
        '00000000', $6, $7
      )
      """;
}
