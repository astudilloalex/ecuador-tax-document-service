package com.alexastudillo.taxdocument.support.fiscalpreparation;

import static java.util.Objects.requireNonNull;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Bounded local implementation of authoritative-fiscal-context consumer contract 1.0.0. */
@NullMarked
public final class AuthoritativeFiscalContextFixture implements AutoCloseable {
  public static final int PORT = 18082;
  public static final String PATH = "/fiscal-context-resolutions/invoice-issuance";
  private static final Duration WAIT = requireNonNull(Duration.ofSeconds(5));
  private static final String VALID_CONTEXT =
      """
      {
        "issuerReference":"issuer-fixture-1",
        "issuerRuc":"1790012345001",
        "legalName":"Fixture Issuer S.A.",
        "commercialName":"Fixture Issuer",
        "headOfficeAddress":"Quito",
        "accountingRequired":true,
        "rimpeClassification":"NONE",
        "establishmentReference":"establishment-fixture-1",
        "establishmentCode":"001",
        "establishmentAddress":"Quito",
        "emissionPointId":"123e4567-e89b-12d3-a456-426614174000",
        "emissionPointCode":"001",
        "environmentCode":"1",
        "documentTypeCode":"01",
        "emissionTypeCode":"1",
        "invoiceIssuanceEligible":true,
        "sourceEvidence":{
          "authority":"SRI fixture authority",
          "revision":"fixture-revision-1",
          "effectiveFrom":"2026-07-01",
          "observedAt":"2026-07-18T12:00:00Z"
        }
      }
      """;

  private final Vertx vertx;
  private final HttpServer server;
  private final Set<HttpConnection> connections;
  private final AtomicInteger calls = new AtomicInteger();
  private final AtomicReference<@NonNull ResponsePlan> response =
      new AtomicReference<>(new ResponsePlan(200, VALID_CONTEXT, zeroDuration()));
  private final AtomicReference<@NonNull Optional<@NonNull CapturedRequest>> lastRequest =
      new AtomicReference<>(requireNonNull(Optional.empty()));

  private AuthoritativeFiscalContextFixture(
      Vertx vertx, HttpServer server, Set<HttpConnection> connections) {
    this.vertx = vertx;
    this.server = server;
    this.connections = connections;
  }

  public static AuthoritativeFiscalContextFixture start() {
    Vertx vertx = Vertx.vertx();
    Router router = Router.router(vertx);
    Set<HttpConnection> connections = requireNonNull(ConcurrentHashMap.newKeySet());
    AtomicReference<AuthoritativeFiscalContextFixture> fixture = new AtomicReference<>();
    router
        .route(HttpMethod.POST, PATH)
        .handler(
            context ->
                context
                    .request()
                    .bodyHandler(
                        body -> {
                          AuthoritativeFiscalContextFixture active =
                              Objects.requireNonNull(fixture.get(), "fixture");
                          active.calls.incrementAndGet();
                          active.lastRequest.set(
                              requireNonNull(
                                  Optional.of(
                                      new CapturedRequest(
                                          context.request().getHeader("X-Company-Id"),
                                          context.request().getHeader("X-Correlation-Id"),
                                          requireNonNull(body.toString())))));
                          @Nullable ResponsePlan nullablePlan = active.response.get();
                          ResponsePlan plan = requireNonNull(nullablePlan, "response plan");
                          Runnable send =
                              () ->
                                  context
                                      .response()
                                      .setStatusCode(plan.status())
                                      .putHeader("Content-Type", contentType(plan.status()))
                                      .putHeader("Connection", "close")
                                      .end(plan.body());
                          if (plan.delay().isZero()) {
                            send.run();
                          } else {
                            context.vertx().setTimer(plan.delay().toMillis(), _ -> send.run());
                          }
                        }));
    HttpServer server =
        vertx
            .createHttpServer()
            .connectionHandler(
                connection -> {
                  connections.add(connection);
                  connection.closeHandler(_ -> connections.remove(connection));
                })
            .requestHandler(router)
            .listen(PORT, "127.0.0.1")
            .toCompletionStage()
            .toCompletableFuture()
            .orTimeout(WAIT.toMillis(), TimeUnit.MILLISECONDS)
            .join();
    AuthoritativeFiscalContextFixture result =
        new AuthoritativeFiscalContextFixture(
            vertx, requireNonNull(server, "HTTP server"), connections);
    fixture.set(result);
    return result;
  }

  public void valid() {
    plan(200, VALID_CONTEXT, zeroDuration());
  }

  public void providerStatus(int status) {
    plan(status, "{\"code\":\"FIXTURE_PROVIDER_FAILURE\"}", zeroDuration());
  }

  public void providerProblem(int status, String safeCode) {
    if (!safeCode.matches("^[A-Z][A-Z0-9_]{0,63}$")) {
      throw new IllegalArgumentException("safeCode must be a bounded machine code");
    }
    plan(status, "{\"code\":\"" + safeCode + "\"}", zeroDuration());
  }

  public void delayed(Duration delay) {
    plan(200, VALID_CONTEXT, delay);
  }

  public void malformed() {
    plan(200, "{not-json", zeroDuration());
  }

  public void partial() {
    plan(200, "{\"issuerReference\":\"partial\"}", zeroDuration());
  }

  public void oversized(int bytes) {
    if (bytes < 1) {
      throw new IllegalArgumentException("bytes must be positive");
    }
    plan(200, requireNonNull("x".repeat(bytes)), zeroDuration());
  }

  public void plan(int status, String body, Duration delay) {
    if (status < 100 || status > 599) {
      throw new IllegalArgumentException("status must be an HTTP status code");
    }
    if (delay.isNegative()) {
      throw new IllegalArgumentException("delay must not be negative");
    }
    response.set(new ResponsePlan(status, body, delay));
  }

  public int callCount() {
    return calls.get();
  }

  public Optional<@NonNull CapturedRequest> lastRequest() {
    return requireNonNull(lastRequest.get(), "last captured request");
  }

  public void reset() {
    calls.set(0);
    lastRequest.set(requireNonNull(Optional.empty()));
    valid();
  }

  @Override
  public void close() {
    connections.forEach(HttpConnection::close);
    server
        .close()
        .toCompletionStage()
        .toCompletableFuture()
        .orTimeout(WAIT.toMillis(), TimeUnit.MILLISECONDS)
        .join();
    vertx.close();
  }

  private static String contentType(int status) {
    return status == 200 ? "application/json" : "application/problem+json";
  }

  private static Duration zeroDuration() {
    return requireNonNull(Duration.ZERO);
  }

  private record ResponsePlan(int status, String body, Duration delay) {}

  public record CapturedRequest(
      @Nullable String companyId, @Nullable String correlationId, String body) {}
}
