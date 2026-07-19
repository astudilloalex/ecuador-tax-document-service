# Implementation-Readiness Requirements Checklist: Prepare Invoice for Fiscal Issuance

**Purpose**: Validate requirement-to-design coverage, clarity, consistency, measurability, and
scenario completeness before Feature 002 task generation and implementation

**Created**: 2026-07-18

**Feature**: [Feature 002 specification](../spec.md)

**Audience/Timing**: Architecture and fiscal-domain reviewers; formal pre-task implementation-readiness gate

**Depth**: Comprehensive, risk-focused review of the specification, [implementation plan](../plan.md),
[data model](../data-model.md), research decisions, and inbound/outbound contracts

**Note**: This checklist tests the quality of the written requirements and design. Mark an item
complete only when the referenced artifacts state the answer objectively; it does not assert that
implementation code exists or behaves correctly.

## Requirement-to-Plan Coverage

- [ ] CHK001 Is the single bounded outcome consistently defined as transforming one existing Company-owned Invoice Draft into zero-or-one immutable Fiscal Preparation, without absorbing later issuance work? [Completeness, Spec §Bounded Outcome, Plan §Summary]
- [ ] CHK002 Is every functional requirement group FR-001–FR-081 represented by an explicit plan, data-model, contract, or evidence decision rather than merely repeated in prose? [Traceability, Spec §Functional Requirements, Plan §Constitution Check]
- [ ] CHK003 Are all domain rules DR-001–DR-011 mapped to a named domain value, state invariant, ordering rule, persistence invariant, or null-safety rule? [Traceability, Spec §Domain Rules and Invariants, Data Model §Modeling Boundaries]
- [ ] CHK004 Are success criteria SC-001–SC-016 each paired with objective evidence requirements in the plan, including concurrency, failure atomicity, redaction, exclusions, and warning-free null safety? [Measurability, Spec §Measurable Outcomes, Plan §Test and Operational Evidence Plan]
- [ ] CHK005 Are all 24 acceptance scenarios represented by primary, alternate, exception, recovery, concurrency, or non-functional design coverage without introducing an unapproved behavior? [Coverage, Spec §User Story 1, Plan §Data and External Consistency Design]
- [ ] CHK006 Are the specification, plan, data model, research, and both OpenAPI contracts mutually traceable on terminology, route, identity, state, errors, and excluded side effects? [Consistency, Spec §Requirements, Plan §Project Structure]
- [ ] CHK007 Are all material design additions—external port, two persistent structures, locking, shared request-context extraction, nullness tooling, and commit-boundary fault evidence—justified by a current requirement? [Simplicity, Spec §FR-018–FR-081, Plan §Complexity Tracking]

## Authority, Terminology, and Source Quality

- [ ] CHK008 Are the governing Ecuadorian regulation and SRI Offline Technical Sheet identified by exact version/date and by the sections governing this pre-XML outcome? [Traceability, Spec §Authority and Evidence, Plan §Source and Terminology Evidence]
- [ ] CHK009 Is the authority order clear enough that legislation and SRI v2.33 override the constitution, plan, and all legacy observations when they conflict? [Consistency, Spec §Authority and Evidence, Constitution Principle I]
- [ ] CHK010 Are legacy algorithms and database/API observations restricted to candidate scenarios and negative evidence, never target authority or compatibility obligations? [Clarity, Spec §FR-053/FR-080, Plan §Source Conflicts and Resolutions]
- [ ] CHK011 Is the SRI page-64 Access Key inconsistency documented with the governing rule, independent calculation, and negative-vector disposition so reviewers cannot mistake it for a positive example? [Conflict Resolution, Spec §FR-048/FR-053, Research §Access Key Composition and Validation]
- [ ] CHK012 Are the required terminology changes for Fiscal Preparation, Numeric Code, Access Key, Official Sequential Number, Official Sequence Baseline, and Fiscal Source Evidence explicitly recorded? [Completeness, Spec §Key Entities, Plan §Terminology Mapping Impact]
- [ ] CHK013 Are English canonical terms used across domain, API, database, errors, evidence, and package decisions while exact SRI acronyms/codes remain unchanged only where authoritative? [Consistency, Spec §Scope and Evidence, Constitution Principle II]
- [ ] CHK014 Is the statement that no Pending Functional Validation remains consistent with every dependency and conflict recorded in the plan, without disguising an unresolved business decision as a technical assumption? [Ambiguity, Spec §Pending Functional Validation, Plan §Pending Functional Validation]

