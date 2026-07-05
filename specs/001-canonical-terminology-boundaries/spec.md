# Feature Specification: Canonical Terminology and Architecture Boundaries

**Feature Branch**: `1-ft-1`

**Created**: 2026-07-05

**Status**: Draft

**Input**: User description: "Define the canonical English terminology,
legacy-to-target mapping rules, and Clean Architecture boundaries for the new
Quarkus-based Ecuador electronic tax document service."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Establish canonical terminology (Priority: P1)

As a software architect or backend developer, I need an approved English
terminology model for the target service so that future specifications, APIs,
database objects, code, tests, and documentation use the same business language.

**Why this priority**: Naming decisions are foundational. Future feature specs
cannot safely model invoices, credit notes, debit notes, withholdings, waybills,
authorization retries, synchronization, or webhooks until shared target terms
exist.

**Independent Test**: Review a representative list of legacy Spanish terms and
verify that each term has an English target term, a classification, and a clear
decision status before planning any business feature.

**Acceptance Scenarios**:

1. **Given** a future feature references a legacy term such as "comprobante",
   **When** the feature specification is written, **Then** the specification uses
   "taxDocument" for target artifacts and records the legacy mapping.
2. **Given** a future feature references a term without an approved English
   target name, **When** the specification is reviewed, **Then** the term is
   recorded as a Pending Naming Decision before planning continues.

---

### User Story 2 - Separate SRI contract language from target language (Priority: P2)

As a software architect or backend developer, I need clear rules for official
SRI names so that legally required XML and SOAP contract terms remain accurate
without leaking into the target domain model, APIs, or database objects.

**Why this priority**: SRI interoperability must be preserved, but the target
system must not treat the external SRI contract as its internal language model.

**Independent Test**: Review sample SRI XML/SOAP terms and verify that each one
is either mapped to English target terminology or classified as SRI adapter-only
for contract-specific artifacts.

**Acceptance Scenarios**:

1. **Given** an official SRI XML tag uses Spanish terminology, **When** the term
   is needed by a future feature, **Then** the term is allowed only in SRI
   adapter contract artifacts, official fixtures, or migration mapping
   documentation.
2. **Given** a proposed target API field uses a Spanish SRI contract name,
   **When** the specification is reviewed, **Then** the field is rejected unless
   an explicit legacy compatibility exception is documented.

---

### User Story 3 - Define architecture boundaries for future specifications (Priority: P3)

As a software architect or backend developer, I need clear Clean Architecture
layer boundaries so that future specifications assign responsibilities to the
right layer and do not copy legacy coupling into the target service.

**Why this priority**: Boundary rules prevent future feature plans from placing
business rules in REST resources, persistence adapters, SRI adapters, or
framework wiring.

**Independent Test**: Review a future feature outline and verify that it
identifies domain responsibilities, application use cases, inbound adapters,
outbound adapters, and bootstrap responsibilities without cross-layer model
reuse.

**Acceptance Scenarios**:

1. **Given** a future feature describes a business operation, **When** the
   specification is reviewed, **Then** the operation is expressed as an explicit
   application use case instead of a generic service.
2. **Given** a future feature needs persistence, SRI communication, storage,
   queues, webhooks, or time access, **When** the specification is reviewed,
   **Then** the dependency is described as an application port implemented by an
   adapter.
3. **Given** a future feature has unresolved behavior from the legacy system,
   **When** the specification is reviewed, **Then** the behavior is recorded as
   Pending Functional Validation instead of being guessed into target rules.

### Edge Cases

- A legacy Spanish term and an official SRI contract term have the same spelling
  but different target meaning.
- A legacy database table or column name is Spanish and appears useful to copy
  into the target schema.
- A legacy payload name is still required by a compatibility adapter but is not
  acceptable for target APIs.
- A literal English translation exists but does not represent the correct
  business concept.
- A future feature attempts to introduce business functionality before the term
  mapping and layer ownership are decided.
- A future feature uses a generic name such as "DocumentService" or "Manager"
  that hides the business operation.

## Requirements *(mandatory)*

### Scope Boundaries

This architecture enabler includes:

- Canonical English terminology for the initial electronic tax document domain.
- Legacy Spanish term to target English term mappings.
- Naming rules for packages, classes, methods, DTOs, APIs, tables, columns,
  events, tests, and documentation.
- Terms that may remain Spanish only inside SRI contract or compatibility
  artifacts.
- Clean Architecture layer boundaries for domain, application, adapters, and
  bootstrap.
- Forbidden legacy terms and generic names for target artifacts.
- A structure for future specifications to record mapping, pending naming
  decisions, and pending functional validations.

