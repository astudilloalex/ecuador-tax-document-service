# Implementation Plan: Create Invoice Draft

**Branch**: `6-ft-1` | **Date**: 2026-07-12 | **Spec**:
[`spec.md`](spec.md)

**Input**: Approved feature specification at `specs/001-create-invoice-draft/spec.md`

## Summary

Implement one synchronous, authenticated target API that validates and atomically persists a
complete USD invoice draft for the caller's authorized Company. The application resolves the
Company, its single Issuer, and the selected emission point through an authoritative non-blocking
company adapter; validates versioned local SRI identification, IVA, and payment catalogs; performs
all deterministic `BigDecimal` calculations in the pure domain; and commits the draft graph and
company-scoped idempotency binding in one reactive PostgreSQL transaction. It performs no fiscal
numbering, XML, certificate, SRI, PDF, messaging, or notification work.

## Technical Context

**Language/Version**: Java 25; Gradle 9.5.1 wrapper

**Framework**: Quarkus Platform `3.33.2.1` LTS. This is the production-recommended LTS through
2027-03-25 and includes the applicable June 2026 security correction. The scaffold's non-LTS
`3.37.2` properties must be aligned during implementation.

**Reactive Model**: Mutiny `Uni` for the API-to-application flow, company REST adapter, catalog
lookups, and persistence. Domain calculation remains synchronous, deterministic, and framework-free.

**Persistence**: Hibernate Reactive with Panache repository-style infrastructure entities;
`@WithTransaction` at the infrastructure persistence adapter; one narrowly scoped native
PostgreSQL statement for idempotency arbitration.

**Database**: PostgreSQL 18, pinned to the current 18.x security release (`18.4` at planning time);
UTF-8; database/session diagnostics in UTC.

**Schema Migration**: Flyway only, using a JDBC PostgreSQL driver solely at migration/startup time.
Hibernate schema mutation is disabled and non-mutating mapping validation is enabled.

**Authentication**: Keycloak-issued bearer JWTs through Quarkus OIDC. Require exact issuer,
explicit service audience, `sub`, approved `azp`, and Keycloak client role `billing_operator`.
The company capability remains authoritative for current subject access. The propagated token
must also carry the configured company-capability audience and is sent only to its allow-listed
TLS base URL.

**Testing**: JUnit 6 through the Quarkus platform; pure domain tests; application use-case tests;
REST Assured/OpenAPI contract tests; real PostgreSQL 18 Dev Services reactive tests with
`UniAsserter`; empty-database Flyway tests; Keycloak Dev Services token tests; company-adapter
contract/failure tests; concurrency/idempotency tests; observability leak tests; packaged JVM
integration tests; native build/runtime smoke only before native support is claimed.

**Target Execution**: JVM execution in a Linux OCI container is mandatory. The JVM artifact must
pass packaged integration tests with PostgreSQL, Keycloak, and a company-capability test endpoint.

**Native Compatibility**: Candidate, not claimed. Build and runtime evidence is planned; JVM
deployment remains accepted if native evidence fails or requires unjustified complexity.

**External Integrations**: Keycloak OIDC and one authoritative company-context REST adapter are in
scope. SRI SOAP/XML, certificates, storage, rendering, email, webhooks, queues, and notifications
are explicitly out of scope. Identification, tax, and payment catalogs are local versioned
PostgreSQL reference data, not new remote services.

**Performance Goals**: With healthy dependencies, p95 create/replay latency at one instance is at
most 750 ms for drafts up to 50 lines and 2 seconds for the approved 500-line maximum; sustain 25
create/replay requests per second and 50 concurrent requests without event-loop blocking. These are
initial validation targets, not external SLAs, and must be measured on JVM before release.

**Constraints**: 2 MiB maximum request body; 1-second company connect timeout; 3-second company
response timeout; 5-second total outbound validation budget; no internal company-call retry;
5-second local persistence budget; 500 lines, 10 payments, and 15 additional-information entries;
idempotency binding retained for the draft lifetime; no request-thread blocking or synchronous wait.

