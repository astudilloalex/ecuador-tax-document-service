<!--
Sync Impact Report
- Version change: 1.1.0 -> 2.0.0
- Modified principles:
  - III. Required Technology Baseline and JVM Safety (Keycloak/OIDC removed)
  - VII. Zero-Trust Identity and Tenant Isolation -> VII. Internal Company Context Without Identity
  - X. Target-First APIs and Observable Asynchronous Work (application authorization removed)
  - XI. Port-Bound SRI and External Integrations (Keycloak removed)
  - XII. Risk-Based Testing and Evidence (Company-header/scoping evidence replaces auth evidence)
  - XIV. Mandatory Spec Kit Delivery Workflow (Company-context boundary added to critical gates)
  - XVI. Company Master Data Ownership and Immutable Fiscal Snapshots -> XVI. Definitive Company Context and Draft Boundary
- Added sections: None
- Modified sections:
  - Definition of Done (header, scoping, no-auth, and no-Company-dependency evidence)
- Removed sections:
  - Mandatory Keycloak/OIDC/JWT authentication and application authorization governance
  - Tenant-derived Company context and cross-tenant authorization governance
  - Create-draft Company Service resolution and fiscal-snapshot requirements
- Consistency updates:
  - ✅ .specify/templates/spec-template.md
  - ✅ .specify/templates/plan-template.md
  - ✅ .specify/templates/tasks-template.md
  - ✅ .specify/templates/checklist-template.md
  - ✅ .specify/workflows/speckit/workflow.yml
  - ✅ .agents/skills/speckit-specify/SKILL.md
  - ✅ .agents/skills/speckit-clarify/SKILL.md
  - ✅ .agents/skills/speckit-tasks/SKILL.md
  - ✅ .agents/skills/speckit-analyze/SKILL.md
  - ✅ .agents/skills/speckit-implement/SKILL.md
  - ✅ docs/migration/terminology-mapping.md
  - ✅ specs/001-create-invoice-draft/spec.md
  - ✅ specs/001-create-invoice-draft/plan.md
  - ✅ specs/001-create-invoice-draft/research.md
  - ✅ specs/001-create-invoice-draft/data-model.md
  - ✅ specs/001-create-invoice-draft/contracts/company-context-port.md (removed)
  - ✅ specs/001-create-invoice-draft/contracts/invoice-draft-api.openapi.yaml
  - ✅ specs/001-create-invoice-draft/quickstart.md
  - ✅ specs/001-create-invoice-draft/checklists/requirements.md
  - ✅ specs/001-create-invoice-draft/checklists/planning.md
- Reviewed without changes:
  - .specify/templates/constitution-template.md remains the upstream placeholder template
  - README.md contains runtime commands but no conflicting governance requirements
  - .specify/workflows/workflow-registry.json contains workflow registration metadata only
  - specs/001-create-invoice-draft/tasks.md does not exist; no task artifact was available to update
  - No .specify/templates/commands directory exists in this installation
  - .agents/skills/speckit-plan/SKILL.md and .agents/skills/speckit-checklist/SKILL.md load the
    constitution and their updated templates dynamically
  - .agents/skills/speckit-converge/SKILL.md extracts every constitutional MUST dynamically
  - .agents/skills/speckit-taskstoissues/SKILL.md transfers approved tasks without redefining them
- Follow-up TODOs: None
-->
# Ecuador Tax Document Service Constitution

## Core Principles

### I. Evidence-Governed Greenfield Reengineering

The project MUST build a new Ecuadorian electronic tax document service. The legacy NestJS
system MAY be used only as evidence for domain discovery and test-scenario discovery. The
target is a greenfield reengineering effort; it MUST NOT be treated as a line-by-line migration,
framework conversion, compatibility rewrite, or requirement to "migrate the entire legacy
system."

- Legacy source code, routes, payloads, responses, DTOs, database structures, tables, statuses,
  deployment files, authentication mechanisms, asynchronous behavior, operational behavior,
  and defects MUST NOT become target requirements by default.
- A legacy route, payload, response, table, status, or behavior MUST NOT be preserved unless an
  approved target feature specification requires that exact compatibility.
