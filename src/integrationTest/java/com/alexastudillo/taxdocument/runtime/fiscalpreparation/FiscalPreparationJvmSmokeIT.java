package com.alexastudillo.taxdocument.runtime.fiscalpreparation;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.support.fiscalpreparation.AuthoritativeFiscalContextFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.response.Response;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
@NullMarked
class FiscalPreparationJvmSmokeIT {
  private static final String COMPANY = "b1111111-1111-4111-8111-111111111111";
  private static final UUID EMISSION_POINT =
      Objects.requireNonNull(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
  private static final ObjectMapper JSON =
      Objects.requireNonNull(new ObjectMapper().findAndRegisterModules());
  private static @Nullable AuthoritativeFiscalContextFixture fixture;
  private static @Nullable DevServicesContext devServicesContext;

  private static AuthoritativeFiscalContextFixture fixture() {
    return Objects.requireNonNull(fixture, "fixture");
  }

  @BeforeAll
  static void startFixture() {
    fixture = AuthoritativeFiscalContextFixture.start();
  }

  @AfterAll
  static void stopFixture() {
    if (fixture != null) {
      fixture.close();
    }
  }

  @Test
  void packagedJvmPreparesReplaysDuringOutageAndPreservesTheDraft() throws Exception {
    LocalDate today = Objects.requireNonNull(LocalDate.now(ZoneId.of("America/Guayaquil")));
    String scopeSuffix = Objects.requireNonNull(UUID.randomUUID()).toString();
    configureProvider("issuer-" + scopeSuffix, "establishment-" + scopeSuffix, today);
    String draftId = createDraft(today, "fiscal-smoke-" + scopeSuffix);
    provisionBaseline(
        Objects.requireNonNull(UUID.randomUUID()),
        "issuer-" + scopeSuffix,
        "establishment-" + scopeSuffix,
        122);
    JsonNode before = invoiceDraftRow(draftId);

    String path = "/api/v1/invoice-drafts/" + draftId + "/fiscal-preparation";
    JsonNode first =
        JSON.readTree(
            Objects.requireNonNull(
                given()
                    .header("X-Company-Id", COMPANY)
                    .header("X-Correlation-Id", "jvm-fiscal-first")
                    .when()
                    .post(path)
                    .then()
                    .statusCode(201)
                    .header("Fiscal-Preparation-Replayed", "false")
                    .header("Cache-Control", "no-store")
                    .body("officialSequentialNumber", equalTo("000000123"))
                    .body("numericCode", matchesPattern("^[0-9]{8}$"))
                    .body("accessKey", matchesPattern("^[0-9]{49}$"))
                    .extract()
                    .asString()));
    fixture().providerStatus(503);
    JsonNode replay =
        JSON.readTree(
            given()
                .header("X-Company-Id", COMPANY)
                .header("X-Correlation-Id", "jvm-fiscal-replay")
                .when()
                .post(path)
                .then()
                .statusCode(200)
                .header("Fiscal-Preparation-Replayed", "true")
                .header("Cache-Control", "no-store")
                .extract()
                .asString());
    assertEquals(first, replay);
    assertEquals(before, invoiceDraftRow(draftId));
    String fiscalAudit =
        Files.readString(Path.of("build/quarkus.log"))
            .lines()
            .filter(line -> line.contains("fiscal_preparation_audit"))
            .collect(Collectors.joining("\n"));
    assertTrue(fiscalAudit.contains("jvm-fiscal-first"));
    assertTrue(fiscalAudit.contains("jvm-fiscal-replay"));
    for (String forbidden :
        new String[] {
          COMPANY,
          draftId,
          first.required("accessKey").asText(),
          first.required("numericCode").asText(),
          first.required("fiscalContextSnapshot").required("issuerRuc").asText(),
          first.required("fiscalContextSnapshot").required("sourceRevision").asText()
        }) {
      assertFalse(fiscalAudit.contains(forbidden), forbidden);
    }
  }

  @Test
  void packagedJvmProviderTimeoutFailsBeforeMissingBaselineIsObserved() {
    LocalDate today = Objects.requireNonNull(LocalDate.now(ZoneId.of("America/Guayaquil")));
    String suffix = Objects.requireNonNull(UUID.randomUUID()).toString();
    configureProvider("issuer-" + suffix, "establishment-" + suffix, today);
    String draftId = createDraft(today, "fiscal-timeout-" + suffix);
    fixture().delayed(Objects.requireNonNull(java.time.Duration.ofSeconds(3)));
    given()
        .header("X-Company-Id", COMPANY)
        .when()
        .post("/api/v1/invoice-drafts/" + draftId + "/fiscal-preparation")
        .then()
        .statusCode(503)
        .body("code", equalTo("FISCAL_CONTEXT_UNAVAILABLE"));
  }

  @Test
  void packagedJvmDiscardedSuccessIsRecoveredByNaturalReplay() throws Exception {
    LocalDate today = Objects.requireNonNull(LocalDate.now(ZoneId.of("America/Guayaquil")));
    String suffix = Objects.requireNonNull(UUID.randomUUID()).toString();
    configureProvider("discarded-issuer-" + suffix, "discarded-establishment-" + suffix, today);
    String draftId = createDraft(today, "fiscal-discarded-" + suffix);
    provisionBaseline(
        Objects.requireNonNull(UUID.randomUUID()),
        "discarded-issuer-" + suffix,
        "discarded-establishment-" + suffix,
        0);
    String path = "/api/v1/invoice-drafts/" + draftId + "/fiscal-preparation";

    given().header("X-Company-Id", COMPANY).when().post(path).then().statusCode(201);
    fixture().providerStatus(503);
    given()
        .header("X-Company-Id", COMPANY)
        .when()
        .post(path)
        .then()
        .statusCode(200)
        .header("Fiscal-Preparation-Replayed", "true")
        .body("officialSequentialNumber", equalTo("000000001"));
  }

  @Test
  void packagedJvmConfirmedPersistenceFailureRollsBackPreparationAndBaseline() throws Exception {
    LocalDate today = Objects.requireNonNull(LocalDate.now(ZoneId.of("America/Guayaquil")));
    String suffix = Objects.requireNonNull(UUID.randomUUID()).toString();
    String issuer = "rollback-issuer-" + suffix;
    String establishment = "rollback-establishment-" + suffix;
    configureProvider(issuer, establishment, today);
    String draftId = createDraft(today, "fiscal-rollback-" + suffix);
    UUID baseline = Objects.requireNonNull(UUID.randomUUID());
    provisionBaseline(
        baseline,
        issuer,
        establishment,
        7,
        Objects.requireNonNull(java.time.OffsetDateTime.parse("2099-01-01T00:00:00Z")));

    given()
        .header("X-Company-Id", COMPANY)
        .when()
        .post("/api/v1/invoice-drafts/" + draftId + "/fiscal-preparation")
        .then()
        .statusCode(503)
        .body("code", equalTo("PERSISTENCE_FAILURE"));
    assertEquals(7, baselineValue(baseline));
    assertEquals(0L, preparationCount(draftId));
  }

  @Test
  void packagedJvmCommitDeadlineReturnsConservativeGuidanceAndRetryConverges() throws Exception {
    LocalDate today = Objects.requireNonNull(LocalDate.now(ZoneId.of("America/Guayaquil")));
    String suffix = Objects.requireNonNull(UUID.randomUUID()).toString();
    String issuer = "uncertain-issuer-" + suffix;
    String establishment = "uncertain-establishment-" + suffix;
    configureProvider(issuer, establishment, today);
    String draftId = createDraft(today, "fiscal-uncertain-" + suffix);
    UUID baseline = Objects.requireNonNull(UUID.randomUUID());
    provisionBaseline(baseline, issuer, establishment, 18);
    installCommitDelay(draftId);
    String path = "/api/v1/invoice-drafts/" + draftId + "/fiscal-preparation";

    try {
      Response uncertain =
          given().header("X-Company-Id", COMPANY).when().post(path).then().extract().response();
      assertEquals(503, uncertain.statusCode());
      assertEquals("PREPARATION_OUTCOME_UNKNOWN", uncertain.jsonPath().getString("code"));
      assertTrue(uncertain.jsonPath().getString("detail").contains("retry the same Company"));
    } finally {
      removeCommitDelay();
    }

    int retryStatus =
        given().header("X-Company-Id", COMPANY).when().post(path).then().extract().statusCode();
    assertTrue(retryStatus == 200 || retryStatus == 201);
    assertEquals(1L, preparationCount(draftId));
    assertEquals(19, baselineValue(baseline));
  }

  private static String createDraft(LocalDate date, String key) {
    return Objects.requireNonNull(
        given()
            .contentType("application/json")
            .header("X-Company-Id", COMPANY)
            .header("Idempotency-Key", key)
            .body(invoiceDraftBody(date))
            .when()
            .post("/api/v1/invoice-drafts")
            .then()
            .statusCode(201)
            .extract()
            .path("id"));
  }

  private static void provisionBaseline(
      UUID id, String issuerReference, String establishmentReference, int lastAllocated)
      throws Exception {
    provisionBaseline(
        id,
        issuerReference,
        establishmentReference,
        lastAllocated,
        Objects.requireNonNull(java.time.OffsetDateTime.parse("2026-07-18T11:00:00Z")));
  }

  private static void provisionBaseline(
      UUID id,
      String issuerReference,
      String establishmentReference,
      int lastAllocated,
      java.time.OffsetDateTime timestamp)
      throws Exception {
    try (Connection connection = connection();
        PreparedStatement statement =
            connection.prepareStatement(
                "INSERT INTO official_sequence_baseline "
                    + "(id, company_id, issuer_reference, establishment_reference, emission_point_id, "
                    + "establishment_code, emission_point_code, document_type_code, last_allocated, "
                    + "created_at, updated_at) VALUES (?,?::uuid,?,?,?::uuid,'001','001','01',?,?,?)")) {
      statement.setObject(1, id);
      statement.setString(2, COMPANY);
      statement.setString(3, issuerReference);
      statement.setString(4, establishmentReference);
      statement.setString(5, EMISSION_POINT.toString());
      statement.setInt(6, lastAllocated);
      statement.setObject(7, timestamp);
      statement.setObject(8, timestamp);
      statement.executeUpdate();
    }
  }

  private static int baselineValue(UUID baselineId) throws Exception {
    try (Connection connection = connection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT last_allocated FROM official_sequence_baseline WHERE id=?")) {
      statement.setObject(1, baselineId);
      try (var rows = statement.executeQuery()) {
        if (!rows.next()) {
          throw new IllegalStateException("Packaged JVM baseline fixture is absent");
        }
        return rows.getInt(1);
      }
    }
  }

  private static long preparationCount(String draftId) throws Exception {
    try (Connection connection = connection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT count(*) FROM fiscal_preparation "
                    + "WHERE company_id=?::uuid AND invoice_draft_id=?::uuid")) {
      statement.setString(1, COMPANY);
      statement.setString(2, draftId);
      try (var rows = statement.executeQuery()) {
        if (!rows.next()) {
          throw new IllegalStateException("Packaged JVM preparation count is unavailable");
        }
        return rows.getLong(1);
      }
    }
  }