This architecture enabler excludes Quarkus code, REST endpoints, database
migrations, persistence entities, SRI clients, invoice issuance, authentication,
authorization, webhooks, production data migration, and legacy refactoring.

### Functional Requirements

- **FR-001**: The specification MUST define a canonical English glossary for the
  initial Ecuador electronic tax document domain.
- **FR-002**: The specification MUST map each approved legacy Spanish term to a
  target English term and decision status.
- **FR-003**: The specification MUST classify each migrated concept as one of:
  Target domain concept, Target API field, Target database object, SRI
  adapter-only concept, Legacy compatibility concept, Migration-only concept,
  Deprecated concept, Pending Naming Decision, or Pending Functional Validation.
- **FR-004**: The specification MUST define naming rules for target packages,
  classes, methods, DTOs, APIs, database tables, database columns, events, tests,
  and documentation.
- **FR-005**: The specification MUST define which Spanish SRI contract names may
  appear only in SRI adapters, official fixtures, legacy compatibility adapters,
  migration scripts, or mapping documentation.
- **FR-006**: The specification MUST define forbidden Spanish legacy names and
  generic service names for target domain, application, API, and persistence
  artifacts.
- **FR-007**: The specification MUST require future specifications to record
  Pending Naming Decisions for unclear terms.
- **FR-008**: The specification MUST require future specifications to record
  Pending Functional Validation for unclear or unverified legacy behavior.
- **FR-009**: The specification MUST define clean layer ownership for domain,
  application, inbound REST adapters, outbound persistence adapters, outbound
  SRI adapters, outbound storage adapters, outbound queue adapters, outbound
  webhook adapters, and bootstrap.
- **FR-010**: The specification MUST define that future business operations are
  modeled as explicit use cases rather than generic service classes.
- **FR-011**: The specification MUST require separation of REST DTOs,
  application commands and results, domain objects, persistence entities, and
  SRI XML/SOAP DTOs.
- **FR-012**: The specification MUST state that AS-IS legacy documentation,
  migration mapping, architecture decisions, and target specifications remain
  separate artifact categories.

### Architectural Requirements

- **AR-001**: Target project identity MUST be recorded as group
  `com.alexastudillo`, artifact `ecuador-tax-document-service`, base package
  `com.alexastudillo.taxdocument`, backend-only service, Java 25, Maven, and
  Quarkus.
- **AR-002**: Target source dependencies MUST follow the direction
  `adapter -> application -> domain`, `bootstrap -> adapter`, and
  `bootstrap -> application`.
- **AR-003**: Domain concepts MUST remain independent from framework,
  persistence, transport, filesystem, messaging, XML, SOAP, SRI, HTTP client,
  and external API concerns.
- **AR-004**: Application use cases MUST be the entry point for business
  operations and MUST own orchestration, application validation, transaction
  boundaries, and external dependency ports.
- **AR-005**: Inbound adapters MUST only translate external input, perform
  transport validation, map errors, and call application input ports.
- **AR-006**: Outbound adapters MUST implement application ports and isolate
  persistence, SRI, storage, queue, webhook, time, and transaction details.
- **AR-007**: Bootstrap MUST be limited to runtime configuration and dependency
  wiring.
- **AR-008**: Business rules MUST NOT be assigned to REST resources,
  persistence adapters, SRI adapters, storage adapters, queue adapters, webhook
  adapters, or bootstrap configuration.
- **AR-009**: Future target database names MUST use English lowercase
  snake_case.
- **AR-010**: Future target API resources and operation names MUST use English
  business terminology and MUST NOT expose Spanish legacy names unless a legacy
  compatibility exception is documented.

### Naming and Migration Requirements

- **NR-001**: Target artifacts MUST use business-oriented English names, not
  literal translations.
- **NR-002**: The canonical target term for "comprobante" MUST be
  "taxDocument".
- **NR-003**: The canonical target term for "factura" MUST be "invoice".
- **NR-004**: The canonical target term for "nota credito" MUST be
  "creditNote".
- **NR-005**: The canonical target term for "nota debito" MUST be "debitNote".
- **NR-006**: The canonical target term for "retencion" MUST be "withholding".
- **NR-007**: The canonical target term for "guia remision" MUST be "waybill".
- **NR-008**: The canonical target term for "emisor" MUST be "issuer".
- **NR-009**: The canonical target term for "comprador" MUST be "buyer".
- **NR-010**: The canonical target term for "receptor" MUST be "recipient".
- **NR-011**: The canonical target term for "clave acceso" MUST be "accessKey".
- **NR-012**: The canonical target term for "numero autorizacion" MUST be
  "authorizationNumber".
- **NR-013**: The canonical target term for "fecha emision" MUST be
  "issueDate".
