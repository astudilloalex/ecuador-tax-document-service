# Planning Requirements Checklist: Create Invoice Draft

**Purpose**: Validate the completeness, clarity, consistency, measurability, and risk coverage of
the approved specification and implementation plan before task generation

**Created**: 2026-07-12

**Feature**: [`spec.md`](../spec.md) and [`plan.md`](../plan.md)

**Audience/Timing**: Peer reviewer; standard-depth pre-task requirement-quality gate

**Focus**: Fiscal and monetary correctness, authorization and tenant isolation, idempotency,
external contracts, persistence consistency, and operational evidence

**Note**: This checklist tests the quality of the written requirements and planning decisions. It
MUST NOT be used as an implementation test or marked complete merely to unblock task generation.

## Requirement Completeness

- [ ] CHK001 Are all inputs, calculated outputs, persisted snapshots, identifiers, statuses, and timestamps needed to review a complete invoice draft explicitly enumerated? [Completeness, Spec §FR-005, Spec §FR-022]
- [ ] CHK002 Are the supported buyer identification types, IVA treatments, payment rules, text fields, and collection limits completely specified without relying on legacy behavior? [Completeness, Spec §FR-007–FR-015]
- [ ] CHK003 Are every prohibited fiscal side effect and every excluded document or administrative capability stated consistently enough to prevent accidental scope expansion? [Completeness, Spec §Exclusions and Non-Goals, Spec §FR-023]
- [ ] CHK004 Are persistence requirements complete for the draft aggregate, child records, catalog references, immutable snapshots, and idempotency binding lifetime? [Completeness, Spec §FR-020–FR-021, Spec §FR-030, Plan §Data Model]
- [ ] CHK005 Are requirements present for every externally observable API outcome identified by the plan, including new creation, replay, conflict, malformed input, business rejection, concealed denial, dependency failure, timeout, and unexpected rollback? [Completeness, Spec §FR-024–FR-026, Plan §API and Error Contract]

## Requirement Clarity

- [ ] CHK006 Is the exact line-level rounding pipeline unambiguous about operation order, intermediate scale, aggregation sources, overflow handling, and payment reconciliation? [Clarity, Spec §DR-002–DR-010]
- [ ] CHK007 Are the distinctions among percentage-rate IVA, IVA 0%, not subject to IVA, and exempt from IVA clear for selection, calculation, persistence, grouping, and response representation? [Clarity, Spec §FR-011, Spec §DR-005, Spec §DR-008]
- [ ] CHK008 Is “current Ecuadorian civil date at creation” tied to one unambiguous creation instant, including midnight-boundary behavior and replay after the original emission date? [Clarity, Spec §FR-006, Spec §DR-012, Plan §Date and Time Semantics]
- [ ] CHK009 Is a complete stable error-code catalog defined for each transport, authorization, idempotency, catalog, monetary, dependency, and persistence failure rather than only a code format and selected examples? [Gap, Spec §FR-025, Plan §API and Error Contract]
- [ ] CHK010 Is canonical decimal equivalence defined for numerically equal representations such as `2`, `2.0`, and `2.000000` when normalized idempotency content is compared? [Ambiguity, Spec §FR-029, Plan §Idempotency Arbitration and Normalized Content]
- [ ] CHK011 Are case sensitivity, Unicode normalization, and exact comparison rules defined for idempotency keys, buyer text, product codes, and additional-information names wherever equality or uniqueness matters? [Ambiguity, Spec §FR-015, Spec §FR-027, Spec §DR-019]

## Requirement Consistency

