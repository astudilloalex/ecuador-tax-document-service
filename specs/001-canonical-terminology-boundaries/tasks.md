# Tasks: Canonical Terminology and Architecture Boundaries

**Input**: Design documents from
`/specs/001-canonical-terminology-boundaries/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`,
`contracts/`, `quickstart.md`, and
`checklists/governance.md`

**Tests**: This architecture enabler is documentation-only. No Java, Quarkus,
REST, persistence, SRI client, migration, or test code is created. Validation is
performed through document review and the checks in `quickstart.md`.

**Organization**: Tasks are grouped by user story so each governance outcome can
be reviewed independently. Every task cites at least one governing requirement,
success criterion, plan section, constitution rule, or contract section.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it touches different files and has no
  dependency on another unfinished task
- **[Story]**: Which user story this task belongs to
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Documentation Structure)

**Purpose**: Create the durable documentation locations and target document
shells required by the plan.

- [ ] T001 Create durable documentation directories `docs/architecture/` and
  `docs/migration/`. (FR-013, FR-014, Plan `Project Structure`)
- [ ] T002 [P] Create the required heading outline in
  `docs/architecture/backend-clean-architecture.md` from
  `specs/001-canonical-terminology-boundaries/contracts/architecture-rules-document.md`.
  (TR-004, Contract `Architecture Rules Document`)
- [ ] T003 [P] Create the required heading outline in
  `docs/architecture/canonical-terminology.md` from
  `specs/001-canonical-terminology-boundaries/contracts/canonical-terminology-document.md`.
  (FR-001, TR-004, Contract `Canonical Terminology Document`)
- [ ] T004 [P] Create the required heading outline in
  `docs/migration/legacy-to-target-terminology.md` from
  `specs/001-canonical-terminology-boundaries/contracts/legacy-mapping-document.md`.
  (FR-014, TR-004, Contract `Legacy-To-Target Mapping Document`)

---

## Phase 2: Foundational (Blocking Governance Rules)

**Purpose**: Establish cross-story source-of-truth, traceability, and
documentation-only constraints before filling the story-specific content.

**Critical**: No user story documentation work should begin until this phase is
complete.

- [ ] T005 Add project identity and documentation-only scope to
  `docs/architecture/backend-clean-architecture.md`, including group
  `com.alexastudillo`, artifact `ecuador-tax-document-service`, base package
  `com.alexastudillo.taxdocument`, Java 25, Maven, Quarkus, backend-only, and no
  source-code changes for this enabler. (AR-001, Plan `Summary`)
- [ ] T006 Add durable source-of-truth and traceability rules to
  `docs/architecture/backend-clean-architecture.md`, including `FR-###`,
  `AR-###`, `NR-###`, `TR-###`, `SC-###`, future `T###`, and the role of
  `docs/architecture` and `docs/migration`. (TR-001, TR-005, TR-006)
- [ ] T007 [P] Add terminology traceability rules to
  `docs/architecture/canonical-terminology.md`, including how approved terms
  trace to the constitution or feature plan and how pending terms trace to
  `docs/migration/legacy-to-target-terminology.md`. (TR-003, Contract
  `Canonical Terminology Document`)
- [ ] T008 [P] Add mapping source-of-truth and extension rules to
  `docs/migration/legacy-to-target-terminology.md`, including how future
  feature plans extend the mapping without changing its structure. (FR-014,
  TR-006, Contract `Legacy-To-Target Mapping Document`)
- [ ] T009 Add documentation governance and durable location rules to
  `docs/architecture/backend-clean-architecture.md`, distinguishing AS-IS
  legacy documentation in `docs/legacy/`, migration mappings and canonical
  terminology in `docs/migration/`, architecture rules and decisions in
  `docs/architecture/` or `docs/adr/`, and target specifications in
  `.specify/specs/`. (FR-012, Contract `Future Specification Governance`)

**Checkpoint**: Target documents exist, identify their authority, and preserve
the documentation-only scope.

---

## Phase 3: User Story 1 - Establish Canonical Terminology (Priority: P1)

**Goal**: Publish an approved English terminology model and baseline
legacy-to-target mappings for future specifications, APIs, database objects,
code, tests, and documentation.

**Independent Test**: Review the approved baseline Spanish terms and verify that
each term has an English target term, a classification, and a decision status in
`docs/architecture/canonical-terminology.md` and
`docs/migration/legacy-to-target-terminology.md`.

