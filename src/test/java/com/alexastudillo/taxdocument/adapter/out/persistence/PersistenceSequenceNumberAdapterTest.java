package com.alexastudillo.taxdocument.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.adapter.out.persistence.error.PersistenceExceptionTranslator;
import com.alexastudillo.taxdocument.application.error.PersistenceFailureCategory;
import com.alexastudillo.taxdocument.application.port.out.SequenceNumberPort;
import com.alexastudillo.taxdocument.domain.taxdocument.DocumentType;
import com.alexastudillo.taxdocument.domain.taxdocument.Establishment;
import com.alexastudillo.taxdocument.domain.taxdocument.Issuer;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuingPoint;
import com.alexastudillo.taxdocument.domain.taxdocument.SequenceNumber;
import io.smallrye.mutiny.Uni;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PersistenceSequenceNumberAdapterTest {
    @Inject
    SequenceNumberPort sequenceNumberPort;

    @Inject
    PersistenceExceptionTranslator exceptionTranslator;

    @Test
    @TestTransaction
    void reservesRequestedSequenceNumber() {
        Issuer issuer = new Issuer("sequence-issuer-1", "1790011111001", "Sequence Legal Name", null);
        Establishment establishment = new Establishment("sequence-establishment-1", "001", issuer.issuerId());
        IssuingPoint issuingPoint =
                new IssuingPoint("sequence-issuing-point-1", "002", establishment.establishmentId());

        SequenceNumber sequenceNumber = await(sequenceNumberPort.reserve(
                DocumentType.INVOICE,
                issuer,
                establishment,
                issuingPoint,
                "000000301"));

        assertEquals("000000301", sequenceNumber.value());
        assertEquals(DocumentType.INVOICE, sequenceNumber.documentType());
        assertEquals(issuer, sequenceNumber.issuer());
    }

    @Test
    @TestTransaction
    void exactRepeatedReservationIsIdempotent() {
        Issuer issuer = new Issuer("sequence-issuer-2", "1790011111002", "Sequence Legal Name", null);
        Establishment establishment = new Establishment("sequence-establishment-2", "001", issuer.issuerId());
        IssuingPoint issuingPoint =
                new IssuingPoint("sequence-issuing-point-2", "002", establishment.establishmentId());

        SequenceNumber first = await(sequenceNumberPort.reserve(
                DocumentType.INVOICE,
                issuer,
                establishment,
                issuingPoint,
                "000000302"));
        SequenceNumber second = await(sequenceNumberPort.reserve(
                DocumentType.INVOICE,
                issuer,
                establishment,
                issuingPoint,
                "000000302"));

        assertEquals(first, second);
    }

    @Test
    @TestTransaction
    void reportsAvailabilityFromPersistedReservations() {
        Issuer issuer = new Issuer("sequence-issuer-3", "1790011111003", "Sequence Legal Name", null);
        Establishment establishment = new Establishment("sequence-establishment-3", "001", issuer.issuerId());
        IssuingPoint issuingPoint =
                new IssuingPoint("sequence-issuing-point-3", "002", establishment.establishmentId());

        assertTrue(await(sequenceNumberPort.isAvailable(
                DocumentType.INVOICE,
                issuer,
                establishment,
                issuingPoint,
                "000000303")));

        await(sequenceNumberPort.reserve(DocumentType.INVOICE, issuer, establishment, issuingPoint, "000000303"));

        assertFalse(await(sequenceNumberPort.isAvailable(
                DocumentType.INVOICE,
                issuer,
                establishment,
                issuingPoint,
                "000000303")));
    }

    @Test
    void mapsUnavailableSequenceConstraintToApplicationCategory() {
        assertEquals(
                PersistenceFailureCategory.UNAVAILABLE_SEQUENCE_RESERVATION_CONFLICT,
                exceptionTranslator.translateConstraint("uk_issuance_sequences_identity").category());
    }

    private static <T> T await(Uni<T> uni) {
        return uni.await().indefinitely();
    }
}
