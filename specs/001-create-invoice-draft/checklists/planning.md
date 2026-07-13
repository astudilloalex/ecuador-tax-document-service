# Pre-Task Requirements-Quality Checklist: Create Invoice Draft

**Purpose**: Validate the completeness, clarity, consistency, measurability, traceability, and
boundary coverage of the approved written requirements and planning evidence before task generation

**Created**: 2026-07-12

**Feature**: [`spec.md`](../spec.md) and [`plan.md`](../plan.md)

**Audience/Timing**: Formal peer-review gate after planning and before `$speckit-tasks`

**Depth**: Comprehensive, risk-focused review

**Note**: This checklist tests the quality of written requirements and planning artifacts. It is
not an implementation test, test-execution record, or evidence that code exists. An item may be
marked complete only when the cited artifacts objectively contain consistent, unambiguous evidence.

## API and Contract Completeness

- [ ] CHK001 Is the create operation documented consistently as `POST /invoice-drafts` beneath the
  approved API base/version, without treating Company as a parent resource? [Completeness,
  Evidence required: `spec.md` §In Scope/FR-002; `plan.md` §API and Error Contract; OpenAPI
  `servers` and `paths./invoice-drafts`]
- [ ] CHK002 Is CompanyId specified exclusively as the value derived from `X-Company-Id`, with no
  alternative source through path, query, body, token, or session? [Consistency, Evidence required:
  `spec.md` §FR-001–FR-003; `plan.md` §Company Header Contract; OpenAPI §CompanyContext]
- [ ] CHK003 Are the prohibition and strict rejection semantics for CompanyId in the path, query,
  and request body explicit and mutually consistent? [Coverage, Evidence required: `spec.md`
  §FR-002/Scenario 10; `plan.md` §API and Error Contract; OpenAPI §CreateInvoiceDraftRequest]
- [ ] CHK004 Are presence, single-value cardinality, whitespace trimming, nonblank content, UUID
  syntax, nil rejection, and lowercase-hyphenated normalization all specified without an undefined
  validation order? [Clarity, Evidence required: `spec.md` §FR-001–FR-002/FR-041; `research.md`
  §Company Context and API Shape; OpenAPI §CompanyContext]
- [ ] CHK005 Are `COMPANY_CONTEXT_REQUIRED` and `COMPANY_CONTEXT_INVALID` distinguished by complete,
  testable conditions for missing, blank, malformed, nil, and multiple header values? [Clarity,
  Evidence required: `spec.md` §Scenarios 6/47/48 and FR-001; `plan.md` §Company Header Contract;
  OpenAPI responses/`error-catalog.md` §Stable Outcomes]
- [ ] CHK006 Do all Company-header failure requirements explicitly require safe English errors,
  correlation evidence, and zero draft, child, and idempotency-binding state? [Completeness,
  Evidence required: `spec.md` §FR-001/SC-006; `quickstart.md` §Verify Company Header Validation;
  OpenAPI §BadRequest]
- [ ] CHK007 Are the optional `X-Correlation-Id` acceptance rules, generated-when-absent behavior,
  response propagation, invalid-input outcome, and idempotency exclusion specified consistently?
  [Consistency, Evidence required: `plan.md` §API and Error Contract; `research.md` §Correlation
  Contract; OpenAPI §CorrelationId; `quickstart.md` §Create a Valid Draft]
- [ ] CHK008 Are request and response fields, requiredness, strict unknown-property handling,
  decimal representations, calculated results, normalized CompanyId, status, and timestamps
  complete and consistent? [Completeness, Evidence required: `spec.md` §FR-007–FR-022; `plan.md`
  §API and Error Contract; OpenAPI §CreateInvoiceDraftRequest/InvoiceDraftResponse]

## Clean Architecture and Company Boundary

- [ ] CHK009 Is the written handoff explicit from API `X-Company-Id` parsing to an application
  `CompanyId` field and then to an immutable domain value on the Invoice Draft aggregate?
  [Traceability, Evidence required: `spec.md` §FR-037/FR-040; `plan.md` §Clean Architecture
  Mapping; `data-model.md` §Boundary Rules/Aggregate Root]
