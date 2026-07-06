package com.alexastudillo.taxdocument.domain.taxdocument;

/**
 * Internal lifecycle state for a tax document.
 */
public enum DocumentState {
    PENDING,
    IN_PROGRESS,
    RECEIVED,
    AUTHORIZED,
    NOT_AUTHORIZED,
    RETURNED,
    REJECTED,
    IRRECOVERABLE,
    VOIDED
}
