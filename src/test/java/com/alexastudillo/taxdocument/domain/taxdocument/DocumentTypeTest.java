package com.alexastudillo.taxdocument.domain.taxdocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DocumentTypeTest {
    @Test
    void supportsRequiredCanonicalDocumentTypes() {
        Set<String> names = Arrays.stream(DocumentType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(Set.of("INVOICE", "CREDIT_NOTE", "DEBIT_NOTE", "WAYBILL", "WITHHOLDING"), names);
    }

    @Test
    void enumNamesAreNotSriNumericCodes() {
        for (DocumentType documentType : DocumentType.values()) {
            assertFalse(documentType.name().matches("\\d+"));
        }
    }
}
