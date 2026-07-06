# Contract: Persistence Schema

## Purpose

Define the target database contract for the common tax document persistence
foundation. This is a design contract for migrations and adapter tests, not a
database migration file.

## Required Tables

| Table | Required | Purpose |
|-------|----------|---------|
| `issuers` | Yes | Issuer identity and canonical issuer metadata. |
| `establishments` | Yes | Establishments owned by issuers. |
| `issuing_points` | Yes | Issuing points owned by establishments. |
| `issuance_sequences` | Yes | Requested sequence reservations. |
| `tax_documents` | Yes | Common tax document identity and lifecycle state. |
| `tax_document_audit_events` | No | Deferred by PFV-PER-004. |

## Required Constraints

### `issuers`

- Primary or unique key on `issuer_id`.
- `legal_identifier` required.

### `establishments`

- Primary or unique key on `establishment_id`.
- Foreign key from `issuer_id` to `issuers.issuer_id`.
- Unique key on `(issuer_id, establishment_code)`.

### `issuing_points`

- Primary or unique key on `issuing_point_id`.
- Foreign key from `establishment_id` to `establishments.establishment_id`.
- Unique key on `(establishment_id, issuing_point_code)`.

### `issuance_sequences`

- Primary key on `issuance_sequence_id`.
- Foreign keys to issuer, establishment, and issuing point records.
- Unique key on `(issuer_id, establishment_id, issuing_point_id,
  document_type, sequence_number)`.
- `document_type` stores canonical document type values only.

### `tax_documents`

- Primary key on `tax_document_id`.
- Unique key on `access_key`.
- Unique key on `(issuer_id, document_type, establishment_id,
  issuing_point_id, sequence_number)`.
- Foreign keys to issuer, establishment, and issuing point records.
- Indexed lookup by `access_key`.
- Indexed lookup by issuance identity.
- `document_type`, `document_state`, `authorization_state`, and
  `issuance_mode` store canonical target values.

## Forbidden Schema Content

- Spanish target table or column names.
- SRI numeric document codes as internal `document_type` values.
- SRI XML/SOAP payload columns.
- XML path columns unless a future XML storage spec owns them.
- Legacy compatibility views.
- Document-specific line, tax, total, withholding, waybill, or PDF/RIDE tables.

## Traceability

- Spec: `FR-001`, `FR-002`, `FR-003`, `FR-008`, `FR-009`, `FR-010`,
  `FR-013`, `NR-001`, `NR-002`, `NR-003`, `NR-004`
- Data Model: Target Tables
- Constitution: English canonical terminology and database naming rules
