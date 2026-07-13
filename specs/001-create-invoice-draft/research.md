# Research: Create Invoice Draft

**Feature**: `001-create-invoice-draft`
**Date**: 2026-07-12
**Constitution**: v2.0.0
**Status**: Complete — official evidence and target mappings are approved in
`reference-data-baseline.md`; the separate Constitution-on-main governance gate remains outside
this research status

## 1. Runtime and Framework Baseline

**Decision**: Use Java 25, Quarkus 3.33.2.1 LTS, and mandatory JVM execution.

The repository currently declares Quarkus `3.37.2` for both plugin and platform in
`gradle.properties`. The first implementation setup action MUST align both values to `3.33.2.1`;
mixed plugin/platform versions are prohibited. This plan records the change but does not mutate
build configuration during planning.

**Rationale**: Quarkus identifies 3.33 as the current production-recommended LTS line and
3.33.2.1 as its current community micro release. Quarkus 3.31 introduced full Java 25 support, so
the selected later LTS line satisfies the constitutional Java baseline. JVM execution is the
release baseline; native execution remains an optional evidence-based capability.

**Alternatives considered**:

- Quarkus 3.37: newer but non-LTS; rejected for this initial service baseline.
- Quarkus 4 preview/development lines: rejected because no approved requirement needs them.
- Mandatory native deployment: rejected by the constitution; JVM remains acceptable.

**Official evidence**:

