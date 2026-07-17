# Specification Boundary Requirements Checklist: Create Invoice Draft

**Purpose**: Validate that the feature specification remains stakeholder-facing, behaviorally
complete, internally consistent, measurable, and traceable after separating functional semantics
from planning and design decisions

**Created**: 2026-07-16

**Feature**: [spec.md](../spec.md)

**Audience/Timing**: Feature author and reviewer; post-remediation, pre-implementation
requirements-quality gate

**Note**: This checklist tests the quality of the written requirements. It does not test whether an
implementation works or whether an implementation task is complete.

## Specification Boundary Completeness

- [x] CHK001 Does the specification define one bounded stakeholder outcome without introducing
  unrelated document types, administrative capabilities, or fiscal-issuance work? [Completeness,
  Spec §Bounded Outcome, Spec §Exclusions and Non-Goals]
  - Evidence: §Bounded Outcome limits the feature to creating and reviewing one Invoice Draft;
    §Exclusions explicitly removes other document types, administration, and fiscal issuance.
- [x] CHK002 Are stakeholder goals, observable contract behavior, business rules, assumptions,
  exclusions, and measurable outcomes all present without relying on planning artifacts to explain
  what the feature does? [Completeness, Spec §Scope and Evidence, Spec §Requirements]
  - Evidence: The specification contains Bounded Outcome, scope, scenarios, FR/DR, assumptions,
    exclusions, official evidence, and measurable outcomes as self-contained behavioral sections.
- [x] CHK003 Are technical realization decisions consistently confined to planning and design
  artifacts rather than stated as functional obligations in the specification? [Consistency,
  Spec §Functional Requirements, Plan §Technical Context]
  - Evidence: A forbidden-term scan finds no framework, language, storage, architecture-layer,
    internal-model, task, source-path, or workflow-command prescription in `spec.md`; those choices
    remain in planning/design artifacts.
- [x] CHK004 Are all 47 FR, 24 DR, 33 SC, and 71 acceptance scenarios retained with stable identifiers
  or positions sufficient for downstream traceability? [Traceability, Spec §Requirements, Spec
  §Success Criteria, Spec §User Scenarios & Testing]
  - Evidence: Automated counts return exactly FR=47, DR=24, SC=33, and acceptance scenarios=71;
    identifier uniqueness and `traceability.md` mappings are preserved.
- [x] CHK005 Is the Definition of Done limited to externally observable completion conditions rather
  than build, framework, storage, migration, class, test-suite, or task completion details?
  [Boundary, Spec §Feature Definition of Done]
  - Evidence: Definition of Done addresses accepted/rejected outcomes, complete saved state,
    isolation, idempotency, excluded side effects, reference evidence, and contract outcomes only.

## Requirement Clarity and Stakeholder Readability

- [x] CHK006 Can a product owner, fiscal analyst, QA analyst, or API consumer interpret every
  functional requirement without needing architecture or source-code knowledge? [Clarity,
  Spec §Functional Requirements]
  - Evidence: FR-001–FR-047 use external inputs, business/fiscal rules, observable saved state,
    response fields, stable errors, and exclusions without implementation vocabulary.
- [x] CHK007 Are terms such as “saved,” “all-or-nothing,” “equivalent replay,” “conclusive outcome,”
  and “authoritative Company context” defined consistently enough to avoid hidden implementation
  assumptions? [Clarity, Spec §Clarifications, Spec §Functional Requirements]
  - Evidence: Clarifications, FR-020–FR-024, FR-029–FR-034, and FR-041–FR-043 distinguish complete
    save, equivalent replay, conclusive outcomes, uncertain saves, and header-only Company context.
- [x] CHK008 Are all normative quantities, lengths, cardinalities, dates, character repertoires,
  rounding modes, and timing limits expressed with exact inclusive or exclusive boundaries?
  [Clarity, Spec §FR-006–FR-016, Spec §FR-027, Spec §FR-035, Spec §FR-041–FR-044]
  - Evidence: The cited requirements state exact counts, ranges, scales, regexes, Unicode units,
    inclusive dates, `HALF_UP`, 2 MiB, and 10-second precedence; FR-006 now fixes date capture before
    body consumption.
- [x] CHK009 Are English canonical terms used consistently while exact SRI codes and official names
  remain unchanged in their approved contexts? [Consistency, Spec §Authority and Evidence, Spec
  §Key Entities]
  - Evidence: Narrative and entity terminology is English; exact SRI codes, `CONSUMIDOR FINAL`,
    official document names, and baseline identifiers remain unchanged where legally meaningful.