- **NR-014**: The canonical target term for "fecha autorizacion" MUST be
  "authorizedAt".
- **NR-015**: The canonical target term for "razon social" MUST be
  "legalName".
- **NR-016**: The canonical target term for "nombre comercial" MUST be
  "tradeName".
- **NR-017**: The canonical target term for "establecimiento" MUST be
  "establishment".
- **NR-018**: The canonical target term for "punto emision" MUST be
  "issuingPoint".
- **NR-019**: The canonical target term for "secuencial" MUST be
  "sequenceNumber".
- **NR-020**: Generic business behavior names such as `DocumentService`,
  `SriService`, `ProcessService`, `Manager`, `Helper`, and `Util` MUST be
  rejected for target business operations.

### Key Entities *(include if feature involves data)*

- **Canonical Term**: An approved English business term used by target
  specifications, code, APIs, database objects, tests, and documentation.
- **Legacy Term Mapping**: A relationship between a legacy Spanish term, its
  target English term, its allowed locations, and its decision status.
- **Architecture Boundary Rule**: A rule that assigns responsibilities to
  domain, application, adapter, or bootstrap layers.
- **SRI Contract Term**: An official SRI XML or SOAP term that may remain
  Spanish only inside approved contract-specific artifacts.
- **Pending Naming Decision**: A term whose target English name is not yet
  approved.
- **Pending Functional Validation**: A legacy behavior whose target behavior is
  not yet verified.
- **Compatibility Exception**: A documented exception that allows a legacy or
  SRI-specific name outside the normal target language rules for a bounded
  reason.

## Migration Classification *(mandatory for migrated concepts)*

| Legacy Concept | Target Name | Classification | Decision Status |
|----------------|-------------|----------------|-----------------|
| comprobante | taxDocument | Target domain concept | Decided |
| factura | invoice | Target domain concept | Decided |
| nota credito | creditNote | Target domain concept | Decided |
| nota debito | debitNote | Target domain concept | Decided |
| retencion | withholding | Target domain concept | Decided |
| guia remision | waybill | Target domain concept | Decided |
| emisor | issuer | Target domain concept | Decided |
| comprador | buyer | Target domain concept | Decided |
| receptor | recipient | Target domain concept | Decided |
| clave acceso | accessKey | Target domain concept | Decided |
| numero autorizacion | authorizationNumber | Target domain concept | Decided |
| fecha emision | issueDate | Target API field | Decided |
| fecha autorizacion | authorizedAt | Target API field | Decided |
| razon social | legalName | Target API field | Decided |
| nombre comercial | tradeName | Target API field | Decided |
| establecimiento | establishment | Target domain concept | Decided |
| punto emision | issuingPoint | Target domain concept | Decided |
| secuencial | sequenceNumber | Target domain concept | Decided |
| Official SRI XML tags | English target terms or contract-only names | SRI adapter-only concept | Decided |
| Legacy API payload names | English target terms or compatibility-only names | Legacy compatibility concept | Pending per feature |
| Legacy database object names | English target database names or migration-only names | Migration-only concept | Pending per feature |
| Unmapped legacy terms | To be assigned | Pending Naming Decision | Pending per feature |
| Unverified legacy behavior | To be validated | Pending Functional Validation | Pending per feature |

Allowed classifications: Target domain concept, Target API field, Target
database object, SRI adapter-only concept, Legacy compatibility concept,
Migration-only concept, Deprecated concept, Pending Naming Decision, Pending
Functional Validation.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of future migrated concepts in specifications have a target
  name or an explicit Pending Naming Decision before planning begins.
- **SC-002**: Reviewers can classify a representative set of 20 legacy names
  with at least 95% agreement using this specification.
- **SC-003**: 100% of future feature specifications identify whether touched
  behavior belongs to domain, application, inbound adapter, outbound adapter, or
  bootstrap before task generation.
- **SC-004**: 100% of official SRI Spanish names found in target artifacts are
  either isolated to approved SRI artifacts or documented as compatibility
  exceptions.
- **SC-005**: No future feature proceeds to implementation with undocumented
  Pending Naming Decisions, Pending Functional Validations, or architecture
  boundary exceptions.
- **SC-006**: A reviewer can determine from the specification alone whether a
  proposed target name, database name, API field, or layer assignment is allowed.

## Assumptions

- The initial canonical glossary from the project constitution is accepted as
  the starting terminology baseline.
- Legacy Spanish names found in AS-IS documentation, source code, payloads, and
  database structures are migration inputs, not target names.
- Official SRI XML and SOAP names must remain exact where required for external
  interoperability.
- This feature establishes governance for future specifications and does not
  create runtime behavior.
- Future business features will expand the mapping table as additional legacy
  concepts are discovered.
