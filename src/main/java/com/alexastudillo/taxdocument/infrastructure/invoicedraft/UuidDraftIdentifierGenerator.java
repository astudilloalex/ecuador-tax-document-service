package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import com.alexastudillo.taxdocument.application.invoicedraft.DraftIdentifierGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/** Random generator for local non-fiscal draft and child identifiers only. */
@ApplicationScoped
@NullMarked
public final class UuidDraftIdentifierGenerator implements DraftIdentifierGenerator {
  @Override
  public UUID nextIdentifier() {
    return Objects.requireNonNull(UUID.randomUUID());
  }
}
