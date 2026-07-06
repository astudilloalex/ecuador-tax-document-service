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
  - `adapter.out.persistence`
  - `src/main/resources/db/migration/`
  - approved persistence configuration
  - domain restore path required by the spec
  - persistence adapter tests

## Validation Steps

### 1. Run All Tests

```bash
./gradlew test
```

Expected result:

- All domain/application tests continue to pass without infrastructure.
- Persistence adapter tests pass with approved test support.
- No REST, SRI, XML storage, queue, webhook, or bootstrap behavior is required.

### 2. Verify Source Scope

```bash
find src/main/java/com/alexastudillo/taxdocument -type f | sort
```

Expected result:

- Persistence-specific implementation files exist only under
  `src/main/java/com/alexastudillo/taxdocument/adapter/out/persistence/`.
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

### 5. Verify Forbidden Scope

```bash
find src/main/java/com/alexastudillo/taxdocument -type f | \
  rg "adapter/in/rest|adapter/out/sri|adapter/out/storage|adapter/out/queue|adapter/out/webhook|bootstrap"
```

Expected result:

- No matches for this feature.

### 6. Verify Persistence Behavior

Expected persistence adapter test coverage:

- Save and load `TaxDocument` by `AccessKey`.
- Load by issuance identity.
- Rehydrate authorized documents preserving `authorizationNumber` and
  `authorizedAt`.
- Reject invalid persisted authorization combinations with data integrity
  errors.
- Reject duplicate `accessKey` saves with duplicate conflict errors.
- Reject duplicate issuance identity saves with duplicate conflict errors.
- Return existing `SequenceNumber` for exact repeated sequence reservation.
- Return unavailable for reserved sequence values.
- Execute repository operations inside `TransactionPort` boundaries.

### 7. Verify Migration Documentation

```bash
rg -n "issuers|establishments|issuing_points|issuance_sequences|tax_documents|access_key|document_type" \
  docs/migration/legacy-to-target-terminology.md
```

Expected result:

- All introduced target tables and key columns are documented in
  `docs/migration/legacy-to-target-terminology.md`.
- Each introduced database object is classified as a Target database object.
- Deferred compatibility, XML path, and audit persistence decisions reference
  the active PFV IDs.

## Handoff

Before task generation, verify that:

- [ ] `plan.md`, `research.md`, `data-model.md`, contracts, and `quickstart.md`
  exist.
- [ ] No `NEEDS CLARIFICATION` markers remain.
- [ ] No task will create REST, SRI, XML storage, queue, webhook, or bootstrap
  runtime behavior.
- [ ] Deferred PFVs remain deferred unless a later plan explicitly resolves
  them.
