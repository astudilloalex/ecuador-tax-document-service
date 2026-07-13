# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]

**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled by `$speckit-plan`. Every placeholder and every
`NEEDS CLARIFICATION` entry MUST be resolved before Phase 1 design is approved.

## Summary

[Summarize the bounded stakeholder outcome and the technical approach selected through research.]

## Technical Context

**Language/Version**: Java 25

**Framework**: Quarkus [exact version and selection rationale]

**Reactive Model**: Mutiny for asynchronous application and adapter flows

**Persistence**: Hibernate Reactive with Panache; Panache models remain in `infrastructure`

**Database**: PostgreSQL [supported version and environment assumptions]

**Schema Migration**: Flyway only

**Authentication**: Keycloak through OIDC [realm/client assumptions and validated claims]

**Testing**: [test libraries plus applicable domain, use-case, authorization, persistence,
migration, API, SRI, XML, signature, monetary, resilience, security, JVM, and native evidence]

**Target Execution**: JVM execution MUST be supported on [target platform]

**Native Compatibility**: [claimed, deferred, unsupported, or not applicable; reference the
evidence table below]

**External Integrations**: [SRI SOAP/XML, Keycloak, certificate, storage, rendering,
notification, or other adapters in scope]

**Company Context Ownership**: [Company bounded-context source, external Company identifier,
document-owned immutable fiscal snapshot, and explicit absence of local Company master data,
shared persistence, cache, or replication]

**Performance Goals**: [measurable latency, throughput, concurrency, and resource targets]

**Constraints**: [timeouts, payload limits, executor bounds, retention, availability, and other
measurable limits]

**Scale/Scope**: [tenant, issuer, document, request, and data-volume assumptions]

**Sensitive Data**: [sensitive values in scope and their approved storage, redaction, retention,
and deletion controls]

## Source and Terminology Evidence

Record exact versions, dates, or repository paths. A missing legacy observation MUST NOT be used
to remove target behavior, and unresolved or contradictory behavior MUST remain Pending Functional
Validation.

| Authority | Applicable source and version/path | Requirements or decisions governed |
|-----------|------------------------------------|-------------------------------------|
| Ecuadorian legislation and official SRI documentation | [citation and version, or justified non-applicability] | [affected rules] |
| Constitution | `.specify/memory/constitution.md` v[version] | [affected gates] |
| Approved specification and clarifications | [spec sections and clarification session] | [affected behavior] |
| Architecture decisions | [ADR paths or None] | [affected choices] |
| Legacy evidence | [paths under `docs/legacy/` or legacy source references] | [discovery evidence only] |

**Source Conflicts and Resolutions**: [List each conflict, governing higher-authority source, and
recorded resolution, or state None.]

**Pending Functional Validation**: [List unresolved matters and the validation owner/condition, or
state None.]

**Terminology Mapping Impact**: [Entries to add or update in
`docs/migration/terminology-mapping.md`, or state None.]

## Constitution Check

*GATE: Every row MUST pass before Phase 0 research begins. Re-evaluate every row after Phase 1
design. A deviation requires recorded approval under the constitution before implementation.*

| Gate | Pre-Research evidence | Post-Design evidence |
|------|-----------------------|----------------------|
| Greenfield scope: one bounded outcome; no implicit legacy compatibility | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Authority: official sources are versioned; conflicts and Pending Functional Validation are recorded | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Language: target names are English and terminology mapping decisions are respected | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Baseline: required technologies are fixed; Quarkus version research is identified pre-research and resolved with justification post-design | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Architecture: `api`, `application`, `domain`, and `infrastructure` dependencies and mappings comply | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Domain purity: no framework, transport, persistence, JSON, OIDC, or Mutiny types in `domain` | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Reactive safety: every blocking or CPU-intensive operation is isolated, bounded, timed out, and testable | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Fiscal correctness: official rules, `BigDecimal` policies, time semantics, and invalid-data behavior are explicit | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Security: deny-by-default authorization, token validation, ownership enforcement, and cross-tenant tests are defined | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Company boundary: Company master data remains externally owned; documents store only the external Company ownership identifier and immutable fiscal snapshots; no shared persistence, cache, or replication exists | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Sensitive data: storage, encryption, redaction, retention, and certificate lifecycle are defined before implementation | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Persistence: Flyway-only evolution, immutable migrations, database invariants, and empty-database tests are defined | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Boundary consistency: states, retries, idempotency, duplicates, timeouts, recovery, reconciliation, and terminal outcomes are defined | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| API and async quality: DTO separation, validation ownership, stable errors, correlation, and result observation are defined | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| External adapters: ports, endpoint configuration, sanitized observability, resilience, and contract evidence are defined | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Testing: acceptance scenarios and applicable risk-based tests are identified before production tasks | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Operations: meaningful liveness/readiness, structured logs, auditing, and destination-consistent health checks are defined | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Simplicity: every dependency, abstraction, process, store, and distributed interaction is justified | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |
| Runtime evidence: JVM verification is mandatory and native status has an evidence path | [PASS/FAIL and evidence] | [PASS/FAIL and evidence] |

## Reactive and Resource Boundary Design

List every blocking or CPU-intensive operation. If none exist, state why each external operation is
non-blocking.

