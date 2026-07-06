package com.alexastudillo.taxdocument.domain.taxdocument;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TaxDocumentRestoreTest {
    @Test
    void restoresPersistedAuthorizedDocumentWithoutPendingConstructorState() {
        AuthorizationNumber authorizationNumber = new AuthorizationNumber("authorization-1");
        AuthorizedAt authorizedAt = new AuthorizedAt(Instant.parse("2026-07-05T12:00:00Z"));

        TaxDocument taxDocument = TaxDocument.restore(
                DocumentType.INVOICE,
                issuer(),
                establishment(),
                issuingPoint(),
                sequenceNumber(),
                new AccessKey("1234567890123456789012345678901234567890123456789"),
                new IssueDate(LocalDate.of(2026, 7, 5)),
                DocumentState.AUTHORIZED,
                AuthorizationState.AUTHORIZED,
                authorizationNumber,
                authorizedAt,
                IssuanceMode.ASYNCHRONOUS,
                "request-1");

        assertEquals(DocumentState.AUTHORIZED, taxDocument.documentState());
        assertEquals(AuthorizationState.AUTHORIZED, taxDocument.authorizationState());
        assertEquals(authorizationNumber, taxDocument.authorizationNumber().orElseThrow());
        assertEquals(authorizedAt, taxDocument.authorizedAt().orElseThrow());
    }

    @Test
    void restoresPersistedReturnedDocumentWithoutAuthorizationData() {
        TaxDocument taxDocument = TaxDocument.restore(
                DocumentType.INVOICE,
                issuer(),
                establishment(),
                issuingPoint(),
                sequenceNumber(),
                new AccessKey("1234567890123456789012345678901234567890123456789"),
                new IssueDate(LocalDate.of(2026, 7, 5)),
                DocumentState.RETURNED,
                AuthorizationState.RETURNED,
                null,
                null,
                IssuanceMode.ASYNCHRONOUS,
                null);

        assertEquals(DocumentState.RETURNED, taxDocument.documentState());
        assertEquals(AuthorizationState.RETURNED, taxDocument.authorizationState());
        assertFalse(taxDocument.authorizationNumber().isPresent());
        assertFalse(taxDocument.authorizedAt().isPresent());
        assertFalse(taxDocument.externalRequestId().isPresent());
    }

    @Test
    void rejectsInvalidAuthorizationCombinations() {
        AuthorizationNumber authorizationNumber = new AuthorizationNumber("authorization-1");
        AuthorizedAt authorizedAt = new AuthorizedAt(Instant.parse("2026-07-05T12:00:00Z"));

        assertThrows(IllegalArgumentException.class, () -> restore(
                DocumentState.RECEIVED,
                AuthorizationState.RECEIVED,
                authorizationNumber,
                null));
        assertThrows(IllegalArgumentException.class, () -> restore(
                DocumentState.AUTHORIZED,
                AuthorizationState.AUTHORIZED,
                null,
                authorizedAt));
        assertThrows(IllegalArgumentException.class, () -> restore(
                DocumentState.AUTHORIZED,
                AuthorizationState.AUTHORIZED,
                null,
                null));
        assertThrows(IllegalArgumentException.class, () -> restore(
                DocumentState.AUTHORIZED,
                AuthorizationState.RECEIVED,
                authorizationNumber,
                authorizedAt));
    }

    @Test
    void remainsFrameworkFree() {
        assertDoesNotThrow(() -> Class.forName("com.alexastudillo.taxdocument.domain.taxdocument.TaxDocument"));

        for (Class<?> dependency : TaxDocument.class.getDeclaredClasses()) {
            assertFalse(dependency.getName().contains("quarkus"));
            assertFalse(dependency.getName().contains("hibernate"));
            assertFalse(dependency.getName().contains("persistence"));
        }
    }

    private static TaxDocument restore(
            DocumentState documentState,
            AuthorizationState authorizationState,
            AuthorizationNumber authorizationNumber,
            AuthorizedAt authorizedAt) {
        return TaxDocument.restore(
                DocumentType.INVOICE,
                issuer(),
                establishment(),
                issuingPoint(),
                sequenceNumber(),
                new AccessKey("1234567890123456789012345678901234567890123456789"),
                new IssueDate(LocalDate.of(2026, 7, 5)),
                documentState,
                authorizationState,
                authorizationNumber,
                authorizedAt,
                IssuanceMode.ASYNCHRONOUS,
                "request-1");
    }

    private static Issuer issuer() {
        return new Issuer("issuer-1", "1790012345001", "Example Legal Name", "Example Trade Name");
    }

    private static Establishment establishment() {
        return new Establishment("establishment-1", "001", issuer().issuerId());
    }

    private static IssuingPoint issuingPoint() {
        return new IssuingPoint("issuing-point-1", "002", establishment().establishmentId());
    }

    private static SequenceNumber sequenceNumber() {
        return new SequenceNumber("000000123", DocumentType.INVOICE, issuer(), establishment(), issuingPoint());
    }
}
