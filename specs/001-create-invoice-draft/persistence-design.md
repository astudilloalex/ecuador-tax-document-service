# Persistence Design: Create Invoice Draft

**Feature**: `001-create-invoice-draft`
**Database**: PostgreSQL 18.4
**Evolution authority**: Flyway only

## Responsibilities

Persistence owns only the local Invoice Draft aggregate, local tax-document reference catalogs,
and the local idempotency binding. It does not own or validate Company, Issuer, establishment, or
emission-point master data.

Hibernate Reactive with Panache is used for business persistence. Panache models remain in
`infrastructure` and are never returned through the API or used as domain entities. Flyway uses its
supported migration datasource during controlled startup; production schema auto-generation is
disabled except for non-mutating validation where supported.

## Flyway Migration Design

The initial feature migration set MUST:

1. create versioned local identification, IVA tax-rule, and payment-method catalogs;
2. seed only approved versioned reference data;
3. create `invoice_draft` and its local child structures;
4. create `invoice_draft_idempotency`;
5. add all local foreign keys, unique constraints, checks, and required indexes;
6. verify creation from an empty PostgreSQL database.

Committed migrations are immutable. Corrections use new migrations. Manual SQL is not a normal
deployment step, and no legacy database dump is accepted as the target schema.

## Aggregate Write Transaction

For a logically new command:

1. Complete payload/header/key/body validation and fingerprint generation before opening the write
   transaction.
2. Resolve applicable local reference-catalog rows and domain values without external calls.
3. Start one bounded reactive PostgreSQL transaction.
4. Persist the Invoice Draft root.
5. Persist every line and exactly one line-tax selection per line.
6. Persist grouped tax totals, payments, and additional-information rows.
7. Persist the idempotency binding last.
8. Commit once; return `201` only after commit is confirmed.

A failure at any write step rolls back the root, all children, and the binding. No external call,
filesystem write, event publication, SRI operation, or Company operation occurs within or adjacent
to this transaction.

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

## Company-Scoped Reads and Mutations

Any repository operation that addresses an existing draft uses both CompanyId and draftId. A
binding-to-draft load also uses CompanyId. The binding composite foreign key guarantees that a
binding cannot reference a draft in another Company partition.

Child loads occur through the already Company-scoped root and local foreign keys; CompanyId is not
duplicated on children. This is aggregate consistency and data partitioning, not caller
authorization.

## Required Constraints and Indexes

| Structure | Required persistence evidence |
|-----------|-------------------------------|
| `invoice_draft` | PK `id`; `UNIQUE (company_id,id)`; non-nil Company/emission UUID checks; fixed status/currency; non-negative totals |
| `invoice_line` | Local FK; `UNIQUE (invoice_draft_id,position)`; quantity/monetary checks; FK index |
| `invoice_line_tax` | Local FK; `UNIQUE (invoice_line_id)`; IVA treatment/rate checks |
| `invoice_tax_total` | Local FK; unique complete grouping key; non-negative totals |
| `invoice_payment` | Local FK; `UNIQUE (invoice_draft_id,payment_method_id)`; amount checks |
| `invoice_additional_information` | Local FK; unique canonical name and position per draft |
| `invoice_draft_idempotency` | `UNIQUE (company_id,idempotency_key_hash)`; unique draft; 32-byte hash checks; composite Company/draft FK |

Indexes are added for primary/unique constraints and child foreign-key loads. No speculative
Company chronological/search index is added until an approved retrieval use case requires it.

## Timeout and Availability Semantics

- Overall create request deadline: 10 seconds.
- Local write-transaction timeout: 5 seconds.
- PostgreSQL pool acquisition/query timeouts MUST be bounded beneath the overall request deadline.
- Confirmed pre-commit database unavailability maps to `PERSISTENCE_UNAVAILABLE` (`503`).
- Deadline exhaustion maps to `REQUEST_TIMEOUT` (`504`).
- SQL state, query text, table/column names, connection details, and stack traces are never exposed.

If commit status is uncertain or a response is lost after commit, the client retries the same
CompanyId, idempotency key, and content. The local binding resolves the authoritative result.

## Readiness

Readiness checks the same configured PostgreSQL destination used by reactive persistence and the
successful completion of required local Flyway/catalog initialization. The check is read-only and
non-destructive. It does not call Company Service, Keycloak, a gateway/BFF, SRI, or any other
external service.

Liveness remains independent of PostgreSQL availability.

## Migration and Persistence Evidence

- empty-database Flyway migration;
- migration repeatability and production auto-generation disabled;
- real PostgreSQL constraint tests;
- aggregate load through CompanyId plus draftId;
- composite binding-to-draft Company integrity;
- same key independent across Companies;
- 50-way equivalent concurrency yields one root/binding;
- conflicting concurrency yields one winner and stable conflict;
- injected failure after every write phase leaves zero partial rows/binding;
- unavailable/timeout outcomes are safe and correlated;
- no buyer payload, raw idempotency key, or normalized request stored in the binding;
- schema inspection confirms every prohibited Company/identity/snapshot structure is absent.
