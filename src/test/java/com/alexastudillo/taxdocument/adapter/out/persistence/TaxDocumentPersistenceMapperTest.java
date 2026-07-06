package com.alexastudillo.taxdocument.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.alexastudillo.taxdocument.adapter.out.persistence.entity.EstablishmentEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.IssuerEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.IssuingPointEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.TaxDocumentEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.error.PersistenceExceptionTranslator;
import com.alexastudillo.taxdocument.adapter.out.persistence.mapper.TaxDocumentPersistenceMapper;
import com.alexastudillo.taxdocument.domain.taxdocument.AuthorizationNumber;
import com.alexastudillo.taxdocument.domain.taxdocument.AuthorizedAt;
import com.alexastudillo.taxdocument.domain.taxdocument.DocumentState;
import com.alexastudillo.taxdocument.domain.taxdocument.TaxDocument;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class TaxDocumentPersistenceMapperTest {
    private final TaxDocumentPersistenceMapper mapper =
            new TaxDocumentPersistenceMapper(new PersistenceExceptionTranslator());

    @Test
    void mapsDomainToEntityUsingCanonicalValues() {
        TaxDocument taxDocument = PersistenceTestData.taxDocument();
        IssuerEntity issuer = PersistenceTestData.issuerEntity();
        EstablishmentEntity establishment = PersistenceTestData.establishmentEntity(issuer);
        IssuingPointEntity issuingPoint = PersistenceTestData.issuingPointEntity(establishment);

        TaxDocumentEntity entity = mapper.toEntity(taxDocument, issuer, establishment, issuingPoint);

        assertEquals(taxDocument.accessKey().value(), entity.accessKey());
        assertEquals("INVOICE", entity.documentType());
        assertEquals("PENDING", entity.documentState());
        assertEquals("NOT_SUBMITTED", entity.authorizationState());
        assertEquals("ASYNCHRONOUS", entity.issuanceMode());
    }

    @Test
    void rehydratesEntityToDomainWithTemporalPrecision() {
        TaxDocument taxDocument = PersistenceTestData.taxDocument();
        taxDocument.transitionTo(DocumentState.IN_PROGRESS);
        taxDocument.transitionTo(DocumentState.RECEIVED);
        Instant authorizedInstant = Instant.parse("2026-07-05T12:00:00.123456Z")
                .truncatedTo(ChronoUnit.MICROS);
        taxDocument.authorize(new AuthorizationNumber("authorization-1"), new AuthorizedAt(authorizedInstant));

        IssuerEntity issuer = PersistenceTestData.issuerEntity();
        EstablishmentEntity establishment = PersistenceTestData.establishmentEntity(issuer);
        IssuingPointEntity issuingPoint = PersistenceTestData.issuingPointEntity(establishment);
        TaxDocumentEntity entity = mapper.toEntity(taxDocument, issuer, establishment, issuingPoint);

        TaxDocument restored = mapper.toDomain(entity);

        assertEquals(taxDocument.accessKey(), restored.accessKey());
        assertEquals(DocumentState.AUTHORIZED, restored.documentState());
        assertEquals(taxDocument.authorizationNumber(), restored.authorizationNumber());
        assertEquals(authorizedInstant, restored.authorizedAt().orElseThrow().value());
        assertEquals(taxDocument.issueDate(), restored.issueDate());
        assertFalse(restored.externalRequestId().isEmpty());
    }
}
