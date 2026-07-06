# Feature Specification: Tax Document Issuance Foundation

**Feature Branch**: `002-tax-document-issuance-foundation`

**Created**: 2026-07-05

**Status**: Draft

**Input**: User description: "Establish the foundational domain and
application model for electronic tax document issuance in the new backend,
without creating REST endpoints, persistence adapters, SRI SOAP/XML adapters,
database migrations, or document-specific issuance flows yet."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Establish common issuance language (Priority: P1)

As a backend architect or developer, I need a common English model for tax
document issuance so that invoices, credit notes, debit notes, withholdings,
waybills, retries, synchronization, and webhooks can share consistent target
language.

**Why this priority**: All future issuance features depend on these common
concepts. Without a shared foundation, later specifications risk duplicating
legacy names, states, and coupling.

**Independent Test**: Review the feature artifacts and verify that every
required common concept is defined with an English canonical name, a migration
classification, and a traceable source.

**Acceptance Scenarios**:

1. **Given** a future feature references a legacy concept such as
   "comprobante", **When** the feature is specified, **Then** it uses
   `TaxDocument` or `taxDocument` according to the target artifact format.
2. **Given** a future document-specific use case needs a document type,
   **When** it references the common foundation, **Then** it uses one of
   `INVOICE`, `CREDIT_NOTE`, `DEBIT_NOTE`, `WAYBILL`, or `WITHHOLDING` without
   using the SRI numeric code as an internal name.
3. **Given** a future feature needs issuer, establishment, issuing point,
   sequence, access key, issue date, authorization number, or authorization
   timestamp concepts, **When** it is reviewed, **Then** it uses the canonical
   English names from this foundation and the project terminology documents.

---

### User Story 2 - Define issuance lifecycle rules (Priority: P2)

As a backend architect or developer, I need common document states,
authorization states, state transitions, retry eligibility, local voiding
rules, and authorized-document immutability so that later use cases preserve
validated business behavior consistently.

**Why this priority**: State and retry rules affect correctness, idempotency,
auditability, and legal processing. They must be shared before document-specific
issuance behavior is implemented.

**Independent Test**: Review the state model and verify that each legacy state
has a target English state or a pending decision, and that retry and voiding
rules are explicit enough to reject invalid transitions.

**Acceptance Scenarios**:

1. **Given** a legacy state such as `PENDIENTE` or `AUTORIZADO`, **When** the
   target state model is reviewed, **Then** the state has a canonical English
   name and a migration classification.
2. **Given** a tax document is already authorized, **When** a future use case
   attempts to modify business content or locally void it, **Then** the common
   rules reject the operation unless a future specification explicitly defines a
   controlled exception.
3. **Given** a retry is requested for a retry candidate state, **When** the
   future retry use case is specified, **Then** it must also satisfy the common
   retry preconditions and any unresolved Pending Functional Validation.

---

### User Story 3 - Define application boundaries for future use cases (Priority: P3)

As a backend architect or developer, I need application-layer ports and common
use case contracts so that future features access persistence, SRI, storage,
queues, webhooks, time, transactions, and audit logging through stable
boundaries.

**Why this priority**: Future issuance behavior must enter through explicit use
cases and ports. This prevents REST resources, repositories, SRI adapters,
queues, or framework configuration from becoming business-rule owners.

**Independent Test**: Review future use case contracts and verify that every
external dependency is represented by an application port and no adapter model
is exposed to domain or application rules.

**Acceptance Scenarios**:

1. **Given** a future invoice issuance feature needs persistence or sequence
   assignment, **When** it is planned, **Then** it depends on
   `TaxDocumentRepository` and `SequenceNumberPort` rather than direct database
   access.
2. **Given** a future SRI authorization retry feature needs SRI communication,
   **When** it is planned, **Then** it depends on `SriAuthorizationPort` rather
   than an SRI XML, SOAP, or HTTP client model.
3. **Given** a future webhook feature publishes events, **When** it is planned,
   **Then** it depends on `WebhookPublisherPort` and common audit events without
   exposing delivery adapter details inward.

