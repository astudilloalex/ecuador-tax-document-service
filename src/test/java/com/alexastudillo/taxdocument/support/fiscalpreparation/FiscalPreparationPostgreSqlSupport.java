package com.alexastudillo.taxdocument.support.fiscalpreparation;

import com.alexastudillo.taxdocument.infrastructure.invoicedraft.PostgreSqlTestResource;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Controlled Feature 002 rows on the existing Quarkus PostgreSQL 18.4 test lifecycle. */
@ApplicationScoped
public final class FiscalPreparationPostgreSqlSupport {
  private static final Duration WAIT = Duration.ofSeconds(10);
  private static final String REFERENCE_VERSION = "SRI-OFFLINE-2.32-TARGET-1";

  private final PostgreSqlTestResource database;
  private final Pool pool;

  public FiscalPreparationPostgreSqlSupport(PostgreSqlTestResource database, Pool pool) {
    this.database = database;
    this.pool = pool;
  }

  public void resetSchema() {
    database.resetSchema();
  }

  public void insertControlledDraft(
      UUID companyId,
      UUID invoiceDraftId,
      UUID emissionPointId,
      LocalDate emissionDate,
      Instant createdAt) {
    execute(
        """
        INSERT INTO invoice_draft (
          id, company_id, emission_point_id, emission_date,
          buyer_identification_type_code, buyer_identification_catalog_version,
          buyer_identification, buyer_legal_name, status, currency,
          subtotal_before_taxes, total_discount, grand_total, created_at, updated_at
        ) VALUES ($1, $2, $3, $4, '06', $5, 'FIXTURE001', 'Fixture Buyer', 'DRAFT', 'USD',
                  10.00, 0.00, 11.50, $6, $6)
        """,
        Tuple.of(
            invoiceDraftId,
            companyId,
            emissionPointId,
            emissionDate,
            REFERENCE_VERSION,
            OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC)));
  }

  public void insertControlledBaseline(
      UUID baselineId,
      UUID companyId,
      String issuerReference,
      String establishmentReference,
      UUID emissionPointId,
      String establishmentCode,
      String emissionPointCode,
      int lastAllocated,
      Instant createdAt) {
    execute(
        """
        INSERT INTO official_sequence_baseline (
          id, company_id, issuer_reference, establishment_reference, emission_point_id,
          establishment_code, emission_point_code, document_type_code,
          last_allocated, created_at, updated_at
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, '01', $8, $9, $9)
        """,
        Tuple.from(
            List.of(
                baselineId,
                companyId,
                issuerReference,
                establishmentReference,
                emissionPointId,
                establishmentCode,
                emissionPointCode,
                lastAllocated,
                OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC))));
  }

  public long fiscalPreparationCount(UUID companyId, UUID invoiceDraftId) {
    return requireRow(
            "SELECT count(*) AS value FROM fiscal_preparation "
                + "WHERE company_id = $1 AND invoice_draft_id = $2",
            Tuple.of(companyId, invoiceDraftId))
        .getLong("value");
  }

  public int lastAllocated(UUID companyId, UUID baselineId) {
    return requireRow(
            "SELECT last_allocated FROM official_sequence_baseline "
                + "WHERE company_id = $1 AND id = $2",
            Tuple.of(companyId, baselineId))
        .getInteger("last_allocated");
  }

  public DraftSnapshot draftSnapshot(UUID companyId, UUID invoiceDraftId) {
    Row row =
        requireRow(
            """
            SELECT emission_point_id, emission_date, status, currency,
                   subtotal_before_taxes, total_discount, grand_total, created_at, updated_at
              FROM invoice_draft
             WHERE company_id = $1 AND id = $2
            """,
            Tuple.of(companyId, invoiceDraftId));
    return new DraftSnapshot(
        row.getUUID("emission_point_id"),
        row.getLocalDate("emission_date"),
        row.getString("status"),
        row.getString("currency"),
        row.getBigDecimal("subtotal_before_taxes"),
        row.getBigDecimal("total_discount"),
        row.getBigDecimal("grand_total"),
        row.getOffsetDateTime("created_at").toInstant(),
        row.getOffsetDateTime("updated_at").toInstant());
  }

  public long rowCount(String tableName) {
    return database.rowCount(tableName);
  }

  private void execute(String sql, Tuple parameters) {
    pool.preparedQuery(sql).execute(parameters).await().atMost(WAIT);
  }

  private Row requireRow(String sql, Tuple parameters) {
    RowSet<Row> rows = pool.preparedQuery(sql).execute(parameters).await().atMost(WAIT);
    if (rows.size() != 1) {
      throw new IllegalStateException("Expected exactly one controlled fixture row");
    }
    return Objects.requireNonNull(rows.iterator().next(), "row");
  }

  public record DraftSnapshot(
      UUID emissionPointId,
      LocalDate emissionDate,
      String status,
      String currency,
      BigDecimal subtotalBeforeTaxes,
      BigDecimal totalDiscount,
      BigDecimal grandTotal,
      Instant createdAt,
      Instant updatedAt) {}
}
