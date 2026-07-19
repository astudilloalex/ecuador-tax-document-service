package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Infrastructure-only Invoice Draft root row. */
@Entity
@Table(name = "invoice_draft")
public class InvoiceDraftEntity extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "company_id", nullable = false)
  public UUID companyId;

  @Column(name = "emission_point_id", nullable = false)
  public UUID emissionPointId;

  @Column(name = "emission_date", nullable = false)
  public LocalDate emissionDate;

  @Column(name = "buyer_identification_type_code", length = 2, nullable = false)
  public String buyerIdentificationTypeCode;

  @Column(name = "buyer_identification_catalog_version", length = 64, nullable = false)
  public String buyerIdentificationCatalogVersion;

  @Column(name = "buyer_identification", length = 20, nullable = false)
  public String buyerIdentification;

  @Column(name = "buyer_legal_name", length = 300, nullable = false)
  public String buyerLegalName;

  @Column(name = "buyer_address", length = 300)
  public String buyerAddress;

  @Column(name = "buyer_email", length = 254)
  public String buyerEmail;

  @Column(name = "buyer_telephone", length = 20)
  public String buyerTelephone;

  @Column(length = 16, nullable = false)
  public String status;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 3, nullable = false)
  public String currency;

  @Column(name = "subtotal_before_taxes", precision = 17, scale = 2, nullable = false)
  public BigDecimal subtotalBeforeTaxes;

  @Column(name = "total_discount", precision = 17, scale = 2, nullable = false)
  public BigDecimal totalDiscount;

  @Column(name = "grand_total", precision = 17, scale = 2, nullable = false)
  public BigDecimal grandTotal;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;
}
