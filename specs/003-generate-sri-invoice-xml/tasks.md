---

description: "Dependency-ordered implementation tasks for Feature 003"
---

# Tasks: Generate Standard SRI Invoice XML

**Input**: Completed design documents from `/specs/003-generate-sri-invoice-xml/`

**Prerequisites**: `spec.md`, completed clarifications, `plan.md`, completed requirement-quality checklist, `research.md`, `data-model.md`, `contracts/`, and `quickstart.md`

**Analysis Gate**: `$speckit-analyze` MUST run after this file is generated and before any task is implemented. Unresolved critical findings MUST block `$speckit-implement`.

**Tests**: The specification and constitution require domain, application, Company-scope, PostgreSQL, Flyway, API-contract, XML/XSD, concurrency, timeout/reconciliation, sensitive-data, architecture, health, and packaged-JVM evidence. Test tasks precede the production tasks whose behavior or invariant they cover.

**Organization**: All three user stories are P1. They are ordered by implementation dependency: establish one valid committed artifact, add exact replay and convergence, then complete the fail-closed boundary matrix. Every target name is English; exact official SRI element names remain confined to the SRI adapter and official schema assets.

## Format: `[ID] [P?] [Story] Description with exact file path and requirement reference`

- **[P]**: The task can run in parallel because it changes different files and has no dependency on incomplete work.
- **[Story]**: Required only in user-story phases and maps the task to `[US1]`, `[US2]`, or `[US3]`.
- Every task has an exact repository-relative path and an approved requirement, success criterion, domain rule, research decision, or constitutional gate.

## Phase 1: Setup

**Purpose**: Establish the bounded execution, schema-resource, package, and synthetic-fixture baseline without adding another XML library, cache, store, or runtime integration.

