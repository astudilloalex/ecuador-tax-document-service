---

description: "Dependency-ordered implementation tasks for Prepare Invoice for Fiscal Issuance"
---

# Tasks: Prepare Invoice for Fiscal Issuance

**Input**: Approved design documents from `specs/002-prepare-invoice-issuance/`

**Prerequisites**: Approved `specs/002-prepare-invoice-issuance/spec.md`; completed
`specs/002-prepare-invoice-issuance/plan.md`, `research.md`, `data-model.md`, and `quickstart.md`;
validated inbound and outbound contracts under `specs/002-prepare-invoice-issuance/contracts/`;
completed 122/122 implementation-readiness gate in
`specs/002-prepare-invoice-issuance/checklists/readiness.md`; Constitution v2.0.1; canonical
`docs/migration/terminology-mapping.md`; and the completed Feature 001 implementation and V1–V5
migrations.

**Analysis Gate**: `$speckit-analyze` MUST run after this file is generated and before T001 or any
other task is implemented. Any unresolved CRITICAL inconsistency involving fiscal correctness,
Company ownership, sequence atomicity, natural idempotency, commit uncertainty, external contracts,
null safety, sensitive data, or database evolution MUST block `$speckit-implement`.

**Tests**: Tests are mandatory because the specification defines 24 acceptance scenarios, 16
measurable success criteria, official SRI vectors, PostgreSQL invariants, quantified concurrency,
failure-atomicity, and packaged-JVM evidence. Every required-evidence task precedes the production
task whose behavior or invariant it covers.

**Organization**: Feature 002 contains one independently valuable P1 story. Setup and foundational
controls preserve Feature 001 and unblock the approved architecture; Phase 3 delivers and proves the
complete bounded story; the final phase records Definition of Done evidence without adding scope.

## Format: `[ID] [P?] [Story] Description with exact file path and requirement reference`

- **[P]**: May run in parallel only after its stated phase prerequisites are complete because it
  changes different files and has no dependency on another incomplete task.
- **[US1]**: Serves User Story 1, Prepare an Invoice Exactly Once.
- Every path is repository-relative and every target name uses approved English terminology.
- Tasks MUST correct every JSpecify, NullAway, Error Prone, generic-inference, or `javac` warning at
  its source; broad suppression, `@NullUnmarked`, analyzer exclusion, or warning downgrade is not
  completion evidence.

## Phase 1: Setup

**Purpose**: Add only the approved REST-client and warning-free null-safety tooling, bounded
configuration, and null-marked capability boundaries.

- [ ] T001 Configure BOM-managed `quarkus-rest-client-jackson`, production `compileOnly` JSpecify 1.0.0, Error Prone Gradle plugin 5.1.0, Error Prone 2.50.0, and NullAway 0.13.7 on every JavaCompile task with `OnlyNullMarked=true`, JSpecify mode, generic-inference checks, error severity, existing `-Xlint:all -Werror`, and no broad exclusions or suppressions in `build.gradle.kts` for FR-081/SC-016 and Plan §Null Safety Scope
- [ ] T002 [P] Add environment-only `authoritative-fiscal-context` base-URL configuration, 1-second connect and 2-second response ceilings, the 10-second Fiscal Preparation request deadline, remaining-budget persistence/lock ceilings, zero automatic retry, and no `.invalid`, security, SRI, XML, certificate, queue, or notification runtime default in `src/main/resources/application.properties` for FR-018/FR-022/FR-068–FR-071
- [ ] T003 [P] Configure the local authoritative-fiscal-context fixture, PostgreSQL 18.4, pool size 20, sanitized telemetry, and Feature 002 timeout overrides without a second container lifecycle or production-like secrets in `src/test/resources/application.properties` for SC-003–SC-004/SC-008/SC-016
- [ ] T004 [P] Establish the null-marked transport and telemetry boundaries and their no-security/no-persistence dependency contract in `src/main/java/com/alexastudillo/taxdocument/api/fiscalpreparation/package-info.java` and `src/main/java/com/alexastudillo/taxdocument/api/fiscalpreparation/telemetry/package-info.java` for Constitution IV/VII and FR-010–FR-017/FR-065–FR-075
- [ ] T005 [P] Establish the null-marked transport-neutral use-case and port boundary in `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/package-info.java` for Constitution IV and FR-018–FR-064
- [ ] T006 [P] Establish the null-marked synchronous framework-free fiscal domain boundary in `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/package-info.java` for DR-001–DR-011 and Constitution IV/VI
- [ ] T007 [P] Establish the null-marked REST Client, CSPRNG, Panache, locking, SQLSTATE, and reconciliation adapter boundary in `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/package-info.java` for FR-018–FR-064 and Constitution IV/V
- [ ] T008 [P] Establish narrowly shared and test-evidence null-marked boundaries—without a generic request framework or marking untouched Feature 001 packages—in `src/main/java/com/alexastudillo/taxdocument/api/requestcontext/package-info.java`, `src/main/java/com/alexastudillo/taxdocument/api/problem/package-info.java`, `src/main/java/com/alexastudillo/taxdocument/application/requestcontext/package-info.java`, `src/main/java/com/alexastudillo/taxdocument/infrastructure/requestcontext/package-info.java`, `src/main/java/com/alexastudillo/taxdocument/infrastructure/persistence/package-info.java`, `src/main/java/com/alexastudillo/taxdocument/infrastructure/health/package-info.java`, `src/test/java/com/alexastudillo/taxdocument/architecture/package-info.java`, `src/test/java/com/alexastudillo/taxdocument/support/fiscalpreparation/package-info.java`, and `src/integrationTest/java/com/alexastudillo/taxdocument/runtime/fiscalpreparation/package-info.java` for Plan §Structure Decision/§Null Safety Scope/CHK022/CHK090

