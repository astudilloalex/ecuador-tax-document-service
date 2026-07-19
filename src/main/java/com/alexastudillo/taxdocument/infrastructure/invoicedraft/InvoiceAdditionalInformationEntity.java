package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Persisted normalized additional-information entry. */
@Entity
@Table(name = "invoice_additional_information")
public class InvoiceAdditionalInformationEntity extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "invoice_draft_id", nullable = false)
  public UUID invoiceDraftId;

  @Column(nullable = false)
  public int position;

  @Column(length = 300, nullable = false)
  public String name;

  @Column(name = "canonical_name", length = 300, nullable = false)
  public String canonicalName;

  @Column(length = 300, nullable = false)
  public String value;
}