## Clean Architecture and Dependency Direction

- [ ] CHK015 Is `fiscalpreparation` justified as the capability name by the bounded outcome, with no name implying XML generation, signing, SRI transmission, or complete fiscal issuance? [Clarity, Spec §Bounded Outcome, Plan §Structure Decision]
- [ ] CHK016 Are the permitted dependency directions for `api`, `application`, `domain`, and `infrastructure` explicitly stated and consistent with Constitution Principle IV? [Completeness, Plan §Project Structure, Constitution Principle IV]
- [ ] CHK017 Is the domain boundary specified as synchronous and framework-free, excluding Mutiny, HTTP/JSON, Quarkus, Jakarta REST, Panache, PostgreSQL, security, and provider DTO types? [Purity, Spec §DR-001–DR-011, Plan §Constitution Check]
- [ ] CHK018 Are Application responsibilities limited to use-case ordering, transport-neutral commands/results, business preconditions, and ports, with no dependency on API or Infrastructure? [Clarity, Spec §FR-018–FR-064, Plan §Structure Decision]
- [ ] CHK019 Are API responsibilities limited to header/path/representation classification, DTO mapping, deadline result selection, correlation, and HTTP status/error mapping? [Completeness, Spec §FR-010–FR-017/FR-065–FR-075, Plan §API and async quality gate]
- [ ] CHK020 Are Infrastructure responsibilities explicit for REST Client mapping, CSPRNG policy execution, reactive persistence, row locking, SQLSTATE/constraint classification, and reconciliation? [Completeness, Spec §FR-018–FR-064, Plan §Structure Decision]
- [ ] CHK021 Is every planned interface tied to an actual boundary—authoritative fiscal source, atomic persistence store, identifier/code generation—without a speculative repository hierarchy, generic unit of work, or factory layer? [Simplicity, Spec §FR-018/FR-060, Plan §Complexity Tracking]
- [ ] CHK022 Is the narrow extraction of request-context, Problem Details, deadline, clock, and operation-budget behavior specified precisely enough to preserve Feature 001 semantics without creating a generic request framework? [Consistency, Plan §Structure Decision, Plan §Source Conflicts and Resolutions]

## Company Context and Ownership Scoping

- [ ] CHK023 Is exactly one mandatory `X-Company-Id` defined as the sole Company input, including SP/HTAB trimming, UUID grammar, nil rejection, canonical form, and repeated/combined-value rejection? [Clarity, Spec §FR-010–FR-012, Contract §CompanyContext]
- [ ] CHK024 Are `COMPANY_CONTEXT_REQUIRED` and `COMPANY_CONTEXT_INVALID` requirements explicit about zero Company-owned reads, external fiscal reads, baseline access, and mutations before valid context? [Completeness, Spec §FR-011/FR-067, Plan §Company Header Contract]
- [ ] CHK025 Is Company Identifier consistently prohibited from request body, input schema, path, query, token, session, and response, with no undocumented exception? [Consistency, Spec §FR-012/FR-017, Contract §FiscalPreparationResponse]
- [ ] CHK026 Are all Company-owned operations enumerated as scoped operations: preflight draft/replay read, locked draft read, preparation read/insert, baseline lookup/lock/update, foreign-key linkage, uniqueness, and reconciliation? [Coverage, Spec §FR-013, Plan §Company Ownership Scoping]
- [ ] CHK027 Is the safe cross-Company behavior defined as indistinguishable from an absent draft, including zero leakage and zero state crossing? [Clarity, Spec §FR-014, Spec §SC-006]
- [ ] CHK028 Is Company ownership clearly distinguished from the fiscal numbering dimension, so the exact Issuer/Establishment/Emission Point/document-type scope cannot be duplicated merely under another Company? [Consistency, Spec §FR-034–FR-035/DR-005, Data Model §OfficialSequenceScope]
- [ ] CHK029 Is propagation of the canonical Company UUID to the external fiscal-context boundary specified only as business selection context and never as an authentication credential? [Clarity, Spec §FR-019/FR-024, Outbound Contract §CompanyContext]
- [ ] CHK030 Are authentication, authorization, Keycloak, JWT/OIDC, principals, roles, Company lookup/status, local Company master data, and Company administration consistently absent from requirements, plan, and contracts? [Coverage, Spec §FR-015–FR-016/FR-077, Plan §Internal Caller Boundary]