**Checkpoint**: The build and package boundaries admit only the approved Feature 002 dependencies
and enforce warning-free nullness without implementing business behavior.

---

## Phase 2: Foundational Controls

**Purpose**: Preserve Feature 001 while extracting only proven shared request helpers and creating
the PostgreSQL/provider/fault evidence harnesses required by the one story.

**CRITICAL**: Phase 3 production work MUST NOT begin until this phase and the post-generation
`$speckit-analyze` gate are complete.

- [ ] T009 [P] Add red/green regression tests for missing/repeated/comma-combined/blank/malformed/nil Company values, one ASCII SP/HTAB trim, canonical UUID mapping, zero-access precedence, and always-safe correlation replacement in `src/test/java/com/alexastudillo/taxdocument/api/requestcontext/CompanyContextHeaderTest.java` and `src/test/java/com/alexastudillo/taxdocument/api/requestcontext/CorrelationHeaderTest.java` for FR-010–FR-014/FR-055/FR-073
- [ ] T010 [P] Add monotonic deadline, fixed request-entry instant/Ecuador date, persistence-time precision, and remaining-budget clamp regression tests in `src/test/java/com/alexastudillo/taxdocument/application/requestcontext/RequestContextTest.java`, `src/test/java/com/alexastudillo/taxdocument/infrastructure/requestcontext/SystemRequestClockTest.java`, and `src/test/java/com/alexastudillo/taxdocument/infrastructure/persistence/ReactiveOperationBudgetTest.java` for FR-068–FR-072/DR-004
- [ ] T011 [P] Narrow global prohibitions to allow only the approved `fiscalpreparation` capability and BOM REST Client while continuing to reject reversed dependencies, security/identity, Company or Issuer master-data clients/repositories, caches, brokers, XML/SRI/certificate/PDF adapters, background executors, and unmarked owned Feature 002 packages in `src/test/java/com/alexastudillo/taxdocument/architecture/CleanArchitectureTest.java` for Constitution IV/VII/XV/XVI and FR-076–FR-081
- [ ] T012 [P] Make Feature 001 boundary assertions capability-specific so they continue to prohibit fiscal identity inside Invoice Draft creation without globally forbidding approved Feature 002 types in `src/test/java/com/alexastudillo/taxdocument/architecture/InvoiceDraftBoundaryTest.java` for Plan §Source Conflicts and Resolutions/FR-080
- [ ] T013 [P] Replace Feature 001 runtime-OpenAPI byte equality with semantic preservation assertions that tolerate the one approved Feature 002 path while preserving every Feature 001 operation/schema/error and security absence, and place this modified existing test type in checked scope with type-level `@NullMarked` without marking its untouched package, in `src/test/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftOpenApiContractTest.java` for Plan §Source Conflicts and Resolutions/§Null Safety Scope/CHK086
- [ ] T014 Relocate the exact Company and correlation parsing behavior from `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/CompanyContextHeader.java` and `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/CorrelationHeader.java` into `src/main/java/com/alexastudillo/taxdocument/api/requestcontext/CompanyContextHeader.java` and `src/main/java/com/alexastudillo/taxdocument/api/requestcontext/CorrelationHeader.java`, reuse `src/main/java/com/alexastudillo/taxdocument/domain/invoicedraft/CompanyId.java`, reject repeated/comma-combined Company values, and remove the obsolete capability-local copies only after Feature 001 consumers compile unchanged for FR-010–FR-017/FR-073
- [ ] T015 Relocate the safe RFC 9457 transport representation and API-only exception carrier from `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/ProblemDetails.java` into `src/main/java/com/alexastudillo/taxdocument/api/problem/ProblemDetails.java`, update all Feature 001 API consumers, keep status/error selection capability-specific, and place each modified existing consumer type in checked scope with type-level `@NullMarked` without marking its untouched package, including `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftExceptionMapper.java`, for FR-065–FR-067 and Plan §Structure Decision/§Null Safety Scope
- [ ] T016 Relocate `RequestClock` and monotonic `RequestDeadline` from `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/RequestClock.java` and `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/RequestDeadline.java` into `src/main/java/com/alexastudillo/taxdocument/application/requestcontext/RequestClock.java` and `src/main/java/com/alexastudillo/taxdocument/application/requestcontext/RequestDeadline.java`, add immutable request-entry instant plus `America/Guayaquil` civil date in `src/main/java/com/alexastudillo/taxdocument/application/requestcontext/RequestContext.java`, and remove obsolete copies after consumer migration for FR-007/FR-068–FR-072/DR-004
- [ ] T017 Refactor Feature 001 consumers to the shared header, correlation, Problem Details, clock, and deadline types without changing its route, Company/idempotency rules, terminal arbitration, timestamps, or errors, and place every modified existing type in checked scope with type-level `@NullMarked` without marking either untouched Feature 001 package, in `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftRequestBoundary.java`, `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftRequestGateFilter.java`, `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftRequestDeadlineHandler.java`, `src/main/java/com/alexastudillo/taxdocument/api/invoicedraft/InvoiceDraftResource.java`, and `src/main/java/com/alexastudillo/taxdocument/application/invoicedraft/CreateInvoiceDraftCommand.java` for Plan §Structure Decision/§Null Safety Scope/CHK022
- [ ] T018 Relocate the injectable UTC clock and remaining-budget clamp from `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/SystemRequestClock.java` and `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/ReactiveOperationBudget.java` into `src/main/java/com/alexastudillo/taxdocument/infrastructure/requestcontext/SystemRequestClock.java` and `src/main/java/com/alexastudillo/taxdocument/infrastructure/persistence/ReactiveOperationBudget.java`, update `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftRepositoryAdapter.java`, `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/ReferenceDataRepositoryAdapter.java`, and `src/test/java/com/alexastudillo/taxdocument/support/FixedRequestClock.java`, place each modified existing type in checked scope with type-level `@NullMarked` without marking its untouched package, and remove obsolete copies only after Feature 001 behavior remains green for FR-068–FR-072/FR-081 and Plan §Null Safety Scope
- [ ] T019 [P] Create reusable Feature 002 schema reset, controlled Invoice Draft/baseline fixture insertion, row-count, baseline-value, and unchanged-draft snapshot support on the existing PostgreSQL Dev Service in `src/test/java/com/alexastudillo/taxdocument/support/fiscalpreparation/FiscalPreparationPostgreSqlSupport.java` for SC-003–SC-004/SC-009–SC-012
- [ ] T020 [P] Create a bounded local Vert.x fixture implementing `authoritative-fiscal-context` contract 1.0.0 with call counters, delayed/malformed/oversized/partial responses, all provider status codes, and no production endpoint or credentials in `src/test/java/com/alexastudillo/taxdocument/support/fiscalpreparation/AuthoritativeFiscalContextFixture.java` for FR-018–FR-033/SC-008
- [ ] T021 [P] Create a test-only PostgreSQL transport fault harness capable of interrupting COMMIT acknowledgement without adding a production dependency or treating a mocked exception as commit uncertainty in `src/test/java/com/alexastudillo/taxdocument/support/fiscalpreparation/PostgreSqlCommitFaultProxy.java` for FR-059/FR-064/SC-010
- [ ] T022 Run the complete Feature 001 regression, architecture, OpenAPI, null-analysis, and PostgreSQL suite configured by `build.gradle.kts` and fix extraction-caused defects without suppressions before beginning User Story 1 for Plan §Structure Decision/Constitution XII