---

### User Story 4 - Preserve migration traceability (Priority: P4)

As a software architect or migration reviewer, I need each migrated issuance
concept and unresolved behavior to be traceable to governance or AS-IS evidence
so that the target system preserves validated behavior without copying legacy
architecture.

**Why this priority**: This foundation starts the first backend source
structure. Reviewers must be able to distinguish target concepts, SRI
contract-only values, migration-only evidence, and unresolved behavior before
task generation.

**Independent Test**: Review the migration classification and pending validation
tables and verify that every legacy concept introduced by this feature has a
classification, decision status, and durable documentation path.

**Acceptance Scenarios**:

1. **Given** an unclear legacy behavior is found during planning, **When** it
   affects this foundation, **Then** it is recorded as Pending Functional
   Validation before affected tasks are generated.
2. **Given** a newly discovered Spanish term lacks an approved English target
   term, **When** it is needed by future work, **Then** it is recorded as a
   Pending Naming Decision before affected planning continues.

### Edge Cases

- A future specification attempts to use an SRI numeric code such as `01` as a
  package, class, method, database, or use case name.
- A future implementation attempts to reuse REST DTOs, persistence entities, or
  SRI XML/SOAP DTOs as domain or application models.
- A duplicate issuance request arrives with the same access key, the same
  issuer/document/establishment/issuing-point/sequence combination, or the same
  external request identifier.
- A retry is requested for an authorized, voided, irrecoverable, or otherwise
  non-retryable document.
- A local voiding request is made for an authorized or already voided document.
- A legacy state name has more than one plausible English meaning.
- A future feature tries to introduce REST resources, persistence adapters, SRI
  adapters, database migrations, queues, webhooks, or bootstrap wiring before a
  dedicated specification covers those boundaries.
- SRI is unavailable, delayed, or returns authorization information after local
  processing has already recorded a terminal document state.

## Requirements *(mandatory)*

### Scope Boundaries

This architecture enabler includes:

- Common domain concepts for electronic tax document issuance.
- Common value objects and identifiers used across issuance features.
- Canonical English document type, document state, authorization state,
  issuance request, issuance result, and issuance mode language.
- Legacy-to-target state mappings and classifications.
- Common state transition, retry eligibility, local voiding, authorized
  immutability, idempotency, and audit requirements.
- Application-layer output port responsibilities needed by future issuance use
  cases.
- Common contracts that future document-specific use cases must follow.
- Testability rules for domain and application behavior without infrastructure.
- Requirements for durable updates to `docs/migration/` when state and concept
  mappings are finalized by later phases.

This architecture enabler excludes:

- REST endpoints, REST DTOs, public API contracts, and legacy route
  compatibility endpoints.
- PostgreSQL persistence adapters, JPA entities, Panache entities, repositories
  backed by a database, and database migrations.
- SRI SOAP clients, SRI XML generation, XML signing, XML parsing, SRI fixture
  files, and SRI adapter implementation.
- Filesystem or object-storage implementation for XML artifacts.
- Queue implementation, webhook delivery implementation, authentication,
  authorization implementation, PDF/RIDE generation, production scheduling,
  production data migration, and legacy system refactoring.
- Invoice-specific, credit-note-specific, debit-note-specific,
  withholding-specific, and waybill-specific tax calculations or line rules.

### Functional Requirements

- **FR-001**: The foundation MUST define a common domain foundation for
  electronic tax documents using approved English canonical terminology.
- **FR-002**: The foundation MUST support the document types `INVOICE`,
  `CREDIT_NOTE`, `DEBIT_NOTE`, `WAYBILL`, and `WITHHOLDING`.
- **FR-003**: The foundation MUST treat SRI numeric document codes as external
  contract mappings and MUST NOT use those codes to drive internal package
  names, class names, method names, use case names, or domain terms.
- **FR-004**: The foundation MUST define `AccessKey` as a value object with a
  49-digit structural validation rule.
- **FR-005**: The foundation MUST define canonical English document states and
  map each legacy Spanish state listed in this specification to a target state
  or a Pending Naming Decision.
