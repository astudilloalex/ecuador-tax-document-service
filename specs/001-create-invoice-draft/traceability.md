# Traceability Matrix: Create Invoice Draft

This matrix maps every approved functional requirement, domain rule, and measurable outcome to
design evidence and an observable verification target. It does not create implementation tasks.

**Planning blocker**: `PFV-001`, `PFV-002`, and `PFV-003` remain unresolved. Therefore `FR-045`
through `FR-047`, `DR-001`, `SC-031`, and `SC-032` are not satisfied, `$speckit-tasks` remains
blocked, and catalog-dependent API examples and fixtures MUST NOT invent identifiers or rules.

## Functional Requirements

| ID | Design and contract evidence | Acceptance / success evidence | Planned verification |
|----|------------------------------|-------------------------------|----------------------|
| `FR-001` | OpenAPI `X-Company-Id`; `error-catalog.md` Company errors | Scenarios 1, 6, 47, 48; `SC-006` | API presence, blank, repeated, malformed, nil, safe-correlation, and zero-state tests |
| `FR-002` | OpenAPI operation/header/body; `plan.md` API boundary | Scenarios 1, 10, 34, 47, 48; `SC-024`, `SC-025` | Contract test proves canonical UUID and no Company path/query/body field |
| `FR-003` | `plan.md` Company boundary; no Company port contract | Scenarios 35, 41, 43; `SC-023` | Valid externally unknown UUID succeeds; architecture test proves zero lookup |
| `FR-004` | OpenAPI `emissionPointId`; `data-model.md` root | Scenarios 1, 34; `SC-007`, `SC-022` | UUID syntax/canonicalization, persistence, response, and no ownership lookup |
| `FR-005` | Strict OpenAPI request schema; `error-catalog.md` | Scenarios 10, 44; `SC-022`, `SC-025` | Reject Issuer/establishment/emission fiscal and snapshot properties |
| `FR-006` | `plan.md` clock mapping; `data-model.md` temporal model | Scenarios 26, 27, 51, 52; `SC-013`, `SC-030` | One captured `requestCreationInstant`, Guayaquil conversion, midnight and replay tests |
| `FR-007` | Approved identification baseline required by `research.md` | Scenarios 7, 9, 13–16, 53; `SC-010`, `SC-031` | Official effective-date vectors after `PFV-001` is resolved |
| `FR-008` | OpenAPI buyer/contact limits; `data-model.md` buyer fields | Scenarios 31–33; `SC-016` | Address/email/telephone boundary and invalid-format tests |
| `FR-009` | OpenAPI line cardinality; `data-model.md` aggregate | Scenarios 4, 29, 30; `SC-015` | 1/500 accepted; 0/501 rejected without state |
| `FR-010` | OpenAPI line numeric/text bounds; `data-model.md` decimals | Scenarios 2, 3, 31, 32, 49, 50; `SC-004`, `SC-016`, `SC-029` | Quantity/price/discount precision, range, text, and overflow vectors |
| `FR-011` | Tax-rule request reference; approved baseline gate | Scenarios 9, 12, 40, 53, 54; `SC-009`, `SC-021`, `SC-031`, `SC-032` | Effective IVA-only selection after `PFV-002`; reject direct code/rate and multiple tax |
| `FR-012` | Strict request schema; calculation model; error catalog | Scenarios 2, 28, 49, 50; `SC-003`, `SC-004`, `SC-014`, `SC-029` | Pure calculation, prohibited-field, intermediate/grouped overflow, and no-state tests |
| `FR-013` | Payment schema/model and local uniqueness constraint | Scenarios 5, 17–19, 29, 30, 39, 50, 53, 54; `SC-011`, `SC-020`, `SC-029`, `SC-032` | Positive/zero payment, cardinality, duplicate method, range, and baseline UUID tests |
| `FR-014` | Calculation/reconciliation design | Scenarios 5, 17–19, 50; `SC-003`, `SC-011`, `SC-029` | Exact two-decimal equality and payment-sum overflow tests |
| `FR-015` | OpenAPI additional-information schema; data constraints | Scenarios 29–33; `SC-015`, `SC-016` | 0/15 accepted, 16 rejected, trimmed-name uniqueness and text tests |
| `FR-016` | `data-model.md` fixed `USD`; OpenAPI response currency | Scenarios 1, 2, 17; `SC-001`, `SC-007` | Every created/replayed representation and persisted root reports exactly `USD` |
| `FR-017` | Draft state model; OpenAPI `DRAFT` enum | Scenarios 1, 8; `SC-001`, `SC-005`, `SC-007` | Persist/return only internal `DRAFT`; never expose it as SRI status |
| `FR-018` | Local draft identifier model | Scenarios 1, 8; `SC-001`, `SC-005` | Unique local IDs; absence of sequence/access-key/authorization-number fields |
| `FR-019` | `data-model.md` `timestamptz` fields and clock design | Scenarios 1, 51, 52; `SC-001`, `SC-007`, `SC-013`, `SC-030` | Commit-instant `createdAt`, ordered timestamps, midnight and replay evidence |
| `FR-020` | `persistence-design.md` single reactive transaction | Scenarios 1, 11, 22, 46; `SC-001`, `SC-002`, `SC-012`, `SC-028` | Real PostgreSQL aggregate-plus-binding commit and injected rollback tests |
| `FR-021` | Persistence failure model; local foreign keys | All rejection scenarios; `SC-002`, `SC-028`, `SC-029` | Row-count assertions after contract, business, catalog, timeout, and rollback failures |
| `FR-022` | OpenAPI created/replay response; `data-model.md` aggregate | Scenarios 1, 2, 17, 34; `SC-001`, `SC-007`, `SC-022` | Field-by-field response/persistence contract including CompanyId, USD, totals and timestamps; no snapshots |
| `FR-023` | `plan.md` exclusions and negative architecture boundary | Scenarios 8, 41, 43; `SC-005`, `SC-008`, `SC-023` | Dependency, trace, persistence, and side-effect inspection proves zero fiscal/SRI action |
| `FR-024` | Repository port contract and `persistence-design.md` scoping | Scenarios 20–25, 34; `SC-012`, `SC-017` | Every existing-draft/binding query takes CompanyId plus local identifier; cross-Company tests |
| `FR-025` | `error-catalog.md`; observability redaction rules | All failure scenarios; `SC-002`, `SC-006`, `SC-028`, `SC-033` | Stable English codes/messages and sensitive/internal-data leakage tests |
| `FR-026` | OpenAPI correlation header; `operational-requirements.md` | Scenarios 55–58; `SC-033` | Absent/valid/blank/repeated/65-char/unsafe/combined-failure correlation vectors |
| `FR-027` | OpenAPI idempotency header; `idempotency-design.md` | Scenarios 20–25, 32; `SC-012`, `SC-016`, `SC-018` | Trim/ASCII/1–128 tests and Company-scope independence |
| `FR-028` | Identification validation boundary | Scenarios 7, 13–16; `SC-010` | Official syntax/checksum vectors; zero online registry/name lookup |
| `FR-029` | `idempotency-design.md` canonical content version 1 | Scenarios 20, 21, 25, 37, 38, 52, 56; `SC-012`, `SC-019`, `SC-030`, `SC-033` | Golden fingerprints for property/order/text/decimal/correlation/Company exclusions |
| `FR-030` | Binding schema and `UNIQUE (company_id, idempotency_key_hash)` | Scenarios 20–25, 36; `SC-012`, `SC-018` | New/replay/conflict/lifetime and atomic binding tests |
| `FR-031` | PostgreSQL uniqueness arbitration design | Scenarios 22, 25; `SC-012`, `SC-017` | 50-way same-scope concurrency and cross-Company independence |
| `FR-032` | Transaction and retry design | Scenarios 11, 23, 24, 46; `SC-002`, `SC-012`, `SC-028` | Pre-commit rollback leaves no binding; post-commit response loss replays |
| `FR-033` | Replay flow in `idempotency-design.md` | Scenarios 20, 24, 36, 42, 52; `SC-018`, `SC-023`, `SC-030` | Local scoped lookup only; no recalc/date/Company/auth/external refresh |
| `FR-034` | Local binding repository in persistence design | Scenarios 20–25, 43; `SC-012`, `SC-023` | Architecture evidence proves no delegated idempotency service |
| `FR-035` | Text canonicalization design and API limits | Scenarios 31–33, 37; `SC-016`, `SC-019` | Trim-before-validation/persistence/fingerprint, blank and control-character tests |
| `FR-036` | Negative architecture inventory | Scenarios 35, 41–43; `SC-023` | Source/dependency/config/schema/health inspection proves no Company dependency |
| `FR-037` | Aggregate/persistence model immutable Company UUID | Scenarios 1, 20–25, 34; `SC-017`, `SC-022` | Root immutability, local child ownership, Company-scoped repository tests |
| `FR-038` | Draft-vs-issuance model exclusions | Scenarios 8, 41, 42, 44; `SC-005`, `SC-022`, `SC-023`, `SC-025` | Schema/response/dependency inspection proves no snapshot or issuance data |
| `FR-039` | OpenAPI and build/config negative boundary | Scenario 43; `SC-023`, `SC-024` | Zero security scheme/requirement/Auth/401/403/dependency/config tests |
| `FR-040` | Clean Architecture mapping in `plan.md` | Scenarios 34, 43; `SC-017`, `SC-023` | API maps to application `CompanyId`; dependency tests reject HTTP/security context below API |
| `FR-041` | `error-catalog.md` precedence; application flow | Scenarios 45, 57, 58; `SC-026`, `SC-033` | Combined-failure table covers all 12 ordered stages and safe correlation initialization |
| `FR-042` | HTTP body limit; OpenAPI 413 | Scenario 45; `SC-002`, `SC-027` | Exact 2 MiB proceeds; 2 MiB+1 rejects before Company/correlation validation with safe correlation |
| `FR-043` | Persistence/error/timeout design | Scenarios 24, 46; `SC-028` | 503/504/500 injection, zero pre-commit state, and post-commit replay recovery |
| `FR-044` | OpenAPI decimal constraints; domain/data numeric envelopes | Scenarios 49, 50; `SC-029` | API/domain/intermediate/group/payment/persistence/response boundary and overflow vectors |
| `FR-045` | Required `reference-data-baseline.md` (not yet approved) | Scenario 53; `SC-031` | Pre-task evidence audit of every row and metadata field; currently blocked by `PFV-001`–`PFV-003` |
| `FR-046` | OpenAPI published UUIDs plus Flyway reference baseline | Scenario 54; `SC-032` | Contract-to-seed UUID equality and no startup generation/catalog-query tests; currently blocked |
| `FR-047` | PFV registry in `spec.md`; authority rules | Scenario 53; `SC-031`, `SC-032` | Pre-task gate fails on any missing source/mapping/rate/validity/UUID; currently failing by design |

