package com.alexastudillo.taxdocument.application.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceDraft;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CreateInvoiceDraftUseCaseTest {
  @Test
  void applicationNormalizesBuildsTimestampFreeCandidateAndMapsPersistedResult() {
    AtomicReference<InvoiceDraftCandidate> captured = new AtomicReference<>();
    Instant timestamp = Instant.parse("2026-07-17T12:00:01Z");
    InvoiceDraftRepository repository =
        new InvoiceDraftRepository() {
          @Override
          public Uni<IdempotencyLookup> findByIdempotency(
              com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId companyId,
              byte[] keyHash,
              byte[] requestFingerprint,
              Duration remaining) {
            return Uni.createFrom().item(new IdempotencyLookup.Missing());
          }

          @Override
          public Uni<PersistedInvoiceDraft> persist(
              InvoiceDraftCandidate candidate, Duration remaining) {
            captured.set(candidate);
            return Uni.createFrom()
                .item(new PersistedInvoiceDraft(candidate.draft(), timestamp, timestamp));
          }
        };
    CreateInvoiceDraftResult result =
        new CreateInvoiceDraftService(
                repository,
                ApplicationTestFixtures.references(),
                ApplicationTestFixtures.identifiers())
            .create(ApplicationTestFixtures.command())
            .await()
            .indefinitely();

    InvoiceDraft draft = captured.get().draft();
    assertEquals("123e4567-e89b-12d3-a456-426614174000", draft.emissionPointId().toString());
    assertEquals("Café Buyer", draft.buyer().legalName());
    assertEquals("ABC123", draft.buyer().identification());
    assertEquals("SKU1", draft.lines().getFirst().productCode());
    assertEquals("client ref", draft.additionalInformation().getFirst().canonicalName());
    assertEquals(timestamp, result.createdAt());
    assertEquals(timestamp, result.updatedAt());
    assertFalse(result.replayed());
    assertNotNull(captured.get().lineTaxIdentifiers().get(draft.lines().getFirst().id()));
    assertTrue(
        InvoiceDraftCandidate.class.getRecordComponents().length > 0
            && java.util.Arrays.stream(InvoiceDraftCandidate.class.getRecordComponents())
                .noneMatch(component -> component.getName().endsWith("At")));
  }

  @Test
  void invalidEmissionPointFailsBeforeReferenceOrPersistenceWork() {
    java.util.concurrent.atomic.AtomicInteger calls =
        new java.util.concurrent.atomic.AtomicInteger();
    InvoiceDraftRepository repository =
        new InvoiceDraftRepository() {
          @Override
          public Uni<IdempotencyLookup> findByIdempotency(
              com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId companyId,
              byte[] keyHash,
              byte[] requestFingerprint,
              Duration remaining) {
            calls.incrementAndGet();
            return Uni.createFrom().item(new IdempotencyLookup.Missing());
          }

          @Override
          public Uni<PersistedInvoiceDraft> persist(
              InvoiceDraftCandidate candidate, Duration remaining) {
            calls.incrementAndGet();
            return Uni.createFrom().failure(new AssertionError("must not persist"));
          }
        };
    CreateInvoiceDraftCommand valid = ApplicationTestFixtures.command();
    CreateInvoiceDraftCommand invalid =
        new CreateInvoiceDraftCommand(
            valid.companyId(),
            valid.requestCreationInstant(),
            valid.deadline(),
            valid.idempotencyKey(),
            valid.correlationId(),
            "not-a-uuid",
            valid.emissionDate(),
            valid.buyer(),
            valid.lines(),
            valid.payments(),
            valid.additionalInformation());
    Throwable failure =
        assertThrows(
            InvoiceDraftApplicationException.class,
            () ->
                new CreateInvoiceDraftService(
                        repository,
                        ApplicationTestFixtures.references(),
                        ApplicationTestFixtures.identifiers())
                    .create(invalid)
                    .await()
                    .indefinitely());
    assertTrue(failure instanceof InvoiceDraftApplicationException);
    assertEquals(0, calls.get());
  }
}
