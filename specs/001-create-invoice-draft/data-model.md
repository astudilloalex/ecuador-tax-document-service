# Data Model: Create Invoice Draft

**Feature**: `001-create-invoice-draft`
**Constitution**: v2.0.1
**Source**: `specs/001-create-invoice-draft/spec.md`

## Boundary Rules

- The domain uses Java values such as `UUID`, `BigDecimal`, `LocalDate`, and `Instant`; it contains
  no HTTP, JSON, Quarkus, Panache, PostgreSQL, security, or Mutiny types.
- The API maps `X-Company-Id` to an application `CompanyId`. The application command carries
  CompanyId explicitly. The aggregate stores it as immutable ownership data.
- Panache models are infrastructure persistence models and are mapped explicitly to/from domain
  objects.
- Flyway is the only schema and reference-data evolution mechanism.
- No Company Service, identity provider, SRI service, cache, or other external datastore
  participates in this model.

## Aggregate Root: Invoice Draft

Logical persistence name: `invoice_draft`.

| Field | Domain type | PostgreSQL type | Required | Rule |
|-------|-------------|-----------------|----------|------|
| `id` | `UUID` | `uuid` | Yes | Local primary key; not a fiscal identifier |
| `companyId` | `CompanyId` | `uuid` | Yes | Canonical non-nil UUID; immutable ownership partition |
| `emissionPointId` | `UUID` | `uuid` | Yes | Canonical non-nil opaque external reference; no ownership/status inference |
| `emissionDate` | `LocalDate` | `date` | Yes | Date derived once from `requestCreationInstant` in `America/Guayaquil` |
| `buyerIdentificationTypeCode` | approved code | `varchar(2)` | Yes | One active/effective supported code |
| `buyerIdentificationCatalogVersion` | approved version | `varchar(64)` | Yes | Version of the identification rule used for validation |
| `buyerIdentification` | validated text | `varchar(20)` | Yes | Type-specific official validation; codes `06`/`08` use case-sensitive ASCII `^[A-Za-z0-9]{1,20}$` |
| `buyerLegalName` | normalized text | `varchar(300)` | Yes | NFC once, surrounding `U+0020` trimmed, 1–300 code points, prohibited categories/separators rejected |
| `buyerAddress` | optional normalized text | `varchar(300)` | No | Same general-text policy; 1–300 code points when supplied |
| `buyerEmail` | optional text | `varchar(254)` | No | One syntactically valid address |
| `buyerTelephone` | optional text | `varchar(20)` | No | Approved character and digit-count rules |
| `status` | `DraftStatus` | `varchar(16)` | Yes | Exactly `DRAFT` |
| `currency` | `CurrencyCode` | `char(3)` | Yes | Exactly `USD` |
| `subtotalBeforeTaxes` | `BigDecimal` | `numeric(17,2)` | Yes | System-calculated; `0.00`–`999999999999999.99` |
| `totalDiscount` | `BigDecimal` | `numeric(17,2)` | Yes | System-calculated; `0.00`–`999999999999999.99` |
| `grandTotal` | `BigDecimal` | `numeric(17,2)` | Yes | System-calculated; `0.00`–`999999999999999.99` |
| `createdAt` | `Instant` | `timestamptz` | Yes | UTC value captured once inside the transaction after successful business validation and immediately before root persistence; persisted/returned unchanged after commit |
| `updatedAt` | `Instant` | `timestamptz` | Yes | Initially equal to or after `createdAt` |

Mandatory root constraints:

- primary key on `id`;
- `CHECK (company_id <> '00000000-0000-0000-0000-000000000000')`;
- equivalent non-nil check for `emission_point_id`;
- fixed `DRAFT` and `USD` checks;
- non-negative stored totals;
- a local composite reference from buyer identification code and catalog version to the approved
  identification-type catalog row;
- type-conditional locale-independent ASCII checks, including `^[A-Za-z0-9]{1,20}$` for buyer
  codes `06` and `08`;
