# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]

**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See
`.specify/templates/plan-template.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach]

## Technical Context

**Language/Version**: Java 25

**Primary Dependencies**: Quarkus, Maven, and project-approved Quarkus
extensions only

**Storage**: [PostgreSQL, Redis, XML storage, queue, or N/A]

**Testing**: JUnit, Quarkus test support for adapters only, Testcontainers or
contract fixtures when needed

**Target Platform**: Backend service

**Project Type**: Quarkus backend using Clean Architecture and Ports and
Adapters

**Base Package**: `com.alexastudillo.taxdocument`

**Performance Goals**: [Domain-specific goals or NEEDS CLARIFICATION]

**Constraints**: [Domain-specific constraints or NEEDS CLARIFICATION]

**Scale/Scope**: [Domain-specific scale or NEEDS CLARIFICATION]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Clean Architecture**: Domain, application, adapters, and bootstrap
  responsibilities are identified. Dependencies point inward only.
- **Use Cases**: Each business operation maps to an explicit application use
  case; generic service names are not used for business behavior.
- **Ports and Adapters**: Every external dependency is accessed through an
  application port with a named adapter implementation.
- **SRI Isolation**: XML tags, SOAP DTOs, official Spanish SRI names, signing,
  reception, authorization, and response parsing remain inside
  `adapter.out.sri`.
- **English Terminology**: Target names use canonical English terminology.
  Unclear terms are listed as Pending Naming Decisions.
- **DTO Separation**: REST DTOs, application commands/results, domain objects,
  persistence entities, and SRI DTOs are not reused across layers.
- **Validation Separation**: Transport, application, and domain validation
  responsibilities are documented.
- **Idempotency and Auditability**: Repeatable critical operations define
  idempotency keys or rules and audit events without secrets.
- **Testing Boundary**: Domain and application tests avoid Quarkus and real
  infrastructure; adapter tests identify fixtures, mocks, or containers.
- **Migration Classification**: Every migrated concept is classified as target
  domain, target API field, target database object, SRI adapter-only, legacy
  compatibility, migration-only, deprecated, Pending Naming Decision, or Pending
  Functional Validation.
- **No Legacy Copying**: The plan does not copy legacy module structure,
  database design, naming, or technical coupling.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command)
```

### Source Code (repository root)

```text
src/main/java/com/alexastudillo/taxdocument/
├── domain/
├── application/
├── adapter/
│   ├── in/
│   │   └── rest/
│   └── out/
│       ├── persistence/
│       ├── sri/
│       ├── storage/
│       ├── queue/
│       └── webhook/
└── bootstrap/

src/main/resources/

src/test/java/com/alexastudillo/taxdocument/
├── domain/
├── application/
└── adapter/
```

**Structure Decision**: [Document concrete packages and files used by this
feature. Explain any omitted adapter package.]

## Layer and Boundary Design

**Domain Concepts**: [Entities, value objects, aggregates, domain services,
domain events, invariants, and business exceptions]

**Application Use Cases**: [Use case names, commands, queries, results, input
ports, output ports, orchestration, transaction boundaries]

**Inbound REST Adapter**: [REST resources, request DTOs, response DTOs,
transport validation, error mapping]

**Outbound Adapters**: [Persistence, SRI, storage, queue, webhook, clock,
transaction, and other port implementations]

**DTO Mapping Flow**: [REST DTO -> command, result -> REST DTO, domain ->
persistence entity, domain -> SRI DTO, SRI response DTO -> application result]

## Naming and Migration Classification

**Canonical Terms Used**: [List target English names introduced or reused]

**Legacy Terms Mapped**:

| Legacy Term | Target Term | Classification | Notes |
|-------------|-------------|----------------|-------|
| [legacy] | [target] | [classification] | [notes] |

**Pending Naming Decisions**: [List or "None"]

**Pending Functional Validations**: [List or "None"]

## Idempotency, Audit, and Error Handling

**Idempotency Rules**: [Issuance, retries, synchronization, webhook delivery,
XML generation, sequence assignment, or N/A]

**Audit Events**: [Events emitted or recorded, with sensitive data exclusions]

**Error Mapping**: [Domain/application exceptions to REST errors; adapter errors
to application results]

## Complexity Tracking

> Fill only if Constitution Check has violations or compatibility exceptions
> that must be justified.

| Violation or Exception | Why Needed | Scope and Expiration | Safer Alternative Rejected Because |
|------------------------|------------|----------------------|------------------------------------|
| [example] | [reason] | [scope] | [reason] |
