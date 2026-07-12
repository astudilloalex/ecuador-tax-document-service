# Legacy Source Baseline

## 1. Document Control

| Field | Value |
|---|---|
| Target project | `ecuador-tax-document-service` |
| Baseline status | `PROVISIONAL` |
| Legacy system | `open-api-facturacion-sri` |
| Legacy repository | `https://github.com/AngeloBarzolaVillamar/open-api-facturacion-sri.git` |
| Legacy branch | `main` |
| Legacy source revision | `2dc632dfaa26361e9f86b8e9226d139ca5164833` |
| AS-IS documentation revision | Not available: `docs/as-is/` is untracked in the supplied snapshot |
| Baseline tag | Not available |
| AS-IS cut-off date | 2026-07-10 |
| Baseline inspection date | 2026-07-12 |
| Source archive | `open-api-facturacion-sri.zip` |
| Archive size | 928,462 bytes |
| Archive SHA-256 | `4104af56f2ee497ad34d8db0499d1c82c6416c92149ef99db72e5e6542188992` |

## 2. Purpose

This document identifies the exact legacy source snapshot used as evidence for the reengineering of `ecuador-tax-document-service`.

The legacy system is a discovery source only. It helps identify domain concepts, SRI integrations, business rules, validation behavior, data relationships, operational risks, and unresolved functional questions.

This baseline does not authorize a line-by-line migration and does not establish compatibility requirements for the target service.

## 3. Reengineering Boundary

The target service is a greenfield implementation. Unless an approved target specification explicitly requires otherwise, the following legacy elements are non-authoritative:

- HTTP routes, status codes, payloads, and response structures.
- NestJS modules, controllers, services, DTOs, guards, interceptors, and processors.
- Local JWT authentication and authorization behavior.
- PostgreSQL tables, columns, constraints, seed data, and SQL naming.
- BullMQ and Redis queue behavior.
- Filesystem storage conventions.
- Docker and Compose deployment definitions.
- Environment variable names and defaults.
- Error messages, retry behavior, status vocabularies, and undocumented conventions.
- Defects, inconsistencies, technical debt, and incomplete behavior.

Legacy behavior may become a target requirement only after it is evaluated, explicitly accepted, and recorded in an approved target specification or architecture decision.

## 4. Evidence Authority

When sources conflict, the following precedence applies:

1. Applicable Ecuadorian legislation and versioned official SRI technical documentation.
2. The target project's constitution.
3. Approved target feature specifications and recorded clarifications.
4. Approved architecture decisions and implementation plans.
5. Legacy AS-IS documents and legacy source code.

Legacy implementation behavior must not override an applicable fiscal, legal, security, or target requirement.

## 5. Supplied Snapshot State

The supplied archive contains a Git working tree based on commit `2dc632dfaa26361e9f86b8e9226d139ca5164833`, dated 2026-06-27.

The working tree is not clean. The following state was observed during baseline inspection:

### Deleted tracked documentation

- `docs/README.md`
- `docs/api-sri.md`
- `docs/base-datos.md`
- `docs/catalogos.md`
- `docs/configuracion.md`
- `docs/guia-remision.md`
- `docs/nota-credito.md`
- `docs/nota-debito.md`
- `docs/retenciones.md`

These deletions were reported as intentional. The deleted files are not part of the current AS-IS evidence set and must not be restored or incorporated without an explicit owner decision.

### Modified tracked file

- `package-lock.json`

The modification is not part of the documented AS-IS baseline and must not be included in a documentation baseline commit unless independently reviewed and approved.

### Untracked paths

- `.codex/`
- `AGENTS.md`
- `docs/as-is/`

Because `docs/as-is/` is untracked, its content cannot yet be linked to an immutable Git revision. This is the reason this baseline remains `PROVISIONAL`.

## 6. AS-IS Documentation Inventory

The supplied AS-IS documentation consists of the following 16 files:

1. `00-system-overview.md`
2. `01-source-inventory.md`
3. `02-module-inventory.md`
4. `03-entry-points.md`
5. `04-data-model.md`
6. `05-business-rules.md`
7. `06-validation-rules.md`
8. `07-process-flows.md`
9. `08-integrations.md`
10. `09-batch-processes.md`
11. `10-security-access-control.md`
12. `11-reports-outputs.md`
13. `12-error-handling.md`
14. `13-technical-debt.md`
15. `14-pending-functional-validation.md`
16. `15-sdd-migration-backlog.md`

When transferred to the target repository, these files must remain unchanged under:

```text
docs/legacy/as-is/
```

Historical documents may remain in Spanish. Their language and wording must not be normalized as part of target implementation work because doing so would alter the captured evidence.