- `docs/legacy/as-is/` and legacy source code are historical evidence, not authoritative target
  requirements.
- Historical documents under `docs/legacy/` MUST remain unchanged, including documents written
  in Spanish.
- New requirements and decisions MUST be recorded under `specs/`, `docs/migration/`, or
  `docs/architecture/`, according to their purpose.
- Unknown or contradictory behavior MUST be recorded as **Pending Functional Validation**. It
  MUST NOT be silently inferred from legacy behavior, convention, or implementation convenience.
- Absence of legacy evidence MUST NOT be interpreted as evidence that a behavior is unnecessary.
- Each feature specification MUST describe one bounded, independently valuable user or
  stakeholder outcome. It MUST NOT bundle unrelated document types or administrative
  capabilities.

Project decisions MUST apply this authority order, from highest to lowest:

1. Applicable Ecuadorian legislation and versioned official SRI technical documentation.
2. This constitution.
3. Approved target feature specifications and recorded clarifications.
4. Approved architecture decisions and implementation plans.
5. Legacy AS-IS documentation and legacy source code.

When sources conflict, the higher-authority source MUST govern. The affected specification,
plan, or architecture decision MUST identify the conflict, the sources and versions involved,
and the approved resolution.

**Rationale**: Explicit authority and scope rules prevent accidental reproduction of legacy
defects and ensure that fiscal obligations, rather than historical implementation details, drive
the target service.

### II. English Canonical Language and Controlled Terminology

All target artifacts MUST use English. This requirement includes:

- Java packages, classes, interfaces, methods, variables, constants, and enum names.
- API routes, operations, schemas, fields, and error codes.
- Database schemas, tables, columns, constraints, indexes, and migrations.
- Tests, fixtures, comments, JavaDoc, logs, configuration keys, documentation, architecture
  decision records, specifications, plans, tasks, and commit messages.

Only these exceptions are permitted:

- Official SRI XML element names, catalog codes, status values, and legally defined terms that
  MUST remain exact.
- Historical documents under `docs/legacy/`.
- User-visible content whose approved language is Spanish.

`docs/migration/terminology-mapping.md` MUST be maintained as the canonical
**Legacy-To-Target Terminology Mapping**. Every mapped term MUST have exactly one of these
classifications:

- Target Domain
- Target API
- Target Database
- SRI Adapter Only
- Legacy Compatibility
- Migration Only
- Deprecated
- Pending Naming Decision
- Pending Functional Validation

A developer or agent MUST NOT introduce a Spanish-derived target name or an unapproved
translation while the applicable mapping is marked **Pending Naming Decision** or
**Pending Functional Validation**. Official SRI names that are not canonical domain terms MUST
remain within the SRI adapter.

**Rationale**: A single controlled vocabulary makes cross-layer mappings reviewable while
preserving exact regulatory terms only where their exact form is required.

### III. Required Technology Baseline and JVM Safety

The target implementation MUST use:

- Java 25.
- Quarkus, with its exact version selected and justified in the applicable implementation plan.
- Mutiny for reactive application and adapter flows.
- PostgreSQL.
- Hibernate Reactive with Panache for reactive persistence.
- Flyway as the only authoritative database schema migration mechanism.

The service MUST support JVM execution. Native compilation is a desired capability, not a
mandatory production outcome. Every implementation plan involving native-sensitive behavior
MUST define a native-compatibility evaluation and, before feature completion, record actual
build and runtime evidence for each applicable area:

- SOAP clients.
- XML generation and schema validation.
- XML digital signatures.
- PKCS#12 certificate handling.
- Cryptographic providers.
- Reflection and resource loading.

A JVM deployment MUST remain acceptable when native compilation creates unjustified complexity,
uses unsupported dependencies, weakens security, or creates disproportionate operational risk.
The plan MUST record the evidence and the decision to support, defer, or reject native execution.

**Rationale**: The fixed baseline enables consistent architecture and testing while the evidence
gate prevents native compilation from compromising fiscal or security correctness.

### IV. Clean Architecture and Explicit Boundaries

The base package MUST be `com.alexastudillo.taxdocument`. Production code beneath it MUST use
these top-level boundaries: `api`, `application`, `domain`, and `infrastructure`. Packages within
each boundary SHOULD be grouped by business capability; a different grouping MUST be justified
in the implementation plan with the cohesion problem it solves.

