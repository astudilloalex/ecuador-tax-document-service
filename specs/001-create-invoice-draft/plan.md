# Implementation Plan: Create Invoice Draft

**Branch**: `001-create-invoice-draft` | **Date**: 2026-07-16 | **Spec**: `specs/001-create-invoice-draft/spec.md`

**Input**: Reconciled feature specification with Constitution v2.0.1 and the approved 2026-07-16
normalization, persistence-boundary, timestamp, and canonical-name clarifications

**Implementation progression**: `GATE-GOV-001` retains its **RELEASED** governance status. The
approved `governance-corrective-assignment-addendum.md` assigns red evidence to T017 and V5/green
persistence evidence to T018. A later analysis identified non-governance documentary
inconsistencies in request-time and request-contract semantics. Those inconsistencies are remediated
in the current artifacts, but implementation permission remains `PENDING_SUCCESSFUL_ANALYSIS` until
a new analysis confirms that no CRITICAL finding remains. T017 and T018 remain pending, T018
depends on successful T017, and T019 remains blocked until both complete successfully.

## Summary

Deliver one synchronous internal `POST /api/v1/invoice-drafts` operation. API owns
transport decoding, header validation, request-state initialization, deadline arbitration, and HTTP
mapping. Application owns ordered validation, first-in-Stage-6 emission-point validation followed
by business-text normalization, use-case orchestration, candidate construction, and
transport-neutral outcomes. Domain owns fiscal and
commercial invariants plus deterministic monetary calculation. Infrastructure owns reactive
PostgreSQL access, atomic aggregate and idempotency-binding persistence, and the single
transactional timestamp capture. CompanyId plus a hashed idempotency key defines the durable
concurrency scope. The design has no authentication, authorization, Company Service, Company
master data, fiscal snapshot, cache, or SRI side effect.

## Technical Context

**Language/Version**: Java 25

**Framework**: Quarkus 3.33.2.1 LTS; selected from the production-recommended LTS line and justified
in `research.md`. Completed T001 aligned both `quarkusPluginVersion` and
`quarkusPlatformVersion` to Quarkus `3.33.2.1`. The current `gradle.properties` already contains
the approved identical versions.

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

**Time Boundary**: The earliest API boundary captures one `requestCreationInstant` per request
before body consumption and derives the expected emission date once in `America/Guayaquil`. The
derived date remains fixed when body consumption, validation, or commit crosses midnight. The
resource consumes the captured value and never reads request time again. Separately, the T076
persistence adapter is the sole owner of the creation
timestamp invocation. It invokes the injected transactional clock exactly once inside the active
reactive transaction, after all business validations have succeeded and immediately before root
persistence, and assigns the exact same UTC `java.time.Instant` to `createdAt` and `updatedAt`.
The timestamp-free `InvoiceDraftCandidate` cannot carry either value; T063, Domain, API, and all
mappers neither generate nor replace them. T076 persists both values atomically and returns them
through `PersistedInvoiceDraft` only after commit. Rollback exposes neither value, while replay
returns both originally persisted values without another clock call. Neither value is a PostgreSQL
physical commit timestamp; no `track_commit_timestamp`, post-commit query, or reconstruction is
used. The clock remains injectable and deterministic for tests.

**Scale/Scope**: One Company partition and one Invoice Draft per logical command; no draft update,
delete, fiscal issuance, other tax-document type, or Company administration

**Sensitive Data**: Buyer identity/contact and fiscal payload content are sensitive. Logs,
errors, metrics, traces, fixtures, and idempotency storage exclude personal values, raw keys, and
complete normalized requests. Fingerprints are SHA-256 values with normalization version metadata.

## Source and Terminology Evidence

| Authority | Applicable source and version/path | Requirements or decisions governed |
|-----------|------------------------------------|-------------------------------------|
| Ecuadorian legislation/SRI | SRI Electronic Tax Documents Offline Scheme Technical Sheet v2.32 and SRI electronic-invoicing resources referenced by the spec | Buyer types, IVA treatments, final-consumer threshold, fiscal vocabulary |
| Constitution | `.specify/memory/constitution.md` v2.0.1 | Company request/input prohibition and contracted response allowance; Company-owned aggregate scope/global-catalog exclusion; no identity/Company dependency; architecture, persistence, testing, operations |
| Specification | `specs/001-create-invoice-draft/spec.md`, clarification sessions through 2026-07-17 | Stakeholder goal, external contract, calculations, observable normalization results, all-or-nothing save behavior, timestamp results, failure precedence, and acceptance outcomes |
| Reference baseline | `specs/001-create-invoice-draft/reference-data-baseline.md` | Approved buyer-type, IVA-rule, and payment-method rows; official evidence; target decisions; deterministic UUIDv5 mappings |
| Architecture decisions | This plan and supporting Phase 0/1 artifacts; no separate ADR | Feature-local technical choices |
| Technology authorities | Quarkus release/Java 25 guidance; PostgreSQL 18.4 release/versioning guidance linked in `research.md` | Runtime/database versions |
| Legacy evidence | `docs/legacy/as-is/` paths listed in the spec | Historical discovery only |

**Source Conflicts and Resolutions**: Historical and superseded feature artifacts required
Keycloak, tenant-derived Company context, a Company client/port, status/eligibility checks, and
fiscal snapshots. Constitution v2.0.1 and the clarified spec have higher authority. All such
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

*GATE: Recorded before Phase 0 and re-evaluated after Phase 1. Reconciled again on 2026-07-16
against approved Constitution v2.0.1, the current feature specification, and repository HEAD
`2b72fbdd72aa701101ee232bf2d60caadc9cdca7`.*

