package com.alexastudillo.taxdocument.domain.taxdocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LocalVoidingRuleTest {
    @Test
    void allowsLocalVoidingBeforeAuthorization() {
        TaxDocument taxDocument = pendingTaxDocument("request-void");

        taxDocument.voidLocally();

        assertEquals(DocumentState.VOIDED, taxDocument.documentState());
    }

    @Test
    void rejectsLocalVoidingForAuthorizedOrAlreadyVoidedDocuments() {
        TaxDocument authorized = pendingTaxDocument("request-authorized");
        authorized.transitionTo(DocumentState.IN_PROGRESS);
        authorized.transitionTo(DocumentState.RECEIVED);
        authorized.authorize(
                new AuthorizationNumber("authorization-1"),
                new AuthorizedAt(Instant.parse("2026-07-05T12:00:00Z")));

        assertThrows(ImmutableAuthorizedDocumentException.class, authorized::voidLocally);

        TaxDocument voided = pendingTaxDocument("request-voided");
        voided.voidLocally();

        assertThrows(InvalidStateTransitionException.class, voided::voidLocally);
    }

    private static TaxDocument pendingTaxDocument(String externalRequestId) {
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
                externalRequestId);
    }
}
