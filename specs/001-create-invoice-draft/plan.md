# Implementation Plan: Create Invoice Draft

**Branch**: `6-ft-1` | **Date**: 2026-07-12 | **Spec**: `specs/001-create-invoice-draft/spec.md`

**Input**: Clarified feature specification with Constitution v2.0.0 Company-context decisions

**Implementation progression**: **BLOCKED before T017** by `GATE-GOV-001`. See
`governance-nonconformity.md`; T001–T016 require retrospective review and real owner approval.

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
crosses midnight. Separately, `createdAt` is the UTC `java.time.Instant` captured exactly once
inside the persistence transaction, after all business validations succeed and immediately before
the new Invoice Draft is persisted. The same immutable value is persisted and returned only after
commit confirmation; rollback never exposes it and replay returns the original. It is not a
PostgreSQL physical commit timestamp, requires no `track_commit_timestamp`, and is never queried or
reconstructed after commit. The clock remains injectable and deterministic for tests.

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
| Testing | PASS — 61 scenarios and 33 success criteria | PASS — traceability maps every FR-001–FR-047, DR-001–DR-024, SC-001–SC-033, and AS-001–AS-061 |
| Operations | PASS — health/correlation required | PASS — PostgreSQL-only readiness, metrics/log/trace/performance budgets defined |
| Simplicity | PASS — no speculative platform/component | PASS — only local ports, datastore, and two justified persisted capabilities |
| Runtime evidence | PASS — JVM mandatory/native optional | PASS — packaged JVM and conditional native evidence paths defined |
| Mandatory Spec Kit workflow | PASS for the pre-task planning evidence available at that point | **NON-CONFORMANT** — T001–T016 preceded a verifiable post-task `$speckit-analyze`; `GOV-001` and `GATE-GOV-001` block T017 and later work |

No constitutional complexity exception is requested. A workflow non-conformity exists because
T001–T016 were implemented before the mandatory analysis gate; it is recorded without
retroactive correction in `governance-nonconformity.md`.

**Current implementation gate**: **BLOCKED before T017** — retrospective review, deviation
disposition, explicit owner approval, and a new analysis with no related CRITICAL finding are
required.

## Clean Architecture Mapping

```text
HTTP request
  → api: start one monotonic request deadline; parse/validate X-Company-Id and transport DTOs
  → application: CreateInvoiceDraftCommand(CompanyId, fixed request instant, mapped business
    inputs, idempotency/correlation, fixed RequestDeadline)
  → domain: InvoiceDraft aggregate and deterministic calculation
  → application outbound ports
  → infrastructure: reactive Panache/PostgreSQL, local catalogs, clock/identifier adapters
```

| Boundary | Responsibility | Allowed dependencies | Prohibited inputs/types |
|----------|----------------|----------------------|-------------------------|
| `api` | Own the one monotonic 10-second deadline from earliest routing through response completion; enforce FR-041 stages 1–5, including exact Idempotency-Key cardinality/normalization/errors; initialize safe correlation; capture one request instant; map transport inputs; return Problem Details | `application` | Persistence entities returned directly; Company/security clients; HTTP/header concerns passed to domain |
| `application` | Receive an already mapped CompanyId, fixed request instant, and transport-independent fixed deadline; enforce FR-041 stages 6–12, remaining-budget propagation, idempotency, use-case preconditions, and transaction-port orchestration | `domain`, Mutiny, application ports | HTTP headers/requests, `SecurityIdentity`, `JsonWebToken`, thread-local/Gateway objects |
| `domain` | Own immutable CompanyId on Invoice Draft; buyer/line/tax/payment invariants and exact calculation | Java/approved domain libraries | HTTP/JSON/Quarkus/Panache/PostgreSQL/Mutiny/security types |
| `infrastructure` | Implement local repository/catalog/clock/identifier ports with reactive PostgreSQL/Panache and clamp database work to the supplied remaining deadline budget | application ports/domain types | Company/SRI/security adapter, shared database, cache |

Actual outbound boundaries are limited to the Invoice Draft repository, local identification/tax/
payment catalog access, clock, and identifier generation. No interface named or equivalent to
`CompanyContextPort`, `ResolveCompanyFiscalContextPort`, or Company authorization/eligibility is
introduced.

**Failure-Precedence Ownership**: FR-041 stages 1–5 use one explicit pre-application pipeline:

1. A CDI `Router` observer registers an earliest-ordered handler for
   `POST /api/v1/invoice-drafts` before body consumption. It creates one fixed monotonic
   `RequestDeadline`, initializes safe correlation through the shared API correlation classifier
   solely for safe propagation, stores both in request-local API state, and arms one non-blocking
   deadline timer. It neither evaluates Company validity nor emits the stage-3 correlation error.
   Quarkus HTTP separately enforces the exact bare-byte setting
   `quarkus.http.limits.max-body-size=2097152` at its upload-limit route before REST dispatch. The
   same routing boundary registers the exclusive Vert.x status-`413` failure handler for this
   operation. That handler preserves one classified-valid correlation value or uses the already
   generated safe replacement, maps the approved Problem Details response, and never lets Company
   or correlation invalidity replace the payload-size outcome. Correlation classification on this
   path exists solely for safe response propagation; it never emits stage-3 `INVALID_REQUEST`.
   After selecting `REQUEST_PAYLOAD_TOO_LARGE`, the handler permits no normal deserialization,
   idempotency/reference lookup, validation, calculation, persistence, or other database operation.
   It delegates every other method, path, status, and failure through `RoutingContext.next()`.
2. After routing and before entity deserialization, one
   `@ServerRequestFilter(nonBlocking = true)` restricted to the create resource method by a custom
   Jakarta REST `@NameBinding` evaluates Company-header validity, the stored correlation
   classification, and idempotency-key presence/cardinality/one-time SP/HTAB trim/normalized
   grammar in exactly that order. Only this gate emits
   `INVALID_REQUEST` for correlation invalidity, and only after Company validation passes. It stores
   only the accepted mapped values in request-local API state.
3. Jackson deserialization and Bean Validation perform representation validation only after that
   gate passes. `quarkus.jackson.fail-on-unknown-properties=true` enforces strict bodies, and the
   built-in Jackson mismatched-input mapper is disabled with
   `quarkus.rest.exception-mapping.disable-mapper-for=io.quarkus.resteasy.reactive.jackson.runtime.mappers.BuiltinMismatchedInputExceptionMapper`
   so the feature mapper returns the approved stable response.

The route handlers and request gate use one shared API correlation classifier that always returns a
safe response value plus an absent/valid/invalid classification. Absent input generates a UUID,
valid input is preserved, and invalid input is replaced. Classification may occur for stage-1
safety, but only the request gate may turn invalidity into the stage-3 error, so it cannot overtake
payload-size or Company outcomes.

The earliest route handler is the sole overall-deadline owner. The deadline starts before body
reading from a monotonic elapsed-time source, is never restarted, remains armed through response
serialization, and is cancelled when the response ends. FR-041 orders conclusive stage outcomes;
the deadline is cross-cutting rather than a stage-12 persistence result. At every stage, an outcome
conclusively selected before expiry wins. If expiry occurs first, the handler atomically selects
correlated `REQUEST_TIMEOUT`; later stage/database completion cannot replace it. A later deadline
signal likewise cannot replace a terminal outcome selected before expiry.

This arbitration covers slow-body `413`/`504`, header `400`/`504`, replay or conflict/`504`,
validation or calculation/`504`, and persistence races. Confirmed rollback/failure or confirmed
commit before expiry selects its approved result. Expiry while commit outcome is unresolved selects
`504` as an uncertain outcome; the eventual database completion does not change that HTTP result,
and same-Company/key/content replay determines authoritative state.

The same fixed, transport-independent deadline is mapped into `CreateInvoiceDraftCommand`;
application work checks it before asynchronous stages, and every aggregate and reference-data
repository call receives an explicit remaining `Duration`. Each reactive adapter clamps pool
acquisition and every query subscription to the minimum of configured database-operation timeout
and that remainder, starts no work when it is zero or negative, and additionally clamps a write
transaction to the lesser of the effective budget and five seconds. A configured database timeout
that becomes conclusive while request budget remains maps to `PERSISTENCE_UNAVAILABLE`; only shared
request-deadline expiry maps to `REQUEST_TIMEOUT`.

Before response commitment, the atomic selection rules govern. Once the HTTP response is committed,
deadline expiry is telemetry-only: it cannot change status, emit an alternate response/body, start
another database or domain mutation, or compensate/delete a committed draft or binding. Existing
serialization may complete normally, and the safe
`request_deadline_exceeded_after_response_commit` event/counter is recorded. Equivalent replay
resolves any response loss.

