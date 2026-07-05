# Tasks: Tax Document Issuance Foundation

**Input**: Design documents from
`specs/002-tax-document-issuance-foundation/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`,
`contracts/`, `quickstart.md`

**Tests**: Mandatory for every touched layer and risk area. Domain tests must
run without Quarkus. Application tests must run without real PostgreSQL, SRI,
Redis, filesystems, queues, webhooks, or external HTTP services.

**Scope Guard**: This feature creates only domain and application foundation
artifacts. Do not create REST endpoints, REST DTOs, persistence adapters, JPA or
Panache entities, SRI XML/SOAP adapters, database migrations, queue adapters,
webhook adapters, or bootstrap wiring.

**Organization**: Tasks are grouped by user story so each story can be
implemented and validated independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it touches different files and has no
  dependency on another unfinished task.
- **[Story]**: User story traceability from `spec.md`.
- Every task includes exact file paths.

## Path Conventions

- Domain source:
  `src/main/java/com/alexastudillo/taxdocument/domain/`
- Application source:
  `src/main/java/com/alexastudillo/taxdocument/application/`
- Domain tests:
  `src/test/java/com/alexastudillo/taxdocument/domain/`
- Application tests:
  `src/test/java/com/alexastudillo/taxdocument/application/`
- Durable architecture docs: `docs/architecture/`
- Durable migration docs: `docs/migration/`

---

## Phase 1: Setup (Shared Feature Structure)

**Purpose**: Establish the allowed package structure for this foundation
without creating adapters or bootstrap wiring.

- [ ] T001 Create root package boundary documentation in
  `src/main/java/com/alexastudillo/taxdocument/domain/package-info.java` and
  `src/main/java/com/alexastudillo/taxdocument/application/package-info.java`
  (AR-001, AR-002, AR-004).
- [ ] T002 [P] Create tax document domain package documentation in
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/package-info.java`
  (FR-001, AR-001, NR-001).
- [ ] T003 [P] Create application issuance and output port package
  documentation in
  `src/main/java/com/alexastudillo/taxdocument/application/issuance/package-info.java`
  and
  `src/main/java/com/alexastudillo/taxdocument/application/port/out/package-info.java`
  (FR-012, AR-002, AR-008).
- [ ] T004 [P] Create matching test package documentation in
  `src/test/java/com/alexastudillo/taxdocument/domain/taxdocument/package-info.java`
  and
  `src/test/java/com/alexastudillo/taxdocument/application/issuance/package-info.java`
  (FR-013, AR-004).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Add shared primitives needed before any user story implementation.

**Critical**: No user story implementation should begin until this phase is
complete.

- [ ] T005 [P] Create the domain exception foundation in
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/TaxDocumentException.java`,
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/InvalidAccessKeyException.java`,
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/InvalidStateTransitionException.java`,
  and
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/ImmutableAuthorizedDocumentException.java`
  (FR-004, FR-008, FR-009, AR-001).
- [ ] T006 [P] Create application issuance error support in
  `src/main/java/com/alexastudillo/taxdocument/application/issuance/IssuanceError.java`
  without REST, persistence, SRI, queue, or framework types (FR-014, FR-019,
  AR-002).

**Checkpoint**: Domain/application package boundaries and shared error
primitives exist, and no adapter or bootstrap package has been introduced.

---

## Phase 3: User Story 1 - Establish Common Issuance Language (Priority: P1)

**Goal**: Define the canonical English domain model shared by future issuance
features.

**Independent Test**: Domain tests verify document types, access key structure,
and issuance identity value objects without Quarkus or infrastructure.

### Tests for User Story 1

- [ ] T007 [P] [US1] Add document type support tests in
  `src/test/java/com/alexastudillo/taxdocument/domain/taxdocument/DocumentTypeTest.java`
  covering `INVOICE`, `CREDIT_NOTE`, `DEBIT_NOTE`, `WAYBILL`, and
  `WITHHOLDING` without using SRI numeric codes as internal names (FR-002,
  FR-003, NR-004).
- [ ] T008 [P] [US1] Add access key validation tests in
  `src/test/java/com/alexastudillo/taxdocument/domain/taxdocument/AccessKeyTest.java`
  covering exactly 49 digits, non-digit rejection, and length rejection
  (FR-004, FR-013).
- [ ] T009 [P] [US1] Add issuer numbering identity tests in
  `src/test/java/com/alexastudillo/taxdocument/domain/taxdocument/IssuerIssuanceIdentityTest.java`
  covering issuer, establishment, issuing point, and sequence number canonical
  names (FR-007, NR-001).

### Implementation for User Story 1

- [ ] T010 [US1] Implement the canonical document type model in
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/DocumentType.java`
  without package names, class names, or enum fields driven by SRI numeric codes
  (FR-002, FR-003, NR-004).
