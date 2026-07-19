package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftApplicationException;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftCandidate;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftFailure;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftRepository;
import com.alexastudillo.taxdocument.application.invoicedraft.PersistedInvoiceDraft;
import com.alexastudillo.taxdocument.application.invoicedraft.RequestClock;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import io.vertx.pgclient.PgException;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.config.ConfigProvider;

/** Company-scoped reactive aggregate repository and sole transactional timestamp owner. */
@ApplicationScoped
public final class InvoiceDraftRepositoryAdapter implements InvoiceDraftRepository {
  private final RequestClock clock;
  private final InvoiceDraftPersistenceMapper mapper;
  private final Duration queryTimeout;
  private final Duration writeTimeout;

  public InvoiceDraftRepositoryAdapter(RequestClock clock, InvoiceDraftPersistenceMapper mapper) {
    this.clock = clock;
    this.mapper = mapper;
    this.queryTimeout =
        ConfigProvider.getConfig()
            .getValue("invoice-draft.persistence.operation-timeout", Duration.class);
    this.writeTimeout =
        ConfigProvider.getConfig()
            .getValue("invoice-draft.persistence.write-transaction-timeout", Duration.class);
  }

  @Override
  public Uni<IdempotencyLookup> findByIdempotency(
      CompanyId companyId, byte[] keyHash, byte[] requestFingerprint, Duration remaining) {
    if (remaining.isZero() || remaining.isNegative()) {
      return Uni.createFrom().failure(deadlineExhausted());
    }
    Uni<IdempotencyLookup> operation =
        Panache.withSession(
            () ->
                InvoiceDraftIdempotencyEntity.find(
                        "companyId = ?1 and idempotencyKeyHash = ?2", companyId.value(), keyHash)
                    .<InvoiceDraftIdempotencyEntity>firstResult()
                    .onItem()
                    .transformToUni(
                        binding -> {
                          if (binding == null) {
                            return Uni.createFrom()
                                .<IdempotencyLookup>item(new IdempotencyLookup.Missing());
                          }
                          if (!Arrays.equals(binding.requestFingerprint, requestFingerprint)) {
                            return Uni.createFrom()
                                .<IdempotencyLookup>item(
                                    new IdempotencyLookup.Conflict(binding.requestFingerprint));
                          }
                          return load(companyId, binding.invoiceDraftId)
                              .onItem()
                              .<IdempotencyLookup>transform(IdempotencyLookup.Equivalent::new);
                        }));
    return bounded(operation, remaining, queryTimeout);
  }

  @Override
  public Uni<PersistedInvoiceDraft> persist(InvoiceDraftCandidate candidate, Duration remaining) {
    if (remaining.isZero() || remaining.isNegative()) {
      return Uni.createFrom().failure(deadlineExhausted());
    }
    Uni<PersistedInvoiceDraft> operation =
        Panache.withTransaction(
            () -> {
              Instant timestamp = clock.persistenceTime();
              InvoiceDraftPersistenceMapper.MappedAggregate aggregate =
                  mapper.toEntities(candidate, timestamp);
              InvoiceDraftIdempotencyEntity binding = binding(candidate, timestamp);
              List<PanacheEntityBase> entities = new ArrayList<>();
              entities.add(aggregate.root());
              entities.addAll(aggregate.lines());
              entities.addAll(aggregate.lineTaxes());
              entities.addAll(aggregate.taxTotals());
              entities.addAll(aggregate.payments());
              entities.addAll(aggregate.additionalInformation());
              entities.add(binding);
              return PanacheEntityBase.persist(entities)
                  .replaceWith(
                      mapper.toPersisted(
                          aggregate.root(),
                          aggregate.lines(),
                          aggregate.lineTaxes(),
                          aggregate.taxTotals(),
                          aggregate.payments(),
                          aggregate.additionalInformation()));
            });
    long startedNanos = System.nanoTime();
    return bounded(operation, remaining, writeTimeout)
        .onFailure(this::isUniqueViolation)
        .recoverWithUni(
            ignored ->
                recoverIdempotencyWinner(
                    candidate, remainingAfter(remaining, System.nanoTime() - startedNanos)));
  }

  private Uni<PersistedInvoiceDraft> recoverIdempotencyWinner(
      InvoiceDraftCandidate candidate, Duration remaining) {
    return findByIdempotency(
            candidate.draft().companyId(),
            candidate.idempotencyKeyHash(),
            candidate.requestFingerprint(),
            remaining)
        .onItem()
        .transformToUni(
            lookup -> {
              if (lookup instanceof IdempotencyLookup.Equivalent equivalent) {
                return Uni.createFrom().item(equivalent.persisted());
              }
              if (lookup instanceof IdempotencyLookup.Conflict) {
                return Uni.createFrom().failure(idempotencyConflict());
              }
              return Uni.createFrom()
                  .failure(
                      persistenceUnavailable(
                          "The winning idempotency binding could not be loaded"));
            });
  }

  private Uni<PersistedInvoiceDraft> load(CompanyId companyId, UUID draftId) {
    return InvoiceDraftEntity.find("companyId = ?1 and id = ?2", companyId.value(), draftId)
        .<InvoiceDraftEntity>firstResult()
        .onItem()
        .ifNull()
        .failWith(() -> persistenceUnavailable("The idempotent draft could not be loaded"))
        .onItem()
        .transformToUni(root -> loadAggregate(root, draftId));
  }

