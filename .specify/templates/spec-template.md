# Feature Specification: [FEATURE NAME]

**Feature Branch**: `[###-feature-name]`

**Created**: [DATE]

**Status**: Draft

**Input**: User description: "$ARGUMENTS"

<!--
  A specification describes WHAT is required and WHY. It MUST NOT select framework versions,
  code structure, database design, or other implementation choices that belong in planning.
  All target terminology MUST be English except exact official SRI terms or approved Spanish
  user-visible content.
-->

## Scope and Evidence *(mandatory)*

### Bounded Outcome

[Describe one independently valuable user or stakeholder outcome. Do not use a generic outcome
such as "migrate the entire legacy system" and do not bundle unrelated document types or
administrative capabilities.]

### In Scope

- [Behavior required to deliver the bounded outcome]
- [Applicable actor, document type, or operational boundary]

### Exclusions and Non-Goals

- [Explicitly excluded behavior]
- [Legacy compatibility that is not required]
- [Related document types or administrative capabilities deferred to separate specifications]
- [Company master-data ownership, administration, replication, caching, or exposure excluded when
  Company fiscal context is consumed]

### Authority and Evidence

List exact official document versions and exact repository paths. Legacy evidence informs
discovery only and MUST NOT be stated as a target requirement unless another approved requirement
does so explicitly.

| Authority | Source and version/path | Relevance to this feature |
|-----------|-------------------------|---------------------------|
| Applicable Ecuadorian legislation | [citation/version or justified non-applicability] | [governed requirements] |
| Official SRI technical documentation | [title/version/date or justified non-applicability] | [governed fiscal rules] |
| Project constitution | `.specify/memory/constitution.md` v[version] | [applicable principles] |
| Legacy evidence | [paths under `docs/legacy/` or legacy source references; state when no evidence was located] | [historical observation only] |

**Source Conflicts**: [For each conflict, identify both sources, the higher-authority source that
governs, and the recorded resolution. State None when no conflict is known.]

**Terminology Mapping**: [Relevant entries in `docs/migration/terminology-mapping.md`, including
pending naming decisions.]

### Pending Functional Validation

Unknown or contradictory behavior MUST be registered here and MUST NOT be silently inferred. The
absence of legacy evidence MUST NOT be used to declare behavior unnecessary. Use `None` only after
the authority and evidence review above.

- **PFV-001**: [Unresolved functional question] — **Evidence needed**: [official or stakeholder
  validation] — **Blocks**: [requirement, scenario, or None]

## User Scenarios & Testing *(mandatory)*

<!--
  Prioritize user journeys by stakeholder value. Every story MUST contribute to the single bounded
  outcome and MUST be independently testable. Acceptance scenarios MUST cover the primary path and
  applicable authorization, failure, boundary, duplicate, or recovery paths.
-->

### User Story 1 - [Brief Title] (Priority: P1)

[Describe the user or stakeholder journey in plain, technology-agnostic language.]

**Why this priority**: [State the stakeholder value and why it is the smallest viable outcome.]

**Independent Test**: [Describe the observable result that proves this story delivers value on its
own.]

**Acceptance Scenarios**:

1. **Given** [authorized initial state], **When** [actor action], **Then** [observable outcome]
2. **Given** [applicable invalid, forbidden, duplicate, or failure state], **When** [action],
   **Then** [safe observable outcome]

---

### User Story 2 - [Brief Title] (Priority: P2)

[Describe another independently testable journey that contributes to the same bounded outcome.]

**Why this priority**: [Explain its value and ordering.]

**Independent Test**: [Describe its independently observable value.]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [observable outcome]
2. **Given** [alternate or failure state], **When** [action], **Then** [safe observable outcome]

---

[Add only stories required for this bounded outcome. Remove unused sample stories.]

### Edge Cases

Address each applicable category with a required outcome; do not answer an unknown by guessing.

