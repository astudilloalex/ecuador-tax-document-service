package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalContextPort;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalContextResolution;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationApplicationException;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationFailure;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalDesignation;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalSourceEvidence;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** One-attempt, bounded and redacted authoritative fiscal-context adapter. */
@ApplicationScoped
@NullMarked
public final class FiscalContextHttpAdapter implements FiscalContextPort {
  private static final Duration RESPONSE_CEILING =
      Objects.requireNonNull(Duration.ofSeconds(2), "response ceiling");
  private final AuthoritativeFiscalContextClient client;

  public FiscalContextHttpAdapter(@RestClient AuthoritativeFiscalContextClient client) {
    this.client = Objects.requireNonNull(client, "client");
  }

  @Override
  public Uni<FiscalContextResolution> resolve(Request request) {
    Objects.requireNonNull(request, "request");
    Duration timeout =
        request.remaining().compareTo(RESPONSE_CEILING) < 0
            ? request.remaining()
            : RESPONSE_CEILING;
    AuthoritativeFiscalContextDto.Selection selection =
        new AuthoritativeFiscalContextDto.Selection(
            request.emissionPointId(), request.emissionDate(), request.documentTypeCode());
    return client
        .resolve(request.companyId().toString(), request.safeCorrelationId(), selection)
        .ifNoItem()
        .after(timeout)
        .fail()
        .onItem()
        .transform(context -> map(Objects.requireNonNull(context, "authoritative fiscal context")))
        .onFailure()
        .transform(failure -> classify(Objects.requireNonNull(failure, "provider failure")));
  }

  private static FiscalContextResolution map(AuthoritativeFiscalContextDto.Context context) {
    try {
      AuthoritativeFiscalContextDto.SourceEvidence source =
          Objects.requireNonNull(context.sourceEvidence(), "sourceEvidence");
      return new FiscalContextResolution(
          Objects.requireNonNull(context.issuerReference(), "issuerReference"),
          Objects.requireNonNull(context.issuerRuc(), "issuerRuc"),
          Objects.requireNonNull(context.legalName(), "legalName"),
          optional(context.commercialName()),
          Objects.requireNonNull(context.headOfficeAddress(), "headOfficeAddress"),
          Objects.requireNonNull(context.accountingRequired(), "accountingRequired"),
          specialTaxpayer(context.specialTaxpayer()),
          withholdingAgent(context.withholdingAgent()),
          FiscalDesignation.RimpeClassification.valueOf(
              Objects.requireNonNull(context.rimpeClassification(), "rimpeClassification")),
          largeContributor(context.largeContributor()),
          Objects.requireNonNull(context.establishmentReference(), "establishmentReference"),
          Objects.requireNonNull(context.establishmentCode(), "establishmentCode"),
          Objects.requireNonNull(context.establishmentAddress(), "establishmentAddress"),
          Objects.requireNonNull(context.emissionPointId(), "emissionPointId"),
          Objects.requireNonNull(context.emissionPointCode(), "emissionPointCode"),
          Objects.requireNonNull(context.environmentCode(), "environmentCode"),
          Objects.requireNonNull(context.documentTypeCode(), "documentTypeCode"),
          Objects.requireNonNull(context.emissionTypeCode(), "emissionTypeCode"),
          Objects.requireNonNull(context.invoiceIssuanceEligible(), "invoiceIssuanceEligible"),
          new FiscalSourceEvidence(
              Objects.requireNonNull(source.authority(), "sourceAuthority"),
              Objects.requireNonNull(source.revision(), "sourceRevision"),
              Objects.requireNonNull(source.effectiveFrom(), "effectiveFrom"),
              optional(source.effectiveThrough()),
              Objects.requireNonNull(source.observedAt(), "observedAt")));
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw failure(FiscalPreparationFailure.Code.FISCAL_CONTEXT_INVALID);
    }
  }

