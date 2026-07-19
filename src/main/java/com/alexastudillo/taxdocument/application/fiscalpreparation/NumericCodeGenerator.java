package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.NumericCode;

/** Replaceable eight-digit Numeric Code generator. */
@FunctionalInterface
public interface NumericCodeGenerator {
  NumericCode nextNumericCode();
}
