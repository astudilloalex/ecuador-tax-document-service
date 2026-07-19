package com.alexastudillo.taxdocument.application.invoicedraft;

import java.util.UUID;

/** Generates only local non-fiscal aggregate and child identifiers. */
public interface DraftIdentifierGenerator {
  UUID nextIdentifier();
}
