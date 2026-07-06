# Quickstart: Tax Document Persistence Foundation

This guide describes validation scenarios for the persistence foundation after
implementation tasks are generated and completed. It does not define REST
endpoints, SRI adapters, XML generation, queue adapters, webhook adapters, or
database migration contents.

## Prerequisites

- Java 25 available locally.
- Gradle wrapper available as `./gradlew`.
- Container runtime available if persistence tests use Testcontainers.
- Feature implementation limited to:
  - `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/`
  - `src/main/java/com/alexastudillo/taxdocument/application/error/` for the
    framework-free persistence error contract only
  - `src/main/java/com/alexastudillo/taxdocument/application/port/out/` only
    for existing output port return signatures using Mutiny `Uni`
  - `src/main/resources/db/migration/`
  - `src/test/java/com/alexastudillo/taxdocument/adapter/out/persistence/`
  - `src/test/java/com/alexastudillo/taxdocument/application/port/out/` only
    for `ApplicationPortBoundaryTest`
  - `src/test/java/com/alexastudillo/taxdocument/domain/` for
    framework-free `TaxDocument.restore(...)` validation only
  - `build.gradle.kts` and `src/main/resources/application.properties` for
    required persistence dependencies/configuration only
  - framework-free `TaxDocument` restore behavior required by the spec

## Validation Steps

### 1. Run All Tests

```bash
./gradlew test
```

Expected result:

- All domain/application tests continue to pass without infrastructure.
- Persistence adapter tests pass with approved test support.
- No REST, SRI, XML generation, XML signing, XML storage, queue, webhook, or
  bootstrap behavior is required.
- No archive, purge, delete, production correction, rollback, repair, or
  auto-numbering behavior is required.

### 2. Verify Source Scope

```bash
find src/main/java/com/alexastudillo/taxdocument -type f | sort
```

Expected result:

- Persistence-specific implementation files exist only under
  `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/`.
- Application-layer persistence error files exist only under
  `src/main/java/com/alexastudillo/taxdocument/application/error/` and do not
  import adapter or persistence framework types.
- Existing application output port files may be updated only under
  `src/main/java/com/alexastudillo/taxdocument/application/port/out/` to expose
  Mutiny `Uni` return types and must not import persistence-framework,
  adapter-local, REST, SRI, XML, queue, storage, webhook, or bootstrap types.
- Application output port operations may return Mutiny `Uni` as the approved
  reactive boundary contract; `Uni` must not appear in domain models or
  persistence entity state.
- Framework-free domain restore tests may exist only under
  `src/test/java/com/alexastudillo/taxdocument/domain/` for
  `TaxDocument.restore(...)` validation.
- No files are created under:
  - `adapter/in/rest`
  - `adapter/out/sri`
  - `adapter/out/storage`
  - `adapter/out/queue`
  - `adapter/out/webhook`
  - `bootstrap`

### 3. Verify Domain and Application Independence

```bash
rg -n "jakarta\\.persistence|org\\.hibernate|io\\.quarkus|Panache|java\\.sql|javax\\.sql|Flyway|PostgreSQL" \
  src/main/java/com/alexastudillo/taxdocument/domain \
  src/main/java/com/alexastudillo/taxdocument/application
```

Expected result:

- No matches in domain/application source.
- Application error abstractions do not import `adapter.out.persistence`
  exception or diagnostic types.
- Application ports may import `io.smallrye.mutiny.Uni`; domain source must
  not import Mutiny.

### 3a. Verify Reactive Port Boundary

```bash
rg -n "Uni<" src/main/java/com/alexastudillo/taxdocument/application/port/out
```

Expected result:

- Every application output port operation returns `Uni`.
- `Uni` wraps only domain/application payload types.
- Domain source files do not import `io.smallrye.mutiny`.
- `TaxDocumentRepository`, `SequenceNumberPort`, and `TransactionPort`
  signatures match the SPEC 003 persistence port contract.

### 3b. Verify Reactive Database Boundary

```bash
rg -n "quarkus-jdbc|EntityManager|javax\\.sql|java\\.sql|hibernate-orm" \
  build.gradle.kts \
  src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence \
  src/main/resources/application.properties
```

Expected result:

- No runtime persistence dependency or adapter code uses JDBC, blocking JPA
  `EntityManager`, or blocking Hibernate ORM for repository, sequence, or
  transaction adapter operations.
- Any JDBC-related dependency or property needed only for Flyway migration
  execution is documented as migration-only and is not used by repository,
  sequence, or transaction adapter code.
- Runtime persistence access uses reactive PostgreSQL APIs isolated inside
  `adapter.out.persistence`.
