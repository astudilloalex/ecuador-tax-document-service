# Data Model: Create Invoice Draft

**Feature**: `001-create-invoice-draft`
**Constitution**: v2.0.1
**Source**: `specs/001-create-invoice-draft/spec.md`

## Boundary Rules

- The domain uses Java values such as `UUID`, `BigDecimal`, `LocalDate`, and `Instant`; it contains
  no HTTP, JSON, Quarkus, Panache, PostgreSQL, security, or Mutiny types.
- The API maps `X-Company-Id` to an application `CompanyId`. The application command carries
  CompanyId explicitly. The aggregate stores it as immutable ownership data.
- The API decodes JSON and validates transport representation but forwards business text without
  NFC normalization, trimming, internal-space collapse, lowercase conversion, or `canonicalName`
  derivation. Application owns exactly one Stage-6 `BusinessTextNormalizer` invocation for each
  supplied applicable value and invokes it zero times for an absent optional value; Domain and
  Infrastructure receive only normalized output and do not normalize again.
- Panache models are infrastructure persistence models and are mapped explicitly to/from domain
  objects.
- Flyway is the only schema and reference-data evolution mechanism.
- No Company Service, identity provider, SRI service, cache, or other external datastore
  participates in this model.

## Application Persistence Models

### InvoiceDraftCandidate

`InvoiceDraftCandidate` is the fully normalized, validated, and calculated Application model passed
to the persistence port for a logically new draft. It contains all approved business data but no
`createdAt`, `updatedAt`, Panache/database entity, HTTP information, physical commit metadata, or
provisional timestamp. Null, zero, placeholder, provisional, and fabricated timestamps are not
valid candidate state. Application obtains every local root and child UUID through
`DraftIdentifierGenerator` after validation/calculation and before constructing this model; the
candidate therefore carries the final identifiers that persistence must preserve. It also carries
authoritative CompanyId and the already derived idempotency key hash, request fingerprint, and
normalization version required to persist the binding atomically, but never the raw key or an HTTP
header representation.

### PersistedInvoiceDraft

`PersistedInvoiceDraft` is the successful persistence-port result returned only after commit. It
contains the final local identifier, the persisted business values required by the application
response, `createdAt`, and `updatedAt`. On initial creation, both timestamp properties contain the
exact same `java.time.Instant` obtained by the persistence adapter's single transactional-clock
invocation. A replay loads and returns the originally persisted result without invoking the clock or
changing either timestamp.

Neither conceptual model is an HTTP DTO or Panache entity. Application constructs the candidate;
Infrastructure constructs the persisted result from committed persistence state. Application,
Domain, API, and mapping components never generate either timestamp; mappers only copy the values
carried by `PersistedInvoiceDraft`.

The conceptual application-owned persistence-port operation is:

```text
persist(InvoiceDraftCandidate) -> Uni<PersistedInvoiceDraft>
```

The port accepts no HTTP type, timestamped candidate, placeholder timestamp, Panache entity, or
commit metadata and returns no persistence entity or HTTP error. Application is the sole local
identifier-allocation owner; Infrastructure never generates, replaces, or repairs a candidate
identifier. The persistence adapter is the sole timestamp owner and returns the final committed
representation. Domain objects still enforce invariants over normalized values; the candidate is
the Application handoff assembled from those accepted domain/calculation results, not a transport
DTO or replacement persistence entity.

Mapper responsibilities are intentionally asymmetric:

- API request mapping decodes transport structure and preserves raw decoded business text;
- Application mapping consumes Stage-6 normalized values, accepted domain values, calculations,
  and Application-owned identifiers to construct the candidate;
- Infrastructure mapping copies candidate values into Panache records, copies the adapter's single
  clock result into both root timestamp fields and the binding creation timestamp, and reconstructs
  `PersistedInvoiceDraft` from committed state;
- Application response mapping copies the persisted result into a transport-neutral result, and API
  response mapping alone produces the HTTP representation.

No mapper normalizes text, derives `canonicalName`, allocates an identifier, fabricates a
timestamp, invokes a clock, or maps a persistence failure to HTTP.

## Persisted Aggregate Root: Invoice Draft

Logical persistence name: `invoice_draft`.

| Field | Domain type | PostgreSQL type | Required | Rule |
|-------|-------------|-----------------|----------|------|
| `id` | `UUID` | `uuid` | Yes | Final local primary key allocated by Application through `DraftIdentifierGenerator` and preserved by persistence; not a fiscal identifier |
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
| `updatedAt` | `Instant` | `timestamptz` | Yes | On initial creation, exactly the same immutable `Instant` as `createdAt` |

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
- `updated_at >= created_at` remains V3's immutable database guard; the stricter feature creation
  invariant is enforced by T076 assigning `updated_at = created_at` from one clock result;
