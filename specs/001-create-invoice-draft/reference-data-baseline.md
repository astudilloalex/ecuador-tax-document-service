# Reference Data Baseline: Create Invoice Draft

**Feature**: `001-create-invoice-draft`  
**Baseline candidate**: `SRI-OFFLINE-2.32`  
**Official document date**: 2025-10-08  
**Approval status**: **BLOCKED — no row is authorized for Flyway seeding**

## Purpose and Approval Rule

This artifact inventories every official candidate row relevant to the approved buyer-identification,
IVA, and payment-method scope. It does not convert an official code list into an approved target
baseline by inference.

A row is seed-authorized only when every required field has authoritative evidence and the target
mapping has explicit approval. An empty value, `PFV`, candidate English name, placeholder UUID,
random startup UUID, inferred effective date, or inferred active state is not approval. PFV-001,
PFV-002, and PFV-003 remain planning blockers; `$speckit-tasks` and the Flyway seed migration MUST
NOT proceed while any supported row remains unverified.

## Authoritative Source Register

- **SRI-FT-2.32**: [SRI Electronic Tax Documents Offline Scheme Technical Sheet v2.32, modified
  2025-10-08](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf).
  The exact locators used here are Table 6 (identification types), Table 16 (IVA tax code `2`),
  Table 17 (IVA percentage/treatment codes), and Table 24 (payment methods and dates).