## External Fiscal-Context Ordering and Boundary Quality

- [ ] CHK031 Is the first-preparation ordering explicit as valid Company/correlation → Company-scoped replay/draft lookup → fixed-date eligibility → authoritative fiscal resolution/validation → local commit attempt? [Clarity, Spec §FR-007/FR-033/FR-057/FR-063, Plan §Transaction and Lock State Transitions]
- [ ] CHK032 Is replay ordering unambiguous that a valid committed preparation is returned before current-date validation, provider access, baseline access, identity generation, or new timestamp capture? [Consistency, Spec §FR-005/FR-031/FR-057, Spec §SC-007]
- [ ] CHK033 Are the only permitted fiscal selection inputs documented as canonical Company context, exact draft Emission Point UUID, unchanged emission date, and invoice Document Type Code `01`? [Completeness, Spec §FR-018–FR-020, Outbound Contract §FiscalContextSelection]
- [ ] CHK034 Does the external contract require one unambiguous, eligible, supported, effective, versioned result with all required Issuer, Establishment, Emission Point, designation, and source-evidence fields? [Completeness, Spec §FR-020/FR-025–FR-028, Outbound Contract §AuthoritativeFiscalContext]
- [ ] CHK035 Are unavailable, missing, invalid, unsupported, ineffective, ineligible, ambiguous, and inconsistent source outcomes assigned unambiguous local error categories before sequence allocation? [Clarity, Spec §FR-021–FR-024/FR-033, Plan §Stable Error-to-HTTP Mapping]
- [ ] CHK036 Is it explicit that the read-only provider result is fully validated before the database transaction and that no external operation is claimed atomic with PostgreSQL? [Consistency, Spec §FR-033/FR-063, Plan §Data and External Consistency Design]
- [ ] CHK037 Are connection timeout, response timeout, overall-deadline clamping, one-attempt behavior, retry classification, and the documented reason for no circuit breaker all specified? [Non-Functional Completeness, Spec §FR-022/FR-068–FR-071, Research §Authoritative Fiscal Context Boundary]
- [ ] CHK038 Is the permitted concurrency consequence—duplicate read-only fiscal resolutions among simultaneous initial contenders but exactly one committed local winner—documented without claiming provider exactly-once behavior? [Clarity, Spec §FR-058/FR-063, Research §Authoritative Fiscal Context Boundary]

## Transaction Atomicity, Lock Ordering, and Sequence Baseline

- [ ] CHK039 Is the all-or-nothing local boundary defined to include preparation identity, draft link, snapshot, sequence, Numeric Code, Access Key, creation instant, and baseline advancement in one transaction? [Completeness, Spec §FR-060–FR-063/DR-003, Plan §Data and External Consistency Design]
- [ ] CHK040 Is the absence of any durable provisional, reservation, `PREPARING`, or partial snapshot state explicit and consistent across specification and data model? [Consistency, Spec §FR-008–FR-009/FR-061, Data Model §Transaction and Lock State Transitions]
- [ ] CHK041 Is draft-first pessimistic locking stated as the mandatory first row lock using the authoritative Company-plus-draft predicate? [Clarity, Spec §FR-013/FR-058, Plan §Reactive and Resource Boundary Design]
- [ ] CHK042 Is a second same-draft winner lookup under the draft lock required before baseline access or Numeric Code/Access Key generation? [Completeness, Spec §FR-057–FR-058, Data Model §First preparation]
- [ ] CHK043 Is exact-baseline locking stated as the mandatory second row lock only after draft eligibility and winner recheck? [Clarity, Spec §FR-033/FR-042/FR-058, Plan §Summary]
- [ ] CHK044 Is the fixed draft-then-baseline order justified for both same-draft arbitration and deadlock avoidance, with no alternative order left implicit? [Consistency, Spec §FR-042/FR-058, Research §Official Sequence Allocation and Local Atomicity]
- [ ] CHK045 Are concurrent different-draft allocations in one fiscal scope specified to serialize only on the exact baseline and produce the next committed contiguous set in lock-acquisition order? [Coverage, Spec §FR-042/SC-003, Data Model §Concurrent different drafts]
- [ ] CHK046 Is the exact sequence scope complete and stable—Issuer reference, Establishment reference, Emission Point reference, official 3+3 codes, and document type `01`—while Company and environment are correctly excluded as numbering dimensions? [Clarity, Spec §FR-034–FR-035/DR-005, Data Model §OfficialSequenceScope]
- [ ] CHK047 Is baseline state modeled unambiguously as an explicitly provisioned `lastAllocated` value `0..999999999`, with derived next value, explicit exhaustion, and no inference that an absent row means zero? [Clarity, Spec §FR-037–FR-040, Data Model §Official Sequence Baseline]
- [ ] CHK048 Are absent, duplicated, regressed, out-of-range, scope-inconsistent, and exhausted baseline states each defined to fail closed without creation, repair, wrap, reset, or mutation? [Edge Coverage, Spec §FR-038–FR-040/SC-009]
- [ ] CHK049 Is the rejection of PostgreSQL `nextval`, `max+1`, JVM counters, advisory locks, and out-of-transaction allocation documented by their specific atomicity/concurrency shortcomings? [Design Rationale, Spec §FR-042/FR-060–FR-062, Research §Official Sequence Allocation and Local Atomicity]

