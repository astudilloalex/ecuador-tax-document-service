package com.alexastudillo.taxdocument.application.issuance;

import com.alexastudillo.taxdocument.domain.taxdocument.DocumentType;
import com.alexastudillo.taxdocument.domain.taxdocument.Establishment;
import com.alexastudillo.taxdocument.domain.taxdocument.IssueDate;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuanceMode;
import com.alexastudillo.taxdocument.domain.taxdocument.Issuer;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuingPoint;
import com.alexastudillo.taxdocument.domain.taxdocument.SequenceNumber;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Common application input model for future document-specific issuance use cases.
 */
public final class IssuanceRequest {
    private final DocumentType documentType;
    private final Issuer issuer;
    private final Establishment establishment;
    private final IssuingPoint issuingPoint;
    private final SequenceNumber sequenceNumber;
    private final IssueDate issueDate;
    private final IssuanceMode issuanceMode;
    private final String externalRequestId;
    private final String auditCorrelationId;

    public IssuanceRequest(
            DocumentType documentType,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint,
            SequenceNumber sequenceNumber,
            IssueDate issueDate,
            IssuanceMode issuanceMode,
            String externalRequestId,
            String auditCorrelationId) {
        this.documentType = Objects.requireNonNull(documentType, "documentType must not be null");
        this.issuer = Objects.requireNonNull(issuer, "issuer must not be null");
        this.establishment = Objects.requireNonNull(establishment, "establishment must not be null");
        this.issuingPoint = Objects.requireNonNull(issuingPoint, "issuingPoint must not be null");
        this.sequenceNumber = Objects.requireNonNull(sequenceNumber, "sequenceNumber must not be null");
        this.issueDate = Objects.requireNonNull(issueDate, "issueDate must not be null");
        this.issuanceMode = Objects.requireNonNull(issuanceMode, "issuanceMode must not be null");
        this.externalRequestId = normalize(externalRequestId);
        this.auditCorrelationId = normalize(auditCorrelationId);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    public DocumentType documentType() {
        return documentType;
    }

    public Issuer issuer() {
        return issuer;
    }

    public Establishment establishment() {
        return establishment;
    }

    public IssuingPoint issuingPoint() {
        return issuingPoint;
    }

    public SequenceNumber sequenceNumber() {
        return sequenceNumber;
    }

    public IssueDate issueDate() {
        return issueDate;
    }

    public IssuanceMode issuanceMode() {
        return issuanceMode;
    }

    public Optional<String> externalRequestId() {
        return Optional.ofNullable(externalRequestId);
    }

    public Optional<String> auditCorrelationId() {
        return Optional.ofNullable(auditCorrelationId);
    }

    public Set<String> idempotencyKeys() {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        keys.add("issuanceIdentity:%s:%s:%s:%s:%s".formatted(
                issuer.issuerId(),
                documentType.name(),
                establishment.code(),
                issuingPoint.code(),
                sequenceNumber.value()));
        externalRequestId().ifPresent(value -> keys.add("externalRequestId:" + value));
        return Set.copyOf(keys);
    }
}