- [ ] T010 [US1] Document artifact-specific format rules in
  `docs/architecture/canonical-terminology.md` for package segments, class
  names, DTO class names, fields, methods, database objects, URL path segments,
  event type names, test class names, and documentation file names. (FR-004,
  FR-016, NR-021, Contract `Canonical Terminology Document`)
- [ ] T011 [US1] Populate approved canonical terms in
  `docs/architecture/canonical-terminology.md` for `taxDocument`, `invoice`,
  `creditNote`, `debitNote`, `withholding`, `waybill`, `issuer`, `buyer`,
  `recipient`, `accessKey`, `authorizationNumber`, `issueDate`, `authorizedAt`,
  `legalName`, `tradeName`, `establishment`, `issuingPoint`, and
  `sequenceNumber`. (FR-001, NR-002-NR-019)
- [ ] T012 [US1] Document business-oriented English naming guidance and
  literal-translation rejection rules in
  `docs/architecture/canonical-terminology.md`. (NR-001, SC-006)
- [ ] T013 [US1] Document forbidden generic business names in
  `docs/architecture/canonical-terminology.md`, including `DocumentService`,
  `SriService`, `ProcessService`, `Manager`, `Helper`, and `Util`. (FR-006,
  NR-020)
- [ ] T014 [P] [US1] Populate the approved baseline mapping table in
  `docs/migration/legacy-to-target-terminology.md` with all baseline legacy
  terms from `comprobante` through `secuencial`, including target term,
  classification, and `Decided` status. (FR-002, FR-003, FR-015,
  Contract `Legacy-To-Target Mapping Document`)
- [ ] T015 [US1] Document Pending Naming Decision registration and resolution
  rules in `docs/migration/legacy-to-target-terminology.md`, including
  resolution before affected task generation and recording final decisions in
  the feature plan and mapping document. (FR-007, FR-017, SC-010)
- [ ] T016 [US1] Validate User Story 1 by comparing
  `docs/architecture/canonical-terminology.md` and
  `docs/migration/legacy-to-target-terminology.md` against
  `specs/001-canonical-terminology-boundaries/contracts/canonical-terminology-document.md`
  and
  `specs/001-canonical-terminology-boundaries/contracts/legacy-mapping-document.md`.
  (SC-001, SC-002, SC-008, SC-009)

**Checkpoint**: Canonical terms and baseline mappings can be reviewed
independently without inspecting legacy source code.

---

## Phase 4: User Story 2 - Separate SRI Contract Language (Priority: P2)

**Goal**: Preserve official SRI XML/SOAP terminology only in approved
contract-specific or migration artifacts while keeping target domain,
application, API, and persistence language English.

**Independent Test**: Review sample SRI XML/SOAP Spanish terms and verify that
each is either mapped to English target terminology or classified as SRI
adapter-only in `docs/migration/legacy-to-target-terminology.md`.

- [ ] T017 [US2] Document SRI contract isolation rules in
  `docs/architecture/backend-clean-architecture.md`, including allowed
  locations for official SRI XML tags, SOAP request/response models, XML
  signing, reception calls, authorization calls, SRI response parsing, official
  fixtures, compatibility adapters, migration scripts, and mapping documents.
  (FR-005, AR-010, Contract `Architecture Rules Document`)
- [ ] T018 [P] [US2] Document SRI adapter-only concept rules in
  `docs/migration/legacy-to-target-terminology.md`, including classification of
  official SRI XML/SOAP names and the rule that they cannot become internal
  domain, application, API, persistence, or target database names. (FR-005,
  Data Model `SRIContractTerm`)
- [ ] T019 [US2] Document compatibility exception records in
  `docs/migration/legacy-to-target-terminology.md`, including exception type,
  name, scope, owner, expiration condition, and rejected safer alternative.
  (FR-005, FR-006, Data Model `CompatibilityException`)
- [ ] T020 [US2] Document the rejection rule for Spanish SRI or legacy names in
  target APIs and target database objects in
  `docs/architecture/backend-clean-architecture.md`, with a cross-reference to
  `docs/migration/legacy-to-target-terminology.md` for bounded compatibility
  exceptions. (AR-009, AR-010, SC-004)
