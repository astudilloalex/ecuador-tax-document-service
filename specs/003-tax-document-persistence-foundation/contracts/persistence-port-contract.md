# Contract: Persistence Port Implementations

## Purpose

Define the adapter obligations for implementing existing application output
ports through the outbound persistence adapter.

This contract clarifies behavior for the ports approved by
`002-tax-document-issuance-foundation`; it does not rename, redesign, or
broaden those ports.

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

### `save(TaxDocument taxDocument)`

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
- Returns a domain `TaxDocument`, never a persistence entity.

Forbidden behavior:

- Returning persistence entities.
- Leaking JPA, Hibernate, JDBC, SQL, PostgreSQL, or Flyway exceptions.
- Leaking `adapter.out.persistence` exception or diagnostic types to
  application callers.
- Creating REST, SRI, XML, storage, queue, webhook, bootstrap, archive, purge,
  delete, production correction, or migration repair behavior.

### `findByAccessKey(AccessKey accessKey)`

Required behavior:

- Loads a persisted tax document by `access_key`.
- Returns `Optional.empty()` when no document exists.
- Rehydrates through the domain restore path.
- Rejects invalid persisted authorization combinations with an
  application-facing data integrity error.

### `findByIssuanceIdentity(...)`

Required behavior:

- Loads by issuer, canonical document type, establishment, issuing point, and
  sequence number.
- Returns `Optional.empty()` when no document exists.
- Uses canonical `DocumentType` values, not SRI numeric codes.

### Existence Checks

Required behavior:

- `existsByAccessKey` reflects persisted `access_key`.
- `existsByIssuanceIdentity` reflects persisted issuance identity.
- `existsByAccessKey` returns `false` when no persisted tax document exists for
  the access key.
- `existsByIssuanceIdentity` returns `false` when no persisted tax document
  exists for the issuance identity.
- Existence checks are not the only duplicate protection; database constraints
  remain authoritative.

## `SequenceNumberPort`

### `reserve(...)`

Required behavior:

- Reserves a requested sequence value for issuer, establishment, issuing point,
  and canonical document type.
- Returns a domain `SequenceNumber`.
- Exact repeated reservation for the same identity returns the existing
  `SequenceNumber` idempotently.
- Conflicting duplicate reservation must fail with an application-facing
  sequence reservation conflict error and must not create a second valid
  reservation.
- Database uniqueness and transaction behavior are required reliability
  guarantees; application-only checks are insufficient.
- Automatic next-number allocation is out of scope.

### `isAvailable(...)`

Required behavior:

- Returns `true` only when the requested reservation identity has no existing
  reservation.
- Returns `false` when a conflicting reservation exists.

## `TransactionPort`

Required behavior:

- Executes application operations inside persistence transaction boundaries.
- Supports return-value and void operations.
- Does not expose persistence transaction types to application callers.
- Translates transaction failures to application-facing failures.

## Traceability

- Spec: `FR-005`, `FR-009`, `FR-010`, `FR-011`, `FR-012`, `FR-014`,
  `FR-017`, `FR-018`
- Plan: Ports and Adapters, Error Mapping
- Constitution: Principles IV, VII, IX