## 7. Confirmed Coverage

The AS-IS documentation provides:

- Static source, module, entry-point, configuration, and storage inventories.
- A structural description of the NestJS application.
- A detailed analysis of the common SRI flow as exercised by invoice issuance.
- Observed invoice validation, calculation, access-key generation, XML generation, signing, SOAP interaction, persistence, event, and output behavior.
- Integration descriptions for PostgreSQL, Redis, BullMQ, filesystem storage, SRI SOAP services, Carbone, and outgoing webhooks.
- Identified security weaknesses, technical debt, failure boundaries, and unresolved questions.

All findings were obtained through static inspection. The documentation explicitly states that the application, jobs, SRI calls, and tests were not executed as part of the AS-IS analysis.

## 8. Known Coverage Limitations

The current detailed functional analysis does not provide equivalent coverage for:

- Credit notes.
- Debit notes.
- Withholding documents.
- Remission guides.
- All administrative modules outside the analyzed invoice path.
- Actual production configuration and deployment behavior.
- Runtime queue behavior and effective retry configuration.
- Production database compatibility with the supplied SQL dump.
- Operational reconciliation across SRI, PostgreSQL, Redis, and filesystem storage.

Target specifications for these areas must not claim complete legacy coverage unless additional evidence is collected or the target requirements are independently defined and approved.

## 9. Pending Functional Validation Status

The supplied PFV register contains 39 entries:

- 11 entries contain text in the answer column.
- 28 entries have no answer.

A non-empty answer does not automatically mean that a PFV is resolved. For example:

- An answer that restates that no evidence was found remains unresolved.
- A yes/no answer that does not identify the selected authority remains ambiguous.
- A future implementation choice does not necessarily resolve the underlying functional or data requirement.

Every PFV used by a target feature must be classified as one of the following before implementation:

- `RESOLVED_FOR_TARGET`
- `NOT_APPLICABLE_TO_TARGET`
- `DEFERRED_WITH_BOUNDARY`
- `BLOCKING`

Agents and implementers must not infer missing PFV answers.

## 10. Legacy Artifact Disposition

| Legacy artifact | Target disposition |
|---|---|
| `docs/as-is/*.md` | Copy unchanged to `docs/legacy/as-is/` after the baseline is finalized |
| NestJS `src/` | Keep in the legacy repository only |
| `database/init.sql` | Retain as legacy evidence; do not use as the target migration baseline |
| `database/Install BD.txt` | Retain as legacy evidence only |
| Postman collection | Retain as a legacy contract example; do not publish as the target API contract |
| Jest tests | Use only to discover scenarios; rewrite tests from approved target requirements |
| Docker and Compose files | Do not copy as target deployment artifacts |
| Legacy `.env.example` | Do not copy as target configuration |
| Legacy `.git/` | Never copy |
| Legacy `.codex/`, `.agents/`, and `AGENTS.md` | Do not copy as target agent governance |
| P12 files, passwords, tokens, real XML, or production data | Never copy |

## 11. Security and Privacy Restrictions

Legacy evidence transferred to the target repository must not contain:

- Private keys or PKCS#12 files.
- Certificate passwords.
- JWT signing secrets.
- Database, Redis, webhook, or encryption credentials.
- Real taxpayer or customer information.
- Production XML documents.
- Reusable administrative credentials.

Any required test evidence must be synthetic, anonymized, or explicitly approved for repository use.

## 12. Finalization Procedure

Before changing this baseline from `PROVISIONAL` to `FINAL`:

1. Commit the intentional deletion of obsolete legacy documentation, if the owner wants those deletions represented in the baseline history.
2. Commit all 16 files under `docs/as-is/` in the legacy repository.
3. Exclude the unrelated `package-lock.json` modification from the documentation baseline commit.
4. Record the resulting AS-IS documentation commit in this file.
5. Create and record an annotated baseline tag, such as `legacy-baseline-2026-07-10`.
6. Verify that the committed AS-IS files match the files transferred to `docs/legacy/as-is/`.
7. Replace the `PROVISIONAL` status with `FINAL` only after the revision and tag are verifiable.

## 13. Change Control

Historical files under `docs/legacy/as-is/` are immutable evidence. Corrections, interpretations, target decisions, and PFV resolutions must be recorded in separate target-controlled documents, such as:

- `docs/migration/legacy-disposition-register.md`
- `docs/migration/terminology-mapping.md`
- `docs/migration/open-decisions.md`
- `docs/architecture/decisions/`
- Feature specifications under `specs/`

Changing this baseline requires a documented reason and must not rewrite the historical evidence it references.