**Scale/Scope**: One Company and its single Issuer per draft; one active emission point; one
company-scoped idempotency key; at most 500 ordered lines, 10 unique payment methods, 15
order-insensitive additional entries, and four supported IVA treatments. Invoice-line order is
business-significant; payment and additional-information order is not.

**Sensitive Data**: Bearer tokens, buyer identification/contact information, fiscal inputs,
idempotency keys, normalized idempotency content, and persisted drafts are sensitive. Production
uses TLS to PostgreSQL/company/Keycloak, encrypted database volumes and backups, a least-privilege
database role, no request-body or value logging, no sensitive metric/trace labels, and sanitized
errors. Draft data and its binding remain until a later approved lifecycle feature deletes the
draft; this feature performs no automatic deletion or time-based purge.

## Source and Terminology Evidence

| Authority | Applicable source and version/path | Requirements or decisions governed |
|-----------|------------------------------------|-------------------------------------|
| Ecuadorian legislation and official SRI documentation | SRI Regulation for Sales, Withholding, and Complementary Documents, consolidated 2023-12-29; Electronic Tax Documents Offline Scheme Technical Sheet v2.32, updated 2025-10-08; invoice XSD versions through 2.1.0 | Buyer identifiers, IVA catalog/effective-date rules, field bounds, exact decimals, final-consumer identity and threshold, and the absence of fiscal side effects |
| Constitution | `.specify/memory/constitution.md` v1.0.0 | Technology baseline, clean architecture, reactive safety, fiscal rules, tenant isolation, Flyway, tests, operations, simplicity, and runtime evidence |
| Approved specification and clarifications | `specs/001-create-invoice-draft/spec.md`, clarification session 2026-07-12 | Complete functional behavior, monetary pipeline, ownership, zero totals, idempotency, limits, tax scope, ordering, and errors |
| Architecture decisions | None; decisions are recorded in this plan and `research.md` | Quarkus/PostgreSQL versions, ports, persistence, API, security, operations, and evidence |
| Legacy evidence | `docs/legacy/as-is/04-data-model.md`, `05-business-rules.md`, `06-validation-rules.md`, `07-process-flows.md`, `10-security-access-control.md`, `14-pending-functional-validation.md` | Historical scenario discovery only; no route, schema, algorithm, or behavior is inherited |

**Source Conflicts and Resolutions**:

- Legacy client-supplied tax bases, rates, values, and totals are rejected; the approved target
  specification makes the system authoritative for calculations.
- Legacy RUC-based selection with incomplete tenant enforcement is replaced by authenticated
  Company scope, the Company's single Issuer, and concealed cross-scope denial.
- Legacy issuance side effects are excluded; `DRAFT` is internal and has no SRI status or fiscal
  identity.
- The existing scaffold uses Quarkus `3.37.2`; the researched production baseline `3.33.2.1` LTS
  governs implementation because the plan is the approved version-selection authority.

**Pending Functional Validation**: None.

**Terminology Mapping Impact**: None. `Company`, `Issuer`, `Invoice Draft`, `Idempotency Key`, and
the other applicable terms are already approved in `docs/migration/terminology-mapping.md`.

## Constitution Check

*GATE: Re-evaluated before research and after Phase 1 design. No deviations are approved or
required.*

