package com.alexastudillo.taxdocument.adapter.out.persistence.mapper;

import com.alexastudillo.taxdocument.adapter.out.persistence.entity.IssuanceSequenceEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.error.PersistenceExceptionTranslator;
import com.alexastudillo.taxdocument.application.error.PersistenceFailure;
import com.alexastudillo.taxdocument.domain.taxdocument.DocumentType;
import com.alexastudillo.taxdocument.domain.taxdocument.Establishment;
import com.alexastudillo.taxdocument.domain.taxdocument.Issuer;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuingPoint;
import com.alexastudillo.taxdocument.domain.taxdocument.SequenceNumber;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Objects;

@ApplicationScoped
public class SequenceNumberPersistenceMapper {
    private final PersistenceExceptionTranslator exceptionTranslator;
    private final TaxDocumentPersistenceMapper taxDocumentMapper;

    public SequenceNumberPersistenceMapper(
            PersistenceExceptionTranslator exceptionTranslator,
            TaxDocumentPersistenceMapper taxDocumentMapper) {
        this.exceptionTranslator = Objects.requireNonNull(exceptionTranslator, "exceptionTranslator must not be null");
        this.taxDocumentMapper = Objects.requireNonNull(taxDocumentMapper, "taxDocumentMapper must not be null");
    }

    public SequenceNumber toDomain(IssuanceSequenceEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        try {
            Issuer issuer = taxDocumentMapper.toDomain(entity.issuer());
            Establishment establishment = taxDocumentMapper.toDomain(entity.establishment());
            IssuingPoint issuingPoint = taxDocumentMapper.toDomain(entity.issuingPoint());
            return new SequenceNumber(
                    entity.sequenceNumber(),
                    DocumentType.valueOf(entity.documentType()),
                    issuer,
                    establishment,
                    issuingPoint);
        } catch (PersistenceFailure failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw exceptionTranslator.invalidPersistedTaxDocumentState();
        }
    }
}