## Natural Idempotency, Concurrency, and Commit Uncertainty

- [ ] CHK050 Is natural equivalence defined only as normalized Company Identifier plus Invoice Draft Identifier, with no caller idempotency key, correlation value, timing, or transport metadata in the identity? [Clarity, Spec §FR-054–FR-055, Contract §operation description]
- [ ] CHK051 Are first commit and replay outcomes distinguished observably without making the replay indicator part of business equivalence or fiscal identity? [Consistency, Spec §FR-005/FR-054–FR-056, Inbound Contract §201/200]
- [ ] CHK052 Are 100 equivalent concurrent requests specified to converge on one preparation, sequence, Numeric Code, Access Key, and creation timestamp, with every success identifying the same winner? [Measurability, Spec §FR-058/SC-004]
- [ ] CHK053 Is a committed replay required to return every originally persisted fiscal and source-evidence value unchanged and perform zero provider, baseline, generator, or clock work? [Completeness, Spec §FR-031/FR-056–FR-059, Spec §SC-007]
- [ ] CHK054 Is response loss after commit distinguished from unknown database commit outcome, with natural replay defined as the recovery mechanism for both? [Clarity, Spec §FR-059/FR-064, Research §Natural Replay and Commit Uncertainty]
- [ ] CHK055 Are confirmed pre-commit failure, confirmed rollback, acknowledged commit, and genuinely unknown outcome defined as mutually exclusive knowledge states with accurate zero-state guarantees? [Consistency, Spec §FR-061/FR-064/FR-067/FR-072]
- [ ] CHK056 Is reconciliation specified to occur after the failed transaction ends, using Company plus Invoice Draft, returning a complete winner when found and never directly allocating a replacement while outcome remains unknown? [Recovery Coverage, Spec §FR-064, Data Model §Persistence Failure and Outcome Knowledge]
- [ ] CHK057 Are SQLSTATE phase, rollback acknowledgement, and named constraint identity required to distinguish a same-draft race winner from Access Key, scoped-sequence, FK, or check-constraint defects? [Clarity, Spec §FR-058/FR-064, Data Model §Persistence Failure and Outcome Knowledge]
- [ ] CHK058 Is genuine lost-COMMIT-acknowledgement evidence distinguished from a mocked persistence exception and from post-commit HTTP response loss? [Evidence Quality, Spec §SC-010, Plan §Complexity Tracking]
- [ ] CHK059 Are overall deadline outcomes explicit for commit-not-started, rollback-confirmed, commit-acknowledged, and commit-possibly-succeeded cases, including prohibition of false zero-state claims? [Edge Coverage, Spec §FR-068–FR-072]

## SRI v2.33 Access Key and Numeric Code Correctness

