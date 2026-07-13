# Planning Requirements Checklist: Create Invoice Draft

**Purpose**: Revalidate requirement and planning quality after the Constitution v2.0.0 definitive
Company-context amendment
**Created/Revalidated**: 2026-07-12
**Feature**: [`spec.md`](../spec.md), [`plan.md`](../plan.md), and affected design artifacts

`[x]` means the written artifacts objectively satisfy the criterion. It does not mean application
code exists or a runtime test has passed.

## Definitive Company Header Contract

- [x] CHK001 Is `X-Company-Id` mandatory for every Company-scoped operation and limited to exactly
  one nonblank, syntactically valid, non-nil UUID? [Spec §FR-001, Contract §CompanyContext]
- [x] CHK002 Are missing/blank values mapped to `COMPANY_CONTEXT_REQUIRED` and
  repeated/malformed/nil values mapped to `COMPANY_CONTEXT_INVALID`, with no persisted state?
  [Spec §FR-001, Contract §BadRequest]
- [x] CHK003 Is Company identifier prohibited from resource paths, query strings, request bodies,
  tokens, and sessions while a global/version prefix remains allowed? [Spec §FR-002]
- [x] CHK004 Does the target operation use an owned-resource path rather than Company as a parent
  resource? [Contract `POST /api/v1/invoice-drafts`]
- [x] CHK005 Is accepted UUID canonicalization optional at the API boundary and normalized before
  application use? [Spec §FR-001–FR-002, Plan §Company Header Contract]

## No Identity or Entitlement Model

- [x] CHK006 Do all artifacts exclude Keycloak, OIDC, OAuth, JWT, API keys, access tokens, sessions,
  principals, users, roles, permissions, tenant authorization, and service authentication?
  [Spec §FR-039, Plan §Internal Caller Boundary]
- [x] CHK007 Does OpenAPI contain no security scheme, security requirement, Authorization header,
  `401`, or `403` response? [Contract]
- [x] CHK008 Is Company context consistently described as opaque business metadata rather than an
  authentication/authorization credential or proof of entitlement? [Spec §FR-003, DR-018]
- [x] CHK009 Is the accepted risk explicit that any reachable process may submit any syntactically
  valid Company UUID and that upstream responsibilities are not verified? [Spec §Assumptions]

## No Company Dependency or Draft Snapshot

- [x] CHK010 Do all artifacts prohibit a Company port/client/repository/entity/adapter, lookup,
  eligibility or status check, cache, replica, direct database access, cross-service foreign key,
  repository, or transaction? [Spec §FR-036, Plan §Company Master-Data Boundary]
- [x] CHK011 Are Company failure, timeout, availability, and readiness outcomes absent from draft
  creation? [Spec §FR-003/FR-036, Plan §Liveness and Readiness]
- [x] CHK012 Is the emission-point identifier explicitly opaque and unverified rather than resolved
  to an Issuer or establishment? [Spec §FR-004, DR-023]
- [x] CHK013 Are Company-context version/observation time and Company/Issuer/establishment/emission
  fiscal snapshots excluded from draft data and response? [Spec §FR-005/FR-022/FR-038]
- [x] CHK014 Is authoritative fiscal-context resolution and snapshotting deferred to a separately
  approved fiscal-issuance specification? [Spec §FR-038]
- [x] CHK015 Has the obsolete Company-context port contract been removed? [Contracts directory]

## Ownership and Idempotency

- [x] CHK016 Does every draft store exactly one immutable normalized Company UUID as its external
  ownership partition and no tenant/Issuer alternative? [Spec §FR-037, Data Model §Invoice Draft]
- [x] CHK017 Do child records belong through local draft aggregate relationships without becoming
  independent Company master data? [Spec §DR-020, Data Model §Child Models]
- [x] CHK018 Is every repository query, mutation, and idempotency lookup scoped by Company UUID
  without describing that scoping as caller authorization? [Spec §FR-024]
- [x] CHK019 Is idempotency scope exactly Company UUID plus key, with persistence uniqueness on
  `company_id + idempotency_key_hash` and independent reuse across Companies? [Spec §FR-027/FR-030]
- [x] CHK020 Is Company UUID excluded from normalized business-content fingerprints while
  correlation and other transport-only headers are also excluded? [Spec §FR-029]
- [x] CHK021 Are equivalent replay, different-content conflict, concurrent one-winner behavior,
  rollback non-binding, response-loss recovery, and lifetime binding unambiguous? [Spec §FR-030–FR-033]
- [x] CHK022 Does an equivalent replay return the original draft without any Company call or
  external-data refresh? [Spec §FR-033]

## Fiscal and Monetary Preservation

- [x] CHK023 Are the prior deterministic `BigDecimal`, precision, line-level `HALF_UP`, aggregation,
  USD, and payment-reconciliation rules preserved? [Spec §DR-002–DR-011]
- [x] CHK024 Are configured percentage IVA, IVA 0%, not subject, and exempt preserved as exactly one
  active/effective line treatment with distinct zero-tax grouping? [Spec §FR-011, DR-005/DR-008]
- [x] CHK025 Are all supported buyer types and official effective-date validation rules preserved,
  including final-consumer exact values and threshold? [Spec §FR-007/FR-028, DR-014/DR-015]
- [x] CHK026 Are zero-value drafts, one zero payment, explicit tax selection, and no deemed-base
  inference preserved? [Spec §FR-013–FR-014, DR-016]
- [x] CHK027 Are calculated input fields rejected, local catalog states validated, and all
  no-SRI-side-effect rules preserved? [Spec §FR-011–FR-012/FR-023]

## Artifact Consistency and Evidence

- [x] CHK028 Do specification, plan, research, data model, OpenAPI, quickstart, terminology mapping,
  and templates use the same Company-header/no-auth/no-Company-dependency model? [All affected artifacts]
- [x] CHK029 Are stable API outcomes limited to locally possible request, idempotency, business,
  persistence, and internal failures rather than Company or security failures? [Plan §API and Error Contract]
- [x] CHK030 Are PostgreSQL/Flyway, header, Company-scoping, concurrency, sensitive-data, JVM, and
  optional-native evidence requirements objectively identified before implementation? [Plan §Test
  and Operational Evidence Plan]

## Revalidation Result

All 30 current-boundary planning-quality criteria pass. No clarification is needed because the
Company-context decisions were explicitly approved and declared non-reopenable. This checklist
does not authorize implementation; `$speckit-tasks`, `$speckit-analyze`, and `$speckit-implement`
remain separate workflow steps.
