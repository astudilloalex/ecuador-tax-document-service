# Governance Non-Conformity Record: Analyze Gate Ordering

**Record ID**: `GOV-001`

**Feature**: `001-create-invoice-draft`

**Detected**: 2026-07-15 (`America/Guayaquil`)

**Current governing constitution**: `.specify/memory/constitution.md` v2.0.1, Principle XIV and
Definition of Done

**Status**: `APPROVED_WITH_MANDATORY_CORRECTIVE_ACTIONS`

## Exact non-conformity

Tasks T001–T016 were implemented and marked complete before a verifiable post-`tasks.md`
`$speckit-analyze` execution. That sequence violated Constitution Principle XIV and cannot be
corrected retroactively. Approval does not rewrite Git history, erase completed work, backdate an
analysis, or pretend that the required order occurred.

The completed evidence-based review is
[`governance-retrospective-review.md`](governance-retrospective-review.md). Its formal approval is
[`governance-owner-approval.md`](governance-owner-approval.md). The later, formally approved change
to corrective execution order is recorded separately in
[`governance-corrective-assignment-addendum.md`](governance-corrective-assignment-addendum.md).

## Repository evidence

| Evidence | Repository commit | Date (`America/Guayaquil`) |
|----------|-------------------|-----------------------------|
| Feature approval and dependency-ordered task generation | `1289871aafb6795a1a4d7f36589646470a550aae` | 2026-07-13 18:47:47 -05:00 |
| T001–T012 foundations | `8bdd54813e55ce896b9c994e514b864668cbac09` | 2026-07-14 23:31:06 -05:00 |
| T013–T016 migrations and supporting evidence | `5e5452ab069006496264a5e3ebf2a6fc072b5cfe` | 2026-07-15 19:46:25 -05:00 |
| Reviewed remediation baseline | `07393539db7d1e994ddf2e93c4d88949d8a35f66` | 2026-07-15 20:54:15 -05:00 |

**Retrospective SHA-256**:
`540f840a1903840a19566675b6935c2591a903218b05a3ebfef4c805cdae0063`

## Approved retrospective outcome

The task-by-task review covers T001–T016 against the current constitution, specification, plan,
tasks, OpenAPI contract, data model, supporting designs, implementation, tests, migrations, and Git
history. Approval was recorded by `astudilloalex` at `2026-07-16T03:35:25Z`, acting as both
`Constitutional Governance Owner` and `Owner of 001-create-invoice-draft`, using identity method
`Explicit declaration by the repository owner`.

The following table preserves the corrective-task assignment as it was approved with the original
retrospective disposition. It is historical approval evidence and is not rewritten as though the
later sequencing had always existed.

| ID | Affected task | Approved disposition | Originally assigned corrective task |
|----|---------------|----------------------|---------------------------|
| D1 | T001–T016 | Accepted as a documented historical process non-conformity; it is not erased or retroactively corrected | Retain this record and current analysis evidence |
| D2 | T010 | Accepted only with mandatory executable ASCII/PostgreSQL, Flyway, and cross-layer evidence | T018 |
| D3 | T015/V3 | Accepted only with immutable V3 and a new V5 enforcing the approved ASCII barriers | T017, validated by T018 |

T001–T009, T012–T014, and T016 retain conforming dispositions. T008 and T011 retain evidence
clarifications. T010 remains historically completed but has a mandatory unresolved corrective
dependency. T015 remains superseded only for the two affected ASCII barriers by the mandatory
corrective work. The approved addendum below changes execution assignment, not these findings.

## Approved corrective-assignment addendum

`astudilloalex`, acting as both `Constitutional Governance Owner` and
`Owner of 001-create-invoice-draft`, approved
[`governance-corrective-assignment-addendum.md`](governance-corrective-assignment-addendum.md) at
`2026-07-17T00:12:28Z` on the basis `Explicit repository-owner declaration`.

The addendum decision is `APPROVED_CORRECTIVE_ASSIGNMENT_ADDENDUM`; its final disposition is
`T017_RED_EVIDENCE_T018_V5_IMPLEMENTATION`. It preserves the retrospective review and its SHA-256,
D1–D3, Constitution 2.0.1, T001–T016 completion history, and V3 immutability. Only the prospective
execution sequence for D2/D3 changes:

- T017 owns the shared authoritative vectors, fixture-integrity checks, and intentional red
  PostgreSQL/Flyway evidence exposing the known V3 mismatch. It creates no V5, does not modify V3,
  and does not claim production Java-validator equivalence.
- T018 depends on completed T017, creates V5 without modifying V3, and makes the T017
  PostgreSQL/Flyway evidence green through V3-to-V5 upgrade and Flyway validation.
