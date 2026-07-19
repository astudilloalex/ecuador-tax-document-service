package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jspecify.annotations.Nullable;

/** MicroProfile consumer of authoritative-fiscal-context contract 1.0.0. */
@RegisterRestClient(configKey = "authoritative-fiscal-context")
@Path("/fiscal-context-resolutions/invoice-issuance")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface AuthoritativeFiscalContextClient {
  Set<String> SAFE_PROVIDER_CODES =
      Set.of(
          "FISCAL_CONTEXT_NOT_FOUND",
          "FISCAL_CONTEXT_INEFFECTIVE",
          "FISCAL_CONTEXT_INELIGIBLE",
          "FISCAL_CONTEXT_AMBIGUOUS",
          "FISCAL_CONTEXT_INVALID",
          "FISCAL_CONTEXT_UNSUPPORTED",
          "FISCAL_CONTEXT_INCONSISTENT",
          "PROVIDER_UNAVAILABLE",
          "PROVIDER_TIMEOUT");

  @POST
  Uni<AuthoritativeFiscalContextDto.Context> resolve(
      @HeaderParam("X-Company-Id") String companyId,
      @HeaderParam("X-Correlation-Id") String safeCorrelationId,
      AuthoritativeFiscalContextDto.Selection selection);

  @ClientExceptionMapper
  static RuntimeException mapFailure(Response response) {
    int status = response.getStatus();
    @Nullable String code = null;
    try {
      AuthoritativeFiscalContextDto.ProviderProblem problem =
          response.readEntity(AuthoritativeFiscalContextDto.ProviderProblem.class);
      if (problem != null && SAFE_PROVIDER_CODES.contains(problem.code())) {
        code = problem.code();
      }
    } catch (RuntimeException ignored) {
      // The raw provider response is deliberately discarded and classified only by status.
    }
    return new ProviderFailure(status, code);
  }

  final class ProviderFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int status;
    private final @Nullable String safeCode;

    ProviderFailure(int status, @Nullable String safeCode) {
      super("Authoritative fiscal context provider rejected the selection");
      this.status = status;
      this.safeCode = safeCode;
    }

    int status() {
      return status;
    }

    @Nullable String safeCode() {
      return safeCode;
    }
  }
}