- Flyway migration artifacts remain versioned schema-management artifacts and
  do not become the runtime database access path.

### 4. Verify Target Schema Naming

```bash
rg -n "CREATE TABLE|access_key|document_type|issuance_sequences|tax_documents" src/main/resources/db/migration
```

Expected result:

- Required target tables exist:
  - `issuers`
  - `establishments`
  - `issuing_points`
  - `issuance_sequences`
  - `tax_documents`
- Table and column names use English lowercase snake_case.
- `document_type` stores canonical target values, not SRI numeric codes.
- Primary keys, foreign keys, unique constraints, important indexes, and
  delete/update restrictions are present for required tables.
- `issue_date` is represented as a date and `authorized_at` is represented as a
  UTC-normalized timestamp.
- No `tax_document_audit_events` table is created unless SPEC 003 plan
  explicitly justifies audit persistence. While `PFV-PER-004` remains deferred,
  the table is absent from migrations and schema documentation.

### 5. Verify Forbidden Scope

```bash
find src/main/java/com/alexastudillo/taxdocument -type f | \
  rg "adapter/in/rest|adapter/out/sri|adapter/out/storage|adapter/out/queue|adapter/out/webhook|bootstrap"
```

Expected result:

- No matches for this feature.
- No runtime bootstrap package or bootstrap class is created for persistence
  wiring.

### 6. Verify Persistence Behavior

Expected persistence adapter test coverage:

- Save and load `TaxDocument` by `AccessKey`.
- Load by issuance identity.
- Missing `findByAccessKey` and `findByIssuanceIdentity` return empty results.
- Missing `existsByAccessKey` and `existsByIssuanceIdentity` return `false`.
- `save` creates a new persisted document when no matching record exists.
- `save` updates the same persisted aggregate when the same tax document
  identity already exists.
- `save` never silently overwrites another document with a duplicate
  `accessKey` or duplicate issuance identity.
- Rehydrate authorized documents preserving `authorizationNumber` and
  `authorizedAt`.
- Reject invalid persisted authorization combinations with data integrity
  errors.
- Reject unknown canonical document type, document state, authorization state,
  and issuance mode values with data integrity errors.
- Reject missing or inconsistent issuer, establishment, and issuing point
  relationships with data integrity errors.
- Rehydrate `issue_date` as the same calendar date and `authorized_at` at the
  documented UTC precision.
- Reject duplicate `accessKey` saves with duplicate conflict errors.
- Reject duplicate issuance identity saves with duplicate conflict errors.
- Return existing `SequenceNumber` for exact repeated sequence reservation.
- Fail conflicting duplicate sequence reservations with application-facing
  sequence reservation conflict errors.
- Return unavailable for reserved sequence values through availability checks.
- Execute repository operations inside `TransactionPort` boundaries.
- Do not cover archive, purge, delete, production correction, migration
  rollback, migration repair, or automatic numbering behavior in this feature.

### 7. Verify Migration Documentation

```bash
rg -n "issuers|establishments|issuing_points|issuance_sequences|tax_documents|access_key|document_type" \
  docs/migration/legacy-to-target-terminology.md
```

Expected result:

- All introduced target tables and key columns are documented in
  `docs/migration/legacy-to-target-terminology.md`.
- Each introduced database object is classified as a Target database object.
- Deferred compatibility, XML path, audit persistence, production data
  migration, auto-numbering policy, migration rollback/repair, and lifecycle
  correction decisions reference the active PFV IDs.

## Deferred PFV Summary

The following items remain excluded from SPEC 003 task generation unless a
future specification explicitly resolves them:

- `PFV-PER-001`: automatic sequence increment behavior.
- `PFV-PER-002`: legacy compatibility views.
- `PFV-PER-003`: historical XML path storage.
- `PFV-PER-004`: audit persistence.
- `PFV-PER-005`: auto-numbering policy.
- `PFV-PER-006`: migration failure handling, rollback playbooks, and persisted
  data repair workflows.
- `PFV-PER-007`: archive, purge, delete, production correction, and lifecycle
  correction workflows.
- `PFV-PER-008`: production data migration.

## Handoff

Before task generation, verify that:

- [x] `plan.md`, `research.md`, `data-model.md`, contracts, and `quickstart.md`
  exist.
- [x] No unresolved clarification markers remain.
- [x] No task will create REST, SRI, XML generation, XML signing, XML storage,
  queue, webhook, or bootstrap runtime behavior.
- [x] No task will create archive, purge, delete, production correction,
  migration rollback, migration repair, or automatic numbering behavior.
- [x] Deferred PFVs remain deferred unless a later plan explicitly resolves
  them.
