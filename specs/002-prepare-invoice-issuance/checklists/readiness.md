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

- [x] CHK001 Is the single bounded outcome consistently defined as transforming one existing Company-owned Invoice Draft into zero-or-one immutable Fiscal Preparation, without absorbing later issuance work? [Completeness, Spec §Bounded Outcome, Plan §Summary]
- [x] CHK002 Is every functional requirement group FR-001–FR-081 represented by an explicit plan, data-model, contract, or evidence decision rather than merely repeated in prose? [Traceability, Spec §Functional Requirements, Plan §Constitution Check]
- [x] CHK003 Are all domain rules DR-001–DR-011 mapped to a named domain value, state invariant, ordering rule, persistence invariant, or null-safety rule? [Traceability, Spec §Domain Rules and Invariants, Data Model §Modeling Boundaries]
- [x] CHK004 Are success criteria SC-001–SC-016 each paired with objective evidence requirements in the plan, including concurrency, failure atomicity, redaction, exclusions, and warning-free null safety? [Measurability, Spec §Measurable Outcomes, Plan §Test and Operational Evidence Plan]
- [x] CHK005 Are all 24 acceptance scenarios represented by primary, alternate, exception, recovery, concurrency, or non-functional design coverage without introducing an unapproved behavior? [Coverage, Spec §User Story 1, Plan §Data and External Consistency Design]
- [x] CHK006 Are the specification, plan, data model, research, and both OpenAPI contracts mutually traceable on terminology, route, identity, state, errors, and excluded side effects? [Consistency, Spec §Requirements, Plan §Project Structure]
- [x] CHK007 Are all material design additions—external port, two persistent structures, locking, shared request-context extraction, nullness tooling, and commit-boundary fault evidence—justified by a current requirement? [Simplicity, Spec §FR-018–FR-081, Plan §Complexity Tracking]

## Authority, Terminology, and Source Quality

- [x] CHK008 Are the governing Ecuadorian regulation and SRI Offline Technical Sheet identified by exact version/date and by the sections governing this pre-XML outcome? [Traceability, Spec §Authority and Evidence, Plan §Source and Terminology Evidence]
- [x] CHK009 Is the authority order clear enough that legislation and SRI v2.33 override the constitution, plan, and all legacy observations when they conflict? [Consistency, Spec §Authority and Evidence, Constitution Principle I]
- [x] CHK010 Are legacy algorithms and database/API observations restricted to candidate scenarios and negative evidence, never target authority or compatibility obligations? [Clarity, Spec §FR-053/FR-080, Plan §Source Conflicts and Resolutions]
- [x] CHK011 Is the SRI page-64 Access Key inconsistency documented with the governing rule, independent calculation, and negative-vector disposition so reviewers cannot mistake it for a positive example? [Conflict Resolution, Spec §FR-048/FR-053, Research §Access Key Composition and Validation]
- [x] CHK012 Are the required terminology changes for Fiscal Preparation, Numeric Code, Access Key, Official Sequential Number, Official Sequence Baseline, and Fiscal Source Evidence explicitly recorded? [Completeness, Spec §Key Entities, Plan §Terminology Mapping Impact]
- [ ] CHK013 Are English canonical terms used across domain, API, database, errors, evidence, and package decisions while exact SRI acronyms/codes remain unchanged only where authoritative? [Consistency, Spec §Scope and Evidence, Constitution Principle II]
  - **Finding — BLOCKING**: English names are used, but canonical classification is contradictory: `docs/migration/terminology-mapping.md` still marks Access Key and Official Sequential Number as `SRI Adapter Only`, reserves Fiscal Context Snapshot for a later feature, and has no entries for Fiscal Preparation, Numeric Code, Official Sequence Baseline, or Fiscal Source Evidence, while Feature 002 makes them target concepts. **Affected**: `docs/migration/terminology-mapping.md` §Legacy-To-Target Terminology Mapping; `spec.md` §Terminology Mapping; `plan.md` §Terminology Mapping Impact. **Minimum correction**: update and approve the canonical mapping with one valid classification and Feature 002 definition for every affected term while preserving Feature 001 history.
- [ ] CHK014 Is the statement that no Pending Functional Validation remains consistent with every dependency and conflict recorded in the plan, without disguising an unresolved business decision as a technical assumption? [Ambiguity, Spec §Pending Functional Validation, Plan §Pending Functional Validation]
  - **Finding — BLOCKING**: `spec.md` and `plan.md` declare that no Pending Functional Validation remains, but neither an approved fiscal-context provider identity/contract acceptance nor an approved production baseline-provisioning responsibility is evidenced; these are material dependencies, not implementation-only assumptions. **Affected**: `spec.md` §Pending Functional Validation and §Assumptions and Dependencies; `plan.md` §Pending Functional Validation; outbound contract §info/servers. **Minimum correction**: record the unresolved decisions as `[NEEDS CLARIFICATION]`/Pending Functional Validation, or cite objective approval evidence for both dependencies.

