package com.alexastudillo.taxdocument.application.issuance;

import java.util.Objects;

/**
 * Application-level error returned by future issuance use cases.
 */
public record IssuanceError(Code code, String message) {
    public IssuanceError {
        Objects.requireNonNull(code, "code must not be null");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }

    public enum Code {
        ISSUER_ACCESS_DENIED,
        DUPLICATE_ISSUANCE_IDENTITY,
        SEQUENCE_UNAVAILABLE,
        RETRY_INELIGIBLE,
        IDEMPOTENCY_CONFLICT,
        EXTERNAL_DEPENDENCY_RESULT_MISSING
    }
}
