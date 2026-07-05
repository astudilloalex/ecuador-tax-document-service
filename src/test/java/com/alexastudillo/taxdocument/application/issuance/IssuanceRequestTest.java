package com.alexastudillo.taxdocument.application.issuance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.domain.taxdocument.DocumentType;
import com.alexastudillo.taxdocument.domain.taxdocument.Establishment;
import com.alexastudillo.taxdocument.domain.taxdocument.IssueDate;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuanceMode;
import com.alexastudillo.taxdocument.domain.taxdocument.Issuer;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuingPoint;
import com.alexastudillo.taxdocument.domain.taxdocument.SequenceNumber;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class IssuanceRequestTest {
    @Test
    void exposesCanonicalFieldsAndIdempotencyKeys() {
        Issuer issuer = new Issuer("issuer-1", "1790012345001", "Example Legal Name", null);
        Establishment establishment = new Establishment("establishment-1", "001", issuer.issuerId());
        IssuingPoint issuingPoint = new IssuingPoint("issuing-point-1", "002", establishment.establishmentId());
        SequenceNumber sequenceNumber = new SequenceNumber("000000123", DocumentType.INVOICE, issuer, establishment, issuingPoint);

        IssuanceRequest request = new IssuanceRequest(
                DocumentType.INVOICE,
                issuer,
                establishment,
                issuingPoint,
                sequenceNumber,
                new IssueDate(LocalDate.of(2026, 7, 5)),
                IssuanceMode.ASYNCHRONOUS,
                "external-request-1",
                "audit-correlation-1");

        assertEquals(DocumentType.INVOICE, request.documentType());
        assertTrue(request.externalRequestId().isPresent());
        assertTrue(request.idempotencyKeys().contains("externalRequestId:external-request-1"));
        assertTrue(request.idempotencyKeys().stream()
                .anyMatch(key -> key.startsWith("issuanceIdentity:issuer-1:INVOICE:001:002:000000123")));
    }
}
