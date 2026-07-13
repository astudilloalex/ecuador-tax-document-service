# Data Model: Create Invoice Draft

**Feature**: `001-create-invoice-draft`
**Constitution**: v2.0.0
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
| `emissionDate` | `LocalDate` | `date` | Yes | Current Ecuadorian civil date at original creation |
| `buyerIdentificationTypeCode` | approved code | `varchar(2)` | Yes | One active/effective supported code |
| `buyerIdentification` | validated text | `varchar(20)` | Yes | Type-specific official validation |
| `buyerLegalName` | validated text | `varchar(300)` | Yes | Trimmed, nonblank, no control characters |
| `buyerAddress` | optional text | `varchar(300)` | No | Trimmed and valid when present |
| `buyerEmail` | optional text | `varchar(254)` | No | One syntactically valid address |
| `buyerTelephone` | optional text | `varchar(20)` | No | Approved character and digit-count rules |
| `status` | `DraftStatus` | `varchar(16)` | Yes | Exactly `DRAFT` |
| `currency` | `CurrencyCode` | `char(3)` | Yes | Exactly `USD` |
| `subtotalBeforeTaxes` | `BigDecimal` | `numeric(14,2)` | Yes | System-calculated, non-negative |
| `totalDiscount` | `BigDecimal` | `numeric(14,2)` | Yes | System-calculated, non-negative |
| `grandTotal` | `BigDecimal` | `numeric(14,2)` | Yes | System-calculated, non-negative |
| `createdAt` | `Instant` | `timestamptz` | Yes | Unambiguous commit-time audit instant |
| `updatedAt` | `Instant` | `timestamptz` | Yes | Initially equal to or after `createdAt` |

Mandatory root constraints:

- primary key on `id`;
- `CHECK (company_id <> '00000000-0000-0000-0000-000000000000')`;
- equivalent non-nil check for `emission_point_id`;
- fixed `DRAFT` and `USD` checks;
- non-negative stored totals;
- `updated_at >= created_at`;
- `UNIQUE (company_id, id)` to support local Company-consistent composite references.

Every repository read or mutation that addresses an existing draft carries both CompanyId and
draftId. This is persistence partitioning and aggregate consistency, not authorization.

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
| `productCode` | `varchar(25)` | Trimmed approved alphanumeric value |
| `description` | `varchar(300)` | Trimmed, nonblank text |
| `quantity` | `numeric(18,6)` | Greater than zero |
| `unitPrice` | `numeric(18,6)` | Non-negative |
| `discount` | `numeric(14,2)` | Non-negative; no greater than gross amount |
| `grossAmount` | `numeric(14,2)` | System-calculated |
| `netAmount` | `numeric(14,2)` | System-calculated, non-negative |
| `lineTotal` | `numeric(14,2)` | Net amount plus tax amount |

Constraints include `UNIQUE (invoice_draft_id, position)` and row-local non-negative/scale checks.
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
| `taxBase` | `numeric(14,2)` | Rounded line net amount |
| `taxAmount` | `numeric(14,2)` | System-calculated, non-negative |
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
| `taxBase` | `numeric(14,2)` | Sum of grouped rounded bases |
| `taxAmount` | `numeric(14,2)` | Sum of grouped rounded taxes |
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
| `amount` | `numeric(14,2)` | Approved positive/zero behavior |
| `catalogVersion` | `varchar(64)` | Applied catalog version |

`UNIQUE (invoice_draft_id, payment_method_id)` prevents duplicate payment methods. Cross-row
payment reconciliation remains a domain/application invariant verified before commit.

## Child: Additional Information

Logical persistence name: `invoice_additional_information`.

| Field | PostgreSQL type | Rule |
|-------|-----------------|------|
| `id` | `uuid` | Local primary key |
| `invoiceDraftId` | `uuid` | Required local foreign key |
| `position` | `integer` | Stable response order |
| `name` | `varchar(300)` | Trimmed, nonblank, no control characters |
| `canonicalName` | `varchar(300)` | Canonical uniqueness value |
| `value` | `varchar(300)` | Trimmed, nonblank, no control characters |

Constraints include `UNIQUE (invoice_draft_id, canonical_name)` and
`UNIQUE (invoice_draft_id, position)`. At most 15 entries are accepted.

## Local Idempotency Binding

Logical persistence name: `invoice_draft_idempotency`.

| Field | PostgreSQL type | Required | Rule |
|-------|-----------------|----------|------|
| `companyId` | `uuid` | Yes | Same canonical Company partition as the draft |
| `idempotencyKeyHash` | `bytea` | Yes | 32-byte domain-separated SHA-256; raw key not stored |
| `requestFingerprint` | `bytea` | Yes | 32-byte SHA-256 of normalized client business content |
| `normalizationVersion` | `smallint` | Yes | Positive version; initial value `1` |
| `invoiceDraftId` | `uuid` | Yes | Bound local draft identifier |
| `createdAt` | `timestamptz` | Yes | Successful binding instant |

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

Versioned, active, effective-dated local tables provide:

- buyer identification types and validation-rule version metadata;
- IVA tax categories/rules, treatment, official codes, percentage rates, and effective periods;
- payment methods and official codes.

Flyway owns baseline and change migrations. Production code does not hard-code catalog codes or
rates. These catalogs are tax-document reference data, not Company master data.

## Aggregate Relationships

```text
InvoiceDraft (companyId, id)
├── 1..500 InvoiceLine
│   └── exactly 1 InvoiceLineTax
├── 1..n InvoiceTaxTotal
├── 1..10 InvoicePayment
├── 0..15 InvoiceAdditionalInformation
└── exactly 1 InvoiceDraftIdempotency after successful creation
```

Child foreign keys are local to the Tax Document Service database. There are no cross-service or
cross-database foreign keys. Because update/deletion is outside this feature, destructive cascade
behavior is not introduced by this plan.

## Creation and Replay Lifecycle

1. Map and normalize CompanyId, idempotency key, optional correlation, and request representation.
2. Compute key hash and request fingerprint under normalization version 1.
3. Look up a binding by CompanyId and key hash.
4. Return the original Company-scoped draft when the stored fingerprint is equal, or return
   `IDEMPOTENCY_CONFLICT` when different.
5. For an unbound command, validate local catalogs/domain rules and calculate all amounts.
6. Persist root, children, and binding in one reactive PostgreSQL transaction.
7. If uniqueness arbitration loses, roll back all tentative rows and resolve the committed winner
   in a fresh Company-scoped read.

Every pre-commit failure leaves no aggregate or binding. A committed binding remains authoritative
after response loss and has no time-based expiration while its draft exists.

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