**Checkpoint**: Feature 001 remains semantically unchanged, the global guards recognize the
approved Feature 002 boundary, and all shared/test foundations are ready.

---

## Phase 3: User Story 1 - Prepare an Invoice Exactly Once (Priority: P1) MVP

**Goal**: Prepare one existing eligible Company-owned Invoice Draft into exactly one immutable
Fiscal Preparation containing authoritative source evidence, the next controlled nine-digit
Official Sequential Number, one eight-digit Numeric Code, and one valid SRI v2.33 Access Key, with
natural replay and no excluded side effects.

**Independent Test**: Create one Feature 001 Invoice Draft for the fixed current Ecuador date,
configure one complete authoritative provider response, and test-provision a valid exact-scope
baseline with `lastAllocated=122`. The first bodyless Company-scoped POST returns `201`, sequence
`000000123`, a complete immutable snapshot, one eight-digit Numeric Code, and a fully validated
49-digit Access Key; a retry with a different correlation identifier and the provider offline
returns the identical committed row as `200` without provider, baseline, generator, clock, draft
mutation, or excluded-system activity. The concurrency suites additionally prove 100 same-draft
requests converge on one identity and 100 drafts in one scope receive exactly the next 100 values.

### Required Evidence for User Story 1

- [ ] T023 [P] [US1] Create independently recalculated SRI v2.33 positive vectors, raw `11→0` and `10→1` Modulo 11 edges, leading-zero Numeric Codes, every component/check-digit mutation, and the page-64 printed-key negative vector in `src/test/resources/fiscalpreparation/sri-access-key-v2.33-vectors.json` for FR-043–FR-053/SC-005
- [ ] T024 [US1] Add pure construction, parsing, exact `8+2+13+1+3+3+9+8+1+1` composition, right-to-left `2..7` Modulo 11, source-component equality, Ecuador-date formatting, and vector-consumption tests in `src/test/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/AccessKeyGeneratorTest.java` for FR-045–FR-053/DR-006–DR-008/SC-005; depends on T023
- [ ] T025 [P] [US1] Add exact range/width/ASCII/leading-zero tests for Official Sequential Number `000000001..999999999`, Numeric Code `00000000..99999999`, and immutable exact-scope semantics in `src/test/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/FiscalIdentityValueTest.java` for FR-034–FR-044/DR-005/DR-011
- [ ] T026 [P] [US1] Add complete/minimal immutable snapshot tests covering SRI technical-rule identifier/date, Numeric Code policy version, effective intervals, exact codes, source revision/observation, optional Commercial Name, required Special Taxpayer/Withholding resolutions, complete Large Contributor resolution/legend pairing, and no invented Accounting Required/RIMPE resolution in `src/test/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/FiscalContextSnapshotTest.java` for FR-025–FR-032/DR-002/DR-011/SC-002
- [ ] T027 [P] [US1] Add aggregate tests proving one indivisible immutable Fiscal Preparation, Company-plus-draft identity, unchanged emission date, one creation instant, and no partial/update/delete/cancel/reuse state in `src/test/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/FiscalPreparationTest.java` for FR-004–FR-009/DR-001–DR-004
- [ ] T028 [P] [US1] Add application ordering tests for valid Company/correlation then Company-scoped replay/draft lookup; absent, non-DRAFT, orphan, duplicate, and inconsistent prior identity states; fixed Ecuador date; complete provider validation before any store/transaction call; replay-before-date/provider/baseline/generator/clock; stable provider failures; and unchanged draft projection in `src/test/java/com/alexastudillo/taxdocument/application/fiscalpreparation/PrepareInvoiceForFiscalIssuanceUseCaseTest.java` for FR-003–FR-033/FR-054–FR-064/FR-068–FR-072/SC-007–SC-012
- [ ] T029 [P] [US1] Add outbound-contract metadata tests for capability `authoritative-fiscal-context`, version 1.0.0, `Fiscal Context Provider Owner`, and non-routable `.invalid` placeholder semantics in `src/test/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/AuthoritativeFiscalContextContractTest.java`, plus adapter tests for exact Company header/selection body, response/designation/source-evidence mapping, configurable destination, 1s/2s remaining-budget clamps, one attempt, cancellation, 404/409/422/503/504 classification, malformed/oversized/partial JSON, and redaction in `src/test/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalContextHttpAdapterTest.java` for FR-018–FR-033/SC-008
- [ ] T030 [P] [US1] Add PostgreSQL 18.4 empty-database and V5-upgrade migration tests for exactly two V6 tables, immutable V1–V5 checksums, no baseline seed, no PostgreSQL sequence/identity allocation for the Official Sequential Number, no JSONB/provisional/master table, named FK/unique/check/trigger invariants, optional pairs, and direct preparation/baseline mutation rejection in `src/test/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalPreparationMigrationTest.java` for FR-008–FR-009/FR-025–FR-029/FR-034–FR-042/FR-052/FR-060–FR-062
- [ ] T031 [P] [US1] Add reactive PostgreSQL adapter tests for every Company-scoped preflight/replay/locked read, safe cross-Company not-found, exact-scope baseline lookup, draft-first then baseline-second locking, missing/invalid/exhausted handling, one-step advancement, timestamp ownership, and persisted domain mapping in `src/test/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalPreparationRepositoryAdapterTest.java` for FR-003–FR-014/FR-034–FR-042/FR-056–FR-063
- [ ] T032 [P] [US1] Add PostgreSQL 18.4 concurrency tests proving 100 equivalent requests commit one identity/increment and 100 different drafts in one scope commit exactly the next 100 distinct sequential values and Access Keys under pool size 20, with distinct-scope independence and loser-to-winner convergence in `src/test/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalPreparationConcurrencyTest.java` for FR-042/FR-052/FR-054–FR-062/SC-003–SC-004
- [ ] T033 [P] [US1] Add fault tests before/after lock, baseline update, preparation insert, flush, and commit initiation; prove confirmed rollback consumes nothing, response loss replays, lost COMMIT acknowledgement reconciles or returns unknown, and no path directly reallocates after uncertainty in `src/test/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalPreparationRollbackTest.java` for FR-059–FR-064/FR-072/SC-010
- [ ] T034 [P] [US1] Add API tests for bodyless POST, non-nil draft UUID, exact Company-header cardinality and zero-access precedence, cross-Company 404, prohibited body/idempotency/fiscal inputs, `201` new/`200` replay, replay and no-store headers, full safe error catalog, correlation replacement, and success-only sensitive fields in `src/test/java/com/alexastudillo/taxdocument/api/fiscalpreparation/FiscalPreparationResourceTest.java` for FR-001–FR-017/FR-065–FR-080/SC-006/SC-014
- [ ] T035 [P] [US1] Add deterministic 10-second deadline and single-terminal-result tests for fixed request-entry date, external and local budget clamping, conclusive-result precedence, commit-not-started/rollback-confirmed zero-state claims, possible-commit replay guidance, late-result discard, and midnight crossing in `src/test/java/com/alexastudillo/taxdocument/api/fiscalpreparation/FiscalPreparationRequestDeadlineHandlerTest.java` for FR-007/FR-068–FR-074/SC-011
- [ ] T036 [P] [US1] Add semantic runtime OpenAPI tests that preserve Feature 001 and exactly match the Feature 002 inbound contract: one bodyless path, mandatory Company header, no Company response/body/query/path field, no Idempotency-Key/security/Authorization/401/403/excluded route, and complete response/error schemas in `src/test/java/com/alexastudillo/taxdocument/api/fiscalpreparation/FiscalPreparationOpenApiContractTest.java` for FR-001–FR-002/FR-010–FR-017/FR-065–FR-080
- [ ] T037 [P] [US1] Add captured error/log/trace/metric/audit tests proving raw provider material, credentials, endpoints, SQL, baseline values, Company/draft UUID labels, RUC, names, addresses, Numeric Code, Access Key, and source revision never leak outside the explicit success representation in `src/test/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/SensitiveFiscalDataExposureTest.java` for FR-032/FR-065/FR-073/FR-075/SC-014
- [ ] T038 [P] [US1] Add capability-level dependency, exact checked-scope `@NullMarked`, no-suppression, and exclusion tests proving pure Domain, one real provider port/store/generator boundary, every new Feature 002/extracted shared package and each retained existing type modified by T013/T015/T017/T018 is marked, the health types relocated by T039/T070 enter the marked shared package, untouched Feature 001 packages are not marked transitively, no PostgreSQL sequence exists, and no auth/Company master/admin/baseline admin/XML/SRI/certificate/PDF/storage/messaging/background implementation exists in `src/test/java/com/alexastudillo/taxdocument/architecture/FiscalPreparationBoundaryTest.java` for FR-015–FR-016/FR-076–FR-081/SC-013/SC-016 and Plan §Null Safety Scope
- [ ] T039 [P] [US1] Relocate the existing health regression evidence from `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftHealthTest.java` to the null-marked shared service boundary at `src/test/java/com/alexastudillo/taxdocument/infrastructure/health/ServiceHealthTest.java`, proving liveness remains process-only and readiness requires the same PostgreSQL/Flyway V6 destination but never calls the provider, requires a Company/baseline row, or mutates state, and remove the obsolete Feature 001-owned test only after its behavior is preserved for FR-022/FR-031 and Plan §Structure Decision/§Liveness and Readiness/§Null Safety Scope
- [ ] T040 [P] [US1] Add packaged-JVM first-preparation, replay during provider outage, response-loss recovery, provider timeout-before-baseline, rollback, commit-uncertainty, sanitized telemetry, and unchanged-draft smoke scenarios in the dedicated null-marked Feature 002 package at `src/integrationTest/java/com/alexastudillo/taxdocument/runtime/fiscalpreparation/FiscalPreparationJvmSmokeIT.java` without marking the existing Feature 001 runtime package for SC-001/SC-007–SC-015 and Plan §Null Safety Scope
- [ ] T041 [P] [US1] Add packaged-JVM 100-same-draft and 100-one-scope-drafts load, event-loop/blocking inspection, pool-20 queueing, deadline-clamp, and post-load recovery evidence in the dedicated null-marked Feature 002 package at `src/integrationTest/java/com/alexastudillo/taxdocument/runtime/fiscalpreparation/FiscalPreparationJvmPerformanceIT.java` without marking the existing Feature 001 runtime package for SC-003–SC-004 and Plan §Performance Goals/§Null Safety Scope

