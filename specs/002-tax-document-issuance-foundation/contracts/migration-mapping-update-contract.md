# Contract: Migration Mapping Update

## Target Artifacts

- `docs/architecture/canonical-terminology.md`
- `docs/migration/legacy-to-target-terminology.md`

## Purpose

Define the durable documentation updates required by this feature so later
features can consume foundation terms, state mappings, SRI code mappings, and
pending validations without relying only on temporary feature files.

## Required Canonical Terminology Updates

`docs/architecture/canonical-terminology.md` must include or reference the
foundation canonical terms introduced by this feature:

- `taxDocument`
- `documentType`
- `documentState`
- `authorizationState`
- `issuer`
- `establishment`
- `issuingPoint`
- `sequenceNumber`
- `accessKey`
- `authorizationNumber`
- `issueDate`
- `authorizedAt`
- `issuanceRequest`
- `issuanceResult`
- `issuanceMode`

## Required Migration Mapping Updates

`docs/migration/legacy-to-target-terminology.md` must include:

1. Common concept mappings from legacy Spanish terms to canonical English target
   terms.
2. SRI document code mappings:

   | SRI Contract Code | Target Document Type |
   |-------------------|----------------------|
   | `01` | `INVOICE` |
   | `04` | `CREDIT_NOTE` |
   | `05` | `DEBIT_NOTE` |
   | `06` | `WAYBILL` |
   | `07` | `WITHHOLDING` |

3. Legacy state mappings:

   | Legacy State | Target State |
   |--------------|--------------|
   | `PENDIENTE` | `PENDING` |
   | `EN_PROCESO` | `IN_PROGRESS` |
   | `RECIBIDO` | `RECEIVED` |
   | `AUTORIZADO` | `AUTHORIZED` |
   | `NO_AUTORIZADO` | `NOT_AUTHORIZED` |
   | `DEVUELTA` | `RETURNED` |
   | `RECHAZADO` | `REJECTED` |
   | `IRRECUPERABLE` | `IRRECOVERABLE` |
   | `ANULADO` | `VOIDED` |

4. Pending Functional Validations:

   - PFV-ISS-001: issuance mode runtime default.
   - PFV-ISS-002: legacy route compatibility.
   - PFV-ISS-003: synchronization scheduling.
   - PFV-ISS-004: retry policy and signed-XML precondition.
   - PFV-ISS-005: post-authorization corrections.

## Acceptance Checks

- Every mapping has one migration classification.
- SRI document codes are classified as SRI adapter-only concepts.
- Legacy states are classified as target domain concepts after accepted mapping.
- Pending Functional Validations include affected work, validation source or
  required source, decision due, and resolution handling.
- No Spanish legacy term is authorized in target domain/application code by
  this update.

## Traceability

- Spec `FR-003`, `FR-005`, `FR-017`, `FR-018`, `NR-005`, `SC-002`, `SC-003`
- Constitution Principle X: Specification-Governed Migration
