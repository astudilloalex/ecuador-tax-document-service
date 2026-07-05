package com.alexastudillo.taxdocument.domain.taxdocument;

import java.util.Objects;
import java.util.Optional;

/**
 * Common aggregate for tax document issuance lifecycle behavior.
 */
public final class TaxDocument {
    private final DocumentType documentType;
    private final Issuer issuer;
    private final Establishment establishment;
    private final IssuingPoint issuingPoint;
    private final SequenceNumber sequenceNumber;
    private final AccessKey accessKey;
    private final IssueDate issueDate;
    private final IssuanceMode issuanceMode;
    private final String externalRequestId;
    private DocumentState documentState;
    private AuthorizationState authorizationState;
    private AuthorizationNumber authorizationNumber;
    private AuthorizedAt authorizedAt;

    public TaxDocument(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            SequenceNumber sequenceNumber,
            AccessKey accessKey,
            IssueDate issueDate,
            IssuanceMode issuanceMode,
            String externalRequestId) {
        this.documentType = Objects.requireNonNull(documentType, "documentType must not be null");
        this.issuer = Objects.requireNonNull(issuer, "issuer must not be null");
        this.establishment = Objects.requireNonNull(establishment, "establishment must not be null");
        this.issuingPoint = Objects.requireNonNull(issuingPoint, "issuingPoint must not be null");
        this.sequenceNumber = Objects.requireNonNull(sequenceNumber, "sequenceNumber must not be null");
        this.accessKey = Objects.requireNonNull(accessKey, "accessKey must not be null");
        this.issueDate = Objects.requireNonNull(issueDate, "issueDate must not be null");
        this.issuanceMode = Objects.requireNonNull(issuanceMode, "issuanceMode must not be null");
        this.externalRequestId = normalizeExternalRequestId(externalRequestId);
        this.documentState = DocumentState.PENDING;
        this.authorizationState = AuthorizationState.NOT_SUBMITTED;
        validateIssuanceIdentity();
    }

    private static String normalizeExternalRequestId(String externalRequestId) {
        if (externalRequestId == null || externalRequestId.isBlank()) {
            return null;
        }
        return externalRequestId;
    }

    private void validateIssuanceIdentity() {
        if (!sequenceNumber.documentType().equals(documentType)) {
            throw new IllegalArgumentException("sequenceNumber documentType must match taxDocument documentType");
        }
        if (!sequenceNumber.issuer().equals(issuer)) {
            throw new IllegalArgumentException("sequenceNumber issuer must match taxDocument issuer");
        }
        if (!sequenceNumber.establishment().equals(establishment)) {
            throw new IllegalArgumentException("sequenceNumber establishment must match taxDocument establishment");
        }
        if (!sequenceNumber.issuingPoint().equals(issuingPoint)) {
            throw new IllegalArgumentException("sequenceNumber issuingPoint must match taxDocument issuingPoint");
        }
    }

    public DocumentType documentType() {
        return documentType;
    }

    public Issuer issuer() {
        return issuer;
    }

    public Establishment establishment() {
        return establishment;
    }

    public IssuingPoint issuingPoint() {
        return issuingPoint;
    }

    public SequenceNumber sequenceNumber() {
        return sequenceNumber;
    }

    public AccessKey accessKey() {
        return accessKey;
    }

    public IssueDate issueDate() {
        return issueDate;
    }

    public IssuanceMode issuanceMode() {
        return issuanceMode;
    }

    public Optional<String> externalRequestId() {
        return Optional.ofNullable(externalRequestId);
    }

    public DocumentState documentState() {
        return documentState;
    }

    public AuthorizationState authorizationState() {
        return authorizationState;
    }

    public Optional<AuthorizationNumber> authorizationNumber() {
        return Optional.ofNullable(authorizationNumber);
    }

    public Optional<AuthorizedAt> authorizedAt() {
        return Optional.ofNullable(authorizedAt);
    }

    public void transitionTo(DocumentState targetState) {
        Objects.requireNonNull(targetState, "targetState must not be null");
        if (documentState == DocumentState.AUTHORIZED && targetState != DocumentState.AUTHORIZED) {
            throw new ImmutableAuthorizedDocumentException();
        }
        TaxDocumentLifecycle.requireTransition(documentState, targetState);
        documentState = targetState;
        synchronizeAuthorizationState(targetState);
    }

    public void authorize(AuthorizationNumber authorizationNumber, AuthorizedAt authorizedAt) {
        Objects.requireNonNull(authorizationNumber, "authorizationNumber must not be null");
        Objects.requireNonNull(authorizedAt, "authorizedAt must not be null");
        transitionTo(DocumentState.AUTHORIZED);
        this.authorizationNumber = authorizationNumber;
        this.authorizedAt = authorizedAt;
        this.authorizationState = AuthorizationState.AUTHORIZED;
    }

    public void voidLocally() {
        if (documentState == DocumentState.AUTHORIZED) {
            throw new ImmutableAuthorizedDocumentException();
        }
        transitionTo(DocumentState.VOIDED);
    }

    private void synchronizeAuthorizationState(DocumentState targetState) {
        authorizationState = switch (targetState) {
            case PENDING -> AuthorizationState.NOT_SUBMITTED;
            case IN_PROGRESS -> AuthorizationState.SUBMITTED;
            case RECEIVED -> AuthorizationState.RECEIVED;
            case AUTHORIZED -> AuthorizationState.AUTHORIZED;
            case NOT_AUTHORIZED -> AuthorizationState.NOT_AUTHORIZED;
            case RETURNED -> AuthorizationState.RETURNED;
            case REJECTED, IRRECOVERABLE, VOIDED -> AuthorizationState.REJECTED;
        };
    }
}
