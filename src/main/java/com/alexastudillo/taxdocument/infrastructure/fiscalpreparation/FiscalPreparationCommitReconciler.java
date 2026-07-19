package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalPreparation;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/** Read-only Company-plus-draft reconciliation after an uncertain commit acknowledgement. */
@ApplicationScoped
@NullMarked
public final class FiscalPreparationCommitReconciler {
  private final Pool pool;
  private final FiscalPreparationPersistenceMapper mapper;

  public FiscalPreparationCommitReconciler(Pool pool, FiscalPreparationPersistenceMapper mapper) {
    this.pool = Objects.requireNonNull(pool, "pool");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  public Uni<Result> reconcile(CompanyId companyId, UUID invoiceDraftId, Duration remaining) {
    Objects.requireNonNull(companyId, "companyId");
    Objects.requireNonNull(invoiceDraftId, "invoiceDraftId");
    Objects.requireNonNull(remaining, "remaining");
    if (remaining.isZero() || remaining.isNegative()) {
      return Uni.createFrom().item(new Result.Unknown());
    }
    return pool.preparedQuery(FiscalPreparationRepositoryAdapter.SELECT_PREPARATION)
        .execute(Tuple.of(companyId.value(), invoiceDraftId))
        .ifNoItem()
        .after(remaining)
        .fail()
        .onItem()
        .transform(rows -> result(Objects.requireNonNull(rows, "reconciliation rows")))
        .onFailure()
        .recoverWithItem(new Result.Unknown());
  }

  private Result result(RowSet<Row> rows) {
    if (!rows.iterator().hasNext()) {
      return new Result.Unknown();
    }
    Row row = Objects.requireNonNull(rows.iterator().next(), "row");
    return new Result.Winner(mapper.fromRow(row));
  }

  public sealed interface Result {
    record Winner(FiscalPreparation preparation) implements Result {
      public Winner {
        Objects.requireNonNull(preparation, "preparation");
      }
    }

    record Unknown() implements Result {}
  }
}
