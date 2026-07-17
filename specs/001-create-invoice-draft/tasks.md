---

description: "Dependency-ordered implementation tasks for Create Invoice Draft"
---

# Tasks: Create Invoice Draft

**Input**: Approved design documents from `specs/001-create-invoice-draft/`

**Prerequisites**: Approved `specs/001-create-invoice-draft/spec.md`; reconciled
`specs/001-create-invoice-draft/plan.md`; complete
`specs/001-create-invoice-draft/research.md`; approved
`specs/001-create-invoice-draft/reference-data-baseline.md`;
`specs/001-create-invoice-draft/data-model.md`;
`specs/001-create-invoice-draft/persistence-design.md`;
`specs/001-create-invoice-draft/idempotency-design.md`;
`specs/001-create-invoice-draft/error-catalog.md`;
`specs/001-create-invoice-draft/operational-requirements.md`;
`specs/001-create-invoice-draft/traceability.md`;
`specs/001-create-invoice-draft/quickstart.md`; validated
`specs/001-create-invoice-draft/contracts/invoice-draft-api.openapi.yaml`; and completed
requirements-quality checklists. The formally approved revised corrective assignment is recorded
in `specs/001-create-invoice-draft/governance-corrective-assignment-addendum.md`.

**Analysis Gate**: The required post-generation, pre-implementation `$speckit-analyze` gate was
violated for T001–T016. The completed evidence-based review is recorded in
`governance-retrospective-review.md`; `astudilloalex` approved its mandatory corrective disposition
in both required capacities in `governance-owner-approval.md`. `GATE-GOV-001` is released. The next
mandatory workflow step is a new `$speckit-analyze` after the approved corrective-assignment
addendum and artifact reconciliation. Current implementation permission is
`PENDING_SUCCESSFUL_ANALYSIS`; T017 remains pending, T018 depends on completed T017, and T019
remains blocked until both complete successfully.

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
- T001–T016 were completed before the mandatory analysis gate and were subsequently reviewed
  through the approved retrospective governance process. D1 remains a historical process
  non-conformity. D2 and D3 are addressed through mandatory corrective tasks T017 and T018.

**Task-ID renumbering record**: T001–T016 are unchanged. New corrective tasks occupy T017 and T018.
Every formerly pending identifier T017–T099 maps bijectively to T019–T101 by adding two; no former
pending task was removed, combined, or marked complete.

## Phase 1: Setup

**Purpose**: Establish the approved Java 25, Quarkus 3.33.2.1 LTS, Clean Architecture, and test
tooling baseline before feature dependencies or source implementation.

- [X] T001 Align both Quarkus plugin and platform versions to 3.33.2.1 LTS per the approved runtime decision in `gradle.properties`
- [X] T002 Configure exactly the Quarkus-BOM-managed `quarkus-rest-jackson`, `quarkus-hibernate-validator`, `quarkus-hibernate-reactive-panache`, `quarkus-reactive-pg-client`, `quarkus-flyway`, `quarkus-jdbc-postgresql`, `quarkus-smallrye-openapi`, `quarkus-smallrye-health`, `quarkus-micrometer`, `quarkus-opentelemetry`, `quarkus-junit`, `quarkus-test-vertx`, `quarkus-test-hibernate-reactive-panache`, and `io.rest-assured:rest-assured` dependencies; Spotless `8.8.0` with google-java-format `1.35.0`; and JDK 25 `-Xlint:all -Werror` in `build.gradle.kts`
- [X] T003 [P] Establish the API capability boundary documented by the plan in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/package-info.java`
- [X] T004 [P] Establish the application capability boundary documented by the plan in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/package-info.java`
- [X] T005 [P] Establish the framework-free domain capability boundary documented by the plan in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/package-info.java`
- [X] T006 [P] Establish the infrastructure capability boundary documented by the plan in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/package-info.java`
- [X] T007 Configure sanitized Quarkus Database Dev Services with `docker.io/library/postgres:18.4`, Flyway, bounded persistence timeouts, and observability for tests in `src/test/resources/application.properties`; do not add clock or identifier test switches to configuration

**Checkpoint**: The build and four production boundaries match the approved plan without adding
identity, Company, cache, broker, or fiscal-issuance capabilities.

---

## Phase 2: Foundational Persistence and Architecture Controls

**Purpose**: Create the test and persistence foundations that block User Story 1 production work.

**CRITICAL**: User Story 1 production tasks MUST NOT begin until this phase is complete.

