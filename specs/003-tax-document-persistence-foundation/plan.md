# Implementation Plan: Tax Document Persistence Foundation

**Branch**: `5-ft-3` | **Feature Directory**: `003-tax-document-persistence-foundation` | **Date**: 2026-07-05 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/003-tax-document-persistence-foundation/spec.md`

## Summary

Implement the outbound persistence foundation for the common tax document
issuance model. The implementation will add versioned PostgreSQL schema
migrations, persistence adapter entities and mappers, implementations of
`TaxDocumentRepository`, `SequenceNumberPort`, and `TransactionPort`, and a
domain-safe `TaxDocument` rehydration path. The feature stays outbound
persistence only: no REST, SRI, XML generation, storage, queue, webhook,
bootstrap behavior, or document-specific issuance flows.

## Technical Context

**Language/Version**: Java 25

**Primary Dependencies**: Quarkus, Gradle with Kotlin DSL, Quarkus Hibernate
ORM with plain JPA entities, Quarkus JDBC PostgreSQL, Quarkus Flyway, Quarkus
Arc, JUnit, Quarkus JUnit, Testcontainers for PostgreSQL-backed adapter tests.
Panache active-record style is not used for domain objects and is not needed
for this foundation.

**Storage**: PostgreSQL target schema managed by Flyway migrations under
`src/main/resources/db/migration/`.

**Testing**: Domain/application tests remain plain JUnit without Quarkus.
Persistence adapter tests may use Quarkus test support and Testcontainers. Test
coverage must include mapping, rehydration, uniqueness, sequence reservation,
transaction behavior, and architecture boundary checks.

**Target Platform**: Backend service

**Project Type**: Quarkus backend using Clean Architecture and Ports and
Adapters

**Base Package**: `com.alexastudillo.taxdocument`

**Performance Goals**: Repository lookups by `accessKey` and issuance identity
must be indexed. Sequence reservation must rely on database uniqueness and
transactional behavior instead of application-only duplicate checks.

**Constraints**: Persistence-specific code is limited to
`adapter.out.persistence` plus approved configuration and Flyway locations.
Domain and application layers must remain free of JPA, Hibernate, Panache,
PostgreSQL, Flyway, JDBC, SQL, Quarkus persistence APIs, and persistence
annotations. Target database names are English lowercase snake_case.

**Scale/Scope**: Initial common issuance tables only:
`issuers`, `establishments`, `issuing_points`, `issuance_sequences`, and
`tax_documents`. `tax_document_audit_events`, XML path columns, compatibility
views, and production data migration are deferred.

**Temporal Rules**: `issue_date` persists the domain `IssueDate` as a database
`date` without timezone conversion. `authorized_at` persists the domain
`AuthorizedAt` as a UTC-normalized database timestamp. Rehydration validation
compares `authorized_at` at microsecond precision or the selected database
precision, whichever is lower.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Clean Architecture**: PASS. Persistence adapters depend inward on
  application ports and domain models. Domain/application do not depend on
  persistence.
- **Use Cases**: PASS. This feature implements output ports for future use
  cases and does not create generic business services or document-specific use
  cases.
- **Ports and Adapters**: PASS. PostgreSQL, Flyway, transactions, and sequence
  reservation are accessed through existing application ports.
- **SRI Isolation**: PASS. SRI XML/SOAP, signing, reception, authorization,
  response parsing, and official Spanish names remain out of scope.
- **English Terminology**: PASS. Target tables, columns, packages, classes, and
  docs use canonical English names.
- **DTO Separation**: PASS. Persistence entities stay in
  `adapter.out.persistence`; repository methods expose only domain/application
  models.
- **Validation Separation**: PASS. Database constraints enforce persistence
  integrity; domain invariants remain in domain; application-facing errors
  translate persistence failures without leaking SQL/framework types.
- **Idempotency and Auditability**: PASS. Duplicate saves reject with duplicate
  conflict errors. Exact repeated sequence reservations are idempotent. Audit
  persistence is deferred; sensitive diagnostics are forbidden.
- **Testing Boundary**: PASS. Domain/application tests stay infrastructure-free;
  persistence adapter tests may use Quarkus and Testcontainers.
- **Migration Classification**: PASS. All introduced persistence concepts are
  target database objects or deferred PFVs.
- **No Legacy Copying**: PASS. The schema is a target English model, not a
  one-to-one copy of legacy tables or names.

## Project Structure

### Documentation (this feature)

```text
specs/003-tax-document-persistence-foundation/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── persistence-port-contract.md
│   ├── persistence-schema-contract.md
│   ├── persistence-error-contract.md
│   └── migration-documentation-contract.md
└── tasks.md
```

### Source Code (repository root)

```text
src/main/java/com/alexastudillo/taxdocument/
├── domain/
│   └── taxdocument/
│       └── TaxDocument.java              # add restore path only
├── application/
│   └── port/out/
│       └── existing ports                # signatures preserved unless task proves a minimal error type is needed
└── adapter/
    └── out/
        └── persistence/
            ├── entity/
            ├── mapper/
            ├── repository/
            └── transaction/

