package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftApplicationException;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftCandidate;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftFailure;
import com.alexastudillo.taxdocument.application.invoicedraft.InvoiceDraftRepository;
import com.alexastudillo.taxdocument.application.invoicedraft.PersistedInvoiceDraft;
import com.alexastudillo.taxdocument.application.requestcontext.RequestClock;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import com.alexastudillo.taxdocument.infrastructure.persistence.ReactiveOperationBudget;
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
import java.util.Objects;
import java.util.UUID;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Company-scoped reactive aggregate repository and sole transactional timestamp owner. */
@NullMarked
@ApplicationScoped
public final class InvoiceDraftRepositoryAdapter implements InvoiceDraftRepository {
  private final RequestClock clock;
  private final InvoiceDraftPersistenceMapper mapper;
  private final Duration queryTimeout;
  private final Duration writeTimeout;

  public InvoiceDraftRepositoryAdapter(RequestClock clock, InvoiceDraftPersistenceMapper mapper) {
    this.clock = clock;
    this.mapper = mapper;
    @Nullable Duration configuredQueryTimeout =
        ConfigProvider.getConfig()
            .getValue("invoice-draft.persistence.operation-timeout", Duration.class);
    @Nullable Duration configuredWriteTimeout =
        ConfigProvider.getConfig()
            .getValue("invoice-draft.persistence.write-transaction-timeout", Duration.class);
    this.queryTimeout = requireHydrated(configuredQueryTimeout, "operation timeout");
    this.writeTimeout = requireHydrated(configuredWriteTimeout, "write timeout");
  }

