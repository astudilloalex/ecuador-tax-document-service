# Implementation Plan: Create Invoice Draft

**Branch**: `6-ft-1` | **Date**: 2026-07-12 | **Spec**: `specs/001-create-invoice-draft/spec.md`

## Summary

Provide one synchronous internal API operation that accepts a mandatory opaque Company UUID in
`X-Company-Id`, validates commercial and local versioned catalog inputs, performs deterministic USD
calculations, and atomically persists and returns one complete invoice draft. Company UUID plus
idempotency-key hash arbitrates duplicate and concurrent commands. The feature performs no
authentication, authorization, Company lookup, fiscal-context resolution, snapshot creation, or
SRI side effect.

## Technical Context

**Language/Version**: Java 25
**Framework**: Quarkus 3.33.2.1 LTS, selected in `research.md` for supported Java 25 and LTS evidence
**Reactive Model**: Mutiny for the HTTP, application, and persistence flows
**Persistence**: Hibernate Reactive with Panache; infrastructure-only persistence models
**Database**: PostgreSQL 18.4
**Schema Migration**: Flyway only
**Caller Security**: None in this repository; no authentication, authorization, tokens, principals,
roles, permissions, or service authentication
**Testing**: JUnit 5, REST Assured, Quarkus `UniAsserter`, real PostgreSQL integration tests,
empty-database Flyway tests, API contract tests, domain vectors, concurrency tests, JVM tests, and
native smoke evidence only when native compatibility is claimed
**Target Execution**: JVM execution is mandatory
**Native Compatibility**: Candidate capability; actual build and runtime evidence is required
before it may be claimed
**External Integrations**: None for Create Invoice Draft
**Company Context Boundary**: `X-Company-Id` is mapped to an immutable normalized application
Company UUID. No Company port, client, lookup, status/eligibility check, readiness dependency,
master data, cache, replication, shared persistence, or draft-time fiscal snapshot exists.
**Performance Goals**: At the approved upper bounds, a valid request is processed with one bounded
reactive database transaction; concurrency evidence includes at least 50 equivalent requests for
one Company-and-key scope and proves one draft.
**Constraints**: 500 lines, 10 positive payments or one zero payment, 15 additional-information
entries, exact approved decimal precision, current Ecuadorian emission date, request/body limits
defined before implementation, and no blocking event-loop work
**Scale/Scope**: One Company ownership partition and one draft per logical command; no other tax
document type or fiscal-issuance lifecycle
**Sensitive Data**: Buyer identity/contact and fiscal payload content are redacted from logs,
errors, metrics, and traces. Raw idempotency keys are not required in persistence.

## Source and Terminology Evidence

| Authority | Applicable source and version/path | Requirements or decisions governed |
|-----------|------------------------------------|-------------------------------------|
| Official SRI documentation | SRI Electronic Tax Documents Offline Scheme Technical Sheet v2.32 and applicable versioned catalogs | Buyer types, IVA treatments, final-consumer threshold, fiscal vocabulary |
| Constitution | `.specify/memory/constitution.md` v2.0.0 | Definitive Company header/no-auth/no-Company-dependency boundary and all project gates |
| Specification | `specs/001-create-invoice-draft/spec.md`, clarification session 2026-07-12 | Functional behavior and approved boundaries |
| Architecture decisions | None | No ADR is needed for the approved constitutional boundary |
| Legacy evidence | `docs/legacy/as-is/` references listed in the specification | Discovery only; no target compatibility |

**Source Conflicts and Resolutions**: Earlier feature artifacts required Keycloak, tenant-derived
Company context, a Company-context adapter, and fiscal snapshots. Constitution v2.0.0 has higher
authority and removes those requirements from draft creation. The specification and all planning
artifacts are reconciled accordingly.

**Pending Functional Validation**: None.

**Terminology Mapping Impact**: Company Identifier is the normalized opaque UUID from
`X-Company-Id`; Fiscal Context Snapshot is reserved for later fiscal issuance.

## Constitution Check

