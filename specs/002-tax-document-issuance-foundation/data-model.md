# Data Model: Tax Document Issuance Foundation

This model describes domain and application concepts. It is not a persistence
schema and does not define REST DTOs, SRI XML DTOs, JPA entities, Panache
entities, or database migrations.

## TaxDocument

**Purpose**: Common aggregate for electronic tax document issuance lifecycle
behavior.

**Fields**:

- `documentType`: one `DocumentType`.
- `issuer`: one `Issuer`.
- `establishment`: one `Establishment`.
- `issuingPoint`: one `IssuingPoint`.
- `sequenceNumber`: one `SequenceNumber`.
- `accessKey`: one `AccessKey`.
- `issueDate`: one `IssueDate`.
- `documentState`: one `DocumentState`.
- `authorizationState`: one `AuthorizationState`.
- `authorizationNumber`: optional `AuthorizationNumber`.
- `authorizedAt`: optional `AuthorizedAt`.
- `issuanceMode`: one `IssuanceMode`.
- `externalRequestId`: optional caller-provided idempotency identifier.

**Relationships**:

- References one issuer, one establishment, and one issuing point.
- Owns one document type, sequence number, access key, document state, and
  authorization state.
- May have authorization number and authorized timestamp only when authorized.

**Validation Rules**:

- `accessKey` must be structurally valid and 49 digits.
- `documentType` must be one supported canonical type.
- `AUTHORIZED`, `VOIDED`, and `IRRECOVERABLE` are terminal unless a future spec
  defines a controlled operation.
- Authorized documents are immutable by default.
- Local voiding rejects `AUTHORIZED` and `VOIDED` documents.
- State transitions must follow the common transition table.

**Traceability**: Spec `FR-001`, `FR-002`, `FR-004`, `FR-008`, `FR-009`,
`FR-011`, `AR-001`, `AR-003`.

## DocumentType

**Purpose**: Canonical document type model used internally by domain and
application behavior.

**Values**:

| Value | SRI Contract Code |
|-------|-------------------|
| `INVOICE` | `01` |
| `CREDIT_NOTE` | `04` |
| `DEBIT_NOTE` | `05` |
| `WAYBILL` | `06` |
| `WITHHOLDING` | `07` |

**Validation Rules**:

- Internal code must use the canonical English value.
- SRI numeric codes are external mappings and must not drive package, class,
  method, use case, or database names.

**Traceability**: Spec `FR-002`, `FR-003`, `NR-004`, `SC-002`.

## DocumentState

**Purpose**: Internal lifecycle state for a tax document.

**Values**:

- `PENDING`
- `IN_PROGRESS`
- `RECEIVED`
- `AUTHORIZED`
- `NOT_AUTHORIZED`
- `RETURNED`
- `REJECTED`
- `IRRECOVERABLE`
- `VOIDED`

**State Transitions**:

| From | To | Rule |
|------|----|------|
| `PENDING` | `IN_PROGRESS` | Processing, retry, or synchronization starts. |
| `IN_PROGRESS` | `RECEIVED` | SRI reception confirms receipt. |
| `IN_PROGRESS` | `RETURNED` | SRI reception or validation returns the document. |
| `IN_PROGRESS` | `REJECTED` | Processing confirms rejection before authorization. |
| `RECEIVED` | `AUTHORIZED` | Authorization confirms authorization. |
| `RECEIVED` | `NOT_AUTHORIZED` | Authorization confirms non-authorization. |
| `RECEIVED` | `REJECTED` | Authorization or synchronization confirms rejection. |
| `RETURNED` | `IN_PROGRESS` | Retry processing is allowed and starts. |
| `REJECTED` | `IN_PROGRESS` | Retry processing is allowed and starts. |
| `PENDING` | `VOIDED` | Local voiding is allowed before authorization. |
| `IN_PROGRESS` | `VOIDED` | Local voiding is allowed only if no authorization occurred and future rules permit it. |
| `RETURNED` | `VOIDED` | Local voiding is allowed when retry is not desired and no authorization occurred. |
| `REJECTED` | `VOIDED` | Local voiding is allowed when no authorization occurred and future rules permit it. |
| `NOT_AUTHORIZED` | `VOIDED` | Local voiding is allowed only when future rules confirm it is not an SRI cancellation process. |
| `RETURNED` | `IRRECOVERABLE` | Future rules mark the document terminally unrecoverable. |
| `REJECTED` | `IRRECOVERABLE` | Future rules mark the document terminally unrecoverable. |
| `NOT_AUTHORIZED` | `IRRECOVERABLE` | Future rules mark the document terminally unrecoverable. |

