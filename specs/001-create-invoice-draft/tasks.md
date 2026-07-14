---

description: "Dependency-ordered implementation tasks for Create Invoice Draft"
---

# Tasks: Create Invoice Draft

**Input**: Approved design documents from `specs/001-create-invoice-draft/`

**Prerequisites**: Approved `spec.md`; reconciled `plan.md`; complete `research.md`; approved
`reference-data-baseline.md`; `data-model.md`; `persistence-design.md`; `idempotency-design.md`;
`error-catalog.md`; `operational-requirements.md`; `traceability.md`; `quickstart.md`; validated
`contracts/invoice-draft-api.openapi.yaml`; and completed requirements-quality checklists.

**Analysis Gate**: `$speckit-analyze` MUST run after this file is generated and before any task is
implemented. Unresolved critical findings MUST block `$speckit-implement`.

**Tests**: Every applicable constitutional test category is included before the production work it
covers. SRI SOAP, XML, access-key, signature, certificate, PDF, notification, and other external
adapter tests are not applicable because the approved plan excludes those capabilities entirely.

**Organization**: The feature has one independently valuable P1 story. Setup and foundational
controls precede its evidence-first implementation; Definition of Done validation follows it.

## Format: `[ID] [P?] [Story] Description with exact file path and requirement reference`

- **[P]**: May run in parallel only after its stated prerequisites are complete because it changes
  a different file and has no remaining dependency on an incomplete task.
- **[US1]**: Serves User Story 1, Create and Review an Invoice Draft.
- Every path is repository-relative and uses the approved English terminology.

## Phase 1: Setup

**Purpose**: Establish the approved Java 25, Quarkus 3.33.2.1 LTS, Clean Architecture, and test
tooling baseline before feature dependencies or source implementation.

- [ ] T001 Align both Quarkus plugin and platform versions to 3.33.2.1 LTS per the approved runtime decision in `gradle.properties`
- [ ] T002 Configure exactly the Quarkus-BOM-managed `quarkus-rest-jackson`, `quarkus-hibernate-validator`, `quarkus-hibernate-reactive-panache`, `quarkus-reactive-pg-client`, `quarkus-flyway`, `quarkus-jdbc-postgresql`, `quarkus-smallrye-openapi`, `quarkus-smallrye-health`, `quarkus-micrometer`, `quarkus-opentelemetry`, `quarkus-junit`, `quarkus-test-vertx`, `quarkus-test-hibernate-reactive-panache`, and `io.rest-assured:rest-assured` dependencies; Spotless `8.8.0` with google-java-format `1.35.0`; and JDK 25 `-Xlint:all -Werror` in `build.gradle.kts`
- [ ] T003 [P] Establish the API capability boundary documented by the plan in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/package-info.java`
- [ ] T004 [P] Establish the application capability boundary documented by the plan in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/package-info.java`
- [ ] T005 [P] Establish the framework-free domain capability boundary documented by the plan in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/package-info.java`
- [ ] T006 [P] Establish the infrastructure capability boundary documented by the plan in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/package-info.java`
- [ ] T007 Configure sanitized Quarkus Database Dev Services with `docker.io/library/postgres:18.4`, Flyway, timeout, observability, and deterministic clock/identifier overrides for tests in `src/test/resources/application.properties`

**Checkpoint**: The build and four production boundaries match the approved plan without adding
identity, Company, cache, broker, or fiscal-issuance capabilities.

---

## Phase 2: Foundational Persistence and Architecture Controls

**Purpose**: Create the test and persistence foundations that block User Story 1 production work.

**CRITICAL**: User Story 1 production tasks MUST NOT begin until this phase is complete.

