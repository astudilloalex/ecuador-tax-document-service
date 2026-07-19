package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalContextPort;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalContextResolution;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationApplicationException;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationFailure;
import com.alexastudillo.taxdocument.domain.invoicedraft.CompanyId;
import com.alexastudillo.taxdocument.support.fiscalpreparation.AuthoritativeFiscalContextFixture;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@NullMarked
class FiscalContextHttpAdapterTest {
  private static @Nullable AuthoritativeFiscalContextFixture fixture;

  @Inject FiscalContextHttpAdapter adapter;

  @BeforeAll
  static void startFixture() {
    fixture = AuthoritativeFiscalContextFixture.start();
  }

  @AfterAll
  static void stopFixture() {
    fixture().close();
  }

  @BeforeEach
  void resetFixture() {
    fixture().reset();
  }

  private static AuthoritativeFiscalContextFixture fixture() {
    return Objects.requireNonNull(fixture);
  }

  @Test
  void sendsExactCompanyAndSelectionOnceAndMapsCompleteAuthoritativeEvidence() {
    FiscalContextResolution resolution =
        adapter
            .resolve(request(Objects.requireNonNull(Duration.ofSeconds(5))))
            .await()
            .indefinitely();

    assertEquals("1790012345001", resolution.issuerRuc());
    assertEquals("001", resolution.establishmentCode());
    assertEquals("001", resolution.emissionPointCode());
    assertEquals("fixture-revision-1", resolution.sourceEvidence().revision());
    assertEquals(1, fixture().callCount());
    AuthoritativeFiscalContextFixture.CapturedRequest captured =
        fixture().lastRequest().orElseThrow();
    assertEquals("11111111-1111-4111-8111-111111111111", captured.companyId());
    assertEquals("corr-1", captured.correlationId());
    assertFalse(captured.body().contains("company"));
    assertFalse(captured.body().contains("sequential"));
  }

  @Test
  void classifiesEveryProviderOutcomeWithoutRetryOrSensitiveEcho() {
    for (int status : new int[] {404, 409, 422, 503, 504}) {
      fixture().reset();
      fixture().providerStatus(status);
      FiscalPreparationApplicationException failure =
          assertThrows(
              FiscalPreparationApplicationException.class,
              () ->
                  adapter
                      .resolve(request(Objects.requireNonNull(Duration.ofSeconds(5))))
                      .await()
                      .indefinitely());
      assertEquals(
          switch (status) {
            case 409 -> FiscalPreparationFailure.Code.FISCAL_CONTEXT_INCONSISTENT;
            case 503, 504 -> FiscalPreparationFailure.Code.FISCAL_CONTEXT_UNAVAILABLE;
            default -> FiscalPreparationFailure.Code.FISCAL_CONTEXT_INVALID;
          },
          failure.failure().code());
      assertFalse(failure.failure().detail().contains("FIXTURE_PROVIDER_FAILURE"));
      assertEquals(1, fixture().callCount());
    }

    fixture().reset();
    fixture().providerProblem(422, "FISCAL_CONTEXT_UNSUPPORTED");
    assertCode(FiscalPreparationFailure.Code.FISCAL_CONTEXT_UNSUPPORTED);
    fixture().reset();
    fixture().providerProblem(422, "FISCAL_CONTEXT_INCONSISTENT");
    assertCode(FiscalPreparationFailure.Code.FISCAL_CONTEXT_INCONSISTENT);
  }

  @Test
  void malformedPartialOversizedAndTimeoutResponsesFailBeforeAnyLocalTransaction() {
    fixture().malformed();
    assertCode(FiscalPreparationFailure.Code.FISCAL_CONTEXT_INVALID);
    fixture().reset();
    fixture().partial();
    assertCode(FiscalPreparationFailure.Code.FISCAL_CONTEXT_INVALID);
    fixture().reset();
    fixture().oversized(400_000);
    assertCode(FiscalPreparationFailure.Code.FISCAL_CONTEXT_INVALID);
    fixture().reset();
    fixture().delayed(Objects.requireNonNull(Duration.ofSeconds(3)));
    assertCode(FiscalPreparationFailure.Code.FISCAL_CONTEXT_UNAVAILABLE);
    assertEquals(1, fixture().callCount());
  }

  private void assertCode(FiscalPreparationFailure.Code expected) {
    FiscalPreparationApplicationException failure =
        assertThrows(
            FiscalPreparationApplicationException.class,
            () ->
                adapter
                    .resolve(request(Objects.requireNonNull(Duration.ofSeconds(5))))
                    .await()
                    .indefinitely());
    assertEquals(expected, failure.failure().code());
  }

  private static FiscalContextPort.Request request(Duration remaining) {
    return new FiscalContextPort.Request(
        new CompanyId(
            Objects.requireNonNull(UUID.fromString("11111111-1111-4111-8111-111111111111"))),
        Objects.requireNonNull(UUID.fromString("123e4567-e89b-12d3-a456-426614174000")),
        Objects.requireNonNull(LocalDate.of(2026, 7, 18)),
        "01",
        "corr-1",
        remaining);
  }
}
