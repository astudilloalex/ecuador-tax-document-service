# Implementation Plan: Create Invoice Draft

**Branch**: `6-ft-1` | **Date**: 2026-07-12 | **Spec**: `specs/001-create-invoice-draft/spec.md`

**Input**: Clarified feature specification with Constitution v2.0.0 Company-context decisions

## Summary

Deliver one synchronous internal `POST /invoice-drafts` operation under `/api/v1`. The API reads
exactly one `X-Company-Id`, canonicalizes it, maps it to an application `CompanyId`, validates local
commercial/fiscal inputs, calculates deterministic USD amounts, and atomically persists and returns
one complete `DRAFT` aggregate. CompanyId plus a hashed idempotency key defines the durable
concurrency scope. The design has no authentication, authorization, Company Service, Company
master data, fiscal snapshot, cache, or SRI side effect.

## Technical Context

**Language/Version**: Java 25

**Framework**: Quarkus 3.33.2.1 LTS; selected from the current production-recommended LTS line and
justified in `research.md`. Implementation setup MUST align both `quarkusPluginVersion` and
`quarkusPlatformVersion` in `gradle.properties` from the current `3.37.2` to `3.33.2.1` before any
feature dependency or source work; the plugin and platform versions MUST remain identical.

**Reactive Model**: Mutiny for HTTP/application orchestration and every I/O port; synchronous pure
domain calculation

**Persistence**: Hibernate Reactive with Panache; Panache models remain infrastructure-only

**Database**: PostgreSQL 18.4; real PostgreSQL is required for persistence/concurrency evidence

**Schema Migration**: Flyway only, including versioned local reference data

**Caller Security**: None. The repository contains no authentication, authorization, identity,
token, role, permission, tenant, or application-level service security behavior.

**Testing**: JUnit 5, REST Assured, `UniAsserter`, real PostgreSQL integration/concurrency tests,
empty-database Flyway tests, static and served `/q/openapi` contract tests, ordered HTTP-gate tests,
architecture tests, pure domain vectors, packaged JVM tests, and optional native build/runtime smoke

**Approved Build Dependencies**: Quarkus-BOM-managed
`quarkus-rest-jackson`, `quarkus-hibernate-validator`,
`quarkus-hibernate-reactive-panache`, `quarkus-reactive-pg-client`, `quarkus-flyway`,
`quarkus-jdbc-postgresql`, `quarkus-smallrye-openapi`, `quarkus-smallrye-health`,
`quarkus-micrometer`, and `quarkus-opentelemetry`; test dependencies are BOM-managed
`quarkus-junit`, `quarkus-test-vertx`, `quarkus-test-hibernate-reactive-panache`, and
`io.rest-assured:rest-assured`. Exact rationale and rejected alternatives are recorded in
`research.md`.

**PostgreSQL Test Harness**: Quarkus Database Dev Services from the approved PostgreSQL extensions,
with test configuration pinned to `docker.io/library/postgres:18.4`. No direct Testcontainers
dependency or competing container lifecycle is introduced; shared helpers consume the Quarkus test
resource/Dev Services context.

**Build Quality**: Spotless Gradle plugin `com.diffplug.spotless` `8.8.0` with
`google-java-format` `1.35.0` is the only formatter. Static analysis uses JDK 25
`javac -Xlint:all -Werror` plus the focused architecture tests; no Checkstyle, PMD, Error Prone, or
second formatter is planned.

**Target Execution**: JVM is mandatory

**Native Compatibility**: Optional candidate; it may be claimed only with build and runtime
evidence. JVM remains acceptable when native evidence fails or complexity is unjustified.

**External Integrations**: None. Create Invoice Draft calls neither Company Service nor SRI and
uses no certificate, storage, queue, rendering, notification, or identity adapter.

**Company Context Boundary**: Exactly one `X-Company-Id` request header is trimmed, parsed as a
non-nil UUID, normalized to lowercase hyphenated form, explicitly mapped to application
`CompanyId`, stored immutably on the aggregate, returned in the response, and used for persistence
and idempotency partitioning. It is not a credential or entitlement proof.

