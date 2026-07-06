# Contract: Persistence Error Translation

## Purpose

Define how persistence adapter failures are translated before crossing inward
to the application layer.

## Error Categories

| Adapter Condition | Application-Facing Category | Notes |
|-------------------|-----------------------------|-------|
| Duplicate `access_key` | Duplicate conflict | Required by clarification and `FR-009`. |
| Duplicate issuance identity | Duplicate conflict | Required by clarification and `FR-010`. |
| Invalid persisted authorization combination | Data integrity | Required by `FR-008`. |
| Unknown canonical enum value in persistence | Data integrity | Prevents invalid state rehydration. |
| Transaction failure | Persistence transaction failure | Must not expose framework transaction type. |
| Foreign key or missing relationship inconsistency | Data integrity | Adapter must not silently create partial domain objects. |

## Boundary Rules

- Do not expose SQL exception classes outside `adapter.out.persistence`.
- Do not expose Hibernate, JPA, JDBC, PostgreSQL, or Flyway types outside the
  adapter.
- Error messages must not include credentials, tokens, passwords, private keys,
  connection strings with secrets, or sensitive configuration values.
- Errors returned to application/domain code must be stable enough for tests to
  assert category and context.

## Validation Requirements

Adapter tests must verify:

- Duplicate access key translation.
- Duplicate issuance identity translation.
- Invalid persisted authorization combination translation.
- Transaction failure translation when practical without relying on
  implementation-specific internals.

## Traceability

- Spec: `FR-008`, `FR-009`, `FR-010`, `FR-014`, `FR-017`, `FR-018`
- Plan: Idempotency, Audit, and Error Handling
- Constitution: Ports and Adapters; DTO and Validation Separation