  private static Optional<FiscalDesignation.SpecialTaxpayer> specialTaxpayer(
      AuthoritativeFiscalContextDto.@Nullable ResolutionDesignation designation) {
    if (designation == null) {
      return Objects.requireNonNull(Optional.empty(), "empty special taxpayer");
    }
    return Objects.requireNonNull(
        Optional.of(
            new FiscalDesignation.SpecialTaxpayer(
                Objects.requireNonNull(
                    designation.resolutionIdentifier(), "resolutionIdentifier"))),
        "special taxpayer");
  }

  private static Optional<FiscalDesignation.WithholdingAgent> withholdingAgent(
      AuthoritativeFiscalContextDto.@Nullable ResolutionDesignation designation) {
    if (designation == null) {
      return Objects.requireNonNull(Optional.empty(), "empty withholding agent");
    }
    return Objects.requireNonNull(
        Optional.of(
            new FiscalDesignation.WithholdingAgent(
                Objects.requireNonNull(
                    designation.resolutionIdentifier(), "resolutionIdentifier"))),
        "withholding agent");
  }

  private static Optional<FiscalDesignation.LargeContributor> largeContributor(
      AuthoritativeFiscalContextDto.@Nullable LargeContributorDesignation designation) {
    if (designation == null) {
      return Objects.requireNonNull(Optional.empty(), "empty large contributor");
    }
    return Objects.requireNonNull(
        Optional.of(
            new FiscalDesignation.LargeContributor(
                Objects.requireNonNull(designation.resolutionIdentifier(), "resolutionIdentifier"),
                Objects.requireNonNull(designation.requiredLegend(), "requiredLegend"))),
        "large contributor");
  }

  private static <T> Optional<T> optional(@Nullable T value) {
    return value == null
        ? Objects.requireNonNull(Optional.empty(), "empty optional")
        : Objects.requireNonNull(Optional.of(value), "present optional");
  }

  private static Throwable classify(Throwable throwable) {
    if (throwable instanceof FiscalPreparationApplicationException) {
      return throwable;
    }
    if (throwable instanceof AuthoritativeFiscalContextClient.ProviderFailure providerFailure) {
      return failure(providerFailureCode(providerFailure));
    }
    for (Throwable current = throwable; current != null; current = current.getCause()) {
      String typeName = current.getClass().getName();
      if (current instanceof TimeoutException
          || typeName.contains("Timeout")
          || typeName.contains("ConnectException")) {
        return failure(FiscalPreparationFailure.Code.FISCAL_CONTEXT_UNAVAILABLE);
      }
    }
    return failure(FiscalPreparationFailure.Code.FISCAL_CONTEXT_INVALID);
  }

  private static FiscalPreparationFailure.Code providerFailureCode(
      AuthoritativeFiscalContextClient.ProviderFailure failure) {
    String code = failure.safeCode();
    if ("FISCAL_CONTEXT_UNSUPPORTED".equals(code)) {
      return FiscalPreparationFailure.Code.FISCAL_CONTEXT_UNSUPPORTED;
    }
    if ("FISCAL_CONTEXT_AMBIGUOUS".equals(code)
        || "FISCAL_CONTEXT_INCONSISTENT".equals(code)
        || failure.status() == 409) {
      return FiscalPreparationFailure.Code.FISCAL_CONTEXT_INCONSISTENT;
    }
    if ("PROVIDER_UNAVAILABLE".equals(code)
        || "PROVIDER_TIMEOUT".equals(code)
        || failure.status() == 503
        || failure.status() == 504) {
      return FiscalPreparationFailure.Code.FISCAL_CONTEXT_UNAVAILABLE;
    }
    return FiscalPreparationFailure.Code.FISCAL_CONTEXT_INVALID;
  }

  private static FiscalPreparationApplicationException failure(FiscalPreparationFailure.Code code) {
    return new FiscalPreparationApplicationException(FiscalPreparationFailure.of(code));
  }
}
