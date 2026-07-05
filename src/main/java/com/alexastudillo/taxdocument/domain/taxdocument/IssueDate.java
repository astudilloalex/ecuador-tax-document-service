package com.alexastudillo.taxdocument.domain.taxdocument;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Date on which a tax document is issued.
 */
public record IssueDate(LocalDate value) {
    public IssueDate {
        Objects.requireNonNull(value, "issueDate must not be null");
    }
}