## Clean Architecture and Dependency Direction

- [x] CHK015 Is `fiscalpreparation` justified as the capability name by the bounded outcome, with no name implying XML generation, signing, SRI transmission, or complete fiscal issuance? [Clarity, Spec §Bounded Outcome, Plan §Structure Decision]
- [x] CHK016 Are the permitted dependency directions for `api`, `application`, `domain`, and `infrastructure` explicitly stated and consistent with Constitution Principle IV? [Completeness, Plan §Project Structure, Constitution Principle IV]
- [x] CHK017 Is the domain boundary specified as synchronous and framework-free, excluding Mutiny, HTTP/JSON, Quarkus, Jakarta REST, Panache, PostgreSQL, security, and provider DTO types? [Purity, Spec §DR-001–DR-011, Plan §Constitution Check]
- [x] CHK018 Are Application responsibilities limited to use-case ordering, transport-neutral commands/results, business preconditions, and ports, with no dependency on API or Infrastructure? [Clarity, Spec §FR-018–FR-064, Plan §Structure Decision]
- [x] CHK019 Are API responsibilities limited to header/path/representation classification, DTO mapping, deadline result selection, correlation, and HTTP status/error mapping? [Completeness, Spec §FR-010–FR-017/FR-065–FR-075, Plan §API and async quality gate]
- [x] CHK020 Are Infrastructure responsibilities explicit for REST Client mapping, CSPRNG policy execution, reactive persistence, row locking, SQLSTATE/constraint classification, and reconciliation? [Completeness, Spec §FR-018–FR-064, Plan §Structure Decision]
- [x] CHK021 Is every planned interface tied to an actual boundary—authoritative fiscal source, atomic persistence store, identifier/code generation—without a speculative repository hierarchy, generic unit of work, or factory layer? [Simplicity, Spec §FR-018/FR-060, Plan §Complexity Tracking]
- [x] CHK022 Is the narrow extraction of request-context, Problem Details, deadline, clock, and operation-budget behavior specified precisely enough to preserve Feature 001 semantics without creating a generic request framework? [Consistency, Plan §Structure Decision, Plan §Source Conflicts and Resolutions]

## Company Context and Ownership Scoping

- [x] CHK023 Is exactly one mandatory `X-Company-Id` defined as the sole Company input, including SP/HTAB trimming, UUID grammar, nil rejection, canonical form, and repeated/combined-value rejection? [Clarity, Spec §FR-010–FR-012, Contract §CompanyContext]
- [x] CHK024 Are `COMPANY_CONTEXT_REQUIRED` and `COMPANY_CONTEXT_INVALID` requirements explicit about zero Company-owned reads, external fiscal reads, baseline access, and mutations before valid context? [Completeness, Spec §FR-011/FR-067, Plan §Company Header Contract]
- [x] CHK025 Is Company Identifier consistently prohibited from request body, input schema, path, query, token, session, and response, with no undocumented exception? [Consistency, Spec §FR-012/FR-017, Contract §FiscalPreparationResponse]
- [x] CHK026 Are all Company-owned operations enumerated as scoped operations: preflight draft/replay read, locked draft read, preparation read/insert, baseline lookup/lock/update, foreign-key linkage, uniqueness, and reconciliation? [Coverage, Spec §FR-013, Plan §Company Ownership Scoping]
- [x] CHK027 Is the safe cross-Company behavior defined as indistinguishable from an absent draft, including zero leakage and zero state crossing? [Clarity, Spec §FR-014, Spec §SC-006]
- [x] CHK028 Is Company ownership clearly distinguished from the fiscal numbering dimension, so the exact Issuer/Establishment/Emission Point/document-type scope cannot be duplicated merely under another Company? [Consistency, Spec §FR-034–FR-035/DR-005, Data Model §OfficialSequenceScope]
- [x] CHK029 Is propagation of the canonical Company UUID to the external fiscal-context boundary specified only as business selection context and never as an authentication credential? [Clarity, Spec §FR-019/FR-024, Outbound Contract §CompanyContext]
- [x] CHK030 Are authentication, authorization, Keycloak, JWT/OIDC, principals, roles, Company lookup/status, local Company master data, and Company administration consistently absent from requirements, plan, and contracts? [Coverage, Spec §FR-015–FR-016/FR-077, Plan §Internal Caller Boundary]

## External Fiscal-Context Ordering and Boundary Quality