| Gate | Pre-Research evidence | Post-Design evidence |
|------|-----------------------|----------------------|
| Greenfield scope: one bounded outcome; no implicit legacy compatibility | PASS — spec defines only create-and-review draft and explicitly excludes legacy compatibility | PASS — one create API and one aggregate; no legacy route/schema or compatibility layer |
| Authority: official sources are versioned; conflicts and Pending Functional Validation are recorded | PASS — spec cites SRI v2.32 and records resolved conflicts; no PFV remains | PASS — `research.md`, model, and contract preserve the authority order and list no unresolved PFV |
| Language: target names are English and terminology mapping decisions are respected | PASS — approved English mapping exists | PASS — packages, schemas, error codes, tables, and documents are English; exact SRI codes remain data |
| Baseline: required technologies are fixed; Quarkus version research is identified pre-research and resolved with justification post-design | PASS — Java 25, Quarkus, Mutiny, PostgreSQL, reactive Panache, Flyway, and OIDC fixed; exact Quarkus version was a research task | PASS — Quarkus `3.33.2.1` LTS and PostgreSQL 18.4 selected with official evidence |
| Architecture: `api`, `application`, `domain`, and `infrastructure` dependencies and mappings comply | PASS — constitution directions are applicable | PASS — project structure and explicit DTO/command/domain/entity mappings follow allowed direction |
| Domain purity: no framework, transport, persistence, JSON, OIDC, or Mutiny types in `domain` | PASS — required by constitution | PASS — domain contains Java/`BigDecimal`/time types only; JSONB and Panache remain infrastructure |
| Reactive safety: every blocking or CPU-intensive operation is isolated, bounded, timed out, and testable | PASS — research identified Flyway and all I/O boundaries | PASS — request path is non-blocking; Flyway is startup-only; bounded calculation has performance/concurrency evidence |
| Fiscal correctness: official rules, `BigDecimal` policies, time semantics, and invalid-data behavior are explicit | PASS — spec contains deterministic rules and vectors | PASS — exact numeric/date mappings, reference catalogs, constraints, and test paths are defined |
| Security: deny-by-default authorization, token validation, ownership enforcement, and cross-tenant tests are defined | PASS — spec requires all | PASS — OIDC claims, role, company port, concealment, and cross-scope contract/tests are explicit |
| Sensitive data: storage, encryption, redaction, retention, and certificate lifecycle are defined before implementation | PASS — certificates are excluded; sensitive values identified | PASS — TLS/encrypted storage/backups, least privilege, redaction, draft-lifetime retention, and no certificate material are explicit |
| Persistence: Flyway-only evolution, immutable migrations, database invariants, and empty-database tests are defined | PASS — constitution gate identified | PASS — relational constraints, Flyway workflow, and PostgreSQL 18 empty-database evidence are specified |
| Boundary consistency: states, retries, idempotency, duplicates, timeouts, recovery, reconciliation, and terminal outcomes are defined | PASS — spec resolves idempotency and all-or-nothing behavior | PASS — company and database boundaries define states, no internal retry, unique arbitration, recovery, and HTTP outcomes |
| API and async quality: DTO separation, validation ownership, stable errors, correlation, and result observation are defined | PASS — synchronous result and stable errors required | PASS — OpenAPI/RFC 9457 contract, mapping ownership, `201`/replay `200`, and correlation behavior are explicit |
| External adapters: ports, endpoint configuration, sanitized observability, resilience, and contract evidence are defined | PASS — only company/Keycloak are applicable | PASS — company port contract, same-destination readiness, timeouts, no retry, TLS/token propagation, and tests are defined |
| Testing: acceptance scenarios and applicable risk-based tests are identified before production tasks | PASS — 40 scenarios and 21 success criteria exist | PASS — evidence table maps domain, use case, PostgreSQL, API, security, operations, JVM, and native candidate tests |
| Operations: meaningful liveness/readiness, structured logs, auditing, and destination-consistent health checks are defined | PASS — constitution requirements identified | PASS — separate health semantics, bounded labels, sanitized audit/log/trace rules, and same destinations are defined |
| Simplicity: every dependency, abstraction, process, store, and distributed interaction is justified | PASS — no speculative infrastructure allowed | PASS — one service, one database, one required company call; no broker/cache/background process/generic repository |
| Runtime evidence: JVM verification is mandatory and native status has an evidence path | PASS — JVM required; native research identified | PASS — packaged JVM test is mandatory; native remains candidate pending actual build/runtime evidence |

## Reactive and Resource Boundary Design

