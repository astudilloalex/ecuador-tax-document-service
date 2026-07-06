package com.alexastudillo.taxdocument.domain.taxdocument;

import java.time.Instant;
import java.util.Objects;

/**
 * Timestamp at which tax document authorization is received.
 */
public record AuthorizedAt(Instant value) {
    public AuthorizedAt {
        Objects.requireNonNull(value, "authorizedAt must not be null");
    }
}
