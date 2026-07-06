# Research: Tax Document Persistence Foundation

## Decision: Use Plain JPA Entities Inside the Persistence Adapter

**Decision**: Implement persistence records as plain JPA entities located under
`adapter.out.persistence`, separate from domain objects.

**Rationale**: The constitution forbids annotating domain classes as persistence
entities and forbids leaking persistence entities into application/domain
layers. Plain JPA entities give the adapter enough mapping control without
making the domain model depend on persistence concerns.

**Alternatives considered**:

- Annotate domain objects as JPA entities: rejected because it violates Clean
  Architecture and DTO/entity separation.
- Use Panache active-record entities for domain behavior: rejected because the
  active-record style would encourage persistence behavior to own model logic.
- Use direct JDBC-only repositories: deferred because JPA provides clearer
  entity mapping for this foundation while keeping SQL details inside the
  adapter.

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
- Data migration scripts: deferred because production data migration is out of
  scope.

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
sequence reservation using target database constraints. Translate resulting
constraint violations to application-facing duplicate conflict errors.

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

## Decision: Exact Repeated Sequence Reservation Is Idempotent

**Decision**: `SequenceNumberPort.reserve` returns the existing
`SequenceNumber` when the exact same issuer, establishment, issuing point,
document type, and sequence value reservation already exists.

**Rationale**: Sequence reservation is a repeatable critical operation. The
existing port returns a `SequenceNumber` rather than a result status, so
idempotent return keeps the contract stable and supports retries.

**Alternatives considered**:

- Reject exact repeated reservations: rejected because retries would become
  harder to distinguish from conflicts.
- Change the port to return a reservation status: deferred because it expands
  the application contract beyond this foundation's clarified need.

## Decision: Add Domain-Safe Rehydration to `TaxDocument`

**Decision**: Add a framework-free rehydration mechanism, such as
`TaxDocument.restore(...)`, that accepts persisted state and validates domain
invariants without exposing persistence entities to domain code.

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
with application-facing data integrity errors.

**Rationale**: Tax document authorization state is legally sensitive. Silent
normalization would hide bad persisted data and make audits unreliable.

**Alternatives considered**:

- Normalize to the closest valid state: rejected because it mutates historical
  evidence.
- Load invalid records and mark them as repair-needed: deferred because repair
  workflows are not part of this persistence foundation.

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