- [ ] T008 Create reusable Quarkus Database Dev Services context, schema-reset, and row-count support for PostgreSQL migration, transaction, timeout, and concurrency evidence without declaring or starting a second Testcontainers lifecycle in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/PostgreSqlTestResource.java`
- [ ] T009 [P] Add Clean Architecture and prohibited-dependency assertions for Constitution IV, VII, XV, and XVI in `src/test/java/com/alexastudillo/taxdocument/architecture/CleanArchitectureTest.java`
- [ ] T010 [P] Add empty-database Flyway, schema constraint, prohibited-structure, and repeatability tests for FR-020–FR-024/FR-030/FR-036–FR-039/SC-002/SC-017/SC-022–SC-024 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftMigrationTest.java`
- [ ] T011 [P] Add exact 5-buyer/6-IVA/8-payment row-count, evidence metadata, UUIDv5 recalculation, validity-overlap, and no-runtime-generation tests for FR-045–FR-047/DR-001/SC-031–SC-032 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/ReferenceDataBaselineTest.java`
- [ ] T012 Configure Flyway-only schema evolution, non-mutating Hibernate validation, reactive and migration datasources, pool/query/write/overall timeouts, 2 MiB payload bounds, and PostgreSQL-only readiness in `src/main/resources/application.properties`
- [ ] T013 Create the exact versioned buyer-identification, IVA-rule, and payment-method catalog structures from the approved model in `src/main/resources/db/migration/V1__create_invoice_draft_reference_catalogs.sql`
- [ ] T014 Seed exactly the approved SRI-OFFLINE-2.32-TARGET-1 rows and fixed UUIDs with verification queries in `src/main/resources/db/migration/V2__seed_invoice_draft_reference_data.sql`
- [ ] T015 Create the Invoice Draft root, line, line-tax, grouped-tax, payment, and additional-information structures and constraints for FR-020–FR-024/FR-037–FR-038/DR-020–DR-023 in `src/main/resources/db/migration/V3__create_invoice_draft_aggregate.sql`
- [ ] T016 Create the local Company-scoped binding with `UNIQUE (company_id, idempotency_key_hash)`, 32-byte hashes, normalization version, unique draft, and composite Company/draft foreign key for FR-027–FR-034 in `src/main/resources/db/migration/V4__create_invoice_draft_idempotency.sql`

**Checkpoint**: Empty PostgreSQL can be migrated to the exact approved local schema and reference
baseline, with no Company master data, identity state, fiscal snapshot, or cross-service relation.

---

## Phase 3: User Story 1 - Create and Review an Invoice Draft (Priority: P1) MVP

**Goal**: Allow an internal billing client to create and immediately review one complete,
Company-scoped USD Invoice Draft while preserving deterministic calculation, atomic persistence,
durable idempotency, safe errors, and the explicit pre-issuance boundary.

**Independent Test**: Submit `POST /api/v1/invoice-drafts` with exactly one valid
`X-Company-Id`, one valid `Idempotency-Key`, approved catalog identifiers, a current Ecuadorian
emission date, valid buyer/line/payment inputs, and matching system-calculated totals. Exactly one
complete `DRAFT` is committed and returned; equivalent replay returns it; conflicting or invalid
requests create no partial state; and no excluded Company, identity, or fiscal side effect occurs.

### Required Evidence for User Story 1

- [ ] T017 [P] [US1] Add exact HALF_UP, percentage, zero-treatment, numeric-boundary, grouped-overflow, and payment-sum golden vectors for FR-010–FR-016/FR-044/DR-002–DR-011/SC-003–SC-004/SC-029 in `src/test/resources/invoicedraft/calculation-vectors.json`
- [ ] T018 [P] [US1] Add normalization-version-1 key-hash and fingerprint golden vectors covering UUID, text, decimal, null/empty, property-order, line-order, payment-order, additional-information-order, Company, key, and correlation rules for FR-027–FR-033/DR-017–DR-021/SC-012/SC-018–SC-019/SC-030/SC-033 in `src/test/resources/invoicedraft/idempotency-v1-vectors.json`
- [ ] T019 [P] [US1] Add pure BigDecimal calculation, rounding, exact intermediate, zero-value, range, inconsistent-total rejection, and deterministic repetition tests using T017 for FR-010–FR-016/FR-044/DR-002–DR-011/DR-013/DR-016/SC-003–SC-004/SC-011/SC-029 in `src/test/java/com/alexastudillo/taxdocument/domain/invoicedraft/InvoiceDraftCalculatorTest.java`
- [ ] T020 [P] [US1] Add all five approved buyer strategy, final-consumer USD 50.00, activity/effectivity, unsupported-type rejection, and no-checksum/registry tests for FR-007/FR-028/DR-001/DR-013–DR-015/SC-010 in `src/test/java/com/alexastudillo/taxdocument/domain/invoicedraft/BuyerTest.java`
- [ ] T021 [P] [US1] Add one-effective-IVA-rule, inactive/inapplicable rejection, four-treatment, separate-zero-group, caller-selection, non-IVA, multiple-tax, and no-classification tests for FR-010–FR-012/DR-001/DR-005/DR-008/DR-013/SC-009/SC-021 in `src/test/java/com/alexastudillo/taxdocument/domain/invoicedraft/InvoiceDraftTaxTest.java`
- [ ] T022 [P] [US1] Add positive-total, zero-total, inconsistent-total rejection, exact reconciliation, duplicate-method, cardinality, and overflow tests for FR-013–FR-014/DR-013/DR-016/DR-022/SC-011/SC-020/SC-029 in `src/test/java/com/alexastudillo/taxdocument/domain/invoicedraft/PaymentReconciliationTest.java`
- [ ] T023 [P] [US1] Add aggregate status, USD, local identifier, immutable Company ownership, opaque emission point, valid child membership, invalid aggregate-relationship rejection, calculated-input exclusion, and no-snapshot tests for FR-004–FR-005/FR-017–FR-024/FR-037–FR-038/DR-013/DR-020/DR-023/SC-001/SC-005/SC-007/SC-017/SC-022 in `src/test/java/com/alexastudillo/taxdocument/domain/invoicedraft/InvoiceDraftTest.java`
- [ ] T024 [P] [US1] Add trim-before-validation, length, nonblank, control-character, email, telephone, collection-limit, canonical-name, and product-code tests for FR-008–FR-010/FR-015/FR-027/FR-035/DR-019/SC-015–SC-016 in `src/test/java/com/alexastudillo/taxdocument/domain/invoicedraft/DraftTextRulesTest.java`
- [ ] T025 [P] [US1] Add one-instant America/Guayaquil current-date, impossible/different-date rejection without normalization, midnight-commit, confirmed-createdAt, and later-date replay tests for FR-006/FR-019/FR-033/DR-012–DR-013/SC-013/SC-030 in `src/test/java/com/alexastudillo/taxdocument/application/invoicedraft/RequestDatePolicyTest.java`
- [ ] T026 [P] [US1] Add golden key-hash/fingerprint, privacy-minimization, version, equivalence, ordering, cross-Company-scope, and correlation-exclusion tests using T018 for FR-027–FR-034/DR-017–DR-021/SC-012/SC-018–SC-019/SC-030/SC-033 in `src/test/java/com/alexastudillo/taxdocument/application/invoicedraft/IdempotencyFingerprintTest.java`
- [ ] T027 [P] [US1] Add application use-case tests beginning at FR-041 stage 6 with an already mapped CompanyId and fixed API-captured request instant, covering normalization, local replay/conflict, current validation, calculation, atomic command handoff, response loss, and zero external calls for FR-006–FR-021/FR-024–FR-038/FR-041/FR-043–FR-047/DR-013/SC-001–SC-002/SC-012/SC-023/SC-026/SC-028 in `src/test/java/com/alexastudillo/taxdocument/application/invoicedraft/CreateInvoiceDraftUseCaseTest.java`
- [ ] T028 [P] [US1] Add OpenAPI parse/reference, operation, required-header, strict-schema, decimal, response, error-status, security-absence, and byte/semantic equality tests between the canonical `specs/001-create-invoice-draft/contracts/invoice-draft-api.openapi.yaml` and runtime publication `src/main/resources/META-INF/openapi.yaml` for FR-001–FR-005/FR-022/FR-025–FR-027/FR-039/FR-042/FR-044/SC-024–SC-025/SC-027/SC-032 in `src/test/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftOpenApiContractTest.java`
- [ ] T029 [P] [US1] Add missing, blank, repeated, malformed, nil, mixed-case, canonicalization, no-path/query/body substitution, and no-state Company-header tests for FR-001–FR-003/FR-024/FR-037/FR-040/SC-006/SC-017/SC-024 in `src/test/java/com/alexastudillo/taxdocument/api/invoicedraft/CompanyContextHeaderTest.java`
- [ ] T030 [P] [US1] Add absent, valid, blank, repeated, over-length, unsafe, non-echo, replacement, combined-precedence, response-header, and fingerprint-invariance correlation tests for FR-026/FR-029/FR-041/DR-024/SC-033 in `src/test/java/com/alexastudillo/taxdocument/api/invoicedraft/CorrelationHeaderTest.java`
- [ ] T031 [P] [US1] Add 2 MiB boundary, idempotency-key grammar, strict unknown/prohibited property, calculated-field, numeric representation, text, and collection contract tests for FR-002/FR-005/FR-008–FR-015/FR-027/FR-035/FR-041–FR-044/SC-014–SC-016/SC-025–SC-027/SC-029 in `src/test/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftRequestValidationTest.java`
- [ ] T032 [P] [US1] Add HTTP 201/200 create/replay, complete response mapping, stable Problem Details, failure precedence, safe correlation, and zero-state endpoint tests for FR-017–FR-026/FR-030–FR-033/FR-041–FR-043/SC-001–SC-008/SC-026/SC-028/SC-033 in `src/test/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftResourceTest.java`
- [ ] T033 [P] [US1] Add real-PostgreSQL active/effective reference lookup, exact approved metadata, unknown/inactive/inapplicable/ambiguous rejection, and consistent-state tests for FR-007/FR-011/FR-013/FR-045–FR-047/DR-001/DR-013/SC-009–SC-010/SC-021/SC-031–SC-032 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/ReferenceDataRepositoryAdapterTest.java`
- [ ] T034 [P] [US1] Add real-PostgreSQL aggregate round-trip, Company-plus-draft scoping, local child ownership, invalid aggregate-relationship rejection, composite binding integrity, numeric constraint, and all-or-nothing transaction tests for FR-020–FR-024/FR-030/FR-037/FR-044/DR-013/SC-001–SC-002/SC-017/SC-022/SC-029 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftRepositoryAdapterTest.java`
- [ ] T035 [P] [US1] Add 50-way equivalent winner, conflicting contender, uniqueness-loser rollback/re-read, cross-Company independence, and no-expiry tests for FR-027–FR-034/DR-017–DR-018/SC-012/SC-018 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftIdempotencyConcurrencyTest.java`
- [ ] T036 [P] [US1] Add injected failure after every write phase plus unavailable, query/write timeout, uncertain commit, post-commit response-loss recovery, and zero-partial-state assertions for FR-020–FR-021/FR-032/FR-043/DR-013/SC-002/SC-028 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftRollbackTest.java`
- [ ] T037 [P] [US1] Add liveness/readiness separation, same-datasource, migration/catalog readiness, PostgreSQL outage/recovery, and no unrelated dependency checks for Constitution XIII/FR-036/SC-023 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftHealthTest.java`
- [ ] T038 [P] [US1] Add safe-error, structured-log, metric-label, trace-attribute, raw-key, normalized-request, buyer-data, SQL, invalid-correlation, and fingerprint-storage exposure tests for FR-025–FR-026/FR-029/SC-002/SC-023/SC-033 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/SensitiveDataExposureTest.java`
- [ ] T039 [P] [US1] Add warmed typical, maximum, replay, conflict, 50-way concurrency, 10-second deadline, pool-recovery, and blocked-event-loop performance evidence for SC-012/SC-015/SC-027 and `operational-requirements.md` in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftPerformanceTest.java`
- [ ] T040 [P] [US1] Add packaged JVM boot, migration, OpenAPI, health, create, replay, conflict, correlation, rollback, timeout, date, and monetary smoke evidence for Constitution III/XII and SC-001–SC-033 in `src/test/java/com/alexastudillo/taxdocument/runtime/InvoiceDraftJvmSmokeTest.java`
- [ ] T041 [P] [US1] Add boundary evidence proving zero identity/security processing, Company lookup/dependency/master data/snapshot/cache, and fiscal-issuance adapter or side effect for FR-003/FR-023/FR-033/FR-036/FR-038–FR-040/SC-005/SC-008/SC-023–SC-025 in `src/test/java/com/alexastudillo/taxdocument/architecture/InvoiceDraftBoundaryTest.java`

