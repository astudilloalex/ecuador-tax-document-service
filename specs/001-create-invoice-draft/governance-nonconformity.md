# Governance Non-Conformity Record: Analyze Gate Ordering

**Record ID**: `GOV-001`

**Feature**: `001-create-invoice-draft`

**Detected**: 2026-07-15 (`America/Guayaquil`)

**Governing constitution**: `.specify/memory/constitution.md` v2.0.0, Principle XIV and the
Definition of Done

**Status**: **BLOCKING — RETROSPECTIVE REVIEW AND OWNER APPROVAL NOT RECORDED**

## Exact Non-Conformity

Tasks T001 through T016 were implemented and marked complete before a verifiable
`$speckit-analyze` execution had run after generation of `tasks.md`. This violated Constitution
v2.0.0 Principle XIV, which requires `$speckit-analyze` after task generation and before any
implementation, and which prohibits implementation while a critical inconsistency remains.

The affected tasks are exactly T001–T016. Their completion markers record historical execution;
they are not evidence that the mandatory sequence was followed or that the completed artifacts
conform to the current governance and feature artifacts.

The sequence violation cannot be corrected retroactively. This record does not rewrite Git
history, erase completed work, backdate an analysis, or claim that the required gate ran. The only
honest remediation is a retrospective conformity review and an explicit decision by the required
owner authority.

## Repository Evidence and Dates

The Specification Analysis Report identified the following repository evidence:

| Evidence | Repository commit | Date (`America/Guayaquil`) |
|----------|-------------------|-----------------------------|
| Feature approval and dependency-ordered task generation | `1289871` | 2026-07-13 18:47:47 -05:00 |
| Implementation of T001–T012 foundations | `8bdd548` | 2026-07-14 23:31:06 -05:00 |
| Implementation of T013–T016 migrations and supporting evidence | `5e5452a` | 2026-07-15 19:46:25 -05:00 |

These references establish that implementation occurred after task generation but before a
verifiable post-task `$speckit-analyze` gate. Reviewers MUST inspect the full commits and their
parents rather than relying only on the summaries in this table.

## Mandatory Retrospective Review

T001–T016 MUST be reviewed against the current versions presented for retrospective owner
approval—not only the historical pre-implementation snapshots—of:

- `.specify/memory/constitution.md`;
- `specs/001-create-invoice-draft/spec.md`;
- `specs/001-create-invoice-draft/plan.md`;
- `specs/001-create-invoice-draft/tasks.md`;
- `specs/001-create-invoice-draft/contracts/invoice-draft-api.openapi.yaml`;
- all feature data, persistence, idempotency, error, reference-data, operational, quickstart, and
  traceability artifacts;
- the completed build/configuration, boundary, test-support, test, and Flyway artifacts attributed
  to T001–T016; and
- the diffs and repository state represented by commits `1289871`, `8bdd548`, and `5e5452a`.

The review MUST record, task by task, the reviewer, evidence inspected, conformity result, every
deviation, and either the corrective evidence or the explicitly accepted residual risk. A
completion marker alone is insufficient.

At minimum, the retrospective review MUST resolve these already observed items without editing a
committed migration in place:

- `V3__create_invoice_draft_aggregate.sql` currently expresses `product_code` with the PostgreSQL
  POSIX class `[[:alnum:]]`; the current approved rule is the locale-independent ASCII expression
  `^[A-Za-z0-9]{1,25}$`.
- That migration currently checks buyer identification only for nonblank text and does not provide
  the current type-conditional database barrier for codes `06` and `08`, whose approved expression
  is `^[A-Za-z0-9]{1,20}$`.

This record does not authorize a migration change. If correction is required, it MUST be planned
as a new immutable Flyway migration and performed only after this governance gate permits
implementation work.

## Approval Authority and Required Evidence

Approval requires the repository owner accountable for constitutional governance and the owner
accountable for feature `001-create-invoice-draft`. If one person holds both responsibilities, one
approval MAY cover both only when it explicitly states both capacities.

Before approval, that authority MUST review:

1. the task-by-task T001–T016 retrospective review record;
2. every deviation and its correction evidence or explicit residual-risk disposition;
3. the current constitution, specification, plan, tasks, contract, and supporting feature designs;
4. the exact Git diffs and commit references listed above;
5. migration immutability and empty-database evidence for T013–T016;
6. Clean Architecture, dependency, configuration, reference-baseline, Company-scope, exact ASCII
   repertoire, and prohibited-capability evidence applicable to T001–T016; and
7. the Specification Analysis Report that identified this non-conformity and every subsequently
   available `$speckit-analyze` report, with each reported deviation dispositioned in the
   retrospective review.