| Gate | Pre-Research evidence | Post-Design evidence |
|------|-----------------------|----------------------|
| Constitution version | PASS — v2.0.0 was the approved historical baseline at commit `137d1c8c59cc98402f0a1fed211a6caccad4c883` | PASS — formal PATCH v2.0.1 clarifies Company request/response and owned/global repository scope; `astudilloalex` approved both required capacities and released `GATE-GOV-001` without waiving T017/T018 |
| Greenfield bounded outcome | PASS — one target create/review outcome | PASS — no legacy compatibility or unrelated lifecycle |
| Authority/versioned evidence | PASS — Constitution v2.0.1 and SRI v2.32 identified | PASS — source register separates official facts from target decisions with exact locators |
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
| Testing | PASS — 71 scenarios and 33 success criteria | PASS — traceability maps every FR-001–FR-047, DR-001–DR-024, SC-001–SC-033, and AS-001–AS-071 |
| Operations | PASS — health/correlation required | PASS — PostgreSQL-only readiness, metrics/log/trace/performance budgets defined |
| Simplicity | PASS — no speculative platform/component | PASS — only local ports, datastore, and two justified persisted capabilities |
| Runtime evidence | PASS — JVM mandatory/native optional | PASS — packaged JVM and conditional native evidence paths defined |
| Mandatory Spec Kit workflow | PASS for the pre-task planning evidence available at that point | **APPROVED HISTORICAL NON-CONFORMITY WITH MANDATORY CORRECTION** — T001–T016 preceded a verifiable post-task `$speckit-analyze`; D1–D3 remain approved without erasing the violation; the approved corrective-assignment addendum assigns T017 red evidence and T018 V5/green persistence evidence; `GATE-GOV-001` retains its released status; current documentary remediation awaits a successful new analysis before T017 may begin |

No constitutional complexity exception is requested. A workflow non-conformity exists because
T001–T016 were implemented before the mandatory analysis gate; it is recorded without
retroactive correction in `governance-nonconformity.md`.

**Current implementation gate**: governance approval and the corrective-assignment addendum are
complete, and `GATE-GOV-001` retains its released status. The request-time and request-contract
findings from the latest analysis are remediated documentarily, and all requirements-quality
checklists are evaluated with evidence. Implementation permission remains
`PENDING_SUCCESSFUL_ANALYSIS` until a new analysis confirms that no CRITICAL finding remains. T017
is pending and blocked by that condition; T018 depends on successful T017, and T019 remains blocked
until T017 and T018 both complete successfully.

## Clean Architecture Mapping

```text
HTTP request
  → api: at the earliest boundary capture one requestCreationInstant and start one monotonic
    request deadline before body consumption; parse/validate headers and transport DTOs; forward
    decoded business and identifier values unchanged
  → application: CreateInvoiceDraftCommand(CompanyId, fixed request instant, mapped business
    inputs, idempotency/correlation, fixed RequestDeadline); begin Stage 6 with emission-point
    validation and invoke BusinessTextNormalizer only after that validation succeeds; orchestrate
    ordered validation and invoke Domain operations; construct
    InvoiceDraftCandidate with final local identifiers
  → domain: enforce fiscal/commercial invariants and perform deterministic monetary calculation
  → application outbound persistence port: persist(InvoiceDraftCandidate)
  → infrastructure: reactive Panache/PostgreSQL transaction; capture one Instant for both
    timestamps; return PersistedInvoiceDraft
  → application: transport-neutral result
  → api: terminal arbitration and HTTP mapping
```

| Boundary | Responsibility | Allowed dependencies | Prohibited inputs/types |
|----------|----------------|----------------------|-------------------------|
| `api` | At the earliest boundary, before body consumption, capture one request instant, start the monotonic 10-second race, and initialize request state; exclusively own terminal-result arbitration, the one-response guard, HTTP status/Problem Details mapping, and late-result discard; enforce stages 1–5, including exact Idempotency-Key rules; decode JSON and validate transport representation; forward decoded business and identifier values unchanged | `application` | NFC normalization, business or identifier trimming, UUID canonicalization, space collapse, lowercase text conversion, `canonicalName` construction, persistence entities, Company/security clients, or deadline/HTTP/header responsibilities delegated below API |
| `application` | Receive mapped CompanyId, the fixed request instant, raw decoded business/identifier values, and a neutral RequestDeadline only for cooperative budget checks; begin Stage 6 by trimming/validating/canonicalizing `emissionPointId` once and, only after that succeeds, invoke `BusinessTextNormalizer` exactly once for every supplied applicable business-text value; derive/validate canonical values; orchestrate the approved validation sequence and invoke Domain invariants/calculation without reimplementing them; allocate all local draft/child identifiers through `DraftIdentifierGenerator`; construct `InvoiceDraftCandidate`; call the persistence port; return transport-neutral outcomes | `domain`, Mutiny, application ports | Independent implementation or recalculation of Domain fiscal/commercial invariants or monetary rules; HTTP headers/requests/status/exceptions/envelopes; terminal arbitration; request- or persistence-clock invocation; timestamp construction; `SecurityIdentity`; `JsonWebToken`; thread-local/Gateway objects |
| `domain` | Receive normalized values; own immutable CompanyId on Invoice Draft; exclusively enforce buyer/line/tax/payment fiscal and commercial invariants and perform exact deterministic monetary calculation when invoked by Application | Java/approved domain libraries | Unicode normalization mechanics, use-case orchestration, HTTP/JSON/Quarkus/Panache/PostgreSQL/Mutiny/security types |
| `infrastructure` | Persist exactly the supplied normalized/canonical values and final local identifiers; implement transport-neutral local repository/catalog/clock/identifier ports with reactive PostgreSQL/Panache; clamp work to remaining budget; let the T076 transaction own the sole persistence-clock invocation and return committed state as `PersistedInvoiceDraft` | application ports/domain types | Independent NFC/trim/collapse/lowercase/canonical derivation, identifier replacement, HTTP status/exception/envelope/arbiter, Company/SRI/security adapter, shared database, cache |

Actual outbound boundaries are limited to the Invoice Draft repository, local identification/tax/
payment catalog access, clock, and identifier generation. No interface named or equivalent to
`CompanyContextPort`, `ResolveCompanyFiscalContextPort`, or Company authorization/eligibility is
introduced.

### Application Persistence Boundary

The conceptual creation operation on the application-owned persistence port is:

```text
persist(InvoiceDraftCandidate) -> Uni<PersistedInvoiceDraft>
```

`InvoiceDraftCandidate` is the complete Application-to-persistence input for a logically new
draft. Application constructs it only after Stage 6 normalization, all business validation, and
monetary calculation. It carries the final root and child UUIDs allocated by Application through
`DraftIdentifierGenerator`, all normalized/validated/calculated business values, authoritative
CompanyId, and the already derived key hash, request fingerprint, and normalization version needed
for the atomic binding. It carries no raw idempotency key or header representation. It contains no
`createdAt`, `updatedAt`, null/zero/placeholder/provisional timestamp, HTTP type, commit metadata,
or database/Panache entity.

