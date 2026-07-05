package com.alexastudillo.taxdocument.application.port.out;

import java.time.Instant;
import java.time.LocalDate;

public interface ClockPort {
    Instant now();

    LocalDate today();
}
