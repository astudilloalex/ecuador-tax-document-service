# Research: Tax Document Persistence Foundation

## Decision: Use Reactive Persistence Records Inside the Persistence Adapter

**Decision**: Implement persistence records or reactive persistence entities
inside `adapter.out.persistence`, separate from domain objects, and access
PostgreSQL through a reactive connection path such as Quarkus Reactive
PostgreSQL Client or Hibernate Reactive over the reactive PostgreSQL client.

**Rationale**: The constitution forbids annotating domain classes as persistence
entities and forbids leaking persistence entities into application/domain
layers. The constitution also defines Quarkus with Mutiny as the reactive
target stack. Reactive persistence records/entities give the adapter enough
mapping control without making the domain model depend on persistence concerns
or introducing blocking JDBC/ORM runtime access.

**Alternatives considered**:

- Annotate domain objects as JPA entities: rejected because it violates Clean
  Architecture and DTO/entity separation.
- Use Panache active-record entities for domain behavior: rejected because the
  active-record style would encourage persistence behavior to own model logic.
- Use blocking Hibernate ORM/JPA `EntityManager`: rejected because runtime
  database access must be reactive for this project.
- Use direct JDBC-only repositories: rejected because JDBC is blocking and does
  not satisfy the reactive database connection requirement.

## Decision: Use Flyway Versioned Migrations for Target Schema

**Decision**: Manage the target persistence schema through Flyway migrations
under `src/main/resources/db/migration/`.

**Rationale**: The feature requires versioned schema changes and durable target
database object names. Flyway migration files make schema reviewable, ordered,
and separate from runtime adapter code.

**Alternatives considered**:

- ORM auto-generation: rejected because target schema must be explicit and
  reviewable.
- Manual database setup outside the repo: rejected because it would not satisfy
  Spec Kit traceability or reproducible validation.
- Data migration scripts: deferred by PFV-PER-008 because production data
  migration is out of scope.

## Decision: Limit Persistence Dependencies to Adapter and Test Surfaces

**Decision**: Persistence dependencies and configuration may support only the
outbound persistence adapter, Flyway migrations, and persistence adapter tests.
They must not introduce persistence framework imports, annotations, or runtime
dependencies into the domain or application layers.
Runtime persistence access must use reactive PostgreSQL connectivity; Flyway
remains a versioned schema-management mechanism and not the runtime database
access path for repositories.

**Rationale**: SPEC 003 introduces the first persistence boundary. Keeping
dependencies at the adapter edge preserves Clean Architecture and prevents
future task generation from treating persistence frameworks as application or
domain dependencies.

**Alternatives considered**:

- Allow application ports to import persistence error or transaction types:
  rejected because it would leak infrastructure inward.
- Annotate domain objects for ORM mapping convenience: rejected by the
  constitution and DTO separation rules.
- Add bootstrap runtime wiring now: rejected because runtime bootstrap behavior
  is outside this feature and configuration files are sufficient for the
  persistence foundation.

## Decision: Store Canonical Document Type Values

**Decision**: Store canonical `DocumentType` values such as `INVOICE`,
`CREDIT_NOTE`, `DEBIT_NOTE`, `WAYBILL`, and `WITHHOLDING` in
`tax_documents.document_type` and `issuance_sequences.document_type`.

**Rationale**: SRI numeric codes are external contract values and must not be
treated as internal model names. Canonical values preserve English terminology
and prevent SRI leakage into the target database model.

**Alternatives considered**:

- Store SRI numeric codes: rejected because it makes external SRI codes the
  internal persistence value.
- Store both canonical values and SRI codes: rejected for this foundation
  because it duplicates state and introduces SRI-specific persistence before
  the SRI adapter specification.

## Decision: Enforce Uniqueness With Database Constraints and Adapter Translation

**Decision**: Enforce uniqueness for `access_key`, issuance identity, and
sequence reservation using target database constraints and transaction
boundaries. Translate resulting constraint violations to framework-free
application-layer duplicate or sequence reservation conflict errors defined by
the `application.error` contract.

**Rationale**: Pre-save existence checks alone are race-prone. Database
constraints are the reliable enforcement point, while adapter translation keeps
SQL/framework details from leaking inward.

**Alternatives considered**:

- Application-only duplicate checks: rejected because concurrent requests could
  still create duplicates.
- Returning existing tax documents for duplicate saves: rejected by
  clarification; duplicate saves must fail with conflict errors.
- Exposing database exceptions directly: rejected because application/domain
  layers must not depend on persistence error types.

## Decision: Repository `save` Creates or Updates Only the Same Aggregate