- [x] CHK031 Is the first-preparation ordering explicit as valid Company/correlation → Company-scoped replay/draft lookup → fixed-date eligibility → authoritative fiscal resolution/validation → local commit attempt? [Clarity, Spec §FR-007/FR-033/FR-057/FR-063, Plan §Transaction and Lock State Transitions]
- [x] CHK032 Is replay ordering unambiguous that a valid committed preparation is returned before current-date validation, provider access, baseline access, identity generation, or new timestamp capture? [Consistency, Spec §FR-005/FR-031/FR-057, Spec §SC-007]
- [x] CHK033 Are the only permitted fiscal selection inputs documented as canonical Company context, exact draft Emission Point UUID, unchanged emission date, and invoice Document Type Code `01`? [Completeness, Spec §FR-018–FR-020, Outbound Contract §FiscalContextSelection]
- [x] CHK034 Does the external contract require one unambiguous, eligible, supported, effective, versioned result with all required Issuer, Establishment, Emission Point, designation, and source-evidence fields? [Completeness, Spec §FR-020/FR-025–FR-028, Outbound Contract §AuthoritativeFiscalContext]
- [x] CHK035 Are unavailable, missing, invalid, unsupported, ineffective, ineligible, ambiguous, and inconsistent source outcomes assigned unambiguous local error categories before sequence allocation? [Clarity, Spec §FR-021–FR-024/FR-033, Plan §Stable Error-to-HTTP Mapping]
- [x] CHK036 Is it explicit that the read-only provider result is fully validated before the database transaction and that no external operation is claimed atomic with PostgreSQL? [Consistency, Spec §FR-033/FR-063, Plan §Data and External Consistency Design]
- [x] CHK037 Are connection timeout, response timeout, overall-deadline clamping, one-attempt behavior, retry classification, and the documented reason for no circuit breaker all specified? [Non-Functional Completeness, Spec §FR-022/FR-068–FR-071, Research §Authoritative Fiscal Context Boundary]
- [x] CHK038 Is the permitted concurrency consequence—duplicate read-only fiscal resolutions among simultaneous initial contenders but exactly one committed local winner—documented without claiming provider exactly-once behavior? [Clarity, Spec §FR-058/FR-063, Research §Authoritative Fiscal Context Boundary]

## Transaction Atomicity, Lock Ordering, and Sequence Baseline

- [x] CHK039 Is the all-or-nothing local boundary defined to include preparation identity, draft link, snapshot, sequence, Numeric Code, Access Key, creation instant, and baseline advancement in one transaction? [Completeness, Spec §FR-060–FR-063/DR-003, Plan §Data and External Consistency Design]
- [x] CHK040 Is the absence of any durable provisional, reservation, `PREPARING`, or partial snapshot state explicit and consistent across specification and data model? [Consistency, Spec §FR-008–FR-009/FR-061, Data Model §Transaction and Lock State Transitions]
- [x] CHK041 Is draft-first pessimistic locking stated as the mandatory first row lock using the authoritative Company-plus-draft predicate? [Clarity, Spec §FR-013/FR-058, Plan §Reactive and Resource Boundary Design]
- [x] CHK042 Is a second same-draft winner lookup under the draft lock required before baseline access or Numeric Code/Access Key generation? [Completeness, Spec §FR-057–FR-058, Data Model §First preparation]
- [x] CHK043 Is exact-baseline locking stated as the mandatory second row lock only after draft eligibility and winner recheck? [Clarity, Spec §FR-033/FR-042/FR-058, Plan §Summary]
- [x] CHK044 Is the fixed draft-then-baseline order justified for both same-draft arbitration and deadlock avoidance, with no alternative order left implicit? [Consistency, Spec §FR-042/FR-058, Research §Official Sequence Allocation and Local Atomicity]
- [x] CHK045 Are concurrent different-draft allocations in one fiscal scope specified to serialize only on the exact baseline and produce the next committed contiguous set in lock-acquisition order? [Coverage, Spec §FR-042/SC-003, Data Model §Concurrent different drafts]
- [x] CHK046 Is the exact sequence scope complete and stable—Issuer reference, Establishment reference, Emission Point reference, official 3+3 codes, and document type `01`—while Company and environment are correctly excluded as numbering dimensions? [Clarity, Spec §FR-034–FR-035/DR-005, Data Model §OfficialSequenceScope]
- [x] CHK047 Is baseline state modeled unambiguously as an explicitly provisioned `lastAllocated` value `0..999999999`, with derived next value, explicit exhaustion, and no inference that an absent row means zero? [Clarity, Spec §FR-037–FR-040, Data Model §Official Sequence Baseline]
- [x] CHK048 Are absent, duplicated, regressed, out-of-range, scope-inconsistent, and exhausted baseline states each defined to fail closed without creation, repair, wrap, reset, or mutation? [Edge Coverage, Spec §FR-038–FR-040/SC-009]
- [x] CHK049 Is the rejection of PostgreSQL `nextval`, `max+1`, JVM counters, advisory locks, and out-of-transaction allocation documented by their specific atomicity/concurrency shortcomings? [Design Rationale, Spec §FR-042/FR-060–FR-062, Research §Official Sequence Allocation and Local Atomicity]