- [X] T008 Create reusable Quarkus Database Dev Services context, schema-reset, and row-count support for PostgreSQL migration, transaction, timeout, and concurrency evidence without declaring or starting a second Testcontainers lifecycle in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/PostgreSqlTestResource.java`
- [X] T009 [P] Add Clean Architecture and prohibited-dependency assertions for Constitution IV, VII, XV, and XVI in `src/test/java/com/alexastudillo/taxdocument/architecture/CleanArchitectureTest.java`
- [X] T010 [P] Add empty-database Flyway, exact locale-independent ASCII product/buyer constraints, aggregate/binding Company enforcement, global reference-catalog no-Company-column checks, prohibited-structure, and repeatability tests for FR-007/FR-010/FR-020–FR-024/FR-030/FR-036–FR-039/DR-014/SC-002/SC-017/SC-022–SC-024 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftMigrationTest.java`; retrospective review found that the historical test omits executable product/buyer ASCII vectors, so pending T017 records the failing pre-V5 evidence and pending T018 supplies the corrective V5 implementation
- [X] T011 [P] Add exact 5-buyer/6-IVA/8-payment row-count, evidence metadata, UUIDv5 recalculation, validity-overlap, and no-runtime-generation tests for FR-045–FR-047/DR-001/SC-031–SC-032 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/ReferenceDataBaselineTest.java`
- [X] T012 Configure Flyway-only schema evolution, non-mutating Hibernate validation, reactive and migration datasources, bounded pool/query timeouts, the five-second write-transaction ceiling, the exact `invoice-draft.request-deadline=PT10S` duration consumed by the API deadline owner, exact bare-byte `quarkus.http.limits.max-body-size=2097152`, `quarkus.jackson.fail-on-unknown-properties=true`, `quarkus.rest.exception-mapping.disable-mapper-for=io.quarkus.resteasy.reactive.jackson.runtime.mappers.BuiltinMismatchedInputExceptionMapper`, `mp.openapi.scan.disable=true`, and PostgreSQL-only readiness in `src/main/resources/application.properties`; do not treat a Quarkus connection or idle timeout as the end-to-end request deadline
- [X] T013 Create the exact globally governed, non-Company-owned versioned buyer-identification, IVA-rule, and payment-method catalog structures—with no Company column—plus evidence metadata, validity boundaries, and stable-identifier columns from the approved model for FR-045/FR-046/FR-047/SC-031/SC-032 in `src/main/resources/db/migration/V1__create_invoice_draft_reference_catalogs.sql`
- [X] T014 Seed exactly the approved SRI-OFFLINE-2.32-TARGET-1 rows and fixed UUIDs, with verification queries proving official evidence, exact target mappings, no unsupported rows, and UUIDv5 stability for FR-045/FR-046/FR-047/SC-031/SC-032 in `src/main/resources/db/migration/V2__seed_invoice_draft_reference_data.sql`
- [X] T015 Create the Invoice Draft root, line, line-tax, grouped-tax, payment, and additional-information structures and constraints, including locale-independent ASCII `product_code` and type-conditional buyer-identification barriers matching `^[A-Za-z0-9]{1,25}$` and `^[A-Za-z0-9]{1,20}$` where applicable, for FR-007/FR-010/FR-020–FR-024/FR-037–FR-038/DR-014/DR-020–DR-023 in `src/main/resources/db/migration/V3__create_invoice_draft_aggregate.sql`; retrospective review found the affected constraints non-conforming, V3 remains immutable, T017 records the mismatch, and T018 replaces only those barriers through a new V5 migration
- [X] T016 Create the local Company-scoped binding with `UNIQUE (company_id, idempotency_key_hash)`, 32-byte hashes, normalization version, unique draft, and composite Company/draft foreign key for FR-027–FR-034 in `src/main/resources/db/migration/V4__create_invoice_draft_idempotency.sql`

**Checkpoint**: Historical V1–V4 create the local schema and approved reference baseline with no
Company master data, identity state, fiscal snapshot, or cross-service relation. Exact approved
ASCII integrity remains pending in T017–T018 and is not represented as complete.

---

## Phase 3: User Story 1 - Create and Review an Invoice Draft (Priority: P1) MVP

**Goal**: Allow an internal billing client to create and immediately review one complete,
Company-scoped USD Invoice Draft while preserving deterministic calculation, atomic persistence,
durable idempotency, safe errors, and the explicit pre-issuance boundary.

**Independent Test**: Submit `POST /api/v1/invoice-drafts` with exactly one valid
`X-Company-Id`, one valid `Idempotency-Key`, approved catalog identifiers, a current Ecuadorian
emission date, valid buyer/line inputs, and payment amounts reconciling to the service-calculated
grand total. Exactly one complete `DRAFT` is committed and returned; equivalent replay returns it;
conflicting or invalid requests create no partial state; and no excluded Company, identity, or
fiscal side effect occurs.

### Required Evidence for User Story 1

### GATE-GOV-001 — Retrospective Analyze-Gate Remediation

**Status**: **RELEASED — APPROVED WITH MANDATORY CORRECTIVE ACTIONS; IMPLEMENTATION PERMISSION
PENDING SUCCESSFUL ANALYSIS.**

**Approver**: `astudilloalex`

**Released at**: `2026-07-16T03:35:25Z`

The governance release accepts D1 as historical and D2/D3 only with T017/T018 correction. The
approved `governance-corrective-assignment-addendum.md` prospectively assigns red evidence to T017
and V5/green persistence evidence to T018 without changing D1–D3 or the retrospective review/hash.
The addendum does not itself authorize implementation: T017 may start only after a new
`$speckit-analyze` removes the current CRITICAL assignment finding. The release does not authorize
skipping T017/T018, starting T019, marking corrective work complete, or executing later business
implementation prematurely. Failure of T017 or T018 blocks all later work.

### Mandatory Corrective Database Evidence

- [ ] T017 [US1] Define the authoritative shared buyer-identification and product-code fixture in `src/test/resources/invoicedraft/ascii-validation-vectors.json`, with each entry recording field, literal value, expected validity, applicable minimum/maximum boundary, expected stable error code when invalid, rationale, and consuming layers. Update or prepare `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftMigrationTest.java` and `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/ReferenceDataBaselineTest.java` to validate fixture parsing/integrity and create intentional failing pre-V5 PostgreSQL/Flyway assertions exposing the known V3 mismatch while unrelated behavior stays stable. Cover minimum/maximum lengths, uppercase/lowercase ASCII letters, digits, the explicitly empty approved-punctuation set, rejected punctuation, ASCII/Unicode whitespace, empty values, accented Unicode letters, locale-sensitive characters, and below/above-boundary violations for `^[A-Za-z0-9]{1,20}$` buyer values and `^[A-Za-z0-9]{1,25}$` product codes. Standalone Java `Pattern` checks MAY verify only fixture parsing or the approved literal regex. T017 MUST NOT create or modify V5, modify V3, invoke/test future production Java validators, claim productive Java equivalence, or depend on T045/T050 for FR-007/FR-010/FR-020/DR-014/SC-002/SC-010/SC-016/SC-022
- [ ] T018 [US1] Depends explicitly on completed T017. Create `src/main/resources/db/migration/V5__tighten_invoice_draft_ascii_constraints.sql` without editing V3; use T017's red evidence to inspect and replace only V3's affected buyer-identification and product-code constraints with the approved explicit ASCII expressions; make every T017 PostgreSQL/Flyway assertion pass; validate empty-database and V3-to-V5 upgrade paths plus Flyway validation; update five-migration inventory/final-constraint assertions in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftMigrationTest.java` and affected reference assertions in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/ReferenceDataBaselineTest.java`; and prove final constraint definitions contain no `[[:alnum:]]`, locale-dependent POSIX class, `\w`, Unicode shorthand, or equivalent ambiguous expression. T018 establishes PostgreSQL/Flyway green behavior from `src/test/resources/invoicedraft/ascii-validation-vectors.json` only and MUST NOT invoke or claim equivalence with production Java validators for FR-007/FR-010/FR-020/DR-014/SC-002/SC-010/SC-016/SC-022

**Cross-layer equivalence rule**: Independent layer-specific suites consume
`src/test/resources/invoicedraft/ascii-validation-vectors.json` and assert the same stored expected
outcome: T017/T018 own PostgreSQL/Flyway, T030 owns OpenAPI, T045 owns production
buyer-identification Java validation, and T050 owns production product-code/text-rule Java
validation. No domain test directly depends on PostgreSQL, Flyway, OpenAPI parser, or HTTP transport
infrastructure.

- [ ] T019 [P] [US1] Depends explicitly on released `GATE-GOV-001`, completed T017, and completed T018. Add exact HALF_UP, percentage, zero-treatment, numeric-boundary, grouped-overflow, Stage 11A calculations, and deterministic Stage 11B validation-precedence/payment-sum golden vectors for FR-010–FR-016/FR-041/FR-044/DR-002–DR-011/SC-003–SC-004/SC-026/SC-029 in `src/test/resources/invoicedraft/calculation-vectors.json`
- [ ] T020 [P] [US1] Add normalization-version-1 key-hash and fingerprint golden vectors covering UUID; NFC-composed/decomposed general text; U+0020 trim/collapse; prohibited Unicode categories and separators; post-normalization display length; case preservation; persisted `canonicalName`; post-`Locale.ROOT` canonical length; accepted 150-`U+0130` expansion at 300 code points; rejected 151-`U+0130` expansion with `CANONICAL_NAME_TOO_LONG`; no truncation; decimal, null/empty, property order, line order, payment order, additional-information order, Company, key, and correlation rules for FR-027–FR-035/DR-017–DR-021/SC-012/SC-016/SC-018–SC-019/SC-030/SC-033 in `src/test/resources/invoicedraft/idempotency-v1-vectors.json`
- [ ] T021 [P] [US1] Add pure BigDecimal tests using T019 that prove Stage 11A calculates exact gross, rounded discount/net/tax/line totals, aggregates, invoice taxes, invoice total, and payment-reconciliation reference total, then Stage 11B selects the first violation in this exact order: calculated range/overflow by line then canonical tax-group then aggregate, discount-over-gross by lowest line position, final-consumer total limit, total-dependent payment shape/positivity, exact payment-sum reconciliation; cover zero values, repetition, and no implementation-dependent ordering for FR-010–FR-016/FR-041/FR-044/DR-002–DR-011/DR-013/DR-015–DR-016/SC-003–SC-004/SC-011/SC-026/SC-029 in `src/test/java/com/alexastudillo/taxdocument/domain/invoicedraft/InvoiceDraftCalculatorTest.java`
- [ ] T022 [P] [US1] Consume T017's authoritative buyer vectors as expected domain outcomes in `src/test/java/com/alexastudillo/taxdocument/domain/invoicedraft/BuyerTest.java`; add all five approved buyer strategies, Stage-10 required/type/syntax checks, and exact case-sensitive ASCII `^[A-Za-z0-9]{1,20}$` passport/foreign-identification assertions over Application-normalized values. Keep this suite independent of PostgreSQL, Flyway, OpenAPI parser, and HTTP infrastructure; T045 later supplies the production Java implementation evidence. Separately prove the final-consumer USD 50.00 check occurs only in Stage 11B after Stage 11A supplies the calculated invoice total, with unsupported-type rejection and no checksum/registry lookup for FR-007/FR-028/FR-041/DR-001/DR-013–DR-015/SC-010/SC-016/SC-026
- [ ] T023 [P] [US1] Add one active/effective immutable `family=IVA` rule, wrong-family/inactive/inapplicable rejection, four-treatment, separate-zero-group, caller-selection, non-IVA, multiple-tax, no-parent-category, and no-classification tests for FR-010–FR-012/DR-001/DR-005/DR-008/DR-013/SC-009/SC-021 in `src/test/java/com/alexastudillo/taxdocument/domain/invoicedraft/InvoiceDraftTaxTest.java`
- [ ] T024 [P] [US1] Add domain-only tests over already normalized and validated payment-method references for positive/zero totals, inconsistent-total rejection, exact reconciliation, duplicate method, exact 1/8 acceptance and 9 rejection, and overflow. Prove total-dependent shape/positivity and reconciliation consume Stage 11A totals only in their exact Stage 11B positions; keep all transport-adapter concerns outside this suite for FR-013–FR-014/FR-041/DR-013/DR-016/DR-022/SC-011/SC-015/SC-020/SC-026/SC-029 in `src/test/java/com/alexastudillo/taxdocument/domain/invoicedraft/PaymentReconciliationTest.java`
- [ ] T025 [P] [US1] Add aggregate status, USD, local identifier, immutable Company ownership, opaque emission point, valid child membership, invalid aggregate-relationship rejection, calculated-input exclusion, and no-snapshot tests for FR-004–FR-005/FR-017–FR-024/FR-037–FR-038/DR-013/DR-020/DR-023/SC-001/SC-005/SC-007/SC-017/SC-022 in `src/test/java/com/alexastudillo/taxdocument/domain/invoicedraft/InvoiceDraftTest.java`
- [ ] T026 [P] [US1] Add domain-only tests over already Application-normalized values for Unicode-code-point length, nonblank text, case-sensitive display preservation, email, telephone, collection limits, persisted `canonicalName`, and exact ASCII product-code `^[A-Za-z0-9]{1,25}$`; consume T017's authoritative product vectors as expected domain outcomes while remaining independent of PostgreSQL, Flyway, OpenAPI parser, and HTTP infrastructure. T050 later supplies the production Java implementation evidence. Use general-text vectors proving NFC accented names and emoji (`So`) accepted within field limits, decomposed/composed equivalence supplied as NFC, leading/trailing U+0020 removed, internal U+0020 preserved for display, and tab/newline/NBSP/U+2028/U+2029/zero-width `Cf` rejected before domain entry; Domain MUST NOT normalize, trim, collapse, lowercase, or derive `canonicalName` for FR-008–FR-010/FR-015/FR-035/DR-019/SC-015–SC-016 in `src/test/java/com/alexastudillo/taxdocument/domain/invoicedraft/DraftTextRulesTest.java`
- [ ] T027 [P] [US1] Add one-instant America/Guayaquil request-date, impossible/different-date, midnight, rollback non-exposure, immutable replay, and later-date replay tests; prove T063/API/Domain/mappers never invoke the persistence clock or generate/supply/replace `createdAt` or `updatedAt`; prove initial `updatedAt = createdAt`; and prove replay returns both originally persisted values unchanged without a clock invocation, while T036 repository-adapter evidence owns the single T076 transactional invocation through a resettable test-only fixed clock for FR-006/FR-019/FR-033/DR-012–DR-013/SC-013/SC-030 in `src/test/java/com/alexastudillo/taxdocument/application/invoicedraft/RequestDatePolicyTest.java`
- [ ] T028 [P] [US1] Add golden key-hash/fingerprint, privacy-minimization, version, equivalence, ordering, cross-Company-scope, and correlation-exclusion tests using T020 for FR-027–FR-034/DR-017–DR-021/SC-012/SC-018–SC-019/SC-030/SC-033 in `src/test/java/com/alexastudillo/taxdocument/application/invoicedraft/IdempotencyFingerprintTest.java`
- [ ] T029 [P] [US1] Add transport-neutral application use-case tests beginning at FR-041 Stage 6 with raw decoded business values, mapped CompanyId, fixed API-captured request instant, and neutral T054 deadline context. Prove Application invokes `BusinessTextNormalizer` exactly once per supplied applicable value; rejects prohibited code points, invalid post-normalization display length, and post-`Locale.ROOT` canonical overflow including 151 `U+0130` with `CANONICAL_NAME_TOO_LONG`; accepts the 150-`U+0130` boundary; never truncates; constructs a timestamp-free `InvoiceDraftCandidate` after Stage 10/11A/11B and final local-ID allocation; passes it to T057; maps `PersistedInvoiceDraft` to the result; returns both original timestamps on replay without clock invocation or persisted-canonical rebuild; and preserves cooperative budget, zero-state, uncertain-commit, no-HTTP, and no-external-call invariants for FR-006–FR-021/FR-024–FR-038/FR-041/FR-043–FR-047/DR-012–DR-013/DR-019/SC-001–SC-002/SC-012/SC-016/SC-023/SC-026/SC-028 in `src/test/java/com/alexastudillo/taxdocument/application/invoicedraft/CreateInvoiceDraftUseCaseTest.java`
- [ ] T030 [P] [US1] Depends explicitly on T017. Independently consume `src/test/resources/invoicedraft/ascii-validation-vectors.json` in `src/test/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftOpenApiContractTest.java` and prove the approved OpenAPI product-code and buyer-identification patterns/bounds match every stored expected outcome without invoking PostgreSQL, Flyway, HTTP runtime behavior, or domain validators. Also add OpenAPI parse/reference, operation, mandatory exactly-one Idempotency-Key documentation/stable errors, strict request/input Company exclusion with contract-required response `companyId`, API-decodes/Application-normalizes general Unicode and `CANONICAL_NAME_TOO_LONG` expectations, payment `emissionDate` effectiveness, Stage 10/11A/11B and API-only deadline/HTTP ownership, decimal, T076-produced equal `createdAt`/`updatedAt`, replay preservation, response/error/security absence, `mp.openapi.scan.disable=true`, and byte/semantic equality tests between canonical `specs/001-create-invoice-draft/contracts/invoice-draft-api.openapi.yaml` and runtime publication `src/main/resources/META-INF/openapi.yaml` for FR-001–FR-005/FR-007/FR-010/FR-013/FR-019/FR-022/FR-025–FR-027/FR-035/FR-039/FR-041–FR-044/DR-014/SC-010/SC-016/SC-020/SC-024–SC-027/SC-032
- [ ] T031 [P] [US1] Add missing, blank, repeated, malformed, nil, mixed-case, canonicalization, no-path/query/request-body substitution, contract-required response `companyId`, and no-state Company-header tests for FR-001–FR-003/FR-024/FR-037/FR-040/SC-006/SC-017/SC-024 in `src/test/java/com/alexastudillo/taxdocument/api/invoicedraft/CompanyContextHeaderTest.java`
- [ ] T032 [P] [US1] Add absent, valid, blank, repeated, over-length, unsafe, non-echo, replacement, request-gate ordering, earlier-stage suppression, response-header, fingerprint-invariance, and oversized-body correlation tests proving a valid value is preserved, absent/invalid input receives a safe UUID, invalid input never changes selected `413` to `400`, and no database port is invoked for FR-026/FR-029/FR-041–FR-042/DR-024/SC-027/SC-033 in `src/test/java/com/alexastudillo/taxdocument/api/invoicedraft/CorrelationHeaderTest.java`
- [ ] T033 [P] [US1] Add API contract tests for exact Idempotency-Key transport errors/one-time SP/HTAB header normalization, payload-size and strict Company/input rules, exact product/buyer representation, and raw decodable Unicode vectors. Prove API only decodes JSON, rejects malformed representation, validates transport structure, and forwards business values unchanged; API MUST NOT perform NFC, trim business values, collapse spaces, lowercase, derive `canonicalName`, apply post-normalization/canonical lengths, or emit `CANONICAL_NAME_TOO_LONG`. Keep Company identifiers out of request bodies/input schemas and Stage-5 representation errors under API arbitration for FR-002/FR-005/FR-007–FR-015/FR-027/FR-035/FR-041–FR-044/DR-019/SC-012/SC-014–SC-016/SC-024–SC-027/SC-029/SC-033 in `src/test/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftRequestValidationTest.java`
- [ ] T034 [P] [US1] Add HTTP 201/200 create/replay and pairwise/multi-failure evidence proving the API adapter is the exclusive request-deadline racer, terminal-result arbiter, HTTP-status selector, Problem Details mapper, and second-response guard: race the application `Uni` against T054 without real sleeps; cover all FR-041 stages including Stage 11A and the exact Stage 11B order; verify new and replay responses copy T076's persisted `createdAt` and `updatedAt`, replay preserves both unchanged and invokes no persistence clock, and API generates neither; cover timeout-first versus payload/header/replay/conflict/validation/calculation/persistence outcomes, late-result discard, API-only 504/all HTTP mapping, pre-expiry result preservation, and telemetry-only post-commit expiry for FR-017–FR-026/FR-030–FR-033/FR-041–FR-043/SC-001–SC-008/SC-012/SC-026–SC-028/SC-033 in `src/test/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftResourceTest.java`
- [ ] T035 [P] [US1] Add real-PostgreSQL reference lookup tests with exact approved metadata and neutral adapter failures; for payment methods pass `(paymentMethodId, emissionDate)` and prove inclusive `effectiveFrom`, inclusive finite `effectiveTo`, before-start, after-end, open-ended end, inactive-but-temporally-effective, and active-but-temporally-ineffective outcomes, never using server current date, request arrival, transaction time, or `createdAt`. Verify `min(configured operation timeout, remaining Duration)`, no query when exhausted, and transport-neutral deadline/persistence results that the API later maps for FR-007/FR-011/FR-013/FR-041/FR-043/FR-045–FR-047/DR-001/DR-013/SC-002/SC-009–SC-010/SC-021/SC-026/SC-028/SC-031–SC-032 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/ReferenceDataRepositoryAdapterTest.java`
- [ ] T036 [P] [US1] Add real-PostgreSQL aggregate round-trip; authoritative Company enforcement for aggregate/binding and no Company scope on global catalogs; T018 V5 ASCII barriers; numeric/canonical-text constraints; timestamp-free `InvoiceDraftCandidate` handoff; committed `PersistedInvoiceDraft` result; and all-or-nothing transaction tests. Prove T076 is the sole transactional clock owner, invokes T059 exactly once after all validations and immediately before root persistence, assigns the same returned Instant to root `created_at`, root `updated_at`, and binding `created_at`, preserves candidate identifiers, exposes neither timestamp on rollback, never calls twice, and returns both originally persisted timestamps on replay without another call for FR-007/FR-010/FR-019–FR-024/FR-030/FR-037/FR-044/DR-012–DR-014/DR-019/SC-001–SC-002/SC-012/SC-013/SC-017/SC-022/SC-029 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftRepositoryAdapterTest.java`
- [ ] T037 [P] [US1] Add 50-way equivalent winner, conflicting contender, uniqueness-loser rollback/re-read, cross-Company independence, and no-expiry tests; every replay loads the winner's original identifier, `createdAt`, and `updatedAt` unchanged without clock invocation, canonical rebuild, or another aggregate for FR-027–FR-034/DR-017–DR-018/SC-012/SC-018 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftIdempotencyConcurrencyTest.java`
- [ ] T038 [P] [US1] Add injected confirmed pre-commit failure after every write phase plus unavailable and controlled query/write-timeout cases without real sleeps, asserting zero partial state and no exposed timestamps/key binding only when pre-commit or full rollback is confirmed; when neutral deadline context expires while commit is unresolved, allow eventual rollback or commit and expose only a typed uncertain outcome for API arbitration, then prove same-Company/key/content replay recovers the single authoritative draft with its original identifier, `createdAt`, and `updatedAt` and no replay clock call; simulate post-commit response loss with no compensation and assert no infrastructure HTTP status/envelope behavior for FR-020–FR-021/FR-032/FR-041/FR-043/DR-013/SC-002/SC-012/SC-026/SC-028 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftRollbackTest.java`
- [ ] T039 [P] [US1] Add liveness/readiness separation, same-datasource, migration/catalog readiness, PostgreSQL outage/recovery, and no unrelated dependency checks for Constitution XIII/FR-036/SC-023 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftHealthTest.java`
- [ ] T040 [P] [US1] Add safe-error, structured-log, metric-label, trace-attribute, raw-key, normalized-request, buyer-data, SQL, invalid-correlation, fingerprint-storage, and `request_deadline_exceeded_after_response_commit` event exposure tests, proving the late-deadline event contains only approved safe fields and no buyer PII, request body, raw idempotency key, or token for FR-025–FR-026/FR-029/FR-043/SC-002/SC-023/SC-028/SC-033 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/SensitiveDataExposureTest.java`
- [ ] T041 [P] [US1] Add warmed typical; maximum valid 500-line/8-payment/15-additional request with body at most 2 MiB; replay; conflict; 50-way concurrency; earliest-boundary 10-second deadline across body, application, persistence, and serialization; aggregate/reference remaining-budget clamping; post-response-commit telemetry-only expiry; timer cancellation; pool recovery; and blocked-event-loop performance evidence for FR-041/FR-043/SC-012/SC-015/SC-026–SC-028 and `specs/001-create-invoice-draft/operational-requirements.md` in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftPerformanceTest.java`
- [ ] T042 [P] [US1] Add packaged JVM boot, migration, served `/q/openapi` resolution and semantic equality/security-absence checks, health, Stage-6 Application normalization/canonical overflow, timestamp-free candidate, create, replay with original identifier and both timestamps/no replay clock, conflict, correlation, rollback, timeout, date, and monetary smoke evidence for Constitution III/XII and SC-001–SC-033 in `src/test/java/com/alexastudillo/taxdocument/runtime/InvoiceDraftJvmSmokeTest.java`
- [ ] T043 [P] [US1] Add boundary evidence proving zero identity/security processing, Company lookup/dependency/master data/snapshot/cache, and fiscal-issuance adapter or side effect for FR-003/FR-023/FR-033/FR-036/FR-038–FR-040/SC-005/SC-008/SC-023–SC-025 in `src/test/java/com/alexastudillo/taxdocument/architecture/InvoiceDraftBoundaryTest.java`

