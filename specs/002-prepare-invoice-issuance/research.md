# Research: Prepare Invoice for Fiscal Issuance

**Feature**: `002-prepare-invoice-issuance`  
**Date**: 2026-07-18  
**Constitution**: v2.0.1  
**Status**: Complete — all material technical unknowns are resolved; no Pending Functional
Validation or unresolved clarification marker remains

**Canonical terminology**: `docs/migration/terminology-mapping.md` registers Fiscal Preparation,
Fiscal Context Snapshot, Official Sequence Baseline, Official Sequential Number, Numeric Code,
Access Key, and Fiscal Source Evidence as Feature 002 Target Domain concepts. Feature 001 correctly
excluded these fiscal-identity values from Invoice Draft creation; this research preserves that
historical boundary.

Provider availability, baseline provisioning, and sensitive-data platform controls are approved
architecture/deployment dependencies with accountable roles and evidence paths described below.
Approval does not claim that a production provider destination, production baseline, or target-
environment control evidence already exists, and no remaining dependency requires a new material
business decision.

**Primary legal authority**: [Consolidated Regulation for Sales, Withholding, and Complementary
Documents, last modified 2023-12-29](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/9fb49475-f058-49a1-b08a-f31bf4deb074/Reglamento_Comprobantes_Venta_RetencionYDC_29122023.pdf),
especially articles 18, 19, 26, 41, 42, 49, and 50. It governs required invoice facts,
Establishment/Emission Point numbering, consecutive issuance, retention, and non-reuse context;
later delivery/cancellation behavior does not expand this pre-XML feature.

## 1. Runtime and Dependency Baseline

**Decision**: Retain the implemented Java 25, Quarkus `3.33.2.1`, PostgreSQL 18.4 test/runtime,
Mutiny, Hibernate Reactive with Panache, Flyway, Quarkus REST Jackson, validation, health,
Micrometer, OpenTelemetry, JUnit 5, REST Assured, and Spotless baseline. Add only the BOM-managed
`io.quarkus:quarkus-rest-client-jackson` production capability for the approved non-blocking fiscal
context boundary.

**Rationale**: The repository already fixes and verifies the constitutional baseline. Quarkus REST
Client is non-blocking at its core, supports `Uni`, JSON mapping, configured connection/read
timeouts, and uses the same reactive HTTP foundation as the existing service. No XML, SOAP,
certificate, broker, cache, security, or scheduler capability is needed.

**Alternatives considered**:

- A blocking JDK or Apache HTTP client was rejected because it would require worker isolation and
  would violate the existing reactive request path.
- A second persistence technology or direct JDBC in the request path was rejected; JDBC remains
  Flyway-only.
- SmallRye Fault Tolerance, automatic retries, and circuit breaking were rejected for this bounded
  read because one short attempt plus natural caller replay is sufficient and avoids spending the
  ten-second budget on hidden duplicate source reads.

**Evidence**:

- Repository `gradle.properties`, `build.gradle.kts`, and test `application.properties`, inspected
  2026-07-18.
