<!--
Sync Impact Report
Version change: unratified template -> 1.0.0
Modified principles:
- Placeholder principles -> I. Clean Architecture First
- Placeholder principles -> II. Mandatory Layer Responsibilities
- Placeholder principles -> III. Use Case-Centered Design
- Placeholder principles -> IV. Ports and Adapters for External Dependencies
- Placeholder principles -> V. SRI Contract Isolation
- Placeholder principles -> VI. English Canonical Terminology
- Placeholder principles -> VII. DTO and Validation Separation
- Placeholder principles -> VIII. Idempotency and Auditability
- Placeholder principles -> IX. Layered Testing Requirements
- Placeholder principles -> X. Specification-Governed Migration
Added sections:
- Target Stack, Naming, and API Constraints
- Documentation and Compliance Workflow
Removed sections:
- Placeholder Section 2
- Placeholder Section 3
Templates requiring updates:
- UPDATED .specify/templates/plan-template.md
- UPDATED .specify/templates/spec-template.md
- UPDATED .specify/templates/tasks-template.md
- NOT PRESENT .specify/templates/commands/*.md
Runtime guidance reviewed:
- REVIEWED README.md, docs/README.md, AGENTS.md
Follow-up TODOs:
- Future setup work must reconcile the current Gradle scaffold with the target Maven stack.
- Future documentation work must classify or relocate legacy Spanish docs under docs/legacy.
-->

# ecuador-tax-document-service Constitution

## Core Principles

### I. Clean Architecture First
The backend MUST follow Clean Architecture. Source dependencies MUST point inward:
`adapter -> application -> domain`, `bootstrap -> adapter`, and
`bootstrap -> application`. The domain layer MUST NOT depend on Quarkus,
Hibernate, Panache, REST, JAX-RS, JSON serialization, PostgreSQL, Redis,
filesystems, SOAP, XML, SRI clients, HTTP clients, external APIs, messaging
infrastructure, or any framework-specific type.

Business rules MUST NOT be implemented in REST resources, persistence adapters,
SRI adapters, storage adapters, queue adapters, webhook adapters, or framework
configuration. Business operations MUST enter through application use cases.

Rationale: domain behavior must remain portable, independently testable, and
protected from framework, infrastructure, and legacy migration concerns.

### II. Mandatory Layer Responsibilities
Each layer has mandatory responsibilities and MUST NOT assume another layer's
role.

- `domain`: pure business concepts, entities, value objects, aggregates, domain
  services, domain events, business invariants, business exceptions, document
  totals, tax rules, state transitions, and access key business structure.
- `application`: use cases, commands, queries, input ports, output ports,
  orchestration, transaction boundaries, application-level validation,
  idempotency decisions, and result models returned to inbound adapters.
- `adapter.in.rest`: thin REST resources, transport validation, authentication
  context extraction, REST DTO mapping, HTTP status mapping, and calls to
  application input ports only.
- `adapter.out.persistence`: application output port implementations,
  persistence entity mapping, database query details, optimistic locking, and
  persistence error translation.
- `adapter.out.sri`: SRI XML and SOAP contract isolation, official SRI DTOs,
  XML generation, XML signing integration, reception calls, authorization calls,
  SRI response parsing, and SRI error mapping.
- `adapter.out.storage`: XML and related artifact storage implementations
  behind application ports.
- `adapter.out.queue`: queue publisher and consumer implementations behind
  application ports.
- `adapter.out.webhook`: webhook delivery implementations behind application
  ports, including remote HTTP details and delivery error translation.
- `bootstrap`: Quarkus configuration, dependency injection wiring, runtime
  property binding, and application startup only.

Rationale: explicit responsibilities prevent hidden coupling and make violations
reviewable before implementation spreads across layers.

### III. Use Case-Centered Design
Every business operation MUST be modeled as an explicit application use case.
Use case names MUST describe the business action, not the technical mechanism.
Preferred use case names include `IssueInvoiceUseCase`,
`IssueCreditNoteUseCase`, `IssueDebitNoteUseCase`,
`IssueWithholdingUseCase`, `IssueWaybillUseCase`,
`RetrySriAuthorizationUseCase`, `SynchronizeTaxDocumentsUseCase`, and
`DeliverWebhookUseCase`.

Generic service names are forbidden for business behavior, including
`DocumentService`, `SriService`, `ProcessService`, `Manager`, `Helper`, and
`Util`. Static utility classes MUST NOT contain business logic.

Rationale: use cases make intent, transaction boundaries, authorization,
idempotency, and tests explicit.

### IV. Ports and Adapters for External Dependencies
All external dependencies MUST be accessed through application-layer ports.
Required output ports include, where relevant, `TaxDocumentRepository`,
`SriAuthorizationPort`, `XmlStoragePort`, `AccessKeyGeneratorPort`,
`WebhookPublisherPort`, `ClockPort`, and `TransactionPort`.

Adapters MUST implement ports and MUST NOT leak external DTOs, persistence
entities, SRI XML structures, Quarkus types, Hibernate types, Panache types,
HTTP client types, or storage SDK types into the application or domain layers.

Rationale: ports preserve the application boundary and allow use cases to be
tested without PostgreSQL, Redis, SRI, filesystems, or external HTTP services.

### V. SRI Contract Isolation
SRI is an external system and MUST NOT be treated as the internal domain model.
Official SRI XML tags, SOAP request and response models, Spanish SRI names,
authorization formats, XML signing, reception calls, authorization calls, and
SRI response parsing MUST be isolated inside `adapter.out.sri`.

Spanish SRI contract names MAY appear only in SRI XML mappers, SRI SOAP DTOs,
SRI adapter tests, official SRI fixture files, legacy compatibility adapters,
migration scripts, and legacy-to-target mapping documents. SRI-specific names
MUST NOT appear in internal domain models, application commands, REST APIs,
persistence entities, or target database objects unless a compatibility
exception is documented in the feature plan.

Rationale: the target system must preserve SRI interoperability without copying
the SRI contract into core business language.

### VI. English Canonical Terminology
All target code, packages, APIs, DTOs, database objects, events, tests, and
documentation MUST use English canonical terminology. Names MUST be
business-oriented English names, not literal translations. The base package for
target Java code is `com.alexastudillo.taxdocument`.

Initial canonical terminology:

| Legacy Term | Target Term |
|-------------|-------------|
| comprobante | taxDocument |
| factura | invoice |
| nota credito | creditNote |
| nota debito | debitNote |
| retencion | withholding |
| guia remision | waybill |
| emisor | issuer |
| comprador | buyer |
| receptor | recipient |
| clave acceso | accessKey |
| numero autorizacion | authorizationNumber |
| fecha emision | issueDate |
| fecha autorizacion | authorizedAt |
| razon social | legalName |
| nombre comercial | tradeName |
| establecimiento | establishment |
| punto emision | issuingPoint |
| secuencial | sequenceNumber |

Unclear terms MUST be registered as Pending Naming Decisions before feature
planning completes. Target domain, application, API, persistence, tests, and
documentation MUST NOT use Spanish legacy names unless the concept is classified
as an allowed SRI adapter, legacy compatibility, or migration-only exception.

Rationale: canonical terminology prevents legacy naming from becoming target
architecture.

### VII. DTO and Validation Separation
DTOs MUST NOT be reused across layers. REST DTOs belong to `adapter.in.rest`.
Persistence entities belong to `adapter.out.persistence`. SRI XML and SOAP DTOs
belong to `adapter.out.sri`. Domain objects MUST NOT be annotated as persistence
entities. Persistence entities MUST NOT be returned by use cases. REST DTOs MUST
NOT enter the domain layer. SRI DTOs MUST NOT leave the SRI adapter.

Required mapping flow:

```text
REST DTO -> Application Command
Application Result -> REST Response DTO
Domain Object -> Persistence Entity
Domain Object -> SRI XML DTO
SRI Response DTO -> Application Result
```

Validation MUST be separated into three levels:

- Transport validation in `adapter.in.rest`: JSON shape, required fields, date
  format, enum values, and path parameters.
- Application validation in `application`: authorization to issue documents,
  active issuing point, duplicate access key, retry eligibility, and sequence
  availability.
- Domain validation in `domain`: business invariants, totals, tax calculations,
  document state transitions, access key structure, and immutable authorized
  documents.

Rationale: separate models and validation levels keep each boundary explicit and
make failures easier to map to users, logs, and audits.

### VIII. Idempotency and Auditability
Every feature involving critical operations MUST define explicit idempotency
rules for tax document issuance, SRI authorization retries, synchronization
runs, webhook delivery, XML generation, and sequence assignment.

Critical operations MUST be auditable:

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

Audit logs MUST NOT contain secrets, private keys, credentials, tokens, signing
passwords, or sensitive configuration values.

Rationale: electronic tax document processing needs repeatable behavior,
traceable outcomes, and safe operational evidence.

### IX. Layered Testing Requirements
Domain tests MUST run without Quarkus. Application tests MUST run without real
PostgreSQL, SRI, Redis, filesystems, or external HTTP services. Adapter tests
MAY use Quarkus test support, Testcontainers, mocks, or contract fixtures.

Required test categories, when the corresponding behavior or layer is touched:

- Domain unit tests
- Application use case tests
- Adapter mapping tests
- REST resource tests
- Persistence adapter tests
- SRI adapter contract tests
- Idempotency tests
- Error mapping tests

Rationale: the test suite must prove business behavior independently from
infrastructure and prove adapter contracts at the system edges.

### X. Specification-Governed Migration
No implementation may be created unless it is backed by Spec Kit requirements,
a technical design, and a task list. Implementation MUST be limited to the work
covered by tasks.

AS-IS documentation MUST remain separated from target specifications. Required
locations:

- AS-IS legacy documentation: `docs/legacy`
- Migration mapping: `docs/migration`
- Architecture decisions: `docs/architecture` or `docs/adr`
- Target specifications: `.specify/specs` or the active Spec Kit specs folder

Every migrated concept MUST have one classification:

- Target domain concept
- Target API field
- Target database object
- SRI adapter-only concept
- Legacy compatibility concept
- Migration-only concept
- Deprecated concept
- Pending Naming Decision
- Pending Functional Validation

Rationale: migration work must preserve validated business behavior without
copying legacy architecture, naming, modules, coupling, or database design.

## Target Stack, Naming, and API Constraints

The target backend stack is Java 25, Quarkus, Maven, backend only, Clean
Architecture, and Ports and Adapters. Target Java code MUST live under
`com.alexastudillo.taxdocument`.

Target database tables and columns MUST use English lowercase snake_case.
Examples include `tax_documents`, `invoice_lines`, `tax_document_taxes`,
`issuers`, `establishments`, `issuing_points`, `issuance_sequences`,
`webhook_subscriptions`, and `webhook_delivery_attempts`. Spanish database
names are forbidden in the target schema except in migration scripts,
compatibility views, or legacy mapping documentation.

REST APIs MUST use English resource names and explicit business operations.
Recommended endpoint shapes include:

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

Target APIs MUST NOT expose Spanish legacy names unless an explicit legacy
compatibility adapter is required and documented in the feature plan.

Forbidden practices:

- Business logic in REST resources
- Business logic in repositories or persistence adapters
- Business logic in SRI adapters
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
- Implementing features without requirements, design, and tasks

## Documentation and Compliance Workflow

For every feature:

1. Start with a target specification.
2. Generate or update the technical plan.
3. Generate the task list.
4. Implement only what is covered by tasks.
5. Validate Clean Architecture boundaries before completion.

Every future specification, plan, task list, and implementation MUST satisfy
this compliance checklist before completion:

- Clean Architecture boundaries are explicit and respected.
- English canonical terminology is used for target artifacts.
- SRI contract details are isolated in `adapter.out.sri`.
- DTO separation is defined for all touched boundaries.
- External dependencies are accessed through application ports.
- No legacy architecture, module structure, naming, or database design is copied.
- Domain and application behavior is testable without infrastructure.
- Every migrated legacy concept has an explicit classification.
- Pending Naming Decisions and Pending Functional Validations are documented.
- No implementation exists without Spec Kit requirements, design, and tasks.
- Idempotency rules are defined for all critical repeatable operations.
- Audit events are defined without secrets or sensitive configuration values.

Plans MUST document any compatibility exception, including the reason, scope,
owner, expiration condition, and safer alternative that was rejected.

## Governance

This constitution supersedes conflicting repository conventions, generated
framework defaults, legacy implementation patterns, and informal practices. Any
specification, plan, task list, or implementation that violates this constitution
MUST be corrected before completion unless the violation is documented as a
temporary compatibility exception in the active plan.

Amendments require:

1. A written change to this constitution.
2. A Sync Impact Report describing affected principles, templates, and follow-up
   work.
3. Updates to dependent Spec Kit templates when governance changes affect future
   specifications, plans, tasks, or compliance checks.
4. Review of runtime guidance documents for conflicts.

Versioning policy:

- MAJOR version changes remove or redefine non-negotiable principles in a
  backward-incompatible way.
- MINOR version changes add principles, sections, or materially expanded
  governance.
- PATCH version changes clarify wording, fix errors, or make non-semantic
  refinements.

Compliance review is mandatory at specification, planning, task generation, and
implementation completion. The plan's Constitution Check is the enforcement gate
before Phase 0 research and again after Phase 1 design.

**Version**: 1.0.0 | **Ratified**: 2026-07-05 | **Last Amended**: 2026-07-05
