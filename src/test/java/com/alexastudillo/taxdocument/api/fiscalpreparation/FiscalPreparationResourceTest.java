package com.alexastudillo.taxdocument.api.fiscalpreparation;

import static io.restassured.RestAssured.given;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alexastudillo.taxdocument.support.FixedRequestClock;
import com.alexastudillo.taxdocument.support.fiscalpreparation.AuthoritativeFiscalContextFixture;
import com.alexastudillo.taxdocument.support.fiscalpreparation.FiscalPreparationPostgreSqlSupport;
import com.alexastudillo.taxdocument.support.fiscalpreparation.FiscalPreparationTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@NullMarked
class FiscalPreparationResourceTest {
  private static @Nullable AuthoritativeFiscalContextFixture fixture;

  @Inject FiscalPreparationPostgreSqlSupport database;
  @Inject FixedRequestClock clock;

  @BeforeAll
  static void startFixture() {
    fixture = AuthoritativeFiscalContextFixture.start();
  }

  @AfterAll
  static void stopFixture() {
    contextFixture().close();
  }

  @BeforeEach
  void reset() {
    contextFixture().reset();
    database.resetSchema();
    clock.reset(instant("2026-07-18T12:00:00Z"), instant("2026-07-18T12:00:01Z"));
    database.insertControlledDraft(
        FiscalPreparationTestFixtures.COMPANY_UUID,
        FiscalPreparationTestFixtures.DRAFT,
        FiscalPreparationTestFixtures.EMISSION_POINT,
        FiscalPreparationTestFixtures.DATE,
        beforeCreatedAt());
    database.insertControlledBaseline(
        FiscalPreparationTestFixtures.BASELINE,
        FiscalPreparationTestFixtures.COMPANY_UUID,
        "issuer-fixture-1",
        "establishment-fixture-1",
        FiscalPreparationTestFixtures.EMISSION_POINT,
        "001",
        "001",
        122,
        beforeCreatedAt());
  }

  @Test
  void bodylessFirstPreparationAndCorrelationIndependentReplayReturnTheSamePersistedIdentity()
      throws Exception {
    String path = path(FiscalPreparationTestFixtures.DRAFT);
    String first =
        given()
            .header("X-Company-Id", FiscalPreparationTestFixtures.COMPANY_UUID)
            .header("X-Correlation-Id", "corr-first")
            .when()
            .post(path)
            .then()
            .statusCode(201)
            .header("Fiscal-Preparation-Replayed", "false")
            .header("Cache-Control", "no-store")
            .header("X-Correlation-Id", "corr-first")
            .body("invoiceDraftId", equalTo(FiscalPreparationTestFixtures.DRAFT.toString()))
            .body("officialSequentialNumber", equalTo("000000123"))
            .body("numericCode", matchesPattern("^[0-9]{8}$"))
            .body("accessKey", matchesPattern("^[0-9]{49}$"))
            .body("companyId", org.hamcrest.Matchers.nullValue())
            .extract()
            .asString();
    assertEquals(1, contextFixture().callCount());

    contextFixture().providerStatus(503);
    String replay =
        given()
            .header("X-Company-Id", FiscalPreparationTestFixtures.COMPANY_UUID)
            .header("X-Correlation-Id", "corr-replay")
            .when()
            .post(path)
            .then()
            .statusCode(200)
            .header("Fiscal-Preparation-Replayed", "true")
            .header("Cache-Control", "no-store")
            .header("X-Correlation-Id", "corr-replay")
            .extract()
            .asString();
    assertEquals(
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(first),
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(replay));
    assertEquals(1, contextFixture().callCount());
    assertEquals(
        123,
        database.lastAllocated(
            FiscalPreparationTestFixtures.COMPANY_UUID, FiscalPreparationTestFixtures.BASELINE));
  }

  @Test
  void companyAndRequestShapeFailuresPrecedeEveryCompanyOwnedReadAndSideEffect() {
    String path = path(FiscalPreparationTestFixtures.DRAFT);
    given()
        .when()
        .post(path)
        .then()
        .statusCode(400)
        .body("code", equalTo("COMPANY_CONTEXT_REQUIRED"));
    given()
        .header("X-Company-Id", "00000000-0000-0000-0000-000000000000")
        .when()
        .post(path)
        .then()
        .statusCode(400)
        .body("code", equalTo("COMPANY_CONTEXT_INVALID"));
    given()
        .header("X-Company-Id", FiscalPreparationTestFixtures.COMPANY_UUID)
        .contentType("application/json")
        .body("{}")
        .when()
        .post(path)
        .then()
        .statusCode(400)
        .body("code", equalTo("INVALID_REQUEST"));
    given()
        .header("X-Company-Id", FiscalPreparationTestFixtures.COMPANY_UUID)
        .header("Idempotency-Key", "caller-must-not-control-equivalence")
        .when()
        .post(path)
        .then()
        .statusCode(400)
        .body("code", equalTo("INVALID_REQUEST"));
    assertEquals(0, contextFixture().callCount());
    assertEquals(0L, database.rowCount("fiscal_preparation"));
  }

  @Test
  void crossCompanyAndUnsafeCorrelationNeverLeakExistenceOrUnsafeInput() {
    given()
        .header("X-Company-Id", UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"))
        .header("X-Correlation-Id", "unsafe/value")
        .when()
        .post(path(FiscalPreparationTestFixtures.DRAFT))
        .then()
        .statusCode(404)
        .header("X-Correlation-Id", matchesPattern("^[0-9a-f-]{36}$"))
        .body("code", equalTo("INVOICE_DRAFT_NOT_FOUND"))
        .body(
            "detail",
            org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("unsafe/value")));
    assertEquals(0, contextFixture().callCount());
  }

  private static String path(UUID draftId) {
    return "/api/v1/invoice-drafts/" + draftId + "/fiscal-preparation";
  }

  private static AuthoritativeFiscalContextFixture contextFixture() {
    return requireNonNull(fixture, "authoritative fiscal context fixture");
  }

  private static Instant instant(String value) {
    return requireNonNull(Instant.parse(value));
  }

  private static Instant beforeCreatedAt() {
    return requireNonNull(FiscalPreparationTestFixtures.CREATED_AT.minusSeconds(60));
  }
}