- [ ] T011 [US1] Implement the access key value object in
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/AccessKey.java`
  with 49-digit structural validation (FR-004, AR-001).
- [ ] T012 [P] [US1] Implement authorization and date value objects in
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/AuthorizationNumber.java`,
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/IssueDate.java`,
  and
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/AuthorizedAt.java`
  using Java standard types only (FR-001, FR-006, AR-001).
- [ ] T013 [P] [US1] Implement issuer context value objects in
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/Issuer.java`,
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/Establishment.java`,
  and
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/IssuingPoint.java`
  using English canonical terminology only (FR-007, NR-001, NR-002).
- [ ] T014 [P] [US1] Implement the sequence number value object in
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/SequenceNumber.java`
  with issuer, establishment, issuing point, and document type identity inputs
  (FR-007, FR-015).
- [ ] T015 [US1] Implement the initial tax document aggregate identity and
  required fields in
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/TaxDocument.java`
  without lifecycle behavior that depends on adapters or framework types
  (FR-001, FR-007, AR-001).

**Checkpoint**: User Story 1 is independently testable with domain unit tests
and establishes the canonical English foundation for common issuance concepts.

---

## Phase 4: User Story 2 - Define Issuance Lifecycle Rules (Priority: P2)

**Goal**: Define document states, authorization states, state transitions,
retry eligibility, local voiding, and authorized-document immutability.

**Independent Test**: Domain and application tests verify transitions, terminal
states, retry candidate handling, and local voiding without SRI or persistence.

### Tests for User Story 2

- [ ] T016 [P] [US2] Add document state transition tests in
  `src/test/java/com/alexastudillo/taxdocument/domain/taxdocument/DocumentStateTransitionTest.java`
  covering allowed transitions and rejected unspecified transitions (FR-005,
  FR-008).
- [ ] T017 [P] [US2] Add authorization state separation tests in
  `src/test/java/com/alexastudillo/taxdocument/domain/taxdocument/AuthorizationStateTest.java`
  verifying `AuthorizationState` remains separate from `DocumentState`
  (FR-006, AR-001).
- [ ] T018 [P] [US2] Add authorized-document immutability tests in
  `src/test/java/com/alexastudillo/taxdocument/domain/taxdocument/TaxDocumentImmutabilityTest.java`
  covering authorized terminal behavior (FR-009).
- [ ] T019 [P] [US2] Add local voiding rule tests in
  `src/test/java/com/alexastudillo/taxdocument/domain/taxdocument/LocalVoidingRuleTest.java`
  covering rejection of `AUTHORIZED` and `VOIDED` local voiding (FR-011).
- [ ] T020 [P] [US2] Add retry eligibility tests in
  `src/test/java/com/alexastudillo/taxdocument/application/issuance/RetryEligibilityPolicyTest.java`
  covering candidate states and PFV-ISS-004 deferral boundaries (FR-010,
  FR-017).

### Implementation for User Story 2

- [ ] T021 [US2] Implement canonical document states in
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/DocumentState.java`
  using the accepted English state names from `spec.md` (FR-005, NR-001).
- [ ] T022 [P] [US2] Implement authorization states in
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/AuthorizationState.java`
  without SRI XML, SOAP, or HTTP model dependencies (FR-006, AR-001).
- [ ] T023 [P] [US2] Implement issuance mode in
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/IssuanceMode.java`
  while keeping runtime sync/async defaults deferred to PFV-ISS-001 (FR-017).
