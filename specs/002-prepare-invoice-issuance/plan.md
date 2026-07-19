# Implementation Plan: Prepare Invoice for Fiscal Issuance

**Branch**: `002-prepare-invoice-issuance` feature context (workspace Git branch: `8-ft-2`) |
**Date**: 2026-07-18 | **Spec**: `specs/002-prepare-invoice-issuance/spec.md`

**Input**: Completed Feature 002 specification and 2026-07-18 Product Owner clarification `Q1: A`.

## Summary

Add one `fiscalpreparation` capability that takes an existing Company-owned Invoice Draft and
returns one immutable Fiscal Preparation. The synchronous operation checks natural replay first,
fixes Ecuador date eligibility once, resolves a minimal authoritative fiscal context through one
read-only non-blocking boundary, and then uses one short reactive PostgreSQL transaction to lock the
Company-scoped draft followed by its exact controlled sequence baseline. The winning transaction
allocates the next nine-digit number, creates an eight-digit Numeric Code under versioned policy,
constructs and fully validates the SRI v2.33 Access Key, inserts the append-only preparation and
snapshot, and advances the baseline atomically. Concurrent/retried equivalent requests return that
same committed winner. No draft mutation, fiscal-master replication, XML, signature, certificate,
SRI communication, delivery, administration, or identity/security feature is added.

## Technical Context

**Language/Version**: Java 25

**Framework**: Quarkus `3.33.2.1`, retaining the repository's exact LTS baseline and BOM alignment

**Reactive Model**: Mutiny for asynchronous application, REST Client, and persistence flows;
constant bounded domain work remains synchronous on the event loop

**Persistence**: Hibernate Reactive with Panache; Panache records, locking, SQLSTATE/constraint
classification, and transaction orchestration remain in `infrastructure`

**Database**: PostgreSQL 18.4 in verification; schema targets supported PostgreSQL 18 minors

**Schema Migration**: Flyway only; add the next immutable version after V5 with no baseline seed

**Caller Security**: None inside this repository. No authentication, authorization, Keycloak, JWT,
OIDC, token, principal, role, permission, service credential, `Authorization` contract, `401`, or
`403` behavior is introduced.

**Testing**: JUnit 5, Quarkus JUnit, REST Assured, Quarkus Vert.x/Hibernate Reactive test support,
the existing PostgreSQL 18.4 test resource, a lightweight Vert.x fiscal-provider fixture, pure
domain vectors, API/contract, Application ordering, migration/constraint, rollback/commit-
uncertainty, 100-request concurrency, Company-scoping, sensitive-data, architecture, health,
packaged JVM smoke, and packaged JVM performance evidence. XML, signature, certificate, SRI, PDF,
queue, and notification tests assert absence only.

**Target Execution**: Mandatory packaged JVM execution on Linux with Java 25 and PostgreSQL 18.x

**Native Compatibility**: Deferred and unclaimed. A later claim requires actual native build and
runtime evidence for REST Client JSON mapping, Hibernate Reactive persistence, configuration,
reflection, and resource loading; JVM remains mandatory.

**External Integrations**: One approved contract-first, read-only REST capability identified as
`authoritative-fiscal-context`, governed for this consumer by
`contracts/authoritative-fiscal-context.openapi.yaml` version `1.0.0` and accountable to the
`Fiscal Context Provider Owner`. The provider base URL is environment configuration and is never
hardcoded. There is no SRI, SOAP/XML, certificate, storage, rendering, broker, notification,
Company master, or identity integration.

**Company Context Boundary**: Require exactly one `X-Company-Id` value, trim only surrounding ASCII
SP/HTAB, parse a syntactically valid non-nil UUID, normalize it canonically, and map it to the
existing immutable `CompanyId`. Carry it explicitly through every local and external port. There is
no Company lookup/status validation, Company client, local Company row, cache, replication,
cross-service foreign key/transaction, token/session source, or response Company property.

**Performance Goals**: One terminal response within the fixed ten-second overall deadline; provider
connection within one second and response within two seconds, clamped to remaining budget; all
local locks/queries clamped below remaining budget; 100 same-draft requests commit one identity;
100 different drafts in one scope commit the exact next 100 values with no duplicate or locally
caused gap. The existing reactive pool maximum of 20 is retained and tested under queued load.

**Constraints**: No request body; non-empty content is `INVALID_REQUEST`. One draft per request, one
read-only provider attempt on a first-preparation path, no automatic provider or identity-
transaction retry after commit may have begun, fixed draft-then-baseline lock order, PostgreSQL
transaction-local lock/statement limits, no baseline auto-initialization, range
`000000001..999999999`, Numeric Code policy `SECURE_RANDOM_8_V1`, and exact SRI v2.33 key rules.

**Scale/Scope**: Company-owned Invoice only, one Invoice Draft and zero-or-one preparation per
natural key, one exact sequence scope per transaction, with 100-request concurrency acceptance
evidence. Other tax documents and batch preparation are outside scope.

**Sensitive Data**: Snapshot RUC, names, addresses, source evidence, Numeric Code, and Access Key are
stored only in the existing managed PostgreSQL boundary and returned only in the explicitly
contracted successful internal representation with `Cache-Control: no-store`. The `Platform
Operations Owner` is accountable for TLS in transit, PostgreSQL encryption at rest, encrypted
backup handling/restoration, and the Invoice-record retention policy. Errors, logs, traces,
metrics, health, audit signals, and test production-like examples omit complete values, raw source
bodies, credentials, internal endpoints, SQL, and baseline values. No certificate lifecycle,
custom application database encryption/key management, deletion API, or separate retention
scheduler is introduced; Fiscal Preparation is retained and disposed of with its related Invoice
record under the approved platform policy.

