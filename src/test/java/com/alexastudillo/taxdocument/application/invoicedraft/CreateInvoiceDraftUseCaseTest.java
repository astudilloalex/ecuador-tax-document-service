package com.alexastudillo.taxdocument.application.invoicedraft;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.domain.invoicedraft.AdditionalInformation;
import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceDraft;
import com.alexastudillo.taxdocument.domain.invoicedraft.InvoiceLine;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@NullMarked
class CreateInvoiceDraftUseCaseTest {
  @Test
  void applicationNormalizesBuildsTimestampFreeCandidateAndMapsPersistedResult() {
    AtomicReference<@NonNull InvoiceDraftCandidate> captured = new AtomicReference<>();
    Instant timestamp = requireNonNull(Instant.parse("2026-07-17T12:00:01Z"));
    InvoiceDraftRepository repository =
        new InvoiceDraftRepository() {
          @Override
          public Uni<InvoiceDraftRepository.@NonNull IdempotencyLookup> findByIdempotency(
              com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId companyId,
              byte[] keyHash,
              byte[] requestFingerprint,
              Duration remaining) {
            return uniItem(new IdempotencyLookup.Missing());
          }

          @Override
          public Uni<@NonNull PersistedInvoiceDraft> persist(
              InvoiceDraftCandidate candidate, Duration remaining) {
            captured.set(candidate);
            return uniItem(
                new PersistedInvoiceDraft(requireNonNull(candidate.draft()), timestamp, timestamp));
          }
        };
    @Nullable CreateInvoiceDraftResult nullableResult =
        new CreateInvoiceDraftService(
                repository,
                ApplicationTestFixtures.references(),
                ApplicationTestFixtures.identifiers())
            .create(ApplicationTestFixtures.command())
            .await()
            .indefinitely();
    CreateInvoiceDraftResult result = requireNonNull(nullableResult, "create-draft result");

    @Nullable InvoiceDraftCandidate nullableCandidate = captured.get();
    InvoiceDraftCandidate capturedCandidate =
        requireNonNull(nullableCandidate, "captured invoice-draft candidate");
    InvoiceDraft draft = capturedCandidate.draft();
    @Nullable InvoiceLine nullableLine = draft.lines().getFirst();
    InvoiceLine firstLine = requireNonNull(nullableLine, "first invoice line");
    @Nullable AdditionalInformation nullableAdditionalInformation =
        draft.additionalInformation().getFirst();
    AdditionalInformation firstAdditionalInformation =
        requireNonNull(nullableAdditionalInformation, "first additional-information entry");
    assertEquals("123e4567-e89b-12d3-a456-426614174000", draft.emissionPointId().toString());
    assertEquals("Café Buyer", draft.buyer().legalName());
    assertEquals("ABC123", draft.buyer().identification());
    assertEquals("SKU1", firstLine.productCode());
    assertEquals("client ref", firstAdditionalInformation.canonicalName());
    assertEquals(timestamp, result.createdAt());
    assertEquals(timestamp, result.updatedAt());
    assertFalse(result.replayed());
    assertNotNull(capturedCandidate.lineTaxIdentifiers().get(firstLine.id()));
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
          public Uni<InvoiceDraftRepository.@NonNull IdempotencyLookup> findByIdempotency(
              com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId companyId,
              byte[] keyHash,
              byte[] requestFingerprint,
              Duration remaining) {
            calls.incrementAndGet();
            return uniItem(new IdempotencyLookup.Missing());
          }

          @Override
          public Uni<@NonNull PersistedInvoiceDraft> persist(
              InvoiceDraftCandidate candidate, Duration remaining) {
            calls.incrementAndGet();
            return requireNonNull(Uni.createFrom().failure(new AssertionError("must not persist")));
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

  private static <T extends @NonNull Object> Uni<@NonNull T> uniItem(T value) {
    @Nullable Uni<@NonNull T> nullable = Uni.createFrom().item(value);
    return requireNonNull(nullable, "Uni item");
  }
}
