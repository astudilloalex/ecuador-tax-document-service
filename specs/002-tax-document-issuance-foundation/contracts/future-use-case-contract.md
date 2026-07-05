# Contract: Future Use Case Contract

## Target Consumers

Future document-specific use case specifications, plans, tasks, and reviews.

## Purpose

Define the common behavior contract that future tax document use cases must
follow when using the issuance foundation.

## Covered Future Use Cases

- `IssueInvoiceUseCase`
- `IssueCreditNoteUseCase`
- `IssueDebitNoteUseCase`
- `IssueWithholdingUseCase`
- `IssueWaybillUseCase`
- `RetrySriAuthorizationUseCase`
- `SynchronizeTaxDocumentsUseCase`
- `DeliverWebhookUseCase`

## Required Use Case Flow Rules

1. Accept application commands or queries, not REST DTOs.
2. Validate issuer-scoped access through `IssuerAccessPolicyPort` when issuer,
   establishment, or issuing point scope is involved.
3. Apply idempotency checks before sequence assignment, SRI submission, queue
   publication, XML generation, or webhook publication.
4. Reserve or validate sequence numbers through `SequenceNumberPort`.
5. Generate access keys through `AccessKeyGeneratorPort`.
6. Persist common document state through `TaxDocumentRepository`.
7. Use `TransactionPort` for atomic application-level operations.
8. Use `ClockPort` for time-dependent application decisions.
9. Emit audit events through `AuditLogPort`.
10. Access SRI, XML storage, queues, and webhooks only through application
    ports.
11. Return application results, not REST DTOs, persistence entities, SRI DTOs,
    queue job models, storage SDK responses, or framework results.

## Required Validation Ownership

| Validation Area | Owning Layer |
|-----------------|--------------|
| JSON shape, date format, enum syntax, path parameters | Future inbound REST adapter |
| Authorization to issue, active issuing point, duplicate access key, sequence availability, retry eligibility, idempotency | Application |
| Access key structure, state transitions, totals, immutable authorized documents | Domain |

## Required State and Idempotency Rules

- Authorized documents are immutable by default.
- Local voiding rejects `AUTHORIZED` and `VOIDED` documents.
- Retry behavior must not be implemented for PFV-ISS-004 until retry behavior
  and signed-XML preconditions are validated.
- Runtime issuance mode defaults must not be implemented until PFV-ISS-001 is
  resolved by a future affected feature.
- Repeated issuance requests with the same accepted idempotency key must not
  create a second tax document.

## Required Audit Event Names

Future use cases must use these canonical audit event names when the
corresponding operation is in scope. This contract defines names only; adapter
logging, webhook delivery, and external observability implementations remain
out of scope for this feature.

- `TaxDocumentIssuanceRequested`
- `TaxDocumentQueuedForIssuance`
- `TaxDocumentXmlGenerated`
- `TaxDocumentSigned`
- `TaxDocumentSubmittedToSri`
- `TaxDocumentReceivedBySri`
- `TaxDocumentAuthorized`
- `TaxDocumentRejected`
- `TaxDocumentAuthorizationRetryRequested`
- `TaxDocumentVoided`

## Forbidden Names

Future use cases must not introduce business behavior classes named:

- `DocumentService`
- `SriService`
- `ProcessService`
- `Manager`
- `Helper`
- `Util`

## Acceptance Checks

- A future use case can be reviewed against the 11 flow rules above.
- Every external dependency is represented by an application port.
- No adapter model enters the application or domain layer.
- Any unresolved PFV affecting the future use case is resolved, excluded, or
  deferred before affected tasks are generated.

## Traceability

- Spec `FR-008` through `FR-019`, `AR-003`, `AR-007`, `AR-008`
- Constitution Principle III: Use Case-Centered Design
- Constitution Principle IV: Ports and Adapters for External Dependencies
- Constitution Principle VII: DTO and Validation Separation
- Constitution Principle VIII: Idempotency and Auditability
