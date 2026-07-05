# Implementation Plan: Canonical Terminology and Architecture Boundaries

**Branch**: `001-canonical-terminology-boundaries` | **Date**: 2026-07-05 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-canonical-terminology-boundaries/spec.md`

## Summary

Define the target English terminology model, legacy-to-target mapping rules, and
Clean Architecture boundary rules for the Ecuador electronic tax document
service. The implementation approach is documentation-only: publish durable
architecture rules under `docs/architecture` and baseline migration mappings
under `docs/migration`, with document contracts and validation criteria captured
in this feature's design artifacts.

This plan does not create Quarkus classes, REST endpoints, database migrations,
persistence entities, SRI clients, or runtime behavior.

## Technical Context

**Language/Version**: Java 25 for the target backend; no Java source is created
by this enabler.

**Primary Dependencies**: Quarkus and Gradle with Kotlin DSL are the target stack constraints for
future backend work; this enabler requires only Markdown documentation.

**Storage**: N/A for this enabler. Future persistence rules are documented, but
no target database objects are created.

**Testing**: Documentation validation by review and shell checks. Future code
features must use JUnit, Quarkus test support for adapters only, Testcontainers
or contract fixtures when needed.

**Target Platform**: Backend service governance documentation.

**Project Type**: Quarkus backend using Clean Architecture and Ports and
Adapters; this feature is an architecture enabler.

**Base Package**: `com.alexastudillo.taxdocument`

**Performance Goals**: A reviewer can validate naming and boundary compliance
for a future feature using the generated documents without inspecting legacy
source code directly.

**Constraints**: No implementation code, REST endpoints, database migrations,
JPA/Panache entities, SRI clients, authentication, webhooks, production data
migration, or legacy refactoring. Current Gradle scaffold remains unchanged;
Gradle with Kotlin DSL migration is a future setup concern.

**Scale/Scope**: Initial baseline covers the approved constitution glossary,
artifact-specific rendering rules, architecture layer boundaries, compatibility
exception rules, pending decision gates, and a pending classification path for
newly discovered legacy terms and behaviors.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Clean Architecture**: PASS. This feature documents boundaries and creates no
  runtime dependencies.
- **Use Cases**: PASS. No business use cases are implemented; future business
  operations are required to be explicit use cases.
- **Ports and Adapters**: PASS. No external dependency is introduced; future
  dependency rules require application ports and adapter implementations.
- **SRI Isolation**: PASS. Official SRI names are restricted to SRI adapter,
  fixture, compatibility, migration, or mapping artifacts.
- **English Terminology**: PASS. Target terminology is English and canonical;
  unresolved terms must be tracked as Pending Naming Decisions.
- **DTO Separation**: PASS. No DTOs are created; future DTO separation rules are
  documented.
- **Validation Separation**: PASS. No validation logic is implemented; future
  transport, application, and domain validation ownership is documented.
- **Idempotency and Auditability**: PASS. No critical runtime operation is
  implemented; future critical features must define idempotency and audit rules.
- **Testing Boundary**: PASS. Documentation validation does not require
  infrastructure; future code testing boundaries are documented.
- **Migration Classification**: PASS. Migrated concepts are classified and
  pending classification paths are defined.
- **No Legacy Copying**: PASS. The plan creates target governance documents and
  does not copy legacy module structure, database design, or technical coupling.

Post-design re-check: PASS. The Phase 1 artifacts preserve the same boundaries
and do not introduce implementation work.

## Project Structure

### Documentation (this feature)

```text
specs/001-canonical-terminology-boundaries/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── architecture-rules-document.md
│   ├── canonical-terminology-document.md
│   ├── legacy-mapping-document.md
│   └── future-spec-governance.md
└── tasks.md
```

### Target Documentation To Be Created By Tasks

```text
docs/
├── architecture/
│   ├── backend-clean-architecture.md
│   └── canonical-terminology.md
└── migration/
    └── legacy-to-target-terminology.md
```

### Constitution-Governed Durable Locations

This enabler creates the target governance documents listed above and documents
the full constitution-governed location policy for future work:

| Artifact Category | Required Location | Purpose |
|-------------------|-------------------|---------|
| AS-IS legacy documentation | `docs/legacy/` | Evidence-based documentation of the legacy system without mixing it with target specifications. |
| Migration mappings and canonical terminology | `docs/migration/` | Canonical language, legacy-to-target mappings, forbidden legacy terms, pending naming decisions, and migration classifications. |
| Architecture rules and decisions | `docs/architecture/` or `docs/adr/` | Clean Architecture rules, architectural decisions, and backend governance documentation. |
| Target specifications | `.specify/specs/` | Active Spec Kit target specifications, plans, tasks, contracts, and feature artifacts. |

No AS-IS legacy documentation, ADR, or target runtime artifact is created by this
feature unless it is part of the documentation deliverables explicitly listed in
this plan.

### Source Code (repository root)

```text
src/main/java/com/alexastudillo/taxdocument/
└── no changes for this feature