`PersistedInvoiceDraft` is the persistence-to-Application result after successful commit. It
contains the same final root identifier, the persisted business values needed by the application
result, and both timestamps. The persistence adapter must not generate or replace identifiers;
Application is their only allocation owner. Conversely, Application must not generate either
timestamp; T076 is their only invocation and assignment owner.

The persistence port and its results are transport-neutral. They expose no HTTP request, status,
exception, response envelope, or persistence entity. A persistence mapper may copy candidate
values into Panache entities, copy the one adapter-owned Instant into the timestamp columns, and
reconstruct `PersistedInvoiceDraft` from committed state; it must not normalize text, derive
canonical values, allocate identifiers, invoke an additional clock, or map HTTP failures.

### Authoritative Creation Sequence

For a logically new request, the architectural sequence is exactly:

1. Before body consumption, API captures the fixed request instant, derives the Ecuadorian request
   date, initializes request-local state, and starts the one request deadline.
2. API performs FR-041 stages 1–5: body-size enforcement; Company, correlation, and idempotency
   header validation; then JSON decoding and representation validation. It forwards decoded
   `emissionPointId` and business text unchanged.
3. Application begins Stage 6 by trimming and validating `emissionPointId`, emitting the stable
   neutral `EMISSION_POINT_INVALID` violation when the decoded string is blank after trim,
   malformed, or nil, and otherwise producing its canonical UUID. Only after that check passes,
   Application invokes `BusinessTextNormalizer` exactly once for each supplied applicable business
   text value.
4. Application performs Company-scoped idempotency stages 7–9, then invokes Domain for Stage 10
   fiscal/commercial invariants and Stage 11A/11B deterministic calculation and validation. Domain
   calculates the deterministic monetary values; Application does not independently implement or
   recalculate those rules.
5. Application obtains final local draft and child identifiers through `DraftIdentifierGenerator`
   and constructs `InvoiceDraftCandidate`.
6. Application passes the candidate to the persistence port.
7. The persistence adapter opens or joins the bounded reactive transaction.
8. The persistence adapter invokes the injected transactional clock exactly once.
9. The persistence adapter assigns that same `Instant` to `createdAt` and `updatedAt`.
10. The persistence adapter persists the complete aggregate and idempotency binding atomically.
11. The persistence adapter returns `PersistedInvoiceDraft` after successful commit.
12. Application maps the persisted result to a transport-neutral result.
13. API arbitrates the terminal result and maps the accepted result to HTTP.

The adapter invokes the clock only after all business validations have succeeded and immediately
before the new root is persisted. The reactive transaction boundary encloses clock invocation,
root, children, calculated values, and binding writes. A rollback exposes no created resource and
neither timestamp. No mapper or layer outside T076 may add a missing identifier or timestamp.

**Failure-Precedence Ownership**: The API adapter alone owns the deadline race and all HTTP
semantics. FR-041 stages 1–5 use one explicit pre-application pipeline:

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
3. After that gate passes, the API decodes one JSON object and an API-owned
   `InvoiceDraftRequestPropertyClassifier` inspects its complete tree before DTO binding. Malformed,
   unsupported, or non-object input returns `INVALID_REQUEST`. For a decoded object, the classifier
   recognizes exactly the calculated-property path set from FR-012: any match returns
   `PROHIBITED_CALCULATED_FIELD` before ordinary unknown/prohibited, missing-property, or type
   classification, regardless of supplied value/type and without a `violations` array. Only after
   the scan finds no calculated path do Jackson DTO binding and Bean Validation complete ordinary
   representation validation. `quarkus.jackson.fail-on-unknown-properties=true` remains a defensive
   strict-body barrier, and the
   built-in Jackson mismatched-input mapper is disabled with
   `quarkus.rest.exception-mapping.disable-mapper-for=io.quarkus.resteasy.reactive.jackson.runtime.mappers.BuiltinMismatchedInputExceptionMapper`
   so the feature mapper returns the approved stable response.

After API stages 1–5 succeed, Application owns Stage 6 in a fixed internal order. It first trims
surrounding ASCII SP/HTAB from the decoded `emissionPointId` once, rejects blank, malformed, or nil
UUID text with transport-neutral `BUSINESS_VALIDATION_FAILED` / `EMISSION_POINT_INVALID`, and
canonicalizes a valid UUID. It then performs general business-text normalization and canonical
length validation. Missing `emissionPointId` or a non-string JSON representation never reaches
Application and is the Stage-5 `INVALID_REQUEST` outcome. Neither failure exposes the rejected
value, and the API maps only the terminal outcome accepted by its deadline arbiter.

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

The API handler races the application `Uni` and deadline signal, atomically accepts exactly one
terminal result, maps HTTP only after that arbitration, and discards a late application/database
result so it cannot produce a second response. Timeout wins when expiry becomes terminal before the
application outcome is accepted. HTTP `504`, Company `400`, and every other status/envelope mapping
exist only in API code.

The same fixed, transport-neutral deadline context is mapped into `CreateInvoiceDraftCommand`
solely for cooperative cancellation and remaining-budget checks;
application work checks it before asynchronous stages, and every aggregate and reference-data
repository call receives an explicit remaining `Duration`. Each reactive adapter clamps pool
acquisition and every query subscription to the minimum of configured database-operation timeout
and that remainder, starts no work when it is zero or negative, and additionally clamps a write
transaction to the lesser of the effective budget and five seconds. A configured database timeout
becomes a transport-neutral persistence failure while request budget remains. Repositories never
map `503`/`504`; only API maps the accepted neutral outcome after terminal arbitration.

Before response commitment, the atomic selection rules govern. Once the HTTP response is committed,
deadline expiry is telemetry-only: it cannot change status, emit an alternate response/body, start
another database or domain mutation, or compensate/delete a committed draft or binding. Existing
serialization may complete normally, and the safe
`request_deadline_exceeded_after_response_commit` event/counter is recorded. Equivalent replay
resolves any response loss.