### Null Safety Scope

Every new Feature 002 package and every extracted shared package is package-level `@NullMarked`.
An existing Feature 001 Java type modified by Feature 002 enters checked scope through a narrow
type-level `@NullMarked` annotation; its otherwise untouched package is not declared audited. New
packaged-JVM evidence belongs to the null-marked `runtime.fiscalpreparation` subpackage so the
existing Feature 001 `runtime` package is not marked transitively. The shared service-health types
and their tests belong to the null-marked `infrastructure.health` package. Error Prone and NullAway
run on every Java compilation task with `OnlyNullMarked=true`; every resulting warning is corrected
at its source without `@NullUnmarked`, broad exclusions, or new warning suppressions.

## Source and Terminology Evidence

| Authority | Applicable source and version/path | Requirements or decisions governed |
|-----------|------------------------------------|-------------------------------------|
| Ecuadorian legislation and official SRI documentation | [Consolidated Regulation for Sales, Withholding, and Complementary Documents, last modified 2023-12-29](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/9fb49475-f058-49a1-b08a-f31bf4deb074/Reglamento_Comprobantes_Venta_RetencionYDC_29122023.pdf); [SRI Offline Technical Sheet v2.33, updated July 2026 and modified 2026-07-13](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/5a547488-80f3-4966-a2a4-841f2e951986/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.33.pdf), sections 5.2–5.5, Tables 1–4, Annexes 21/22/24; SRI portal reviewed 2026-07-18 | Issuer facts, 3+3+9 numbering, consecutive issuance, Access Key composition, document/environment/emission codes, Numeric Code discretion, Modulo 11, designations, retention/non-reuse context |
| Constitution | `.specify/memory/constitution.md` v2.0.1, approved 2026-07-16 | Authority precedence, English terminology, reactive Clean Architecture, Company boundary, persistence atomicity, external adapter and test/operations gates |
| Approved specification and clarifications | `specs/002-prepare-invoice-issuance/spec.md`, especially FR-001–FR-081, DR-001–DR-011, SC-001–SC-016; clarification 2026-07-18 `Q1: A` | Complete target behavior, exact same-Ecuador-date rule, errors, exclusions, null safety |
| Approved prior feature | `specs/001-create-invoice-draft/spec.md`, `plan.md`, `data-model.md`, implementation and V1–V5 migrations | Existing Company-owned Invoice Draft, commercial authority, opaque Emission Point UUID, `DRAFT` state, PostgreSQL and API patterns |
| Architecture decisions | No ADR exists; decisions are recorded in `research.md` and `data-model.md` | Capability naming, external boundary, locks, two-table model, recovery, nullness enforcement |
| Canonical terminology | `docs/migration/terminology-mapping.md`, Feature 002 registration verified 2026-07-18 | English Target Domain registration for Fiscal Preparation, Fiscal Context Snapshot, Official Sequence Baseline, Official Sequential Number, Numeric Code, Access Key, and Fiscal Source Evidence while preserving Feature 001 history |
| Legacy evidence | `docs/legacy/source-baseline.md`; `docs/legacy/as-is/05-business-rules.md`, `06-validation-rules.md`, `07-process-flows.md`, `13-technical-debt.md`, and `14-pending-functional-validation.md` citations already enumerated in `spec.md` | Discovery and negative/candidate vectors only; no target behavior derives from legacy authority |

**Source Conflicts and Resolutions**:

1. SRI v2.33 page 64 prints an Access Key ending in `6`, but independent application of its own
   v2.33 Modulo 11 rule produces `0`. The rule sections govern; the printed value is a negative
   regression vector, not a target positive vector.
2. Feature 001 correctly excluded Fiscal Context Snapshot, Access Key, Official Sequential Number,
   and the other fiscal-identity values from Invoice Draft creation. The canonical terminology
   mapping now registers them for Feature 002 as Target Domain concepts without rewriting that
   historical scope.
3. Existing Feature 001 architecture tests globally prohibit fiscal capability artifacts and its
   OpenAPI test expects whole-file equality to a one-path contract. Those lower-level guards are
   narrowed to Feature 001 and replaced with Feature-002-aware positive/negative assertions; the
   approved Feature 002 specification and Constitution Principle XVI govern.
4. The repository currently has warning-as-error but no production nullness analyzer. JSpecify is
   test-only and `javac -Xlint` does not analyze nullness. Feature 002 closes this evidence gap with
   incremental JSpecify/NullAway enforcement rather than claiming the prior state was null-safe.
5. Existing liveness/readiness classes are owned by the Feature 001 `invoicedraft` infrastructure
   package, but successful Flyway V6 readiness is service-wide once a second capability exists.
   Feature 002 therefore relocates them to the shared `infrastructure.health` boundary. This is a
   justified service-level technical grouping: one process and one PostgreSQL/Flyway destination
   require one coherent health signal, while ownership by either business capability would be false
   and duplicated checks could disagree.

**Pending Functional Validation**: None. The later XML feature must choose its own exact Invoice XSD
version, but Feature 002 is deliberately schema-neutral and this does not block its Fiscal Context Snapshot
or identity outcome. Provider availability, baseline provisioning, and sensitive-data platform
controls are approved architecture/deployment dependencies with accountable roles and evidence
paths below. Approval does not claim that a production provider destination, production baseline,
or target-environment control evidence already exists. No remaining dependency requires a new
material business decision.