- **FR-006**: The foundation MUST separate internal `DocumentState` from
  `AuthorizationState`. Authorization state exists to represent SRI
  authorization lifecycle outcomes without making the SRI contract the internal
  document model.
- **FR-007**: The foundation MUST represent issuer, establishment, issuing
  point, and sequence number concepts using English canonical terminology and
  without Spanish legacy names.
- **FR-008**: The foundation MUST define allowed common state transitions for
  issuance, SRI reception, authorization, rejection, retry processing, terminal
  failure, and local voiding.
- **FR-009**: The foundation MUST prevent authorized documents from being
  modified by future use cases unless a future specification explicitly defines
  a controlled operation and its audit trail.
- **FR-010**: The foundation MUST define retry eligibility for retry candidate
  states `RETURNED`, `REJECTED`, `PENDING`, and `IN_PROGRESS`, and MUST record
  any uncertain retry behavior as Pending Functional Validation before affected
  retry tasks are generated.
- **FR-011**: The foundation MUST model the local voiding rule that authorized
  or already voided documents cannot be locally voided.
- **FR-012**: The application layer MUST define output ports for persistence,
  issuer access policy, sequence number assignment, access key generation, SRI
  authorization, XML storage, queues, webhooks, clock access, transactions, and
  audit logging.
- **FR-013**: Domain and application tests for this foundation MUST run without
  Quarkus, PostgreSQL, Redis, filesystem access, SRI services, queues, webhooks,
  or external HTTP services.
- **FR-014**: Domain and application layers MUST NOT expose REST DTOs,
  persistence entities, SRI XML DTOs, Quarkus types, Hibernate types, Panache
  types, queue types, filesystem types, storage SDK types, HTTP client types, or
  other adapter-specific models.
- **FR-015**: The foundation MUST define idempotency keys for future issuance
  requests. The baseline keys are `accessKey`,
  `issuer + documentType + establishment + issuingPoint + sequenceNumber`, and
  `externalRequestId` when supplied by the caller.
- **FR-016**: The foundation MUST define canonical audit event names for future
  issuance flows: `TaxDocumentIssuanceRequested`,
  `TaxDocumentQueuedForIssuance`, `TaxDocumentXmlGenerated`,
  `TaxDocumentSigned`, `TaxDocumentSubmittedToSri`,
  `TaxDocumentReceivedBySri`, `TaxDocumentAuthorized`,
  `TaxDocumentRejected`, `TaxDocumentAuthorizationRetryRequested`, and
  `TaxDocumentVoided`.
- **FR-017**: Any unclear state name, transition, idempotency behavior, retry
  behavior, asynchronous issuance behavior, route compatibility behavior,
  synchronization scheduling behavior, or authorization behavior MUST be
  registered as Pending Naming Decision or Pending Functional Validation before
  affected task generation.
- **FR-018**: The feature MUST keep AS-IS evidence separate from target
  specifications and MUST require durable governance outputs to be maintained in
  their constitution-aligned locations: AS-IS legacy documentation in
  `docs/legacy/` when relocated from `docs/as-is/`, migration mappings and
  canonical terminology in `docs/migration/`, architecture rules and decisions
  in `docs/architecture/` or `docs/adr/`, and active feature artifacts under
  the active Spec Kit specs folder.
- **FR-019**: The foundation MUST define the common contract that future use
  cases such as `IssueInvoiceUseCase`, `IssueCreditNoteUseCase`,
  `IssueDebitNoteUseCase`, `IssueWithholdingUseCase`,
  `IssueWaybillUseCase`, `RetrySriAuthorizationUseCase`,
  `SynchronizeTaxDocumentsUseCase`, and `DeliverWebhookUseCase` must follow.

### Architectural Requirements

- **AR-001**: Dependencies MUST point inward. Domain rules MUST NOT depend on
  application, adapters, bootstrap, frameworks, persistence, SRI, XML, SOAP,
  queues, storage, filesystems, HTTP clients, or external APIs.
