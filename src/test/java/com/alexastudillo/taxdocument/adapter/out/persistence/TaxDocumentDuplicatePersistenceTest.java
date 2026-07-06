package com.alexastudillo.taxdocument.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alexastudillo.taxdocument.adapter.out.persistence.error.PersistenceExceptionTranslator;
import com.alexastudillo.taxdocument.application.error.PersistenceFailure;
import com.alexastudillo.taxdocument.application.error.PersistenceFailureCategory;
import com.alexastudillo.taxdocument.application.port.out.TaxDocumentRepository;
import com.alexastudillo.taxdocument.domain.taxdocument.TaxDocument;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TaxDocumentDuplicatePersistenceTest {
    @Inject
    TaxDocumentRepository repository;

    @Inject
    PersistenceExceptionTranslator exceptionTranslator;

    @Test
    @TestTransaction
    void rejectsDuplicateAccessKey() {
        TaxDocument first =
                PersistenceTestData.taxDocument("dup-access-1", PersistenceTestData.accessKey(201), "000000201");
        TaxDocument second =
                PersistenceTestData.taxDocument("dup-access-2", PersistenceTestData.accessKey(201), "000000202");

        repository.save(first).await().indefinitely();

        PersistenceFailure failure =
                assertThrows(PersistenceFailure.class, () -> repository.save(second).await().indefinitely());

        assertEquals(PersistenceFailureCategory.DUPLICATE_ACCESS_KEY_CONFLICT, failure.category());
    }

    @Test
    @TestTransaction
    void rejectsDuplicateIssuanceIdentity() {
        TaxDocument first =
                PersistenceTestData.taxDocument("dup-identity", PersistenceTestData.accessKey(202), "000000203");
        TaxDocument second =
                PersistenceTestData.taxDocument("dup-identity", PersistenceTestData.accessKey(203), "000000203");

        repository.save(first).await().indefinitely();

        PersistenceFailure failure =
                assertThrows(PersistenceFailure.class, () -> repository.save(second).await().indefinitely());

        assertEquals(PersistenceFailureCategory.DUPLICATE_ISSUANCE_IDENTITY_CONFLICT, failure.category());
    }

    @Test
    void mapsDatabaseConstraintNamesToStableDuplicateCategories() {
        assertEquals(
                PersistenceFailureCategory.DUPLICATE_ACCESS_KEY_CONFLICT,
                exceptionTranslator.translateConstraint("uk_tax_documents_access_key").category());
        assertEquals(
                PersistenceFailureCategory.DUPLICATE_ISSUANCE_IDENTITY_CONFLICT,
                exceptionTranslator.translateConstraint("uk_tax_documents_issuance_identity").category());
    }
}
