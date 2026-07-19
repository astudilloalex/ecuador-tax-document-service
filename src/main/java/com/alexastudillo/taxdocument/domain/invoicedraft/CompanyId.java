package com.alexastudillo.taxdocument.domain.invoicedraft;

import java.util.Objects;
import java.util.UUID;

/** Immutable, opaque Company ownership identifier. */
public record CompanyId(UUID value) {
  private static final UUID NIL = new UUID(0L, 0L);

  public CompanyId {
    Objects.requireNonNull(value, "value");
    if (NIL.equals(value)) {
      throw new DraftValidationException("COMPANY_CONTEXT_INVALID", "CompanyId must not be nil");
    }
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