The resource method is invoked only after stage 5. It calls the application `RequestClock` exactly
once to derive the immutable request instant carried by `CreateInvoiceDraftCommand`. That instant
is not reused as `createdAt`. For a logically new command only, persistence calls the injectable
clock exactly once inside the transaction after all business validations succeed and immediately
before root persistence, then stores that returned UTC Instant as immutable `createdAt`. There is no
post-commit clock/database timestamp read. Application orchestration begins at normalized-content
generation (stage 6), continues through replay/conflict, business/reference/domain validation and
calculation, and ends with atomic persistence (stage 12). HTTP headers, Vert.x/REST request state,
and payload representations never enter application or domain logic.

## Reactive and Resource Boundary Design

| Operation | Adapter/port boundary | Classification | Execution context | Timeout/resource bound | Required evidence |
|-----------|-----------------------|----------------|-------------------|------------------------|-------------------|
| Header/body mapping, correlation, deadline, and time initialization | API → application mapping | Non-blocking bounded CPU | Event loop | 2 MiB, bounded fields/collections; one monotonic 10 s deadline; one request instant | Controlled no-sleep payload/header deadline races; oversized absent/valid/invalid correlation with zero database calls; expiry/cancellation/post-commit telemetry; midnight vector; max payload; blocked-thread check |
| Fingerprint generation | Application normalization service | Bounded CPU, no I/O | Calling context | ≤2 MiB, SHA-256, version 1 | Vectors and maximum-payload benchmark |
| Monetary/domain calculation | Pure domain | Synchronous deterministic bounded CPU | Calling context | ≤500 lines | Exact vectors, p99 budget, no event-loop warning |
| Binding/root lookup | Repository port/reactive adapter | Non-blocking database I/O | Reactive PostgreSQL client | Pool/query clamped to request's remaining budget | Unavailable, deadline, replay/conflict tests |
| Buyer/IVA/payment reference lookup | ReferenceDataPort/reactive adapter | Non-blocking database I/O | Reactive PostgreSQL client | Every invocation receives remaining `Duration`; pool/query subscription uses min(configured timeout, remainder); no work at zero/negative remainder | Both minimum branches and mappings (configured timeout→503, shared deadline→504), exhausted-before-call, expiry-during-lookup, cancellation, and zero-state tests |
| Aggregate/binding write | Repository port/reactive adapter | Non-blocking database I/O | Reactive transaction | Lesser of remaining request budget and 5 seconds | Rollback phase injection, deadline, uncertain-commit recovery, and concurrency |
| Flyway startup migration | Startup infrastructure | Blocking lifecycle operation outside requests | Controlled startup | Deployment timeout/empty-db evidence | Migration and readiness tests |

No blocking filesystem/network operation, SOAP, XML, signature, certificate, SRI, or Company call
exists. A reactive wrapper is not evidence that an underlying operation is non-blocking.

## Company Context and Sensitive-Data Design

**Internal Caller Boundary**: No operation has application authentication or authorization. The
OpenAPI contract defines no security scheme/requirement, Authorization header, `401`, or `403`.

**Company Header Contract**: Exactly one `X-Company-Id` is required. Trim; reject blank/missing as
`COMPANY_CONTEXT_REQUIRED`; reject repeated/malformed/nil as `COMPANY_CONTEXT_INVALID`; normalize
accepted UUID to lowercase hyphenated form. Company identifiers are forbidden in request bodies
and input schemas, and no path/query/token/session value substitutes for the header. Canonical
`companyId` appears in the response only because the approved contract explicitly requires it.

**Idempotency Header Contract**: `Idempotency-Key` is mandatory and single-valued after HTTP
parsing. The API trims leading/trailing ASCII SP/HTAB exactly once, preserves internal characters
and case, then validates 1–128 characters against
`^[\x21-\x2B\x2D-\x7E](?:[\x20-\x2B\x2D-\x7E]{0,126}[\x21-\x2B\x2D-\x7E])?$`.
Missing returns `IDEMPOTENCY_KEY_REQUIRED`; blank/whitespace-only, over-length, control, non-ASCII,
or other non-comma grammar failure returns `IDEMPOTENCY_KEY_INVALID`; repeated/parser-multiple or
any comma-containing/comma-combined ambiguous value returns `IDEMPOTENCY_KEY_MULTIPLE`. No first
value is selected. Only the normalized value enters lookup, hashing, and persistence. These rules
remain in `api`; domain tests receive already normalized and validated values.

