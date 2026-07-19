package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/** Exact issuer, establishment, emission point, and invoice document-type numbering scope. */
@NullMarked
public record OfficialSequenceScope(
    String issuerReference,
    String establishmentReference,
    UUID emissionPointId,
    String establishmentCode,
    String emissionPointCode,
    String documentTypeCode) {
  private static final Pattern THREE_DIGITS = Objects.requireNonNull(Pattern.compile("^[0-9]{3}$"));

  public OfficialSequenceScope {
    FiscalSourceEvidence.requireText(issuerReference, 128, "issuerReference");
    FiscalSourceEvidence.requireText(establishmentReference, 128, "establishmentReference");
    FiscalContextSnapshot.requireNonNil(emissionPointId, "emissionPointId");
    if (establishmentCode == null || !THREE_DIGITS.matcher(establishmentCode).matches()) {
      throw new IllegalArgumentException("Establishment Code is invalid");
    }
    if (emissionPointCode == null || !THREE_DIGITS.matcher(emissionPointCode).matches()) {
      throw new IllegalArgumentException("Emission Point Code is invalid");
    }
    if (!AccessKeyGenerator.INVOICE_DOCUMENT_TYPE.equals(documentTypeCode)) {
      throw new IllegalArgumentException("Official Sequence Scope supports invoices only");
    }
  }
}
