package com.alexastudillo.taxdocument.adapter.out.persistence;

import com.alexastudillo.taxdocument.adapter.out.persistence.entity.EstablishmentEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.IssuerEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.IssuingPointEntity;
import com.alexastudillo.taxdocument.domain.taxdocument.AccessKey;
import com.alexastudillo.taxdocument.domain.taxdocument.DocumentType;
import com.alexastudillo.taxdocument.domain.taxdocument.Establishment;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuanceMode;
import com.alexastudillo.taxdocument.domain.taxdocument.IssueDate;
import com.alexastudillo.taxdocument.domain.taxdocument.Issuer;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuingPoint;
import com.alexastudillo.taxdocument.domain.taxdocument.SequenceNumber;
import com.alexastudillo.taxdocument.domain.taxdocument.TaxDocument;
import java.time.LocalDate;

final class PersistenceTestData {
    static final String ACCESS_KEY_VALUE = "1234567890123456789012345678901234567890123456789";

    private PersistenceTestData() {
    }

    static Issuer issuer() {
        return new Issuer("issuer-1", "1790012345001", "Example Legal Name", "Example Trade Name");
    }

    static Establishment establishment() {
        return new Establishment("establishment-1", "001", issuer().issuerId());
    }

    static IssuingPoint issuingPoint() {
        return new IssuingPoint("issuing-point-1", "002", establishment().establishmentId());
    }

    static SequenceNumber sequenceNumber() {
        return new SequenceNumber("000000123", DocumentType.INVOICE, issuer(), establishment(), issuingPoint());
    }

    static TaxDocument taxDocument() {
        return taxDocument("1", ACCESS_KEY_VALUE, "000000123");
    }

    static TaxDocument taxDocument(String suffix, String accessKeyValue, String sequenceValue) {
        Issuer issuer = new Issuer(
                "issuer-" + suffix,
                "1790012345" + suffix,
                "Example Legal Name " + suffix,
                "Example Trade Name " + suffix);
        Establishment establishment = new Establishment(
                "establishment-" + suffix,
                "0" + suffix,
                issuer.issuerId());
        IssuingPoint issuingPoint = new IssuingPoint(
                "issuing-point-" + suffix,
                "1" + suffix,
                establishment.establishmentId());
        SequenceNumber sequenceNumber =
                new SequenceNumber(sequenceValue, DocumentType.INVOICE, issuer, establishment, issuingPoint);
        return new TaxDocument(
                DocumentType.INVOICE,
                issuer,
                establishment,
                issuingPoint,
                sequenceNumber,
                new AccessKey(accessKeyValue),
                new IssueDate(LocalDate.of(2026, 7, 5)),
                IssuanceMode.ASYNCHRONOUS,
                "request-" + suffix);
    }

    static String accessKey(long value) {
        return "%049d".formatted(value);
    }

    static IssuerEntity issuerEntity() {
        Issuer issuer = issuer();
        return new IssuerEntity(issuer.issuerId(), issuer.legalIdentifier(), issuer.legalName(), issuer.tradeName());
    }

    static EstablishmentEntity establishmentEntity(IssuerEntity issuerEntity) {
        Establishment establishment = establishment();
        return new EstablishmentEntity(establishment.establishmentId(), issuerEntity, establishment.code());
    }

    static IssuingPointEntity issuingPointEntity(EstablishmentEntity establishmentEntity) {
        IssuingPoint issuingPoint = issuingPoint();
        return new IssuingPointEntity(issuingPoint.issuingPointId(), establishmentEntity, issuingPoint.code());
    }
}