- **AR-002**: Application rules MAY depend on domain concepts and application
  ports, but MUST NOT depend on adapter implementations or framework-specific
  types.
- **AR-003**: Business rules MUST live in domain or application behavior and
  MUST NOT be assigned to REST resources, persistence adapters, SRI adapters,
  storage adapters, queue adapters, webhook adapters, or bootstrap wiring.
- **AR-004**: Future implementation tasks for this feature MAY create source
  artifacts only under:

  ```text
  src/main/java/com/alexastudillo/taxdocument/domain/
  src/main/java/com/alexastudillo/taxdocument/application/
  src/test/java/com/alexastudillo/taxdocument/domain/
  src/test/java/com/alexastudillo/taxdocument/application/
  ```

- **AR-005**: Future implementation tasks for this feature MUST NOT create
  source artifacts under `adapter.in.rest`, `adapter.out.persistence`,
  `adapter.out.sri`, `adapter.out.storage`, `adapter.out.queue`,
  `adapter.out.webhook`, or `bootstrap`.
- **AR-006**: Future implementation tasks for this feature MUST NOT create REST
  resources, REST DTOs, persistence entities, database migrations, SRI
  XML/SOAP DTOs, SRI clients, queue adapters, webhook adapters, or Quarkus
  bootstrap wiring.
- **AR-007**: Generic business classes such as `DocumentService`, `SriService`,
  `ProcessService`, `Manager`, `Helper`, and `Util` MUST NOT be introduced for
  issuance behavior.
- **AR-008**: Ports MUST be owned by the application layer. Adapter details,
  external DTOs, persistence entities, SRI XML structures, and framework types
  MUST NOT leak into domain or application signatures.
- **AR-009**: This feature MUST preserve the prior governance feature
  `001-canonical-terminology-boundaries` as the terminology and boundary
  baseline and MUST NOT redefine competing architecture rules.

### Naming and Migration Requirements

- **NR-001**: Target packages, classes, methods, fields, events, tests, and
  documentation introduced by this feature MUST use English canonical
  terminology.
- **NR-002**: Spanish legacy names MUST NOT appear in target domain or
  application artifacts. Official SRI Spanish names MAY appear only in SRI
  adapter, fixture, compatibility, migration, or mapping artifacts, none of
  which are implemented by this feature.
- **NR-003**: Canonical terms MUST follow the artifact-specific rendering rules
  from `docs/architecture/canonical-terminology.md`.
- **NR-004**: SRI document type codes `01`, `04`, `05`, `06`, and `07` MUST be
  documented as external contract mappings, not target domain names.
- **NR-005**: The accepted state mapping from this specification MUST be added
  to `docs/migration/legacy-to-target-terminology.md` during the planning or
  task execution phases before the feature is considered complete.
- **NR-006**: `ANULADO` is accepted as `VOIDED` for this foundation because the
  AS-IS behavior is a local voiding operation. If a future SRI cancellation or
  annulment process is validated, it MUST be modeled as a separate behavior
  rather than renaming this state silently.
- **NR-007**: New unclear legacy terms discovered during planning MUST be
  recorded as Pending Naming Decisions in `docs/migration/legacy-to-target-terminology.md`.

### Key Entities

- **TaxDocument**: The common tax document aggregate for issuance lifecycle
  behavior. It owns the document type, issuer, establishment, issuing point,
  sequence number, access key, issue date, document state, authorization state,
  authorization number when present, authorization timestamp when present, and
  common invariants.
- **DocumentType**: The canonical document type model for `INVOICE`,
  `CREDIT_NOTE`, `DEBIT_NOTE`, `WAYBILL`, and `WITHHOLDING`.
- **DocumentState**: The internal state of a tax document in the target system.
  It is separate from SRI authorization state and owns common transition rules.
- **AuthorizationState**: The authorization lifecycle state reported or derived
  from SRI interactions, represented internally without SRI XML or SOAP models.
- **Issuer**: The legal tax document issuer concept used for access policy,
  issuance, sequence ownership, and audit traceability.
- **Establishment**: The issuer establishment involved in document numbering
  and issuance identity.
