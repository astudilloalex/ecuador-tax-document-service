package com.alexastudillo.taxdocument.application.port.out;

import com.alexastudillo.taxdocument.domain.taxdocument.AccessKey;
import com.alexastudillo.taxdocument.domain.taxdocument.DocumentType;
import com.alexastudillo.taxdocument.domain.taxdocument.Establishment;
import com.alexastudillo.taxdocument.domain.taxdocument.Issuer;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuingPoint;
import com.alexastudillo.taxdocument.domain.taxdocument.SequenceNumber;
import com.alexastudillo.taxdocument.domain.taxdocument.TaxDocument;
import java.util.Optional;

public interface TaxDocumentRepository {
    TaxDocument save(TaxDocument taxDocument);

    Optional<TaxDocument> findByAccessKey(AccessKey accessKey);

    Optional<TaxDocument> findByIssuanceIdentity(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            SequenceNumber sequenceNumber);

    boolean existsByAccessKey(AccessKey accessKey);

    boolean existsByIssuanceIdentity(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            SequenceNumber sequenceNumber);
}