- T030, T045, and T050 independently consume the same fixture for OpenAPI, production
  buyer-identification Java validation, and production product-code/text-rule Java validation,
  respectively. Domain tests do not directly depend on PostgreSQL, Flyway, OpenAPI parser, or HTTP
  infrastructure.

## Mandatory corrective work

- T017 creates the authoritative shared fixture, validates its integrity, and prepares intentional
  red PostgreSQL/Flyway evidence that proves the known V3 barriers do not reject every approved
  invalid vector. A standalone Java `Pattern` may verify only fixture parsing or the approved
  literal expression. T017 creates no migration and must never edit V3.
- T018 creates `V5__tighten_invoice_draft_ascii_constraints.sql`, replaces only V3's affected named
  constraints with the exact approved explicit-ASCII expressions, and makes the T017 persistence
  evidence green through V3→V5, empty-database migration, final-schema behavior, migration
  inventory, affected reference assertions, and Flyway validation. V3 must never be edited.
- Productive cross-layer equivalence does not belong to T017 or T018. It is established by the
  independent T017/T018, T030, T045, and T050 suites consuming the same authoritative fixture and
  asserting the same expected outcomes.

T017 and T018 remain pending and cannot be skipped. T018 depends explicitly on completed T017.
T019 and every later business task remain blocked until T017 and T018 complete successfully. A
failure in either corrective task blocks all later business implementation.

## Constitutional amendment approval

Constitution v2.0.1 is `APPROVED` only for these PATCH clarifications:

- Company identifiers are prohibited in request bodies and input schemas.
- Company identifiers may appear in responses only when the approved contract explicitly requires
  them.
- Company scope is mandatory for the Invoice Draft aggregate and its idempotency binding.
- Immutable global SRI catalogs are not automatically Company-scoped.

No other constitutional change is approved by GOV-001.

## GATE-GOV-001 — RELEASED

**Gate status**: `RELEASED`

**Approver**: `astudilloalex`

**Release timestamp**: `2026-07-16T03:35:25Z`

**Identity verification method**: `Explicit declaration by the repository owner`

- [x] T001–T016 were retrospectively reviewed.
- [x] D1–D3 were explicitly accepted with their recorded dispositions.
- [x] Constitution v2.0.1 was approved by the constitutional-governance owner.
- [x] The same approver explicitly exercises both required approval capacities.
- [x] The reviewed baseline commit and retrospective SHA-256 are recorded.
- [x] T017 and T018 remain mandatory and pending.

Releasing this governance gate authorizes only progression to T017 after the mandatory new
`$speckit-analyze` gate. It does not authorize skipping T017 or T018, starting T019, marking
corrective work complete, or executing later business implementation prematurely.

The addendum is approved, but the latest analysis contains a CRITICAL finding about the previously
unreconciled assignment. Therefore current implementation permission is
`PENDING_SUCCESSFUL_ANALYSIS`; T017 cannot begin until a new `$speckit-analyze` confirms that the
CRITICAL finding is gone.

## Latest analysis remediation index

| Finding | Explicit remediation | Disposition |
|---------|----------------------|-------------|
| C1 | Complete retrospective, approved D1–D3 dispositions, and released governance gate | Approved without erasing the historical violation |
| C2 | Approved addendum assigns T017 red PostgreSQL/Flyway evidence and T018 immutable V5/green persistence evidence; T030/T045/T050 independently complete OpenAPI/Java equivalence from the same fixture | Mandatory corrective work; V3 unchanged; implementation pending successful analysis |
| I3 | Stage 10 independent validation, Stage 11A calculation, and ordered Stage 11B validation | Artifact inconsistency remediated |
| I4 | API-exclusive deadline/HTTP arbitration and transport-neutral application/repositories | Architecture inconsistency remediated |
| A4 | Constitution v2.0.1 request/input prohibition and explicit contract-required response allowance | PATCH approved |
| A5 | Constitution v2.0.1 aggregate/binding Company scope and global-catalog exclusion | PATCH approved |
| U2 | Executable Unicode/NFC/U+0020/code-point/canonicalName policy | Specification ambiguity remediated |
| U3 | Payment-method effectiveness bound inclusively to invoice `emissionDate` | Specification ambiguity remediated |
| A6 | T076 sole persistence-clock owner; T063 cannot supply or overwrite `createdAt` | Ownership ambiguity remediated |

## Owner approval

**Status**: `APPROVED`

**Retrospective decision**: `APPROVED_WITH_MANDATORY_CORRECTIVE_ACTIONS`

**Final governance decision**: `APPROVED`

**Corrective-assignment addendum decision**: `APPROVED_CORRECTIVE_ASSIGNMENT_ADDENDUM`

**Current implementation permission**: `PENDING_SUCCESSFUL_ANALYSIS`

**approvalCommit**: `TO_BE_RECORDED_AFTER_COMMIT`
