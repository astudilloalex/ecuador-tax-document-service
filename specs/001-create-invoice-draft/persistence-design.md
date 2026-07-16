# Persistence Design: Create Invoice Draft

**Feature**: `001-create-invoice-draft`
**Database**: PostgreSQL 18.4
**Evolution authority**: Flyway only

## Responsibilities

Persistence owns only the local Invoice Draft aggregate, local tax-document reference catalogs,
and the local idempotency binding. It does not own or validate Company, Issuer, establishment, or
emission-point master data.

The reference catalogs are locally stored but globally governed and immutable for their published
version. They are not Company-owned, have no `company_id`, and are not filtered by Company.

Hibernate Reactive with Panache is used for business persistence. Panache models remain in
`infrastructure` and are never returned through the API or used as domain entities. Flyway uses its
supported migration datasource during controlled startup; production schema auto-generation is
disabled except for non-mutating validation where supported.

## Flyway Migration Design

The initial feature migration set MUST:

1. create versioned local identification, IVA tax-rule, and payment-method catalogs;
2. seed only rows marked approved in `reference-data-baseline.md`, including published stable UUIDs
   where required;
3. create `invoice_draft` and its local child structures;
4. create `invoice_draft_idempotency`;
5. add all local foreign keys, unique constraints, checks, and required indexes;
6. verify creation from an empty PostgreSQL database.

Committed migrations are immutable. Corrections use new migrations. Manual SQL is not a normal
deployment step, and no legacy database dump is accepted as the target schema.

The runtime exposes no reference-data administration write path. Each official catalog change is
delivered by a new immutable migration containing versioned rows and a verification query that
fails on duplicate or overlapping active target-effective intervals. The later seed migration MUST
reproduce exactly the 5 buyer, 6 IVA, and 8 payment rows approved in
`reference-data-baseline.md`; no migration is created by this remediation.

## Exact Local Catalog Structures

`buyer_identification_type_catalog` contains exactly: `official_code varchar(2)`,
`official_label varchar(100)`, `display_name varchar(100)`, `validation_strategy varchar(64)`,
`validation_rule_version varchar(64)`, nullable `source_valid_from date`, nullable
`source_valid_to date`, `target_valid_from date`, nullable `target_valid_to date`,
`active boolean`, `catalog_version varchar(64)`, `official_source_uri text`, and
`official_source_locator varchar(128)`. Its primary key is `(official_code, catalog_version)`.
Codes are exactly two ASCII digits, source and target dates are independently ordered,
`source_valid_to` requires `source_valid_from`, active rows require complete validation and source
metadata, and Flyway rejects overlapping active target intervals for the same code. Every column
is `NOT NULL` except `source_valid_from`, `source_valid_to`, and `target_valid_to`.

`iva_tax_rule_catalog` contains exactly: `id uuid`, `family varchar(16)`,
`official_tax_code varchar(8)`, `official_percentage_code varchar(8)`,
`official_label varchar(100)`, `display_name varchar(100)`, `treatment varchar(32)`,
`rate numeric(5,2)`, nullable `source_valid_from date`, nullable `source_valid_to date`,
`target_valid_from date`, nullable `target_valid_to date`, `active boolean`,
`catalog_version varchar(64)`,
`official_source_uri text`, and `official_source_locator varchar(128)`. The primary key is
`(id, catalog_version)`; `id` is the published stable `taxRuleId`. The unique natural-version key is
`(official_tax_code, official_percentage_code, target_valid_from, catalog_version)`. Family is exactly
`IVA`; treatment is approved; rate is `0.00` through `100.00`; non-percentage treatments require
`0.00`; percentage treatments require a positive approved rate; source and target dates are
independently ordered; `source_valid_to` requires `source_valid_from`; and Flyway rejects
overlapping active target intervals for the same official tax/percentage code. Every column is
`NOT NULL` except `source_valid_from`, `source_valid_to`, and `target_valid_to`.

There is no separate tax-category table or lifecycle. The rule's immutable `family=IVA` value is
the complete category representation for this feature, while the rule row owns activity and
effectivity.

