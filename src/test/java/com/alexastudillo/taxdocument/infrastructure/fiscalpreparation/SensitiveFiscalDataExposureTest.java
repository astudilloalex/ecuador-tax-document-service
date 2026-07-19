package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.api.fiscalpreparation.FiscalPreparationResponse;
import com.alexastudillo.taxdocument.api.fiscalpreparation.telemetry.FiscalPreparationTelemetry;
import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationFailure;
import com.alexastudillo.taxdocument.support.fiscalpreparation.FiscalPreparationTestFixtures;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@NullMarked
class SensitiveFiscalDataExposureTest {
  private static final Set<String> FORBIDDEN_PERSISTED_OR_SIGNAL_FIELDS =
      requireNonNull(
          Set.of(
              "rawrequest",
              "rawresponse",
              "credential",
              "password",
              "authorization",
              "internalerror",
              "sql",
              "endpoint",
              "correlationid"));

  @Test
  void persistenceModelsContainNoRawProviderCredentialInternalOrCorrelationMaterial() {
    for (Class<?> type :
        ListOfClasses.values(FiscalPreparationEntity.class, OfficialSequenceBaselineEntity.class)) {
      for (var field : type.getDeclaredFields()) {
        assertFalse(
            FORBIDDEN_PERSISTED_OR_SIGNAL_FIELDS.contains(field.getName().toLowerCase(Locale.ROOT)),
            type.getSimpleName() + "." + field.getName());
      }
    }
  }

  @Test
  void stableFailuresContainOnlyFixedSafeDetailsAndNeverFiscalPayloadValues() {
    String sensitive =
        "1790012345001 Issuer Quito 00000001 1807202601179001234500110010010000001230000000610 revision-1";
    for (FiscalPreparationFailure.Code code : FiscalPreparationFailure.Code.values()) {
      FiscalPreparationFailure failure = FiscalPreparationFailure.of(code);
      assertFalse(sensitive.contains(failure.detail()));
      assertFalse(failure.detail().matches(".*(?:jdbc:|postgresql:|https?://|SELECT |INSERT ).*"));
    }
  }

  @Test
  void onlyExplicitSuccessRepresentationCarriesContractedFiscalIdentityFields() throws Exception {
    Set<String> responseFields =
        Arrays.stream(FiscalPreparationResponse.class.getRecordComponents())
            .map(RecordComponent::getName)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    assertTrue(responseFields.contains("accessKey"));
    assertTrue(responseFields.contains("numericCode"));
    assertTrue(responseFields.contains("fiscalContextSnapshot"));
    assertFalse(responseFields.contains("companyId"));

    try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/alexastudillo/taxdocument"))) {
      for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file);
        assertFalse(source.contains("System.out.print"), file.toString());
        assertFalse(source.contains("System.err.print"), file.toString());
      }
    }
  }

  @Test
  void emittedMetricsAndAuditLogsUseOnlyBoundedSafeLabels() {
    String safeCorrelation = "99999999-9999-4999-8999-999999999999";
    StringBuilder captured = new StringBuilder();
    Handler handler =
        new Handler() {
          @Override
          public void publish(@Nullable LogRecord record) {
            if (record != null) {
              captured.append(record.getMessage()).append('\n');
            }
          }

          @Override
          public void flush() {}

          @Override
          public void close() {}
        };
    Logger logger = Logger.getLogger(FiscalPreparationTelemetry.class.getName());
    logger.addHandler(handler);
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    try {
      FiscalPreparationTelemetry telemetry = new FiscalPreparationTelemetry(registry);
      telemetry.completed(safeCorrelation, false);
      telemetry.completed(safeCorrelation, true);
      for (FiscalPreparationFailure.Code code : FiscalPreparationFailure.Code.values()) {
        telemetry.failed(safeCorrelation, FiscalPreparationFailure.of(code));
      }

      registry
          .getMeters()
          .forEach(
              meter -> {
                assertEqualsName(meter.getId().getName());
                meter
                    .getId()
                    .getTags()
                    .forEach(
                        tag -> {
                          assertTrue(Set.of("outcome", "commit_knowledge").contains(tag.getKey()));
                          assertFalse(
                              tag.getValue()
                                  .contains(FiscalPreparationTestFixtures.COMPANY_UUID.toString()));
                          assertFalse(
                              tag.getValue()
                                  .contains(FiscalPreparationTestFixtures.DRAFT.toString()));
                        });
              });
    } finally {
      registry.close();
      logger.removeHandler(handler);
    }

    String signals = captured.toString();
    assertNotNull(signals);
    assertTrue(signals.contains(safeCorrelation));
    for (String forbidden :
        List.of(
            FiscalPreparationTestFixtures.COMPANY_UUID.toString(),
            FiscalPreparationTestFixtures.DRAFT.toString(),
            "1792146739001",
            "Issuer S.A.",
            "Head Office",
            "00000000",
            "revision-1",
            "postgresql://",
            "SELECT ")) {
      assertFalse(signals.contains(forbidden), forbidden);
    }
  }

  private static void assertEqualsName(String name) {
    assertTrue(name.equals("fiscal_preparation_requests_total"), name);
  }

  private static final class ListOfClasses {
    private ListOfClasses() {}

    private static Class<?>[] values(Class<?> first, Class<?> second) {
      return new Class<?>[] {first, second};
    }
  }
}