Dependencies MUST follow these rules:

- `domain` MUST depend only on the Java language and explicitly approved domain-level libraries.
- `application` MAY depend on `domain` and on Mutiny for asynchronous use cases and I/O ports. It
  MUST NOT depend on `api` or `infrastructure`.
- `api` MAY depend on `application` and MUST explicitly map transport models to application
  inputs and outputs. It MUST NOT depend on infrastructure persistence models.
- `infrastructure` MUST implement outbound application ports and MAY depend on application ports
  and domain types where required.
- Framework-specific composition and dependency-injection wiring MUST remain outside `domain`.

The `domain` boundary MUST NOT contain Quarkus APIs, Jakarta REST annotations, Hibernate or
Panache types, PostgreSQL-specific types, Keycloak or OIDC types, JSON serialization annotations,
transport DTOs, persistence entities, or Mutiny `Uni` or `Multi`. Domain behavior MUST be
synchronous, deterministic, and independently testable.

Panache entities MUST remain infrastructure persistence models. They MUST NOT be used as domain
entities or returned directly by API endpoints. Mapping among API DTOs, application
commands/results, domain objects, and persistence entities MUST be explicit and testable.

Interfaces MUST represent an actual architectural boundary or replaceable external dependency.
The project MUST NOT introduce an interface, factory, base class, repository hierarchy, or
generic abstraction without a current use case documented in the plan.

**Rationale**: Enforced dependency direction protects fiscal rules from framework churn and
makes transport, persistence, and external integration choices replaceable at real boundaries.

### V. Non-Blocking Reactive Execution

HTTP, application, and reactive persistence flows MUST remain non-blocking. Code executing on an
event-loop thread MUST NOT perform:

- Blocking filesystem access or blocking network calls.
- Certificate parsing or cryptographic signing.
- CPU-intensive XML generation, transformation, or schema validation.
- Blocking SOAP operations.
- Thread sleeps.
- Synchronous waits on a `Uni`, `CompletionStage`, future, or equivalent asynchronous result.

Every unavoidable blocking or CPU-intensive operation MUST be identified in the implementation
plan, isolated in an infrastructure adapter, executed on an appropriate worker or bounded
executor, protected by timeouts and resource limits, and covered by concurrency and failure
tests. A reactive wrapper MUST NOT be used to disguise a blocking call.

**Rationale**: Explicit isolation preserves event-loop capacity and makes resource exhaustion,
timeouts, and blocking behavior observable and testable.

### VI. Official-Rule Fiscal Correctness

Applicable, versioned official SRI rules MUST govern access-key generation, check-digit
algorithms including Modulo 11, XML structure and namespaces, catalog codes, tax calculations,
rounding, document states, certificate and signature requirements, and reception and
authorization flows. Legacy algorithms MAY supply candidate test scenarios but MUST NOT become
authoritative until validated against the governing official source.

Monetary values, rates, taxable bases, discounts, and taxes MUST use `BigDecimal`; floating-point
types MUST NOT represent them. Every monetary rule MUST define its scale, rounding mode,
permitted precision, currency, validation boundaries, and the component responsible for
calculation or reconciliation.

Tax-document dates MUST distinguish Ecuadorian civil or fiscal dates, interpreted under the
applicable Ecuador timezone, from technical audit timestamps represented as unambiguous instants.
The system MUST NOT silently normalize impossible dates, inconsistent totals, invalid catalog
combinations, or unsupported identification types.

**Rationale**: Fiscal validity depends on versioned official rules, deterministic arithmetic,
and explicit time semantics rather than incidental legacy behavior.

### VII. Internal Company Context Without Identity

This repository MUST NOT own user management, authentication, authorization, user-to-Company
permission decisions, an API gateway, or a backend-for-frontend. The Tax Document Service MUST NOT
implement, require, parse, propagate, persist, or interpret Keycloak, OpenID Connect, OAuth, JWTs,
access tokens, API keys, user sessions, authenticated principals, user identifiers, roles,
permissions, tenant authorization, user-to-Company authorization, or application-level service
authentication.