- [x] CHK010 Are implementation-neutral phrases used wherever the requirement concerns observable
  timestamps, normalization, atomicity, Company isolation, reference data, or deadline outcomes?
  [Boundary, Spec §FR-019–FR-024, Spec §FR-035–FR-047]
  - Evidence: These requirements state UTC equality/replay, deterministic normalized results,
    complete-or-no-state behavior, Company isolation, approved references, and first-conclusive
    deadline outcomes without naming internal owners or technologies.

## External Contract and Company Context

- [x] CHK011 Is the `POST /api/v1/invoice-drafts` operation specified with exact Company,
  idempotency, correlation, body, success-response, and stable-error obligations? [Completeness,
  Spec §FR-001–FR-005, Spec §FR-022, Spec §FR-025–FR-027]
  - Evidence: The route and cited requirements define all three headers, strict input exclusions,
    complete success representation, status/error families, safe correlation, and zero-state rules.
- [x] CHK012 Is `X-Company-Id` unambiguously the sole authoritative Company input, with exact
  cardinality, normalization, UUID, nil, placement, body-rejection, and response-representation
  semantics? [Clarity, Spec §FR-001–FR-003, Spec §FR-022, Spec §SC-024]
  - Evidence: FR-001–FR-003 require exactly one header value, one SP/HTAB trim, non-nil UUID and
    canonical form; Company input elsewhere is forbidden while contracted response `companyId`
    remains allowed.
- [x] CHK013 Are Company ownership and isolation requirements stated as observable prevention of
  cross-Company access, reuse, conflict, or mutation without presenting them as authentication or
  authorization? [Consistency, Spec §FR-024, Spec §FR-037, Spec §DR-018, Spec §DR-020]
  - Evidence: FR-024/FR-037 and DR-018/DR-020 prohibit cross-Company draft and idempotency behavior
    while explicitly excluding authentication/authorization responsibility.
- [x] CHK014 Are shared immutable SRI catalogs clearly excluded from automatic Company ownership
  everywhere Company-scoped draft behavior is described? [Consistency, Spec §Clarifications, Spec
  §FR-024, Spec §FR-037]
  - Evidence: Clarifications, FR-024, and FR-037 consistently identify VAT, payment, identification,
    and other approved immutable SRI catalogs as globally shared and not Company-owned.
- [x] CHK015 Are identity, entitlement, Company lookup, Company master-data, fiscal snapshot, and
  security-scheme exclusions complete and mutually consistent? [Coverage, Spec §Exclusions and
  Non-Goals, Spec §FR-003, Spec §FR-036–FR-040]
  - Evidence: Exclusions and FR-003/FR-036–FR-040 prohibit all named capabilities, dependencies,
    snapshots, cache, and security schemes while retaining only syntactic Company context.

## Business, Fiscal, and Text Semantics

- [x] CHK016 Are buyer-identification rules complete for types `04`–`08`, including activity,
  effectiveness, exact format-only behavior, final-consumer rules, and prohibited unapproved
  checks? [Completeness, Spec §FR-007, Spec §FR-028, Spec §DR-014–DR-015]
  - Evidence: FR-007/FR-028 and DR-014–DR-015 enumerate all five codes, exact formats, activity and
    `emissionDate` effectiveness, final-consumer values/limit, and checksum/registry prohibitions.
- [x] CHK017 Are IVA selection, supported treatments, effective-date behavior, caller-selection
  responsibility, grouping, and unsupported-tax outcomes specified without ambiguity? [Clarity,
  Spec §FR-011–FR-012, Spec §DR-005, Spec §DR-008]
  - Evidence: FR-011–FR-012 and DR-005/DR-008 define exactly one caller-selected effective IVA rule,
    four treatments, separate zero groups, calculated code/rate, and rejection of other taxes.
- [x] CHK018 Are exact decimal inputs, formulas, rounding sequence, range checks, aggregation, zero
  outcomes, and payment reconciliation stated as business results rather than storage choices?
  [Boundary, Spec §FR-010–FR-016, Spec §DR-002–DR-011]
  - Evidence: The cited sections contain the complete decimal envelope, formulas, line-level
    `HALF_UP` sequence, grouped totals, overflow result, zero-total payment rule, and exact
    reconciliation without physical storage types.