- `updated_at >= created_at`;
- `UNIQUE (company_id, id)` to support local Company-consistent composite references.

Every repository query or mutation involving the Invoice Draft aggregate or its idempotency binding
includes and enforces authoritative CompanyId. Draft lookup uses CompanyId plus draftId; binding
lookup uses CompanyId plus key hash; creation/mutation enforces CompanyId on root and composite
binding integrity. Global VAT, payment-method, identification-type, and other immutable SRI
reference catalogs are not Company-owned and have no Company filter or column. This is persistence
partitioning and aggregate consistency, not authorization.

`createdAt` is not PostgreSQL's physical commit timestamp. Persistence does not query or
reconstruct it after commit and does not require `track_commit_timestamp`. The T076 transactional
persistence operation is the sole persistence-clock owner: it obtains one `java.time.Instant`
inside the active transaction at the point above and writes that same value to root and binding.
T063 orchestration neither calls this clock nor supplies, replaces, or overwrites the value. A
second call in one creation attempt is prohibited; rollback prevents exposure, and replay returns
the originally persisted value.

Correlation identifiers are not persisted on the aggregate. They remain request/response,
structured-log, trace, and operational-event evidence because no approved business requirement
requires durable correlation data on the draft.

## Child: Invoice Line

Logical persistence name: `invoice_line`.

| Field | PostgreSQL type | Rule |
|-------|-----------------|------|
| `id` | `uuid` | Local primary key |
| `invoiceDraftId` | `uuid` | Required local foreign key to `invoice_draft.id` |
| `position` | `integer` | 1–500; unique within the draft; business-significant order |
| `productCode` | `varchar(25)` | Case-sensitive ASCII `^[A-Za-z0-9]{1,25}$` after one surrounding SP/HTAB trim; no other normalization |
| `description` | `varchar(300)` | General NFC/U+0020 policy; 1–300 Unicode code points |
| `quantity` | `numeric(12,6)` | `0.000001`–`999999.999999` |
| `unitPrice` | `numeric(12,6)` | `0.000000`–`999999.999999` |
| `discount` | `numeric(17,2)` | `0.00`–`999999999999999.99`; no greater than gross amount |
| `grossAmount` | `numeric(17,2)` | System-calculated; `0.00`–`999999999999999.99` |
| `netAmount` | `numeric(17,2)` | System-calculated; `0.00`–`999999999999999.99` |
| `lineTotal` | `numeric(17,2)` | Net plus tax; `0.00`–`999999999999999.99` |

The final V3→V5 schema includes `UNIQUE (invoice_draft_id, position)`, locale-independent
`CHECK (product_code ~ '^[A-Za-z0-9]{1,25}$')`, and row-local non-negative/scale checks. V3 is
immutable; pending V5 replaces only the affected product/buyer constraints with exact ASCII rules.
The maximum collection count is enforced before persistence and verified after aggregate loading;
no speculative trigger is introduced.

## Child: Line Tax Selection

Logical persistence name: `invoice_line_tax`.

Each line has exactly one persisted IVA selection and calculated result.

| Field | PostgreSQL type | Rule |
|-------|-----------------|------|
| `id` | `uuid` | Local primary key |
| `invoiceLineId` | `uuid` | Unique required local foreign key to `invoice_line.id` |
| `taxRuleId` | `uuid` | Selected local catalog rule |
| `family` | `varchar(16)` | Exactly `IVA` |
| `treatment` | `varchar(32)` | `PERCENTAGE_RATE`, `ZERO_RATE`, `NOT_SUBJECT`, or `EXEMPT` |
| `officialTaxCode` | `varchar(8)` | Versioned catalog value |
| `officialPercentageCode` | `varchar(8)` | Versioned catalog value |
| `rate` | `numeric(5,2)` | Percentage points from `0.00` through `100.00` |
| `taxBase` | `numeric(17,2)` | Rounded line net amount; `0.00`–`999999999999999.99` |
| `taxAmount` | `numeric(17,2)` | System-calculated; `0.00`–`999999999999999.99` |
| `catalogVersion` | `varchar(64)` | Rule version applied at creation |