**Performance Goals**: Typical create p95 ≤750 ms/p99 ≤1.5 s; maximum supported create p95 ≤3
s/p99 ≤5 s; replay/conflict p95 ≤250 ms/p99 ≤500 ms; 50 equivalent concurrent requests complete
within 10 s and create exactly one aggregate/binding

**Constraints**: 2,097,152-byte request-body maximum; 500 lines; 8 payments; 15 additional entries;
10-second overall request deadline; 5-second local write-transaction timeout; quantity and unit
price `numeric(12,6)` with maximum `999999.999999`; all money `numeric(17,2)` with maximum
`999999999999999.99`; percentage rates `numeric(5,2)` from `0.00` through `100.00`; exact
`BigDecimal` calculation and approved line-level `HALF_UP` rounding; every overflow rejected as
`BUSINESS_VALIDATION_FAILED`/`MONETARY_RANGE_EXCEEDED` before persistence; no synchronous wait or
unbounded retry

**Time Boundary**: The API boundary captures one `requestCreationInstant` per new request and derives
the expected emission date once in `America/Guayaquil`. The derived date remains fixed when commit
crosses midnight. `createdAt` is the confirmed commit instant, and an equivalent replay returns the
original date without current-date revalidation.

**Scale/Scope**: One Company partition and one Invoice Draft per logical command; no draft update,
delete, fiscal issuance, other tax-document type, or Company administration

**Sensitive Data**: Buyer identity/contact and fiscal payload content are sensitive. Logs,
errors, metrics, traces, fixtures, and idempotency storage exclude personal values, raw keys, and
complete normalized requests. Fingerprints are SHA-256 values with normalization version metadata.

## Source and Terminology Evidence

| Authority | Applicable source and version/path | Requirements or decisions governed |
|-----------|------------------------------------|-------------------------------------|
| Ecuadorian legislation/SRI | SRI Electronic Tax Documents Offline Scheme Technical Sheet v2.32 and SRI electronic-invoicing resources referenced by the spec | Buyer types, IVA treatments, final-consumer threshold, fiscal vocabulary |
| Constitution | `.specify/memory/constitution.md` v2.0.0 | Company header, no identity/Company dependency, architecture, persistence, testing, operations |
| Specification | `specs/001-create-invoice-draft/spec.md`, clarification session 2026-07-12 | Actor, inputs, calculations, validation, failure precedence, acceptance |
| Reference baseline | `specs/001-create-invoice-draft/reference-data-baseline.md` | Approved buyer-type, IVA-rule, and payment-method rows; official evidence; target decisions; deterministic UUIDv5 mappings |
| Architecture decisions | This plan and supporting Phase 0/1 artifacts; no separate ADR | Feature-local technical choices |
| Technology authorities | Quarkus release/Java 25 guidance; PostgreSQL 18.4 release/versioning guidance linked in `research.md` | Runtime/database versions |
| Legacy evidence | `docs/legacy/as-is/` paths listed in the spec | Historical discovery only |

**Source Conflicts and Resolutions**: Historical and superseded feature artifacts required
Keycloak, tenant-derived Company context, a Company client/port, status/eligibility checks, and
fiscal snapshots. Constitution v2.0.0 and the clarified spec have higher authority. All such
components/outcomes are removed or explicitly prohibited. Current SRI IVA guidance identifies 13%
and 5% applicability, while Circulars NAC-DGECCGC25-00000006 and
NAC-DGECCGC26-00000005 provide official 15% applicability evidence. The approved baseline retains
13% and 15% as distinct caller-selectable rules with applicability notes; neither is labeled
universal. Product or transaction classification remains the upstream billing workflow's
responsibility. The 15% acceptance vector is mathematical only.

**Reference-Data Resolution**: `reference-data-baseline.md` approves five buyer-identification
rows, six IVA tax-rule rows, and eight payment-method rows under
`SRI-OFFLINE-2.32-TARGET-1`. Tax and payment identifiers use UUIDv5 namespace
`32576bbf-b70d-5c24-98ff-d5f9b48e8826`. RUC and Ecuadorian identity-card validation are explicitly
`FORMAT_ONLY` because no exact governing checksum algorithm was established from the approved
primary sources for this draft feature. Checksum and registry verification are outside scope, not
deferred implementation work.

