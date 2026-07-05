package com.alexastudillo.taxdocument.domain.taxdocument;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class AuthorizationStateTest {
    @Test
    void authorizationStateIsSeparateFromDocumentState() {
        assertNotEquals(DocumentState.class, AuthorizationState.class);
        assertTrue(Arrays.stream(AuthorizationState.values())
                .anyMatch(state -> state == AuthorizationState.NOT_SUBMITTED));
        assertTrue(Arrays.stream(DocumentState.values())
                .anyMatch(state -> state == DocumentState.PENDING));
    }
}
