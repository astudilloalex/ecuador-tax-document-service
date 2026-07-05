package com.alexastudillo.taxdocument.domain.taxdocument;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DocumentStateTransitionTest {
    @Test
    void allowsCommonIssuanceTransitions() {
        assertTrue(TaxDocumentLifecycle.canTransition(DocumentState.PENDING, DocumentState.IN_PROGRESS));
        assertTrue(TaxDocumentLifecycle.canTransition(DocumentState.IN_PROGRESS, DocumentState.RECEIVED));
        assertTrue(TaxDocumentLifecycle.canTransition(DocumentState.IN_PROGRESS, DocumentState.RETURNED));
        assertTrue(TaxDocumentLifecycle.canTransition(DocumentState.RECEIVED, DocumentState.AUTHORIZED));
        assertTrue(TaxDocumentLifecycle.canTransition(DocumentState.RECEIVED, DocumentState.NOT_AUTHORIZED));
        assertTrue(TaxDocumentLifecycle.canTransition(DocumentState.RETURNED, DocumentState.IN_PROGRESS));
    }

    @Test
    void rejectsUnspecifiedTransitions() {
        assertFalse(TaxDocumentLifecycle.canTransition(DocumentState.PENDING, DocumentState.AUTHORIZED));
        assertFalse(TaxDocumentLifecycle.canTransition(DocumentState.AUTHORIZED, DocumentState.IN_PROGRESS));

        assertThrows(InvalidStateTransitionException.class,
                () -> TaxDocumentLifecycle.requireTransition(DocumentState.PENDING, DocumentState.AUTHORIZED));
    }
}