- [ ] T001 Add the BOM-managed `quarkus-smallrye-context-propagation` dependency required by the planned `ManagedExecutor`, without adding an XML library, in `build.gradle.kts` (R-009, R-016; Constitution III/V)
- [ ] T002 [P] Add the ten-second request budget, 4s/3s/5s persistence ceilings, max-four/queue-100 worker limits, 2 MiB XML limit, and sanitized test overrides in `src/main/resources/application.properties` and `src/test/resources/application.properties` (FR-078, FR-088; R-009/R-010/R-015)
- [ ] T003 [P] Add a red provenance test that compares every planning and runtime schema resource by exact length and SHA-256 in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/OfficialInvoiceSchemaResourceTest.java` (FR-064–FR-068; R-003)
- [ ] T004 Copy the byte-identical `catalog.xml`, `factura_V1.1.0.xsd`, `xmldsig-core-schema.xsd`, `XMLSchema.dtd`, and `datatypes.dtd` authority closure into `src/main/resources/sri/invoice/1.1.0/catalog.xml`, `src/main/resources/sri/invoice/1.1.0/factura_V1.1.0.xsd`, `src/main/resources/sri/invoice/1.1.0/xmldsig-core-schema.xsd`, `src/main/resources/sri/invoice/1.1.0/XMLSchema.dtd`, and `src/main/resources/sri/invoice/1.1.0/datatypes.dtd` (FR-064–FR-068; R-003)
- [ ] T005 [P] Create package contracts without package-level nullness defaults in `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/package-info.java`, `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/telemetry/package-info.java`, `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/package-info.java`, `src/main/java/com/alexastudillo/taxdocument/domain/invoicexml/package-info.java`, `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/package-info.java`, `src/test/java/com/alexastudillo/taxdocument/support/invoicexml/package-info.java`, and `src/integrationTest/java/com/alexastudillo/taxdocument/runtime/invoicexml/package-info.java` (plan Null safety scope; Constitution IV)
- [ ] T006 [P] Add synthetic, non-production mapping/ordering and text/decimal boundary vectors in `src/test/resources/invoicexml/sri-invoice-1.1.0-mapping-vectors.json` and `src/test/resources/invoicexml/xml-text-decimal-vectors.json` (FR-031–FR-063, SC-010–SC-011; Constitution VIII/XII)

---

## Phase 2: Foundational Controls

**Purpose**: Evolve Feature 002 to commit exhaustive profile evidence, create the immutable PostgreSQL foundation, and establish the bounded CPU execution control required by every story.

**CRITICAL**: User-story production work MUST NOT begin until this phase is complete. Proposed provider contract `2.0.0` may drive synthetic tests, but first-generation acceptance remains blocked until the owner approval evidence in T073 exists.

### Required Foundational Evidence

- [ ] T007 [P] Add semantic consumer-contract tests for version `2.0.0`, the fixed profile/rule/trigger-set identifiers, all fourteen assessments, designation constraints, no security scheme, and proposed approval status in `src/test/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/AuthoritativeFiscalContextContractTest.java` (FR-024–FR-030; R-007)
- [ ] T008 [P] Add Feature 002 HTTP-adapter tests for complete profile mapping, missing/unknown fields, each assessment value, and zero Feature 003 runtime provider use in `src/test/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalContextHttpAdapterTest.java` (FR-021, FR-024–FR-028; R-007)
- [ ] T009 [P] Add pure-domain tests for exhaustive trigger lookup, eligibility classification, technical-rule matching, tightened Special Taxpayer/Withholding Agent/Large Contributor evidence, and legacy absence in `src/test/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/StandardInvoiceXmlProfileEvidenceTest.java` and `src/test/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/FiscalContextSnapshotTest.java` (FR-024–FR-030, DR-004–DR-005)
- [ ] T010 [P] Add Feature 002 use-case regression fixtures proving complete provider evidence is atomically carried into a new preparation while invalid responses commit nothing in `src/test/java/com/alexastudillo/taxdocument/application/fiscalpreparation/PrepareInvoiceForFiscalIssuanceUseCaseTest.java`, `src/test/java/com/alexastudillo/taxdocument/support/fiscalpreparation/AuthoritativeFiscalContextFixture.java`, and `src/test/java/com/alexastudillo/taxdocument/support/fiscalpreparation/FiscalPreparationTestFixtures.java` (FR-028; SC-009)
- [ ] T011 [P] Add Feature 002 persistence tests for all-sixteen-or-null profile columns, exact round trip, complete inserts, and unchanged readable legacy rows in `src/test/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalPreparationRepositoryAdapterTest.java` (FR-024–FR-028; R-007/R-011)
- [ ] T012 [P] Add PostgreSQL 18.4 empty-database and V6-to-V7 migration tests covering unchanged V1–V6 checksums, profile completeness, prepared-source mutation guards, composite ownership FKs, exact-byte/digest/length checks, 2 MiB cap, uniqueness, and append-only artifacts in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/InvoiceXmlArtifactMigrationTest.java` (FR-022–FR-023, FR-069–FR-075; SC-008/SC-012)
- [ ] T013 [P] Add bounded-executor tests for max active work, queue rejection, cleared unrelated context, captured Vert.x resumption, late-result discard, and zero event-loop XML/XSD/Base64 work in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/InvoiceXmlWorkerExecutorTest.java` (FR-078, FR-088; R-009/R-015)

### Foundational Implementation

- [ ] T014 Implement the exhaustive immutable evidence model in `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/StandardInvoiceXmlProfileEvidence.java`, `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/SpecializedInvoiceProfileTrigger.java`, and `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/ProfileTriggerAssessment.java` (FR-024–FR-028, DR-004)
- [ ] T015 Evolve the committed fiscal domain to carry optional historical-or-complete profile evidence and exact designation envelopes in `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/FiscalContextSnapshot.java`, `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/FiscalPreparation.java`, and `src/main/java/com/alexastudillo/taxdocument/domain/fiscalpreparation/FiscalDesignation.java` (FR-022, FR-028–FR-030)
- [ ] T016 Evolve consumer-side validation and transport-neutral mapping for contract `2.0.0` in `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/FiscalContextResolution.java` and `src/main/java/com/alexastudillo/taxdocument/application/fiscalpreparation/FiscalContextValidator.java` (FR-024–FR-030; R-007)
- [ ] T017 Evolve the Feature 002 REST client DTO and adapter to reject incomplete/unknown v2 evidence and map all fourteen explicit assessments in `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/AuthoritativeFiscalContextDto.java`, `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/AuthoritativeFiscalContextClient.java`, and `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalContextHttpAdapter.java` (FR-024–FR-028; R-007)
- [ ] T018 Extend Feature 002 persistence inserts and hydration with the sixteen nullable evidence columns while preserving old rows unchanged in `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalPreparationEntity.java`, `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalPreparationPersistenceMapper.java`, and `src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/FiscalPreparationRepositoryAdapter.java` (FR-028; R-007/R-011)
- [ ] T019 [P] Create Flyway V7 with the all-null/all-complete profile check, exact preparation composite key, six prepared-source mutation guards, and immutable `unsigned_sri_invoice_xml_artifact` table/constraints/triggers in `src/main/resources/db/migration/V7__create_unsigned_sri_invoice_xml_artifact.sql` (FR-022–FR-023, FR-069–FR-075; R-011)
- [ ] T020 [P] Implement the named bounded `ManagedExecutor` bridge with max-four active tasks, queue 100, context clearing/resumption, deadline-aware submission, and safe rejection in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/InvoiceXmlWorkerExecutor.java` (FR-078, FR-088; R-009/R-015)

**Checkpoint**: New Feature 002 preparations can persist complete explicit profile evidence, legacy preparations remain all-null and undetermined, V7 enforces the source/artifact invariants, and CPU work has a bounded non-event-loop execution path.

---

## Phase 3: User Story 1 - Generate One Valid Unsigned Invoice XML (Priority: P1) MVP

**Goal**: Generate, validate, atomically persist, and return one exact unsigned Invoice XML `1.1.0` artifact from one valid prepared Company-owned draft.

