package com.alexastudillo.taxdocument.application.port.out;

import com.alexastudillo.taxdocument.domain.taxdocument.AccessKey;
import com.alexastudillo.taxdocument.domain.taxdocument.DocumentType;
import com.alexastudillo.taxdocument.domain.taxdocument.Establishment;
import com.alexastudillo.taxdocument.domain.taxdocument.Issuer;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuingPoint;
import com.alexastudillo.taxdocument.domain.taxdocument.SequenceNumber;
import com.alexastudillo.taxdocument.domain.taxdocument.TaxDocument;
import io.smallrye.mutiny.Uni;
import java.util.Optional;

public interface TaxDocumentRepository {
    Uni<TaxDocument> save(TaxDocument taxDocument);

    Uni<Optional<TaxDocument>> findByAccessKey(AccessKey accessKey);

    Uni<Optional<TaxDocument>> findByIssuanceIdentity(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            SequenceNumber sequenceNumber);

    Uni<Boolean> existsByAccessKey(AccessKey accessKey);

    Uni<Boolean> existsByIssuanceIdentity(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            SequenceNumber sequenceNumber);
}
