package com.alexastudillo.taxdocument.domain.taxdocument;

/**
 * Issuer establishment used in tax document issuance identity.
 */
public record Establishment(String establishmentId, String code, String issuerId) {
    public Establishment {
        requireNotBlank(establishmentId, "establishmentId");
        requireNotBlank(code, "code");
        requireNotBlank(issuerId, "issuerId");
    }

    private static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
