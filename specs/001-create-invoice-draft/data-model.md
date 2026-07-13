# Data Model: Create Invoice Draft

**Feature**: `001-create-invoice-draft`
**Constitution**: v2.0.0
**Status**: Reconciled with the definitive Company-context boundary

## Model Boundaries

- Domain values use Java 25 language types such as `UUID`, `BigDecimal`, `LocalDate`, and
  `Instant`; they contain no Quarkus, HTTP, JSON, persistence, security, or Mutiny types.
- Panache models remain infrastructure persistence models and are mapped explicitly.
- `X-Company-Id` is mapped by the API adapter to one normalized application-level `CompanyId`.
  The HTTP header does not enter the domain model.
- The Company UUID is opaque. No Company, Issuer, establishment, or emission-point master data is
  resolved, validated, cached, replicated, or stored as a snapshot.
- Every schema and reference-data change is delivered only by an immutable Flyway migration.

## Aggregate: Invoice Draft

`InvoiceDraft` is the aggregate root. It contains the complete locally owned commercial draft and
has no fiscal sequence, access key, authorization number, XML, signature, certificate reference,
Company snapshot, or Issuer snapshot.

| Field | Domain type | PostgreSQL type | Required | Invariant |
|-------|-------------|-----------------|----------|-----------|
| `draftId` | `UUID` | `uuid` | Yes | Unique internal identifier; not a fiscal identifier |
| `companyId` | `CompanyId` | `uuid` | Yes | Normalized non-nil external UUID; immutable ownership partition |
| `emissionPointId` | `UUID` | `uuid` | Yes | Opaque external reference; no relationship or status is inferred |
| `emissionDate` | `LocalDate` | `date` | Yes | Current Ecuadorian civil date at creation |
| `status` | `DraftStatus` | `varchar` | Yes | Exactly `DRAFT` |
| `currency` | `CurrencyCode` | `char(3)` | Yes | Exactly `USD` |
| `buyerIdentificationTypeCode` | official code value | `varchar(2)` | Yes | Supported active/effective code `04`–`08` |
| `buyerIdentification` | validated text | `varchar(20)` | Yes | Canonical type-specific value |
| `buyerLegalName` | validated text | `varchar(300)` | Yes | Trimmed, nonblank, no control characters |
| `buyerAddress` | optional validated text | `varchar(300)` | No | Trimmed and valid when present |
| `buyerEmail` | optional validated text | `varchar(254)` | No | One syntactically valid address |
| `buyerTelephone` | optional validated text | `varchar(20)` | No | Approved representation and digit count |
| `subtotalBeforeTaxes` | `BigDecimal` | `numeric(14,2)` | Yes | Sum of rounded line net amounts; non-negative |
| `totalDiscount` | `BigDecimal` | `numeric(14,2)` | Yes | Sum of two-decimal discounts; non-negative |
| `grandTotal` | `BigDecimal` | `numeric(14,2)` | Yes | Subtotal plus grouped tax amounts; non-negative |
| `createdAt` | `Instant` | `timestamptz` | Yes | Unambiguous creation instant |
| `lastModifiedAt` | `Instant` | `timestamptz` | Yes | Not before `createdAt` |

Required persistence constraints include a primary key on `draft_id`, non-nil Company UUID check,
fixed `DRAFT` and `USD` checks, non-negative monetary checks, and Company-scoped indexes such as
`(company_id, created_at)`. Every repository query or mutation includes `company_id`; this is data
partitioning, not caller authorization.

### Child: Invoice Line

| Field | PostgreSQL type | Invariant |
|-------|-----------------|-----------|
| `lineId` | `uuid` | Local primary key |
| `draftId` | `uuid` | Required local foreign key to the aggregate root with aggregate deletion behavior |
| `position` | `integer` | 1–500; unique within the draft; order is business-significant |
| `productCode` | `varchar(25)` | Trimmed SRI-valid alphanumeric text |
| `description` | `varchar(300)` | Trimmed, nonblank text |
| `quantity` | `numeric(18,6)` | Greater than zero |
| `unitPrice` | `numeric(18,6)` | Non-negative |
| `discount` | `numeric(14,2)` | Non-negative and not greater than gross amount |
| `grossAmount` | `numeric(14,2)` | System-calculated |
| `netAmount` | `numeric(14,2)` | System-calculated and non-negative |
| `lineTotal` | `numeric(14,2)` | Net amount plus tax amount |

Lines do not store `company_id`; they belong to Company-owned data only through the local draft
foreign key. This prevents children from becoming independent ownership or Company records.

### Child: Line Tax Selection

Each line has exactly one tax selection with a local one-to-one constraint on `line_id`.

