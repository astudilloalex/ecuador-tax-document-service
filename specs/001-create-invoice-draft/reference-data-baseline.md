# Reference Data Baseline: Create Invoice Draft

**Feature**: `001-create-invoice-draft`
**Target catalog version**: `SRI-OFFLINE-2.32-TARGET-1`
**Target adoption date**: 2026-07-12
**Approval status**: **APPROVED FOR TASK GENERATION**

## Approval Scope

This artifact is the executable planning baseline for the buyer-identification types, IVA tax
rules, and payment methods supported by Create Invoice Draft. It separates official facts from
target-service decisions. An official SRI code, label, source date, or applicability statement is
recorded as an official fact. A UUID, English display name, validation strategy, target validity
interval, and target `active` flag is a governed target-service decision unless an official source
explicitly states otherwise.

All target rows use `target_valid_from = 2026-07-12`, `target_valid_to = null`, and `active = true`.
The target date is the adoption date of this service baseline; it MUST NOT be described as the
legal origin date of an SRI rule. Official source dates are retained separately.

## Authoritative Source Register

| ID | Exact title | Publisher | Version/date | Exact locator | Supported official fact |
|----|-------------|-----------|--------------|---------------|-------------------------|
| `SRI-FE-CURRENT` | Facturación Electrónica — Información técnica y guías | Servicio de Rentas Internas (SRI) | Current page observed 2026-07-12; technical-sheet link updated November 2025 | “Esquema actual” and “Recuerde” sections | Publishes Technical Sheet v2.32 and states that some SRI-issued RUC classes have no specific validation algorithm. |
| `SRI-FT-2.32` | Ficha Técnica de Comprobantes Electrónicos Esquema Off-line | Servicio de Rentas Internas (SRI) | Version 2.32, document update 2025-10-08 | §5.7 Table 6, p. 11; §9.10, p. 27; §9.12 Table 16 and §9.13 Table 17, pp. 27–28; invoice XML field table, pp. 65–66; Table 24, p. 79 | Official buyer-type codes and labels; final-consumer identifier and USD 50.00 ceiling; invoice buyer identifier source prose says “alphanumeric” with maximum 20 characters, resolved for this target baseline to the executable ASCII rule below; IVA tax and percentage codes; payment codes, labels, and source dates. |
| `SRI-IVA-CURRENT` | Impuesto al Valor Agregado (IVA) | Servicio de Rentas Internas (SRI) | Current page observed 2026-07-12 | “¿Cuál es la tarifa?”, lines identifying 0%, 13%, 5%, non-object and exempt transactions | Current public guidance identifies 0% and 13% for goods/services, 5% for construction materials, non-object transactions, and exempt transactions. |
| `SRI-RES-24-13` | Resolución Nro. NAC-DGERCGC24-00000013 | Servicio de Rentas Internas (SRI) | Signed 2024-03-28; effective on publication in Registro Oficial Supplement 529, 2024-04-01 | Article 1 and Final Provision, pp. 3–4 | Local transfers of the listed construction materials use IVA 5% from the resolution's effective date. |
| `SRI-CIR-25-06` | Circular No. NAC-DGECCGC25-00000006 | Servicio de Rentas Internas (SRI) | Signed 2025-12-26 | §2 “Pronunciamiento”, pp. 2–3 | States that IVA 15% remains applicable until modified by executive decree. |
| `SRI-CIR-26-05` | Circular Nro. NAC-DGECCGC26-00000005 | Servicio de Rentas Internas (SRI) | Signed 2026-04-02; published 2026-04-06 | §2 “Pronunciamiento”, pp. 2–3 | Identifies concrete 2026 transactions to which IVA 15% applies and distinguishes them from 0% transactions. |

Official URLs:

- `SRI-FE-CURRENT`: https://www.sri.gob.ec/facturacion-electronica
- `SRI-FT-2.32`: https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf
- `SRI-IVA-CURRENT`: https://www.sri.gob.ec/impuesto-al-valor-agregado-iva
- `SRI-RES-24-13`: https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar?id=6b8588f2-a4bf-44bb-ac40-085391ba2aed&nombre=NAC-DGERCGC24-00000013.pdf
- `SRI-CIR-25-06`: https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar?id=236482f4-6125-42fd-b073-62c99d08233d&nombre=NAC-DGECCGC25-00000006.pdf
- `SRI-CIR-26-05`: https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar?id=a3e4257d-ba2e-4ec7-89c7-635fa764b22a&nombre=NAC-DGECCGC26-00000005.pdf

