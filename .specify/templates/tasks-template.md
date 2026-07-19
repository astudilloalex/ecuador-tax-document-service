---

description: "Dependency-ordered implementation tasks for an approved feature"
---

# Tasks: [FEATURE NAME]

**Input**: Approved design documents from `/specs/[###-feature-name]/`

**Prerequisites**: `spec.md`, completed clarifications, `plan.md`, completed requirement-quality
checklist, `research.md`, `data-model.md`, `contracts/`, and `quickstart.md` where applicable

**Analysis Gate**: `$speckit-analyze` MUST run after this file is generated and before any task is
implemented. Unresolved critical findings MUST block `$speckit-implement`.

**Tests**: Applicable constitutional test categories are mandatory. A category MAY be omitted only
when the implementation plan records why it is not applicable. Test tasks MUST precede the
production tasks whose behavior or invariant they cover.

**Organization**: Tasks are grouped by user story so each story remains an independently valuable,
independently testable increment of the single bounded feature outcome.

## Format: `[ID] [P?] [Story] Description with exact file path and requirement reference`

- **[P]**: The task can run in parallel because it changes different files and has no dependency
  on incomplete work.
- **[Story]**: The user story served by the task, such as `[US1]` or `[US2]`; it is required only in
  user-story phases.
- Every description MUST include an exact repository-relative file path.
- Every user-story task MUST reference the applicable story, requirement, success criterion, or
  documented risk identifier in its description.
- All target names, paths, comments, fixtures, and documentation MUST use approved English
  terminology.
- A Company-scoped API feature MUST include tasks for the `X-Company-Id` header contract,
  request-body/input-schema exclusion, any explicitly contracted response Company identifier,
  application-level Company UUID mapping, Company-owned aggregate/persistence/idempotency scoping,
  immutable global-SRI-catalog non-scoping, and boundary tests. It MUST NOT include Company lookup
  ports or clients, authentication or authorization, Company master-data CRUD or tables,
  cross-service foreign keys/repositories/transactions, Company caches, background replication, or
  draft-time fiscal snapshots.

## Path Conventions

```text
src/main/java/com/alexastudillo/taxdocument/
├── api/<capability>/
├── application/<capability>/
├── domain/<capability>/
└── infrastructure/<capability>/

src/main/resources/db/migration/
src/test/java/com/alexastudillo/taxdocument/
src/test/resources/
```

<!--
  The tasks below are examples only. `$speckit-tasks` MUST replace them with concrete tasks from
  the approved artifacts. Keep the phase structure and strict task format, but remove unused
  examples. Do not preserve a legacy route, payload, table, status, or behavior unless an explicit
  approved requirement requires it.
-->

## Phase 1: Setup

**Purpose**: Establish the approved Java 25 and Quarkus baseline and verification tooling.

- [ ] T001 Configure the approved Java 25 and justified Quarkus baseline in `build.gradle.kts`
- [ ] T002 Create capability-grouped Clean Architecture package roots under `src/main/java/com/alexastudillo/taxdocument/`
- [ ] T003 [P] Configure formatting and static-analysis checks in `build.gradle.kts`
- [ ] T004 [P] Configure sanitized test settings in `src/test/resources/application.properties`
- [ ] T005 Record approved terminology introduced by the feature in `docs/migration/terminology-mapping.md`

---

## Phase 2: Foundational Controls

**Purpose**: Complete the cross-cutting controls that block all user stories.

**CRITICAL**: User-story production work MUST NOT begin until this phase is complete.

- [ ] T006 Configure Flyway as the only schema evolution mechanism in `src/main/resources/application.properties`
- [ ] T007 Create the first repeatable target migration in `src/main/resources/db/migration/[version]__[english_description].sql`
- [ ] T008 [P] Define the mandatory `X-Company-Id` request-header contract in `src/main/java/com/alexastudillo/taxdocument/api/company/[CompanyContextHeader].java`
- [ ] T009 [P] Define the stable target error contract in `src/main/java/com/alexastudillo/taxdocument/api/error/[ErrorContract].java`
- [ ] T010 [P] Implement correlation propagation in `src/main/java/com/alexastudillo/taxdocument/api/observability/[CorrelationFilter].java`
- [ ] T011 [P] Define distinct liveness and readiness behavior in `src/main/java/com/alexastudillo/taxdocument/infrastructure/health/[HealthChecks].java`
- [ ] T012 Create PostgreSQL and Flyway integration-test support in `src/test/java/com/alexastudillo/taxdocument/infrastructure/persistence/[DatabaseTestSupport].java`

