package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationStore;
import com.alexastudillo.taxdocument.infrastructure.invoicedraft.PostgreSqlTestResource;
import com.alexastudillo.taxdocument.support.FixedRequestClock;
import com.alexastudillo.taxdocument.support.fiscalpreparation.FiscalPreparationPostgreSqlSupport;
import com.alexastudillo.taxdocument.support.fiscalpreparation.FiscalPreparationTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.sqlclient.Pool;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FiscalPreparationMigrationTest {
  @Inject PostgreSqlTestResource database;
  @Inject FiscalPreparationPostgreSqlSupport fiscalDatabase;
  @Inject FiscalPreparationStore store;
  @Inject Pool pool;
  @Inject FixedRequestClock clock;

  @Test
  void emptyDatabaseAndV5UpgradeCreateExactlyTwoUnseededFeatureTwoTables() {
    database.resetSchemaTo("5");
    Map<String, Integer> before = checksums(database.appliedMigrations());
    database.migrate();
    Map<String, Integer> after = checksums(database.appliedMigrations());
    before.forEach((version, checksum) -> assertEquals(checksum, after.get(version)));

    assertEquals(0L, database.rowCount("official_sequence_baseline"));
    assertEquals(0L, database.rowCount("fiscal_preparation"));
    assertEquals(
        List.of("fiscal_preparation", "official_sequence_baseline"),
        database
            .rows(
                "SELECT table_name FROM information_schema.tables "
                    + "WHERE table_schema='public' AND table_name IN "
                    + "('fiscal_preparation','official_sequence_baseline') ORDER BY table_name")
            .stream()
            .map(row -> row.getString("table_name"))
            .toList());
  }

  @Test
  void migrationHasNoSequenceIdentityJsonbProvisionalMasterOrBaselineSeed() throws Exception {
    String sql =
        Files.readString(
                Path.of("src/main/resources/db/migration/V6__create_fiscal_preparation.sql"))
            .toLowerCase(java.util.Locale.ROOT);
    assertFalse(sql.contains("create sequence"));
    assertFalse(sql.contains("generated "));
    assertFalse(sql.contains("serial"));
    assertFalse(sql.contains("jsonb"));
    assertFalse(sql.contains("insert into official_sequence_baseline"));
    assertFalse(sql.contains("preparing"));
    List<String> createdTables =
        java.util.regex.Pattern.compile("create table\\s+([a-z_]+)")
            .matcher(sql)
            .results()
            .map(result -> result.group(1))
            .toList();
    assertEquals(List.of("official_sequence_baseline", "fiscal_preparation"), createdTables);
    assertEquals(2, sql.split("create table", -1).length - 1);
  }

  @Test
  void databasePublishesNamedCompanyScopeUniquenessCheckAndImmutabilityInvariants() {
    database.resetSchema();
    List<String> constraints =
        database
            .rows(
                "SELECT conname FROM pg_constraint WHERE conrelid IN "
                    + "('official_sequence_baseline'::regclass,'fiscal_preparation'::regclass)")
            .stream()
            .map(row -> row.getString("conname"))
            .toList();
    for (String required :
        List.of(
            "uq_official_sequence_baseline_scope",
            "uq_fiscal_preparation_company_draft",
            "uq_fiscal_preparation_scoped_sequential",
            "uq_fiscal_preparation_access_key",
            "fk_fiscal_preparation_company_draft",
            "fk_fiscal_preparation_company_baseline",
            "ck_fiscal_preparation_designation_pairs",
            "ck_fiscal_preparation_access_key")) {
      assertTrue(constraints.contains(required), required);
    }
    List<String> triggers =
        database
            .rows(
                "SELECT tgname FROM pg_trigger WHERE NOT tgisinternal AND tgrelid IN "
                    + "('official_sequence_baseline'::regclass,'fiscal_preparation'::regclass)")
            .stream()
            .map(row -> row.getString("tgname"))
            .toList();
    assertTrue(triggers.contains("trg_fiscal_preparation_append_only"));
    assertTrue(triggers.contains("trg_official_sequence_baseline_guard"));
  }

  @Test
  void databaseRejectsDirectPreparationAndUnpairedBaselineMutation() {
    database.resetSchema();
    clock.reset(FiscalPreparationTestFixtures.CREATED_AT, FiscalPreparationTestFixtures.CREATED_AT);
    fiscalDatabase.insertControlledDraft(
        FiscalPreparationTestFixtures.COMPANY_UUID,
        FiscalPreparationTestFixtures.DRAFT,
        FiscalPreparationTestFixtures.EMISSION_POINT,
        FiscalPreparationTestFixtures.DATE,
        FiscalPreparationTestFixtures.CREATED_AT.minusSeconds(60));
    fiscalDatabase.insertControlledBaseline(
        FiscalPreparationTestFixtures.BASELINE,
        FiscalPreparationTestFixtures.COMPANY_UUID,
        "issuer-1",
        "establishment-1",
        FiscalPreparationTestFixtures.EMISSION_POINT,
        "001",
        "001",
        0,
        FiscalPreparationTestFixtures.CREATED_AT.minusSeconds(60));
    store
        .commit(FiscalPreparationTestFixtures.intent(), Duration.ofSeconds(5))
        .await()
        .indefinitely();

    assertThrows(
        RuntimeException.class,
        () ->
            pool.query("UPDATE fiscal_preparation SET legal_name = 'Changed'")
                .execute()
                .await()
                .indefinitely());
    assertThrows(
        RuntimeException.class,
        () -> pool.query("DELETE FROM fiscal_preparation").execute().await().indefinitely());
    assertThrows(
        RuntimeException.class,
        () ->
            pool.query(
                    "UPDATE official_sequence_baseline "
                        + "SET last_allocated = last_allocated + 1, updated_at = now()")
                .execute()
                .await()
                .indefinitely());
    assertThrows(
        RuntimeException.class,
        () ->
            pool.query("DELETE FROM official_sequence_baseline").execute().await().indefinitely());

    assertEquals(1L, database.rowCount("fiscal_preparation"));
    assertEquals(
        1,
        fiscalDatabase.lastAllocated(
            FiscalPreparationTestFixtures.COMPANY_UUID, FiscalPreparationTestFixtures.BASELINE));
  }

  private static Map<String, Integer> checksums(List<MigrationInfo> migrations) {
    return migrations.stream()
        .filter(info -> Integer.parseInt(info.getVersion().getVersion()) <= 5)
        .collect(
            Collectors.toUnmodifiableMap(
                info -> info.getVersion().getVersion(),
                info -> Objects.requireNonNull(info.getChecksum(), "checksum")));
  }
}