**Terminology Mapping Impact**: The canonical mapping now classifies `Fiscal Preparation`, `Fiscal
Context Snapshot`, `Official Sequence Baseline`, `Official Sequential Number`, `Numeric Code`,
`Access Key`, and `Fiscal Source Evidence` as Target Domain concepts for Feature 002 while retaining
exact SRI representations and Feature 001 historical exclusions. Company, Issuer, Establishment,
and Emission Point remain externally owned; raw provider field names remain Infrastructure-only.

## Approved Architecture and Deployment Dependencies

**Fiscal-context provider**: The approved logical capability identifier is
`authoritative-fiscal-context`; the approved consumer contract is
`contracts/authoritative-fiscal-context.openapi.yaml` version `1.0.0`; and the accountable role is
`Fiscal Context Provider Owner`. The provider owns authoritative Company-to-Issuer, Establishment,
Emission Point, eligibility, effective-period, and immutable source-revision data. Feature 002 owns
only the consumer port, response validation/mapping, bounded timeout, and caller-safe error
classification. Provider implementation, master-data administration, and deployment are outside
scope. The contract's `.invalid` server is deliberately non-routable metadata, not a runtime
destination; the base URL comes only from environment configuration. Implementation and automated
acceptance may proceed against the approved contract fixture. Production deployment is blocked
until a concrete provider destination and accountable operational owner are registered in
deployment configuration. No local Company, Issuer, Establishment, or Emission Point master-data
copy is permitted.

**Official Sequence Baseline provisioning**: The accountable role is `Database Operations Owner`.
Production baselines are provisioned outside Feature 002 through a controlled, reviewed, auditable
SQL/runbook procedure that validates Company ownership, exact Issuer/Establishment/Emission Point/
document-type scope including the official codes, and initial `lastAllocated`. Its external audit
evidence identifies requester, approver, execution time, scope, and resulting baseline identifier
without exposing sensitive values in general telemetry. Feature 002 only reads, locks, validates,
and increments an existing baseline inside the successful preparation transaction. It exposes no
administration API and performs no missing-row creation, Flyway seed, upsert-on-missing, reset,
decrement, repair, wrap, or reuse. Tests may provision fixture rows only. Production readiness for
one fiscal scope requires approved provisioning evidence before its first preparation request; this
is an operational gate, not a Company-specific service-readiness probe.

**Sensitive-data platform controls**: The accountable role is `Platform Operations Owner`. That
role owns TLS-enabled service and PostgreSQL connections, approved PostgreSQL encryption at rest,
encrypted backup policy/handling, successful restoration evidence, and the approved Invoice-record
retention policy under which Fiscal Preparation is retained and disposed of with its Invoice.
These artifacts are required release evidence for the target environment. Feature 002 introduces
no custom application-level database encryption, key management, deletion API, or retention
scheduler. Raw provider requests, responses, credentials, and internal errors are absent from
primary persistence and backups because they are never stored. The intentional absence of a
deletion API does not override the platform retention policy.

These are approved responsibilities and evidence paths, not claims that target production
destinations, baseline rows, or platform controls are already deployed.

## Constitution Check

*GATE: Every row passed before research and was re-evaluated after the Phase 1 design.*