- [ ] CHK010 Are HTTP headers, container request objects, security identities, JWT objects,
  thread-local request state, and Gateway/BFF objects explicitly prohibited from application and
  domain interfaces? [Coverage, Evidence required: `spec.md` §FR-040; `plan.md` §Clean Architecture
  Mapping; `research.md` §Company Context and API Shape]
- [ ] CHK011 Is CompanyId described consistently as ownership/persistence partitioning and never as
  authentication, authorization, caller entitlement, or security isolation? [Consistency,
  Evidence required: `spec.md` §FR-024/DR-018/Key Entities; `plan.md` §Company Ownership Scoping;
  `data-model.md` §Aggregate Root]
- [ ] CHK012 Are all non-validations explicit—Company existence, active state, fiscal eligibility,
  user entitlement, tenant ownership, and Issuer/establishment/emission-point ownership—and is a
  syntactically valid externally unknown Company allowed? [Completeness, Evidence required:
  `spec.md` §FR-003/Scenario 35; `plan.md` §Company Master-Data Boundary; `quickstart.md` §Verify
  Company Header Validation]
- [ ] CHK013 Are Company lookup/client/port/adapter, status/eligibility policy, timeout, retry,
  health/readiness, cache, replica, and shared-persistence requirements all absent or explicitly
  prohibited rather than merely omitted? [Coverage, Evidence required: `spec.md` §FR-036;
  `plan.md` §Company Master-Data Boundary; `research.md` §Company Context and API Shape;
  `quickstart.md` §Negative Architecture Boundary]
- [ ] CHK014 Do the latency, availability, and readiness requirements exclude Company Service
  consistently and depend only on PostgreSQL plus mandatory local initialization? [Consistency,
  Evidence required: `plan.md` §Technical Context/Test and Operational Evidence; `research.md`
  §Health, Performance, and Runtime Evidence; `quickstart.md` §Verify Health Boundaries]

## Persistence and Aggregate Ownership

- [ ] CHK015 Is the Invoice Draft root documented with a non-null, non-nil PostgreSQL UUID
  `company_id`, immutable CompanyId domain meaning, and canonical response representation?
  [Completeness, Evidence required: `spec.md` §FR-022/FR-037; `data-model.md` §Aggregate Root;
  OpenAPI §InvoiceDraftResponse]
- [ ] CHK016 Are requirements explicit that existing-draft repository reads and mutations carry
  both CompanyId and local draftId, without presenting this as caller authorization? [Clarity,
  Evidence required: `spec.md` §FR-024/FR-037; `plan.md` §Company Ownership Scoping;
  `data-model.md` §Aggregate Root; `persistence-design.md` §Company-Scoped Reads and Mutations]
- [ ] CHK017 Are all line, line-tax, grouped-tax, payment, and additional-information relationships
  documented as local children owned through local foreign keys? [Completeness, Evidence required:
  `spec.md` §FR-020/DR-020; `data-model.md` §Child sections/Aggregate Relationships;
  `plan.md` §Persistence and Idempotency Design]
- [ ] CHK018 Are a local Company master table, Issuer/establishment/emission-point master tables,
  shared database structures, and cross-service foreign keys explicitly prohibited? [Coverage,
  Evidence required: `spec.md` §FR-036–FR-038; `data-model.md` §Explicitly Excluded Data;
  `quickstart.md` §Empty-Database Baseline]
- [ ] CHK019 Are `tenant_id`, authenticated subject, roles, authorization fields, Company-context
  version/time, and every Company/Issuer/establishment/emission-point fiscal snapshot consistently
  excluded from persistence? [Consistency, Evidence required: `spec.md` §FR-038–FR-039/SC-022;
  `data-model.md` §Explicitly Excluded Data; `plan.md` §Company Master-Data Boundary]
- [ ] CHK020 Is authoritative fiscal-context resolution and immutable snapshot creation clearly
  assigned to a later fiscal-issuance specification, without leaving draft-time ownership ambiguous?
  [Clarity, Evidence required: `spec.md` §FR-038/Key Entities; `plan.md` §Company Master-Data
  Boundary; `research.md` §Persistence Ownership and Constraints]

## Idempotency Requirement Quality

