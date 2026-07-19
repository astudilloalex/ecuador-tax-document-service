package com.alexastudillo.taxdocument.runtime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.response.Response;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
class InvoiceDraftJvmSmokeIT {
  private static final String COMPANY = "A1111111-1111-4111-8111-111111111111";
  private static final String CANONICAL_COMPANY = "a1111111-1111-4111-8111-111111111111";
  private static final String CORRELATION_PATTERN = "^[A-Za-z0-9][A-Za-z0-9._:-]{0,63}$";
  private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
  private static final ObjectMapper YAML = new YAMLMapper();

  @Test
  void packagedJvmPublishesCanonicalOpenApiAndHealthyBoundaries() throws Exception {
    given().when().get("/q/health/live").then().statusCode(200).body("status", equalTo("UP"));
    given().when().get("/q/health/ready").then().statusCode(200).body("status", equalTo("UP"));

    String served =
        given()
            .accept("application/yaml")
            .when()
            .get("/q/openapi")
            .then()
            .statusCode(200)
            .extract()
            .asString();
    String canonical =
        Files.readString(
            Path.of("specs/001-create-invoice-draft/contracts/invoice-draft-api.openapi.yaml"));
    assertEquals(YAML.readTree(canonical), YAML.readTree(served));
    assertFalse(served.contains("securitySchemes:"));
    assertFalse(served.contains("security:"));
    assertFalse(served.contains("'401':"));
    assertFalse(served.contains("'403':"));
  }