| Gate | Pre-Research evidence | Post-Design evidence |
|------|-----------------------|----------------------|
| Greenfield scope: one bounded outcome; no implicit legacy compatibility | PASS — one existing draft becomes zero-or-one preparation; legacy is evidence only | PASS — one `fiscalpreparation` capability, no compatibility route/schema/status |
| Authority: official sources are versioned; conflicts and Pending Functional Validation are recorded | PASS — legislation and SRI v2.33/version/date cited | PASS — page-64 defect resolved explicitly; no PFV remains |
| Language: target names are English and terminology mapping decisions are respected | PASS — spec uses canonical English | PASS — Feature 002 Target Domain registrations are complete and preserve Feature 001 historical exclusions |
| Baseline: required technologies are fixed; Quarkus version research is identified pre-research and resolved with justification post-design | PASS — repository baseline and one client unknown identified | PASS — Java 25, Quarkus 3.33.2.1, PostgreSQL 18.4 retained; only BOM REST Client added |
| Architecture: `api`, `application`, `domain`, and `infrastructure` dependencies and mappings comply | PASS — four-boundary design required | PASS — capability-local boundaries, transport-neutral ports, pure domain, explicit mappings |
| Domain purity: no framework, transport, persistence, JSON, security, or Mutiny types in `domain` | PASS — pure fiscal values planned | PASS — Access Key, snapshot, sequence, preparation are Java-only immutable values |
| Reactive safety: every blocking or CPU-intensive operation is isolated, bounded, timed out, and testable | PASS — external/database operations identified | PASS — REST Client and PostgreSQL are non-blocking; constant domain work bounded; Flyway startup-only |
| Fiscal correctness: official rules, `BigDecimal` policies, time semantics, and invalid-data behavior are explicit | PASS — no monetary calculation; SRI/date research required | PASS — exact v2.33 composition/DV, secure code policy, named Ecuador zone, unchanged totals/date |
| Internal caller boundary: no authentication, authorization, identity, token, security scheme, `401`, or `403` behavior is introduced | PASS — explicitly excluded | PASS — neither inbound nor outbound contract defines security behavior |
| Company boundary: Company context uses the mandatory single `X-Company-Id` UUID header; Company identifiers are absent from request bodies/input schemas and appear in responses only when explicitly contracted; Company-owned aggregate/persistence/idempotency operations enforce the UUID while immutable global SRI catalogs remain outside automatic Company scope; no Company lookup, snapshot, shared persistence, cache, or replication exists | PASS — exact header and ownership requirements approved | PASS — Company carried by every port/query/FK/scope and provider header; omitted from bodies/response; no master state |
| Sensitive data: storage, encryption, redaction, retention, and certificate lifecycle are defined before implementation | PASS — Fiscal Context Snapshot is sensitive; certificates excluded | PASS — `Platform Operations Owner`, target TLS/at-rest/backup-restore/Invoice-retention evidence, PostgreSQL-only storage, no-store success, and telemetry redaction are explicit |
| Persistence: Flyway-only evolution, immutable migrations, database invariants, and empty-database tests are defined | PASS — baseline must be existing/atomic | PASS — V6 after immutable V1–V5, two tables/no seed, constraints/triggers, empty/V5-upgrade tests, and external `Database Operations Owner` provisioning evidence |
| Boundary consistency: states, retries, idempotency, duplicates, timeouts, recovery, reconciliation, and terminal outcomes are defined | PASS — natural replay/unknown outcome required | PASS — draft-first arbitration, no hidden identity retry, Company+draft reconciliation, exact stable outcomes |
| API and async quality: DTO separation, validation ownership, stable errors, correlation, and result observation are defined | PASS — synchronous terminal contract | PASS — bodyless POST, 201/200, replay header, separate DTOs, safe correlation, Problem Details catalog |
| External adapters: ports, endpoint configuration, sanitized observability, resilience, and contract evidence are defined | PASS — approved fiscal boundary explicitly required | PASS — `authoritative-fiscal-context` v1.0.0, `Fiscal Context Provider Owner`, configurable destination, fixture acceptance, production registration gate, 1s/2s limits, no auto-retry, sanitized mapping |
| Testing: acceptance scenarios and applicable risk-based tests are identified before production tasks | PASS — 24 acceptance scenarios/16 success criteria | PASS — domain, use-case, provider, PostgreSQL, concurrency, API, architecture, JVM evidence paths listed |
| Operations: meaningful liveness/readiness, structured logs, auditing, and destination-consistent health checks are defined | PASS — PostgreSQL/provider semantics identified | PASS — service health semantics and separate production provider/baseline/platform evidence gates have accountable roles without false deployment claims |
| Simplicity: every dependency, abstraction, process, store, and distributed interaction is justified | PASS — bounded outcome permits one source/one baseline | PASS — one production extension, one port, one transaction-shaped store, two tables, no background process |
| Runtime evidence: JVM verification is mandatory and native status has an evidence path | PASS — JVM required/native unclaimed | PASS — exact JVM commands/scenarios planned; native remains honestly deferred |

No constitution deviation or approval is required.

## Reactive and Resource Boundary Design

| Operation | Infrastructure adapter and application port | Blocking/CPU classification | Execution context | Timeout and resource bound | Required concurrency/failure evidence |
|-----------|---------------------------------------------|-----------------------------|-------------------|----------------------------|---------------------------------------|
| Inbound header/path classification and DTO mapping | Fiscal Preparation API boundary to use case | Non-blocking, bounded string/UUID work | Quarkus event loop | Fixed small headers; no body; overall deadline begins before classification | Header precedence/cardinality, unsafe correlation replacement, no body, blocked-event-loop inspection |
| Authoritative fiscal resolution | REST Client adapter behind `FiscalContextPort` | Non-blocking network I/O plus bounded JSON mapping | Quarkus REST Client/Vert.x event loop | Connect 1s, response 2s, both clamped to remaining 10s; one attempt; bounded 300-char fields | timeout/unavailable/invalid payload/cancellation, no retry, 100 equivalent pre-commit reads tolerated, no sensitive leakage |
| Invoice Draft/preparation lookup | Reactive persistence adapter behind transaction-shaped store | Non-blocking PostgreSQL I/O | Reactive session/event loop | Remaining budget clamped to local ceiling; pool max 20 | Company predicates, replay during provider outage, cross-Company safe 404 |
| Draft/baseline locks, insert, baseline increment, flush/commit | Reactive persistence adapter/store | Non-blocking PostgreSQL I/O with lock wait | Reactive transaction/event loop | Transaction-local `lock_timeout`/`statement_timeout` below remaining budget; fixed two-row lock order | 100 same draft, 100 same scope, distinct scopes, rollback, deadlock/timeout/SQLSTATE classification |
| Numeric Code selection | Infrastructure generator implementing application port | Constant CPU/random work; singleton CSPRNG initialized/warmed during controlled startup, no blocking entropy acquisition on request path | Request pipeline after both locks | One bounded integer sample per winning transaction | leading zeros/`00000000`, concurrent uniqueness of final Access Keys, generator failure rollback |
| Access Key construction and validation | Pure domain generator/value | Constant CPU over exactly 48/49 digits | Request pipeline inside local transaction | No executor; fixed input size | official/synthetic vectors, every component mutation, SRI printed defect negative vector |
| Flyway migration | Existing Flyway/JDBC startup boundary | Blocking database startup work | Controlled startup, never request event loop | Existing startup controls; exact PostgreSQL 18.4 | empty schema, V5 upgrade, checksums, no seed |