**Validation Rules**:

- Transitions not listed are rejected unless a future specification adds a
  controlled transition.
- `AUTHORIZED`, `VOIDED`, and `IRRECOVERABLE` are terminal for this foundation.

**Traceability**: Spec `FR-005`, `FR-008`, `FR-009`, `FR-011`, `SC-003`.

## AuthorizationState

**Purpose**: Internal representation of authorization lifecycle outcomes
without using SRI XML/SOAP DTOs as the domain model.

**Values**:

- `NOT_SUBMITTED`
- `SUBMITTED`
- `RECEIVED`
- `AUTHORIZED`
- `NOT_AUTHORIZED`
- `RETURNED`
- `REJECTED`

**Validation Rules**:

- Authorization state must remain separate from `DocumentState`.
- SRI adapter responses must be mapped into this model by future SRI adapter
  features without leaking SRI DTOs inward.

**Traceability**: Spec `FR-006`, `AR-008`.

## Issuer

**Purpose**: Legal tax document issuer concept used for access policy,
issuance identity, sequence ownership, and audit traceability.

**Fields**:

- `issuerId`: target identity value.
- `legalIdentifier`: issuer tax identifier.
- `legalName`: canonical issuer legal name when available.
- `tradeName`: canonical issuer trade name when available.

**Validation Rules**:

- Issuer access checks are application validation through
  `IssuerAccessPolicyPort`.
- Spanish legacy field names must not be used in target domain/application
  artifacts.

**Traceability**: Spec `FR-007`, `FR-012`, `NR-001`.

## Establishment

**Purpose**: Issuer establishment involved in document numbering and issuance
identity.

**Fields**:

- `establishmentId`: target identity value.
- `code`: establishment code as target value.
- `issuerId`: issuer relationship.

**Validation Rules**:

- Must belong to the issuer context used by the tax document.
- Active/inactive validation is application-level and belongs to future use
  cases through ports.

**Traceability**: Spec `FR-007`, `FR-012`.

## IssuingPoint

**Purpose**: Issuing point within an establishment used for sequence assignment
and document identity.

**Fields**:

- `issuingPointId`: target identity value.
- `code`: issuing point code as target value.
- `establishmentId`: establishment relationship.

**Validation Rules**:

- Must belong to the establishment context used by the tax document.
- Active issuing point validation belongs to application use cases.

**Traceability**: Spec `FR-007`, `FR-012`.

## SequenceNumber

**Purpose**: Issuance sequence assigned for issuer, establishment, issuing
point, and document type.

**Fields**:

- `value`: sequence value.
- `documentType`: associated document type.
- `issuer`: associated issuer.
- `establishment`: associated establishment.
- `issuingPoint`: associated issuing point.

**Validation Rules**:

- Sequence assignment must be idempotent for the same issuer, establishment,
  issuing point, document type, and sequence value.
- Sequence availability is application validation through `SequenceNumberPort`.

**Traceability**: Spec `FR-007`, `FR-015`, `SC-008`.

## AccessKey

**Purpose**: Value object for the Ecuador electronic tax document access key.

**Fields**:

- `value`: exactly 49 digits.

**Validation Rules**:

- Must contain only digits.
- Must be exactly 49 characters.
- Structural validation is domain validation.
- Generation is accessed through `AccessKeyGeneratorPort`.

**Traceability**: Spec `FR-004`, `FR-012`, `FR-015`.