The approved IVA catalog represents tax category only through the immutable `family=IVA` field on
each versioned rule. Rule activity and effectivity govern selection; this feature has no separate
parent tax-category entity, active flag, repository, or lifecycle.

**Terminology Mapping Impact**: `Company Identifier`/`CompanyId` remains the canonical opaque
ownership value from `X-Company-Id`. `Fiscal Context Snapshot` remains reserved for a later
fiscal-issuance specification. `X-Correlation-Id` is the canonical transport spelling in these
artifacts.

## Constitution Check

*GATE: Recorded before Phase 0 and re-evaluated after Phase 1. Reconciled on 2026-07-13 against
authoritative `main` and `origin/main` commit `137d1c8c59cc98402f0a1fed211a6caccad4c883`.*

| Gate | Pre-Research evidence | Post-Design evidence |
|------|-----------------------|----------------------|
| Constitution approval on main | PASS — authoritative `main` and `origin/main` contain Constitution v2.0.0 at commit `137d1c8c59cc98402f0a1fed211a6caccad4c883` | PASS — `6-ft-1` descends from that approved mainline and the specification, plan, supporting artifacts, and pre-task gate are reconciled with v2.0.0 |
| Greenfield bounded outcome | PASS — one target create/review outcome | PASS — no legacy compatibility or unrelated lifecycle |
| Authority/versioned evidence | PASS — Constitution v2.0.0 and SRI v2.32 identified | PASS — source register separates official facts from target decisions with exact locators |
| English terminology | PASS — target terms are English | PASS — all generated artifacts use canonical English with exact SRI exceptions |
| Required baseline | PASS — Java/Quarkus/Mutiny/PostgreSQL/Panache/Flyway fixed | PASS — Quarkus 3.33.2.1 LTS and PostgreSQL 18.4 justified |
| Reference-data evidence | PASS — evidence needs identified without inference | PASS — 5 buyer, 6 IVA, and 8 payment rows approved with fixed identifiers where applicable |
| Clean Architecture | PASS — four boundaries required | PASS — explicit API→application→domain/infrastructure mapping below |
| Domain purity | PASS — synchronous deterministic fiscal model | PASS — no transport/framework/persistence/security/Mutiny types in domain |
| Reactive safety | PASS — no blocking external operation in scope | PASS — reactive PostgreSQL only; bounded domain work and evidence budget |
| Fiscal correctness | PASS — approved IVA/buyer/date/decimal rules | PASS — catalog, FORMAT_ONLY buyer strategies, applicability boundary, numeric, and date rules are complete |
| Internal caller boundary | PASS — no identity/security state | PASS — OpenAPI/dependencies/config/tests contain no security behavior |
| Company boundary | PASS — mandatory header, no lookup/snapshot | PASS — CompanyId mapping/storage/scoping and negative architecture evidence explicit |
| Sensitive data | PASS — buyer/fiscal data identified | PASS — fingerprint-only binding and observability redaction defined |
| Persistence | PASS — atomic Flyway/PostgreSQL required | PASS — constraints, transaction, rollback, empty-db evidence defined |
| Boundary consistency | PASS — idempotency/failure precedence approved | PASS — fingerprint, race arbitration, timeout/recovery outcomes complete |
| API/async quality | PASS — synchronous observable result, stable errors | PASS — strict OpenAPI, Problem Details, correlation, no opaque job |
| External adapters | PASS — none applicable | PASS — no Company/SRI/security port/client/health destination |
| Testing | PASS — 58 scenarios and 33 success criteria | PASS — traceability maps every FR-001–FR-047, DR-001–DR-024, and SC-001–SC-033 |
| Operations | PASS — health/correlation required | PASS — PostgreSQL-only readiness, metrics/log/trace/performance budgets defined |
| Simplicity | PASS — no speculative platform/component | PASS — only local ports, datastore, and two justified persisted capabilities |
| Runtime evidence | PASS — JVM mandatory/native optional | PASS — packaged JVM and conditional native evidence paths defined |

No constitutional deviation or complexity exception is requested. Phase 1 design and reference
data are complete, and no material planning blocker remains.