- [ ] CHK060 Is SRI Offline Technical Sheet v2.33, updated July 2026 and modified 2026-07-13, the sole stated technical authority for this Access Key? [Traceability, Spec §FR-045/DR-006, Plan §Source and Terminology Evidence]
- [ ] CHK061 Is the exact 49-digit composition documented in authoritative order and widths `8+2+13+1+3+3+9+8+1+1`? [Clarity, Spec §FR-046, Data Model §AccessKey]
- [ ] CHK062 Is the six-digit series explicitly defined as authoritative Establishment Code followed immediately by authoritative Emission Point Code? [Clarity, Spec §FR-047]
- [ ] CHK063 Is Modulo 11 completely specified as right-to-left cyclic weights `2..7`, weighted sum, `11 - sum mod 11`, with raw `11→0` and `10→1` mappings? [Clarity, Spec §FR-048/DR-008]
- [ ] CHK064 Does final validation require both syntactic/check-digit correctness and equality of every parsed component to the draft, snapshot, allocation, and generated Numeric Code? [Completeness, Spec §FR-049/SC-005]
- [ ] CHK065 Are invoice code `01`, environment `1|2`, normal emission type `1`, RUC length 13, 3+3 series, 9-digit sequence, and 8-digit Numeric Code constraints stated consistently across spec, model, and contracts? [Consistency, Spec §FR-027/FR-044/FR-046–FR-050, Inbound Contract §FiscalContextSnapshot]
- [ ] CHK066 Is the draft emission date specified as the unchanged Ecuador civil date formatted `ddMMyyyy`, with no UTC shift, current-date substitution, or correction? [Clarity, Spec §FR-007/FR-050/DR-004]
- [ ] CHK067 Is Numeric Code policy `SECURE_RANDOM_8_V1` documented as uniform `0..99,999,999`, exactly eight digits, leading-zero preserving, caller-independent, and accepting `00000000`? [Clarity, Spec §FR-043–FR-044, Research §Numeric Code Policy]
- [ ] CHK068 Is RUC validation intentionally limited to 13 decimal digits plus authoritative-source equality, with the decision not to invent one generic local checksum documented? [Design Clarity, Spec §FR-027/FR-049, Research §Access Key Composition and Validation]
- [ ] CHK069 Are independently recalculated official vectors, synthetic raw-10/raw-11 vectors, every component mutation, and the invalid page-64 printed vector all required as evidence? [Coverage, Spec §FR-053/SC-005, Research §Access Key Composition and Validation]

## Flyway and Persistence Invariant Quality

- [ ] CHK070 Is Flyway defined as the sole schema authority, with V1–V5 immutable and one new migration after V5 rather than modification of committed migrations? [Consistency, Spec §FR-060, Plan §Technical Context, Constitution Principle IX]
- [ ] CHK071 Is the migration explicitly required to create an empty controlled-baseline structure with no seed, default scope, runtime initializer, or administration endpoint? [Completeness, Spec §FR-038/FR-078, Plan §Project Structure]
- [ ] CHK072 Are both empty-database creation and V5-to-Feature-002 upgrade evidence required on exact PostgreSQL 18.4, including prior migration checksum preservation? [Measurability, Plan §Test and Operational Evidence Plan, Constitution Principle IX]
- [ ] CHK073 Is the two-structure design justified as one mutable baseline plus one append-only preparation row embedding the logical snapshot, without a redundant master model, JSONB snapshot, or provisional table? [Simplicity, Spec §FR-025/FR-029/FR-060, Data Model §Modeling Boundaries]
- [ ] CHK074 Are database invariants complete for non-nil ownership, Company-consistent draft/baseline links, Company-plus-draft natural uniqueness, global exact-scope uniqueness, scoped sequential uniqueness, and global Access Key uniqueness? [Completeness, Spec §FR-009/FR-013/FR-035/FR-042/FR-052, Data Model §Fiscal Preparation]
- [ ] CHK075 Are exact digit representations specified as bounded `varchar` values with ASCII checks, avoiding padding or loss of leading zeros from `char(n)` or numeric types? [Clarity, Spec §FR-027/FR-041/FR-044/FR-046, Data Model §Value Objects]
- [ ] CHK076 Are required columns non-null and every legitimate optional column documented with effective-interval or paired-designation constraints that reject partial state? [Null/Invariant Completeness, Spec §FR-026–FR-028/DR-011, Data Model §Fiscal Context Snapshot]
- [ ] CHK077 Is committed Fiscal Preparation immutability protected at both model/repository contract and database update/delete boundary, with no cancellation or delete path implied? [Completeness, Spec §FR-008/DR-001–DR-003, Data Model §Immutability and Retention]
- [ ] CHK078 Are baseline scope immutability, monotonic one-step advancement, `updatedAt`, maximum exhaustion, and prohibition of decrement/reset/jump tied clearly to the corresponding successful preparation transaction? [Clarity, Spec §FR-037–FR-042/FR-060–FR-062, Data Model §Official Sequence Baseline]

