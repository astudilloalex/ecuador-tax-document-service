package com.alexastudillo.taxdocument.domain.invoicedraft;

import org.jspecify.annotations.Nullable;

/** Stable framework-independent business validation failure. */
public final class DraftValidationException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final String code;
  private final @Nullable String field;

  public DraftValidationException(String code, String message) {
    this(code, null, message);
  }

  public DraftValidationException(String code, @Nullable String field, String message) {
    super(message);
    this.code = code;
    this.field = field;
  }

  public String code() {
    return code;
  }

  public @Nullable String field() {
    return field;
  }
}
