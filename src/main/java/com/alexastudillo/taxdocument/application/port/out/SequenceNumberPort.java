package com.alexastudillo.taxdocument.application.port.out;

import com.alexastudillo.taxdocument.domain.taxdocument.DocumentType;
import com.alexastudillo.taxdocument.domain.taxdocument.Establishment;
import com.alexastudillo.taxdocument.domain.taxdocument.Issuer;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuingPoint;
import com.alexastudillo.taxdocument.domain.taxdocument.SequenceNumber;

public interface SequenceNumberPort {
    SequenceNumber reserve(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            String requestedValue);

    boolean isAvailable(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            String value);
}
