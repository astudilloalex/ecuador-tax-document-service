package com.alexastudillo.taxdocument.application.issuance;

import com.alexastudillo.taxdocument.domain.taxdocument.DocumentState;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Application policy for retry candidate states.
 */
public final class RetryEligibilityPolicy {
    public static final String RETRY_POLICY_VALIDATION_ID = "PFV-ISS-004";

    private static final Set<DocumentState> RETRY_CANDIDATES = EnumSet.of(
            DocumentState.RETURNED,
            DocumentState.REJECTED,
            DocumentState.PENDING,
            DocumentState.IN_PROGRESS);

    public Result evaluate(DocumentState documentState) {
        Objects.requireNonNull(documentState, "documentState must not be null");
        boolean candidateState = RETRY_CANDIDATES.contains(documentState);
        return new Result(
                candidateState,
                candidateState,
                candidateState ? Optional.of(RETRY_POLICY_VALIDATION_ID) : Optional.empty());
    }

    public record Result(
            boolean candidateState,
            boolean requiresFunctionalValidation,
            Optional<String> functionalValidationId) {
        public Result {
            Objects.requireNonNull(functionalValidationId, "functionalValidationId must not be null");
        }
    }
}
