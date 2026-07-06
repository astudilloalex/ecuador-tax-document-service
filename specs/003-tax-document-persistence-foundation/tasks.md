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

- [X] T001 Add Quarkus Mutiny, Hibernate ORM, JDBC PostgreSQL, Flyway, and Testcontainers dependencies in `build.gradle.kts`
- [X] T002 Add persistence-only datasource and Flyway configuration placeholders in `src/main/resources/application.properties`
- [X] T003 [P] Create persistence adapter package markers in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/package-info.java`
- [X] T004 [P] Create persistence adapter test package markers in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/package-info.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create schema, shared adapter infrastructure, and error translation required by all persistence stories.

**Critical**: No user story implementation should start until this phase is complete.

- [X] T005 Create Flyway schema migration for `issuers`, `establishments`, `issuing_points`, `issuance_sequences`, and `tax_documents` with English snake_case names, primary keys, foreign keys, unique constraints, indexes, temporal columns, and restrictive delete/update rules in `src/main/resources/db/migration/V1__create_tax_document_persistence_foundation.sql`
- [X] T006 [P] Create stable application-layer persistence failure categories for duplicate access key conflict, duplicate issuance identity conflict, unavailable sequence reservation conflict, invalid persisted tax document state, invalid persistence relationship, generic persistence failure, and transaction failure in `src/main/java/com/alexastudillo/taxdocument/application/error/PersistenceFailureCategory.java`
- [X] T007 [P] Create framework-free application-layer persistence failure abstraction carrying stable categories without adapter or persistence framework dependencies in `src/main/java/com/alexastudillo/taxdocument/application/error/PersistenceFailure.java`
- [X] T008 Create persistence exception translator for duplicate, relationship, invalid state, transaction, and generic database/framework failures that maps to application-layer errors while keeping adapter-local diagnostics inside `adapter.out.persistence` in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/error/PersistenceExceptionTranslator.java`
- [X] T009 [P] Create issuer hierarchy JPA entities for `issuers`, `establishments`, and `issuing_points` in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/entity/IssuerEntity.java`, `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/entity/EstablishmentEntity.java`, and `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/entity/IssuingPointEntity.java`
- [X] T010 [P] Add shared persistence test fixture builders for issuer, establishment, issuing point, sequence, and tax document data in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceTestData.java`
- [X] T011 [P] Add transaction adapter tests for return-value, void, and translated failure behavior in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceTransactionAdapterTest.java`
- [X] T012 Implement `TransactionPort` using adapter-local transaction handling in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/transaction/PersistenceTransactionAdapter.java`

**Checkpoint**: Migration foundation, shared entities, transaction boundary, and persistence error translation are ready without inward framework leakage.

---

## Phase 3: User Story 1 - Persist and Load Tax Documents (Priority: P1)

**Goal**: Persist a common `TaxDocument`, load it by `AccessKey` and issuance identity, rehydrate persisted state, and preserve temporal/authorization fields without exposing persistence entities.

**Independent Test**: Save a common tax document through `TaxDocumentRepository`, load it by access key and issuance identity, verify preserved identity/state/authorization/temporal values, and verify missing lookups return empty or `false`.

### Tests for User Story 1

