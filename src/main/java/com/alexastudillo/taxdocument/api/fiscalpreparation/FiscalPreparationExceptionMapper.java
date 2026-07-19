package com.alexastudillo.taxdocument.api.fiscalpreparation;

import com.alexastudillo.taxdocument.api.fiscalpreparation.telemetry.FiscalPreparationTelemetryPort;
import com.alexastudillo.taxdocument.api.problem.ProblemDetails;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationApplicationException;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationFailure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Maps stable Fiscal Preparation failures without exposing causes, SQL, or fiscal values. */
@NullMarked
@Provider
@ApplicationScoped
public final class FiscalPreparationExceptionMapper
    implements ExceptionMapper<FiscalPreparationApplicationException> {
  private static final String PROBLEM_JSON = "application/problem+json";
  private final FiscalPreparationRequestState state;
  private final FiscalPreparationTelemetryPort telemetry;

  public FiscalPreparationExceptionMapper(
      FiscalPreparationRequestState state, FiscalPreparationTelemetryPort telemetry) {
    this.state = Objects.requireNonNull(state, "state");
    this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
  }

  @Override
  public Response toResponse(@Nullable FiscalPreparationApplicationException exception) {
    if (exception == null) {
      return Objects.requireNonNull(Response.status(500).build());
    }
    FiscalPreparationFailure failure = exception.failure();
    int status = status(failure.code());
    String correlation = Objects.requireNonNull(state.safeCorrelationOrGenerated());
    telemetry.failed(correlation, failure);
    String codeName = Objects.requireNonNull(failure.code().name());
    ProblemDetails problem =
        new ProblemDetails(
            Objects.requireNonNull(
                URI.create(
                    "urn:ecuador-tax-document-service:problem:"
                        + codeName.toLowerCase(Locale.ROOT))),
            title(status),
            status,
            codeName,
            failure.detail(),
            Objects.requireNonNull(URI.create("/api/v1/invoice-drafts/fiscal-preparation")),
            correlation,
            null);
    return Objects.requireNonNull(
        Response.status(status)
            .type(PROBLEM_JSON)
            .header("X-Correlation-Id", correlation)
            .header("Cache-Control", "no-store")
            .entity(problem)
            .build());
  }

  private static int status(FiscalPreparationFailure.Code code) {
    return switch (code) {
      case COMPANY_CONTEXT_REQUIRED, COMPANY_CONTEXT_INVALID, INVALID_REQUEST -> 400;
      case INVOICE_DRAFT_NOT_FOUND -> 404;
      case INVOICE_DRAFT_NOT_PREPARABLE,
          OFFICIAL_SEQUENCE_BASELINE_MISSING,
          OFFICIAL_SEQUENCE_BASELINE_INVALID,
          OFFICIAL_SEQUENCE_EXHAUSTED ->
          409;
      case EMISSION_DATE_STALE,
          FISCAL_CONTEXT_INVALID,
          FISCAL_CONTEXT_UNSUPPORTED,
          FISCAL_CONTEXT_INCONSISTENT ->
          422;
      case ACCESS_KEY_INVALID, INTERNAL_ERROR -> 500;
      case FISCAL_CONTEXT_UNAVAILABLE, PERSISTENCE_FAILURE, PREPARATION_OUTCOME_UNKNOWN -> 503;
      case REQUEST_TIMEOUT -> 504;
    };
  }

  private static String title(int status) {
    return switch (status) {
      case 400 -> "Invalid request";
      case 404 -> "Invoice Draft not found";
      case 409 -> "Fiscal Preparation conflict";
      case 422 -> "Fiscal Preparation validation failed";
      case 503 -> "Fiscal Preparation unavailable";
      case 504 -> "Request timeout";
      default -> "Internal error";
    };
  }
}
