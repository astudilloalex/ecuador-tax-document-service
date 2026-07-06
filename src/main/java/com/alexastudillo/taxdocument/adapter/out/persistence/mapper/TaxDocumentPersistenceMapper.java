package com.alexastudillo.taxdocument.adapter.out.persistence.mapper;

import com.alexastudillo.taxdocument.adapter.out.persistence.entity.EstablishmentEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.IssuerEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.IssuingPointEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.TaxDocumentEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.error.PersistenceExceptionTranslator;
import com.alexastudillo.taxdocument.application.error.PersistenceFailure;
import com.alexastudillo.taxdocument.domain.taxdocument.AccessKey;
import com.alexastudillo.taxdocument.domain.taxdocument.AuthorizationNumber;
import com.alexastudillo.taxdocument.domain.taxdocument.AuthorizationState;
import com.alexastudillo.taxdocument.domain.taxdocument.AuthorizedAt;
import com.alexastudillo.taxdocument.domain.taxdocument.DocumentState;
import com.alexastudillo.taxdocument.domain.taxdocument.DocumentType;
import com.alexastudillo.taxdocument.domain.taxdocument.Establishment;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuanceMode;
import com.alexastudillo.taxdocument.domain.taxdocument.IssueDate;
import com.alexastudillo.taxdocument.domain.taxdocument.Issuer;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuingPoint;
import com.alexastudillo.taxdocument.domain.taxdocument.SequenceNumber;
import com.alexastudillo.taxdocument.domain.taxdocument.TaxDocument;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Objects;

@ApplicationScoped
public class TaxDocumentPersistenceMapper {
    private final PersistenceExceptionTranslator exceptionTranslator;

    public TaxDocumentPersistenceMapper(PersistenceExceptionTranslator exceptionTranslator) {
        this.exceptionTranslator = Objects.requireNonNull(exceptionTranslator, "exceptionTranslator must not be null");
    }

    public TaxDocumentEntity toEntity(
            TaxDocument taxDocument,
            IssuerEntity issuer,
            EstablishmentEntity establishment,
            IssuingPointEntity issuingPoint) {
        TaxDocumentEntity entity = TaxDocumentEntity.create();
        updateEntity(entity, taxDocument, issuer, establishment, issuingPoint);
        return entity;
    }

    public void updateEntity(
            TaxDocumentEntity entity,
            TaxDocument taxDocument,
            IssuerEntity issuer,
            EstablishmentEntity establishment,
            IssuingPointEntity issuingPoint) {
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(taxDocument, "taxDocument must not be null");
        entity.updateFrom(
                taxDocument.accessKey().value(),
                issuer,
                establishment,
                issuingPoint,
                taxDocument.documentType().name(),
                taxDocument.sequenceNumber().value(),
                taxDocument.issueDate().value(),
                taxDocument.documentState().name(),
                taxDocument.authorizationState().name(),
                taxDocument.authorizationNumber().map(AuthorizationNumber::value).orElse(null),
                taxDocument.authorizedAt().map(AuthorizedAt::value).orElse(null),
                taxDocument.issuanceMode().name(),
                taxDocument.externalRequestId().orElse(null));
    }

    public TaxDocument toDomain(TaxDocumentEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        try {
            validateRelationships(entity);
            Issuer issuer = toDomain(entity.issuer());
            Establishment establishment = toDomain(entity.establishment());
            IssuingPoint issuingPoint = toDomain(entity.issuingPoint());
            DocumentType documentType = DocumentType.valueOf(entity.documentType());
            SequenceNumber sequenceNumber =
                    new SequenceNumber(entity.sequenceNumber(), documentType, issuer, establishment, issuingPoint);
            return TaxDocument.restore(
                    documentType,
                    issuer,
                    establishment,
                    issuingPoint,
                    sequenceNumber,
                    new AccessKey(entity.accessKey()),
                    new IssueDate(entity.issueDate()),
                    DocumentState.valueOf(entity.documentState()),
                    AuthorizationState.valueOf(entity.authorizationState()),
                    authorizationNumber(entity.authorizationNumber()),
                    authorizedAt(entity.authorizedAt()),
                    IssuanceMode.valueOf(entity.issuanceMode()),
                    entity.externalRequestId());
        } catch (PersistenceFailure failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw exceptionTranslator.invalidPersistedTaxDocumentState();
        }
    }

    public Issuer toDomain(IssuerEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        return new Issuer(entity.issuerId(), entity.legalIdentifier(), entity.legalName(), entity.tradeName());
    }

    public Establishment toDomain(EstablishmentEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        return new Establishment(entity.establishmentId(), entity.establishmentCode(), entity.issuer().issuerId());
    }

    public IssuingPoint toDomain(IssuingPointEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        return new IssuingPoint(
                entity.issuingPointId(),
                entity.issuingPointCode(),
                entity.establishment().establishmentId());
    }

    private void validateRelationships(TaxDocumentEntity entity) {
        if (!entity.establishment().issuer().issuerId().equals(entity.issuer().issuerId())) {
            throw exceptionTranslator.invalidPersistenceRelationship();
        }
        if (!entity.issuingPoint().establishment().establishmentId().equals(entity.establishment().establishmentId())) {
            throw exceptionTranslator.invalidPersistenceRelationship();
        }
    }

    private static AuthorizationNumber authorizationNumber(String value) {
        return value == null ? null : new AuthorizationNumber(value);
    }

    private static AuthorizedAt authorizedAt(java.time.Instant value) {
        return value == null ? null : new AuthorizedAt(value);
    }
}
