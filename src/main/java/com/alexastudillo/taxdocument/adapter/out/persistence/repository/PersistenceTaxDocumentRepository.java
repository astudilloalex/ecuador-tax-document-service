package com.alexastudillo.taxdocument.adapter.out.persistence.repository;

import com.alexastudillo.taxdocument.adapter.out.persistence.entity.EstablishmentEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.IssuerEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.IssuingPointEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.TaxDocumentEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.error.PersistenceExceptionTranslator;
import com.alexastudillo.taxdocument.adapter.out.persistence.mapper.TaxDocumentPersistenceMapper;
import com.alexastudillo.taxdocument.application.error.PersistenceFailure;
import com.alexastudillo.taxdocument.application.port.out.TaxDocumentRepository;
import com.alexastudillo.taxdocument.domain.taxdocument.AccessKey;
import com.alexastudillo.taxdocument.domain.taxdocument.DocumentType;
import com.alexastudillo.taxdocument.domain.taxdocument.Establishment;
import com.alexastudillo.taxdocument.domain.taxdocument.Issuer;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuingPoint;
import com.alexastudillo.taxdocument.domain.taxdocument.SequenceNumber;
import com.alexastudillo.taxdocument.domain.taxdocument.TaxDocument;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class PersistenceTaxDocumentRepository implements TaxDocumentRepository {
    private final EntityManager entityManager;
    private final TaxDocumentPersistenceMapper mapper;
    private final PersistenceExceptionTranslator exceptionTranslator;

    public PersistenceTaxDocumentRepository(
            EntityManager entityManager,
            TaxDocumentPersistenceMapper mapper,
            PersistenceExceptionTranslator exceptionTranslator) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.exceptionTranslator = Objects.requireNonNull(exceptionTranslator, "exceptionTranslator must not be null");
    }

    @Override
    @Transactional
    public Uni<TaxDocument> save(TaxDocument taxDocument) {
        Objects.requireNonNull(taxDocument, "taxDocument must not be null");
        try {
            IssuerEntity issuer = issuerEntity(taxDocument.issuer());
            EstablishmentEntity establishment = establishmentEntity(taxDocument.establishment(), issuer);
            IssuingPointEntity issuingPoint = issuingPointEntity(taxDocument.issuingPoint(), establishment);

            Optional<TaxDocumentEntity> existingByAccessKey = findEntityByAccessKey(taxDocument.accessKey());
            Optional<TaxDocumentEntity> existingByIdentity = findEntityByIssuanceIdentity(
                    taxDocument.documentType(),
                    taxDocument.issuer(),
                    taxDocument.establishment(),
                    taxDocument.issuingPoint(),
                    taxDocument.sequenceNumber());

            TaxDocumentEntity target = resolveSaveTarget(existingByAccessKey, existingByIdentity);
            if (target == null) {
                target = mapper.toEntity(taxDocument, issuer, establishment, issuingPoint);
                entityManager.persist(target);
            } else {
                mapper.updateEntity(target, taxDocument, issuer, establishment, issuingPoint);
            }

            entityManager.flush();
            return Uni.createFrom().item(mapper.toDomain(target));
        } catch (PersistenceFailure failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw exceptionTranslator.translate(failure);
        }
    }

    @Override
    @Transactional
    public Uni<Optional<TaxDocument>> findByAccessKey(AccessKey accessKey) {
        Objects.requireNonNull(accessKey, "accessKey must not be null");
        try {
            return Uni.createFrom().item(findEntityByAccessKey(accessKey).map(mapper::toDomain));
        } catch (PersistenceFailure failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw exceptionTranslator.translate(failure);
        }
    }

    @Override
    @Transactional
    public Uni<Optional<TaxDocument>> findByIssuanceIdentity(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            SequenceNumber sequenceNumber) {
        try {
            Optional<TaxDocument> taxDocument =
                    findEntityByIssuanceIdentity(documentType, issuer, establishment, issuingPoint, sequenceNumber)
                            .map(mapper::toDomain);
            return Uni.createFrom().item(taxDocument);
        } catch (PersistenceFailure failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw exceptionTranslator.translate(failure);
        }
    }

    @Override
    @Transactional
    public Uni<Boolean> existsByAccessKey(AccessKey accessKey) {
        Objects.requireNonNull(accessKey, "accessKey must not be null");
        Long count = entityManager.createQuery(
                        "select count(t) from TaxDocumentEntity t where t.accessKey = :accessKey",
                        Long.class)
                .setParameter("accessKey", accessKey.value())
                .getSingleResult();
        return Uni.createFrom().item(count > 0);
    }

    @Override
    @Transactional
    public Uni<Boolean> existsByIssuanceIdentity(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            SequenceNumber sequenceNumber) {
        Long count = entityManager.createQuery(
                        """
                        select count(t)
                        from TaxDocumentEntity t
                        where t.documentType = :documentType
                          and t.issuer.issuerId = :issuerId
                          and t.establishment.establishmentId = :establishmentId
                          and t.issuingPoint.issuingPointId = :issuingPointId
                          and t.sequenceNumber = :sequenceNumber
                        """,
                        Long.class)
                .setParameter("documentType", documentType.name())
                .setParameter("issuerId", issuer.issuerId())
                .setParameter("establishmentId", establishment.establishmentId())
                .setParameter("issuingPointId", issuingPoint.issuingPointId())
                .setParameter("sequenceNumber", sequenceNumber.value())
                .getSingleResult();
        return Uni.createFrom().item(count > 0);
    }

    private TaxDocumentEntity resolveSaveTarget(
            Optional<TaxDocumentEntity> existingByAccessKey,
            Optional<TaxDocumentEntity> existingByIdentity) {
        if (existingByAccessKey.isEmpty() && existingByIdentity.isEmpty()) {
            return null;
        }
        if (existingByAccessKey.isPresent() && existingByIdentity.isPresent()) {
            TaxDocumentEntity byAccessKey = existingByAccessKey.orElseThrow();
            TaxDocumentEntity byIdentity = existingByIdentity.orElseThrow();
            if (Objects.equals(byAccessKey.taxDocumentId(), byIdentity.taxDocumentId())) {
                return byAccessKey;
            }
            throw exceptionTranslator.duplicateAccessKey();
        }
        if (existingByAccessKey.isPresent()) {
            throw exceptionTranslator.duplicateAccessKey();
        }
        throw exceptionTranslator.duplicateIssuanceIdentity();
    }

    private Optional<TaxDocumentEntity> findEntityByAccessKey(AccessKey accessKey) {
        return entityManager.createQuery(
                        "select t from TaxDocumentEntity t where t.accessKey = :accessKey",
                        TaxDocumentEntity.class)
                .setParameter("accessKey", accessKey.value())
                .getResultStream()
                .findFirst();
    }

    private Optional<TaxDocumentEntity> findEntityByIssuanceIdentity(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            SequenceNumber sequenceNumber) {
        return entityManager.createQuery(
                        """
                        select t
                        from TaxDocumentEntity t
                        where t.documentType = :documentType
                          and t.issuer.issuerId = :issuerId
                          and t.establishment.establishmentId = :establishmentId
                          and t.issuingPoint.issuingPointId = :issuingPointId
                          and t.sequenceNumber = :sequenceNumber
                        """,
                        TaxDocumentEntity.class)
                .setParameter("documentType", documentType.name())
                .setParameter("issuerId", issuer.issuerId())
                .setParameter("establishmentId", establishment.establishmentId())
                .setParameter("issuingPointId", issuingPoint.issuingPointId())
                .setParameter("sequenceNumber", sequenceNumber.value())
                .getResultStream()
                .findFirst();
    }

    private IssuerEntity issuerEntity(Issuer issuer) {
        IssuerEntity entity = entityManager.find(IssuerEntity.class, issuer.issuerId());
        if (entity == null) {
            entity = new IssuerEntity(
                    issuer.issuerId(),
                    issuer.legalIdentifier(),
                    issuer.legalName(),
                    issuer.tradeName());
            entityManager.persist(entity);
            return entity;
        }
        entity.updateFrom(issuer.legalIdentifier(), issuer.legalName(), issuer.tradeName());
        return entity;
    }

    private EstablishmentEntity establishmentEntity(Establishment establishment, IssuerEntity issuer) {
        EstablishmentEntity entity = entityManager.find(EstablishmentEntity.class, establishment.establishmentId());
        if (entity == null) {
            entity = new EstablishmentEntity(establishment.establishmentId(), issuer, establishment.code());
            entityManager.persist(entity);
            return entity;
        }
        if (!entity.issuer().issuerId().equals(issuer.issuerId())) {
            throw exceptionTranslator.invalidPersistenceRelationship();
        }
        entity.updateFrom(issuer, establishment.code());
        return entity;
    }

    private IssuingPointEntity issuingPointEntity(IssuingPoint issuingPoint, EstablishmentEntity establishment) {
        IssuingPointEntity entity = entityManager.find(IssuingPointEntity.class, issuingPoint.issuingPointId());
        if (entity == null) {
            entity = new IssuingPointEntity(issuingPoint.issuingPointId(), establishment, issuingPoint.code());
            entityManager.persist(entity);
            return entity;
        }
        if (!entity.establishment().establishmentId().equals(establishment.establishmentId())) {
            throw exceptionTranslator.invalidPersistenceRelationship();
        }
        entity.updateFrom(establishment, issuingPoint.code());
        return entity;
    }
}
