# Legacy-To-Target Terminology Mapping

This document is the migration source of truth for approved canonical terms,
legacy Spanish term mappings, SRI adapter-only terms, pending naming decisions,
pending functional validations, and compatibility exceptions.

The target system uses English canonical terminology. Legacy Spanish names are
migration inputs and must not be copied into target domain, application, API, or
persistence artifacts unless an explicit compatibility exception exists.

## Classification Rules

Every migrated concept entry must have one primary classification. If the same concept appears in multiple artifact surfaces, each surface-specific mapping must be documented explicitly:

- Target domain concept
- Target API field
- Target database object
- SRI adapter-only concept
- Legacy compatibility concept
- Migration-only concept
- Deprecated concept
- Pending Naming Decision
- Pending Functional Validation

Allowed target locations depend on classification. Target domain concepts,
target API fields, and target database objects must use English canonical
terminology. SRI adapter-only concepts may retain exact official SRI names only
inside approved SRI, fixture, compatibility, migration, or mapping artifacts.
Migration-only concepts may appear in migration scripts or mapping documents.
Deprecated concepts must not be introduced into target artifacts.

## Approved Baseline Mappings

| Legacy Term | Target Term | Classification | Decision Status |
|-------------|-------------|----------------|-----------------|
| comprobante | taxDocument | Target domain concept | Decided |
| factura | invoice | Target domain concept | Decided |
| nota credito | creditNote | Target domain concept | Decided |
| nota debito | debitNote | Target domain concept | Decided |
| retencion | withholding | Target domain concept | Decided |
| guia remision | waybill | Target domain concept | Decided |
| emisor | issuer | Target domain concept | Decided |
| comprador | buyer | Target domain concept | Decided |
| receptor | recipient | Target domain concept | Decided |
| clave acceso | accessKey | Target domain concept | Target domain concept / value object |
| numero autorizacion | authorizationNumber | Target domain concept | Target domain concept / value object |
| fecha emision | issueDate | Target API field | Target domain |
| fecha autorizacion | authorizedAt | Target API field | Target domain |
| razon social | legalName | Target API field | Target domain |
| nombre comercial | tradeName | Target API field | Target domain |
| establecimiento | establishment | Target domain concept | Decided |
| punto emision | issuingPoint | Target domain concept | Decided |
| secuencial | sequenceNumber | Target domain concept | Decided |

## Issuance Foundation Concept Mappings

Feature `002-tax-document-issuance-foundation` confirms the following common
issuance concepts for target domain and application artifacts.

| Legacy Term or Source Concept | Target Term | Classification | Decision Status |
|-------------------------------|-------------|----------------|-----------------|
| comprobante | taxDocument | Target domain concept | Decided |
| factura | invoice / INVOICE | Target domain concept | Decided |
| nota credito | creditNote / CREDIT_NOTE | Target domain concept | Decided |
| nota debito | debitNote / DEBIT_NOTE | Target domain concept | Decided |
| retencion | withholding / WITHHOLDING | Target domain concept | Decided |
| guia remision | waybill / WAYBILL | Target domain concept | Decided |
| emisor | issuer | Target domain concept | Decided |
| establecimiento | establishment | Target domain concept | Decided |
| punto emision | issuingPoint | Target domain concept | Decided |
| secuencial | sequenceNumber | Target domain concept | Decided |
| clave acceso | accessKey | Target domain concept | Decided |
| numero autorizacion | authorizationNumber | Target domain concept | Decided |
| fecha emision | issueDate | Target domain concept | Decided |
| fecha autorizacion | authorizedAt | Target domain concept | Decided |

## SRI Document Code Mappings

SRI document type codes are external contract values. They are classified as
SRI adapter-only concepts and must not drive target package, class, method, use
case, API, persistence, or database names.

| SRI Contract Code | Target Document Type | Classification | Decision Status |
|-------------------|----------------------|----------------|-----------------|
| `01` | `INVOICE` | SRI adapter-only concept | Decided |
| `04` | `CREDIT_NOTE` | SRI adapter-only concept | Decided |
| `05` | `DEBIT_NOTE` | SRI adapter-only concept | Decided |
| `06` | `WAYBILL` | SRI adapter-only concept | Decided |
| `07` | `WITHHOLDING` | SRI adapter-only concept | Decided |

## Legacy State Mappings

