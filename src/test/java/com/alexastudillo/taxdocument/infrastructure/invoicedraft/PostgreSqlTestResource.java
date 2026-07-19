package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import static java.util.Objects.requireNonNull;

import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.ValidateResult;
import org.jspecify.annotations.NullMarked;

/** Shared access to the Quarkus-managed PostgreSQL Dev Service used by persistence tests. */
@ApplicationScoped
@NullMarked
public class PostgreSqlTestResource {
  private static final Duration DATABASE_WAIT = requireNonNull(Duration.ofSeconds(10));
  private static final Pattern SQL_IDENTIFIER = requireNonNull(Pattern.compile("[a-z][a-z0-9_]*"));

  private final Flyway flyway;
  private final Pool pool;

  @Inject
  public PostgreSqlTestResource(Flyway flyway, Pool pool) {
    this.flyway = flyway;
    this.pool = pool;
  }

  /** Restores the schema exclusively through the configured Flyway lifecycle. */
  public synchronized MigrateResult resetSchema() {
    flyway.clean();
    return requireNonNull(flyway.migrate());
  }

  public synchronized MigrateResult migrate() {
    return requireNonNull(flyway.migrate());
  }

  public synchronized MigrateResult resetSchemaTo(String targetVersion) {
    flyway.clean();
    Flyway target =
        Flyway.configure().configuration(flyway.getConfiguration()).target(targetVersion).load();
    return requireNonNull(target.migrate());
  }

  public ValidateResult validate() {
    return requireNonNull(flyway.validateWithResult());
  }

  public List<MigrationInfo> appliedMigrations() {
    return requireNonNull(List.of(flyway.info().applied()));
  }

  public long rowCount(String tableName) {
    String identifier = requireIdentifier(tableName);
    return scalarLong("SELECT count(*) FROM public." + identifier);
  }

  public long scalarLong(String sql) {
    Row row = firstRow(sql);
    return row.getLong(0);
  }

  public String scalarString(String sql) {
    Row row = firstRow(sql);
    return requireNonNull(row.getString(0));
  }

  public List<Row> rows(String sql) {
    RowSet<Row> rowSet = pool.query(sql).execute().await().atMost(DATABASE_WAIT);
    List<Row> result = new ArrayList<>(rowSet.size());
    rowSet.forEach(result::add);
    return requireNonNull(List.copyOf(result));
  }

  private Row firstRow(String sql) {
    List<Row> result = rows(sql);
    if (result.size() != 1) {
      throw new IllegalStateException("Expected exactly one row but received " + result.size());
    }
    return requireNonNull(result.getFirst());
  }

  private static String requireIdentifier(String value) {
    String normalized = value.toLowerCase(Locale.ROOT);
    if (!SQL_IDENTIFIER.matcher(normalized).matches()) {
      throw new IllegalArgumentException("Invalid SQL identifier");
    }
    return requireNonNull(normalized);
  }
}
