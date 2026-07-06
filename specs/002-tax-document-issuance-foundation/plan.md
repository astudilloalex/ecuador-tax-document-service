# Implementation Plan: Tax Document Issuance Foundation

**Branch**: `002-tax-document-issuance-foundation` | **Date**: 2026-07-05 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-tax-document-issuance-foundation/spec.md`

## Summary

Define and implement the common domain and application foundation for Ecuador
electronic tax document issuance. The plan introduces only domain and
application source artifacts in future implementation tasks: common tax document
concepts, value objects, state models, application ports, issuance
request/result models, idempotency rules, audit event names, and testable
lifecycle policies.

The plan does not create REST endpoints, REST DTOs, persistence adapters, JPA or
Panache entities, database migrations, SRI XML/SOAP adapters, queue adapters,
webhook adapters, bootstrap wiring, or document-specific issuance flows.

## Technical Context

**Language/Version**: Java 25.

**Primary Dependencies**: Quarkus with Mutiny and Gradle with Kotlin DSL are
the project stack. Domain artifacts for this feature must use only Java and
project-local domain types; they must not depend on Quarkus APIs, Mutiny, or
adapter/runtime APIs. Application output ports may use Mutiny `Uni` as the
framework-free reactive contract required by the constitution, but must not
depend on adapter implementations or Quarkus-specific types.

**Storage**: N/A for this feature. Persistence is represented only by
application ports. No PostgreSQL, Redis, XML storage, filesystem, queue, or
webhook implementation is created.

**Testing**: JUnit-based domain and application tests. Tests for this feature
must not require Quarkus test support, PostgreSQL, Redis, filesystem access,
SRI services, queues, webhooks, or external HTTP services.

**Target Platform**: Backend service domain and application foundation.

**Project Type**: Quarkus backend using Clean Architecture and Ports and
Adapters.

**Base Package**: `com.alexastudillo.taxdocument`

**Performance Goals**: Domain and application foundation tests can run as unit
tests without infrastructure. Reviewers can validate all common document types,
state mappings, ports, idempotency keys, audit event names, and source-boundary
constraints from the generated artifacts before implementation.

**Constraints**: Source work is limited to `domain` and `application` packages
and their matching test packages. The feature must not introduce adapter,
bootstrap, REST, persistence, SRI, XML, queue, webhook, filesystem, or database
dependencies. Spanish legacy names are allowed only in migration mapping
documentation and AS-IS evidence references.

**Scale/Scope**: Foundation covers 5 document types, 9 document states, 7
authorization states, 11 application output ports, 10 canonical audit event
names, common idempotency keys, common state transitions, and 5 Pending
Functional Validations that are deferred or constrained before affected tasks.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Clean Architecture**: PASS. Domain and application responsibilities are
  explicit. Dependencies point inward, and no adapter or bootstrap source is in
  scope.
- **Use Cases**: PASS. Future business operations are explicit use cases. This
  feature defines common contracts and models only; it does not introduce
  generic business service classes.
- **Ports and Adapters**: PASS. External dependencies are represented by
  application output ports. Adapter implementations are explicitly excluded and
  deferred to future features.
- **SRI Isolation**: PASS. SRI numeric document codes are external contract
  mappings. XML, SOAP, signing, reception, authorization calls, and official
  SRI DTOs remain out of scope.
- **English Terminology**: PASS. Target names use canonical English
  terminology. Legacy Spanish names appear only in migration mapping tables and
  evidence references.
- **DTO Separation**: PASS. No REST, persistence, or SRI DTOs are created.
  Application request/result models remain separate from all adapter DTOs.
- **Validation Separation**: PASS. Domain validation covers invariants, access
  key structure, state transitions, and authorized-document immutability.
  Application validation covers issuer access, duplicate detection, sequence
  availability, retry eligibility, and idempotency orchestration.
- **Idempotency and Auditability**: PASS. Issuance, retry, synchronization,
  webhook delivery, XML generation, and sequence assignment idempotency scopes
  are defined. Audit event names exclude secrets and sensitive configuration.
- **Testing Boundary**: PASS. Domain and application tests are infrastructure
  free and do not require Quarkus.
- **Migration Classification**: PASS. Common concepts, SRI document codes, and
  legacy states are classified. Open behavioral questions are recorded as
  Pending Functional Validations.
- **No Legacy Copying**: PASS. The plan preserves validated behavior while
  rejecting legacy module structure, Spanish target names, database design, and
  technical coupling.

Post-design re-check: PASS. Phase 1 artifacts keep implementation limited to
domain/application foundations, define no runtime adapters, and preserve all
constitution gates.

## Project Structure

### Documentation (this feature)

```text
specs/002-tax-document-issuance-foundation/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   |-- application-port-contracts.md
|   |-- future-use-case-contract.md
|   |-- migration-mapping-update-contract.md
|   `-- source-boundary-contract.md
`-- tasks.md
```

### Durable Documentation To Be Updated By Tasks

```text
docs/
|-- architecture/
|   `-- canonical-terminology.md
`-- migration/
    `-- legacy-to-target-terminology.md
```

