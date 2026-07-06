package com.alexastudillo.taxdocument.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "issuing_points")
public class IssuingPointEntity {
    @Id
    @Column(name = "issuing_point_id", nullable = false, length = 64)
    private String issuingPointId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private EstablishmentEntity establishment;

    @Column(name = "issuing_point_code", nullable = false, length = 16)
    private String issuingPointCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IssuingPointEntity() {
    }

    public IssuingPointEntity(String issuingPointId, EstablishmentEntity establishment, String issuingPointCode) {
        this.issuingPointId = issuingPointId;
        this.establishment = establishment;
        this.issuingPointCode = issuingPointCode;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String issuingPointId() {
        return issuingPointId;
    }

    public EstablishmentEntity establishment() {
        return establishment;
    }

    public String issuingPointCode() {
        return issuingPointCode;
    }

    public void updateFrom(EstablishmentEntity establishment, String issuingPointCode) {
        this.establishment = establishment;
        this.issuingPointCode = issuingPointCode;
        this.updatedAt = Instant.now();
    }
}