| Operation | Infrastructure adapter and application port | Blocking/CPU classification | Execution context | Timeout and resource bound | Required concurrency/failure evidence |
|-----------|---------------------------------------------|-----------------------------|-------------------|----------------------------|---------------------------------------|
| Company authorization scope and creation-context evaluation | `CompanyContextRestAdapter` → `CompanyContextPort` | Non-blocking network I/O | Vert.x event loop via Quarkus REST Client/Mutiny | Connect 1 s; response 3 s; total 5 s; response size 64 KiB; no internal retry | Timeout/unavailable/concealed denial; eligible/ineligible context; mutable-context replay; 50 concurrent requests; no binding on failure |
| Catalog resolution and draft persistence | `ReferenceCatalogPersistenceAdapter` and `InvoiceDraftPersistenceAdapter` | Non-blocking PostgreSQL I/O | Vert.x context/Hibernate Reactive | Local transaction budget 5 s; reactive pool measured under 50 concurrent requests | Empty DB, rollback, pool pressure, cancellation, timeout, and no partial graph |
| Monetary calculation and normalization, maximum 500 lines | Plain domain services invoked by application | Bounded synchronous CPU; no blocking I/O; not classified CPU-intensive under approved bounds | Event loop only after benchmark proves p99 domain work at maximum payload <= 25 ms | 500 lines, 10 payments, 15 additional entries, 2 MiB request | Deterministic vectors and 50-concurrent max-payload test; revise execution design before release if budget fails |
| JSON request/response mapping | API DTO mapper; normalized JSONB mapping in persistence adapter | Bounded serialization CPU; no blocking I/O | Quarkus REST event loop | 2 MiB inbound; 4 MiB response safety bound | Maximum payload test, malformed input, memory/latency evidence |
| Flyway migration | Flyway infrastructure at startup/deployment; no application port | Blocking JDBC | Startup/deployment thread, never event loop or request | Migration lock/startup budget 60 s; one migration owner in production | Empty database, repeat startup, checksum/immutable migration failure |

Reactive wrappers are not evidence that an underlying operation is non-blocking. Any future
blocking company client or file/crypto/XML work requires a new plan review and bounded worker
adapter.

## Security and Ownership Design

**Protected and Public Operations**:

- `POST /api/v1/companies/{companyId}/invoice-drafts` is protected and requires
  `billing_operator`.
- No business endpoint is public. Unannotated application endpoints are denied.
- Liveness and readiness are read-only platform endpoints exposed only on the deployment management
  interface/network. Swagger UI and Dev UI are disabled outside development/test.

**Token Validation**: Verify JWK signature, exact issuer, explicit audience, expiration, required
`sub`, allowed `azp`, and the Keycloak client role `billing_operator`. Reject unsigned, malformed,
expired, wrong-issuer, wrong-audience, or wrong-authorized-party tokens with `401`; reject a valid
token lacking the role with `403`. Direct propagation to the company capability is permitted only
for a token containing that capability's configured audience; otherwise the adapter fails closed.

**Effective Authorization Scope**: The API path Company identifier is untrusted input. The use case
sends the authenticated subject and requested Company/Issuer/emission-point identifiers through
`CompanyContextPort`. The result separates current authorized Company/tenant scope from an
eligible or ineligible current Issuer/emission-point creation context. Every draft and idempotency
lookup uses the authoritative authorization scope. A bound request compares normalized content and
returns the immutable original or a conflict without revalidating mutable creation eligibility; an
unbound request requires the eligible single-Issuer/active-emission-point snapshot. Foreign or
inaccessible identifiers map to the same safe `404`, and revoked current Company access prevents
replay.

**Cross-Tenant Evidence**: API and application tests cover Tenant A/Company A attempting Tenant B,
same key across companies, wrong Issuer for Company, wrong emission point, and current access
revoked before replay. Assertions require no foreign fields, no existence signal, and no mutation.

**Sensitive Data and Certificate Lifecycle**: Tokens are memory-only and propagated only to the
TLS company endpoint; no token is stored. Drafts, buyer data, normalized content, and keys use
encrypted database volumes/backups and TLS; access is limited to the service database role. Logs,
errors, metrics, and traces omit all sensitive values. Data remains for the draft lifetime; deletion
requires a later approved lifecycle feature. Certificates/PKCS#12/signing keys are not loaded,
stored, or required by this feature.