`payment_method_catalog` contains exactly: `id uuid`, `official_code varchar(8)`,
`official_label varchar(160)`, `display_name varchar(100)`, `source_valid_from date`, nullable
`source_valid_to date`, `target_valid_from date`, nullable `target_valid_to date`, `active boolean`,
`catalog_version varchar(64)`, `official_source_uri text`, and
`official_source_locator varchar(128)`. The primary key is `(id, catalog_version)`; `id` is the
published stable `paymentMethodId`. The unique natural-version key is
`(official_code, target_valid_from, catalog_version)`. Source and target dates are independently
ordered and Flyway rejects overlapping active target intervals for the same official code. Every
column is `NOT NULL` except `source_valid_to` and `target_valid_to`.

Drafts reference identification rows by `(buyer_identification_type_code,
buyer_identification_catalog_version)`. Line-tax and payment rows reference the approved catalog
rows by their UUID and applied catalog version. All catalog foreign keys are local. Existing drafts
retain the applied official code and display/evidence fields defined by the aggregate model; later
catalog versions MUST NOT reinterpret historical drafts.

## Aggregate Write Transaction

For a logically new command:

1. Complete payload, Company/correlation header, exact single-valued Idempotency-Key
   presence/cardinality/one-time SP/HTAB normalization/grammar, request-body validation, and
   fingerprint generation before opening the write transaction. Only the normalized accepted key
   proceeds; repeated or comma-combined input never selects a first value.
2. Resolve applicable local reference-catalog rows and domain values without external calls. Every
   reference-data invocation receives the explicit remaining `Duration` derived from the one
   application `RequestDeadline`; its reactive adapter clamps pool acquisition and every query
   subscription to the minimum of configured database-operation timeout and that remainder and
   starts no database operation when the remainder is zero or negative.
3. Start one bounded reactive PostgreSQL transaction.
4. After all business validations have succeeded and immediately before root persistence, call the
   injectable application clock exactly once inside the transaction to obtain `createdAt` as a UTC
   `java.time.Instant`.
5. Persist the Invoice Draft root with that immutable value.
6. Persist every line and exactly one line-tax selection per line.
7. Persist grouped tax totals, payments, and additional-information rows.
8. Persist the idempotency binding last with the same `createdAt` value.
9. Commit once; return `201` only after commit is confirmed, exposing the same persisted value.

A failure at any write step rolls back the root, all children, and the binding. No external call,
filesystem write, event publication, SRI operation, or Company operation occurs within or adjacent
to this transaction.

Catalog resolution MUST reject an inactive, not-yet-target-effective, target-expired, unknown, or
ambiguous row before persistence. Initial valid references are exactly those published in
`SRI-OFFLINE-2.32-TARGET-1`; product/tax legal classification remains upstream responsibility.

`createdAt` is not PostgreSQL's physical commit timestamp. Persistence MUST NOT query or reconstruct
it after commit and MUST NOT require `track_commit_timestamp`. Rollback means it is never exposed as
a created resource; equivalent replay loads and returns the originally persisted value.

## Concurrency Arbitration

The database constraint `UNIQUE (company_id, idempotency_key_hash)` is the sole concurrent-create
arbiter.

Two requests may both observe no committed binding. Both may calculate and begin tentative writes.
The first binding insert that commits wins. A losing insert receives the uniqueness outcome and its
entire tentative aggregate transaction is rolled back. The application then performs a fresh read
by CompanyId and key hash:

- matching fingerprint and normalization version → return the committed draft as replay;
- different fingerprint → return `IDEMPOTENCY_CONFLICT`.

PostgreSQL `READ COMMITTED` plus the unique constraint is sufficient. `SERIALIZABLE`, advisory
locks, application mutexes, caches, and reservation rows are not introduced without evidence that
the approved invariant cannot otherwise be met.

## Company-Scoped Aggregate and Binding Operations

