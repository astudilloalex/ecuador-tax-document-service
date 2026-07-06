package com.alexastudillo.taxdocument.application.issuance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AuditEventNameTest {
    @Test
    void exposesCanonicalAuditEventNames() {
        Set<String> values = Arrays.stream(AuditEventName.values())
                .map(AuditEventName::value)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "TaxDocumentIssuanceRequested",
                "TaxDocumentQueuedForIssuance",
                "TaxDocumentXmlGenerated",
                "TaxDocumentSigned",
                "TaxDocumentSubmittedToSri",
                "TaxDocumentReceivedBySri",
                "TaxDocumentAuthorized",
                "TaxDocumentRejected",
                "TaxDocumentAuthorizationRetryRequested",
                "TaxDocumentVoided"), values);
    }

    @Test
    void eventNamesDoNotContainSensitiveDataTerms() {
        for (AuditEventName eventName : AuditEventName.values()) {
            String value = eventName.value().toLowerCase();
            assertFalse(value.contains("secret"));
            assertFalse(value.contains("password"));
            assertFalse(value.contains("credential"));
            assertFalse(value.contains("privatekey"));
            assertFalse(value.contains("token"));
        }
    }
}