**Independent Test**: With one synthetic Company-scoped draft, one exact committed preparation, and complete eligible profile evidence, the bodyless POST returns `201`; Base64 decodes to the compact UTF-8 bytes, all persisted commercial/fiscal values and only approved adapter mappings are present, the official XSD passes, SHA-256/length match, one immutable row exists, and no signature or excluded side effect occurs.

### Required Evidence for User Story 1

- [ ] T021 [US1] Add shared synthetic source builders plus pure-domain tests for defensive copies, non-nil/source identity, fixed schema/algorithm, exact digest/length, and absence of mutable lifecycle state in `src/test/java/com/alexastudillo/taxdocument/support/invoicexml/InvoiceXmlTestFixtures.java`, `src/test/java/com/alexastudillo/taxdocument/domain/invoicexml/UnsignedSriInvoiceXmlArtifactTest.java`, `src/test/java/com/alexastudillo/taxdocument/domain/invoicexml/ValidatedUnsignedSriInvoiceXmlTest.java`, and `src/test/java/com/alexastudillo/taxdocument/domain/invoicexml/XmlIntegrityEvidenceTest.java` (US1; FR-069–FR-073, DR-001–DR-002/DR-011–DR-012)
- [ ] T022 [P] [US1] Add exact-byte StAX tests for declaration/root attributes, official order/cardinality, every core and conditional mapping, persisted collection ordering, `USD`→`DOLAR`, `SI`/`NO`, `propina=0.00`, 2–6 decimals, escaping round trip, compact output, and unsupported-section/signature absence in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/StaxSriInvoiceXmlAdapterTest.java` (US1; FR-031–FR-063, SC-002–SC-003/SC-010–SC-011)
- [ ] T023 [P] [US1] Add secure JAXP tests for exact final-byte validation, shared `Schema`/fresh `Validator`, all pinned hashes, valid/invalid official vectors, strict catalog resolution, missing/mutated dependencies, XXE/SSRF/file denial, concurrent validators, and sanitized SAX failures in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/JaxpSriInvoiceXsdValidatorTest.java` (US1; FR-064–FR-068, SC-002/SC-012)
- [ ] T024 [P] [US1] Add canonical padded RFC 4648 encoding, 2 MiB boundary, decoded-byte equality, worker-thread, and defensive-copy tests in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/JdkInvoiceXmlContentEncoderTest.java` (US1; FR-070–FR-071/FR-076, SC-006)
- [ ] T025 [P] [US1] Add reactive PostgreSQL tests and support for complete deterministic source hydration, physical-row reordering, Company predicates, absent/cross-Company drafts, missing or inconsistent preparation relationships, exact `bytea`/digest/length round trip, source composite relationships, and winner-only UUID/time generation in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/InvoiceXmlArtifactRepositoryAdapterTest.java` and `src/test/java/com/alexastudillo/taxdocument/support/invoicexml/InvoiceXmlPostgreSqlSupport.java` (US1; FR-009/FR-014–FR-023/FR-069–FR-073, SC-003/SC-006–SC-008)
- [ ] T026 [P] [US1] Add application tests for valid first-generation ordering, persisted-source exclusivity, profile acceptance, representative legacy/unsupported/source-invalid failures before XML work, generator→validator→digest→encoder→store byte identity, one winner clock call, XSD-invalid zero state, and zero provider/catalog/current-date/excluded calls in `src/test/java/com/alexastudillo/taxdocument/application/invoicexml/GenerateUnsignedSriInvoiceXmlUseCaseTest.java` (US1; FR-014–FR-030/FR-064–FR-077, SC-001–SC-003/SC-008–SC-009/SC-013–SC-014)
- [ ] T027 [P] [US1] Add semantic contract tests that merge the standalone Feature 003 OpenAPI exactly, preserve Feature 001/002 operations and explicit empty `security: []`, forbid security schemes, non-empty security requirements, Authorization, `401`, and `403`, expose only one bodyless Company-header POST, return the Base64 envelope, omit Company/replay/lifecycle fields, and narrow the old global XML-path prohibition in `src/test/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlOpenApiContractTest.java` and `src/test/java/com/alexastudillo/taxdocument/api/fiscalpreparation/FiscalPreparationOpenApiContractTest.java` (US1; FR-001–FR-013/FR-076–FR-077, SC-016–SC-017)
- [ ] T028 [P] [US1] Add REST Assured tests for the valid `201 Created` response plus representative missing/invalid Company, malformed draft, prohibited input, and schema-invalid failures, asserting zero premature access, stable Problem Details, safe static detail/correlation, payload redaction, no Company response field, canonical Base64 metadata, and `Cache-Control: no-store` in `src/test/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlResourceTest.java` (US1; FR-001–FR-013/FR-067/FR-076–FR-084, SC-001/SC-006–SC-007/SC-012/SC-015)
- [ ] T029 [P] [US1] Add packaged-JVM first-generation smoke evidence for resource hashes, worker isolation, valid/invalid XSD, exact PostgreSQL bytes/digest, HTTP `201`, and zero certificate/signature/SRI side effects in `src/integrationTest/java/com/alexastudillo/taxdocument/runtime/invoicexml/InvoiceXmlJvmSmokeIT.java` (US1; SC-001–SC-003/SC-006/SC-014/SC-017; Constitution III/XII)

