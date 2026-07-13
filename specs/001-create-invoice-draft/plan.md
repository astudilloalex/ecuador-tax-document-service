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
empty-database Flyway tests, OpenAPI contract tests, architecture tests, pure domain vectors,
packaged JVM tests, and optional native build/runtime smoke

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

**Constraints**: 2,097,152-byte request-body maximum; 500 lines; 10 payments; 15 additional entries;
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
| Reference baseline | `specs/001-create-invoice-draft/reference-data-baseline.md` | Official buyer-type, IVA-rule, and payment-method candidates; row-level evidence and target-UUID approval gate |
| Architecture decisions | This plan and supporting Phase 0/1 artifacts; no separate ADR | Feature-local technical choices |
| Technology authorities | Quarkus release/Java 25 guidance; PostgreSQL 18.4 release/versioning guidance linked in `research.md` | Runtime/database versions |
| Legacy evidence | `docs/legacy/as-is/` paths listed in the spec | Historical discovery only |

**Source Conflicts and Resolutions**: Historical and superseded feature artifacts required
Keycloak, tenant-derived Company context, a Company client/port, status/eligibility checks, and
fiscal snapshots. Constitution v2.0.0 and the clarified spec have higher authority. All such
components/outcomes are removed or explicitly prohibited. Current SRI IVA guidance reports `13%`,
while Circular NAC-DGECCGC25-00000006 states that `15%` remains effective until modified and the
approved feature vector uses `15%`. No effective-date resolution is inferred; PFV-002 records the
conflict and blocks a percentage-rate seed row until authoritative reconciliation is approved.

**Pending Functional Validation**:

- `PFV-001`: approve every buyer-identification baseline row and its official validation evidence;
- `PFV-002`: reconcile the effective percentage-rate IVA row, including the current official-rate
  source conflict, effective interval, and stable target UUID;
- `PFV-003`: approve every payment-method row and stable target UUID mapping.

`reference-data-baseline.md` records the row-level evidence and approval state. Planning MUST NOT
promote an unverified row into a Flyway seed, quickstart request, or fixture. These PFVs block
`$speckit-tasks` but do not require inventing an answer during this Phase 0/1 design pass.

**Terminology Mapping Impact**: `Company Identifier`/`CompanyId` remains the canonical opaque
ownership value from `X-Company-Id`. `Fiscal Context Snapshot` remains reserved for a later
fiscal-issuance specification. `X-Correlation-Id` is the canonical transport spelling in these
artifacts.

## Constitution Check

*GATE: Recorded before Phase 0 and re-evaluated after Phase 1.*

| Gate | Pre-Research evidence | Post-Design evidence |
|------|-----------------------|----------------------|
| Greenfield bounded outcome | PASS — one target create/review outcome | PASS — no legacy compatibility or unrelated lifecycle |
| Authority/versioned evidence | PASS — Constitution v2.0.0 and SRI v2.32 identified | BLOCKED FOR TASKS — research and the baseline register exact evidence, but PFV-001–PFV-003 remain pending |
| English terminology | PASS — target terms are English | PASS — all generated artifacts use canonical English with exact SRI exceptions |
| Required baseline | PASS — Java/Quarkus/Mutiny/PostgreSQL/Panache/Flyway fixed | PASS — Quarkus 3.33.2.1 LTS and PostgreSQL 18.4 justified |
| Reference-data evidence | PFV-001–PFV-003 registered; inference prohibited | BLOCKED FOR TASKS — candidate rows are inventoried, but zero rows are seed-authorized |
| Clean Architecture | PASS — four boundaries required | PASS — explicit API→application→domain/infrastructure mapping below |
| Domain purity | PASS — synchronous deterministic fiscal model | PASS — no transport/framework/persistence/security/Mutiny types in domain |
| Reactive safety | PASS — no blocking external operation in scope | PASS — reactive PostgreSQL only; bounded domain work and evidence budget |
| Fiscal correctness | PASS — approved IVA/buyer/date/decimal rules | BLOCKED FOR TASKS — numeric/date behavior is complete; unverified catalog rows cannot be seeded or tested as authoritative |
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

No constitutional deviation or complexity exception is requested. Phase 1 design may complete with
the explicitly registered evidence gate, but task generation and implementation MUST remain blocked
until every row required by `reference-data-baseline.md` is approved and the research status can be
changed to complete.