## Target UUID Namespace and Derivation

The approved reference namespace is:

```text
UUIDv5(UUID.NAMESPACE_DNS, "com.alexastudillo.taxdocument.reference-data.v1")
= 32576bbf-b70d-5c24-98ff-d5f9b48e8826
```

Tax-rule identifiers use:

```text
UUIDv5(referenceDataNamespace,
  "tax-rule|SRI-OFFLINE-2.32|<tax-code>|<percentage-code>|<rate>|<treatment>")
```

Payment-method identifiers use:

```text
UUIDv5(referenceDataNamespace,
  "payment-method|SRI-OFFLINE-2.32|<official-code>")
```

Rates in UUID names use exactly two decimal digits. The values below are calculated once and
published as immutable contract identifiers. Runtime UUID generation and random placeholders are
prohibited. Changing an identifier requires a new governed reference-data version and MUST NOT
silently mutate an existing mapping.

## Approved Buyer-Identification Types

Buyer-identification types use the official two-character code as their canonical identifier; no
UUID is introduced.

| Code | Exact official Spanish label | Canonical English target name | Target validation strategy | Source validity evidence | Target validity | Active | Catalog version | Exact evidence | Status |
|------|------------------------------|-------------------------------|----------------------------|--------------------------|-----------------|--------|-----------------|----------------|--------|
| `04` | RUC | RUC | `FORMAT_ONLY_NUMERIC_13`: exactly 13 ASCII digits; no checksum in this feature | No row dates in Table 6 | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | `SRI-FT-2.32` Table 6 and Table 5 RUC shape; `SRI-FE-CURRENT` RUC algorithm notice | APPROVED |
| `05` | CÉDULA | Ecuadorian identity card | `FORMAT_ONLY_NUMERIC_10`: exactly 10 ASCII digits; no checksum in this feature | No row dates in Table 6 | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | `SRI-FT-2.32` Table 6 and Table 5 Cédula shape | APPROVED |
| `06` | PASAPORTE | Passport | `FORMAT_ONLY_ALPHANUMERIC_1_TO_20`: case-sensitive ASCII `^[A-Za-z0-9]{1,20}$` after one leading/trailing SP/HTAB trim; no other normalization, checksum, or country rule | No row dates in Table 6 | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | `SRI-FT-2.32` Table 6 and invoice `identificacionComprador` field, pp. 65–66 | APPROVED |
| `07` | VENTA A CONSUMIDOR FINAL | Final consumer sale | `FINAL_CONSUMER_EXACT`: identifier `9999999999999`, buyer name `CONSUMIDOR FINAL`, rounded grand total no greater than USD `50.00` | No row dates in Table 6; threshold stated in §9.10 | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | `SRI-FT-2.32` Table 6 note and §9.10, p. 27 | APPROVED |
| `08` | IDENTIFICACIÓN DEL EXTERIOR | Foreign identification | `FORMAT_ONLY_ALPHANUMERIC_1_TO_20`: tax-authority identifier from the buyer's fiscal-residence country; case-sensitive ASCII `^[A-Za-z0-9]{1,20}$` after one leading/trailing SP/HTAB trim; no other normalization or checksum | No row dates in Table 6 | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | `SRI-FT-2.32` Table 6 note and invoice `identificacionComprador` field, pp. 65–66 | APPROVED |

`FORMAT_ONLY` is an approved feature-scope decision, not an SRI checksum algorithm. Draft creation
does not perform online registry existence checks or buyer-name registry matching. Checksum and
registry verification are outside this draft feature; they are not hidden implementation work.
For codes `06` and `08`, comparison is case-sensitive. Valid examples are `A1234567` and `EC9Z`;
invalid examples are `A-123`, `A 123`, `Á123`, empty, and 21 characters. OpenAPI, Java/API
validation, domain rules, locale-independent PostgreSQL constraints, and test vectors MUST enforce
the same target repertoire; a broader Unicode or locale-dependent character class is not approved.

## Approved IVA Tax Rules

The SRI sources publish representation codes and applicability evidence, not this service's UUIDs,
English names, target activation dates, or active flags. Every row below is an available target
selection, not an automatic product-classification result. The upstream billing workflow selects
the appropriate published rule; this service validates existence, target activity, and target
effectivity only.

