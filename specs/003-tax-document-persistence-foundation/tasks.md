# Tasks: Tax Document Persistence Foundation

**Input**: Design documents from `specs/003-tax-document-persistence-foundation/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Tests**: Required by the constitution and SPEC 003. Domain restore tests must run without Quarkus. Persistence adapter tests may use Quarkus test support and Testcontainers. No REST, SRI, XML generation/signing/storage, queue, webhook, bootstrap runtime wiring, document-specific issuance flow, archive, purge, delete, production correction, migration rollback/repair, auto-numbering, or production data migration task is included.

**Organization**: Tasks are grouped by user story to enable independently reviewable increments while preserving Clean Architecture boundaries.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it touches different files and has no dependency on another unfinished task
- **[Story]**: User story label for story phases only
- Every task includes an exact file path

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add only approved persistence dependencies, configuration, and package scaffolding.

- [ ] T001 Add Quarkus Hibernate ORM, JDBC PostgreSQL, Flyway, and Testcontainers dependencies in `build.gradle.kts`
- [ ] T002 Add persistence-only datasource and Flyway configuration placeholders in `src/main/resources/application.properties`
- [ ] T003 [P] Create persistence adapter package markers in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/package-info.java`
- [ ] T004 [P] Create persistence adapter test package markers in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/package-info.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create schema, shared adapter infrastructure, and error translation required by all persistence stories.

**Critical**: No user story implementation should start until this phase is complete.

- [ ] T005 Create Flyway schema migration for `issuers`, `establishments`, `issuing_points`, `issuance_sequences`, and `tax_documents` with English snake_case names, primary keys, foreign keys, unique constraints, indexes, temporal columns, and restrictive delete/update rules in `src/main/resources/db/migration/V1__create_tax_document_persistence_foundation.sql`
- [ ] T006 [P] Create stable persistence failure categories in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/error/PersistenceFailureCategory.java`
- [ ] T007 [P] Create persistence adapter exception type carrying stable categories in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/error/PersistenceAdapterException.java`
- [ ] T008 Create persistence exception translator for duplicate, relationship, invalid state, transaction, and generic failures in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/error/PersistenceExceptionTranslator.java`
- [ ] T009 [P] Create issuer hierarchy JPA entities for `issuers`, `establishments`, and `issuing_points` in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/entity/IssuerEntity.java`, `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/entity/EstablishmentEntity.java`, and `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/entity/IssuingPointEntity.java`
- [ ] T010 [P] Add shared persistence test fixture builders for issuer, establishment, issuing point, sequence, and tax document data in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceTestData.java`
- [ ] T011 [P] Add transaction adapter tests for return-value, void, and translated failure behavior in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceTransactionAdapterTest.java`
- [ ] T012 Implement `TransactionPort` using adapter-local transaction handling in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/transaction/PersistenceTransactionAdapter.java`

**Checkpoint**: Migration foundation, shared entities, transaction boundary, and persistence error translation are ready without inward framework leakage.

---

## Phase 3: User Story 1 - Persist and Load Tax Documents (Priority: P1)

**Goal**: Persist a common `TaxDocument`, load it by `AccessKey` and issuance identity, rehydrate persisted state, and preserve temporal/authorization fields without exposing persistence entities.

**Independent Test**: Save a common tax document through `TaxDocumentRepository`, load it by access key and issuance identity, verify preserved identity/state/authorization/temporal values, and verify missing lookups return empty or `false`.

### Tests for User Story 1

- [ ] T013 [P] [US1] Add domain restore tests for persisted non-`PENDING` state, authorized state, invalid authorization combinations, and framework-free imports in `src/test/java/com/alexastudillo/taxdocument/domain/taxdocument/TaxDocumentRestoreTest.java`
- [ ] T014 [P] [US1] Add tax document persistence mapper tests for domain-to-entity and entity-to-domain rehydration, canonical enum names, and temporal precision in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/TaxDocumentPersistenceMapperTest.java`
- [ ] T015 [P] [US1] Add repository save/load/missing lookup tests for access key and issuance identity in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceTaxDocumentRepositoryTest.java`
- [ ] T016 [P] [US1] Add invalid persisted tax document state translation tests for unknown enum values and inconsistent relationships in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceErrorTranslationTest.java`