**Checkpoint**: The architectural, migration, Company-context, error, correlation, health, and test
foundations required by the approved plan are ready.

---

## Phase 3: User Story 1 - [Title] (Priority: P1) MVP

**Goal**: [Bounded stakeholder value delivered by this story]

**Independent Test**: [Observable scenario proving this story works without later stories]

### Required Evidence for User Story 1

Include every applicable category from the plan. These examples are not a fixed test list.

- [ ] T013 [P] [US1] Add pure domain tests for [FR/DR IDs] in `src/test/java/com/alexastudillo/taxdocument/domain/[capability]/[DomainType]Test.java`
- [ ] T014 [P] [US1] Add Company-header and ownership-scoping tests for [FR IDs] in `src/test/java/com/alexastudillo/taxdocument/application/[capability]/[UseCase]Test.java`
- [ ] T015 [P] [US1] Add PostgreSQL persistence and Flyway invariant tests for [FR IDs] in `src/test/java/com/alexastudillo/taxdocument/infrastructure/persistence/[RepositoryAdapter]Test.java`
- [ ] T016 [P] [US1] Add API contract and safe-error tests for [FR/SC IDs] in `src/test/java/com/alexastudillo/taxdocument/api/[capability]/[Resource]Test.java`
- [ ] T017 [P] [US1] Add external-adapter timeout, retry, idempotency, and reconciliation tests for [FR/SC IDs] in `src/test/java/com/alexastudillo/taxdocument/infrastructure/[adapter]/[Adapter]Test.java`
- [ ] T018 [P] [US1] Add official-rule vectors for XML, access keys, signatures, or monetary boundaries for [DR IDs] in `src/test/resources/[capability]/`

### Production Implementation for User Story 1

- [ ] T019 [P] [US1] Implement synchronous domain behavior for [FR/DR IDs] in `src/main/java/com/alexastudillo/taxdocument/domain/[capability]/[DomainType].java`
- [ ] T020 [P] [US1] Define the outbound application port for [FR IDs] in `src/main/java/com/alexastudillo/taxdocument/application/[capability]/port/[Port].java`
- [ ] T021 [US1] Implement the Company-scoped application use case for [FR IDs] in `src/main/java/com/alexastudillo/taxdocument/application/[capability]/[UseCase].java`
- [ ] T022 [P] [US1] Implement the explicit API DTO mappings for [FR IDs] in `src/main/java/com/alexastudillo/taxdocument/api/[capability]/[Mapper].java`
- [ ] T023 [P] [US1] Implement the Panache persistence model and domain mapping for [FR IDs] in `src/main/java/com/alexastudillo/taxdocument/infrastructure/persistence/[capability]/[PersistenceModel].java`
- [ ] T024 [US1] Implement the outbound infrastructure adapter with approved timeouts and bounds for [FR IDs] in `src/main/java/com/alexastudillo/taxdocument/infrastructure/[adapter]/[Adapter].java`
- [ ] T025 [US1] Expose the target-first operation and correlation-safe errors for [FR/SC IDs] in `src/main/java/com/alexastudillo/taxdocument/api/[capability]/[Resource].java`

**Checkpoint**: User Story 1 satisfies its acceptance scenarios and applicable failure,
Company-scoping, persistence, integration, and observability evidence independently.

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Bounded stakeholder value delivered by this story]

**Independent Test**: [Observable scenario proving this story independently]

### Required Evidence for User Story 2

- [ ] T026 [P] [US2] Add applicable domain and use-case tests for [FR/DR IDs] in `src/test/java/com/alexastudillo/taxdocument/[boundary]/[capability]/[Test].java`
- [ ] T027 [P] [US2] Add applicable API, persistence, Company-scoping, and adapter tests for [FR/SC IDs] in `src/test/java/com/alexastudillo/taxdocument/[boundary]/[capability]/[Test].java`

### Production Implementation for User Story 2

- [ ] T028 [P] [US2] Implement domain behavior for [FR/DR IDs] in `src/main/java/com/alexastudillo/taxdocument/domain/[capability]/[Type].java`
- [ ] T029 [US2] Implement the application use case and effective ownership scope for [FR IDs] in `src/main/java/com/alexastudillo/taxdocument/application/[capability]/[UseCase].java`
- [ ] T030 [P] [US2] Implement required infrastructure mappings and adapters for [FR IDs] in `src/main/java/com/alexastudillo/taxdocument/infrastructure/[capability]/[Adapter].java`
- [ ] T031 [US2] Expose the target-first operation for [FR/SC IDs] in `src/main/java/com/alexastudillo/taxdocument/api/[capability]/[Resource].java`

