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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Bodyless POST adapter for natural Company-plus-Invoice-Draft fiscal
 * preparation.
 */
@Path("/api/v1/invoice-drafts")
@Produces(MediaType.APPLICATION_JSON)
@NullMarked
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
    public Uni<@NonNull Response> prepare(@PathParam("invoiceDraftId") String invoiceDraftId) {
        PrepareInvoiceForFiscalIssuanceCommand command = requestBoundary.accept(invoiceDraftId, routing, state);
        Uni<@NonNull PrepareInvoiceForFiscalIssuanceResult> application = Objects.requireNonNull(
                useCase
                        .prepare(command)
                        .onFailure(
                                error -> error != null && !(error instanceof FiscalPreparationApplicationException))
                        .transform(
                                error -> new FiscalPreparationApplicationException(
                                        FiscalPreparationFailure.of(
                                                FiscalPreparationFailure.Code.INTERNAL_ERROR),
                                        Objects.requireNonNull(error))));
        return requireUni(
                deadlineHandler
                        .race(application, state)
                        .onItem()
                        .transform(res -> successResponse(Objects.requireNonNull(res))),
                "fiscal preparation response");
    }

    private Response successResponse(PrepareInvoiceForFiscalIssuanceResult result) {
        int status = result.replayed() ? 200 : 201;
        telemetry.completed(state.correlationId(), result.replayed());
        return Objects.requireNonNull(
                Response.status(status)
                        .header("Fiscal-Preparation-Replayed", result.replayed())
                        .header("X-Correlation-Id", state.correlationId())
                        .header("Cache-Control", "no-store")
                        .entity(mapper.toResponse(result.preparation()))
                        .build());
    }

    private static <T extends @NonNull Object> Uni<@NonNull T> requireUni(
            @Nullable Uni<@NonNull T> value, String field) {
        return Objects.requireNonNull(value, field);
    }
}