### Implementation for User Story 1

- [ ] T017 [US1] Add framework-free `TaxDocument.restore(...)` rehydration path preserving access key, document type, issuer, establishment, issuing point, sequence number, issue date, document state, authorization state, authorization number, authorized timestamp, issuance mode, and external request ID in `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/TaxDocument.java`
- [ ] T018 [US1] Create tax document JPA entity for `tax_documents` using canonical field names and adapter-only annotations in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/entity/TaxDocumentEntity.java`
- [ ] T019 [US1] Create mapper between `TaxDocument` and `TaxDocumentEntity` with domain restore and no SRI code translation in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/mapper/TaxDocumentPersistenceMapper.java`
- [ ] T020 [US1] Implement `TaxDocumentRepository.save`, `findByAccessKey`, `findByIssuanceIdentity`, `existsByAccessKey`, and `existsByIssuanceIdentity` using domain/application signatures only in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/repository/PersistenceTaxDocumentRepository.java`
- [ ] T021 [US1] Implement missing-record behavior returning `Optional.empty()` or `false` without throwing not-found persistence exceptions in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/repository/PersistenceTaxDocumentRepository.java`
- [ ] T022 [US1] Implement invalid persisted state and relationship translation to stable persistence failure categories in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/error/PersistenceExceptionTranslator.java`

**Checkpoint**: User Story 1 can persist and rehydrate tax documents and handle missing/invalid persisted data without leaking persistence entities or framework types.

---

## Phase 4: User Story 2 - Protect Issuance Identity and Access Key Uniqueness (Priority: P2)

**Goal**: Reject duplicate access keys and duplicate issuance identities consistently, including races after earlier existence checks.

**Independent Test**: Persist one tax document, attempt another save with the same `accessKey`, attempt another save with the same issuance identity, and verify stable duplicate conflict categories without overwriting existing rows.

### Tests for User Story 2

- [ ] T023 [P] [US2] Add duplicate access key conflict tests for pre-save and database-enforced race scenarios in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/TaxDocumentDuplicatePersistenceTest.java`
- [ ] T024 [P] [US2] Add duplicate issuance identity conflict tests for pre-save and database-enforced race scenarios in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/TaxDocumentDuplicatePersistenceTest.java`
- [ ] T025 [P] [US2] Add same-aggregate update tests proving `save` updates the same persisted tax document but does not replace a different document in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceTaxDocumentRepositoryTest.java`

### Implementation for User Story 2

- [ ] T026 [US2] Enforce duplicate access key and duplicate issuance identity translation to `DuplicateAccessKeyConflict` and `DuplicateIssuanceIdentityConflict` in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/error/PersistenceExceptionTranslator.java`
- [ ] T027 [US2] Implement same-aggregate update and different-aggregate conflict behavior in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/repository/PersistenceTaxDocumentRepository.java`
- [ ] T028 [US2] Ensure database unique constraint names for `access_key` and issuance identity are mapped to stable duplicate categories in `src/main/resources/db/migration/V1__create_tax_document_persistence_foundation.sql`

**Checkpoint**: Duplicate access key and duplicate issuance identity scenarios are stable, race-safe, and application-facing.

---

## Phase 5: User Story 3 - Reserve Sequence Numbers Safely (Priority: P3)

**Goal**: Reserve requested sequence numbers per issuer, establishment, issuing point, and document type with exact-repeat idempotency and conflict prevention.

**Independent Test**: Reserve a requested sequence value, reserve the exact same identity again, check availability, and verify conflicting duplicate reservations fail with a stable conflict category.

### Tests for User Story 3

