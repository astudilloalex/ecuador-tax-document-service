# Contract: Application Port Contracts

## Target Consumers

Future application use cases and outbound adapter specifications.

## Purpose

Define the required application-layer output ports for the common tax document
issuance foundation. This contract defines responsibilities and boundary rules;
it does not define adapter implementations.

## Required Ports

| Port | Required Responsibility | Forbidden Leakage |
|------|-------------------------|-------------------|
| `TaxDocumentRepository` | Persist, load, and query `TaxDocument` using target identifiers; detect duplicate access keys and duplicate issuance identities. | Persistence entities, SQL types, Hibernate, Panache, PostgreSQL details. |
| `IssuerAccessPolicyPort` | Decide whether the current application context may issue or inspect documents for an issuer, establishment, or issuing point. | REST security context, HTTP request types, framework annotations. |
| `SequenceNumberPort` | Reserve, assign, and validate sequence numbers idempotently for issuer, establishment, issuing point, and document type. | Database locks, SQL exceptions, persistence framework types. |
| `AccessKeyGeneratorPort` | Generate an `AccessKey` from approved domain/application inputs. | SRI XML DTOs, REST DTOs, persistence entities. |
| `SriAuthorizationPort` | Represent SRI reception, authorization, retry, and synchronization as application-level operations. | SOAP DTOs, XML models, HTTP clients, official SRI DTOs. |
| `XmlStoragePort` | Store and retrieve XML artifacts through target identifiers. | Filesystem paths as domain identifiers, storage SDK types. |
| `TaxDocumentQueuePort` | Request asynchronous issuance work in application terms. | Queue job classes, Redis/BullMQ concepts, messaging SDK types. |
| `WebhookPublisherPort` | Publish canonical tax document events for future webhook delivery. | Remote HTTP request/response types, delivery adapter DTOs. |
| `ClockPort` | Provide date/time values for deterministic tests and application decisions. | Direct system clock calls inside domain behavior. |
| `TransactionPort` | Provide application transaction boundaries. | Persistence framework transaction objects. |
| `AuditLogPort` | Append audit events and safe metadata. | Secrets, private keys, credentials, tokens, signing passwords, certificates, raw private key material. |

## Acceptance Checks

- Each port is owned by the application layer.
- No port signature uses REST DTOs, persistence entities, SRI XML/SOAP DTOs,
  queue job models, filesystem types, HTTP client types, or framework-specific
  classes.
- Each future adapter implementation maps external models at the adapter
  boundary before returning application/domain results.
- Domain tests can run without these ports.
- Application tests can use in-memory fakes or test doubles for these ports.

## Traceability

- Spec `FR-012`, `FR-013`, `FR-014`, `FR-015`, `FR-016`, `AR-008`
- Constitution Principle IV: Ports and Adapters for External Dependencies
- Constitution Principle VII: DTO and Validation Separation
- Constitution Principle VIII: Idempotency and Auditability
- Constitution Principle IX: Layered Testing Requirements