`UNIQUE (invoice_line_id)` prevents more than one simultaneous line tax.

## Child: Grouped Tax Total

Logical persistence name: `invoice_tax_total`.

| Field | PostgreSQL type | Rule |
|-------|-----------------|------|
| `id` | `uuid` | Local primary key |
| `invoiceDraftId` | `uuid` | Required local foreign key |
| `family` | `varchar(16)` | Exactly `IVA` |
| `treatment` | `varchar(32)` | Approved treatment |
| `officialTaxCode` | `varchar(8)` | Versioned catalog value |
| `officialPercentageCode` | `varchar(8)` | Versioned catalog value |
| `rate` | `numeric(5,2)` | Applicable percentage points |
| `taxBase` | `numeric(17,2)` | Sum of grouped rounded bases; `0.00`–`999999999999999.99` |
| `taxAmount` | `numeric(17,2)` | Sum of grouped rounded taxes; `0.00`–`999999999999999.99` |
| `catalogVersion` | `varchar(64)` | Applied catalog version |

The unique group key is `(invoice_draft_id, treatment, official_tax_code,
official_percentage_code, rate, catalog_version)`. IVA 0%, not subject, and exempt remain distinct
because `treatment` participates in the key.

## Child: Payment

Logical persistence name: `invoice_payment`.

| Field | PostgreSQL type | Rule |
|-------|-----------------|------|
| `id` | `uuid` | Local primary key |
| `invoiceDraftId` | `uuid` | Required local foreign key |
| `paymentMethodId` | `uuid` | Selected local catalog method |
| `officialCode` | `varchar(8)` | Versioned catalog value |
| `name` | `varchar(100)` | English target display name |
| `amount` | `numeric(17,2)` | Approved positive/zero behavior; maximum `999999999999999.99` |
| `catalogVersion` | `varchar(64)` | Applied catalog version |

`UNIQUE (invoice_draft_id, payment_method_id)` prevents duplicate payment methods. Cross-row
payment reconciliation remains a calculated-value invariant verified in Stage 11B. The selected
catalog row must exist, be active, and satisfy `target_valid_from <= emission_date` and
`target_valid_to IS NULL OR emission_date <= target_valid_to`; lookup receives `emissionDate`, not
server current date, request/transaction time, or `createdAt`.

## Child: Additional Information

Logical persistence name: `invoice_additional_information`.

| Field | PostgreSQL type | Rule |
|-------|-----------------|------|
| `id` | `uuid` | Local primary key |
| `invoiceDraftId` | `uuid` | Required local foreign key |
| `position` | `integer` | Stable response order |
| `name` | `varchar(300)` | NFC once, surrounding `U+0020` trimmed, display case/internal punctuation/spaces preserved, 1–300 code points |
| `canonicalName` | `varchar(300)` | Persisted Java canonical value: NFC → U+0020 trim → collapse U+0020 runs → `Locale.ROOT` lowercase |
| `value` | `varchar(300)` | Same general display-text policy, 1–300 code points |

Constraints include `UNIQUE (invoice_draft_id, canonical_name)` and
`UNIQUE (invoice_draft_id, position)`. At most 15 entries are accepted. PostgreSQL requires stored
non-null/nonempty/bounded canonical values but never recalculates Java NFC or `Locale.ROOT` behavior.

## Local Idempotency Binding

Logical persistence name: `invoice_draft_idempotency`.

| Field | PostgreSQL type | Required | Rule |
|-------|-----------------|----------|------|
| `companyId` | `uuid` | Yes | Same canonical Company partition as the draft |
| `idempotencyKeyHash` | `bytea` | Yes | 32-byte domain-separated SHA-256; raw key not stored |
| `requestFingerprint` | `bytea` | Yes | 32-byte SHA-256 of normalized client business content |
| `normalizationVersion` | `smallint` | Yes | Positive version; initial value `1` |
| `invoiceDraftId` | `uuid` | Yes | Bound local draft identifier |
| `createdAt` | `timestamptz` | Yes | Same immutable transaction-captured UTC value as the bound draft |

