# Pre-Task Requirements-Quality Checklist: Create Invoice Draft

**Purpose**: Validate completeness, clarity, consistency, measurability, evidence quality, and
traceability before task generation

**Created**: 2026-07-12

**Regenerated**: 2026-07-13 against Constitution v2.0.0 and the reconciled specification, plan,
contract, reference baseline, data model, and supporting design artifacts

**Feature**: [`spec.md`](../spec.md) and [`plan.md`](../plan.md)

**Audience/Timing**: Owner and peer reviewer; formal pre-task gate

**Note**: This checklist tests the written requirements and planning evidence. It is not an
implementation test. `[x]` means the cited documents objectively satisfy the criterion; it does
not assert that code, migrations, or runtime evidence already exist.

**Governance supersession**: The historical requirements-quality PASS did not satisfy or evidence
the later mandatory post-task `$speckit-analyze` gate. `governance-nonconformity.md` and
`GATE-GOV-001` now block T017 and later work; the checked items below do not constitute
retrospective owner approval.

## Governance and Authority

- [x] CHK001 Is Constitution v2.0.0 approved on the authoritative main branch as required before
  feature task generation? [Dependency, Governance]
  - Evidence: authoritative `main` and `origin/main` commit
    `137d1c8c59cc98402f0a1fed211a6caccad4c883` contains `.specify/memory/constitution.md` v2.0.0;
    `spec.md` is Approved for Task Generation; `plan.md` §Constitution Check records the approved
    constitutional baseline separately from the later workflow non-conformity.
- [x] CHK002 Is the source-authority order applied and are official facts distinguished from
  target-service decisions? [Consistency, Evidence]
  - Evidence: `spec.md` §Authority and Evidence/Source Conflicts;
    `reference-data-baseline.md` §Approval Scope/Authoritative Source Register.
- [x] CHK003 Are all official reference facts identified by publisher, document title, version or
  date, exact locator, and supported fact? [Completeness, Evidence]
  - Evidence: `reference-data-baseline.md` §Authoritative Source Register.
- [x] CHK004 Are legacy sources limited to historical discovery and excluded as target authority?
  [Consistency]
  - Evidence: `spec.md` §Authority and Evidence/Source Conflicts; `plan.md` §Source and Terminology
    Evidence.

## API and Company Context

- [x] CHK005 Is the synchronous operation specified consistently as
  `POST /api/v1/invoice-drafts`? [Consistency]
  - Evidence: `spec.md` §In Scope; `plan.md` §Summary/API and Error Contract; OpenAPI `servers` and
    `paths./invoice-drafts.post`.
- [x] CHK006 Is authoritative CompanyId input accepted exclusively through required
  `X-Company-Id`, with request-body/input/path/query/token/session/thread-local alternatives
  prohibited while explicitly contracted response CompanyId remains allowed? [Coverage]
  - Evidence: `spec.md` FR-001–FR-003/FR-040; OpenAPI `CompanyContext` and
    `CreateInvoiceDraftRequest`; `plan.md` §Company Context and Sensitive-Data Design.
- [x] CHK007 Are header presence, one-value cardinality, trimming, nonblank content, UUID syntax,
  nil rejection, and canonical lowercase-hyphenated normalization unambiguous? [Clarity]
  - Evidence: `spec.md` FR-001–FR-002/FR-041; OpenAPI `CompanyContext`; `error-catalog.md`
    §Stable Outcomes/Failure Precedence.
- [x] CHK008 Are missing, blank, malformed, nil, and multiple Company-header outcomes stable,
  correlated, and free of persistence side effects? [Completeness]
  - Evidence: `spec.md` Scenarios 6/47/48 and SC-006; `error-catalog.md` Company outcomes;
    `quickstart.md` §Verify Company Header Validation.
- [x] CHK009 Is CompanyId explicitly mapped API → application command → immutable aggregate value,
  without leaking HTTP abstractions below the API boundary? [Traceability]
  - Evidence: `spec.md` FR-037/FR-040; `plan.md` §Clean Architecture Mapping;
    `data-model.md` §Invoice Draft Aggregate Root/Boundary Rules.
- [x] CHK010 Does every aggregate/binding repository query or mutation enforce CompanyId as
  partitioning—not authorization—while global SRI reference catalogs remain unscoped? [Consistency]
  - Evidence: `spec.md` FR-024/DR-018/Key Entities; `plan.md` §Company Ownership Scoping;
    `docs/migration/terminology-mapping.md` Company entries.

## Absence of Identity and Company Integration

- [x] CHK011 Are authentication, authorization, Keycloak, OIDC, OAuth, JWT, API keys, principals,
  users, roles, permissions, and tenant-derived Company context explicitly outside the feature?
  [Coverage]
  - Evidence: `spec.md` Exclusions/FR-039; `plan.md` §Technical Context/Internal Caller Boundary;
    `quickstart.md` §Negative Architecture Boundary.