### Production Implementation for User Story 1

- [ ] T030 [US1] Implement defensive-copy domain values and immutable artifact invariants in `src/main/java/com/alexastudillo/taxdocument/domain/invoicexml/ValidatedUnsignedSriInvoiceXml.java`, `src/main/java/com/alexastudillo/taxdocument/domain/invoicexml/XmlIntegrityEvidence.java`, and `src/main/java/com/alexastudillo/taxdocument/domain/invoicexml/UnsignedSriInvoiceXmlArtifact.java` (US1; FR-069–FR-073, DR-001–DR-002/DR-011–DR-012)
- [ ] T031 [US1] Define the complete detached persisted-source model and sealed Company-scoped lookup outcomes in `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/InvoiceXmlGenerationSource.java` and `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/InvoiceXmlGenerationSourceStore.java` (US1; FR-014–FR-023, DR-003/DR-006)
- [ ] T032 [P] [US1] Define independent asynchronous application ports for deterministic generation, exact schema validation, and response encoding in `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/SriInvoiceXmlGeneratorPort.java`, `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/SriInvoiceSchemaValidatorPort.java`, and `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/InvoiceXmlContentEncoderPort.java` (US1; FR-064–FR-066/FR-076/FR-088; Constitution IV/XI)
- [ ] T033 [P] [US1] Define the artifact store, commit intent/result/knowledge tracker, and winner identifier boundary in `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/InvoiceXmlArtifactStore.java`, `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/InvoiceXmlCommitIntent.java`, `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/InvoiceXmlCommitResult.java`, `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/InvoiceXmlCommitTracker.java`, and `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/InvoiceXmlArtifactIdentifierGenerator.java` (US1; FR-069–FR-075/FR-079–FR-080, DR-015)
- [ ] T034 [US1] Define the Company-scoped command/result/use-case and stable transport-neutral failure model in `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/GenerateUnsignedSriInvoiceXmlCommand.java`, `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/GenerateUnsignedSriInvoiceXmlResult.java`, `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/GenerateUnsignedSriInvoiceXmlUseCase.java`, `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/InvoiceXmlFailure.java`, and `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/InvoiceXmlApplicationException.java` (US1; FR-001–FR-013/FR-076–FR-081)
- [ ] T035 [P] [US1] Implement bounded deterministic Java 25 StAX serialization with official SRI names confined to the adapter in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/StaxSriInvoiceXmlAdapter.java` (US1; FR-031–FR-063, DR-006–DR-010; R-004/R-005)
- [ ] T036 [P] [US1] Implement fail-closed startup provenance/compilation and fresh secure per-request validation in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/OfficialInvoiceSchemaDescriptor.java` and `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/JaxpSriInvoiceXsdValidator.java` (US1; FR-064–FR-068; R-003/R-006)
- [ ] T037 [P] [US1] Implement bounded canonical Base64 encoding over exact persisted bytes in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/JdkInvoiceXmlContentEncoder.java` (US1; FR-071/FR-076, SC-006; R-009/R-010)
- [ ] T038 [P] [US1] Implement the immutable Panache model, lossless byte/digest mapping, and UUID winner generator in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/UnsignedSriInvoiceXmlArtifactEntity.java`, `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/InvoiceXmlPersistenceMapper.java`, and `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/UuidInvoiceXmlArtifactIdentifierGenerator.java` (US1; FR-069–FR-073, DR-001/DR-011)
- [ ] T039 [US1] Implement complete Company-scoped source/artifact reads plus draft-first locked first-commit persistence on the captured Vert.x context in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/InvoiceXmlArtifactRepositoryAdapter.java` (US1; FR-009/FR-014–FR-023/FR-069–FR-075; R-008/R-011/R-012)
- [ ] T040 [US1] Implement first-generation orchestration with persisted-source/profile validation, exact port ordering, remaining-budget clamps, winner-only persistence time, and zero excluded calls in `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/GenerateUnsignedSriInvoiceXmlService.java` (US1; FR-014–FR-030/FR-064–FR-080/FR-085–FR-088)
- [ ] T041 [P] [US1] Implement the bodyless request state, earliest monotonic deadline, exactly-one Company header parsing, path UUID mapping, safe correlation, and no current-date capture in `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlRequestState.java`, `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlRequestBoundary.java`, and `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlRequestDeadlineHandler.java` (US1; FR-001–FR-013/FR-078–FR-080; R-013/R-015)
- [ ] T042 [P] [US1] Implement the explicit success envelope and exact artifact-to-API mapping with Company omitted and canonical Base64 retained in `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/UnsignedSriInvoiceXmlArtifactResponse.java` and `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlApiMapper.java` (US1; FR-012/FR-070–FR-071/FR-076–FR-077)
- [ ] T043 [P] [US1] Implement safe Problem Details translation and sanitized completion telemetry contracts in `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlExceptionMapper.java`, `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/telemetry/InvoiceXmlTelemetryPort.java`, and `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/telemetry/InvoiceXmlTelemetry.java` (US1; FR-081–FR-084, SC-015)
- [ ] T044 [US1] Expose `POST /api/v1/invoice-drafts/{invoiceDraftId}/unsigned-sri-invoice-xml` with no body/query/idempotency input, `201` for the confirmed committer, no-store headers, and no lifecycle mutation in `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlResource.java` (US1; FR-001–FR-013/FR-076–FR-077/FR-085–FR-087)
- [ ] T045 [P] [US1] Merge only the approved standalone Feature 003 operation/components into the runtime API source of truth in `src/main/resources/META-INF/openapi.yaml` (US1; FR-001–FR-013/FR-076–FR-081; contract `unsigned-sri-invoice-xml-api.openapi.yaml`)

**Checkpoint**: User Story 1 independently creates one exact, schema-valid, immutable unsigned artifact and returns it as the approved `201` JSON/Base64 representation without any signing, SRI, sequence, mutation, delivery, or identity side effect.

---

## Phase 4: User Story 2 - Replay the Exact Committed Artifact (Priority: P1)

**Goal**: Return the original persisted artifact byte-for-byte on equivalent retries and make 100 concurrent first requests converge on that one winner.

**Independent Test**: After one artifact exists, repeat and race the same Company-plus-draft request with provider/catalog/schema publication sites unavailable and clock spies that fail on use; every response returns the original identity/content/digest/length/time, exactly one first request is `201`, all followers are `200`, and replay performs no XML/XSD/current-date/write work.

### Required Evidence for User Story 2

- [ ] T046 [P] [US2] Add application replay-precedence tests for matching source identities, zero profile/generator/validator/provider/catalog/current-date/persistence-clock/write calls, persisted-winner encoding, restart-equivalent loading, and response-loss recovery in `src/test/java/com/alexastudillo/taxdocument/application/invoicexml/GenerateUnsignedSriInvoiceXmlReplayUseCaseTest.java` (US2; FR-014–FR-016/FR-074/FR-077, SC-005)
- [ ] T047 [P] [US2] Add API tests proving existing and concurrent-follower outcomes return `200` with representation values equal to the original `201`, `xmlContentBase64` decoding to byte-identical XML, and no replay header/flag/status field without imposing raw JSON whitespace or property-order identity in `src/test/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlReplayResourceTest.java` (US2; FR-015–FR-016/FR-076–FR-077, SC-005/SC-016)
- [ ] T048 [P] [US2] Add PostgreSQL 18.4 tests for 100 equivalent requests, one row/one `201`/99 successful `200`, same-draft different-Company isolation, database uniqueness races from independent Vert.x contexts, fixed lock order, and pool/executor recovery in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/InvoiceXmlArtifactConcurrencyTest.java` (US2; FR-072–FR-074, SC-004)
- [ ] T049 [P] [US2] Add confirmed-rollback, uniqueness-loser, lost-COMMIT-acknowledgement, fresh-session reconciliation, inconclusive outcome, and never-regenerate/reinsert tests in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/InvoiceXmlArtifactRollbackTest.java` and `src/test/java/com/alexastudillo/taxdocument/support/invoicexml/PostgreSqlInvoiceXmlCommitFaultProxy.java` (US2; FR-074–FR-075/FR-079–FR-080, SC-012; DR-015)
- [ ] T050 [P] [US2] Extend packaged-JVM smoke coverage with a committed artifact replayed after process restart while provider/catalog/publication sites are unavailable, asserting identical persisted bytes and zero generator/validator/current-date/write calls, and add maximum-source, 100-request contention/replay, ten-second budget, worker/queue/pool bound, no blocked-event-loop, and recovery evidence in `src/integrationTest/java/com/alexastudillo/taxdocument/runtime/invoicexml/InvoiceXmlJvmSmokeIT.java` and `src/integrationTest/java/com/alexastudillo/taxdocument/runtime/invoicexml/InvoiceXmlJvmPerformanceIT.java` (US2; SC-004–SC-005; R-009/R-013/R-015)

### Production Implementation for User Story 2

- [ ] T051 [US2] Implement PostgreSQL commit-knowledge classification and one fresh Company-plus-draft reconciliation lookup in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/PostgreSqlInvoiceXmlCommitOutcomeClassifier.java` and `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/InvoiceXmlCommitReconciler.java` (US2; FR-074–FR-075/FR-079–FR-080; R-012)
- [ ] T052 [US2] Complete draft-first recheck, existing-winner selection, uniqueness-race recovery, confirmed rollback, possible-commit reconciliation, and no blind retry in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/InvoiceXmlArtifactRepositoryAdapter.java` (US2; FR-014–FR-016/FR-072–FR-075, DR-015)
- [ ] T053 [US2] Implement replay-before-profile precedence, persisted-winner Base64 encoding, candidate discard for followers, exact zero-call guarantees, and `Created` versus `Replay` results in `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/GenerateUnsignedSriInvoiceXmlService.java` (US2; FR-014–FR-016/FR-074/FR-077, SC-004–SC-005)
- [ ] T054 [US2] Map only the confirmed committer to `201` and every existing/follower/reconciled winner to `200` while keeping identical representations and sanitized telemetry in `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlResource.java` and `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/telemetry/InvoiceXmlTelemetry.java` (US2; FR-077, SC-004–SC-005)

**Checkpoint**: User Stories 1 and 2 produce and recover one immutable byte identity; concurrency and ambiguous commit recovery never create a replacement or reinterpret historical source evidence.

---

## Phase 5: User Story 3 - Reject Unsafe or Unsupported Generation (Priority: P1)

**Goal**: Fail closed with stable, non-sensitive Problem Details whenever input, ownership, source, profile, schema capability, deadline, or persistence outcome cannot be trusted.

**Independent Test**: Exercise every header/request, not-found/cross-Company, missing/duplicate/inconsistent preparation, incomplete/unsupported profile, mandatory-value, validator, timeout, rollback, and unknown-outcome vector; each returns its stable code and state guarantee, leaks no payload, mutates no source, and performs none of the excluded side effects.

### Required Evidence for User Story 3

- [ ] T055 [P] [US3] Add application failure-matrix tests for missing/cross-linked source, every absent/indeterminate/`APPLIES` trigger, wrong rule/set, Popular Business, incomplete designation, unrepresentable mandatory value, generator/schema/encoder rejection, stage budget expiry, and zero downstream calls after failure in `src/test/java/com/alexastudillo/taxdocument/application/invoicexml/GenerateUnsignedSriInvoiceXmlFailureUseCaseTest.java` (US3; FR-018–FR-030/FR-061/FR-067–FR-068/FR-075/FR-078–FR-080, SC-008–SC-012)
- [ ] T056 [P] [US3] Add boundary tests for missing/repeated/comma-combined/blank/malformed/nil Company headers, malformed/nil draft IDs, non-empty/chunked bodies, any query, `Idempotency-Key`, prohibited Company/fiscal/XML input, safe correlation replacement, and zero Company-owned access in `src/test/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlRequestValidationTest.java` (US3; FR-002–FR-013, SC-007)
- [ ] T057 [P] [US3] Add monotonic deadline-race tests for pre-submit/read/worker/lock/commit/reconcile expiry, one terminal response, late pure-result discard, and possible-commit no-zero-state semantics in `src/test/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlRequestDeadlineHandlerTest.java` (US3; FR-078–FR-080, SC-001/SC-012)
- [ ] T058 [P] [US3] Extend repository tests with absent/cross-Company drafts, zero/duplicate/partial/corrupt preparations, Access Key/source mismatch, other-Company artifacts, statement/lock/transaction timeouts, database outage, and defensive-copy failure classification in `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/InvoiceXmlArtifactRepositoryAdapterTest.java` (US3; FR-009–FR-020/FR-075/FR-079–FR-080, SC-007–SC-008/SC-012)
- [ ] T059 [P] [US3] Add a complete API error matrix for all sixteen stable codes and their 400/404/409/422/500/503/504 statuses, Problem Details fields, static safe detail, correlation, state claim, content type, and no-store header, plus sentinel-based response/log/metric/trace/health/audit tests proving zero XML, digest, Access Key, Numeric Code, RUC, buyer, name, address, parser, SQL, path, or endpoint disclosure outside the explicit success body in `src/test/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlExceptionMapperTest.java` and `src/test/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/SensitiveInvoiceXmlDataExposureTest.java` (US3; FR-067–FR-068/FR-075/FR-080–FR-084, SC-012/SC-015; Constitution VIII/XII/XIII)
- [ ] T060 [P] [US3] Add and narrow static architecture guards so official XML exists only behind `invoicexml` infrastructure ports, fiscal preparation can own evidence but not generation, every concrete type is directly `@NullMarked`, and no auth/Company/cache/signature/certificate/SRI/PDF/broker/event/notification capability appears in `src/test/java/com/alexastudillo/taxdocument/architecture/InvoiceXmlBoundaryTest.java`, `src/test/java/com/alexastudillo/taxdocument/architecture/CleanArchitectureTest.java`, and `src/test/java/com/alexastudillo/taxdocument/architecture/FiscalPreparationBoundaryTest.java` (US3; FR-085–FR-088, SC-013–SC-017; Constitution IV/VII/XI)
- [ ] T061 [P] [US3] Extend health tests for process-only liveness and readiness on the same PostgreSQL/Flyway V7 destination plus exact local validator availability, with zero SRI/W3C/provider/Company/catalog business probes and no sensitive details in `src/test/java/com/alexastudillo/taxdocument/infrastructure/health/ServiceHealthTest.java` (US3; FR-068/FR-082, SC-012/SC-015; R-017)

### Production Implementation for User Story 3

- [ ] T062 [US3] Implement explicit source/profile/mandatory-field classification with undetermined-versus-unsupported precedence and fail-before-XML behavior in `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/InvoiceXmlSourceValidator.java` and `src/main/java/com/alexastudillo/taxdocument/application/invoicexml/GenerateUnsignedSriInvoiceXmlService.java` (US3; FR-018–FR-030/FR-061, DR-004–DR-005/DR-014)
- [ ] T063 [P] [US3] Complete strict body/query/prohibited-header rejection and commit-knowledge-aware one-deadline terminal selection in `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlRequestBoundary.java` and `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlRequestDeadlineHandler.java` (US3; FR-002–FR-013/FR-078–FR-080)
- [ ] T064 [P] [US3] Complete the stable error-code/status table, static English safe details, state claims, correlation, no-store headers, and unknown-exception sanitization in `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/InvoiceXmlExceptionMapper.java` (US3; FR-067–FR-068/FR-075/FR-080–FR-084; Stable Error Catalog)
- [ ] T065 [P] [US3] Complete Company-scoped inconsistency detection, stage-ceiling enforcement, rollback knowledge, and safe PostgreSQL error classification in `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/InvoiceXmlArtifactRepositoryAdapter.java` and `src/main/java/com/alexastudillo/taxdocument/infrastructure/invoicexml/PostgreSqlInvoiceXmlCommitOutcomeClassifier.java` (US3; FR-009–FR-020/FR-075/FR-078–FR-080)
- [ ] T066 [P] [US3] Advance readiness from Flyway V6 to V7 and incorporate only the safe local schema-descriptor availability signal in `src/main/java/com/alexastudillo/taxdocument/infrastructure/health/ServiceReadinessCheck.java` (US3; FR-068/FR-082; R-017)
- [ ] T067 [P] [US3] Emit only bounded safe commit/replay/rollback/unknown outcome signals with opaque local IDs and no Company/digest/payload labels in `src/main/java/com/alexastudillo/taxdocument/api/invoicexml/telemetry/InvoiceXmlTelemetry.java` (US3; FR-082–FR-083, SC-015; plan Structured Observability/Audit Events)

**Checkpoint**: Every unsafe or unsupported vector fails with its stable state guarantee before an invalid artifact or prohibited side effect can occur, and no operational surface exposes fiscal payload values.

---

## Final Phase: Cross-Cutting Validation and Documentation

**Purpose**: Satisfy release evidence and the constitutional Definition of Done without hiding unfinished story behavior in this phase.

- [ ] T068 [P] Verify the five Feature 003 English classifications and SRI-Adapter-Only element names are complete and non-duplicative in `docs/migration/terminology-mapping.md` (plan Terminology Mapping Impact; Constitution II)
- [ ] T069 Run the focused `*invoicexml*` tests plus Feature 001/002 regressions and record actual empty/V6-upgrade, contract, architecture, sensitive-data, and fail-closed results in `specs/003-generate-sri-invoice-xml/quickstart.md` (SC-002/SC-007–SC-017; Constitution XII/Definition of Done)
- [ ] T070 Build the packaged JVM and run `InvoiceXmlJvmSmokeIT`, then record exact resource, PostgreSQL round-trip, worker-isolation, first/replay, restart, invalid-XSD, and zero-side-effect evidence in `specs/003-generate-sri-invoice-xml/plan.md` (SC-001–SC-006/SC-014–SC-017; Constitution III/XII)
- [ ] T071 Run `InvoiceXmlJvmPerformanceIT` in a warmed packaged JVM and record maximum-source, 100-request convergence, ten-second deadline, event-loop, pool, worker, and queue recovery evidence in `specs/003-generate-sri-invoice-xml/quickstart.md` (SC-004–SC-005; R-009/R-015)
- [ ] T072 Record the evidence-based native deferral as unclaimed, including the future Mandrel/GraalVM 25 resource/catalog/JAXP/SHA-256/runtime proof required before any claim, in `specs/003-generate-sri-invoice-xml/plan.md` (R-018; Constitution III/Definition of Done)
- [ ] T073 Record actual approval/deployment evidence from the Fiscal Context Provider Owner and Feature 002 Contract Owner plus Platform Operations TLS/encryption/backup/restore/retention/disposal evidence in `specs/003-generate-sri-invoice-xml/plan.md`; leave this task open and block first-generation acceptance or production release while either evidence set is absent (spec Assumptions and Dependencies; plan Approved architecture and release dependencies)
- [ ] T074 Verify that no unresolved critical `$speckit-analyze` finding remains, then run `spotlessCheck`, static analysis, the full unit/integration suite, `quarkusBuild`, and the packaged JVM suites and record the final constitution/Definition-of-Done review in `specs/003-generate-sri-invoice-xml/plan.md` (Constitution XIV/Definition of Done)

---

## Dependencies & Execution Order

### Workflow Dependencies

- The constitution is approved on `main`, and the completed specification, clarifications, requirement checklist, plan, research, model, contracts, and quickstart are inputs to this file.
- `$speckit-analyze` MUST run after this file and before implementation; unresolved critical findings block `$speckit-implement`.
- Tests may use the proposed provider `2.0.0` synthetic contract, but T073 is a non-waivable acceptance/release gate and cannot be marked complete without actual accountable-owner evidence.

### Phase and Story Graph

```text
Phase 1 Setup
    -> Phase 2 Foundational Controls
        -> US1 Generate One Valid Artifact (MVP)
            -> US2 Exact Replay and Convergence
            -> US3 Fail-Closed Rejection
                -> Final Cross-Cutting Validation (after both US2 and US3)