The approval record MUST identify the approver, their authority/capacity, approval date, reviewed
commit(s), evidence reviewed, deviations accepted or confirmed corrected, and the explicit decision
to approve or reject the retrospective review. Approval of the retrospective review does not by
itself release the gate: after approval is recorded, the final `$speckit-analyze` condition below
MUST also pass.

## Blocking Gate

No work starting with T017, and no later implementation task, may continue until all of the
following are true:

- [ ] T001–T016 have been retrospectively reviewed.
- [ ] Every deviation has been recorded or corrected.
- [ ] Explicit owner approval has been documented with the authority and evidence defined above.
- [ ] A new `$speckit-analyze` report contains no CRITICAL finding related to this
  non-conformity.

This gate is intentionally incomplete. No owner approval has been supplied or inferred.

## Feature-Level Constitutional Interpretation

Constitution v2.0.0 Definition of Done says that Company identifiers do not enter “bodies,” while
Principle XVI and the approved feature contract govern caller-supplied Company context. For this
feature, the precise interpretation is:

> Company identifiers are forbidden in request bodies and input schemas. The authoritative
> Company context is obtained only from the approved request header. A Company identifier may
> appear in a response representation when explicitly required by the feature contract.

This interpretation does not permit a client to supply CompanyId in any request body and does not
amend or silently reinterpret Constitution v2.0.0. Clarifying the Constitution's unqualified word
“bodies” remains pending through its formal amendment mechanism, including a Sync Impact Report,
version decision, ratification metadata, and dependent-template review.

For this feature, Company scoping is also precise:

> Every repository query or mutation involving the Invoice Draft aggregate or its idempotency
> binding must include and enforce the authoritative Company identifier.

Global VAT, payment-method, identification-type, and other immutable SRI reference catalogs are
not Company-owned unless another approved requirement explicitly says otherwise. This feature does
not add Company columns to those catalogs.

Constitution Principle XVI also uses the broad phrase “CompanyId MUST scope repository queries,
mutations, idempotency.” This feature applies that requirement to Company-owned Invoice Draft
aggregate and binding operations and does not treat globally governed reference data as Company
master data. Clarifying that unqualified constitutional repository wording is likewise pending the
formal amendment mechanism. This record does not change Constitution v2.0.0 semantics or version
metadata.

## Specification Analysis Remediation Index

| Finding | Artifact remediation | Current disposition |
|---------|----------------------|---------------------|
| `C1` | This record, repository evidence, task-by-task retrospective requirements, and incomplete `GATE-GOV-001` immediately before T017 | Contained but unresolved; real review/approval and clean new analysis remain mandatory |
| `I1` | FR-019, DR-012, scenarios/SCs, plan, data/persistence/error designs, OpenAPI, quickstart, traceability, and T025/T034/T057/T061/T065/T074/T075/T078/T086 now use one in-transaction pre-persist UTC Instant | Artifact inconsistency remediated; new analysis pending |
| `U1` | FR-027, scenarios 59–61, SC-012, OpenAPI, error/idempotency designs, quickstart, traceability, and T028/T031/T080/T082/T085 define exact single cardinality, normalization, grammar, and three stable errors | Artifact ambiguity remediated; new analysis pending |
| `I2` | T024 and domain tasks contain only normalized domain value/invariant work; header validation and contract evidence reside in API tasks T028/T031/T080/T082/T085 | Boundary inconsistency remediated; new analysis pending |
| `A1` | Feature-level request/input prohibition and explicitly contracted response allowance are aligned in FR-002/FR-022, SC-024, plan, OpenAPI, quickstart, tasks, and this pending constitutional-clarification record | Feature ambiguity remediated without amending Constitution v2.0.0; formal constitutional clarification remains pending |
| `A2` | FR-024/FR-037, plan, repository/persistence designs, tests/tasks, quickstart, and traceability scope only aggregate/binding operations and exclude global catalogs | Artifact ambiguity remediated; new analysis pending |
| `A3` | Product/passport/foreign fields now have exact ASCII regex, bounds, case, normalization, examples, layer enforcement, and vectors; observed V3 differences are recorded above | Specification ambiguity remediated; migration conformity deviation remains in retrospective review |
| `T1` | T013 and T014 now reference and substantively implement FR-045, FR-046, FR-047, SC-031, and SC-032; traceability explains the mapping | Traceability gap remediated; retrospective conformity review pending |

## Owner Approval

**Status**: **NOT RECORDED**

No approver, approval date, approval commit, risk acceptance, or release decision is recorded.
These fields MUST remain unasserted until the real approval authority completes the review.
