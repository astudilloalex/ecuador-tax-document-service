package com.alexastudillo.taxdocument.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.application.port.out.TaxDocumentRepository;
import com.alexastudillo.taxdocument.domain.taxdocument.AccessKey;
import com.alexastudillo.taxdocument.domain.taxdocument.DocumentState;
import com.alexastudillo.taxdocument.domain.taxdocument.TaxDocument;
import io.smallrye.mutiny.Uni;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PersistenceTaxDocumentRepositoryTest {
    @Inject
    TaxDocumentRepository repository;

    @Test
    @TestTransaction
    void savesAndLoadsByAccessKeyAndIssuanceIdentity() {
        TaxDocument taxDocument =
                PersistenceTestData.taxDocument("repo-1", PersistenceTestData.accessKey(101), "000000101");

        TaxDocument saved = await(repository.save(taxDocument));

        Optional<TaxDocument> byAccessKey = await(repository.findByAccessKey(saved.accessKey()));
        Optional<TaxDocument> byIdentity = await(repository.findByIssuanceIdentity(
                saved.documentType(),
                saved.issuer(),
                saved.establishment(),
                saved.issuingPoint(),
                saved.sequenceNumber()));

        assertTrue(byAccessKey.isPresent());
        assertTrue(byIdentity.isPresent());
        assertEquals(saved.accessKey(), byAccessKey.orElseThrow().accessKey());
        assertEquals(saved.sequenceNumber(), byIdentity.orElseThrow().sequenceNumber());
    }

    @Test
    @TestTransaction
    void missingLookupsReturnEmptyOrFalse() {
        TaxDocument taxDocument =
                PersistenceTestData.taxDocument("repo-2", PersistenceTestData.accessKey(102), "000000102");

        assertFalse(await(repository.findByAccessKey(taxDocument.accessKey())).isPresent());
        assertFalse(await(repository.existsByAccessKey(taxDocument.accessKey())));
        assertFalse(await(repository.findByIssuanceIdentity(
                        taxDocument.documentType(),
                        taxDocument.issuer(),
                        taxDocument.establishment(),
                        taxDocument.issuingPoint(),
                        taxDocument.sequenceNumber()))
                .isPresent());
        assertFalse(await(repository.existsByIssuanceIdentity(
                taxDocument.documentType(),
                taxDocument.issuer(),
                taxDocument.establishment(),
                taxDocument.issuingPoint(),
                taxDocument.sequenceNumber())));
    }

    @Test
    @TestTransaction
    void existenceChecksReflectPersistedDocuments() {
        TaxDocument taxDocument =
                PersistenceTestData.taxDocument("repo-3", PersistenceTestData.accessKey(103), "000000103");

        await(repository.save(taxDocument));

        assertTrue(await(repository.existsByAccessKey(new AccessKey(PersistenceTestData.accessKey(103)))));
        assertTrue(await(repository.existsByIssuanceIdentity(
                taxDocument.documentType(),
                taxDocument.issuer(),
                taxDocument.establishment(),
                taxDocument.issuingPoint(),
                taxDocument.sequenceNumber())));
    }

    @Test
    @TestTransaction
    void saveUpdatesSamePersistedAggregate() {
        TaxDocument taxDocument =
                PersistenceTestData.taxDocument("repo-update", PersistenceTestData.accessKey(104), "000000104");

        await(repository.save(taxDocument));
        taxDocument.transitionTo(DocumentState.IN_PROGRESS);
        TaxDocument updated = await(repository.save(taxDocument));

        assertEquals(DocumentState.IN_PROGRESS, updated.documentState());
        assertEquals(DocumentState.IN_PROGRESS, await(repository.findByAccessKey(taxDocument.accessKey()))
                .orElseThrow()
                .documentState());
    }

    private static <T> T await(Uni<T> uni) {
        return uni.await().indefinitely();
    }
}