Legacy document states are mapped to canonical English target states for the
common tax document lifecycle.

| Legacy State | Target State | Classification | Decision Status |
|--------------|--------------|----------------|-----------------|
| `PENDIENTE` | `PENDING` | Target domain concept | Decided |
| `EN_PROCESO` | `IN_PROGRESS` | Target domain concept | Decided |
| `RECIBIDO` | `RECEIVED` | Target domain concept | Decided |
| `AUTORIZADO` | `AUTHORIZED` | Target domain concept | Decided |
| `NO_AUTORIZADO` | `NOT_AUTHORIZED` | Target domain concept | Decided |
| `DEVUELTA` | `RETURNED` | Target domain concept | Decided |
| `RECHAZADO` | `REJECTED` | Target domain concept | Decided |
| `IRRECUPERABLE` | `IRRECOVERABLE` | Target domain concept | Decided |
| `ANULADO` | `VOIDED` | Target domain concept | Decided |

`ANULADO` is accepted as `VOIDED` for this foundation because the validated
behavior is local voiding before authorization. A future SRI cancellation or
annulment process must be specified as a separate behavior.

## SRI Adapter-Only Terms

SRI is an external contract, not the internal domain model. Official SRI XML and
SOAP names must remain exact where the SRI contract requires them, but they are
allowed only in SRI XML mappers, SRI SOAP DTOs, SRI adapter tests, official SRI
fixture files, legacy compatibility adapters, migration scripts, and mapping
documents.

SRI contract terms must not appear in internal domain models, application
commands, REST APIs, persistence entities, or target database objects unless a
bounded compatibility exception is documented.

Initial SRI contract examples:

| Official SRI Name | Target Term | Classification | Allowed Locations | Internal Model Allowed |
|-------------------|-------------|----------------|-------------------|------------------------|
| `claveAcceso` | accessKey | SRI adapter-only concept | SRI mappers, SRI DTOs, fixtures, adapter tests, migration mapping | No |
| `numeroAutorizacion` | authorizationNumber | SRI adapter-only concept | SRI mappers, SRI DTOs, fixtures, adapter tests, migration mapping | No |
| `fechaAutorizacion` | authorizedAt | SRI adapter-only concept | SRI mappers, SRI DTOs, fixtures, adapter tests, migration mapping | No |
| `razonSocial` | legalName | SRI adapter-only concept | SRI mappers, SRI DTOs, fixtures, adapter tests, migration mapping | No |
| `nombreComercial` | tradeName | SRI adapter-only concept | SRI mappers, SRI DTOs, fixtures, adapter tests, migration mapping | No |

Future features must add exact official SRI XML or SOAP names when those
contracts are touched.

## Pending Naming Decisions

Current baseline status: no Pending Naming Decisions are open for the approved
baseline terms.

New unmapped legacy terms must be registered in this section before affected
planning continues.

Required record fields:

| Field | Requirement |
|-------|-------------|
| Legacy term | The unresolved legacy or source term |
| Candidate terms | Proposed English target names, if known |
| Affected artifacts | Specifications, APIs, database objects, code, tests, or docs blocked by the decision |
| Owner | Role responsible for resolution |
| Decision due | `beforeTaskGeneration` |
| Final decision | Approved target term when resolved |

Pending Naming Decisions may be recorded during specification or planning, but
affected task generation must not proceed until the final decision is recorded
in the feature plan and this mapping document.

## Pending Functional Validations

Feature `002-tax-document-issuance-foundation` records the following Pending
Functional Validations. The affected behavior is excluded or deferred from the
foundation implementation until the required validation source is available.

New unverified legacy behavior must be registered in this section before
affected planning continues.

Required record fields:

| Field | Requirement |
|-------|-------------|
| Legacy behavior | The behavior requiring validation |
| Affected work | Feature scope blocked or constrained by the behavior |
| Validation source | Legacy documentation, stakeholder decision, SRI contract, test fixture, or migration analysis |
| Decision due | `beforeAffectedTaskGeneration` |
| Resolution | Validated target behavior, excluded behavior, or deferred behavior |

Pending Functional Validations must resolve before affected task generation, or
the affected work must be excluded or deferred. A generated task list must not
contain implementation work for unresolved behavior unless the affected behavior
is explicitly excluded or deferred.

