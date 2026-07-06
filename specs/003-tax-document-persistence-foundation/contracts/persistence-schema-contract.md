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

- Primary key on `issuer_id`.
- `legal_identifier` required.
- Important index on `legal_identifier` when adapter lookup by legal identifier
  is introduced.
- Delete/update restriction: referenced issuer rows must not be deleted or have
  `issuer_id` changed while referenced by `establishments`,
  `issuance_sequences`, or `tax_documents`.

### `establishments`

- Primary key on `establishment_id`.
- Foreign key from `issuer_id` to `issuers.issuer_id`.
- Unique key on `(issuer_id, establishment_code)`.
- Important index on `issuer_id`.
- Delete/update restriction: referenced establishment rows must not be deleted
  or have `establishment_id` changed while referenced by `issuing_points`,
  `issuance_sequences`, or `tax_documents`.

### `issuing_points`

- Primary key on `issuing_point_id`.
- Foreign key from `establishment_id` to `establishments.establishment_id`.
- Unique key on `(establishment_id, issuing_point_code)`.
- Important index on `establishment_id`.
- Delete/update restriction: referenced issuing point rows must not be deleted
  or have `issuing_point_id` changed while referenced by
  `issuance_sequences` or `tax_documents`.

### `issuance_sequences`

- Primary key on `issuance_sequence_id`.
- Foreign keys:
  - `issuer_id -> issuers.issuer_id`
  - `establishment_id -> establishments.establishment_id`
  - `issuing_point_id -> issuing_points.issuing_point_id`
- Unique key on `(issuer_id, establishment_id, issuing_point_id,
  document_type, sequence_number)`.
- Important index on the unique reservation identity.
- `document_type` stores canonical document type values only.
- Delete/update restriction: referenced issuer, establishment, and issuing
  point rows must not be deleted or have key values changed while referenced by
  sequence reservations.

### `tax_documents`

- Primary key on `tax_document_id`.
- Unique key on `access_key`.
- Unique key on `(issuer_id, document_type, establishment_id,
  issuing_point_id, sequence_number)`.
- Foreign keys:
  - `issuer_id -> issuers.issuer_id`
  - `establishment_id -> establishments.establishment_id`
  - `issuing_point_id -> issuing_points.issuing_point_id`
- Indexed lookup by `access_key`.
- Indexed lookup by issuance identity.
- `document_type`, `document_state`, `authorization_state`, and
  `issuance_mode` store canonical target values.
- `issue_date` uses a database `date` column.
- `authorized_at` uses a UTC-normalized database timestamp column.
- Delete/update restriction: referenced issuer, establishment, and issuing
  point rows must not be deleted or have key values changed while referenced by
  tax documents.

## Relationship Rules

- `establishments.issuer_id` must identify the issuer that owns the
  establishment.
- `issuing_points.establishment_id` must identify the establishment that owns
  the issuing point.
- `issuance_sequences` and `tax_documents` must reference an issuer,
  establishment, and issuing point combination that is internally consistent.
- Missing or inconsistent relationships are data integrity failures, not
  silently rehydratable records.

## Delete and Update Restrictions

- Cascade deletes are not part of this feature.
- Key updates on referenced issuer, establishment, and issuing point rows are
  restricted while dependent sequence or tax document rows exist.
- Future archival, purge, or production correction behavior requires a
  separate specification.
- Delete operations for tax documents, sequence reservations, issuers,
  establishments, and issuing points are not repository behavior in SPEC 003.
- Migration failure handling, rollback playbooks, and persisted data repair
  workflows are deferred to a future operations or migration specification.

## Temporal Columns

| Column | Domain Type | Database Type | Timezone Rule | Rehydration Precision |
|--------|-------------|---------------|---------------|-----------------------|
| `issue_date` | `IssueDate` calendar date | `date` | No timezone conversion | Same calendar date |
| `authorized_at` | `AuthorizedAt` instant/timestamp | UTC-normalized timestamp | Normalize to UTC | Microsecond precision or selected database precision, whichever is lower |

## Forbidden Schema Content

- Spanish target table or column names.
- SRI numeric document codes as internal `document_type` values.
- SRI XML/SOAP payload columns.
- XML path columns unless a future XML storage spec owns them.
- Legacy compatibility views.
- Document-specific line, tax, total, withholding, waybill, or PDF/RIDE tables.
- Automatic next-number allocation policy.
- Archive, purge, delete, production correction, rollback, or repair workflow
  tables or columns.

## Traceability

- Spec: `FR-001`, `FR-002`, `FR-003`, `FR-008`, `FR-009`, `FR-010`,
  `FR-013`, `FR-021`, `FR-022`, `NR-001`, `NR-002`, `NR-003`, `NR-004`
- Data Model: Target Tables
- Constitution: English canonical terminology and database naming rules