### Production Implementation for User Story 1

- [ ] T042 [P] [US1] Implement immutable exact-width/range `OfficialSequentialNumber` and `NumericCode` values with locale-independent ASCII representation and retained leading zeroes in `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/OfficialSequentialNumber.java` and `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/NumericCode.java` for FR-037–FR-044/DR-005/DR-011
- [ ] T043 [US1] Implement pure SRI v2.33 Access Key construction, component parser/equality validation, and Modulo 11 Verification Digit logic in `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/AccessKey.java` and `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/AccessKeyGenerator.java` for FR-045–FR-053/DR-006–DR-008; depends on T042
- [ ] T044 [P] [US1] Implement immutable Fiscal Source Evidence, `SRI-OFFLINE-2.33`/`2026-07-13` and `SECURE_RANDOM_8_V1` rule-policy evidence, explicit conditional designation values, authoritative exact-code/text validation, effective-period rules, and the minimal non-master Fiscal Context Snapshot in `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/FiscalSourceEvidence.java`, `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/FiscalDesignation.java`, and `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/FiscalContextSnapshot.java` for FR-025–FR-032/DR-002/DR-011
- [ ] T045 [P] [US1] Implement immutable `OfficialSequenceScope` plus explicit allocatable/exhausted existing-baseline state without Company or environment as numbering dimensions in `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/OfficialSequenceScope.java` and `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/OfficialSequenceBaseline.java` for FR-034–FR-042/DR-005
- [ ] T046 [US1] Implement the immutable one-draft Fiscal Preparation aggregate joining snapshot, baseline identity, Official Sequential Number, Numeric Code, Access Key, unchanged emission date, and creation instant with no partial/update/delete/cancel state in `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/FiscalPreparation.java` for FR-004–FR-009/FR-060–FR-062/DR-001–DR-004; depends on T042–T045
- [ ] T047 [P] [US1] Define the Company-scoped minimal Invoice Draft preparation projection and sealed preflight lookup alternatives `Existing`, `EligibleDraft`, `NotFound`, and `NotPreparable` in `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/InvoiceDraftPreparationView.java` and `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/FiscalPreparationLookup.java` for FR-003–FR-007/FR-013–FR-014/FR-057
- [ ] T048 [P] [US1] Define the transport-neutral authoritative resolution model and read-only `FiscalContextPort` carrying canonical Company, exact Emission Point, unchanged date, document type `01`, safe correlation, and remaining budget in `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/FiscalContextResolution.java` and `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/FiscalContextPort.java` for FR-018–FR-024/FR-063
- [ ] T049 [US1] Implement complete pre-transaction fiscal-context selection, field, effective-period, eligibility, designation-pair, source-evidence, and exact-code validation with typed local failures in `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/FiscalContextValidator.java` for FR-020–FR-033/SC-008; depends on T044/T048
- [ ] T050 [P] [US1] Define only the currently required replaceable generators for Fiscal Preparation UUID and eight-digit Numeric Code, with no caller input or generic factory hierarchy, in `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/FiscalPreparationIdentifierGenerator.java` and `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/NumericCodeGenerator.java` for FR-043–FR-045/CHK021
- [ ] T051 [US1] Define the transaction-shaped Company-scoped `FiscalPreparationStore`, prevalidated commit intent, and sealed `Created`/`Replay` commit result without HTTP, Panache, nullable, provisional state, or caller/proposed sequential, Numeric Code, Verification Digit, Access Key, identifier, or creation-time fields in `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/FiscalPreparationStore.java`, `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/FiscalPreparationCommitIntent.java`, and `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/FiscalPreparationCommitResult.java` for FR-054–FR-064/DR-011
- [ ] T052 [P] [US1] Implement the complete transport-neutral stable failure catalog, retry/commit-knowledge classification, safe details, and no sensitive/internal cause exposure in `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/FiscalPreparationFailure.java` and `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/FiscalPreparationApplicationException.java` for FR-022–FR-024/FR-038–FR-040/FR-051/FR-064–FR-072
- [ ] T053 [US1] Define the no-body preparation command, created/replayed result, and synchronous-observable Mutiny use-case port with natural identity exactly Company plus Invoice Draft in `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/PrepareInvoiceForFiscalIssuanceCommand.java`, `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/PrepareInvoiceForFiscalIssuanceResult.java`, and `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/PrepareInvoiceForFiscalIssuanceUseCase.java` for FR-001–FR-002/FR-054–FR-059/FR-074
- [ ] T054 [US1] Implement replay-first orchestration, one fixed Ecuador eligibility decision, one bounded provider attempt, full validation before transaction start, and delegation of winning identity selection only to the atomic store in `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/PrepareInvoiceForFiscalIssuanceService.java` for FR-003–FR-033/FR-054–FR-064/FR-068–FR-072; depends on T046–T053
- [ ] T055 [P] [US1] Create immutable Flyway V6 with exactly the `official_sequence_baseline` and append-only flattened `fiscal_preparation` tables; named non-nil and Company-consistent FKs; Company-plus-draft, global exact-scope, scoped-sequential, and global Access Key uniqueness; exact ASCII widths/ranges/pairs/effective intervals; preparation/scope immutability and one-step monotonic-allocation guards; no PostgreSQL sequence or identity for the fiscal sequential; and zero seed/baseline/master/provisional rows in `src/main/resources/db/migration/V6__create_fiscal_preparation.sql` for FR-008–FR-009/FR-025–FR-029/FR-034–FR-042/FR-052/FR-060–FR-062
- [ ] T056 [US1] Map the two V6 structures as infrastructure-only Panache records with legitimate nullable columns confined to documented optional fields and immediate explicit-state conversion in `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalPreparationEntity.java` and `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/OfficialSequenceBaselineEntity.java` for FR-025–FR-029/FR-034–FR-042/FR-081; depends on T055
- [ ] T057 [P] [US1] Implement the MicroProfile REST Client interface plus bounded request/response/problem DTOs that exactly consume contract 1.0.0 and never persist or log raw provider material in `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/AuthoritativeFiscalContextClient.java` and `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/AuthoritativeFiscalContextDto.java` for FR-018–FR-024/FR-032
- [ ] T058 [US1] Implement the non-blocking one-attempt Fiscal Context HTTP adapter with environment-configured destination, Company header propagation, exact selection mapping, remaining-budget timeout clamps, typed 404/409/422/503/504/transport/JSON failures, cancellation, and sanitized observability in `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalContextHttpAdapter.java` for FR-018–FR-024/FR-032–FR-033/FR-068–FR-071; depends on T048/T052/T057
- [ ] T059 [P] [US1] Implement startup-initialized/warmed JDK CSPRNG policy `SECURE_RANDOM_8_V1`, uniform `0..99,999,999` formatting, and UUID generation with no request-path blocking or production switch in `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/SecureRandomNumericCodeGenerator.java` and `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/UuidFiscalPreparationIdentifierGenerator.java` for FR-043–FR-044/SC-005 and Plan §Reactive Boundary
- [ ] T060 [US1] Implement explicit row-to-domain and commit-intent-to-row mapping without normalization, hidden defaults, timestamp generation, HTTP types, raw provider payloads, or Panache leakage in `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalPreparationPersistenceMapper.java` for FR-008/FR-025–FR-032/FR-056/FR-081; depends on T046/T051/T056
- [ ] T061 [P] [US1] Implement commit-phase, rollback-knowledge, stable named-constraint, and SQLSTATE classification for `23502`/`23503`/`23505`/`23514`, conclusively aborted `40P01`/`40001`, and conservative class `08`/`08007`/`40003`/`57014`/shutdown outcomes, plus post-transaction Company-plus-draft reconciliation that returns a complete winner or conservative unknown and never reallocates directly in `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/PostgreSqlCommitOutcomeClassifier.java` and `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalPreparationCommitReconciler.java` for FR-058–FR-064/FR-072/SC-010
- [ ] T062 [US1] Implement bounded Company-scoped preflight/replay/draft projection reads and post-failure reconciliation reads, with absent and cross-Company drafts indistinguishable and no commercial graph reconstruction or mutation, in `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalPreparationRepositoryAdapter.java` for FR-003–FR-017/FR-056–FR-059
- [ ] T063 [US1] Complete the same repository adapter with one short reactive transaction that applies transaction-local timeouts, locks Company-plus-draft first, rechecks winner/eligibility, locks the exact baseline second, derives the next value, generates and validates identity once, captures one creation instant, inserts the complete preparation and advances only `lastAllocated+1`/`updatedAt`, flushes, and classifies/reconciles failures without consuming a rolled-back candidate in `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalPreparationRepositoryAdapter.java` for FR-033–FR-064/SC-003–SC-004/SC-009–SC-010; depends on T043/T045/T050/T059–T062
- [ ] T064 [P] [US1] Implement earliest non-blocking API request state/boundary handling for exact Company precedence, non-nil draft path UUID, absent/invalid correlation replacement, fixed request instant/Ecuador date, body rejection, one 10-second deadline, and single terminal result without an Idempotency-Key in `src/main/java/com/alexastudillo/taxdocument/api/fiscalpreparation/FiscalPreparationRequestBoundary.java`, `src/main/java/com/alexastudillo/taxdocument/api/fiscalpreparation/FiscalPreparationRequestState.java`, and `src/main/java/com/alexastudillo/taxdocument/api/fiscalpreparation/FiscalPreparationRequestDeadlineHandler.java` for FR-001–FR-017/FR-054–FR-055/FR-068–FR-074
- [ ] T065 [P] [US1] Implement the exact success DTO and explicit domain-to-transport mapper, omitting Company while copying persisted sensitive fiscal fields only for `201`/`200` and never generating or modifying identity/timestamps in `src/main/java/com/alexastudillo/taxdocument/api/fiscalpreparation/FiscalPreparationResponse.java` and `src/main/java/com/alexastudillo/taxdocument/api/fiscalpreparation/FiscalPreparationApiMapper.java` for FR-017/FR-025–FR-031/FR-041–FR-050/FR-056/FR-075
- [ ] T066 [P] [US1] Map every stable failure to the approved 400/404/409/422/500/503/504 Problem Details response, safe correlation, accurate zero-state/replay guidance, `Cache-Control: no-store`, and sanitized fallback without racing outcomes or exposing causes in `src/main/java/com/alexastudillo/taxdocument/api/fiscalpreparation/FiscalPreparationExceptionMapper.java` for FR-065–FR-075/SC-014
- [ ] T067 [US1] Expose bodyless `POST /api/v1/invoice-drafts/{invoiceDraftId}/fiscal-preparation`, invoke only the accepted request context and use case, and emit identical persisted representations as `201` new or `200` replay with `Fiscal-Preparation-Replayed`, `X-Correlation-Id`, and `Cache-Control: no-store` in `src/main/java/com/alexastudillo/taxdocument/api/fiscalpreparation/FiscalPreparationResource.java` for FR-001–FR-005/FR-054–FR-059/FR-065–FR-075; depends on T054/T064–T066
- [ ] T068 [P] [US1] Implement bounded sanitized metrics, trace/log context, and audit events for committed, replayed, baseline failure, rollback, timeout owner, and unknown outcome using only safe correlation, hardened local references, bounded labels, versions, timings, and commit-knowledge class in `src/main/java/com/alexastudillo/taxdocument/api/fiscalpreparation/telemetry/FiscalPreparationTelemetryPort.java` and `src/main/java/com/alexastudillo/taxdocument/api/fiscalpreparation/telemetry/FiscalPreparationTelemetry.java` for FR-065/FR-073/FR-075/SC-014
- [ ] T069 [P] [US1] Merge the approved inbound Feature 002 operation into the runtime OpenAPI source while semantically preserving Feature 001 and defining no request body, Company body/path/query/response property, Idempotency-Key, security scheme, Authorization, 401/403, provider endpoint, or excluded operation in `src/main/resources/META-INF/openapi.yaml` for FR-001–FR-002/FR-010–FR-017/FR-065–FR-080
- [ ] T070 [US1] Relocate and rename the existing service health checks from `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftLivenessCheck.java` and `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicedraft/InvoiceDraftReadinessCheck.java` to the null-marked shared boundary at `src/main/java/com/alexastudillo/taxdocument/infrastructure/health/ServiceLivenessCheck.java` and `src/main/java/com/alexastudillo/taxdocument/infrastructure/health/ServiceReadinessCheck.java`; recognize successful Flyway V6 on the configured PostgreSQL destination while retaining process-only liveness, bounded read-only readiness, and zero provider, Company-specific, baseline-row, or mutation probe, and remove the obsolete Feature 001-owned classes only after T039 proves semantic preservation for FR-022/FR-031 and Plan §Structure Decision/§Liveness and Readiness/§Null Safety Scope

