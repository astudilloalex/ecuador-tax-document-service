package com.alexastudillo.taxdocument.domain.taxdocument;

/**
 * Legal issuer context for tax document issuance.
 */
public record Issuer(String issuerId, String legalIdentifier, String legalName, String tradeName) {
    public Issuer {
        requireNotBlank(issuerId, "issuerId");
        requireNotBlank(legalIdentifier, "legalIdentifier");
    }

    private static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
