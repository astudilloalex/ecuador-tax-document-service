package com.alexastudillo.taxdocument.domain.taxdocument;

/**
 * Value object for a 49-digit tax document access key.
 */
public record AccessKey(String value) {
    private static final int REQUIRED_LENGTH = 49;

    public AccessKey {
        if (value == null) {
            throw new InvalidAccessKeyException("accessKey must not be null");
        }
        if (value.length() != REQUIRED_LENGTH) {
            throw new InvalidAccessKeyException("accessKey must contain exactly 49 digits");
        }
        if (!value.chars().allMatch(Character::isDigit)) {
            throw new InvalidAccessKeyException("accessKey must contain only digits");
        }
    }
}
