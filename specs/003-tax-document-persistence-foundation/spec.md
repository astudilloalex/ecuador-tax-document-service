# Feature Specification: Tax Document Persistence Foundation

**Feature Branch**: `5-ft-3`

**Created**: 2026-07-05

**Status**: Draft

**Input**: User description: "Establish the persistence foundation for the
common tax document issuance model using PostgreSQL, Flyway, and Clean
Architecture outbound adapters, without creating REST endpoints, SRI adapters,
XML generation, queue adapters, webhook delivery, or document-specific issuance
flows."

## Clarifications

### Session 2026-07-05

- Q: How should duplicate `accessKey` or duplicate issuance identity saves behave? → A: Reject duplicate `accessKey` or issuance identity saves with an application-facing duplicate conflict error.
- Q: How should an exact repeated sequence reservation behave? → A: Exact repeated sequence reservation is idempotent and returns the existing `SequenceNumber`.
- Q: What value should `tax_documents.document_type` store? → A: Store canonical document type values such as `INVOICE`, `CREDIT_NOTE`, and `WITHHOLDING`.
- Q: How should invalid persisted authorization combinations be handled during rehydration? → A: Reject invalid persisted authorization combinations with an application-facing data integrity error.
- Q: What does repository `save(TaxDocument)` mean? → A: `save` may create a new persisted document or update the same persisted aggregate, but must never silently overwrite another document with duplicate `accessKey` or duplicate issuance identity.
- Q: What must missing repository lookups return? → A: `findByAccessKey` and `findByIssuanceIdentity` return empty results, while `existsByAccessKey` and `existsByIssuanceIdentity` return `false`.
- Q: What temporal persistence rules apply? → A: `issue_date` uses the domain issue date as a calendar date, and `authorized_at` uses an instant/timestamp normalized to UTC with microsecond-level rehydration precision.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Persist and Load Tax Documents (Priority: P1)

As a backend developer building future issuance use cases, I need the common
tax document model to be saved and loaded through the existing application
repository port so that document-specific features can reuse one persistence
foundation without depending on database details.

**Why this priority**: Future invoice, credit note, debit note, withholding,
waybill, retry, synchronization, and webhook features cannot store issuance
state until the common tax document aggregate can be persisted and rehydrated
through Clean Architecture boundaries.

**Independent Test**: Create a tax document with the common issuance fields,
save it through the application repository contract, load it by access key, and
verify that the loaded model preserves the same domain identity, state, and
authorization information without exposing persistence entities.

**Acceptance Scenarios**:

1. **Given** a valid common tax document exists in memory, **When** it is saved
   through `TaxDocumentRepository`, **Then** it is persisted using English
   target database objects and can be loaded back by `AccessKey`.
2. **Given** a persisted authorized tax document exists, **When** it is loaded
   through `TaxDocumentRepository`, **Then** the resulting domain object
   preserves `accessKey`, `documentState`, `authorizationState`,
   `authorizationNumber`, and `authorizedAt`.
3. **Given** application code calls repository methods, **When** method
   signatures are reviewed, **Then** they expose only domain or application
   models and never persistence entities or database-specific types.
4. **Given** no persisted tax document exists for an access key or issuance
   identity, **When** repository lookup or existence operations are used,
   **Then** find operations return an empty result and existence operations
   return `false`.

---

### User Story 2 - Protect Issuance Identity and Access Key Uniqueness (Priority: P2)

As a backend developer building issuance workflows, I need duplicate access
keys and duplicate issuance identities to be detected consistently so that
future features do not issue or store conflicting tax documents.

**Why this priority**: Access key and issuance identity uniqueness are core
idempotency and audit requirements for electronic tax documents. They prevent
duplicate legal document numbers and make retries predictable.

**Independent Test**: Persist one tax document, attempt to save another with
the same access key, and attempt to save another with the same issuer,
document type, establishment, issuing point, and sequence number. Verify that
the persistence foundation rejects each duplicate with an application-facing
duplicate conflict error.