The internal API explicitly accepts that any process capable of reaching it MAY submit any
syntactically valid Company UUID. The service MUST NOT determine whether that process is entitled
to use the Company identifier. Network, firewall, ingress, gateway, BFF, authentication, and
authorization controls belong outside this repository.

The upstream Gateway or BFF is responsible for platform authentication when required,
user-to-Company authorization when required, validation against the authoritative Company
capability, and validation of Company/Issuer/establishment/emission-point relationships when the
upstream workflow requires them. Before forwarding a Company-scoped request, it MUST remove every
externally supplied `X-Company-Id` value and MUST inject exactly one canonical, previously validated
`X-Company-Id` value. Those upstream responsibilities remain outside this repository and MUST NOT
become Tax Document Service dependencies, error outcomes, or claims of enforcement. The Tax
Document Service MUST NOT verify whether the upstream component fulfilled them.

The OpenAPI contract MUST NOT define a security scheme, security requirement, Authorization
header, `401` authentication response, or `403` authorization response.

**Rationale**: An explicit caller-agnostic internal boundary prevents accidental duplication of
platform identity concerns and keeps Company context as business metadata rather than a security
credential.

### VIII. Sensitive Data and Certificate Security

PKCS#12 files, certificate passwords, signing keys, tax documents, personal identification,
webhook secrets, and fiscal payloads MUST be treated as sensitive information. Sensitive
values MUST NOT appear in source control, examples containing real values, logs, exception
messages, metric labels, traces, queue metadata without an approved retention requirement, or
test fixtures derived from production data.

Certificate passwords and webhook secrets MUST NOT be stored in plaintext. Before certificate
management implementation begins, its approved specification or plan MUST define certificate
storage, encryption, rotation, expiration, revocation, deletion, backup, and Company ownership.
Cryptographic failures MUST fail closed.

**Rationale**: Fiscal documents and signing material can create legal and identity harm; this risk
therefore requires lifecycle design before storage or signing code is written.

### IX. Flyway-Governed Persistence and Explicit Consistency

Flyway migrations MUST be the authoritative, repeatable procedure for creating and evolving
PostgreSQL structures. The project MUST NOT use production schema auto-generation, manual SQL as
the normal deployment procedure, a copied legacy database dump as the target schema, or a
destructive migration without explicit approval and recovery instructions.

A committed migration MUST be immutable. A correction MUST use a new migration. Database
constraints MUST enforce critical invariants that remain valid regardless of entry point,
including applicable uniqueness, referential integrity, required ownership, and idempotency
constraints. Migration tests MUST prove creation from an empty database.

A database transaction MUST NOT be represented as atomic with an external SRI call, filesystem
write, object-storage operation, queue publication, or webhook. A feature crossing any of those
boundaries MUST define intermediate states, retry behavior, idempotency behavior, duplicate
handling, timeout behavior, failure recovery, reconciliation, and observable terminal outcomes.

**Rationale**: Repeatable evolution and explicit distributed failure semantics prevent hidden
manual state and false atomicity claims.

### X. Target-First APIs and Observable Asynchronous Work

Target APIs MUST be designed from approved target use cases and MUST NOT inherit a legacy
contract implicitly. API DTOs MUST be distinct from domain objects and persistence entities.
Validation ownership MUST be explicit:

- Transport validation MUST check structure and representation.
- Application validation MUST check use-case preconditions and Company-scoped business invariants.
- Domain validation MUST protect business invariants.
- Infrastructure validation MUST handle external-system constraints.

All API errors MUST follow one target error contract with stable machine-readable codes. Internal
stack traces, SQL errors, filesystem paths, certificate details, and unfiltered exception
messages MUST NOT be exposed. Every request MUST carry or receive a correlation identifier that
is propagated through logs and external integrations.

An asynchronous command MUST NOT return only an opaque job identifier without an approved method
to observe its final result. Every asynchronous operation MUST define status query or delivery,
correlation identifiers, terminal states, caller-safe error details, retry semantics, retention,
idempotency, and Company-scoped result access when applicable.

**Rationale**: Explicit contracts and observable outcomes let callers recover safely without
coupling the target service to legacy representations.

### XI. Port-Bound SRI and External Integrations

