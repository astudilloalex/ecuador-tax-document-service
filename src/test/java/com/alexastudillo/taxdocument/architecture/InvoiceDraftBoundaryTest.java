package com.alexastudillo.taxdocument.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class InvoiceDraftBoundaryTest {
  @Test
  void domainHasNoFrameworkTransportPersistenceOrIdentityDependency() throws Exception {
    Path domain = Path.of("src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft");
    try (Stream<Path> files = Files.list(domain)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file);
        String code = source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
        assertFalse(code.contains("jakarta.ws.rs"), file.toString());
        assertFalse(code.contains("io.quarkus"), file.toString());
        assertFalse(code.contains("Panache"), file.toString());
        assertFalse(code.contains("Uni<"), file.toString());
      }
    }
  }

  @Test
  void invoiceDraftCapabilityContainsNoAuthenticationCompanyClientCacheOrFiscalIdentity()
      throws Exception {
    List<Path> capabilityRoots =
        List.of(
            Path.of("src/main/java/com/alexastudillo/taxdocument/api/invoicedraft"),
            Path.of("src/main/java/com/alexastudillo/taxdocument/application/invoicedraft"),
            Path.of("src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft"),
            Path.of("src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft"));
    List<String> names = new java.util.ArrayList<>();
    for (Path capabilityRoot : capabilityRoots) {
      try (Stream<Path> files = Files.walk(capabilityRoot)) {
        names.addAll(
            files.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).toList());
      }
    }
    assertFalse(
        names.stream()
            .anyMatch(name -> name.matches(".*(Auth|Issuer|Certificate|SriClient|Cache).*")));
    assertTrue(
        Files.exists(
            Path.of(
                "src/main/java/com/alexastudillo/taxdocument/api/requestcontext/CompanyContextHeader.java")));
    assertFalse(names.contains("CompanyRepository.java"));

    for (Path capability : capabilityRoots) {
      try (Stream<Path> files = Files.walk(capability)) {
        for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
          String source = Files.readString(file);
          assertFalse(
              source.matches(
                  "(?s).*(FiscalPreparation|FiscalContextSnapshot|OfficialSequentialNumber|"
                      + "NumericCode|AccessKey|OfficialSequenceBaseline).*$"),
              file.toString());
        }
      }
    }
  }
}
