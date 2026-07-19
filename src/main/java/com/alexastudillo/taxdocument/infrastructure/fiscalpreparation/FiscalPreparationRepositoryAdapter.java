package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationApplicationException;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationCommitIntent;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationCommitResult;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationCommitTracker;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationFailure;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationIdentifierGenerator;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationLookup;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationStore;
import com.alexastudillo.taxdocument.application.fiscalpreparation.InvoiceDraftPreparationView;
import com.alexastudillo.taxdocument.application.fiscalpreparation.NumericCodeGenerator;
import com.alexastudillo.taxdocument.application.requestcontext.RequestClock;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.AccessKey;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.AccessKeyGenerator;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalContextSnapshot;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalPreparation;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.NumericCode;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.OfficialSequenceBaseline;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.OfficialSequenceScope;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.OfficialSequentialNumber;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import com.alexastudillo.taxdocument.infrastructure.persistence.ReactiveOperationBudget;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.SqlConnection;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Company-scoped reactive store with fixed draft-first and exact-baseline-second locking. */
@ApplicationScoped
@NullMarked
public final class FiscalPreparationRepositoryAdapter implements FiscalPreparationStore {
  static final String SELECT_PREPARATION =
      "SELECT * FROM fiscal_preparation WHERE company_id = $1 AND invoice_draft_id = $2";
  private static final String SELECT_DRAFT =
      "SELECT id, company_id, emission_point_id, emission_date, status FROM invoice_draft "
          + "WHERE company_id = $1 AND id = $2";
  private static final String LOCK_DRAFT = SELECT_DRAFT + " FOR UPDATE";
  private static final String LOCK_BASELINE =
      "SELECT * FROM official_sequence_baseline WHERE company_id = $1 "
          + "AND issuer_reference = $2 AND establishment_reference = $3 "
          + "AND emission_point_id = $4 AND establishment_code = $5 "
          + "AND emission_point_code = $6 AND document_type_code = $7 FOR UPDATE";
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
        $1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,
        $19,$20,$21,$22,$23,$24,$25,$26,$27,$28,$29,$30,$31,$32,$33,$34,$35,$36
      ) RETURNING *
      """;
  private static final String ADVANCE_BASELINE =
      "UPDATE official_sequence_baseline SET last_allocated = $3, updated_at = $4 "
          + "WHERE company_id = $1 AND id = $2 AND last_allocated = $3 - 1";

  private final Pool pool;
  private final RequestClock clock;
  private final FiscalPreparationPersistenceMapper mapper;
  private final FiscalPreparationIdentifierGenerator identifierGenerator;
  private final NumericCodeGenerator numericCodeGenerator;
  private final AccessKeyGenerator accessKeyGenerator = new AccessKeyGenerator();
  private final PostgreSqlCommitOutcomeClassifier outcomeClassifier =
      new PostgreSqlCommitOutcomeClassifier();
  private final FiscalPreparationCommitReconciler reconciler;
  private final Duration operationTimeout;
  private final Duration writeTimeout;
  private final Duration lockTimeout;

  public FiscalPreparationRepositoryAdapter(
      Pool pool,
      RequestClock clock,
      FiscalPreparationPersistenceMapper mapper,
      FiscalPreparationIdentifierGenerator identifierGenerator,
      NumericCodeGenerator numericCodeGenerator,
      FiscalPreparationCommitReconciler reconciler) {
    this.pool = Objects.requireNonNull(pool, "pool");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator");
    this.numericCodeGenerator =
        Objects.requireNonNull(numericCodeGenerator, "numericCodeGenerator");
    this.reconciler = Objects.requireNonNull(reconciler, "reconciler");
    @Nullable Duration configuredOperationTimeout =
        ConfigProvider.getConfig()
            .getValue("fiscal-preparation.persistence.operation-timeout", Duration.class);
    @Nullable Duration configuredWriteTimeout =
        ConfigProvider.getConfig()
            .getValue("fiscal-preparation.persistence.write-transaction-timeout", Duration.class);
    @Nullable Duration configuredLockTimeout =
        ConfigProvider.getConfig()
            .getValue("fiscal-preparation.persistence.lock-timeout", Duration.class);
    this.operationTimeout =
        requireHydrated(configuredOperationTimeout, "fiscal preparation operation timeout");
    this.writeTimeout = requireHydrated(configuredWriteTimeout, "fiscal preparation write timeout");
    this.lockTimeout = requireHydrated(configuredLockTimeout, "fiscal preparation lock timeout");
  }

  @Override
  public Uni<@NonNull FiscalPreparationLookup> lookup(
      CompanyId companyId, UUID invoiceDraftId, Duration remaining) {
    Objects.requireNonNull(companyId, "companyId");
    Objects.requireNonNull(invoiceDraftId, "invoiceDraftId");
    return boundedLookup(
        requireUni(
            findPreparation(companyId, invoiceDraftId)
                .onItem()
                .transformToUni(
                    existing ->
                        existing instanceof PreparationFound found
                            ? Uni.createFrom()
                                .item(new FiscalPreparationLookup.Existing(found.preparation()))
                            : findDraft(companyId, invoiceDraftId)),
            "lookup operation"),
        remaining);
  }

  @Override
  public Uni<@NonNull FiscalPreparationCommitResult> commit(
      FiscalPreparationCommitIntent intent, Duration remaining) {
    return commit(intent, remaining, new FiscalPreparationCommitTracker());
  }

  @Override
  public Uni<@NonNull FiscalPreparationCommitResult> commit(
      FiscalPreparationCommitIntent intent,
      Duration remaining,
      FiscalPreparationCommitTracker commitTracker) {
    Objects.requireNonNull(intent, "intent");
    Objects.requireNonNull(commitTracker, "commitTracker");
    ReactiveOperationBudget budget = requireBudget(remaining, writeTimeout);
    AtomicBoolean localChangesCompleted = new AtomicBoolean();
    Uni<@NonNull FiscalPreparationCommitResult> operation =
        pool.withTransaction(
            connection -> {
              SqlConnection transactionConnection =
                  requireHydrated(connection, "transaction connection");
              return configureTransaction(
                      requireHydrated(transactionConnection, "transaction connection"),
                      budget.timeout())
                  .chain(
                      () ->
                          commitLocked(
                              requireHydrated(transactionConnection, "transaction connection"),
                              intent))
                  .invoke(
                      () -> {
                        localChangesCompleted.set(true);
                        commitTracker.possibleCommit();
                      });
            });
    return requireUni(
        operation
            .invoke(_ -> commitTracker.committed())
            .ifNoItem()
            .after(budget.timeout())
            .failWith(TimeoutException::new)
            .onFailure()
            .recoverWithUni(
                failure ->
                    recoverFailure(
                        requireHydrated(failure, "transaction failure"),
                        intent.draft().companyId(),
                        intent.draft().invoiceDraftId(),
                        remaining,
                        budget.timeoutOwner(),
                        localChangesCompleted.get(),
                        commitTracker)),
        "commit operation");
  }

  private Uni<@NonNull FiscalPreparationCommitResult> commitLocked(
      SqlConnection connection, FiscalPreparationCommitIntent intent) {
    InvoiceDraftPreparationView expected = intent.draft();
    return requireUni(
        connection
            .preparedQuery(LOCK_DRAFT)
            .execute(Tuple.of(expected.companyId().value(), expected.invoiceDraftId()))
            .onItem()
            .transformToUni(
                rows ->
                    afterDraftLock(connection, intent, requireHydrated(rows, "locked draft rows"))),
        "draft lock");
  }

  private Uni<@NonNull FiscalPreparationCommitResult> afterDraftLock(
      SqlConnection connection, FiscalPreparationCommitIntent intent, RowSet<Row> rows) {
    Row row = first(rows);
    if (row == null) {
      return requireUni(
          Uni.createFrom().failure(failure(FiscalPreparationFailure.Code.INVOICE_DRAFT_NOT_FOUND)),
          "draft not found failure");
    }
    if (!"DRAFT".equals(row.getString("status"))) {
      return requireUni(
          Uni.createFrom()
              .failure(failure(FiscalPreparationFailure.Code.INVOICE_DRAFT_NOT_PREPARABLE)),
          "draft not preparable failure");
    }
    if (!intent.draft().emissionPointId().equals(row.getUUID("emission_point_id"))
        || !intent.draft().emissionDate().equals(row.getLocalDate("emission_date"))) {
      return requireUni(
          Uni.createFrom()
              .failure(failure(FiscalPreparationFailure.Code.INVOICE_DRAFT_NOT_PREPARABLE)),
          "draft not preparable failure");
    }
    return requireUni(
        findPreparation(connection, intent.draft().companyId(), intent.draft().invoiceDraftId())
            .onItem()
            .transformToUni(
                existing ->
                    existing instanceof PreparationFound found
                        ? Uni.createFrom()
                            .item(new FiscalPreparationCommitResult.Replay(found.preparation()))
                        : lockBaseline(connection, intent)),
        "post-draft-lock result");
  }

  private Uni<@NonNull FiscalPreparationCommitResult> lockBaseline(
      SqlConnection connection, FiscalPreparationCommitIntent intent) {
    FiscalContextSnapshot snapshot = intent.snapshot();
    return requireUni(
        connection
            .preparedQuery(LOCK_BASELINE)
            .execute(
                Tuple.of(
                        intent.draft().companyId().value(),
                        snapshot.issuerReference(),
                        snapshot.establishmentReference(),
                        snapshot.emissionPointId(),
                        snapshot.establishmentCode(),
                        snapshot.emissionPointCode())
                    .addString(snapshot.documentTypeCode()))
            .onItem()
            .transformToUni(
                rows ->
                    allocate(connection, intent, requireHydrated(rows, "locked baseline rows"))),
        "baseline lock");
  }

  private Uni<@NonNull FiscalPreparationCommitResult> allocate(
      SqlConnection connection, FiscalPreparationCommitIntent intent, RowSet<Row> rows) {
    Row row = first(rows);
    if (row == null) {
      return requireUni(
          Uni.createFrom()
              .failure(failure(FiscalPreparationFailure.Code.OFFICIAL_SEQUENCE_BASELINE_MISSING)),
          "missing baseline failure");
    }
    OfficialSequenceBaseline baseline;
    try {
      baseline = baseline(row);
    } catch (IllegalArgumentException | NullPointerException exception) {
      return requireUni(
          Uni.createFrom()
              .failure(failure(FiscalPreparationFailure.Code.OFFICIAL_SEQUENCE_BASELINE_INVALID)),
          "invalid baseline failure");
    }
    OfficialSequenceBaseline.AllocationDecision decision = baseline.allocationDecision();
    if (decision instanceof OfficialSequenceBaseline.AllocationDecision.Exhausted) {
      return requireUni(
          Uni.createFrom()
              .failure(failure(FiscalPreparationFailure.Code.OFFICIAL_SEQUENCE_EXHAUSTED)),
          "exhausted sequence failure");
    }
    OfficialSequentialNumber sequential =
        ((OfficialSequenceBaseline.AllocationDecision.Next) decision).sequentialNumber();
    return persistAllocation(connection, intent, baseline, sequential);
  }

  private Uni<@NonNull FiscalPreparationCommitResult> persistAllocation(
      SqlConnection connection,
      FiscalPreparationCommitIntent intent,
      OfficialSequenceBaseline baseline,
      OfficialSequentialNumber sequential) {
    NumericCode numericCode = numericCodeGenerator.nextNumericCode();
    AccessKey accessKey;
    try {
      FiscalContextSnapshot snapshot = intent.snapshot();
      accessKey =
          accessKeyGenerator.generate(
              intent.draft().emissionDate(),
              snapshot.issuerRuc(),
              snapshot.environmentCode(),
              snapshot.establishmentCode(),
              snapshot.emissionPointCode(),
              sequential,
              numericCode);
    } catch (IllegalArgumentException exception) {
      return requireUni(
          Uni.createFrom().failure(failure(FiscalPreparationFailure.Code.ACCESS_KEY_INVALID)),
          "invalid access-key failure");
    }
    FiscalPreparation preparation =
        new FiscalPreparation(
            identifierGenerator.nextIdentifier(),
            intent.draft().companyId(),
            intent.draft().invoiceDraftId(),
            baseline.id(),
            intent.draft().emissionDate(),
            intent.snapshot(),
            sequential,
            numericCode,
            accessKey,
            clock.persistenceTime());
    OffsetDateTime updatedAt =
        requireHydrated(
            OffsetDateTime.ofInstant(preparation.createdAt(), ZoneOffset.UTC),
            "preparation updatedAt");
    return requireUni(
        connection
            .preparedQuery(INSERT_PREPARATION)
            .execute(mapper.toInsertParameters(preparation))
            .onItem()
            .transformToUni(
                inserted ->
                    afterPreparationInsert(
                        connection,
                        baseline,
                        sequential,
                        requireHydrated(updatedAt, "preparation updatedAt"),
                        requireHydrated(inserted, "inserted preparation rows"))),
        "preparation insert");
  }

  private Uni<@NonNull FiscalPreparationCommitResult> afterPreparationInsert(
      SqlConnection connection,
      OfficialSequenceBaseline baseline,
      OfficialSequentialNumber sequential,
      OffsetDateTime updatedAt,
      RowSet<Row> inserted) {
    Row insertedRow = first(inserted);
    if (inserted.rowCount() != 1 || insertedRow == null) {
      return requireUni(
          Uni.createFrom().failure(new IllegalStateException("insert did not win")),
          "lost preparation insert");
    }
    FiscalPreparation persisted = mapper.fromRow(insertedRow);
    return requireUni(
        connection
            .preparedQuery(ADVANCE_BASELINE)
            .execute(
                Tuple.of(
                    persisted.companyId().value(), baseline.id(), sequential.number(), updatedAt))
            .onItem()
            .transformToUni(
                updated ->
                    updated.rowCount() == 1
                        ? Uni.createFrom()
                            .item(new FiscalPreparationCommitResult.Created(persisted))
                        : Uni.createFrom()
                            .failure(new IllegalStateException("baseline did not advance"))),
        "baseline advance result");
  }

  private Uni<Void> configureTransaction(SqlConnection connection, Duration statementTimeout) {
    long statementMillis = Math.max(1L, statementTimeout.toMillis());
    long lockMillis = Math.max(1L, Math.min(lockTimeout.toMillis(), statementMillis));
    return requireHydrated(
        connection
            .preparedQuery(
                "SELECT set_config('statement_timeout', $1, true), "
                    + "set_config('lock_timeout', $2, true)")
            .execute(Tuple.of(statementMillis + "ms", lockMillis + "ms"))
            .replaceWithVoid(),
        "transaction configuration");
  }

  private Uni<@NonNull FiscalPreparationCommitResult> recoverFailure(
      Throwable failure,
      CompanyId companyId,
      UUID invoiceDraftId,
      Duration remaining,
      ReactiveOperationBudget.TimeoutOwner timeoutOwner,
      boolean localChangesCompleted,
      FiscalPreparationCommitTracker commitTracker) {
    if (failure instanceof FiscalPreparationApplicationException applicationFailure) {
      commitTracker.confirmedRollback();
      return requireUni(
          Uni.createFrom()
              .failure(
                  new FiscalPreparationApplicationException(
                      applicationFailure
                          .failure()
                          .withCommitKnowledge(
                              FiscalPreparationFailure.CommitKnowledge.CONFIRMED_ROLLBACK))),
          "confirmed rollback failure");
    }
    PostgreSqlCommitOutcomeClassifier.Knowledge knowledge =
        outcomeClassifier.classify(
            failure,
            localChangesCompleted
                ? PostgreSqlCommitOutcomeClassifier.CommitPhase.COMMIT_INITIATED
                : PostgreSqlCommitOutcomeClassifier.CommitPhase.BEFORE_COMMIT,
            !localChangesCompleted);
    if (knowledge == PostgreSqlCommitOutcomeClassifier.Knowledge.UNKNOWN) {
      commitTracker.possibleCommit();
      return requireUni(
          reconciler
              .reconcile(companyId, invoiceDraftId, remaining)
              .onItem()
              .transformToUni(
                  result ->
                      reconciledResult(
                          requireHydrated(result, "reconciliation result"), commitTracker)),
          "reconciled commit result");
    }
    commitTracker.confirmedRollback();
    FiscalPreparationFailure.Code code;
    if (failure instanceof TimeoutException
        && timeoutOwner == ReactiveOperationBudget.TimeoutOwner.REQUEST_DEADLINE) {
      code = FiscalPreparationFailure.Code.REQUEST_TIMEOUT;
    } else {
      code =
          switch (outcomeClassifier.failureCode(failure)) {
            case BASELINE_INVALID ->
                FiscalPreparationFailure.Code.OFFICIAL_SEQUENCE_BASELINE_INVALID;
            case ACCESS_KEY_INVALID -> FiscalPreparationFailure.Code.ACCESS_KEY_INVALID;
            case DRAFT_NOT_PREPARABLE -> FiscalPreparationFailure.Code.INVOICE_DRAFT_NOT_PREPARABLE;
            case PERSISTENCE_FAILURE -> FiscalPreparationFailure.Code.PERSISTENCE_FAILURE;
          };
    }
    return requireUni(
        Uni.createFrom()
            .failure(
                new FiscalPreparationApplicationException(
                    FiscalPreparationFailure.of(code)
                        .withCommitKnowledge(
                            FiscalPreparationFailure.CommitKnowledge.CONFIRMED_ROLLBACK),
                    failure)),
        "classified commit failure");
  }

  private static Uni<@NonNull FiscalPreparationCommitResult> reconciledResult(
      FiscalPreparationCommitReconciler.Result result,
      FiscalPreparationCommitTracker commitTracker) {
    if (result instanceof FiscalPreparationCommitReconciler.Result.Winner winner) {
      commitTracker.committed();
      return requireUni(
          Uni.createFrom().item(new FiscalPreparationCommitResult.Replay(winner.preparation())),
          "replayed preparation");
    }
    commitTracker.possibleCommit();
    return requireUni(
        Uni.createFrom()
            .failure(failure(FiscalPreparationFailure.Code.PREPARATION_OUTCOME_UNKNOWN)),
        "unknown preparation outcome");
  }

  private Uni<@NonNull PreparationRead> findPreparation(CompanyId companyId, UUID invoiceDraftId) {
    return requireUni(
        pool.preparedQuery(SELECT_PREPARATION)
            .execute(Tuple.of(companyId.value(), invoiceDraftId))
            .onItem()
            .transform(rows -> preparationRead(requireHydrated(rows, "preparation query rows"))),
        "preparation query");
  }

  private Uni<@NonNull PreparationRead> findPreparation(
      SqlConnection connection, CompanyId companyId, UUID invoiceDraftId) {
    return requireUni(
        connection
            .preparedQuery(SELECT_PREPARATION)
            .execute(Tuple.of(companyId.value(), invoiceDraftId))
            .onItem()
            .transform(rows -> preparationRead(requireHydrated(rows, "preparation query rows"))),
        "transactional preparation query");
  }

  private PreparationRead preparationRead(RowSet<Row> rows) {
    Row row = first(rows);
    return row == null ? new PreparationAbsent() : new PreparationFound(mapper.fromRow(row));
  }

  private Uni<@NonNull FiscalPreparationLookup> findDraft(
      CompanyId companyId, UUID invoiceDraftId) {
    return requireUni(
        pool.preparedQuery(SELECT_DRAFT)
            .execute(Tuple.of(companyId.value(), invoiceDraftId))
            .onItem()
            .transform(
                rows -> {
                  Row row = first(requireHydrated(rows, "invoice draft query rows"));
                  if (row == null) {
                    return new FiscalPreparationLookup.NotFound();
                  }
                  if (!"DRAFT".equals(row.getString("status"))) {
                    return new FiscalPreparationLookup.NotPreparable(
                        FiscalPreparationLookup.NotPreparableReason.NON_DRAFT);
                  }
                  return new FiscalPreparationLookup.EligibleDraft(
                      new InvoiceDraftPreparationView(
                          Objects.requireNonNull(row.getUUID("id"), "invoiceDraftId"),
                          companyId,
                          Objects.requireNonNull(
                              row.getUUID("emission_point_id"), "emissionPointId"),
                          Objects.requireNonNull(row.getLocalDate("emission_date"), "emissionDate"),
                          "DRAFT"));
                }),
        "invoice draft lookup");
  }

  private Uni<@NonNull FiscalPreparationLookup> boundedLookup(
      Uni<@NonNull FiscalPreparationLookup> operation, Duration remaining) {
    ReactiveOperationBudget budget = requireBudget(remaining, operationTimeout);
    return requireUni(
        operation
            .ifNoItem()
            .after(budget.timeout())
            .failWith(
                () ->
                    failure(
                        budget.timeoutOwner()
                                == ReactiveOperationBudget.TimeoutOwner.REQUEST_DEADLINE
                            ? FiscalPreparationFailure.Code.REQUEST_TIMEOUT
                            : FiscalPreparationFailure.Code.PERSISTENCE_FAILURE))
            .onFailure(error -> !(error instanceof FiscalPreparationApplicationException))
            .transform(_ -> failure(FiscalPreparationFailure.Code.PERSISTENCE_FAILURE)),
        "bounded lookup");
  }

  private static ReactiveOperationBudget requireBudget(
      Duration remaining, Duration configuredTimeout) {
    Objects.requireNonNull(remaining, "remaining");
    if (remaining.isZero() || remaining.isNegative()) {
      throw failure(FiscalPreparationFailure.Code.REQUEST_TIMEOUT);
    }
    return ReactiveOperationBudget.clamp(remaining, configuredTimeout);
  }

  private static OfficialSequenceBaseline baseline(Row row) {
    return new OfficialSequenceBaseline(
        Objects.requireNonNull(row.getUUID("id"), "baselineId"),
        new CompanyId(Objects.requireNonNull(row.getUUID("company_id"), "companyId")),
        new OfficialSequenceScope(
            Objects.requireNonNull(row.getString("issuer_reference"), "issuerReference"),
            Objects.requireNonNull(
                row.getString("establishment_reference"), "establishmentReference"),
            Objects.requireNonNull(row.getUUID("emission_point_id"), "emissionPointId"),
            Objects.requireNonNull(row.getString("establishment_code"), "establishmentCode"),
            Objects.requireNonNull(row.getString("emission_point_code"), "emissionPointCode"),
            Objects.requireNonNull(row.getString("document_type_code"), "documentTypeCode")),
        Objects.requireNonNull(row.getInteger("last_allocated"), "lastAllocated"),
        requireHydrated(
            Objects.requireNonNull(row.getOffsetDateTime("created_at"), "createdAt").toInstant(),
            "createdAt instant"),
        requireHydrated(
            Objects.requireNonNull(row.getOffsetDateTime("updated_at"), "updatedAt").toInstant(),
            "updatedAt instant"));
  }

  private static @Nullable Row first(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? rows.iterator().next() : null;
  }

  private static FiscalPreparationApplicationException failure(FiscalPreparationFailure.Code code) {
    return new FiscalPreparationApplicationException(FiscalPreparationFailure.of(code));
  }

  private static <T> @NonNull T requireHydrated(@Nullable T value, String field) {
    return Objects.requireNonNull(value, field);
  }

  private static <T extends @NonNull Object> Uni<@NonNull T> requireUni(
      @Nullable Uni<@NonNull T> value, String field) {
    return Objects.requireNonNull(value, field);
  }

  private sealed interface PreparationRead permits PreparationFound, PreparationAbsent {}

  private record PreparationFound(FiscalPreparation preparation) implements PreparationRead {
    private PreparationFound {
      Objects.requireNonNull(preparation, "preparation");
    }
  }

  private record PreparationAbsent() implements PreparationRead {}
}