The earliest API deadline/request-state boundary calls the application `RequestClock` exactly once
before body consumption, derives the immutable request instant, and stores it in request-local API
state. The resource and mapper consume that captured value after stage 5 without another clock
read; it is carried by `CreateInvoiceDraftCommand` and is not reused as either persistence
timestamp. Application orchestration begins at Stage 6 by trimming, validating, and canonicalizing
the raw `emissionPointId` exactly once. Only after that validation succeeds does it invoke
`BusinessTextNormalizer` exactly once for each supplied applicable business-text value; API has not
normalized either category. It continues
through replay/conflict, Stage 10 independent
validation, Stage 11A calculation, exact Stage 11B calculated-value validation, Application-owned
local identifier allocation, timestamp-free candidate construction, and persistence delegation.
For a logically new command only, T076 calls the injected persistence clock exactly once inside its
active transaction after all validations succeed and immediately before root persistence, then
assigns that one UTC Instant to both `createdAt` and `updatedAt` and to binding creation time where
required. T063 never calls that clock or supplies, replaces, or overwrites either timestamp; a
second persistence-clock invocation in one attempt is prohibited. There is no post-commit
clock/database timestamp read. HTTP headers, Vert.x/REST request state, and payload
representations never enter application or domain logic.

**Business Validation and Calculation Stages**:

- Stage 10 validates only calculation-independent buyer, identification syntax, line/cardinality,
  product/description, quantity/unit-price scale/sign, tax catalog/applicability, payment-method
  existence/activity/`emissionDate` effectiveness, payment structure/basic amount format,
  text/catalog/structural rules.
- Stage 11A deterministically calculates line gross, discount, taxable base, taxes, line totals,
  subtotal aggregates, invoice taxes/total, and the payment-reconciliation reference total.
- Stage 11B runs only after 11A and in this order: calculated range/overflow (lowest line, canonical
  tax group, aggregate); discount-over-gross by lowest line; final-consumer total limit;
  total-dependent payment shape/positivity; exact payment reconciliation. A future derived rule
  requires an approved deterministic insertion point.

Deadline precedence remains cross-cutting and API-owned; it does not allow Stage 10 to perform a
calculated-value check or permit Stage 11B to run before Stage 11A.

## Reactive and Resource Boundary Design

| Operation | Adapter/port boundary | Classification | Execution context | Timeout/resource bound | Required evidence |
|-----------|-----------------------|----------------|-------------------|------------------------|-------------------|
| Header/body mapping, correlation, deadline, and time initialization | API → application mapping | Non-blocking bounded CPU | Event loop | 2 MiB, bounded fields/collections; one monotonic 10 s deadline; one request instant | Controlled no-sleep payload/header deadline races; oversized absent/valid/invalid correlation with zero database calls; expiry/cancellation/post-commit telemetry; midnight vector; max payload; blocked-thread check |
| Business-text normalization, canonical derivation, and fingerprint generation | Application `BusinessTextNormalizer` plus canonical fingerprint encoder | Bounded CPU, no I/O | Calling context | One Stage-6 normalizer invocation per supplied applicable value; ≤2 MiB; canonical-name maximum 300 Unicode code points; SHA-256 version 1 | Invocation-count, Unicode/`U+0130`, canonical-overflow, fingerprint vectors, and maximum-payload benchmark |
| Monetary/domain calculation | Pure domain | Synchronous deterministic bounded CPU | Calling context | ≤500 lines | Exact vectors, p99 budget, no event-loop warning |
| Binding/root lookup | Repository port/reactive adapter | Non-blocking database I/O | Reactive PostgreSQL client | Pool/query clamped to request's remaining budget | Unavailable, deadline, replay/conflict tests |
| Buyer/IVA/payment reference lookup | ReferenceDataPort/reactive adapter | Non-blocking database I/O | Reactive PostgreSQL client | Every invocation receives remaining `Duration`; pool/query subscription uses min(configured timeout, remainder); no work at zero/negative remainder | Both minimum branches and mappings (configured timeout→503, shared deadline→504), exhausted-before-call, expiry-during-lookup, cancellation, and zero-state tests |
| Aggregate/binding write | `persist(InvoiceDraftCandidate) -> Uni<PersistedInvoiceDraft>` / reactive adapter | Non-blocking database I/O | One opened-or-joined reactive transaction | Lesser of remaining request budget and 5 seconds | Timestamp-free candidate, final Application-owned identifiers, single clock call, equal creation timestamps, rollback phase injection, deadline, uncertain-commit recovery, and concurrency |
| Flyway startup migration | Startup infrastructure | Blocking lifecycle operation outside requests | Controlled startup | Deployment timeout/empty-db evidence | Migration and readiness tests |

No blocking filesystem/network operation, SOAP, XML, signature, certificate, SRI, or Company call
exists. A reactive wrapper is not evidence that an underlying operation is non-blocking.

## Company Context and Sensitive-Data Design

**Internal Caller Boundary**: No operation has application authentication or authorization. The
OpenAPI contract defines no security scheme/requirement, Authorization header, `401`, or `403`.

**Company Header Contract**: Exactly one `X-Company-Id` is required. Trim surrounding ASCII SP/HTAB
once; reject blank/missing as
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
after Company validation and before idempotency-key validation. Preserve one value after one
surrounding ASCII SP/HTAB trim when it has
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

Constitution v2.0.1 formally governs the request/input prohibition, explicitly contracted response
allowance, Company-owned aggregate/binding enforcement, and immutable-global-catalog exclusion.
Client request data can never override authoritative header context.

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
- Stage 5 first decodes one JSON object. Malformed/unsupported/non-object representations use
  `INVALID_REQUEST`; an API-owned pre-binding classifier then scans the exhaustive case-sensitive
  calculated-property paths from FR-012. Any match uses `PROHIBITED_CALCULATED_FIELD`, regardless of
  value/type, and precedes ordinary unknown/prohibited, missing-property, and property-type outcomes.
  Multiple matches produce the same top-level result with no `violations` array. Only a decoded
  object with no calculated-path match proceeds to strict request/input-schema rejection of
  `companyId`, `issuerId`, fiscal/snapshot data, and other unknown properties as `INVALID_REQUEST`.
  The response's explicitly contracted canonical `companyId` does not weaken the input prohibition.
- The exhaustive calculated paths are `/taxTotals`, `/subtotalBeforeTaxes`, `/totalDiscount`,
  `/grandTotal`; and, for every zero-based line index `{i}`, `/lines/{i}/grossAmount`,
  `/lines/{i}/netAmount`, `/lines/{i}/lineTotal`, `/lines/{i}/tax`, `/lines/{i}/taxBase`,
  `/lines/{i}/taxAmount`, `/lines/{i}/taxCode`, `/lines/{i}/taxRate`,
  `/lines/{i}/officialTaxCode`, `/lines/{i}/officialPercentageCode`, and `/lines/{i}/rate`.
  `/taxTotals` and `/lines/{i}/tax` classify their complete supplied subtrees.
