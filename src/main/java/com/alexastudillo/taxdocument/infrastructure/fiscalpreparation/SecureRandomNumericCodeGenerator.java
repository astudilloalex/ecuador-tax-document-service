package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import com.alexastudillo.taxdocument.application.fiscalpreparation.NumericCodeGenerator;
import com.alexastudillo.taxdocument.domain.fiscalpreparation.NumericCode;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.security.SecureRandom;

/** Startup-warmed uniform eight-digit Numeric Code generator policy SECURE_RANDOM_8_V1. */
@ApplicationScoped
public final class SecureRandomNumericCodeGenerator implements NumericCodeGenerator {
  private final SecureRandom random = new SecureRandom();

  @PostConstruct
  void warm() {
    random.nextBytes(new byte[1]);
  }

  @Override
  public NumericCode nextNumericCode() {
    return NumericCode.of(random.nextInt(100_000_000));
  }
}