Every repository query or mutation involving the Invoice Draft aggregate or its idempotency binding
includes and enforces authoritative CompanyId. This covers creation, draft lookup, duplicate and
idempotency lookup, binding creation/retrieval, aggregate persistence mutations, and any future
repository operation introduced by this feature for the aggregate. Existing-draft access uses
CompanyId plus draftId; binding access uses CompanyId plus key hash; binding-to-draft load uses
CompanyId. The composite foreign key prevents a cross-Company binding.

Child loads occur through the already Company-scoped root and local foreign keys; CompanyId is not
duplicated on children. This is aggregate consistency and data partitioning, not caller
authorization.

This scope does not automatically apply to global VAT, payment-method, identification-type, or
other immutable global SRI reference catalogs. Their reads use the applicable global code/version/
effective-date keys and their tables contain no Company column.

## Required Constraints and Indexes

| Structure | Required persistence evidence |
|-----------|-------------------------------|
| `invoice_draft` | PK `id`; `UNIQUE (company_id,id)`; non-nil Company/emission UUID checks; type-conditional case-sensitive ASCII `^[A-Za-z0-9]{1,20}$` for buyer codes 06/08; fixed status/currency; immutable `created_at`; non-negative totals |
| `invoice_line` | Local FK; `UNIQUE (invoice_draft_id,position)`; locale-independent case-sensitive ASCII `^[A-Za-z0-9]{1,25}$` product code; `numeric(12,6)` quantity/unit-price and `numeric(17,2)` money checks; FK index |
| `invoice_line_tax` | Local FK; `UNIQUE (invoice_line_id)`; IVA treatment/rate checks |
| `invoice_tax_total` | Local FK; unique complete grouping key; non-negative totals |
| `invoice_payment` | Local FK; `UNIQUE (invoice_draft_id,payment_method_id)`; amount checks |
| `invoice_additional_information` | Local FK; unique canonical name and position per draft |
| `invoice_draft_idempotency` | `UNIQUE (company_id,idempotency_key_hash)`; unique draft; 32-byte hash checks; composite Company/draft FK |
| `buyer_identification_type_catalog` | Exact two-digit code/version PK; ordered dates; complete evidence; no overlapping active interval |
| `iva_tax_rule_catalog` | Stable UUID plus catalog-version PK; official natural-version uniqueness; `numeric(5,2)` rate/treatment/date/evidence checks; no overlapping active interval |
| `payment_method_catalog` | Stable UUID plus catalog-version PK; official natural-version uniqueness; date/evidence checks; no overlapping active interval |

Indexes are added for primary/unique constraints and child foreign-key loads. No speculative
Company chronological/search index is added until an approved retrieval use case requires it.

## Numeric Overflow Boundary

All quantity and unit-price persistence columns use `numeric(12,6)`. All discount, payment,
tax-base, tax-amount, line-total, grouped-total, subtotal, and grand-total columns use
`numeric(17,2)`. Catalog and applied percentage rates use `numeric(5,2)`. The application validates
input scale and range, every exact intermediate, every rounded line result, every grouped sum,
payment reconciliation, and final totals before opening persistence. A violation returns
`BUSINESS_VALIDATION_FAILED` with `MONETARY_RANGE_EXCEEDED`.

PostgreSQL numeric overflow, implicit scale rounding, truncation, clamping, or driver conversion
MUST NOT serve as business validation. Database checks repeat stable row-local limits only as a
final integrity barrier. Boundary-plus-one precision and magnitude tests MUST fail before any
root, child, or idempotency row is written.

## Timeout and Availability Semantics

- Overall create request deadline: 10 seconds.
- Local write-transaction timeout: 5 seconds.
- Every aggregate and buyer/IVA/payment reference-data repository operation receives an explicit
  remaining monotonic `Duration`; no persistence port depends on HTTP or Quarkus request objects.
- Each adapter bounds PostgreSQL pool acquisition and every reactive query subscription by
  `minimum(configured database operation timeout, remaining request budget)` and starts no database
  operation when the supplied remainder is zero or negative.
