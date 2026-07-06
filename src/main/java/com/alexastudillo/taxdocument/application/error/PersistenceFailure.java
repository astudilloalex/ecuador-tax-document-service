package com.alexastudillo.taxdocument.application.error;

import java.util.Objects;

/**
 * Framework-free application error raised by persistence port implementations.
 */
public final class PersistenceFailure extends RuntimeException {
    private final PersistenceFailureCategory category;

    public PersistenceFailure(PersistenceFailureCategory category, String message) {
        super(requireMessage(message));
        this.category = Objects.requireNonNull(category, "category must not be null");
    }

    public PersistenceFailureCategory category() {
        return category;
    }

    private static String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        return message;
    }
}