- [X] T013 [P] [US1] Add framework-free domain restore tests for persisted non-`PENDING` state, authorized state, invalid authorization combinations, and absence of Quarkus, JPA, Hibernate, Panache, PostgreSQL, Flyway, JDBC, SQL, REST, XML, SOAP, SRI, queue, storage, webhook, adapter, or filesystem imports in `src/test/java/com/alexastudillo/taxdocument/domain/taxdocument/TaxDocumentRestoreTest.java`
- [X] T014 [P] [US1] Add tax document persistence mapper tests for domain-to-entity and entity-to-domain rehydration, canonical enum names, and temporal precision in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/TaxDocumentPersistenceMapperTest.java`
- [X] T015 [P] [US1] Add repository save/load/missing lookup tests for access key and issuance identity in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceTaxDocumentRepositoryTest.java`
- [X] T016 [P] [US1] Add invalid persisted tax document state translation tests for unknown enum values and inconsistent relationships in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceErrorTranslationTest.java`

### Implementation for User Story 1

- [X] T017 [US1] Add framework-free `TaxDocument.restore(...)` rehydration path preserving access key, document type, issuer, establishment, issuing point, sequence number, issue date, document state, authorization state, authorization number, authorized timestamp, issuance mode, and external request ID in `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/TaxDocument.java`
- [X] T018 [US1] Create tax document JPA entity for `tax_documents` using canonical field names and adapter-only annotations in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/entity/TaxDocumentEntity.java`
- [X] T019 [US1] Create mapper between `TaxDocument` and `TaxDocumentEntity` with domain restore and no SRI code translation in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/mapper/TaxDocumentPersistenceMapper.java`
- [X] T020 [US1] Implement `TaxDocumentRepository.save`, `findByAccessKey`, `findByIssuanceIdentity`, `existsByAccessKey`, and `existsByIssuanceIdentity` using Mutiny `Uni` domain/application signatures only in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/repository/PersistenceTaxDocumentRepository.java`
- [X] T021 [US1] Implement missing-record behavior returning `Uni` results containing `Optional.empty()` or `false` without throwing not-found persistence exceptions in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/repository/PersistenceTaxDocumentRepository.java`
- [X] T022 [US1] Implement invalid persisted state and relationship translation to stable application-layer persistence failure categories in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/error/PersistenceExceptionTranslator.java`

**Checkpoint**: User Story 1 can persist and rehydrate tax documents and handle missing/invalid persisted data without leaking persistence entities or framework types.

---

## Phase 4: User Story 2 - Protect Issuance Identity and Access Key Uniqueness (Priority: P2)

**Goal**: Reject duplicate access keys and duplicate issuance identities consistently, including races after earlier existence checks.

**Independent Test**: Persist one tax document, attempt another save with the same `accessKey`, attempt another save with the same issuance identity, and verify stable duplicate conflict categories without overwriting existing rows.

### Tests for User Story 2