### Domain Implementation for User Story 1

- [ ] T042 [P] [US1] Implement the canonical non-nil immutable Company ownership value for FR-002/FR-024/FR-037/DR-018/DR-020 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/CompanyId.java`
- [ ] T043 [P] [US1] Implement approved buyer type, format-only, contact, and final-consumer invariants for FR-007–FR-008/FR-028/DR-014–DR-015 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/Buyer.java`
- [ ] T044 [P] [US1] Implement the versioned IVA-only selected rule and four treatment invariants for FR-010–FR-012/DR-001/DR-005/DR-008 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/TaxSelection.java`
- [ ] T045 [P] [US1] Implement positive/zero amount, method identity, and reconciliation inputs for FR-013–FR-014/DR-016/DR-022 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/Payment.java`
- [ ] T046 [P] [US1] Implement trimmed name/value and canonical uniqueness semantics for FR-015/FR-035/DR-019/DR-021 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/AdditionalInformation.java`
- [ ] T047 [P] [US1] Implement stable domain validation violations without framework or transport dependencies for FR-025/FR-044 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/DraftValidationException.java`
- [ ] T048 [US1] Implement ordered line inputs, one tax selection, exact calculated amounts, text, precision, discount, and range invariants for FR-009–FR-012/DR-002–DR-005/DR-010 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/InvoiceLine.java`
- [ ] T049 [US1] Implement versioned treatment/rate grouping with distinct zero-tax treatments for FR-012/DR-005/DR-008 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/TaxTotal.java`
- [ ] T050 [US1] Implement deterministic line-level HALF_UP calculation, aggregation, overflow and inconsistent-total rejection, and payment reconciliation for FR-012–FR-016/FR-044/DR-002–DR-011/DR-013/DR-016 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/InvoiceDraftCalculator.java`
- [ ] T051 [US1] Implement the USD DRAFT aggregate with immutable CompanyId, opaque emission point, buyer, ordered local child relationships, taxes, payments, additional information, totals, timestamps, invalid-relationship rejection, and no fiscal snapshot for FR-004–FR-024/FR-037–FR-038/DR-013/DR-020/DR-023 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/InvoiceDraft.java`