src/main/resources/
├── application.properties                # persistence config only if required
└── db/migration/
    └── V1__create_tax_document_persistence_foundation.sql

src/test/java/com/alexastudillo/taxdocument/
├── domain/
│   └── taxdocument/                      # restore invariant tests
└── adapter/
    └── out/
        └── persistence/                  # adapter integration tests
```

**Structure Decision**: The only domain change is the required
`TaxDocument` rehydration mechanism from `FR-006` and `FR-007`. It must remain
framework-free and must not import persistence entities. All persistence
entities, mappers, query details, duplicate translation, and transaction
integration belong to `adapter.out.persistence`. No inbound REST, outbound SRI,
outbound storage, outbound queue, outbound webhook, or bootstrap package is
created by this feature.

## Layer and Boundary Design

**Domain Concepts**: Reuse `TaxDocument`, `DocumentType`, `DocumentState`,
`AuthorizationState`, `AccessKey`, `AuthorizationNumber`, `AuthorizedAt`,
`IssueDate`, `Issuer`, `Establishment`, `IssuingPoint`, `SequenceNumber`, and
`IssuanceMode`. Add a domain-safe restore path that validates persisted
authorization combinations and preserves existing invariants.

**Application Use Cases**: None created. Existing output ports are implemented:
`TaxDocumentRepository`, `SequenceNumberPort`, and `TransactionPort`. Any
application-facing error type introduced for duplicate conflict or data
integrity must live in the application layer and must not expose SQL,
Hibernate, JPA, Flyway, or PostgreSQL types.

**Inbound REST Adapter**: Not applicable. No REST resources, request DTOs,
response DTOs, transport validation, or HTTP error mapping are created.

**Outbound Adapters**: Create only `adapter.out.persistence`. It owns JPA
entities, persistence repositories/helpers, mappers, transaction adapter,
database error translation, and PostgreSQL-backed implementations of the
specified application ports.

**DTO Mapping Flow**: This feature implements only:

```text
Domain Object -> Persistence Entity
Persistence Entity -> Domain Object
Database/Persistence Error -> Application-facing error
```

REST DTO, SRI XML DTO, queue model, storage model, and webhook payload mapping
are out of scope.

## Naming and Migration Classification

**Canonical Terms Used**: `taxDocument`, `issuer`, `establishment`,
`issuingPoint`, `issuanceSequence`, `accessKey`, `documentType`,
`documentState`, `authorizationState`, `authorizationNumber`, `authorizedAt`,
`issueDate`, `issuanceMode`, `externalRequestId`, `sequenceNumber`.

**Legacy Terms Mapped**:

| Legacy Term | Target Term | Classification | Notes |
|-------------|-------------|----------------|-------|
| emisor | `issuers`, `issuer_id`, `legal_identifier`, `legal_name`, `trade_name` | Target database object | Target names only; exact legacy source columns remain mapping evidence. |
| establecimiento | `establishments`, `establishment_id`, `establishment_code` | Target database object | English target schema, not legacy table copy. |
| punto emision | `issuing_points`, `issuing_point_id`, `issuing_point_code` | Target database object | English target schema. |
| secuencial | `issuance_sequences`, `sequence_number` | Target database object | Requested-value reservation; automatic increment deferred. |
| comprobante | `tax_documents` | Target database object | Common tax document lifecycle persistence. |
| clave acceso | `access_key` | Target database object | Unique. |
| numero autorizacion | `authorization_number` | Target database object | Optional and consistent with authorized state only. |
| fecha emision | `issue_date` | Target database object | Required. |
| fecha autorizacion | `authorized_at` | Target database object | Optional and consistent with authorization number/state. |

**Pending Naming Decisions**: None.

**Pending Functional Validations**:

| ID | Status in Plan |
|----|----------------|
| PFV-PER-001 | Requested-value reservation is in scope and exact repeated reservation is idempotent. Automatic increment behavior is deferred to future issuance use cases. |
| PFV-PER-002 | Legacy compatibility views are deferred to a migration or compatibility specification. |
| PFV-PER-003 | Historical XML paths are deferred to an XML storage specification. |
| PFV-PER-004 | Audit persistence is deferred; `tax_document_audit_events` is not included in this foundation. |
| PFV-PER-005 | Auto-numbering policy is deferred to a future numbering policy or document-specific issuance specification. |

## Idempotency, Audit, and Error Handling

**Idempotency Rules**:

- `TaxDocumentRepository.save` may create a new persisted document when no
  matching record exists.
- `TaxDocumentRepository.save` may update the same persisted aggregate when the
  same tax document identity already exists.
- `TaxDocumentRepository.save` must never silently overwrite a different tax
  document with duplicate `accessKey` or duplicate issuance identity.
- `TaxDocumentRepository.save` rejects duplicate `accessKey` with an
  application-facing duplicate conflict error.
- `TaxDocumentRepository.save` rejects duplicate issuance identity with an
  application-facing duplicate conflict error.
- Missing `findByAccessKey` and `findByIssuanceIdentity` operations return
  empty results; missing `existsByAccessKey` and `existsByIssuanceIdentity`
  operations return `false`.
- `SequenceNumberPort.reserve` returns the existing domain `SequenceNumber`
  when the exact reservation identity already exists.
- `SequenceNumberPort.isAvailable` returns false for already reserved
  sequences that would conflict.
- Conflicting duplicate sequence reservations fail with an
  application-facing sequence reservation conflict error.

**Audit Events**: No audit event persistence table is created. Persistence
diagnostics must not include secrets, credentials, tokens, signing passwords,
private keys, database passwords, or sensitive configuration values.

**Error Mapping**:

- Database uniqueness violations map to application-facing duplicate conflict
  errors.
- Duplicate or unavailable sequence reservations map to application-facing
  sequence reservation conflict errors.
- Invalid persisted authorization combinations map to application-facing data
  integrity errors during rehydration.
- Unknown canonical document type, document state, authorization state, or
  issuance mode values map to application-facing data integrity errors.
- Missing or inconsistent issuer, establishment, or issuing point relationships
  map to application-facing data integrity errors.
- Transaction failures map to application-facing persistence/transaction
  failures without exposing SQL, Hibernate, JPA, or PostgreSQL types beyond the
  adapter.
- Generic persistence failures map to application-facing persistence failure
  errors without exposing SQL, Hibernate, JPA, Panache, Flyway, or
  PostgreSQL-specific types.

## Schema Relationship and Constraint Design

The target schema must define primary keys, foreign keys, unique constraints,
important indexes, relationship rules, and delete/update restrictions for all
required tables.

| Table | Primary Key | Foreign Keys | Unique Constraints | Important Indexes | Delete/Update Restrictions |
|-------|-------------|--------------|--------------------|-------------------|----------------------------|
| `issuers` | `issuer_id` | None | `issuer_id` | `legal_identifier` lookup if required by adapter tests | Deleting or updating an issuer referenced by establishments, sequences, or tax documents is restricted. |
| `establishments` | `establishment_id` | `issuer_id -> issuers.issuer_id` | `(issuer_id, establishment_code)` | `issuer_id` | Deleting or updating an establishment referenced by issuing points, sequences, or tax documents is restricted. |
| `issuing_points` | `issuing_point_id` | `establishment_id -> establishments.establishment_id` | `(establishment_id, issuing_point_code)` | `establishment_id` | Deleting or updating an issuing point referenced by sequences or tax documents is restricted. |
| `issuance_sequences` | `issuance_sequence_id` | `issuer_id`, `establishment_id`, `issuing_point_id` references | `(issuer_id, establishment_id, issuing_point_id, document_type, sequence_number)` | unique reservation identity | Deleting or updating a referenced issuer, establishment, or issuing point is restricted. |
| `tax_documents` | `tax_document_id` | `issuer_id`, `establishment_id`, `issuing_point_id` references | `access_key`; `(issuer_id, document_type, establishment_id, issuing_point_id, sequence_number)` | `access_key`; issuance identity | Deleting or updating a referenced issuer, establishment, or issuing point is restricted. |

Cascade deletes are not part of this feature. Any future archival, purge, or
production data correction behavior requires a separate specification.

## Complexity Tracking

No constitution violations or compatibility exceptions are introduced.

| Violation or Exception | Why Needed | Scope and Expiration | Safer Alternative Rejected Because |
|------------------------|------------|----------------------|------------------------------------|
| None | N/A | N/A | N/A |

## Phase 0 Research Summary

Research decisions are captured in [research.md](./research.md). All technical
unknowns are resolved or explicitly deferred through PFV records before task
generation.

## Phase 1 Design Summary

Design artifacts generated:

- [data-model.md](./data-model.md)
- [contracts/persistence-port-contract.md](./contracts/persistence-port-contract.md)
- [contracts/persistence-schema-contract.md](./contracts/persistence-schema-contract.md)
- [contracts/persistence-error-contract.md](./contracts/persistence-error-contract.md)
- [contracts/migration-documentation-contract.md](./contracts/migration-documentation-contract.md)
- [quickstart.md](./quickstart.md)

Agent context update: no agent context update script is present under
`.specify/scripts/bash/`; no context update command was available to run.

## Post-Design Constitution Check

- **Clean Architecture**: PASS. Design artifacts keep persistence outward and
  preserve inward dependencies.
- **Use Cases**: PASS. No business use cases are added; future use cases will
  consume application ports.
- **Ports and Adapters**: PASS. Persistence, sequence, and transaction behavior
  are behind application ports.
- **SRI Isolation**: PASS. SRI codes are not persisted as internal document type
  values and no SRI adapter artifacts are introduced.
- **English Terminology**: PASS. Schema and source names use English canonical
  terminology.
- **DTO Separation**: PASS. Persistence entities are adapter-only models.
- **Validation Separation**: PASS. Database integrity, domain invariants, and
  application-facing errors have distinct responsibilities.
- **Idempotency and Auditability**: PASS. Persistence idempotency is explicit;
  audit persistence is deferred.
- **Testing Boundary**: PASS. Adapter tests may use Quarkus/Testcontainers;
  domain/application tests remain infrastructure-free.
- **Migration Classification**: PASS. Target database objects and deferred PFVs
  are documented.
- **No Legacy Copying**: PASS. No Spanish target schema or legacy compatibility
  view is introduced.
