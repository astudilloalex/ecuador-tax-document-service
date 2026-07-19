package com.alexastudillo.taxdocument.infrastructure.health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.infrastructure.invoicedraft.PostgreSqlTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@QuarkusTest
@NullMarked
class ServiceHealthTest {
  @Inject PostgreSqlTestResource database;
  @Inject @Readiness ServiceReadinessCheck readiness;

  @Test
  void livenessIsProcessOnlyAndReadinessUsesOnlyTheMigratedPostgreSqlV6Destination() {
    HealthCheckResponse liveness = new ServiceLivenessCheck().call();
    assertEquals(HealthCheckResponse.Status.UP, liveness.getStatus());
    assertEquals("service-process", liveness.getName());

    database.resetSchema();
    long baselineRows = database.rowCount("official_sequence_baseline");
    long preparationRows = database.rowCount("fiscal_preparation");
    HealthCheckResponse response = readiness.call().await().indefinitely();
    assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
    assertEquals("service-readiness", response.getName());
    assertTrue(
        database.appliedMigrations().stream()
            .anyMatch(info -> info.getVersion().getVersion().equals("6")));
    assertEquals(baselineRows, database.rowCount("official_sequence_baseline"));
    assertEquals(preparationRows, database.rowCount("fiscal_preparation"));
  }
}