```

- **Setup** has no feature-task dependency.
- **Foundational Controls** depends on Setup and blocks all user-story production work.
- **US1** depends on Foundational Controls because it requires committed profile evidence, V7, exact schema resources, and the bounded worker.
- **US2** depends on US1's committed artifact/store/API but is independently verified through replay, concurrency, and commit-uncertainty scenarios.
- **US3** depends on US1's endpoint/orchestration surfaces and the foundational profile/migration controls; after US1 it can proceed in parallel with US2 if shared-file ownership is coordinated.
- **Final Validation** depends on US1, US2, and US3 and cannot waive T073 or a critical analysis finding.

### Within-Phase Ordering

- In Phase 2, complete T007–T013 before T014–T020; T014 precedes T015–T018, while T019 and T020 can proceed independently after their red tests.
- In US1, T021 establishes shared synthetic builders, then T022–T029 can be authored in parallel. T030 precedes ports/models that reference artifact values; T031–T034 precede adapters/orchestration; T039 precedes T040; T040–T043 precede T044.
- In US2, T046–T050 precede production changes; T051 precedes repository recovery T052, which precedes application/API completion T053–T054.
- In US3, T055–T061 precede T062–T067. Coordinate edits to `GenerateUnsignedSriInvoiceXmlService.java`, `InvoiceXmlArtifactRepositoryAdapter.java`, and `InvoiceXmlExceptionMapper.java` with earlier story work.

## Parallel Opportunities

### User Story 1

- After T021, T022–T029 target distinct test/fixture boundaries and can run in parallel.
- After T030 and the required ports exist, T035 (StAX), T036 (JAXP), T037 (Base64), T038 (persistence mapping), T041 (request boundary), T042 (response mapping), T043 (telemetry/errors), and T045 (OpenAPI merge) are separable by file ownership.
- T039 and T040 remain sequential because orchestration depends on the repository contract and exact commit result.

### User Story 2

- T046–T050 can be authored in parallel against the US1 fixture/support baseline.
- Recovery classification T051 is isolated, but T052–T054 are sequential because repository winner selection feeds application result classification and HTTP status.

### User Story 3

- T055–T061 cover distinct application, API, persistence, privacy, architecture, and health evidence and can run in parallel.
- After the failure catalog is stable, T063–T067 can proceed in parallel when they do not edit the same shared class; T062 owns the service-level classification pass.

## Implementation Strategy

### MVP First

1. Complete Setup and Foundational Controls.
2. Complete all US1 evidence tasks before US1 production tasks.
3. Validate US1 independently with exact decode/hash/XSD/source assertions and zero excluded side effects.
4. Treat Setup + Foundation + US1 as the development MVP; do not call it accepted or releasable until T073 and the final JVM/DoD gates are complete.

### Incremental Delivery

1. Add US2 without changing the committed artifact representation: replay and followers must select persisted bytes rather than rebuild or substitute a candidate.
2. Add US3 by completing failure classification and boundary evidence without weakening any US1/US2 success invariant.
3. Finish packaged-JVM, performance, protected-storage, provider-approval, native-status, analysis, and constitutional evidence.

## Notes

- Historical files under `docs/legacy/` MUST NOT be edited.
- Company context is business partitioning only. Do not add authentication, authorization, a security scheme, `401`, `403`, Company lookup/master data, Company ports/clients/tables, cross-service FKs, caches, or replication.
- Feature 003 never calls the Fiscal Context provider, current catalogs, current Ecuador date, SRI, certificates, signatures, rendering, storage services, brokers, events, webhooks, email, or notifications.
- Official SRI XML names belong only in the infrastructure SRI adapter, official contracts/resources, and their tests; API, application, domain, and persistence terminology remains English.
- Every new concrete Java type is directly `@NullMarked`; mutable byte arrays are defensively copied; no broad suppression or package-level nullness default is allowed.
- A task is complete only with its observable evidence. Coverage-only tests, inferred approval, false zero-state claims, and unchecked completion are prohibited.
