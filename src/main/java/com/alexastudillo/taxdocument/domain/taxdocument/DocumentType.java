package com.alexastudillo.taxdocument.domain.taxdocument;

/**
 * Canonical tax document types used by the target domain model.
 */
public enum DocumentType {
    INVOICE,
    CREDIT_NOTE,
    DEBIT_NOTE,
    WAYBILL,
    WITHHOLDING
}
