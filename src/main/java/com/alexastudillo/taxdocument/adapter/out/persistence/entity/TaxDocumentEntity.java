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
import java.time.LocalDate;

@Entity
@Table(name = "tax_documents")
public class TaxDocumentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tax_document_id", nullable = false)
    private Long taxDocumentId;

    @Column(name = "access_key", nullable = false, length = 49)
    private String accessKey;

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

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "document_state", nullable = false, length = 32)
    private String documentState;

    @Column(name = "authorization_state", nullable = false, length = 32)
    private String authorizationState;

    @Column(name = "authorization_number", length = 128)
    private String authorizationNumber;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "issuance_mode", nullable = false, length = 32)
    private String issuanceMode;

    @Column(name = "external_request_id", length = 128)
    private String externalRequestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaxDocumentEntity() {
    }

    public static TaxDocumentEntity create() {
        return new TaxDocumentEntity();
    }

    public Long taxDocumentId() {
        return taxDocumentId;
    }

    public String accessKey() {
        return accessKey;
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

    public LocalDate issueDate() {
        return issueDate;
    }

    public String documentState() {
        return documentState;
    }

    public String authorizationState() {
        return authorizationState;
    }

    public String authorizationNumber() {
        return authorizationNumber;
    }

    public Instant authorizedAt() {
        return authorizedAt;
    }

    public String issuanceMode() {
        return issuanceMode;
    }

    public String externalRequestId() {
        return externalRequestId;
    }

    public void updateFrom(
            String accessKey,
            IssuerEntity issuer,
            EstablishmentEntity establishment,
            IssuingPointEntity issuingPoint,
            String documentType,
            String sequenceNumber,
            LocalDate issueDate,
            String documentState,
            String authorizationState,
            String authorizationNumber,
            Instant authorizedAt,
            String issuanceMode,
            String externalRequestId) {
        this.accessKey = accessKey;
        this.issuer = issuer;
        this.establishment = establishment;
        this.issuingPoint = issuingPoint;
        this.documentType = documentType;
        this.sequenceNumber = sequenceNumber;
        this.issueDate = issueDate;
        this.documentState = documentState;
        this.authorizationState = authorizationState;
        this.authorizationNumber = authorizationNumber;
        this.authorizedAt = authorizedAt;
        this.issuanceMode = issuanceMode;
        this.externalRequestId = externalRequestId;
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }
}
