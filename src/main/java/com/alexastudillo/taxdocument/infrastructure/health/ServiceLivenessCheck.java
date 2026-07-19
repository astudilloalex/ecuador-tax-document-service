package com.alexastudillo.taxdocument.infrastructure.health;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Objects;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.jspecify.annotations.NullMarked;

/** Process-only service liveness with no datastore or external dependency. */
@Liveness
@ApplicationScoped
@NullMarked
public final class ServiceLivenessCheck implements HealthCheck {
  @Override
  public HealthCheckResponse call() {
    return Objects.requireNonNull(HealthCheckResponse.up("service-process"));
  }
}
