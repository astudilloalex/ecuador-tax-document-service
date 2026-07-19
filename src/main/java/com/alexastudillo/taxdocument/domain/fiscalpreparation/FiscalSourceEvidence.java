package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Immutable authority, revision, effective interval, and observation evidence. */
@NullMarked
public record FiscalSourceEvidence(
    String authority,
    String revision,
    LocalDate effectiveFrom,
    Optional<LocalDate> effectiveThrough,
    Instant observedAt) {
  public FiscalSourceEvidence {
    requireText(authority, 128, "authority");
    requireText(revision, 128, "revision");
    Objects.requireNonNull(effectiveFrom, "effectiveFrom");
    Objects.requireNonNull(effectiveThrough, "effectiveThrough");
    Objects.requireNonNull(observedAt, "observedAt");
    if (effectiveThrough.isPresent() && effectiveThrough.orElseThrow().isBefore(effectiveFrom)) {
      throw new IllegalArgumentException("Fiscal Source Evidence effective interval is invalid");
    }
  }

  public boolean effectiveOn(LocalDate date) {
    Objects.requireNonNull(date, "date");
    return !date.isBefore(effectiveFrom)
        && effectiveThrough.map(end -> !date.isAfter(end)).orElse(true);
  }

  static void requireText(@Nullable String value, int maximum, String field) {
    if (value == null
        || value.isBlank()
        || value.length() > maximum
        || value.codePoints().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException(field + " is invalid");
    }
  }
}