### Domain Implementation for User Story 1

- [ ] T044 [P] [US1] Implement the canonical non-nil immutable Company ownership value for FR-002/FR-024/FR-037/DR-018/DR-020 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/CompanyId.java`
- [ ] T045 [P] [US1] Depends explicitly on T017, T018, and T022. Implement the production buyer validator for approved buyer type, exact case-sensitive ASCII `^[A-Za-z0-9]{1,20}$` passport/foreign-identification values received after Application normalization, numeric format-only, contact, and final-consumer invariants in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/Buyer.java`; make `src/test/java/com/alexastudillo/taxdocument/domain/invoicedraft/BuyerTest.java` independently consume `src/test/resources/invoicedraft/ascii-validation-vectors.json`, exercise the actual production buyer-identification Java validator, and compare every applicable result with the fixture's expected outcome. This suite MUST NOT invoke PostgreSQL, Flyway, OpenAPI parser, or HTTP infrastructure; cross-layer equivalence is established collectively with the independent T017/T018 and T030 suites for FR-007–FR-008/FR-028/DR-014–DR-015/SC-010/SC-016
- [ ] T046 [P] [US1] Implement the versioned IVA-only selected rule and four treatment invariants for FR-010–FR-012/DR-001/DR-005/DR-008 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/TaxSelection.java`
- [ ] T047 [P] [US1] Implement positive/zero amount, method identity, and reconciliation inputs for FR-013–FR-014/DR-016/DR-022 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/Payment.java`
- [ ] T048 [P] [US1] Implement domain invariants over already NFC-normalized additional-information display values and an already supplied persisted `canonicalName`; enforce Unicode-code-point lengths and require the canonical value produced exactly once by the application policy—NFC, trim U+0020, collapse U+0020 runs, lowercase with `Locale.ROOT`—without database-locale recomputation for FR-015/FR-035/DR-019/DR-021 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/AdditionalInformation.java`
- [ ] T049 [P] [US1] Implement stable domain validation violations without framework or transport dependencies for FR-025/FR-044 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/DraftValidationException.java`
- [ ] T050 [US1] Depends explicitly on T017, T018, and T026. Implement the production line validator for ordered inputs, exact case-sensitive ASCII `^[A-Za-z0-9]{1,25}$` product codes received after Application normalization, one tax selection, normalized description, basic quantity/unit-price/discount scale/sign/range inputs, and storage of Stage 11A calculated amounts in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/InvoiceLine.java`; make `src/test/java/com/alexastudillo/taxdocument/domain/invoicedraft/DraftTextRulesTest.java` independently consume `src/test/resources/invoicedraft/ascii-validation-vectors.json`, exercise the actual production product-code/text-rule Java validators, and compare every applicable result with the fixture's expected outcome. This suite MUST NOT invoke PostgreSQL, Flyway, OpenAPI parser, or HTTP infrastructure; cross-layer equivalence is established collectively with the independent T017/T018 and T030 suites. Do not perform discount-over-calculated-gross validation here before Stage 11A; T052 owns that ordered Stage 11B check for FR-009–FR-012/FR-041/DR-002–DR-005/DR-010/SC-016
- [ ] T051 [US1] Implement versioned treatment/rate grouping with distinct zero-tax treatments for FR-012/DR-005/DR-008 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/TaxTotal.java`
- [ ] T052 [US1] Implement Stage 11A deterministic HALF_UP line gross/discount/net/tax/line-total, subtotal, grouped tax, invoice-total, and payment-reference calculation, followed by Stage 11B validation in the exact order defined by FR-041: calculated range/overflow, discount-over-gross by lowest line position, final-consumer total limit, total-dependent payment shape/positivity, then exact payment reconciliation; emit deterministic ordered domain violations for FR-012–FR-016/FR-041/FR-044/DR-002–DR-011/DR-013/DR-015–DR-016 in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/InvoiceDraftCalculator.java`
- [ ] T053 [US1] Implement the validated/calculated USD DRAFT business aggregate with immutable CompanyId, opaque emission point, buyer, ordered local child relationships, taxes, payments, additional information, totals, invalid-relationship rejection, and no fiscal snapshot in `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/InvoiceDraft.java`; aggregate construction MUST NOT require, generate, default, or fabricate `createdAt` or `updatedAt`, because creation timestamps exist only in T055's persisted result after T076 commits for FR-004–FR-024/FR-037–FR-038/DR-012–DR-013/DR-020/DR-023

### Application Implementation for User Story 1

- [ ] T054 [P] [US1] Define immutable transport-neutral monotonic start/expiry/remaining-budget context and controllable time source in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/RequestDeadline.java`; define `CreateInvoiceDraftCommand` in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/CreateInvoiceDraftCommand.java` with mapped CompanyId, API-captured request instant, neutral deadline/idempotency/correlation evidence, and raw decoded commercial text; and define Application-owned `BusinessTextNormalizer` in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/BusinessTextNormalizer.java`. At FR-041 Stage 6 it MUST be invoked exactly once per supplied applicable value to perform NFC, permitted trim, prohibited-code-point/whitespace checks, post-normalization display length, canonical-space collapse/`Locale.ROOT` lowercase, post-canonicalization 1–300-code-point validation, and `CANONICAL_NAME_TOO_LONG` without truncation. It contains no HTTP, terminal arbitration, timer, response-commit, persistence clock, or timestamp-generation behavior for FR-004–FR-015/FR-026–FR-029/FR-035/FR-040–FR-043/DR-019
- [ ] T055 [P] [US1] Define timestamp-free `InvoiceDraftCandidate` with final Application-allocated root/child identifiers, normalized/validated/calculated business values, authoritative CompanyId, and safe binding inputs in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/InvoiceDraftCandidate.java`; define committed `PersistedInvoiceDraft` with the final identifier, response-relevant persisted values, `createdAt`, and `updatedAt` in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/PersistedInvoiceDraft.java`; and define the new-or-replayed application result that copies both persisted timestamps without generating them in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/CreateInvoiceDraftResult.java`. Candidate timestamps—null, zero, placeholder, provisional, or fabricated—are prohibited, and replay retains both original values for FR-017–FR-022/FR-033/SC-001/SC-007/SC-012/SC-018
- [ ] T056 [US1] Define the Mutiny input port for the synchronous observable create operation in FR-020/FR-022/FR-040 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/CreateInvoiceDraftUseCase.java`
- [ ] T057 [P] [US1] Depends explicitly on T055. Define the transport-neutral local repository boundary with conceptual creation contract `persist(InvoiceDraftCandidate) -> Uni<PersistedInvoiceDraft>` in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/InvoiceDraftRepository.java`; it accepts no timestamped candidate, placeholder timestamp, HTTP type, persistence entity, or commit metadata and returns no HTTP error/entity. Every aggregate/binding query or mutation enforces authoritative CompanyId—including create, draft lookup, duplicate/idempotency lookup, binding create/read, persistence mutation, and future feature operations—while global SRI catalogs remain outside this port and unscoped by Company; approved neutral remaining-budget handling applies to asynchronous operations for FR-020–FR-024/FR-030–FR-034/FR-043
- [ ] T058 [P] [US1] Define the minimal transport-independent buyer/IVA/payment reference lookup boundary whose every asynchronous invocation receives an explicit remaining `Duration`; payment-method lookup receives exactly `(paymentMethodId, emissionDate)` and applies inclusive `effectiveFrom <= emissionDate` and `(effectiveTo == null || emissionDate <= effectiveTo)`, never current server/request/transaction/createdAt time. Return only typed neutral results/failures with no HTTP or Quarkus type for FR-007/FR-011/FR-013/FR-041/FR-043/FR-045–FR-047/DR-001/DR-013 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/ReferenceDataPort.java`
- [ ] T059 [P] [US1] Define an injectable clock port with separate request-time and persistence-time `java.time.Instant` operations: API alone calls request time once after FR-041 stages 1–5; T076 alone calls persistence time once inside the active transaction after all validations and immediately before root persistence and assigns that one Instant to both `createdAt` and `updatedAt`. The port supplies no physical-commit timestamp and performs no post-commit query or reconstruction for FR-006/FR-019/DR-012 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/RequestClock.java`
- [ ] T060 [P] [US1] Define the local non-fiscal draft and child identifier boundary for FR-018/FR-023 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/DraftIdentifierGenerator.java`
- [ ] T061 [US1] Implement version-1 domain-separated key hashing and privacy-minimal fingerprinting over the once-normalized NFC display values and persisted additional-information `canonicalName`; composed/decomposed equivalents fingerprint identically, display case remains significant except where the specified canonical name is used, and prohibited input never reaches fingerprinting for FR-027–FR-035/DR-017–DR-021 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/IdempotencyFingerprint.java`
- [ ] T062 [P] [US1] Define stable application failures, retry classification, and safe violation metadata aligned to FR-025/FR-041–FR-044 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/InvoiceDraftFailure.java`
- [ ] T063 [US1] Depends on T053–T062. Implement transport-neutral FR-041 stages 6–12 in `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/CreateInvoiceDraftService.java`: invoke T054 `BusinessTextNormalizer` exactly once per supplied applicable value; reject prohibited code points, post-normalization display overflow, and post-canonicalization overflow with `CANONICAL_NAME_TOO_LONG` before fingerprint/Domain/persistence and never truncate; perform Company-scoped binding lookup, Stage 10, Stage 11A, exact Stage 11B, and T060 final local-ID allocation; construct T055's timestamp-free `InvoiceDraftCandidate`; call T057; map `PersistedInvoiceDraft` to the application result; and return only typed neutral outcomes. T063 MUST NOT invoke T059 persistence time or calculate/supply/replace/overwrite either timestamp. Equivalent replay returns the original identifier, `createdAt`, and `updatedAt` without a clock call, canonical rebuild, or new aggregate; cooperative remaining-budget, unique-race, and uncertain-commit behavior remains transport-neutral for FR-006–FR-021/FR-024–FR-038/FR-041/FR-043–FR-047/DR-012–DR-013/DR-019

