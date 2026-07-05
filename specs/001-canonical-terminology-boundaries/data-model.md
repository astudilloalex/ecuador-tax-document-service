# Data Model: Canonical Terminology and Architecture Boundaries

This model describes governance records and document concepts, not runtime
persistence entities.

## CanonicalTerm

**Purpose**: Approved English business term used by target artifacts.

**Fields**:

- `targetTerm`: canonical English term, rendered per artifact type.
- `definition`: concise business meaning.
- `allowedArtifactTypes`: target artifact categories where the term may appear.
- `artifactFormats`: package, class, DTO class, field, method, database, URL,
  event, test, and documentation file representations.
- `status`: `approved`, `pending`, `deprecated`.
- `source`: constitution, architecture document, migration mapping, or feature
  plan.
- `notes`: optional rationale or usage guidance.

**Validation Rules**:

- `targetTerm` uses business-oriented English.
- `targetTerm` is not a literal translation when a better business term exists.
- Approved terms must include artifact-specific renderings.
- Artifact formats must include lowercase package segments, PascalCase class
  and DTO class names, camelCase fields and methods, lowercase snake_case
  database objects, kebab-case URL path segments, PascalCase event type names,
  PascalCase test class names with a `Test` suffix, and lowercase kebab-case
  documentation file names.

**Traceability**: Constrained by Spec `FR-001`, `FR-004`, `FR-016`, `NR-001`,
`NR-021`, and `TR-003`.

## LegacyTermMapping

**Purpose**: Maps a legacy Spanish term or legacy artifact name to the target
language model.

**Fields**:

- `legacyTerm`: Spanish or legacy name being mapped.
- `targetTerm`: approved English term or `pending`.
- `classification`: one allowed migration classification.
- `decisionStatus`: `decided`, `pendingNamingDecision`,
  `pendingFunctionalValidation`, `deprecated`, or `deferred`.
- `allowedLocations`: where the legacy term may still appear.
- `resolutionRequiredBy`: planning gate where the decision must be resolved.
- `notes`: rationale, source reference, or exception scope.

**Validation Rules**:

- Every mapping has exactly one classification.
- Pending Naming Decisions must resolve before affected task generation.
- Pending Functional Validations must resolve, be excluded, or be deferred
  before affected task generation.
- Spanish legacy names are not allowed in target domain, application, API, or
  persistence artifacts.

**Traceability**: Constrained by Spec `FR-002`, `FR-003`, `FR-014`, `FR-015`,
`FR-017`, `FR-018`, `NR-002` through `NR-019`, and `TR-003`.

## ArchitectureBoundaryRule

**Purpose**: Defines a Clean Architecture responsibility or dependency rule.

**Fields**:

- `layer`: `domain`, `application`, `adapter.in.rest`,
  `adapter.out.persistence`, `adapter.out.sri`, `adapter.out.storage`,
  `adapter.out.queue`, `adapter.out.webhook`, or `bootstrap`.
- `responsibility`: what the layer owns.
- `forbiddenResponsibilities`: what the layer must not own.
- `allowedDependencies`: inward dependency rule for the layer.
- `reviewCheck`: concrete compliance check for future plans and tasks.

**Validation Rules**:

- Domain rules cannot include framework, persistence, transport, XML, SOAP, SRI,
  HTTP client, filesystem, messaging, or external API dependencies.
- Adapter rules cannot assign business logic to adapters.
- Bootstrap rules cannot assign business logic to configuration or wiring.

**Traceability**: Constrained by Spec `FR-009`, `AR-002` through `AR-008`, and
`TR-003`.

## SRIContractTerm

**Purpose**: Official SRI XML or SOAP term that may remain Spanish only inside
contract-specific artifacts.

**Fields**:

- `officialName`: exact SRI contract name.
- `targetTerm`: English target term when used internally, or contract-only when
  no internal term exists.
- `allowedLocations`: SRI mappers, SOAP DTOs, adapter tests, official fixtures,
  compatibility adapters, migration scripts, or mapping documents.
- `isInternalModelAllowed`: always `false`.
- `notes`: contract rationale or fixture reference.

**Validation Rules**:

- Official names remain exact in SRI contract artifacts.
- Official Spanish names cannot appear in domain models, application commands,
  target APIs, persistence entities, or target database objects.

**Traceability**: Constrained by Spec `FR-005`, `AR-010`, and `TR-003`.

## PendingNamingDecision

**Purpose**: Tracks unresolved target terminology.

**Fields**:

- `legacyTerm`: unresolved legacy term.
- `candidateTerms`: proposed English names, if known.
- `affectedArtifacts`: artifacts blocked or constrained by the decision.
- `owner`: role responsible for resolution.
- `decisionDue`: `beforeTaskGeneration`.
- `finalDecision`: approved target term when resolved.

**State Transitions**:

```text
registered -> resolved
registered -> deferred
deferred -> resolved
```

**Validation Rules**:

- A generated task list cannot include affected implementation work while the
  decision is unresolved.
- Final decisions must be recorded in the feature plan and `docs/migration`.

**Traceability**: Constrained by Spec `FR-007`, `FR-017`, `SC-010`, and
`TR-003`.

## PendingFunctionalValidation

**Purpose**: Tracks legacy behavior that has not been validated as target
behavior.

**Fields**:

- `legacyBehavior`: behavior requiring validation.
- `affectedWork`: feature scope that depends on the behavior.
- `validationSource`: legacy documentation, stakeholder decision, SRI contract,
  test fixture, or migration analysis.
- `decisionDue`: `beforeAffectedTaskGeneration`.
- `resolution`: validated target behavior, excluded behavior, or deferred
  behavior.

**State Transitions**:

```text
registered -> validated
registered -> excluded
registered -> deferred
deferred -> validated
```

**Validation Rules**:

- A generated task list cannot include affected implementation work while the
  behavior is unresolved.
- Excluded or deferred behavior must be visible in the plan.

**Traceability**: Constrained by Spec `FR-008`, `FR-018`, `SC-011`, and
`TR-003`.

## CompatibilityException

**Purpose**: Documents a bounded exception for legacy or SRI-specific names.

**Fields**:

- `exceptionType`: SRI adapter-only, legacy compatibility, migration-only, or
  compatibility view.
- `name`: legacy or official name allowed by the exception.
- `scope`: exact artifacts where the exception applies.
- `owner`: role responsible for removing or reviewing the exception.
- `expirationCondition`: condition that ends the exception.
- `saferAlternativeRejectedBecause`: reason a cleaner option was not selected.

**Validation Rules**:

- Exceptions must be documented before affected tasks are generated.
- Exceptions cannot authorize Spanish legacy names in normal target domain,
  application, API, or persistence code.

**Traceability**: Constrained by Spec `FR-005`, `FR-006`, `AR-010`, and
`TR-003`.

## Relationships

- A `LegacyTermMapping` references zero or one approved `CanonicalTerm` while it
  is pending, and exactly one approved `CanonicalTerm` when decided.
- An `SRIContractTerm` may map to a `CanonicalTerm` but remains allowed only in
  contract-specific artifacts.
- A `PendingNamingDecision` becomes a `LegacyTermMapping` when resolved.
- A `PendingFunctionalValidation` can block or defer tasks associated with one
  or more `ArchitectureBoundaryRule` checks.
- A `CompatibilityException` constrains where a legacy or SRI-specific term may
  appear.
