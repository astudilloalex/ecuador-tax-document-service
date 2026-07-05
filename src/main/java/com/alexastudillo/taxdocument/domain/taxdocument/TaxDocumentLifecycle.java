package com.alexastudillo.taxdocument.domain.taxdocument;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Common state transition policy for tax document issuance.
 */
public final class TaxDocumentLifecycle {
    private static final Map<DocumentState, Set<DocumentState>> ALLOWED_TRANSITIONS = new EnumMap<>(DocumentState.class);

    static {
        ALLOWED_TRANSITIONS.put(DocumentState.PENDING, EnumSet.of(DocumentState.IN_PROGRESS, DocumentState.VOIDED));
        ALLOWED_TRANSITIONS.put(DocumentState.IN_PROGRESS, EnumSet.of(
                DocumentState.RECEIVED,
                DocumentState.RETURNED,
                DocumentState.REJECTED,
                DocumentState.VOIDED));
        ALLOWED_TRANSITIONS.put(DocumentState.RECEIVED, EnumSet.of(
                DocumentState.AUTHORIZED,
                DocumentState.NOT_AUTHORIZED,
                DocumentState.REJECTED));
        ALLOWED_TRANSITIONS.put(DocumentState.RETURNED, EnumSet.of(
                DocumentState.IN_PROGRESS,
                DocumentState.VOIDED,
                DocumentState.IRRECOVERABLE));
        ALLOWED_TRANSITIONS.put(DocumentState.REJECTED, EnumSet.of(
                DocumentState.IN_PROGRESS,
                DocumentState.VOIDED,
                DocumentState.IRRECOVERABLE));
        ALLOWED_TRANSITIONS.put(DocumentState.NOT_AUTHORIZED, EnumSet.of(
                DocumentState.VOIDED,
                DocumentState.IRRECOVERABLE));
        ALLOWED_TRANSITIONS.put(DocumentState.AUTHORIZED, EnumSet.noneOf(DocumentState.class));
        ALLOWED_TRANSITIONS.put(DocumentState.VOIDED, EnumSet.noneOf(DocumentState.class));
        ALLOWED_TRANSITIONS.put(DocumentState.IRRECOVERABLE, EnumSet.noneOf(DocumentState.class));
    }

    private TaxDocumentLifecycle() {
    }

    public static boolean canTransition(DocumentState from, DocumentState to) {
        if (from == null || to == null) {
            return false;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static void requireTransition(DocumentState from, DocumentState to) {
        if (!canTransition(from, to)) {
            throw new InvalidStateTransitionException(from, to);
        }
    }
}