- [Quarkus release and LTS status](https://quarkus.io/releases/)
- [Quarkus Java 25 support](https://quarkus.io/blog/quarkus-3-31-released/)

## 2. Required Quarkus Capabilities

**Decision**: Plan only the capabilities required for the approved feature:

- Quarkus REST with Jackson;
- Hibernate Reactive with Panache;
- reactive PostgreSQL client;
- Flyway and the PostgreSQL JDBC migration driver;
- Hibernate Validator;
- SmallRye OpenAPI and Health;
- Micrometer and OpenTelemetry when enabled by the operational profile;
- JUnit 5, REST Assured, `UniAsserter`, and PostgreSQL test support.

No OIDC, OAuth, JWT, Keycloak, security, token-propagation, Company REST client, cache, broker,
SOAP, XML, signature, certificate, PDF, notification, or SRI adapter capability is in scope.

**Rationale**: The capability set implements the approved Clean Architecture boundaries and local
reactive persistence without introducing a distributed interaction or identity layer.

## 3. PostgreSQL Baseline

**Decision**: Target PostgreSQL 18.4 while keeping schema constructs compatible with supported
PostgreSQL 18 minors.

**Rationale**: PostgreSQL 18.4 is the current supported minor release, and PostgreSQL recommends
running the current minor for a supported major. It provides the UUID, exact numeric,
`timestamptz`, transactional uniqueness, and referential-integrity behavior required here.

**Alternatives considered**:

- An older supported major: technically possible but offers no feature benefit for a greenfield
  service.
- Another datastore: prohibited by the constitutional baseline.

**Official evidence**:

- [PostgreSQL 18.4 release notes](https://www.postgresql.org/docs/release/18.4/)
- [PostgreSQL versioning policy](https://www.postgresql.org/support/versioning/)

## 4. Company Context and API Shape

**Decision**: Expose `POST /invoice-drafts` under the existing `/api/v1` base. Require exactly one
`X-Company-Id` header and one `Idempotency-Key` header. Accept an optional `X-Correlation-Id`.

The Company header is trimmed, parsed as a non-nil UUID, and normalized to lowercase hyphenated
form. It is mapped by the API layer to an application `CompanyId`; the application command carries
that value explicitly; the aggregate stores it immutably.

**Rationale**: This is the definitive constitutional business-context boundary. CompanyId is an
ownership and idempotency partition, not a credential. No Company path, query, body, token,
session, principal, or thread-local source is allowed.

**Alternatives considered**:

- `/companies/{companyId}/invoice-drafts`: prohibited because Company is not a resource owned by
  this bounded context.
- Body or query CompanyId: prohibited by the approved API contract.
- Authentication-derived Company context: impossible by design because this feature has no
  application identity state.

## 5. Correlation Contract

**Decision**: `X-Correlation-Id` accepts one trimmed identifier containing 1–64 characters, starting
with an ASCII letter or digit, and then using only `A-Z`, `a-z`, `0-9`, `.`, `_`, `:`, or `-`. A
valid supplied value is preserved and returned. When absent, the API generates a safe UUID and
returns it. Blank, repeated, over-length, or unsafe supplied values are never echoed; the API
generates a safe replacement UUID for the error response and returns `INVALID_REQUEST` when
correlation validation governs. Correlation validation follows Company validation and precedes
idempotency-key validation.

**Rationale**: The bounded character set prevents log/header injection while accepting common
distributed-tracing identifiers. Correlation is transport evidence, is excluded from idempotency,
and is not persisted on the Invoice Draft aggregate. Structured logs and traces carry it.

**Alternative considered**: UUID-only caller values were rejected as unnecessarily restrictive;
generated identifiers remain UUIDs.

## 6. Error Status Mapping

**Decision**: Use the stable catalog in `error-catalog.md` and RFC-style
`application/problem+json` responses.

| Code | HTTP status | Retry classification |
|------|-------------|----------------------|
| `COMPANY_CONTEXT_REQUIRED` | 400 | Correct request |
| `COMPANY_CONTEXT_INVALID` | 400 | Correct request |
| `INVALID_REQUEST` | 400 | Correct request |
| `PROHIBITED_CALCULATED_FIELD` | 422 | Correct request |
| `IDEMPOTENCY_CONFLICT` | 409 | Use a new key or original content |
| `REQUEST_PAYLOAD_TOO_LARGE` | 413 | Reduce request |
| `BUSINESS_VALIDATION_FAILED` | 422 | Correct business content |
| `PERSISTENCE_UNAVAILABLE` | 503 | Retry same Company/key |
| `REQUEST_TIMEOUT` | 504 | Retry same Company/key |
| `INTERNAL_ERROR` | 500 | Retry only under operational guidance |

**Rationale**: `400` covers representation/header syntax, `422` covers well-formed content rejected
by approved validation, `409` covers the idempotency binding conflict, and `503`/`504` identify
retryable local infrastructure outcomes. A retry uses the same Company and key because commit may
have succeeded before response loss.

## 7. Deterministic Request Fingerprinting

**Decision**: Persist two separate 32-byte SHA-256 values:

- a domain-separated hash of the trimmed `Idempotency-Key`;
- a domain-separated fingerprint of normalized client-controlled business content.

Persist `normalization_version = 1`. Do not persist the raw idempotency key or complete normalized
request.

**Rationale**: A cryptographic fingerprint is sufficient for equivalence and avoids duplicating
buyer personal data in the idempotency table. Domain separation prevents the two hash purposes
from sharing the same input namespace.

Normalization version 1:

- canonical lowercase hyphenated UUIDs;
- ISO `YYYY-MM-DD` emission date;
- trimmed validated text;
- canonical plain decimal representations with numerical equivalents normalized identically;
- absent optional collections normalized to empty collections;
- invoice-line order preserved;
- payments sorted by payment-method identifier and amount;
- additional information sorted by canonical name and value;
- JSON property order ignored.

The fingerprint includes emission-point identifier, emission date, buyer data, lines, selected tax
rules, payments, and additional information. It excludes CompanyId, idempotency key, correlation
identifier, headers, and all other transport metadata.

**Alternatives considered**:

- Persist normalized JSON: rejected because it duplicates buyer personal data.
- Hash raw JSON: rejected because property ordering and equivalent number representations would
  cause false conflicts.
- Global key uniqueness: rejected because the approved scope is CompanyId plus key.

## 8. Transaction and Concurrency Arbitration

**Decision**: Use one short reactive PostgreSQL transaction for a new draft aggregate, all child
records, and the idempotency binding. Use `UNIQUE (company_id, idempotency_key_hash)` as the race
arbiter under PostgreSQL `READ COMMITTED`.

Before the transaction, perform the approved request/fingerprint checks and a Company-scoped
binding lookup. If no binding exists, validate local catalogs and domain rules, calculate, then
write the aggregate and binding atomically. If concurrent insertion loses the uniqueness race, its
transaction rolls back completely; a fresh Company-scoped lookup compares the winner's
fingerprint and returns replay or conflict.

**Rationale**: The uniqueness constraint provides cross-process arbitration without application
locks, cache, `SERIALIZABLE`, a reservation state machine, or distributed coordination.

## 9. Persistence Ownership and Constraints

**Decision**: Store `company_id uuid NOT NULL` on `invoice_draft`, enforce a non-nil check, and add
`UNIQUE (company_id, id)` so idempotency can reference the root through a composite local foreign
key. Every existing-draft repository operation requires CompanyId and draftId.

Children store only local foreign keys to the root or owning line. They do not repeat CompanyId and
do not represent Company master data. Deletion behavior is not invented because draft deletion is
out of scope.

No tenant, subject, role, authorization, Company/Issuer/establishment/emission master table,
Company-context version/time, fiscal snapshot, or cross-database foreign key is allowed.

## 10. Reactive and Blocking Boundaries

**Decision**: HTTP mapping, application orchestration, and business persistence remain reactive.
Domain calculation is synchronous, deterministic, and bounded by 500 lines. No synchronous wait,
blocking network call, filesystem access, certificate work, SOAP, or XML processing exists.

Flyway runs during controlled startup through its migration datasource and is not part of the
event-loop request path. Business persistence uses the reactive PostgreSQL client only.

## 11. Fiscal and Monetary Rules

**Decision**: Preserve the exact approved `BigDecimal`, line-level `HALF_UP`, IVA-only,
buyer-identification, zero-value, payment, text, collection, and no-calculated-input rules from the
specification. Quantity and unit price use `numeric(12,6)` with maximum `999999.999999`; money uses
`numeric(17,2)` with maximum `999999999999999.99`; percentage rates use `numeric(5,2)` from `0.00`
through `100.00`. Every input, intermediate, rounded, grouped, payment-sum, or final range breach is
rejected before persistence as `BUSINESS_VALIDATION_FAILED` with
`MONETARY_RANGE_EXCEEDED`.

Capture one `requestCreationInstant` at the request boundary and derive the expected emission date
once using `America/Guayaquil`. Commit crossing midnight does not change the accepted date;
`createdAt` records the confirmed commit instant; equivalent replay does not revalidate the date.
Versioned local reference catalogs are managed and seeded only by Flyway; codes, rates, validity,
and identifiers are not hard-coded or startup-generated.

**Official evidence**:

- [SRI electronic invoicing resources and Technical Sheet v2.32](https://www.sri.gob.ec/facturacion-electronica)

## 12. Health, Performance, and Runtime Evidence

**Decision**: Liveness reports process viability. Readiness uses only the configured PostgreSQL
destination and successful local migration/catalog initialization. It performs no destructive
query and has no Company, identity-provider, gateway, BFF, or SRI dependency.

Use the measurable budgets in `operational-requirements.md`. They include only parsing,
validation, fingerprinting, calculation, PostgreSQL, and serialization within this service.
Company Service latency is nonexistent and excluded.

Packaged JVM evidence is mandatory. Native support may be claimed only after both build and
runtime smoke evidence covers DTO reflection/resources, validation, reactive persistence, Flyway
resources, OpenAPI, health, create, replay, conflict, rollback, and timeout behavior.

## 13. Reference-Data Baseline Evidence Gate

**Decision**: `reference-data-baseline.md` is the approved executable planning baseline for five
buyer-identification types, six IVA tax rules, and eight payment methods. Flyway will own the
corresponding fixed rows in a later implementation task. Buyer types use official codes `04`
through `08`; tax and payment references use deterministic UUIDv5 identifiers. Application startup
MUST NOT create or replace them, and all examples and fixtures MUST use only the published values.

The namespace is:

```text
UUIDv5(UUID.NAMESPACE_DNS, "com.alexastudillo.taxdocument.reference-data.v1")
= 32576bbf-b70d-5c24-98ff-d5f9b48e8826
```

Tax-rule names are
`tax-rule|SRI-OFFLINE-2.32|<tax-code>|<percentage-code>|<rate>|<treatment>` and payment names are
`payment-method|SRI-OFFLINE-2.32|<official-code>`. Exact values and independent recalculation
requirements are recorded in the baseline.

**Official evidence reviewed**:

- [SRI electronic invoicing page, publishing Technical Sheet v2.32](https://www.sri.gob.ec/facturacion-electronica)
- [SRI Technical Sheet v2.32, Table 6, §9.10, Tables 16–17, invoice field table, and Table 24](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf)
- [SRI IVA guidance](https://www.sri.gob.ec/impuesto-al-valor-agregado-iva)
- [SRI Resolution NAC-DGERCGC24-00000013](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar?id=6b8588f2-a4bf-44bb-ac40-085391ba2aed&nombre=NAC-DGERCGC24-00000013.pdf)
- [SRI Circular NAC-DGECCGC25-00000006](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar?id=236482f4-6125-42fd-b073-62c99d08233d&nombre=NAC-DGECCGC25-00000006.pdf)
- [SRI Circular NAC-DGECCGC26-00000005](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar?id=a3e4257d-ba2e-4ec7-89c7-635fa764b22a&nombre=NAC-DGECCGC26-00000005.pdf)

**Resolution**: The current SRI IVA page identifies 13% and 5% applicability, while the 2025 and
2026 circulars provide primary evidence for active 15% contexts. Technical Sheet v2.32 publishes
all corresponding representation codes. The target baseline therefore includes 13% and 15% as
distinct caller-selectable rules with explicit applicability notes; neither is represented as a
universal rate. The upstream billing workflow selects the appropriate rule, and this feature does
not classify products or determine legal eligibility. The approved 15% scenario remains a
mathematical rounding vector.

No exact governing checksum algorithm was located in the approved sources for draft-time RUC or
Ecuadorian identity-card validation. Under the approved feature policy, both use explicitly named
`FORMAT_ONLY` strategies. This is a resolved scope decision: checksum and registry verification are
outside Create Invoice Draft, not deferred implementation work.
