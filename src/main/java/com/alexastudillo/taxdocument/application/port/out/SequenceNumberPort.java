package com.alexastudillo.taxdocument.application.port.out;

import com.alexastudillo.taxdocument.domain.taxdocument.DocumentType;
import com.alexastudillo.taxdocument.domain.taxdocument.Establishment;
import com.alexastudillo.taxdocument.domain.taxdocument.Issuer;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuingPoint;
import com.alexastudillo.taxdocument.domain.taxdocument.SequenceNumber;
import io.smallrye.mutiny.Uni;

public interface SequenceNumberPort {
    Uni<SequenceNumber> reserve(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            String requestedValue);

    Uni<Boolean> isAvailable(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            String value);
}
