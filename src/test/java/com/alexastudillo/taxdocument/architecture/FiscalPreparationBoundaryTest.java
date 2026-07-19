package com.alexastudillo.taxdocument.architecture;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class FiscalPreparationBoundaryTest {
  private static final Path ROOT =
      requireNonNull(Path.of("src/main/java/com/alexastudillo/taxdocument"));

  @Test
  void fiscalDomainIsSynchronousFrameworkFreeAndAllDependenciesPointInward() throws Exception {
    for (Path file : javaFiles(resolve("domain/fiscalpreparation"))) {
      String source = Files.readString(file);
      assertFalse(
          source.matches(
              "(?s).*import (?:jakarta|io\\.(?:quarkus|smallrye|vertx)|org\\.hibernate|"
                  + "com\\.fasterxml|java\\.sql).*"),
          file.toString());
    }
    for (Path file : javaFiles(resolve("application/fiscalpreparation"))) {
      String source = Files.readString(file);
      assertFalse(source.contains("taxdocument.api."), file.toString());
      assertFalse(source.contains("taxdocument.infrastructure."), file.toString());
      assertFalse(source.contains("jakarta.ws.rs"), file.toString());
    }
    for (Path file : javaFiles(resolve("infrastructure/fiscalpreparation"))) {
      assertFalse(Files.readString(file).contains("taxdocument.api."), file.toString());
    }
  }

  @Test
  void exactOwnedAndExtractedTypesAreDirectlyNullMarkedWithoutPackageDefaults() throws Exception {
    for (String packagePath :
        List.of(
            "api/fiscalpreparation",
            "api/fiscalpreparation/telemetry",
            "api/requestcontext",
            "api/problem",
            "application/fiscalpreparation",
            "application/requestcontext",
            "domain/fiscalpreparation",
            "infrastructure/fiscalpreparation",
            "infrastructure/requestcontext",
            "infrastructure/persistence",
            "infrastructure/health")) {
      Path directory = ROOT.resolve(packagePath);
      String packageInfo = Files.readString(directory.resolve("package-info.java"));
      assertFalse(packageInfo.contains("@NullMarked"), packagePath);
      try (Stream<Path> files = Files.list(directory)) {
        for (Path type :
            files
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                .sorted()
                .toList()) {
          assertTrue(Files.readString(type).contains("@NullMarked"), type.toString());
        }
      }
    }
    for (String file :
        List.of(
            "api/invoicedraft/InvoiceDraftExceptionMapper.java",
            "api/invoicedraft/InvoiceDraftRequestBoundary.java",
            "api/invoicedraft/InvoiceDraftRequestGateFilter.java",
            "api/invoicedraft/InvoiceDraftRequestDeadlineHandler.java",
            "api/invoicedraft/InvoiceDraftResource.java",
            "application/invoicedraft/CreateInvoiceDraftCommand.java",
            "infrastructure/invoicedraft/InvoiceDraftRepositoryAdapter.java",
            "infrastructure/invoicedraft/ReferenceDataRepositoryAdapter.java")) {
      assertTrue(Files.readString(ROOT.resolve(file)).contains("@NullMarked"), file);
    }
    assertFalse(
        Files.readString(ROOT.resolve("application/invoicedraft/package-info.java"))
            .contains("@NullMarked"));
    assertFalse(
        Files.readString(ROOT.resolve("infrastructure/invoicedraft/package-info.java"))
            .contains("@NullMarked"));
  }

  @Test
  void noSuppressionSequenceOrExcludedCapabilityCanEnterFeatureTwo() throws Exception {
    Pattern excluded =
        Pattern.compile(
            "(?i)(authentication|authorization|security|keycloak|jwt|companyrepository|"
                + "issuerrepository|establishmentrepository|emissionpointrepository|"
                + "baselineadmin|xmlgenerator|xmlsign|certificate|sriclient|ridegenerator|"
                + "pdfgenerator|emailclient|webhook|messagequeue|notification|scheduler|executor)");
    for (Path file : javaFiles(ROOT)) {
      String relative = ROOT.relativize(file).toString().replace('\\', '/');
      if (relative.contains("fiscalpreparation") || relative.contains("infrastructure/health")) {
        String source = Files.readString(file);
        assertFalse(source.contains("@SuppressWarnings"), relative);
        assertFalse(source.contains("@NullUnmarked"), relative);
        assertFalse(excluded.matcher(file.getFileName().toString()).find(), relative);
      }
    }
    String migration =
        Files.readString(
                Path.of("src/main/resources/db/migration/V6__create_fiscal_preparation.sql"))
            .toLowerCase(Locale.ROOT);
    assertFalse(migration.contains("create sequence"));
    assertFalse(migration.contains("nextval("));
  }

  private static List<Path> javaFiles(Path root) throws Exception {
    try (Stream<Path> files = Files.walk(root)) {
      return requireNonNull(
          files.filter(path -> path.toString().endsWith(".java")).sorted().toList());
    }
  }

  private static Path resolve(String relative) {
    return requireNonNull(ROOT.resolve(relative));
  }
}
