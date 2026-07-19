package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Persisted Invoice Draft payment. */
@Entity
@Table(name = "invoice_payment")
public class InvoicePaymentEntity extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "invoice_draft_id", nullable = false)
  public UUID invoiceDraftId;

  @Column(name = "payment_method_id", nullable = false)
  public UUID paymentMethodId;

  @Column(name = "official_code", length = 8, nullable = false)
  public String officialCode;

  @Column(length = 100, nullable = false)
  public String name;

  @Column(precision = 17, scale = 2, nullable = false)
  public BigDecimal amount;

  @Column(name = "catalog_version", length = 64, nullable = false)
  public String catalogVersion;
}
