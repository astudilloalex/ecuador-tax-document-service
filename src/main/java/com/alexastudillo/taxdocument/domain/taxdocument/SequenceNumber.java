package com.alexastudillo.taxdocument.domain.taxdocument;

import java.util.Objects;

/**
 * Sequence value assigned for an issuer, establishment, issuing point, and type.
 */
public record SequenceNumber(
        String value,
        DocumentType documentType,
        Issuer issuer,
        Establishment establishment,
        IssuingPoint issuingPoint) {
    public SequenceNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("sequenceNumber must not be blank");
        }
        if (!value.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("sequenceNumber must contain only digits");
        }
        Objects.requireNonNull(documentType, "documentType must not be null");
        Objects.requireNonNull(issuer, "issuer must not be null");
        Objects.requireNonNull(establishment, "establishment must not be null");
        Objects.requireNonNull(issuingPoint, "issuingPoint must not be null");
        if (!establishment.issuerId().equals(issuer.issuerId())) {
            throw new IllegalArgumentException("establishment must belong to issuer");
        }
        if (!issuingPoint.establishmentId().equals(establishment.establishmentId())) {
            throw new IllegalArgumentException("issuingPoint must belong to establishment");
        }
    }
}