- **IssuingPoint**: The issuing point within an establishment involved in
  sequence assignment and document identity.
- **SequenceNumber**: The sequence assigned for an issuer, establishment,
  issuing point, and document type.
- **AccessKey**: The 49-digit tax document access key value object.
- **AuthorizationNumber**: The authorization identifier assigned when a document
  is authorized.
- **IssueDate**: The date on which the tax document is issued.
- **AuthorizedAt**: The timestamp at which authorization is received.
- **IssuanceRequest**: The common application input model for future issuance
  use cases. It captures the document type, issuer context, idempotency
  identifiers, issuance mode, and common request metadata without REST DTOs.
- **IssuanceResult**: The common application result model for future issuance
  use cases. It captures the access key, document state, authorization state,
  queued or completed outcome, audit correlation, and errors without adapter
  models.
- **IssuanceMode**: The canonical mode concept for synchronous or asynchronous
  issuance intent. Runtime behavior and default mode remain subject to Pending
  Functional Validation for affected future tasks.

### Application Ports

The following ports MUST be defined by the application layer when future
implementation tasks are generated for this foundation:

| Port | Responsibility |
|------|----------------|
| `TaxDocumentRepository` | Save, load, and query tax documents through application terms; detect duplicates by access key and issuance identity without leaking persistence entities. |
| `IssuerAccessPolicyPort` | Validate whether the current actor or context may issue or inspect documents for an issuer, establishment, or issuing point. |
| `SequenceNumberPort` | Reserve, assign, and validate sequence numbers idempotently for an issuer, establishment, issuing point, and document type. |
| `AccessKeyGeneratorPort` | Generate access keys from approved target inputs and return the common `AccessKey` value object. |
| `SriAuthorizationPort` | Represent SRI reception, authorization, retry, and synchronization interactions as application-level operations without SRI XML, SOAP, or HTTP types. |
| `XmlStoragePort` | Store and retrieve XML artifacts by application-level identifiers without filesystem, storage SDK, or path details leaking inward. |
| `TaxDocumentQueuePort` | Request asynchronous issuance work through application terms without queue implementation types. |
| `WebhookPublisherPort` | Publish canonical tax document events for future webhook delivery without remote HTTP delivery details. |
| `ClockPort` | Provide current date and time for issue dates, audit timestamps, and deterministic tests. |
| `TransactionPort` | Provide application transaction boundaries without exposing persistence framework types. |
| `AuditLogPort` | Append audit events and metadata without secrets, credentials, private keys, tokens, signing passwords, or sensitive configuration values. |

### Future Use Case Contract

Future document-specific issuance use cases MUST follow the common contract
below:

1. Accept application commands or queries, not REST DTOs.
2. Validate issuer access through `IssuerAccessPolicyPort` when issuer-scoped
   behavior is involved.
3. Apply idempotency checks before sequence assignment, SRI submission, queue
   publication, XML generation, or webhook publication.
4. Reserve or assign sequence numbers through `SequenceNumberPort`.
5. Generate access keys through `AccessKeyGeneratorPort`.
6. Persist common document state through `TaxDocumentRepository`.
7. Use `TransactionPort` for atomic application-level operations.
8. Use `ClockPort` for time-dependent decisions.
9. Emit audit events through `AuditLogPort`.
10. Interact with SRI, XML storage, queues, and webhooks only through their
    application ports.
11. Return application results, not persistence entities, REST DTOs, SRI DTOs,
    queue job models, or framework-specific results.

### State and Transition Rules

The common `DocumentState` model MUST include these canonical states:

| State | Meaning |
|-------|---------|
| `PENDING` | The document has been requested or created but processing has not completed. |
| `IN_PROGRESS` | The document is actively being processed, submitted, retried, or synchronized. |
| `RECEIVED` | SRI reception has accepted or received the document and authorization is pending or being checked. |
| `AUTHORIZED` | The document has been authorized and is immutable by default. |
| `NOT_AUTHORIZED` | SRI authorization completed without authorization. |
| `RETURNED` | SRI reception or validation returned the document for correction or retry analysis. |
| `REJECTED` | The document was rejected and may be eligible for retry only when retry rules allow it. |
| `IRRECOVERABLE` | The document has reached a terminal failure that future retry rules must not automatically retry. |
| `VOIDED` | The document was locally voided before authorization under the common voiding rule. |

