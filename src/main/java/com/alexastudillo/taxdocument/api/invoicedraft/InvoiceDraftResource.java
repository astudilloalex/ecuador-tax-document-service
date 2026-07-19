package com.alexastudillo.taxdocument.api.invoicedraft;

import com.alexastudillo.taxdocument.api.invoicedraft.telemetry.InvoiceDraftTelemetryPort;
import com.alexastudillo.taxdocument.api.problem.ProblemDetails;
import com.alexastudillo.taxdocument.application.invoicedraft.CreateInvoiceDraftResult;
import com.alexastudillo.taxdocument.application.invoicedraft.CreateInvoiceDraftUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** POST /api/v1/invoice-drafts transport adapter. */
@NullMarked
@Path("/api/v1/invoice-drafts")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@InvoiceDraftRequestGate
public final class InvoiceDraftResource {
  private final CreateInvoiceDraftUseCase useCase;
  private final InvoiceDraftRequestDeadlineHandler deadlineHandler;
  private final InvoiceDraftRequestState state;
  private final InvoiceDraftRequestPropertyClassifier classifier;
  private final InvoiceDraftApiMapper mapper;
  private final ObjectMapper objectMapper;
  private final InvoiceDraftTelemetryPort telemetry;

  public InvoiceDraftResource(
      CreateInvoiceDraftUseCase useCase,
      InvoiceDraftRequestDeadlineHandler deadlineHandler,
      InvoiceDraftRequestState state,
      InvoiceDraftRequestPropertyClassifier classifier,
      InvoiceDraftApiMapper mapper,
      ObjectMapper objectMapper,
      InvoiceDraftTelemetryPort telemetry) {
    this.useCase = useCase;
    this.deadlineHandler = deadlineHandler;
    this.state = state;
    this.classifier = classifier;
    this.mapper = mapper;
    this.objectMapper = objectMapper;
    this.telemetry = telemetry;
  }

  @POST
  public Uni<Response> create(JsonNode decodedRequest) {
    Uni<CreateInvoiceDraftResult> application =
        Objects.requireNonNull(
            Uni.createFrom()
                .item(() -> bind(decodedRequest))
                .onItem()
                .transform(mapperRequest -> mapper.toCommand(mapperRequest, state))
                .onItem()
                .transformToUni(useCase::create));
    return Objects.requireNonNull(
        deadlineHandler
            .race(application, state)
            .onItem()
            .transform(
                result -> {
                  int status = result.replayed() ? 200 : 201;
                  state.acceptedStatus(status);
                  telemetry.completed(state, result, status);
                  return Objects.requireNonNull(
                      Response.status(status)
                          .header("X-Correlation-Id", state.correlationId())
                          .header("Idempotency-Replayed", String.valueOf(result.replayed()))
                          .entity(mapper.toResponse(result))
                          .build());
                }));
  }

  private CreateInvoiceDraftRequest bind(JsonNode request) {
    if (classifier.containsCalculatedProperty(request)) {
      throw new ProblemDetails.ApiException(
          422,
          "PROHIBITED_CALCULATED_FIELD",
          "Calculated properties must not be supplied by the client");
    }
    validateRepresentation(request);
    try {
      return Objects.requireNonNull(
          objectMapper.treeToValue(request, CreateInvoiceDraftRequest.class));
    } catch (JsonProcessingException exception) {
      throw new ProblemDetails.ApiException(
          400, "INVALID_REQUEST", "The request representation is invalid");
    }
  }

  private static void validateRepresentation(JsonNode request) {
    requireObject(request);
    requireText(request, "emissionPointId");
    requireText(request, "emissionDate");

    JsonNode buyer = requireObject(request.get("buyer"));
    requireText(buyer, "identificationType");
    requireText(buyer, "identification");
    requireText(buyer, "legalName");
    requireOptionalText(buyer, "address");
    requireOptionalText(buyer, "email");
    requireOptionalText(buyer, "telephone");

    JsonNode lines = requireArray(request.get("lines"));
    for (JsonNode line : lines) {
      JsonNode value = requireObject(line);
      requireText(value, "productCode");
      requireText(value, "description");
      requireText(value, "quantity");
      requireText(value, "unitPrice");
      requireText(value, "discount");
      requireText(value, "taxRuleId");
    }

    JsonNode payments = requireArray(request.get("payments"));
    for (JsonNode payment : payments) {
      JsonNode value = requireObject(payment);
      requireText(value, "paymentMethodId");
      requireText(value, "amount");
    }

    JsonNode additional = request.get("additionalInformation");
    if (additional != null) {
      for (JsonNode entry : requireArray(additional)) {
        JsonNode value = requireObject(entry);
        requireText(value, "name");
        requireText(value, "value");
      }
    }
  }

  private static JsonNode requireObject(@Nullable JsonNode value) {
    if (value != null && value.isObject()) {
      return value;
    }
    throw invalidRequest();
  }

  private static JsonNode requireArray(@Nullable JsonNode value) {
    if (value != null && value.isArray()) {
      return value;
    }
    throw invalidRequest();
  }

  private static void requireText(JsonNode object, String property) {
    JsonNode value = object.get(property);
    if (value == null || !value.isTextual()) {
      throw invalidRequest();
    }
  }

  private static void requireOptionalText(JsonNode object, String property) {
    JsonNode value = object.get(property);
    if (value != null && !value.isTextual()) {
      throw invalidRequest();
    }
  }

  private static ProblemDetails.ApiException invalidRequest() {
    return new ProblemDetails.ApiException(
        400, "INVALID_REQUEST", "The request representation is invalid");
  }
}