- [ ] CHK021 Is the binding schema complete for CompanyId, key hash, request fingerprint,
  normalization version, draft reference, and creation time, with uniqueness exactly
  `company_id + idempotency_key_hash`? [Completeness, Evidence required: `spec.md` §FR-027–FR-030;
  `data-model.md` §Local Idempotency Binding; `plan.md` §Persistence and Idempotency Design]
- [ ] CHK022 Are fingerprint inclusion rules complete for every client-controlled business field
  and explicit about line-order significance versus payment/additional-information order
  insensitivity? [Clarity, Evidence required: `spec.md` §FR-029/DR-021; `research.md`
  §Deterministic Request Fingerprinting; `idempotency-design.md` §Request Fingerprint]
- [ ] CHK023 Are CompanyId, idempotency key, correlation identifier, transport metadata, server
  results, and property ordering explicitly excluded from the request fingerprint for stated
  reasons? [Completeness, Evidence required: `spec.md` §FR-029; `plan.md` §Persistence and
  Idempotency Design; `idempotency-design.md` §Request Fingerprint]
- [ ] CHK024 Is the privacy decision to persist only cryptographic hashes plus normalization
  version—never the complete normalized buyer request—unambiguous across all planning artifacts?
  [Consistency, Evidence required: `plan.md` §Sensitive Data/Persistence and Idempotency Design;
  `research.md` §Deterministic Request Fingerprinting; `data-model.md` §Local Idempotency Binding]
- [ ] CHK025 Are equivalent replay, different-content conflict, response-loss recovery, binding
  lifetime, and current-header Company scoping specified as mutually consistent terminal outcomes?
  [Consistency, Evidence required: `spec.md` §FR-030–FR-033; `plan.md` §Data and External
  Consistency Design; `quickstart.md` §Verify Idempotency]
- [ ] CHK026 Are concurrent same-Company arbitration and same-key cross-Company independence
  measurable without relying on cache, application locks, or external coordination? [Measurability,
  Evidence required: `spec.md` §FR-031/Scenarios 22/25; `research.md` §Transaction and Concurrency
  Arbitration; `persistence-design.md` §Concurrency Arbitration; `quickstart.md` §Verify Idempotency]

## Fiscal Separation and Identity Exclusions

- [ ] CHK027 Are `issuerId`, Issuer RUC, legal/trade name, address, fiscal attributes,
  establishment data, and emission-point fiscal snapshots explicitly prohibited as create inputs?
  [Coverage, Evidence required: `spec.md` §FR-005/Scenarios 10/44; `plan.md` §API and Error
  Contract; OpenAPI §CreateInvoiceDraftRequest]
- [ ] CHK028 Are draft-time fiscal snapshots excluded consistently from request, response, domain,
  persistence, and idempotency content while later fiscal issuance remains explicitly deferred?
  [Consistency, Evidence required: `spec.md` §FR-005/FR-022/FR-038; `data-model.md` §Explicitly
  Excluded Data; OpenAPI request/response schemas; `research.md` §Persistence Ownership]
- [ ] CHK029 Are Keycloak, OIDC, OAuth, JWT, bearer/API tokens, authenticated principals, roles,
  permissions, and tenant-derived Company context absent or explicitly prohibited throughout the
  plan and supporting evidence? [Coverage, Evidence required: `spec.md` §FR-039; `plan.md`
  §Technical Context/Internal Caller Boundary; `quickstart.md` §Negative Architecture Boundary]
- [ ] CHK030 Does the OpenAPI contract contain no `securitySchemes`, document/operation security
  declaration, Authorization header, bearer token, `401`, or `403`, and is this absence required by
  the plan rather than accidental? [Consistency, Evidence required: `spec.md` §FR-039/SC-024;
  `plan.md` §API and Error Contract; OpenAPI document/components/responses]
- [ ] CHK031 Is the upstream Gateway/BFF assumption documented as an unenforced integration
  assumption while explicitly accepting that any reachable process can submit a syntactically
  valid Company UUID? [Assumption, Evidence required: `spec.md` §Assumptions and Dependencies;
  `plan.md` §Internal Caller Boundary/Company Header Contract; `research.md` §Company Context]

## Fiscal and Commercial Rule Completeness