- [ ] T029 [P] [US3] Add sequence reservation tests for first reservation and exact repeated idempotent reservation in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceSequenceNumberAdapterTest.java`
- [ ] T030 [P] [US3] Add sequence availability and conflicting duplicate reservation tests in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceSequenceNumberAdapterTest.java`

### Implementation for User Story 3

- [ ] T031 [US3] Create issuance sequence JPA entity for `issuance_sequences` with canonical `document_type` and requested `sequence_number` fields in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/entity/IssuanceSequenceEntity.java`
- [ ] T032 [US3] Create mapper between `IssuanceSequenceEntity` and domain `SequenceNumber` in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/mapper/SequenceNumberPersistenceMapper.java`
- [ ] T033 [US3] Implement `SequenceNumberPort.reserve` and `isAvailable` with requested-value behavior in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/repository/PersistenceSequenceNumberAdapter.java`
- [ ] T034 [US3] Translate unavailable sequence reservation conflicts to `UnavailableSequenceReservationConflict` in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/error/PersistenceExceptionTranslator.java`

**Checkpoint**: Sequence reservation is requested-value only, idempotent for exact repeats, conflict-safe for duplicates, and does not introduce auto-numbering.

---

## Phase 6: User Story 4 - Preserve Architecture and Migration Traceability (Priority: P4)

**Goal**: Keep persistence artifacts, schema names, and migration mappings traceable to canonical English terminology and the constitution.

**Independent Test**: Review source paths and migration documentation to verify persistence code is adapter-only, schema names are English lowercase snake_case, deferred PFVs remain deferred, and legacy Spanish names appear only in approved migration documentation contexts.

### Tests for User Story 4

- [ ] T035 [P] [US4] Add Clean Architecture boundary tests ensuring domain/application do not import JPA, Hibernate, Panache, JDBC, SQL, PostgreSQL, Flyway, Quarkus persistence APIs, or adapter types in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceBoundaryTest.java`
- [ ] T036 [P] [US4] Add forbidden scope tests ensuring no SPEC 003 source exists under `adapter/in/rest`, `adapter/out/sri`, `adapter/out/storage`, `adapter/out/queue`, `adapter/out/webhook`, or `bootstrap` in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceBoundaryTest.java`
- [ ] T037 [P] [US4] Add migration naming tests for English lowercase snake_case tables, canonical `document_type` values, and absence of SRI numeric document type storage in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceSchemaMigrationTest.java`
- [ ] T038 [P] [US4] Add migration documentation coverage tests for target tables, columns, constraints, temporal mappings, and PFV references in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/MigrationDocumentationCoverageTest.java`

### Implementation for User Story 4

- [ ] T039 [US4] Update durable migration mapping documentation with target tables, target columns, constraints, temporal mappings, classifications, and PFV references in `docs/migration/legacy-to-target-terminology.md`
- [ ] T040 [US4] Add package-level documentation stating persistence adapter scope and forbidden behavior in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/package-info.java`
- [ ] T041 [US4] Ensure persistence diagnostics exclude credentials, tokens, passwords, private keys, connection strings with secrets, and sensitive configuration values in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/error/PersistenceExceptionTranslator.java`

**Checkpoint**: Architecture boundaries, English terminology, migration classification, and deferred PFVs are traceable and reviewable.

---

## Phase 7: Compliance and Polish

**Purpose**: Validate the full persistence foundation against the constitution, quickstart, and task traceability rules.

