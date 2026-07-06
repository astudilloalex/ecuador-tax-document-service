# Data Model: Tax Document Persistence Foundation

This model defines target persistence records and their mapping to the existing
domain/application model. It does not define REST DTOs, SRI XML/SOAP DTOs,
queue models, webhook payloads, XML storage records, or production data
migration.

## Target Tables

Minimum target tables:

- `issuers`
- `establishments`
- `issuing_points`
- `issuance_sequences`
- `tax_documents`

Deferred tables:

- `tax_document_audit_events` is deferred by PFV-PER-004.

## Issuer Persistence Record

**Target Table**: `issuers`

**Purpose**: Persist issuer identity required by common issuance identity and
relationships.

**Fields**:

| Field | Required | Notes |
|-------|----------|-------|
| `issuer_id` | Yes | Target issuer identity used by domain `Issuer.issuerId`. |
| `legal_identifier` | Yes | Issuer tax/legal identifier. |
| `legal_name` | No | Canonical legal name. |
| `trade_name` | No | Canonical trade name. |
| `created_at` | Yes | Persistence timestamp for diagnostics. |
| `updated_at` | Yes | Persistence timestamp for diagnostics. |

**Relationships**:

- One issuer has many `establishments`.
- One issuer has many `issuance_sequences`.
- One issuer has many `tax_documents`.

**Validation Rules**:

- `issuer_id` must be unique.
- `legal_identifier` must be present.
- Spanish legacy names are forbidden in target columns.

**Schema Rules**:

- Primary key: `issuer_id`.
- Important index: `legal_identifier` if adapter queries require issuer lookup
  by legal identifier.
- Delete/update restriction: referenced issuer rows must not be deleted or have
  their `issuer_id` changed while referenced by `establishments`,
  `issuance_sequences`, or `tax_documents`.
- Cascade deletes are not part of this feature.

**Traceability**: `FR-001`, `FR-003`, `FR-013`, `FR-015`, `FR-022`,
`NR-001`, `NR-003`, `NR-004`, `SC-009`.

## Establishment Persistence Record

**Target Table**: `establishments`

**Purpose**: Persist establishments owned by issuers for issuance identity.

**Fields**:

| Field | Required | Notes |
|-------|----------|-------|
| `establishment_id` | Yes | Target establishment identity used by domain `Establishment.establishmentId`. |
| `issuer_id` | Yes | References `issuers.issuer_id`. |
| `establishment_code` | Yes | Canonical target establishment code. |
| `created_at` | Yes | Persistence timestamp for diagnostics. |
| `updated_at` | Yes | Persistence timestamp for diagnostics. |

**Relationships**:

- Many establishments belong to one issuer.
- One establishment has many `issuing_points`.
- One establishment participates in many `issuance_sequences` and
  `tax_documents`.

**Validation Rules**:

- `establishment_id` must be unique.
- `(issuer_id, establishment_code)` must be unique.
- `issuer_id` must reference an existing issuer.

**Schema Rules**:

- Primary key: `establishment_id`.
- Foreign key: `issuer_id -> issuers.issuer_id`.
- Unique constraint: `(issuer_id, establishment_code)`.
- Important index: `issuer_id`.
- Delete/update restriction: referenced establishment rows must not be deleted
  or have their `establishment_id` changed while referenced by
  `issuing_points`, `issuance_sequences`, or `tax_documents`.
- Cascade deletes are not part of this feature.

**Traceability**: `FR-001`, `FR-003`, `FR-010`, `FR-015`, `FR-022`,
`NR-001`, `NR-003`, `SC-009`.

## IssuingPoint Persistence Record

**Target Table**: `issuing_points`

**Purpose**: Persist issuing points within establishments for sequence
reservation and document identity.

**Fields**:

| Field | Required | Notes |
|-------|----------|-------|
| `issuing_point_id` | Yes | Target issuing point identity used by domain `IssuingPoint.issuingPointId`. |
| `establishment_id` | Yes | References `establishments.establishment_id`. |
| `issuing_point_code` | Yes | Canonical target issuing point code. |
| `created_at` | Yes | Persistence timestamp for diagnostics. |
| `updated_at` | Yes | Persistence timestamp for diagnostics. |

**Relationships**:

- Many issuing points belong to one establishment.
- One issuing point participates in many `issuance_sequences` and
  `tax_documents`.