### Application Implementation for User Story 1

- [ ] T052 [P] [US1] Define the transport-independent command carrying an already mapped application CompanyId, the one fixed request instant captured by the API, idempotency/correlation evidence, and mapped but not yet fingerprint-normalized commercial inputs for FR-004–FR-015/FR-026–FR-029/FR-040 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/CreateInvoiceDraftCommand.java`
- [ ] T053 [P] [US1] Define the complete new-or-replayed application result required by FR-017–FR-022/SC-001/SC-007 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/CreateInvoiceDraftResult.java`
- [ ] T054 [US1] Define the Mutiny input port for the synchronous observable create operation in FR-020/FR-022/FR-040 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/CreateInvoiceDraftUseCase.java`
- [ ] T055 [P] [US1] Define the minimal Company-scoped local aggregate, binding lookup, and atomic create boundary for FR-020–FR-024/FR-030–FR-034 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/InvoiceDraftRepository.java`
- [ ] T056 [P] [US1] Define the minimal local buyer/IVA/payment reference lookup boundary for FR-007/FR-011/FR-013/FR-045–FR-047/DR-001 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/ReferenceDataPort.java`
- [ ] T057 [P] [US1] Define the clock port whose request-time operation is called exactly once by the API after FR-041 stages 1–5 and whose commit-time operation supplies confirmed persistence instants for FR-006/FR-019/DR-012 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/RequestClock.java`
- [ ] T058 [P] [US1] Define the local non-fiscal draft and child identifier boundary for FR-018/FR-023 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/DraftIdentifierGenerator.java`
- [ ] T059 [US1] Implement version-1 domain-separated key hashing and privacy-minimal normalized request fingerprinting for FR-027–FR-034/DR-017–DR-021 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/IdempotencyFingerprint.java`
- [ ] T060 [P] [US1] Define stable application failures, retry classification, and safe violation metadata aligned to FR-025/FR-041–FR-044 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/InvoiceDraftFailure.java`
- [ ] T061 [US1] Implement only FR-041 stages 6–12 from the already mapped command: normalized content, Company-scoped replay/conflict, current catalog/domain validation including DR-013 rejection, deterministic calculation, atomic repository handoff, uniqueness-winner recovery, and safe terminal results for FR-006–FR-021/FR-024–FR-038/FR-041/FR-043–FR-047/DR-013 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/CreateInvoiceDraftService.java`