Required constraints:

- primary key or equivalent unique row identity chosen by the migration;
- `UNIQUE (company_id, idempotency_key_hash)`;
- `UNIQUE (invoice_draft_id)`;
- length checks requiring 32-byte hashes;
- positive `normalization_version`;
- composite local foreign key `(company_id, invoice_draft_id)` referencing
  `invoice_draft(company_id, id)`.

The binding stores no raw key and no normalized request content. The fingerprint excludes
CompanyId because CompanyId is the explicit binding scope. It also excludes `Idempotency-Key`,
`X-Correlation-Id`, headers, property order, and transport metadata.

## Local Reference Catalogs

The approved executable baseline is defined in `reference-data-baseline.md`. It contains 5 buyer,
6 IVA, and 8 payment rows under `SRI-OFFLINE-2.32-TARGET-1`. Reference UUIDs use deterministic
UUIDv5 namespace `32576bbf-b70d-5c24-98ff-d5f9b48e8826`; runtime generation is prohibited.

### Buyer Identification Type Catalog

Logical persistence name: `buyer_identification_type_catalog`.

| Field | PostgreSQL type | Rule |
|-------|-----------------|------|
| `officialCode` | `varchar(2)` | Official SRI code; exactly two ASCII digits |
| `officialLabel` | `varchar(100)` | Exact official Spanish label |
| `displayName` | `varchar(100)` | Approved English target display name |
| `validationStrategy` | `varchar(64)` | Approved named validation behavior; never an unevidenced regex or algorithm |
| `validationRuleVersion` | `varchar(64)` | Exact approved rule-set version |
| `sourceValidFrom` | `date` | Nullable official effective start; null means the cited source provides none |
| `sourceValidTo` | `date` | Nullable official effective end |
| `targetValidFrom` | `date` | Inclusive target activation date; `2026-07-12` for this baseline |
| `targetValidTo` | `date` | Inclusive target end, null for the initial open target interval |
| `active` | `boolean` | Approved baseline state; not inferred by production code |
| `catalogVersion` | `varchar(64)` | Versioned target baseline identifier |
| `officialSourceUri` | `text` | Exact authoritative SRI source |
| `officialSourceLocator` | `varchar(128)` | Exact table, section, schema, or rule locator |

The primary key is `(official_code, catalog_version)`. Each source or target end must be null or
not precede its corresponding start; `source_valid_to` requires `source_valid_from`. An active row
must have a complete validation strategy and evidence locator. A
Flyway verification statement MUST reject overlapping active intervals for the same official code.
`invoice_draft` references `(buyer_identification_type_code,
buyer_identification_catalog_version)` locally.

Every column is `NOT NULL` except `source_valid_from`, `source_valid_to`, and `target_valid_to`.

### IVA Tax Rule Catalog

Logical persistence name: `iva_tax_rule_catalog`.

| Field | PostgreSQL type | Rule |
|-------|-----------------|------|
| `id` | `uuid` | Approved, published stable target `taxRuleId`; never startup-generated |
| `family` | `varchar(16)` | Exactly `IVA` |
| `officialTaxCode` | `varchar(8)` | Exact official SRI tax code; `2` for the approved family |
| `officialPercentageCode` | `varchar(8)` | Exact official percentage/treatment code |
| `officialLabel` | `varchar(100)` | Exact official Spanish label |
| `displayName` | `varchar(100)` | Approved English target display name |
| `treatment` | `varchar(32)` | `PERCENTAGE_RATE`, `ZERO_RATE`, `NOT_SUBJECT`, or `EXEMPT` |
| `rate` | `numeric(5,2)` | `0.00`–`100.00`; exact configured percentage points |
| `sourceValidFrom` | `date` | Nullable official effective start; null means the cited source provides none |
| `sourceValidTo` | `date` | Nullable official effective end |
| `targetValidFrom` | `date` | Inclusive target activation date; `2026-07-12` for this baseline |
| `targetValidTo` | `date` | Inclusive target end, null for the initial open target interval |
| `active` | `boolean` | Approved baseline state |
| `catalogVersion` | `varchar(64)` | Versioned target baseline identifier |
| `officialSourceUri` | `text` | Exact authoritative SRI source |
| `officialSourceLocator` | `varchar(128)` | Exact source table/row or legal rule locator |

