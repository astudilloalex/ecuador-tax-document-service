package com.alexastudillo.taxdocument.runtime;

import static io.restassured.RestAssured.given;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
@NullMarked
class InvoiceDraftJvmPerformanceIT {
  private static final String COMPANY = "a1111111-1111-4111-8111-111111111111";
  private static final String PATH = "/api/v1/invoice-drafts";
  private static final int WARM_UP_SAMPLES = 20;
  private static final int TYPICAL_SAMPLES = 100;
  private static final int MAXIMUM_SAMPLES = 20;
  private static final int REPLAY_SAMPLES = 100;
  private static final int CONFLICT_SAMPLES = 100;

  @Test
  void packagedJvmMeetsLatencyConcurrencyAndPoolRecoveryBudgets() throws Exception {
    LocalDate today = requireNonNull(LocalDate.now(requireNonNull(ZoneId.of("America/Guayaquil"))));
    awaitResourceObservationWindow();
    String typical = typicalBody(today, "Performance Buyer");
    long warmUpStarted = System.nanoTime();
    for (int sample = 0; sample < WARM_UP_SAMPLES; sample++) {
      post("warmup-" + UUID.randomUUID(), typical, 201);
    }
    double warmUpMillis = elapsedMillis(warmUpStarted);

    long measurementStarted = System.nanoTime();
    Profile typicalProfile =
        measure(
            "typical-new",
            TYPICAL_SAMPLES,
            750.0,
            1_500.0,
            _ -> post("typical-" + UUID.randomUUID(), typical, 201));

    String maximum = maximumBody(today);
    Profile maximumProfile =
        measure(
            "maximum-new",
            MAXIMUM_SAMPLES,
            3_000.0,
            5_000.0,
            _ -> post("maximum-" + UUID.randomUUID(), maximum, 201));

    String replayKey = "replay-profile-" + UUID.randomUUID();
    post(replayKey, typical, 201);
    Profile replayProfile =
        measure(
            "equivalent-replay", REPLAY_SAMPLES, 250.0, 500.0, _ -> post(replayKey, typical, 200));

    String conflictKey = "conflict-profile-" + UUID.randomUUID();
    post(conflictKey, typical, 201);
    String conflicting = typicalBody(today, "Different Performance Buyer");
    Profile conflictProfile =
        measure(
            "idempotency-conflict",
            CONFLICT_SAMPLES,
            250.0,
            500.0,
            _ -> post(conflictKey, conflicting, 409));

    ConcurrencyResult concurrency = runFiftyEquivalent(today);
    long recoveryStarted = System.nanoTime();
    post("pool-recovery-" + UUID.randomUUID(), typical, 201);
    double recoveryMillis = elapsedMillis(recoveryStarted);
    assertTrue(recoveryMillis <= 1_500.0, "Pool recovery request exceeded 1.5 seconds");
    double measurementMillis = elapsedMillis(measurementStarted);

    System.out.printf(
        Locale.ROOT,
        "INVOICE_DRAFT_PERFORMANCE_EVIDENCE "
            + "warmup={samples:%d,elapsedMs:%.3f} measurementElapsedMs=%.3f "
            + "profiles=[%s,%s,%s,%s] "
            + "concurrency={requests:50,new:%d,replay:%d,elapsedMs:%.3f} "
            + "poolRecoveryMs=%.3f%n",
        WARM_UP_SAMPLES,
        warmUpMillis,
        measurementMillis,
        typicalProfile,
        maximumProfile,
        replayProfile,
        conflictProfile,
        concurrency.created(),
        concurrency.replayed(),
        concurrency.elapsedMillis(),
        recoveryMillis);
  }

  private static void awaitResourceObservationWindow() throws InterruptedException {
    // This blocks only the external JUnit client, never a service event-loop thread.
    new java.util.concurrent.CountDownLatch(1).await(10, TimeUnit.SECONDS);
  }

  private static Profile measure(
      String name,
      int samples,
      double p95BudgetMillis,
      double p99BudgetMillis,
      MeasuredRequest request) {
    long[] values = new long[samples];
    for (int sample = 0; sample < samples; sample++) {
      long started = System.nanoTime();
      request.execute(sample);
      values[sample] = System.nanoTime() - started;
    }
    Arrays.sort(values);
    double p50 = percentileMillis(values, 0.50);
    double p95 = percentileMillis(values, 0.95);
    double p99 = percentileMillis(values, 0.99);
    double maximum = values[values.length - 1] / 1_000_000.0;
    assertTrue(p95 <= p95BudgetMillis, name + " p95 exceeded its budget: " + p95);
    assertTrue(p99 <= p99BudgetMillis, name + " p99 exceeded its budget: " + p99);
    return new Profile(name, samples, p50, p95, p99, maximum);
  }