## Natural Idempotency, Concurrency, and Commit Uncertainty

- [x] CHK050 Is natural equivalence defined only as normalized Company Identifier plus Invoice Draft Identifier, with no caller idempotency key, correlation value, timing, or transport metadata in the identity? [Clarity, Spec §FR-054–FR-055, Contract §operation description]
- [x] CHK051 Are first commit and replay outcomes distinguished observably without making the replay indicator part of business equivalence or fiscal identity? [Consistency, Spec §FR-005/FR-054–FR-056, Inbound Contract §201/200]
- [x] CHK052 Are 100 equivalent concurrent requests specified to converge on one preparation, sequence, Numeric Code, Access Key, and creation timestamp, with every success identifying the same winner? [Measurability, Spec §FR-058/SC-004]
- [x] CHK053 Is a committed replay required to return every originally persisted fiscal and source-evidence value unchanged and perform zero provider, baseline, generator, or clock work? [Completeness, Spec §FR-031/FR-056–FR-059, Spec §SC-007]
- [x] CHK054 Is response loss after commit distinguished from unknown database commit outcome, with natural replay defined as the recovery mechanism for both? [Clarity, Spec §FR-059/FR-064, Research §Natural Replay and Commit Uncertainty]
- [x] CHK055 Are confirmed pre-commit failure, confirmed rollback, acknowledged commit, and genuinely unknown outcome defined as mutually exclusive knowledge states with accurate zero-state guarantees? [Consistency, Spec §FR-061/FR-064/FR-067/FR-072]
- [x] CHK056 Is reconciliation specified to occur after the failed transaction ends, using Company plus Invoice Draft, returning a complete winner when found and never directly allocating a replacement while outcome remains unknown? [Recovery Coverage, Spec §FR-064, Data Model §Persistence Failure and Outcome Knowledge]
- [x] CHK057 Are SQLSTATE phase, rollback acknowledgement, and named constraint identity required to distinguish a same-draft race winner from Access Key, scoped-sequence, FK, or check-constraint defects? [Clarity, Spec §FR-058/FR-064, Data Model §Persistence Failure and Outcome Knowledge]
- [x] CHK058 Is genuine lost-COMMIT-acknowledgement evidence distinguished from a mocked persistence exception and from post-commit HTTP response loss? [Evidence Quality, Spec §SC-010, Plan §Complexity Tracking]
- [x] CHK059 Are overall deadline outcomes explicit for commit-not-started, rollback-confirmed, commit-acknowledged, and commit-possibly-succeeded cases, including prohibition of false zero-state claims? [Edge Coverage, Spec §FR-068–FR-072]

## SRI v2.33 Access Key and Numeric Code Correctness

- [x] CHK060 Is SRI Offline Technical Sheet v2.33, updated July 2026 and modified 2026-07-13, the sole stated technical authority for this Access Key? [Traceability, Spec §FR-045/DR-006, Plan §Source and Terminology Evidence]
- [x] CHK061 Is the exact 49-digit composition documented in authoritative order and widths `8+2+13+1+3+3+9+8+1+1`? [Clarity, Spec §FR-046, Data Model §AccessKey]
- [x] CHK062 Is the six-digit series explicitly defined as authoritative Establishment Code followed immediately by authoritative Emission Point Code? [Clarity, Spec §FR-047]
- [x] CHK063 Is Modulo 11 completely specified as right-to-left cyclic weights `2..7`, weighted sum, `11 - sum mod 11`, with raw `11→0` and `10→1` mappings? [Clarity, Spec §FR-048/DR-008]
- [x] CHK064 Does final validation require both syntactic/check-digit correctness and equality of every parsed component to the draft, snapshot, allocation, and generated Numeric Code? [Completeness, Spec §FR-049/SC-005]
- [x] CHK065 Are invoice code `01`, environment `1|2`, normal emission type `1`, RUC length 13, 3+3 series, 9-digit sequence, and 8-digit Numeric Code constraints stated consistently across spec, model, and contracts? [Consistency, Spec §FR-027/FR-044/FR-046–FR-050, Inbound Contract §FiscalContextSnapshot]
- [x] CHK066 Is the draft emission date specified as the unchanged Ecuador civil date formatted `ddMMyyyy`, with no UTC shift, current-date substitution, or correction? [Clarity, Spec §FR-007/FR-050/DR-004]
- [x] CHK067 Is Numeric Code policy `SECURE_RANDOM_8_V1` documented as uniform `0..99,999,999`, exactly eight digits, leading-zero preserving, caller-independent, and accepting `00000000`? [Clarity, Spec §FR-043–FR-044, Research §Numeric Code Policy]
- [x] CHK068 Is RUC validation intentionally limited to 13 decimal digits plus authoritative-source equality, with the decision not to invent one generic local checksum documented? [Design Clarity, Spec §FR-027/FR-049, Research §Access Key Composition and Validation]
- [x] CHK069 Are independently recalculated official vectors, synthetic raw-10/raw-11 vectors, every component mutation, and the invalid page-64 printed vector all required as evidence? [Coverage, Spec §FR-053/SC-005, Research §Access Key Composition and Validation]

