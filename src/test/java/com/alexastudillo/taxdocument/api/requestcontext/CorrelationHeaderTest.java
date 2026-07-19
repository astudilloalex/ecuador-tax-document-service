package com.alexastudillo.taxdocument.api.requestcontext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class CorrelationHeaderTest {
  private final CorrelationHeader header = new CorrelationHeader();

  @Test
  void preservesOnlyOneSafeValueAfterOneAsciiSpaceAndTabTrim() {
    CorrelationHeader.Classification result = header.classify(List.of("\t corr:42 \t"));
    assertTrue(result.validInput());
    assertEquals("corr:42", result.safeValue());
  }

  @Test
  void absentCreatesSafeCorrelationAndEveryUnsafeFormIsReplacedWithoutEcho() {
    CorrelationHeader.Classification absent = header.classify(null);
    assertTrue(absent.validInput());
    UUID.fromString(absent.safeValue());

    for (List<String> values :
        List.of(
            List.of("   "),
            List.of("unsafe value"),
            List.of("unsafe/value"),
            List.of("ñ"),
            List.of("x".repeat(65)),
            List.of("first,second"),
            List.of("first", "second"))) {
      CorrelationHeader.Classification invalid = header.classify(values);
      assertFalse(invalid.validInput());
      UUID.fromString(invalid.safeValue());
      assertNotEquals(values.getFirst(), invalid.safeValue());
    }
  }
}
