# Contract: Persistence Port Implementations

## Purpose

Define the adapter obligations for implementing existing application output
ports through the outbound persistence adapter.

This contract clarifies behavior for the ports approved by
`002-tax-document-issuance-foundation`; it does not rename, redesign, or
broaden those ports.

All implemented port operations return Mutiny `Uni` results according to the
constitution target stack. `Uni` is the application-layer reactive boundary
contract; payloads inside `Uni` remain domain/application models only.
The existing application output ports remain application-layer abstractions:
they may import Mutiny `Uni`, but they must not import adapter-local types,
persistence entities, JDBC, Hibernate ORM, JPA transaction objects, reactive
PostgreSQL client types, REST, SRI, XML, queue, storage, webhook, or bootstrap
types.

The outbound persistence adapter implementation must use a reactive PostgreSQL
connection path for runtime repository, sequence, and transaction operations.
Blocking JDBC datasource access, blocking Hibernate ORM sessions, and blocking
JPA `EntityManager` are not valid runtime database access mechanisms for this
feature. Flyway migration artifacts remain schema-management artifacts only.

Application-facing persistence errors referenced by this contract are defined
only in the framework-free application error contract under
`com.alexastudillo.taxdocument.application.error`. Persistence adapters may use
adapter-local diagnostics internally, but application code must never depend on
`adapter.out.persistence` exception types.

## Scope

Included ports:

- `TaxDocumentRepository`
- `SequenceNumberPort`
- `TransactionPort`

Excluded ports:

- `SriAuthorizationPort`
- `XmlStoragePort`
- `TaxDocumentQueuePort`
- `WebhookPublisherPort`
- `AuditLogPort`
- Inbound REST input ports
- Application port redesign
- Archive, purge, delete, production correction, and migration repair workflows

## `TaxDocumentRepository`

### `Uni<TaxDocument> save(TaxDocument taxDocument)`

Required behavior:

- Persists a common `TaxDocument` using target English schema.
- Creates a new persisted document when no matching persisted record exists.
- Updates the same persisted aggregate when the same tax document identity
  already exists.
- Never silently overwrites another persisted tax document with a duplicate
  `accessKey` or duplicate issuance identity.
- Enforces unique `accessKey`.
- Enforces unique issuance identity:
  `issuer + documentType + establishment + issuingPoint + sequenceNumber`.
- Rejects duplicate `accessKey` saves with an application-facing duplicate
  conflict error.
- Rejects duplicate issuance identity saves with an application-facing
  duplicate conflict error.
- Returns a `Uni` containing a domain `TaxDocument`, never a persistence
  entity.

Forbidden behavior:

- Returning persistence entities.
- Leaking JPA, Hibernate, JDBC, SQL, PostgreSQL, reactive PostgreSQL client, or
  Flyway exceptions.
- Leaking `adapter.out.persistence` exception or diagnostic types to
  application callers.
- Implementing runtime repository operations through blocking JDBC, blocking
  Hibernate ORM sessions, or blocking JPA `EntityManager`.
- Creating REST, SRI, XML, storage, queue, webhook, bootstrap, archive, purge,
  delete, production correction, or migration repair behavior.

### `Uni<Optional<TaxDocument>> findByAccessKey(AccessKey accessKey)`

Required behavior:

- Loads a persisted tax document by `access_key`.
- Returns a `Uni` containing `Optional.empty()` when no document exists.
- Rehydrates through the domain restore path.
- Rejects invalid persisted authorization combinations with an
  application-facing data integrity error.

### `Uni<Optional<TaxDocument>> findByIssuanceIdentity(...)`

Required behavior:

- Loads by issuer, canonical document type, establishment, issuing point, and
  sequence number.
- Returns a `Uni` containing `Optional.empty()` when no document exists.
- Uses canonical `DocumentType` values, not SRI numeric codes.

### Existence Checks

Required behavior:

- `existsByAccessKey` reflects persisted `access_key`.
- `existsByIssuanceIdentity` reflects persisted issuance identity.
- `existsByAccessKey` returns a `Uni` containing `false` when no persisted tax
  document exists for the access key.
- `existsByIssuanceIdentity` returns a `Uni` containing `false` when no
  persisted tax document exists for the issuance identity.
- Existence checks are not the only duplicate protection; database constraints
  remain authoritative.

## `SequenceNumberPort`

### `Uni<SequenceNumber> reserve(...)`

Required behavior:

- Reserves a requested sequence value for issuer, establishment, issuing point,
  and canonical document type.
- Returns a `Uni` containing a domain `SequenceNumber`.
- Exact repeated reservation for the same identity returns the existing
  `SequenceNumber` idempotently.
- Conflicting duplicate reservation must fail with an application-facing
  sequence reservation conflict error and must not create a second valid
  reservation.
- Database uniqueness and transaction behavior are required reliability
  guarantees; application-only checks are insufficient.
- Automatic next-number allocation is out of scope.

### `Uni<Boolean> isAvailable(...)`

Required behavior:

- Returns a `Uni` containing `true` only when the requested reservation
  identity has no existing reservation.
- Returns a `Uni` containing `false` when a conflicting reservation exists.

## `TransactionPort`

Required behavior:

- Executes application operations inside persistence transaction boundaries.
- Supports return-value and void operations.
- Returns Mutiny `Uni` results for both return-value and void operations.
- Does not expose persistence transaction types to application callers.
- Translates transaction failures to application-facing failures.

## Traceability

- Spec: `FR-005`, `FR-009`, `FR-010`, `FR-011`, `FR-012`, `FR-014`,
  `FR-017`, `FR-018`, `FR-024`
- Plan: Ports and Adapters, Error Mapping
- Constitution: Principles IV, VII, IX