**Acceptance Scenarios**:

1. **Given** a tax document already exists for an `accessKey`, **When** another
   tax document with the same `accessKey` is saved, **Then** the persistence
   foundation rejects the duplicate with an application-facing duplicate
   conflict error.
2. **Given** a tax document already exists for an issuer, document type,
   establishment, issuing point, and sequence number, **When** another tax
   document with the same issuance identity is saved, **Then** the persistence
   foundation rejects the duplicate with an application-facing duplicate
   conflict error.
3. **Given** duplicate checks are performed before a save, **When** the
   application asks whether an access key or issuance identity exists, **Then**
   the result is accurate for persisted documents.

---

### User Story 3 - Reserve Sequence Numbers Safely (Priority: P3)

As a backend developer building future issuance flows, I need sequence numbers
to be reserved through the application sequence port so that document numbers
remain unique per issuer, establishment, issuing point, and document type.

**Why this priority**: Sequence reservation controls document numbering. It
must be safe before future issuance use cases rely on it for legal document
identity.

**Independent Test**: Reserve a requested sequence value for an issuer context,
verify that the same value cannot be reserved twice as a conflicting document
number, and verify that availability checks reflect reservations.

**Acceptance Scenarios**:

1. **Given** a sequence number has not been reserved for an issuer context,
   **When** it is reserved through `SequenceNumberPort`, **Then** the
   reservation is recorded and returned as a domain `SequenceNumber`.
2. **Given** a sequence number is already reserved for the same issuer,
   establishment, issuing point, document type, and sequence value, **When**
   the same value is requested again for the same reservation identity, **Then**
   the adapter returns the existing domain `SequenceNumber` idempotently.
3. **Given** application code checks sequence availability, **When** the value
   is already reserved, **Then** the port reports that the value is not
   available for a conflicting reservation.

---

### User Story 4 - Preserve Architecture and Migration Traceability (Priority: P4)

As a software architect or migration reviewer, I need persistence artifacts,
table names, column names, and mappings to remain traceable to the approved
English canonical terminology so that the target system does not copy legacy
database naming or architecture.

**Why this priority**: Persistence is the first outward adapter boundary for
the issuance model. If database entities or names leak inward, future features
will inherit legacy coupling and violate the project constitution.

**Independent Test**: Review the persistence specification and future source
layout to verify that persistence-specific artifacts are limited to the
approved outbound adapter and configuration locations, target database objects
use English lowercase snake_case names, and legacy-to-target mapping
documentation is updated.

**Acceptance Scenarios**:

1. **Given** persistence source files are created, **When** their paths are
   reviewed, **Then** persistence-specific code exists only under the outbound
   persistence adapter or approved configuration locations.
2. **Given** target schema artifacts are reviewed, **When** table and column
   names are inspected, **Then** they use English lowercase snake_case and do
   not copy Spanish legacy names.
3. **Given** a migrated persistence concept is introduced, **When** migration
   documentation is reviewed, **Then** the concept has a target name,
   classification, and decision status in durable documentation.

### Edge Cases

- A persisted authorized document must be rehydrated without running the
  new-document constructor path that always starts in `PENDING`.
- A persisted document has `authorizationNumber` without an authorized document
  state or authorization state; rehydration must reject it with an
  application-facing data integrity error.
- A persisted document has `authorizedAt` without an authorization number;
  rehydration must reject it with an application-facing data integrity error.
- A duplicate access key is detected during save after an earlier existence
  check returned false.
- A duplicate issuance identity is detected during save after an earlier
  existence check returned false.
- Two sequence reservations request the same issuer, establishment, issuing
  point, document type, and sequence number.
- An exact repeated sequence reservation is requested for an already reserved
  issuer, document type, establishment, issuing point, and sequence value.
- A sequence reservation collides with an existing reservation that does not
  represent the same reservation identity.
