package com.alexastudillo.taxdocument.application.fiscalpreparation;

import com.alexastudillo.taxdocument.domain.fiscalpreparation.NumericCode;
import org.jspecify.annotations.NullMarked;

/** Replaceable eight-digit Numeric Code generator. */
@FunctionalInterface
@NullMarked
public interface NumericCodeGenerator {
  NumericCode nextNumericCode();
}