## Flyway and Persistence Invariant Quality

- [x] CHK070 Is Flyway defined as the sole schema authority, with V1–V5 immutable and one new migration after V5 rather than modification of committed migrations? [Consistency, Spec §FR-060, Plan §Technical Context, Constitution Principle IX]
- [x] CHK071 Is the migration explicitly required to create an empty controlled-baseline structure with no seed, default scope, runtime initializer, or administration endpoint? [Completeness, Spec §FR-038/FR-078, Plan §Project Structure]
- [x] CHK072 Are both empty-database creation and V5-to-Feature-002 upgrade evidence required on exact PostgreSQL 18.4, including prior migration checksum preservation? [Measurability, Plan §Test and Operational Evidence Plan, Constitution Principle IX]
- [x] CHK073 Is the two-structure design justified as one mutable baseline plus one append-only preparation row embedding the logical snapshot, without a redundant master model, JSONB snapshot, or provisional table? [Simplicity, Spec §FR-025/FR-029/FR-060, Data Model §Modeling Boundaries]
- [x] CHK074 Are database invariants complete for non-nil ownership, Company-consistent draft/baseline links, Company-plus-draft natural uniqueness, global exact-scope uniqueness, scoped sequential uniqueness, and global Access Key uniqueness? [Completeness, Spec §FR-009/FR-013/FR-035/FR-042/FR-052, Data Model §Fiscal Preparation]
- [x] CHK075 Are exact digit representations specified as bounded `varchar` values with ASCII checks, avoiding padding or loss of leading zeros from `char(n)` or numeric types? [Clarity, Spec §FR-027/FR-041/FR-044/FR-046, Data Model §Value Objects]
- [ ] CHK076 Are required columns non-null and every legitimate optional column documented with effective-interval or paired-designation constraints that reject partial state? [Null/Invariant Completeness, Spec §FR-026–FR-028/DR-011, Data Model §Fiscal Context Snapshot]
  - **Finding — HIGH**: FR-028 universally couples conditional fiscal designations with resolution identifiers, while `research.md`, `data-model.md`, and the outbound contract state that accounting obligation and RIMPE have no resolution identifier; the authoritative artifacts therefore define incompatible valid/invalid states. **Affected**: `spec.md` §FR-028; `research.md` §Fiscal Context Snapshot; `data-model.md` §Conditional fiscal designations; outbound contract §AuthoritativeFiscalContext. **Minimum correction**: limit designation-resolution pairing to the designations for which the governing source requires a resolution and align the specification wording with the model and contract.
- [x] CHK077 Is committed Fiscal Preparation immutability protected at both model/repository contract and database update/delete boundary, with no cancellation or delete path implied? [Completeness, Spec §FR-008/DR-001–DR-003, Data Model §Immutability and Retention]
- [x] CHK078 Are baseline scope immutability, monotonic one-step advancement, `updatedAt`, maximum exhaustion, and prohibition of decrement/reset/jump tied clearly to the corresponding successful preparation transaction? [Clarity, Spec §FR-037–FR-042/FR-060–FR-062, Data Model §Official Sequence Baseline]

## OpenAPI and Error-Contract Consistency