- Request `emissionPointId` is a decoded string without a wire-level UUID pattern that could
  preempt Stage 6. API forwards it unchanged; Application trims surrounding ASCII SP/HTAB once,
  rejects blank/malformed/nil UUIDs, and supplies the canonical lowercase hyphenated value. The
  response schema enforces
  `^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$` for every new and replay result.
  Contract metadata and tests enumerate four accepted request forms—already canonical, uppercase,
  surrounding SP, and surrounding HTAB—and bind each to its exact lowercase-hyphenated response.
  They also enumerate absent/non-string Stage-5 failures and empty, SP/HTAB trim-to-empty,
  malformed, nil, internal-SP, internal-HTAB, braced, and non-hyphenated Stage-6 failures. No
  request-schema UUID pattern may preempt these Stage-6 cases.
- Response includes canonical `companyId`, local draft `id`, opaque `emissionPointId`, complete
  commercial/calculated draft, `createdAt`, and `updatedAt`.
- New commit returns `201`; equivalent replay returns `200`; both identify replay state.
- Stable statuses/codes are defined in `error-catalog.md` and represented in OpenAPI.
- Monetary envelope violations use `BUSINESS_VALIDATION_FAILED` with violation code
  `MONETARY_RANGE_EXCEEDED`; API, calculation, persistence, response, and test limits are identical.
- Failure evaluation follows FR-041 exactly, with `REQUEST_TIMEOUT` as a cross-cutting arbiter:
  conclusive stage outcome before expiry wins, otherwise `504` wins, and no later signal replaces
  the selected result.
- Pairwise evidence covers calculated-path and ordinary unknown-property Stage-5 outcomes against
  both emission-point failure and the concrete Stage-6 general-text outcome
  `CANONICAL_NAME_TOO_LONG`. Calculated-path classification wins every decoded-object competition
  in which such a path exists; otherwise the ordinary Stage-5 outcome wins before Stage 6. Within
  Stage 6, emission-point rejection wins before canonical overflow. Controlled deadline vectors
  race each of those outcomes, Stage-10
  `EMAIL_INVALID`, and Stage-11B calculated failures independently: the stage result wins only
  when accepted before expiry; otherwise `REQUEST_TIMEOUT` wins. Exactly one terminal response is
  observable in every row.
- T085 defines and accepts exactly one terminal outcome after deadline arbitration. T087 depends on
  T085 and maps only that already accepted transport-neutral outcome to HTTP; T087 neither races
  outcomes nor reopens terminal arbitration.

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
`^[A-Za-z0-9]{1,20}$`. API decodes and forwards these business values unchanged. At Stage 6,
Application performs their one permitted leading/trailing ASCII SP/HTAB trim, changes no internal
character or case, performs no case folding or Unicode normalization, and validates the exact
ASCII expression. Valid/invalid vectors are `ABC123`/`sku9` versus
`ABC-123`/`ÁBC1`/26 characters for product codes, and `A1234567`/`EC9Z` versus
`A-123`/`Á123`/21 characters for buyer values.

The standard OpenAPI request schema accepts a decoded string representation and does not reject a
raw value that Application is permitted to trim. Contract metadata `x-application-stage-6`
documents Application-owned SP/HTAB trimming, normalized bounds, and the exact normalized regex.
Response schemas may enforce the exact persisted representation pattern. Production Java
validation, domain invariants over accepted normalized values, and locale-independent PostgreSQL
defenses must implement their own layer responsibilities. PostgreSQL POSIX `[[:alnum:]]` is not
equivalent to these ASCII expressions. The approved corrective evidence sequence is:

1. T017 defines `src/test/resources/invoicedraft/ascii-validation-vectors.json` as the one
   authoritative fixture.
2. T017 validates fixture integrity and prepares intentional red PostgreSQL/Flyway evidence that
   exposes the known V3 mismatch. It creates no migration and does not modify V3.
3. T018 creates `V5__tighten_invoice_draft_ascii_constraints.sql`, guided by T017's failing
   evidence and preserving immutable V3.
4. T018 makes the PostgreSQL/Flyway evidence green, proves the V3-to-V5 upgrade, and runs Flyway
   validation.
5. T030 independently validates request-contract ownership and `x-application-stage-6` metadata
   using raw and normalized expectations from the fixture; it does not invoke the database or
   domain validators and does not make the standard request schema preempt Stage 6.
6. T045 independently validates the production buyer-identification Java behavior against the
   fixture and does not invoke PostgreSQL, Flyway, OpenAPI parser, or HTTP infrastructure.
7. T050 independently validates production product-code and text-rule Java behavior against the
   fixture under the same dependency restrictions.
8. The complete set of independent layer-specific results establishes cross-layer equivalence.

T017 may use a standalone Java `Pattern` only to verify fixture parsing or the approved literal
regular expression; productive Java-validator equivalence does not exist during T017 or T018.
Domain test suites remain free of PostgreSQL, Flyway, OpenAPI parser, and HTTP transport
dependencies.

The ASCII fixture represents request-to-storage evidence rather than one ambiguous literal value.
Each request-pipeline entry contains `id`, `field`, `identificationType` when applicable,
`rawValue`, `applicationNormalizedValue`, `expectedApplicationOutcome`, `expectedStoredValue`,
`expectedErrorCode`, `failureStage`, `rationale`, and `consumers`. Allowed `failureStage` values are
`NONE`, `TRANSPORT_REPRESENTATION`, `APPLICATION_STAGE_6`, and `PERSISTENCE_DEFENSE`.
`rawValue` is the exact decoded string forwarded unchanged by API and may contain surrounding ASCII
SP or HTAB. `applicationNormalizedValue` is the result of the one Application-owned surrounding
SP/HTAB trim; these ASCII fields receive no NFC pass, case fold, internal-character change, or
internal-whitespace collapse. It is `null` when transport decoding fails. The
`expectedApplicationOutcome` applies the relevant normalized expression—`^[A-Za-z0-9]{1,20}$` for
buyer types `06`/`08` or `^[A-Za-z0-9]{1,25}$` for product code. `expectedStoredValue` is the exact
accepted normalized value and is `null` for every rejected request. Successful PostgreSQL
request-pipeline probes use `expectedStoredValue`, never `rawValue`.