## Data and External Consistency Design

| Boundary or logical command | Intermediate states | Retry and idempotency | Duplicate handling | Timeout | Failure recovery and reconciliation | Observable terminal outcomes |
|-----------------------------|---------------------|-----------------------|--------------------|---------|-------------------------------------|------------------------------|
| Company authorization/context resolution before persistence | No durable local state; in-memory `RESOLVING_CONTEXT`, then authorized scope plus eligible/ineligible creation context or failure | No internal retry; caller retries whole command with same key | Authorized scope permits a local binding replay check; no new row exists before success | 1 s connect, 3 s response, 5 s total | Bound equivalent content returns original despite mutable context; unbound ineligible context is concealed `404`; unavailable `503`; timeout `504` | Authorized replay/conflict, eligible new-command path, or safe terminal failure |
| Local create/replay command | In-memory validated command; transaction claim; committed `DRAFT` plus binding, or rollback | Scoped key retained with draft; equivalent content returns existing draft | PostgreSQL unique constraint arbitrates concurrent claims; different content is `409` | 5 s persistence budget | Transaction rollback removes claim and children; caller retries; a lost response replays existing committed draft | `201` new draft, `200` replay, `409` conflict, `422` validation, safe `500` rollback failure |

No SRI, filesystem, object storage, queue, webhook, rendering, certificate, or notification boundary
exists in this feature.

## Native Compatibility Evaluation

| Risk area | Applicable? | Build evidence | Runtime evidence | Decision and consequences |
|-----------|-------------|----------------|------------------|---------------------------|
| SOAP clients | No — no SRI call | Not applicable by approved scope | Assert zero SOAP adapter invocation | Excluded; later SRI feature must reopen evaluation |
| XML generation and schema validation | No | Not applicable by approved scope | Assert no XML artifact | Excluded |
| XML digital signatures | No | Not applicable by approved scope | Assert no signature operation | Excluded |
| PKCS#12 certificate handling | No | Not applicable by approved scope | Assert no certificate read | Excluded |
| Cryptographic providers | No feature-specific crypto | Quarkus/JDK native build planned | OIDC verification smoke through Keycloak | OIDC platform behavior tested; no custom provider |
| Reflection and resource loading | Yes — Jackson DTOs, Panache mappings, OpenAPI, Flyway resources | Planned: `./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true` | Planned native HTTP smoke for create/replay/conflict/OIDC/PostgreSQL/health | Candidate only; JVM remains supported if evidence fails |

## Project Structure

### Documentation (this feature)

```text
specs/001-create-invoice-draft/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── invoice-draft-api.openapi.yaml
│   └── company-context-port.md
└── tasks.md
```

### Source Code (repository root)

```text
src/main/java/com/alexastudillo/taxdocument/
├── api/invoicedraft/
│   ├── dto/
│   ├── error/
│   ├── InvoiceDraftResource.java
│   └── InvoiceDraftApiMapper.java
├── application/invoicedraft/
│   ├── command/
│   ├── port/in/
│   ├── port/out/
│   ├── result/
│   └── CreateInvoiceDraftUseCase.java
├── domain/invoicedraft/
│   ├── model/
│   ├── validation/
│   └── calculation/
└── infrastructure/invoicedraft/
    ├── company/
    ├── configuration/
    ├── observability/
    └── persistence/
        ├── entity/
        ├── mapper/
        └── repository/

src/main/resources/
├── application.properties
├── META-INF/openapi.yaml
└── db/migration/

src/test/java/com/alexastudillo/taxdocument/
├── api/invoicedraft/
├── application/invoicedraft/
├── domain/invoicedraft/
├── infrastructure/invoicedraft/
└── runtime/
```

**Structure Decision**: Group within each constitutional layer by the `invoicedraft` capability.
API DTOs map explicitly to application commands/results; application commands map to pure domain
objects; persistence mappers map domain/application data to Panache entities. Application ports are
introduced only for the inbound create use case, company context, reference catalogs, clock, and
aggregate persistence. Quarkus CDI producers and composition live under infrastructure
configuration; the domain has no framework annotations or reactive types.