The common `AuthorizationState` model MUST be separate from `DocumentState` and
MUST include at least:

| State | Meaning |
|-------|---------|
| `NOT_SUBMITTED` | No SRI reception submission has been made or recorded. |
| `SUBMITTED` | Submission to SRI has been requested or sent. |
| `RECEIVED` | SRI has received the document and authorization is pending or being checked. |
| `AUTHORIZED` | SRI has authorized the document. |
| `NOT_AUTHORIZED` | SRI has not authorized the document. |
| `RETURNED` | SRI returned the document during reception or authorization processing. |
| `REJECTED` | SRI rejected the document during reception or authorization processing. |

Common state transitions MUST be constrained as follows:

| From | To | Trigger |
|------|----|---------|
| `PENDING` | `IN_PROGRESS` | Issuance processing, retry processing, or synchronization starts. |
| `IN_PROGRESS` | `RECEIVED` | SRI reception confirms the document was received. |
| `IN_PROGRESS` | `RETURNED` | SRI reception or validation returns the document. |
| `IN_PROGRESS` | `REJECTED` | Processing confirms rejection before authorization. |
| `RECEIVED` | `AUTHORIZED` | Authorization confirms the document is authorized. |
| `RECEIVED` | `NOT_AUTHORIZED` | Authorization confirms the document is not authorized. |
| `RECEIVED` | `REJECTED` | Authorization or synchronization confirms rejection. |
| `RETURNED` | `IN_PROGRESS` | Retry processing is allowed and starts. |
| `REJECTED` | `IN_PROGRESS` | Retry processing is allowed and starts. |
| `PENDING` | `VOIDED` | Local voiding is allowed before authorization. |
| `IN_PROGRESS` | `VOIDED` | Local voiding is allowed only when no authorization has occurred and future rules permit it. |
| `RETURNED` | `VOIDED` | Local voiding is allowed when retry is not desired and no authorization has occurred. |
| `REJECTED` | `VOIDED` | Local voiding is allowed when no authorization has occurred and future rules permit it. |
| `NOT_AUTHORIZED` | `VOIDED` | Local voiding is allowed only when future rules confirm it is not an SRI cancellation process. |
| `RETURNED` | `IRRECOVERABLE` | Future rules mark the document as terminally unrecoverable. |
| `REJECTED` | `IRRECOVERABLE` | Future rules mark the document as terminally unrecoverable. |
| `NOT_AUTHORIZED` | `IRRECOVERABLE` | Future rules mark the document as terminally unrecoverable. |

`AUTHORIZED`, `VOIDED`, and `IRRECOVERABLE` are terminal for this foundation
unless a future specification defines a controlled operation with explicit
requirements, idempotency, and audit events. Local voiding MUST reject
`AUTHORIZED` and `VOIDED` documents.

## Migration Classification *(mandatory for migrated concepts)*

### Common Concept Mapping

| Legacy Concept | Target Name | Classification | Decision Status |
|----------------|-------------|----------------|-----------------|
| comprobante | `TaxDocument` / `taxDocument` | Target domain concept | Decided |
| factura | `INVOICE` / `invoice` | Target domain concept | Decided |
| nota credito | `CREDIT_NOTE` / `creditNote` | Target domain concept | Decided |
| nota debito | `DEBIT_NOTE` / `debitNote` | Target domain concept | Decided |
| retencion | `WITHHOLDING` / `withholding` | Target domain concept | Decided |
| guia remision | `WAYBILL` / `waybill` | Target domain concept | Decided |
| emisor | `Issuer` / `issuer` | Target domain concept | Decided |
| establecimiento | `Establishment` / `establishment` | Target domain concept | Decided |
| punto emision | `IssuingPoint` / `issuingPoint` | Target domain concept | Decided |
| secuencial | `SequenceNumber` / `sequenceNumber` | Target domain concept | Decided |
| clave acceso | `AccessKey` / `accessKey` | Target domain concept | Decided |
| numero autorizacion | `AuthorizationNumber` / `authorizationNumber` | Target domain concept | Decided |
| fecha emision | `IssueDate` / `issueDate` | Target domain concept | Decided |
| fecha autorizacion | `AuthorizedAt` / `authorizedAt` | Target domain concept | Decided |

