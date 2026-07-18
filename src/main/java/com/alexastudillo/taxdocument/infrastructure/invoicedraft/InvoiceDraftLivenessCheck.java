package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/** Process-only liveness with no datastore or external dependency. */
@Liveness
@ApplicationScoped
public final class InvoiceDraftLivenessCheck implements HealthCheck {
  @Override
  public HealthCheckResponse call() {
    return HealthCheckResponse.up("invoice-draft-process");
  }
}