**Checkpoint**: User Story 1 independently satisfies all 24 acceptance scenarios, SC-001–SC-016,
and every applicable constitutional test category on the mandatory JVM runtime without any later
story or excluded capability.

---

## Final Phase: Definition of Done and Cross-Cutting Validation

**Purpose**: Execute and record the approved evidence without concealing unfinished story behavior
or claiming absent production deployment prerequisites.

- [ ] T071 Run Spotless, `javac -Xlint:all -Werror`, Error Prone, JSpecify/NullAway including generic inference, unit/API/architecture/PostgreSQL tests, and Feature 001 regressions through `build.gradle.kts`; accept zero warnings, zero broad suppressions/exclusions, and zero failing requirements for FR-081/SC-016
- [ ] T072 Re-run PostgreSQL 18.4 empty-database, V5-to-V6, checksum, no-seed/no-sequence, named-constraint, immutability, rollback, and repeatability evidence in `src/test/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalPreparationMigrationTest.java` for SC-003/SC-009–SC-010 and Constitution IX
- [ ] T073 Run the focused provider, application-ordering, API, Company-scoping, 100-request concurrency, rollback, response-loss, genuine COMMIT-uncertainty, health, sensitive-data, and exclusion suites documented in `specs/002-prepare-invoice-issuance/quickstart.md` and record only actual results for SC-001–SC-015
- [ ] T074 Build the packaged JVM artifact and run `FiscalPreparationJvmSmokeIT` plus `FiscalPreparationJvmPerformanceIT` against PostgreSQL 18.4 and the approved provider fixture, recording environment, pool/event-loop recovery, deadlines, and measured outcomes in `specs/002-prepare-invoice-issuance/quickstart.md` for SC-001/SC-003–SC-004/SC-007–SC-015
- [ ] T075 Verify served runtime OpenAPI semantic consistency, success-only sensitive fields, `no-store`, sanitized telemetry, PostgreSQL/Flyway-only readiness, and every explicit absence assertion using `src/test/java/com/alexastudillo/taxdocument/api/fiscalpreparation/FiscalPreparationOpenApiContractTest.java`, `src/test/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/SensitiveFiscalDataExposureTest.java`, and `src/test/java/com/alexastudillo/taxdocument/architecture/FiscalPreparationBoundaryTest.java` for FR-065–FR-080/SC-013–SC-015
- [ ] T076 Record the final Constitution v2.0.1/Definition of Done review, mandatory JVM evidence, evidence-based native deferral, and actual status of the `Fiscal Context Provider Owner` destination/operational-owner registration, `Database Operations Owner` baseline evidence (requester, approver, execution time, Company ownership, exact scope, validated initial `lastAllocated`, resulting identifier), and `Platform Operations Owner` TLS/encryption/backup/restore/Invoice-retention-and-linked-disposal evidence—retaining the production block wherever evidence is absent—in `specs/002-prepare-invoice-issuance/plan.md` without inventing deployment evidence
- [ ] T077 Reconcile the independent acceptance and recovery procedure with actual results and explicitly distinguish fixture acceptance from production provider registration, baseline provisioning, and platform-control release evidence in `specs/002-prepare-invoice-issuance/quickstart.md` for the approved dependency responsibilities
- [ ] T078 Verify Feature 002 source, API, database, errors, tests, fixtures, telemetry, and documentation use the registered Target Domain terms and update only genuine drift in `docs/migration/terminology-mapping.md` without rewriting Feature 001 history for Constitution II/FR-080