SOAP, XML, certificate, rendering, notification, and storage integrations MUST be
infrastructure adapters behind application ports. Official SRI names and payload structures MUST
remain confined to the SRI adapter when they are not canonical target-domain concepts.

Every external call MUST define connection and request timeouts, bounded retry rules, retryable
and non-retryable failures, circuit or degradation behavior or a documented reason it is
unnecessary, environment-specific endpoints, sanitized observability, and contract tests or
representative fixtures. Health checks MUST use the same configured destination as the
corresponding business adapter.

A retry of one logical command MUST NOT generate a new fiscal operation, access key, sequence, or
persisted document unless an approved fiscal rule explicitly requires it.

**Rationale**: Port isolation and stable logical-command identity make external failures
recoverable without duplicating legally significant operations.

### XII. Risk-Based Testing and Evidence

Tests MUST derive from approved target requirements, applicable official SRI rules, and documented
risk scenarios. Legacy tests MAY contribute scenarios but MUST NOT be treated as automatically
authoritative. The testing strategy MUST include every applicable category below:

- Pure domain unit tests.
- Application use-case tests.
- `X-Company-Id` contract and Company-scoping tests.
- Reactive persistence integration tests using PostgreSQL.
- Flyway migration tests from an empty database.
- API contract tests.
- SRI SOAP adapter tests.
- XML schema-validation tests.
- Deterministic access-key and signature test vectors.
- Monetary boundary and rounding tests.
- Idempotency, retry, timeout, and reconciliation tests.
- Security tests for sensitive-data exposure.
- JVM runtime tests.
- Native build and runtime smoke tests when native compatibility is claimed.

Acceptance scenarios and required tests MUST be identified before production implementation. A
test MUST verify observable behavior or an invariant. A test written only to increase coverage
is prohibited. A requirement MUST NOT be considered complete while its critical failure,
security, or boundary scenarios remain untested.

**Rationale**: Risk-based evidence proves the fiscal and security properties that coverage
percentages alone cannot establish.

### XIII. Operational Observability and Auditability

The service MUST expose distinct liveness and readiness behavior. Liveness MUST report whether the
process can execute its internal health logic; it MUST NOT duplicate readiness or fail solely
because a downstream service is unavailable. Readiness MUST reflect dependencies required to
accept traffic safely and MUST NOT invoke a destructive operation. Logs MUST be structured,
include correlation context, and exclude sensitive data.

Operationally significant state transitions and administrative certificate operations MUST be
auditable. Metrics, traces, and health checks MUST observe the same configured external
destinations used by business operations; a health check MUST NOT report a different endpoint
from the endpoint used by its adapter.

**Rationale**: Operational signals are trustworthy only when they represent the real execution
path and preserve enough sanitized context for reconciliation and audit.

### XIV. Mandatory Spec Kit Delivery Workflow

Work MUST follow this order:

1. `$speckit-constitution`
2. `$speckit-specify`
3. `$speckit-clarify`
4. `$speckit-plan`
5. `$speckit-checklist`
6. `$speckit-tasks`
7. `$speckit-analyze`
8. `$speckit-implement`

This constitution MUST be approved on `main` before feature work begins. Each feature
specification MUST focus on what is required and why, describe one bounded outcome, contain
independently testable acceptance scenarios, identify exclusions and non-goals, reference
relevant legacy evidence without granting it authority, cite applicable official SRI rules and
versions, record unresolved matters as Pending Functional Validation, and avoid bundling
unrelated document types or administrative capabilities.

Material ambiguity MUST be processed through `$speckit-clarify` before planning. Technical
choices MUST be made in `$speckit-plan`, not prematurely fixed in the feature specification.
Generated checklists MUST validate requirement quality rather than implementation completion;
items MUST be evaluated honestly and MUST NOT be marked complete merely to unblock work.

`$speckit-analyze` MUST run after task generation and before implementation. Implementation MUST
NOT begin while analysis has an unresolved critical inconsistency involving fiscal correctness,
Company context, Company master-data ownership, data loss, idempotency, external integration
contracts, certificate management, or database evolution.

**Rationale**: Ordered, reviewable artifacts expose ambiguity and governance violations before
they become expensive or legally significant code.