- **SRI-FE-CURRENT**: [SRI electronic invoicing technical-information page](https://www.sri.gob.ec/facturacion-electronica),
  which publishes v2.32 as the current technical sheet. This page confirms publication status; it
  does not supply target UUIDs or missing row-effective dates.

The SRI sources do not publish this service's `taxRuleId` or `paymentMethodId` UUIDs. No target UUID
namespace or fixed mapping has been approved. Consequently, no UUID may be generated or copied into
a seed migration yet.

## PFV-001 — Buyer Identification Types

Table 6 authoritatively identifies the five approved feature codes. It does not state effective
start/end dates, active flags, or the complete class-specific RUC and Ecuadorian identity-card
algorithms. The following rows therefore remain candidates, not seed rows.

| Code | Exact official label | Candidate English display name | Required target validation strategy | Valid from | Valid to | Active | Catalog version | Exact source | Status |
|------|----------------------|---------------------------------|-------------------------------------|------------|----------|--------|-----------------|--------------|--------|
| `04` | RUC | RUC | Official RUC format; apply an official checksum only for a class for which the official rule defines one | PFV | PFV | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Table 6; separate official class/checksum source still required | Blocked |
| `05` | CÉDULA | Ecuadorian identity card | Official numeric format and competent-authority check digit; no alternative algorithm | PFV | PFV | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Table 6; exact official algorithm source still required | Blocked |
| `06` | PASAPORTE | Passport | Format and length only as established by the applicable SRI catalog/schema; no checksum | PFV | PFV | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Table 6; exact format/length source still required | Blocked |
| `07` | VENTA A CONSUMIDOR FINAL | Final consumer sale | Identification exactly `9999999999999`; buyer name exactly `CONSUMIDOR FINAL`; approved effective threshold rule also applies | PFV | PFV | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Table 6 and its final-consumer note; threshold evidence remains separately required | Blocked |
| `08` | IDENTIFICACIÓN DEL EXTERIOR | Foreign identification | Identification issued by the tax authority of the buyer's fiscal-residence country; applicable SRI/schema format and length only; no checksum | PFV | PFV | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Table 6 and its foreign-identification note; exact format/length source still required | Blocked |

No buyer-identification row may be marked active or effective merely because its code appears in
the current technical sheet. Approval requires the missing dates/state and exact validation-rule
evidence recorded in the table.

## PFV-002 — IVA Tax Rules

Table 16 establishes official tax code `2` for IVA. Table 17 lists the following official
percentage/treatment codes. The table does not provide row-effective intervals or active state,
and it does not identify which percentage rate governs a transaction on 2026-07-12. It also does
not publish target UUIDs. All rows remain blocked.

| taxRuleId | Tax code | Percentage code | Exact official label | Candidate treatment | Rate (`numeric(5,2)`) | Valid from | Valid to | Active | Catalog version | Exact source | Status |
|-----------|----------|-----------------|----------------------|---------------------|------------------------|------------|----------|--------|-----------------|--------------|--------|
| PFV | `2` | `0` | 0% | `ZERO_RATE` | `0.00` | PFV | PFV | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Tables 16–17 | Blocked |
| PFV | `2` | `2` | 12% | `PERCENTAGE_RATE` | `12.00` | PFV | PFV | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Tables 16–17 | Blocked |
| PFV | `2` | `3` | 14% | `PERCENTAGE_RATE` | `14.00` | PFV | PFV | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Tables 16–17 | Blocked |
| PFV | `2` | `4` | 15% | `PERCENTAGE_RATE` | `15.00` | PFV | PFV | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Tables 16–17 | Blocked |
| PFV | `2` | `5` | 5% | `PERCENTAGE_RATE` | `5.00` | PFV | PFV | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Tables 16–17 | Blocked |
| PFV | `2` | `6` | No Objeto de Impuesto | `NOT_SUBJECT` | `0.00` | PFV | PFV | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Tables 16–17 | Blocked |
| PFV | `2` | `7` | Exento de IVA | `EXEMPT` | `0.00` | PFV | PFV | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Tables 16–17 | Blocked |
| PFV | `2` | `8` | IVA diferenciado | PFV | PFV | PFV | PFV | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Tables 16–17; exact rate/applicability source required | Blocked |
| PFV | `2` | `10` | 13% | `PERCENTAGE_RATE` | `13.00` | PFV | PFV | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Tables 16–17 | Blocked |

The approved feature supports only emission-date-effective configured percentage IVA, IVA 0%, not
subject to IVA, and exempt from IVA. Appearance in Table 17 proves an official representation code,
not current applicability. In particular, Table 17 contains both 15% and 13% representation codes
while the specification contains an approved 15% calculation vector; those facts cannot establish
the emission-date-effective rule without official effective-period evidence. Code `8`
cannot become a rule until its treatment, exact rate, and applicability are evidenced. Rows not
approved for the initial effective baseline MUST be omitted from the seed migration rather than
seeded inactive by guesswork.

For each eventually approved row, the target integration contract must publish one exact stable
UUID and this artifact must record it before Flyway uses it. A deterministic UUID scheme MAY be
approved later, but no namespace or derivation rule is approved by this plan.

## PFV-003 — Payment Methods

Table 24 supplies official code, label, start date, and an open end marker (`-`) for all eight
listed methods. It does not publish target UUIDs or an explicit target `active` flag. The dates are
authoritative candidates; active state and fixed target mapping remain blocked.

| paymentMethodId | Code | Exact official label | Candidate English display name | Valid from | Valid to | Active | Catalog version | Exact source | Status |
|-----------------|------|----------------------|--------------------------------|------------|----------|--------|-----------------|--------------|--------|
| PFV | `01` | SIN UTILIZACION DEL SISTEMA FINANCIERO | Without use of the financial system | 2013-01-01 | Open (`-`) | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Table 24 | Blocked |
| PFV | `15` | COMPENSACIÓN DE DEUDAS | Debt compensation | 2013-01-01 | Open (`-`) | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Table 24 | Blocked |
| PFV | `16` | TARJETA DE DÉBITO | Debit card | 2016-06-01 | Open (`-`) | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Table 24 | Blocked |
| PFV | `17` | DINERO ELECTRÓNICO | Electronic money | 2016-06-01 | Open (`-`) | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Table 24 | Blocked |
| PFV | `18` | TARJETA PREPAGO | Prepaid card | 2016-06-01 | Open (`-`) | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Table 24 | Blocked |
| PFV | `19` | TARJETA DE CRÉDITO | Credit card | 2016-06-01 | Open (`-`) | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Table 24 | Blocked |
| PFV | `20` | OTROS CON UTILIZACIÓN DEL SISTEMA FINANCIERO | Other with use of the financial system | 2016-06-01 | Open (`-`) | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Table 24 | Blocked |
| PFV | `21` | ENDOSO DE TÍTULOS | Endorsement of securities | 2016-06-01 | Open (`-`) | PFV | `SRI-OFFLINE-2.32` | SRI-FT-2.32, Table 24 | Blocked |

The open end marker is recorded exactly as official evidence; it becomes SQL `valid_to IS NULL`
only after the target mapping is approved. Candidate English names also require target terminology
approval. Each approved payment method must receive one exact published stable UUID before seeding.

## Flyway Seed Ownership and Release Gate

- Flyway is the sole owner of catalog schema and reference-data changes.
- The runtime MUST expose no catalog create, update, delete, or activation operation.
- A committed catalog migration is immutable; corrections and official changes use a new migration
  and versioned rows.
- No baseline row may be generated at startup, derived from legacy data, or inserted manually.
- A migration verification step MUST reject missing evidence, missing required UUIDs, invalid date
  order, duplicate natural-version keys, and overlapping active intervals.
- Existing drafts retain the exact applied codes, rates, treatments, display evidence, and catalog
  version required by the data model; later catalog changes do not reinterpret them.

**Current release gate**: zero rows are seed-authorized. PFV-001, PFV-002, and PFV-003 MUST be
resolved and this artifact must be approved before `$speckit-tasks` or a reference-data seed
migration is produced.
