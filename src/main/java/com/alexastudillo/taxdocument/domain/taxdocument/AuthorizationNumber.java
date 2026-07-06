package com.alexastudillo.taxdocument.domain.taxdocument;

/**
 * Authorization identifier assigned when a tax document is authorized.
 */
public record AuthorizationNumber(String value) {
    public AuthorizationNumber {
        requireNotBlank(value, "authorizationNumber");
    }

    private static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