### SRI Document Code Mapping

| SRI Contract Code | Target Document Type | Classification | Decision Status |
|-------------------|----------------------|----------------|-----------------|
| `01` | `INVOICE` | SRI adapter-only concept | Decided |
| `04` | `CREDIT_NOTE` | SRI adapter-only concept | Decided |
| `05` | `DEBIT_NOTE` | SRI adapter-only concept | Decided |
| `06` | `WAYBILL` | SRI adapter-only concept | Decided |
| `07` | `WITHHOLDING` | SRI adapter-only concept | Decided |

### Legacy State Mapping

| Legacy State | Target State | Classification | Decision Status |
|--------------|--------------|----------------|-----------------|
| `PENDIENTE` | `PENDING` | Target domain concept | Decided |
| `EN_PROCESO` | `IN_PROGRESS` | Target domain concept | Decided |
| `RECIBIDO` | `RECEIVED` | Target domain concept | Decided |
| `AUTORIZADO` | `AUTHORIZED` | Target domain concept | Decided |
| `NO_AUTORIZADO` | `NOT_AUTHORIZED` | Target domain concept | Decided |
| `DEVUELTA` | `RETURNED` | Target domain concept | Decided |
| `RECHAZADO` | `REJECTED` | Target domain concept | Decided |
| `IRRECUPERABLE` | `IRRECOVERABLE` | Target domain concept | Decided |
| `ANULADO` | `VOIDED` | Target domain concept | Decided |

### Pending Naming Decisions

Current status: no Pending Naming Decisions are open for the required concepts
or states in this foundation. Newly discovered unresolved Spanish terms MUST be
registered in `docs/migration/legacy-to-target-terminology.md` before affected
planning continues.

### Pending Functional Validations

| ID | Area | Question | Handling |
|----|------|----------|----------|
| PFV-ISS-001 | Issuance mode | Should target issuance be asynchronous-only, synchronous-only, or support both modes? | Model `IssuanceMode` as a common concept now. Defer runtime behavior and default mode to a future issuance or queue specification. |
| PFV-ISS-002 | Route compatibility | Should legacy route shapes be preserved through compatibility endpoints? | Out of scope for this feature. Defer to a REST API or legacy compatibility specification. |
| PFV-ISS-003 | Synchronization scheduling | Is synchronization manual only, externally scheduled, or internally scheduled? | Out of scope for this feature. Defer to a synchronization specification. |
| PFV-ISS-004 | Retry policy | Are all candidate retry states `RETURNED`, `REJECTED`, `PENDING`, and `IN_PROGRESS` valid in the target system, and what signed-XML precondition must be required? | Define candidate retry states now from AS-IS evidence. Require validation before implementing `RetrySriAuthorizationUseCase`. |
| PFV-ISS-005 | Post-authorization corrections | Are post-authorization changes always handled by separate tax documents such as credit notes, or are any controlled metadata updates allowed? | Treat authorized documents as immutable now. Require a future specification before any exception. |

## Idempotency and Audit Requirements *(include if feature is critical)*

**Idempotency Scope**: This foundation covers common idempotency rules for tax
document issuance, SRI authorization retries, synchronization runs, webhook
delivery, XML generation, and sequence assignment. It defines keys and required
behavior only; it does not implement storage, queues, SRI calls, or webhook
delivery.

**Required Idempotency Keys**:

- `accessKey`
- `issuer + documentType + establishment + issuingPoint + sequenceNumber`
- `externalRequestId` when supplied by the caller
- Retry correlation identifier for future authorization retry requests
- Synchronization run identifier for future synchronization features
- Webhook delivery attempt identifier for future webhook features