### Infrastructure Implementation for User Story 1

- [ ] T062 [P] [US1] Implement the Panache buyer-identification catalog persistence model from the exact approved schema for FR-007/FR-028/FR-045/DR-001 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/BuyerIdentificationTypeEntity.java`
- [ ] T063 [P] [US1] Implement the Panache IVA-rule catalog persistence model with fixed UUID, treatment, rate, validity, activity, version, and evidence fields for FR-011/FR-045–FR-047/DR-001 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/IvaTaxRuleEntity.java`
- [ ] T064 [P] [US1] Implement the Panache payment-method catalog persistence model with fixed UUID, validity, activity, version, and evidence fields for FR-013/FR-045–FR-047/DR-001 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/PaymentMethodEntity.java`
- [ ] T065 [P] [US1] Implement the infrastructure-only Invoice Draft root persistence model with Company ownership, buyer, status, currency, totals, and instants for FR-004/FR-007–FR-008/FR-016–FR-024/FR-037 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftEntity.java`
- [ ] T066 [P] [US1] Implement the ordered line persistence model and exact numeric columns for FR-009–FR-012/FR-044 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceLineEntity.java`
- [ ] T067 [P] [US1] Implement the one-per-line applied IVA selection persistence model for FR-010–FR-012/DR-001/DR-005 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceLineTaxEntity.java`
- [ ] T068 [P] [US1] Implement grouped treatment/code/rate tax-total persistence for FR-012/DR-008 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceTaxTotalEntity.java`
- [ ] T069 [P] [US1] Implement unique-method payment persistence with exact numeric amounts and catalog evidence for FR-013–FR-014/DR-022 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoicePaymentEntity.java`
- [ ] T070 [P] [US1] Implement ordered response and canonical-name additional-information persistence for FR-015/DR-019/DR-021 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceAdditionalInformationEntity.java`
- [ ] T071 [P] [US1] Implement the Company/key hash, request fingerprint, version, draft, and created-at binding persistence model for FR-027–FR-034 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftIdempotencyEntity.java`
- [ ] T072 [US1] Implement explicit domain-to-Panache and Panache-to-domain aggregate mappings without leaking persistence types for Constitution IV/FR-020–FR-024/FR-037 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftPersistenceMapper.java`
- [ ] T073 [P] [US1] Implement reactive local effective-date lookups and inactive, inapplicable, and ambiguity rejection for the approved buyer, IVA, and payment catalogs for FR-007/FR-011/FR-013/FR-045–FR-047/DR-013 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/ReferenceDataRepositoryAdapter.java`
- [ ] T074 [P] [US1] Implement reactive Company-scoped binding/root reads, atomic root/children/binding persistence, invalid local-relationship and partial-state rejection, unique-race rollback and winner re-read, and safe timeout/unavailable mapping for FR-020–FR-024/FR-027–FR-034/FR-043/DR-013 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftRepositoryAdapter.java`
- [ ] T075 [P] [US1] Implement the injectable system clock adapter used once by the API for the request instant and by persistence for confirmed commit instants for FR-006/FR-019/DR-012 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/SystemRequestClock.java`
- [ ] T076 [P] [US1] Implement local random UUID generation solely for draft and child identifiers, never reference or fiscal identifiers, for FR-018/FR-023/FR-046 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/UuidDraftIdentifierGenerator.java`

### API and Operational Implementation for User Story 1

- [ ] T077 [P] [US1] Implement strict client-controlled create DTOs with approved field, text, numeric-string, collection, and prohibited-property semantics for FR-004–FR-015/FR-027/FR-035/FR-042/FR-044 in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/CreateInvoiceDraftRequest.java`
- [ ] T078 [P] [US1] Implement the complete distinct response DTO with canonical CompanyId, captured inputs, calculated outputs, DRAFT, USD, and timestamps for FR-017–FR-022 in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftResponse.java`
- [ ] T079 [P] [US1] Implement exactly-one required X-Company-Id presence/cardinality/trim/UUID/nil/canonicalization handling and map it to application CompanyId for FR-001–FR-003/FR-040 in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/CompanyContextHeader.java`
- [ ] T080 [P] [US1] Implement required trimmed printable-ASCII 1–128 Idempotency-Key parsing without logging or persistence of the raw key for FR-027/FR-041 in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/IdempotencyKeyHeader.java`
- [ ] T081 [P] [US1] Implement absent generation, valid preservation, invalid non-echo/replacement, response propagation, and precedence-safe correlation initialization for FR-026/FR-041/DR-024 in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/CorrelationContextFilter.java`
- [ ] T082 [P] [US1] Implement the 2,097,152-byte request limit and earliest failure-precedence behavior for FR-041–FR-042/SC-027 in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftPayloadSizeFilter.java`
- [ ] T083 [P] [US1] Implement the safe application/problem+json transport model with stable English code, status, instance, correlation, and value-free violations for FR-025/FR-041–FR-044 in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/ProblemDetails.java`
- [ ] T084 [US1] Implement explicit request-to-command Company UUID/business-input mapping that accepts one API-captured request instant without reading the clock, plus result-to-response mapping, for FR-006/FR-022/FR-037/FR-040/DR-012 in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftApiMapper.java`
- [ ] T085 [US1] Implement exact 400/409/413/422/500/503/504 safe error mapping and correlation headers from the approved catalog for FR-025–FR-026/FR-030/FR-041–FR-044 in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftExceptionMapper.java`
- [ ] T086 [US1] Expose POST `/api/v1/invoice-drafts`, coordinate FR-041 stages 1–5, call `RequestClock` exactly once after those stages pass, pass that instant to T084, and implement required Company/idempotency headers, optional correlation, 201 new, 200 replay, and complete response semantics for FR-001–FR-003/FR-006/FR-017–FR-022/FR-026–FR-033/FR-041/DR-012 in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftResource.java`
- [ ] T087 [P] [US1] Publish `specs/001-create-invoice-draft/contracts/invoice-draft-api.openapi.yaml` byte-for-byte as the non-independently-authored runtime contract, preserving OpenAPI 3.1, security absence, Company header-only input, and no 401/403 outcomes for FR-002/FR-039/SC-024 in `src/main/resources/META-INF/openapi.yaml`
- [ ] T088 [P] [US1] Implement process-only liveness that remains independent of PostgreSQL and every excluded dependency for Constitution XIII/FR-036/SC-023 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftLivenessCheck.java`
- [ ] T089 [P] [US1] Implement bounded read-only readiness against the same PostgreSQL datasource plus required Flyway/catalog initialization and no other dependency for Constitution XIII/FR-036/SC-023 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftReadinessCheck.java`
- [ ] T090 [P] [US1] Implement bounded metrics, safe structured audit/log/trace context, monotonic durations, and new/replay/conflict/rollback outcomes without sensitive or high-cardinality labels for FR-025–FR-026/SC-002/SC-012/SC-023/SC-033 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftTelemetry.java`

