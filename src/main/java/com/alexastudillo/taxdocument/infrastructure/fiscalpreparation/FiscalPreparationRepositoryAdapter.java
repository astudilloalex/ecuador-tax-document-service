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
import org.jspecify.annotations.Nullable;

/** Company-scoped reactive store with fixed draft-first and exact-baseline-second locking. */
@ApplicationScoped
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
    this.operationTimeout =
        ConfigProvider.getConfig()
            .getValue("fiscal-preparation.persistence.operation-timeout", Duration.class);
    this.writeTimeout =
        ConfigProvider.getConfig()
            .getValue("fiscal-preparation.persistence.write-transaction-timeout", Duration.class);
    this.lockTimeout =
        ConfigProvider.getConfig()
            .getValue("fiscal-preparation.persistence.lock-timeout", Duration.class);
  }

  @Override
  public Uni<FiscalPreparationLookup> lookup(
      CompanyId companyId, UUID invoiceDraftId, Duration remaining) {
    Objects.requireNonNull(companyId, "companyId");
    Objects.requireNonNull(invoiceDraftId, "invoiceDraftId");
    return boundedLookup(
        findPreparation(companyId, invoiceDraftId)
            .onItem()
            .transformToUni(
                existing ->
                    existing instanceof PreparationFound found
                        ? Uni.createFrom()
                            .item(new FiscalPreparationLookup.Existing(found.preparation()))
                        : findDraft(companyId, invoiceDraftId)),
        remaining);
  }

  @Override
  public Uni<FiscalPreparationCommitResult> commit(
      FiscalPreparationCommitIntent intent, Duration remaining) {
    return commit(intent, remaining, new FiscalPreparationCommitTracker());
  }

  @Override
  public Uni<FiscalPreparationCommitResult> commit(
      FiscalPreparationCommitIntent intent,
      Duration remaining,
      FiscalPreparationCommitTracker commitTracker) {
    Objects.requireNonNull(intent, "intent");
    Objects.requireNonNull(commitTracker, "commitTracker");
    ReactiveOperationBudget budget = requireBudget(remaining, writeTimeout);
    AtomicBoolean localChangesCompleted = new AtomicBoolean();
    Uni<FiscalPreparationCommitResult> operation =
        pool.withTransaction(
            connection ->
                configureTransaction(connection, budget.timeout())
                    .chain(() -> commitLocked(connection, intent))
                    .invoke(
                        () -> {
                          localChangesCompleted.set(true);
                          commitTracker.possibleCommit();
                        }));
    return operation
        .invoke(ignored -> commitTracker.committed())
        .ifNoItem()
        .after(budget.timeout())
        .failWith(TimeoutException::new)
        .onFailure()
        .recoverWithUni(
            failure ->
                recoverFailure(
                    failure,
                    intent.draft().companyId(),
                    intent.draft().invoiceDraftId(),
                    remaining,
                    budget.timeoutOwner(),
                    localChangesCompleted.get(),
                    commitTracker));
  }

  private Uni<FiscalPreparationCommitResult> commitLocked(
      SqlConnection connection, FiscalPreparationCommitIntent intent) {
    InvoiceDraftPreparationView expected = intent.draft();
    return connection
        .preparedQuery(LOCK_DRAFT)
        .execute(Tuple.of(expected.companyId().value(), expected.invoiceDraftId()))
        .onItem()
        .transformToUni(rows -> afterDraftLock(connection, intent, rows));
  }

  private Uni<FiscalPreparationCommitResult> afterDraftLock(
      SqlConnection connection, FiscalPreparationCommitIntent intent, RowSet<Row> rows) {
    Row row = first(rows);
    if (row == null) {
      return Uni.createFrom()
          .failure(failure(FiscalPreparationFailure.Code.INVOICE_DRAFT_NOT_FOUND));
    }
    if (!"DRAFT".equals(row.getString("status"))) {
      return Uni.createFrom()
          .failure(failure(FiscalPreparationFailure.Code.INVOICE_DRAFT_NOT_PREPARABLE));
    }
    if (!intent.draft().emissionPointId().equals(row.getUUID("emission_point_id"))
        || !intent.draft().emissionDate().equals(row.getLocalDate("emission_date"))) {
      return Uni.createFrom()
          .failure(failure(FiscalPreparationFailure.Code.INVOICE_DRAFT_NOT_PREPARABLE));
    }
    return findPreparation(connection, intent.draft().companyId(), intent.draft().invoiceDraftId())
        .onItem()
        .transformToUni(
            existing ->
                existing instanceof PreparationFound found
                    ? Uni.createFrom()
                        .item(new FiscalPreparationCommitResult.Replay(found.preparation()))
                    : lockBaseline(connection, intent));
  }

  private Uni<FiscalPreparationCommitResult> lockBaseline(
      SqlConnection connection, FiscalPreparationCommitIntent intent) {
    FiscalContextSnapshot snapshot = intent.snapshot();
    return connection
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
        .transformToUni(rows -> allocate(connection, intent, rows));
  }

  private Uni<FiscalPreparationCommitResult> allocate(
      SqlConnection connection, FiscalPreparationCommitIntent intent, RowSet<Row> rows) {
    Row row = first(rows);
    if (row == null) {
      return Uni.createFrom()
          .failure(failure(FiscalPreparationFailure.Code.OFFICIAL_SEQUENCE_BASELINE_MISSING));
    }
    OfficialSequenceBaseline baseline;
    try {
      baseline = baseline(row);
    } catch (IllegalArgumentException | NullPointerException exception) {
      return Uni.createFrom()
          .failure(failure(FiscalPreparationFailure.Code.OFFICIAL_SEQUENCE_BASELINE_INVALID));
    }
    OfficialSequenceBaseline.AllocationDecision decision = baseline.allocationDecision();
    if (decision instanceof OfficialSequenceBaseline.AllocationDecision.Exhausted) {
      return Uni.createFrom()
          .failure(failure(FiscalPreparationFailure.Code.OFFICIAL_SEQUENCE_EXHAUSTED));
    }
    OfficialSequentialNumber sequential =
        ((OfficialSequenceBaseline.AllocationDecision.Next) decision).sequentialNumber();
    return persistAllocation(connection, intent, baseline, sequential);
  }

  private Uni<FiscalPreparationCommitResult> persistAllocation(
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
      return Uni.createFrom().failure(failure(FiscalPreparationFailure.Code.ACCESS_KEY_INVALID));
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
    OffsetDateTime updatedAt = OffsetDateTime.ofInstant(preparation.createdAt(), ZoneOffset.UTC);
    return connection
        .preparedQuery(INSERT_PREPARATION)
        .execute(mapper.toInsertParameters(preparation))
        .onItem()
        .transformToUni(
            inserted ->
                afterPreparationInsert(connection, baseline, sequential, updatedAt, inserted));
  }

  private Uni<FiscalPreparationCommitResult> afterPreparationInsert(
      SqlConnection connection,
      OfficialSequenceBaseline baseline,
      OfficialSequentialNumber sequential,
      OffsetDateTime updatedAt,
      RowSet<Row> inserted) {
    Row insertedRow = first(inserted);
    if (inserted.rowCount() != 1 || insertedRow == null) {
      return Uni.createFrom().failure(new IllegalStateException("insert did not win"));
    }
    FiscalPreparation persisted = mapper.fromRow(insertedRow);
    return connection
        .preparedQuery(ADVANCE_BASELINE)
        .execute(
            Tuple.of(persisted.companyId().value(), baseline.id(), sequential.number(), updatedAt))
        .onItem()
        .transformToUni(
            updated ->
                updated.rowCount() == 1
                    ? Uni.createFrom().item(new FiscalPreparationCommitResult.Created(persisted))
                    : Uni.createFrom()
                        .failure(new IllegalStateException("baseline did not advance")));
  }

  private Uni<Void> configureTransaction(SqlConnection connection, Duration statementTimeout) {
    long statementMillis = Math.max(1L, statementTimeout.toMillis());
    long lockMillis = Math.max(1L, Math.min(lockTimeout.toMillis(), statementMillis));
    return connection
        .preparedQuery(
            "SELECT set_config('statement_timeout', $1, true), "
                + "set_config('lock_timeout', $2, true)")
        .execute(Tuple.of(statementMillis + "ms", lockMillis + "ms"))
        .replaceWithVoid();
  }

  private Uni<FiscalPreparationCommitResult> recoverFailure(
      Throwable failure,
      CompanyId companyId,
      UUID invoiceDraftId,
      Duration remaining,
      ReactiveOperationBudget.TimeoutOwner timeoutOwner,
      boolean localChangesCompleted,
      FiscalPreparationCommitTracker commitTracker) {
    if (failure instanceof FiscalPreparationApplicationException applicationFailure) {
      commitTracker.confirmedRollback();
      return Uni.createFrom()
          .failure(
              new FiscalPreparationApplicationException(
                  applicationFailure
                      .failure()
                      .withCommitKnowledge(
                          FiscalPreparationFailure.CommitKnowledge.CONFIRMED_ROLLBACK)));
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
      return reconciler
          .reconcile(companyId, invoiceDraftId, remaining)
          .onItem()
          .transformToUni(result -> reconciledResult(result, commitTracker));
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
    return Uni.createFrom()
        .failure(
            new FiscalPreparationApplicationException(
                FiscalPreparationFailure.of(code)
                    .withCommitKnowledge(
                        FiscalPreparationFailure.CommitKnowledge.CONFIRMED_ROLLBACK),
                failure));
  }

  private static Uni<FiscalPreparationCommitResult> reconciledResult(
      FiscalPreparationCommitReconciler.Result result,
      FiscalPreparationCommitTracker commitTracker) {
    if (result instanceof FiscalPreparationCommitReconciler.Result.Winner winner) {
      commitTracker.committed();
      return Uni.createFrom().item(new FiscalPreparationCommitResult.Replay(winner.preparation()));
    }
    commitTracker.possibleCommit();
    return Uni.createFrom()
        .failure(failure(FiscalPreparationFailure.Code.PREPARATION_OUTCOME_UNKNOWN));
  }

  private Uni<PreparationRead> findPreparation(CompanyId companyId, UUID invoiceDraftId) {
    return pool.preparedQuery(SELECT_PREPARATION)
        .execute(Tuple.of(companyId.value(), invoiceDraftId))
        .onItem()
        .transform(this::preparationRead);
  }

  private Uni<PreparationRead> findPreparation(
      SqlConnection connection, CompanyId companyId, UUID invoiceDraftId) {
    return connection
        .preparedQuery(SELECT_PREPARATION)
        .execute(Tuple.of(companyId.value(), invoiceDraftId))
        .onItem()
        .transform(this::preparationRead);
  }

  private PreparationRead preparationRead(RowSet<Row> rows) {
    Row row = first(rows);
    return row == null ? new PreparationAbsent() : new PreparationFound(mapper.fromRow(row));
  }

  private Uni<FiscalPreparationLookup> findDraft(CompanyId companyId, UUID invoiceDraftId) {
    return pool.preparedQuery(SELECT_DRAFT)
        .execute(Tuple.of(companyId.value(), invoiceDraftId))
        .onItem()
        .transform(
            rows -> {
              Row row = first(rows);
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
                      Objects.requireNonNull(row.getUUID("emission_point_id"), "emissionPointId"),
                      Objects.requireNonNull(row.getLocalDate("emission_date"), "emissionDate"),
                      "DRAFT"));
            });
  }

  private Uni<FiscalPreparationLookup> boundedLookup(
      Uni<FiscalPreparationLookup> operation, Duration remaining) {
    ReactiveOperationBudget budget = requireBudget(remaining, operationTimeout);
    return operation
        .ifNoItem()
        .after(budget.timeout())
        .failWith(
            () ->
                failure(
                    budget.timeoutOwner() == ReactiveOperationBudget.TimeoutOwner.REQUEST_DEADLINE
                        ? FiscalPreparationFailure.Code.REQUEST_TIMEOUT
                        : FiscalPreparationFailure.Code.PERSISTENCE_FAILURE))
        .onFailure(error -> !(error instanceof FiscalPreparationApplicationException))
        .transform(error -> failure(FiscalPreparationFailure.Code.PERSISTENCE_FAILURE));
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
        Objects.requireNonNull(row.getOffsetDateTime("created_at"), "createdAt").toInstant(),
        Objects.requireNonNull(row.getOffsetDateTime("updated_at"), "updatedAt").toInstant());
  }

  private static @Nullable Row first(RowSet<Row> rows) {
    return rows.iterator().hasNext() ? rows.iterator().next() : null;
  }

  private static FiscalPreparationApplicationException failure(FiscalPreparationFailure.Code code) {
    return new FiscalPreparationApplicationException(FiscalPreparationFailure.of(code));
  }

  private sealed interface PreparationRead permits PreparationFound, PreparationAbsent {}

  private record PreparationFound(FiscalPreparation preparation) implements PreparationRead {
    private PreparationFound {
      Objects.requireNonNull(preparation, "preparation");
    }
  }

  private record PreparationAbsent() implements PreparationRead {}
}
