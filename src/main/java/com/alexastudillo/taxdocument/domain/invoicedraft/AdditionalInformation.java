package com.alexastudillo.taxdocument.domain.invoicedraft;

import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Additional information after Application normalization and canonical-name derivation. */
public record AdditionalInformation(
    UUID id, int position, String name, @Nullable String canonicalName, String value) {
  public AdditionalInformation {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(value, "value");
    if (position < 1 || position > 15) {
      throw invalid("additionalInformation[].position");
    }
    requireLength(name, "additionalInformation[].name");
    if (canonicalName != null) {
      requireLength(canonicalName, "additionalInformation[].name");
    }
    requireLength(value, "additionalInformation[].value");
  }

  private static void requireLength(String value, String field) {
    int length = value.codePointCount(0, value.length());
    if (length < 1 || length > 300) {
      throw invalid(field);
    }
  }

  private static DraftValidationException invalid(String field) {
    return new DraftValidationException(
        "BUSINESS_VALIDATION_FAILED", field, "Additional information is invalid");
  }
}