**Final planning gate**: PASS — Eligible for pre-task checklist regeneration

## Clean Architecture Mapping

```text
HTTP request
  → api: parse/validate X-Company-Id and transport DTOs
  → application: CreateInvoiceDraftCommand(CompanyId, fixed request instant, mapped business
    inputs, idempotency/correlation)
  → domain: InvoiceDraft aggregate and deterministic calculation
  → application outbound ports
  → infrastructure: reactive Panache/PostgreSQL, local catalogs, clock/identifier adapters
```

| Boundary | Responsibility | Allowed dependencies | Prohibited inputs/types |
|----------|----------------|----------------------|-------------------------|
| `api` | Enforce FR-041 stages 1–5, initialize safe correlation for every outcome, capture one request instant after those stages pass, map transport inputs, and return Problem Details | `application` | Persistence entities returned directly; Company/security clients |
| `application` | Receive an already mapped CompanyId and fixed request instant; enforce FR-041 stages 6–12, idempotency, use-case preconditions, and transaction-port orchestration | `domain`, Mutiny, application ports | HTTP headers/requests, `SecurityIdentity`, `JsonWebToken`, thread-local/Gateway objects |
| `domain` | Own immutable CompanyId on Invoice Draft; buyer/line/tax/payment invariants and exact calculation | Java/approved domain libraries | HTTP/JSON/Quarkus/Panache/PostgreSQL/Mutiny/security types |
| `infrastructure` | Implement local repository/catalog/clock/identifier ports with reactive PostgreSQL/Panache | application ports/domain types | Company/SRI/security adapter, shared database, cache |

Actual outbound boundaries are limited to the Invoice Draft repository, local identification/tax/
payment catalog access, clock, and identifier generation. No interface named or equivalent to
`CompanyContextPort`, `ResolveCompanyFiscalContextPort`, or Company authorization/eligibility is
introduced.

**Failure-Precedence Ownership**: FR-041 stages 1–5 use one explicit pre-application pipeline:

1. Quarkus HTTP enforces the exact bare-byte setting
   `quarkus.http.limits.max-body-size=2097152` at its upload-limit route before
   REST dispatch. A CDI `Router` observer registers a Vert.x failure handler that owns only status
   `413` for `POST /api/v1/invoice-drafts`, maps it to the approved Problem Details response, and
   bootstraps a safe correlation value without evaluating Company or correlation validity. It
   delegates every other method, path, status, and failure through `RoutingContext.next()`.
2. After routing and before entity deserialization, one
   `@ServerRequestFilter(nonBlocking = true)` restricted to the create resource method by a custom
   Jakarta REST `@NameBinding` evaluates Company-header validity, correlation validity, and
   idempotency-key syntax in exactly that order. It stores only the accepted mapped values in
   request-local API state.
3. Jackson deserialization and Bean Validation perform representation validation only after that
   gate passes. `quarkus.jackson.fail-on-unknown-properties=true` enforces strict bodies, and the
   built-in Jackson mismatched-input mapper is disabled with
   `quarkus.rest.exception-mapping.disable-mapper-for=io.quarkus.resteasy.reactive.jackson.runtime.mappers.BuiltinMismatchedInputExceptionMapper`
   so the feature mapper returns the approved stable response.

The route failure handler and request gate share safe correlation initialization: absent input
generates a UUID, valid input is preserved, and invalid input is replaced without allowing a stage
3 error to overtake stage 1 or 2. The resource method is invoked only after stage 5. It then calls
the application `RequestClock` exactly once, derives the immutable request instant carried by
`CreateInvoiceDraftCommand`, and performs no later clock read for that command. Application
orchestration begins at normalized-content generation (stage 6), continues through
replay/conflict, business/reference/domain validation and calculation, and ends with atomic
persistence (stage 12). HTTP headers, request-local API state, and payload representations never
enter application logic.

## Reactive and Resource Boundary Design

