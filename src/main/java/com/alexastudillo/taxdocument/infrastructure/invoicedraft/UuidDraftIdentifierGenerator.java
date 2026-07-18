package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import com.alexastudillo.taxdocument.application.invoicedraft.DraftIdentifierGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/** Random generator for local non-fiscal draft and child identifiers only. */
@ApplicationScoped
public final class UuidDraftIdentifierGenerator implements DraftIdentifierGenerator {
  @Override
  public UUID nextIdentifier() {
    return UUID.randomUUID();
  }
}
