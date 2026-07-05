# Backend Clean Architecture

This document defines mandatory Clean Architecture, Ports and Adapters, naming,
documentation, validation, idempotency, audit, and compliance rules for the
`ecuador-tax-document-service` backend.

## Project Identity and Scope

Target project identity:

| Property | Value |
|----------|-------|
| Group | `com.alexastudillo` |
| Artifact | `ecuador-tax-document-service` |
| Base package | `com.alexastudillo.taxdocument` |
| Runtime stack | Java 25, Quarkus, Gradle with Kotlin DSL |
| Project type | Backend only |
| Architecture | Clean Architecture with Ports and Adapters |
| Canonical language | English |
| Domain | Ecuador SRI electronic tax documents |

This architecture enabler is documentation-only. It creates no Java source,
Quarkus classes, REST endpoints, persistence entities, SRI clients, database
migrations, authentication, authorization, webhooks, production data migration,
or legacy refactoring.

Durable documentation locations:

| Artifact Category | Required Location | Purpose |
|-------------------|-------------------|---------|
| AS-IS legacy documentation | `docs/legacy/` | Evidence-based documentation of the legacy system without mixing it with target specifications. |
| Migration mappings and canonical terminology | `docs/migration/` | Canonical language, legacy-to-target mappings, forbidden legacy terms, pending naming decisions, and migration classifications. |
| Architecture rules and decisions | `docs/architecture/` or `docs/adr/` | Clean Architecture rules, architectural decisions, and backend governance documentation. |
| Target specifications | `.specify/specs/` | Active Spec Kit target specifications, plans, tasks, contracts, and feature artifacts. |

AS-IS legacy documentation must not be stored as target specification artifacts.
Migration mappings must not be stored only inside temporary feature files.
Architecture rules and decisions must not compete with the constitution as a
second source of truth.

## Dependency Direction

Source dependencies must point inward:

```text
adapter -> application -> domain
bootstrap -> adapter
bootstrap -> application
```

The domain layer must not depend on Quarkus, Hibernate, Panache, REST, JAX-RS,
JSON serialization, PostgreSQL, Redis, filesystems, SOAP, XML, SRI clients,
HTTP clients, external APIs, messaging infrastructure, storage SDKs, or any
framework-specific type.

Business rules must not be implemented in REST resources, persistence adapters,
SRI adapters, storage adapters, queue adapters, webhook adapters, or bootstrap
configuration. Business operations must enter through application use cases.

## Layer Responsibilities

`domain` owns pure business concepts, entities, value objects, aggregates,
domain services, domain events, business invariants, business exceptions,
document totals, tax rules, state transitions, access key business structure,
and immutable authorized document rules.

`application` owns use cases, commands, queries, input ports, output ports,
orchestration, transaction boundaries, application-level validation,
idempotency decisions, authorization checks, retry eligibility, sequence
availability, and result models returned to inbound adapters.

`adapter.in.rest` owns thin REST resources, transport validation, authentication
context extraction, REST DTO mapping, HTTP status mapping, error response
mapping, path parameter parsing, and calls to application input ports only.

`adapter.out.persistence` owns application output port implementations,
persistence entity mapping, database query details, optimistic locking,
persistence error translation, and persistence-specific transaction integration.

`adapter.out.sri` owns SRI XML and SOAP contract isolation, official SRI DTOs,
SRI XML generation, XML signing integration, reception calls, authorization
calls, SRI response parsing, and SRI error mapping.

`adapter.out.storage` owns XML and related artifact storage implementations
behind application ports.

`adapter.out.queue` owns queue publisher and consumer implementations behind
application ports.

`adapter.out.webhook` owns webhook delivery implementations behind application
ports, including remote HTTP details and delivery error translation.

`bootstrap` owns Quarkus configuration, dependency injection wiring, runtime
property binding, feature toggles, configuration validation, and application
startup only.

## Use Case-Centered Design