## OpenAPI and Error-Contract Consistency

- [ ] CHK079 Is the inbound operation consistently defined as bodyless `POST /api/v1/invoice-drafts/{invoiceDraftId}/fiscal-preparation` with a non-nil draft UUID and no editable fiscal content? [Consistency, Spec §FR-001–FR-002/FR-036/FR-043/FR-045, Inbound Contract §path]
- [ ] CHK080 Is non-empty or prohibited preparation content assigned deterministically to `INVALID_REQUEST` without permitting fiscal fields or another idempotency key to influence equivalence? [Clarity, Spec §FR-002/FR-036/FR-054, Acceptance Scenario 22]
- [ ] CHK081 Are `201` new and `200` replay responses specified with one identical committed representation, stable correlation, replay classification, and no provisional/job response? [Completeness, Spec §FR-005/FR-056/FR-074, Inbound Contract §responses]
- [ ] CHK082 Are all stable error codes mapped consistently to HTTP status, safe Problem Details fields, retry guidance, and the exact local-state guarantee from FR-067? [Consistency, Spec §FR-065–FR-072, Plan §Stable Error-to-HTTP Mapping]
- [ ] CHK083 Is correlation behavior complete for absent, valid, repeated, blank, unsafe, and over-length inputs, including safe replacement and exclusion from equivalence? [Coverage, Spec §FR-055/FR-073, Inbound Contract §CorrelationId]
- [ ] CHK084 Is the successful-response exception for complete RUC/name/address/Access Key data explicit, while errors and all operational signals remain redacted and responses are non-cacheable? [Clarity, Spec §FR-075, Inbound Contract §FiscalPreparationResponse/NoStore]
- [ ] CHK085 Are security schemes, security requirements, `Authorization`, authentication APIs, and `401`/`403` responses explicitly absent from inbound and outbound contracts? [Coverage, Spec §FR-015/FR-024/FR-076, Constitution Principle VII]
- [ ] CHK086 Is the runtime OpenAPI source-of-truth strategy explicit: merge Feature 002 into `META-INF/openapi.yaml`, preserve Feature 001 semantically, and avoid obsolete whole-file equality assumptions? [Consistency, Plan §Source Conflicts and Resolutions, Plan §Contract Verification]
- [ ] CHK087 Is the outbound consumer contract explicitly read-only despite POST selection semantics, with exact Company/header/body fields, source evidence, provider errors, and no provider master-data or security contract invented locally? [Clarity, Spec §FR-018–FR-024, Outbound Contract §operation]

## Warning-Free Null Safety

- [ ] CHK088 Is the current gap accurately stated—that `javac -Xlint` and test-only JSpecify do not by themselves enforce production nullness—so readiness does not rely on a false baseline claim? [Clarity, Spec §FR-081/SC-016, Plan §Source Conflicts and Resolutions]
- [ ] CHK089 Are the chosen nullness mechanisms and versions explicit—JSpecify 1.0.0, Error Prone plugin 5.1.0, Error Prone 2.50.0, and NullAway 0.13.7—with Java 25 compatibility evidence required? [Dependency, Spec §FR-081, Research §Null Safety and Warning Policy]
- [ ] CHK090 Is every new or modified Feature 002 and extracted shared package required to be `@NullMarked`, with a requirement preventing silently unmarked owned code? [Completeness, Spec §DR-011/FR-081, Plan §Null safety evidence]
- [ ] CHK091 Are required values non-null by default and legitimate absence represented by `Optional`, sealed state, or dedicated value object rather than undocumented null? [Clarity, Spec §DR-011, Data Model §Modeling Boundaries]
- [ ] CHK092 Are unavoidable nullable JSON/ORM lifecycle fields, optional database columns, and Panache missing-result semantics confined to mapper boundaries and converted immediately to explicit states? [Boundary Coverage, Spec §FR-081/DR-011, Data Model §Application Port Models]
- [ ] CHK093 Is zero-warning acceptance measurable across NullAway/JSpecify, generic inference, `javac -Xlint:all -Werror`, and test compilation, rather than described only as an aspiration? [Measurability, Spec §SC-016, Quickstart §Quality and Null-Safety Gate]
- [ ] CHK094 Are broad suppression, `@NullUnmarked`, analyzer exclusions, warning downgrades, and new `@SuppressWarnings` explicitly rejected as substitutes for fixing owned-code causes? [Clarity, Spec §FR-081/SC-016, Research §Null Safety and Warning Policy]