  @Test
  void packagedJvmCoversCreateReplayConflictAndSafeFailures() throws Exception {
    LocalDate today = LocalDate.now(ZoneId.of("America/Guayaquil"));
    String key = "jvm-smoke-" + UUID.randomUUID();
    String correlation = "jvm-smoke-create-" + UUID.randomUUID();
    String body = validBody(today);

    Response created =
        given()
            .contentType("application/json")
            .header("X-Company-Id", COMPANY)
            .header("Idempotency-Key", key)
            .header("X-Correlation-Id", correlation)
            .body(body)
            .when()
            .post("/api/v1/invoice-drafts")
            .then()
            .statusCode(201)
            .header("X-Correlation-Id", correlation)
            .header("Idempotency-Replayed", "false")
            .body("companyId", equalTo(CANONICAL_COMPANY))
            .body("emissionPointId", equalTo("123e4567-e89b-12d3-a456-426614174000"))
            .body("emissionDate", equalTo(today.toString()))
            .body("status", equalTo("DRAFT"))
            .body("currency", equalTo("USD"))
            .body("grandTotal", equalTo("17.25"))
            .extract()
            .response();
    JsonNode first = JSON.readTree(created.asString());
    assertEquals(first.required("createdAt"), first.required("updatedAt"));

    Response replayed =
        given()
            .contentType("application/json")
            .header("X-Company-Id", COMPANY)
            .header("Idempotency-Key", key)
            .body(body)
            .when()
            .post("/api/v1/invoice-drafts")
            .then()
            .statusCode(200)
            .header("Idempotency-Replayed", "true")
            .extract()
            .response();
    JsonNode second = JSON.readTree(replayed.asString());
    assertEquals(first.required("id"), second.required("id"));
    assertEquals(first.required("createdAt"), second.required("createdAt"));
    assertEquals(first.required("updatedAt"), second.required("updatedAt"));

    given()
        .contentType("application/json")
        .header("X-Company-Id", COMPANY)
        .header("Idempotency-Key", key)
        .body(body.replace("\"17.25\"", "\"17.24\""))
        .when()
        .post("/api/v1/invoice-drafts")
        .then()
        .statusCode(409)
        .body("code", equalTo("IDEMPOTENCY_CONFLICT"));

    assertBusinessFailure(
        body.replace("123E4567-E89B-12D3-A456-426614174000", "not-a-uuid"),
        "EMISSION_POINT_INVALID");
    assertBusinessFailure(body.replace("buyer@example.com", "buyer@example.com."), "EMAIL_INVALID");
    assertBusinessFailure(
        body.replace("\"discount\": \"5.00\"", "\"discount\": \"25.00\"")
            .replace("\"amount\": \"17.25\"", "\"amount\": \"0.00\""),
        "DISCOUNT_EXCEEDS_GROSS");

    given()
        .contentType("application/json")
        .header("X-Company-Id", COMPANY)
        .header("Idempotency-Key", "jvm-date-" + UUID.randomUUID())
        .body(validBody(today.minusDays(1)))
        .when()
        .post("/api/v1/invoice-drafts")
        .then()
        .statusCode(422)
        .body("code", equalTo("BUSINESS_VALIDATION_FAILED"));

    String unsafeCorrelation = "unsafe correlation";
    given()
        .contentType("application/json")
        .header("X-Company-Id", COMPANY)
        .header("Idempotency-Key", "jvm-correlation-" + UUID.randomUUID())
        .header("X-Correlation-Id", unsafeCorrelation)
        .body(body)
        .when()
        .post("/api/v1/invoice-drafts")
        .then()
        .statusCode(400)
        .header("X-Correlation-Id", matchesPattern(CORRELATION_PATTERN))
        .body("code", equalTo("INVALID_REQUEST"));

    given()
        .contentType("application/json")
        .header("Idempotency-Key", "jvm-company-" + UUID.randomUUID())
        .body(body)
        .when()
        .post("/api/v1/invoice-drafts")
        .then()
        .statusCode(400)
        .body("code", equalTo("COMPANY_CONTEXT_REQUIRED"));

    given()
        .contentType("application/json")
        .header("X-Company-Id", COMPANY)
        .header("Idempotency-Key", "jvm-calculated-" + UUID.randomUUID())
        .body(body.replace("\"payments\"", "\"grandTotal\": null, \"payments\""))
        .when()
        .post("/api/v1/invoice-drafts")
        .then()
        .statusCode(422)
        .body("code", equalTo("PROHIBITED_CALCULATED_FIELD"));

    given()
        .contentType("application/json")
        .body("{\"padding\":\"" + "x".repeat(2_097_152) + "\"}")
        .when()
        .post("/api/v1/invoice-drafts")
        .then()
        .statusCode(413)
        .header("X-Correlation-Id", matchesPattern(CORRELATION_PATTERN))
        .body("code", equalTo("REQUEST_PAYLOAD_TOO_LARGE"));

    String oversizedBody = "{\"padding\":\"" + "x".repeat(2_097_152) + "\"}";
    given()
        .contentType("application/json")
        .header("X-Correlation-Id", "jvm-oversized-valid")
        .body(oversizedBody)
        .when()
        .post("/api/v1/invoice-drafts")
        .then()
        .statusCode(413)
        .header("X-Correlation-Id", "jvm-oversized-valid")
        .body("correlationId", equalTo("jvm-oversized-valid"))
        .body("code", equalTo("REQUEST_PAYLOAD_TOO_LARGE"));

    given()
        .contentType("application/json")
        .header("X-Correlation-Id", "unsafe oversized correlation")
        .body(oversizedBody)
        .when()
        .post("/api/v1/invoice-drafts")
        .then()
        .statusCode(413)
        .header("X-Correlation-Id", matchesPattern(CORRELATION_PATTERN))
        .body("correlationId", matchesPattern(CORRELATION_PATTERN))
        .body("code", equalTo("REQUEST_PAYLOAD_TOO_LARGE"));
  }

  private static void assertBusinessFailure(String body, String violationCode) {
    given()
        .contentType("application/json")
        .header("X-Company-Id", COMPANY)
        .header("Idempotency-Key", "jvm-failure-" + UUID.randomUUID())
        .body(body)
        .when()
        .post("/api/v1/invoice-drafts")
        .then()
        .statusCode(422)
        .body("code", equalTo("BUSINESS_VALIDATION_FAILED"))
        .body("violations[0].code", equalTo(violationCode));
  }

  private static String validBody(LocalDate emissionDate) {
    return """
        {
          "emissionPointId": "\\t123E4567-E89B-12D3-A456-426614174000\\t",
          "emissionDate": "%s",
          "buyer": {
            "identificationType": "06",
            "identification": "ABC123",
            "legalName": "Comprador JVM",
            "email": "buyer@example.com"
          },
          "lines": [
            {
              "productCode": "SKU1",
              "description": "Servicio JVM",
              "quantity": "2",
              "unitPrice": "10.000000",
              "discount": "5.00",
              "taxRuleId": "5b34b038-931c-50e3-a84c-10af272fdcd4"
            }
          ],
          "payments": [
            {
              "paymentMethodId": "639f2b7e-10a3-5d92-a1a3-28223896f5b5",
              "amount": "17.25"
            }
          ],
          "additionalInformation": []
        }
        """
        .formatted(emissionDate);
  }
}
