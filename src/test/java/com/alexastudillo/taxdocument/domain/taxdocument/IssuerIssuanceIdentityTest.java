package com.alexastudillo.taxdocument.domain.taxdocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IssuerIssuanceIdentityTest {
    @Test
    void representsIssuerEstablishmentIssuingPointAndSequenceNumber() {
        Issuer issuer = new Issuer("issuer-1", "1790012345001", "Example Legal Name", "Example Trade Name");
        Establishment establishment = new Establishment("establishment-1", "001", issuer.issuerId());
        IssuingPoint issuingPoint = new IssuingPoint("issuing-point-1", "002", establishment.establishmentId());
        SequenceNumber sequenceNumber = new SequenceNumber(
                "000000123",
                DocumentType.INVOICE,
                issuer,
                establishment,
                issuingPoint);

        assertEquals("issuer-1", sequenceNumber.issuer().issuerId());
        assertEquals("001", sequenceNumber.establishment().code());
        assertEquals("002", sequenceNumber.issuingPoint().code());
        assertEquals("000000123", sequenceNumber.value());
        assertEquals(DocumentType.INVOICE, sequenceNumber.documentType());
    }

    @Test
    void rejectsBlankIdentityValues() {
        assertThrows(IllegalArgumentException.class,
                () -> new Issuer(" ", "1790012345001", "Example Legal Name", null));
        assertThrows(IllegalArgumentException.class,
                () -> new Establishment("establishment-1", " ", "issuer-1"));
        assertThrows(IllegalArgumentException.class,
                () -> new IssuingPoint("issuing-point-1", "002", " "));
        assertThrows(IllegalArgumentException.class,
                () -> new SequenceNumber("abc", DocumentType.INVOICE,
                        new Issuer("issuer-1", "1790012345001", "Example Legal Name", null),
                        new Establishment("establishment-1", "001", "issuer-1"),
                        new IssuingPoint("issuing-point-1", "002", "establishment-1")));
    }
}