## Test and Operational Evidence Plan

| Requirement/risk | Test level and environment | Planned path | Observable behavior or invariant | Failure/boundary cases |
|------------------|----------------------------|--------------|----------------------------------|------------------------|
| DR-002 through DR-010 monetary rules | Pure domain/JUnit | `src/test/java/com/alexastudillo/taxdocument/domain/invoicedraft/calculation/InvoiceCalculatorTest.java` | Exact approved vectors, line/group/invoice rounding, USD totals | Precision limits, half-cent boundaries, discounts equal/exceed gross, zero totals, overflow |
| FR-007/FR-028 buyer identification | Pure domain + catalog fixtures | `.../domain/invoicedraft/validation/BuyerIdentificationValidatorTest.java` | Official effective rule selected by emission date | Identity card checksum, RUC class without invented checksum, passport/foreign format, final-consumer threshold |
| FR-009 through FR-015 aggregate validation | Domain/use case | `.../domain/invoicedraft/validation/InvoiceDraftValidatorTest.java` | Counts, unique payments/additional names, selected IVA rule, payment reconciliation | Empty/501 lines, 11 payments, duplicate method, 16 additional, unsupported taxes, supplied tax rate |
| FR-001 through FR-005/FR-024 authorization | Application/API with company stub | `.../application/invoicedraft/CreateInvoiceDraftAuthorizationTest.java`; `.../api/invoicedraft/InvoiceDraftTenantIsolationTest.java` | Effective Company/tenant/Issuer/emission scope enforced and concealed | Foreign tenant/company, wrong Issuer/emission point, inactive context, revoked replay access |
| Company port contract/timeouts | Adapter contract test with HTTP stub | `.../infrastructure/invoicedraft/company/CompanyContextRestAdapterTest.java` | Token/correlation propagation, authorization-scope mapping, and eligible/ineligible creation evaluation | 404, malformed response, 503, connect/read timeout, no retry, mutable-context replay |
| FR-020/FR-021 atomic graph | PostgreSQL 18 reactive integration | `.../infrastructure/invoicedraft/persistence/InvoiceDraftPersistenceAdapterTest.java` | Complete graph and binding commit or complete rollback | Child constraint failure, cancellation, transaction timeout |
| FR-027 through FR-033 idempotency | PostgreSQL committed-concurrency integration | `.../infrastructure/invoicedraft/persistence/InvoiceDraftIdempotencyConcurrencyTest.java` | One winner, exact replay, conflict, scope independence, lifetime binding | Concurrent equivalent/different content, nullable tenant, response loss, reordered collections, mutable Issuer/emission-point data after commit |
| Flyway authority | Empty PostgreSQL 18 database | `.../infrastructure/invoicedraft/persistence/FlywayMigrationTest.java` | Migrations create schema and seed versioned catalogs; Hibernate validates | Empty DB, repeat startup, checksum mismatch, no auto-generation |
| API contract/FR-022 | `@QuarkusTest` + REST Assured | `.../api/invoicedraft/InvoiceDraftContractTest.java` | OpenAPI request/response, `201`, replay `200`, full result, exact decimal strings | Calculated fields `400`, business `422`, conflict `409`, safe `500` |
| OIDC claims and role | Real Keycloak Dev Services | `.../api/invoicedraft/InvoiceDraftOidcTest.java` | Signature, issuer, audience, expiration, azp, subject, client role | Missing/expired/wrong claims `401`; missing role `403` |
| Correlation and safe errors | API/integration | `.../api/invoicedraft/InvoiceDraftProblemDetailsTest.java` | RFC 9457 code, status, safe fields, returned correlation | Invalid supplied correlation, no rejected values/stack/SQL/path/token |
| Sensitive observability | Integration/log/metric/trace capture | `.../infrastructure/invoicedraft/observability/SensitiveDataObservabilityTest.java` | Structured bounded signals contain no buyer/key/token/payload | Validation, auth denial, dependency failure, conflict |
| Liveness/readiness | Quarkus integration | `.../runtime/HealthBehaviorIT.java` | Liveness independent; readiness uses real configured PostgreSQL/company/OIDC destinations | Each dependency down/slow; no destructive check |
| Performance/reactive safety | JVM load/concurrency evidence | `.../runtime/InvoiceDraftPerformanceIT.java` | p95 targets, <=25 ms p99 bounded calculation, no blocked event loop | 500 lines, 50 concurrent, pool pressure, 2 MiB rejection |
| JVM runtime | `@QuarkusIntegrationTest` packaged JVM | `.../runtime/InvoiceDraftJvmIT.java` | Create/replay/conflict/auth/PostgreSQL/health work in packaged process | Startup migration and dependency recovery |
| Native candidate | Native build + `@QuarkusIntegrationTest` | `.../runtime/InvoiceDraftNativeIT.java` | Same smoke paths when and only when native support is claimed | Reflection/resource/JWK/Flyway failures lead to documented deferment |