**Correlation Contract**: Bootstrap one safe correlation value at the earliest HTTP boundary so
payload-size and Company errors also carry it; evaluate correlation validity as FR-041 stage 3,
after Company validation and before idempotency-key validation. Preserve one trimmed safe value of
1–64 approved ASCII characters and generate a UUID when absent. Blank, repeated, over-length, or
unsafe supplied values MUST NOT be echoed; generate a safe replacement UUID and return
`INVALID_REQUEST` only when correlation validation governs. Correlation never affects idempotency
equivalence.

**Company Ownership Scoping**: API maps to application `CompanyId`; the command carries it; the
aggregate stores it immutably; response returns it. Every repository query or mutation involving
the Invoice Draft aggregate or its idempotency binding includes and enforces authoritative
CompanyId. This includes creation, draft lookup, duplicate/idempotency lookup, binding create/read,
aggregate persistence mutation, and future feature operations for that aggregate. Global VAT,
payment-method, identification-type, and other immutable SRI reference catalogs are not
Company-owned and receive no Company columns or automatic Company filter. This is partitioning,
not authorization.

**Company Master-Data Boundary**: No Company/Issuer/establishment/emission lookup, validation,
port, client, adapter, repository, table, shared persistence, cross-service foreign key/transaction,
cache, replication, retry/timeout, health/readiness, fiscal context, or snapshot exists.

The feature-level meaning of the Constitution's unqualified “bodies” and repository-scoping phrases,
and the pending formal constitutional clarification, are recorded in
`governance-nonconformity.md`. This plan does not amend Constitution v2.0.0 or its version metadata.

**Sensitive Data**: Raw idempotency keys, normalized request content, buyer data, payloads, SQL,
and internals are absent from errors/observability/binding storage. SHA-256 fingerprints and a
normalization version are persisted. Correlation remains transport/operational evidence, not draft
data.

Certificate lifecycle is not applicable: certificate use/management is explicitly out of scope.

## API and Error Contract

- Effective operation: `POST /api/v1/invoice-drafts` (`/api/v1` server base plus
  `/invoice-drafts` resource path).
- Required headers: exactly one `X-Company-Id` and exactly one parsed field value for
  `Idempotency-Key`; the latter follows the normalization, grammar, and three stable error codes
  above.
- Optional `X-Correlation-Id`: preserve one valid supplied value; generate a UUID when absent; for
  invalid input, never echo it, generate a safe replacement UUID, and return `INVALID_REQUEST` when
  that validation step governs.
- Strict request/input schemas reject `companyId`, `issuerId`, fiscal/snapshot data, unknown
  properties, and calculated fields. The response's explicitly contracted canonical `companyId`
  does not weaken the input prohibition.
- Response includes canonical `companyId`, local draft `id`, opaque `emissionPointId`, complete
  commercial/calculated draft, `createdAt`, and `updatedAt`.
- New commit returns `201`; equivalent replay returns `200`; both identify replay state.
- Stable statuses/codes are defined in `error-catalog.md` and represented in OpenAPI.
- Monetary envelope violations use `BUSINESS_VALIDATION_FAILED` with violation code
  `MONETARY_RANGE_EXCEEDED`; API, calculation, persistence, response, and test limits are identical.
- Failure evaluation follows FR-041 exactly, with `REQUEST_TIMEOUT` as a cross-cutting arbiter:
  conclusive stage outcome before expiry wins, otherwise `504` wins, and no later signal replaces
  the selected result.

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

## Executable Text-Repertoire Design

`productCode` uses the existing authoritative OpenAPI rule, made explicit as case-sensitive ASCII
`^[A-Za-z0-9]{1,25}$`. Passport (`06`) and foreign identification (`08`) use case-sensitive ASCII
`^[A-Za-z0-9]{1,20}$`. For all three, the API trims leading/trailing ASCII SP/HTAB once, changes no
internal character or case, and performs no case folding or Unicode normalization. Valid/invalid
vectors are `ABC123`/`sku9` versus `ABC-123`/`ÁBC1`/26 characters for product codes, and
`A1234567`/`EC9Z` versus `A-123`/`Á123`/21 characters for buyer values.