- A persisted document contains an unknown canonical document type, document
  state, authorization state, or issuance mode value.
- A persisted document has missing or inconsistent issuer, establishment, or
  issuing point relationships.
- A future task attempts to place persistence entities in the domain or
  application layer.
- A future task attempts to create REST, SRI, XML generation, queue, webhook,
  or document-specific issuance behavior in this feature.
- A legacy table or column name is proposed for the target schema.
- XML path or audit-event storage is requested before its persistence ownership
  is validated.

## Requirements *(mandatory)*

### Scope Boundaries

This architecture and infrastructure enabler includes:

- Target persistence schema requirements for the common tax document issuance
  foundation.
- Versioned schema migration requirements for approved target tables.
- Outbound persistence adapter requirements for `TaxDocumentRepository`,
  `SequenceNumberPort`, and `TransactionPort`.
- Mapping requirements between persistence records and domain objects.
- Domain-safe rehydration requirements for persisted `TaxDocument` instances.
- Uniqueness and idempotency requirements for `accessKey`, issuance identity,
  and sequence reservation.
- Persistence adapter test requirements for mapping, repository behavior,
  duplicate constraints, sequence reservation, and transaction boundaries.
- Durable migration documentation requirements for target table and column
  mappings.

This architecture and infrastructure enabler excludes:

- REST endpoints, REST DTOs, public API contracts, and legacy route
  compatibility endpoints.
- SRI SOAP/XML adapters, SRI request or response DTOs, XML generation, XML
  signing, XML parsing, SRI fixture files, and SRI authorization behavior.
- XML storage adapter implementation and historical XML path persistence unless
  a later plan explicitly validates and scopes it.
- Queue adapters, webhook adapters, webhook delivery behavior, authentication,
  authorization, PDF/RIDE generation, production scheduling, production data
  migration, and legacy system refactoring.
- Invoice-specific, credit-note-specific, debit-note-specific,
  withholding-specific, and waybill-specific line, tax, total, or business
  calculation rules.

### Functional Requirements

- **FR-001**: The feature MUST define a target persistence schema for the
  common tax document issuance foundation using English lowercase snake_case
  table and column names.
- **FR-002**: The feature MUST manage target persistence schema changes through
  versioned migration artifacts.
- **FR-003**: The target persistence schema MUST include, at minimum, the
  tables `issuers`, `establishments`, `issuing_points`,
  `issuance_sequences`, and `tax_documents`.
- **FR-004**: The feature MUST keep persistence entities, repository
  implementation details, mapping details, database constraints, and
  database-specific errors inside the outbound persistence adapter or approved
  configuration locations.
- **FR-005**: The feature MUST implement the application-facing
  `TaxDocumentRepository` contract so future use cases can save a
  `TaxDocument`, load by `AccessKey`, load by issuance identity, check
  existence by `AccessKey`, and check existence by issuance identity. `save`
  MAY create a new persisted document when no matching record exists and MAY
  update the same persisted aggregate when the same tax document identity
  already exists. `save` MUST NOT silently overwrite a different persisted tax
  document with a duplicate `accessKey` or duplicate issuance identity.
  Duplicate `accessKey` and duplicate issuance identity conditions MUST be
  translated into application-facing duplicate conflict errors. Repository
  missing-record behavior MUST be explicit:
  `findByAccessKey(accessKey)` and `findByIssuanceIdentity(identity)` MUST
  return an empty optional/result when no persisted tax document exists, and
  `existsByAccessKey(accessKey)` and `existsByIssuanceIdentity(identity)` MUST
  return `false` when no persisted tax document exists.
- **FR-006**: The feature MUST support rehydrating a persisted `TaxDocument`
  with persisted `DocumentState`, `AuthorizationState`, optional
  `AuthorizationNumber`, optional `AuthorizedAt`, `IssueDate`,
  `IssuanceMode`, and optional `externalRequestId`, while preserving
  `AccessKey`, `DocumentType`, `Issuer`, `Establishment`, `IssuingPoint`, and
  `SequenceNumber`.
