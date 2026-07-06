# Contract: Persistence Error Translation

## Purpose

Define how persistence adapter failures are translated before crossing inward
to the application layer.

## Error Categories

| Adapter Condition | Stable Application-Facing Category | Notes |
|-------------------|------------------------------------|-------|
| Duplicate `access_key` | `DuplicateAccessKeyConflict` | Required by clarification and `FR-009`. |
| Duplicate issuance identity | `DuplicateIssuanceIdentityConflict` | Required by clarification and `FR-010`. |
| Duplicate or unavailable sequence reservation | `UnavailableSequenceReservationConflict` | Required by `FR-012`; exact repeated reservation remains idempotent. |
| Invalid persisted authorization combination | `InvalidPersistedTaxDocumentState` | Required by `FR-008`. |
| Unknown canonical document type | `InvalidPersistedTaxDocumentState` | Prevents invalid document type rehydration. |
| Unknown document state | `InvalidPersistedTaxDocumentState` | Prevents invalid state rehydration. |
| Unknown authorization state | `InvalidPersistedTaxDocumentState` | Prevents invalid authorization state rehydration. |
| Unknown issuance mode | `InvalidPersistedTaxDocumentState` | Prevents invalid issuance mode rehydration. |
| Transaction failure | `PersistenceTransactionFailure` | Must not expose framework transaction type. |
| Foreign key, missing relationship, or inconsistent issuer/establishment/issuing point relationship | `InvalidPersistenceRelationship` | Adapter must not silently create partial domain objects. |
| Any uncategorized persistence framework/database failure | `GenericPersistenceFailure` | Stable fallback category without leaking framework type. |

## Boundary Rules

- Do not expose SQL exception classes outside `adapter.out.persistence`.
- Do not expose `SQLException`, `PersistenceException`,
  `ConstraintViolationException`, Hibernate exceptions, Panache exceptions,
  JDBC types, PostgreSQL-specific exceptions, Flyway exceptions, or equivalent
  persistence-specific types outside the adapter.
- Error messages must not include credentials, tokens, passwords, private keys,
  connection strings with secrets, or sensitive configuration values.
- Errors returned to application/domain code must be stable enough for tests to
  assert category and context.

## Validation Requirements

Adapter tests must verify:

- Duplicate access key translation.
- Duplicate issuance identity translation.
- Duplicate or unavailable sequence reservation translation.
- Invalid persisted authorization combination translation.
- Unknown canonical document type, document state, authorization state, and
  issuance mode translation.
- Missing or inconsistent relationship translation.
- Transaction failure translation when practical without relying on
  implementation-specific internals.
- Generic persistence failure translation.

## Traceability

- Spec: `FR-008`, `FR-009`, `FR-010`, `FR-012`, `FR-014`, `FR-017`,
  `FR-018`
- Plan: Idempotency, Audit, and Error Handling
- Constitution: Ports and Adapters; DTO and Validation Separation