| Operation | Adapter/port boundary | Classification | Execution context | Timeout/resource bound | Required evidence |
|-----------|-----------------------|----------------|-------------------|------------------------|-------------------|
| Header/body mapping, correlation, and time initialization | API → application mapping | Non-blocking bounded CPU | Event loop | 2 MiB, bounded fields/collections; one request instant | Header/correlation matrix, midnight vector, max payload, blocked-thread check |
| Fingerprint generation | Application normalization service | Bounded CPU, no I/O | Calling context | ≤2 MiB, SHA-256, version 1 | Vectors and maximum-payload benchmark |
| Monetary/domain calculation | Pure domain | Synchronous deterministic bounded CPU | Calling context | ≤500 lines | Exact vectors, p99 budget, no event-loop warning |
| Binding/root lookup | Repository port/reactive adapter | Non-blocking database I/O | Reactive PostgreSQL client | Pool/query bound below 10 s | Unavailable, timeout, replay/conflict tests |
| Aggregate/binding write | Repository port/reactive adapter | Non-blocking database I/O | Reactive transaction | 5-second transaction timeout | Rollback phase injection and concurrency |
| Flyway startup migration | Startup infrastructure | Blocking lifecycle operation outside requests | Controlled startup | Deployment timeout/empty-db evidence | Migration and readiness tests |

No blocking filesystem/network operation, SOAP, XML, signature, certificate, SRI, or Company call
exists. A reactive wrapper is not evidence that an underlying operation is non-blocking.

## Company Context and Sensitive-Data Design

**Internal Caller Boundary**: No operation has application authentication or authorization. The
OpenAPI contract defines no security scheme/requirement, Authorization header, `401`, or `403`.

**Company Header Contract**: Exactly one `X-Company-Id` is required. Trim; reject blank/missing as
`COMPANY_CONTEXT_REQUIRED`; reject repeated/malformed/nil as `COMPANY_CONTEXT_INVALID`; normalize
accepted UUID to lowercase hyphenated form. CompanyId never appears in path/query/body.

**Correlation Contract**: Bootstrap one safe correlation value at the earliest HTTP boundary so
payload-size and Company errors also carry it; evaluate correlation validity as FR-041 stage 3,
after Company validation and before idempotency-key validation. Preserve one trimmed safe value of
1–64 approved ASCII characters and generate a UUID when absent. Blank, repeated, over-length, or
unsafe supplied values MUST NOT be echoed; generate a safe replacement UUID and return
`INVALID_REQUEST` only when correlation validation governs. Correlation never affects idempotency
equivalence.

**Company Ownership Scoping**: API maps to application `CompanyId`; the command carries it; the
aggregate stores it immutably; response returns it. Existing-draft repository reads/mutations use
CompanyId plus draftId. Idempotency uses CompanyId plus key hash. This is partitioning, not
authorization.

**Company Master-Data Boundary**: No Company/Issuer/establishment/emission lookup, validation,
port, client, adapter, repository, table, shared persistence, cross-service foreign key/transaction,
cache, replication, retry/timeout, health/readiness, fiscal context, or snapshot exists.

**Sensitive Data**: Raw idempotency keys, normalized request content, buyer data, payloads, SQL,
and internals are absent from errors/observability/binding storage. SHA-256 fingerprints and a
normalization version are persisted. Correlation remains transport/operational evidence, not draft
data.

Certificate lifecycle is not applicable: certificate use/management is explicitly out of scope.

## API and Error Contract

- Effective operation: `POST /api/v1/invoice-drafts` (`/api/v1` server base plus
  `/invoice-drafts` resource path).
- Required headers: `X-Company-Id`, `Idempotency-Key`.
- Optional `X-Correlation-Id`: preserve one valid supplied value; generate a UUID when absent; for
  invalid input, never echo it, generate a safe replacement UUID, and return `INVALID_REQUEST` when
  that validation step governs.
- Strict body schemas reject `companyId`, `issuerId`, fiscal/snapshot data, unknown properties, and
  calculated fields.
- Response includes canonical `companyId`, local draft `id`, opaque `emissionPointId`, complete
  commercial/calculated draft, `createdAt`, and `updatedAt`.
- New commit returns `201`; equivalent replay returns `200`; both identify replay state.
- Stable statuses/codes are defined in `error-catalog.md` and represented in OpenAPI.
- Monetary envelope violations use `BUSINESS_VALIDATION_FAILED` with violation code
  `MONETARY_RANGE_EXCEEDED`; API, calculation, persistence, response, and test limits are identical.