OpenAPI patterns and bounds, Java API validation, domain invariants over normalized values,
locale-independent PostgreSQL checks, and test fixtures MUST be equivalent. PostgreSQL POSIX
`[[:alnum:]]` is not equivalent to these ASCII expressions. Because committed Flyway migrations
are immutable and this remediation changes no migration, the observed T015 constraint differences
are recorded for retrospective review in `governance-nonconformity.md`; any later correction must
use a new migration after governance release.

## Data and External Consistency Design

| Boundary/command | Intermediate states | Retry/idempotency | Duplicate handling | Timeout | Recovery/reconciliation | Observable outcome |
|------------------|---------------------|-------------------|--------------------|---------|-------------------------|--------------------|
| Header/body acceptance | No durable state | Payload → Company → correlation → key → body precedence; each result must become conclusive before expiry | N/A | One monotonic 10 s deadline begins before body consumption and is never reset | 413/400 when classification wins; 504 when deadline wins; no later work after selection | 400/413/504 or continue |
| Binding/reference lookup | No new durable state | CompanyId + key hash; fingerprint/version compare; effective local catalogs | Equivalent replay; different content conflict | Each call receives remainder and clamps pool/query subscription to min(configured timeout, remainder) | Same key safely retried; no DB call at exhausted budget | 200/409/422/503/504 or continue |
| New aggregate write | Tentative root/children/binding inside one transaction | Binding inserted in same commit | Unique Company/key hash arbitrates race | Lesser of remaining budget and 5-second write ceiling | Confirmed rollback leaves zero state; unresolved-at-expiry is uncertain; replay reconciles; loser rolls back and re-reads winner | 201/200/409 or safe 503/504/500 |
| Response delivery | Commit already authoritative and success selected | Same equivalent retry | Returns original | Timer remains armed through serialization; after HTTP commitment expiry is telemetry-only | No status/body rewrite or compensation; serialization may finish; binding reconciles response loss | Selected response or connection-level response loss; original draft replayable |

There is no external-system boundary and no claim of atomicity beyond local PostgreSQL.

## Persistence and Idempotency Design

Detailed schema/constraint/transaction decisions are in `data-model.md` and
`persistence-design.md`. Detailed hashing/canonicalization/replay decisions are in
`idempotency-design.md`.

Key invariants:

- root `company_id uuid NOT NULL` and non-nil;
- root `created_at timestamptz NOT NULL` stores the single immutable UTC Instant captured once
  inside the write transaction after validation and immediately before persistence; the response
  and replay return that exact value, with no physical-commit-timestamp query;
- quantity/unit price columns use `numeric(12,6)`; monetary columns use `numeric(17,2)`; tax-rate
  columns use `numeric(5,2)` with range checks mirrored by pre-persistence validation;
- product and applicable buyer-identification checks use the exact locale-independent ASCII
  expressions above;
- local children reference only the draft/line;
- every aggregate/binding query or mutation uses authoritative CompanyId; global reference-catalog
  reads do not;
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
├── tasks.md
├── research.md
├── reference-data-baseline.md
├── data-model.md
├── persistence-design.md
├── idempotency-design.md
├── error-catalog.md
├── operational-requirements.md
├── traceability.md
├── quickstart.md
├── governance-nonconformity.md
└── contracts/
    └── invoice-draft-api.openapi.yaml
