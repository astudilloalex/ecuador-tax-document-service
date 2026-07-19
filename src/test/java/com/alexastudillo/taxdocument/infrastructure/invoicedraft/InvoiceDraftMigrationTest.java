package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.sqlclient.Row;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class InvoiceDraftMigrationTest {
  private static final String ASCII_FIXTURE = "invoicedraft/ascii-validation-vectors.json";
  private static final String BUYER_PATTERN = "^[A-Za-z0-9]{1,20}$";
  private static final String PRODUCT_PATTERN = "^[A-Za-z0-9]{1,25}$";
  private static final Set<String> FAILURE_STAGES =
      Set.of("NONE", "TRANSPORT_REPRESENTATION", "APPLICATION_STAGE_6", "PERSISTENCE_DEFENSE");
  private static final Set<String> EXPECTED_TABLES =
      Set.of(
          "buyer_identification_type_catalog",
          "iva_tax_rule_catalog",
          "payment_method_catalog",
          "invoice_draft",
          "invoice_line",
          "invoice_line_tax",
          "invoice_tax_total",
          "invoice_payment",
          "invoice_additional_information",
          "invoice_draft_idempotency",
          "official_sequence_baseline",
          "fiscal_preparation",
          "flyway_schema_history");

  @Inject PostgreSqlTestResource database;
  @Inject ObjectMapper objectMapper;

  @BeforeEach
  void restoreEmptyDatabase() {
    assertEquals(6, database.resetSchema().migrationsExecuted);
  }

  @Test
  void flywayCreatesTheApprovedSchemaFromEmptyPostgreSql() {
    assertTrue(database.scalarString("SHOW server_version").startsWith("18.4"));
    assertEquals(6, database.appliedMigrations().size());

    Set<String> tables =
        database
            .rows(
                "SELECT table_name FROM information_schema.tables "
                    + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")
            .stream()
            .map(row -> row.getString("table_name"))
            .collect(Collectors.toUnmodifiableSet());
    assertEquals(EXPECTED_TABLES, tables);

    assertEquals(5, database.rowCount("buyer_identification_type_catalog"));
    assertEquals(6, database.rowCount("iva_tax_rule_catalog"));
    assertEquals(8, database.rowCount("payment_method_catalog"));
    assertEquals(0, database.rowCount("invoice_draft"));
    assertEquals(0, database.rowCount("invoice_draft_idempotency"));
  }

  @Test
  void asciiFixtureIsAuthoritativeAndSelectsTheCorrectPipelineValue() throws IOException {
    JsonNode fixture = loadAsciiFixture(objectMapper);
    assertEquals(1, fixture.required("schemaVersion").intValue());
    assertEquals(
        BUYER_PATTERN,
        fixture.required("approvedPatterns").required("buyerIdentification").textValue());
    assertEquals(
        PRODUCT_PATTERN, fixture.required("approvedPatterns").required("productCode").textValue());

    Set<String> identifiers = new java.util.HashSet<>();
    for (JsonNode vector : fixture.required("requestPipelineVectors")) {
      String id = requiredText(vector, "id");
      assertTrue(identifiers.add(id), () -> "Duplicate vector id: " + id);
      String field = requiredText(vector, "field");
      String rawValue = requiredText(vector, "rawValue");
      String normalizedValue = requiredText(vector, "applicationNormalizedValue");
      String outcome = requiredText(vector, "expectedApplicationOutcome");
      String failureStage = requiredText(vector, "failureStage");
      assertTrue(FAILURE_STAGES.contains(failureStage));
      assertEquals(trimAsciiSpAndHtab(rawValue), normalizedValue, id);
      assertTrue(vector.required("rationale").textValue().length() > 0, id);
      assertTrue(vector.required("consumers").isArray(), id);

      Pattern pattern = Pattern.compile(patternFor(field));
      boolean matches = pattern.matcher(normalizedValue).matches();
      assertEquals("ACCEPTED".equals(outcome), matches, id);
      if (matches) {
        assertEquals(normalizedValue, vector.required("expectedStoredValue").textValue(), id);
        assertTrue(vector.required("expectedErrorCode").isNull(), id);
        assertEquals("NONE", failureStage, id);
      } else {
        assertTrue(vector.required("expectedStoredValue").isNull(), id);
        assertEquals("BUSINESS_VALIDATION_FAILED", requiredText(vector, "expectedErrorCode"), id);
        assertEquals("APPLICATION_STAGE_6", failureStage, id);
      }
      if (field.equals("buyer.identification")) {
        assertTrue(Set.of("06", "08").contains(requiredText(vector, "identificationType")), id);
      }
    }

    for (JsonNode vector : fixture.required("persistenceDefenseVectors")) {
      String id = requiredText(vector, "id");
      assertTrue(identifiers.add(id), () -> "Duplicate vector id: " + id);
      assertEquals("PERSISTENCE_DEFENSE", requiredText(vector, "failureStage"), id);
      assertEquals("REJECTED", requiredText(vector, "expectedPersistenceOutcome"), id);
      assertEquals("CHECK_CONSTRAINT_VIOLATION", requiredText(vector, "expectedErrorCode"), id);
      assertNotNull(vector.required("storedProbeValue").textValue(), id);
    }
  }

  @Test
  void postgreSqlUsesStoredAndDefenseValuesFromTheAuthoritativeFixture() throws IOException {
    JsonNode fixture = loadAsciiFixture(objectMapper);
    int position = 1;
    UUID productDraftId =
        UUID.nameUUIDFromBytes("ascii-product-root".getBytes(StandardCharsets.UTF_8));
    insertDraft(productDraftId, "04", "1790012345001");

    for (JsonNode vector : fixture.required("requestPipelineVectors")) {
      if (!"ACCEPTED".equals(requiredText(vector, "expectedApplicationOutcome"))) {
        continue;
      }
      String id = requiredText(vector, "id");
      String storedValue = requiredText(vector, "expectedStoredValue");
      if ("buyer.identification".equals(requiredText(vector, "field"))) {
        UUID draftId = UUID.nameUUIDFromBytes(("buyer:" + id).getBytes(StandardCharsets.UTF_8));
        assertDoesNotThrow(
            () -> insertDraft(draftId, requiredText(vector, "identificationType"), storedValue),
            id);
      } else {
        int linePosition = position++;
        assertDoesNotThrow(() -> insertLine(productDraftId, linePosition, storedValue), id);
      }
    }

    for (JsonNode vector : fixture.required("persistenceDefenseVectors")) {
      String id = requiredText(vector, "id");
      String probe = requiredText(vector, "storedProbeValue");
      if ("buyer.identification".equals(requiredText(vector, "field"))) {
        UUID draftId =
            UUID.nameUUIDFromBytes(("buyer-defense:" + id).getBytes(StandardCharsets.UTF_8));
        assertThrows(
            RuntimeException.class,
            () -> insertDraft(draftId, requiredText(vector, "identificationType"), probe),
            id);
      } else {
        int linePosition = position++;
        assertThrows(
            RuntimeException.class, () -> insertLine(productDraftId, linePosition, probe), id);
      }
    }
  }

  @Test
  void flywayUpgradesV3ThroughV6AndPreservesTheFinalAsciiConstraints() {
    assertEquals(3, database.resetSchemaTo("3").migrationsExecuted);
    assertEquals(3, database.appliedMigrations().size());
    assertTrue(constraintDefinition("ck_invoice_line_product_code").contains("[[:alnum:]]"));

    assertEquals(3, database.migrate().migrationsExecuted);
    assertEquals(6, database.appliedMigrations().size());
    assertTrue(database.validate().validationSuccessful);

    String buyer = constraintDefinition("ck_invoice_draft_buyer_identification");
    String product = constraintDefinition("ck_invoice_line_product_code");
    assertTrue(buyer.contains("^[0-9]{13}$"));
    assertTrue(buyer.contains("^[0-9]{10}$"));
    assertTrue(buyer.contains("^[A-Za-z0-9]{1,20}$"));
    assertTrue(buyer.contains("9999999999999"));
    assertTrue(product.contains("^[A-Za-z0-9]{1,25}$"));

    String finalDefinitions = buyer + "\n" + product;
    assertFalse(finalDefinitions.contains("[[:alnum:]]"));
    assertFalse(finalDefinitions.contains("[[:alpha:]]"));
    assertFalse(finalDefinitions.contains("[[:digit:]]"));
    assertFalse(finalDefinitions.contains("\\w"));
    assertFalse(finalDefinitions.contains("\\p{"));
  }

  @Test
  void schemaEnforcesOwnershipAtomicityAndNumericBoundaries() {
    Set<String> constraintNames =
        database
            .rows("SELECT conname FROM pg_constraint WHERE connamespace = 'public'::regnamespace")
            .stream()
            .map(row -> row.getString("conname"))
            .collect(Collectors.toUnmodifiableSet());

    Set<String> required =
        Set.of(
            "uq_invoice_draft_company_id",
            "fk_invoice_draft_buyer_catalog",
            "uq_invoice_line_position",
            "uq_invoice_line_tax_line",
            "uq_invoice_tax_total_group",
            "uq_invoice_payment_method",
            "uq_invoice_additional_information_name",
            "uq_invoice_additional_information_position",
            "pk_invoice_draft_idempotency",
            "uq_invoice_draft_idempotency_draft",
            "fk_invoice_draft_idempotency_company_draft",
            "ck_invoice_draft_idempotency_key_hash",
            "ck_invoice_draft_idempotency_fingerprint");
    assertTrue(constraintNames.containsAll(required), () -> "Missing constraints: " + required);

    assertNumericColumn("invoice_line", "quantity", 12, 6);
    assertNumericColumn("invoice_line", "unit_price", 12, 6);
    assertNumericColumn("invoice_line", "line_total", 17, 2);
    assertNumericColumn("invoice_line_tax", "rate", 5, 2);
    assertNumericColumn("invoice_payment", "amount", 17, 2);
    assertNumericColumn("invoice_draft", "grand_total", 17, 2);

    assertEquals(
        0,
        database.scalarLong(
            "SELECT count(*) FROM pg_constraint "
                + "WHERE contype = 'f' AND confdeltype = 'c' "
                + "AND connamespace = 'public'::regnamespace"));
  }

  @Test
  void schemaContainsNoCompanyIdentityFiscalSnapshotOrSharedPersistenceStructures() {
    assertEquals(
        0,
        database.scalarLong(
            "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' "
                + "AND table_name ~ '(company|issuer|tenant|identity|security|certificate|sri|snapshot|cache)'"));

    assertEquals(
        0,
        database.scalarLong(
            "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'public' "
                + "AND column_name IN ('tenant_id','created_by_subject','company_context_version',"
                + "'company_context_observed_at','issuer_snapshot','establishment_snapshot',"
                + "'emission_point_snapshot','raw_idempotency_key','normalized_request','expires_at')"));

    Set<String> childTables =
        Set.of(
            "invoice_line",
            "invoice_line_tax",
            "invoice_tax_total",
            "invoice_payment",
            "invoice_additional_information");
    long childCompanyColumns =
        database
            .rows(
                "SELECT table_name FROM information_schema.columns WHERE table_schema = 'public' "
                    + "AND column_name = 'company_id'")
            .stream()
            .map(row -> row.getString("table_name"))
            .filter(childTables::contains)
            .count();
    assertEquals(0, childCompanyColumns);

    assertEquals(
        "validate",
        ConfigProvider.getConfig()
            .getValue("quarkus.hibernate-orm.schema-management.strategy", String.class));
  }

  @Test
  void migrationsAreImmutableAndRepeatable() {
    Map<String, Integer> before = migrationChecksums();
    Map<String, Long> rowCounts = approvedRowCounts();

    assertEquals(0, database.migrate().migrationsExecuted);

    assertEquals(before, migrationChecksums());
    assertEquals(rowCounts, approvedRowCounts());
    assertFalse(before.containsValue(null));
  }

  @Test
  void productionConfigurationKeepsSchemaOwnershipAndTimeoutsExplicit() throws IOException {
    Properties configuration = new Properties();
    try (var reader =
        Files.newBufferedReader(
            Path.of("src/main/resources/application.properties"), StandardCharsets.UTF_8)) {
      configuration.load(reader);
    }

    assertConfiguration(configuration, "quarkus.datasource.db-kind", "postgresql");
    assertConfiguration(configuration, "quarkus.datasource.jdbc", "true");
    assertConfiguration(configuration, "quarkus.datasource.reactive", "true");
    assertConfiguration(configuration, "quarkus.datasource.health.enabled", "true");
    assertConfiguration(configuration, "quarkus.flyway.migrate-at-start", "true");
    assertConfiguration(configuration, "quarkus.flyway.validate-at-start", "true");
    assertConfiguration(configuration, "quarkus.flyway.clean-at-start", "false");
    assertConfiguration(configuration, "quarkus.flyway.clean-disabled", "true");
    assertConfiguration(
        configuration, "quarkus.hibernate-orm.schema-management.strategy", "validate");
    assertConfiguration(configuration, "quarkus.datasource.jdbc.login-timeout", "PT5S");
    assertConfiguration(configuration, "quarkus.datasource.jdbc.acquisition-timeout", "PT5S");
    assertConfiguration(configuration, "quarkus.datasource.reactive.reconnect-attempts", "0");
    assertConfiguration(configuration, "invoice-draft.persistence.operation-timeout", "PT5S");
    assertConfiguration(
        configuration, "invoice-draft.persistence.write-transaction-timeout", "PT5S");
    assertConfiguration(configuration, "invoice-draft.request-deadline", "PT10S");
    assertConfiguration(configuration, "quarkus.http.limits.max-body-size", "2097152");
    assertConfiguration(configuration, "quarkus.jackson.fail-on-unknown-properties", "true");
    assertConfiguration(
        configuration,
        "quarkus.rest.exception-mapping.disable-mapper-for",
        "io.quarkus.resteasy.reactive.jackson.runtime.mappers.BuiltinMismatchedInputExceptionMapper");
    assertConfiguration(configuration, "mp.openapi.scan.disable", "true");
  }

  private void assertNumericColumn(String table, String column, int precision, int scale) {
    Row definition =
        database
            .rows(
                "SELECT numeric_precision, numeric_scale FROM information_schema.columns "
                    + "WHERE table_schema = 'public' AND table_name = '"
                    + table
                    + "' AND column_name = '"
                    + column
                    + "'")
            .getFirst();
    assertEquals(precision, definition.getInteger("numeric_precision"));
    assertEquals(scale, definition.getInteger("numeric_scale"));
  }

  private String constraintDefinition(String constraintName) {
    return database.scalarString(
        "SELECT pg_get_constraintdef(oid) FROM pg_constraint "
            + "WHERE connamespace = 'public'::regnamespace AND conname = '"
            + constraintName
            + "'");
  }

  static JsonNode loadAsciiFixture(ObjectMapper mapper) throws IOException {
    try (InputStream input =
        InvoiceDraftMigrationTest.class.getClassLoader().getResourceAsStream(ASCII_FIXTURE)) {
      assertNotNull(input, ASCII_FIXTURE);
      return mapper.readTree(input);
    }
  }

  private void insertDraft(UUID id, String identificationType, String identification) {
    database.scalarLong(
        "INSERT INTO invoice_draft (id, company_id, emission_point_id, emission_date, "
            + "buyer_identification_type_code, buyer_identification_catalog_version, "
            + "buyer_identification, buyer_legal_name, status, currency, subtotal_before_taxes, "
            + "total_discount, grand_total, created_at, updated_at) VALUES ('"
            + id
            + "', '11111111-1111-1111-1111-111111111111', "
            + "'22222222-2222-2222-2222-222222222222', DATE '2026-07-17', '"
            + sqlLiteral(identificationType)
            + "', 'SRI-OFFLINE-2.32-TARGET-1', '"
            + sqlLiteral(identification)
            + "', 'Fixture Buyer', 'DRAFT', 'USD', 0.00, 0.00, 0.00, "
            + "TIMESTAMPTZ '2026-07-17 12:00:00Z', TIMESTAMPTZ '2026-07-17 12:00:00Z') "
            + "RETURNING 1");
  }

  private void insertLine(UUID draftId, int position, String productCode) {
    UUID lineId =
        UUID.nameUUIDFromBytes(
            (draftId + ":" + position + ":" + productCode).getBytes(StandardCharsets.UTF_8));
    database.scalarLong(
        "INSERT INTO invoice_line (id, invoice_draft_id, position, product_code, description, "
            + "quantity, unit_price, discount, gross_amount, net_amount, line_total) VALUES ('"
            + lineId
            + "', '"
            + draftId
            + "', "
            + position
            + ", '"
            + sqlLiteral(productCode)
            + "', 'Fixture product', 1.000000, 1.000000, 0.00, 1.00, 1.00, 1.00) "
            + "RETURNING 1");
  }

  private static String patternFor(String field) {
    return switch (field) {
      case "buyer.identification" -> BUYER_PATTERN;
      case "lines[].productCode" -> PRODUCT_PATTERN;
      default -> throw new IllegalArgumentException("Unsupported fixture field: " + field);
    };
  }

  private static String requiredText(JsonNode node, String field) {
    JsonNode value = node.required(field);
    if (!value.isTextual()) {
      throw new IllegalArgumentException(field + " must be textual");
    }
    return value.textValue();
  }

  private static String trimAsciiSpAndHtab(String value) {
    int start = 0;
    int end = value.length();
    while (start < end && isAsciiTrim(value.charAt(start))) {
      start++;
    }
    while (end > start && isAsciiTrim(value.charAt(end - 1))) {
      end--;
    }
    return value.substring(start, end);
  }

  private static boolean isAsciiTrim(char value) {
    return value == ' ' || value == '\t';
  }

  private static String sqlLiteral(String value) {
    return value.replace("'", "''");
  }

  private Map<String, Integer> migrationChecksums() {
    return database.appliedMigrations().stream()
        .collect(
            Collectors.toMap(
                info -> info.getVersion().getVersion(),
                info -> info.getChecksum(),
                (left, right) -> right,
                LinkedHashMap::new));
  }

  private Map<String, Long> approvedRowCounts() {
    Map<String, Long> counts = new LinkedHashMap<>();
    counts.put("buyer", database.rowCount("buyer_identification_type_catalog"));
    counts.put("iva", database.rowCount("iva_tax_rule_catalog"));
    counts.put("payment", database.rowCount("payment_method_catalog"));
    return Map.copyOf(counts);
  }

  private static void assertConfiguration(
      Properties configuration, String propertyName, String expectedValue) {
    assertEquals(expectedValue, configuration.getProperty(propertyName), propertyName);
  }
}