| Gate | Pre-Research evidence | Post-Design evidence |
|------|-----------------------|----------------------|
| Greenfield bounded outcome | PASS — one invoice-draft creation outcome | PASS — no legacy contract or unrelated document type |
| Authority and language | PASS — SRI v2.32 and English target terms identified | PASS — conflicts and terminology changes recorded |
| Technology baseline | PASS — Java 25, Quarkus, Mutiny, PostgreSQL, reactive Panache, Flyway | PASS — supported versions justified in research |
| Clean Architecture and domain purity | PASS — four required boundaries | PASS — explicit DTO/application/domain/persistence mappings; HTTP Company header remains in API |
| Reactive safety | PASS — no external or blocking adapters | PASS — reactive PostgreSQL only; no synchronous waits |
| Fiscal and monetary correctness | PASS — approved IVA, buyer, date, decimal, and zero-total rules | PASS — exact calculation pipeline and catalog evidence defined |
| Internal caller boundary | PASS — identity and entitlement explicitly out of scope | PASS — no security dependencies or OpenAPI security constructs |
| Company boundary | PASS — mandatory opaque header; no Company lookup or snapshots | PASS — data model stores only immutable Company UUID and opaque emission-point ID |
| Sensitive data | PASS — sensitive buyer/fiscal inputs identified | PASS — safe errors and observability tests planned |
| Persistence and consistency | PASS — Flyway and all-or-nothing outcome required | PASS — Company-plus-key-hash uniqueness and aggregate constraints defined |
| API quality | PASS — stable English errors/correlation/idempotency | PASS — OpenAPI contract contains required header and no Company parent resource |
| External adapters | PASS — none applicable | PASS — no Company or security adapter introduced |
| Testing and operations | PASS — acceptance/risk tests required | PASS — JVM, PostgreSQL, migration, header, calculation, concurrency, and health evidence listed |
| Simplicity | PASS — no extra service or abstraction | PASS — only locally required ports and persistence are planned |

No constitutional deviation is requested.

## Reactive and Resource Boundary Design

| Operation | Boundary | Classification | Execution context | Bound | Evidence |
|-----------|----------|----------------|-------------------|-------|----------|
| HTTP request mapping and validation | API adapter | Non-blocking, bounded CPU | Event loop | Configured payload and collection limits | Maximum-size and malformed-input tests |
| Decimal calculation and domain validation | Domain/application | Synchronous deterministic bounded CPU | Calling context | At most 500 lines | Calculation vectors and concurrent upper-bound tests |
| Draft/binding lookup and persistence | Repository port and reactive Panache adapter | Non-blocking database I/O | Reactive PostgreSQL client | Database/query/transaction timeout | Rollback, timeout, connection-loss, and concurrency tests |

No filesystem, SOAP, certificate, cryptographic, XML, or other blocking operation exists in this
feature. Reactive wrappers may not hide blocking work.

## Company Context and Sensitive-Data Design

**Internal Caller Boundary**: The internal operation is neither protected nor public in an
application-security sense because this repository implements no caller security. The OpenAPI
contract has no security schemes, requirements, Authorization header, `401`, or `403`.

**Company Header Contract**: The API accepts exactly one `X-Company-Id` value and validates only
presence, single cardinality, UUID syntax, and non-nil value. Missing/blank maps to
`COMPANY_CONTEXT_REQUIRED`; repeated/malformed/nil maps to `COMPANY_CONTEXT_INVALID`. Accepted
values are normalized to lowercase hyphenated UUID form. Company identifier never appears in
path, query, body, token, or session.

**Company Ownership Scoping**: The application `CompanyId` is immutable on the draft and scopes
every draft query, mutation, and idempotency lookup. Children belong through the local aggregate.
This is business partitioning, not caller authorization or security isolation.

**Company Master-Data Boundary**: No Company aggregate, table, repository, port, client, adapter,
status/eligibility validation, database access, foreign key, transaction, cache, replication,
health check, timeout, failure, or snapshot exists. `emissionPointId` is opaque draft input.

**Sensitive Data**: Problem responses contain stable codes and safe field paths without rejected
values. Buyer identity/contact, raw idempotency keys, payloads, and Company identifiers are not
metric labels and are redacted from unsafe observability fields.

## Data and Consistency Design

| Command/boundary | States | Retry/idempotency | Duplicate handling | Timeout | Recovery | Terminal outcome |
|------------------|--------|-------------------|--------------------|---------|----------|------------------|
| Create or replay invoice draft | No durable state before transaction; committed complete draft+binding or rollback | Scope is normalized Company UUID + key hash; fingerprint excludes Company and transport headers | Equivalent returns original; different content conflicts; concurrent equivalent produces one draft | Bounded database transaction; no Company timeout exists | Caller retries the same command/key after response loss; committed binding remains authoritative | `201` new draft, `200` replay, stable `400`/`409`/`422`/`500` errors |