- Failure evaluation follows FR-041 exactly.

**OpenAPI Source of Truth**: `specs/001-create-invoice-draft/contracts/invoice-draft-api.openapi.yaml`
is the only independently authored contract. `src/main/resources/META-INF/openapi.yaml` is a
byte-for-byte runtime publication copy created from that canonical file and MUST NOT be edited as a
second contract. Contract tests parse and resolve both files and fail unless their bytes and
semantics are identical. Runtime configuration MUST set `mp.openapi.scan.disable=true`, preventing
SmallRye OpenAPI from merging annotation-scanned endpoint models into the static contract. The
packaged JVM suite MUST fetch `/q/openapi`, resolve the served document, prove semantic equality to
the canonical contract, and reassert SC-024 against what the running service actually publishes.

The contract contains no Company-not-found/inactive/unavailable/timeout/authorization result and
no authentication or authorization result.

## Data and External Consistency Design

| Boundary/command | Intermediate states | Retry/idempotency | Duplicate handling | Timeout | Recovery/reconciliation | Observable outcome |
|------------------|---------------------|-------------------|--------------------|---------|-------------------------|--------------------|
| Header/body acceptance | No durable state | Payload → Company → correlation → key → body precedence | N/A | Overall 10 s begins at acceptance; one request instant is captured after stage 5 passes | Correct request | 400/413 or continue |
| Binding lookup | No new durable state | CompanyId + key hash; fingerprint/version compare | Equivalent replay; different content conflict | Bounded query | Same key safely retried | 200/409 or continue |
| New aggregate write | Tentative root/children/binding inside one transaction | Binding inserted in same commit | Unique Company/key hash arbitrates race | 5-second write tx | Loser rolls back and re-reads winner | 201/200/409 or safe 503/504/500 |
| Response delivery | Commit already authoritative | Same equivalent retry | Returns original | 10-second request deadline | Binding reconciles response loss | Original draft observable |

There is no external-system boundary and no claim of atomicity beyond local PostgreSQL.

## Persistence and Idempotency Design

Detailed schema/constraint/transaction decisions are in `data-model.md` and
`persistence-design.md`. Detailed hashing/canonicalization/replay decisions are in
`idempotency-design.md`.

Key invariants:

- root `company_id uuid NOT NULL` and non-nil;
- quantity/unit price columns use `numeric(12,6)`; monetary columns use `numeric(17,2)`; tax-rate
  columns use `numeric(5,2)` with range checks mirrored by pre-persistence validation;
- local children reference only the draft/line;
- existing-root operations use CompanyId + draftId;
- binding uniqueness is `UNIQUE (company_id, idempotency_key_hash)`;
- binding-to-root composite Company/draft foreign key;
- SHA-256 key hash and request fingerprint are 32 bytes;
- `normalization_version = 1` for new bindings;
- no raw key or complete normalized request is persisted;
- root, children, and binding commit or roll back together.

## Native Compatibility Evaluation

| Risk area | Applicable? | Build evidence required | Runtime evidence required | Decision/consequence |
|-----------|-------------|-------------------------|---------------------------|----------------------|
| SOAP clients | No | Excluded by spec | None | Not applicable |
| XML generation/schema | No | Excluded by spec | None | Not applicable |
| XML signatures | No | Excluded by spec | None | Not applicable |
| PKCS#12 handling | No | Excluded by spec | None | Not applicable |
| Cryptographic providers | SHA-256 via Java platform only | Optional native build if native claimed | Fingerprint vectors | No custom provider |
| Reflection/resources | Yes: Jackson, validation, Panache, Flyway, OpenAPI, health | Native build if claimed | Create/replay/conflict/PostgreSQL/health smoke | JVM remains accepted if evidence fails |

## Project Structure

### Documentation

```text
specs/001-create-invoice-draft/
├── spec.md
├── plan.md
├── research.md
├── reference-data-baseline.md
├── data-model.md
├── persistence-design.md
├── idempotency-design.md
├── error-catalog.md
├── operational-requirements.md
├── traceability.md
├── quickstart.md
└── contracts/
    └── invoice-draft-api.openapi.yaml
```

