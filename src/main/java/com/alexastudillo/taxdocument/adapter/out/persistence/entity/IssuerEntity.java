package com.alexastudillo.taxdocument.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "issuers")
public class IssuerEntity {
    @Id
    @Column(name = "issuer_id", nullable = false, length = 64)
    private String issuerId;

    @Column(name = "legal_identifier", nullable = false, length = 32)
    private String legalIdentifier;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IssuerEntity() {
    }

    public IssuerEntity(String issuerId, String legalIdentifier, String legalName, String tradeName) {
        this.issuerId = issuerId;
        this.legalIdentifier = legalIdentifier;
        this.legalName = legalName;
        this.tradeName = tradeName;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String issuerId() {
        return issuerId;
    }

    public String legalIdentifier() {
        return legalIdentifier;
    }

    public String legalName() {
        return legalName;
    }

    public String tradeName() {
        return tradeName;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public void updateFrom(String legalIdentifier, String legalName, String tradeName) {
        this.legalIdentifier = legalIdentifier;
        this.legalName = legalName;
        this.tradeName = tradeName;
        this.updatedAt = Instant.now();
    }
}