Every business operation must be modeled as an explicit application use case.
Use case names must describe the business action, not the technical mechanism.

Preferred use case names include:

- `IssueInvoiceUseCase`
- `IssueCreditNoteUseCase`
- `IssueDebitNoteUseCase`
- `IssueWithholdingUseCase`
- `IssueWaybillUseCase`
- `RetrySriAuthorizationUseCase`
- `SynchronizeTaxDocumentsUseCase`
- `DeliverWebhookUseCase`

Generic business behavior names are forbidden, including `DocumentService`,
`SriService`, `ProcessService`, `Manager`, `Helper`, and `Util`. Static utility
classes must not contain business logic.

## Ports and Adapters

All external dependencies must be accessed through application-layer ports.
Adapters implement ports and isolate framework, persistence, SRI, storage,
queue, webhook, time, transaction, HTTP client, and external API details.

Required output port examples include, when relevant:

- `TaxDocumentRepository`
- `SriAuthorizationPort`
- `XmlStoragePort`
- `AccessKeyGeneratorPort`
- `WebhookPublisherPort`
- `ClockPort`
- `TransactionPort`

Adapters must not leak external DTOs, persistence entities, SRI XML structures,
Quarkus types, Hibernate types, Panache types, HTTP client types, queue SDK
types, storage SDK types, or database-specific types into the application or
domain layers.

## DTO Separation

DTOs must not be reused across layers.

- REST DTOs belong to `adapter.in.rest`.
- Application commands and results belong to `application`.
- Domain objects belong to `domain`.
- Persistence entities belong to `adapter.out.persistence`.
- SRI XML and SOAP DTOs belong to `adapter.out.sri`.

Domain objects must not be annotated as persistence entities. Persistence
entities must not be returned by use cases. REST DTOs must not enter the domain
layer. SRI DTOs must not leave the SRI adapter.

Required mapping flow:

```text
REST DTO -> Application Command
Application Result -> REST Response DTO
Domain Object -> Persistence Entity
Domain Object -> SRI XML DTO
SRI Response DTO -> Application Result
```

## Validation Separation

Validation must be separated into three levels.

Transport validation belongs to `adapter.in.rest` and covers JSON shape,
required fields, date format, enum values, path parameters, and request syntax.

Application validation belongs to `application` and covers authorization to
issue documents, active issuing point, duplicate access key, retry eligibility,
sequence availability, idempotency request handling, and transaction boundary
preconditions.

Domain validation belongs to `domain` and covers business invariants, totals,
tax calculations, document state transitions, access key structure, and
immutable authorized documents.

## SRI Contract Isolation

SRI is an external system and must not be treated as the internal domain model.
Official SRI XML tags, SOAP request and response models, Spanish SRI names,
authorization formats, XML signing, reception calls, authorization calls, and
SRI response parsing must be isolated inside `adapter.out.sri`.

Spanish SRI contract names may appear only in SRI XML mappers, SRI SOAP DTOs,
SRI adapter tests, official SRI fixture files, legacy compatibility adapters,
migration scripts, and legacy-to-target mapping documents.

SRI-specific names must not appear in internal domain models, application
commands, REST APIs, persistence entities, or target database objects unless a
bounded compatibility exception is documented in the feature plan and in
`docs/migration/legacy-to-target-terminology.md`.

Target APIs and target database objects must reject Spanish SRI or legacy names
unless a compatibility exception exists. Compatibility exceptions must include
reason, scope, owner, expiration condition, and the safer alternative that was
rejected.

## Naming Rules

All target packages, classes, APIs, DTOs, database objects, events, tests, and
documentation must use English canonical terminology.

Canonical terms must be rendered by artifact type:

| Artifact Type | Required Format |
|---------------|-----------------|
| Package segments | lowercase |
| Class names | PascalCase |
| DTO class names | PascalCase |
| Fields | camelCase |
| Methods | camelCase |
| Database tables and columns | lowercase snake_case |
| URL path segments | kebab-case |
| Event type names | PascalCase |
| Test class names | PascalCase with a Test suffix |
| Documentation file names | lowercase kebab-case |