### Infrastructure Implementation for User Story 1

- [ ] T064 [P] [US1] Implement the Panache buyer-identification catalog persistence model from the exact approved schema for FR-007/FR-028/FR-045/DR-001 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/BuyerIdentificationTypeEntity.java`
- [ ] T065 [P] [US1] Implement the Panache IVA-rule catalog persistence model with fixed UUID, immutable `family=IVA`, treatment, rate, validity, activity, version, and evidence fields and no parent tax-category entity for FR-011/FR-045–FR-047/DR-001 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/IvaTaxRuleEntity.java`
- [ ] T066 [P] [US1] Implement the Panache payment-method catalog persistence model with fixed UUID, validity, activity, version, and evidence fields for FR-013/FR-045–FR-047/DR-001 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/PaymentMethodEntity.java`
- [ ] T067 [P] [US1] Implement the infrastructure-only Invoice Draft root persistence model with authoritative Company ownership, exact buyer-identification repertoire, status, currency, totals, `createdAt`, and `updatedAt` in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftEntity.java`; expose mapping accessors that persist and load the values supplied by T076 without invoking a clock, generating/defaulting timestamps, or normalizing business text, and preserve initial `updatedAt = createdAt` for FR-004/FR-007–FR-008/FR-016–FR-024/FR-037/DR-012/DR-014
- [ ] T068 [P] [US1] Implement the ordered line persistence model, locale-independent ASCII `^[A-Za-z0-9]{1,25}$` product code, and exact numeric columns for FR-009–FR-012/FR-044 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceLineEntity.java`
- [ ] T069 [P] [US1] Implement the one-per-line applied IVA selection persistence model for FR-010–FR-012/DR-001/DR-005 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceLineTaxEntity.java`
- [ ] T070 [P] [US1] Implement grouped treatment/code/rate tax-total persistence for FR-012/DR-008 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceTaxTotalEntity.java`
- [ ] T071 [P] [US1] Implement unique-method payment persistence with exact numeric amounts and catalog evidence for FR-013–FR-014/DR-022 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoicePaymentEntity.java`
- [ ] T072 [P] [US1] Implement ordered additional-information persistence with required NFC-normalized display values and required application-produced `canonicalName`; persist the supplied canonical value and never recompute Unicode normalization, whitespace collapse, or lowercase using the database locale for FR-015/FR-035/DR-019/DR-021 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceAdditionalInformationEntity.java`
- [ ] T073 [P] [US1] Implement the Company/key hash, request fingerprint, version, draft, and binding `createdAt` persistence model using T076's same single immutable transaction-captured Instant as root `createdAt`/`updatedAt`; replay loads the existing binding without invoking the clock for FR-019/FR-027–FR-034/DR-012 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftIdempotencyEntity.java`
- [ ] T074 [US1] Depends on T055 and T067–T073. Implement explicit `InvoiceDraftCandidate`-to-Panache and committed Panache-to-`PersistedInvoiceDraft` mappings without leaking persistence types in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftPersistenceMapper.java`; copy final identifiers and Application-normalized/canonical values unchanged, map both persisted timestamps, and never allocate identifiers, normalize/trim/lowercase/derive canonical values, invoke a clock, generate/default timestamps, or map HTTP failures for Constitution IV/FR-019–FR-024/FR-037/DR-012/DR-019
- [ ] T075 [P] [US1] Implement reactive local buyer/IVA/payment lookups with typed transport-neutral results. Payment lookup accepts `(paymentMethodId, emissionDate)` and requires existence, active state, `effectiveFrom <= emissionDate`, and `(effectiveTo IS NULL OR emissionDate <= effectiveTo)` with inclusive boundaries; never consult current server/request/transaction/createdAt time. For every T058 call clamp pool/query work to `min(configured timeout, remaining Duration)`, start no work at zero remainder, and return neutral persistence-unavailable or deadline-exhausted failures for later API arbitration—never HTTP 503/504—for FR-007/FR-011/FR-013/FR-041/FR-043/FR-045–FR-047/DR-013 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/ReferenceDataRepositoryAdapter.java`
- [ ] T076 [US1] Depends explicitly on T017, T018, T055, T057, T059, T067–T074, and T077. Implement T057 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftRepositoryAdapter.java`: accept only timestamp-free `InvoiceDraftCandidate`; open or join one bounded reactive transaction; after all validations succeed and immediately before root persistence invoke T059 persistence time exactly once; assign that same `java.time.Instant` to root `createdAt`, root `updatedAt`, and binding `createdAt`; preserve all candidate identifiers/normalized values; atomically persist aggregate and binding; and return committed `PersistedInvoiceDraft`. T076 is the sole transactional-clock invocation/assignment owner; rollback exposes neither timestamp, no second call is permitted, and replay loads the original identifier and both timestamps without clock invocation, canonical rebuild, or new aggregate. Enforce Company scope for aggregate/binding only, keep global SRI catalogs unscoped, and return only typed neutral race/budget/uncertain outcomes—never HTTP status/envelope/arbitration—for FR-019–FR-024/FR-027–FR-034/FR-041/FR-043/DR-012–DR-013
- [ ] T077 [P] [US1] Implement the default injectable system clock adapter returning `java.time.Instant` through distinct request and persistence operations; API owns the one request-time invocation and T076 owns the one persistence-time invocation whose single result becomes both `createdAt` and `updatedAt`. The adapter has no orchestration, timestamp-assignment, or call-count ownership, does not query/reconstruct after commit, does not use `track_commit_timestamp`, and does not label either value a physical commit timestamp. Add a resettable deterministic fixed-clock test alternative with invocation counters and no production switch for FR-006/FR-019/DR-012 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/SystemRequestClock.java` and `src/test/java/com/alexastudillo/taxdocument/support/FixedRequestClock.java`
- [ ] T078 [P] [US1] Implement the default local random UUID generator solely for draft and child identifiers, never reference or fiscal identifiers, in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/UuidDraftIdentifierGenerator.java`, plus a resettable deterministic-sequence CDI `@Alternative` with test-only `@Priority` activation confined to `src/test/java/com/alexastudillo/taxdocument/support/DeterministicDraftIdentifierGenerator.java`, with no production configuration switch, for FR-018/FR-023/FR-046

### API and Operational Implementation for User Story 1

- [ ] T079 [P] [US1] Implement strict client-controlled DTOs with no Company identifier and transport descriptions for exact product/buyer ASCII plus general single-line Unicode inputs in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/CreateInvoiceDraftRequest.java`. API decodes JSON, rejects malformed representation, and validates transport structure only; it MUST forward decoded business text unchanged and MUST NOT perform NFC normalization, trim business values, collapse spaces, lowercase, derive `canonicalName`, enforce post-normalization/canonical lengths, or emit `CANONICAL_NAME_TOO_LONG`. FR-027 header handling remains exclusively in the API header adapter for FR-004–FR-015/FR-035/FR-041–FR-042/FR-044/DR-019
- [ ] T080 [P] [US1] Implement the complete distinct response DTO with contract-required canonical CompanyId, captured inputs, calculated outputs, DRAFT, USD, and both persisted immutable `createdAt` and `updatedAt` in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftResponse.java`; copy them from `CreateInvoiceDraftResult` without clock invocation, generation, replacement, or normalization, require equality on initial creation, and preserve both original values on replay for FR-017–FR-022/FR-033/SC-001/SC-007/SC-012/SC-018; response CompanyId does not permit Company input in request schemas
- [ ] T081 [P] [US1] Implement exactly-one required X-Company-Id presence/cardinality, one surrounding ASCII SP/HTAB trim, UUID/nil/canonicalization handling and map it to application CompanyId for FR-001–FR-003/FR-040 in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/CompanyContextHeader.java`
- [ ] T082 [P] [US1] Implement mandatory single-valued Idempotency-Key handling in the API adapter: require exactly one parsed header-field value; return `IDEMPOTENCY_KEY_REQUIRED` when missing; trim leading/trailing ASCII SP/HTAB exactly once; return `IDEMPOTENCY_KEY_INVALID` for blank/whitespace-only, normalized length outside 1–128, controls/non-ASCII, or any non-comma value outside `^[\x21-\x2B\x2D-\x7E](?:[\x20-\x2B\x2D-\x7E]{0,126}[\x21-\x2B\x2D-\x7E])?$`; return `IDEMPOTENCY_KEY_MULTIPLE` for repeated fields, parser-produced multiple values, or any comma-containing/comma-combined ambiguous input; never choose a first value; preserve internal characters and case; and pass only the normalized value to lookup/hash/persistence without logging or persisting the raw key for FR-027/FR-041 in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/IdempotencyKeyHeader.java`
- [ ] T083 [P] [US1] Implement the safe application/problem+json transport model with stable English code, status, instance, correlation, and value-free violations for FR-025/FR-041–FR-044 in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/ProblemDetails.java`
- [ ] T084 [US1] Implement the shared always-safe correlation classifier—including one surrounding ASCII SP/HTAB trim and exact 1–64 ASCII grammar—in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/CorrelationHeader.java`, deadline/correlation/accepted-header terminal-outcome state in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftRequestState.java`, custom Jakarta REST `@NameBinding` in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftRequestGate.java`, and one name-bound `@ServerRequestFilter(nonBlocking = true)` pre-entity gate in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftRequestGateFilter.java`; the API gate uses T081/T082/T083, evaluates Company first, turns stored correlation classification into an error only at FR-041 stage 3, then enforces Idempotency-Key presence/cardinality/normalization/grammar and its three stable codes, and places only accepted mapped values in request-local API state for FR-001–FR-003/FR-026–FR-027/FR-040–FR-041/DR-024
- [ ] T085 [US1] Implement the earliest API-only request-deadline and terminal-outcome owner for `POST /api/v1/invoice-drafts` in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftRequestDeadlineHandler.java`: before body consumption create T054's neutral fixed deadline, store API request state, arm one timer, race every accepted application `Uni` against expiry, and atomically accept exactly one terminal outcome. Timeout wins when terminal before an application outcome is accepted; late application/database results are discarded and cannot write a second response; stage-first outcomes survive later expiry; HTTP 504 exists only here/through API mapping; Company 400 and all other HTTP mappings occur only after arbitration; response-commit expiry is telemetry-only and the timer is cancelled on response end. Keep the exclusive 413 handler in `InvoiceDraftPayloadSizeFailureHandler.java` under the same arbiter for FR-026/FR-041–FR-043/SC-026–SC-028/SC-033
- [ ] T086 [US1] Depends on T054–T055 and T079–T080. Implement explicit request-to-command mapping in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftApiMapper.java` that carries the request-local fixed deadline, mapped Company UUID, raw decoded business inputs, and one API-captured request instant without reading the clock; it MUST NOT normalize, trim, collapse spaces, lowercase, derive canonical values, or generate timestamps. Map `CreateInvoiceDraftResult` to the response by copying both persisted timestamps without alteration for FR-006/FR-022/FR-037/FR-040/FR-043/DR-012/DR-019
- [ ] T087 [US1] After T085 accepts one terminal outcome, map typed transport-neutral application failures to exact 400/409/422/500/503/504 Problem Details and correlation headers; only API maps deadline exhaustion to 504, and no application/repository/domain exception carries HTTP semantics. Include the exact idempotency errors and delegate 413 exclusively to T085 for FR-025–FR-027/FR-030/FR-041–FR-044 in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftExceptionMapper.java`
- [ ] T088 [US1] Expose POST `/api/v1/invoice-drafts`; consume only accepted request-local Company/key/correlation/deadline values; call request time once after stages 1–5; map raw decoded business values without normalization; submit the application `Uni` to T085's exclusive deadline race; and emit exactly one 201/200/error result selected by the API arbiter. Discard late outcomes, never allow a second response, return T076's persisted `createdAt` and `updatedAt` unchanged for new/replay outcomes, and never invoke the persistence clock or generate either timestamp for FR-001–FR-003/FR-006/FR-017–FR-022/FR-026–FR-033/FR-041/FR-043/DR-012/DR-019 in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftResource.java`
- [ ] T089 [P] [US1] Publish `specs/001-create-invoice-draft/contracts/invoice-draft-api.openapi.yaml` byte-for-byte as the non-independently-authored runtime contract, preserving OpenAPI 3.1, security absence, Company header-only input/contract-required response CompanyId, exactly-one Idempotency-Key, API-decodes/Application-normalizes Unicode ownership, exact ASCII/payment-emissionDate/Stage 10–11B/API-only deadline, equal transactional `createdAt`/`updatedAt`, replay preservation, and no 401/403 outcomes for FR-002/FR-013/FR-019/FR-027/FR-035/FR-039/FR-041/SC-016/SC-020/SC-024 in `src/main/resources/META-INF/openapi.yaml`
- [ ] T090 [P] [US1] Implement process-only liveness that remains independent of PostgreSQL and every excluded dependency for Constitution XIII/FR-036/SC-023 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftLivenessCheck.java`
- [ ] T091 [P] [US1] Implement bounded read-only readiness against the same PostgreSQL datasource plus required Flyway/catalog initialization and no other dependency for Constitution XIII/FR-036/SC-023 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftReadinessCheck.java`
- [ ] T092 [P] [US1] Implement bounded metrics, safe structured audit/log/trace context, durations from the same earliest-boundary monotonic start used by T085, and new/replay/conflict/rollback/timeout outcomes; include `request_deadline_exceeded_after_response_commit` with only correlation identifier, operation, already selected status, elapsed duration, and optional already available CompanyId/draftId under existing audit policy, never buyer PII, request body, raw key, token, or high-cardinality metric labels, for FR-025–FR-026/FR-041/FR-043/SC-002/SC-012/SC-023/SC-028/SC-033 in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftTelemetry.java`

**Checkpoint**: User Story 1 independently satisfies all 71 acceptance scenarios, all 33 success
criteria, and every applicable constitutional evidence category on the mandatory JVM runtime.

---

## Final Phase: Definition of Done and Cross-Cutting Validation

**Purpose**: Validate the completed bounded feature without hiding unfinished story work here.

- [ ] T093 Re-run empty-database Flyway, V3-to-T018-V5 upgrade, Flyway validation, exact seed, prohibited-structure, authoritative T017 ASCII vectors, final constraint definitions without POSIX/locale/Unicode shorthand, global-catalog no-Company-column checks, aggregate/binding Company-scope constraints, initial equal timestamp persistence, and repeatability evidence against PostgreSQL 18.4 using `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftMigrationTest.java`
- [ ] T094 Run formatting, static analysis, architecture, domain, application, API, PostgreSQL, concurrency, rollback, sensitive-data, health, observability, and performance suites configured in `build.gradle.kts`
- [ ] T095 Run packaged JVM boot, served `/q/openapi` equality/security absence, and all critical create/replay/conflict/failure/date/correlation/monetary smoke paths from `src/test/java/com/alexastudillo/taxdocument/runtime/InvoiceDraftJvmSmokeTest.java`
- [ ] T096 Validate and record all approved end-to-end, recovery, exact header-cardinality/error, T017-vector/V3→V5 ASCII, Application-only Stage-6 Unicode/`canonicalName`/`U+0130` behavior, candidate/result handoff, Stage 10/11A/11B, payment-emissionDate, API-only deadline, T076-only single-clock equal `createdAt`/`updatedAt`, replay preservation/no clock, Company request/response, repository/global-catalog, negative-architecture, and dynamic America/Guayaquil date scenarios in `specs/001-create-invoice-draft/quickstart.md`
- [ ] T097 Record the exact JVM/PostgreSQL measurement environment, warm-up, samples, percentiles, resource use, blocked-thread result, earliest-boundary deadline start, aggregate/reference remaining-budget and configured-timeout minimum clamping, first-conclusive deadline arbitration, post-response-commit telemetry-only behavior, response-end timer cancellation, and pool recovery evidence in `specs/001-create-invoice-draft/operational-requirements.md`
- [ ] T098 Record mandatory JVM evidence and either native build-plus-runtime evidence or an evidence-based native deferral without weakening JVM support in `specs/001-create-invoice-draft/plan.md`
- [ ] T099 Record the final Constitution v2.0.1 and Definition of Done review, including `GOV-001` and the approved corrective-assignment addendum disposition, Company request/input exclusion and contract-required response, Company-owned aggregate/binding scope with unscoped global catalogs, no identity/Company dependency/snapshot/fiscal side effect, T017 red vectors/evidence, T018 V5/green persistence evidence, T030/T045/T050 distributed equivalence, Application-only normalization, candidate/persisted-result boundary, payment effectivity, API-only deadline arbitration, single T076 clock with equal timestamps/replay preservation, sensitive-data, and runtime evidence in `specs/001-create-invoice-draft/plan.md`
- [ ] T100 Reconcile final FR-001–FR-047, DR-001–DR-024, SC-001–SC-033, AS-001–AS-071, stable-error, governance gate/addendum, shared ASCII-fixture consumers, candidate/result, normalization/timestamp ownership, and prohibited-boundary evidence in `specs/001-create-invoice-draft/traceability.md`
- [ ] T101 Verify approved English terminology remains complete and update only affected classifications in `docs/migration/terminology-mapping.md`

---

## Dependencies and Execution Order

### Workflow Dependency Graph

```text
Constitution v2.0.1 amendment APPROVED
  → approved spec and reconciled plan
  → complete requirements-quality checklists
  → tasks.md generated
  → REQUIRED $speckit-analyze before implementation (violated for T001–T016)
  → historical T001–T016 implementation
  → completed GOV-001 retrospective review + deviation disposition
  → explicit astudilloalex owner approval in both capacities
  → GATE-GOV-001 RELEASED
  → approved corrective-assignment addendum: T017 red evidence, T018 V5/green evidence
  → new $speckit-analyze with no related CRITICAL finding
      → T017 authoritative ASCII vectors + failing pre-V5 PostgreSQL/Flyway evidence
      → T018 immutable V5 corrective migration + passing upgrade/Flyway evidence
      → T019 through Phase 3 US1 Create and Review an Invoice Draft
      → Final Definition of Done Validation
