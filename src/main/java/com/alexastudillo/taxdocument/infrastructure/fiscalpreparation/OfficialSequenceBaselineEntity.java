package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Infrastructure-only persistence representation of an Official Sequence Baseline row. */
@Entity
@Table(name = "official_sequence_baseline")
public class OfficialSequenceBaselineEntity extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "company_id", nullable = false)
  public UUID companyId;

  @Column(name = "issuer_reference", length = 128, nullable = false)
  public String issuerReference;

  @Column(name = "establishment_reference", length = 128, nullable = false)
  public String establishmentReference;

  @Column(name = "emission_point_id", nullable = false)
  public UUID emissionPointId;

  @Column(name = "establishment_code", length = 3, nullable = false)
  public String establishmentCode;

  @Column(name = "emission_point_code", length = 3, nullable = false)
  public String emissionPointCode;

  @Column(name = "document_type_code", length = 2, nullable = false)
  public String documentTypeCode;

  @Column(name = "last_allocated", nullable = false)
  public int lastAllocated;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;
}