- [x] CHK019 Are general Unicode normalization, prohibited code points, whitespace, display-case,
  code-point counting, canonical-name derivation, overflow metadata, and no-truncation behavior
  completely specified? [Completeness, Spec §FR-035, Spec §DR-019, Spec §Executable General Text
  Policy and Vectors]
  - Evidence: FR-035, DR-019, and the executable policy define NFC-equivalent behavior, `U+0020`,
    prohibited categories/separators, preserved display case, code-point limits, deterministic
    lowercase canonicalization, error metadata, `U+0130` boundaries, and no truncation.
- [x] CHK020 Are stricter ASCII fields explicitly separated from general-text rules with exact
  patterns, trim behavior, case sensitivity, valid examples, and invalid examples? [Clarity, Spec
  §FR-007, Spec §FR-010, Spec §Executable ASCII Repertoires]
  - Evidence: Buyer types `06`/`08` and `productCode` have separate case-sensitive ASCII regexes,
    one SP/HTAB trim, no other transformation, exact bounds, and positive/negative examples;
    FR-004 separately defines opaque UUID trimming/canonicalization.

## Idempotency, Failure, and Recovery Semantics

- [x] CHK021 Are idempotency scope, normalized-key behavior, equivalent-content rules, significant
  collection ordering, conflict, concurrency, lifetime, and cross-Company independence all
  documented? [Completeness, Spec §FR-027, Spec §FR-029–FR-034, Spec §DR-017–DR-018, Spec §DR-021]
  - Evidence: The cited requirements define Company-plus-key scope, key normalization, fingerprint
    content, line/payment/additional ordering, replay/conflict, one-winner concurrency, draft-lifetime
    retention, cross-Company independence, and uncertain-save recovery.
- [x] CHK022 Are missing, invalid, repeated, parser-multiple, and comma-combined idempotency-header
  cases assigned deterministic and mutually exclusive stable errors without first-value selection?
  [Clarity, Spec §FR-027, Spec §Scenarios 59–61]
  - Evidence: FR-027 and scenarios 59–61 map missing, invalid, and multiple/comma cases to three
    stable codes, define precedence, and explicitly forbid selecting a first value.
- [x] CHK023 Are confirmed rejection, complete reversal, unresolved save, successful save with lost
  response, and later replay distinguished without making contradictory zero-state claims?
  [Consistency, Spec §FR-020–FR-021, Spec §FR-032–FR-033, Spec §FR-043]
  - Evidence: FR-020–FR-021/FR-032–FR-033/FR-043 distinguish confirmed no-state outcomes from
    unresolved saves and committed response loss, requiring same-scope replay without compensation.
- [x] CHK024 Is the all-or-nothing requirement complete for the draft, children, calculated values,
  and idempotency association while avoiding internal save-mechanism prescriptions? [Boundary,
  Spec §FR-020–FR-021, Spec §Scenario 11, Spec §Scenario 71]
  - Evidence: FR-020–FR-021 and scenarios 11/71 enumerate draft, lines, taxes, payments, additional
    information, calculated values, and association as one externally complete-or-absent outcome.
- [x] CHK025 Are timestamp requirements limited to observable equality, UTC representation,
  non-exposure on failed saves, unchanged replay values, and separation from emission-date
  determination? [Boundary, Spec §FR-019, Spec §DR-012, Spec §SC-013]
  - Evidence: FR-019, DR-012, and SC-013 state equal UTC creation values, failure non-exposure,
    unchanged replay, and separation from the entry-date decision without clock/storage ownership.

## Deadline, Precedence, and Error Quality

- [x] CHK026 Is the complete validation precedence defined from payload size through successful
  response, including the exact order of calculated-value rules? [Completeness, Spec §FR-041]
  - Evidence: FR-041 enumerates stages 1–12, assigns missing/non-string `emissionPointId` to Stage 5,
    makes its trim/UUID validation the first Stage-6 check before general-text normalization, and
    orders Stage 11B overflow, discount, final-consumer, payment shape/positivity, and reconciliation
    deterministically before the success response.
- [x] CHK027 Are stage-first, deadline-first, late-outcome, unresolved-save, and post-response-expiry
  semantics mutually exclusive and objectively determinable? [Clarity, Spec §FR-041–FR-043, Spec
  §SC-026–SC-028]
  - Evidence: FR-041–FR-043 and SC-026–SC-028 define first-conclusive selection, timeout fallback,
    immutable terminal result, uncertain save, discarded late outcomes, and telemetry-only expiry.
