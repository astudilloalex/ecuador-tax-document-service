package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.junit.jupiter.api.Test;

@QuarkusTest
class InvoiceDraftHealthTest {
  @Inject PostgreSqlTestResource database;
  @Inject @Readiness InvoiceDraftReadinessCheck readiness;

  @Test
  void livenessIsProcessOnlyAndReadinessUsesMigratedCatalogDatasource() {
    assertTrue(new InvoiceDraftLivenessCheck().call().getStatus() == HealthCheckResponse.Status.UP);
    database.resetSchema();
    HealthCheckResponse response = readiness.call().await().indefinitely();
    assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
  }
}
