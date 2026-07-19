package com.alexastudillo.taxdocument.api.fiscalpreparation;

import com.alexastudillo.taxdocument.api.fiscalpreparation.telemetry.FiscalPreparationTelemetryPort;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationApplicationException;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationFailure;
import com.alexastudillo.taxdocument.application.fiscalpreparation.PrepareInvoiceForFiscalIssuanceCommand;
import com.alexastudillo.taxdocument.application.fiscalpreparation.PrepareInvoiceForFiscalIssuanceResult;
import com.alexastudillo.taxdocument.application.fiscalpreparation.PrepareInvoiceForFiscalIssuanceUseCase;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Objects;

/** Bodyless POST adapter for natural Company-plus-Invoice-Draft fiscal preparation. */
@Path("/api/v1/invoice-drafts")
@Produces(MediaType.APPLICATION_JSON)
public final class FiscalPreparationResource {
  private final PrepareInvoiceForFiscalIssuanceUseCase useCase;
  private final FiscalPreparationRequestBoundary requestBoundary;
  private final FiscalPreparationRequestDeadlineHandler deadlineHandler;
  private final FiscalPreparationRequestState state;
  private final FiscalPreparationApiMapper mapper;
  private final FiscalPreparationTelemetryPort telemetry;
  private final RoutingContext routing;

  public FiscalPreparationResource(
      PrepareInvoiceForFiscalIssuanceUseCase useCase,
      FiscalPreparationRequestBoundary requestBoundary,
      FiscalPreparationRequestDeadlineHandler deadlineHandler,
      FiscalPreparationRequestState state,
      FiscalPreparationApiMapper mapper,
      FiscalPreparationTelemetryPort telemetry,
      RoutingContext routing) {
    this.useCase = Objects.requireNonNull(useCase, "useCase");
    this.requestBoundary = Objects.requireNonNull(requestBoundary, "requestBoundary");
    this.deadlineHandler = Objects.requireNonNull(deadlineHandler, "deadlineHandler");
    this.state = Objects.requireNonNull(state, "state");
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
    this.routing = Objects.requireNonNull(routing, "routing");
  }

  @POST
  @Path("/{invoiceDraftId}/fiscal-preparation")
  public Uni<Response> prepare(@PathParam("invoiceDraftId") String invoiceDraftId) {
    PrepareInvoiceForFiscalIssuanceCommand command =
        requestBoundary.accept(invoiceDraftId, routing, state);
    Uni<PrepareInvoiceForFiscalIssuanceResult> application =
        useCase
            .prepare(command)
            .onFailure(error -> !(error instanceof FiscalPreparationApplicationException))
            .transform(
                error ->
                    new FiscalPreparationApplicationException(
                        FiscalPreparationFailure.of(FiscalPreparationFailure.Code.INTERNAL_ERROR),
                        error));
    return deadlineHandler.race(application, state).onItem().transform(res -> successResponse(res));
  }

  private Response successResponse(PrepareInvoiceForFiscalIssuanceResult result) {
    int status = result.replayed() ? 200 : 201;
    telemetry.completed(state.correlationId(), result.replayed());
    return Response.status(status)
        .header("Fiscal-Preparation-Replayed", result.replayed())
        .header("X-Correlation-Id", state.correlationId())
        .header("Cache-Control", "no-store")
        .entity(mapper.toResponse(result.preparation()))
        .build();
  }
}