No Company port contract or task file is generated.

### Planned Source Boundaries

```text
src/main/java/com/alexastudillo/taxdocument/
├── api/invoicedraft/
├── application/invoicedraft/
├── domain/invoicedraft/
└── infrastructure/invoicedraft/

src/main/resources/
├── application.properties
├── db/migration/
└── META-INF/
    └── openapi.yaml  # byte-for-byte publication copy of the canonical contract

src/test/java/com/alexastudillo/taxdocument/
├── api/invoicedraft/
├── application/invoicedraft/
├── architecture/
├── domain/invoicedraft/
├── infrastructure/invoicedraft/
└── runtime/

src/test/resources/
├── application.properties
└── invoicedraft/
    ├── calculation-vectors.json
    └── idempotency-v1-vectors.json
```

Composition remains outside the domain. No `company`, `security`, Company-client, or cache package
is planned.

## Test and Operational Evidence Plan

| Requirement/risk | Level/environment | Observable invariant | Required negative/boundary evidence |
|------------------|-------------------|----------------------|-------------------------------------|
| Company header/canonicalization | API + application | Valid/mixed-case UUID maps/stores/returns canonical CompanyId | missing/blank/malformed/nil/repeated; Company body/path/query rejected |
| Clean layer handoff | Architecture + application | Command has explicit CompanyId; aggregate immutable CompanyId | no HTTP/security/thread-local/Gateway types below API |
| No Company/security integration | Architecture/config/runtime trace | zero Company/auth calls/dependencies/spans | no Company existence/status/tenant/emission ownership tests |
| Draft business rules | Domain/application | official buyer/IVA/date/decimal/zero/payment/text/collection outcomes | numeric maxima/overflow, invalid/unsupported/calculated input, and midnight/replay vectors |
| Persistence/Flyway | Real PostgreSQL from empty | constraints, local child ownership, Company-scoped root access | no prohibited tables/fields; all write-phase rollbacks |
| Idempotency | Real PostgreSQL concurrency | replay/conflict/cross-Company independence/one winner | property/collection order, line order, response loss, no normalized payload storage |
| API errors/correlation | Contract/integration | ordered upload/header/entity gate; exact code/status; safe Problem Details; supplied/generated/replacement correlation | Content-Length/chunked over-limit; malformed JSON plus earlier header failure; blank/repeated/unsafe/over-length correlation; 400/409/413/422/503/504/500; no 401/403 |
| Published OpenAPI | Static and packaged runtime | canonical/runtime file equality; scan disabled; served `/q/openapi` semantic equality | no merged path/schema/security/401/403 drift |
| No fiscal side effects | Application/architecture/trace | zero sequence/access-key/XML/signature/certificate/SRI/PDF/event activity | no fiscal adapter/config/span |
| Health/observability | Packaged runtime | liveness/readiness separation; bounded metrics/logs/traces | PostgreSQL down; no Company readiness; no sensitive/high-cardinality labels |
| Performance/resources | Warmed packaged JVM + PostgreSQL | all budgets in operational requirements | max payload, 50-way contention, pool recovery, no blocked event loop |
| JVM/native | Packaged JVM; optional native | mandatory JVM critical smoke; native build+runtime if claimed | no native claim from build alone |

`traceability.md` maps each requirement group, acceptance/success evidence, design artifact,
contract/data evidence, test level, and prohibited behavior.

**Liveness and Readiness**: PostgreSQL/local initialization only for readiness; liveness remains
independent. No Company, identity, gateway/BFF, or SRI check.

**Structured Observability**: Correlation propagates through safe structured logs/traces. Metrics
use bounded labels only. CompanyId is never a metric label.

**Audit Events**: New commit, replay, conflict, persistence unavailable/timeout, and significant
rollback are observable without buyer/raw-key/payload data.

**External Destination Consistency**: The readiness check uses the same PostgreSQL datasource as
business persistence. No other destination exists.

## Complexity Tracking