---

## Dependencies and Execution Order

### Workflow Dependency Graph

```text
Constitution v2.0.1 approved
  → Feature 002 specification and clarification complete
  → implementation plan and design artifacts complete
  → readiness checklist 122/122 PASS
  → this tasks.md generated
  → $speckit-analyze with no unresolved CRITICAL finding
  → Phase 1 Setup
  → Phase 2 Foundational Controls
  → Phase 3 US1 required evidence
  → Phase 3 US1 production implementation
  → Final Definition of Done validation
```

### Phase Dependencies

- **Phase 1** has no implementation-task dependency but is blocked by the mandatory
  post-generation `$speckit-analyze` gate.
- **Phase 2** depends on Phase 1. T014–T018 depend on the red/green extraction tests T009–T013;
  T022 depends on T014–T018 and blocks Phase 3.
- **US1 Required Evidence** depends on Phase 2. T024 depends on vector fixture T023; all other
  evidence tasks T025–T041 may be authored in parallel after their Phase 2 support exists.
- **US1 Production** begins only after T023–T041 exist as executable red evidence. Domain tasks
  T042/T044/T045 can proceed independently; T043 depends on T042; T046 depends on T042–T045.
  Application orchestration T054 depends on T046–T053. Provider work T057–T059 and persistence
  work T055–T063 may proceed on separate files once their application/domain contracts exist.
  API tasks T064–T069 depend on the application contracts; T067 integrates T054/T064–T066.