Tasks must update these durable documents with foundation terms, state
mappings, SRI document code mappings, and deferred Pending Functional
Validations where applicable. AS-IS evidence remains under `docs/as-is/` until
it is relocated to `docs/legacy/`.

### Source Code (repository root)

```text
src/main/java/com/alexastudillo/taxdocument/
|-- domain/
|   `-- taxdocument/
|       |-- TaxDocument
|       |-- DocumentType
|       |-- DocumentState
|       |-- AuthorizationState
|       |-- IssuanceMode
|       |-- value objects for access key, authorization number,
|       |   issue date, authorized timestamp, sequence number,
|       |   issuer, establishment, and issuing point
|       |-- lifecycle policy for allowed transitions
|       `-- business exceptions for invariant violations
`-- application/
    |-- issuance/
    |   |-- IssuanceRequest
    |   |-- IssuanceResult
    |   `-- retry eligibility policy or result model
    `-- port/
        `-- out/
            |-- TaxDocumentRepository
            |-- IssuerAccessPolicyPort
            |-- SequenceNumberPort
            |-- AccessKeyGeneratorPort
            |-- SriAuthorizationPort
            |-- XmlStoragePort
            |-- TaxDocumentQueuePort
            |-- WebhookPublisherPort
            |-- ClockPort
            |-- TransactionPort
            `-- AuditLogPort

src/test/java/com/alexastudillo/taxdocument/
|-- domain/
|   `-- taxdocument/
`-- application/
    |-- issuance/
    `-- port/
```

**Structure Decision**: Future implementation tasks for this feature may create
only domain and application source/test artifacts. Adapter and bootstrap
packages are omitted because REST, persistence, SRI, storage, queue, webhook,
and runtime wiring are future-feature responsibilities.

## Layer and Boundary Design

**Domain Concepts**: `TaxDocument`, `DocumentType`, `DocumentState`,
`AuthorizationState`, `Issuer`, `Establishment`, `IssuingPoint`,
`SequenceNumber`, `AccessKey`, `AuthorizationNumber`, `IssueDate`,
`AuthorizedAt`, `IssuanceMode`, lifecycle policy, business invariants, and
business exceptions.

**Application Use Cases**: No document-specific use case is implemented by this
feature. The feature defines common request/result models, application output
ports, retry eligibility contract, transaction boundary contract, and the rules
future use cases must follow.

**Inbound REST Adapter**: Out of scope. No REST resources, request DTOs,
response DTOs, path parameters, transport validation, or HTTP error mapping are
created.

**Outbound Adapters**: Out of scope. Persistence, SRI, storage, queue, webhook,
clock, transaction, and audit implementations are represented only by
application output ports.

**DTO Mapping Flow**: This feature creates no DTO mapping. Future adapter
features must preserve the constitution-required flow:

```text
REST DTO -> Application Command
Application Result -> REST Response DTO
Domain Object -> Persistence Entity
Domain Object -> SRI XML DTO
SRI Response DTO -> Application Result
```

## Naming and Migration Classification

**Canonical Terms Used**: taxDocument, invoice, creditNote, debitNote,
withholding, waybill, issuer, establishment, issuingPoint, sequenceNumber,
accessKey, authorizationNumber, issueDate, authorizedAt, issuanceRequest,
issuanceResult, issuanceMode, documentType, documentState, authorizationState.

**Legacy Terms Mapped**:

| Legacy Term | Target Term | Classification | Notes |
|-------------|-------------|----------------|-------|
| comprobante | taxDocument | Target domain concept | Existing approved baseline |
| factura | invoice / INVOICE | Target domain concept | Existing approved baseline |
| nota credito | creditNote / CREDIT_NOTE | Target domain concept | Existing approved baseline |
| nota debito | debitNote / DEBIT_NOTE | Target domain concept | Existing approved baseline |
| retencion | withholding / WITHHOLDING | Target domain concept | Existing approved baseline |
| guia remision | waybill / WAYBILL | Target domain concept | Existing approved baseline |
| emisor | issuer | Target domain concept | Existing approved baseline |
| establecimiento | establishment | Target domain concept | Existing approved baseline |
| punto emision | issuingPoint | Target domain concept | Existing approved baseline |
| secuencial | sequenceNumber | Target domain concept | Existing approved baseline |
| clave acceso | accessKey | Target domain concept | 49-digit value object |
| numero autorizacion | authorizationNumber | Target domain concept | Present only when authorized |
| fecha emision | issueDate | Target domain concept | Value object or domain field |
| fecha autorizacion | authorizedAt | Target domain concept | Present only when authorized |
| `01` | INVOICE | SRI adapter-only concept | External SRI document code |
| `04` | CREDIT_NOTE | SRI adapter-only concept | External SRI document code |
| `05` | DEBIT_NOTE | SRI adapter-only concept | External SRI document code |
| `06` | WAYBILL | SRI adapter-only concept | External SRI document code |
| `07` | WITHHOLDING | SRI adapter-only concept | External SRI document code |
| `PENDIENTE` | PENDING | Target domain concept | State mapping |
| `EN_PROCESO` | IN_PROGRESS | Target domain concept | State mapping |
| `RECIBIDO` | RECEIVED | Target domain concept | State mapping |
| `AUTORIZADO` | AUTHORIZED | Target domain concept | State mapping |
| `NO_AUTORIZADO` | NOT_AUTHORIZED | Target domain concept | State mapping |
| `DEVUELTA` | RETURNED | Target domain concept | State mapping |
| `RECHAZADO` | REJECTED | Target domain concept | State mapping |
| `IRRECUPERABLE` | IRRECOVERABLE | Target domain concept | State mapping |
| `ANULADO` | VOIDED | Target domain concept | Accepted as local voiding behavior |

