package com.alexastudillo.taxdocument.domain.fiscalpreparation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Immutable authority, revision, effective interval, and observation evidence. */
@NullMarked
public record FiscalSourceEvidence(
    String authority,
    String revision,
    LocalDate effectiveFrom,
    Optional<@NonNull LocalDate> effectiveThrough,
    Instant observedAt) {
  public FiscalSourceEvidence(
      String authority,
      String revision,
      LocalDate effectiveFrom,
      Optional<@NonNull LocalDate> effectiveThrough,
      Instant observedAt) {
    requireText(authority, 128, "authority");
    requireText(revision, 128, "revision");
    Objects.requireNonNull(effectiveFrom, "effectiveFrom");
    Objects.requireNonNull(effectiveThrough, "effectiveThrough");
    Objects.requireNonNull(observedAt, "observedAt");
    if (effectiveThrough.isPresent()) {
      @Nullable LocalDate nullableEnd = effectiveThrough.get();
      LocalDate end = Objects.requireNonNull(nullableEnd, "effectiveThrough value");
      if (end.isBefore(effectiveFrom)) {
        throw new IllegalArgumentException("Fiscal Source Evidence effective interval is invalid");
      }
    }
    this.authority = authority;
    this.revision = revision;
    this.effectiveFrom = effectiveFrom;
    this.effectiveThrough = effectiveThrough;
    this.observedAt = observedAt;
  }

  public boolean effectiveOn(LocalDate date) {
    Objects.requireNonNull(date, "date");
    if (date.isBefore(effectiveFrom) || effectiveThrough.isEmpty()) {
      return !date.isBefore(effectiveFrom);
    }
    @Nullable LocalDate nullableEnd = effectiveThrough.get();
    LocalDate end = Objects.requireNonNull(nullableEnd, "effectiveThrough value");
    return !date.isAfter(end);
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