- `UNIQUE (company_id, id)` to support local Company-consistent composite references.

Every repository query or mutation involving the Invoice Draft aggregate or its idempotency binding
includes and enforces authoritative CompanyId. Draft lookup uses CompanyId plus draftId; binding
lookup uses CompanyId plus key hash; creation/mutation enforces CompanyId on root and composite
binding integrity. Global VAT, payment-method, identification-type, and other immutable SRI
reference catalogs are not Company-owned and have no Company filter or column. This is persistence
partitioning and aggregate consistency, not authorization.

Neither `createdAt` nor `updatedAt` is PostgreSQL's physical commit timestamp. Persistence does not
query or reconstruct either value after commit and does not require `track_commit_timestamp`. The
T076 transactional persistence operation is the sole persistence-clock owner: it accepts the
timestamp-free `InvoiceDraftCandidate`, obtains one `java.time.Instant` inside the active
transaction at the point above, and writes that same value to root `created_at`, root `updated_at`,
and binding `created_at`. It returns a `PersistedInvoiceDraft` carrying both equal root timestamps.
T063 orchestration neither calls this clock nor supplies, replaces, or overwrites either value. A
second call in one creation attempt is prohibited; rollback prevents exposure, and replay returns
both originally persisted root values without another clock invocation.

Correlation identifiers are not persisted on the aggregate. They remain request/response,
structured-log, trace, and operational-event evidence because no approved business requirement
requires durable correlation data on the draft.

## Child: Invoice Line

Logical persistence name: `invoice_line`.

| Field | PostgreSQL type | Rule |
|-------|-----------------|------|
| `id` | `uuid` | Final local key allocated by Application through `DraftIdentifierGenerator`; persistence preserves it |
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
| `id` | `uuid` | Final local key allocated by Application through `DraftIdentifierGenerator`; persistence preserves it |
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
| `id` | `uuid` | Final local key allocated by Application through `DraftIdentifierGenerator`; persistence preserves it |
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
| `id` | `uuid` | Final local key allocated by Application through `DraftIdentifierGenerator`; persistence preserves it |
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
| `id` | `uuid` | Final local key allocated by Application through `DraftIdentifierGenerator`; persistence preserves it |
| `invoiceDraftId` | `uuid` | Required local foreign key |
| `position` | `integer` | Stable response order |
| `name` | `varchar(300)` | NFC once, surrounding `U+0020` trimmed, display case/internal punctuation/spaces preserved, 1–300 code points |
| `canonicalName` | `varchar(300)` | Persisted Application-produced value: reuse the display name's one NFC + U+0020-trim result → collapse U+0020 runs → `Locale.ROOT` lowercase → count 1–300 Unicode code points; never truncate |
| `value` | `varchar(300)` | Same general display-text policy, 1–300 code points |

Constraints include `UNIQUE (invoice_draft_id, canonical_name)` and
`UNIQUE (invoice_draft_id, position)`. At most 15 entries are accepted. PostgreSQL requires stored
non-null/nonempty/bounded canonical values but never recalculates Java NFC or `Locale.ROOT` behavior.
Application validates the derived value before fingerprinting, binding lookup, Domain entry, or
persistence. Lowercase expansion can make a valid display name invalid: 150 occurrences of
`U+0130` produce exactly 300 canonical code points and are accepted, while 151 produce 302 and are
rejected without truncation using `BUSINESS_VALIDATION_FAILED` with violation
`CANONICAL_NAME_TOO_LONG`, the original request field, maximum `300`, counting unit
`UNICODE_CODE_POINTS`, and stage `CANONICALIZATION`.

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

API decodes HTTP/JSON, rejects malformed representation, validates transport structure, and passes
business text to Application unchanged. It does not perform NFC normalization, trim business text,
collapse spaces, lowercase values, or calculate `canonicalName`.