| Addition | Requirement | Simpler alternative | Why insufficient | Consequence |
|----------|-------------|---------------------|------------------|-------------|
| Quarkus HTTP/validation/contract/health extensions: `quarkus-rest-jackson`, `quarkus-hibernate-validator`, `quarkus-smallrye-openapi`, `quarkus-smallrye-health` | FR-001–FR-026, FR-039–FR-044, Constitution X/XIII | JDK HTTP/hand-written JSON, validation, OpenAPI, and health | Reimplements framework capabilities, weakens contract/validation evidence, and conflicts with the Quarkus baseline | BOM-managed extensions; API, contract, validation, health, JVM, and optional-native evidence |
| Quarkus reactive persistence/migration extensions: `quarkus-hibernate-reactive-panache`, `quarkus-reactive-pg-client`, `quarkus-flyway`, `quarkus-jdbc-postgresql` | FR-020–FR-024, FR-030–FR-034, FR-043, Constitution III/V/IX | Hand-written reactive SQL and manual migrations | Panache/Flyway/PostgreSQL are mandatory; JDBC remains migration-only | Reactive adapter tests, empty-database migrations, transaction/timeout/concurrency evidence |
| Quarkus observability extensions: `quarkus-micrometer`, `quarkus-opentelemetry` | FR-025–FR-026, SC-002, SC-012, SC-023, SC-033, Constitution XIII | Logs alone | Cannot prove bounded metrics and trace/correlation behavior | Profile-controlled exporters and sensitive/high-cardinality exposure tests |
| Quarkus test stack: `quarkus-junit`, `quarkus-test-vertx`, `quarkus-test-hibernate-reactive-panache`, `io.rest-assured:rest-assured`, and Database Dev Services supplied by the PostgreSQL extensions | Constitution XII and all acceptance evidence | Plain JUnit with mocked persistence or direct Testcontainers lifecycle | Cannot prove packaged Quarkus HTTP/reactive/PostgreSQL behavior; direct lifecycle duplicates Dev Services | Real PostgreSQL 18.4, HTTP, reactive transaction, concurrency, migration, and JVM evidence; no direct Testcontainers dependency |
| Spotless `8.8.0` with google-java-format `1.35.0` | Constitution Definition of Done | Manual formatting or unpinned formatter default | Not deterministic or enforceable in CI | One reproducible `spotlessCheck`; no second formatter |
| Four outbound abstractions: `InvoiceDraftRepository`, `ReferenceDataPort`, `RequestClock`, `DraftIdentifierGenerator` | FR-006–FR-024, FR-027–FR-034, FR-045–FR-047, Constitution IV | Call Panache, system clock, or UUID generation directly from application/domain | Would couple use-case/domain policy to infrastructure and prevent deterministic tests at actual replaceable boundaries | Four capability-local interfaces only; no generic hierarchy, Company port, or speculative factory |
| Durable fingerprint-only idempotency binding | FR-027–FR-034 | In-memory/key-only dedupe | Not durable, cross-process safe, semantically comparable, or privacy-minimal | Migration, fingerprint vectors, concurrency/rollback evidence |
| Versioned local tax-document reference catalogs | DR-001 | Hard-coded codes/rates | Prohibited; cannot represent activity/effective dates/version | Flyway reference migrations and catalog tests |

JDK 25 `javac -Xlint:all -Werror` adds no external dependency and complements, rather than
duplicates, the architecture tests. There is no additional microservice, external client, cache,
broker, background process, shared database, second datastore, generic repository hierarchy, or
custom authentication framework.

## Phase 0 and Phase 1 Outputs

- Phase 0 research: `research.md` — status reflects any material reference-data evidence gate.
- Reference baseline and row-level approval register: `reference-data-baseline.md`.
- Phase 1 model: `data-model.md`.
- Phase 1 API contract: `contracts/invoice-draft-api.openapi.yaml`.
- Phase 1 validation guide: `quickstart.md`.
- Supporting designs: `error-catalog.md`, `persistence-design.md`,
  `idempotency-design.md`, `operational-requirements.md`, and `traceability.md`.

The requirements-quality artifacts are complete. Constitution v2.0.0 is approved on `main` and
`origin/main`, `6-ft-1` descends from that approved mainline, and the Constitution Check now passes.
This plan is eligible for honest pre-task checklist regeneration and does not itself generate
implementation tasks.
