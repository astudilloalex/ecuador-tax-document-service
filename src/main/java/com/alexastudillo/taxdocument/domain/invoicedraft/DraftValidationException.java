package com.alexastudillo.taxdocument.domain.invoicedraft;

/** Stable framework-independent business validation failure. */
public final class DraftValidationException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final String code;
  private final String field;

  public DraftValidationException(String code, String message) {
    this(code, null, message);
  }

  public DraftValidationException(String code, String field, String message) {
    super(message);
    this.code = code;
    this.field = field;
  }

  public String code() {
    return code;
  }

  public String field() {
    return field;
  }
}