- [x] CHK079 Is the inbound operation consistently defined as bodyless `POST /api/v1/invoice-drafts/{invoiceDraftId}/fiscal-preparation` with a non-nil draft UUID and no editable fiscal content? [Consistency, Spec §FR-001–FR-002/FR-036/FR-043/FR-045, Inbound Contract §path]
- [x] CHK080 Is non-empty or prohibited preparation content assigned deterministically to `INVALID_REQUEST` without permitting fiscal fields or another idempotency key to influence equivalence? [Clarity, Spec §FR-002/FR-036/FR-054, Acceptance Scenario 22]
- [x] CHK081 Are `201` new and `200` replay responses specified with one identical committed representation, stable correlation, replay classification, and no provisional/job response? [Completeness, Spec §FR-005/FR-056/FR-074, Inbound Contract §responses]
- [x] CHK082 Are all stable error codes mapped consistently to HTTP status, safe Problem Details fields, retry guidance, and the exact local-state guarantee from FR-067? [Consistency, Spec §FR-065–FR-072, Plan §Stable Error-to-HTTP Mapping]
- [x] CHK083 Is correlation behavior complete for absent, valid, repeated, blank, unsafe, and over-length inputs, including safe replacement and exclusion from equivalence? [Coverage, Spec §FR-055/FR-073, Inbound Contract §CorrelationId]
- [x] CHK084 Is the successful-response exception for complete RUC/name/address/Access Key data explicit, while errors and all operational signals remain redacted and responses are non-cacheable? [Clarity, Spec §FR-075, Inbound Contract §FiscalPreparationResponse/NoStore]
- [x] CHK085 Are security schemes, security requirements, `Authorization`, authentication APIs, and `401`/`403` responses explicitly absent from inbound and outbound contracts? [Coverage, Spec §FR-015/FR-024/FR-076, Constitution Principle VII]
- [x] CHK086 Is the runtime OpenAPI source-of-truth strategy explicit: merge Feature 002 into `META-INF/openapi.yaml`, preserve Feature 001 semantically, and avoid obsolete whole-file equality assumptions? [Consistency, Plan §Source Conflicts and Resolutions, Plan §Contract Verification]
- [x] CHK087 Is the outbound consumer contract explicitly read-only despite POST selection semantics, with exact Company/header/body fields, source evidence, provider errors, and no provider master-data or security contract invented locally? [Clarity, Spec §FR-018–FR-024, Outbound Contract §operation]

## Warning-Free Null Safety

- [x] CHK088 Is the current gap accurately stated—that `javac -Xlint` and test-only JSpecify do not by themselves enforce production nullness—so readiness does not rely on a false baseline claim? [Clarity, Spec §FR-081/SC-016, Plan §Source Conflicts and Resolutions]
- [x] CHK089 Are the chosen nullness mechanisms and versions explicit—JSpecify 1.0.0, Error Prone plugin 5.1.0, Error Prone 2.50.0, and NullAway 0.13.7—with Java 25 compatibility evidence required? [Dependency, Spec §FR-081, Research §Null Safety and Warning Policy]
- [x] CHK090 Is every new or modified Feature 002 and extracted shared package required to be `@NullMarked`, with a requirement preventing silently unmarked owned code? [Completeness, Spec §DR-011/FR-081, Plan §Null safety evidence]
- [x] CHK091 Are required values non-null by default and legitimate absence represented by `Optional`, sealed state, or dedicated value object rather than undocumented null? [Clarity, Spec §DR-011, Data Model §Modeling Boundaries]
- [x] CHK092 Are unavoidable nullable JSON/ORM lifecycle fields, optional database columns, and Panache missing-result semantics confined to mapper boundaries and converted immediately to explicit states? [Boundary Coverage, Spec §FR-081/DR-011, Data Model §Application Port Models]
- [x] CHK093 Is zero-warning acceptance measurable across NullAway/JSpecify, generic inference, `javac -Xlint:all -Werror`, and test compilation, rather than described only as an aspiration? [Measurability, Spec §SC-016, Quickstart §Quality and Null-Safety Gate]
- [x] CHK094 Are broad suppression, `@NullUnmarked`, analyzer exclusions, warning downgrades, and new `@SuppressWarnings` explicitly rejected as substitutes for fixing owned-code causes? [Clarity, Spec §FR-081/SC-016, Research §Null Safety and Warning Policy]

## Sensitive Data, Observability, and Operational Requirements

- [x] CHK095 Are RUC, Legal/Commercial Names, addresses, source evidence, Numeric Code, Access Key, raw provider data, credentials, internal endpoints, SQL, and baseline values consistently classified for storage/response/redaction purposes? [Completeness, Spec §FR-032/FR-065/FR-075, Plan §Sensitive Data]
- [x] CHK096 Is the exact successful-response need for sensitive fiscal fields distinguished from their prohibition in errors, logs, traces, metric labels, health, audit payloads, and production-derived fixtures? [Consistency, Spec §FR-075/SC-014, Plan §Structured Observability]
- [x] CHK097 Are provider request/response bodies, credentials, and internal provider errors explicitly excluded from snapshot persistence and caller-visible failures? [Coverage, Spec §FR-032, Plan §Sensitive Data and Certificate Lifecycle]
- [ ] CHK098 Are platform TLS, encryption-at-rest, backup, and Invoice-record retention responsibilities identified clearly enough to avoid an unowned sensitive-data control or an invented deletion API? [Dependency, Spec §FR-075, Plan §Sensitive Data]
  - **Finding — HIGH**: TLS, encryption at rest, backups, and Invoice-record retention are delegated to unspecified platform controls/policy without a cited owner, approved control reference, retention rule, or required evidence, leaving sensitive fiscal storage controls unowned. **Affected**: `plan.md` §Technical Context/§Company Context and Sensitive-Data Design; `research.md` §Health, Observability, Sensitive Data, and Native Status; `data-model.md` §Immutability and Retention. **Minimum correction**: reference the approved platform and Invoice-retention controls, their accountable owner, and the evidence required for this feature without adding a deletion API.
