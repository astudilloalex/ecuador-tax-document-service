package com.alexastudillo.taxdocument.application.error;

/**
 * Stable application-layer categories for persistence failures.
 */
public enum PersistenceFailureCategory {
    DUPLICATE_ACCESS_KEY_CONFLICT("DuplicateAccessKeyConflict"),
    DUPLICATE_ISSUANCE_IDENTITY_CONFLICT("DuplicateIssuanceIdentityConflict"),
    UNAVAILABLE_SEQUENCE_RESERVATION_CONFLICT("UnavailableSequenceReservationConflict"),
    INVALID_PERSISTED_TAX_DOCUMENT_STATE("InvalidPersistedTaxDocumentState"),
    INVALID_PERSISTENCE_RELATIONSHIP("InvalidPersistenceRelationship"),
    GENERIC_PERSISTENCE_FAILURE("GenericPersistenceFailure"),
    PERSISTENCE_TRANSACTION_FAILURE("PersistenceTransactionFailure");

    private final String stableName;

    PersistenceFailureCategory(String stableName) {
        this.stableName = stableName;
    }

    public String stableName() {
        return stableName;
    }
}
