---

description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`

**Prerequisites**: plan.md (required), spec.md (required for user stories),
research.md, data-model.md, contracts/

**Tests**: Tests are mandatory for every touched layer and risk area identified
by the constitution. Domain tests must run without Quarkus. Application tests
must run without real PostgreSQL, SRI, Redis, filesystems, or external HTTP
services. Adapter tests may use Quarkus test support, Testcontainers, mocks, or
contract fixtures.

**Organization**: Tasks are grouped by user story to enable independent
implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it touches different files and has no
  dependency on another unfinished task
- **[Story]**: Which user story this task belongs to (for example US1, US2,
  US3)
- Include exact file paths in descriptions

## Path Conventions

- Main source: `src/main/java/com/alexastudillo/taxdocument/`
- Main resources: `src/main/resources/`
- Tests: `src/test/java/com/alexastudillo/taxdocument/`
- Domain: `src/main/java/com/alexastudillo/taxdocument/domain/`
- Application: `src/main/java/com/alexastudillo/taxdocument/application/`
- REST adapter: `src/main/java/com/alexastudillo/taxdocument/adapter/in/rest/`
- Persistence adapter:
  `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/`
- SRI adapter: `src/main/java/com/alexastudillo/taxdocument/adapter/out/sri/`
- Storage adapter:
  `src/main/java/com/alexastudillo/taxdocument/adapter/out/storage/`
- Queue adapter: `src/main/java/com/alexastudillo/taxdocument/adapter/out/queue/`
- Webhook adapter:
  `src/main/java/com/alexastudillo/taxdocument/adapter/out/webhook/`
- Bootstrap: `src/main/java/com/alexastudillo/taxdocument/bootstrap/`

<!--
  ============================================================================
  IMPORTANT: The tasks below are sample tasks for illustration only.

  The /speckit-tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md with priorities P1, P2, P3, etc.
  - Functional, architectural, naming, idempotency, and audit requirements
  - Layer and boundary design from plan.md
  - Entities from data-model.md
  - API contracts from contracts/
  - Constitution compliance gates

  DO NOT keep these sample tasks in the generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and feature structure

- [ ] T001 Confirm feature package structure follows the plan under
  `src/main/java/com/alexastudillo/taxdocument/`
- [ ] T002 Confirm Gradle with Kotlin DSL and Quarkus dependencies required by this feature are
  documented in the plan before build changes
- [ ] T003 [P] Add or update test package structure under
  `src/test/java/com/alexastudillo/taxdocument/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Cross-story boundaries that MUST be complete before user story
implementation begins

**Critical**: No user story work can begin until this phase is complete.

- [ ] T004 Define domain concepts, value objects, invariants, and exceptions in
  `src/main/java/com/alexastudillo/taxdocument/domain/`
- [ ] T005 Define application commands, queries, results, input ports, and output
  ports in `src/main/java/com/alexastudillo/taxdocument/application/`
- [ ] T006 Define idempotency rules and audit event names for critical
  operations in the relevant application use case design
- [ ] T007 [P] Define REST request and response DTOs in
  `src/main/java/com/alexastudillo/taxdocument/adapter/in/rest/`
- [ ] T008 [P] Define persistence entities and mappers in
  `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/`
- [ ] T009 [P] Define SRI adapter DTOs, mappers, or fixtures in
  `src/main/java/com/alexastudillo/taxdocument/adapter/out/sri/`
- [ ] T010 Record legacy-to-target terminology and classifications in the
  feature documentation

**Checkpoint**: Boundaries, names, ports, DTO separation, idempotency, and audit
design are ready.

---

## Phase 3: User Story 1 - [Title] (Priority: P1)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 1

- [ ] T011 [P] [US1] Add domain unit tests in
  `src/test/java/com/alexastudillo/taxdocument/domain/`
- [ ] T012 [P] [US1] Add application use case tests with fake ports in
  `src/test/java/com/alexastudillo/taxdocument/application/`
- [ ] T013 [P] [US1] Add adapter mapping tests for touched adapters in
  `src/test/java/com/alexastudillo/taxdocument/adapter/`
- [ ] T014 [US1] Add idempotency or error mapping tests for this story when
  required by the plan

### Implementation for User Story 1

- [ ] T015 [US1] Implement domain behavior for [concept] in
  `src/main/java/com/alexastudillo/taxdocument/domain/`
- [ ] T016 [US1] Implement [BusinessAction]UseCase in
  `src/main/java/com/alexastudillo/taxdocument/application/`