| taxRuleId | Tax code | Percentage code | Exact official Spanish label | Canonical English target name | Treatment | Rate | Applicability evidence | Source validity evidence | Exact evidence | Target validity | Active | Catalog version | Status |
|-----------|----------|-----------------|------------------------------|-------------------------------|-----------|------|------------------------|--------------------------|----------------|-----------------|--------|-----------------|--------|
| `84cb3f03-574b-54de-9e73-efb8d485476a` | `2` | `0` | 0% | IVA 0% | `ZERO_RATE` | `0.00` | Transactions governed by IVA 0% rules | No row dates in Table 17; current guidance observed 2026-07-12 | `SRI-FT-2.32` Table 17, p. 28; `SRI-IVA-CURRENT` “¿Cuál es la tarifa?” | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | APPROVED |
| `2b31de9b-20f2-50c7-aeff-fed9babfe112` | `2` | `5` | 5% | IVA 5% | `PERCENTAGE_RATE` | `5.00` | Listed construction-material transfers; caller chooses only when applicable | Source valid from 2024-04-01; no source end date | `SRI-FT-2.32` Table 17, p. 28; `SRI-RES-24-13` Article 1 and Final Provision, pp. 3–4 | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | APPROVED |
| `3aa0fb56-17ad-5310-a10c-64c1f6dbe2fb` | `2` | `10` | 13% | IVA 13% | `PERCENTAGE_RATE` | `13.00` | Current SRI public IVA guidance identifies 13% for goods/services except construction materials | Current guidance observed 2026-07-12; no stated row start/end | `SRI-FT-2.32` Table 17, p. 28; `SRI-IVA-CURRENT` “¿Cuál es la tarifa?” | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | APPROVED |
| `5b34b038-931c-50e3-a84c-10af272fdcd4` | `2` | `4` | 15% | IVA 15% | `PERCENTAGE_RATE` | `15.00` | Official circulars identify active 15% contexts; this row is not labeled a universal general rate | Circular dated 2025-12-26 and concrete applicability circular dated 2026-04-02; no stated end date | `SRI-FT-2.32` Table 17, p. 28; `SRI-CIR-25-06` §2, pp. 2–3; `SRI-CIR-26-05` §2, pp. 2–3 | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | APPROVED |
| `a70a77f5-1176-5b0b-a539-74ead416a3ff` | `2` | `6` | No Objeto de Impuesto | Not subject to IVA | `NOT_SUBJECT` | `0.00` | Transactions outside the IVA taxable event | No row dates in Table 17; current guidance observed 2026-07-12 | `SRI-FT-2.32` Table 17, p. 28; `SRI-IVA-CURRENT` “¿Cuál es la tarifa?” | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | APPROVED |
| `a7eeaf77-dbdc-5f99-9bdd-d783c072a7de` | `2` | `7` | Exento de IVA | Exempt from IVA | `EXEMPT` | `0.00` | Transactions legally exempt from IVA | No row dates in Table 17; current guidance observed 2026-07-12 | `SRI-FT-2.32` Table 17, p. 28; `SRI-IVA-CURRENT` “¿Cuál es la tarifa?” | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | APPROVED |

The concurrent publication of 13% guidance and 15% circular evidence is recorded as an
applicability distinction, not resolved by declaring either percentage universal. The service
does not infer which product or transaction qualifies; the upstream billing workflow owns that
selection. The 15% acceptance vector is a deterministic mathematical rounding vector only.

## Approved Payment Methods

Table 24's dash end marker is interpreted as `source_valid_to = null`. English names, UUIDs,
target validity, and target active state are target-service decisions.