```

No Company port contract is generated. `tasks.md` is a separate `$speckit-tasks` output and appears
in the tree only because this is now the post-task artifact set; this plan did not generate it.

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
├── runtime/
└── support/  # FixedRequestClock and DeterministicDraftIdentifierGenerator from T075–T076

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
| Company header/canonicalization | API + application | Valid/mixed-case UUID maps/stores/returns canonical CompanyId | missing/blank/malformed/nil/repeated; Company request body/input/path/query rejected while explicit response CompanyId remains present |
| Clean layer handoff | Architecture + application | Command has explicit CompanyId; aggregate immutable CompanyId | no HTTP/security/thread-local/Gateway types below API |
| No Company/security integration | Architecture/config/runtime trace | zero Company/auth calls/dependencies/spans | no Company existence/status/tenant/emission ownership tests |
| Draft business rules | Domain/application | official buyer/IVA/date/decimal/zero/payment/text/collection outcomes | numeric maxima/overflow, invalid/unsupported/calculated input, and midnight/replay vectors |
| Persistence/Flyway | Real PostgreSQL from empty | exact ASCII constraints, local child ownership, authoritative Company on aggregate/binding operations, unscoped global catalogs, and immutable transaction-captured `createdAt` | no prohibited tables/fields/Company catalog columns; all write-phase rollbacks; no physical commit timestamp dependency |
| Idempotency | Real PostgreSQL concurrency | replay/conflict/cross-Company independence/one winner | property/collection order, line order, response loss, no normalized payload storage |
| API errors/correlation/deadline | Contract/integration with controlled deadline signal | ordered upload/header/entity gate; mandatory exactly-one normalized Idempotency-Key and three stable errors; exclusive 413 ownership; exact code/status; safe Problem Details; supplied/generated/replacement correlation; atomic first-conclusive outcome | Missing/blank/whitespace/repeated/comma/over-length/grammar key vectors never select first; Content-Length/chunked over-limit with valid correlation preserved or absent/invalid safely replaced and no DB call; deadline-first slow body/header/replay/validation/calculation; stage-first counterparts; post-response-commit expiry emits no 504/second write and records safe telemetry; malformed JSON plus earlier header failure; 400/409/413/422/503/504/500; no sleeps, 401, or 403 |
| Published OpenAPI | Static and packaged runtime | canonical/runtime file equality; scan disabled; served `/q/openapi` semantic equality | no merged path/schema/security/401/403 drift |
| No fiscal side effects | Application/architecture/trace | zero sequence/access-key/XML/signature/certificate/SRI/PDF/event activity | no fiscal adapter/config/span |
| Health/observability | Packaged runtime | liveness/readiness separation; bounded metrics/logs/traces | PostgreSQL down; no Company readiness; no sensitive/high-cardinality labels |
| Performance/resources | Warmed packaged JVM + PostgreSQL | one earliest-boundary monotonic deadline, aggregate and reference-data remaining-budget propagation, timer cancellation, and all budgets in operational requirements | maximum valid body ≤2 MiB, controlled deadline arbitration plus measured 10-second boundary across body/application/persistence/serialization, both database-timeout minimum branches, exhausted reference budget with no query, 50-way contention, pool recovery, no blocked event loop |
| JVM/native | Packaged JVM; optional native | mandatory JVM critical smoke; native build+runtime if claimed | no native claim from build alone |

`traceability.md` maps each requirement group, acceptance/success evidence, design artifact,
contract/data evidence, test level, and prohibited behavior.

Deterministic clock and draft-identifier evidence uses CDI alternatives confined to
`src/test/java`; production configuration contains no clock or identifier test switch. The
production adapters remain the default injectable implementations, while the Quarkus test archive
activates resettable fixed-clock and deterministic-sequence alternatives explicitly.

**Liveness and Readiness**: PostgreSQL/local initialization only for readiness; liveness remains
independent. No Company, identity, gateway/BFF, or SRI check.

**Structured Observability**: Correlation propagates through safe structured logs/traces. Metrics
use bounded labels only. CompanyId is never a metric label. Deadline expiry after response
commitment increments `request_deadline_exceeded_after_response_commit` and records only safe
correlation/audit fields; buyer data, request content, raw idempotency keys, and tokens are excluded.

**Audit Events**: New commit, replay, conflict, persistence unavailable/timeout, significant
rollback, and post-response-commit deadline expiry are observable without buyer/raw-key/payload
data. The late-deadline event contains only correlation identifier, operation, selected response
status, elapsed duration, and optionally an already available CompanyId/draftId under the existing
safe audit policy.

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
  `idempotency-design.md`, `operational-requirements.md`, `traceability.md`, and the blocking
  `governance-nonconformity.md` record.

The requirements-quality content checks are complete. Constitution v2.0.0 is approved on `main`
and `origin/main`, and `6-ft-1` descends from that approved mainline. The design-content rows of the
Constitution Check pass, but overall workflow conformance does not: the recorded pre-analysis
implementation sequence cannot be corrected retroactively. This plan remains eligible for
requirements-quality checklist review but is not eligible for T017 or later implementation while
`GATE-GOV-001` is blocked. It does not itself generate implementation tasks or fabricate owner
approval.
