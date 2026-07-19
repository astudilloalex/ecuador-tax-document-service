package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Read-only global versioned payment-method row. */
@Entity
@Table(name = "payment_method_catalog")
@IdClass(PaymentMethodEntity.CatalogId.class)
public class PaymentMethodEntity extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "official_code", length = 8, nullable = false)
  public String officialCode;

  @Column(name = "official_label", length = 160, nullable = false)
  public String officialLabel;

  @Column(name = "display_name", length = 100, nullable = false)
  public String displayName;

  @Column(name = "source_valid_from", nullable = false)
  public LocalDate sourceValidFrom;

  @Column(name = "source_valid_to")
  public LocalDate sourceValidTo;

  @Column(name = "target_valid_from", nullable = false)
  public LocalDate targetValidFrom;

  @Column(name = "target_valid_to")
  public LocalDate targetValidTo;

  @Column(nullable = false)
  public boolean active;

  @Id
  @Column(name = "catalog_version", length = 64, nullable = false)
  public String catalogVersion;

  @Column(name = "official_source_uri", columnDefinition = "text", nullable = false)
  public String officialSourceUri;

  @Column(name = "official_source_locator", length = 128, nullable = false)
  public String officialSourceLocator;

  public static final class CatalogId implements Serializable {
    private static final long serialVersionUID = 1L;
    public UUID id;
    public String catalogVersion;

    public CatalogId() {}

    @Override
    public boolean equals(Object other) {
      return other instanceof CatalogId that
          && Objects.equals(id, that.id)
          && Objects.equals(catalogVersion, that.catalogVersion);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, catalogVersion);
    }
  }
}
