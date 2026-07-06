package com.alexastudillo.taxdocument.application.issuance;

import com.alexastudillo.taxdocument.domain.taxdocument.AccessKey;
import com.alexastudillo.taxdocument.domain.taxdocument.AuthorizationNumber;
import com.alexastudillo.taxdocument.domain.taxdocument.AuthorizationState;
import com.alexastudillo.taxdocument.domain.taxdocument.AuthorizedAt;
import com.alexastudillo.taxdocument.domain.taxdocument.DocumentState;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Common application result model for future issuance use cases.
 */
public final class IssuanceResult {
    private final AccessKey accessKey;
    private final DocumentState documentState;
    private final AuthorizationState authorizationState;
    private final AuthorizationNumber authorizationNumber;
    private final AuthorizedAt authorizedAt;
    private final Outcome outcome;
    private final String auditCorrelationId;
    private final List<IssuanceError> errors;

    public IssuanceResult(
            AccessKey accessKey,
            DocumentState documentState,
            AuthorizationState authorizationState,
            AuthorizationNumber authorizationNumber,
            AuthorizedAt authorizedAt,
            Outcome outcome,
            String auditCorrelationId,
            List<IssuanceError> errors) {
        this.accessKey = Objects.requireNonNull(accessKey, "accessKey must not be null");
        this.documentState = Objects.requireNonNull(documentState, "documentState must not be null");
        this.authorizationState = Objects.requireNonNull(authorizationState, "authorizationState must not be null");
        this.authorizationNumber = authorizationNumber;
        this.authorizedAt = authorizedAt;
        this.outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        this.auditCorrelationId = normalize(auditCorrelationId);
        this.errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    public AccessKey accessKey() {
        return accessKey;
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

    public Outcome outcome() {
        return outcome;
    }

    public Optional<String> auditCorrelationId() {
        return Optional.ofNullable(auditCorrelationId);
    }

    public List<IssuanceError> errors() {
        return errors;
    }

    public enum Outcome {
        QUEUED,
        COMPLETED,
        FAILED
    }
}