  private static void installCommitDelay(String draftId) throws Exception {
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          """
          CREATE OR REPLACE FUNCTION fiscal_preparation_commit_delay_test()
          RETURNS trigger LANGUAGE plpgsql AS $function$
          BEGIN
            IF NEW.invoice_draft_id = '%s'::uuid THEN
              PERFORM pg_sleep(11);
            END IF;
            RETURN NEW;
          END
          $function$
          """
              .formatted(draftId));
      statement.execute(
          """
          CREATE CONSTRAINT TRIGGER fiscal_preparation_commit_delay_test
          AFTER INSERT ON fiscal_preparation
          DEFERRABLE INITIALLY DEFERRED
          FOR EACH ROW EXECUTE FUNCTION fiscal_preparation_commit_delay_test()
          """);
    }
  }

  private static void removeCommitDelay() throws Exception {
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          "DROP TRIGGER IF EXISTS fiscal_preparation_commit_delay_test ON fiscal_preparation");
      statement.execute("DROP FUNCTION IF EXISTS fiscal_preparation_commit_delay_test()");
    }
  }

  private static JsonNode invoiceDraftRow(String draftId) throws Exception {
    try (Connection connection = connection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT row_to_json(d)::text FROM invoice_draft d WHERE company_id=?::uuid AND id=?::uuid")) {
      statement.setString(1, COMPANY);
      statement.setString(2, draftId);
      try (var rows = statement.executeQuery()) {
        if (!rows.next()) {
          throw new IllegalStateException("Packaged JVM draft fixture is absent");
        }
        return Objects.requireNonNull(JSON.readTree(rows.getString(1)));
      }
    }
  }

  private static Connection connection() throws Exception {
    DevServicesContext context = Objects.requireNonNull(devServicesContext, "devServicesContext");
    String url =
        context
            .devServicesProperties()
            .getOrDefault(
                "quarkus.datasource.jdbc.url",
                Objects.requireNonNullElse(
                    System.getenv("DB_URL"), "jdbc:postgresql://localhost:5432/sri_db"));
    String user =
        context
            .devServicesProperties()
            .getOrDefault(
                "quarkus.datasource.username",
                Objects.requireNonNullElse(System.getenv("DB_USER"), "postgres"));
    String password =
        context
            .devServicesProperties()
            .getOrDefault(
                "quarkus.datasource.password",
                Objects.requireNonNullElse(System.getenv("DB_PASSWORD"), "admin"));
    return Objects.requireNonNull(DriverManager.getConnection(url, user, password));
  }

  private static void configureProvider(
      String issuerReference, String establishmentReference, LocalDate date) {
    fixture()
        .plan(
            200,
            Objects.requireNonNull(
                """
            {
              "issuerReference":"%s","issuerRuc":"1790012345001","legalName":"Fixture Issuer S.A.",
              "headOfficeAddress":"Quito","accountingRequired":true,"rimpeClassification":"NONE",
              "establishmentReference":"%s","establishmentCode":"001","establishmentAddress":"Quito",
              "emissionPointId":"%s","emissionPointCode":"001","environmentCode":"1",
              "documentTypeCode":"01","emissionTypeCode":"1","invoiceIssuanceEligible":true,
              "sourceEvidence":{"authority":"SRI fixture","revision":"%s","effectiveFrom":"%s",
              "observedAt":"%s"}
            }
            """
                    .formatted(
                        issuerReference,
                        establishmentReference,
                        EMISSION_POINT,
                        issuerReference,
                        date,
                        Instant.now())),
            Objects.requireNonNull(java.time.Duration.ZERO));
  }

  private static String invoiceDraftBody(LocalDate date) {
    return Objects.requireNonNull(
        """
        {
          "emissionPointId":"%s","emissionDate":"%s",
          "buyer":{"identificationType":"06","identification":"FISCAL123","legalName":"Fiscal Buyer"},
          "lines":[{"productCode":"FISCAL1","description":"Fiscal service","quantity":"1",
          "unitPrice":"10.000000","discount":"0.00",
          "taxRuleId":"5b34b038-931c-50e3-a84c-10af272fdcd4"}],
          "payments":[{"paymentMethodId":"639f2b7e-10a3-5d92-a1a3-28223896f5b5","amount":"11.50"}],
          "additionalInformation":[]
        }
        """
            .formatted(EMISSION_POINT, date));
  }
}