**Checkpoint**: User Story 1 independently satisfies all 58 acceptance scenarios, all 33 success
criteria, and every applicable constitutional evidence category on the mandatory JVM runtime.

---

## Final Phase: Definition of Done and Cross-Cutting Validation

**Purpose**: Validate the completed bounded feature without hiding unfinished story work here.

- [ ] T091 Re-run empty-database Flyway, exact seed, prohibited-structure, constraint, and repeatability evidence against PostgreSQL 18.4 using `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftMigrationTest.java`
- [ ] T092 Run formatting, static analysis, architecture, domain, application, API, PostgreSQL, concurrency, rollback, sensitive-data, health, observability, and performance suites configured in `build.gradle.kts`
- [ ] T093 Run packaged JVM boot and all critical create/replay/conflict/failure/date/correlation/monetary smoke paths from `src/test/java/com/alexastudillo/taxdocument/runtime/InvoiceDraftJvmSmokeTest.java`
- [ ] T094 Validate and record all approved end-to-end, recovery, boundary, negative-architecture, and dynamic America/Guayaquil date scenarios in `specs/001-create-invoice-draft/quickstart.md`
- [ ] T095 Record the exact JVM/PostgreSQL measurement environment, warm-up, samples, percentiles, resource use, blocked-thread result, and pool recovery evidence in `specs/001-create-invoice-draft/operational-requirements.md`
- [ ] T096 Record mandatory JVM evidence and either native build-plus-runtime evidence or an evidence-based native deferral without weakening JVM support in `specs/001-create-invoice-draft/plan.md`
- [ ] T097 Record the final Constitution v2.0.0 and Definition of Done review, including Company-header/scoping, no-identity, no-Company-dependency/master-data/snapshot, no-fiscal-side-effect, migration, sensitive-data, and runtime evidence in `specs/001-create-invoice-draft/plan.md`
- [ ] T098 Reconcile final FR-001–FR-047, DR-001–DR-024, SC-001–SC-033, AS-001–AS-058, stable-error, and prohibited-boundary evidence in `specs/001-create-invoice-draft/traceability.md`
- [ ] T099 Verify approved English terminology remains complete and update only affected classifications in `docs/migration/terminology-mapping.md`