- **FR-007**: The rehydration mechanism MUST preserve domain invariants, MUST
  allow persisted states other than `PENDING`, MUST NOT expose persistence
  entities to the domain, and MUST NOT add framework annotations to domain
  classes.
- **FR-008**: The rehydration mechanism MUST reject invalid persisted
  authorization combinations with an application-facing data integrity error,
  including authorization number or authorization timestamp combinations that
  are inconsistent with `DocumentState.AUTHORIZED` and
  `AuthorizationState.AUTHORIZED`. It MUST reject: authorization number present
  when authorization state is not `AUTHORIZED`, authorized timestamp present
  without authorization number, authorized state without authorization number,
  authorized state without authorized timestamp, unknown canonical document
  type, unknown document state, and unknown authorization state.
- **FR-009**: The persistence foundation MUST enforce uniqueness for
  `accessKey` and MUST reject duplicate `accessKey` saves with an
  application-facing duplicate conflict error.
- **FR-010**: The persistence foundation MUST enforce uniqueness for the
  issuance identity composed of issuer, document type, establishment, issuing
  point, and sequence number, and MUST reject duplicate issuance identity saves
  with an application-facing duplicate conflict error.
- **FR-011**: The persistence foundation MUST implement sequence reservation
  through `SequenceNumberPort` for the issuer, establishment, issuing point,
  document type, and requested sequence value.
- **FR-012**: Sequence reservation MUST prevent conflicting duplicate
  reservations. An exact repeated reservation for the same issuer,
  establishment, issuing point, document type, and sequence value MUST be
  idempotent and return the existing domain `SequenceNumber`. A conflicting
  duplicate sequence reservation MUST fail with an application-facing conflict
  error. Database uniqueness and transaction behavior MUST be part of the
  reliability guarantee; application-only checks are insufficient.
- **FR-013**: `tax_documents.document_type` and `issuance_sequences.document_type`
  MUST store canonical `DocumentType` values such as `INVOICE`, `CREDIT_NOTE`,
  `DEBIT_NOTE`, `WAYBILL`, and `WITHHOLDING`. Target persistence tables MUST
  NOT store SRI numeric document codes as the internal document type value.
- **FR-014**: The persistence foundation MUST implement transaction boundaries
  through `TransactionPort` or an equivalent application-facing transaction
  abstraction without exposing persistence transaction types to domain or
  application code. Persistence adapters MUST translate database and framework
  failures into stable application-facing error categories, including duplicate
  access key conflict, duplicate issuance identity conflict, duplicate or
  unavailable sequence reservation conflict, invalid persisted tax document
  state, invalid persistence relationship, generic persistence failure, and
  transaction failure. Contracts outside `adapter.out.persistence` MUST NOT
  expose `SQLException`, `PersistenceException`, `ConstraintViolationException`,
  Hibernate exceptions, Panache exceptions, PostgreSQL-specific exceptions, or
  equivalent persistence-specific types.
- **FR-015**: The persistence foundation MUST update durable migration
  documentation with legacy-to-target table and column mappings for all
  database objects introduced by this feature.
- **FR-016**: Domain and application layers MUST NOT depend on JPA, Hibernate,
  Panache, PostgreSQL, Flyway, JDBC, SQL, Quarkus persistence APIs, or
  persistence annotations.
- **FR-017**: Repository methods and sequence methods MUST expose only
  domain/application models or primitive values already accepted by the
  application port contracts, never persistence entities or database-specific
  result types.
- **FR-018**: Persistence adapter tests MUST verify tax document mapping,
  rehydration, duplicate access key handling, duplicate issuance identity
  handling, invalid persisted authorization data handling, sequence reservation
  behavior, sequence availability behavior, and transaction behavior.
