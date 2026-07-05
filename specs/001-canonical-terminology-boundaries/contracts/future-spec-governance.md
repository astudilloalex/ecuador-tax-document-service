# Contract: Future Specification Governance

## Target Consumers

Future Spec Kit specifications, plans, task lists, and code reviews.

## Purpose

Define the minimum governance checks that future features must satisfy before
task generation and implementation.

## Required Future Specification Content

- Canonical English terms used by the feature.
- Legacy terms mapped or newly registered as pending.
- Classification for every migrated concept.
- SRI contract-only names isolated to allowed artifacts.
- Explicit out-of-scope legacy behaviors.
- Pending Naming Decisions and Pending Functional Validations, if any.
- Stable requirement identifiers using `FR-###`, `AR-###`, `NR-###`,
  `TR-###`, and `SC-###` as applicable.

## Required Future Plan Content

- Clean Architecture boundary assignment.
- Explicit application use cases for business operations.
- Application ports for each external dependency.
- DTO separation across REST, application, domain, persistence, and SRI
  boundaries.
- Validation ownership across transport, application, and domain levels.
- Compatibility exceptions with reason, scope, owner, expiration condition, and
  rejected safer alternative.
- Resolution of Pending Naming Decisions before task generation.
- Resolution, exclusion, or deferral of Pending Functional Validation before
  affected task generation.
- Traceability from planned artifacts to requirement identifiers and contract
  sections.

## Required Future Task List Checks

- No task creates target artifacts with unresolved names.
- No task implements behavior marked Pending Functional Validation.
- No task puts business logic in REST resources, repositories, SRI adapters, or
  bootstrap configuration.
- No task exposes persistence entities through REST APIs.
- No task reuses DTOs across layer boundaries.
- No task places official SRI Spanish names outside allowed SRI, fixture,
  compatibility, migration, or mapping artifacts.
- Every task uses a `T###` identifier and cites at least one governing
  requirement identifier or contract section.

## Source Of Truth Rule

- Constitution-governed durable locations:

  | Artifact Category | Required Location | Purpose |
  |-------------------|-------------------|---------|
  | AS-IS legacy documentation | `docs/legacy/` | Store evidence-based documentation of the legacy system without mixing it with target specifications. |
  | Migration mappings and canonical terminology | `docs/migration/` | Store canonical language, legacy-to-target mappings, forbidden legacy terms, pending naming decisions, and migration classifications. |
  | Architecture rules and decisions | `docs/architecture/` or `docs/adr/` | Store Clean Architecture rules, architectural decisions, and backend governance documentation. |
  | Target specifications | `.specify/specs/` | Store active Spec Kit target specifications, plans, tasks, contracts, and feature artifacts. |

- After this enabler is implemented, `docs/architecture` and `docs/migration`
  are the durable source of truth for architecture rules, canonical
  terminology, and legacy-to-target mappings.
- AS-IS legacy documentation must remain under `docs/legacy/` and must not be
  stored as target specification artifacts.
- Architecture rules and decisions must remain under `docs/architecture/` or
  `docs/adr/` and must not compete with the constitution as a second source of
  truth.
- Feature artifacts under `specs/` remain planning, contract, and review
  records and must not contradict the durable documentation outputs.

## Acceptance Checks

- A future reviewer can block a task list that includes unresolved naming or
  functional validation work.
- A future reviewer can trace every migrated concept to a classification.
- A future reviewer can identify whether a Spanish term is target-forbidden,
  SRI adapter-only, compatibility-only, migration-only, deprecated, or pending.
- A future reviewer can trace every generated task to a requirement identifier
  or contract section.