**Decision**: `TaxDocumentRepository.save(TaxDocument)` may create a new
persisted document when no matching record exists and may update the same
persisted aggregate when the same tax document identity already exists. It must
never silently overwrite another persisted tax document with a duplicate
`accessKey` or duplicate issuance identity.

**Rationale**: Future use cases need one simple repository save contract for
initial persistence and lifecycle state updates, while duplicate legal document
identity must remain protected. This keeps the port stable and relies on
database uniqueness plus adapter translation for race-safe duplicate handling.

**Alternatives considered**:

- Create-only save: rejected because future lifecycle transitions need to
  persist updates to the same aggregate.
- Upsert-by-any-unique-key: rejected because it could silently overwrite a
  different document when duplicate `accessKey` or issuance identity appears.
- Persist-or-replace: rejected because replacement semantics are unsafe for
  auditable tax document state.

## Decision: Missing Repository Lookups Return Empty Results

**Decision**: `findByAccessKey` and `findByIssuanceIdentity` return empty
optional/results when no persisted document exists. `existsByAccessKey` and
`existsByIssuanceIdentity` return `false` when no persisted document exists.

**Rationale**: Missing lookup is a normal query result, not a persistence
failure. This keeps absence handling explicit and avoids using exceptions for
ordinary control flow.

**Alternatives considered**:

- Throw not-found exceptions for repository lookups: rejected because the
  existing port returns `Optional`.
- Return placeholder domain objects: rejected because it would create invalid
  domain state.

## Decision: Exact Repeated Sequence Reservation Is Idempotent

**Decision**: `SequenceNumberPort.reserve` returns the existing
`SequenceNumber` when the exact same issuer, establishment, issuing point,
document type, and sequence value reservation already exists. A conflicting
duplicate sequence reservation fails with an application-facing conflict error.

**Rationale**: Sequence reservation is a repeatable critical operation. The
existing port returns a `SequenceNumber` rather than a result status, so
idempotent return keeps the contract stable and supports retries.

**Alternatives considered**:

- Reject exact repeated reservations: rejected because retries would become
  harder to distinguish from conflicts.
- Change the port to return a reservation status: deferred because it expands
  the application contract beyond this foundation's clarified need.
- Automatic next-number allocation: deferred to a future numbering policy or
  document-specific issuance specification.

## Decision: Add Domain-Safe Rehydration to `TaxDocument`

**Decision**: Add a framework-free rehydration mechanism, such as
`TaxDocument.restore(...)`, that accepts persisted state and validates domain
invariants without exposing persistence entities to domain code. The restore
path is limited to rehydrating common tax document state and does not add
invoice, credit note, debit note, withholding, waybill, SRI, XML, queue,
webhook, archive, purge, delete, or production correction behavior.

**Rationale**: The existing constructor creates new documents in `PENDING`.
Repository loading must preserve historical states, authorization state,
authorization number, and authorization timestamp. A domain-owned restore path
keeps invariant checks in the domain.

**Alternatives considered**:

- Set private fields from the persistence adapter: rejected because it bypasses
  domain invariants.
- Add persistence annotations or ORM lifecycle methods to the domain: rejected
  by the constitution.
- Reconstruct by replaying transitions: rejected because persisted historical
  states may not correspond to one simple replay path in this foundation.

## Decision: Reject Invalid Persisted Authorization Combinations

**Decision**: During rehydration, invalid combinations of document state,
authorization state, authorization number, and authorized timestamp are rejected
with application-facing data integrity errors. Invalid combinations include
authorization number present when authorization state is not `AUTHORIZED`,
authorized timestamp present without authorization number, authorized state
without authorization number, authorized state without authorized timestamp,
unknown canonical document type, unknown document state, and unknown
authorization state.

**Rationale**: Tax document authorization state is legally sensitive. Silent
normalization would hide bad persisted data and make audits unreliable.

**Alternatives considered**:

- Normalize to the closest valid state: rejected because it mutates historical
  evidence.
- Load invalid records and mark them as repair-needed: deferred because repair
  workflows are not part of this persistence foundation.

## Decision: Use Restrictive Relationships and No Cascading Deletes

**Decision**: Required tables define explicit primary keys, foreign keys,
unique constraints, and important lookup indexes. Delete/update operations on
issuer, establishment, and issuing point rows referenced by sequences or tax
documents are restricted. Cascade deletes are not part of this foundation.

**Rationale**: Tax document persistence is auditable. Cascading deletes or
implicit relationship mutation would risk losing or altering legally relevant
state. Restrictive relationships keep accidental data loss out of this
foundation.