- A configured database-operation timeout that becomes conclusive while request budget remains maps
  to `PERSISTENCE_UNAVAILABLE`; exhausted shared request budget maps to `REQUEST_TIMEOUT`.
- A write transaction uses the lesser of the remaining request budget and 5 seconds, and no new
  persistence operation starts after the request budget is exhausted.
- Confirmed pre-commit database unavailability maps to `PERSISTENCE_UNAVAILABLE` (`503`).
- Deadline exhaustion before a persistence result is conclusive maps to `REQUEST_TIMEOUT` (`504`);
  confirmed rollback/failure or confirmed commit before expiry keeps its already selected result.
- A database completion arriving after deadline expiry does not replace the selected HTTP result.
- SQL state, query text, table/column names, connection details, and stack traces are never exposed.

Zero-state guarantees apply only to a confirmed pre-commit failure or a transaction confirmed fully
rolled back. If commit status is uncertain or a response is lost after commit, the service does not
claim zero state or attempt compensating deletion; the client retries the same CompanyId,
idempotency key, and content, and the local binding resolves the authoritative result.

Once the HTTP response is committed, deadline expiry is transport telemetry only. Persistence does
not receive another write command, mutate aggregate/domain status, delete the draft or binding, or
perform compensation. Existing response serialization may complete, and the safe
`request_deadline_exceeded_after_response_commit` event records the already selected status without
buyer data, request body, or raw idempotency key.

## Readiness

Readiness checks the same configured PostgreSQL destination used by reactive persistence and the
successful completion of required local Flyway/catalog initialization. The check is read-only and
non-destructive. It does not call Company Service, Keycloak, a gateway/BFF, SRI, or any other
external service.

Liveness remains independent of PostgreSQL availability.

## Migration and Persistence Evidence

- empty-database Flyway migration;
- migration refusal when a seed row lacks approval, evidence, a required stable UUID, or a
  non-overlapping effective interval;
- migration repeatability and production auto-generation disabled;
- real PostgreSQL constraint tests;
- aggregate load through CompanyId plus draftId;
- every aggregate/binding query and mutation enforces CompanyId while global catalogs have no
  Company column or filter;
- composite binding-to-draft Company integrity;
- same key independent across Companies;
- 50-way equivalent concurrency yields one root/binding;
- conflicting concurrency yields one winner and stable conflict;
- injected confirmed pre-commit failure after every write phase leaves zero partial rows/binding;
- unavailable/timeout outcomes are safe and correlated;
- reference-data timeout clamping proves both sides of the configured-timeout/remaining-budget
  minimum, exhausted-before-invocation with no query, and expiry during lookup with no state;
- unresolved-at-deadline commit makes no zero-state claim and same-scope replay creates at most one
  draft;
- post-response-commit expiry causes no second response-driven database write or compensation;
- no buyer payload, raw idempotency key, or normalized request stored in the binding;
- schema inspection confirms every prohibited Company/identity/snapshot structure is absent;
- numeric boundary tests confirm overflow and excess precision are rejected before persistence and
  never surface as PostgreSQL errors.
- product/passport/foreign-identification database checks use the same locale-independent ASCII
  expressions as OpenAPI/Java/domain tests, never POSIX `[[:alnum:]]`;
- `createdAt` capture occurs once at the defined in-transaction point, persists/returns unchanged,
  is absent from rolled-back resources, replays unchanged, and uses no physical commit timestamp.

## Governance and Planning Gate

The reference-data planning gate is complete. `reference-data-baseline.md` separates official
facts from target decisions, publishes UUIDv5 namespace
`32576bbf-b70d-5c24-98ff-d5f9b48e8826`, and approves every initial row. Flyway remains the sole
future schema/seed owner. Constitution v2.0.0 is approved on `main`, the reconciled plan passes its
Constitution Check, but `GATE-GOV-001` blocks T017 and all later implementation. The completed
T013–T016 persistence artifacts require retrospective review, including the exact ASCII constraint
deviations recorded in `governance-nonconformity.md`. No committed migration is edited by this
remediation; any correction requires a later immutable migration after real owner approval and a
clean analysis gate.
