package com.alexastudillo.taxdocument.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alexastudillo.taxdocument.adapter.out.persistence.error.PersistenceExceptionTranslator;
import com.alexastudillo.taxdocument.adapter.out.persistence.transaction.PersistenceTransactionAdapter;
import com.alexastudillo.taxdocument.application.error.PersistenceFailure;
import com.alexastudillo.taxdocument.application.error.PersistenceFailureCategory;
import org.junit.jupiter.api.Test;

class PersistenceTransactionAdapterTest {
    private final PersistenceTransactionAdapter transactionAdapter =
            new PersistenceTransactionAdapter(new PersistenceExceptionTranslator());

    @Test
    void returnsOperationValue() {
        String result = transactionAdapter.withinTransaction(() -> "completed").await().indefinitely();

        assertEquals("completed", result);
    }

    @Test
    void runsVoidOperation() {
        StringBuilder result = new StringBuilder();

        assertDoesNotThrow(() -> transactionAdapter.withinTransaction(() -> result.append("completed"))
                .await()
                .indefinitely());

        assertEquals("completed", result.toString());
    }

    @Test
    void translatesRuntimeFailureToApplicationFailure() {
        PersistenceFailure failure = assertThrows(
                PersistenceFailure.class,
                () -> transactionAdapter.withinTransaction(() -> {
                    throw new IllegalStateException("database password=secret");
                }).await().indefinitely());

        assertEquals(PersistenceFailureCategory.PERSISTENCE_TRANSACTION_FAILURE, failure.category());
        assertEquals("Persistence transaction failed", failure.getMessage());
    }
}
