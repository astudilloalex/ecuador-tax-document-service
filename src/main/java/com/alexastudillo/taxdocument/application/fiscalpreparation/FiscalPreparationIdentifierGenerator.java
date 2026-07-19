package com.alexastudillo.taxdocument.application.fiscalpreparation;

import java.util.UUID;

/** Replaceable local Fiscal Preparation identifier generator. */
@FunctionalInterface
public interface FiscalPreparationIdentifierGenerator {
  UUID nextIdentifier();
}