## Domain Rules and Invariants

| ID | Design and data evidence | Acceptance / success evidence | Planned verification |
|----|--------------------------|-------------------------------|----------------------|
| `DR-001` | Required approved reference-data baseline and Flyway seeds | Scenarios 9, 13–16, 40, 53, 54; `SC-009`, `SC-010`, `SC-021`, `SC-031`, `SC-032` | Official-source/metadata/UUID audit; blocked until all three PFVs resolve |
| `DR-002` | Calculation pipeline | Scenario 2; `SC-003`, `SC-004` | Exact multiplication and upper-bound intermediate tests |
| `DR-003` | Calculation pipeline | Scenarios 2, 3; `SC-003`, `SC-004` | Rounded gross less two-decimal discount vectors |
| `DR-004` | Line invariant | Scenario 3; `SC-002` | Discount equal to gross accepted; greater rejected without state |
| `DR-005` | IVA calculation/treatment model | Scenarios 2, 12, 17, 40; `SC-004`, `SC-009`, `SC-021` | Percentage and three distinct zero-tax treatment vectors |
| `DR-006` | Aggregate totals model | Scenarios 2, 49, 50; `SC-003`, `SC-029` | Sum of rounded line net amounts and overflow tests |
| `DR-007` | Aggregate totals model | Scenarios 2, 49, 50; `SC-003`, `SC-029` | Sum of two-decimal discounts and overflow tests |
| `DR-008` | Grouped tax-total model | Scenarios 2, 12, 17, 49, 50; `SC-003`, `SC-009`, `SC-029` | Group by code+rate; keep zero treatments separate; grouped overflow |
| `DR-009` | Aggregate totals model | Scenarios 2, 17, 49, 50; `SC-003`, `SC-004`, `SC-011`, `SC-029` | Subtotal plus grouped taxes, zero and overflow vectors |
| `DR-010` | BigDecimal and PostgreSQL numeric design | Scenarios 2, 49, 50; `SC-003`, `SC-004`, `SC-029` | Precision/scale/HALF_UP/envelope vectors in domain, API, persistence and JVM/native |
| `DR-011` | Calculation ownership and strict input contract | Scenarios 2, 5, 28, 40; `SC-003`, `SC-014`, `SC-021` | Caller supplies commercial/payment inputs only; every calculated/code/rate input rejected, never reconciled |
| `DR-012` | Single request clock and temporal model | Scenarios 26, 27, 51, 52; `SC-013`, `SC-030` | One-instant Guayaquil conversion, midnight commit, commit timestamp, later replay |
| `DR-013` | Validation/error/transaction boundaries | Scenarios 3–7, 9, 12–16, 27, 39, 40, 50; `SC-002`, `SC-009`, `SC-010`, `SC-029` | Impossible/inconsistent/inactive/unsupported/local-relation failures reject without normalization or state |
| `DR-014` | Official buyer validation strategy | Scenarios 7, 13, 14, 16; `SC-010` | Cédula/RUC/passport/foreign official vectors; no invented rules |
| `DR-015` | Final-consumer rule | Scenario 15; `SC-010` | Exact code/value/name and effective USD threshold boundaries |
| `DR-016` | Zero-value and tax-treatment rules | Scenarios 17–19; `SC-011` | Zero lines/totals/payment accepted only in approved shape; no automatic IVA 0% |
| `DR-017` | Durable idempotency binding | Scenarios 20–24, 36; `SC-012`, `SC-018` | Commit-only binding, conflict, response-loss replay, no time expiry |
| `DR-018` | Company+key scope | Scenarios 20–25; `SC-012`, `SC-017` | Same/different Company tests; neither header nor key treated as credential |
| `DR-019` | Text normalization/fingerprint design | Scenarios 31–33, 37; `SC-016`, `SC-019` | Trim before length, format, uniqueness, persistence and equivalence; no silent truncation |
| `DR-020` | Root Company ownership and local foreign keys | Scenarios 1, 22, 25, 34; `SC-001`, `SC-017`, `SC-022` | Exactly one immutable CompanyId and zero cross-draft/cross-Company child mixing |
| `DR-021` | Canonical collection ordering | Scenarios 37, 38; `SC-019` | Lines order-sensitive; payments/additional information order-insensitive |
| `DR-022` | Payment uniqueness constraint | Scenario 39; `SC-020` | One payment-method identity per draft at domain and PostgreSQL levels |
| `DR-023` | Opaque emission-point data model | Scenarios 1, 34, 35, 41, 44; `SC-001`, `SC-007`, `SC-022`, `SC-025` | Store/return canonical opaque ID only; no ownership/state lookup or fiscal representation |
| `DR-024` | API correlation mapping and observability design | Scenarios 55–58; `SC-033` | Generate/preserve/replace safely, never echo invalid input, exclude from fingerprint |

