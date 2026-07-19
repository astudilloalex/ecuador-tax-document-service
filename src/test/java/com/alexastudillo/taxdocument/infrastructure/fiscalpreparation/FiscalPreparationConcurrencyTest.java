package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationCommitIntent;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationCommitResult;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationStore;
import com.alexastudillo.taxdocument.application.fiscalpreparation.InvoiceDraftPreparationView;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.FiscalPreparation;
import com.alexastudillo.taxdocument.support.FixedRequestClock;
import com.alexastudillo.taxdocument.support.fiscalpreparation.FiscalPreparationPostgreSqlSupport;
import com.alexastudillo.taxdocument.support.fiscalpreparation.FiscalPreparationTestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@NullMarked
class FiscalPreparationConcurrencyTest {
  @Inject FiscalPreparationStore store;
  @Inject FiscalPreparationPostgreSqlSupport database;
  @Inject FixedRequestClock clock;

  @BeforeEach
  void reset() {
    database.resetSchema();
    clock.reset(FiscalPreparationTestFixtures.CREATED_AT, FiscalPreparationTestFixtures.CREATED_AT);
    database.insertControlledBaseline(
        FiscalPreparationTestFixtures.BASELINE,
        FiscalPreparationTestFixtures.COMPANY_UUID,
        "issuer-1",
        "establishment-1",
        FiscalPreparationTestFixtures.EMISSION_POINT,
        "001",
        "001",
        0,
        requireNonNull(FiscalPreparationTestFixtures.CREATED_AT.minusSeconds(60)));
  }

  @Test
  void oneHundredEquivalentRequestsConvergeOnOneIdentityAndOneIncrement() {
    insertDraft(FiscalPreparationTestFixtures.DRAFT);
    List<FiscalPreparation> preparations =
        executeConcurrent(List.of(FiscalPreparationTestFixtures.intent()), 100);

    assertEquals(100, preparations.size());
    assertEquals(1, new HashSet<>(preparations).size());
    assertEquals(
        1,
        database.lastAllocated(
            FiscalPreparationTestFixtures.COMPANY_UUID, FiscalPreparationTestFixtures.BASELINE));
    assertEquals(1L, database.rowCount("fiscal_preparation"));
  }

  @Test
  void oneHundredDifferentDraftsInOneScopeReceiveExactlyTheNextOneHundredValuesAndKeys() {
    List<FiscalPreparationCommitIntent> intents = new ArrayList<>();
    for (int index = 1; index <= 100; index++) {
      UUID draftId = new UUID(0x2222222222224222L, 0x8222000000000000L + index);
      insertDraft(draftId);
      intents.add(
          new FiscalPreparationCommitIntent(
              new InvoiceDraftPreparationView(
                  draftId,
                  FiscalPreparationTestFixtures.COMPANY,
                  FiscalPreparationTestFixtures.EMISSION_POINT,
                  FiscalPreparationTestFixtures.DATE,
                  "DRAFT"),
              FiscalPreparationTestFixtures.snapshot()));
    }

    List<FiscalPreparation> preparations = executeConcurrent(intents, 1);
    List<Integer> sequentialNumbers =
        preparations.stream()
            .map(value -> value.officialSequentialNumber().number())
            .sorted()
            .toList();
    assertEquals(
        java.util.stream.IntStream.rangeClosed(1, 100).boxed().toList(), sequentialNumbers);
    assertEquals(
        100, preparations.stream().map(value -> value.accessKey().value()).distinct().count());
    assertEquals(
        100,
        database.lastAllocated(
            FiscalPreparationTestFixtures.COMPANY_UUID, FiscalPreparationTestFixtures.BASELINE));
    assertEquals(100L, database.rowCount("fiscal_preparation"));
  }

  @Test
  void distinctScopesAllocateTheirOwnFirstSequentialIndependently() {
    UUID secondBaseline = uuid("66666666-6666-4666-8666-666666666666");
    UUID secondEmissionPoint = uuid("77777777-7777-4777-8777-777777777777");
    UUID secondDraft = uuid("88888888-8888-4888-8888-888888888888");
    insertDraft(FiscalPreparationTestFixtures.DRAFT);
    database.insertControlledDraft(
        FiscalPreparationTestFixtures.COMPANY_UUID,
        secondDraft,
        secondEmissionPoint,
        FiscalPreparationTestFixtures.DATE,
        requireNonNull(FiscalPreparationTestFixtures.CREATED_AT.minusSeconds(60)));
    database.insertControlledBaseline(
        secondBaseline,
        FiscalPreparationTestFixtures.COMPANY_UUID,
        "issuer-2",
        "establishment-2",
        secondEmissionPoint,
        "002",
        "002",
        0,
        requireNonNull(FiscalPreparationTestFixtures.CREATED_AT.minusSeconds(60)));
    FiscalPreparationCommitIntent secondIntent =
        new FiscalPreparationCommitIntent(
            new InvoiceDraftPreparationView(
                secondDraft,
                FiscalPreparationTestFixtures.COMPANY,
                secondEmissionPoint,
                FiscalPreparationTestFixtures.DATE,
                "DRAFT"),
            FiscalPreparationTestFixtures.snapshot(
                secondEmissionPoint, "issuer-2", "establishment-2", "002", "002"));

    List<FiscalPreparation> preparations =
        executeConcurrent(List.of(FiscalPreparationTestFixtures.intent(), secondIntent), 1);

    assertEquals(
        List.of(1, 1),
        preparations.stream()
            .map(value -> value.officialSequentialNumber().number())
            .sorted()
            .toList());
    assertEquals(
        1,
        database.lastAllocated(
            FiscalPreparationTestFixtures.COMPANY_UUID, FiscalPreparationTestFixtures.BASELINE));
    assertEquals(
        1, database.lastAllocated(FiscalPreparationTestFixtures.COMPANY_UUID, secondBaseline));
  }

  private List<FiscalPreparation> executeConcurrent(
      List<FiscalPreparationCommitIntent> intents, int repetitions) {
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<CompletableFuture<FiscalPreparation>> futures = new ArrayList<>();
      for (FiscalPreparationCommitIntent candidateIntent : intents) {
        FiscalPreparationCommitIntent intent = requireNonNull(candidateIntent);
        for (int repetition = 0; repetition < repetitions; repetition++) {
          futures.add(
              CompletableFuture.supplyAsync(
                  () ->
                      preparation(
                          requireNonNull(
                              store
                                  .commit(intent, requireNonNull(Duration.ofSeconds(10)))
                                  .await()
                                  .indefinitely())),
                  executor));
        }
      }
      return requireNonNull(futures.stream().map(CompletableFuture::join).toList());
    }
  }

  private void insertDraft(UUID draftId) {
    database.insertControlledDraft(
        FiscalPreparationTestFixtures.COMPANY_UUID,
        draftId,
        FiscalPreparationTestFixtures.EMISSION_POINT,
        FiscalPreparationTestFixtures.DATE,
        requireNonNull(FiscalPreparationTestFixtures.CREATED_AT.minusSeconds(60)));
  }

  private static FiscalPreparation preparation(FiscalPreparationCommitResult result) {
    return switch (result) {
      case FiscalPreparationCommitResult.Created created -> created.preparation();
      case FiscalPreparationCommitResult.Replay replay -> replay.preparation();
    };
  }

  private static UUID uuid(String value) {
    return requireNonNull(UUID.fromString(value));
  }
}
