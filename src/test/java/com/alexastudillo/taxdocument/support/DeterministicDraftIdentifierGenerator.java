package com.alexastudillo.taxdocument.support;

import com.alexastudillo.taxdocument.application.invoicedraft.DraftIdentifierGenerator;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.NullMarked;

/** Resettable test-only deterministic local identifier sequence. */
@Alternative
@Priority(1)
@Singleton
@NullMarked
public final class DeterministicDraftIdentifierGenerator implements DraftIdentifierGenerator {
  private final AtomicLong sequence = new AtomicLong(1L);

  @Override
  public UUID nextIdentifier() {
    return new UUID(0x123456789abcdef0L, sequence.getAndIncrement());
  }

  public void reset() {
    sequence.set(1L);
  }
}