- [ ] T017 [US1] Implement required outbound port adapters in
  `src/main/java/com/alexastudillo/taxdocument/adapter/out/`
- [ ] T018 [US1] Implement thin REST adapter mapping in
  `src/main/java/com/alexastudillo/taxdocument/adapter/in/rest/`
- [ ] T019 [US1] Add Quarkus wiring only in
  `src/main/java/com/alexastudillo/taxdocument/bootstrap/`

**Checkpoint**: User Story 1 is independently functional and constitution
compliant.

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 2

- [ ] T020 [P] [US2] Add domain unit tests for new rules
- [ ] T021 [P] [US2] Add application use case tests with fake ports
- [ ] T022 [P] [US2] Add REST, persistence, SRI, storage, queue, or webhook
  adapter tests for touched boundaries

### Implementation for User Story 2

- [ ] T023 [US2] Implement domain changes for [concept]
- [ ] T024 [US2] Implement [BusinessAction]UseCase and ports
- [ ] T025 [US2] Implement adapter mappings and infrastructure details
- [ ] T026 [US2] Wire dependencies in bootstrap

**Checkpoint**: User Stories 1 and 2 work independently and preserve existing
boundaries.

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 3

- [ ] T027 [P] [US3] Add domain unit tests for new rules
- [ ] T028 [P] [US3] Add application use case tests with fake ports
- [ ] T029 [P] [US3] Add adapter tests for touched boundaries

### Implementation for User Story 3

- [ ] T030 [US3] Implement domain changes for [concept]
- [ ] T031 [US3] Implement [BusinessAction]UseCase and ports
- [ ] T032 [US3] Implement adapter mappings and infrastructure details
- [ ] T033 [US3] Wire dependencies in bootstrap

**Checkpoint**: All selected user stories are independently functional.

---

[Add more user story phases as needed, following the same pattern]

---

## Phase N: Compliance and Polish

**Purpose**: Constitution compliance and cross-cutting verification

- [ ] TXXX Verify Clean Architecture dependencies point inward only
- [ ] TXXX Verify target names use English canonical terminology
- [ ] TXXX Verify SRI contract names and DTOs are isolated in `adapter.out.sri`
- [ ] TXXX Verify DTOs are not reused across REST, application, domain,
  persistence, and SRI boundaries
- [ ] TXXX Verify every external dependency is accessed through an application
  port
- [ ] TXXX Verify migrated legacy concepts have classifications and pending
  decisions are documented
- [ ] TXXX Verify domain and application tests run without infrastructure
- [ ] TXXX Verify audit logs exclude secrets and sensitive configuration values
- [ ] TXXX Run quickstart.md validation when present

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on Setup completion and blocks all user
  stories
- **User Stories (Phase 3+)**: Depend on Foundational completion
- **Compliance and Polish**: Depends on selected user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational
- **User Story 2 (P2)**: Can start after Foundational; may integrate with US1
  without breaking independent testability
- **User Story 3 (P3)**: Can start after Foundational; may integrate with prior
  stories without breaking independent testability

### Within Each User Story

- Tests for touched layers before implementation
- Domain before application use cases
- Application ports before outbound adapters
- Use cases before REST resources
- Adapter mappings before bootstrap wiring
- Story compliance check before moving to the next priority

### Parallel Opportunities

- Setup tasks marked [P] can run in parallel
- Foundational tasks marked [P] can run in parallel when they touch different
  files
- Tests for different layers can run in parallel
- Adapter implementations for different ports can run in parallel
- Independent user stories can run in parallel after Foundational completion

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: User Story 1.
4. Stop and validate User Story 1 independently.
5. Run the compliance gate before expanding scope.

### Incremental Delivery

1. Complete Setup and Foundational boundaries.
2. Add User Story 1, test independently, and run compliance checks.
3. Add User Story 2, test independently, and run compliance checks.
4. Add User Story 3, test independently, and run compliance checks.
5. Keep each story valuable without breaking previous stories.

### Parallel Team Strategy

1. Team completes Setup and Foundational boundaries together.
2. Developers split by independent user stories or independent adapters.
3. Integration occurs through application ports and documented contracts only.

---

## Notes

- [P] tasks touch different files and have no unfinished dependencies.
- [Story] labels map tasks to user stories for traceability.
- Each user story must remain independently completable and testable.
- Avoid vague tasks, same-file conflicts, cross-story hidden dependencies,
  business logic in adapters, Spanish legacy names in target code, and
  implementation not covered by Spec Kit artifacts.