- [x] CHK099 Are observability fields limited to safe correlation, bounded outcome/error labels, timings, deadline owner, replay/new classification, and commit-knowledge class without high-cardinality fiscal identifiers? [Clarity, Spec §FR-065/FR-073/FR-075, Plan §Structured Observability]
- [x] CHK100 Are sanitized audit-event requirements complete for committed, replayed, baseline-failure, rollback, and unknown-outcome transitions without creating a second fiscal payload store? [Completeness, Spec §FR-065/FR-075, Plan §Audit Events]
- [x] CHK101 Are liveness and readiness meanings explicit—process viability versus PostgreSQL/Flyway readiness—while provider outage remains a business failure and committed replay remains available? [Clarity, Spec §FR-022/FR-031, Plan §Liveness and Readiness]

## Packaged JVM and Evidence Quality

- [x] CHK102 Is packaged JVM execution mandatory and native compatibility explicitly deferred/unclaimed unless actual native build and runtime evidence are later supplied? [Consistency, Plan §Native Compatibility Evaluation, Constitution Principle III]
- [x] CHK103 Are evidence requirements complete across pure domain, Application ordering, provider consumer contract, PostgreSQL persistence, Flyway, API/OpenAPI, architecture, sensitive-data, and packaged JVM levels? [Coverage, Spec §SC-001–SC-016, Plan §Test and Operational Evidence Plan]
- [x] CHK104 Are concurrency criteria quantified as 100 equivalent requests and 100 different drafts in one scope, including exact committed cardinality, values, uniqueness, and locally caused gap rules? [Measurability, Spec §SC-003–SC-004]
- [x] CHK105 Are PostgreSQL evidence requirements pinned to 18.4 for empty migration, upgrade, row locking, rollback, constraint, timeout, and concurrency semantics rather than an in-memory substitute? [Dependency, Plan §Technical Context/Test and Operational Evidence Plan]
- [x] CHK106 Are provider and database outage, timeout, cancellation, response-loss, rollback, and commit-uncertainty evidence scenarios distinguished by their required observable state claims? [Recovery Coverage, Spec §FR-061–FR-072/SC-008–SC-010]
- [x] CHK107 Are event-loop non-blocking, CSPRNG startup/warm-up, pool-size 20 contention, deadline clamping, and post-load recovery requirements measurable in packaged JVM evidence? [Non-Functional Completeness, Plan §Reactive and Resource Boundary Design]
- [x] CHK108 Are the proposed validation commands and expected evidence artifacts specific enough for a reviewer to decide readiness without using coverage percentage as a proxy for requirement satisfaction? [Clarity, Quickstart §Quality and Null-Safety Gate/JVM Runtime Evidence]

## Explicit Exclusion Coverage

- [x] CHK109 Are XML generation/schema validation, PKCS#12/certificate handling, signing, SRI reception/authorization communication, SRI retry, and SRI reconciliation consistently excluded from requirements, plan, dependencies, contracts, and evidence scope? [Coverage, Spec §FR-076/SC-013]
- [x] CHK110 Are RIDE/PDF, email, webhook, queue, event, notification, storage, rendering, and delivery side effects explicitly absent from the bounded outcome and architecture? [Coverage, Spec §FR-076/SC-013, Plan §External Integrations]
- [x] CHK111 Are Company, Issuer, Establishment, and Emission Point administration and complete/local editable master-data replication excluded while the minimal immutable snapshot remains clearly allowed? [Boundary Consistency, Spec §FR-025/FR-029/FR-077]
- [x] CHK112 Are manual sequential assignment, baseline creation/repair/reset/administration, caller sequence override, cancellation, reversal, and reuse excluded without undermining the required controlled baseline dependency? [Coverage, Spec §FR-036/FR-038–FR-040/FR-078]
- [x] CHK113 Are Invoice Draft mutation, recalculation, normalization, date correction, commercial-data copying beyond required evidence, and timestamp changes explicitly prohibited? [Completeness, Spec §FR-006–FR-007/DR-009/SC-012]
- [x] CHK114 Are every non-Invoice tax document type, batch preparation, and any document type other than `01` excluded before sequence allocation? [Coverage, Spec §FR-079/DR-007]
- [x] CHK115 Are legacy API/database/status/error compatibility, authentication/authorization behavior, and asynchronous legacy queue flows explicitly non-governing? [Coverage, Spec §FR-080, Plan §Source Conflicts and Resolutions]

