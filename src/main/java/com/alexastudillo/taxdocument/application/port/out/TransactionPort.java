package com.alexastudillo.taxdocument.application.port.out;

import io.smallrye.mutiny.Uni;
import java.util.function.Supplier;

public interface TransactionPort {
    <T> Uni<T> withinTransaction(Supplier<T> operation);

    Uni<Void> withinTransaction(Runnable operation);
}