- **FR-019**: The feature MUST NOT create REST resources, REST DTOs, public API
  endpoints, SRI adapters, SRI XML/SOAP DTOs, XML generation, XML signing, XML
  storage adapters, queue adapters, webhook adapters, or document-specific
  issuance flows.
- **FR-020**: The optional `tax_document_audit_events` table MUST NOT be added
  unless the plan explicitly justifies why audit persistence belongs in this
  foundation instead of a future audit adapter specification.
- **FR-021**: Temporal persistence rules MUST be explicit and measurable:
  `issue_date` represents the existing domain `IssueDate` as a database
  `date` without timezone conversion, while `authorized_at` represents the
  existing domain `AuthorizedAt` as a database timestamp normalized to UTC.
  Rehydration tests MUST compare `authorized_at` values at microsecond
  precision or the database-supported precision selected by the plan, whichever
  is lower.
- **FR-022**: Target schema documentation MUST define primary keys, foreign
  keys, unique constraints, important indexes, relationship rules, and
  delete/update restrictions for `issuers`, `establishments`,
  `issuing_points`, `issuance_sequences`, and `tax_documents`.

### Architectural Requirements

- **AR-001**: Persistence-specific source artifacts MUST be limited to
  `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/` and
  approved persistence configuration locations.
- **AR-002**: Persistence tests MUST be limited to persistence adapter test
  locations and MUST NOT require domain or application tests to start the
  runtime framework or external infrastructure.
- **AR-003**: Source dependencies MUST preserve Clean Architecture direction:
  persistence adapters may depend on application ports and domain models;
  domain and application code MUST NOT depend on persistence adapters.
- **AR-004**: Business rules MUST remain in the domain or application layers.
  Persistence adapters MUST NOT decide issuance policy, authorization policy,
  document-specific calculations, retry eligibility, or SRI behavior.
- **AR-005**: Persistence mappers MUST translate between domain objects and
  persistence entities without reusing either model across the boundary.
- **AR-006**: Persistence errors MUST be translated to application-facing
  outcomes or exceptions that do not expose database-specific types beyond the
  adapter boundary.
- **AR-007**: Transaction handling MUST be available to application
  orchestration through `TransactionPort` and MUST NOT require use cases to
  import persistence transaction objects.
- **AR-008**: SRI document codes MUST remain external contract values mapped
  from canonical `DocumentType`; they MUST NOT be stored as the internal value
  in target persistence tables and MUST NOT drive table names, column names,
  class names, or internal model names.
- **AR-009**: The feature MUST preserve DTO separation: persistence entities
  belong only to the persistence adapter, domain objects remain persistence
  ignorant, and application ports return domain/application models only.
- **AR-010**: The feature MUST remain a persistence foundation and MUST NOT
  introduce inbound REST, outbound SRI, outbound storage, outbound queue,
  outbound webhook, or bootstrap runtime behavior beyond approved persistence
  configuration.

### Naming and Migration Requirements

- **NR-001**: Target database tables and columns MUST use approved English
  canonical terminology and lowercase snake_case.
- **NR-002**: Spanish legacy names MUST NOT appear in target persistence
  entities, target table names, target column names, target constraints, target
  indexes, application code, domain code, or tests except inside migration
  mapping documentation or explicitly approved compatibility artifacts.
- **NR-003**: Required target table names are `issuers`, `establishments`,
  `issuing_points`, `issuance_sequences`, and `tax_documents`.
- **NR-004**: Required target persistence field names include, as applicable,
  `issuer_id`, `establishment_id`, `issuing_point_id`, `document_type`,
  `sequence_number`, `access_key`, `issue_date`, `document_state`,
  `authorization_state`, `authorization_number`, `authorized_at`,
  `issuance_mode`, and `external_request_id`. `document_type` stores canonical
  document type values, not SRI numeric codes.
- **NR-005**: Every database object introduced by this feature MUST be
  classified as a Target database object in `docs/migration/`.
