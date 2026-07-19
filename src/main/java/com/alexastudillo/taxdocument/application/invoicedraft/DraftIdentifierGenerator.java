package com.alexastudillo.taxdocument.application.invoicedraft;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/** Generates only local non-fiscal aggregate and child identifiers. */
@NullMarked
public interface DraftIdentifierGenerator {
  UUID nextIdentifier();
}