- **Final Validation** depends on the complete US1 checkpoint. T076/T077 MUST report actual
  evidence and MUST NOT convert an absent production prerequisite into a false PASS.

### User Story Dependencies

- **User Story 1 (P1)** is the only story and has no dependency on any later feature. It is complete
  and independently usable before XML generation, signing, SRI communication, rendering, or
  delivery exists.

### Parallel Opportunities

- After T001, setup files T002–T008 are independent.
- In Phase 2, extraction tests T009–T013 and test supports T019–T021 use different files; production
  extraction T014–T018 then converges in the Feature 001 regression gate T022.
- After Phase 2, evidence tasks T023 and T025–T041 use separate test/fixture files; T024 starts after
  T023.
- After evidence exists, domain branches T042/T044/T045 are independent. Provider client work
  T057–T059 can proceed separately from Flyway/Panache work T055–T063. API boundary, response,
  exception, telemetry, and OpenAPI tasks T064–T066/T068–T069 use distinct files before resource
  integration T067.
- Tasks that edit `build.gradle.kts`, either `application.properties`, the V6 migration, the shared
  Feature 001 extraction consumers, runtime OpenAPI, `plan.md`, `quickstart.md`, or the terminology
  mapping MUST remain sequential for that file.

## Parallel Execution Example: User Story 1