The service never claims atomicity with an external system because no external operation is in
scope. PostgreSQL constraints enforce aggregate ownership and idempotency uniqueness.

## API and Error Contract

- Operation: `POST /api/v1/invoice-drafts`; the version prefix is global and Company is not a path
  resource.
- Required headers: `X-Company-Id`, `Idempotency-Key`; optional/generatable correlation header.
- Request body: buyer, opaque emission-point identifier, emission date, ordered lines and tax-rule
  selections, payments, optional additional information. Issuer/fiscal/snapshot/calculated fields
  are rejected as unknown inputs.
- Stable Company header codes: `COMPANY_CONTEXT_REQUIRED`, `COMPANY_CONTEXT_INVALID`.
- The feature defines no Company-not-found/inactive/not-authorized/unavailable/timeout/mismatch,
  authentication, or authorization error.
- Response returns the Company UUID, opaque emission-point ID, complete captured commercial data,
  calculated amounts, `DRAFT`, and timestamps; it returns no fiscal master-data snapshot.

## Native Compatibility Evaluation

| Risk area | Applicable? | Planned evidence | Decision |
|-----------|-------------|------------------|----------|
| SOAP, XML/XSD, signatures, PKCS#12 | No | Excluded by specification | Not applicable to this feature |
| Cryptographic providers | No feature-specific use | JVM and optional native smoke | No custom provider |
| Reflection/resources | Yes — DTOs, Panache, OpenAPI, Flyway | Native build plus create/replay/conflict/health runtime smoke if claimed | JVM remains acceptable if native evidence fails |

## Project Structure

```text
src/main/java/com/alexastudillo/taxdocument/
├── api/invoicedraft/
├── application/invoicedraft/
├── domain/invoicedraft/
└── infrastructure/invoicedraft/

src/main/resources/db/migration/
src/test/java/com/alexastudillo/taxdocument/{api,application,domain,infrastructure}/invoicedraft/
```

No `company` client or adapter package and no security package is introduced. Interfaces are
limited to actual local application boundaries: persistence, approved reference catalogs, clock,
and identifier generation.

## Test and Operational Evidence Plan

| Requirement/risk | Level/environment | Observable evidence |
|------------------|-------------------|---------------------|
| FR-001–FR-005, FR-039–FR-040 | API contract/integration | Exact header validation, canonical UUID mapping, owned path, rejected snapshot/Issuer fields, zero security constructs |
| FR-006–FR-016, DR-001–DR-016 | Domain/application | Official identification vectors, IVA treatments, exact rounding, zero-value rules, text and collection bounds |
| FR-020–FR-024, FR-036–FR-038 | PostgreSQL/architecture | Atomic aggregate, Company-scoped queries, child ownership, no Company dependency/table/snapshot |
| FR-027–FR-034 | PostgreSQL concurrency/API | Company+key-hash uniqueness, one winner, replay, conflict, scope independence, response-loss recovery |
| Flyway | Empty PostgreSQL | Complete repeatable migration and constraints from empty database |
| Sensitive data | API/observability | No buyer/raw key/payload leakage in errors, logs, metrics, or traces |
| Runtime | Packaged JVM | Create, replay, conflict, rollback, health |
| Native claim | Native process if claimed | Same critical smoke behavior against PostgreSQL |

**Liveness and Readiness**: Liveness reports process viability. Readiness checks only dependencies
needed to accept traffic, which for this feature is the configured PostgreSQL destination. No
Company or identity-provider check exists.

**Structured Observability**: Structured logs and traces carry correlation context and safe
operation/status fields. High-cardinality or sensitive values are not metric labels.

**Audit Events**: Draft creation and idempotency conflict/replay are operationally observable;
there are no certificate or authentication audit events.

**External Destination Consistency**: PostgreSQL readiness uses the same configured datasource as
business persistence. There is no Company or OIDC destination.

## Complexity Tracking

| Addition | Requirement | Simpler alternative | Why insufficient | Consequence |
|----------|-------------|---------------------|------------------|-------------|
| Durable idempotency binding with key hash and normalized content | FR-027–FR-034 | In-memory or key-only deduplication | Cannot survive restart, prove semantic equivalence, or arbitrate concurrency | Migration and PostgreSQL concurrency evidence required |
| Versioned local fiscal reference catalogs | DR-001 | Hard-coded codes/rates | Prohibited and cannot represent effective dating | Flyway reference-data evolution and catalog tests required |

No external service, background process, cache, broker, additional datastore, or Company
abstraction is justified or planned.