- [Quarkus REST Client guide](https://quarkus.io/guides/rest-client)
- [Quarkus reactive architecture](https://quarkus.io/guides/quarkus-reactive-architecture)
- [Quarkus Hibernate Reactive with Panache](https://quarkus.io/guides/hibernate-reactive-panache)

## 2. Capability and API Shape

**Decision**: Add one `fiscalpreparation` capability across `api`, `application`, `domain`, and
`infrastructure`. Expose:

```text
POST /api/v1/invoice-drafts/{invoiceDraftId}/fiscal-preparation
```

The operation has no request body, requires exactly one `X-Company-Id`, accepts optional
`X-Correlation-Id`, and accepts no `Idempotency-Key`. A new commit returns `201`; a natural replay
returns `200`; both return the same persisted preparation. `Fiscal-Preparation-Replayed: false` or
`true` makes the result classification observable without joining it to equivalence.

**Rationale**: The path identifies the existing aggregate and the single subordinate result. The
natural identity is Company plus Invoice Draft, so another caller-generated idempotency key would
create conflicting equivalence rules. A body would provide no legitimate input and would invite
the prohibited caller control of fiscal fields.

**Alternatives considered**:

- `/companies/{companyId}/...` and a body/query Company Identifier were rejected by the Company
  boundary.
- `PUT` was rejected because the client does not choose the preparation representation or its
  identifier.
- An asynchronous job endpoint was rejected because the feature has one synchronous terminal
  result and no later side effect.
- Returning `201` for every replay was rejected because a distinct `200` makes recovery observable
  while preserving the exact committed body.

## 3. Company and Existing-Draft Boundary

**Decision**: Parse the Company header before any owned-data or external access, trim only the
approved surrounding ASCII SP/HTAB, require exactly one canonical non-nil UUID, and carry the
existing immutable `CompanyId` value explicitly through every port. Every query and mutation uses
`(companyId, invoiceDraftId)` or another complete Company-owned key. Cross-Company and absent drafts
both return `INVOICE_DRAFT_NOT_FOUND`.

The application loads only a minimal preparation view of the existing Invoice Draft: identifier,
Company ownership, opaque emission-point UUID, date-only emission date, lifecycle status, and prior
fiscal-link state. It does not reconstruct or mutate the commercial child graph. Existing header
and correlation rules are reused through narrowly shared API helpers; Feature 001 behavior must
remain covered by regression tests.

**Rationale**: This preserves Feature 001 as commercial authority, prevents data leakage, and
avoids a new Company/Issuer master-data model. Company scoping is business partitioning, never
caller authentication or authorization.

**Alternatives considered**:

- Loading by draft identifier and checking Company afterward was rejected because it reads outside
  the authoritative Company scope.
- Calling a Company service or validating Company status was rejected as explicitly excluded.
- Loading the complete draft aggregate was rejected because preparation needs only immutable
  eligibility inputs and must not recalculate commercial values.

## 4. Authoritative Fiscal Context Boundary

**Decision**: The approved logical capability is `authoritative-fiscal-context`, governed for this
consumer by `contracts/authoritative-fiscal-context.openapi.yaml` version `1.0.0`; the accountable
role is `Fiscal Context Provider Owner`. Define a read-only application port resolved by a Quarkus
REST Client adapter. The adapter sends `X-Company-Id` plus the exact draft emission-point UUID,
unchanged emission date, and document type `01` to that approved Company bounded-context fiscal
capability. It accepts no caller fiscal values and uses no local Company/Issuer/Establishment/
Emission Point master data.

The provider owns authoritative Company-to-Issuer, Establishment, Emission Point, eligibility,
effective-period, and immutable source-revision data. Feature 002 owns only the consumer port,
validation, mapping, bounded timeout, and safe error classification. Provider implementation,
master-data administration, and deployment remain outside scope. The provider base URL is
environment configuration and is not hardcoded. The contract's `.invalid` server is deliberately
non-routable planning metadata, not an unapproved runtime endpoint. Implementation and automated
acceptance may use the approved contract fixture; production deployment remains blocked until a
concrete provider destination and accountable operational owner are registered in deployment
configuration.

The external contract is `POST /fiscal-context-resolutions/invoice-issuance`. Although expressed as
POST to carry a structured selection request, the provider contract declares it read-only. One
attempt has a one-second connection timeout and a two-second response timeout, always clamped to
the remaining overall deadline. There is no automatic retry. Missing/invalid/unsupported/
inconsistent results map to their local stable codes; connection, provider, and provider-deadline
failures map to `FISCAL_CONTEXT_UNAVAILABLE`. Raw payloads and internal errors are neither stored
nor returned.

**Rationale**: Fiscal context must be completely known and validated before sequence locking. A
short non-blocking boundary leaves budget for the local atomic operation and makes retry behavior
explicit. Concurrent initial requests may perform more than one read-only resolution, but the
database arbiter permits only one committed preparation; requests arriving after commit perform no
source read.

**Alternatives considered**:

- Local fiscal master tables, a replicated cache, cross-service database reads, or caller-supplied
  values were rejected by the specification and constitution.
- Holding a database lock while calling the provider was rejected because a remote operation is
  not atomic with PostgreSQL and would extend scarce row-lock time.
- Automatic provider retries were rejected because they consume shared deadline and add no
  correctness beyond natural replay.

## 5. Minimal Fiscal Context Snapshot

**Decision**: Persist only the issuance facts required by the specification and later invoice XML,
plus immutable evidence:

- authoritative Issuer and Establishment references and the draft's exact emission-point UUID;
- 13-digit RUC, Legal Name, optional Commercial Name, Head Office Address, three-digit
  Establishment and Emission Point codes, and Establishment Address;
- accounting obligation as an exact boolean with no resolution identifier; optional Special
  Taxpayer and Withholding Agent designations with their authoritative required resolution
  identifiers; required RIMPE classification `NONE`, `RIMPE_CONTRIBUTOR`, or `POPULAR_BUSINESS`
  without an invented resolution; optional Large Contributor designation with the authoritative
  resolution and legend evidence required together by the approved consumer contract and governing
  source;
- environment `1` or `2`, document type `01`, normal emission type `1`;
- technical-rule identifier `SRI-OFFLINE-2.33`, rule modification date `2026-07-13`, and Numeric
  Code policy identifier/version;
- source authority, immutable source revision, inclusive effective-from/effective-through Ecuador
  civil dates (with an explicitly open end when applicable), and observation `Instant`.

An optional designation is represented as an optional value object, not as unrelated nullable
flags and strings. A designation for which the governing source requires a resolution identifier or
paired evidence is persisted atomically with that evidence when applicable and is absent with it
when not applicable; any partial required pair is invalid. Accounting Required and RIMPE
Classification never invent a resolution identifier.

**Rationale**: These values are immutable issuance evidence, not administrative master data. The
shape is sufficient to validate the Access Key and later construct common invoice fiscal headers
without persisting raw provider representations or unrelated mutable attributes.

**Alternatives considered**:

- Persisting only Access Key components was rejected because it omits required issuer/address and
  designation evidence.
- Persisting the full source response or complete master model was rejected as unnecessary
  replication and a sensitive-data expansion.
- Generic designation maps or JSON blobs were rejected because their null/pairing rules and schema
  evolution are weaker than explicit domain values.

## 6. Ecuador Date and Time Semantics

**Decision**: At initial request entry capture one `Instant`, derive one civil date with the named
`America/Guayaquil` zone, and retain both in the request context. A first preparation requires exact
equality with the draft's unchanged `LocalDate`; prior and future dates return
`EMISSION_DATE_STALE` before external access. Replays are checked first and do not re-evaluate the
date. The same draft date is formatted `ddMMyyyy` in the Access Key. Source effective dates remain
civil dates; source observation and preparation creation remain `Instant` values.

**Rationale**: This is the Product Owner clarification, not an inferred SRI rule. Capturing once
prevents midnight races and avoids changing a date-only fact through UTC conversion.

**Alternatives considered**: JVM default zone, a hard-coded UTC offset, repeated `now()` calls, and
converting `LocalDate` through an instant were rejected.

**Evidence**:

- Feature clarification dated 2026-07-18.
- [IANA tzdb 2026c South America data](https://data.iana.org/time-zones/tzdb-2026c/southamerica),
  published 2026-07-08.

## 7. Official Sequence Allocation and Local Atomicity

**Decision**: Persist one Company-owned controlled baseline row per exact fiscal business scope:

```text
Issuer reference + Establishment reference + Emission Point reference
+ Establishment code + Emission Point code + document type 01
```

Company is a mandatory ownership predicate and composite-reference constraint, but it is not an
extra numbering dimension. A second baseline with the same exact fiscal scope under another Company
is invalid/ambiguous and is prevented by global exact-scope uniqueness.

The baseline stores one non-null `lastAllocated` integer in `0..999999999`; the next candidate is
`lastAllocated + 1`, while `999999999` is explicitly exhausted. A `0` row exists only when a
controlled provisioning process has deliberately established a new scope; absence never implies
zero. No migration or runtime path seeds or initializes a missing scope.

The approved accountable role for production provisioning is `Database Operations Owner`.
Provisioning occurs outside Feature 002 through a controlled, reviewed, auditable SQL/runbook
procedure. It validates Company ownership, the exact Issuer/Establishment/Emission Point/document-
type scope including official codes, and the initial `lastAllocated`. Its external evidence records
requester, approver, execution time, scope, and resulting baseline identifier without placing
sensitive values in general telemetry. Production readiness for a fiscal scope requires that
approved evidence before the first preparation request. Tests may insert controlled fixture rows
only. Feature 002 still performs no missing-row creation, Flyway seed, upsert-on-missing, reset,
decrement, repair, wrap, reuse, or administration operation.

For a first preparation, one short reactive PostgreSQL transaction acquires row locks in a fixed
order: Company-scoped Invoice Draft first, exact sequence baseline second. After locking the draft,
it rechecks eligibility and loads any winner; an existing winner returns as replay without locking
the baseline or generating identity. It then locks and validates the exact baseline, selects its
current value, creates and validates the Numeric Code and Access Key, inserts the immutable
preparation, and increments `lastAllocated` in the same transaction. Allocating `999999999` leaves
that value as the explicit exhausted boundary. Any rollback restores both rows.

Database constraints enforce unique Company-plus-draft, unique fiscal-scope-plus-sequential, unique
Access Key, complete Company-consistent linkage, widths/ranges, and explicit optionality. A
deterministic draft-then-baseline lock order prevents application-created deadlock cycles.

**Rationale**: PostgreSQL `SELECT ... FOR UPDATE` blocks competing lockers until transaction end;
fixed lock order is the documented defense against deadlocks. A transactionally updated baseline
prevents duplicates and locally caused gaps without a reservation table, advisory lock, external
coordinator, or `SERIALIZABLE` scope.

**Alternatives considered**:

- `max(sequence)+1`, JVM locks, optimistic retries alone, or an in-memory counter were rejected as
  unsafe across processes.
- PostgreSQL sequences were rejected because `nextval` is not rolled back and would consume values
  on local failure.
- Advisory locks were rejected because a controlled baseline row already provides the correct
  durable transaction-scoped lock target.
- Holding a provisional reservation and later finalizing it was rejected because it adds an
  intermediate state prohibited by the all-or-nothing outcome.

**Evidence**:

- [PostgreSQL 18 explicit locking](https://www.postgresql.org/docs/18/explicit-locking.html)
- [PostgreSQL 18 constraints](https://www.postgresql.org/docs/18/ddl-constraints.html)

## 8. Numeric Code Policy

**Decision**: Numeric Code policy `SECURE_RANDOM_8_V1` uniformly selects an integer in
`0..99,999,999` from the JDK cryptographically strong random source and formats it as exactly eight
digits with retained leading zeros. `00000000` is valid. The code is generated only after the
same-draft winner check and baseline lock, then committed once and replayed unchanged. Persist the
policy identifier/version, not random generator state.

**Rationale**: SRI v2.33 section 5.2 delegates the eight-digit algorithm to the Issuer. Secure
random selection supplies the intended security value without a caller field, secret-key
lifecycle, or coupling to the official sequence.

**Alternatives considered**: sequence-derived digits are predictable; hash/HMAC adds collision and
key-policy concerns; caller input is forbidden; generation before replay arbitration wastes values
and weakens exactly-once evidence.

## 9. Access Key Composition and Validation

**Decision**: A pure domain value constructs exactly 49 decimal digits in this order:

| Component | Width |
|-----------|-------|
| Emission date `ddMMyyyy` | 8 |
| Invoice document type `01` | 2 |
| Issuer RUC | 13 |
| Environment `1` or `2` | 1 |
| Establishment code | 3 |
| Emission Point code | 3 |
| Official Sequential Number | 9 |
| Numeric Code | 8 |
| Normal emission type `1` | 1 |
| Modulo 11 Verification Digit | 1 |

For the first 48 digits, apply weights `2,3,4,5,6,7` cyclically from right to left, sum the
products, calculate `11 - (sum mod 11)`, map raw `11` to `0` and raw `10` to `1`, and otherwise use
the raw result. Final validation reparses and compares every component with the draft, snapshot,
allocation, and generated code; it does not accept a merely self-consistent but context-mismatched
key. RUC validation for this feature is exactly 13 digits plus equality to the authoritative
source; no generic local RUC checksum is invented.

**Rationale**: This is the exact SRI v2.33 algorithm and prevents both width/check-digit errors and
substitution of a different valid fiscal identity.

**Official positive vectors independently verified**:

- Modulo-only `41261533` produces `6`.
- `2110201101179214673900110020010000000011234567813` produces `3`.
- `0503201201176001321000110010030009900641234567814` produces `4`.
- `0601201601176001321000110011230000000081234567817` produces `7`.

**Synthetic edge vectors**:

- Raw `11` maps to `0`:
  `1807202601179214673900110010010000000010000000110`.
- Raw `10` maps to `1`:
  `1807202601179214673900110010010000000010000000511`.

**Source inconsistency and resolution**: SRI v2.33 page 64 prints
`0403201301176815353000110015010000000081234567816`. Its first 48 digits produce weighted sum
`473`, remainder `0`, and therefore Verification Digit `0`, not `6`. The printed value is retained
only as a negative regression vector. Independently calculated v2.33 rules govern; no PDF example
is accepted without recalculation.

**Primary evidence**:

- [SRI Offline Technical Sheet v2.33, updated July 2026, revision 2026-07-13](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/5a547488-80f3-4966-a2a4-841f2e951986/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.33.pdf),
  sections 5.2–5.5, Tables 1–4, and Annexes 21, 22, and 24.
- [Current SRI electronic-invoicing portal](https://www.sri.gob.ec/facturacion-electronica),
  reviewed 2026-07-18.

## 10. Natural Replay and Commit Uncertainty

**Decision**: Company plus Invoice Draft is the only equivalence key. Read a committed preparation
before date/source work and again under the draft lock. Unique constraints arbitrate cross-process
races. A constraint loser or a failure after a possible commit performs a fresh Company-scoped
reconciliation read outside the failed transaction:

- a complete row returns as replay;
- confirmed rollback/no row returns `PERSISTENCE_FAILURE` only when the database outcome is
  conclusive;
- a connection loss, timeout, or database condition that can leave commit result indeterminate,
  followed by an inconclusive reconciliation, returns `PREPARATION_OUTCOME_UNKNOWN`;
- overall expiry before commit begins, or after confirmed rollback, returns `REQUEST_TIMEOUT` with
  the permitted zero-state claim; otherwise it directs natural replay and makes no zero-state
  claim.

No automatic retry reruns the identity transaction after commit may have occurred. Safe deadlock or
serialization retries are permitted only when PostgreSQL has conclusively aborted the entire
transaction and remaining deadline is sufficient; retry must restart with the same draft-first lock
order.

Commit-knowledge tests use two distinct vectors: a test-only post-commit HTTP response failure
proves natural replay, while a transport fault around PostgreSQL COMMIT (TCP fault proxy or an
equivalent real connection interruption) proves conservative unknown-outcome classification. A
mocked repository exception alone is not accepted as evidence of lost commit acknowledgement.

**Rationale**: A client connection can fail around commit, so local code cannot always infer
rollback. Reconciliation by the natural unique key is the only safe recovery and cannot allocate a
replacement identity when one may exist.

**Alternatives considered**: treating every exception as rollback, blindly rerunning commit, or
using correlation as an idempotency key were rejected because each can allocate a second fiscal
identity or make a false state claim.

## 11. Stable Error-to-HTTP Mapping

**Decision**: Extend the existing Problem Details representation and correlation rules.

| Code | HTTP | Retry guidance |
|------|------|----------------|
| `COMPANY_CONTEXT_REQUIRED`, `COMPANY_CONTEXT_INVALID`, `INVALID_REQUEST` | 400 | Correct request/header |
| `INVOICE_DRAFT_NOT_FOUND` | 404 | Correct Company/draft identity |
| `INVOICE_DRAFT_NOT_PREPARABLE` | 409 | Resolve lifecycle/fiscal-state conflict |
| `EMISSION_DATE_STALE` | 422 | Draft cannot be silently corrected |
| `FISCAL_CONTEXT_INVALID`, `FISCAL_CONTEXT_UNSUPPORTED`, `FISCAL_CONTEXT_INCONSISTENT` | 422 | Correct authoritative source state |
| `OFFICIAL_SEQUENCE_BASELINE_MISSING`, `OFFICIAL_SEQUENCE_BASELINE_INVALID`, `OFFICIAL_SEQUENCE_EXHAUSTED` | 409 | Baseline administration is external to this operation |
| `FISCAL_CONTEXT_UNAVAILABLE`, `PERSISTENCE_FAILURE`, `PREPARATION_OUTCOME_UNKNOWN` | 503 | Retry same Company/draft; unknown outcome mandates replay |
| `ACCESS_KEY_INVALID` | 500 | Operational defect/evidence mismatch; no baseline consumption |
| `REQUEST_TIMEOUT` | 504 | Retry same Company/draft |

Unexpected failures use the existing safe `INTERNAL_ERROR`/500 fallback. Error bodies expose no
RUC, name, address, Access Key, source payload, SQL, endpoint, stack trace, or baseline value.

## 12. Null Safety and Warning Policy

**Decision**: Promote JSpecify `1.0.0` from test-only availability to `compileOnly` production
annotations and incrementally null-mark every new Feature 002 package. Add the Gradle Error Prone
plugin `5.1.0`, Error Prone `2.50.0`, and NullAway `0.13.7`; run NullAway at error severity with
`OnlyNullMarked=true`, JSpecify mode, and generic-inference warnings. Retain `javac -Xlint:all
-Werror` and Spotless. Every warning caused by Feature 002 is fixed at its source; broad exclusions,
`@NullUnmarked`, and new warning suppressions are not acceptance mechanisms.

Required fields are non-null by default. Legitimate absence uses domain `Optional` or a dedicated
sum/value type. `@Nullable` is limited to real framework, JSON, and persistence edges and is mapped
immediately. Persistence nullable columns correspond only to documented optional business state;
baseline exhaustion uses an explicit domain state instead of leaking a nullable number.

**Rationale**: JSpecify defines contracts but explicitly requires a compatible analyzer to enforce
them. `-Xlint` alone does not perform nullness analysis. Incremental `@NullMarked` adoption covers
all new/modified Feature 002 code without falsely declaring the unaudited Feature 001 packages
safe. NullAway 0.13.7 includes current Jakarta Persistence initialization handling and Java 25+
compatibility work.

The checked-scope boundary is exact: new Feature 002 and extracted shared packages use package-level
`@NullMarked`; an existing Feature 001 type modified by Feature 002 uses type-level `@NullMarked`
without marking untouched neighbors. Feature 002 packaged-JVM tests use the dedicated
`runtime.fiscalpreparation` package rather than marking the existing Feature 001 `runtime` package.
The service-wide health types move from false Invoice Draft ownership to the new null-marked
`infrastructure.health` boundary, with their regression test in the same checked package.

**Alternatives considered**:

- JSpecify annotations without an analyzer were rejected because they do not satisfy zero
  unresolved null-safety warnings.
- Null-marking the whole existing codebase in this feature was rejected as an unbounded migration;
  only code changed for Feature 002 must enter checked scope.
- Converting warnings to informational output or adding suppressions was rejected by owner direction.

**Evidence**:

- [JSpecify applying annotations](https://jspecify.dev/docs/applying/)
- [JSpecify nullness user guide](https://jspecify.dev/docs/user-guide/)
- [NullAway 0.13.7 release](https://github.com/uber/NullAway/releases/tag/v0.13.7)
- [Gradle Error Prone plugin 5.1.0](https://github.com/tbroyer/gradle-errorprone-plugin/releases/tag/v5.1.0)

## 13. Health, Observability, Sensitive Data, and Native Status

**Decision**: Liveness remains process viability. Readiness continues to require PostgreSQL/Flyway
schema readiness. The fiscal source is monitored with sanitized business-call metrics but is not a
readiness dependency: an outage has a stable first-preparation result while committed natural
replays must remain available. Health does not call or mutate the provider.

Structured logs/traces carry correlation, outcome code, replay/new classification, timings, and
opaque or hashed local identifiers. Metrics cover preparation outcomes, replay count, source
latency/failure class, baseline lock/allocation latency, deadline owner, rollback/unknown outcomes,
and concurrency. They never carry full Company UUID, RUC, names, addresses, Numeric Code, Access
Key, provider body, source revision, or baseline value as labels.

Snapshot fiscal data and Access Key are stored only in the existing managed PostgreSQL boundary and
returned in the explicitly contracted successful representation. The accountable role is
`Platform Operations Owner`. That role owns TLS-enabled service and PostgreSQL connections,
approved PostgreSQL encryption at rest, encrypted backup policy and handling, successful
restoration evidence, and the approved Invoice-record retention policy applicable to Fiscal
Preparation. Release evidence must
show each control in the target environment and confirm that Fiscal Preparation is retained and
disposed of with its related Invoice record. Feature 002 introduces no custom application-level
database encryption, key management, deletion API, or retention scheduler. The intentional absence
of a deletion API does not override platform retention. Secrets, raw provider requests/responses,
credentials, and internal provider errors are excluded from primary persistence and backups because
they are never stored. Audit records are sanitized success/failure operational events, not a second
fiscal payload store.

JVM build/runtime evidence is mandatory. Native support is deferred until an actual native build
and runtime suite verifies REST Client JSON mapping, Hibernate Reactive entities, configuration,
and resource/reflection behavior. SOAP, XML, signing, PKCS#12, and cryptographic-provider rows are
not applicable to this feature.

**Alternatives considered**: marking readiness down on every provider outage was rejected because
it would also block safe replay; logging fiscal payloads for diagnostics was rejected as unnecessary
sensitive-data exposure; claiming native compatibility from JVM tests was rejected.

**Readiness resolution**: Fiscal-context provider ownership, Official Sequence Baseline
provisioning, and sensitive-data platform controls now have approved accountable roles and evidence
paths. These decisions close planning ambiguity without asserting completed production deployment
evidence and without introducing authentication, master-data or baseline administration, XML,
signing, SRI communication, PDF, messaging, or background processing.
