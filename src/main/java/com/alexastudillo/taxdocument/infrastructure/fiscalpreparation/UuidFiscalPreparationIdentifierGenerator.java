package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import com.alexastudillo.taxdocument.application.fiscalpreparation.FiscalPreparationIdentifierGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/** Local opaque Fiscal Preparation identifier generator. */
@ApplicationScoped
@NullMarked
public final class UuidFiscalPreparationIdentifierGenerator
    implements FiscalPreparationIdentifierGenerator {
  @Override
  public UUID nextIdentifier() {
    return Objects.requireNonNull(UUID.randomUUID());
  }
}