- [x] CHK012 Does the API contract explicitly omit security schemes, security requirements,
  Authorization headers, and 401/403 outcomes? [Consistency]
  - Evidence: `spec.md` FR-039/SC-024; `plan.md` §API and Error Contract; OpenAPI root,
    components, and response map; `mp.openapi.scan.disable=true` plus packaged `/q/openapi`
    semantic-equality evidence planned in T012/T040.
- [x] CHK013 Are Company existence, state, eligibility, caller entitlement, tenant ownership, and
  Company/Issuer/establishment/emission-point relationship checks explicitly absent? [Completeness]
  - Evidence: `spec.md` FR-003/FR-036/Scenario 35; `plan.md` §Company Master-Data Boundary;
    `quickstart.md` §Verify Company Header Validation.
- [x] CHK014 Are Company ports, clients, repositories, tables, cache, replication, timeouts,
  retries, health/readiness, shared persistence, and cross-service transactions prohibited?
  [Coverage]
  - Evidence: `spec.md` FR-036; `plan.md` §Company Master-Data Boundary;
    `persistence-design.md` §Responsibilities; `quickstart.md` §Negative Architecture Boundary.
- [x] CHK015 Is the upstream Gateway/BFF behavior documented as an unenforced assumption while the
  internal API accepts any syntactically valid Company UUID? [Assumption]
  - Evidence: `spec.md` §Assumptions and Dependencies; `plan.md` §Internal Caller Boundary.

## Request, Response, and Fiscal Separation

- [x] CHK016 Are request/response fields, requiredness, strict unknown-property handling,
  calculated outputs, CompanyId, status, currency, and timestamps complete? [Completeness]
  - Evidence: `spec.md` FR-004–FR-022; OpenAPI `CreateInvoiceDraftRequest` and
    `InvoiceDraftResponse`; `plan.md` §API and Error Contract.
- [x] CHK017 Are `issuerId`, Issuer attributes, Company/fiscal snapshots, establishment fiscal
  data, and emission-point fiscal snapshots prohibited from draft input and persistence?
  [Coverage]
  - Evidence: `spec.md` FR-005/FR-038/Scenarios 10/44; OpenAPI strict request schema;
    `data-model.md` §Explicitly Excluded Data.
- [x] CHK018 Is later fiscal-context resolution and immutable snapshot creation clearly deferred
  to a separate fiscal-issuance specification? [Clarity]
  - Evidence: `spec.md` FR-038/Key Entities; `plan.md` §Company Master-Data Boundary.
- [x] CHK019 Are official sequence, access key, XML, signature, certificate, SRI, PDF/RIDE, queue,
  and notification side effects excluded and covered by observable acceptance evidence? [Coverage]
  - Evidence: `spec.md` Exclusions/Scenario 8/FR-023/SC-005; `plan.md` §Test and Operational
    Evidence Plan; `quickstart.md` §Negative Architecture Boundary.

## Reference-Data Baseline

- [x] CHK020 Is the supported buyer-identification baseline complete for official codes 04–08,
  with labels, English names, executable ASCII repertoires, case/normalization rules, validation
  strategies, target validity, activity, version, source, and approval? [Completeness]
  - Evidence: `reference-data-baseline.md` §Approved Buyer-Identification Types.
- [x] CHK021 Are buyer strategies evidence-bounded and explicit that RUC/Cédula checksum and online
  registry/name verification are outside this feature? [Evidence, Clarity]
  - Evidence: `reference-data-baseline.md` buyer table and FORMAT_ONLY note; `spec.md` FR-028 and
    DR-014; OpenAPI `BuyerInput.identification`.
- [x] CHK022 Are final-consumer code, identifier, name, and USD 50.00 ceiling specified from an
  exact official source? [Traceability]
  - Evidence: `reference-data-baseline.md` buyer code 07 and source register `SRI-FT-2.32` §9.10;
    `spec.md` DR-015.
- [x] CHK023 Is the initial IVA baseline complete for 0%, 5%, 13%, 15%, not-subject, and exempt,
  with official codes, treatments, rates, applicability notes, validity, and approval? [Completeness]
  - Evidence: `reference-data-baseline.md` §Approved IVA Tax Rules.
- [x] CHK024 Is the distinction between official IVA representation/applicability evidence and
  target activity/English naming explicit? [Consistency]
  - Evidence: `reference-data-baseline.md` §Approval Scope/Approved IVA Tax Rules;
    `research.md` §Reference-Data Baseline Evidence Gate.