At the beginning of FR-041 Stage 6, Application invokes one `BusinessTextNormalizer` exactly once
for each supplied applicable value and zero times for an absent optional value. In exact order, that
invocation normalizes to NFC; trims surrounding `U+0020`; rejects categories `Cc`, `Cf`, `Cs`,
`Co`, `Cn`, `U+2028`, and `U+2029`; accepts only `U+0020` as spacing and rejects tabs, CR, LF,
NBSP, and every other Unicode separator; preserves internal punctuation, internal `U+0020`, and
display case; then counts and validates display length in Unicode code points. Comparison is
case-sensitive unless a field rule overrides it. Assigned emoji such as `U+1F600` (`So`) is
accepted when field format/length permits. When a canonical name applies, the same invocation
reuses that already NFC-normalized and trimmed display value, collapses internal `U+0020`,
lowercases with Java `Locale.ROOT`, counts the derived result, and rejects values over 300 code
points using `CANONICAL_NAME_TOO_LONG` without truncation. It never repeats NFC or trimming.

Domain receives normalized values and Infrastructure receives supplied normalized/canonical values;
neither normalizes again. PostgreSQL enforces nullability, nonempty values, maximum stored length,
and required `canonical_name` but does not claim to reproduce Java Unicode normalization.
Cross-layer tests use identical accented/decomposed, `U+0020`, tab/newline/NBSP, zero-width, emoji,
case, code-point-boundary, and `U+0130` expansion vectors. Explicit ASCII
product/passport/foreign-identification rules override the Unicode transformation but their one
permitted SP/HTAB trim remains Application-owned.

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
PersistedInvoiceDraft / InvoiceDraft (companyId, id, createdAt, updatedAt)
├── 1..500 InvoiceLine
│   └── exactly 1 InvoiceLineTax
├── 1..n InvoiceTaxTotal
├── 1..8 InvoicePayment
├── 0..15 InvoiceAdditionalInformation
└── exactly 1 InvoiceDraftIdempotency after successful creation
```

`InvoiceDraftCandidate` is intentionally absent from this persistence relationship diagram because
it is an Application handoff model, not a stored root or child record.

Child foreign keys are local to the Tax Document Service database. There are no cross-service or
cross-database foreign keys. Because update/deletion is outside this feature, destructive cascade
behavior is not introduced by this plan.

## Creation and Replay Lifecycle

### Logically New Creation Flow

1. API decodes the request after the earlier payload/header gates and forwards decoded business
   text without normalization.
2. Application performs the FR-041 ordered validation stages using mapped Company, request-time,
   idempotency, correlation, and neutral deadline context.
3. At Stage 6, Application invokes `BusinessTextNormalizer` exactly once for every supplied
   applicable business-text value and derives/validates canonical values.
4. Application performs Stage 11A deterministic monetary calculation after Stage 10 and before
   the ordered Stage 11B calculated-value checks.
5. Application obtains all final local root and child identifiers from `DraftIdentifierGenerator`
   and constructs the timestamp-free `InvoiceDraftCandidate`.
6. Application passes the candidate to the persistence port.
7. The persistence adapter opens or joins one bounded reactive PostgreSQL transaction.
8. After all business validations have succeeded and immediately before root persistence, the
   persistence adapter invokes the injected transactional clock exactly once.
9. The persistence adapter assigns that same `java.time.Instant` to root `created_at` and
   `updated_at` and uses it as binding `created_at` where the creation timestamp is required.
10. The persistence adapter copies the candidate's supplied normalized values, calculated values,
    and final identifiers into the complete root, children, and Company-scoped idempotency binding
    and persists them atomically.
11. After successful commit, the persistence adapter reconstructs and returns
    `PersistedInvoiceDraft` with the original identifiers and equal creation timestamps.
12. Application maps the persisted representation to a transport-neutral result without
    regenerating identifiers, timestamps, or canonical values.
13. API alone arbitrates the terminal result and maps the accepted result to HTTP.

If uniqueness arbitration loses, the tentative transaction rolls back completely and a fresh
Company-scoped read resolves the committed winner. Every confirmed rollback exposes neither
timestamp and no created resource. The transaction may join an adapter-owned reactive transaction
context supplied by the repository implementation, but the port itself exposes no transaction,
Panache, or HTTP type.

### Equivalent Replay Flow

After Stage-6 normalization/fingerprinting and the Company-scoped binding match, the repository
loads the existing persisted representation. Replay returns the original root identifier,
persisted business values, `createdAt`, and `updatedAt`; it does not obtain new root/child
identifiers, invoke the clock, rebuild `canonicalName`, construct another aggregate, change either
timestamp, or revalidate emission date against the current date. A committed binding remains
authoritative after response loss and has no time-based expiration while its draft exists. The
retry request's one Stage-6 pass exists only to compute the comparable fingerprint; a matching
binding never causes reconstruction of the stored aggregate's canonical values.

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
