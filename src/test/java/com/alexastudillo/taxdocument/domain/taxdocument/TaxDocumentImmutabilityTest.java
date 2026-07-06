package com.alexastudillo.taxdocument.domain.taxdocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TaxDocumentImmutabilityTest {
    @Test
    void authorizedDocumentIsTerminalAndImmutableByDefault() {
        TaxDocument taxDocument = pendingTaxDocument();

        taxDocument.transitionTo(DocumentState.IN_PROGRESS);
        taxDocument.transitionTo(DocumentState.RECEIVED);
        taxDocument.authorize(
                new AuthorizationNumber("authorization-1"),
                new AuthorizedAt(Instant.parse("2026-07-05T12:00:00Z")));

        assertEquals(DocumentState.AUTHORIZED, taxDocument.documentState());
        assertEquals(AuthorizationState.AUTHORIZED, taxDocument.authorizationState());
        assertThrows(ImmutableAuthorizedDocumentException.class,
                () -> taxDocument.transitionTo(DocumentState.REJECTED));
    }

    private static TaxDocument pendingTaxDocument() {
        Issuer issuer = new Issuer("issuer-1", "1790012345001", "Example Legal Name", null);
        Establishment establishment = new Establishment("establishment-1", "001", issuer.issuerId());
        IssuingPoint issuingPoint = new IssuingPoint("issuing-point-1", "002", establishment.establishmentId());
        SequenceNumber sequenceNumber = new SequenceNumber("000000123", DocumentType.INVOICE, issuer, establishment, issuingPoint);
        return new TaxDocument(
                DocumentType.INVOICE,
                issuer,
                establishment,
                issuingPoint,
                sequenceNumber,
                new AccessKey("1234567890123456789012345678901234567890123456789"),
                new IssueDate(LocalDate.of(2026, 7, 5)),
                IssuanceMode.ASYNCHRONOUS,
                "request-1");
    }
}