---

## Dependencies and Execution Order

### Workflow Dependency Graph

```text
Constitution v2.0.0 on main
  → approved spec and reconciled plan
  → complete requirements-quality checklists
  → Phase 1 Setup
  → Phase 2 Foundational Persistence and Architecture Controls
  → Phase 3 US1 Create and Review an Invoice Draft
  → Final Definition of Done Validation
  → $speckit-analyze
  → $speckit-implement only after analysis clears every critical finding
```

### Phase Dependencies

- Phase 1 has no feature-task dependency and must finish before foundational work.
- Phase 2 depends on Phase 1. T010 and T011 depend on T008; T013–T016 implement the tests written
  in T010–T011 and run sequentially by migration version.
- US1 depends on Phase 2. T017 and T018 establish golden fixtures; T019–T041 may then be authored in
  parallel because they change separate test files.
- Domain production starts only after its evidence tasks exist. T042–T047 may run in parallel;
  T048–T051 then build line, tax-total, calculation, and aggregate behavior in dependency order.
- Application tasks depend on T051. T052–T053 and T055–T058/T060 may run in parallel; T054/T059
  follow their direct types, and T061 integrates the completed application boundaries.
- T062–T065 depend on the foundational migrations and may run in parallel. T066–T071 depend on
  T065 and may then run in parallel. T072 precedes T074. T073 and T074 may run in parallel after
  their models, ports, and mappings exist. T075–T076 depend only on their application ports.
