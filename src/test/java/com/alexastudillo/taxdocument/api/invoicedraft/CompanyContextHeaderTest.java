package com.alexastudillo.taxdocument.api.invoicedraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class CompanyContextHeaderTest {
  private final CompanyContextHeader header = new CompanyContextHeader();

  @Test
  void acceptsExactlyOneTrimmedMixedCaseUuidAndCanonicalizesIt() {
    assertEquals(
        "123e4567-e89b-12d3-a456-426614174000",
        header.parse(List.of("\t123E4567-E89B-12D3-A456-426614174000 ")).toString());
  }

  @Test
  void missingRepeatedMalformedAndNilAreRejected() {
    assertEquals("COMPANY_CONTEXT_REQUIRED", failure(null).code());
    assertEquals("COMPANY_CONTEXT_INVALID", failure(List.of("a", "b")).code());
    assertEquals("COMPANY_CONTEXT_INVALID", failure(List.of("not-a-uuid")).code());
    assertEquals(
        "COMPANY_CONTEXT_INVALID", failure(List.of("00000000-0000-0000-0000-000000000000")).code());
  }

  private ProblemDetails.ApiException failure(List<String> values) {
    return assertThrows(ProblemDetails.ApiException.class, () -> header.parse(values));
  }
}