  @Override
  public Uni<@NonNull IdempotencyLookup> findByIdempotency(
      CompanyId companyId, byte[] keyHash, byte[] requestFingerprint, Duration remaining) {
    if (remaining.isZero() || remaining.isNegative()) {
      return requireUni(Uni.createFrom().failure(deadlineExhausted()), "deadline failure");
    }
    Uni<@NonNull IdempotencyLookup> operation =
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
                                .<@NonNull IdempotencyLookup>item(new IdempotencyLookup.Missing());
                          }
                          if (!Arrays.equals(binding.requestFingerprint, requestFingerprint)) {
                            return Uni.createFrom()
                                .<@NonNull IdempotencyLookup>item(
                                    new IdempotencyLookup.Conflict(
                                        requireHydrated(
                                            binding.requestFingerprint,
                                            "idempotency.requestFingerprint")));
                          }
                          return load(
                                  companyId,
                                  requireHydrated(
                                      binding.invoiceDraftId, "idempotency.invoiceDraftId"))
                              .onItem()
                              .<@NonNull IdempotencyLookup>transform(
                                  persisted ->
                                      new IdempotencyLookup.Equivalent(
                                          requireHydrated(persisted, "persisted invoice draft")));
                        }));
    return bounded(requireHydrated(operation, "idempotency lookup"), remaining, queryTimeout);
  }

  @Override
  public Uni<@NonNull PersistedInvoiceDraft> persist(
      InvoiceDraftCandidate candidate, Duration remaining) {
    if (remaining.isZero() || remaining.isNegative()) {
      return requireUni(Uni.createFrom().failure(deadlineExhausted()), "deadline failure");
    }
    Uni<@NonNull PersistedInvoiceDraft> operation =
        Panache.withTransaction(
            () -> {
              Instant timestamp = clock.persistenceTime();
              InvoiceDraftPersistenceMapper.MappedAggregate aggregate =
                  mapper.toEntities(candidate, timestamp);
              InvoiceDraftIdempotencyEntity binding = binding(candidate, timestamp);
              List<@NonNull PanacheEntityBase> entities = new ArrayList<>();
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
    return requireUni(
        bounded(requireHydrated(operation, "persist operation"), remaining, writeTimeout)
            .onFailure(
                failure -> isUniqueViolation(requireHydrated(failure, "persistence failure")))
            .recoverWithUni(
                _ ->
                    recoverIdempotencyWinner(
                        candidate, remainingAfter(remaining, System.nanoTime() - startedNanos))),
        "idempotency recovery");
  }

  private Uni<@NonNull PersistedInvoiceDraft> recoverIdempotencyWinner(
      InvoiceDraftCandidate candidate, Duration remaining) {
    return requireUni(
        findByIdempotency(
                candidate.draft().companyId(),
                requireHydrated(candidate.idempotencyKeyHash(), "idempotencyKeyHash"),
                requireHydrated(candidate.requestFingerprint(), "requestFingerprint"),
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
                }),
        "idempotency winner recovery");
  }

  private Uni<@NonNull PersistedInvoiceDraft> load(CompanyId companyId, UUID draftId) {
    return requireUni(
        InvoiceDraftEntity.find("companyId = ?1 and id = ?2", companyId.value(), draftId)
            .<InvoiceDraftEntity>firstResult()
            .onItem()
            .ifNull()
            .failWith(() -> persistenceUnavailable("The idempotent draft could not be loaded"))
            .onItem()
            .transformToUni(
                root -> loadAggregate(requireHydrated(root, "invoice draft entity"), draftId)),
        "loaded invoice draft");
  }

  private Uni<@NonNull PersistedInvoiceDraft> loadAggregate(InvoiceDraftEntity root, UUID draftId) {
    LoadedAggregate loaded = new LoadedAggregate();
    return requireUni(
        InvoiceLineEntity.<InvoiceLineEntity>list("invoiceDraftId", draftId)
            .invoke(lines -> loaded.lines = requireElements(lines, "loaded invoice lines"))
            .chain(() -> loadLineTaxes(loaded.lines))
            .invoke(lineTaxes -> loaded.lineTaxes = requireHydrated(lineTaxes, "loaded line taxes"))
            .chain(
                () -> InvoiceTaxTotalEntity.<InvoiceTaxTotalEntity>list("invoiceDraftId", draftId))
            .invoke(totals -> loaded.taxTotals = requireElements(totals, "loaded tax totals"))
            .chain(() -> InvoicePaymentEntity.<InvoicePaymentEntity>list("invoiceDraftId", draftId))
            .invoke(payments -> loaded.payments = requireElements(payments, "loaded payments"))
            .chain(
                () ->
                    InvoiceAdditionalInformationEntity.<InvoiceAdditionalInformationEntity>list(
                        "invoiceDraftId", draftId))
            .invoke(
                additional ->
                    loaded.additional =
                        requireElements(additional, "loaded additional information"))
            .map(
                _ ->
                    mapper.toPersisted(
                        root,
                        loaded.lines,
                        loaded.lineTaxes,
                        loaded.taxTotals,
                        loaded.payments,
                        loaded.additional)),
        "loaded invoice draft aggregate");
  }

  private Uni<@NonNull List<@NonNull InvoiceLineTaxEntity>> loadLineTaxes(
      List<@NonNull InvoiceLineEntity> lines) {
    if (lines.isEmpty()) {
      return requireUni(
          Uni.createFrom().item(requireHydrated(List.of(), "empty line taxes")),
          "empty line taxes result");
    }
    List<@NonNull UUID> lineIds =
        Objects.requireNonNull(
            lines.stream()
                .<@NonNull UUID>map(value -> requireHydrated(value.id, "invoice line id"))
                .toList(),
            "invoice line ids");
    return requireUni(
        InvoiceLineTaxEntity.<InvoiceLineTaxEntity>list("invoiceLineId in ?1", lineIds)
            .map(values -> requireElements(values, "loaded line taxes")),
        "loaded line taxes");
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

  private <T extends @NonNull Object> Uni<@NonNull T> bounded(
      Uni<@NonNull T> operation, Duration remaining, Duration configuredTimeout) {
    if (remaining.isZero() || remaining.isNegative()) {
      return requireUni(Uni.createFrom().failure(deadlineExhausted()), "deadline failure");
    }
    ReactiveOperationBudget budget = ReactiveOperationBudget.clamp(remaining, configuredTimeout);
    return requireUni(
        operation
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
                        "Invoice Draft persistence is temporarily unavailable",
                        requireHydrated(throwable, "persistence failure"))),
        "bounded persistence operation");
  }

  private static Duration remainingAfter(Duration original, long elapsedNanos) {
    if (elapsedNanos <= 0L) {
      return original;
    }
    Duration elapsed = Duration.ofNanos(elapsedNanos);
    return elapsed.compareTo(original) >= 0
        ? requireHydrated(Duration.ZERO, "Duration.ZERO")
        : requireHydrated(original.minus(elapsed), "remaining duration");
  }

  private InvoiceDraftApplicationException deadlineExhausted() {
    return new InvoiceDraftApplicationException(
        new InvoiceDraftFailure(
            InvoiceDraftFailure.Code.REQUEST_TIMEOUT,
            "The remaining request budget is exhausted",
            true,
            emptyViolations()));
  }

  private static InvoiceDraftApplicationException persistenceUnavailable(String detail) {
    return new InvoiceDraftApplicationException(
        new InvoiceDraftFailure(
            InvoiceDraftFailure.Code.PERSISTENCE_UNAVAILABLE, detail, true, emptyViolations()));
  }

  private static InvoiceDraftApplicationException idempotencyConflict() {
    return new InvoiceDraftApplicationException(
        new InvoiceDraftFailure(
            InvoiceDraftFailure.Code.IDEMPOTENCY_CONFLICT,
            "The idempotency key is already bound to different content",
            false,
            emptyViolations()));
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
            InvoiceDraftFailure.Code.PERSISTENCE_UNAVAILABLE, detail, true, emptyViolations()),
        cause);
  }

  private static List<InvoiceDraftFailure.@NonNull Violation> emptyViolations() {
    return requireHydrated(List.of(), "empty violations");
  }

  private static <T> @NonNull T requireHydrated(@Nullable T value, String field) {
    return Objects.requireNonNull(value, field);
  }

  private static <T extends @NonNull Object> Uni<@NonNull T> requireUni(
      @Nullable Uni<@NonNull T> value, String field) {
    return Objects.requireNonNull(value, field);
  }

  private static <T extends @NonNull Object> List<@NonNull T> requireElements(
      @Nullable List<? extends @Nullable T> values, String field) {
    List<? extends @Nullable T> required = Objects.requireNonNull(values, field);
    List<@NonNull T> result = new ArrayList<>(required.size());
    for (@Nullable T value : required) {
      result.add(Objects.requireNonNull(value, field + " element"));
    }
    return Objects.requireNonNull(List.copyOf(result), field);
  }

  private static final class LoadedAggregate {
    private List<@NonNull InvoiceLineEntity> lines =
        requireHydrated(List.of(), "empty invoice lines");
    private List<@NonNull InvoiceLineTaxEntity> lineTaxes =
        requireHydrated(List.of(), "empty line taxes");
    private List<@NonNull InvoiceTaxTotalEntity> taxTotals =
        requireHydrated(List.of(), "empty tax totals");
    private List<@NonNull InvoicePaymentEntity> payments =
        requireHydrated(List.of(), "empty payments");
    private List<@NonNull InvoiceAdditionalInformationEntity> additional =
        requireHydrated(List.of(), "empty additional information");

    private LoadedAggregate() {}
  }
}