- **NR-006**: Unclear table names, column names, relationship names, or
  constraint semantics MUST be registered as Pending Naming Decision or Pending
  Functional Validation before affected task generation.
- **NR-007**: Legacy compatibility views for Spanish table names are out of
  scope for this feature unless a later compatibility specification approves a
  bounded exception.
- **NR-008**: Target temporal columns MUST use English canonical names:
  `issue_date` for the persisted issue date and `authorized_at` for the
  persisted authorization timestamp.

### Key Entities *(include if feature involves data)*

- **Issuer persistence record**: Target database representation of the issuer
  needed for common issuance identity and relationship ownership. It maps to
  the domain `Issuer` concept without making `Issuer` a persistence entity.
- **Establishment persistence record**: Target database representation of an
  issuer establishment. It maps to the domain `Establishment` concept and must
  relate to one issuer.
- **IssuingPoint persistence record**: Target database representation of an
  issuing point within an establishment. It maps to the domain `IssuingPoint`
  concept and must relate to one establishment.
- **IssuanceSequence persistence record**: Target database representation of a
  sequence reservation for one issuer, establishment, issuing point, canonical
  document type, and sequence number.
- **TaxDocument persistence record**: Target database representation of the
  common tax document lifecycle state, canonical document type, authorization
  state, access key, issue date, issuance mode, optional external request
  identifier, optional authorization number, and optional authorization
  timestamp.
- **TaxDocumentAuditEvent persistence record**: Optional target database
  representation of audit events. It is deferred unless the plan explicitly
  justifies including audit persistence in this foundation.

## Migration Classification *(mandatory for migrated concepts)*

| Legacy Concept | Target Name | Classification | Decision Status |
|----------------|-------------|----------------|-----------------|
| legacy issuer table or fields | `issuers`, `issuer_id`, `legal_identifier`, `legal_name`, `trade_name` | Target database object | Decided for target names; exact legacy source columns require mapping evidence |
| legacy establishment table or fields | `establishments`, `establishment_id`, `establishment_code` | Target database object | Decided for target names; exact legacy source columns require mapping evidence |
| legacy issuing point table or fields | `issuing_points`, `issuing_point_id`, `issuing_point_code` | Target database object | Decided for target names; exact legacy source columns require mapping evidence |
| legacy sequence table or fields | `issuance_sequences`, `sequence_number` | Target database object | Decided for target names; reservation behavior tracked by PFV-PER-001 |
| legacy tax document table or fields | `tax_documents`, `access_key`, `document_type`, `document_state`, `authorization_state`, `authorization_number`, `authorized_at`, `issue_date`, `issuance_mode`, `external_request_id` | Target database object | Decided |
| legacy Spanish table compatibility | compatibility views for legacy table names | Legacy compatibility concept | Deferred by PFV-PER-002 |
| historical XML path columns | XML storage metadata | Pending Functional Validation | Deferred by PFV-PER-003 |
| persisted audit events | `tax_document_audit_events` | Pending Functional Validation | Deferred by PFV-PER-004 unless justified by plan |

Allowed classifications: Target domain concept, Target API field, Target
database object, SRI adapter-only concept, Legacy compatibility concept,
Migration-only concept, Deprecated concept, Pending Naming Decision, Pending
Functional Validation.

## Pending Functional Validations

