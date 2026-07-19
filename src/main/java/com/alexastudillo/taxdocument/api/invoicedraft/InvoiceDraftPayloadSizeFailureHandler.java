package com.alexastudillo.taxdocument.api.invoicedraft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.vertx.http.runtime.RouteConstants;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.net.URI;

/** Exclusive API construction of the payload-too-large outcome. */
@ApplicationScoped
public final class InvoiceDraftPayloadSizeFailureHandler {
  private static final String PATH = "/api/v1/invoice-drafts";
  private static final String PROBLEM_JSON = "application/problem+json";
  private static final long MAX_BODY_SIZE = 2_097_152L;

  private final InvoiceDraftRequestBoundary requestBoundary;
  private final ObjectMapper objectMapper;

  public InvoiceDraftPayloadSizeFailureHandler(
      InvoiceDraftRequestBoundary requestBoundary, ObjectMapper objectMapper) {
    this.requestBoundary = requestBoundary;
    this.objectMapper = objectMapper;
  }

  void install(@Observes Router router) {
    router
        .route(HttpMethod.POST, PATH)
        .order(RouteConstants.ROUTE_ORDER_UPLOAD_LIMIT - 1)
        .handler(this::rejectKnownOversizeBody)
        .failureHandler(this::mapStreamingBodyFailure);
  }

  public ProblemDetails.ApiException payloadTooLarge() {
    return new ProblemDetails.ApiException(
        413, "REQUEST_PAYLOAD_TOO_LARGE", "The request body exceeds 2 MiB");
  }

  private void rejectKnownOversizeBody(RoutingContext context) {
    String contentLength = context.request().getHeader(HttpHeaders.CONTENT_LENGTH);
    if (contentLength != null && exceedsLimit(contentLength)) {
      writePayloadTooLarge(context);
      return;
    }
    context.next();
  }

  private void mapStreamingBodyFailure(RoutingContext context) {
    if (context.statusCode() == 413) {
      writePayloadTooLarge(context);
      return;
    }
    context.next();
  }

  private static boolean exceedsLimit(String contentLength) {
    try {
      return Long.parseLong(contentLength) > MAX_BODY_SIZE;
    } catch (NumberFormatException ignored) {
      return false;
    }
  }

  private void writePayloadTooLarge(RoutingContext context) {
    if (context.response().ended()) {
      return;
    }
    InvoiceDraftRequestBoundary.BoundaryState boundary = requestBoundary.state(context);
    if (!boundary.acceptTerminal()) {
      return;
    }
    CorrelationHeader.Classification correlation = boundary.correlation();
    ProblemDetails problem =
        new ProblemDetails(
            URI.create("urn:ecuador-tax-document-service:problem:request_payload_too_large"),
            "Payload too large",
            413,
            "REQUEST_PAYLOAD_TOO_LARGE",
            "The request body exceeds 2 MiB",
            URI.create(PATH),
            correlation.safeValue(),
            null);
    try {
      Buffer body = Buffer.buffer(objectMapper.writeValueAsBytes(problem));
      context
          .response()
          .setStatusCode(413)
          .putHeader(HttpHeaders.CONTENT_TYPE, PROBLEM_JSON)
          .putHeader(HttpHeaders.CONNECTION, "close")
          .putHeader("X-Correlation-Id", correlation.safeValue())
          .end(body)
          .onComplete(ignored -> context.request().connection().close());
    } catch (JsonProcessingException exception) {
      context.fail(exception);
    }
  }
}