Dedicated persistence-defense entries additionally contain `storedProbeValue` and
`expectedPersistenceOutcome`, with `failureStage: PERSISTENCE_DEFENSE`. They prove that PostgreSQL
rejects invalid stored representations without implying that Infrastructure performs Application
normalization. Required vectors prove that raw `" ABC123 "` and HTAB-surrounded valid values trim
to accepted `"ABC123"`; internal SP or HTAB remains invalid; persistence accepts the normalized
value but rejects direct surrounding/internal-whitespace probes; accented Unicode letters remain
invalid after trimming; empty and trim-to-empty inputs fail in Application; normalized over-maximum
values fail after trimming; and every consumer selects the correct stage value. T017 owns fixture
structure/red PostgreSQL evidence, T018 owns final PostgreSQL/Flyway behavior over stored/probe
values, T030 owns request-contract metadata, T045 owns buyer-validator behavior over
`applicationNormalizedValue`, and T050 owns product-validator behavior over that same stage value.

General human-readable single-line text reaches Application exactly as API decoded it. Stage 6
begins with the emission-point trim/validation/canonicalization defined above. Only after that
succeeds, Application alone invokes `BusinessTextNormalizer` exactly once for each supplied
applicable value. The invocation normalizes to NFC; trims surrounding `U+0020`; rejects
categories `Cc`, `Cf`, `Cs`, `Co`, `Cn`, `U+2028`, and `U+2029`; accepts only `U+0020` as spacing;
rejects tab, CR, LF, NBSP, and all other separators; preserves internal punctuation, internal
`U+0020` runs, and display case; and counts display length in Unicode code points. Comparison is
case-sensitive unless a field overrides it. Assigned emoji category `So` is accepted when field
format/length permits. Domain and Infrastructure receive the normalized values and never repeat
the transformation.

Within that same Stage-6 invocation, additional-information `canonicalName` reuses the normalized
display value, collapses internal `U+0020` runs, lowercases with Java `Locale.ROOT`, and is then
counted. The accepted result contains 1–300 Unicode code points, is never truncated, and is
persisted and used for uniqueness/idempotency. Application rejects overflow before fingerprinting,
Domain entry, or persistence with `BUSINESS_VALIDATION_FAILED` /
`CANONICAL_NAME_TOO_LONG`, identifying the original field, maximum `300`, counting unit
`UNICODE_CODE_POINTS`, and stage `CANONICALIZATION`. PostgreSQL is only a defensive stored-length/
nonempty/canonical-value barrier; it neither reproduces Java normalization nor recalculates using
database locale. OpenAPI documents the policy and Application performs it. Independent
layer-specific suites consume one authoritative fixture while selecting their stage-appropriate
value and responsibility; they do not all validate the same literal or perform the same
transformation. Coverage includes accented/decomposed text, spaces, prohibited code
points/separators, emoji, case, code-point boundaries, and `U+0130` lowercase expansion at 150/151
occurrences.

T020 owns the one authoritative general-text fixture at
`src/test/resources/invoicedraft/unicode-text-validation-vectors.json`. Each entry contains `id`,
`fieldCategory`, `rawValue`, `applicationNormalizedValue`, `canonicalValue` when applicable,
`expectedStage6Outcome`, `expectedBusinessValidationOutcome`, `expectedApplicationOutcome`,
`expectedDomainInput`, `expectedStoredValue`, `expectedErrorCode`, `failureStage`, `rationale`, and
`consumers`; `failureStage` is `NONE`,
`TRANSPORT_REPRESENTATION`, `APPLICATION_STAGE_6`, or `PERSISTENCE_DEFENSE`. Dedicated
persistence-defense entries also contain `storedProbeValue` and `expectedPersistenceOutcome`.
API consumers use `rawValue` only for Unicode/JSON
decoding, malformed-representation rejection, and unchanged forwarding; they perform no business
normalization or length validation. Application consumers perform exactly one NFC pass, surrounding
`U+0020` trim, prohibited-code-point checks, display limits, canonical derivation, `Locale.ROOT`
canonical-length validation, `CANONICAL_NAME_TOO_LONG`, and no truncation. Domain consumers receive
only entries whose `expectedStage6Outcome` is `ACCEPTED`; `expectedDomainInput` is the resulting
normalized value and is `null` for every Stage-6 rejection. A Stage-6-accepted buyer email can still
have `expectedBusinessValidationOutcome: EMAIL_INVALID` and final
`expectedApplicationOutcome: REJECTED`; Domain/T045 then apply only the Stage-10 email grammar to
that non-null input and never repeat normalization. `expectedApplicationOutcome` is the final
ordered Application result, not a synonym for Stage-6 acceptance. Infrastructure/PostgreSQL
consumers use only `expectedStoredValue` and dedicated persistence-defense probes for stored length,
nonempty, required canonical, relational, and uniqueness defenses; PostgreSQL does not claim Java
NFC, `Locale.ROOT`, or prohibited-code-point validation.

The fixture covers NFC accented Latin text; decomposed/composed equivalence; surrounding and
repeated internal `U+0020`; tab, CR, LF, NBSP, `U+2028`, `U+2029`, and zero-width `Cf`; accepted
assigned `So` emoji; preserved display case; code-point boundaries; and `U+0130` canonical
expansion at 150 accepted and 151 rejected occurrences with `CANONICAL_NAME_TOO_LONG` and no
truncation. T026 consumes Stage-6-accepted `expectedDomainInput`, including email values that Stage
10 is expected to accept or reject; T029 consumes and proves Stage-6 values/outcomes;
T030 verifies OpenAPI ownership/metadata; T033 consumes `rawValue` for decoding and unchanged
forwarding; and T036 consumes stored/probe values for defensive persistence evidence. T020 may also
create `idempotency-v1-vectors.json`, but those vectors reference or consume this Unicode fixture
instead of redefining normalization cases; T028 is therefore an indirect consumer through T020.
Independent layer-specific suites establish equivalence by selecting the stage-appropriate fixture
value and responsibility, not by validating the same literal or repeating transformations.