- [ ] CHK012 Do the specification, data model, and OpenAPI contract consistently reject every caller-supplied calculated field, tax code, and tax rate rather than ignore or reconcile it? [Consistency, Spec §FR-011–FR-012, Spec §DR-011]
- [ ] CHK013 Do monetary input patterns, database bounds, calculation rules, and response scales agree for quantity, unit price, discount, rates, bases, taxes, payments, and totals? [Consistency, Spec §DR-010, Plan §Exact Decimal Storage and Validation, Contract §Decimal Schemas]
- [ ] CHK014 Do zero-value draft requirements agree across line eligibility, mandatory tax treatment, payment cardinality, non-negative values, final-consumer threshold, and success criteria? [Consistency, Spec §FR-013–FR-014, Spec §DR-015–DR-016, Spec §SC-011]
- [ ] CHK015 Are current Company authorization and mutable Issuer/emission-point eligibility separated consistently so authorized replays return the original snapshot without bypassing revoked access? [Consistency, Spec §FR-033, Plan §Effective Authorization Scope, Contract §Company Context Port]
- [ ] CHK016 Are all source versions, terminology classifications, authority-order decisions, and legacy conflict resolutions consistent between the specification, research, plan, and canonical terminology mapping? [Consistency, Spec §Authority and Evidence, Plan §Source and Terminology Evidence]

## Acceptance Criteria Quality

- [ ] CHK017 Does every functional requirement and domain invariant have at least one objectively observable acceptance scenario or measurable outcome, especially error, security, and recovery requirements? [Traceability, Spec §User Scenarios & Testing, Spec §FR-001–FR-035, Spec §DR-001–DR-022]
- [ ] CHK018 Are calculation criteria sufficiently exact to distinguish compliant line-level `HALF_UP` aggregation from alternative invoice-level or unrounded aggregation methods? [Measurability, Spec §Scenario 2, Spec §DR-010, Spec §SC-003–SC-004]
- [ ] CHK019 Can tenant isolation and concealment criteria objectively establish both absence of foreign data disclosure and absence of mutation for Company, Issuer, emission-point, binding, and draft records? [Measurability, Spec §FR-024, Spec §SC-006]
- [ ] CHK020 Are performance goals measurable with a defined payload mix, warm-up state, dependency latency assumptions, observation window, percentile method, and resource profile? [Gap, Plan §Performance Goals]

## Scenario Coverage

- [ ] CHK021 Are primary creation and review requirements complete from authenticated context resolution through the returned persisted aggregate without implying a later retrieval or issuance capability? [Coverage, Spec §User Story 1, Spec §FR-022–FR-023]
- [ ] CHK022 Are alternate valid scenarios covered for every buyer type, each supported IVA treatment, multiple lines, multiple unique payments, optional contact information, omitted additional information, and zero-total drafts? [Coverage, Spec §Acceptance Scenarios, Spec §FR-007–FR-016]
- [ ] CHK023 Are exception requirements complete for inactive, unknown, not-yet-effective, expired, contradictory, and unsupported Company, Issuer, emission-point, identification, tax, and payment references? [Coverage, Spec §Edge Cases, Spec §DR-013]
- [ ] CHK024 Are concurrency requirements complete for equivalent commands, different-content commands, transaction losers, nullable tenant scope, independent companies, and response loss after commit? [Coverage, Spec §FR-027–FR-033]
- [ ] CHK025 Are recovery requirements explicit for company dependency timeout/unavailability, PostgreSQL rollback or timeout, client cancellation, response-delivery failure, and subsequent retry? [Coverage, Recovery, Spec §FR-021, Spec §FR-032, Plan §Data and External Consistency Design]

## Edge Case Coverage

- [ ] CHK026 Are all monetary boundaries addressed, including zero, maximum accepted precision, half-cent rounding, discount equal to gross, calculated overflow, grouped-total overflow, and payment-sum overflow? [Edge Case, Spec §DR-004, Spec §DR-010, Spec §DR-016]
- [ ] CHK027 Are buyer-identification boundaries complete for every supported type, RUC classes without an official checksum, exact final-consumer identity, threshold equality, and threshold exceedance after rounding? [Edge Case, Spec §DR-014–DR-015]
- [ ] CHK028 Are text boundaries complete for blank-after-trimming, control characters, maximum length, invalid contact syntax, duplicate trimmed additional-information names, and non-ASCII input? [Edge Case, Spec §FR-008, Spec §FR-010, Spec §FR-015, Spec §FR-035]
- [ ] CHK029 Are collection boundaries and ordering semantics explicit at zero, one, maximum, and maximum-plus-one entries, including which collection order changes idempotency equivalence? [Edge Case, Spec §FR-009, Spec §FR-013, Spec §FR-015, Spec §DR-021–DR-022]

