package com.alexastudillo.taxdocument.adapter.out.persistence.repository;

import com.alexastudillo.taxdocument.adapter.out.persistence.entity.EstablishmentEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.IssuanceSequenceEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.IssuerEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.entity.IssuingPointEntity;
import com.alexastudillo.taxdocument.adapter.out.persistence.error.PersistenceExceptionTranslator;
import com.alexastudillo.taxdocument.adapter.out.persistence.mapper.SequenceNumberPersistenceMapper;
import com.alexastudillo.taxdocument.application.error.PersistenceFailure;
import com.alexastudillo.taxdocument.application.port.out.SequenceNumberPort;
import com.alexastudillo.taxdocument.domain.taxdocument.DocumentType;
import com.alexastudillo.taxdocument.domain.taxdocument.Establishment;
import com.alexastudillo.taxdocument.domain.taxdocument.Issuer;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuingPoint;
import com.alexastudillo.taxdocument.domain.taxdocument.SequenceNumber;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class PersistenceSequenceNumberAdapter implements SequenceNumberPort {
    private final EntityManager entityManager;
    private final SequenceNumberPersistenceMapper mapper;
    private final PersistenceExceptionTranslator exceptionTranslator;

    public PersistenceSequenceNumberAdapter(
            EntityManager entityManager,
            SequenceNumberPersistenceMapper mapper,
            PersistenceExceptionTranslator exceptionTranslator) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.exceptionTranslator = Objects.requireNonNull(exceptionTranslator, "exceptionTranslator must not be null");
    }

    @Override
    @Transactional
    public Uni<SequenceNumber> reserve(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            String requestedValue) {
        try {
            Optional<IssuanceSequenceEntity> existing =
                    findEntity(documentType, issuer, establishment, issuingPoint, requestedValue);
            if (existing.isPresent()) {
                return Uni.createFrom().item(mapper.toDomain(existing.orElseThrow()));
            }

            IssuerEntity issuerEntity = issuerEntity(issuer);
            EstablishmentEntity establishmentEntity = establishmentEntity(establishment, issuerEntity);
            IssuingPointEntity issuingPointEntity = issuingPointEntity(issuingPoint, establishmentEntity);
            IssuanceSequenceEntity entity = new IssuanceSequenceEntity(
                    issuerEntity,
                    establishmentEntity,
                    issuingPointEntity,
                    documentType.name(),
                    requestedValue);
            entityManager.persist(entity);
            entityManager.flush();
            return Uni.createFrom().item(mapper.toDomain(entity));
        } catch (PersistenceFailure failure) {
            throw failure;
        } catch (RuntimeException failure) {
            throw exceptionTranslator.translate(failure);
        }
    }

    @Override
    @Transactional
    public Uni<Boolean> isAvailable(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            String value) {
        return Uni.createFrom().item(findEntity(documentType, issuer, establishment, issuingPoint, value).isEmpty());
    }

    private Optional<IssuanceSequenceEntity> findEntity(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            String sequenceNumber) {
        return entityManager.createQuery(
                        """
                        select s
                        from IssuanceSequenceEntity s
                        where s.documentType = :documentType
                          and s.issuer.issuerId = :issuerId
                          and s.establishment.establishmentId = :establishmentId
                          and s.issuingPoint.issuingPointId = :issuingPointId
                          and s.sequenceNumber = :sequenceNumber
                        """,
                        IssuanceSequenceEntity.class)
                .setParameter("documentType", documentType.name())
                .setParameter("issuerId", issuer.issuerId())
                .setParameter("establishmentId", establishment.establishmentId())
                .setParameter("issuingPointId", issuingPoint.issuingPointId())
                .setParameter("sequenceNumber", sequenceNumber)
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