  private static ConcurrencyResult runFiftyEquivalent(LocalDate date) throws Exception {
    String key = "concurrency-profile-" + UUID.randomUUID();
    String body = typicalBody(date, "Concurrent Performance Buyer");
    long started = System.nanoTime();
    List<Integer> statuses;
    try (ExecutorService executor = Executors.newFixedThreadPool(50)) {
      List<CompletableFuture<Integer>> requests =
          java.util.stream.IntStream.range(0, 50)
              .mapToObj(_ -> CompletableFuture.supplyAsync(() -> postStatus(key, body), executor))
              .toList();
      CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);
      statuses = requests.stream().map(future -> future.join()).toList();
    }
    double elapsedMillis = elapsedMillis(started);
    long created = statuses.stream().filter(status -> status == 201).count();
    long replayed = statuses.stream().filter(status -> status == 200).count();
    assertEquals(1L, created);
    assertEquals(49L, replayed);
    assertTrue(elapsedMillis < Duration.ofSeconds(10).toMillis());
    return new ConcurrencyResult((int) created, (int) replayed, elapsedMillis);
  }

  private static void post(String key, String body, int expectedStatus) {
    given()
        .contentType("application/json")
        .header("X-Company-Id", COMPANY)
        .header("Idempotency-Key", key)
        .body(body)
        .when()
        .post(PATH)
        .then()
        .statusCode(expectedStatus);
  }

  private static int postStatus(String key, String body) {
    return given()
        .contentType("application/json")
        .header("X-Company-Id", COMPANY)
        .header("Idempotency-Key", key)
        .body(body)
        .when()
        .post(PATH)
        .statusCode();
  }

  private static double percentileMillis(long[] sortedNanos, double percentile) {
    int index = Math.max(0, (int) Math.ceil(percentile * sortedNanos.length) - 1);
    return sortedNanos[index] / 1_000_000.0;
  }

  private static double elapsedMillis(long startedNanos) {
    return (System.nanoTime() - startedNanos) / 1_000_000.0;
  }

  private static String typicalBody(LocalDate date, String buyerName) {
    return requireNonNull(
        """
        {
          "emissionPointId": "123e4567-e89b-12d3-a456-426614174000",
          "emissionDate": "%s",
          "buyer": {
            "identificationType": "06",
            "identification": "PERF123",
            "legalName": "%s"
          },
          "lines": [{
            "productCode": "PERF1",
            "description": "Performance service",
            "quantity": "1",
            "unitPrice": "10.000000",
            "discount": "0.00",
            "taxRuleId": "5b34b038-931c-50e3-a84c-10af272fdcd4"
          }],
          "payments": [{
            "paymentMethodId": "639f2b7e-10a3-5d92-a1a3-28223896f5b5",
            "amount": "11.50"
          }],
          "additionalInformation": []
        }
        """
            .formatted(date, buyerName));
  }

  private static String maximumBody(LocalDate date) {
    StringBuilder lines = new StringBuilder();
    for (int position = 1; position <= 500; position++) {
      if (position > 1) {
        lines.append(',');
      }
      lines.append(
          """
          {
            "productCode":"SKU%d",
            "description":"Maximum performance service %d",
            "quantity":"1",
            "unitPrice":"1.000000",
            "discount":"0.00",
            "taxRuleId":"5b34b038-931c-50e3-a84c-10af272fdcd4"
          }
          """
              .formatted(position, position));
    }
    String[] paymentMethods = {
      "639f2b7e-10a3-5d92-a1a3-28223896f5b5",
      "daad9ac7-6a55-5df6-8a9e-60012c5d261b",
      "cbf7e764-0ef5-5422-965e-fe08eaa49772",
      "8b626780-39fb-5c72-b1e2-8453df01b79a",
      "65eee3f8-1c46-5749-8101-6e6d50d08a69",
      "178f5fd1-038b-577f-bac3-21c49ce6d1f2",
      "953df84c-d41c-5e72-b975-9d02c45ee656",
      "f2bc801e-c241-5df8-99f8-ceb9ee870d05"
    };
    StringBuilder payments = new StringBuilder();
    for (int index = 0; index < paymentMethods.length; index++) {
      if (index > 0) {
        payments.append(',');
      }
      String amount = index == paymentMethods.length - 1 ? "71.91" : "71.87";
      payments.append(
          "{\"paymentMethodId\":\"%s\",\"amount\":\"%s\"}"
              .formatted(paymentMethods[index], amount));
    }
    StringBuilder additional = new StringBuilder();
    for (int position = 1; position <= 15; position++) {
      if (position > 1) {
        additional.append(',');
      }
      additional.append(
          "{\"name\":\"Reference %d\",\"value\":\"Maximum value %d\"}"
              .formatted(position, position));
    }
    return requireNonNull(
        """
        {
          "emissionPointId":"123e4567-e89b-12d3-a456-426614174000",
          "emissionDate":"%s",
          "buyer":{
            "identificationType":"06",
            "identification":"PERFMAX",
            "legalName":"Maximum Performance Buyer"
          },
          "lines":[%s],
          "payments":[%s],
          "additionalInformation":[%s]
        }
        """
            .formatted(date, lines, payments, additional));
  }

  @FunctionalInterface
  private interface MeasuredRequest {
    void execute(int sample);
  }

  private record Profile(
      String name,
      int samples,
      double p50Millis,
      double p95Millis,
      double p99Millis,
      double maximumMillis) {
    @Override
    public String toString() {
      return requireNonNull(
          String.format(
              Locale.ROOT,
              "{name:%s,samples:%d,p50Ms:%.3f,p95Ms:%.3f,p99Ms:%.3f,maxMs:%.3f}",
              name,
              samples,
              p50Millis,
              p95Millis,
              p99Millis,
              maximumMillis));
    }
  }

  private record ConcurrencyResult(int created, int replayed, double elapsedMillis) {}
}