## Sensitive Data, Observability, and Operational Requirements

- [ ] CHK095 Are RUC, Legal/Commercial Names, addresses, source evidence, Numeric Code, Access Key, raw provider data, credentials, internal endpoints, SQL, and baseline values consistently classified for storage/response/redaction purposes? [Completeness, Spec §FR-032/FR-065/FR-075, Plan §Sensitive Data]
- [ ] CHK096 Is the exact successful-response need for sensitive fiscal fields distinguished from their prohibition in errors, logs, traces, metric labels, health, audit payloads, and production-derived fixtures? [Consistency, Spec §FR-075/SC-014, Plan §Structured Observability]
- [ ] CHK097 Are provider request/response bodies, credentials, and internal provider errors explicitly excluded from snapshot persistence and caller-visible failures? [Coverage, Spec §FR-032, Plan §Sensitive Data and Certificate Lifecycle]
- [ ] CHK098 Are platform TLS, encryption-at-rest, backup, and Invoice-record retention responsibilities identified clearly enough to avoid an unowned sensitive-data control or an invented deletion API? [Dependency, Spec §FR-075, Plan §Sensitive Data]
- [ ] CHK099 Are observability fields limited to safe correlation, bounded outcome/error labels, timings, deadline owner, replay/new classification, and commit-knowledge class without high-cardinality fiscal identifiers? [Clarity, Spec §FR-065/FR-073/FR-075, Plan §Structured Observability]
- [ ] CHK100 Are sanitized audit-event requirements complete for committed, replayed, baseline-failure, rollback, and unknown-outcome transitions without creating a second fiscal payload store? [Completeness, Spec §FR-065/FR-075, Plan §Audit Events]
- [ ] CHK101 Are liveness and readiness meanings explicit—process viability versus PostgreSQL/Flyway readiness—while provider outage remains a business failure and committed replay remains available? [Clarity, Spec §FR-022/FR-031, Plan §Liveness and Readiness]

## Packaged JVM and Evidence Quality

- [ ] CHK102 Is packaged JVM execution mandatory and native compatibility explicitly deferred/unclaimed unless actual native build and runtime evidence are later supplied? [Consistency, Plan §Native Compatibility Evaluation, Constitution Principle III]
- [ ] CHK103 Are evidence requirements complete across pure domain, Application ordering, provider consumer contract, PostgreSQL persistence, Flyway, API/OpenAPI, architecture, sensitive-data, and packaged JVM levels? [Coverage, Spec §SC-001–SC-016, Plan §Test and Operational Evidence Plan]
- [ ] CHK104 Are concurrency criteria quantified as 100 equivalent requests and 100 different drafts in one scope, including exact committed cardinality, values, uniqueness, and locally caused gap rules? [Measurability, Spec §SC-003–SC-004]
- [ ] CHK105 Are PostgreSQL evidence requirements pinned to 18.4 for empty migration, upgrade, row locking, rollback, constraint, timeout, and concurrency semantics rather than an in-memory substitute? [Dependency, Plan §Technical Context/Test and Operational Evidence Plan]
- [ ] CHK106 Are provider and database outage, timeout, cancellation, response-loss, rollback, and commit-uncertainty evidence scenarios distinguished by their required observable state claims? [Recovery Coverage, Spec §FR-061–FR-072/SC-008–SC-010]
- [ ] CHK107 Are event-loop non-blocking, CSPRNG startup/warm-up, pool-size 20 contention, deadline clamping, and post-load recovery requirements measurable in packaged JVM evidence? [Non-Functional Completeness, Plan §Reactive and Resource Boundary Design]
- [ ] CHK108 Are the proposed validation commands and expected evidence artifacts specific enough for a reviewer to decide readiness without using coverage percentage as a proxy for requirement satisfaction? [Clarity, Quickstart §Quality and Null-Safety Gate/JVM Runtime Evidence]

