package com.alexastudillo.taxdocument.adapter.out.persistence.transaction;

import com.alexastudillo.taxdocument.adapter.out.persistence.error.PersistenceExceptionTranslator;
import com.alexastudillo.taxdocument.application.error.PersistenceFailure;
import com.alexastudillo.taxdocument.application.port.out.TransactionPort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.function.Supplier;

@ApplicationScoped
public class PersistenceTransactionAdapter implements TransactionPort {
    private final PersistenceExceptionTranslator exceptionTranslator;

    public PersistenceTransactionAdapter(PersistenceExceptionTranslator exceptionTranslator) {
        this.exceptionTranslator = Objects.requireNonNull(exceptionTranslator, "exceptionTranslator must not be null");
    }

    @Override
    @Transactional
    public <T> Uni<T> withinTransaction(Supplier<T> operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        try {
            return Uni.createFrom().item(operation.get());
        } catch (PersistenceFailure failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw exceptionTranslator.transactionFailure();
        }
    }

    @Override
    @Transactional
    public Uni<Void> withinTransaction(Runnable operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        try {
            operation.run();
            return Uni.createFrom().voidItem();
        } catch (PersistenceFailure failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw exceptionTranslator.transactionFailure();
        }
    }
}