Buyer email uses a stricter post-normalization ASCII profile. `BusinessTextNormalizer` performs the
one ordinary NFC and surrounding-`U+0020` pass; then Stage 10 validates the normalized value against
the exact specification/OpenAPI pattern
``^(?=.{1,254}$)(?=[^@]{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$``.
The local part is a 1–64-character dot-atom over ASCII
letters, digits, and ``!#$%&'*+/=?^_`{|}~-``; dots separate nonempty atoms. The domain has at least two
labels, each 1–63 ASCII letters/digits/hyphens with alphanumeric ends, and the complete address is at
most 254 ASCII code points. Case is preserved and compared case-sensitively; no component folds case,
re-normalizes, trims again, or truncates. Quoted local parts, comments, domain literals,
internationalized addresses, internal whitespace, and multiple-address forms return transport-
neutral `BUSINESS_VALIDATION_FAILED` with value-free `EMAIL_INVALID` for `buyer.email`.

T020 owns the email entries in `unicode-text-validation-vectors.json`. T033 proves raw API forwarding;
T029 proves the single general-text normalization and passes only its result onward; T030 proves the
OpenAPI metadata; T026 consumes Stage-6-accepted normalized Domain inputs and applies the Stage-10
expected result; T045 tests the actual production buyer-email validator against the same expected
Stage-10 outcomes; T036 persists only finally accepted stored values
and does not claim to reproduce the email grammar. Required vectors cover valid dot-atoms, preserved
case, the one-character local/domain-label minima and explicit empty-local/empty-label rejection,
every permitted local punctuation character,
surrounding `U+0020`, leading/trailing/consecutive local dots, a trailing domain dot,
quoted/comment/domain-literal forms, internal whitespace and prohibited separators, decomposed input
that NFC converts to non-ASCII, non-ASCII local/domain text, multiple addresses, local-part 64/65,
domain-label 63/64, and total-length 254/255. For an email rejected only by the Stage-10 grammar,
`expectedStage6Outcome` is `ACCEPTED`, `expectedDomainInput` contains the normalized value,
`expectedBusinessValidationOutcome` is `EMAIL_INVALID`, and `expectedApplicationOutcome` is
`REJECTED`; a Stage-6 text rejection has no Domain input and never reaches the email grammar.

**Payment-Method Effectiveness**: Reference lookup receives `(paymentMethodId, emissionDate)` and
requires existence, activity, `effectiveFrom <= emissionDate`, and `effectiveTo IS NULL OR
emissionDate <= effectiveTo`. Both finite boundaries are inclusive. Server current date, request
arrival, transaction timestamp, and `createdAt` are never used.

## Data and External Consistency Design

| Boundary/command | Intermediate states | Retry/idempotency | Duplicate handling | Timeout | Recovery/reconciliation | Observable outcome |
|------------------|---------------------|-------------------|--------------------|---------|-------------------------|--------------------|
| Header/body acceptance | No durable state | Payload → Company → correlation → key → body precedence; each result must become conclusive before expiry | N/A | One monotonic 10 s deadline begins before body consumption and is never reset | 413/400 when classification wins; 504 when deadline wins; no later work after selection | 400/413/504 or continue |
| Binding/reference lookup | No new durable state | CompanyId + key hash; fingerprint/version compare; payment lookup uses invoice emissionDate | Equivalent replay; different content conflict | Each call receives remainder and clamps pool/query subscription to min(configured timeout, remainder) | Same key safely retried; no DB call at exhausted budget; adapters return neutral outcomes | API maps accepted neutral outcome to 200/409/422/503/504 or continues |
| New aggregate write | Tentative root/children/binding inside one transaction | Binding inserted in same commit | Unique Company/key hash arbitrates race | Lesser of remaining budget and 5-second write ceiling | Confirmed rollback leaves zero state; unresolved-at-expiry is uncertain; replay reconciles; loser rolls back and re-reads winner | 201/200/409 or safe 503/504/500 |
| Response delivery | Commit already authoritative and success selected | Same equivalent retry | Returns original | Timer remains armed through serialization; after HTTP commitment expiry is telemetry-only | No status/body rewrite or compensation; serialization may finish; binding reconciles response loss | Selected response or connection-level response loss; original draft replayable |

There is no external-system boundary and no claim of atomicity beyond local PostgreSQL.

## Persistence and Idempotency Design

Detailed schema/constraint/transaction decisions are in `data-model.md` and
`persistence-design.md`. Detailed hashing/canonicalization/replay decisions are in
`idempotency-design.md`.

The creation handoff is the conceptual application-port operation
`persist(InvoiceDraftCandidate) -> Uni<PersistedInvoiceDraft>`. For equivalent replay, the
Company-scoped repository retrieves the committed `PersistedInvoiceDraft`; Application returns its
original root identifier, `createdAt`, and `updatedAt`. Replay does not allocate another identifier,
invoke the transactional clock, rebuild canonical values, construct a replacement aggregate, or
mutate persisted state. The incoming retry still undergoes its required Stage-6 emission-point
validation followed by the one business-text normalization pass to produce a comparable
fingerprint; after equivalence is established, no persisted canonical value is reconstructed.

Key invariants:

- root `company_id uuid NOT NULL` and non-nil;
- root `created_at timestamptz NOT NULL` and `updated_at timestamptz NOT NULL` both store the exact
  same immutable UTC Instant on initial creation, captured by T076's single clock call inside the
  active write transaction after validation and immediately before persistence; response and
  replay return both original values with no physical-commit-timestamp query; T063 cannot supply or
  overwrite either value;
- Application owns final local root/child identifier allocation through `DraftIdentifierGenerator`
  before constructing the candidate; persistence copies but never generates or replaces them;
- quantity/unit price columns use `numeric(12,6)`; monetary columns use `numeric(17,2)`; tax-rate
  columns use `numeric(5,2)` with range checks mirrored by pre-persistence validation;
- final schema after immutable V3 then pending V5 uses the exact locale-independent ASCII
  product/buyer expressions above; T017 creates no migration and first supplies the authoritative
  fixture/red evidence; T018 alone creates V5, replaces only the affected constraints, and makes
  the PostgreSQL/Flyway evidence green;
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
├── governance-corrective-assignment-addendum.md
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
└── support/  # FixedRequestClock and DeterministicDraftIdentifierGenerator from T077–T078

src/test/resources/
├── application.properties
└── invoicedraft/
    ├── ascii-validation-vectors.json
    ├── unicode-text-validation-vectors.json
    ├── idempotency-v1-vectors.json
    └── calculation-vectors.json
```

The established feature test-resource convention is
`src/test/resources/invoicedraft/`, so the authoritative ASCII fixture is
`src/test/resources/invoicedraft/ascii-validation-vectors.json`, owned by T017 and consumed by
T017, T018, T030, T045, and T050 with the stage-specific fields and responsibilities defined above.
The authoritative Unicode fixture is
`src/test/resources/invoicedraft/unicode-text-validation-vectors.json`, owned by T020 and consumed
directly by T026, T029, T030, T033, T036, and T045—including its exact buyer-email vectors—with
T028 consuming it indirectly through T020's idempotency vectors. Both fixtures are planned here and are not created by this documentary
reconciliation.

Composition remains outside the domain. No `company`, `security`, Company-client, or cache package
is planned.

## Test and Operational Evidence Plan

| Requirement/risk | Level/environment | Observable invariant | Required negative/boundary evidence |
|------------------|-------------------|----------------------|-------------------------------------|
| Company header/canonicalization | API + application | Valid/mixed-case UUID maps/stores/returns canonical CompanyId | missing/blank/malformed/nil/repeated; Company request body/input/path/query rejected while explicit response CompanyId remains present |
| Clean layer handoff | Architecture + application | Command has explicit CompanyId and earliest-boundary request instant; API forwards decoded business text and `emissionPointId` unchanged; Application alone normalizes at Stage 6 and constructs the timestamp-free candidate; aggregate has immutable CompanyId | no HTTP/security/thread-local/Gateway types below API; no API normalization or UUID parsing; no Domain/Infrastructure re-normalization |
| No Company/security integration | Architecture/config/runtime trace | zero Company/auth calls/dependencies/spans | no Company existence/status/tenant/emission ownership tests |
| Draft business rules | Domain/application | exact Stage 6 emission-point SP/HTAB trim/UUID canonicalization plus general-text normalization → Stage 10 → Stage 11A → ordered Stage 11B; payment effective on emissionDate; Application-owned normalization and local identifiers; Domain consumes only accepted normalized inputs | one normalization pass/value; emission-point surrounded/malformed/nil vectors; pre/post-calculation competing failures; payment inclusive/open/inactive/ineffective vectors; exact Unicode/`U+0130` vectors; 300-code-point canonical limit; numeric maxima/overflow and midnight/replay |
| Stage-5 calculated-property classification | API contract/integration | decoded-object scan uses the exhaustive path set and selects `PROHIBITED_CALCULATED_FIELD` before generic schema binding; malformed/non-object input remains `INVALID_REQUEST` | every exact path; null/wrong-type/equal-value; multiple calculated paths; calculated plus unknown/prohibited/missing/wrong-type; no match plus unknown; no rejected value or violations array |
| ASCII request-to-storage equivalence | Independent fixture consumers: T017/T018 PostgreSQL/Flyway, T030 OpenAPI, T045/T050 production Java | Every suite consumes `ascii-validation-vectors.json` but selects its stage value: raw/normalized contract metadata, `applicationNormalizedValue`, `expectedStoredValue`, or `storedProbeValue`; no domain suite imports transport/database infrastructure | surrounding SP/HTAB trim accepted in Application; internal whitespace, Unicode, punctuation, empty, trim-to-empty, min/max and over-limit vectors; direct invalid storage probes; standalone `Pattern` in T017 limited to literal-regex/fixture verification; no productive Java claim in T017/T018 |
| Unicode text and buyer-email ownership | T020 fixture ownership; T026 Domain, T029 Application, T030 OpenAPI, T033 API, T036 PostgreSQL, T045 production buyer validator | Independent suites consume `unicode-text-validation-vectors.json` and select `rawValue`, `expectedStage6Outcome`, non-null Stage-6-accepted `expectedDomainInput`, `expectedBusinessValidationOutcome`, final `expectedApplicationOutcome`, or stored/probe values according to their boundary; T045 alone proves the production email grammar and never receives a Stage-6-rejected input | malformed representation; NFC composition; spaces/separators/prohibited categories; emoji; case; code-point limits; `U+0130` 150/151 expansion; `CANONICAL_NAME_TOO_LONG`; exact email dot-atom, one-character minima, every permitted punctuation character, trailing domain dot, decomposed-to-non-ASCII, quoted/comment/literal/multiple-address and 64/63/254 boundaries; `EMAIL_INVALID`; no truncation or repeated normalization |
| Persistence/Flyway | Real PostgreSQL from empty | immutable V3; T017 red evidence; T018-only V5 and green exact ASCII constraints; local child ownership, authoritative Company on aggregate/binding operations, unscoped global catalogs, timestamp-free candidate, Application-owned final IDs, T076-only one-call equal `createdAt`/`updatedAt`, and committed `PersistedInvoiceDraft` | T017 failure specificity and unrelated-behavior stability; T018 V3→V5/Flyway validation and final absence of locale-dependent POSIX classes; no prohibited fields/catalog Company columns; rollback; no identifier replacement, placeholder timestamp, physical commit timestamp, or second clock call |
| Idempotency | Real PostgreSQL concurrency | replay/conflict/cross-Company independence/one winner; replay loads `PersistedInvoiceDraft` | property/collection order, line order, response loss, no normalized payload storage, no new aggregate/identifier/clock call/canonical rebuild, original identifier and both timestamps unchanged |
| API errors/correlation/deadline | Contract/integration with controlled deadline and wall-clock signals | API captures request time once at the earliest boundary before body consumption, owns the Uni/deadline race, emits exactly one terminal response, enforces the ordered upload/header/entity gate, and accepts exactly one normalized Idempotency-Key | body crossing Guayaquil midnight retains the entry date; resource/mapper make no later request-time read; raw emission point is forwarded; missing/blank/SP-HTAB-only/repeated/comma/over-length/grammar key vectors never select first; late app/DB results discarded; deadline-first/stage-first vectors; application/repositories have no HTTP types/mapping; malformed JSON/earlier failure; 400/409/413/422/503/504/500; no sleeps, 401, or 403 |
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
  `governance-nonconformity.md`, `governance-retrospective-review.md`, and
  `governance-owner-approval.md` records, plus the approved
  `governance-corrective-assignment-addendum.md`.

The requirements-quality content checks are reconciled to approved Constitution v2.0.1. The
recorded pre-analysis sequence remains an approved historical non-conformity and cannot be
corrected retroactively. `astudilloalex` approved D1–D3 with mandatory T017/T018 correction and
released `GATE-GOV-001`. The later approved addendum assigns T017 red evidence and T018 V5/green
persistence evidence without changing the retrospective findings or hash. The latest documentary
findings are remediated, but T017 remains pending and blocked until a new analysis confirms no
CRITICAL finding. T018 remains dependent on successful T017, and T019 remains ineligible until both
corrective tasks complete successfully.