- T077–T083 may run in parallel after the application contract is stable. T084–T086 run in order;
  T086 also depends on the T075 clock adapter so it can perform the one authorized request-time read.
  T087–T090 may run in parallel after their configuration and application dependencies exist.
- The Final Phase depends on all US1 tasks and runs before `$speckit-analyze`.

### Parallel Execution Example for User Story 1

After Phase 2 and golden fixtures T017–T018:

```text
Evidence batch A: T019 T020 T021 T022 T023 T024 T025 T026 T027
Evidence batch B: T028 T029 T030 T031 T032
Evidence batch C: T033 T034 T035 T036 T037 T038 T039 T040 T041
```

After the evidence tasks are present:

```text
Domain batch:         T042 T043 T044 T045 T046 T047
Application ports:    T052 T053 T055 T056 T057 T058 T060
Reference entities:   T062 T063 T064 T065
Aggregate children:   T066 T067 T068 T069 T070 T071
API foundations:      T077 T078 T079 T080 T081 T082 T083
Operational adapters: T087 T088 T089 T090
```

Tasks listed in the same batch modify independent files but still require the earlier dependency
batch to be complete.

## Implementation Strategy

### MVP First

1. Complete Phase 1 and prove the approved Quarkus/Java build baseline.
2. Complete Phase 2 and prove the exact schema/reference baseline from empty PostgreSQL.
3. Write all US1 evidence tasks before their production tasks.
4. Complete the US1 domain, application, infrastructure, API, and operational work in dependency
   order.
5. Validate US1 independently; it is the complete MVP and the only story in this bounded feature.
6. Complete Definition of Done evidence, then run `$speckit-analyze` before any implementation
   command is authorized.

### Incremental Delivery

The single story may be developed in evidence-first vertical slices—contract/context, reference
validation/calculation, atomic persistence/idempotency, and operations—but no slice is releasable
until the complete create/review outcome remains atomic, Company-scoped, replay-safe, observable,
and free of excluded fiscal effects.

## Notes

- Historical files under `docs/legacy/` are evidence only and MUST NOT be edited.
- CompanyId is accepted only through `X-Company-Id`, mapped to application/domain ownership, and
  used for persistence and idempotency partitioning; it is not a credential or entitlement proof.
- The service owns no identity behavior, Company master data, Company integration, shared Company
  persistence, Company snapshot, application cache, or fiscal-issuance capability.
- Panache models remain infrastructure-only; API DTOs, application types, domain types, and
  persistence models are mapped explicitly.
- The binding stores only hashes/version/draft association; it stores no raw key, correlation, or
  complete normalized buyer request.
- The approved reference identifiers are immutable Flyway seed data and are never generated at
  runtime.
- A task MUST NOT be marked complete merely to unblock the workflow, and coverage-only tests are
  prohibited.
