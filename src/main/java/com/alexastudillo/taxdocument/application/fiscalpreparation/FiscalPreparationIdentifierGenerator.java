package com.alexastudillo.taxdocument.application.fiscalpreparation;

import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/** Replaceable local Fiscal Preparation identifier generator. */
@FunctionalInterface
@NullMarked
public interface FiscalPreparationIdentifierGenerator {
  UUID nextIdentifier();
}
