# Feature Specification: [FEATURE NAME]

**Feature Branch**: `[###-feature-name]`

**Created**: [DATE]

**Status**: Draft

**Input**: User description: "$ARGUMENTS"

## User Scenarios & Testing *(mandatory)*

<!--
  User stories must be prioritized as independently testable user journeys.
  Assign priorities (P1, P2, P3, etc.), where P1 is the most critical.
  Each story must describe the business value it can deliver on its own.
-->

### User Story 1 - [Brief Title] (Priority: P1)

[Describe this user journey in plain language]

**Why this priority**: [Explain the value and why it has this priority level]

**Independent Test**: [Describe how this can be tested independently]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]
2. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

### User Story 2 - [Brief Title] (Priority: P2)

[Describe this user journey in plain language]

**Why this priority**: [Explain the value and why it has this priority level]

**Independent Test**: [Describe how this can be tested independently]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

### User Story 3 - [Brief Title] (Priority: P3)

[Describe this user journey in plain language]

**Why this priority**: [Explain the value and why it has this priority level]

**Independent Test**: [Describe how this can be tested independently]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

[Add more user stories as needed, each with an assigned priority]

### Edge Cases

- [Boundary condition]
- [Error scenario]
- [Duplicate or retry scenario]
- [SRI unavailability or delayed authorization scenario, if applicable]

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST [specific business capability]
- **FR-002**: System MUST [specific validation or rule]
- **FR-003**: System MUST [specific persistence or retrieval behavior]
- **FR-004**: System MUST [specific integration behavior]
- **FR-005**: System MUST [specific audit or idempotency behavior]

*Example of marking unclear requirements:*

- **FR-006**: System MUST use [NEEDS CLARIFICATION: unresolved business rule]
- **FR-007**: System MUST classify [NEEDS CLARIFICATION: legacy concept lacks
  target classification]

### Architectural Requirements

- **AR-001**: Business behavior MUST be implemented in domain or application
  use cases, not REST resources, repositories, SRI adapters, or configuration.
- **AR-002**: External dependencies MUST be accessed through application ports.
- **AR-003**: SRI XML, SOAP, signing, reception, authorization, response
  parsing, and official Spanish SRI names MUST remain in `adapter.out.sri`.
- **AR-004**: REST DTOs, application commands/results, domain objects,
  persistence entities, and SRI DTOs MUST remain separate.
- **AR-005**: Domain and application behavior MUST be testable without real
  PostgreSQL, Redis, SRI, filesystems, or external HTTP services.

### Naming and Migration Requirements

- **NR-001**: Target code, APIs, DTOs, database objects, events, tests, and
  documentation MUST use English canonical terminology.
- **NR-002**: Spanish legacy names MUST NOT appear in target domain,
  application, API, or persistence artifacts.
- **NR-003**: Official Spanish SRI names MAY appear only in allowed SRI adapter,
  fixture, legacy compatibility, migration, or mapping artifacts.
- **NR-004**: Unclear terms MUST be registered as Pending Naming Decisions.

### Key Entities *(include if feature involves data)*

- **[Entity 1]**: [What it represents, key attributes without implementation]
- **[Entity 2]**: [What it represents, relationships to other entities]

## Migration Classification *(mandatory for migrated concepts)*

| Legacy Concept | Target Name | Classification | Decision Status |
|----------------|-------------|----------------|-----------------|
| [legacy] | [target] | [classification] | [decided/pending] |

Allowed classifications: Target domain concept, Target API field, Target
database object, SRI adapter-only concept, Legacy compatibility concept,
Migration-only concept, Deprecated concept, Pending Naming Decision, Pending
Functional Validation.

## Idempotency and Audit Requirements *(include if feature is critical)*

**Idempotency Scope**: [Tax document issuance, SRI retry, synchronization,
webhook delivery, XML generation, sequence assignment, or N/A]

**Audit Events**: [Issuance requested, XML generated, XML signed, SRI reception
submitted, SRI authorization received, document authorized, document rejected,
authorization retry requested, webhook delivery attempted, webhook delivery
failed, synchronization executed, or N/A]

**Sensitive Data Exclusions**: Audit logs MUST NOT contain secrets, private
keys, credentials, tokens, signing passwords, or sensitive configuration values.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: [Measurable business outcome]
- **SC-002**: [Measurable correctness outcome]
- **SC-003**: [Measurable reliability or idempotency outcome]
- **SC-004**: [Measurable integration or auditability outcome]

## Assumptions

- [Assumption about target users or upstream systems]
- [Assumption about scope boundaries]
- [Assumption about legacy data or migration input]
- [Dependency on existing system, SRI behavior, or external service]