```

### Phase Dependencies

- Phase 1 has no feature-task dependency and must finish before foundational work.
- Phase 2 depends on Phase 1. T010 and T011 depend on T008; T013–T016 are historical migration work.
  T017 depends only on the released gate plus successful new analysis and creates shared
  vectors/failing pre-V5 evidence without editing V3 or depending on T045/T050. T018 depends
  explicitly on T017, creates immutable V5 without editing V3, and proves the corrected final
  PostgreSQL/Flyway schema without claiming production-Java equivalence.
- US1 depends on Phase 2, released `GATE-GOV-001`, T017, and T018. T019 and T020 establish golden fixtures;
  T021–T043 may then be authored in parallel because they change separate test files.
- Domain production starts only after its evidence tasks exist. T044–T049 may run in parallel;
  T045 additionally depends on T017/T018/T022, and T050 depends on T017/T018/T026. T030, T045,
  and T050 independently consume T017's fixture for OpenAPI, production buyer Java, and production
  product/text Java evidence; T017/T018 separately own PostgreSQL/Flyway evidence. The combined
  layer-specific outcomes prove equivalence without domain tests importing transport or database
  infrastructure. T050–T053 then build line, tax-total, calculation, and timestamp-free aggregate
  behavior in dependency order.
- Application tasks depend on T053. T054–T055 and T058–T060/T062 may run in parallel; T057 follows
  T055 because it consumes the candidate/result types; T056/T061 follow their direct types, and
  T063 integrates T053–T062, owns Stage-6 normalization, and constructs the timestamp-free candidate.
- T064–T067 depend on the foundational migrations and may run in parallel. T068–T073 depend on
  T067 and may then run in parallel. T075 may proceed after its reference models and port exist.
  T077–T078 depend only on their application ports. T076 is intentionally non-parallel and depends
  explicitly on T017, T018, T055, T057, T059, T067–T074, and T077; it is the sole owner of the one
  persistence-time clock invocation and assigns its result to both timestamps.
- T079–T083 may run in parallel after the application contract is stable. T084 depends on T054 and
  T081–T083. T089–T092 may run in parallel after their configuration and application dependencies
  exist. T085 depends on T054, T083–T084, and T092. T086 depends on T054–T055 and T079–T080 and is
  intentionally not parallel; both application results and API response contracts must exist before
  result mapping begins. T087 depends on T062 and T082–T084. T088 depends on T077 and T079–T087 so it receives
  gated, decoded inputs before the one authorized request-time read.
- `$speckit-analyze` was required after task generation and before T001, but that sequence was not
  followed and cannot be repaired retroactively. The retrospective review and deviation
  dispositions are approved and `GATE-GOV-001` is released. The corrective-assignment addendum is
  also approved, but current implementation permission remains `PENDING_SUCCESSFUL_ANALYSIS`.
  A new analysis must establish the no-CRITICAL implementation condition before T017 starts. The
  Final Phase depends on T017–T092 and the released gate.

### Parallel Execution Example for User Story 1

After Phase 2 and golden fixtures T019–T020:

```text
Evidence batch A: T021 T022 T023 T024 T025 T026 T027 T028 T029
Evidence batch B: T030 T031 T032 T033 T034
Evidence batch C: T035 T036 T037 T038 T039 T040 T041 T042 T043
```

After the evidence tasks are present:

```text
Domain batch:         T044 T045 T046 T047 T048 T049
Application base:     T054 T055 T058 T059 T060 T062
Reference entities:   T064 T065 T066 T067
Aggregate children:   T068 T069 T070 T071 T072 T073
API foundations:      T079 T080 T081 T082 T083
Operational adapters: T089 T090 T091 T092
```

Tasks listed in the same batch modify independent files but still require the earlier dependency
batch to be complete. Complete T057 after T055, and complete non-parallel T076 only after its full
dependency list. After API foundations, complete T084 and the independently authored T092 telemetry
boundary, then T085 before T088; T084 and T085 are intentionally not parallel.

## Implementation Strategy

### MVP First

1. Preserve T001–T016 as historical work and use the completed retrospective/deviation record.
2. Preserve the completed `astudilloalex` approval, released `GATE-GOV-001`, and approved revised
   assignment in `governance-corrective-assignment-addendum.md`.
3. Run the mandatory new `$speckit-analyze`; do not begin implementation if it reports a
   CRITICAL finding related to this non-conformity or database evolution.
4. Do not execute `$speckit-implement` or start T017 before that new analysis gate succeeds.
5. Complete T017's shared vectors/failing pre-V5 evidence, then T018's V5/passing
   PostgreSQL-Flyway evidence, before T019 or any other business task; failure in either blocks all
   later work.
6. Write all remaining US1 evidence tasks before their production tasks.
7. Complete the US1 domain, application, infrastructure, API, and operational work in dependency
   order.
8. Validate US1 independently; it is the complete MVP and the only story in this bounded feature.
9. Complete Definition of Done evidence without representing the retrospective gate as an original
   pre-T001 analysis.

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