### XV. Simplicity and Justified Complexity

The project MUST implement the smallest design that completely satisfies the approved feature and
this constitution. Without a current approved requirement, the project MUST NOT introduce:

- Additional microservices or generic shared platforms.
- Event sourcing, sagas, or CQRS with separate models.
- Custom authentication frameworks.
- Generic repository hierarchies.
- Internal message brokers.
- Plugin architectures.
- Premature caching.
- Compatibility layers for unused legacy contracts.

Every feature plan MUST justify each new external dependency, abstraction, background process,
persistent store, and distributed interaction. A complexity exception MUST identify the
requirement creating the need, simpler alternatives considered, why each alternative is
insufficient, and the testing and operational consequences.

**Rationale**: Requiring a current use case and recorded alternatives keeps the greenfield system
focused on fiscal value instead of speculative infrastructure.

### XVI. Definitive Company Context and Draft Boundary

This repository MUST implement only the Ecuador Tax Document and Billing bounded context. It owns
Invoice Draft aggregates, invoice lines, buyer information captured in a draft, tax selections and
calculated tax amounts, payments, additional information, deterministic monetary calculations,
Company-scoped idempotency, and tax-document lifecycle capabilities introduced through separately
approved specifications.

It MUST NOT own Company master data, Company administration, Company status, Issuer
administration, establishment administration, emission-point administration, user management,
authentication, authorization, user-to-Company permission decisions, API gateway behavior, or BFF
behavior. The Company bounded context remains the sole authority for Company master data and
relationships.

Every Company-scoped HTTP operation MUST receive the Company identifier in exactly one mandatory
`X-Company-Id` request header. The identifier MUST NOT appear in a resource path, query string,
request body, authentication token, or user-session context. Company-scoped paths MUST name only
resources owned by this bounded context; for example, `POST /invoice-drafts` MUST be used instead
of `POST /companies/{companyId}/invoice-drafts`. A global API or version prefix MAY precede the
resource path.

`X-Company-Id` MUST contain exactly one nonblank, non-null, non-nil UUID. The API boundary MUST
validate only header presence, single-value cardinality, UUID syntax, and non-nil value. It MAY
normalize an accepted UUID to lowercase hyphenated form. Missing, repeated, blank, malformed, and
nil values MUST fail with stable codes equivalent to `COMPANY_CONTEXT_REQUIRED` or
`COMPANY_CONTEXT_INVALID` and MUST create no aggregate, child, or idempotency binding.

`X-Company-Id` is business-context metadata. It MUST NOT be treated as an authentication or
authorization credential, API key, token, or proof of entitlement. The service MUST NOT validate
Company existence, active state, fiscal eligibility, tenant membership, caller entitlement, or
Company/Issuer/establishment/emission-point relationships. It MUST NOT define
`COMPANY_NOT_FOUND`, `COMPANY_INACTIVE`, `COMPANY_NOT_AUTHORIZED`,
`COMPANY_CONTEXT_UNAVAILABLE`, `COMPANY_CONTEXT_TIMEOUT`, or
`ISSUER_COMPANY_MISMATCH` outcomes.

The API adapter MUST map the header value to an application-level `CompanyId`. HTTP header objects,
security contexts, thread-local request state, Gateway implementations, BFF implementations, and
Company Service clients MUST NOT enter the application or domain model.

An Invoice Draft MUST store the normalized Company UUID as its immutable external ownership
reference. `CompanyId` MUST scope repository queries, mutations, idempotency, and safe operational
correlation. It MUST NOT be described as caller authorization or security isolation. Every child
record MUST belong to its Invoice Draft through local aggregate relationships and MUST NOT
independently represent Company master data.

Company-scoped idempotency MUST use `CompanyId + Idempotency-Key`. Persistence uniqueness MUST use
`company_id + idempotency_key_hash`. The same key MAY be used independently by different Companies.
For one Company, equivalent normalized content MUST return the original committed aggregate,
different content MUST return a stable conflict, and concurrent equivalent commands MUST create
exactly one aggregate. `CompanyId`, correlation identifiers, and transport-only headers MUST NOT be
duplicated inside or affect the normalized request-content fingerprint.