| Field | PostgreSQL type | Invariant |
|-------|-----------------|-----------|
| `lineTaxId` | `uuid` | Local primary key |
| `lineId` | `uuid` | Unique required local foreign key |
| `taxRuleId` | `uuid` | Selected active/effective local reference-catalog rule |
| `family` | `varchar` | Exactly `IVA` |
| `treatment` | `varchar` | `PERCENTAGE_RATE`, `ZERO_RATE`, `NOT_SUBJECT`, or `EXEMPT` |
| `officialTaxCode` | `varchar(8)` | Versioned catalog value |
| `officialPercentageCode` | `varchar(8)` | Versioned catalog value |
| `rate` | `numeric(5,2)` | Percentage points, 0.00–100.00 |
| `taxBase` | `numeric(14,2)` | Rounded line net amount |
| `taxAmount` | `numeric(14,2)` | System-calculated, non-negative |
| `catalogVersion` | `varchar(64)` | Version used by the calculation |

### Child: Tax Total

One row exists per `(draft_id, official_tax_code, official_percentage_code, treatment, rate,
catalog_version)` group. IVA 0%, not subject to IVA, and exempt from IVA remain separate groups
even when amounts are zero.

### Child: Payment

| Field | PostgreSQL type | Invariant |
|-------|-----------------|-----------|
| `paymentId` | `uuid` | Local primary key |
| `draftId` | `uuid` | Required local foreign key |
| `paymentMethodId` | `uuid` | Active local reference-catalog value |
| `officialCode` | `varchar(8)` | Versioned catalog value |
| `name` | `varchar(100)` | English target display name |
| `amount` | `numeric(14,2)` | Positive for positive totals; one exact `0.00` payment for zero totals |
| `catalogVersion` | `varchar(64)` | Version used at creation |

`UNIQUE (draft_id, payment_method_id)` prevents a payment method from appearing twice. The exact
sum of amounts equals the draft grand total.

### Child: Additional Information

At most 15 local rows belong to a draft. Each has trimmed `name` and `value` of 1–300 characters,
and a uniqueness constraint on the draft plus canonical name.

## Idempotency Binding

| Field | PostgreSQL type | Required | Invariant |
|-------|-----------------|----------|-----------|
| `bindingId` | `uuid` | Yes | Local primary key |
| `companyId` | `uuid` | Yes | Same normalized Company UUID as the bound draft |
| `idempotencyKeyHash` | fixed hash bytes/text | Yes | Deterministic hash of the trimmed caller key; raw key is not required |
| `requestFingerprint` | fixed hash bytes/text | Yes | Hash of normalized business content |
| `normalizedContent` | canonical structured value | Yes | Supports exact collision-safe equivalence comparison |
| `draftId` | `uuid` | Yes | Unique local foreign key to the committed draft |
| `createdAt` | `timestamptz` | Yes | Same successful transaction boundary |

The mandatory concurrency boundary is:

`UNIQUE (company_id, idempotency_key_hash)`

The normalized content contains the opaque emission-point identifier, emission date, buyer,
ordered lines and tax selections, order-insensitive payments, and order-insensitive additional
information. It excludes Company identifier because Company is already part of the scope. It also
excludes the idempotency key, correlation identifier, and every transport-only header.

The same idempotency key may be used by different Companies. Equivalent concurrent requests in
one Company-and-key scope produce one committed draft. A successful binding has no time-based
expiration and remains for the lifetime of its draft.

## Local Reference Catalogs

Versioned, effective-dated local catalogs cover buyer identification types, IVA tax rules, and
payment methods. Each contains official codes, active state, effective interval, version, and the
fields needed for the approved validation or calculation. Codes and rates are never hard-coded in
production logic. Reference-data changes use Flyway migrations only.

These catalogs are not Company master data and do not create a Company Service dependency.

## Creation and Replay Transaction

1. Validate the request representation, including exactly one non-nil `X-Company-Id`, and map it to
   normalized `CompanyId`.
2. Normalize the local business content and calculate its fingerprint without Company or transport
   headers.
3. Within one reactive PostgreSQL transaction, arbitrate the Company-and-key-hash uniqueness,
   compare any existing binding, validate the approved local catalogs and domain invariants, and
   persist the complete aggregate plus binding.
4. Equivalent existing content returns the original draft; different content returns the stable
   conflict; failure rolls back all local state.

No step calls a Company capability or performs authentication, authorization, fiscal-context
resolution, Company availability handling, or snapshot persistence.

## Excluded Data

The model contains no tenant identifier, user/principal/role/permission, token, Company aggregate,
Company master table, Company status, Company-context version, context-observation timestamp,
Issuer fiscal profile or snapshot, establishment snapshot, emission-point fiscal snapshot, fiscal
sequence, access key, XML, signature, certificate, PDF, SRI response, or notification state.
