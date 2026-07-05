# Governance Requirements Checklist: Canonical Terminology and Architecture Boundaries

**Purpose**: Validate the quality, clarity, completeness, and consistency of requirements for terminology governance, legacy mapping, and Clean Architecture boundaries.
**Created**: 2026-07-05
**Feature**: [spec.md](../spec.md)

**Note**: This checklist treats the specification and plan as requirements written in English. It evaluates whether the requirements are ready for task generation; it does not test implementation behavior.

## Requirement Completeness

- [x] CHK001 Are durable output locations for architecture rules and migration mappings explicitly required and tied to acceptance criteria? [Completeness, Spec §FR-013, Spec §FR-014, Spec §SC-007]
- [x] CHK002 Are all target artifact categories covered by naming rules, including packages, classes, methods, DTOs, APIs, tables, columns, events, tests, and documentation? [Completeness, Spec §FR-004]
- [x] CHK003 Are artifact-specific format rules specified for all canonical-term surfaces named in scope? [Completeness, Spec §FR-016, Spec §NR-021]
- [x] CHK004 Are all allowed migration classifications enumerated where migrated concepts are required to be classified? [Completeness, Spec §FR-003, Spec §Migration Classification]
- [x] CHK005 Are explicit exclusions documented for runtime code, endpoints, migrations, entities, SRI clients, authentication, webhooks, production data migration, and legacy refactoring? [Completeness, Spec §Scope Boundaries, Plan §Summary]
- [x] CHK006 Are target documentation deliverables fully identified before task generation? [Completeness, Plan §Project Structure, Contracts §Target Artifact]

## Requirement Clarity

- [x] CHK007 Is "business-oriented English" constrained by concrete glossary entries and format rules rather than left as a subjective phrase? [Clarity, Spec §NR-001, Spec §NR-002-NR-021]
- [x] CHK008 Is the distinction between legacy Spanish names and official SRI contract names defined with allowed locations? [Clarity, Spec §FR-005, Data Model §SRIContractTerm]
- [x] CHK009 Are Pending Naming Decision resolution timing and recording locations stated unambiguously? [Clarity, Spec §FR-017, Data Model §PendingNamingDecision]
- [x] CHK010 Are Pending Functional Validation resolution, exclusion, and deferral options stated unambiguously for affected work? [Clarity, Spec §FR-018, Data Model §PendingFunctionalValidation]
- [x] CHK011 Is the compatibility exception concept defined with scope, owner, expiration condition, and rejected alternative requirements? [Clarity, Data Model §CompatibilityException, Plan §Complexity Tracking]
- [x] CHK012 Are layer ownership statements specific enough to prevent business logic from being assigned to REST resources, repositories, SRI adapters, or bootstrap? [Clarity, Spec §AR-003-AR-008]

## Requirement Consistency

- [x] CHK013 Are the documentation-only scope statements consistent across the spec, plan, research, and quickstart artifacts? [Consistency, Spec §Scope Boundaries, Plan §Summary, Research §Documentation-only]
- [x] CHK014 Are the approved baseline glossary mappings consistent across the spec, plan, migration contract, and data model? [Consistency, Spec §Migration Classification, Plan §Naming and Migration Classification, Contracts §Legacy Mapping]
- [x] CHK015 Are pending decision gates consistent between clarifications, functional requirements, success criteria, and data-model validation rules? [Consistency, Spec §Clarifications, Spec §FR-017-FR-018, Spec §SC-010-SC-011, Data Model §PendingNamingDecision]
- [x] CHK016 Are SRI isolation requirements consistent between the constitution-derived plan gates and the feature requirements? [Consistency, Plan §Constitution Check, Spec §FR-005, Spec §AR-010]
- [x] CHK017 Are source-code non-change statements consistent with the target documentation contracts and quickstart expectations? [Consistency, Plan §Source Code, Quickstart §Expected Target Files]

## Acceptance Criteria Quality