Reactive wrappers are not used to disguise blocking clients. No worker executor or background task is
introduced because all request I/O uses native reactive clients and all CPU work has constant tiny
input. CSPRNG provider initialization occurs outside the request event loop and is runtime-tested.

## Company Context and Sensitive-Data Design

**Internal Caller Boundary**: The service trusts only that an upstream boundary has handled any
platform identity concerns. It does not authenticate, authorize, infer a user, inspect tokens,
define security schemes, accept `Authorization`, or emit `401`/`403`. Company ownership predicates
are data isolation, not permission decisions.

**Company Header Contract**: Exactly one mandatory `X-Company-Id`; one surrounding ASCII SP/HTAB
trim; valid UUID grammar; nil rejected; canonical lowercase hyphenated internal form. Missing uses
`COMPANY_CONTEXT_REQUIRED`; repeated, combined, blank, malformed, or nil uses
`COMPANY_CONTEXT_INVALID`. No owned-data/provider access occurs first. Company is prohibited from
path/query/body/input schema/token/session and omitted from response. Draft ID is a separate
non-nil path UUID. Correlation is validated/replaced after Company classification and never affects
equivalence.

**Company Ownership Scoping**: Preflight read, replay read, locked draft read, preparation read and
insert, baseline lookup/lock/update, composite foreign keys, unique natural identity, reconciliation,
and external fiscal selection all receive the canonical Company value. The external call carries it
only in `X-Company-Id`. Cross-Company draft access is indistinguishable from absence. There is no
global Company-scoped SRI catalog in this feature.

**Company Master-Data Boundary**: No generic Company/Issuer/Establishment/Emission Point repository,
administration client, local master table, cache, replication, shared database, cross-service FK, or
distributed transaction is added. The approved `authoritative-fiscal-context` port is a read-only issuance
resolution required by Constitution Principle XVI and the Feature 002 spec; it returns one minimal
versioned decision, not master data. Per-Company baseline presence is not a readiness probe.

**Sensitive Data and Certificate Lifecycle**: Required snapshot/identity fields are stored together
in PostgreSQL under Company ownership, returned only in success, and governed by the `Platform
Operations Owner` controls and release evidence defined above. Complete Access Key, Numeric Code, RUC, names,
addresses, source revision, raw response/request, provider error/body, internal endpoint, SQL, and
baseline values are excluded from error/telemetry/audit labels. There is no certificate, PKCS#12,
private key, encryption-key, rotation, expiration, revocation, or certificate-deletion lifecycle in
scope. Provider credentials are neither defined nor persisted by this feature. Fiscal Preparation
retention/disposal follows its related Invoice record; no application deletion or retention process
is introduced.

## Data and External Consistency Design

| Boundary or logical command | Intermediate states | Retry and idempotency | Duplicate handling | Timeout | Failure recovery and reconciliation | Observable terminal outcomes |
|-----------------------------|---------------------|-----------------------|--------------------|---------|-------------------------------------|------------------------------|
| Initial Company+draft preparation request | Request context with fixed entry instant/date; no durable state | Natural key only: Company+draft; correlation irrelevant; no Idempotency-Key | Existing complete preparation returns immediately | Overall 10s begins at entry | Invalid header/path/body is conclusive before owned access | 201 new, 200 replay, or stable 4xx/5xx Problem Details |
| Read-only fiscal-context resolution | Validated resolution exists only in memory before commit | No automatic retry; caller may naturally replay whole request | Concurrent first requests may duplicate only this read-only call; one local winner commits | Connect 1s/read 2s clamped to remaining | Map provider outcome safely; discard raw payload; never allocate sequence on failure | Validated snapshot candidate or `FISCAL_CONTEXT_*` |
| First local fiscal-identity commit | No durable provisional state; locks then one atomic commit | No rerun after uncertain commit; safe full-rollback deadlock retry is not required | Draft lock returns same-draft winner; baseline lock serializes different drafts; named unique constraints backstop | Remaining budget via transaction-local lock/statement limits | Rollback restores preparation absence and baseline value; reconcile after transaction ends | Created, Replay, specific baseline/access failure, persistence failure, timeout, or unknown outcome |
| Commit/response knowledge boundary | PostgreSQL is either fully committed or fully rolled back; caller knowledge may be unknown | Same Company+draft retry only | Unique Company+draft prevents second identity | Client/deadline cancellation after commit begins is conservative | Company-scoped read returns winner; inconclusive read yields `PREPARATION_OUTCOME_UNKNOWN`; never compensate/reuse | Success/replay, confirmed `PERSISTENCE_FAILURE`, or unknown outcome without zero-state claim |
| Committed natural replay | One immutable row and advanced baseline already exist | Unlimited equivalent replay with different timing/correlation | Exact committed row returned; no regeneration | Read bounded by remaining overall deadline | Provider/source/date/baseline may be unavailable or changed without affecting replay | 200 with identical preparation and zero excluded side effects |

No table transaction is claimed atomic with the provider read. Provider validation completes before
sequence locking. Commit uncertainty is epistemic only: PostgreSQL never exposes a partial
preparation/baseline transaction.

## Native Compatibility Evaluation

