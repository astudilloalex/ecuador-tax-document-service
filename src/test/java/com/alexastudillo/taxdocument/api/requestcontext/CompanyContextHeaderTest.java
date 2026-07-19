package com.alexastudillo.taxdocument.api.requestcontext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alexastudillo.taxdocument.api.problem.ProblemDetails;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@NullMarked
class CompanyContextHeaderTest {
  private final CompanyContextHeader header = new CompanyContextHeader();

  @Test
  void acceptsExactlyOneAsciiTrimmedUuidAndReturnsItsCanonicalCompanyIdentity() {
    assertEquals(
        "123e4567-e89b-12d3-a456-426614174000",
        header.parse(List.of("\t123E4567-E89B-12D3-A456-426614174000 ")).toString());
  }

  @Test
  void rejectsEveryNonSingleNonCanonicalOrZeroAccessCompanyContextBeforeUseCaseAccess() {
    assertEquals("COMPANY_CONTEXT_REQUIRED", failure(null).code());
    assertEquals("COMPANY_CONTEXT_REQUIRED", failure(List.of()).code());
    assertEquals("COMPANY_CONTEXT_INVALID", failure(List.of("a", "b")).code());
    assertEquals(
        "COMPANY_CONTEXT_INVALID",
        failure(
                List.of(
                    "123e4567-e89b-12d3-a456-426614174000,"
                        + "123e4567-e89b-12d3-a456-426614174001"))
            .code());
    assertEquals("COMPANY_CONTEXT_INVALID", failure(List.of(" ")).code());
    assertEquals("COMPANY_CONTEXT_INVALID", failure(List.of("not-a-uuid")).code());
    assertEquals(
        "COMPANY_CONTEXT_INVALID", failure(List.of("00000000-0000-0000-0000-000000000000")).code());
    assertEquals(
        "COMPANY_CONTEXT_INVALID",
        failure(List.of(" 123e4567-e89b-12d3-a456-426614174000 x ")).code());
  }

  private ProblemDetails.ApiException failure(@Nullable List<String> values) {
    return assertThrows(ProblemDetails.ApiException.class, () -> header.parse(values));
  }
}