| Operation | Infrastructure adapter and application port | Blocking/CPU classification | Execution context | Timeout and resource bound | Required concurrency/failure evidence |
|-----------|---------------------------------------------|-----------------------------|-------------------|----------------------------|---------------------------------------|
| [operation] | [adapter and port] | [blocking or CPU-intensive] | [worker or bounded executor] | [limits] | [tests/evidence] |

Reactive wrappers MUST NOT be accepted as evidence that an underlying operation is non-blocking.

## Security and Ownership Design

**Protected and Public Operations**: [List protected operations and explicitly justified public
operations.]

**Token Validation**: [Signature, issuer, audience, expiration, authorized party, and scope/role
rules that apply.]

**Effective Authorization Scope**: [How tenant, company, issuer, emission point, certificate, and
document ownership are derived and applied to every query and mutation.]

**Company Master-Data Boundary**: [How current Company/Issuer/establishment/emission-point data is
resolved through an application port; how only the external Company identifier is used as the
document ownership reference; which immutable fiscal snapshot fields the document owns; and how
shared databases, cross-service foreign keys/repositories/transactions, Company caches, and
background replication are excluded.]

**Cross-Tenant Evidence**: [Required denial scenarios and test locations.]

**Sensitive Data and Certificate Lifecycle**: [Storage, encryption, rotation, expiration,
revocation, deletion, backup, tenant ownership, redaction, and fail-closed behavior applicable to
this feature.]

## Data and External Consistency Design

For every database/external-system boundary, complete one row. The plan MUST NOT claim a database
transaction is atomic with an external operation.

| Boundary or logical command | Intermediate states | Retry and idempotency | Duplicate handling | Timeout | Failure recovery and reconciliation | Observable terminal outcomes |
|-----------------------------|---------------------|-----------------------|--------------------|---------|-------------------------------------|------------------------------|
| [boundary/command] | [states] | [rules and stable key] | [rules] | [limit/outcome] | [procedure] | [states visible to authorized caller/operator] |

## Native Compatibility Evaluation

Each applicable row MUST contain actual build and runtime evidence before the feature is complete.
Use `Not applicable` only with a specific reason. JVM execution remains mandatory.

| Risk area | Applicable? | Build evidence | Runtime evidence | Decision and consequences |
|-----------|-------------|----------------|------------------|---------------------------|
| SOAP clients | [yes/no] | [command/result or planned evidence] | [scenario/result or planned evidence] | [support/defer/reject] |
| XML generation and schema validation | [yes/no] | [evidence] | [evidence] | [decision] |
| XML digital signatures | [yes/no] | [evidence] | [evidence] | [decision] |
| PKCS#12 certificate handling | [yes/no] | [evidence] | [evidence] | [decision] |
| Cryptographic providers | [yes/no] | [evidence] | [evidence] | [decision] |
| Reflection and resource loading | [yes/no] | [evidence] | [evidence] | [decision] |

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
└── tasks.md
```

### Source Code (repository root)

Replace capability placeholders with concrete English names and remove unused paths.

```text
src/main/java/com/alexastudillo/taxdocument/
├── api/<capability>/
├── application/<capability>/
├── domain/<capability>/
└── infrastructure/<capability>/

src/main/resources/
├── application.properties
└── db/migration/

src/test/java/com/alexastudillo/taxdocument/
├── api/<capability>/
├── application/<capability>/
├── domain/<capability>/
└── infrastructure/<capability>/
```

**Structure Decision**: [Document capability grouping, explicit cross-layer mappings, composition
location, and any justified exception.]

## Test and Operational Evidence Plan

Create one row for each applicable constitutional test category and each acceptance scenario.
Tests MUST target observable behavior or invariants, not coverage percentage.

| Requirement/risk | Test level and environment | Planned path | Observable behavior or invariant | Failure/boundary cases |
|------------------|----------------------------|--------------|----------------------------------|------------------------|
| [FR/SC/risk] | [domain/use-case/PostgreSQL/API/adapter/runtime] | [exact path] | [assertion] | [cases] |

**Liveness and Readiness**: [Distinct semantics and dependencies required to accept traffic.]

**Structured Observability**: [Correlation propagation, sanitized logs, metrics, and traces.]

**Audit Events**: [Operational state transitions and certificate operations requiring audit.]

**External Destination Consistency**: [How health checks and business adapters share the same
configured destinations.]

## Complexity Tracking

Every new dependency, abstraction, background process, persistent store, and distributed
interaction MUST appear below. If there are none, state `None` and explain how the feature uses
the existing baseline directly.

| Addition | Requirement creating the need | Simpler alternatives considered | Why insufficient | Testing and operational consequences |
|----------|-------------------------------|--------------------------------|------------------|--------------------------------------|
| [addition] | [requirement ID] | [alternatives] | [specific reason] | [consequences] |

Constitution deviations require approval before implementation and MUST include an expiration or
objective remediation condition.

| Deviated principle and rule | Scope | Justification | Approval record | Expiration or remediation condition |
|-----------------------------|-------|---------------|-----------------|-------------------------------------|
| [principle/rule] | [scope] | [reason] | [record] | [condition/date] |