| Risk area | Applicable? | Build evidence | Runtime evidence | Decision and consequences |
|-----------|-------------|----------------|------------------|---------------------------|
| SOAP clients | No — no SRI/provider SOAP operation | Not applicable by exclusion | Absence asserted by architecture/dependency tests | No SOAP dependency or native risk |
| XML generation and schema validation | No — explicitly excluded | Not applicable | Absence asserted | Later feature owns XSD/version/native evidence |
| XML digital signatures | No — explicitly excluded | Not applicable | Absence asserted | No signature library/provider |
| PKCS#12 certificate handling | No — explicitly excluded | Not applicable | Absence asserted | No key store or secret lifecycle |
| Cryptographic providers | No special provider — JDK CSPRNG and integer Modulo 11 only | JVM `quarkusBuild` planned | Numeric policy/domain tests and JVM smoke planned | Standard JDK behavior; not a native-support claim |
| Reflection and resource loading | Yes — REST Client/Jackson DTOs, Hibernate Reactive records, configuration, OpenAPI | Mandatory JVM build; native build only if later claimed | Mandatory JVM smoke/concurrency; native runtime only if later claimed | JVM supported; native deferred until actual evidence |

## Project Structure

### Documentation (this feature)

```text
specs/002-prepare-invoice-issuance/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── fiscal-preparation-api.openapi.yaml
│   └── authoritative-fiscal-context.openapi.yaml
├── checklists/
│   ├── requirements.md
│   └── readiness.md
└── tasks.md                              # generated by $speckit-tasks, not this phase
```

### Source Code (repository root)

```text
src/main/java/com/alexastudillo/taxdocument/
├── api/
│   ├── requestcontext/                   # narrow shared Company/correlation parsing
│   ├── problem/                          # shared safe Problem Details representation
│   ├── invoicedraft/                     # existing Feature 001 route-specific behavior
│   └── fiscalpreparation/                # Feature 002 resource, boundary, DTOs, errors, telemetry
├── application/
│   ├── requestcontext/                   # shared request clock/deadline values
│   ├── invoicedraft/
│   └── fiscalpreparation/                # use case, command/result, fiscal port, atomic store port
├── domain/
│   ├── invoicedraft/                     # existing CompanyId and Invoice Draft domain
│   └── fiscalpreparation/                # preparation, snapshot, scope, sequence, code, Access Key
└── infrastructure/
    ├── requestcontext/                   # shared system request clock
    ├── persistence/                      # shared remaining-budget clamp only
    ├── health/                           # justified service-wide liveness/readiness boundary
    ├── invoicedraft/
    └── fiscalpreparation/                # REST adapter, Panache records/mappers/store, generators

src/main/resources/
├── META-INF/openapi.yaml                 # merged runtime source of truth
├── application.properties               # bounded provider/persistence configuration
└── db/migration/
    └── V6__create_fiscal_preparation.sql # two structures, constraints/guards, zero baseline rows

src/test/java/com/alexastudillo/taxdocument/
├── api/fiscalpreparation/
├── application/fiscalpreparation/
├── domain/fiscalpreparation/
├── infrastructure/fiscalpreparation/
├── infrastructure/health/
├── architecture/
└── support/fiscalpreparation/

src/test/resources/fiscalpreparation/
└── sri-access-key-v2.33-vectors.json

src/integrationTest/java/com/alexastudillo/taxdocument/runtime/fiscalpreparation/
├── FiscalPreparationJvmSmokeIT.java
└── FiscalPreparationJvmPerformanceIT.java
```

**Structure Decision**: `fiscalpreparation` names the bounded result and avoids implying XML/SRI
issuance. The API maps HTTP DTOs to a transport-neutral use case. Application owns ordering,
selection validation, and ports. Domain owns immutable fiscal values and the pure Access Key rule.
Infrastructure owns REST Client DTO mapping, CSPRNG, Panache entities, locks, transaction, SQLSTATE
classification, and commit reconciliation. Reuse existing `CompanyId`; do not migrate a shared
domain kernel. Extract only proven generic Company/correlation/deadline/Problem/persistence-budget
helpers from Feature 001; route state, timeout wording, telemetry, and failure mapping stay
capability-specific. Relocate the existing health checks to `infrastructure.health` because
liveness and PostgreSQL/Flyway readiness are one service-wide technical responsibility shared by
both capabilities; retaining them under either capability creates false ownership, while duplicate
checks could produce conflicting service signals. Architecture and Feature 001 semantic regression
tests protect these extractions. Existing Feature 001 packages remain unmarked except for the exact
modified types identified in the tasks; new Feature 002 packages are package-level null-marked.

## Test and Operational Evidence Plan

