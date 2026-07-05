package com.alexastudillo.taxdocument.application.issuance;

/**
 * Canonical audit event names for future issuance flows.
 */
public enum AuditEventName {
    ISSUANCE_REQUESTED("TaxDocumentIssuanceRequested"),
    QUEUED_FOR_ISSUANCE("TaxDocumentQueuedForIssuance"),
    XML_GENERATED("TaxDocumentXmlGenerated"),
    SIGNED("TaxDocumentSigned"),
    SUBMITTED_TO_SRI("TaxDocumentSubmittedToSri"),
    RECEIVED_BY_SRI("TaxDocumentReceivedBySri"),
    AUTHORIZED("TaxDocumentAuthorized"),
    REJECTED("TaxDocumentRejected"),
    AUTHORIZATION_RETRY_REQUESTED("TaxDocumentAuthorizationRetryRequested"),
    VOIDED("TaxDocumentVoided");

    private final String value;

    AuditEventName(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
