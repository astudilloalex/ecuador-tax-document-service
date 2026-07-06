package com.alexastudillo.taxdocument.application.port.out;

import io.smallrye.mutiny.Uni;
import java.time.Instant;
import java.time.LocalDate;

public interface ClockPort {
    Uni<Instant> now();

    Uni<LocalDate> today();
}
