package com.alexastudillo.taxdocument.domain.taxdocument;

/**
 * Raised when a tax document state transition is not allowed.
 */
public final class InvalidStateTransitionException extends TaxDocumentException {
    public InvalidStateTransitionException(DocumentState from, DocumentState to) {
        super("Cannot transition tax document from " + from + " to " + to);
    }
}
