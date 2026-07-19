package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.sqlclient.Row;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ReferenceDataBaselineTest {
  private static final UUID REFERENCE_NAMESPACE =
      UUID.fromString("32576bbf-b70d-5c24-98ff-d5f9b48e8826");
  private static final String CATALOG_VERSION = "SRI-OFFLINE-2.32-TARGET-1";

  private static final List<@NonNull ExpectedTaxRule> TAX_RULES =
      List.of(
          new ExpectedTaxRule(
              "84cb3f03-574b-54de-9e73-efb8d485476a",
              "0",
              "0%",
              "IVA 0%",
              "0.00",
              "ZERO_RATE",
              null),
          new ExpectedTaxRule(
              "2b31de9b-20f2-50c7-aeff-fed9babfe112",
              "5",
              "5%",
              "IVA 5%",
              "5.00",
              "PERCENTAGE_RATE",
              LocalDate.of(2024, 4, 1)),
          new ExpectedTaxRule(
              "3aa0fb56-17ad-5310-a10c-64c1f6dbe2fb",
              "10",
              "13%",
              "IVA 13%",
              "13.00",
              "PERCENTAGE_RATE",
              null),
          new ExpectedTaxRule(
              "5b34b038-931c-50e3-a84c-10af272fdcd4",
              "4",
              "15%",
              "IVA 15%",
              "15.00",
              "PERCENTAGE_RATE",
              LocalDate.of(2025, 12, 26)),
          new ExpectedTaxRule(
              "a70a77f5-1176-5b0b-a539-74ead416a3ff",
              "6",
              "No Objeto de Impuesto",
              "Not subject to IVA",
              "0.00",
              "NOT_SUBJECT",
              null),
          new ExpectedTaxRule(
              "a7eeaf77-dbdc-5f99-9bdd-d783c072a7de",
              "7",
              "Exento de IVA",
              "Exempt from IVA",
              "0.00",
              "EXEMPT",
              null));

  private static final List<@NonNull ExpectedPaymentMethod> PAYMENT_METHODS =
      List.of(
          new ExpectedPaymentMethod(
              "639f2b7e-10a3-5d92-a1a3-28223896f5b5",
              "01",
              "SIN UTILIZACION DEL SISTEMA FINANCIERO",
              "Without use of the financial system",
              LocalDate.of(2013, 1, 1)),
          new ExpectedPaymentMethod(
              "daad9ac7-6a55-5df6-8a9e-60012c5d261b",
              "15",
              "COMPENSACIÓN DE DEUDAS",
              "Debt compensation",
              LocalDate.of(2013, 1, 1)),
          new ExpectedPaymentMethod(
              "cbf7e764-0ef5-5422-965e-fe08eaa49772",
              "16",
              "TARJETA DE DÉBITO",
              "Debit card",
              LocalDate.of(2016, 6, 1)),
          new ExpectedPaymentMethod(
              "8b626780-39fb-5c72-b1e2-8453df01b79a",
              "17",
              "DINERO ELECTRÓNICO",
              "Electronic money",
              LocalDate.of(2016, 6, 1)),
          new ExpectedPaymentMethod(
              "65eee3f8-1c46-5749-8101-6e6d50d08a69",
              "18",
              "TARJETA PREPAGO",
              "Prepaid card",
              LocalDate.of(2016, 6, 1)),
          new ExpectedPaymentMethod(
              "178f5fd1-038b-577f-bac3-21c49ce6d1f2",
              "19",
              "TARJETA DE CRÉDITO",
              "Credit card",
              LocalDate.of(2016, 6, 1)),
          new ExpectedPaymentMethod(
              "953df84c-d41c-5e72-b975-9d02c45ee656",
              "20",
              "OTROS CON UTILIZACIÓN DEL SISTEMA FINANCIERO",
              "Other with use of the financial system",
              LocalDate.of(2016, 6, 1)),
          new ExpectedPaymentMethod(
              "f2bc801e-c241-5df8-99f8-ceb9ee870d05",
              "21",
              "ENDOSO DE TÍTULOS",
              "Endorsement of securities",
              LocalDate.of(2016, 6, 1)));

  @Inject PostgreSqlTestResource database;

  @Inject ObjectMapper objectMapper;

  @BeforeEach
  void restoreApprovedBaseline() {
    assertEquals(6, database.resetSchema().migrationsExecuted);
  }

  @Test
  void baselineContainsExactlyTheApprovedRowsAndEvidenceMetadata() {
    assertEquals(
        List.of("1", "2", "3", "4", "5"),
        database.appliedMigrations().stream()
            .map(info -> info.getVersion().getVersion())
            .filter(version -> Integer.parseInt(version) <= 5)
            .toList());
    assertEquals(5, database.rowCount("buyer_identification_type_catalog"));
    assertEquals(6, database.rowCount("iva_tax_rule_catalog"));
    assertEquals(8, database.rowCount("payment_method_catalog"));

    Map<String, String> buyerMappings =
        database
            .rows(
                "SELECT official_code, official_label, display_name, validation_strategy "
                    + "FROM buyer_identification_type_catalog")
            .stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    row -> row.getString("official_code"),
                    row ->
                        String.join(
                            "|",
                            row.getString("official_label"),
                            row.getString("display_name"),
                            row.getString("validation_strategy"))));
    assertEquals(
        Map.of(
            "04",
            "RUC|RUC|FORMAT_ONLY_NUMERIC_13",
            "05",
            "CÉDULA|Ecuadorian identity card|FORMAT_ONLY_NUMERIC_10",
            "06",
            "PASAPORTE|Passport|FORMAT_ONLY_ALPHANUMERIC_1_TO_20",
            "07",
            "VENTA A CONSUMIDOR FINAL|Final consumer sale|FINAL_CONSUMER_EXACT",
            "08",
            "IDENTIFICACIÓN DEL EXTERIOR|Foreign identification|"
                + "FORMAT_ONLY_ALPHANUMERIC_1_TO_20"),
        buyerMappings);

    for (String table :
        List.of(
            "buyer_identification_type_catalog",
            "iva_tax_rule_catalog",
            "payment_method_catalog")) {
      assertEquals(
          database.rowCount(table),
          database.scalarLong(
              "SELECT count(*) FROM "
                  + table
                  + " WHERE active AND catalog_version = '"
                  + CATALOG_VERSION
                  + "' AND target_valid_from = DATE '2026-07-12' "
                  + "AND target_valid_to IS NULL AND btrim(official_source_uri) <> '' "
                  + "AND btrim(official_source_locator) <> ''"));
    }
  }

  @Test
  void everyTaxAndPaymentIdentifierMatchesThePublishedUuidV5Derivation()
      throws NoSuchAlgorithmException {
    Map<UUID, Row> taxRows =
        database
            .rows(
                "SELECT id, official_tax_code, official_percentage_code, official_label, "
                    + "display_name, rate, treatment, source_valid_from "
                    + "FROM iva_tax_rule_catalog")
            .stream()
            .collect(Collectors.toUnmodifiableMap(row -> row.getUUID("id"), Function.identity()));
    for (ExpectedTaxRule expected : TAX_RULES) {
      String name =
          "tax-rule|SRI-OFFLINE-2.32|2|"
              + expected.percentageCode()
              + "|"
              + expected.rate()
              + "|"
              + expected.treatment();
      UUID derived = uuidV5(REFERENCE_NAMESPACE, name);
      assertEquals(UUID.fromString(expected.id()), derived);
      Row stored = taxRows.get(derived);
      assertNotNull(stored);
      assertEquals("2", stored.getString("official_tax_code"));
      assertEquals(expected.percentageCode(), stored.getString("official_percentage_code"));
      assertEquals(expected.officialLabel(), stored.getString("official_label"));
      assertEquals(expected.displayName(), stored.getString("display_name"));
      assertEquals(expected.rate(), stored.getBigDecimal("rate").toPlainString());
      assertEquals(expected.treatment(), stored.getString("treatment"));
      assertEquals(expected.sourceValidFrom(), stored.getLocalDate("source_valid_from"));
    }

    Map<UUID, Row> paymentRows =
        database
            .rows(
                "SELECT id, official_code, official_label, display_name, source_valid_from "
                    + "FROM payment_method_catalog")
            .stream()
            .collect(Collectors.toUnmodifiableMap(row -> row.getUUID("id"), Function.identity()));
    for (ExpectedPaymentMethod expected : PAYMENT_METHODS) {
      String name = "payment-method|SRI-OFFLINE-2.32|" + expected.officialCode();
      UUID derived = uuidV5(REFERENCE_NAMESPACE, name);
      assertEquals(UUID.fromString(expected.id()), derived);
      Row stored = paymentRows.get(derived);
      assertNotNull(stored);
      assertEquals(expected.officialCode(), stored.getString("official_code"));
      assertEquals(expected.officialLabel(), stored.getString("official_label"));
      assertEquals(expected.displayName(), stored.getString("display_name"));
      assertEquals(expected.sourceValidFrom(), stored.getLocalDate("source_valid_from"));
    }
  }

  @Test
  void activeTargetValidityIntervalsDoNotOverlap() {
    assertEquals(0, overlapCount("buyer_identification_type_catalog", "official_code"));
    assertEquals(
        0, overlapCount("iva_tax_rule_catalog", "official_tax_code, official_percentage_code"));
    assertEquals(0, overlapCount("payment_method_catalog", "official_code"));
  }

  @Test
  void asciiFixtureDoesNotChangeOrCompanyScopeTheGlobalReferenceBaseline() throws IOException {
    JsonNode fixture = InvoiceDraftMigrationTest.loadAsciiFixture(objectMapper);
    assertEquals(
        "^[A-Za-z0-9]{1,20}$",
        fixture.required("approvedPatterns").required("buyerIdentification").textValue());
    assertEquals(
        "^[A-Za-z0-9]{1,25}$",
        fixture.required("approvedPatterns").required("productCode").textValue());
    assertFalse(fixture.required("requestPipelineVectors").isEmpty());
    assertFalse(fixture.required("persistenceDefenseVectors").isEmpty());

    assertEquals(5, database.rowCount("buyer_identification_type_catalog"));
    assertEquals(6, database.rowCount("iva_tax_rule_catalog"));
    assertEquals(8, database.rowCount("payment_method_catalog"));
    assertEquals(
        0,
        database.scalarLong(
            "SELECT count(*) FROM information_schema.columns "
                + "WHERE table_schema = 'public' "
                + "AND table_name IN ('buyer_identification_type_catalog', "
                + "'iva_tax_rule_catalog', 'payment_method_catalog') "
                + "AND column_name = 'company_id'"));
  }

  @Test
  void productionCodeCannotGenerateOrInsertReferenceCatalogIdentifiers() throws IOException {
    Path mainJava = Path.of("src/main/java");
    try (Stream<Path> files = Files.walk(mainJava)) {
      String productionSource =
          files
              .filter(path -> path.toString().endsWith(".java"))
              .map(ReferenceDataBaselineTest::read)
              .collect(Collectors.joining("\n"));
      assertFalse(productionSource.contains(REFERENCE_NAMESPACE.toString()));
      assertFalse(productionSource.contains("reference-data.v1"));
      assertFalse(productionSource.matches("(?s).*UUID\\s*\\.\\s*nameUUIDFromBytes.*"));
      assertFalse(
          productionSource.matches(
              "(?is).*insert\\s+into\\s+(?:buyer_identification_type_catalog|"
                  + "iva_tax_rule_catalog|payment_method_catalog).*"));
    }
  }

  private long overlapCount(String table, String identityColumns) {
    String[] columns = identityColumns.split(", ");
    String identity =
        Stream.of(columns)
            .map(column -> "left_row." + column + " = right_row." + column)
            .collect(Collectors.joining(" AND "));
    return database.scalarLong(
        "SELECT count(*) FROM "
            + table
            + " left_row JOIN "
            + table
            + " right_row ON left_row.ctid < right_row.ctid AND "
            + identity
            + " AND left_row.active AND right_row.active AND "
            + "daterange(left_row.target_valid_from, "
            + "COALESCE(left_row.target_valid_to, 'infinity'::date), '[]') && "
            + "daterange(right_row.target_valid_from, "
            + "COALESCE(right_row.target_valid_to, 'infinity'::date), '[]')");
  }

  private static UUID uuidV5(UUID namespace, String name) throws NoSuchAlgorithmException {
    ByteBuffer namespaceBytes = ByteBuffer.allocate(16);
    namespaceBytes.putLong(namespace.getMostSignificantBits());
    namespaceBytes.putLong(namespace.getLeastSignificantBits());

    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
    sha1.update(namespaceBytes.array());
    byte[] digest = sha1.digest(name.getBytes(UTF_8));
    digest[6] = (byte) ((digest[6] & 0x0f) | 0x50);
    digest[8] = (byte) ((digest[8] & 0x3f) | 0x80);

    ByteBuffer uuidBytes = ByteBuffer.wrap(digest);
    return new UUID(uuidBytes.getLong(), uuidBytes.getLong());
  }

  private static String read(Path path) {
    try {
      return Files.readString(path, UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to inspect production source", exception);
    }
  }

  private record ExpectedTaxRule(
      String id,
      String percentageCode,
      String officialLabel,
      String displayName,
      String rate,
      String treatment,
      LocalDate sourceValidFrom) {}

  private record ExpectedPaymentMethod(
      String id,
      String officialCode,
      String officialLabel,
      String displayName,
      LocalDate sourceValidFrom) {}
}
