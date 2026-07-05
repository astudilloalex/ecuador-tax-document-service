package com.alexastudillo.taxdocument.domain.taxdocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AccessKeyTest {
    private static final String VALID_ACCESS_KEY = "1234567890123456789012345678901234567890123456789";

    @Test
    void acceptsExactlyFortyNineDigits() {
        AccessKey accessKey = new AccessKey(VALID_ACCESS_KEY);

        assertEquals(VALID_ACCESS_KEY, accessKey.value());
    }

    @Test
    void rejectsNonDigitCharacters() {
        assertThrows(InvalidAccessKeyException.class,
                () -> new AccessKey("123456789012345678901234567890123456789012345678A"));
    }

    @Test
    void rejectsInvalidLength() {
        assertThrows(InvalidAccessKeyException.class, () -> new AccessKey("12345"));
    }
}