**Required Behavior**:

- Repeated issuance requests with the same accepted idempotency key MUST return
  the previously established application result or a conflict that explains the
  mismatch without creating a second tax document.
- Sequence assignment MUST be idempotent for the same issuer, establishment,
  issuing point, document type, and sequence number.
- XML generation MUST be idempotent for the same tax document version and
  access key.
- Authorization retry, synchronization, and webhook delivery MUST define their
  own idempotency keys in their future specifications before implementation.

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

**Sensitive Data Exclusions**: Audit logs MUST NOT contain secrets, private
keys, credentials, tokens, signing passwords, certificates, raw private key
material, or sensitive configuration values.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of required common domain concepts listed in this
  specification are defined and classified before task generation.
- **SC-002**: All 5 required document types are represented by canonical
  English names and mapped to SRI numeric codes as external contract values.
- **SC-003**: All 9 required legacy states have canonical English target states
  or a documented Pending Naming Decision.
- **SC-004**: All 11 required application ports have explicit responsibilities
  and no adapter implementation responsibility.
- **SC-005**: Future task generation for this feature contains 0 REST
  endpoints, 0 persistence adapters, 0 SRI adapters, 0 database migrations, and
  0 bootstrap wiring tasks.
- **SC-006**: Domain and application test expectations require 0 external
  infrastructure services.
- **SC-007**: 100% of unresolved naming or behavior questions introduced by
  this feature are registered as Pending Naming Decisions or Pending Functional
  Validations with handling instructions.
- **SC-008**: The idempotency foundation covers issuance requests, SRI
  authorization retries, synchronization runs, webhook delivery, XML generation,
  and sequence assignment.
- **SC-009**: The audit foundation includes all 10 required canonical audit
  event names and excludes sensitive data.
- **SC-010**: The future use case contract covers all 8 named future use cases
  without introducing generic business service names.

### Success Criteria Traceability

| Success Criterion | Requirement Trace |
|-------------------|-------------------|
| `SC-001` | `FR-001`, Key Entities, Migration Classification |
| `SC-002` | `FR-002`, `FR-003`, SRI Document Code Mapping |
| `SC-003` | `FR-005`, Legacy State Mapping |
| `SC-004` | `FR-012`, Application Ports |
| `SC-005` | Scope Boundaries, `AR-004`, `AR-005`, `AR-006` |
| `SC-006` | `FR-013`, `FR-014`, `AR-001`, `AR-002` |
| `SC-007` | `FR-017`, Pending Naming Decisions, Pending Functional Validations |
| `SC-008` | `FR-015`, Idempotency and Audit Requirements |
| `SC-009` | `FR-016`, Idempotency and Audit Requirements |
| `SC-010` | `FR-019`, Future Use Case Contract, `AR-007` |

## Assumptions

- Feature `001-canonical-terminology-boundaries` is the approved governance
  baseline for terminology, SRI isolation, DTO separation, and Clean
  Architecture boundaries.
- AS-IS documentation currently lives under `docs/as-is/`; if it is relocated,
  `docs/legacy/` becomes the durable AS-IS source location required by the
  constitution.
- The target active Spec Kit specs folder in this repository is `specs/`, and
  this feature lives at `specs/002-tax-document-issuance-foundation/`.
- `VOIDED` is accepted for `ANULADO` because the validated legacy behavior is a
  local voiding operation, not a target SRI cancellation workflow.
- SRI document codes are required for interoperability but are external
  contract values and not internal target names.
- This `/speckit-specify` phase creates the specification and quality checklist
  only. Planning, design artifacts, tasks, durable documentation updates, and
  any domain/application source work belong to later Spec Kit phases.
- The legacy AS-IS evidence used by this feature includes
  `docs/as-is/05-business-rules.md`, `docs/as-is/06-validation-rules.md`,
  `docs/as-is/07-process-flows.md`, and
  `docs/as-is/15-sdd-migration-backlog.md`.