- [ ] T042 Run the full test suite and address SPEC 003 failures only in `specs/003-tax-document-persistence-foundation/quickstart.md`
- [ ] T043 Verify no out-of-scope packages or runtime wiring were created by comparing source layout against `specs/003-tax-document-persistence-foundation/quickstart.md`
- [ ] T044 Verify every completed task maps to SPEC 003 requirements, contracts, plan sections, or data-model sections in `specs/003-tax-document-persistence-foundation/tasks.md`
- [ ] T045 Verify deferred PFVs remain excluded from implementation and task scope in `specs/003-tax-document-persistence-foundation/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependencies.
- **Phase 2 Foundational**: Depends on Phase 1 and blocks all user stories.
- **Phase 3 US1**: Depends on Phase 2.
- **Phase 4 US2**: Depends on Phase 3 because duplicate save behavior builds on repository persistence.
- **Phase 5 US3**: Depends on Phase 2 and can run after US1 if shared issuer hierarchy persistence is available.
- **Phase 6 US4**: Depends on Phases 2-5 for full migration and boundary coverage.
- **Phase 7 Compliance and Polish**: Depends on selected story phases being complete.

### User Story Dependencies

- **US1 (P1)**: MVP; enables common tax document persistence and rehydration.
- **US2 (P2)**: Builds on US1 repository save/load behavior.
- **US3 (P3)**: Uses foundational schema and issuer hierarchy; independent from US2 conflict behavior except shared error translation.
- **US4 (P4)**: Validates final architecture and durable migration traceability after persistence artifacts exist.

### Within Each User Story

- Write tests before implementation tasks for that story.
- Domain restore behavior precedes mapper and repository rehydration.
- Migration constraints precede adapter tests that depend on database uniqueness.
- Mappers precede repository adapters.
- Error translation is completed before conflict behavior is considered done.
- Story checkpoint must pass before expanding to the next priority.

## Parallel Opportunities

- **Setup**: T003 and T004 can run in parallel after T001/T002 planning context is clear.
- **Foundational**: T006, T007, T009, T010, and T011 can run in parallel because they touch different files.
- **US1**: T013, T014, T015, and T016 can be prepared in parallel; T018 and T019 can run in parallel after T017.
- **US2**: T023, T024, and T025 can run in parallel before T026-T028.
- **US3**: T029 and T030 can run in parallel; T031 and T032 can run in parallel before T033.
- **US4**: T035, T036, T037, and T038 can run in parallel before T039-T041.

## Parallel Execution Examples

```bash
# US1 tests can be drafted in parallel
Task T013: src/test/java/com/alexastudillo/taxdocument/domain/taxdocument/TaxDocumentRestoreTest.java
Task T014: src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/TaxDocumentPersistenceMapperTest.java
Task T015: src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceTaxDocumentRepositoryTest.java
```

```bash
# US4 boundary and documentation tests can be drafted in parallel
Task T035: src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceBoundaryTest.java
Task T037: src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceSchemaMigrationTest.java
Task T038: src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/MigrationDocumentationCoverageTest.java
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 setup.
2. Complete Phase 2 foundational migration, shared entities, transaction, and error infrastructure.
3. Complete Phase 3 US1 for save/load/rehydration/missing lookup.
4. Stop and validate US1 independently with repository, mapper, restore, and boundary tests.
5. Run the compliance gate before continuing to US2.

### Incremental Delivery

1. Add US2 duplicate protection after US1 repository behavior is stable.
2. Add US3 sequence reservation after schema and shared issuer hierarchy persistence are stable.
3. Add US4 migration documentation and boundary validation before implementation completion.
4. Run Phase 7 compliance and quickstart validation.

### Scope Guardrails

- Do not create `adapter.in.rest`, `adapter.out.sri`, `adapter.out.storage`, `adapter.out.queue`, `adapter.out.webhook`, or `bootstrap` packages.
- Do not create REST DTOs, SRI SOAP/XML DTOs, XML generation/signing/storage, queue adapters, webhook adapters, bootstrap runtime wiring, document-specific issuance flows, archive/purge/delete behavior, production correction, migration rollback/repair, auto-numbering, or production data migration.
- Do not annotate domain objects as JPA entities.
- Do not expose persistence entities, SQL exceptions, Hibernate exceptions, Panache exceptions, PostgreSQL-specific exceptions, Flyway exceptions, or Quarkus persistence types outside `adapter.out.persistence`.