| Requirement/risk | Test level and environment | Planned path | Observable behavior or invariant | Failure/boundary cases |
|------------------|----------------------------|--------------|----------------------------------|------------------------|
| FR-043–053 / SC-005 Access Key | Pure domain/JUnit | `domain/fiscalpreparation/AccessKeyGeneratorTest.java`; vector JSON | Exact 49 digits, components, widths, DV, policy values | official vectors, raw 10/11, 48/50/nondigit, every component mutation, invalid page-64 sample |
| FR-025–033 snapshot | Pure domain/JUnit | `domain/fiscalpreparation/FiscalContextSnapshotTest.java` | Complete immutable minimal values/evidence; exact optionality | missing Special Taxpayer/Withholding Agent resolution, partial Large Contributor resolution/legend, invented accounting/RIMPE resolution, ineffective interval, unsupported codes, source mismatch |
| DR-001–011 aggregate/nullness | Domain + compile analysis | `domain/fiscalpreparation/FiscalPreparationTest.java`; Gradle compile | One indivisible immutable identity; no null/partial state | null inputs, mutation attempts, optional values, warning-as-error |
| FR-003–008 / Q1=A date | Application/JUnit with fixed clock | `application/fiscalpreparation/PrepareInvoiceForFiscalIssuanceUseCaseTest.java` | replay first; fixed Ecuador date once; draft unchanged | prior/future/exact date, midnight crossing, inconsistent prior state |
| FR-018–024 external ordering | Application/port fakes | same use-case test | provider called only after eligibility and before commit; replay skips it | unavailable/invalid/unsupported/inconsistent, different Emission Point, deadline |
| External consumer contract | Quarkus test + local Vert.x HTTP fixture | `infrastructure/fiscalpreparation/FiscalContextHttpAdapterTest.java` | exact header/body, JSON mapping, timeouts, one attempt, sanitized failures | 404/409/422/503/504, malformed/oversized/partial response, cancellation, no retry |
| FR-034–042 baseline constraints | PostgreSQL 18.4 | `infrastructure/fiscalpreparation/FiscalPreparationRepositoryAdapterTest.java` | exact scope, next value, 9 digits, no auto-init/wrap | missing, scope mismatch, invalid, 998/999 boundary, exhausted |
| FR-054–064 natural replay/concurrency | PostgreSQL 18.4 concurrent integration | `infrastructure/fiscalpreparation/FiscalPreparationConcurrencyTest.java` | 100 same draft -> one identity/increment; 100 drafts -> exact next 100 | distinct scopes, pool 20 queueing, loser returns winner, no local gaps |
| FR-060–064 atomicity/uncertainty | PostgreSQL fault integration | `infrastructure/fiscalpreparation/FiscalPreparationRollbackTest.java` | every confirmed rollback leaves zero changes; unknown never claims zero | fault before/after baseline update/insert/flush/commit initiation, response loss, reconciliation unavailable |
| Database invariants/immutability | Empty/V5-upgrade PostgreSQL 18.4 | `infrastructure/fiscalpreparation/FiscalPreparationMigrationTest.java` | V1–V5 immutable; V6 exact tables/FKs/uniques/checks/guards; no seed | direct update/delete, cross-Company FK, duplicate key/scope/sequence, partial designation |
| FR-010–017 Company boundary | API + persistence spy/real PG | `api/fiscalpreparation/FiscalPreparationResourceTest.java` | exact header cardinality; zero access before valid header; safe cross-Company 404 | missing/repeated/blank/malformed/nil, same UUID path under other Company |
| FR-065–075 API/errors/deadline | API/Quarkus | resource test and `FiscalPreparationRequestDeadlineHandlerTest.java` | 201/200, replay header, safe correlation, exact status/code, one terminal result | body supplied, malformed/nil draft, invalid correlation replacement, 10s race, unknown outcome |
| API source of truth | OpenAPI semantic test | `api/fiscalpreparation/FiscalPreparationOpenApiContractTest.java` plus updated Feature 001 test | merged runtime contract preserves Feature 001 and adds only approved Feature 002 route | no Company body/response, no Idempotency-Key/security/401/403/excluded routes |
| FR-075 sensitive data | API/adapter/telemetry inspection | `infrastructure/fiscalpreparation/SensitiveFiscalDataExposureTest.java` | success-only contracted data; no sensitive error/log/metric/trace/health values | provider/SQL exceptions, Access Key/RUC/name/address/source/baseline redaction |
| FR-076–080 exclusions/architecture | Static architecture and dependency tests | `architecture/FiscalPreparationBoundaryTest.java`; updated existing boundary tests | clean dependencies; no XML/SRI/cert/security/admin/queue/Company master paths | forbidden imports, routes, entities, dependencies, executors/background jobs |
| FR-081 / SC-016 null safety | Compile/static analysis | Gradle `compileJava`/`compileTestJava`, package-info and exact modified-type tests | every new package and every modified existing type is in checked scope; untouched Feature 001 packages remain unmarked; zero NullAway/JSpecify/javac warnings | framework nullable edges, Panache first-result null, hydration, optional columns, no suppressions or over-broad package marking |
| SC-001/003/004 JVM runtime | Packaged JVM + PG18.4 + provider fixture | `runtime/fiscalpreparation/FiscalPreparationJvmSmokeIT.java`, `FiscalPreparationJvmPerformanceIT.java` | mandatory packaged JVM success/replay/concurrency/deadline/pool recovery | provider/DB outage, response loss, midnight, 100-request loads, event-loop blocking |
| Approved provider deployment prerequisite | Contract fixture plus target-environment release review | outbound contract metadata and deployment configuration evidence | fixture acceptance may proceed; production has a concrete configured destination and accountable operational owner | `.invalid` placeholder never used at runtime; no hardcoded URL or local master-data fallback |
| Approved baseline provisioning responsibility | Operational evidence review plus PostgreSQL 18.4 fixture setup | controlled SQL/runbook evidence owned by `Database Operations Owner` | requester, approver, execution time, exact scope, resulting baseline identifier, and valid initial value are evidenced before first production use | fixture-only inserts remain non-production; no seed, auto-init, upsert, repair, reset, wrap, or admin API |
| Sensitive-data platform controls | Target-environment release evidence | platform TLS/encryption/backup/restore/retention records owned by `Platform Operations Owner` | TLS service/DB connections, at-rest approval, encrypted backup and successful restore, approved Invoice retention, linked disposal confirmation | no application encryption/key management/deletion/scheduler; raw provider material never stored or backed up |