| paymentMethodId | Code | Exact official Spanish label | Canonical English target name | Source valid from | Source valid to | Target validity | Active | Catalog version | Exact source | Status |
|-----------------|------|------------------------------|-------------------------------|-------------------|-----------------|-----------------|--------|-----------------|--------------|--------|
| `639f2b7e-10a3-5d92-a1a3-28223896f5b5` | `01` | SIN UTILIZACION DEL SISTEMA FINANCIERO | Without use of the financial system | 2013-01-01 | null | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | `SRI-FT-2.32`, Table 24, p. 79 | APPROVED |
| `daad9ac7-6a55-5df6-8a9e-60012c5d261b` | `15` | COMPENSACIÓN DE DEUDAS | Debt compensation | 2013-01-01 | null | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | `SRI-FT-2.32`, Table 24, p. 79 | APPROVED |
| `cbf7e764-0ef5-5422-965e-fe08eaa49772` | `16` | TARJETA DE DÉBITO | Debit card | 2016-06-01 | null | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | `SRI-FT-2.32`, Table 24, p. 79 | APPROVED |
| `8b626780-39fb-5c72-b1e2-8453df01b79a` | `17` | DINERO ELECTRÓNICO | Electronic money | 2016-06-01 | null | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | `SRI-FT-2.32`, Table 24, p. 79 | APPROVED |
| `65eee3f8-1c46-5749-8101-6e6d50d08a69` | `18` | TARJETA PREPAGO | Prepaid card | 2016-06-01 | null | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | `SRI-FT-2.32`, Table 24, p. 79 | APPROVED |
| `178f5fd1-038b-577f-bac3-21c49ce6d1f2` | `19` | TARJETA DE CRÉDITO | Credit card | 2016-06-01 | null | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | `SRI-FT-2.32`, Table 24, p. 79 | APPROVED |
| `953df84c-d41c-5e72-b975-9d02c45ee656` | `20` | OTROS CON UTILIZACIÓN DEL SISTEMA FINANCIERO | Other with use of the financial system | 2016-06-01 | null | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | `SRI-FT-2.32`, Table 24, p. 79 | APPROVED |
| `f2bc801e-c241-5df8-99f8-ceb9ee870d05` | `21` | ENDOSO DE TÍTULOS | Endorsement of securities | 2016-06-01 | null | 2026-07-12 to open | true | `SRI-OFFLINE-2.32-TARGET-1` | `SRI-FT-2.32`, Table 24, p. 79 | APPROVED |

Payment methods remain unique per draft. The zero-value invoice example uses the approved UUID in
the payment-code `01` row above with amount `0.00`.

For Invoice Draft validation, repository lookup receives `(paymentMethodId, emissionDate)`. A row
is usable only when it exists, is active, `target_valid_from <= emissionDate`, and
`target_valid_to IS NULL OR emissionDate <= target_valid_to`; both boundaries are inclusive.
Server current date, request arrival time, transaction time, and `createdAt` are not validity
inputs. Shared vectors cover both exact boundaries, before/after, open end, inactive-but-effective,
and active-but-ineffective rows.

## Excluded Initial Rows

| Official row | Reason excluded from `SRI-OFFLINE-2.32-TARGET-1` |
|--------------|----------------------------------------------------|
| IVA percentage code `2` / 12% | Historical representation remains in Table 17, but no current supported applicability was established for this feature baseline. |
| IVA percentage code `3` / 14% | Historical representation remains in Table 17, but no current supported applicability was established for this feature baseline. |
| IVA percentage code `8` / IVA diferenciado | Table 17 does not provide one deterministic rate or sufficiently bounded applicability for this feature. |
| ICE, IRBPNR, and all non-IVA taxes | Explicitly outside Create Invoice Draft scope. |

Excluded rows MUST be omitted from initial seed planning; they MUST NOT be seeded as inactive merely
because they appear in an SRI table.

## Flyway Ownership and Immutability

- Flyway is the sole owner of catalog schema and reference-data changes.
- The initial seed migration planned later MUST contain exactly the approved rows and identifiers
  in this artifact. No migration is created by this remediation.
- Runtime reference-data UUID generation, startup insertion, manual production insertion, and
  legacy-derived seeding are prohibited.
- A committed mapping is immutable. Corrections, official changes, and identifier changes require
  a new governed catalog version and a new Flyway migration.
- The runtime MUST expose no catalog create, update, delete, activation, or query operation in this
  feature.
- Migration verification MUST recalculate every UUIDv5, compare exact row counts and natural keys,
  reject duplicates or overlapping active intervals, verify source/target validity ordering, and
  prove creation from an empty PostgreSQL database.
- Existing drafts retain the selected official code, percentage code, rate, treatment, English
  display evidence, catalog version, and stable reference identifier required by the data model;
  later catalog changes MUST NOT reinterpret committed drafts.

## Resolved Findings

- PFV-001 is resolved by the five approved buyer rows and explicit evidence-backed `FORMAT_ONLY`
  scope where no governing checksum algorithm was located.
- PFV-002 is resolved by the six approved IVA rows, deterministic identifiers, explicit
  applicability notes, and upstream-selection boundary.
- PFV-003 is resolved by the eight approved Table 24 rows and deterministic identifiers.

## Release-Gate Conclusion

**APPROVED FOR TASK GENERATION**: 5 buyer-identification rows, 6 IVA tax-rule rows, and 8 payment
method rows are complete. Every referenced tax and payment UUID is fixed, deterministic, and
independently recalculable from the published namespace and name.
