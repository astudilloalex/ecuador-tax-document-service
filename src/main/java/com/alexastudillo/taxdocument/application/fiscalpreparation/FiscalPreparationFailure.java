package com.alexastudillo.taxdocument.application.fiscalpreparation;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;

/** Stable transport-neutral safe failure with explicit retry and commit knowledge. */
@NullMarked
public record FiscalPreparationFailure(
    Code code, String detail, boolean retryable, CommitKnowledge commitKnowledge)
    implements Serializable {
  private static final long serialVersionUID = 1L;
  private static final Map<Code, String> DETAILS = details();

  public FiscalPreparationFailure {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(detail, "detail");
    Objects.requireNonNull(commitKnowledge, "commitKnowledge");
  }

  public static FiscalPreparationFailure of(Code code) {
    return new FiscalPreparationFailure(
        code,
        Objects.requireNonNull(DETAILS.get(code), "detail"),
        code == Code.FISCAL_CONTEXT_UNAVAILABLE
            || code == Code.REQUEST_TIMEOUT
            || code == Code.PERSISTENCE_FAILURE
            || code == Code.PREPARATION_OUTCOME_UNKNOWN,
        code == Code.PREPARATION_OUTCOME_UNKNOWN
            ? CommitKnowledge.POSSIBLE_COMMIT
            : CommitKnowledge.NOT_STARTED);
  }

  public FiscalPreparationFailure withCommitKnowledge(CommitKnowledge value) {
    return new FiscalPreparationFailure(code, detail, retryable, value);
  }

  public enum Code {
    COMPANY_CONTEXT_REQUIRED,
    COMPANY_CONTEXT_INVALID,
    INVALID_REQUEST,
    INVOICE_DRAFT_NOT_FOUND,
    INVOICE_DRAFT_NOT_PREPARABLE,
    EMISSION_DATE_STALE,
    FISCAL_CONTEXT_UNAVAILABLE,
    FISCAL_CONTEXT_INVALID,
    FISCAL_CONTEXT_UNSUPPORTED,
    FISCAL_CONTEXT_INCONSISTENT,
    OFFICIAL_SEQUENCE_BASELINE_MISSING,
    OFFICIAL_SEQUENCE_BASELINE_INVALID,
    OFFICIAL_SEQUENCE_EXHAUSTED,
    ACCESS_KEY_INVALID,
    REQUEST_TIMEOUT,
    PERSISTENCE_FAILURE,
    PREPARATION_OUTCOME_UNKNOWN,
    INTERNAL_ERROR
  }

  public enum CommitKnowledge {
    NOT_STARTED,
    CONFIRMED_ROLLBACK,
    POSSIBLE_COMMIT
  }

  private static Map<Code, String> details() {
    EnumMap<Code, String> values = new EnumMap<>(Code.class);
    values.put(Code.COMPANY_CONTEXT_REQUIRED, "X-Company-Id is required");
    values.put(Code.COMPANY_CONTEXT_INVALID, "X-Company-Id is invalid");
    values.put(Code.INVALID_REQUEST, "The Fiscal Preparation request is invalid");
    values.put(Code.INVOICE_DRAFT_NOT_FOUND, "The Invoice Draft was not found");
    values.put(Code.INVOICE_DRAFT_NOT_PREPARABLE, "The Invoice Draft is not preparable");
    values.put(Code.EMISSION_DATE_STALE, "The Invoice Draft emission date is not eligible");
    values.put(Code.FISCAL_CONTEXT_UNAVAILABLE, "Authoritative fiscal context is unavailable");
    values.put(Code.FISCAL_CONTEXT_INVALID, "Authoritative fiscal context is invalid");
    values.put(Code.FISCAL_CONTEXT_UNSUPPORTED, "Authoritative fiscal context is unsupported");
    values.put(Code.FISCAL_CONTEXT_INCONSISTENT, "Authoritative fiscal context is inconsistent");
    values.put(Code.OFFICIAL_SEQUENCE_BASELINE_MISSING, "Official Sequence Baseline is missing");
    values.put(Code.OFFICIAL_SEQUENCE_BASELINE_INVALID, "Official Sequence Baseline is invalid");
    values.put(Code.OFFICIAL_SEQUENCE_EXHAUSTED, "Official Sequence Baseline is exhausted");
    values.put(Code.ACCESS_KEY_INVALID, "Access Key validation failed");
    values.put(
        Code.REQUEST_TIMEOUT,
        "The Fiscal Preparation request exceeded its deadline; retry the same Company and Invoice Draft");
    values.put(Code.PERSISTENCE_FAILURE, "Fiscal Preparation persistence failed");
    values.put(
        Code.PREPARATION_OUTCOME_UNKNOWN,
        "Fiscal Preparation outcome is unknown; retry the same Company and Invoice Draft");
    values.put(Code.INTERNAL_ERROR, "Fiscal Preparation failed");
    return Map.copyOf(values);
  }
}