## Clean Architecture Mapping

```text
HTTP request
  → api: parse/validate X-Company-Id and transport DTOs
  → application: CreateInvoiceDraftCommand(CompanyId, business inputs, idempotency/correlation)
  → domain: InvoiceDraft aggregate and deterministic calculation
  → application outbound ports
  → infrastructure: reactive Panache/PostgreSQL, local catalogs, clock/identifier adapters
```

| Boundary | Responsibility | Allowed dependencies | Prohibited inputs/types |
|----------|----------------|----------------------|-------------------------|
| `api` | Enforce payload/header/body representation, strict unknown fields, correlation, DTO mapping, Problem Details | `application` | Persistence entities returned directly; Company/security clients |
| `application` | Carry explicit CompanyId, enforce failure precedence/idempotency/use-case preconditions, orchestrate transaction ports | `domain`, Mutiny, application ports | HTTP headers/requests, `SecurityIdentity`, `JsonWebToken`, thread-local/Gateway objects |
| `domain` | Own immutable CompanyId on Invoice Draft; buyer/line/tax/payment invariants and exact calculation | Java/approved domain libraries | HTTP/JSON/Quarkus/Panache/PostgreSQL/Mutiny/security types |
| `infrastructure` | Implement local repository/catalog/clock/identifier ports with reactive PostgreSQL/Panache | application ports/domain types | Company/SRI/security adapter, shared database, cache |

Actual outbound boundaries are limited to the Invoice Draft repository, local identification/tax/
payment catalog access, clock, and identifier generation. No interface named or equivalent to
`CompanyContextPort`, `ResolveCompanyFiscalContextPort`, or Company authorization/eligibility is
introduced.

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

**Correlation Contract**: Initialize correlation at the HTTP boundary after Company validation and
before idempotency-key validation. Preserve one trimmed safe value of 1–64 approved ASCII
characters; generate a UUID when absent. Blank, repeated, over-length, or unsafe supplied values
MUST NOT be echoed; generate a safe replacement UUID and return `INVALID_REQUEST` when correlation
validation governs. Correlation never affects idempotency equivalence.

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

The contract contains no Company-not-found/inactive/unavailable/timeout/authorization result and
no authentication or authorization result.

## Data and External Consistency Design

| Boundary/command | Intermediate states | Retry/idempotency | Duplicate handling | Timeout | Recovery/reconciliation | Observable outcome |
|------------------|---------------------|-------------------|--------------------|---------|-------------------------|--------------------|
| Header/body acceptance | No durable state | Payload → Company → correlation → key → body precedence | N/A | Overall 10 s begins at acceptance; one request instant captured | Correct request | 400/413 or continue |
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
└── db/migration/

src/test/java/com/alexastudillo/taxdocument/
├── api/invoicedraft/
├── application/invoicedraft/
├── domain/invoicedraft/
└── infrastructure/invoicedraft/
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
| API errors/correlation | Contract/integration | exact code/status, safe Problem Details, supplied/generated/replacement correlation | blank/repeated/unsafe/over-length correlation; 400/409/413/422/503/504/500; no 401/403 |
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
| Durable fingerprint-only idempotency binding | FR-027–FR-034 | In-memory/key-only dedupe | Not durable, cross-process safe, semantically comparable, or privacy-minimal | Migration, fingerprint vectors, concurrency/rollback evidence |
| Versioned local tax-document reference catalogs | DR-001 | Hard-coded codes/rates | Prohibited; cannot represent activity/effective dates/version | Flyway reference migrations and catalog tests |

There is no additional microservice, external client, cache, broker, background process, shared
database, second datastore, generic repository hierarchy, or custom authentication framework.

## Phase 0 and Phase 1 Outputs

- Phase 0 research: `research.md` — status reflects any material reference-data evidence gate.
- Reference baseline and row-level approval register: `reference-data-baseline.md`.
- Phase 1 model: `data-model.md`.
- Phase 1 API contract: `contracts/invoice-draft-api.openapi.yaml`.
- Phase 1 validation guide: `quickstart.md`.
- Supporting designs: `error-catalog.md`, `persistence-design.md`,
  `idempotency-design.md`, `operational-requirements.md`, and `traceability.md`.

The next workflow command is `$speckit-checklist`; implementation tasks are intentionally not
generated by this plan command.