## Dependencies, Assumptions, and Readiness Conflicts

- [x] CHK116 Is Feature 001's authority over draft ownership, lifecycle, Emission Point reference, emission date, commercial data, totals, and immutability documented without creating a hidden cross-capability mutation contract? [Dependency, Spec §Assumptions and Dependencies, Plan §Company and Existing-Draft Boundary]
- [ ] CHK117 Is the approved fiscal-context provider dependency defined by authoritative ownership, immutable revision, effective-period semantics, bounded availability, and the consumer contract without requiring local master administration? [Dependency, Spec §Assumptions and Dependencies, Outbound Contract]
  - **Finding — BLOCKING**: The artifacts describe a required consumer boundary but do not identify an actually approved provider capability, accountable owner, accepted interface version, or contract-acceptance evidence; the outbound server remains a `.invalid` placeholder, so provider behavior and approval cannot be treated as established. **Affected**: `spec.md` §FR-018 and §Assumptions and Dependencies; `plan.md` §External Integrations; `contracts/authoritative-fiscal-context.openapi.yaml` §info/servers. **Minimum correction**: record the approved provider capability and owner plus objective acceptance/version evidence for the consumer contract, without inventing provider implementation or master administration.
- [ ] CHK118 Is the separately controlled baseline-provisioning dependency explicit enough that Feature 002 can succeed when provisioned but can neither invent nor expose the missing administration capability? [Dependency, Spec §FR-037–FR-040/Assumptions and Dependencies]
  - **Finding — BLOCKING**: No authoritative artifact identifies or approves the external production process/capability and owner that provisions controlled baseline rows; V6 is intentionally seedless, Feature 002 forbids administration/auto-initialization, and `quickstart.md` permits only direct test-fixture inserts. **Affected**: `spec.md` §FR-037–FR-040 and §Assumptions and Dependencies; `plan.md` §Project Structure; `data-model.md` §Controlled Aggregate: Official Sequence Baseline; `quickstart.md` §Prerequisites. **Minimum correction**: reference and approve a separately controlled provisioning responsibility and evidence path that can establish valid production baselines without expanding Feature 002 scope.
- [x] CHK119 Are the stale terminology mapping, Feature 001 architecture prohibition, and whole-runtime OpenAPI equality conflicts recorded as required lower-artifact corrections rather than constitutional deviations? [Conflict, Plan §Source Conflicts and Resolutions]
- [x] CHK120 Is the decision not to make Company-specific fiscal resolution a readiness probe documented with its replay-availability rationale and a condition for any future safe provider health contract? [Design Clarity, Plan §Liveness and Readiness]
- [x] CHK121 Are the fixed `America/Guayaquil` date rule, IANA timezone dependency, source observation instant, and creation instant responsibilities stated without conflating Product Owner policy with an SRI mandate? [Assumption, Spec §FR-007/FR-069/DR-004, Research §Ecuador Date and Time Semantics]
- [ ] CHK122 Are all remaining design dependencies and assumptions objectively resolvable during tasks/implementation without requiring a new material business decision or silently expanding scope? [Readiness, Spec §Assumptions and Dependencies, Plan §Pending Functional Validation]
  - **Finding — BLOCKING**: The unresolved provider approval, baseline provisioning ownership, terminology correction, conditional-designation conflict, and sensitive-control ownership cannot all be completed as mechanical implementation tasks without external approval or authoritative-artifact correction. **Affected**: `spec.md` §Assumptions and Dependencies/§Pending Functional Validation; `plan.md` §Pending Functional Validation and §Terminology Mapping Impact. **Minimum correction**: resolve CHK013, CHK014, CHK076, CHK098, CHK117, and CHK118 in their authoritative artifacts, then rerun this readiness gate.

## Review Notes

- `[x]` means the written requirement/design quality criterion is satisfied; it does not mean code
  or a task was completed.
- Record unresolved findings beside the affected item and update the authoritative specification or
  plan before generating tasks.
- A failed item involving authority, Company isolation, sequence atomicity, Access Key correctness,
  unknown-outcome recovery, null safety, or exclusions is a blocking readiness defect.

## Validation Result

- Total checklist items: 122
- Passed count: 115
- Incomplete count: 7
- Blocking finding count: 5
- High finding count: 2
- Medium finding count: 0
- Low finding count: 0
- Incomplete checklist identifiers: `CHK013`, `CHK014`, `CHK076`, `CHK098`, `CHK117`, `CHK118`, `CHK122`
- Final decision: `BLOCKED_BEFORE_TASKS`

Feature 002 is not ready for task generation because objective written evidence is still missing or contradictory for canonical terminology, dependency approval/provisioning, conditional fiscal-designation semantics, and sensitive-data control ownership. No `tasks.md` was generated.