- [ ] T024 [US2] Implement common lifecycle transition rules in
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/TaxDocumentLifecycle.java`
  for issuance, SRI reception, authorization, rejection, retry processing,
  terminal failure, and local voiding (FR-008, FR-011).
- [ ] T025 [US2] Extend
  `src/main/java/com/alexastudillo/taxdocument/domain/taxdocument/TaxDocument.java`
  with state transition behavior, authorized-document immutability, optional
  authorization number, and optional authorization timestamp (FR-006, FR-008,
  FR-009, FR-011).
- [ ] T026 [US2] Implement application retry eligibility policy in
  `src/main/java/com/alexastudillo/taxdocument/application/issuance/RetryEligibilityPolicy.java`
  for `RETURNED`, `REJECTED`, `PENDING`, and `IN_PROGRESS` while preserving
  PFV-ISS-004 for signed-XML and retry behavior validation (FR-010, FR-017,
  AR-002).

**Checkpoint**: User Story 2 is independently testable and rejects invalid
lifecycle, retry, and local voiding behavior without infrastructure.

---

## Phase 5: User Story 3 - Define Application Boundaries for Future Use Cases (Priority: P3)

**Goal**: Define application request/result models, audit event names, and
output ports for future use cases without adapter implementations.

**Independent Test**: Application tests verify model boundaries and port
signatures use only domain/application types and Java standard types.

### Tests for User Story 3

- [ ] T027 [P] [US3] Add issuance request model tests in
  `src/test/java/com/alexastudillo/taxdocument/application/issuance/IssuanceRequestTest.java`
  covering canonical fields and idempotency keys (FR-015, FR-019).
- [ ] T028 [P] [US3] Add issuance result model tests in
  `src/test/java/com/alexastudillo/taxdocument/application/issuance/IssuanceResultTest.java`
  covering access key, document state, authorization state, authorization
  details, outcome, audit correlation, and errors without adapter DTOs
  (FR-014, FR-019).
- [ ] T029 [P] [US3] Add audit event name tests in
  `src/test/java/com/alexastudillo/taxdocument/application/issuance/AuditEventNameTest.java`
  covering all canonical audit event names and secret-exclusion expectations
  (FR-016).
- [ ] T030 [P] [US3] Add application port boundary tests in
  `src/test/java/com/alexastudillo/taxdocument/application/port/out/ApplicationPortBoundaryTest.java`
  verifying port signatures avoid REST, persistence, SRI DTO, queue, storage,
  filesystem, HTTP client, Quarkus, Hibernate, and Panache types (FR-012,
  FR-014, AR-008).

### Implementation for User Story 3

- [ ] T031 [US3] Implement the application issuance request model in
  `src/main/java/com/alexastudillo/taxdocument/application/issuance/IssuanceRequest.java`
  with document type, issuer context, sequence information, issue date,
  issuance mode, external request id, and audit metadata using application and
  domain terms only (FR-014, FR-015, FR-019).
- [ ] T032 [US3] Implement the application issuance result model in
  `src/main/java/com/alexastudillo/taxdocument/application/issuance/IssuanceResult.java`
  without REST response DTOs, persistence entities, SRI DTOs, queue jobs,
  storage SDK responses, or framework results (FR-014, FR-019).
- [ ] T033 [P] [US3] Implement canonical audit event names in
  `src/main/java/com/alexastudillo/taxdocument/application/issuance/AuditEventName.java`
  for the ten events listed in `spec.md` (FR-016).
- [ ] T034 [P] [US3] Implement persistence, access, sequence, and access-key
  output ports in
  `src/main/java/com/alexastudillo/taxdocument/application/port/out/TaxDocumentRepository.java`,
  `src/main/java/com/alexastudillo/taxdocument/application/port/out/IssuerAccessPolicyPort.java`,
  `src/main/java/com/alexastudillo/taxdocument/application/port/out/SequenceNumberPort.java`,
  and
  `src/main/java/com/alexastudillo/taxdocument/application/port/out/AccessKeyGeneratorPort.java`
  using only domain/application types and Java standard types (FR-012, FR-014,
  AR-008).
- [ ] T035 [P] [US3] Implement SRI, XML storage, queue, and webhook output
  ports in
  `src/main/java/com/alexastudillo/taxdocument/application/port/out/SriAuthorizationPort.java`,
  `src/main/java/com/alexastudillo/taxdocument/application/port/out/XmlStoragePort.java`,
  `src/main/java/com/alexastudillo/taxdocument/application/port/out/TaxDocumentQueuePort.java`,
  and
  `src/main/java/com/alexastudillo/taxdocument/application/port/out/WebhookPublisherPort.java`
  without SRI XML/SOAP DTOs, filesystem paths as domain identifiers, queue SDK
  models, remote HTTP models, or adapter behavior (FR-012, FR-014, AR-008).
- [ ] T036 [P] [US3] Implement clock, transaction, and audit output ports in
  `src/main/java/com/alexastudillo/taxdocument/application/port/out/ClockPort.java`,
  `src/main/java/com/alexastudillo/taxdocument/application/port/out/TransactionPort.java`,
  and
  `src/main/java/com/alexastudillo/taxdocument/application/port/out/AuditLogPort.java`
  without framework transaction objects or sensitive audit metadata (FR-012,
  FR-016, AR-008).

**Checkpoint**: User Story 3 defines future use case boundaries through
application models and ports only, with no adapter implementation.

---

## Phase 6: User Story 4 - Preserve Migration Traceability (Priority: P4)

**Goal**: Update durable governance documents so future features consume this
foundation from stable architecture and migration locations.

**Independent Test**: Documentation review verifies every introduced concept,
state, SRI code, and unresolved behavior has a durable location and
classification.

### Documentation for User Story 4

- [ ] T037 [US4] Update canonical foundation terms in
  `docs/architecture/canonical-terminology.md` for `taxDocument`,
  `documentType`, `documentState`, `authorizationState`, `issuer`,
  `establishment`, `issuingPoint`, `sequenceNumber`, `accessKey`,
  `authorizationNumber`, `issueDate`, `authorizedAt`, `issuanceRequest`,
  `issuanceResult`, and `issuanceMode` (FR-018, NR-003).
- [ ] T038 [US4] Update common concept mappings in
  `docs/migration/legacy-to-target-terminology.md` for the legacy-to-target
  issuance terms used by this feature, with migration classifications for each
  mapping (FR-018, NR-005).
- [ ] T039 [US4] Update SRI document code mappings in
  `docs/migration/legacy-to-target-terminology.md` for `01`, `04`, `05`, `06`,
  and `07` as SRI adapter-only external contract concepts mapped to
  `INVOICE`, `CREDIT_NOTE`, `DEBIT_NOTE`, `WAYBILL`, and `WITHHOLDING`
  (FR-003, NR-004).
- [ ] T040 [US4] Update legacy state mappings in
  `docs/migration/legacy-to-target-terminology.md` for `PENDIENTE`,
  `EN_PROCESO`, `RECIBIDO`, `AUTORIZADO`, `NO_AUTORIZADO`, `DEVUELTA`,
  `RECHAZADO`, `IRRECUPERABLE`, and `ANULADO`, including the accepted
  `ANULADO` to `VOIDED` local-voiding rationale (FR-005, NR-006).
- [ ] T041 [US4] Update Pending Functional Validation records in
  `docs/migration/legacy-to-target-terminology.md` for PFV-ISS-001 through
  PFV-ISS-005 with affected work, required validation source, decision due, and
  resolution handling (FR-017, FR-018, NR-007).

**Checkpoint**: Durable documentation contains the foundation terminology,
state mappings, SRI external mappings, and pending validations required by
future specifications.

---

## Phase 7: Compliance, Validation, and Pull Request

**Purpose**: Verify constitution compliance, run the quickstart checks, and
prepare the GitHub pull request task requested by the user.

- [ ] T042 Run `./gradlew test` and record the outcome in
  `specs/002-tax-document-issuance-foundation/quickstart.md`, confirming domain
  and application tests run without Quarkus or external infrastructure
  (FR-013).
- [ ] T043 [P] Run source scope guard checks from
  `specs/002-tax-document-issuance-foundation/quickstart.md` and confirm no
  feature source exists under adapter, bootstrap, migration, REST, persistence,
  SRI, queue, or webhook packages (AR-004, AR-005, AR-006).
- [ ] T044 [P] Run terminology checks from
  `specs/002-tax-document-issuance-foundation/quickstart.md` against
  `src/main/java/com/alexastudillo/taxdocument/domain`,
  `src/main/java/com/alexastudillo/taxdocument/application`,
  `src/test/java/com/alexastudillo/taxdocument/domain`, and
  `src/test/java/com/alexastudillo/taxdocument/application`, confirming no
  Spanish legacy names appear in target source or tests (NR-001, NR-002).
- [ ] T045 [P] Verify source imports in
  `src/main/java/com/alexastudillo/taxdocument/domain` and
  `src/main/java/com/alexastudillo/taxdocument/application` do not reference
  Quarkus, Hibernate, Panache, JAX-RS, JSON serialization frameworks,
  PostgreSQL, Redis, XML/SOAP clients, filesystems for business behavior, HTTP
  clients, queue SDKs, storage SDKs, or adapter packages (AR-001, AR-002,
  FR-014).
- [ ] T046 [P] Verify durable documentation updates in
  `docs/architecture/canonical-terminology.md` and
  `docs/migration/legacy-to-target-terminology.md` cover canonical terms, SRI
  document code mappings, legacy state mappings, and PFV-ISS-001 through
  PFV-ISS-005 (FR-018, NR-005, NR-007).
- [ ] T047 [Process] Prepare a pull request summary in
  `specs/002-tax-document-issuance-foundation/pull-request.md` for reviewer
  handoff, including completed task groups, generated/modified files,
  validation results, deferred PFV items, and confirmation that the feature
  stayed within domain/application scope. This is a non-feature process task
  and does not create implementation artifacts. (Plan `Traceability and Source
  of Truth`, Quickstart `Validation Steps`, Quickstart `Post-Implementation
  Handoff`, Constitution `Specification-Governed Migration`, Constitution
  `Documentation and Compliance Workflow`)
- [ ] T048 [Process] Create or prepare a GitHub pull request from the feature
  branch to the target integration branch using the currently authenticated
  GitHub account and the prepared body from
  `specs/002-tax-document-issuance-foundation/pull-request.md`. If the
  authenticated account is `codex-bot`, the PR author may be `codex-bot`;
  otherwise do not impersonate `codex-bot`. Optionally assign or request review
  from `codex-bot` only if that repository user exists. This is a non-feature
  process task and does not modify domain/application behavior. (Plan
  `Traceability and Source of Truth`, Quickstart `Post-Implementation Handoff`,
  Constitution `Specification-Governed Migration`, Constitution `Documentation
  and Compliance Workflow`)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup** has no dependencies.
- **Phase 2: Foundational** depends on Phase 1 and blocks all user stories.
- **Phase 3: US1** depends on Phase 2.
- **Phase 4: US2** depends on Phase 2 and uses the `TaxDocument` aggregate
  introduced in US1 for full behavior.
- **Phase 5: US3** depends on Phase 2 and may use domain models from US1/US2.
- **Phase 6: US4** can start after Phase 2, but final documentation must
  reflect the implemented US1-US3 models.
- **Phase 7: Compliance, Validation, and Pull Request** depends on selected
  implementation and documentation tasks being complete.

### User Story Dependencies

- **US1 (P1)**: Establishes common domain language and can be validated first.
- **US2 (P2)**: Adds lifecycle behavior and depends on shared domain concepts.
- **US3 (P3)**: Adds application models and ports; it can progress in parallel
  with US2 after US1 domain names are stable.
- **US4 (P4)**: Preserves durable documentation and should be completed before
  final compliance validation.

### Within Each User Story

- Write tests before implementation tasks in that story.
- Implement domain behavior before application orchestration or ports that
  depend on domain types.
- Keep port signatures in the application layer and adapter-free.
- Update durable documentation before final quickstart validation.
- Do not add REST, persistence, SRI, queue, webhook, or bootstrap artifacts.

---

## Parallel Opportunities

- T002, T003, and T004 can run in parallel after T001.
- T005 and T006 can run in parallel because they touch different layers.
- T007, T008, and T009 can run in parallel.
- T012, T013, and T014 can run in parallel after T010 and T011 test targets are
  understood.
- T016 through T020 can run in parallel.
- T022 and T023 can run in parallel after T021.
- T027 through T030 can run in parallel.
- T034, T035, and T036 can run in parallel after T031 and T032 establish shared
  request/result terms.
- T037 through T041 can run in parallel if edits to
  `docs/migration/legacy-to-target-terminology.md` are coordinated to avoid
  same-section conflicts.
- T043 through T046 can run in parallel after T042 completes if validation
  output is coordinated.

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 for US1.
3. Run the US1 domain tests.
4. Confirm no adapter or bootstrap files were created.

### Incremental Delivery

1. Add US1 common language and value objects.
2. Add US2 lifecycle rules and retry eligibility.
3. Add US3 application request/result models and ports.
4. Add US4 durable documentation updates.
5. Run Phase 7 compliance checks.
6. Prepare and create the GitHub pull request with `codex-bot`.

### Validation Scope

- `./gradlew test`
- Quickstart scope guard checks.
- Quickstart terminology checks.
- Source import review for forbidden framework and adapter dependencies.
- Durable documentation review for canonical terms, mappings, and PFVs.

---

## Summary

- Total tasks: 48
- Setup tasks: 4
- Foundational tasks: 2
- User Story 1 tasks: 9
- User Story 2 tasks: 11
- User Story 3 tasks: 10
- User Story 4 tasks: 5
- Compliance and pull request tasks: 7