**Liveness and Readiness**: Liveness reports only process/event-loop viability. Readiness requires
the configured PostgreSQL destination and successfully applied Flyway schema. It does not depend on
a representative Company, a baseline row, or a Company-specific provider call. Fiscal-provider
outage remains a monitored business dependency that returns `FISCAL_CONTEXT_UNAVAILABLE` for first
preparation while allowing committed replay. A provider health dependency may be added only if the
provider later supplies a safe non-Company-specific read-only health contract against the exact same
base destination; none is invented here. Separately, production enablement for a fiscal scope is
blocked until the provider destination/owner, baseline provisioning evidence, and platform-control
release evidence are registered; those deployment gates do not change service health semantics.

**Structured Observability**: Propagate one safe correlation value; record new/replay/error outcome,
stable error code, total/source/database/lock latency, deadline owner, commit-knowledge class, and
sanitized opaque local identifiers. Metrics include counts and bounded outcome labels only. No
Company UUID, Invoice Draft UUID, Access Key, Numeric Code, RUC, name, address, source revision/raw
body, baseline value, SQL, or endpoint appears as a metric label or unsanitized signal.

**Audit Events**: Emit sanitized operational events for preparation committed, replay observed,
baseline failure class, confirmed rollback, and unknown commit outcome. Audit contains correlation,
opaque/hardened local references, rule/policy version, and timestamps but is not another snapshot or
raw fiscal payload store. There are no certificate events.

**External Destination Consistency**: Business persistence, readiness, Flyway, and tests resolve the
same configured PostgreSQL database, with JDBC used only by Flyway and reactive client used at
runtime. Fiscal REST calls use one named configured base URL; no separate probe destination exists
without an approved provider health contract. The planning `.invalid` URL is never a runtime
destination, and this plan does not claim that target production destination evidence already exists.

## Complexity Tracking

| Addition | Requirement creating the need | Simpler alternatives considered | Why insufficient | Testing and operational consequences |
|----------|-------------------------------|--------------------------------|------------------|--------------------------------------|
| BOM-managed `quarkus-rest-client-jackson` | FR-018–024 authoritative external fiscal context | Blocking client; local data; caller fields | Blocking violates reactive path; local/caller data is non-authoritative/forbidden | Contract fixture, configured limits, provider failure metrics; no automatic retry |
| One external fiscal-context port/adapter and consumer contract | FR-018–033 | Direct adapter call from API; generic master-data client | Clean Architecture requires port; generic client expands scope | Application fakes, adapter contract tests, redaction, configured endpoint |
| `official_sequence_baseline` controlled Company-owned table with globally unique exact fiscal scope | FR-034–042 | PostgreSQL sequence, max+1, JVM counter | `nextval` is non-rollback; others are unsafe across processes; Company is ownership, not another numbering dimension | Row-lock/rollback/concurrency/duplicate-cross-Company-scope tests; no seed/runtime admin, plus external `Database Operations Owner` provisioning evidence before first production use of a scope |
| Append-only `fiscal_preparation` table with embedded snapshot | FR-008–009, FR-025–031, FR-045–064 | Separate snapshot table; JSONB; no durable snapshot | Separate table/JSON weakens atomic/minimal typed invariants; no persistence cannot replay | Flyway constraints/guard, immutable reads, sensitive-data retention |
| Database immutability/scope guards and fixed row-lock order | FR-008, FR-042, FR-058–062 | Repository convention only; advisory locks; serializable isolation | Direct SQL/concurrency needs final arbiter; row targets already exist | Direct mutation negative tests, lock timeout/deadlock evidence |
| Test-only commit-boundary transport fault harness | FR-059, FR-064, SC-010 | Mocked exception; HTTP response-loss hook only | Those prove mapping/replay but not a genuinely lost PostgreSQL COMMIT acknowledgement | No production dependency; isolated PostgreSQL transport-fault scenario accepts either DB truth, then proves retry converges to zero-or-one and never two |
| Narrow shared request-context/problem/deadline extraction | Second API capability plus FR-010–017/068–075 | Import from `invoicedraft`; duplicate parsers; generic request framework | False ownership/drift or excessive abstraction | Feature 001 regression tests and capability-specific boundary tests |
| One shared `infrastructure.health` boundary | Constitution IV/XIII and one service-wide PostgreSQL/Flyway readiness signal across two capabilities | Keep service health under `invoicedraft`; duplicate checks per capability | Capability-local ownership is false after V6; duplicate checks can disagree for one process and destination | Relocation regression test preserves process-only liveness and bounded same-destination readiness with no provider, Company, baseline, or mutation probe |
| JSpecify 1.0.0 production annotations, Error Prone plugin 5.1.0, Error Prone 2.50.0, NullAway 0.13.7 | FR-081 / SC-016 and explicit owner direction | `-Xlint` only; annotations only; broad suppression | Neither `-Xlint` nor annotations alone performs enforced null analysis | Package-level marking for new packages, type-level marking for modified existing types, warnings as errors, no suppression escape or transitive marking of untouched packages; build-time cost only |

There is no background process, distributed lock, cache, broker, scheduler, Company master store,
Issuer master store, or caller idempotency table.

Constitution deviations: None.

| Deviated principle and rule | Scope | Justification | Approval record | Expiration or remediation condition |
|-----------------------------|-------|---------------|-----------------|-------------------------------------|
| None | None | All gates pass without deviation | Not applicable | Not applicable |
