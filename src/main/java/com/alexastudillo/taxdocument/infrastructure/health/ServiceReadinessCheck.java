package com.alexastudillo.taxdocument.infrastructure.health;

import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.Objects;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/** Bounded read-only PostgreSQL/Flyway readiness with no provider or business-row probe. */
@Readiness
@ApplicationScoped
public final class ServiceReadinessCheck implements AsyncHealthCheck {
  private static final String READINESS_SQL =
      "SELECT (SELECT count(*) = 5 FROM buyer_identification_type_catalog) "
          + "AND (SELECT count(*) = 6 FROM iva_tax_rule_catalog) "
          + "AND (SELECT count(*) = 8 FROM payment_method_catalog) "
          + "AND EXISTS (SELECT 1 FROM flyway_schema_history "
          + "WHERE version = '6' AND success) AS ready";
  private final Pool pool;

  public ServiceReadinessCheck(Pool pool) {
    this.pool = Objects.requireNonNull(pool, "pool");
  }

  @Override
  public Uni<HealthCheckResponse> call() {
    return pool.query(READINESS_SQL)
        .execute()
        .ifNoItem()
        .after(Duration.ofSeconds(2))
        .fail()
        .onItem()
        .transform(
            rows ->
                Objects.requireNonNull(rows.iterator().next().getBoolean("ready"), "ready")
                    ? HealthCheckResponse.up("service-readiness")
                    : HealthCheckResponse.down("service-readiness"))
        .onFailure()
        .recoverWithItem(() -> HealthCheckResponse.down("service-readiness"));
  }
}