**Validation Rules**:

- `issuing_point_id` must be unique.
- `(establishment_id, issuing_point_code)` must be unique.
- `establishment_id` must reference an existing establishment.

**Schema Rules**:

- Primary key: `issuing_point_id`.
- Foreign key: `establishment_id -> establishments.establishment_id`.
- Unique constraint: `(establishment_id, issuing_point_code)`.
- Important index: `establishment_id`.
- Delete/update restriction: referenced issuing point rows must not be deleted
  or have their `issuing_point_id` changed while referenced by
  `issuance_sequences` or `tax_documents`.
- Cascade deletes are not part of this feature.

**Traceability**: `FR-001`, `FR-003`, `FR-010`, `FR-015`, `FR-022`,
`NR-001`, `NR-003`, `SC-009`.

## IssuanceSequence Persistence Record

**Target Table**: `issuance_sequences`

**Purpose**: Persist requested sequence reservations for a specific issuer,
establishment, issuing point, canonical document type, and sequence number.

**Fields**:

| Field | Required | Notes |
|-------|----------|-------|
| `issuance_sequence_id` | Yes | Adapter-owned persistence identity. |
| `issuer_id` | Yes | References `issuers.issuer_id`. |
| `establishment_id` | Yes | References `establishments.establishment_id`. |
| `issuing_point_id` | Yes | References `issuing_points.issuing_point_id`. |
| `document_type` | Yes | Canonical `DocumentType` value, not an SRI numeric code. |
| `sequence_number` | Yes | Requested sequence value, digits only. |
| `reserved_at` | Yes | Timestamp when reservation was first recorded. |
| `created_at` | Yes | Persistence timestamp for diagnostics. |
| `updated_at` | Yes | Persistence timestamp for diagnostics. |

**Relationships**:

- References issuer, establishment, and issuing point.
- A matching `tax_documents` row may later use the same issuance identity.

**Validation Rules**:

- Unique reservation identity:
  `(issuer_id, establishment_id, issuing_point_id, document_type,
  sequence_number)`.
- Exact repeated reservation for the same identity returns the existing domain
  `SequenceNumber`.
- Conflicting duplicate sequence reservation fails with an application-facing
  sequence reservation conflict error.
- `document_type` must be one of `INVOICE`, `CREDIT_NOTE`, `DEBIT_NOTE`,
  `WAYBILL`, or `WITHHOLDING`.
- SRI numeric codes must not be stored as the internal document type value.
- Automatic numbering policy is out of scope.

**Schema Rules**:

- Primary key: `issuance_sequence_id`.
- Foreign keys:
  - `issuer_id -> issuers.issuer_id`
  - `establishment_id -> establishments.establishment_id`
  - `issuing_point_id -> issuing_points.issuing_point_id`
- Unique constraint: `(issuer_id, establishment_id, issuing_point_id,
  document_type, sequence_number)`.
- Important index: the unique reservation identity.
- Delete/update restriction: referenced issuer, establishment, and issuing
  point rows must not be deleted or have key values changed while referenced by
  sequence reservations.
- Cascade deletes are not part of this feature.

**Traceability**: `FR-011`, `FR-012`, `FR-013`, `FR-022`, `NR-004`,
`SC-005`, `SC-008`.

## TaxDocument Persistence Record

**Target Table**: `tax_documents`

**Purpose**: Persist the common tax document identity, lifecycle state, and
authorization state required by future issuance use cases.

**Fields**:

| Field | Required | Notes |
|-------|----------|-------|
| `tax_document_id` | Yes | Adapter-owned persistence identity. |
| `access_key` | Yes | 49-digit access key; unique. |
| `issuer_id` | Yes | References `issuers.issuer_id`. |
| `establishment_id` | Yes | References `establishments.establishment_id`. |
| `issuing_point_id` | Yes | References `issuing_points.issuing_point_id`. |
| `document_type` | Yes | Canonical `DocumentType` value. |
| `sequence_number` | Yes | Issuance sequence value. |
| `issue_date` | Yes | Domain `IssueDate` calendar date; database `date`; no timezone conversion. |
| `document_state` | Yes | Canonical `DocumentState` value. |
| `authorization_state` | Yes | Canonical `AuthorizationState` value. |
| `authorization_number` | No | Present only for authorized combinations. |
| `authorized_at` | No | Domain `AuthorizedAt` timestamp normalized to UTC; database timestamp; compare at microsecond or selected database precision. |
| `issuance_mode` | Yes | Canonical `IssuanceMode` value. |
| `external_request_id` | No | Optional caller-provided idempotency identifier. |
| `created_at` | Yes | Persistence timestamp for diagnostics. |
| `updated_at` | Yes | Persistence timestamp for diagnostics. |