```text
After Phase 2:
  Author A: T023 → T024 (official Access Key vectors and domain proof)
  Author B: T026 + T027 + T028 (snapshot, aggregate, and ordering evidence)
  Author C: T029 + T034 + T035 + T036 (provider/API/deadline/contracts evidence)
  Author D: T030 + T031 + T032 + T033 (Flyway, locking, concurrency, uncertainty evidence)

After T023–T041 are executable:
  Author A: T042 → T043 and T044 → T046 (pure domain)
  Author B: T047–T054 (application models, ports, validation, orchestration)
  Author C: T057–T059 (provider client and generators after their ports)
  Author D: T055–T056 → T060–T063 (V6, Panache mapping, atomic persistence)

After T054 and the relevant adapters:
  Author A: T064 + T065 + T066 + T068 + T069
  Integrator: T067 → T070 → T071–T078
```

## Implementation Strategy

### MVP First

1. Run `$speckit-analyze` and resolve every CRITICAL finding before T001.
2. Complete Setup and Foundational Controls, including a clean Feature 001 regression gate.
3. Author all US1 evidence before its production implementation.
4. Implement pure fiscal rules and transport-neutral ports before external/persistence adapters.
5. Complete fixed draft-first/baseline-second atomic persistence before exposing the API route.
6. Validate US1 independently with provider and baseline fixtures, then complete packaged JVM and
   Definition of Done evidence.

Because there is one user story, this independently tested US1 increment is both the MVP and the
entire Feature 002 bounded outcome.

### Incremental Delivery

Each completed layer MUST preserve Company ownership, replay-before-provider ordering, immutable
Fiscal Source Evidence, transactional sequence allocation, exact SRI v2.33 correctness, accurate
commit-knowledge claims, warning-free null safety, sensitive-data redaction, and zero excluded side
effects. No later task may require regenerating a committed Numeric Code, Access Key, or Official
Sequential Number.

## Notes

- Production Official Sequence Baselines are external operational prerequisites owned by the
  `Database Operations Owner`; their reviewed SQL/runbook evidence records requester, approver,
  execution time, exact scope, initial value, and resulting identifier. Tasks may create fixture
  rows only; they MUST NOT create a seed, initializer, upsert, repair/reset command, or
  administration API/runbook implementation.
- The approved provider fixture is sufficient for implementation and automated acceptance. Tasks
  MUST NOT implement the provider, hardcode the planning `.invalid` URL, copy Company/Issuer/
  Establishment/Emission Point master data, or claim a production destination already exists.
- Platform TLS, PostgreSQL encryption at rest, encrypted backup/restore, and Invoice-record
  retention are release-evidence responsibilities of the Platform Operations Owner. Tasks MUST NOT
  add application encryption/key management, deletion APIs, or a retention scheduler.
- Historical files under `docs/legacy/` MUST NOT be edited and legacy algorithms remain candidate or
  negative vectors only.
- No task introduces authentication, authorization, Keycloak/JWT/OIDC, Company administration,
  baseline administration, Invoice Draft mutation, XML/schema validation, certificate/PKCS#12,
  signing, SRI communication/retries/reconciliation, RIDE/PDF, storage, email/webhook, queue/event/
  notification delivery, background processing, other tax-document types, cancellation/reversal/
  reuse, or legacy compatibility.
- A task MUST NOT be marked complete merely to unblock another task or because a test only increases
  coverage; completion requires the stated observable requirement evidence.
