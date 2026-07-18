package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import io.smallrye.health.api.AsyncHealthCheck;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/** Bounded same-datasource migration and reference-baseline readiness. */
@Readiness
@ApplicationScoped
public final class InvoiceDraftReadinessCheck implements AsyncHealthCheck {
  private static final String READINESS_SQL =
      "SELECT (SELECT count(*) = 5 FROM buyer_identification_type_catalog) "
          + "AND (SELECT count(*) = 6 FROM iva_tax_rule_catalog) "
          + "AND (SELECT count(*) = 8 FROM payment_method_catalog) "
          + "AND EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '5' AND success) AS ready";
  private final Pool pool;

  public InvoiceDraftReadinessCheck(Pool pool) {
    this.pool = pool;
  }

  @Override
  public Uni<HealthCheckResponse> call() {
    return pool.query(READINESS_SQL)
        .execute()
        .ifNoItem()
        .after(java.time.Duration.ofSeconds(2))
        .fail()
        .onItem()
        .transform(
            rows ->
                rows.iterator().next().getBoolean("ready")
                    ? HealthCheckResponse.up("invoice-draft-readiness")
                    : HealthCheckResponse.down("invoice-draft-readiness"))
        .onFailure()
        .recoverWithItem(() -> HealthCheckResponse.down("invoice-draft-readiness"));
  }
}
