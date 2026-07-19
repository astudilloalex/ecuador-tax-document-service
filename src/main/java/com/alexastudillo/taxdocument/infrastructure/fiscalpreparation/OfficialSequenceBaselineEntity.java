package com.alexastudillo.taxdocument.infrastructure.fiscalpreparation;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Infrastructure-only persistence representation of an Official Sequence Baseline row. */
@Entity
@Table(name = "official_sequence_baseline")
@NullMarked
public class OfficialSequenceBaselineEntity extends PanacheEntityBase {
  @Id public @Nullable UUID id;

  @Column(name = "company_id", nullable = false)
  public @Nullable UUID companyId;

  @Column(name = "issuer_reference", length = 128, nullable = false)
  public @Nullable String issuerReference;

  @Column(name = "establishment_reference", length = 128, nullable = false)
  public @Nullable String establishmentReference;

  @Column(name = "emission_point_id", nullable = false)
  public @Nullable UUID emissionPointId;

  @Column(name = "establishment_code", length = 3, nullable = false)
  public @Nullable String establishmentCode;

  @Column(name = "emission_point_code", length = 3, nullable = false)
  public @Nullable String emissionPointCode;

  @Column(name = "document_type_code", length = 2, nullable = false)
  public @Nullable String documentTypeCode;

  @Column(name = "last_allocated", nullable = false)
  public int lastAllocated;

  @Column(name = "created_at", nullable = false)
  public @Nullable Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public @Nullable Instant updatedAt;
}
