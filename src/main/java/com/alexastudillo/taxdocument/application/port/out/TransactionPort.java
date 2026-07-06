package com.alexastudillo.taxdocument.application.port.out;

import java.util.function.Supplier;

public interface TransactionPort {
    <T> T withinTransaction(Supplier<T> operation);

    void withinTransaction(Runnable operation);
}