  private Uni<PersistedInvoiceDraft> loadAggregate(InvoiceDraftEntity root, UUID draftId) {
    LoadedAggregate loaded = new LoadedAggregate();
    return InvoiceLineEntity.<InvoiceLineEntity>list("invoiceDraftId", draftId)
        .invoke(lines -> loaded.lines = lines)
        .chain(() -> loadLineTaxes(loaded.lines))
        .invoke(lineTaxes -> loaded.lineTaxes = lineTaxes)
        .chain(() -> InvoiceTaxTotalEntity.<InvoiceTaxTotalEntity>list("invoiceDraftId", draftId))
        .invoke(totals -> loaded.taxTotals = totals)
        .chain(() -> InvoicePaymentEntity.<InvoicePaymentEntity>list("invoiceDraftId", draftId))
        .invoke(payments -> loaded.payments = payments)
        .chain(
            () ->
                InvoiceAdditionalInformationEntity.<InvoiceAdditionalInformationEntity>list(
                    "invoiceDraftId", draftId))
        .invoke(additional -> loaded.additional = additional)
        .map(
            ignored ->
                mapper.toPersisted(
                    root,
                    loaded.lines,
                    loaded.lineTaxes,
                    loaded.taxTotals,
                    loaded.payments,
                    loaded.additional));
  }

  private Uni<List<InvoiceLineTaxEntity>> loadLineTaxes(List<InvoiceLineEntity> lines) {
    if (lines.isEmpty()) {
      return Uni.createFrom().item(List.of());
    }
    List<UUID> lineIds = lines.stream().map(value -> value.id).toList();
    return InvoiceLineTaxEntity.list("invoiceLineId in ?1", lineIds);
  }

  private static InvoiceDraftIdempotencyEntity binding(
      InvoiceDraftCandidate candidate, Instant timestamp) {
    InvoiceDraftIdempotencyEntity entity = new InvoiceDraftIdempotencyEntity();
    entity.companyId = candidate.draft().companyId().value();
    entity.idempotencyKeyHash = candidate.idempotencyKeyHash();
    entity.requestFingerprint = candidate.requestFingerprint();
    entity.normalizationVersion = candidate.normalizationVersion();
    entity.invoiceDraftId = candidate.draft().id();
    entity.createdAt = timestamp;
    return entity;
  }

  private <T> Uni<T> bounded(Uni<T> operation, Duration remaining, Duration configuredTimeout) {
    if (remaining.isZero() || remaining.isNegative()) {
      return Uni.createFrom().failure(deadlineExhausted());
    }
    ReactiveOperationBudget budget = ReactiveOperationBudget.clamp(remaining, configuredTimeout);
    return operation
        .ifNoItem()
        .after(budget.timeout())
        .failWith(
            budget.timeoutOwner() == ReactiveOperationBudget.TimeoutOwner.REQUEST_DEADLINE
                ? this::deadlineExhausted
                : () -> persistenceUnavailable("The persistence operation timed out"))
        .onFailure(throwable -> !(throwable instanceof InvoiceDraftApplicationException))
        .transform(
            throwable ->
                persistenceUnavailable(
                    "Invoice Draft persistence is temporarily unavailable", throwable));
  }

  private static Duration remainingAfter(Duration original, long elapsedNanos) {
    if (elapsedNanos <= 0L) {
      return original;
    }
    Duration elapsed = Duration.ofNanos(elapsedNanos);
    return elapsed.compareTo(original) >= 0 ? Duration.ZERO : original.minus(elapsed);
  }

  private InvoiceDraftApplicationException deadlineExhausted() {
    return new InvoiceDraftApplicationException(
        new InvoiceDraftFailure(
            InvoiceDraftFailure.Code.REQUEST_TIMEOUT,
            "The remaining request budget is exhausted",
            true,
            List.of()));
  }

  private static InvoiceDraftApplicationException persistenceUnavailable(String detail) {
    return new InvoiceDraftApplicationException(
        new InvoiceDraftFailure(
            InvoiceDraftFailure.Code.PERSISTENCE_UNAVAILABLE, detail, true, List.of()));
  }

  private static InvoiceDraftApplicationException idempotencyConflict() {
    return new InvoiceDraftApplicationException(
        new InvoiceDraftFailure(
            InvoiceDraftFailure.Code.IDEMPOTENCY_CONFLICT,
            "The idempotency key is already bound to different content",
            false,
            List.of()));
  }

  private boolean isUniqueViolation(Throwable failure) {
    for (Throwable current = failure; current != null; current = current.getCause()) {
      if (current instanceof PgException postgres && "23505".equals(postgres.getSqlState())) {
        return true;
      }
      if (current instanceof SQLException sql
          && ("23505".equals(sql.getSQLState())
              || (sql.getMessage() != null && sql.getMessage().contains("(23505)")))) {
        return true;
      }
    }
    return false;
  }

  private static InvoiceDraftApplicationException persistenceUnavailable(
      String detail, Throwable cause) {
    return new InvoiceDraftApplicationException(
        new InvoiceDraftFailure(
            InvoiceDraftFailure.Code.PERSISTENCE_UNAVAILABLE, detail, true, List.of()),
        cause);
  }

  private static final class LoadedAggregate {
    private List<InvoiceLineEntity> lines = List.of();
    private List<InvoiceLineTaxEntity> lineTaxes = List.of();
    private List<InvoiceTaxTotalEntity> taxTotals = List.of();
    private List<InvoicePaymentEntity> payments = List.of();
    private List<InvoiceAdditionalInformationEntity> additional = List.of();

    private LoadedAggregate() {}
  }
}