**Pending Naming Decisions**: None for the required foundation concepts and
states. New unresolved terms discovered during tasks must be registered in
`docs/migration/legacy-to-target-terminology.md` before affected work proceeds.

**Pending Functional Validations**:

| ID | Handling in this plan |
|----|-----------------------|
| PFV-ISS-001 | Model `IssuanceMode`; defer runtime sync/async default to future issuance or queue feature. |
| PFV-ISS-002 | Defer legacy route compatibility to REST API or compatibility feature. |
| PFV-ISS-003 | Defer synchronization scheduling to synchronization feature. |
| PFV-ISS-004 | Define retry candidate states; require validation before implementing retry use case behavior. |
| PFV-ISS-005 | Treat authorized documents as immutable; require future spec for any exception. |

## Idempotency, Audit, and Error Handling

**Idempotency Rules**:

- Issuance foundation uses `accessKey`,
  `issuer + documentType + establishment + issuingPoint + sequenceNumber`, and
  `externalRequestId` when present.
- Sequence assignment is idempotent for the same issuer, establishment,
  issuing point, document type, and sequence number.
- XML generation is idempotent by tax document version and access key, but XML
  generation implementation is deferred.
- Retry, synchronization, and webhook delivery must define concrete correlation
  identifiers before affected implementation tasks.

**Audit Events**:

- `TaxDocumentIssuanceRequested`
- `TaxDocumentQueuedForIssuance`
- `TaxDocumentXmlGenerated`
- `TaxDocumentSigned`
- `TaxDocumentSubmittedToSri`
- `TaxDocumentReceivedBySri`
- `TaxDocumentAuthorized`
- `TaxDocumentRejected`
- `TaxDocumentAuthorizationRetryRequested`
- `TaxDocumentVoided`

Audit metadata must exclude secrets, credentials, private keys, tokens, signing
passwords, certificates, raw private key material, and sensitive configuration
values.

**Error Mapping**: Domain errors are business invariant failures and state
transition failures. Application errors are issuer access denial, duplicate
issuance identity, sequence unavailability, retry ineligibility, idempotency
conflict, and missing external dependency result. REST error mapping and
adapter failure translation are future adapter-feature responsibilities.

## Phase 0 Research Summary

Phase 0 decisions are captured in [research.md](./research.md). All planning
unknowns are resolved or explicitly deferred as Pending Functional Validation
outside affected task scope.

## Phase 1 Design Summary

Phase 1 artifacts:

- [data-model.md](./data-model.md)
- [contracts/application-port-contracts.md](./contracts/application-port-contracts.md)
- [contracts/future-use-case-contract.md](./contracts/future-use-case-contract.md)
- [contracts/migration-mapping-update-contract.md](./contracts/migration-mapping-update-contract.md)
- [contracts/source-boundary-contract.md](./contracts/source-boundary-contract.md)
- [quickstart.md](./quickstart.md)

Agent context update: no agent context update script is present in this Spec
Kit installation, so no generated agent context update was run.

## Traceability and Source of Truth

Future task lists must use `T###` identifiers and cite governing `FR-###`,
`AR-###`, `NR-###`, `SC-###`, or contract sections.

The durable source of truth after this feature is implemented is:

- `docs/architecture/canonical-terminology.md` for approved canonical terms and
  artifact rendering rules.
- `docs/migration/legacy-to-target-terminology.md` for state mappings, SRI
  document code mappings, Pending Naming Decisions, Pending Functional
  Validations, and compatibility exceptions.
- `.specify/memory/constitution.md` for non-negotiable project governance.

Feature artifacts under `specs/002-tax-document-issuance-foundation/` remain the
planning and review record. They must not contradict the constitution or the
durable architecture and migration documents.

## Complexity Tracking

No constitution violations or compatibility exceptions are introduced by this
plan.