- [x] CHK025 Is tax selection assigned to the upstream billing workflow while service-side product
  classification and legal eligibility inference are explicitly excluded? [Clarity]
  - Evidence: `spec.md` FR-011/Scenario 40; `plan.md` §Source Conflicts and Resolutions; OpenAPI
    operation and `taxRuleId` descriptions; `quickstart.md` §Fiscal, Monetary, and Boundary Vectors.
- [x] CHK026 Are historical/unsupported IVA 12%, 14%, and differential rows explicitly omitted
  from initial seed planning with reasons? [Coverage]
  - Evidence: `reference-data-baseline.md` §Excluded Initial Rows; `spec.md` FR-047.
- [x] CHK027 Is the payment baseline complete for official codes 01, 15–21, with exact labels,
  English names, source dates, open end, target validity, activity, and approval? [Completeness]
  - Evidence: `reference-data-baseline.md` §Approved Payment Methods.
- [x] CHK028 Is the deterministic UUIDv5 namespace, tax/payment name formula, immutability rule,
  and no-runtime-generation rule exact and independently reproducible? [Measurability]
  - Evidence: `reference-data-baseline.md` §Target UUID Namespace and Derivation/Immutability;
    `research.md` §Reference-Data Baseline Evidence Gate; `spec.md` FR-046.
- [x] CHK029 Do every OpenAPI/quickstart reference UUID and planned fixture identifier correspond
  to exactly one approved baseline row? [Consistency]
  - Evidence: OpenAPI `InvoiceLineInput.taxRuleId`/`PaymentInput.paymentMethodId` examples;
    `quickstart.md` §Approved Reference Inputs; `reference-data-baseline.md` approved tables.
- [x] CHK030 Are local catalog fields, constraints, uniqueness, source/target effective dates,
  Flyway ownership, and verification requirements exact and aligned? [Consistency]
  - Evidence: `data-model.md` §Local Reference Catalogs; `persistence-design.md` §Flyway Migration
    Design/Exact Local Catalog Structures; `reference-data-baseline.md` §Flyway Ownership.

## Monetary, Date, Text, and Collection Rules

- [x] CHK031 Are quantity, unit-price, monetary, and percentage envelopes identical across spec,
  OpenAPI, data model, persistence, errors, quickstart, and planned vectors? [Consistency]
  - Evidence: `spec.md` FR-010/FR-044/DR-010; OpenAPI decimal schemas; `data-model.md` §Numeric
    Storage Boundary; `error-catalog.md` §Numeric Envelope; `quickstart.md` numeric vectors.
- [x] CHK032 Are exact BigDecimal arithmetic, line-level HALF_UP points, scale, USD currency,
  aggregation, and reconciliation ownership unambiguous? [Clarity]
  - Evidence: `spec.md` FR-012–FR-016/DR-002–DR-011; `plan.md` §Technical Context;
    `data-model.md` monetary fields.
- [x] CHK033 Are one active/effective `family=IVA` rule per line, four treatment classes, separate
  zero-tax grouping, and non-IVA/multiple-tax rejection complete without inventing a parent
  tax-category entity? [Coverage]
  - Evidence: `spec.md` FR-010–FR-011/DR-005/DR-008; OpenAPI `TaxTreatment`;
    `data-model.md` §Invoice Line Tax Selection/Grouped Tax Total; `persistence-design.md` exact IVA
    catalog structure.
- [x] CHK034 Are zero-value drafts, explicit tax selection, exactly one zero payment, positive
  payment rules including the eight-method maximum, exact reconciliation, and duplicate-method
  rejection complete? [Coverage]
  - Evidence: `spec.md` FR-013–FR-014/DR-016/DR-022; `data-model.md` §Payment;
    `reference-data-baseline.md` eight approved payment methods; `quickstart.md` §Fiscal, Monetary,
    and Boundary Vectors.
- [x] CHK035 Are one-request-instant, America/Guayaquil current-date, midnight crossing, separate
  once-only in-transaction `createdAt`, rollback/non-physical-commit semantics, and replay-date/
  timestamp semantics complete? [Clarity]
  - Evidence: `spec.md` FR-006/DR-012; `plan.md` §Time Boundary;
    `operational-requirements.md` §Measurement Boundary; `quickstart.md` dynamic date.
- [x] CHK036 Are text trimming, exact product/passport/foreign ASCII patterns, case/normalization,
  limits, contact formats, control-character rejection, collection maxima, uniqueness, and order
  semantics complete? [Coverage]
  - Evidence: `spec.md` FR-008–FR-010/FR-013/FR-015/FR-035/DR-019/DR-021; OpenAPI request schemas;
    `quickstart.md` §Strict Request Fields/Boundary Vectors.
- [x] CHK037 Is every caller-supplied calculated field rejected consistently rather than ignored,
  compared, or persisted? [Consistency]
  - Evidence: `spec.md` FR-012/DR-011/Scenario 28; `error-catalog.md` §Recognized Prohibited
    Calculated Fields; OpenAPI strict request schemas.