| ID | Area | Question | Resolution for This Feature |
|----|------|----------|-----------------------------|
| PFV-PER-001 | Sequence reservation | Should sequence reservation increment automatically or reserve a requested value supplied by the caller? | This feature uses the existing `SequenceNumberPort` contract that reserves a requested value. Exact repeated reservation for the same reservation identity is idempotent and returns the existing `SequenceNumber`. Automatic increment behavior is deferred to future issuance use cases unless the plan validates and updates the port contract before tasks. |
| PFV-PER-002 | Legacy table compatibility | Should the target schema support compatibility views for legacy table names? | Deferred to a migration or compatibility specification. This feature must not create compatibility views. |
| PFV-PER-003 | Historical XML paths | Should XML path fields be stored in `tax_documents` or a separate XML storage table? | Deferred to an XML storage specification. This feature must not persist XML paths unless the plan proves they are required for repository identity. |
| PFV-PER-004 | Audit persistence | Should audit events be stored in this foundation or deferred to an audit adapter specification? | Deferred by default. `tax_document_audit_events` may be included only if the plan justifies audit persistence as necessary for this foundation and keeps it inside persistence scope. |
| PFV-PER-005 | Auto-numbering policy | Should future issuance use cases auto-generate the next sequence number when none is requested? | Deferred to a future numbering policy or document-specific issuance specification. This feature must not generate automatic numbering tasks. |

## Idempotency and Audit Requirements *(include if feature is critical)*

**Idempotency Scope**: This feature covers persistence-level idempotency for
tax document storage, duplicate access key detection, duplicate issuance
identity detection, and sequence assignment. Future issuance, retry,
synchronization, webhook, XML generation, and SRI idempotency remain governed by
their dedicated specifications.

**Audit Events**: This feature must preserve the audit event names defined by
the issuance foundation when storing or rehydrating tax document state, but it
does not create SRI, XML, queue, webhook, or document-specific audit flows.
Audit persistence is deferred unless explicitly justified by the plan.

**Sensitive Data Exclusions**: Audit logs and persistence diagnostics MUST NOT
contain secrets, private keys, credentials, tokens, signing passwords, database
passwords, or sensitive configuration values.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A reviewer can trace 100% of required persistence concepts in
  this feature to an approved canonical English term, a target database object,
  or a documented Pending Functional Validation.
- **SC-002**: Persistence adapter validation proves that saving and loading a
  common tax document preserves all required common fields, including access
  key, issuance identity, document state, authorization state, authorization
  number, and authorization timestamp.
- **SC-003**: Rehydration validation proves that invalid persisted
  authorization combinations are rejected with application-facing data
  integrity errors.
- **SC-004**: Duplicate access key and duplicate issuance identity scenarios
  are rejected with application-facing duplicate conflict errors in all required
  validation cases before the feature is accepted.
- **SC-005**: Sequence reservation validation proves that exact repeated
  reservations return the existing `SequenceNumber` and conflicting duplicate
  sequence reservations cannot create two valid reservations for the same
  issuer, establishment, issuing point, document type, and sequence number.
- **SC-006**: Architecture review finds zero persistence framework imports,
  persistence annotations, database-specific types, or persistence adapter
  dependencies in domain and application source.
- **SC-007**: Scope review finds zero REST resources, REST DTOs, SRI adapters,
  XML generation artifacts, queue adapters, webhook adapters, or
  document-specific issuance flows introduced by this feature.
- **SC-008**: Persistence adapter validation proves that target document type
  storage uses canonical values and does not persist SRI numeric codes as the
  internal document type value.
- **SC-009**: Durable migration documentation contains target table and column
  mappings for 100% of database objects introduced by this feature.
- **SC-010**: Repository contract validation proves missing `find` operations
  return empty results and missing `exists` operations return `false`.
- **SC-011**: Temporal persistence validation proves `issue_date` rehydrates as
  the same calendar date and `authorized_at` rehydrates as the same UTC instant
  at the documented precision.

## Assumptions

- Feature `002-tax-document-issuance-foundation` is the source foundation for
  domain concepts and application port contracts.
- The existing `SequenceNumberPort` requested-value reservation contract remains
  valid for this foundation; automatic sequence increment behavior is deferred
  unless planning validates a contract change.
- The target persistence foundation may use framework-specific persistence
  details only inside the outbound persistence adapter or approved
  configuration locations.
- Production data migration and legacy compatibility views are separate future
  concerns and are not required for this foundation.
- XML storage paths and audit-event persistence are not required for repository
  identity unless planning documents an explicit justification.
