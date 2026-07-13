# Research: Create Invoice Draft

**Feature**: `001-create-invoice-draft`
**Date**: 2026-07-12
**Constitution**: v2.0.0

This record supersedes the earlier design assumptions that draft creation needed Keycloak, a
Company-context client, tenant authorization, or a fiscal snapshot. Those assumptions conflict
with the definitive approved Company-context boundary.

## 1. Quarkus and Java Baseline

**Decision**: Use Java 25 with Quarkus 3.33.2.1 LTS.

**Rationale**: The selected Quarkus LTS line provides the required reactive REST, Hibernate
Reactive with Panache, PostgreSQL, Flyway, validation, OpenAPI, health, metrics, tracing, and test
capabilities while supporting the project’s Java baseline. JVM execution remains mandatory.

**Required capability set**:

- Quarkus REST with Jackson;
- Hibernate Reactive with Panache and the reactive PostgreSQL client;
- Flyway plus the PostgreSQL JDBC driver used only for migration execution;
- Hibernate Validator;
- SmallRye OpenAPI and Health;
- Micrometer and OpenTelemetry where enabled by the operational profile;
- JUnit 5, REST Assured, `UniAsserter`, and PostgreSQL Dev Services/test support.

No OIDC, OAuth, JWT, Keycloak, token propagation, REST client, Company client, or application
security extension is required by this feature.

**Evidence**:

- [Quarkus Java compatibility](https://quarkus.io/guides/building-my-first-extension#java-version)
- [Quarkus LTS releases](https://quarkus.io/blog/lts-releases/)
- [Hibernate Reactive with Panache](https://quarkus.io/guides/hibernate-reactive-panache)
- [Flyway guide](https://quarkus.io/guides/flyway)

## 2. Definitive Company Context

**Decision**: Accept Company context only through exactly one mandatory `X-Company-Id` header. The
API validates presence, single cardinality, UUID syntax, and non-nil value, then maps the canonical
UUID to an application `CompanyId`.

**Rationale**: Constitution v2.0.0 defines the service as an internal Tax Document and Billing
bounded context. The UUID is opaque business metadata and an immutable ownership partition, not a
credential or proof of entitlement. Resource paths remain owned-resource paths, so the create
operation is `/invoice-drafts` beneath an optional global prefix.

**Rejected alternatives**:

- Company in the resource path, query, body, token, or session: constitutionally prohibited.
- Deriving Company from a principal or tenant: there is no identity or authorization context.
- Looking up Company, Issuer, establishment, or emission point: upstream responsibility and not
  verified by this service.
- `CompanyContextPort`, REST client, cache, replica, shared database, or readiness dependency:
  prohibited for Create Invoice Draft.

**Failure contract**: Missing/blank header produces `COMPANY_CONTEXT_REQUIRED`; repeated,
malformed, or nil produces `COMPANY_CONTEXT_INVALID`. No Company-not-found/inactive/not-authorized,
timeout, unavailable, or relationship-mismatch outcome exists.

## 3. No Authentication or Authorization

**Decision**: Define no authentication, authorization, security scheme, Authorization header,
principal, user, role, permission, token, `401`, or `403` behavior.

**Rationale**: Any gateway/BFF security and Company validation occur outside this repository. The
service explicitly does not verify that upstream performed them and accepts any syntactically
valid non-nil Company UUID from a reachable process.

Network/ingress controls may exist operationally outside the application, but they are not modeled
as application dependencies, contracts, requirements, or readiness checks.

## 4. Company-Scoped Durable Idempotency

**Decision**: Use persistence uniqueness on `(company_id, idempotency_key_hash)`. Store a
deterministic request fingerprint plus canonical normalized content and the committed draft
reference in the same local transaction as draft creation.

**Rationale**:

- Company UUID plus key is the exact approved scope.
- Hashing avoids requiring raw idempotency keys in persistence.
- Canonical content comparison protects against the theoretical fingerprint-collision case and
  determines semantic equivalence.
- A database uniqueness constraint is the concurrency arbiter across processes and restarts.
- Company ID is excluded from content because it is already in the uniqueness scope.
- Correlation IDs, idempotency keys, and all transport-only headers are excluded from content.

Invoice-line order is significant. Payments and additional-information entries are normalized as
order-insensitive collections. JSON property order and UUID text case/format after accepted
normalization do not affect equivalence.

**Replay**: Equivalent content in the same Company/key scope returns the original draft. Different
content conflicts. A successful binding does not expire while the draft exists. Replay performs no
Company lookup or mutable-context refresh.

**Rejected alternatives**:

- In-memory locks or caches: not durable and the constitution prohibits application cache.
- Key-only global uniqueness: incorrectly deduplicates across Companies.
- Tenant plus Company scope: tenant is absent from this bounded context.
- Company duplicated in the content fingerprint: redundant and contrary to the approved rule.

**Evidence**:

- [PostgreSQL unique constraints](https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-UNIQUE-CONSTRAINTS)
- [PostgreSQL transaction isolation](https://www.postgresql.org/docs/current/transaction-iso.html)

## 5. Reactive Transaction and Persistence

**Decision**: Use one short reactive PostgreSQL transaction for uniqueness arbitration, existing
binding comparison, local catalog resolution/validation, aggregate persistence, and binding
persistence.

**Rationale**: Draft plus every child plus idempotency binding must commit together or fully roll
back. No external call occurs, so the plan does not claim atomicity across services.

The implementation must not synchronously wait for `Uni`, perform blocking JDBC business access,
or disguise blocking work. Flyway is the sole schema/reference-data evolution mechanism and uses
its supported migration datasource during lifecycle startup.

## 6. Deterministic Monetary Calculation

**Decision**: Use exact `BigDecimal` domain arithmetic and the specification’s line-level
`HALF_UP` pipeline.

- Quantity and unit price: at most six fractional digits; excess rejected.
- Discounts and payments: exactly two fractional digits; excess rejected.
- Catalog rates: percentage points with at most two fractional digits.
- Round each gross, net, and tax amount to scale two using `HALF_UP` at the specified step.
- Aggregate totals only from rounded line values.
- Reconcile exact two-decimal payment sum to exact two-decimal grand total.

**Rationale**: The policy is deterministic across JVM and optional native runtimes and contains no
binary floating-point representation.

## 7. IVA and Fiscal Reference Catalogs

**Decision**: Maintain versioned, active, effective-dated local reference catalogs for buyer
identification types, IVA tax rules, and payment methods. All catalog and schema changes use
Flyway. Production logic never hard-codes tax codes or rates.

Exactly one active/effective IVA rule is selected per line. Supported treatments are configured
percentage-rate IVA, IVA 0%, not subject, and exempt. The three zero-tax treatments remain distinct
groups. ICE, IRBPNR, other taxes, and simultaneous taxes are rejected.

These fiscal catalogs are locally required tax-document reference data, not Company master data.

**Official evidence**:

- SRI Electronic Tax Documents Offline Scheme Technical Sheet v2.32
- [SRI electronic invoicing resources](https://www.sri.gob.ec/facturacion-electronica)

## 8. Buyer Identification and Dates

**Decision**: Apply versioned SRI rules effective on the emission date for RUC `04`, identity card
`05`, passport `06`, final consumer `07`, and foreign identification `08`. No online registry
check, invented checksum, country-specific passport rule, or legacy validator is permitted.

Final consumer requires `9999999999999`, `CONSUMIDOR FINAL`, and the SRI threshold effective on the
date; v2.32 defines USD 50.00.

Emission date uses Ecuador date-only semantics and must equal the current Ecuadorian civil date at
creation. Audit timestamps are unambiguous instants and PostgreSQL uses `timestamptz`; date-only
values use `date`.

## 9. API Contract and Validation Ownership

**Decision**: Publish synchronous `POST /api/v1/invoice-drafts`, with `X-Company-Id` and
`Idempotency-Key` headers and an optional/generatable correlation identifier.

- Transport validation covers header/body structure, representation, unknown fields, cardinality,
  limits, and calculated-field rejection.
- Application validation covers use-case preconditions, Company ownership scoping, local catalog
  applicability, idempotency, and current Ecuador date.
- Domain validation covers monetary and invoice invariants.
- Infrastructure validation covers database constraints and persistence failures.

Success is `201` for a new commit and `200` for an equivalent replay. Stable failures include
`400` request-contract errors, `409` idempotency conflict, `422` business validation, and safe
`500` persistence/internal failure. The contract defines no Company dependency statuses and no
security statuses.

## 10. Draft Versus Fiscal Issuance

**Decision**: Persist only the Company UUID, opaque emission-point identifier, commercial draft,
local catalog selections/evidence, calculations, payments, additional information, and audit
timestamps.

Do not persist Company-context versions/observation times, Issuer/establishment/emission-point
fiscal snapshots, sequences, access keys, XML, signatures, certificates, SRI state, PDFs, or
notifications. A later approved issuance feature must decide how authoritative fiscal context is
resolved and snapshotted.

## 11. Observability and Health

**Decision**: Liveness reports process viability. Readiness checks the same PostgreSQL datasource
used by persistence and no Company or identity-provider destination. Logs are structured and
correlated without buyer data, raw idempotency keys, fiscal payloads, or other sensitive content.
Company identifiers may be used only as safe operational correlation fields, never metric labels.

## 12. Testing and Runtime Evidence

Required evidence includes:

- pure domain calculation and validation vectors;
- header/cardinality/UUID/nil and no-security OpenAPI tests;
- application Company-scoping and no-Company-dependency architecture tests;
- real PostgreSQL reactive persistence and committed-concurrency tests;
- empty-database Flyway migrations including reference data;
- API request/response and safe error tests;
- official buyer, IVA, rounding, final-consumer, and zero-value vectors;
- calculated-input rejection, rollback, response-loss replay, and lifetime binding tests;
- packaged JVM smoke tests.

Native compatibility is not a production requirement. It may be claimed only after a native build
and runtime smoke suite proves create, replay, conflict, PostgreSQL/Flyway resources, OpenAPI,
health, and representative validation behavior. JVM deployment remains acceptable if native
evidence fails or complexity is unjustified.