## Non-Functional Requirements

- [ ] CHK030 Are authentication requirements complete for signature, issuer, both required audiences, expiration, authorized party, subject, role, deny-by-default behavior, and token propagation restrictions? [Completeness, Plan §Security and Ownership Design, Contract §Company Context Port]
- [ ] CHK031 Is the required API outcome for a request exceeding the 2 MiB limit defined, including its stable error code and correlation behavior? [Gap, Plan §Constraints, Plan §API and Error Contract]
- [ ] CHK032 Are sensitive-data requirements complete for storage, backups, transport, logs, errors, metrics, traces, audit records, normalized idempotency content, fixtures, and lifecycle retention? [Completeness, Spec §FR-025, Plan §Sensitive Data]
- [ ] CHK033 Are audit requirements specific about required events, durable destination, access controls, retention, failure handling, and which identifiers may be recorded safely? [Gap, Constitution §Observability and Operations, Plan §Audit Events]
- [ ] CHK034 Are liveness, readiness, metrics, tracing, JVM evidence, and conditional native-compatibility requirements measurable and consistent with the exact business dependency destinations? [Measurability, Plan §Test and Operational Evidence Plan, Plan §Native Compatibility Evaluation]

## Dependencies and Assumptions

- [ ] CHK035 Are ownership, versioning, effective dating, authoritative source, baseline contents, and change procedures defined for identification, tax-rule, and payment-method catalogs? [Dependency, Spec §DR-001, Plan §Company Context and Reference Catalogs]
- [ ] CHK036 Is the Company capability contract explicit about authorization scope, eligible versus ineligible creation context, concealment, response validity, timeout budgets, and replay after mutable context changes? [Dependency, Contract §Company Context Port]
- [ ] CHK037 Are assumptions about Company-to-Issuer cardinality, optional buyer contact data, synchronous result delivery, and Company/tenant boundaries supported by approved requirements rather than inferred implementation convenience? [Assumption, Spec §Assumptions and Dependencies]

## Ambiguities and Conflicts

- [ ] CHK038 Is replay behavior unambiguous when the caller remains authorized but the Company becomes inactive, as distinct from Issuer or emission-point data merely changing? [Ambiguity, Spec §FR-003, Spec §FR-033]
- [ ] CHK039 Is failure precedence specified when a reused idempotency key accompanies malformed representation, different business content, invalid current business data, or an unavailable Company dependency? [Ambiguity, Spec §FR-029–FR-033, Plan §API and Error Contract]
- [ ] CHK040 Is the relationship between the 5-second total company-validation budget, 3-second response timeout, 5-second persistence budget, and overall API latency outcome defined without conflicting terminal status requirements? [Ambiguity, Plan §Constraints, Plan §Data and External Consistency Design]

## Company Master-Data Boundary

- [ ] CHK041 Is the external Company identifier unambiguously the sole tax-document ownership reference, with any tenant identifier confined to authorization or idempotency scope outside the draft aggregate? [Consistency, Spec §FR-027, Spec §FR-037, Spec §DR-023]
- [ ] CHK042 Are the immutable Issuer, establishment, and emission-point snapshot requirements clearly distinguished from current Company master data in the specification, plan, data model, and API contract? [Consistency, Spec §FR-036–FR-038, Plan §Company Master-Data Boundary]
- [ ] CHK043 Are Company master-data CRUD/search exposure, local authority tables, shared databases/schemas, cross-service foreign keys/repositories/transactions, caches, materialized views, and background replication explicitly prohibited? [Completeness, Constitution §XVI, Spec §FR-038]

## Notes

- `[x]` means the written requirement-quality criterion is objectively satisfied; it does not mean
  code or a task was tested.
- Record unresolved findings inline and update the authoritative specification or plan before
  marking the corresponding item complete.
- Items marked `[Gap]`, `[Ambiguity]`, `[Dependency]`, or `[Assumption]` deserve explicit review
  before `$speckit-tasks`.