- [X] T023 [P] [US2] Add duplicate access key conflict tests for pre-save and database-enforced race scenarios in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/TaxDocumentDuplicatePersistenceTest.java`
- [X] T024 [P] [US2] Add duplicate issuance identity conflict tests for pre-save and database-enforced race scenarios in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/TaxDocumentDuplicatePersistenceTest.java`
- [X] T025 [P] [US2] Add same-aggregate update tests proving `save` updates the same persisted tax document but does not replace a different document in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceTaxDocumentRepositoryTest.java`

### Implementation for User Story 2

- [X] T026 [US2] Enforce duplicate access key and duplicate issuance identity translation to application-layer `DuplicateAccessKeyConflict` and `DuplicateIssuanceIdentityConflict` categories in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/error/PersistenceExceptionTranslator.java`
- [X] T027 [US2] Implement same-aggregate update and different-aggregate conflict behavior in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/repository/PersistenceTaxDocumentRepository.java`
- [X] T028 [US2] Ensure database unique constraint names for `access_key` and issuance identity are mapped to stable application-layer duplicate categories in `src/main/resources/db/migration/V1__create_tax_document_persistence_foundation.sql`

**Checkpoint**: Duplicate access key and duplicate issuance identity scenarios are stable, race-safe, and application-facing.

---

## Phase 5: User Story 3 - Reserve Sequence Numbers Safely (Priority: P3)

**Goal**: Reserve requested sequence numbers per issuer, establishment, issuing point, and document type with exact-repeat idempotency and conflict prevention.

**Independent Test**: Reserve a requested sequence value, reserve the exact same identity again, check availability, and verify conflicting duplicate reservations fail with a stable conflict category.

### Tests for User Story 3

- [X] T029 [P] [US3] Add sequence reservation tests for first reservation and exact repeated idempotent reservation in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceSequenceNumberAdapterTest.java`
- [X] T030 [P] [US3] Add sequence availability and conflicting duplicate reservation tests in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceSequenceNumberAdapterTest.java`

### Implementation for User Story 3

- [X] T031 [US3] Create issuance sequence JPA entity for `issuance_sequences` with canonical `document_type` and requested `sequence_number` fields in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/entity/IssuanceSequenceEntity.java`
- [X] T032 [US3] Create mapper between `IssuanceSequenceEntity` and domain `SequenceNumber` in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/mapper/SequenceNumberPersistenceMapper.java`
- [X] T033 [US3] Implement `SequenceNumberPort.reserve` and `isAvailable` with Mutiny `Uni` requested-value behavior in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/repository/PersistenceSequenceNumberAdapter.java`
- [X] T034 [US3] Translate unavailable sequence reservation conflicts to the application-layer `UnavailableSequenceReservationConflict` category in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/error/PersistenceExceptionTranslator.java`

**Checkpoint**: Sequence reservation is requested-value only, idempotent for exact repeats, conflict-safe for duplicates, and does not introduce auto-numbering.

---

## Phase 6: User Story 4 - Preserve Architecture and Migration Traceability (Priority: P4)

**Goal**: Keep persistence artifacts, schema names, and migration mappings traceable to canonical English terminology and the constitution.

**Independent Test**: Review source paths and migration documentation to verify persistence code is adapter-only, schema names are English lowercase snake_case, deferred PFVs remain deferred, and legacy Spanish names appear only in approved migration documentation contexts.

### Tests for User Story 4

- [X] T035 [P] [US4] Add Clean Architecture boundary tests ensuring domain/application do not import JPA, Hibernate, Panache, JDBC, SQL, PostgreSQL, Flyway, Quarkus persistence APIs, or adapter types in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceBoundaryTest.java`
- [X] T036 [P] [US4] Add forbidden scope tests ensuring no SPEC 003 source exists under `adapter/in/rest`, `adapter/out/sri`, `adapter/out/storage`, `adapter/out/queue`, `adapter/out/webhook`, or `bootstrap` in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceBoundaryTest.java`
- [X] T037 [P] [US4] Add migration naming and scope tests for English lowercase snake_case tables, canonical `document_type` values, absence of SRI numeric document type storage, and absence of `tax_document_audit_events` unless SPEC 003 plan explicitly justifies audit persistence; if audit persistence remains deferred as `PFV-PER-004`, assert that the table is absent from migrations and schema documentation in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/PersistenceSchemaMigrationTest.java`
- [X] T038 [P] [US4] Add migration documentation coverage tests for target tables, columns, constraints, temporal mappings, and PFV references in `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/MigrationDocumentationCoverageTest.java`

### Implementation for User Story 4

- [X] T039 [US4] Update durable migration mapping documentation with target tables, target columns, constraints, temporal mappings, classifications, and PFV references in `docs/migration/legacy-to-target-terminology.md`
- [X] T040 [US4] Add package-level documentation stating persistence adapter scope and forbidden behavior in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/package-info.java`
- [X] T041 [US4] Ensure persistence diagnostics exclude credentials, tokens, passwords, private keys, connection strings with secrets, and sensitive configuration values in `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/error/PersistenceExceptionTranslator.java`

**Checkpoint**: Architecture boundaries, English terminology, migration classification, and deferred PFVs are traceable and reviewable.

---

## Phase 7: Compliance and Polish

**Purpose**: Validate the full persistence foundation against the constitution, quickstart, and task traceability rules.

- [X] T042 [Validation] Run the full test suite using the commands documented in `specs/003-tax-document-persistence-foundation/quickstart.md`. If failures are caused by SPEC 003 implementation or test files owned by this task set, fix only those SPEC 003-owned source or test files. Do not modify out-of-scope adapters, REST, SRI, XML, queue, webhook, storage, bootstrap, or document-specific issuance code. If a failure requires an out-of-scope change, document it as a finding instead of fixing it.
- [X] T043 Verify no out-of-scope packages or runtime wiring were created by comparing source layout against `specs/003-tax-document-persistence-foundation/quickstart.md`
- [X] T044 Verify every completed task maps to SPEC 003 requirements, contracts, plan sections, or data-model sections in `specs/003-tax-document-persistence-foundation/tasks.md`
- [X] T045 Verify deferred PFVs remain excluded from implementation and task scope in `specs/003-tax-document-persistence-foundation/quickstart.md`

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
