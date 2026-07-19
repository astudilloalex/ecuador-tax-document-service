package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class FiscalIdentityValueTest {
  @Test
  void officialSequentialNumberEnforcesTheExactNineDigitOfficialRange() {
    assertEquals("000000001", OfficialSequentialNumber.of(1).value());
    assertEquals("999999999", OfficialSequentialNumber.of(999_999_999).value());
    assertEquals(123, OfficialSequentialNumber.parse("000000123").number());
    assertThrows(IllegalArgumentException.class, () -> OfficialSequentialNumber.of(0));
    assertThrows(IllegalArgumentException.class, () -> OfficialSequentialNumber.of(1_000_000_000));
    assertThrows(IllegalArgumentException.class, () -> OfficialSequentialNumber.parse("00000001"));
    assertThrows(IllegalArgumentException.class, () -> OfficialSequentialNumber.parse("00000000ñ"));
  }

  @Test
  void numericCodeRetainsEveryLeadingZeroIncludingAllZeroes() {
    assertEquals("00000000", NumericCode.of(0).value());
    assertEquals("00000007", NumericCode.of(7).value());
    assertEquals("99999999", NumericCode.of(99_999_999).value());
    assertThrows(IllegalArgumentException.class, () -> NumericCode.of(-1));
    assertThrows(IllegalArgumentException.class, () -> NumericCode.parse("1234567"));
    assertThrows(IllegalArgumentException.class, () -> NumericCode.parse("１２３４５６７８"));
  }

  @Test
  void exactScopeUsesIssuerEstablishmentEmissionPointAndInvoiceTypeOnly() {
    UUID emissionPoint = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    OfficialSequenceScope first =
        new OfficialSequenceScope("issuer-1", "establishment-1", emissionPoint, "001", "002", "01");
    assertEquals(
        first,
        new OfficialSequenceScope(
            "issuer-1", "establishment-1", emissionPoint, "001", "002", "01"));
    assertNotEquals(
        first,
        new OfficialSequenceScope(
            "issuer-1", "establishment-1", emissionPoint, "001", "003", "01"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new OfficialSequenceScope(
                "issuer-1", "establishment-1", emissionPoint, "1", "002", "01"));
  }
}
