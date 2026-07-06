package com.alexastudillo.taxdocument.domain.taxdocument;

/**
 * Internal authorization lifecycle state, separate from document state.
 */
public enum AuthorizationState {
    NOT_SUBMITTED,
    SUBMITTED,
    RECEIVED,
    AUTHORIZED,
    NOT_AUTHORIZED,
    RETURNED,
    REJECTED
}