## Success Criteria

| ID | Requirements and design traced | Observable evidence |
|----|--------------------------------|---------------------|
| `SC-001` | `FR-016`–`FR-022`, `FR-037`; aggregate, response and transaction designs | Every logically new valid vector commits exactly one complete USD `DRAFT` and returns every `FR-022` field |
| `SC-002` | `FR-001`, `FR-020`, `FR-021`, `FR-032`, `FR-042`–`FR-044` | Row-count assertions show zero root/child/binding state for every rejected/pre-commit failure vector |
| `SC-003` | `FR-012`, `FR-014`; `DR-002`–`DR-010` | Golden calculations identical across repetitions, persistence, packaged JVM, and claimed native runtime |
| `SC-004` | `DR-002`–`DR-005`, `DR-009`, `DR-010` | `2 × 10.00 − 5.00` at 15% always yields `20.00/15.00/2.25/17.25` |
| `SC-005` | `FR-017`, `FR-018`, `FR-023`, `FR-038` | Dependency/trace/storage audit records zero issuance/SRI/PDF/notification effects |
| `SC-006` | `FR-001`, `FR-002`, `FR-025`, `FR-026` | Complete Company-header matrix returns correct safe code/correlation and zero state |
| `SC-007` | `FR-004`, `FR-016`–`FR-019`, `FR-022`; `DR-020`, `DR-023` | Accepted/replayed representation lets client review CompanyId, opaque emissionPointId, buyer, lines, taxes, payments, USD totals, DRAFT and timestamps without issuance |
| `SC-008` | `FR-017`, `FR-023`, `FR-038` | Acceptance suite runs without update/delete/issuance/XML/signature/SRI/PDF/notification facilities |
| `SC-009` | `FR-010`, `FR-011`; `DR-001`, `DR-005`, `DR-008` | Exactly one effective IVA rule per accepted line; unsupported/multiple rejected without state |
| `SC-010` | `FR-007`, `FR-028`; `DR-001`, `DR-014`, `DR-015` | All approved type vectors pass/fail from official rules with zero online lookup |
| `SC-011` | `FR-013`, `FR-014`; `DR-016` | Valid zero-total draft has one `0.00` payment; all other zero-payment shapes reject |
| `SC-012` | `FR-024`, `FR-027`–`FR-034`; `DR-017`, `DR-018` | New/replay/conflict/failure/50-way concurrency commits at most one per Company+key scope |
| `SC-013` | `FR-006`, `FR-019`; `DR-012` | Captured-instant date, midnight crossing, different/impossible date vectors |
| `SC-014` | `FR-012`; `DR-011` | Every calculated input rejected consistently even when equal to computed value |
| `SC-015` | `FR-009`, `FR-013`, `FR-015` | Exact collection maxima accepted; maxima+1 reject without state |
| `SC-016` | `FR-008`, `FR-010`, `FR-015`, `FR-027`, `FR-035`; `DR-019` | Exact text boundaries accepted; invalid/over/blank/control/contact/duplicate vectors reject |
| `SC-017` | `FR-024`, `FR-037`; `DR-020` | One immutable canonical CompanyId and local child ownership; zero cross-Company mixing |
| `SC-018` | `FR-030`, `FR-033`; `DR-017` | Equivalent replay returns original for draft lifetime with no elapsed-time expiry |
| `SC-019` | `FR-029`; `DR-019`, `DR-021` | Payment/additional reorder replays; line reorder conflicts; no duplicate draft |
| `SC-020` | `FR-013`; `DR-022` | Accepted drafts have unique payment methods; duplicate method rejects with zero state |
| `SC-021` | `FR-011`; `DR-001`, `DR-011` | Tax code/rate comes only from selected approved rule; caller-supplied code/rate rejects |
| `SC-022` | `FR-004`, `FR-005`, `FR-037`, `FR-038`; `DR-020`, `DR-023` | Exactly CompanyId+opaque emissionPointId and zero Company/fiscal snapshot fields |
| `SC-023` | `FR-003`, `FR-023`, `FR-033`, `FR-036`, `FR-039`, `FR-040` | Create/replay traces and architecture show zero Company/auth/cache/replication behavior |
| `SC-024` | `FR-001`–`FR-003`, `FR-039` | OpenAPI static test finds header-only Company context and zero security/Auth/401/403 constructs |
| `SC-025` | `FR-002`, `FR-005`, `FR-012` | Strict body tests reject Company/Issuer/fiscal snapshot and calculated fields with zero state |
| `SC-026` | `FR-041`; error precedence design | Pairwise/multi-failure suite proves earliest outcome and no execution of later stages |
| `SC-027` | `FR-042` | Exact byte-boundary tests: ≤2 MiB continues; >2 MiB returns 413 before Company evaluation |
| `SC-028` | `FR-032`, `FR-043` | 503/504/500 pre-commit failures leave zero state; post-commit loss replays original |
| `SC-029` | `FR-010`, `FR-012`–`FR-014`, `FR-044`; `DR-010` | Same numeric envelope at API/domain/intermediate/group/payment/database/response; every breach gives `MONETARY_RANGE_EXCEEDED` |
| `SC-030` | `FR-006`, `FR-033`; `DR-012`, `DR-017` | Later-date equivalent replay returns original date without validation or mutation |
| `SC-031` | `FR-045`, `FR-047`; `DR-001` | Pre-task audit has zero unverified reference rows; currently blocked by `PFV-001`–`PFV-003` |
| `SC-032` | `FR-046`, `FR-047`; `DR-001` | Contract UUIDs equal Flyway seed UUIDs; zero startup generation/catalog-query operations; currently blocked |
| `SC-033` | `FR-026`, `FR-029`, `FR-041`; `DR-024` | Correlation absent/valid/invalid/combined-failure vectors plus fingerprint invariance |

## Cross-Cutting Constitutional Evidence

| Risk | Governing source | Planned evidence | Explicit prohibition |
|------|------------------|------------------|----------------------|
| Flyway and local constraints | Constitution IX and Definition of Done | Empty-database migration and constraint tests after reference baselines are approved | No auto-generation, manual schema, legacy dump, or cross-service foreign key |
| Health and observability | Constitution XIII | `operational-requirements.md` liveness/readiness/log/metric/trace tests | No Company/identity/SRI readiness and no sensitive/high-cardinality labels |
| JVM/native evidence | Constitution III/XII | Mandatory packaged JVM suite; native build plus runtime only if claimed | No native claim from build alone |
| Performance and reactive safety | Constitution V/XIII | Documented reference environment, latency profiles, 50-way concurrency, blocked-thread evidence | No Company latency, cache, blocking wait, or hidden blocking wrapper |
