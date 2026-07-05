package com.alexastudillo.taxdocument.domain.taxdocument;

/**
 * Issuing point within an establishment for tax document numbering.
 */
public record IssuingPoint(String issuingPointId, String code, String establishmentId) {
    public IssuingPoint {
        requireNotBlank(issuingPointId, "issuingPointId");
        requireNotBlank(code, "code");
        requireNotBlank(establishmentId, "establishmentId");
    }

    private static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