**Relationships**:

- References issuer, establishment, and issuing point.
- Shares issuance identity with `issuance_sequences`.

**Validation Rules**:

- `access_key` must be unique.
- Issuance identity must be unique:
  `(issuer_id, document_type, establishment_id, issuing_point_id,
  sequence_number)`.
- `access_key` must contain exactly 49 digits.
- `document_type` must be one of the canonical values.
- `document_state` must be one of the canonical `DocumentState` values.
- `authorization_state` must be one of the canonical `AuthorizationState`
  values.
- Invalid persisted authorization combinations are rejected during
  rehydration with application-facing data integrity errors.
- `authorization_number` and `authorized_at` must be consistent with
  `DocumentState.AUTHORIZED` and `AuthorizationState.AUTHORIZED`.
- `authorization_number` present while authorization state is not `AUTHORIZED`
  is invalid.
- `authorized_at` present without `authorization_number` is invalid.
- Authorized state without `authorization_number` is invalid.
- Authorized state without `authorized_at` is invalid.
- Unknown canonical document type, document state, authorization state, or
  issuance mode values are invalid.

**Temporal Rules**:

- `issue_date` is stored and rehydrated as the same calendar date.
- `authorized_at` is stored as a UTC-normalized timestamp.
- Rehydration tests compare `authorized_at` at microsecond precision or the
  selected database precision, whichever is lower.

**Schema Rules**:

- Primary key: `tax_document_id`.
- Foreign keys:
  - `issuer_id -> issuers.issuer_id`
  - `establishment_id -> establishments.establishment_id`
  - `issuing_point_id -> issuing_points.issuing_point_id`
- Unique constraints:
  - `access_key`
  - `(issuer_id, document_type, establishment_id, issuing_point_id,
    sequence_number)`
- Important indexes:
  - `access_key`
  - `(issuer_id, document_type, establishment_id, issuing_point_id,
    sequence_number)`
- Delete/update restriction: referenced issuer, establishment, and issuing
  point rows must not be deleted or have key values changed while referenced by
  tax documents.
- Cascade deletes are not part of this feature.

**Traceability**: `FR-005`, `FR-006`, `FR-007`, `FR-008`, `FR-009`, `FR-010`,
`FR-013`, `FR-017`, `FR-021`, `FR-022`, `SC-002`, `SC-003`, `SC-004`,
`SC-008`, `SC-010`, `SC-011`.

## Persistence Entity Mapping

| Domain/Application Model | Persistence Record | Direction |
|--------------------------|--------------------|-----------|
| `Issuer` | `issuers` | Bidirectional adapter mapping |
| `Establishment` | `establishments` | Bidirectional adapter mapping |
| `IssuingPoint` | `issuing_points` | Bidirectional adapter mapping |
| `SequenceNumber` | `issuance_sequences` | Bidirectional adapter mapping |
| `TaxDocument` | `tax_documents` | Bidirectional adapter mapping |

Mapping rules:

- Persistence entities never leave `adapter.out.persistence`.
- Domain objects are not annotated as persistence entities.
- Mappers call a domain-owned restore path for persisted `TaxDocument` state.
- The restore path must preserve access key, document type, issuer,
  establishment, issuing point, sequence number, issue date, document state,
  authorization state, authorization number, and authorized timestamp.
- The restore path must not use the new-document constructor path that forces
  `PENDING` state for persisted historical documents.
- Mappers translate canonical enum values by name and reject unknown values.
- Mappers do not translate SRI XML/SOAP DTOs or SRI numeric codes.

## Deferred Data

- XML paths and XML storage metadata are deferred by PFV-PER-003.
- Audit event persistence is deferred by PFV-PER-004.
- Legacy compatibility views are deferred by PFV-PER-002.
- Invoice, credit note, debit note, withholding, and waybill line/tax details
  are out of scope for this foundation.
