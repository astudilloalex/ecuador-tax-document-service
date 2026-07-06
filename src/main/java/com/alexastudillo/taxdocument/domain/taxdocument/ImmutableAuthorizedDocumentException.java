package com.alexastudillo.taxdocument.domain.taxdocument;

/**
 * Raised when a terminal authorized document would be modified.
 */
public final class ImmutableAuthorizedDocumentException extends TaxDocumentException {
    public ImmutableAuthorizedDocumentException() {
        super("Authorized tax documents are immutable");
    }
}
