package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationIdentifierGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/** Local opaque Fiscal Preparation identifier generator. */
@ApplicationScoped
public final class UuidFiscalPreparationIdentifierGenerator
    implements FiscalPreparationIdentifierGenerator {
  @Override
  public UUID nextIdentifier() {
    return UUID.randomUUID();
  }
}