Target database tables and columns must use English lowercase snake_case.
Examples include `tax_documents`, `invoice_lines`, `tax_document_taxes`,
`issuers`, `establishments`, `issuing_points`, `issuance_sequences`,
`webhook_subscriptions`, and `webhook_delivery_attempts`.

Target REST APIs must use English resource names and explicit business
operations. Recommended shapes include:

```text
POST /api/v1/invoices/issuance-requests
POST /api/v1/credit-notes/issuance-requests
POST /api/v1/debit-notes/issuance-requests
POST /api/v1/withholdings/issuance-requests
POST /api/v1/waybills/issuance-requests

GET  /api/v1/tax-documents/{accessKey}
GET  /api/v1/tax-documents/{accessKey}/xml
POST /api/v1/tax-documents/{accessKey}/authorization-retries
POST /api/v1/tax-documents/synchronization-runs
```

Canonical terminology and artifact-specific examples are maintained in
`docs/architecture/canonical-terminology.md`.

## Idempotency, Auditability, and Error Mapping

Every future feature involving critical operations must define explicit
idempotency rules for tax document issuance, SRI authorization retries,
synchronization runs, webhook delivery, XML generation, and sequence assignment.

Critical operations must be auditable:

- Issuance requested
- XML generated
- XML signed
- SRI reception submitted
- SRI authorization received
- Document authorized
- Document rejected
- Authorization retry requested
- Webhook delivery attempted
- Webhook delivery failed
- Synchronization executed

Audit logs must not contain secrets, private keys, credentials, tokens, signing
passwords, or sensitive configuration values.

Future features must map domain and application errors to REST errors at the
inbound adapter boundary. Adapter failures must be translated into application
results or application-level failures without leaking infrastructure-specific
types inward.

## Forbidden Practices

The following practices are forbidden:

- Business logic in REST resources
- Business logic in repositories or persistence adapters
- Business logic in SRI adapters
- Business logic in storage, queue, or webhook adapters
- Business logic in framework configuration or bootstrap wiring
- Domain classes annotated as JPA entities
- REST DTOs used as domain objects
- Persistence entities returned by use cases
- SRI XML DTOs used as internal models
- Direct database access from REST resources
- Direct SRI calls from REST resources
- Static utility classes for business logic
- Generic service classes with unrelated responsibilities
- Spanish legacy names in target domain, application, API, or persistence code
- One-to-one translation of the legacy code structure
- Implementing features without Spec Kit requirements, design, and tasks

## Compliance Checklist

Every future specification, plan, task list, and implementation must satisfy
this checklist before completion:

- Clean Architecture boundaries are explicit and respected.
- Dependencies point inward.
- Domain and application behavior is testable without infrastructure.
- English canonical terminology is used for target artifacts.
- SRI contract details are isolated in `adapter.out.sri`.
- DTO separation is defined for all touched boundaries.
- Validation ownership is separated across transport, application, and domain.
- External dependencies are accessed through application ports.
- Adapter models do not leak into application or domain layers.
- No legacy architecture, module structure, naming, or database design is copied.
- Every migrated legacy concept has an explicit classification.
- Pending Naming Decisions and Pending Functional Validations are documented.
- Critical operations define idempotency and audit rules.
- Audit events exclude secrets and sensitive configuration values.
- Future tasks use `T###` identifiers and cite governing `FR-###`, `AR-###`,
  `NR-###`, `TR-###`, `SC-###`, or contract sections.
- No implementation exists without Spec Kit requirements, design, and tasks.

After this enabler is implemented, `docs/architecture` and `docs/migration` are
the durable source of truth for architecture rules, canonical terminology, and
legacy-to-target mappings. Feature artifacts under `.specify/specs/` remain the
planning, contract, and review record.
