package com.alexastudillo.taxdocument.api.invoicedraft;

import com.alexastudillo.taxdocument.api.invoicedraft.telemetry.InvoiceDraftTelemetryPort;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftApplicationException;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Maps only an API-accepted terminal outcome to HTTP. */
@Provider
@ApplicationScoped
public final class InvoiceDraftExceptionMapper implements ExceptionMapper<Throwable> {
  private static final String PROBLEM_JSON = "application/problem+json";
  private final InvoiceDraftRequestState state;
  private final InvoiceDraftTelemetryPort telemetry;

  public InvoiceDraftExceptionMapper(
      InvoiceDraftRequestState state, InvoiceDraftTelemetryPort telemetry) {
    this.state = state;
    this.telemetry = telemetry;
  }

  @Override
  public Response toResponse(Throwable exception) {
    Mapping mapping = mapping(exception);
    String correlation = safeCorrelation();
    state.acceptTerminal();
    state.acceptedStatus(mapping.status());
    telemetry.failed(state, telemetryOutcome(mapping), mapping.status());
    List<ProblemDetails.Violation> violations = mapping.violations();
    ProblemDetails problem = new ProblemDetails(
        URI.create("urn:ecuador-tax-document-service:problem:" + mapping.code().toLowerCase()),
        mapping.title(),
        mapping.status(),
        mapping.code(),
        mapping.detail(),
        URI.create("/api/v1/invoice-drafts"),
        correlation,
        violations.isEmpty() ? null : violations);
    return Response.status(mapping.status())
        .type(PROBLEM_JSON)
        .header("X-Correlation-Id", correlation)
        .entity(problem)
        .build();
  }

  private static String telemetryOutcome(Mapping mapping) {
    return switch (mapping.code()) {
      case "IDEMPOTENCY_CONFLICT" -> "conflict";
      case "REQUEST_TIMEOUT" -> "timeout";
      case "PERSISTENCE_UNAVAILABLE", "INTERNAL_ERROR" -> "rollback";
      default -> "rejected";
    };
  }

  private Mapping mapping(Throwable exception) {
    if (exception instanceof ProblemDetails.ApiException api) {
      return new Mapping(
          api.status(), api.code(), title(api.code()), api.getMessage(), api.violations());
    }
    if (exception instanceof InvoiceDraftApplicationException application) {
      InvoiceDraftFailure failure = application.failure();
      int status = status(failure.code());
      List<ProblemDetails.Violation> violations = failure.violations().stream().<ProblemDetails.Violation>map(
          value -> new ProblemDetails.Violation(
              value.code(),
              value.field(),
              value.validationStage(),
              value.maximum(),
              value.countingUnit()))
          .toList();
      return new Mapping(
          status,
          failure.code().name(),
          title(failure.code().name()),
          failure.detail(),
          violations);
    }
    if (exception instanceof JsonProcessingException) {
      return new Mapping(
          400, "INVALID_REQUEST", title("INVALID_REQUEST"), "The request is invalid", List.of());
    }
    if (exception instanceof WebApplicationException web && web.getResponse().getStatus() == 413) {
      return new Mapping(
          413,
          "REQUEST_PAYLOAD_TOO_LARGE",
          title("REQUEST_PAYLOAD_TOO_LARGE"),
          "The request body exceeds 2 MiB",
          List.of());
    }
    return new Mapping(
        500,
        "INTERNAL_ERROR",
        title("INTERNAL_ERROR"),
        "The request could not be completed",
        List.of());
  }

  private String safeCorrelation() {
    return state.correlationId() == null ? UUID.randomUUID().toString() : state.correlationId();
  }

  private static int status(InvoiceDraftFailure.Code code) {
    return switch (code) {
      case BUSINESS_VALIDATION_FAILED -> 422;
      case IDEMPOTENCY_CONFLICT -> 409;
      case PERSISTENCE_UNAVAILABLE -> 503;
      case REQUEST_TIMEOUT -> 504;
      case INTERNAL_ERROR -> 500;
    };
  }

  private static String title(String code) {
    return switch (code) {
      case "COMPANY_CONTEXT_REQUIRED",
          "COMPANY_CONTEXT_INVALID",
          "IDEMPOTENCY_KEY_REQUIRED",
          "IDEMPOTENCY_KEY_INVALID",
          "IDEMPOTENCY_KEY_MULTIPLE",
          "INVALID_REQUEST" ->
        "Invalid request";
      case "PROHIBITED_CALCULATED_FIELD", "BUSINESS_VALIDATION_FAILED" -> "Validation failed";
      case "IDEMPOTENCY_CONFLICT" -> "Idempotency conflict";
      case "REQUEST_PAYLOAD_TOO_LARGE" -> "Payload too large";
      case "PERSISTENCE_UNAVAILABLE" -> "Persistence unavailable";
      case "REQUEST_TIMEOUT" -> "Request timeout";
      default -> "Internal error";
    };
  }

  private record Mapping(
      int status,
      String code,
      String title,
      String detail,
      List<ProblemDetails.Violation> violations) {
  }
}