- [x] CHK018 Are success criteria measurable enough for reviewers to determine pass or fail without implementation inspection? [Measurability, Spec §SC-001-SC-011]
- [x] CHK019 Does each success criterion trace to at least one functional, architectural, or naming requirement? [Traceability, Spec §Functional Requirements, Spec §Architectural Requirements, Spec §Naming and Migration Requirements]
- [x] CHK020 Are acceptance scenarios written at the requirements-governance level rather than as implementation test cases? [Acceptance Criteria, Spec §User Scenarios & Testing]
- [x] CHK021 Are reviewer agreement thresholds and task-generation gates quantified where the feature depends on review outcomes? [Measurability, Spec §SC-002, Spec §SC-010-SC-011]

## Scenario Coverage

- [x] CHK022 Are primary governance flows covered for establishing canonical terms, isolating SRI contract language, and assigning architecture boundaries? [Coverage, Spec §User Story 1, Spec §User Story 2, Spec §User Story 3]
- [x] CHK023 Are alternate flows covered for newly discovered legacy terms and unverified legacy behavior? [Coverage, Spec §FR-007, Spec §FR-008, Spec §FR-017-FR-018]
- [x] CHK024 Are exception flows covered for compatibility exceptions and official SRI names that must remain Spanish in contract artifacts? [Coverage, Spec §FR-005, Data Model §CompatibilityException]
- [x] CHK025 Are future-feature handoff scenarios covered so later specs, plans, and tasks know how to consume the governance documents? [Coverage, Contracts §Future Specification Governance]

## Edge Case Coverage

- [x] CHK026 Are conflicting meanings between a legacy Spanish term and an official SRI contract term addressed in requirements? [Edge Case, Spec §Edge Cases]
- [x] CHK027 Are literal translations that fail to represent the business concept addressed as a naming-quality risk? [Edge Case, Spec §Edge Cases, Spec §NR-001]
- [x] CHK028 Are legacy database and request names that seem reusable addressed as target-forbidden unless classified as compatibility or migration-only concepts? [Edge Case, Spec §Edge Cases, Spec §Migration Classification]
- [x] CHK029 Are generic names such as `DocumentService`, `Manager`, `Helper`, and `Util` explicitly rejected at the requirements level? [Edge Case, Spec §NR-020]

## Dependencies & Assumptions

- [x] CHK030 Are assumptions about the constitution glossary baseline and non-exhaustive legacy vocabulary inventory documented and compatible with the plan? [Assumption, Spec §Assumptions, Plan §Scale/Scope]
- [x] CHK031 Is the current build-tool mismatch handled as a documented future setup concern rather than a hidden requirement of this feature? [Dependency, Plan §Constraints, Research §Build-tool reconciliation]
- [x] CHK032 Are generated document contracts sufficient to guide future tasks without requiring direct access to legacy source code? [Dependency, Plan §Performance Goals, Contracts §Architecture Rules, Contracts §Legacy Mapping]

## Ambiguities & Conflicts

- [x] CHK033 Are there any remaining terms in the spec, plan, or contracts that are target-relevant but lack classification or a pending-decision path? [Ambiguity, Spec §Migration Classification, Plan §Naming and Migration Classification]
- [x] CHK034 Are any requirements using broad phrases such as "future features" or "affected work" without enough gating context to guide task generation? [Ambiguity, Spec §FR-017-FR-018, Contracts §Future Specification Governance]
- [x] CHK035 Are there conflicts between the durable documentation outputs and the active feature artifacts that could create two competing sources of truth? [Conflict, Spec §Clarifications, Plan §Target Documentation To Be Created By Tasks]
- [x] CHK036 Is a requirement and acceptance criteria ID scheme established for traceability across spec, plan, data model, contracts, and future tasks? [Traceability, Spec §Functional Requirements, Plan §Phase 1 Design Summary]

## Notes

- Focus areas: terminology governance, legacy-to-target mapping, Clean Architecture boundaries, SRI isolation, pending decision gates, and documentation contracts.
- Depth: Standard reviewer checklist for requirements quality before task generation.
- Audience/timing: Feature author and peer reviewer before `/speckit-tasks`.
