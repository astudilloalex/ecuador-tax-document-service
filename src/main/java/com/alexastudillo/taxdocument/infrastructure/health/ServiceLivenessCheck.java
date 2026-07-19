package com.alexastudillo.taxdocument.infrastructure.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/** Process-only service liveness with no datastore or external dependency. */
@Liveness
@ApplicationScoped
public final class ServiceLivenessCheck implements HealthCheck {
  @Override
  public HealthCheckResponse call() {
    return HealthCheckResponse.up("service-process");
  }
}