The primary key is `(id, catalog_version)` and required natural uniqueness is
`(official_tax_code, official_percentage_code, target_valid_from, catalog_version)`. The family must be
`IVA`; percentage-rate rows must have a positive approved
rate; the other three treatments must have rate `0.00`. Each source or target end must be null or
not precede its corresponding start; `source_valid_to` requires `source_valid_from`. Flyway
verification MUST reject overlapping active target intervals for the same official
tax/percentage code. `(invoice_line_tax.tax_rule_id, invoice_line_tax.catalog_version)` is a
required local composite foreign key to this table.

Every column is `NOT NULL` except `source_valid_from`, `source_valid_to`, and `target_valid_to`.

### Payment Method Catalog

Logical persistence name: `payment_method_catalog`.

| Field | PostgreSQL type | Rule |
|-------|-----------------|------|
| `id` | `uuid` | Approved, published stable target `paymentMethodId`; never startup-generated |
| `officialCode` | `varchar(8)` | Exact official SRI payment code |
| `officialLabel` | `varchar(160)` | Exact official Spanish label |
| `displayName` | `varchar(100)` | Approved English target display name |
| `sourceValidFrom` | `date` | Official Table 24 start date |
| `sourceValidTo` | `date` | Nullable official end; Table 24 dash maps to null |
| `targetValidFrom` | `date` | Inclusive target activation date; `2026-07-12` for this baseline |
| `targetValidTo` | `date` | Inclusive target end, null for the initial open target interval |
| `active` | `boolean` | Approved baseline state |
| `catalogVersion` | `varchar(64)` | Versioned target baseline identifier |
| `officialSourceUri` | `text` | Exact authoritative SRI source |
| `officialSourceLocator` | `varchar(128)` | Exact source table/row locator |

The primary key is `(id, catalog_version)` and required natural uniqueness is
`(official_code, target_valid_from, catalog_version)`. Each source or target end must be null or not
precede its corresponding start, and Flyway verification MUST reject overlapping active target intervals for the same
official code. `(invoice_payment.payment_method_id, invoice_payment.catalog_version)` is a required
local composite foreign key.

Every column is `NOT NULL` except `source_valid_to` and `target_valid_to`.

Flyway alone owns schema creation, initial baseline rows, and later catalog versions. The runtime
has no catalog administration write path. A later official change adds a new immutable migration
and versioned rows; it does not rewrite a committed migration or silently reinterpret a row used
by an existing draft. These catalogs are locally persisted but globally governed tax-document
reference data, not Company master data. They contain no `company_id`, and aggregate Company
scoping does not apply to their reference lookups.

## General Unicode Storage Boundary

General human-readable text is normalized to NFC once at the API/application boundary. That
boundary rejects categories `Cc`, `Cf`, `Cs`, `Co`, `Cn`, `U+2028`, and `U+2029`; accepts only
`U+0020` as spacing; rejects tabs, CR, LF, NBSP, and every other Unicode separator; trims
surrounding `U+0020`; preserves internal punctuation, internal `U+0020`, and display case; and
counts Unicode code points afterward. Comparison is case-sensitive unless a field rule overrides
it. Assigned emoji such as `U+1F600` (`So`) is accepted when field format/length permits.