## Explicit Exclusion Coverage

- [ ] CHK109 Are XML generation/schema validation, PKCS#12/certificate handling, signing, SRI reception/authorization communication, SRI retry, and SRI reconciliation consistently excluded from requirements, plan, dependencies, contracts, and evidence scope? [Coverage, Spec §FR-076/SC-013]
- [ ] CHK110 Are RIDE/PDF, email, webhook, queue, event, notification, storage, rendering, and delivery side effects explicitly absent from the bounded outcome and architecture? [Coverage, Spec §FR-076/SC-013, Plan §External Integrations]
- [ ] CHK111 Are Company, Issuer, Establishment, and Emission Point administration and complete/local editable master-data replication excluded while the minimal immutable snapshot remains clearly allowed? [Boundary Consistency, Spec §FR-025/FR-029/FR-077]
- [ ] CHK112 Are manual sequential assignment, baseline creation/repair/reset/administration, caller sequence override, cancellation, reversal, and reuse excluded without undermining the required controlled baseline dependency? [Coverage, Spec §FR-036/FR-038–FR-040/FR-078]
- [ ] CHK113 Are Invoice Draft mutation, recalculation, normalization, date correction, commercial-data copying beyond required evidence, and timestamp changes explicitly prohibited? [Completeness, Spec §FR-006–FR-007/DR-009/SC-012]
- [ ] CHK114 Are every non-Invoice tax document type, batch preparation, and any document type other than `01` excluded before sequence allocation? [Coverage, Spec §FR-079/DR-007]
- [ ] CHK115 Are legacy API/database/status/error compatibility, authentication/authorization behavior, and asynchronous legacy queue flows explicitly non-governing? [Coverage, Spec §FR-080, Plan §Source Conflicts and Resolutions]

## Dependencies, Assumptions, and Readiness Conflicts

- [ ] CHK116 Is Feature 001's authority over draft ownership, lifecycle, Emission Point reference, emission date, commercial data, totals, and immutability documented without creating a hidden cross-capability mutation contract? [Dependency, Spec §Assumptions and Dependencies, Plan §Company and Existing-Draft Boundary]
- [ ] CHK117 Is the approved fiscal-context provider dependency defined by authoritative ownership, immutable revision, effective-period semantics, bounded availability, and the consumer contract without requiring local master administration? [Dependency, Spec §Assumptions and Dependencies, Outbound Contract]
- [ ] CHK118 Is the separately controlled baseline-provisioning dependency explicit enough that Feature 002 can succeed when provisioned but can neither invent nor expose the missing administration capability? [Dependency, Spec §FR-037–FR-040/Assumptions and Dependencies]
- [ ] CHK119 Are the stale terminology mapping, Feature 001 architecture prohibition, and whole-runtime OpenAPI equality conflicts recorded as required lower-artifact corrections rather than constitutional deviations? [Conflict, Plan §Source Conflicts and Resolutions]
- [ ] CHK120 Is the decision not to make Company-specific fiscal resolution a readiness probe documented with its replay-availability rationale and a condition for any future safe provider health contract? [Design Clarity, Plan §Liveness and Readiness]
- [ ] CHK121 Are the fixed `America/Guayaquil` date rule, IANA timezone dependency, source observation instant, and creation instant responsibilities stated without conflating Product Owner policy with an SRI mandate? [Assumption, Spec §FR-007/FR-069/DR-004, Research §Ecuador Date and Time Semantics]
- [ ] CHK122 Are all remaining design dependencies and assumptions objectively resolvable during tasks/implementation without requiring a new material business decision or silently expanding scope? [Readiness, Spec §Assumptions and Dependencies, Plan §Pending Functional Validation]

## Review Notes

- `[x]` means the written requirement/design quality criterion is satisfied; it does not mean code
  or a task was completed.
- Record unresolved findings beside the affected item and update the authoritative specification or
  plan before generating tasks.
- A failed item involving authority, Company isolation, sequence atomicity, Access Key correctness,
  unknown-outcome recovery, null safety, or exclusions is a blocking readiness defect.