## AuthorizationNumber

**Purpose**: Authorization identifier assigned when the tax document is
authorized.

**Fields**:

- `value`: authorization identifier.

**Validation Rules**:

- May be absent until authorization succeeds.
- Must not be used as a substitute for `AccessKey`.

**Traceability**: Spec `FR-001`, `FR-006`.

## IssueDate

**Purpose**: Date on which the tax document is issued.

**Fields**:

- `value`: issue date.

**Validation Rules**:

- Date generation or current time access must use `ClockPort` in application
  orchestration.

**Traceability**: Spec `FR-001`, `FR-012`.

## AuthorizedAt

**Purpose**: Timestamp at which authorization is received.

**Fields**:

- `value`: authorization timestamp.

**Validation Rules**:

- May be absent until authorization succeeds.
- Must be set consistently with `AuthorizationState.AUTHORIZED`.

**Traceability**: Spec `FR-001`, `FR-006`, `FR-012`.

## IssuanceMode

**Purpose**: Canonical mode concept for synchronous or asynchronous issuance
intent.

**Values**:

- `SYNCHRONOUS`
- `ASYNCHRONOUS`

**Validation Rules**:

- Runtime default remains Pending Functional Validation PFV-ISS-001.
- Mode must not introduce queue implementation dependencies into domain or
  application models.

**Traceability**: Spec `FR-017`, PFV-ISS-001.

## IssuanceRequest

**Purpose**: Application input model for future document-specific issuance use
cases.

**Fields**:

- `documentType`
- `issuer`
- `establishment`
- `issuingPoint`
- `sequenceNumber` or sequence request reference.
- `issueDate`
- `issuanceMode`
- `externalRequestId` when supplied.
- common metadata needed for idempotency and audit correlation.

**Validation Rules**:

- Must be an application model, not a REST DTO.
- Must use canonical English field names.
- Must not include SRI XML, SOAP, persistence, queue, filesystem, or HTTP
  client types.

**Traceability**: Spec `FR-014`, `FR-015`, `FR-019`.

## IssuanceResult

**Purpose**: Application result model for future issuance use cases.

**Fields**:

- `accessKey`
- `documentState`
- `authorizationState`
- `authorizationNumber` when present.
- `authorizedAt` when present.
- queued or completed outcome.
- audit correlation reference.
- application-level errors when applicable.

**Validation Rules**:

- Must be an application model, not a REST response DTO or persistence entity.
- Must not expose SRI DTOs, queue jobs, storage paths, or framework results.

**Traceability**: Spec `FR-014`, `FR-019`.

## Application Ports

**Purpose**: Output dependency boundaries owned by the application layer.

| Port | Input/Output Contract |
|------|-----------------------|
| `TaxDocumentRepository` | Uses domain/application identifiers and `TaxDocument`; returns domain/application results only. |
| `IssuerAccessPolicyPort` | Uses issuer context and actor/context identifiers; returns access decision without REST or security framework types. |
| `SequenceNumberPort` | Reserves or validates sequence values idempotently through target identifiers. |
| `AccessKeyGeneratorPort` | Generates `AccessKey` from approved target inputs. |
| `SriAuthorizationPort` | Represents SRI interactions as application-level operations without XML, SOAP, or HTTP types. |
| `XmlStoragePort` | Stores/retrieves XML artifacts by target identifiers without filesystem or storage SDK types. |
| `TaxDocumentQueuePort` | Requests asynchronous issuance work without queue implementation types. |
| `WebhookPublisherPort` | Publishes canonical events without remote HTTP delivery details. |
| `ClockPort` | Supplies current date/time for deterministic application behavior. |
| `TransactionPort` | Wraps application operations without persistence framework types. |
| `AuditLogPort` | Appends audit events and safe metadata. |

**Validation Rules**:

- Ports are application-layer contracts.
- Adapters must implement ports in future features.
- Port signatures must not contain adapter DTOs or framework-specific types.

**Traceability**: Spec `FR-012`, `AR-008`, `SC-004`.