| ID | Legacy Behavior | Affected Work | Validation Source | Decision Due | Resolution |
|----|-----------------|---------------|-------------------|--------------|------------|
| PFV-ISS-001 | Runtime issuance mode default is not validated. | Future document-specific issuance or queue runtime behavior. | Future issuance or queue specification. | beforeAffectedTaskGeneration | `IssuanceMode` is modeled now; runtime default is deferred. |
| PFV-ISS-002 | Legacy route compatibility is not validated. | Future REST API or legacy compatibility adapters. | Future REST API or compatibility specification. | beforeAffectedTaskGeneration | Route compatibility is out of scope for this foundation. |
| PFV-ISS-003 | Synchronization scheduling ownership is not validated. | Future synchronization runtime behavior. | Future synchronization specification. | beforeAffectedTaskGeneration | Scheduling is out of scope for this foundation. |
| PFV-ISS-004 | Retry policy and signed-XML precondition require validation. | Future `RetrySriAuthorizationUseCase`. | Future retry specification and validated SRI/XML evidence. | beforeAffectedTaskGeneration | Retry candidate states are modeled; concrete retry behavior is deferred. |
| PFV-ISS-005 | Post-authorization corrections require validation. | Future correction, credit note, metadata update, or controlled post-authorization behavior. | Future document-specific correction specification. | beforeAffectedTaskGeneration | Authorized documents are immutable by default until a future exception is specified. |

## Compatibility Exceptions

Current baseline status: no compatibility exceptions are approved by this
enabler.

Compatibility exceptions are bounded exceptions for SRI adapter-only, legacy
compatibility, migration-only, or compatibility-view names. They cannot
authorize Spanish legacy names in normal target domain, application, API, or
persistence artifacts.

Required record fields:

| Field | Requirement |
|-------|-------------|
| Exception type | SRI adapter-only, legacy compatibility, migration-only, or compatibility view |
| Name | Legacy or official name allowed by the exception |
| Scope | Exact artifacts where the exception applies |
| Owner | Role responsible for review or removal |
| Expiration condition | Condition that ends the exception |
| Safer alternative rejected because | Reason a cleaner option was not selected |

Exceptions must be documented before affected tasks are generated.

## Resolution Gates

Future specifications must identify canonical English terms, mapped legacy
terms, SRI adapter-only concepts, explicit out-of-scope legacy behaviors,
Pending Naming Decisions, Pending Functional Validations, and compatibility
exceptions.

Future plans must resolve Pending Naming Decisions before task generation.
Future plans must resolve Pending Functional Validations before affected task
generation, or explicitly exclude or defer the affected work.

Future task lists must not:

- Create target artifacts with unresolved names.
- Implement behavior marked Pending Functional Validation.
- Put business logic in REST resources, repositories, SRI adapters, or
  bootstrap configuration.
- Reuse DTOs across layer boundaries.
- Place official SRI names outside allowed SRI, fixture, compatibility,
  migration, or mapping artifacts.

Future task lists must use `T###` identifiers and cite governing `FR-###`,
`AR-###`, `NR-###`, `TR-###`, `SC-###`, or contract sections.

After this enabler is completed, `docs/architecture` and `docs/migration` are the durable project documentation sources for architecture rules, canonical terminology, and legacy-to-target mappings.

The project constitution remains the highest governance authority. If any conflict exists between these documents and `.specify/memory/constitution.md`, the constitution prevails unless formally amended.

## Artifact Surface Mapping Rules

A canonical term may appear across multiple target artifact surfaces.

| Canonical Term | Domain | API Field | Database Column | SRI Adapter | Notes |
|---|---|---|---|---|---|
| accessKey | `AccessKey` | `accessKey` | `access_key` | `claveAcceso` | SRI name allowed only in adapter |
| authorizationNumber | `AuthorizationNumber` | `authorizationNumber` | `authorization_number` | `numeroAutorizacion` | SRI name allowed only in adapter |
| issueDate | `issueDate` | `issueDate` | `issue_date` | `fechaEmision` | SRI name allowed only in adapter |
| authorizedAt | `authorizedAt` | `authorizedAt` | `authorized_at` | `fechaAutorizacion` | SRI name allowed only in adapter |
| legalName | `legalName` | `legalName` | `legal_name` | `razonSocial` | SRI name allowed only in adapter |
| tradeName | `tradeName` | `tradeName` | `trade_name` | `nombreComercial` | SRI name allowed only in adapter |