**Alternatives considered**:

- Cascading deletes from issuer hierarchy: rejected because tax documents and
  sequence reservations must remain stable audit evidence.
- Soft-delete policy now: deferred because lifecycle/archive behavior is not in
  scope.
- Mutable issuer hierarchy updates cascading into documents: rejected because
  persisted tax document identity must remain stable.

## Decision: Persist Temporal Values With Measurable Precision

**Decision**: Persist `issue_date` as the domain `IssueDate` calendar date in a
database `date` column without timezone conversion. Persist `authorized_at` as
the domain `AuthorizedAt` instant/timestamp normalized to UTC in a database
timestamp column. Rehydration tests compare `authorized_at` at microsecond
precision or the selected database precision, whichever is lower.

**Rationale**: `issue_date` is a document date, while `authorized_at` is an
authorization timestamp. Distinguishing date and instant semantics keeps
timezone handling testable without overengineering.

**Alternatives considered**:

- Persist both as strings: rejected because it weakens temporal validation.
- Persist `issue_date` as a timestamp: rejected because it introduces timezone
  concerns for a date-only concept.
- Require nanosecond precision for `authorized_at`: rejected because common
  relational databases may truncate timestamp precision.

## Decision: Defer Audit Event Table

**Decision**: Do not include `tax_document_audit_events` in this foundation.

**Rationale**: The feature must preserve audit naming and sensitive data
exclusions, but audit persistence ownership is not required for repository
identity. Deferring avoids coupling audit storage decisions to common document
persistence too early.

**Alternatives considered**:

- Include audit events now: rejected because PFV-PER-004 is unresolved and an
  audit adapter may own this later.
- Store audit events in `tax_documents`: rejected because it would mix lifecycle
  state with event history.

## Decision: Defer XML Paths and Legacy Compatibility Views

**Decision**: Do not store XML paths and do not create legacy compatibility
views in this feature.

**Rationale**: XML storage and legacy compatibility are explicitly out of scope
and have separate PFVs. Including them would expand the persistence foundation
beyond common issuance identity and lifecycle state.

**Alternatives considered**:

- Add XML path columns now: rejected because XML storage belongs to a future
  storage specification.
- Create Spanish legacy compatibility views now: rejected because compatibility
  exceptions require separate scope, owner, and expiration conditions.

## Decision: Defer Migration Rollback and Repair Workflows

**Decision**: Do not define migration failure handling, rollback playbooks, or
persisted data repair workflows in SPEC 003. The feature requires versioned
schema artifacts and validation documentation only.

**Rationale**: Operational rollback and repair behavior depends on deployment,
backup, release, and production data policies that are not part of this
persistence foundation. Deferring this prevents task generation from inventing
operations work without governance.

**Alternatives considered**:

- Define rollback scripts now: rejected because production deployment and
  recovery policy is not established in this feature.
- Add repair tooling now: rejected because persisted data repair workflows
  require a dedicated operations or migration specification.
- Ignore the topic: rejected because task generation needs an explicit
  deferral to avoid guessing.

## Decision: Defer Archive, Purge, Delete, and Production Correction Workflows

**Decision**: Do not implement archive, purge, delete, production data
correction, or lifecycle correction workflows in SPEC 003. The schema defines
restrictive relationships and required persistence operations only.

**Rationale**: Tax document records are auditable. Retention, deletion,
correction, and lifecycle repair behavior require explicit policy, ownership,
and legal review beyond the common persistence foundation.

**Alternatives considered**:

- Add hard deletes or cascade deletes now: rejected because they risk
  accidental loss of auditable tax document state.
- Add soft-delete columns now: rejected because retention and archive policy is
  out of scope.
- Allow repository lifecycle updates beyond required save semantics: rejected
  because future document lifecycle features must define their own business
  rules.

## PFV Traceability Summary

| PFV | Research Status |
|-----|-----------------|
| PFV-PER-001 | Requested-value sequence reservation is in scope; automatic increment behavior is deferred. |
| PFV-PER-002 | Legacy compatibility views are deferred. |
| PFV-PER-003 | Historical XML path storage is deferred. |
| PFV-PER-004 | Audit event table implementation is deferred. |
| PFV-PER-005 | Auto-numbering policy is deferred. |
| PFV-PER-006 | Migration failure handling, rollback playbooks, and persisted data repair workflows are deferred. |
| PFV-PER-007 | Archive, purge, delete, production correction, and lifecycle correction workflows are deferred. |
| PFV-PER-008 | Production data migration is deferred. |