## Persistence, Idempotency, and Failure Semantics

- [x] CHK038 Is Idempotency-Key mandatory/exactly single-valued with stable missing/invalid/multiple
  outcomes and one-time normalization, then scoped exactly by CompanyId plus key hash with
  `UNIQUE (company_id, idempotency_key_hash)`? [Clarity]
  - Evidence: `spec.md` FR-027–FR-030; `data-model.md` §Local Idempotency Binding;
    `persistence-design.md` §Concurrency Arbitration.
- [x] CHK039 Are normalized fingerprint inclusion, exclusions, order semantics, version, and
  personal-data minimization complete? [Completeness]
  - Evidence: `spec.md` FR-029; `idempotency-design.md` §Request Fingerprint;
    `research.md` §Deterministic Request Fingerprinting.
- [x] CHK040 Are replay, conflict, concurrent winner, cross-Company independence, binding
  lifetime, response loss, and later-date replay mutually consistent? [Consistency]
  - Evidence: `spec.md` FR-030–FR-033/DR-017–DR-018; `idempotency-design.md` replay/concurrency
    sections; `quickstart.md` §Verify Idempotency.
- [x] CHK041 Are aggregate, all children, and idempotency binding required to commit atomically,
  with every pre-commit failure rolling back fully? [Completeness]
  - Evidence: `spec.md` FR-020–FR-021/FR-032/FR-043; `persistence-design.md` §Aggregate Write
    Transaction/Concurrency Arbitration.
- [x] CHK042 Are stable errors, HTTP statuses, retry actions, persistence guarantees, and the exact
  12-stage failure precedence aligned? [Consistency]
  - Evidence: `spec.md` FR-025/FR-041–FR-044; `error-catalog.md` §Stable Outcomes/Failure
    Precedence; `plan.md` §Failure-Precedence Ownership ordered upload/pre-entity/entity pipeline;
    OpenAPI responses; T030–T032/T081–T082/T084–T087.

## Sensitive Data, Correlation, Operations, and Runtime

- [x] CHK043 Are buyer data, raw idempotency keys, normalized requests, SQL/internals, and unsafe
  correlation values excluded from logs, metrics, traces, safe errors, and unnecessary storage?
  [Coverage]
  - Evidence: `spec.md` FR-025/SC-002; `plan.md` §Sensitive Data; `error-catalog.md` preamble;
    `idempotency-design.md` storage rules; `operational-requirements.md` observability sections.
- [x] CHK044 Are correlation generation, preservation, invalid replacement, non-echo, precedence,
  response propagation, and idempotency exclusion complete? [Completeness]
  - Evidence: `spec.md` FR-026/FR-041/DR-024; OpenAPI `CorrelationId`; `error-catalog.md`;
    `quickstart.md` §Verify Correlation Initialization and Precedence.
- [x] CHK045 Are liveness/readiness, structured observability, PostgreSQL-only dependency health,
  and bounded metric labels defined without Company or SRI checks? [Coverage]
  - Evidence: `plan.md` §Test and Operational Evidence Plan; `operational-requirements.md`
    §Availability and Health/Observability; `quickstart.md` §Verify Health Boundaries.
- [x] CHK046 Are payload, latency, concurrency, timeout, resource, and PostgreSQL failure budgets
  measurable under a defined environment? [Measurability]
  - Evidence: `plan.md` §Technical Context; `operational-requirements.md` §Measurement Boundary/
    Request Performance Budgets/Capacity; `quickstart.md` §Operational and Performance Evidence.
- [x] CHK047 Are mandatory JVM evidence and optional native build-plus-runtime evidence defined
  without making native success mandatory? [Clarity]
  - Evidence: `plan.md` §Native Compatibility Evaluation/Test Evidence; `research.md` §Runtime
    Baseline; `quickstart.md` §JVM and Optional Native Evidence.

## Traceability and Bounded Outcome

- [x] CHK048 Does traceability explicitly cover every current FR, DR, SC, acceptance scenario,
  stable error, and prohibited side-effect boundary? [Traceability]
  - Evidence: `traceability.md` §Functional Requirements/Domain Rules/Success Criteria/Acceptance
    Scenario Coverage/Stable Error Coverage/Cross-Cutting Constitutional Evidence.

## Gate Result

- Total items: 48
- Checked items: 48
- Unchecked items: 0
- Remaining requirements-quality items: 0
- Remaining implementation-governance conditions: 4 unchecked conditions in `GATE-GOV-001`

**Historical requirements-quality result**: PASS — the written requirements have no remaining
material quality gap.

**Current implementation progression**: **BLOCKED before T017** by `GATE-GOV-001`. The real
retrospective review and owner approval remain outstanding; this checklist does not release the
gate.
