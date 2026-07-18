package com.alexastudillo.taxdocument.api.invoicedraft;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.application.invoicedraft.ApplicationTestFixtures;
import com.alexastudillo.taxdocument.application.invoicedraft.CreateInvoiceDraftService;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftCandidate;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftRepository;
import com.alexastudillo.taxdocument.application.invoicedraft.PersistedInvoiceDraft;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import com.alexastudillo.taxdocument.infrastructure.invoicedraft.InvoiceDraftTelemetry;
import com.alexastudillo.taxdocument.infrastructure.invoicedraft.PostgreSqlTestResource;
import com.alexastudillo.taxdocument.support.FixedRequestClock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class InvoiceDraftResourceTest {
  @Inject PostgreSqlTestResource database;
  @Inject FixedRequestClock fixedClock;

  @BeforeEach
  void resetDatabase() {
    database.resetSchema();
    fixedClock.reset(Instant.parse("2026-07-17T12:00:00Z"), Instant.parse("2026-07-17T12:00:01Z"));
  }

  @Test
  void httpCreateReplayConflictAndStageFivePrecedenceUseThePublishedContract() throws Exception {
    String body =
        InvoiceDraftRequestValidationTest.validJson(
            "\t123E4567-E89B-12D3-A456-426614174000\t", " Cafe\u0301 Buyer ");
    String created =
        given()
            .contentType("application/json")
            .header("X-Company-Id", ApplicationTestFixtures.COMPANY)
            .header("Idempotency-Key", "http-key-1")
            .header("X-Correlation-Id", "http-create")
            .body(body)
            .when()
            .post("/api/v1/invoice-drafts")
            .then()
            .statusCode(201)
            .header("Idempotency-Replayed", "false")
            .extract()
            .asString();
    JsonNode first = new ObjectMapper().findAndRegisterModules().readTree(created);
    assertEquals("123e4567-e89b-12d3-a456-426614174000", first.path("emissionPointId").asText());
    assertEquals(first.path("createdAt"), first.path("updatedAt"));
    assertTrue(first.path("grandTotal").isTextual());

    String replayed =
        given()
            .contentType("application/json")
            .header("X-Company-Id", ApplicationTestFixtures.COMPANY)
            .header("Idempotency-Key", "http-key-1")
            .body(body)
            .when()
            .post("/api/v1/invoice-drafts")
            .then()
            .statusCode(200)
            .header("Idempotency-Replayed", "true")
            .extract()
            .asString();
    JsonNode second = new ObjectMapper().findAndRegisterModules().readTree(replayed);
    assertEquals(first.path("id"), second.path("id"));
    assertEquals(first.path("createdAt"), second.path("createdAt"));
    assertEquals(first.path("updatedAt"), second.path("updatedAt"));
    assertEquals(1, fixedClock.persistenceCalls());

    given()
        .contentType("application/json")
        .header("X-Company-Id", ApplicationTestFixtures.COMPANY)
        .header("Idempotency-Key", "http-key-1")
        .body(body.replace("\"11.50\"", "\"11.51\""))
        .when()
        .post("/api/v1/invoice-drafts")
        .then()
        .statusCode(409);

    given()
        .contentType("application/json")
        .header("X-Company-Id", ApplicationTestFixtures.COMPANY)
        .header("Idempotency-Key", "calculated-key")
        .body(body.replace("\"payments\"", "\"grandTotal\":null,\"unknown\":true,\"payments\""))
        .when()
        .post("/api/v1/invoice-drafts")
        .then()
        .statusCode(422)
        .body("code", org.hamcrest.Matchers.equalTo("PROHIBITED_CALCULATED_FIELD"))
        .body("violations", org.hamcrest.Matchers.nullValue());
  }

  @Test
  void immediateCreateAndEquivalentReplayReturn201Then200WithOriginalTimestamps() throws Exception {
    InMemoryRepository repository = new InMemoryRepository();
    CreateInvoiceDraftService service =
        new CreateInvoiceDraftService(
            repository,
            ApplicationTestFixtures.references(),
            ApplicationTestFixtures.identifiers());
    Response created = invoke(service, repository, "corr-create");
    Response replayed = invoke(service, repository, "corr-replay");

    assertEquals(201, created.getStatus());
    assertEquals(200, replayed.getStatus());
    assertEquals("false", created.getHeaderString("Idempotency-Replayed"));
    assertEquals("true", replayed.getHeaderString("Idempotency-Replayed"));
    InvoiceDraftResponse first = (InvoiceDraftResponse) created.getEntity();
    InvoiceDraftResponse second = (InvoiceDraftResponse) replayed.getEntity();
    assertEquals(first.id(), second.id());
    assertEquals(first.createdAt(), second.createdAt());
    assertEquals(first.updatedAt(), second.updatedAt());
    assertEquals(first.createdAt(), first.updatedAt());
    assertEquals(1, repository.persistCalls);
    String responseJson = new ObjectMapper().findAndRegisterModules().writeValueAsString(first);
    assertTrue(responseJson.contains("\"grandTotal\":\"11.50\""));
    assertTrue(responseJson.contains("\"quantity\":\"1\""));
  }

  @Test
  void calculatedPropertyProducesOneValueFreeApiFailure() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    JsonNode request =
        objectMapper.readTree(
            InvoiceDraftRequestValidationTest.validJson(
                    "123e4567-e89b-12d3-a456-426614174000", "Buyer")
                .replace("\"payments\"", "\"grandTotal\":\"11.50\",\"payments\""));
    assertTrue(new InvoiceDraftRequestPropertyClassifier().containsCalculatedProperty(request));
    ProblemDetails.ApiException failure =
        new ProblemDetails.ApiException(
            422, "PROHIBITED_CALCULATED_FIELD", "Calculated properties must not be supplied");
    assertTrue(failure.violations().isEmpty());
  }

  private static Response invoke(
      CreateInvoiceDraftService service, InMemoryRepository repository, String correlation)
      throws Exception {
    FixedRequestClock clock = new FixedRequestClock();
    clock.reset(Instant.parse("2026-07-17T12:00:00Z"), Instant.parse("2026-07-17T12:00:01Z"));
    InvoiceDraftTelemetry telemetry = new InvoiceDraftTelemetry(new SimpleMeterRegistry());
    InvoiceDraftRequestDeadlineHandler deadline =
        new InvoiceDraftRequestDeadlineHandler(clock, telemetry);
    InvoiceDraftRequestState state = new InvoiceDraftRequestState();
    deadline.initialize(state, correlation);
    state.companyId(new CompanyId(ApplicationTestFixtures.COMPANY));
    state.idempotencyKey("draft-key-1");
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    InvoiceDraftResource resource =
        new InvoiceDraftResource(
            service,
            deadline,
            state,
            new InvoiceDraftRequestPropertyClassifier(),
            new InvoiceDraftApiMapper(),
            objectMapper,
            telemetry);
    String json =
        InvoiceDraftRequestValidationTest.validJson(
            "\t123E4567-E89B-12D3-A456-426614174000\t", " Cafe\u0301 Buyer ");
    Response response = resource.create(objectMapper.readTree(json)).await().indefinitely();
    assertFalse(state.deadline().expired());
    repository.timestamp = Instant.parse("2026-07-17T12:00:01Z");
    return response;
  }

  private static final class InMemoryRepository implements InvoiceDraftRepository {
    private PersistedInvoiceDraft stored;
    private byte[] fingerprint;
    private Instant timestamp = Instant.parse("2026-07-17T12:00:01Z");
    private int persistCalls;

    @Override
    public Uni<IdempotencyLookup> findByIdempotency(
        CompanyId companyId, byte[] keyHash, byte[] requestFingerprint, Duration remaining) {
      if (stored == null) {
        return Uni.createFrom().item(new IdempotencyLookup.Missing());
      }
      if (!Arrays.equals(fingerprint, requestFingerprint)) {
        return Uni.createFrom().item(new IdempotencyLookup.Conflict(fingerprint));
      }
      return Uni.createFrom().item(new IdempotencyLookup.Equivalent(stored));
    }

    @Override
    public Uni<PersistedInvoiceDraft> persist(InvoiceDraftCandidate candidate, Duration remaining) {
      persistCalls++;
      fingerprint = candidate.requestFingerprint();
      stored = new PersistedInvoiceDraft(candidate.draft(), timestamp, timestamp);
      return Uni.createFrom().item(stored);
    }
  }
}
