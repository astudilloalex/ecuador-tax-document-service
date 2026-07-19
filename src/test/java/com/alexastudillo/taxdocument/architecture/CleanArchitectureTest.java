package com.alexastudillo.taxdocument.architecture;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class CleanArchitectureTest {
  private static final Path MAIN_JAVA =
      requireNonNull(Path.of("src/main/java").toAbsolutePath().normalize());
  private static final String BASE_PATH = "com/alexastudillo/taxdocument/";
  private static final Set<String> BOUNDARIES =
      requireNonNull(Set.of("api", "application", "domain", "infrastructure"));
  private static final Pattern DECLARED_PROHIBITED_COMPONENT =
      requireNonNull(
          Pattern.compile(
              "(?m)\\b(?:class|interface|record|enum)\\s+(?:CompanyContextPort|"
                  + "ResolveCompanyFiscalContextPort|CompanyClient|CompanyRepository|CompanyEntity|"
                  + "CompanyService|IssuerRepository|IssuerEntity|EstablishmentRepository|"
                  + "EmissionPointRepository|SecurityService|AuthenticationService|"
                  + "AuthorizationService|SriClient|XmlGenerator|XmlSigner|CertificateStore|"
                  + "RideGenerator|PdfGenerator|BaselineAdministrationService|"
                  + "FiscalIssuanceService|BackgroundExecutor|RetryScheduler)\\b"));

  @Test
  void productionSourcesUseOnlyTheApprovedTopLevelBoundaries() throws IOException {
    for (Path candidateSource : javaSources()) {
      Path source = requireNonNull(candidateSource);
      String relative = MAIN_JAVA.relativize(source).toString().replace('\\', '/');
      assertTrue(relative.startsWith(BASE_PATH), () -> "Unexpected base package: " + relative);
      String remainder = relative.substring(BASE_PATH.length());
      String boundary = remainder.substring(0, remainder.indexOf('/'));
      assertTrue(BOUNDARIES.contains(boundary), () -> "Unexpected boundary: " + relative);
    }
  }

  @Test
  void domainRemainsFrameworkFreeAndApplicationDependencyDirectionIsExplicit() throws IOException {
    for (Path candidateSource : javaSources()) {
      Path source = requireNonNull(candidateSource);
      String text =
          withoutComments(requireNonNull(Files.readString(source, StandardCharsets.UTF_8)));
      String packageName = packageName(text);
      List<String> imports = imports(text);

      if (packageName.startsWith("com.alexastudillo.taxdocument.domain")) {
        assertNoImportPrefix(
            source,
            imports,
            "io.quarkus",
            "io.smallrye.mutiny",
            "io.vertx",
            "jakarta.persistence",
            "jakarta.ws.rs",
            "org.hibernate",
            "com.fasterxml.jackson",
            "java.sql");
      }
      if (packageName.startsWith("com.alexastudillo.taxdocument.application")) {
        assertNoImportPrefix(
            source,
            imports,
            "com.alexastudillo.taxdocument.api",
            "com.alexastudillo.taxdocument.infrastructure",
            "jakarta.ws.rs",
            "io.vertx");
      }
      if (packageName.startsWith("com.alexastudillo.taxdocument.api")) {
        assertNoImportPrefix(
            source,
            imports,
            "com.alexastudillo.taxdocument.infrastructure",
            "jakarta.persistence",
            "org.hibernate");
      }
      if (packageName.startsWith("com.alexastudillo.taxdocument.infrastructure")) {
        assertNoImportPrefix(source, imports, "com.alexastudillo.taxdocument.api");
      }
    }
  }

  @Test
  void prohibitedIdentityCompanyCacheAndFiscalCapabilitiesAreAbsent() throws IOException {
    for (Path candidateSource : javaSources()) {
      Path source = requireNonNull(candidateSource);
      String relative = MAIN_JAVA.relativize(source).toString().replace('\\', '/');
      String code =
          withoutComments(requireNonNull(Files.readString(source, StandardCharsets.UTF_8)));
      assertFalse(
          relative.matches(
                  ".*(?:/security/|/identity/|/company/|/cache/|/sri/|/fiscalissuance/|"
                      + "/xml/|/signing/|/certificate/|/ride/|/pdf/|/queue/|/notification/).*")
              || DECLARED_PROHIBITED_COMPONENT.matcher(code).find(),
          () -> "Prohibited capability in " + relative);
    }

    String build = Files.readString(Path.of("build.gradle.kts"), StandardCharsets.UTF_8);
    assertTrue(
        build.contains("io.quarkus:quarkus-rest-client-jackson"),
        "The approved authoritative-fiscal-context client must use the BOM-managed REST Client");
    String buildWithoutApprovedRestClient =
        build.replace("io.quarkus:quarkus-rest-client-jackson", "");
    assertFalse(
        Pattern.compile(
                "quarkus-(?:oidc|security|smallrye-jwt|keycloak|rest-client|cache|redis|kafka|amqp)|"
                    + "testcontainers",
                Pattern.CASE_INSENSITIVE)
            .matcher(buildWithoutApprovedRestClient)
            .find(),
        "Build must not introduce another REST client, identity, Company integration, cache, "
            + "broker, or a second container lifecycle");
  }

  @Test
  void featureTwoOwnedTypesAreExplicitlyNullMarkedWithoutPackageDefaults() throws IOException {
    List<Path> ownedDirectories =
        List.of(
            Path.of("src/main/java/com/alexastudillo/taxdocument/api/fiscalpreparation"),
            Path.of("src/main/java/com/alexastudillo/taxdocument/api/fiscalpreparation/telemetry"),
            Path.of("src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation"),
            Path.of("src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation"),
            Path.of(
                "src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation"));
    for (Path ownedDirectory : ownedDirectories) {
      Path packageFile = ownedDirectory.resolve("package-info.java");
      assertTrue(Files.exists(packageFile), () -> "Missing package contract " + packageFile);
      assertFalse(
          Files.readString(packageFile, StandardCharsets.UTF_8).contains("@NullMarked"),
          () -> "Package defaults are prohibited: " + packageFile);
      try (Stream<Path> files = Files.list(ownedDirectory)) {
        for (Path source :
            files
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                .sorted()
                .toList()) {
          assertTrue(
              Files.readString(source, StandardCharsets.UTF_8).contains("@NullMarked"),
              () -> "Feature 002 type is not directly null-marked: " + source);
        }
      }
    }
  }

  private static List<Path> javaSources() throws IOException {
    try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
      return requireNonNull(
          files.filter(path -> path.toString().endsWith(".java")).sorted().toList());
    }
  }

  private static String withoutComments(String source) {
    return requireNonNull(source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""));
  }

  private static String packageName(String source) {
    var matcher = Pattern.compile("(?m)^package\\s+([a-zA-Z0-9_.]+);").matcher(source);
    assertTrue(matcher.find(), "Every production source must declare a package");
    return requireNonNull(matcher.group(1));
  }

  private static List<String> imports(String source) {
    var matcher =
        Pattern.compile("(?m)^import\\s+(?:static\\s+)?([a-zA-Z0-9_.]+);").matcher(source);
    var imports = new java.util.ArrayList<String>();
    while (matcher.find()) {
      imports.add(matcher.group(1));
    }
    return requireNonNull(List.copyOf(imports));
  }

  private static void assertNoImportPrefix(
      Path source, List<String> imports, String... prohibitedPrefixes) {
    for (String imported : imports) {
      for (String prefix : prohibitedPrefixes) {
        assertFalse(
            imported.startsWith(prefix),
            () -> "Prohibited dependency " + imported + " in " + source);
      }
    }
  }
}
