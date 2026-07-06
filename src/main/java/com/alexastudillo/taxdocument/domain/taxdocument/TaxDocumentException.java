package com.alexastudillo.taxdocument.domain.taxdocument;

/**
 * Base exception for tax document domain invariant violations.
 */
public class TaxDocumentException extends RuntimeException {
    public TaxDocumentException(String message) {
        super(message);
    }
}