- [ ] CHK032 Are exact decimal types, permitted precision, scales, line-level `HALF_UP` operation
  order, aggregation sources, USD currency, and reconciliation responsibility fully specified?
  [Completeness, Evidence required: `spec.md` §FR-012–FR-016/DR-002–DR-011; `data-model.md` monetary
  fields; `quickstart.md` §Fiscal and Boundary Vectors]
- [ ] CHK033 Are exactly one effective IVA rule per line, all four supported IVA treatments,
  distinct zero-tax grouping, and rejection of other/simultaneous taxes consistently specified?
  [Consistency, Evidence required: `spec.md` §FR-010–FR-011/DR-005/DR-008; `data-model.md` §Line Tax
  Selection/Grouped Tax Total; OpenAPI §TaxTreatment]
- [ ] CHK034 Are all supported buyer-identification rules, current Ecuadorian emission date,
  final-consumer exact values/threshold, and absence of online registry validation complete and
  traceable to official effective-date authority? [Completeness, Evidence required: `spec.md`
  §FR-006–FR-008/FR-028/DR-012/DR-014–DR-015; `research.md` §Fiscal and Monetary Rules;
  `quickstart.md` §Fiscal and Boundary Vectors]
- [ ] CHK035 Are zero-value drafts, explicit tax treatment, positive/zero payment cardinality,
  exact payment reconciliation, duplicate-method rejection, and negative-amount prohibition
  unambiguous? [Clarity, Evidence required: `spec.md` §FR-013–FR-014/DR-016/DR-022; `data-model.md`
  §Payment; `quickstart.md` §Fiscal and Boundary Vectors]
- [ ] CHK036 Are text normalization/limits, contact formats, collection maximums/order semantics,
  strict unknown fields, and caller-calculated-field rejection complete at every applicable
  boundary? [Coverage, Evidence required: `spec.md` §FR-008–FR-015/FR-035/FR-042; OpenAPI request
  schemas; `quickstart.md` §Strict Request Fields/Fiscal and Boundary Vectors]

## Failure, Sensitive Data, and Operational Evidence

- [ ] CHK037 Are the single-transaction persistence boundary, write-phase rollback, uniqueness-race
  loser recovery, pre-commit no-binding guarantee, and post-commit replay recovery specified without
  claiming external atomicity? [Completeness, Evidence required: `spec.md` §FR-020–FR-021/FR-032/
  FR-043; `plan.md` §Data and External Consistency Design; `persistence-design.md` §Aggregate Write
  Transaction/Concurrency Arbitration]
- [ ] CHK038 Are all stable error codes/statuses, FR-041 failure precedence, safe English Problem
  Details, correlation, retry semantics, and sensitive-data exclusions complete and mutually
  consistent? [Consistency, Evidence required: `spec.md` §FR-025–FR-026/FR-041–FR-043; `plan.md`
  §API and Error Contract; OpenAPI responses; `quickstart.md` §Rollback, Availability, and Timeout]
- [ ] CHK039 Are payload-size, typical/maximum/replay/conflict/concurrency performance, PostgreSQL
  unavailable/timeout, health, correlation, logs, metrics, traces, mandatory JVM, and optional
  native requirements quantified with environments and observable evidence? [Measurability,
  Evidence required: `plan.md` §Technical Context/Test and Operational Evidence/Native Evaluation;
  `research.md` §Health, Performance, and Runtime Evidence; `quickstart.md` §Performance/JVM;
  `operational-requirements.md`]
- [ ] CHK040 Does the traceability evidence cover every functional requirement and important
  primary/alternate/error/recovery/non-functional boundary, including explicit zero side effects
  for sequence, access key, XML, signature, certificate, SRI, PDF, queue, and notification work?
  [Traceability, Evidence required: `spec.md` §Acceptance Scenarios/FR-023/SC-005/SC-008;
  `plan.md` §Test and Operational Evidence Plan; `traceability.md`; `quickstart.md` §Negative
  Architecture Boundary]

## Reviewer Outcome

- Record evidence or a linked finding beside every item before changing its marker.
- `[x]` means the written requirements/planning evidence satisfy the question; it does not mean an
  implementation or runtime test passed.
- Any unresolved fiscal, Company-context, persistence, idempotency, sensitive-data, or
  requirement-to-scenario gap blocks `$speckit-tasks` until the authoritative artifact is corrected.
- Checklist markers MUST NOT be changed merely to unblock the workflow.
