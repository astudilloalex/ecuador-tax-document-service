package com.alexastudillo.taxdocument.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alexastudillo.taxdocument.adapter.out.persistence.entity.EstablishmentEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.IssuerEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.IssuingPointEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.TaxDocumentEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.error.PersistenceExceptionTranslator;
import com.alexastudillo.taxdocument.adapter.out.persistence.mapper.TaxDocumentPersistenceMapper;
import com.alexastudillo.taxdocument.application.error.PersistenceFailure;
import com.alexastudillo.taxdocument.application.error.PersistenceFailureCategory;
import com.alexastudillo.taxdocument.domain.taxdocument.TaxDocument;
import org.junit.jupiter.api.Test;

class PersistenceErrorTranslationTest {
    private final PersistenceExceptionTranslator translator = new PersistenceExceptionTranslator();
    private final TaxDocumentPersistenceMapper mapper = new TaxDocumentPersistenceMapper(translator);

    @Test
    void translatesUnknownCanonicalEnumValues() {
        TaxDocumentEntity entity = validEntity();
        entity.updateFrom(
                entity.accessKey(),
                entity.issuer(),
                entity.establishment(),
                entity.issuingPoint(),
                "UNKNOWN",
                entity.sequenceNumber(),
                entity.issueDate(),
                entity.documentState(),
                entity.authorizationState(),
                entity.authorizationNumber(),
                entity.authorizedAt(),
                entity.issuanceMode(),
                entity.externalRequestId());

        PersistenceFailure failure = assertThrows(PersistenceFailure.class, () -> mapper.toDomain(entity));

        assertEquals(PersistenceFailureCategory.INVALID_PERSISTED_TAX_DOCUMENT_STATE, failure.category());
    }

    @Test
    void translatesInconsistentRelationships() {
        TaxDocument taxDocument = PersistenceTestData.taxDocument();
        IssuerEntity issuer = PersistenceTestData.issuerEntity();
        IssuerEntity otherIssuer = new IssuerEntity("issuer-other", "1790099999001", "Other Legal Name", null);
        EstablishmentEntity inconsistentEstablishment =
                new EstablishmentEntity("establishment-1", otherIssuer, "001");
        IssuingPointEntity issuingPoint = PersistenceTestData.issuingPointEntity(inconsistentEstablishment);
        TaxDocumentEntity entity = mapper.toEntity(taxDocument, issuer, inconsistentEstablishment, issuingPoint);

        PersistenceFailure failure = assertThrows(PersistenceFailure.class, () -> mapper.toDomain(entity));

        assertEquals(PersistenceFailureCategory.INVALID_PERSISTENCE_RELATIONSHIP, failure.category());
    }

    @Test
    void genericTranslationDoesNotExposeSensitiveInternalMessages() {
        PersistenceFailure failure = translator.translate(new IllegalStateException("password=secret token=secret"));

        assertEquals(PersistenceFailureCategory.GENERIC_PERSISTENCE_FAILURE, failure.category());
        assertEquals("Persistence operation failed", failure.getMessage());
    }

    private TaxDocumentEntity validEntity() {
        IssuerEntity issuer = PersistenceTestData.issuerEntity();
        EstablishmentEntity establishment = PersistenceTestData.establishmentEntity(issuer);
        IssuingPointEntity issuingPoint = PersistenceTestData.issuingPointEntity(establishment);
        return mapper.toEntity(PersistenceTestData.taxDocument(), issuer, establishment, issuingPoint);
    }
}
