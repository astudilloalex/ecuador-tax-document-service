# Research: Tax Document Issuance Foundation

## Decision: Limit implementation scope to domain and application foundations

**Rationale**: The specification explicitly excludes REST endpoints,
persistence adapters, SRI SOAP/XML adapters, storage adapters, queue adapters,
webhook adapters, bootstrap wiring, and database migrations. The first source
structure should establish core concepts and ports without creating runtime
infrastructure coupling.

**Alternatives considered**:

- Create starter adapter packages: rejected because it would make out-of-scope
  boundaries look implemented and invite business logic leakage.
- Keep the feature documentation-only: rejected because the feature goal
  includes establishing the first domain and application source foundation in
  later implementation tasks.

## Decision: Use a single common `TaxDocument` foundation for shared lifecycle behavior

**Rationale**: The legacy system supports invoices, credit notes, debit notes,
withholdings, and waybills through common issuance, access key, state,
authorization, retry, synchronization, XML, and webhook concerns. A shared
foundation prevents duplicated lifecycle rules across future document-specific
use cases.

**Alternatives considered**:

- Start with invoice-specific classes only: rejected because it would duplicate
  common rules across later document types.
- Create one model per document type now: rejected because type-specific tax
  calculations and line rules are out of scope.

## Decision: Separate `DocumentState` from `AuthorizationState`

**Rationale**: Internal processing state and SRI authorization outcome are
related but not identical. Separating them preserves SRI contract isolation and
allows future synchronization, retry, and webhook flows to reason about local
workflow and SRI outcome independently.

**Alternatives considered**:

- Use one combined state enum: rejected because it conflates local workflow
  with SRI authorization semantics.
- Treat SRI authorization state as the only state model: rejected because SRI is
  an external system, not the domain model.

## Decision: Treat SRI document codes as external contract mappings

**Rationale**: Codes `01`, `04`, `05`, `06`, and `07` are official SRI values
needed for interoperability, but internal names must remain canonical English.
Codes belong in SRI mapping documentation and future SRI adapter code, not in
package names, class names, use case names, or domain language.

**Alternatives considered**:

- Use numeric codes as enum names: rejected because it leaks the SRI contract
  into the domain model.
- Store codes only in future SRI adapter specs: rejected because the foundation
  must define the document type mapping expected by all future features.

## Decision: Accept `VOIDED` for legacy `ANULADO`

**Rationale**: AS-IS evidence describes local voiding behavior that rejects
authorized and already voided documents. `VOIDED` best represents a local target
state without implying an SRI cancellation workflow.

**Alternatives considered**:

- Use `CANCELLED`: rejected because it may imply a broader legal cancellation
  process not validated for this foundation.
- Register a Pending Naming Decision: rejected because the specification
  already accepts `VOIDED` and documents the rationale.

## Decision: Keep retry candidates as constrained pending behavior

**Rationale**: AS-IS evidence identifies `RETURNED`, `REJECTED`, `PENDING`, and
`IN_PROGRESS` as retry candidate states and requires a signed XML precondition.
The foundation should model candidate eligibility but must not implement retry
behavior until the retry use case validates exact behavior.

**Alternatives considered**:

- Fully implement retry rules now: rejected because SRI adapter and XML storage
  implementation are out of scope.
- Omit retry states from the foundation: rejected because future retry behavior
  depends on common state language and idempotency.

## Decision: Define application output ports now, adapters later

**Rationale**: Future issuance use cases need stable dependency boundaries for
persistence, issuer access, sequence assignment, access key generation, SRI,
XML storage, queues, webhooks, time, transactions, and audit logging. Defining
ports now preserves Clean Architecture while keeping adapters out of scope.

**Alternatives considered**:

- Define ports only when each adapter is implemented: rejected because future
  use case contracts would lack stable boundaries.
- Define adapter DTOs with ports: rejected because DTO separation forbids
  adapter models from leaking inward.

## Decision: Use domain/application unit tests without Quarkus

**Rationale**: The constitution requires domain tests to run without Quarkus and
application tests to run without real infrastructure. This feature touches only
domain and application behavior, so test strategy should use in-memory fakes or
test doubles for application ports and pure unit tests for domain rules.

**Alternatives considered**:

- Use Quarkus test support for all tests: rejected because it is unnecessary
  and would couple the foundation to framework runtime.
- Defer tests to adapter features: rejected because state transitions,
  idempotency contracts, value objects, and port orchestration rules are part of
  this feature.

## Decision: Update durable migration and terminology documentation through tasks

**Rationale**: The constitution requires durable locations for migration
mappings and architecture terminology. The feature plan and contracts define
the required updates, while actual document edits belong in task execution so
they can be traceable.

**Alternatives considered**:

- Keep state mappings only in the feature spec: rejected because mappings would
  be temporary and harder for later features to consume.
- Update durable docs during planning: rejected because `/speckit-plan` should
  generate design artifacts, while tasks should perform the planned updates.

## Decision: Defer runtime issuance mode default

**Rationale**: AS-IS evidence shows synchronous and asynchronous issuance
behavior, but this feature excludes queue implementation and document-specific
issuance flows. `IssuanceMode` is modeled as a common concept; the runtime
default belongs to a future issuance or queue specification.

**Alternatives considered**:

- Choose asynchronous-only now: rejected because it would affect runtime
  behavior outside this foundation.
- Choose synchronous-only now: rejected because it would ignore AS-IS evidence
  without a validating feature.

