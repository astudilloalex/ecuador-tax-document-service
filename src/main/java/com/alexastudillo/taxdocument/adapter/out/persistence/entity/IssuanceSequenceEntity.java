package com.alexastudillo.taxdocument.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "issuance_sequences")
public class IssuanceSequenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issuance_sequence_id", nullable = false)
    private Long issuanceSequenceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issuer_id", nullable = false)
    private IssuerEntity issuer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private EstablishmentEntity establishment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issuing_point_id", nullable = false)
    private IssuingPointEntity issuingPoint;

    @Column(name = "document_type", nullable = false, length = 32)
    private String documentType;

    @Column(name = "sequence_number", nullable = false, length = 20)
    private String sequenceNumber;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IssuanceSequenceEntity() {
    }

    public IssuanceSequenceEntity(
            IssuerEntity issuer,
            EstablishmentEntity establishment,
            IssuingPointEntity issuingPoint,
            String documentType,
            String sequenceNumber) {
        this.issuer = issuer;
        this.establishment = establishment;
        this.issuingPoint = issuingPoint;
        this.documentType = documentType;
        this.sequenceNumber = sequenceNumber;
        Instant now = Instant.now();
        this.reservedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long issuanceSequenceId() {
        return issuanceSequenceId;
    }

    public IssuerEntity issuer() {
        return issuer;
    }

    public EstablishmentEntity establishment() {
        return establishment;
    }

    public IssuingPointEntity issuingPoint() {
        return issuingPoint;
    }

    public String documentType() {
        return documentType;
    }

    public String sequenceNumber() {
        return sequenceNumber;
    }
}