- [ ] T021 [US2] Validate User Story 2 by checking
  `docs/architecture/backend-clean-architecture.md` and
  `docs/migration/legacy-to-target-terminology.md` against
  `specs/001-canonical-terminology-boundaries/contracts/future-spec-governance.md`
  acceptance checks for SRI isolation and compatibility exceptions. (SC-004,
  SC-006)

**Checkpoint**: Official SRI Spanish names have an allowed contract boundary and
cannot leak into target internal artifacts.

---

## Phase 5: User Story 3 - Define Architecture Boundaries (Priority: P3)

**Goal**: Publish Clean Architecture and Ports and Adapters boundary rules that
future specifications, plans, tasks, and implementations must follow.

**Independent Test**: Review a future feature outline and verify that it assigns
domain, application, inbound adapter, outbound adapter, and bootstrap
responsibilities without cross-layer model reuse or business logic in adapters.

- [ ] T022 [US3] Document dependency direction in
  `docs/architecture/backend-clean-architecture.md` as
  `adapter -> application -> domain`, `bootstrap -> adapter`, and
  `bootstrap -> application`. (AR-002, Contract `Architecture Rules Document`)
- [ ] T023 [US3] Document layer responsibilities in
  `docs/architecture/backend-clean-architecture.md` for `domain`,
  `application`, `adapter.in.rest`, `adapter.out.persistence`,
  `adapter.out.sri`, `adapter.out.storage`, `adapter.out.queue`,
  `adapter.out.webhook`, and `bootstrap`. (FR-009, AR-003-AR-008)
- [ ] T024 [US3] Document use case-centered design rules in
  `docs/architecture/backend-clean-architecture.md`, including explicit
  application use cases and preferred names such as `IssueInvoiceUseCase`,
  `IssueCreditNoteUseCase`, `IssueDebitNoteUseCase`,
  `IssueWithholdingUseCase`, `IssueWaybillUseCase`,
  `RetrySriAuthorizationUseCase`, `SynchronizeTaxDocumentsUseCase`, and
  `DeliverWebhookUseCase`. (FR-010, AR-004)
- [ ] T025 [US3] Document Ports and Adapters rules in
  `docs/architecture/backend-clean-architecture.md`, including output port
  examples `TaxDocumentRepository`, `SriAuthorizationPort`, `XmlStoragePort`,
  `AccessKeyGeneratorPort`, `WebhookPublisherPort`, `ClockPort`, and
  `TransactionPort`. (FR-009, AR-006, Contract `Architecture Rules Document`)
- [ ] T026 [US3] Document DTO separation and required mapping flow in
  `docs/architecture/backend-clean-architecture.md`, including REST DTO to
  application command, application result to REST response DTO, domain object to
  persistence entity, domain object to SRI XML DTO, and SRI response DTO to
  application result. (FR-011, Contract `Architecture Rules Document`)
- [ ] T027 [US3] Document transport, application, and domain validation
  ownership in `docs/architecture/backend-clean-architecture.md`, including
  required fields/date formats/path parameters, authorization and retry
  eligibility, and business invariants/state transitions. (AR-003-AR-008,
  Constitution `Validation Separation`)
- [ ] T028 [US3] Document future critical-operation idempotency, auditability,
  and error-mapping rules in `docs/architecture/backend-clean-architecture.md`
  for issuance, SRI authorization retries, synchronization runs, webhook
  delivery, XML generation, sequence assignment, secret-free audit logs, and
  adapter failure mapping. (Plan `Idempotency, Audit, and Error Handling`,
  Constitution `Idempotency and Auditability`)
- [ ] T029 [US3] Document forbidden practices and a spec/plan/task/code
  compliance checklist in `docs/architecture/backend-clean-architecture.md`,
  covering Clean Architecture boundaries, English terminology, SRI isolation,
  DTO separation, ports and adapters, no legacy architecture copying,
  infrastructure-free testability, legacy concept mapping, pending decisions,
  and Spec Kit artifact requirements. (AR-008, SC-005,
  Contract `Architecture Rules Document`)
- [ ] T030 [P] [US3] Document Pending Functional Validation registration,
  resolution, exclusion, and deferral rules in
  `docs/migration/legacy-to-target-terminology.md`. (FR-008, FR-018, SC-011,
  Data Model `PendingFunctionalValidation`)
