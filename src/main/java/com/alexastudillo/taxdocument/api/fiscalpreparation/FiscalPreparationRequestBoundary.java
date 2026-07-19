package com.alexastudillo.taxdocument.api.fiscalpreparation;

import com.alexastudillo.taxdocument.api.problem.ProblemDetails;
import com.alexastudillo.taxdocument.api.requestcontext.CompanyContextHeader;
import com.alexastudillo.taxdocument.api.requestcontext.CorrelationHeader;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationApplicationException;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationCommitTracker;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationFailure;
import com.alexastudillo.taxdocument.application.fiscalpreparation.PrepareInvoiceForFiscalIssuanceCommand;
import com.alexastudillo.taxdocument.application.requestcontext.RequestClock;
import com.alexastudillo.taxdocument.application.requestcontext.RequestContext;
import com.alexastudillo.taxdocument.application.requestcontext.RequestDeadline;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jspecify.annotations.NullMarked;

/** Validates the bodyless Company-scoped request before any Company-owned application read. */
@NullMarked
@ApplicationScoped
public final class FiscalPreparationRequestBoundary {
  private static final Pattern UUID_TEXT =
      Objects.requireNonNull(
          Pattern.compile(
              "^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$"));
  private final RequestClock clock;
  private final CompanyContextHeader companyHeader;
  private final CorrelationHeader correlationHeader;
  private final Duration requestDeadline;

  public FiscalPreparationRequestBoundary(
      RequestClock clock, CompanyContextHeader companyHeader, CorrelationHeader correlationHeader) {
    this.clock = Objects.requireNonNull(clock, "clock");
    this.companyHeader = Objects.requireNonNull(companyHeader, "companyHeader");
    this.correlationHeader = Objects.requireNonNull(correlationHeader, "correlationHeader");
    this.requestDeadline =
        Objects.requireNonNull(
            ConfigProvider.getConfig()
                .getValue("fiscal-preparation.request-deadline", Duration.class));
  }

  public PrepareInvoiceForFiscalIssuanceCommand accept(
      String invoiceDraftId, RoutingContext routing, FiscalPreparationRequestState state) {
    Objects.requireNonNull(invoiceDraftId, "invoiceDraftId");
    Objects.requireNonNull(routing, "routing");
    Objects.requireNonNull(state, "state");
    Instant requestInstant = clock.requestTime();
    RequestContext context =
        new RequestContext(
            requestInstant,
            Objects.requireNonNull(
                requestInstant.atZone(RequestContext.ECUADOR_TIME_ZONE).toLocalDate()),
            RequestDeadline.start(requestDeadline));
    List<String> correlationHeaders =
        Objects.requireNonNull(routing.request().headers().getAll("X-Correlation-Id"));
    CorrelationHeader.Classification correlation = correlationHeader.classify(correlationHeaders);
    FiscalPreparationCommitTracker commitTracker = new FiscalPreparationCommitTracker();
    state.initialize(context, Objects.requireNonNull(correlation.safeValue()), commitTracker);
    List<String> companyHeaders =
        Objects.requireNonNull(routing.request().headers().getAll("X-Company-Id"));
    CompanyId companyId = parseCompany(companyHeaders);
    rejectProhibitedShape(routing);
    UUID draftId = parseDraftId(invoiceDraftId);
    return new PrepareInvoiceForFiscalIssuanceCommand(
        companyId, draftId, correlation.safeValue(), context, commitTracker);
  }

  private CompanyId parseCompany(List<String> values) {
    try {
      return companyHeader.parse(values);
    } catch (ProblemDetails.ApiException exception) {
      FiscalPreparationFailure.Code code =
          "COMPANY_CONTEXT_REQUIRED".equals(exception.code())
              ? FiscalPreparationFailure.Code.COMPANY_CONTEXT_REQUIRED
              : FiscalPreparationFailure.Code.COMPANY_CONTEXT_INVALID;
      throw failure(code);
    }
  }

  private static void rejectProhibitedShape(RoutingContext routing) {
    if (!routing.request().headers().getAll("Idempotency-Key").isEmpty()) {
      throw failure(FiscalPreparationFailure.Code.INVALID_REQUEST);
    }
    String length = routing.request().getHeader("Content-Length");
    if ((length != null && !"0".equals(length))
        || routing.request().getHeader("Transfer-Encoding") != null) {
      throw failure(FiscalPreparationFailure.Code.INVALID_REQUEST);
    }
  }

  private static UUID parseDraftId(String value) {
    if (!UUID_TEXT.matcher(value).matches()) {
      throw failure(FiscalPreparationFailure.Code.INVALID_REQUEST);
    }
    try {
      UUID result = UUID.fromString(value);
      if (result.equals(new UUID(0L, 0L))) {
        throw failure(FiscalPreparationFailure.Code.INVALID_REQUEST);
      }
      return result;
    } catch (IllegalArgumentException exception) {
      throw failure(FiscalPreparationFailure.Code.INVALID_REQUEST);
    }
  }

  private static FiscalPreparationApplicationException failure(FiscalPreparationFailure.Code code) {
    return new FiscalPreparationApplicationException(
        FiscalPreparationFailure.of(Objects.requireNonNull(code)));
  }
}
