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
@Table(name = "establishments")
public class EstablishmentEntity {
    @Id
    @Column(name = "establishment_id", nullable = false, length = 64)
    private String establishmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issuer_id", nullable = false)
    private IssuerEntity issuer;

    @Column(name = "establishment_code", nullable = false, length = 16)
    private String establishmentCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EstablishmentEntity() {
    }

    public EstablishmentEntity(String establishmentId, IssuerEntity issuer, String establishmentCode) {
        this.establishmentId = establishmentId;
        this.issuer = issuer;
        this.establishmentCode = establishmentCode;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String establishmentId() {
        return establishmentId;
    }

    public IssuerEntity issuer() {
        return issuer;
    }

    public String establishmentCode() {
        return establishmentCode;
    }

    public void updateFrom(IssuerEntity issuer, String establishmentCode) {
        this.issuer = issuer;
        this.establishmentCode = establishmentCode;
        this.updatedAt = Instant.now();
    }
}
