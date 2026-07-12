# Legacy Evidence

## Purpose

This directory contains historical evidence from the legacy `open-api-facturacion-sri` system used to support the reengineering of `ecuador-tax-document-service`.

The material in this directory helps discover:

- Existing business concepts and terminology.
- Observed electronic tax document behavior.
- SRI integration flows.
- Legacy validation and calculation rules.
- Data relationships and persistence boundaries.
- Security weaknesses and technical debt.
- Unresolved functional, fiscal, security, and operational questions.

This directory does not define the target system.

## Authority Boundary

Legacy artifacts are evidence, not target requirements.

The target service is a greenfield reengineering effort. It is not required to preserve legacy:

- API routes or HTTP contracts.
- Request or response payloads.
- Database schemas or naming.
- NestJS architecture or implementation patterns.
- Local JWT authentication.
- BullMQ or Redis behavior.
- Filesystem storage conventions.
- Deployment definitions.
- Error messages or status vocabularies.
- Defects, incomplete behavior, or undocumented assumptions.

A legacy behavior may become a target requirement only when it is explicitly accepted in an approved target specification or architecture decision.

## Source Baseline

The exact source revision, archive digest, working-tree state, evidence coverage, and finalization requirements are documented in:

- [`source-baseline.md`](source-baseline.md)

The current source baseline is `PROVISIONAL` because the supplied `docs/as-is/` directory was not committed in the legacy Git snapshot.

No legacy finding should be represented as reproducible Git evidence until the baseline finalization procedure has been completed.

## Directory Structure

```text
docs/legacy/
├── README.md
├── source-baseline.md
└── as-is/
    ├── 00-system-overview.md
    ├── 01-source-inventory.md
    ├── 02-module-inventory.md
    ├── 03-entry-points.md
    ├── 04-data-model.md
    ├── 05-business-rules.md
    ├── 06-validation-rules.md
    ├── 07-process-flows.md
    ├── 08-integrations.md
    ├── 09-batch-processes.md
    ├── 10-security-access-control.md
    ├── 11-reports-outputs.md
    ├── 12-error-handling.md
    ├── 13-technical-debt.md
    ├── 14-pending-functional-validation.md
    └── 15-sdd-migration-backlog.md
```

## Evidence Set

The AS-IS evidence set contains 16 documents covering:

| Area | Primary document |
|---|---|
| System scope and observed purpose | `as-is/00-system-overview.md` |
| Reviewed sources and coverage | `as-is/01-source-inventory.md` |
| NestJS module inventory | `as-is/02-module-inventory.md` |
| HTTP, queue, event, and lifecycle entry points | `as-is/03-entry-points.md` |
| Legacy physical data model | `as-is/04-data-model.md` |
| Observed business rules | `as-is/05-business-rules.md` |
| Observed validation behavior | `as-is/06-validation-rules.md` |
| Invoice and common SRI process flows | `as-is/07-process-flows.md` |
| External integrations | `as-is/08-integrations.md` |
| Batch and asynchronous processing | `as-is/09-batch-processes.md` |
| Authentication and access controls | `as-is/10-security-access-control.md` |
| Reports, files, events, and outputs | `as-is/11-reports-outputs.md` |
| Error and failure behavior | `as-is/12-error-handling.md` |
| Confirmed technical debt | `as-is/13-technical-debt.md` |
| Unresolved functional validation | `as-is/14-pending-functional-validation.md` |
| Candidate migration behaviors | `as-is/15-sdd-migration-backlog.md` |

## Coverage Warning

The detailed functional analysis primarily covers the common SRI flow as exercised by invoice issuance.

Equivalent detailed coverage has not been confirmed for:

- Credit notes.
- Debit notes.
- Withholding documents.
- Remission guides.
- All administrative capabilities outside the analyzed invoice path.
- Actual production runtime and deployment behavior.

Target feature specifications for these areas must use additional evidence or independently approved requirements.

## Rules for Using Legacy Evidence

When using a legacy finding in target work:

1. Identify the exact AS-IS document and finding identifier, such as `BR-*`, `VR-*`, `DF-*`, `RISK-*`, `PFV-*`, or `MIG-*`.
2. Verify whether the finding is confirmed, conditional, contradictory, or pending validation.
3. Compare it with applicable, versioned official SRI documentation.
4. Classify the behavior in `docs/migration/legacy-disposition-register.md`.
5. Record any required terminology decision in `docs/migration/terminology-mapping.md`.
6. Express accepted target behavior as a new requirement with independently testable acceptance scenarios.
7. Never copy a legacy defect or ambiguity into the target specification by default.

The permitted target dispositions are:

- `RETAIN`: preserve a confirmed and approved behavior.
- `REDESIGN`: preserve the business need through a new target design.
- `RETIRE`: deliberately remove the legacy behavior.
- `PENDING`: do not implement until the required decision is available.

## Pending Functional Validation

An entry in `14-pending-functional-validation.md` is not resolved merely because its answer column contains text.

Before a PFV affects implementation, it must be classified for the target as:

- `RESOLVED_FOR_TARGET`
- `NOT_APPLICABLE_TO_TARGET`
- `DEFERRED_WITH_BOUNDARY`
- `BLOCKING`

Agents and implementers must not invent missing answers or transform uncertain legacy behavior into target requirements.

## Immutability

Files under `docs/legacy/as-is/` are immutable historical evidence after the source baseline becomes final.

Do not:

- Translate or rewrite them.
- Correct grammar or terminology in place.
- Replace legacy names with target English names.
- Mark PFVs as resolved inside the historical file.
- Remove inconvenient risks or contradictions.
- Add target architecture decisions to AS-IS documents.

Corrections, interpretations, dispositions, and new decisions belong in target-controlled documents outside `docs/legacy/as-is/`.

## Target-Controlled Documentation

Use the following locations for new work:

| Purpose | Location |
|---|---|
| Legacy-to-target decisions | `docs/migration/legacy-disposition-register.md` |
| Canonical English terminology | `docs/migration/terminology-mapping.md` |
| Unresolved target decisions | `docs/migration/open-decisions.md` |
| Architecture decisions | `docs/architecture/decisions/` |
| Feature requirements and planning artifacts | `specs/` |
| Project-wide engineering governance | `.specify/memory/constitution.md` |

## Language Policy

Historical AS-IS files may remain in Spanish to preserve evidence fidelity.

All target artifacts must use English, including:

- Source code and identifiers.
- API contracts.
- Database objects and migrations.
- Tests and fixtures.
- Specifications, plans, tasks, and ADRs.
- Comments, JavaDoc, logs, and configuration keys.

Official SRI XML names, catalog codes, and legally defined values may retain their authoritative representation inside the SRI adapter boundary.

## Sensitive Information

This directory must never contain:

- PKCS#12 files or private keys.
- Certificate passwords.
- JWT, database, Redis, webhook, or encryption secrets.
- Real taxpayer or customer information.
- Production XML documents.
- Reusable administrative credentials.

Test evidence must be synthetic, anonymized, or explicitly approved for repository use.

## Review Responsibility

Every feature specification and implementation plan that relies on legacy evidence must verify:

- The evidence exists in the recorded baseline.
- Its confidence and limitations are understood.
- Related PFVs and risks have been addressed.
- The selected target disposition is explicit.
- The resulting target requirement does not preserve compatibility accidentally.