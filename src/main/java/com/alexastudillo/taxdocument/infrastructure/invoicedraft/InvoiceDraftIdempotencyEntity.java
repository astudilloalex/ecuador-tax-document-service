package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/** Company-scoped durable idempotency binding. */
@Entity
@Table(name = "invoice_draft_idempotency")
@IdClass(InvoiceDraftIdempotencyEntity.BindingId.class)
public class InvoiceDraftIdempotencyEntity extends PanacheEntityBase {
  @Id
  @Column(name = "company_id", nullable = false)
  public UUID companyId;

  @Id
  @Column(name = "idempotency_key_hash", columnDefinition = "bytea", nullable = false)
  public byte[] idempotencyKeyHash;

  @Column(name = "request_fingerprint", columnDefinition = "bytea", nullable = false)
  public byte[] requestFingerprint;

  @Column(name = "normalization_version", nullable = false)
  public short normalizationVersion;

  @Column(name = "invoice_draft_id", nullable = false)
  public UUID invoiceDraftId;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  public static final class BindingId implements Serializable {
    private static final long serialVersionUID = 1L;
    public UUID companyId;
    public byte[] idempotencyKeyHash;

    public BindingId() {}

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof BindingId that)) {
        return false;
      }
      return Objects.equals(companyId, that.companyId)
          && Arrays.equals(idempotencyKeyHash, that.idempotencyKeyHash);
    }

    @Override
    public int hashCode() {
      return 31 * Objects.hashCode(companyId) + Arrays.hashCode(idempotencyKeyHash);
    }
  }
}
