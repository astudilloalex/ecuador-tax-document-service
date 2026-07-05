package com.alexastudillo.taxdocument.domain.taxdocument;

/**
 * Raised when an access key violates the domain structure.
 */
public final class InvalidAccessKeyException extends TaxDocumentException {
    public InvalidAccessKeyException(String message) {
        super(message);
    }
}