- What happens at monetary precision, rounding, date, identifier, or catalog boundaries?
- What happens for impossible dates, inconsistent totals, or invalid catalog combinations?
- What happens when a caller attempts cross-tenant or wrong-issuer access?
- What happens for a duplicate command, retry, timeout, partial external failure, or reconciliation?
- When Company fiscal context is used, what remains current external master data and what becomes
  immutable document-owned historical evidence?
- What caller-visible outcome exists for asynchronous failure or expiration?
- What sensitive information could appear in an error, log, trace, metric, or retained payload?

## Requirements *(mandatory)*

### Functional Requirements

<!--
  Requirements MUST be target-first, testable, and traceable to acceptance scenarios. A legacy
  route, payload, table, status, or behavior is excluded unless a requirement explicitly approves
  that exact compatibility. Use canonical English terminology from the terminology mapping.
-->

- **FR-001**: The service MUST [specific target capability and observable result].
- **FR-002**: The service MUST [specific validation, rejection, or state-transition behavior].
- **FR-003**: An authorized [actor] MUST be able to [bounded action and result].
- **FR-004**: The service MUST deny [unauthorized or cross-tenant behavior] when [condition], if
  tenant-owned information is in scope.
- **FR-005**: The service MUST [idempotency, duplicate, timeout, recovery, or terminal-outcome rule
  when an external or asynchronous boundary is in scope].
- **FR-006**: When Company fiscal context is in scope, the service MUST [resolve current context from
  the Company bounded context, store only the external Company ownership identifier, preserve the
  exact immutable document fiscal snapshot, and exclude Company master-data administration or
  replication].

### Domain Rules and Invariants *(include when fiscal or monetary behavior is in scope)*

- **DR-001**: [Rule] MUST follow [official source and version].
- **DR-002**: [Monetary rule] MUST define scale [value], rounding mode [value], permitted precision
  [value], currency [value], validation boundaries [values], and calculation or reconciliation
  owner [actor/component responsibility without selecting code structure].
- **DR-003**: [Civil/fiscal date] MUST use [Ecuadorian date semantics], while [audit timestamp]
  MUST represent an unambiguous instant.

### Asynchronous Outcome Requirements *(include when asynchronous work is in scope)*

- **AR-001**: An authorized caller MUST be able to observe status through [query or delivery
  behavior].
- **AR-002**: The operation MUST define correlation identifiers, terminal states, caller-safe
  errors, retry semantics, retention, idempotency, and authorization for result access.

### Key Entities *(include when the feature involves data)*

Describe business meaning, identity, ownership, lifecycle, and invariants without API, database,
or persistence-model design. Distinguish an externally owned Company identifier and current master
data from immutable fiscal evidence owned by a tax document.

- **[Entity 1]**: [Meaning, identity, tenant/issuer ownership, lifecycle, and critical invariants]
- **[Entity 2]**: [Meaning and relationship to other business concepts]

## Success Criteria *(mandatory)*

### Measurable Outcomes

Success criteria MUST be technology-agnostic, objectively measurable, and traceable to the bounded
outcome. Include applicable security, failure, and recovery outcomes; do not rely only on a
coverage percentage.

- **SC-001**: [Actor completes the primary outcome within a measurable limit.]
- **SC-002**: [A measurable correctness or rejection rate for governing fiscal scenarios.]
- **SC-003**: [A measurable authorization, tenant-isolation, or sensitive-data outcome.]
- **SC-004**: [A measurable timeout, retry, recovery, or terminal-observability outcome when
  external/asynchronous work is in scope.]

## Assumptions and Dependencies

Assumptions MUST NOT replace Pending Functional Validation. Each assumption MUST state the
evidence or approved default that makes it reasonable.

- **Assumption**: [Target-user or operational assumption] — **Basis**: [approved evidence/default]
- **Dependency**: [External stakeholder, official artifact, or service dependency and its
  availability/version constraint]
- **Dependency**: [Company bounded-context authority and current fiscal-context availability when
  Company, Issuer, establishment, or emission-point information is required]