- [x] CHK028 Are payload-size and correlation interactions specified for absent, valid, and invalid
  correlation input without allowing a later correlation error to replace `413`? [Edge Case, Spec
  §FR-026, Spec §FR-042, Spec §Scenario 45]
  - Evidence: FR-026/FR-042 and scenario 45 define absent/valid/invalid safe correlation behavior,
    payload-before-header precedence, retained `413`, deadline competition, and zero later work.
- [x] CHK029 Are all stable error codes, safe-message obligations, correlation behavior, sensitive
  data exclusions, and zero-state limits defined for every applicable failure family? [Coverage,
  Spec §FR-001, Spec §FR-004, Spec §FR-025–FR-027, Spec §FR-041–FR-044]
  - Evidence: FR-004 and FR-041 now distinguish Stage-5 `INVALID_REQUEST` for missing/non-string
    `emissionPointId` from Stage-6 `BUSINESS_VALIDATION_FAILED` with value-free nested
    `EMISSION_POINT_INVALID` for blank-after-trim, malformed, or nil decoded strings. Together with
    the cited requirements, this defines stable Company/idempotency/payload/business/deadline/
    persistence errors, safe Problem Details and correlation, sensitive-value exclusions, and the
    exact cases where a zero-state claim is valid.

## Acceptance and Scenario Quality

- [x] CHK030 Does every success criterion measure an observable response, saved or absent state,
  deterministic result, elapsed-time outcome, isolation property, or absence of side effects
  without requiring knowledge of implementation technology? [Measurability, Spec §Measurable
  Outcomes]
  - Evidence: SC-001–SC-033 are expressed through requests, responses, accepted/rejected vectors,
    saved/absent state, timing, isolation, stable references, or absence of excluded effects; a
    technology-term scan is empty.
- [x] CHK031 Are primary creation, alternate zero-total and replay flows, validation failures,
  concurrency, recovery, deadline competition, and boundary-review scenarios all represented?
  [Coverage, Spec §User Scenarios & Testing]
  - Evidence: The 71 scenarios include new creation, zero total, equivalent/conflicting replay,
    invalid inputs, 50-way concurrency, reversal/response loss, timeout races, and scope exclusions.
- [x] CHK032 Are every numeric, collection, date, reference-effectiveness, character-repertoire,
  canonical-expansion, and header-cardinality boundary represented by at least one explicit
  scenario or success criterion? [Edge Case, Spec §Acceptance Scenarios, Spec §Edge Cases]
  - Evidence: Scenarios and SCs cover numeric extrema/overflow, 500/8/15 collections, earliest-date
    midnight crossing, inclusive reference dates, ASCII/Unicode boundaries, `U+0130` 150/151, and
    single/repeated/comma headers.

## Authority, Dependencies, and Traceability

- [x] CHK033 Are official legislation, SRI technical evidence, project governance, approved target
  decisions, and legacy evidence ordered and distinguished without treating legacy behavior as
  authority? [Traceability, Spec §Authority and Evidence]
  - Evidence: §Authority and Evidence orders law, official SRI sources, governance, approved target
    decisions, and legacy material, explicitly limiting legacy artifacts to non-authoritative evidence.
- [x] CHK034 Are unsupported or unverified reference entries explicitly unavailable or Pending
  Functional Validation rather than silently inferred? [Gap, Spec §FR-045–FR-047, Spec §SC-031–SC-032]
  - Evidence: FR-045–FR-047 and SC-031–SC-032 require approved evidence/mapping and mark unsupported
    entries unavailable or Pending Functional Validation without invention.
- [x] CHK035 Are assumptions and dependencies stated as external conditions without introducing
  hidden feature behavior or contradicting the no-Company-dependency boundary? [Assumption,
  Dependency, Spec §Assumptions and Dependencies]
  - Evidence: §Assumptions and Dependencies states upstream context, official baseline, currency,
    and local runtime expectations while expressly requiring no Company lookup/dependency.
- [x] CHK036 Can every FR, DR, SC, and acceptance scenario still be linked to its existing design and
  task evidence without requiring the specification to repeat technical realization details?
  [Traceability, Spec §Requirements, Spec §Success Criteria]
  - Evidence: `traceability.md` retains FR-001–FR-047, DR-001–DR-024, SC-001–SC-033, and
    AS-001–AS-071 mappings; request-time and `emissionPointId` evidence now point explicitly to
    T085/T034 and T029/T030/T033 respectively.

## Notes

- `[x]` means the written requirement-quality criterion is satisfied; it does not mean code was
  exercised or an implementation task was completed.
- Record any failure beside the affected item and update the specification or planning boundary
  before implementation begins.
- Do not mark an item complete solely to satisfy a workflow gate.
