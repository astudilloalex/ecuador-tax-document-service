package com.alexastudillo.taxdocument.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class InvoiceDraftBoundaryTest {
  @Test
  void domainHasNoFrameworkTransportPersistenceOrIdentityDependency() throws Exception {
    Path domain = Path.of("src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft");
    for (Path file :
        Files.list(domain).filter(path -> path.toString().endsWith(".java")).toList()) {
      String source = Files.readString(file);
      String code = source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
      assertFalse(code.contains("jakarta.ws.rs"), file.toString());
      assertFalse(code.contains("io.quarkus"), file.toString());
      assertFalse(code.contains("Panache"), file.toString());
      assertFalse(code.contains("Uni<"), file.toString());
    }
  }

  @Test
  void featureContainsNoAuthenticationCompanyClientCacheOrFiscalIssuanceAdapter() throws Exception {
    List<String> names;
    try (var files = Files.walk(Path.of("src/main/java"))) {
      names =
          files.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).toList();
    }
    assertFalse(
        names.stream()
            .anyMatch(name -> name.matches(".*(Auth|Issuer|Certificate|SriClient|Cache).*")));
    assertTrue(names.contains("CompanyContextHeader.java"));
    assertFalse(names.contains("CompanyRepository.java"));
  }
}
