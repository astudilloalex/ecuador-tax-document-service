# Phase 0 Research: Create Invoice Draft

**Feature**: `001-create-invoice-draft`

**Date**: 2026-07-12

**Status**: Complete; no open research items remain

## 1. Quarkus, Java, and Build Baseline

**Decision**: Use Quarkus Platform `3.33.2.1` LTS for both the Gradle plugin and
`io.quarkus.platform:quarkus-bom`, Java 25, and the existing Gradle `9.5.1` wrapper.

**Rationale**: Quarkus identifies 3.33 as the production-recommended LTS line through
2027-03-25. Patch `3.33.2.1` includes the June 2026 security correction and inherits full Java 25
support introduced in Quarkus 3.31. Gradle 9.5.1 can run on Java 25. The existing scaffold uses
non-LTS Quarkus `3.37.2`; implementation must align it to the selected LTS before adding feature
code.

**Alternatives considered**:

- Quarkus `3.37.2`: rejected for this fiscal service because non-LTS releases are maintained only
  until the next minor line.
- Earlier 3.33 patches: rejected because `3.33.2.1` contains the applicable security fix.
- Panache Next: rejected because it is experimental and the constitution requires Hibernate
  Reactive with Panache.

**Evidence**:

- [Quarkus release and support table](https://quarkus.io/releases/)
- [Quarkus 3.33 LTS announcement](https://quarkus.io/blog/quarkus-3-33-released/)
- [Quarkus 3.31 Java 25 support](https://quarkus.io/blog/quarkus-3-31-released/)
- [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html)

## 2. Required Quarkus Extensions

**Decision**: Use the platform-managed extensions below; do not declare standalone versions.

- `quarkus-arc`
- `quarkus-rest-jackson`
- `quarkus-rest-client-jackson`
- `quarkus-hibernate-reactive-panache`
- `quarkus-reactive-pg-client`
- `quarkus-flyway`
- `quarkus-jdbc-postgresql` solely for Flyway
- `org.flywaydb:flyway-database-postgresql`
- `quarkus-oidc`
- `quarkus-rest-client-oidc-token-propagation`
- `quarkus-hibernate-validator`
- `quarkus-smallrye-openapi`
- `quarkus-smallrye-health`
- `quarkus-micrometer-registry-prometheus`
- `quarkus-opentelemetry`
- `quarkus-junit`, REST Assured, OIDC/Keycloak test support in test scope

**Rationale**: This is the smallest extension set that provides the mandated reactive HTTP and
persistence stack, the required company-context adapter, Flyway, OIDC, validation, contract
documentation, health, metrics, traces, and risk-based tests. Mutiny is supplied by the reactive
extensions and does not need a separate unmanaged dependency.

**Alternatives considered**:

- Classic RESTEasy or blocking Hibernate ORM: rejected because they conflict with the reactive
  baseline.
- Unified preview Micrometer/OpenTelemetry bridge: rejected; stable separate extensions are
  sufficient.
- A message broker, cache, or background processor: rejected because draft creation is synchronous
  and has no approved distributed-work requirement.

**Evidence**:

- [Quarkus REST Jackson](https://quarkus.io/extensions/io.quarkus/quarkus-rest-jackson/)
- [Hibernate Reactive with Panache](https://quarkus.io/guides/hibernate-reactive-panache)
- [Flyway with reactive datasources](https://quarkus.io/guides/flyway#flyway-and-reactive-datasources)
- [Quarkus OIDC bearer authentication](https://quarkus.io/guides/security-oidc-bearer-token-authentication)
- [Quarkus observability](https://quarkus.io/guides/observability)

## 3. PostgreSQL Baseline

**Decision**: Target PostgreSQL 18 and require the current 18.x security/minor release; the
planning baseline is `18.4`.

**Rationale**: PostgreSQL 18 is the current stable major, is supported through November 2030, and
18.4 is the current security-corrected minor release on the plan date. PostgreSQL 18 provides
`UNIQUE NULLS NOT DISTINCT`, which directly models the optional tenant portion of the approved
idempotency scope.

**Alternatives considered**:

- PostgreSQL 17: acceptable only when a hosting platform has not certified 18, but it has a shorter
  support horizon and would require a documented deployment exception.
- PostgreSQL 19 beta: rejected because it is not production-ready.
- H2 for tests: rejected because it cannot prove PostgreSQL concurrency, `jsonb`, `numeric`,
  `date`, `timestamptz`, or uniqueness behavior.

**Evidence**:

- [PostgreSQL 18.4 release](https://www.postgresql.org/about/news/postgresql-184-1710-1614-1518-and-1423-released-3297/)
- [PostgreSQL versioning policy](https://www.postgresql.org/support/versioning/)
- [PostgreSQL constraints](https://www.postgresql.org/docs/18/ddl-constraints.html)

## 4. Reactive Transactions and Persistence Boundary

**Decision**: Define one invoice-draft persistence application port returning `Uni`. Implement it
with infrastructure-only Panache repository-style entities. On Quarkus 3.33, place a single
`@WithTransaction` boundary in the infrastructure adapter; do not use blocking `@Transactional`
or mix transaction styles.

Authentication, current Company authorization, and normalization complete before the short local
transaction. For an unbound command, creation-context eligibility, domain validation, catalog
lookup, and calculation also complete before it. The transaction only arbitrates the idempotency
binding and persists or loads the invoice aggregate. No remote company call occurs inside it.

**Rationale**: Hibernate Reactive transactions are single-datasource and non-XA. Repository-style
Panache keeps persistence operations behind an actual application boundary and prevents Panache
entities from becoming domain entities. Quarkus 3.33 predates general `@Transactional` support for
Hibernate Reactive, so its reactive transaction annotation is the supported choice.

**Alternatives considered**:

- Panache active record: rejected because it couples persistence behavior to infrastructure
  entities without benefit.
- Transaction spanning the company service and PostgreSQL: prohibited and not technically atomic.
- Generic repository hierarchy: rejected by the constitution.
- Parallel persistence pipelines inside one transaction: rejected by Hibernate Reactive guidance.

**Evidence**:

- [Hibernate Reactive transaction limitations](https://quarkus.io/guides/hibernate-reactive)
- [Hibernate Reactive with Panache transactions](https://quarkus.io/guides/hibernate-reactive-panache)

## 5. Flyway and Reactive Persistence Coexistence

**Decision**: Flyway is the only schema writer. Use the reactive PostgreSQL client for application
traffic and the JDBC PostgreSQL driver only for Flyway. Disable Hibernate schema mutation and use
non-mutating mapping validation. Run Flyway at dev/test startup; production runs migrations once
before replicas accept traffic.

**Rationale**: Flyway is JDBC/blocking even when the business datasource is reactive. Startup or a
deployment migration job is an explicit non-request boundary and does not block an event-loop.
Separating schema deployment from replicas avoids competing migration mechanisms.

**Alternatives considered**:

- Hibernate create/update/drop: constitution violation.
- Manual production SQL: constitution violation.
- Both startup migration on every replica and a deployment job: rejected as duplicate operational
  ownership.

**Evidence**:

- [Quarkus Flyway guide](https://quarkus.io/guides/flyway)
- [Flyway versioned migrations](https://documentation.red-gate.com/fd/versioned-migrations-273973333.html)

## 6. Idempotency Arbitration and Normalized Content

**Decision**: Persist a dedicated `invoice_draft_idempotency` row in the same transaction as the
draft. Enforce one binding with:

```sql
UNIQUE NULLS NOT DISTINCT (tenant_id, company_id, idempotency_key)
```

The transaction first attempts a non-updating `INSERT ... ON CONFLICT ... DO NOTHING` claim using
a pre-generated draft UUID and a deferred binding-to-draft foreign key. The winner persists the
aggregate. A loser loads the committed binding and returns the original draft when normalized
content is equal, or an idempotency conflict when it differs. Any failure rolls back both claim and
draft.

After current Company authorization establishes the effective scope, a read-only binding lookup is
used as a replay fast path before mutable Issuer, emission-point, or catalog eligibility is
revalidated. This prior read is not the concurrency arbiter; an unbound command still uses the
database uniqueness claim. A bound equivalent command returns the immutable original draft, while
different normalized content conflicts.

Store the normalized business content as fixed-structure `jsonb` with a normalization-version
field. Preserve invoice-line order; sort payments by payment-method identity; sort additional
information by canonical name; trim strings; encode decimals canonically; and exclude transport
details. Do not index JSON contents because lookup is by scoped key.

**Rationale**: The database uniqueness constraint, not a racing prior read or application lock,
arbitrates concurrent commands. Exact normalized content avoids hash-collision semantics and makes
normalization defects diagnosable. PostgreSQL `jsonb` ignores object-key order while preserving
array order.

**Alternatives considered**:

- Read then insert: rejected because it races.
- Application mutex, advisory lock, or separate idempotency service: rejected as unnecessary
  coordination.
- Digest only: rejected because equality would not be exact.
- `ON CONFLICT DO UPDATE`: rejected because a replay must not mutate the binding or draft.

**Evidence**:

- [PostgreSQL INSERT and ON CONFLICT](https://www.postgresql.org/docs/18/sql-insert.html)
- [PostgreSQL unique-index concurrency](https://www.postgresql.org/docs/18/index-unique-checks.html)
- [PostgreSQL JSON types](https://www.postgresql.org/docs/18/datatype-json.html)

## 7. Exact Decimal Storage and Validation

**Decision**: Use `BigDecimal` in Java and unconstrained PostgreSQL `numeric`, with application
validation and named database checks for approved scale, total-digit, sign, and cross-column row
invariants. Do not depend on `numeric(p,s)` coercion because PostgreSQL may round excess fractional
digits before a check can reject them.

- Quantity and unit price: at most 18 total digits and six fractional digits.
- Caller discount and payment: at most 14 total digits and exactly two fractional digits.
- Percentage rate: 0 through 100 percentage points, at most two fractional digits.
- Persisted monetary results and totals: at most 14 total digits and exactly two fractional digits.
- Every calculated result that exceeds an approved precision is a business validation failure,
  never a persistence truncation.

**Rationale**: These limits align the approved scale policy with SRI Technical Sheet v2.32 field
bounds. PostgreSQL `numeric` is exact; explicit checks and pre-persistence validation reject rather
than silently round. Aggregate sums and child counts stay in domain/application validation because
PostgreSQL `CHECK` constraints cannot safely validate other rows.

**Alternatives considered**:

- `numeric(p,s)` alone: rejected because coercion can silently round.
- Floating point: prohibited.
- Integer minor units: rejected because quantity and unit price allow six fractional digits.

**Evidence**:

- [SRI Technical Sheet v2.32](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf)
- [PostgreSQL exact numeric](https://www.postgresql.org/docs/18/datatype-numeric.html)
- [PostgreSQL CHECK constraint limits](https://www.postgresql.org/docs/18/ddl-constraints.html)

## 8. Date and Time Semantics

**Decision**: Map emission date to Java `LocalDate` and PostgreSQL `date`. Map creation and
last-modification timestamps to Java `Instant` and PostgreSQL `timestamptz(6)`. Capture one injected
clock instant, derive the Ecuador date with `America/Guayaquil`, validate the supplied date, and use
that same instant for both initial audit timestamps. Configure database/session diagnostics in UTC.

**Rationale**: `date` cannot acquire an accidental time zone. `timestamptz` identifies an instant
unambiguously. One clock sample prevents a midnight boundary race.

**Alternatives considered**:

- Timestamp for emission date: rejected because the approved fiscal value is date-only.
- Timestamp without time zone for audit data: rejected because it is ambiguous.
- Separate application and database clock reads: rejected because they may disagree at midnight.

**Evidence**:

- [PostgreSQL date/time types](https://www.postgresql.org/docs/18/datatype-datetime.html)
- [Hibernate Java time mappings](https://docs.hibernate.org/orm/7.1/userguide/html_single/#basic-type-java-time)

## 9. Company Context and Reference Catalogs

**Decision**: Place company, tenant, single-Issuer, emission-point, and caller-access resolution
behind one outbound `CompanyContextPort`. Its result separates the current authorized Company
scope from an eligible/ineligible creation-context evaluation. This permits an authorized binding
lookup and immutable replay even if mutable Issuer or emission-point data changed after the first
commit; eligibility is required only for an unbound command. Implement one non-blocking Quarkus
REST Client adapter that propagates the bearer token and correlation ID to the authoritative
company capability. The token must contain both the invoice-service audience and the configured
company-capability audience; propagation is restricted to the allow-listed TLS base URL.
Configure a 1-second connect timeout, 3-second response timeout, and 5-second total validation
budget, with no automatic retry. Map timeout to `504`; map connection/unavailability to `503`; no
failure creates an idempotency binding.

The Company bounded context remains the sole current master-data authority. The Tax Document
Service persists only the external Company identifier as document ownership plus the immutable
Issuer/establishment/emission-point snapshot used by the draft. It uses no shared database/schema,
cross-service foreign key/repository/transaction, Company cache, materialized view, or background
replication. Adapter responses live only for the active request.

Keep versioned identification, IVA tax-rule, and payment-method catalogs as local read-only
PostgreSQL reference tables managed by Flyway. This feature adds no catalog-management API.

**Rationale**: One company call satisfies the explicit source-of-truth requirement without copying
company ownership data. Separating authorization from current creation eligibility preserves the
specified replay semantics without bypassing current caller access. No automatic retry keeps the
synchronous latency bound and avoids retry amplification; the caller can retry the entire
idempotent command. Local fiscal catalogs provide a transactionally consistent effective-date view
and avoid inventing an additional service.

**Alternatives considered**:

- Copy company data into local authoritative tables: rejected because the company capability is the
  approved source of truth.
- Separate remote call per company, Issuer, and emission point: rejected because it creates an
  avoidable partial-failure chain.
- Remote catalog microservice: rejected without a requirement.

**Evidence**:

- [Quarkus REST Client](https://quarkus.io/guides/rest-client)
- [Quarkus OIDC token propagation](https://quarkus.io/guides/security-openid-connect-client-reference#token-propagation-rest)

## 10. API and Error Contract

**Decision**: Expose one synchronous protected operation:

```text
POST /api/v1/companies/{companyId}/invoice-drafts
```

Require `Authorization` and `Idempotency-Key`; accept or generate `X-Correlation-ID`. Use
string-encoded decimal fields so lexical scale is not lost through JSON clients. Return `201` for
the first commit and `200` for an equivalent replay. Use RFC 9457 `application/problem+json` with a
stable English `code`, correlation identifier, and safe field violations.

Status mapping: `400` malformed representation, `401` invalid authentication, `403` missing role,
concealed `404` inaccessible ownership scope, `409` idempotency conflict, `422` business/domain
validation, `503` unavailable dependency, `504` dependency timeout, and safe `500` unexpected
failure.

**Rationale**: The mapping distinguishes transport failures from domain failures and preserves
resource concealment. RFC 9457 supports machine-readable extensions without exposing internals.

**Alternatives considered**:

- Return `201` for replay: rejected because it falsely implies this attempt created the resource.
- JSON numeric amounts: rejected because generated clients may use binary floating point or lose
  required lexical scale.
- One `400` status for every failure: rejected because it obscures stable recovery semantics.

**Evidence**:

- [RFC 9110 HTTP semantics](https://www.rfc-editor.org/rfc/rfc9110.html)
- [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457.html)
- [Quarkus REST exception mapping](https://quarkus.io/guides/rest)
- [SmallRye OpenAPI](https://quarkus.io/guides/openapi-swaggerui)

## 11. OIDC and Authorization

**Decision**: Run Quarkus OIDC in bearer service mode. Require the exact issuer, explicit service
audience, `sub`, approved `azp`, and Keycloak client role `billing_operator`; deny unannotated
application endpoints. The application still authorizes the subject against the company capability
and never trusts path/body ownership identifiers alone. Direct token propagation is allowed only
when the token also contains the configured company-capability audience.

**Rationale**: This covers signature, issuer, audience, expiration, authorized party, role, and
company ownership with deny-by-default behavior. A service-client role is narrower than a
realm-wide billing role.

**Alternatives considered**:

- Trusting gateway authentication or token company claims: prohibited.
- Keycloak Authorization Services policy enforcement: deferred because one role plus authoritative
  company access does not justify the extra policy model.

**Evidence**:

- [Quarkus OIDC bearer-token guide](https://quarkus.io/guides/security-oidc-bearer-token-authentication)
- [Quarkus authorization reference](https://quarkus.io/guides/security-authorize-web-endpoints-reference)

## 12. Observability and Health

**Decision**: Keep caller correlation separate from W3C trace context. Accept a bounded
`X-Correlation-ID` or generate a UUID, always return it, and propagate it through reactive MDC and
the company call. Structured logs, metrics, and traces record only operation, bounded outcome/error
category, latency, dependency, and replay result; they never include tokens, buyer data, request
bodies, idempotency keys, or fiscal payloads.

Liveness checks process health only. Readiness checks PostgreSQL, the configured company endpoint,
and OIDC readiness with read-only bounded checks using the same destinations as business adapters.
Dependency failure lowers readiness but not liveness.

**Rationale**: Separate liveness prevents restart loops; readiness prevents traffic admission when
required validation or persistence cannot succeed. Bounded labels prevent metric-cardinality and
sensitive-data leaks.

**Alternatives considered**:

- Dependencies in liveness: rejected.
- Trace ID as the only caller correlation value: rejected because tracing can be sampled.
- Request-body logging: prohibited.

**Evidence**:

- [Quarkus logging and reactive MDC](https://quarkus.io/guides/logging)
- [Quarkus OpenTelemetry](https://quarkus.io/guides/opentelemetry)
- [Quarkus Micrometer](https://quarkus.io/guides/telemetry-micrometer)
- [SmallRye Health](https://quarkus.io/guides/smallrye-health)

## 13. Test Evidence

**Decision**: Use pure JUnit tests for domain arithmetic and validation; Quarkus use-case/API tests;
real PostgreSQL 18 Dev Services tests with `UniAsserter`; empty-database Flyway tests; real Keycloak
Dev Services token tests; company adapter contract/timeout tests; concurrent committed idempotency
tests; safe-observability tests; and packaged JVM integration tests.

Do not wrap concurrency tests in one rollback transaction. Native compatibility is not claimed
until native build and runtime smoke tests exercise create, replay, conflict, OIDC, persistence, and
health.

**Rationale**: Mocks cannot prove PostgreSQL uniqueness/transactions or JWT verification. Packaged
runtime tests prove behavior outside the in-process test harness.

**Alternatives considered**:

- H2 or mock-only persistence: rejected.
- `@TestSecurity` as token-validation evidence: rejected; it remains useful only for focused use-case
  tests.
- Native build without runtime tests: rejected.

**Evidence**:

- [Hibernate Reactive testing](https://quarkus.io/guides/hibernate-reactive#testing)
- [Database Dev Services](https://quarkus.io/guides/databases-dev-services)
- [Keycloak Dev Services](https://quarkus.io/guides/security-openid-connect-dev-services)
- [Quarkus integration testing](https://quarkus.io/guides/getting-started-testing)

## 14. Native Compatibility Status

**Decision**: Record native compatibility as a candidate, not a claim. JVM execution is mandatory.
SOAP, XML, signature, certificate, and PKCS#12 paths are not applicable to this feature. A future
claim requires an actual Java 25/Mandrel build and runtime smoke covering JSON DTOs, PostgreSQL,
Flyway resources, OIDC/JWK handling, company REST calls, health, replay, and conflict behavior.

**Rationale**: The selected extensions are designed for native execution, but the constitution
requires project evidence rather than framework inference. JVM deployment remains acceptable if
native evidence fails or requires unjustified configuration.

**Alternatives considered**:

- Claim native support during planning: rejected as unevidenced.
- Declare native permanently unsupported: rejected as premature for this feature.

**Evidence**:

- [Building a native executable](https://quarkus.io/guides/building-native-image)
- [Native application tips](https://quarkus.io/guides/writing-native-applications-tips)
