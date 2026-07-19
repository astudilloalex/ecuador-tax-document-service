package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import com.alexastudillo.taxdocument.application.invoicedraft.ApplicationTestFixtures;
import com.alexastudillo.taxdocument.application.invoicedraft.CreateInvoiceDraftCommand;
import com.alexastudillo.taxdocument.application.invoicedraft.CreateInvoiceDraftService;
import com.alexastudillo.taxdocument.application.invoicedraft.DraftIdentifierGenerator;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftCandidate;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftRepository;
import com.alexastudillo.taxdocument.application.invoicedraft.PersistedInvoiceDraft;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

final class InfrastructureTestFixtures {
  private static final AtomicLong FIXTURE_SEQUENCE = new AtomicLong(100L);

  private InfrastructureTestFixtures() {}

  static InvoiceDraftCandidate candidate() {
    long fixtureId = FIXTURE_SEQUENCE.incrementAndGet();
    CreateInvoiceDraftCommand baseline = ApplicationTestFixtures.command();
    CreateInvoiceDraftCommand command =
        new CreateInvoiceDraftCommand(
            new CompanyId(new java.util.UUID(0x1111111111114111L, fixtureId)),
            baseline.requestCreationInstant(),
            baseline.deadline(),
            "infrastructure-key-" + fixtureId,
            baseline.correlationId(),
            baseline.emissionPointId(),
            baseline.emissionDate(),
            baseline.buyer(),
            baseline.lines(),
            baseline.payments(),
            baseline.additionalInformation());
    AtomicLong identifierSequence = new AtomicLong(fixtureId * 100L);
    DraftIdentifierGenerator identifiers =
        () -> new java.util.UUID(0x123456789abcdef0L, identifierSequence.incrementAndGet());
    AtomicReference<InvoiceDraftCandidate> result = new AtomicReference<>();
    InvoiceDraftRepository capture =
        new InvoiceDraftRepository() {
          @Override
          public Uni<IdempotencyLookup> findByIdempotency(
              CompanyId companyId, byte[] keyHash, byte[] requestFingerprint, Duration remaining) {
            return Uni.createFrom().<IdempotencyLookup>item(new IdempotencyLookup.Missing());
          }

          @Override
          public Uni<PersistedInvoiceDraft> persist(
              InvoiceDraftCandidate candidate, Duration remaining) {
            result.set(candidate);
            Instant time = Instant.parse("2026-07-17T12:00:01Z");
            return Uni.createFrom()
                .<PersistedInvoiceDraft>item(
                    new PersistedInvoiceDraft(candidate.draft(), time, time));
          }
        };
    new CreateInvoiceDraftService(capture, ApplicationTestFixtures.references(), identifiers)
        .create(command)
        .subscribe()
        .with(
            ignored -> {},
            failure -> {
              throw new IllegalStateException(failure);
            });
    return result.get();
  }
}