- [ ] T031 [US3] Validate User Story 3 by checking
  `docs/architecture/backend-clean-architecture.md` and
  `docs/migration/legacy-to-target-terminology.md` against
  `specs/001-canonical-terminology-boundaries/contracts/architecture-rules-document.md`
  and
  `specs/001-canonical-terminology-boundaries/contracts/future-spec-governance.md`.
  (SC-003, SC-005, SC-012)

**Checkpoint**: Future feature authors can assign behavior and dependencies to
the correct layer before task generation.

---

## Phase 6: Compliance and Polish

**Purpose**: Validate the generated documentation against the quickstart,
governance checklist, and documentation-only constraints.

- [ ] T032 Run the validation commands from
  `specs/001-canonical-terminology-boundaries/quickstart.md` against
  `docs/architecture/backend-clean-architecture.md`,
  `docs/architecture/canonical-terminology.md`, and
  `docs/migration/legacy-to-target-terminology.md`; correct only documentation
  gaps in those files. (SC-007, Quickstart `Validation Steps`)
- [ ] T033 Verify no runtime artifacts were created or required under
  `src/main/java/com/alexastudillo/taxdocument/`,
  `src/test/java/com/alexastudillo/taxdocument/`, or
  `src/main/resources/` for this feature. (Plan `Source Code`, Quickstart
  `Verify no runtime artifacts were created`)
- [ ] T034 Cross-check `docs/architecture/backend-clean-architecture.md`,
  `docs/architecture/canonical-terminology.md`,
  `docs/migration/legacy-to-target-terminology.md`, and
  `specs/001-canonical-terminology-boundaries/checklists/governance.md` for
  CHK001-CHK036 compliance before marking the feature complete. (SC-005,
  Checklist `Governance Requirements Checklist`)
- [ ] T035 Verify every task in
  `specs/001-canonical-terminology-boundaries/tasks.md` uses a `T###`
  identifier, includes exact file paths, and cites at least one governing
  requirement identifier, plan section, constitution rule, or contract section.
  (TR-005, SC-012)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependencies.
- **Phase 2 Foundational**: Depends on Phase 1 target document shells.
- **Phase 3 User Story 1**: Depends on Phase 2.
- **Phase 4 User Story 2**: Depends on Phase 2 and can start after US1 if the
  canonical terminology tables are available for cross-reference.
- **Phase 5 User Story 3**: Depends on Phase 2 and can proceed independently of
  US1/US2 except for final cross-references.
- **Phase 6 Compliance and Polish**: Depends on all selected user stories.

### User Story Dependencies

- **US1 Establish Canonical Terminology**: MVP scope. Delivers usable glossary
  and baseline mapping by itself.
- **US2 Separate SRI Contract Language**: Requires the target-language baseline
  from US1 for mapping or rejection decisions.
- **US3 Define Architecture Boundaries**: Can be authored after Phase 2, but the
  final compliance checklist should reference US1 and US2 outputs.

### Within Each User Story

- Author governing rules before validation tasks.
- Complete same-file edits sequentially.
- Validate story output against the relevant contract artifacts before moving
  to the next priority.
- Do not create source code, tests, endpoints, entities, clients, or database
  migrations in any phase.

## Parallel Opportunities

- After T001, T002, T003, and T004 can run in parallel because they create
  different target documents.
- T007 and T008 can run in parallel with T005/T006 after document shells exist
  because they touch different files.
- In US1, T014 can run in parallel with T010-T013 because it touches
  `docs/migration/legacy-to-target-terminology.md` while those tasks touch
  `docs/architecture/canonical-terminology.md`.
- In US2, T018 can run in parallel with T017 because it touches the migration
  mapping document while T017 touches the architecture document.
- In US3, T030 can run in parallel with T022-T029 because it touches
  `docs/migration/legacy-to-target-terminology.md` while the other tasks touch
  `docs/architecture/backend-clean-architecture.md`.

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 for canonical terminology and baseline mapping.
3. Validate T016 independently.
4. Stop if only the terminology baseline is needed.

### Incremental Delivery

1. Deliver US1 to establish English canonical terms.
2. Deliver US2 to isolate official SRI contract language.
3. Deliver US3 to publish full architecture boundary governance.
4. Run Phase 6 compliance validation.

### Documentation-Only Guardrail

All tasks are limited to Markdown documentation under `docs/architecture`,
`docs/migration`, and this feature folder. Any task that would create Java
source, Quarkus resources, REST endpoints, persistence entities, SRI clients, or
database migrations is out of scope for this feature.
