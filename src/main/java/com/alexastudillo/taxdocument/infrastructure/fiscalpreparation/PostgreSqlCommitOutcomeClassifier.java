package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import io.vertx.pgclient.PgException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Conservative PostgreSQL commit-knowledge classifier; unknown is never reported as rollback. */
@NullMarked
public final class PostgreSqlCommitOutcomeClassifier {
  public Knowledge classify(Throwable failure, CommitPhase phase, boolean rollbackConfirmed) {
    if (rollbackConfirmed) {
      return Knowledge.CONFIRMED_ROLLBACK;
    }
    String state = sqlState(failure);
    if (state != null && isConclusiveAbort(state)) {
      return Knowledge.CONFIRMED_ROLLBACK;
    }
    return phase == CommitPhase.BEFORE_COMMIT ? Knowledge.CONFIRMED_ROLLBACK : Knowledge.UNKNOWN;
  }

  public FiscalPreparationFailureCode failureCode(Throwable failure) {
    String constraint = constraint(failure).orElse("");
    if (constraint.startsWith("ck_official_sequence_baseline")) {
      return FiscalPreparationFailureCode.BASELINE_INVALID;
    }
    if (constraint.equals("ck_fiscal_preparation_access_key")) {
      return FiscalPreparationFailureCode.ACCESS_KEY_INVALID;
    }
    if (constraint.equals("fk_fiscal_preparation_company_draft")) {
      return FiscalPreparationFailureCode.DRAFT_NOT_PREPARABLE;
    }
    return FiscalPreparationFailureCode.PERSISTENCE_FAILURE;
  }

  private static Optional<String> constraint(Throwable failure) {
    for (Throwable current = failure; current != null; current = current.getCause()) {
      if (current instanceof PgException postgres) {
        return Objects.requireNonNull(Optional.ofNullable(postgres.getConstraint()));
      }
    }
    return Objects.requireNonNull(Optional.empty());
  }

  private static @Nullable String sqlState(Throwable failure) {
    for (Throwable current = failure; current != null; current = current.getCause()) {
      if (current instanceof PgException postgres) {
        return postgres.getSqlState();
      }
      if (current instanceof SQLException sql) {
        return sql.getSQLState();
      }
    }
    return null;
  }

  private static boolean isConclusiveAbort(String state) {
    return switch (state) {
      case "23502", "23503", "23505", "23514", "40P01", "40001" -> true;
      default -> false;
    };
  }

  public enum Knowledge {
    CONFIRMED_ROLLBACK,
    UNKNOWN
  }

  public enum CommitPhase {
    BEFORE_COMMIT,
    COMMIT_INITIATED
  }

  public enum FiscalPreparationFailureCode {
    BASELINE_INVALID,
    ACCESS_KEY_INVALID,
    DRAFT_NOT_PREPARABLE,
    PERSISTENCE_FAILURE
  }
}
