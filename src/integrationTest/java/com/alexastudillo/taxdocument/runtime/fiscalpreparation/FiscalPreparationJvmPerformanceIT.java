package com.alexastudillo.taxdocument.runtime.fiscalpreparation;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.support.fiscalpreparation.AuthoritativeFiscalContextFixture;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.response.Response;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;

@QuarkusIntegrationTest
@NullMarked
class FiscalPreparationJvmPerformanceIT {
  private static final String COMPANY = "c1111111-1111-4111-8111-111111111111";
  private static final UUID EMISSION_POINT =
      Objects.requireNonNull(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
  private static @Nullable AuthoritativeFiscalContextFixture fixture;
  private static @Nullable DevServicesContext devServicesContext;

  @BeforeAll
  static void startFixture() {
    fixture = AuthoritativeFiscalContextFixture.start();
  }

  @AfterAll
  static void stopFixture() {
    Objects.requireNonNull(fixture).close();
  }

  @Test
  void packagedJvmSerializesSameDraftAndOneScopeLoadsWithinDeadlineAndRecoversPool(
      TestReporter reporter) throws Exception {
    LocalDate today = Objects.requireNonNull(LocalDate.now(ZoneId.of("America/Guayaquil")));
    String suffix = Objects.requireNonNull(UUID.randomUUID().toString());
    String issuer = "performance-issuer-" + suffix;
    String establishment = "performance-establishment-" + suffix;
    configureProvider(issuer, establishment, today);
    UUID baseline = Objects.requireNonNull(UUID.randomUUID());
    provisionBaseline(baseline, issuer, establishment);

    String oneDraft = createDraft(today, "same-draft-" + suffix, "Same Draft Buyer");
    long sameStarted = System.nanoTime();
    List<Response> equivalent =
        concurrentPosts(Objects.requireNonNull(java.util.Collections.nCopies(100, oneDraft)));
    Duration sameDuration = elapsed(sameStarted);
    assertTrue(sameDuration.compareTo(Duration.ofSeconds(10)) < 0);
    assertEquals(1L, equivalent.stream().filter(response -> response.statusCode() == 201).count());
    assertEquals(99L, equivalent.stream().filter(response -> response.statusCode() == 200).count());
    assertEquals(
        1,
        new HashSet<>(
                equivalent.stream()
                    .map(response -> response.jsonPath().getString("fiscalPreparationId"))
                    .toList())
            .size());

    List<String> drafts = new ArrayList<>();
    for (int index = 0; index < 100; index++) {
      LocalDate scopeDate = Objects.requireNonNull(today);
      drafts.add(createDraft(scopeDate, "scope-" + suffix + '-' + index, "Scope Buyer " + index));
    }
    long scopeStarted = System.nanoTime();
    List<Response> allocated = concurrentPosts(drafts);
    Duration scopeDuration = elapsed(scopeStarted);
    assertTrue(scopeDuration.compareTo(Duration.ofSeconds(10)) < 0);
    assertEquals(100L, allocated.stream().filter(response -> response.statusCode() == 201).count());
    List<Integer> sequential =
        allocated.stream()
            .map(
                response ->
                    Integer.parseInt(response.jsonPath().getString("officialSequentialNumber")))
            .sorted()
            .toList();
    assertEquals(java.util.stream.IntStream.rangeClosed(2, 101).boxed().toList(), sequential);
    assertEquals(
        100,
        allocated.stream()
            .map(response -> response.jsonPath().getString("accessKey"))
            .distinct()
            .count());
    assertEquals(101, baselineValue(baseline));

    given().when().get("/q/health/ready").then().statusCode(200);
    given().header("X-Company-Id", COMPANY).when().post(path(oneDraft)).then().statusCode(200);
    String runtimeLog = Files.readString(Path.of("build/quarkus.log"));
    assertFalse(runtimeLog.contains("blocked thread"));
    assertFalse(runtimeLog.contains("Thread blocked"));
    reporter.publishEntry(
        "sameDraftMillis", Objects.requireNonNull(Long.toString(sameDuration.toMillis())));
    reporter.publishEntry(
        "oneScopeMillis", Objects.requireNonNull(Long.toString(scopeDuration.toMillis())));
    reporter.publishEntry("poolRecovery", "PASS");
  }

  private static List<Response> concurrentPosts(List<String> draftIds) throws Exception {
    try (ExecutorService executor = Executors.newFixedThreadPool(100)) {
      List<CompletableFuture<Response>> requests =
          Objects.requireNonNull(
              draftIds.stream()
                  .map(
                      draftId ->
                          CompletableFuture.supplyAsync(
                              () ->
                                  given()
                                      .header("X-Company-Id", COMPANY)
                                      .when()
                                      .post(path(Objects.requireNonNull(draftId))),
                              executor))
                  .toList());
      CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new)).get(15, TimeUnit.SECONDS);
      return Objects.requireNonNull(
          requests.stream().map(future -> Objects.requireNonNull(future).join()).toList());
    }
  }

  private static String createDraft(LocalDate date, String key, String buyer) {
    return Objects.requireNonNull(
        given()
            .contentType("application/json")
            .header("X-Company-Id", COMPANY)
            .header("Idempotency-Key", key)
            .body(
                """
                {"emissionPointId":"%s","emissionDate":"%s",
                 "buyer":{"identificationType":"06","identification":"PERF123","legalName":"%s"},
                 "lines":[{"productCode":"PERF1","description":"Performance service","quantity":"1",
                 "unitPrice":"10.000000","discount":"0.00",
                 "taxRuleId":"5b34b038-931c-50e3-a84c-10af272fdcd4"}],
                 "payments":[{"paymentMethodId":"639f2b7e-10a3-5d92-a1a3-28223896f5b5","amount":"11.50"}],
                 "additionalInformation":[]}
                """
                    .formatted(EMISSION_POINT, date, buyer))
            .when()
            .post("/api/v1/invoice-drafts")
            .then()
            .statusCode(201)
            .extract()
            .<String>path("id"));
  }

  private static void provisionBaseline(UUID id, String issuer, String establishment)
      throws Exception {
    try (Connection connection = connection();
        PreparedStatement statement =
            connection.prepareStatement(
                "INSERT INTO official_sequence_baseline "
                    + "(id, company_id, issuer_reference, establishment_reference, emission_point_id, "
                    + "establishment_code, emission_point_code, document_type_code, last_allocated, "
                    + "created_at, updated_at) VALUES (?,?::uuid,?,?,?::uuid,'001','001','01',0,?,?)")) {
      statement.setObject(1, id);
      statement.setString(2, COMPANY);
      statement.setString(3, issuer);
      statement.setString(4, establishment);
      statement.setString(5, EMISSION_POINT.toString());
      statement.setObject(6, java.time.OffsetDateTime.parse("2026-07-18T11:00:00Z"));
      statement.setObject(7, java.time.OffsetDateTime.parse("2026-07-18T11:00:00Z"));
      statement.executeUpdate();
    }
  }

  private static int baselineValue(UUID baseline) throws Exception {
    try (Connection connection = connection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT last_allocated FROM official_sequence_baseline WHERE id=?")) {
      statement.setObject(1, baseline);
      try (var rows = statement.executeQuery()) {
        if (!rows.next()) {
          throw new IllegalStateException("Packaged JVM baseline fixture is absent");
        }
        return rows.getInt(1);
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

  private static void configureProvider(String issuer, String establishment, LocalDate date) {
    Objects.requireNonNull(fixture)
        .plan(
            200,
            Objects.requireNonNull(
                """
                {"issuerReference":"%s","issuerRuc":"1790012345001","legalName":"Performance Issuer",
                 "headOfficeAddress":"Quito","accountingRequired":true,"rimpeClassification":"NONE",
                 "establishmentReference":"%s","establishmentCode":"001","establishmentAddress":"Quito",
                 "emissionPointId":"%s","emissionPointCode":"001","environmentCode":"1",
                 "documentTypeCode":"01","emissionTypeCode":"1","invoiceIssuanceEligible":true,
                 "sourceEvidence":{"authority":"SRI fixture","revision":"%s","effectiveFrom":"%s",
                 "observedAt":"%s"}}
                """
                    .formatted(issuer, establishment, EMISSION_POINT, issuer, date, Instant.now())),
            Objects.requireNonNull(Duration.ZERO));
  }

  private static String path(String draftId) {
    return "/api/v1/invoice-drafts/" + draftId + "/fiscal-preparation";
  }

  private static Duration elapsed(long started) {
    return Objects.requireNonNull(Duration.ofNanos(System.nanoTime() - started));
  }
}
