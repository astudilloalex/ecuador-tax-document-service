package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Ordered persisted Invoice Draft line. */
@Entity
@Table(name = "invoice_line")
public class InvoiceLineEntity extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "invoice_draft_id", nullable = false)
  public UUID invoiceDraftId;

  @Column(nullable = false)
  public int position;

  @Column(name = "product_code", length = 25, nullable = false)
  public String productCode;

  @Column(length = 300, nullable = false)
  public String description;

  @Column(precision = 12, scale = 6, nullable = false)
  public BigDecimal quantity;

  @Column(name = "unit_price", precision = 12, scale = 6, nullable = false)
  public BigDecimal unitPrice;

  @Column(precision = 17, scale = 2, nullable = false)
  public BigDecimal discount;

  @Column(name = "gross_amount", precision = 17, scale = 2, nullable = false)
  public BigDecimal grossAmount;

  @Column(name = "net_amount", precision = 17, scale = 2, nullable = false)
  public BigDecimal netAmount;

  @Column(name = "line_total", precision = 17, scale = 2, nullable = false)
  public BigDecimal lineTotal;
}