**Liveness and Readiness**: `/q/health/live` reports process/internal health only. Readiness
requires reactive PostgreSQL, the same company base URL used by the adapter, and OIDC readiness.
All checks are read-only, bounded, sanitized, and placed on the management interface in deployment.

**Structured Observability**: Return and propagate a validated or generated correlation UUID;
continue W3C trace context separately. Structured logs and traces record operation, outcome,
dependency, latency, and replay result without payload values. Metrics use only bounded labels;
tenant, company, draft, buyer, idempotency, and correlation identifiers are prohibited labels.

**Audit Events**: Record sanitized `INVOICE_DRAFT_CREATED`, `INVOICE_DRAFT_REPLAYED`,
`INVOICE_DRAFT_IDEMPOTENCY_CONFLICT`, and authorization-denied events with instant, operation,
correlation, outcome, and non-sensitive internal audit references. No certificate event applies.

**External Destination Consistency**: The company adapter and readiness check consume one typed
configuration object/base URL. OIDC validation and OIDC readiness use the same configured issuer.
Database readiness and reactive persistence use the same named datasource. Tests mutate each one
and assert readiness follows the business destination.

## Complexity Tracking

| Addition | Requirement creating the need | Simpler alternatives considered | Why insufficient | Testing and operational consequences |
|----------|-------------------------------|--------------------------------|------------------|--------------------------------------|
| Reactive company REST client and token propagation | FR-002 through FR-005 and approved company source-of-truth clarification | Trust request/token claim; copy company tables | Neither proves current Company access or authoritative single Issuer/emission point | Contract, timeout, TLS/token, readiness, and failure tests; one required synchronous dependency |
| Local versioned reference-catalog tables | DR-001, FR-007, FR-011, FR-013 | Hard-coded enums/rates; remote catalog service | Hard coding is prohibited; a remote service is unjustified | Flyway seed/version migrations, effective-date constraints, catalog integration tests |
| Dedicated idempotency table plus normalized JSONB | FR-027 through FR-033 | Read-before-write; application mutex; digest only | Races, adds coordination, or loses exact equivalence | PostgreSQL-specific uniqueness/concurrency tests and sensitive JSONB controls |
| JDBC PostgreSQL driver alongside reactive driver | Constitution Flyway-only rule | Hibernate generation; manual migration | Both alternatives violate governance; Flyway is JDBC-based | Startup-only blocking boundary, migration job, empty-database tests |
| Stable health/metrics/tracing extensions | Constitution operations and security rules | Logs alone | Cannot prove readiness, bounded metrics, or trace propagation | Management endpoints, telemetry redaction tests, operational configuration |

No background process, additional microservice, broker, cache, CQRS model, saga, plugin system,
generic repository hierarchy, or legacy compatibility layer is introduced.

| Deviated principle and rule | Scope | Justification | Approval record | Expiration or remediation condition |
|-----------------------------|-------|---------------|-----------------|-------------------------------------|
| None | None | No constitution deviation is required | Not applicable | Not applicable |
