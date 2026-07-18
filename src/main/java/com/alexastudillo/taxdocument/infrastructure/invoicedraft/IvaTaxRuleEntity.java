package com.alexastudillo.taxdocument.infrastructure.invoicedraft;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Read-only global versioned IVA rule. */
@Entity
@Table(name = "iva_tax_rule_catalog")
@IdClass(IvaTaxRuleEntity.CatalogId.class)
public class IvaTaxRuleEntity extends PanacheEntityBase {
  @Id public UUID id;

  @Column(length = 16, nullable = false)
  public String family;

  @Column(name = "official_tax_code", length = 8, nullable = false)
  public String officialTaxCode;

  @Column(name = "official_percentage_code", length = 8, nullable = false)
  public String officialPercentageCode;

  @Column(name = "official_label", length = 100, nullable = false)
  public String officialLabel;

  @Column(name = "display_name", length = 100, nullable = false)
  public String displayName;

  @Column(length = 32, nullable = false)
  public String treatment;

  @Column(precision = 5, scale = 2, nullable = false)
  public BigDecimal rate;

  @Column(name = "source_valid_from")
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