OpenAPI documents format/length. API/application performs normalization and prohibited-code-point
validation. Domain receives normalized values. PostgreSQL enforces nullability, nonempty values,
maximum stored length, and required `canonical_name` but does not claim to reproduce Java Unicode
normalization. Cross-layer tests use identical accented/decomposed, `U+0020`, tab/newline/NBSP,
zero-width, emoji, case, and code-point-boundary vectors. Explicit ASCII product/passport/foreign-
identification rules override this section.

## Numeric Storage Boundary

Quantity and unit price use `numeric(12,6)`, money uses `numeric(17,2)`, and tax rates use
`numeric(5,2)` everywhere they are persisted. Input precision, exact intermediates, rounded line
amounts, grouped sums, payment sums, and final totals are range-checked before the write
transaction. Any overflow or excess input precision produces `BUSINESS_VALIDATION_FAILED` with
violation `MONETARY_RANGE_EXCEEDED`; it MUST NOT reach PostgreSQL as a numeric overflow, rounding,
clamping, or truncation attempt. Database checks duplicate the stable row-local ranges as a final
integrity barrier but do not replace pre-persistence validation.

## Aggregate Relationships

```text
InvoiceDraft (companyId, id)
├── 1..500 InvoiceLine
│   └── exactly 1 InvoiceLineTax
├── 1..n InvoiceTaxTotal
├── 1..8 InvoicePayment
├── 0..15 InvoiceAdditionalInformation
└── exactly 1 InvoiceDraftIdempotency after successful creation
```

Child foreign keys are local to the Tax Document Service database. There are no cross-service or
cross-database foreign keys. Because update/deletion is outside this feature, destructive cascade
behavior is not introduced by this plan.

## Creation and Replay Lifecycle

1. Capture `requestCreationInstant` exactly once, initialize safe correlation and neutral
   `RequestDeadline`, and let the API adapter exclusively race the eventual application `Uni`,
   arbitrate one terminal result, and perform HTTP mapping.
2. Validate and normalize CompanyId, then validate correlation; in the API enforce exactly one
   Idempotency-Key field value, trim surrounding ASCII SP/HTAB once, apply its stable
   required/invalid/multiple outcome, and only then validate request representation in FR-041
   order.
3. Derive the expected emission date once in `America/Guayaquil`, then compute the key hash and
   request fingerprint under normalization version 1.
4. Look up a binding by CompanyId and key hash.
5. Return the original Company-scoped draft when the stored fingerprint is equal, or return
   `IDEMPOTENCY_CONFLICT` when different.
6. For an unbound command, Stage 10 validates only calculation-independent structure, catalogs,
   normalized text, and payment-method existence/activity/effectiveness against `emissionDate`.
7. Stage 11A calculates amounts. Stage 11B then validates, in order: calculated range/overflow;
   discount-over-gross; final-consumer total limit; total-dependent payment shape/positivity; exact
   payment reconciliation.
8. After those validations succeed, T076 opens/owns one reactive PostgreSQL transaction, invokes
   the persistence clock exactly once immediately before root persistence, and persists the same
   UTC `Instant` unchanged as draft/binding `createdAt`, then root, children, and binding. T063 does
   not supply that value. It becomes externally observable only after commit confirmation and is
   neither the emission-date source nor a physical commit timestamp.
9. If uniqueness arbitration loses, roll back all tentative rows and resolve the committed winner
   in a fresh Company-scoped read.

Every pre-commit failure leaves no aggregate or binding. A committed binding remains authoritative
after response loss and has no time-based expiration while its draft exists. Equivalent replay
returns the original emission date and originally persisted `createdAt` without current-date
revalidation or timestamp reconstruction.

## Explicitly Excluded Data

The model contains none of the following:

- Company, Issuer, establishment, or emission-point master tables;
- Company or Issuer fiscal snapshots;
- `tenant_id`, `created_by_subject`, roles, permissions, or authorization fields;
- `company_context_version` or `company_context_observed_at`;
- Company Service references, shared schemas, or cross-database foreign keys;
- official sequence, access key, XML, signature, certificate, SRI, PDF, or notification state;
- complete normalized idempotency request content;
- application cache state.