Create Invoice Draft MUST NOT call the Company Service or introduce a `CompanyContextPort`,
Company client, Company repository, Company entity, Company validation or authorization adapter,
Company eligibility lookup, Company status cache, Company master-data replication, direct Company
database access, cross-service foreign key, or cross-service transaction. Company dependency
failure, timeout, availability, and readiness outcomes MUST NOT exist for this feature.

Create Invoice Draft MUST persist only the opaque Company identifier and other caller-supplied or
system-calculated draft data approved by its specification. It MUST NOT resolve or persist Company
master-data snapshots, Company-context versions or observation timestamps, Issuer fiscal
snapshots, establishment snapshots, or emission-point fiscal snapshots. An Invoice Draft is not an
issued fiscal document.

A later, separately approved fiscal-issuance specification MUST define how authoritative Company
and Issuer fiscal data is obtained, when Company and fiscal eligibility are revalidated, which
immutable fiscal snapshot is persisted, how the official sequence and access key are allocated,
how XML is generated and signed, and how the document is submitted to the SRI. Those concerns MUST
NOT enter Create Invoice Draft.

The Tax Document Service MUST NOT create a Company aggregate, Company repository, local Company
master table, current Company status replica, Company user or permission model, Company cache,
direct Company database access, cross-service foreign key, or cross-service database transaction.
It MUST NOT use an application cache.

**Rationale**: A single syntactic Company header gives the internal billing boundary deterministic
partitioning and idempotency without duplicating platform identity, Company master data, or later
fiscal-issuance responsibilities.

## Definition of Done

A feature is complete only when all of these conditions are satisfied:

- Its approved requirements and acceptance scenarios are satisfied.
- Constitution compliance has been reviewed and recorded.
- All required tests pass, and static analysis and formatting checks pass.
- Database migrations are repeatable from an empty database.
- Company context is verified: exactly one valid `X-Company-Id` scopes every Company operation,
  invalid headers create no state, and Company identifiers do not enter paths, query strings,
  bodies, identity artifacts, or normalized content fingerprints.
- The no-authentication boundary is verified: no identity, token, security scheme, `401`, `403`,
  tenant authorization, or user-to-Company permission behavior is introduced.
- Company master-data exclusion is verified: no Company aggregate, repository, table, client,
  port, lookup, snapshot, cache, replication, shared persistence, or Company dependency health
  check exists unless a later approved fiscal-issuance specification requires an applicable
  boundary.
- Failure, timeout, retry, duplicate, and reconciliation behavior required by the feature is
  tested.
- Documentation and `docs/migration/terminology-mapping.md` are updated when affected.
- No unresolved critical finding remains from `$speckit-analyze`.
- JVM execution is verified.
- Native compatibility is verified with evidence or explicitly documented as unsupported or
  deferred with evidence.
- No sensitive data is committed or exposed.

## Governance

This constitution is mandatory for specifications, plans, tasks, implementation, reviews, and
releases. Every implementation plan MUST contain a constitution compliance review before design
work proceeds and a second review after design. Before a feature is considered complete, its
review record MUST re-evaluate compliance against the Definition of Done.

Any deviation MUST:

- Identify the affected principle and exact rule.
- Be justified in the feature plan or an architecture decision record.
- Define its scope and expiration date or objective remediation condition.
- State its testing, security, fiscal, and operational consequences where applicable.
- Receive recorded approval before implementation begins.

An amendment MUST be proposed as a reviewed change to this file, include a Sync Impact Report,
identify dependent template or guidance changes, and receive recorded approval before merge to
`main`. Amendments MUST use semantic versioning:

- **MAJOR**: Removes or fundamentally changes a governing principle or makes existing compliant
  work non-compliant by redefining its governing intent.
- **MINOR**: Adds a principle or materially expands governance.
- **PATCH**: Clarifies wording without changing governing intent.

When this constitution conflicts with a lower-authority artifact, the lower-authority artifact
MUST be corrected. A perceived need to change a principle MUST be handled through a separate,
explicit constitution amendment and MUST NOT be resolved by diluting or silently reinterpreting
the principle during feature work.

**Version**: 2.0.0 | **Ratified**: 2026-07-12 | **Last Amended**: 2026-07-12