**Checkpoint**: User Stories 1 and 2 each remain independently testable.

---

[Add one phase per remaining user story. Each phase MUST retain required-evidence tasks before its
production tasks and MUST use exact paths and requirement references.]

---

## Final Phase: Cross-Cutting Validation and Documentation

**Purpose**: Satisfy the feature Definition of Done without hiding unfinished story work here.

- [ ] TXXX [P] Prove Flyway migration from an empty PostgreSQL database in `src/test/java/com/alexastudillo/taxdocument/infrastructure/persistence/[MigrationTest].java`
- [ ] TXXX [P] Add sensitive-data exposure tests in `src/test/java/com/alexastudillo/taxdocument/infrastructure/security/[SensitiveDataTest].java`
- [ ] TXXX [P] Add JVM runtime smoke evidence in `src/test/java/com/alexastudillo/taxdocument/runtime/[JvmSmokeTest].java`
- [ ] TXXX Record native build and runtime evidence or an evidence-based deferral in `specs/[###-feature-name]/plan.md`
- [ ] TXXX Validate end-to-end acceptance and recovery scenarios in `specs/[###-feature-name]/quickstart.md`
- [ ] TXXX Update affected English terminology and classifications in `docs/migration/terminology-mapping.md`
- [ ] TXXX Record final constitution and Definition of Done review in `specs/[###-feature-name]/plan.md`
- [ ] TXXX Run formatting, static analysis, required tests, and sensitive-data checks from `build.gradle.kts`

---

## Dependencies & Execution Order

### Workflow Dependencies

- The constitution MUST already be approved on `main`.
- `$speckit-specify` and `$speckit-clarify` MUST produce the approved bounded requirements.
- `$speckit-plan` MUST pass its pre-research and post-design constitution checks.
- `$speckit-checklist` MUST evaluate requirement quality honestly before task generation.
- `$speckit-analyze` MUST run after this task list and MUST resolve critical findings before
  implementation.

### Phase Dependencies

- **Setup** has no feature-task dependency.
- **Foundational Controls** depends on Setup and blocks all user-story production work.
- **User Stories** depend on Foundational Controls. Stories MAY run in parallel only when their
  files and approved behavior are independent.
- Within a story, required evidence tasks precede the production tasks they cover; domain behavior
  precedes application orchestration, which precedes adapters and API exposure.
- **Cross-Cutting Validation** depends on every story included in the release scope.

### Parallel Opportunities

- A `[P]` marker is permitted only for different files with no incomplete dependency.
- Tests in different boundaries MAY run in parallel after shared test support exists.
- Independent stories MAY run in parallel after Foundational Controls, subject to file ownership.
- Tasks that edit `build.gradle.kts`, `application.properties`, a shared migration, or the same
  terminology mapping MUST run sequentially.

## Implementation Strategy

### MVP First

1. Complete Setup and Foundational Controls.
2. Complete all required evidence and production tasks for User Story 1.
3. Validate User Story 1 independently, including failure and Company-context boundaries.
4. Verify JVM execution and update the plan with evidence.
5. Add later stories only after the MVP remains compliant and independently valuable.

### Incremental Delivery

Each increment MUST preserve prior acceptance scenarios, database evolution, Company scoping,
idempotency, sensitive-data controls, and observable terminal outcomes. A later story MUST NOT be
required to make an earlier story safe or testable.

## Notes

- Historical files under `docs/legacy/` MUST NOT be edited.
- Company master data remains outside this bounded context. Draft tasks MAY persist only the
  immutable external Company UUID supplied by `X-Company-Id`; draft creation MUST NOT create a
  Company or fiscal snapshot.
- Tasks MUST NOT introduce authentication, authorization, Company Service calls, shared Company
  persistence, cross-service foreign keys, Company repositories, Company caches, or Company-data
  replication.
- Panache persistence models MUST NOT become domain or transport models.
- Blocking and CPU-intensive adapter work MUST use the plan's bounded execution context.
- Retries MUST preserve the logical fiscal operation, access key, sequence, and persisted document.
- A task MUST NOT be marked complete merely to unblock the workflow.
- Tests that assert only coverage are prohibited.
