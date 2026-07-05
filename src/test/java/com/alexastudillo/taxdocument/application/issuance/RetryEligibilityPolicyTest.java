package com.alexastudillo.taxdocument.application.issuance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.domain.taxdocument.DocumentState;
import org.junit.jupiter.api.Test;

class RetryEligibilityPolicyTest {
    @Test
    void identifiesRetryCandidateStatesWithDeferredValidation() {
        RetryEligibilityPolicy policy = new RetryEligibilityPolicy();

        for (DocumentState candidate : new DocumentState[] {
                DocumentState.RETURNED,
                DocumentState.REJECTED,
                DocumentState.PENDING,
                DocumentState.IN_PROGRESS
        }) {
            RetryEligibilityPolicy.Result result = policy.evaluate(candidate);
            assertTrue(result.candidateState());
            assertTrue(result.requiresFunctionalValidation());
            assertEquals("PFV-ISS-004", result.functionalValidationId().orElseThrow());
        }
    }

    @Test
    void rejectsTerminalOrAuthorizedStatesAsRetryCandidates() {
        RetryEligibilityPolicy policy = new RetryEligibilityPolicy();

        assertFalse(policy.evaluate(DocumentState.AUTHORIZED).candidateState());
        assertFalse(policy.evaluate(DocumentState.VOIDED).candidateState());
        assertFalse(policy.evaluate(DocumentState.IRRECOVERABLE).candidateState());
    }
}
