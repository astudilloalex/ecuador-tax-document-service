package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Persisted one-per-line IVA selection and calculated values. */
@Entity
@Table(name = "invoice_line_tax")
public class InvoiceLineTaxEntity extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "invoice_line_id", nullable = false)
  public UUID invoiceLineId;

  @Column(name = "tax_rule_id", nullable = false)
  public UUID taxRuleId;

  @Column(length = 16, nullable = false)
  public String family;

  @Column(length = 32, nullable = false)
  public String treatment;

  @Column(name = "official_tax_code", length = 8, nullable = false)
  public String officialTaxCode;

  @Column(name = "official_percentage_code", length = 8, nullable = false)
  public String officialPercentageCode;

  @Column(precision = 5, scale = 2, nullable = false)
  public BigDecimal rate;

  @Column(name = "tax_base", precision = 17, scale = 2, nullable = false)
  public BigDecimal taxBase;

  @Column(name = "tax_amount", precision = 17, scale = 2, nullable = false)
  public BigDecimal taxAmount;

  @Column(name = "catalog_version", length = 64, nullable = false)
  public String catalogVersion;
}