src/test/java/com/alexastudillo/taxdocument/
└── no changes for this feature
```

**Structure Decision**: This enabler is documentation-only. It creates no domain,
application, adapter, bootstrap, resource, entity, DTO, migration, or test code.

## Layer and Boundary Design

**Domain Concepts**: The plan defines future domain ownership rules for tax
document concepts, value objects, aggregates, invariants, domain events, and
business exceptions. No runtime domain classes are created.

**Application Use Cases**: The plan defines future use case naming and ownership
rules. No application use cases, commands, queries, ports, or transaction
boundaries are implemented.

**Inbound REST Adapter**: The plan defines future REST naming, transport
validation, and DTO separation rules. No REST resources or contracts are
implemented.

**Outbound Adapters**: The plan defines future persistence, SRI, storage, queue,
webhook, clock, and transaction port isolation rules. No adapters are
implemented.

**DTO Mapping Flow**: The generated architecture documentation must preserve the
required future mapping flow:

```text
REST DTO -> Application Command
Application Result -> REST Response DTO
Domain Object -> Persistence Entity
Domain Object -> SRI XML DTO
SRI Response DTO -> Application Result
```

## Naming and Migration Classification

**Canonical Terms Used**: taxDocument, invoice, creditNote, debitNote,
withholding, waybill, issuer, buyer, recipient, accessKey,
authorizationNumber, issueDate, authorizedAt, legalName, tradeName,
establishment, issuingPoint, sequenceNumber.

**Legacy Terms Mapped**:

| Legacy Term | Target Term | Classification | Notes |
|-------------|-------------|----------------|-------|
| comprobante | taxDocument | Target domain concept | Approved baseline |
| factura | invoice | Target domain concept | Approved baseline |
| nota credito | creditNote | Target domain concept | Approved baseline |
| nota debito | debitNote | Target domain concept | Approved baseline |
| retencion | withholding | Target domain concept | Approved baseline |
| guia remision | waybill | Target domain concept | Approved baseline |
| emisor | issuer | Target domain concept | Approved baseline |
| comprador | buyer | Target domain concept | Approved baseline |
| receptor | recipient | Target domain concept | Approved baseline |
| clave acceso | accessKey | Target domain concept | Approved baseline |
| numero autorizacion | authorizationNumber | Target domain concept | Approved baseline |
| fecha emision | issueDate | Target API field | Approved baseline |
| fecha autorizacion | authorizedAt | Target API field | Approved baseline |
| razon social | legalName | Target API field | Approved baseline |
| nombre comercial | tradeName | Target API field | Approved baseline |
| establecimiento | establishment | Target domain concept | Approved baseline |
| punto emision | issuingPoint | Target domain concept | Approved baseline |
| secuencial | sequenceNumber | Target domain concept | Approved baseline |
| Official SRI XML tags | English target terms or contract-only names | SRI adapter-only concept | Exact official names remain isolated |
| Legacy API payload names | English target terms or compatibility-only names | Legacy compatibility concept | Pending per future feature |
| Legacy database object names | English target database names or migration-only names | Migration-only concept | Pending per future feature |
| Unmapped legacy terms | To be assigned | Pending Naming Decision | Must resolve before task generation for affected work |
| Unverified legacy behavior | To be validated | Pending Functional Validation | Must resolve, exclude, or defer before affected tasks |

**Pending Naming Decisions**: None for the approved baseline terms. Newly
discovered terms must be recorded in `docs/migration/legacy-to-target-terminology.md`
and resolved before affected task generation.

**Pending Functional Validations**: None for this enabler's deliverables. Future
unverified legacy behaviors must be recorded and resolved, excluded, or deferred
before affected task generation.

## Idempotency, Audit, and Error Handling

**Idempotency Rules**: N/A for this enabler. The generated architecture rules
must require future critical features to define idempotency for tax document
issuance, SRI authorization retries, synchronization runs, webhook delivery,
XML generation, and sequence assignment.

**Audit Events**: N/A for this enabler. The generated architecture rules must
require future critical features to define audit events without secrets,
credentials, private keys, tokens, signing passwords, or sensitive
configuration values.

**Error Mapping**: N/A for this enabler. The generated architecture rules must
require future features to map domain/application errors to REST errors and
adapter failures to application results.

## Phase 0 Research Summary

Phase 0 decisions are captured in [research.md](./research.md). All planning
unknowns are resolved.

## Phase 1 Design Summary

Phase 1 artifacts:

- [data-model.md](./data-model.md)
- [contracts/architecture-rules-document.md](./contracts/architecture-rules-document.md)
- [contracts/canonical-terminology-document.md](./contracts/canonical-terminology-document.md)
- [contracts/legacy-mapping-document.md](./contracts/legacy-mapping-document.md)
- [contracts/future-spec-governance.md](./contracts/future-spec-governance.md)
- [quickstart.md](./quickstart.md)

Agent context update: no agent context update script is present in this Spec Kit
installation, so no generated agent context update was run.

## Traceability and Source of Truth

Feature specifications use `FR-###`, `AR-###`, `NR-###`, `TR-###`, and `SC-###`
identifiers. Future task lists must use `T###` identifiers and cite the
governing requirement ID or contract section for each task.

The durable source of truth after this enabler is implemented is:

- `docs/architecture/backend-clean-architecture.md` for layer boundaries and
  Clean Architecture rules.
- `docs/architecture/canonical-terminology.md` for approved canonical English
  terms and artifact-specific formats.
- `docs/migration/legacy-to-target-terminology.md` for legacy-to-target
  mappings, pending naming decisions, pending functional validations, SRI
  adapter-only terms, and compatibility exceptions.
- `docs/legacy/` for AS-IS legacy documentation produced by future legacy
  documentation work.
- `docs/architecture/` or `docs/adr/` for architecture rules and decisions.
- `.specify/specs/` for active target specifications, plans, tasks, contracts,
  and feature artifacts.

The feature artifacts under `specs/001-canonical-terminology-boundaries/`
remain the planning, contract, and review record. They do not supersede the
durable `docs/architecture` and `docs/migration` outputs after implementation.

## Complexity Tracking

No constitution violations or compatibility exceptions are required.

| Violation or Exception | Why Needed | Scope and Expiration | Safer Alternative Rejected Because |
|------------------------|------------|----------------------|------------------------------------|
| None | N/A | N/A | N/A |
