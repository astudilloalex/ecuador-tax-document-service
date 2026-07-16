# Traceability Matrix: Create Invoice Draft

This matrix maps every approved functional requirement, domain rule, and measurable outcome to
design evidence and an observable verification target. It does not create implementation tasks.

**Reference-data status**: Complete. `reference-data-baseline.md` approves 5 buyer rows, 6 IVA
rows, and 8 payment rows under `SRI-OFFLINE-2.32-TARGET-1`, using deterministic UUIDv5 namespace
`32576bbf-b70d-5c24-98ff-d5f9b48e8826`. Constitution v2.0.0 is approved on `main`, and the
reconciled specification and plan are subject to active `GATE-GOV-001`. T017 and later work remain
blocked pending the T001–T016 retrospective review, deviation disposition, explicit owner approval,
and a new analysis with no related CRITICAL finding.

## Scenario Timing Model

All request-processing evidence in this matrix uses one of these normative timing classes:

1. **Non-deadline scenario precondition**: unless a scenario explicitly tests deadline
   arbitration, its controlled request deadline remains unexpired until the tested stage
   conclusively selects the described terminal outcome. This includes success, Company/correlation
   header validation, replay/conflict, business validation, calculation, and persistence outcomes
   conclusively known before expiry.
2. **Deadline-race behavior**: deadline-focused evidence independently controls stage completion
   and deadline expiry; the first conclusively selected terminal outcome wins and cannot be
   replaced by a later signal.
3. **Deadline-first behavior**: when expiry occurs before another outcome is conclusively selected,
   FR-041 requires `504 REQUEST_TIMEOUT`.

The non-deadline precondition isolates the business behavior under test and never weakens or
overrides FR-041.

## Governance and Completed-Task Traceability

| Record/task | Exact responsibility | Governing evidence | Current status |
|-------------|----------------------|--------------------|----------------|
| `GOV-001` / `GATE-GOV-001` | Retrospective review of T001–T016, deviation disposition, explicit owner approval, and clean related analysis before T017 | `governance-nonconformity.md`; `tasks.md` gate; commits `1289871`, `8bdd548`, `5e5452a` | BLOCKING; owner approval not recorded |
| `T013` | Versioned reference-catalog structures, evidence/validity fields, stable-identifier columns, and constraints | `FR-045`, `FR-046`, `FR-047`; `SC-031`, `SC-032`; `reference-data-baseline.md`; V1 retrospective evidence | Historically complete; conformity review pending under `GOV-001` |
| `T014` | Exact approved baseline rows, fixed UUIDs, exclusion of unsupported rows, and verification queries | `FR-045`, `FR-046`, `FR-047`; `SC-031`, `SC-032`; `reference-data-baseline.md`; V2 retrospective evidence | Historically complete; conformity review pending under `GOV-001` |

The requirement references above match the catalog structure/seed responsibilities: FR-045
governs required row metadata, FR-046 governs fixed published UUID storage/seeding, FR-047 governs
no invented or unsupported rows, SC-031 requires a fully evidenced baseline, and SC-032 requires
stable UUID and no-runtime-generation evidence.

## Functional Requirements

| ID | Design and contract evidence | Acceptance / success evidence | Planned verification |
|----|------------------------------|-------------------------------|----------------------|
| `FR-001` | OpenAPI `X-Company-Id`; `error-catalog.md` Company errors | Scenarios 1, 6, 47, 48; `SC-006`; non-deadline outcomes require conclusive classification before expiry | T029/T032 presence, blank, repeated, malformed, nil, safe-correlation, and zero-state tests prove stage-first `400`; T032 separately proves deadline-first `504` and that a selected `400` is not replaced |
| `FR-002` | OpenAPI operation/header/request-input/response; `plan.md` API boundary | Scenarios 1, 10, 34, 47, 48; `SC-024`, `SC-025` | Contract tests prove Company identifiers absent from request bodies/input/path/query, authoritative header-only input, and explicitly required canonical response CompanyId |
| `FR-003` | `plan.md` Company boundary; no Company port contract | Scenarios 35, 41, 43; `SC-023` | Valid externally unknown UUID succeeds; architecture test proves zero lookup |
| `FR-004` | OpenAPI `emissionPointId`; `data-model.md` root | Scenarios 1, 34; `SC-007`, `SC-022` | UUID syntax/canonicalization, persistence, response, and no ownership lookup |
| `FR-005` | Strict OpenAPI request schema; `error-catalog.md` | Scenarios 10, 44; `SC-022`, `SC-025` | Reject Issuer/establishment/emission fiscal and snapshot properties |
| `FR-006` | `plan.md` request-clock mapping; `data-model.md` temporal model | Scenarios 26, 27, 51, 52; `SC-013`, `SC-030` | One captured `requestCreationInstant`, Guayaquil conversion, midnight and replay tests, separate from transactional `createdAt` |
| `FR-007` | Approved identification baseline and exact ASCII rule in `reference-data-baseline.md` | Scenarios 7, 9, 13–16, 53; `SC-010`, `SC-031` | Exact FORMAT_ONLY/final-consumer vectors; code 06/08 case-sensitive `^[A-Za-z0-9]{1,20}$` across API/domain/PostgreSQL tests |
| `FR-008` | OpenAPI buyer/contact limits; `data-model.md` buyer fields | Scenarios 31–33; `SC-016` | Address/email/telephone boundary and invalid-format tests |
| `FR-009` | OpenAPI line cardinality; `data-model.md` aggregate | Scenarios 4, 29, 30; `SC-015` | 1/500 accepted; 0/501 rejected without state |
| `FR-010` | OpenAPI line numeric/text bounds; `data-model.md` decimals and exact product repertoire | Scenarios 2, 3, 31, 32, 49, 50; `SC-004`, `SC-016`, `SC-029` | Quantity/price/discount precision/range plus case-sensitive ASCII `^[A-Za-z0-9]{1,25}$` product vectors at API/domain/PostgreSQL |
| `FR-011` | OpenAPI `taxRuleId`; approved IVA baseline and upstream-selection boundary | Scenarios 9, 12, 40, 53, 54; `SC-009`, `SC-021`, `SC-031`, `SC-032` | Active/effective immutable `family=IVA` rule selection, zero parent-category entity or automatic classification, direct code/rate and multiple-tax rejection |
| `FR-012` | Strict request schema; calculation model; error catalog | Scenarios 2, 28, 49, 50; `SC-003`, `SC-004`, `SC-014`, `SC-029` | Pure calculation, prohibited-field, intermediate/grouped overflow, and no-state tests |
| `FR-013` | Payment schema/model and local uniqueness constraint | Scenarios 5, 17–19, 29, 30, 39, 50, 53, 54; `SC-011`, `SC-020`, `SC-029`, `SC-032` | Positive/zero payment, cardinality, duplicate method, range, and baseline UUID tests |
| `FR-014` | Calculation/reconciliation design | Scenarios 5, 17–19, 50; `SC-003`, `SC-011`, `SC-029` | Exact two-decimal equality and payment-sum overflow tests |
| `FR-015` | OpenAPI additional-information schema; data constraints | Scenarios 29–33; `SC-015`, `SC-016` | 0/15 accepted, 16 rejected, trimmed-name uniqueness and text tests |
| `FR-016` | `data-model.md` fixed `USD`; OpenAPI response currency | Scenarios 1, 2, 17; `SC-001`, `SC-007` | Every created/replayed representation and persisted root reports exactly `USD` |
| `FR-017` | Draft state model; OpenAPI `DRAFT` enum | Scenarios 1, 8; `SC-001`, `SC-005`, `SC-007` | Persist/return only internal `DRAFT`; never expose it as SRI status |
| `FR-018` | Local draft identifier model | Scenarios 1, 8; `SC-001`, `SC-005` | Unique local IDs; absence of sequence/access-key/authorization-number fields |
| `FR-019` | `data-model.md` `timestamptz` fields; injectable clock and transaction design | Scenarios 1, 51, 52, 61; `SC-001`, `SC-007`, `SC-012`, `SC-013`, `SC-030` | Capture once after validation/immediately before persistence; same Instant persisted/returned/replayed; rollback non-exposure; no physical commit timestamp/post-commit query/`track_commit_timestamp` |
| `FR-020` | `persistence-design.md` single reactive transaction | Scenarios 1, 11, 22, 46; `SC-001`, `SC-002`, `SC-012`, `SC-028` | Real PostgreSQL aggregate-plus-binding commit, injected confirmed rollback, and unresolved-commit replay tests |
| `FR-021` | Persistence failure and deadline model; local foreign keys | Confirmed pre-commit rejection and rollback scenarios; `SC-002`, `SC-028`, `SC-029` | Row-count assertions only after confirmed pre-persistence/rollback outcomes; uncertain/post-commit vectors assert replay and no duplicate instead |
| `FR-022` | OpenAPI created/replay response; `data-model.md` aggregate | Scenarios 1, 2, 17, 34; `SC-001`, `SC-007`, `SC-022` | Field-by-field response/persistence contract including CompanyId, USD, totals and timestamps; no snapshots |
| `FR-023` | `plan.md` exclusions and negative architecture boundary | Scenarios 8, 41, 43; `SC-005`, `SC-008`, `SC-023` | Dependency, trace, persistence, and side-effect inspection proves zero fiscal/SRI action |
| `FR-024` | Repository port contract and `persistence-design.md` aggregate/binding scoping | Scenarios 20–25, 34; `SC-012`, `SC-017` | Every aggregate/binding query or mutation enforces authoritative CompanyId; cross-Company tests; global catalogs prove no Company filter/column |
| `FR-025` | `error-catalog.md`; observability redaction rules | All failure scenarios; `SC-002`, `SC-006`, `SC-028`, `SC-033` | Stable English codes/messages and sensitive/internal-data leakage tests |
| `FR-026` | OpenAPI correlation header; shared API correlation classifier; `operational-requirements.md` | Scenarios 55–58; `SC-033` | Absent/valid/blank/repeated/65-char/unsafe vectors plus oversized bodies with absent, valid, and invalid correlation prove safe 413 propagation without stage-3 takeover |
| `FR-027` | OpenAPI mandatory single-value header; three stable errors; `idempotency-design.md` | Scenarios 20–25, 32, 59–61; `SC-012`, `SC-016`, `SC-018` | T028/T031/T080 prove missing/blank/whitespace/repeated/parser-multiple/comma/over-length/grammar behavior, one-time SP/HTAB trim, no first-value selection, and identical normalized lookup/hash/persistence value; no domain HTTP tests |
| `FR-028` | Identification validation boundary and approved FORMAT_ONLY strategies | Scenarios 7, 13–16; `SC-010` | Exact syntax/length/special-value vectors; zero checksum, online registry, or name lookup |
| `FR-029` | `idempotency-design.md` canonical content version 1 | Scenarios 20, 21, 25, 37, 38, 52, 56; `SC-012`, `SC-019`, `SC-030`, `SC-033` | Golden fingerprints for property/order/text/decimal/correlation/Company exclusions |
| `FR-030` | Binding schema and `UNIQUE (company_id, idempotency_key_hash)` | Scenarios 20–25, 36; `SC-012`, `SC-018` | New/replay/conflict/lifetime and atomic binding tests |
| `FR-031` | PostgreSQL uniqueness arbitration design | Scenarios 22, 25; `SC-012`, `SC-017` | 50-way same-scope concurrency and cross-Company independence |
| `FR-032` | Transaction and retry design | Scenarios 11, 23, 24, 46; `SC-002`, `SC-012`, `SC-028` | Pre-commit rollback leaves no binding; post-commit response loss replays |
| `FR-033` | Replay flow in `idempotency-design.md` | Scenarios 20, 24, 36, 42, 52; `SC-018`, `SC-023`, `SC-030` | Local scoped lookup only; no recalc/date/Company/auth/external refresh |
| `FR-034` | Local binding repository in persistence design | Scenarios 20–25, 43; `SC-012`, `SC-023` | Architecture evidence proves no delegated idempotency service |
| `FR-035` | Text canonicalization design and API limits | Scenarios 31–33, 37; `SC-016`, `SC-019` | Trim-before-validation/persistence/fingerprint, blank and control-character tests |
| `FR-036` | Negative architecture inventory | Scenarios 35, 41–43; `SC-023` | Source/dependency/config/schema/health inspection proves no Company dependency |
| `FR-037` | Aggregate/persistence model immutable Company UUID | Scenarios 1, 20–25, 34; `SC-017`, `SC-022` | Root immutability, local child ownership, Company enforcement for aggregate/binding operations, and unscoped global-catalog tests |
| `FR-038` | Draft-vs-issuance model exclusions | Scenarios 8, 41, 42, 44; `SC-005`, `SC-022`, `SC-023`, `SC-025` | Schema/response/dependency inspection proves no snapshot or issuance data |
| `FR-039` | OpenAPI and build/config negative boundary | Scenario 43; `SC-023`, `SC-024` | Zero security scheme/requirement/Auth/401/403/dependency/config tests |
| `FR-040` | Clean Architecture mapping in `plan.md` | Scenarios 34, 43; `SC-017`, `SC-023` | API maps to application `CompanyId`; dependency tests reject HTTP/security context below API |
| `FR-041` | `error-catalog.md` ordered stages plus cross-cutting deadline; shared API header classifiers; atomic terminal-outcome state; global Scenario Timing Assumption | Scenarios 45–46, 57–61; `SC-012`, `SC-026`, `SC-033` | T031/T032 configure stage-first exact idempotency errors and no-first-value behavior plus controlled deadline-first header races; later stages never start after terminal selection |
| `FR-042` | Quarkus HTTP upload limit and exclusive feature 413 failure handler; OpenAPI 413 | Scenario 45; `SC-002`, `SC-027`, `SC-033` | Exact 2 MiB proceeds; over-limit-first Content-Length/chunked bodies preserve valid correlation or replace absent/invalid input, never emit correlation `400`, perform no database operation, and return 413; deadline-first bodies return 504 |
| `FR-043` | Earliest-route monotonic deadline owner; aggregate/reference remaining-budget propagation; persistence/error/timeout and post-response-commit design | Scenarios 24, 45–46; `SC-002`, `SC-012`, `SC-026`, `SC-028` | Controlled deadline races, both DB-timeout minimum branches, exhausted reference budget with no query, confirmed rollback zero state, unresolved-commit replay, and post-response-commit telemetry-only/no-second-write evidence |
| `FR-044` | OpenAPI decimal constraints; domain/data numeric envelopes | Scenarios 49, 50; `SC-029` | API/domain/intermediate/group/payment/persistence/response boundary and overflow vectors |
| `FR-045` | Approved `reference-data-baseline.md` row tables and source register | Scenario 53; `SC-031` | T013 structures retain required evidence metadata; T014 seeds/verifies every approved row; audit proves official facts, target decisions, validity, activity, version, source, and approval |
| `FR-046` | Published UUIDv5 namespace/names; OpenAPI and quickstart exact IDs | Scenario 54; `SC-032` | T013 provides stable-ID structures; T014 seeds/verifies fixed UUIDs; independently recalculate all 14 UUIDs and prove contract-to-seed equality/no runtime generation/query |
| `FR-047` | Baseline excluded-row table and governance rules | Scenario 53; `SC-031`, `SC-032` | T013 constraints and T014 verification omit unsupported/unapproved rows; any later unevidenced row blocks its own introduction |

## Domain Rules and Invariants

| ID | Design and data evidence | Acceptance / success evidence | Planned verification |
|----|--------------------------|-------------------------------|----------------------|
| `DR-001` | Approved reference-data baseline and planned Flyway ownership | Scenarios 9, 13–16, 40, 53, 54; `SC-009`, `SC-010`, `SC-021`, `SC-031`, `SC-032` | Official-fact/target-decision audit, exact row counts, UUID recalculation, and later empty-database seed verification |
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
| `DR-012` | Separate request-date and transaction-createdAt clock semantics | Scenarios 26, 27, 51, 52, 61; `SC-012`, `SC-013`, `SC-030` | One request Instant/Guayaquil date; one transactional createdAt after validation/before persistence; same persisted/returned/replayed value; rollback non-exposure; no physical commit timestamp |
| `DR-013` | Validation/error/transaction boundaries | Scenarios 3–7, 9, 12–16, 27, 39, 40, 50; `SC-002`, `SC-009`, `SC-010`, `SC-029` | Impossible/inconsistent/inactive/unsupported/local-relation failures reject without normalization or state |
| `DR-014` | Approved executable buyer FORMAT_ONLY strategies | Scenarios 7, 13, 14, 16; `SC-010` | RUC 13 ASCII digits, Cédula 10 ASCII digits, passport/foreign case-sensitive ASCII `^[A-Za-z0-9]{1,20}$` valid/invalid/normalization vectors; no checksum or invented rules |
| `DR-015` | Final-consumer rule | Scenario 15; `SC-010` | Exact code/value/name and effective USD threshold boundaries |
| `DR-016` | Zero-value and tax-treatment rules | Scenarios 17–19; `SC-011` | Zero lines/totals/payment accepted only in approved shape; no automatic IVA 0% |
| `DR-017` | Durable idempotency binding | Scenarios 20–24, 36; `SC-012`, `SC-018` | Commit-only binding, conflict, response-loss replay, no time expiry |
| `DR-018` | Company+key scope | Scenarios 20–25; `SC-012`, `SC-017` | Same/different Company tests; neither header nor key treated as credential |
| `DR-019` | Field-specific text normalization/fingerprint design | Scenarios 31–33, 37, 61; `SC-012`, `SC-016`, `SC-019` | Exact product/buyer SP/HTAB trim and ASCII/case rules; Idempotency-Key remains API transport normalization, not a domain text rule |
| `DR-020` | Root Company ownership and local foreign keys | Scenarios 1, 22, 25, 34; `SC-001`, `SC-017`, `SC-022` | Exactly one immutable CompanyId and zero cross-draft/cross-Company child mixing |
| `DR-021` | Canonical collection ordering | Scenarios 37, 38; `SC-019` | Lines order-sensitive; payments/additional information order-insensitive |
| `DR-022` | Payment uniqueness constraint | Scenario 39; `SC-020` | One payment-method identity per draft at domain and PostgreSQL levels |
| `DR-023` | Opaque emission-point data model | Scenarios 1, 34, 35, 41, 44; `SC-001`, `SC-007`, `SC-022`, `SC-025` | Store/return canonical opaque ID only; no ownership/state lookup or fiscal representation |
| `DR-024` | API correlation mapping and observability design | Scenarios 55–58; `SC-033` | Generate/preserve/replace safely, never echo invalid input, exclude from fingerprint |

## Success Criteria

| ID | Requirements and design traced | Observable evidence |
|----|--------------------------------|---------------------|
| `SC-001` | `FR-016`–`FR-023`, `FR-030`, `FR-037`, `FR-041`; aggregate, response, idempotency, and transaction designs | Under the non-deadline precondition, every logically new valid vector conclusively selects success before expiry, atomically commits exactly one complete USD `DRAFT` plus children and Company-scoped binding, returns the approved `201`/every `FR-022` field and has no SRI side effect; deadline-first uses `504 REQUEST_TIMEOUT` instead |
| `SC-002` | `FR-001`, `FR-020`, `FR-021`, `FR-032`, `FR-042`–`FR-044` | Row-count assertions show zero root/child/binding state only for confirmed pre-commit rejection or complete rollback; uncertain/post-commit vectors assert same-scope replay and no duplicate |
| `SC-003` | `FR-012`, `FR-014`; `DR-002`–`DR-010` | Golden calculations identical across repetitions, persistence, packaged JVM, and claimed native runtime |
| `SC-004` | `DR-002`–`DR-005`, `DR-009`, `DR-010` | `2 × 10.00 − 5.00` at 15% always yields `20.00/15.00/2.25/17.25` |
| `SC-005` | `FR-017`, `FR-018`, `FR-023`, `FR-038` | Dependency/trace/storage audit records zero issuance/SRI/PDF/notification effects |
| `SC-006` | `FR-001`, `FR-002`, `FR-025`, `FR-026`, `FR-041` | Under the non-deadline precondition, the complete Company-header matrix conclusively selects the applicable safe correlated `400` and zero state before expiry; controlled races prove deadline-first `504` and that a stage-first Company `400` remains authoritative |
| `SC-007` | `FR-004`, `FR-016`–`FR-019`, `FR-022`; `DR-020`, `DR-023` | Accepted/replayed representation lets client review CompanyId, opaque emissionPointId, buyer, lines, taxes, payments, USD totals, DRAFT and timestamps without issuance |
| `SC-008` | `FR-017`, `FR-023`, `FR-038` | Acceptance suite runs without update/delete/issuance/XML/signature/SRI/PDF/notification facilities |
| `SC-009` | `FR-010`, `FR-011`; `DR-001`, `DR-005`, `DR-008` | Exactly one effective IVA rule per accepted line; unsupported/multiple rejected without state |
| `SC-010` | `FR-007`, `FR-028`; `DR-001`, `DR-014`, `DR-015` | All approved type vectors pass/fail from official rules with zero online lookup |
| `SC-011` | `FR-013`, `FR-014`; `DR-016` | Valid zero-total draft has one `0.00` payment; all other zero-payment shapes reject |
| `SC-012` | `FR-024`, `FR-027`–`FR-034`, `FR-043`; `DR-017`, `DR-018` | Exact required/invalid/multiple header matrix and normalized-value identity; new/replay/conflict/rollback/unresolved/50-way concurrency commits at most one per Company+key scope; replay returns original createdAt |
| `SC-013` | `FR-006`, `FR-019`; `DR-012` | Captured request-date Instant, midnight crossing, different/impossible dates, and separate once-only transaction-createdAt with no physical commit timestamp |
| `SC-014` | `FR-012`; `DR-011` | Every calculated input rejected consistently even when equal to computed value |
| `SC-015` | `FR-009`, `FR-013`, `FR-015` | Exact 500-line/8-distinct-payment/15-additional maxima accepted; maxima+1 reject without state |
| `SC-016` | `FR-008`, `FR-010`, `FR-015`, `FR-035`; `DR-019` | Exact business-text boundaries accepted; invalid/over/blank/control/contact/duplicate vectors reject; header evidence remains in SC-012 |
| `SC-017` | `FR-024`, `FR-037`; `DR-020` | One immutable canonical CompanyId and local child ownership; zero cross-Company mixing |
| `SC-018` | `FR-030`, `FR-033`; `DR-017` | Equivalent replay returns original for draft lifetime with no elapsed-time expiry |
| `SC-019` | `FR-029`; `DR-019`, `DR-021` | Payment/additional reorder replays; line reorder conflicts; no duplicate draft |
| `SC-020` | `FR-013`; `DR-022` | Accepted drafts have unique payment methods; duplicate method rejects with zero state |
| `SC-021` | `FR-011`; `DR-001`, `DR-011` | Tax code/rate comes only from selected approved rule; caller-supplied code/rate rejects |
| `SC-022` | `FR-004`, `FR-005`, `FR-037`, `FR-038`; `DR-020`, `DR-023` | Exactly CompanyId+opaque emissionPointId and zero Company/fiscal snapshot fields |
| `SC-023` | `FR-003`, `FR-023`, `FR-033`, `FR-036`, `FR-039`, `FR-040` | Create/replay traces and architecture show zero Company/auth/cache/replication behavior |
| `SC-024` | `FR-001`–`FR-003`, `FR-022`, `FR-039` | Static-copy tests plus packaged `/q/openapi` equality prove Company identifiers absent from request/input/path/query, authoritative header-only input, explicitly required response CompanyId, and zero security/Auth/401/403 constructs |
| `SC-025` | `FR-002`, `FR-005`, `FR-012` | Strict body tests reject Company/Issuer/fiscal snapshot and calculated fields with zero state |
| `SC-026` | `FR-041`; ordered HTTP gate and cross-cutting deadline design | Controlled pairwise/multi-failure suite proves first-conclusive stage/deadline arbitration, pre-entity header ordering, no winner replacement, and no execution of later work |
| `SC-027` | `FR-042` | Content-Length/chunked exact byte-boundary and controlled slow-body tests: ≤2 MiB continues; over-limit-first preserves valid or safely replaces absent/invalid correlation and returns 413 with zero DB calls; deadline-first returns 504 |
| `SC-028` | `FR-032`, `FR-043` | 503/500 and confirmed pre-commit timeout/rollback leave zero state; unresolved/post-commit outcomes make no zero-state claim and replay resolves authoritative state; expiry after HTTP commit is telemetry-only with no second response/write/compensation |
| `SC-029` | `FR-010`, `FR-012`–`FR-014`, `FR-044`; `DR-010` | Same numeric envelope at API/domain/intermediate/group/payment/database/response; every breach gives `MONETARY_RANGE_EXCEEDED` |
| `SC-030` | `FR-006`, `FR-033`; `DR-012`, `DR-017` | Later-date equivalent replay returns original date without validation or mutation |
| `SC-031` | `FR-045`, `FR-047`; `DR-001` | Baseline audit records zero unverified supported rows and explicitly omits unsupported rows |
| `SC-032` | `FR-046`, `FR-047`; `DR-001` | Published UUIDs recalculate exactly; later contract UUIDs equal Flyway seed UUIDs; zero startup generation/catalog-query operations |
| `SC-033` | `FR-026`, `FR-029`, `FR-041`, `FR-042`; `DR-024` | Correlation absent/valid/invalid/combined-failure vectors, including 413 preservation/replacement without stage-3 takeover, plus fingerprint invariance |

## Acceptance Scenario Coverage

Every request-processing row below that does not explicitly test a deadline race inherits the
non-deadline scenario precondition defined above; its unqualified success, replay, conflict, `400`,
`422`, `500`, or `503` outcome therefore means that the tested stage conclusively completed before
expiry. AS-045 and AS-046 explicitly exercise deadline arbitration and instead prove both the
stage-first winner and deadline-first `504`. Non-request readiness or static boundary-review rows
do not define a deadline oracle.

| Scenario | Primary trace | Planned observable evidence |
|----------|---------------|-----------------------------|
| `AS-001` | `FR-001`, `FR-020`, `FR-022`, `FR-037`, `FR-041` | Under the non-deadline precondition, valid header and content conclusively select success and commit one complete draft; deadline-first follows FR-041 |
| `AS-002` | `FR-012`; `DR-002`–`DR-005` | 15% mathematical vector yields 20.00/15.00/2.25/17.25 without universal-rate claim |
| `AS-003` | `FR-010`; `DR-003`, `DR-004` | Discount greater than gross rejects with zero state |
| `AS-004` | `FR-009` | Empty line collection rejects with zero state |
| `AS-005` | `FR-014` | Payment mismatch rejects with zero state |
| `AS-006` | `FR-001`, `FR-025`, `FR-041` | Missing/blank Company classification completed before expiry returns `COMPANY_CONTEXT_REQUIRED`; deadline-first returns `REQUEST_TIMEOUT` |
| `AS-007` | `FR-007`, `FR-028`; `DR-014` | Invalid approved buyer format rejects |
| `AS-008` | `FR-023` | Successful create has zero fiscal/SRI/notification side effects |
| `AS-009` | `FR-007`, `FR-011`, `FR-013` | Inactive or ineffective reference rejects |
| `AS-010` | `FR-002`, `FR-005` | Body `companyId` rejects as prohibited/unknown |
| `AS-011` | `FR-020`, `FR-021` | Injected failure at each write phase is confirmed pre-commit/fully rolled back and leaves zero aggregate or binding state |
| `AS-012` | `FR-011`; `DR-005` | Non-IVA, missing, or multiple tax selection rejects |
| `AS-013` | `FR-007`, `FR-028`; `DR-014` | Codes 04/05/06/08 follow exact approved FORMAT_ONLY strategies |
| `AS-014` | `FR-028`; `DR-014` | Valid RUC/Cédula format is not subjected to checksum or legacy algorithm |
| `AS-015` | `FR-007`; `DR-015` | Final-consumer type/value/name/USD 50.00 boundary |
| `AS-016` | `FR-007`, `FR-028` | Unknown/inactive/not-effective/expired type rejects |
| `AS-017` | `FR-013`, `FR-014`; `DR-016` | Valid zero-total shape accepts one 0.00 payment |
| `AS-018` | `FR-013`, `FR-014` | Invalid zero-total payment shapes reject |
| `AS-019` | `FR-013`, `FR-014` | Positive total with zero or mismatched payment rejects |
| `AS-020` | `FR-029`, `FR-030`, `FR-033` | Equivalent same-scope replay returns original |
| `AS-021` | `FR-030` | Different-content same-scope replay conflicts |
| `AS-022` | `FR-031` | Concurrent equivalent requests create one draft |
| `AS-023` | `FR-032` | Validation/rollback failure creates no binding |
| `AS-024` | `FR-032`, `FR-033`, `FR-043` | Response loss after commit preserves state and reconciles by same-scope replay without compensation or duplicate creation |
| `AS-025` | `FR-027`, `FR-031` | Same key is independent across Companies |
| `AS-026` | `FR-006`; `DR-012` | Current Ecuador date accepts from one request instant |
| `AS-027` | `FR-006`; `DR-012`, `DR-013` | Past/future/impossible date rejects without normalization |
| `AS-028` | `FR-012`; `DR-011` | Every supplied calculated field rejects |
| `AS-029` | `FR-009`, `FR-013`, `FR-015` | 500 lines, 8 distinct positive payments, and 15 additional entries accept |
| `AS-030` | `FR-009`, `FR-013`, `FR-015` | 501 lines, 9 payments, or 16 additional entries reject |
| `AS-031` | `FR-008`, `FR-010`, `FR-015`, `FR-035` | Exact text limits accept |
| `AS-032` | `FR-008`, `FR-010`, `FR-015`, `FR-027`, `FR-035` | Over-limit/malformed text rejects; over-length Idempotency-Key returns `IDEMPOTENCY_KEY_INVALID` |
| `AS-033` | `FR-035` | Blank/control/duplicate canonical text rejects |
| `AS-034` | `FR-002`, `FR-004`, `FR-037`, `FR-040` | Header maps to stored/returned CompanyId; emission point remains opaque |
| `AS-035` | `FR-003`, `FR-036` | Externally unknown valid Company UUID proceeds without lookup |
| `AS-036` | `FR-030`; `DR-017` | Binding has draft-lifetime retention |
| `AS-037` | `FR-029`; `DR-021` | Payment/additional reorder remains equivalent |
| `AS-038` | `FR-029`; `DR-021` | Line reorder conflicts |
| `AS-039` | `FR-013`; `DR-022` | Duplicate payment method rejects |
| `AS-040` | `FR-011`; `DR-001`, `DR-011` | Caller-selected published rule is used without classification; direct code/rate rejects |
| `AS-041` | `FR-023`, `FR-036`, `FR-038` | New create performs no Company/fiscal resolution or side effect |
| `AS-042` | `FR-033`, `FR-038` | Replay performs no external refresh or mutation |
| `AS-043` | `FR-036`, `FR-039`, `FR-040` | Static boundary has no Company/security/cache dependency |
| `AS-044` | `FR-005`, `FR-038` | Issuer/fiscal/snapshot request fields reject |
| `AS-045` | `FR-026`, `FR-041`, `FR-042` | Over-limit-first body returns 413, preserves valid or safely replaces absent/invalid correlation without 400 or DB work; deadline-first slow body returns 504 |
| `AS-046` | `FR-021`, `FR-032`, `FR-041`, `FR-043` | Confirmed pre-deadline rollback/failure returns its approved outcome and zero state; unresolved-at-deadline returns uncertain 504 and same-scope replay prevents duplication |
| `AS-047` | `FR-001`, `FR-002`, `FR-041` | Malformed/nil Company classification completed before expiry returns `COMPANY_CONTEXT_INVALID`; deadline-first returns `REQUEST_TIMEOUT` |
| `AS-048` | `FR-001`, `FR-002`, `FR-041` | Multiple-value Company classification completed before expiry returns `COMPANY_CONTEXT_INVALID`; deadline-first returns `REQUEST_TIMEOUT` |
| `AS-049` | `FR-010`, `FR-044`; `DR-010` | Quantity/price/money maxima accept when totals remain in range |
| `AS-050` | `FR-044`; `DR-010` | Any input/intermediate/group/total overflow returns `MONETARY_RANGE_EXCEEDED` |
| `AS-051` | `FR-006`, `FR-019`; `DR-012` | Pre-midnight request retains derived date; separate createdAt is captured at the defined in-transaction point, not physical commit |
| `AS-052` | `FR-019`, `FR-033`; `DR-012`, `DR-017` | Later-date replay returns original emission date and persisted createdAt |
| `AS-053` | `FR-045`, `FR-047`; `DR-001` | Readiness audit blocks any future incomplete reference row; current supported rows all pass |
| `AS-054` | `FR-046`; `DR-001` | Published UUIDs select approved rows and are never startup-generated |
| `AS-055` | `FR-026`; `DR-024` | Missing correlation generates safe UUID |
| `AS-056` | `FR-026`, `FR-029`; `DR-024` | One valid correlation is preserved and excluded from equivalence |
| `AS-057` | `FR-026`, `FR-041`; `DR-024` | Invalid correlation is replaced, never echoed, and returns `INVALID_REQUEST` |
| `AS-058` | `FR-026`, `FR-041`; `DR-024` | Company error precedes correlation error while safe replacement is returned |
| `AS-059` | `FR-027`, `FR-041` | Missing key returns `IDEMPOTENCY_KEY_REQUIRED`; one blank/whitespace/over-length/non-comma grammar-invalid value returns `IDEMPOTENCY_KEY_INVALID`; zero lookup/state |
| `AS-060` | `FR-027`, `FR-041` | Repeated/parser-multiple or comma-containing/comma-combined key returns `IDEMPOTENCY_KEY_MULTIPLE`; no first selection or lookup/state |
| `AS-061` | `FR-019`, `FR-027`, `FR-033`; `DR-012`, `DR-017` | One valid key is SP/HTAB-trimmed once and used identically for lookup/hash/persistence; replay returns original createdAt |

## Stable Error Coverage

| Error code | Governing requirements | Acceptance / planned evidence |
|------------|------------------------|-------------------------------|
| `COMPANY_CONTEXT_REQUIRED` | `FR-001`, `FR-002`, `FR-025` | `AS-006`; safe 400 and zero state |
| `COMPANY_CONTEXT_INVALID` | `FR-001`, `FR-002`, `FR-025` | `AS-047`, `AS-048`; safe 400 and zero state |
| `IDEMPOTENCY_KEY_REQUIRED` | `FR-027`, `FR-041` | `AS-059`; safe 400, no lookup/state |
| `IDEMPOTENCY_KEY_INVALID` | `FR-027`, `FR-041` | `AS-032`, `AS-059`; safe 400 for one invalid normalized value, no lookup/state |
| `IDEMPOTENCY_KEY_MULTIPLE` | `FR-027`, `FR-041` | `AS-060`; safe 400 for repeated/parser-multiple/comma input, no first selection or lookup/state |
| `INVALID_REQUEST` | `FR-002`, `FR-005`, `FR-026`, `FR-041` | `AS-010`, `AS-044`, `AS-057`; safe 400/correlation |
| `PROHIBITED_CALCULATED_FIELD` | `FR-012`, `FR-025`; `DR-011` | `AS-028`; safe 422 and zero state |
| `IDEMPOTENCY_CONFLICT` | `FR-030`, `FR-031` | `AS-021`, `AS-038`; safe 409, original unchanged |
| `REQUEST_PAYLOAD_TOO_LARGE` | `FR-026`, `FR-041`, `FR-042` | `AS-045`; over-limit-first safe 413 before Company validation, valid correlation preserved, absent/invalid replaced, no correlation 400 or DB work |
| `BUSINESS_VALIDATION_FAILED` | `FR-007`–`FR-015`, `FR-028`, `FR-044` | Business rejection scenarios and `AS-050`; safe 422 and zero state |
| `MONETARY_RANGE_EXCEEDED` | `FR-044`; `DR-010` | `AS-050`; nested violation under `BUSINESS_VALIDATION_FAILED` |
| `PERSISTENCE_UNAVAILABLE` | `FR-020`, `FR-021`, `FR-043` | `AS-011`, `AS-046`; safe 503 for unavailable/configured-operation-timeout-first outcomes while request budget remains, then retry same scope |
| `REQUEST_TIMEOUT` | `FR-041`, `FR-043` | `AS-024`, `AS-045`, `AS-046`; cross-cutting deadline-first 504, aggregate/reference remaining-budget propagation, timer cancellation, unresolved-commit replay, and post-response-commit telemetry-only behavior |
| `INTERNAL_ERROR` | `FR-025`, `FR-043` | `AS-046`; safe 500 without internal/sensitive data |

## Deadline Arbitration and Terminal-Outcome Coverage

| Controlled race | Required winner | Focused evidence |
|-----------------|-----------------|------------------|
| Non-deadline acceptance scenario | Tested stage conclusively selects its described terminal outcome before expiry | T032 configures a non-expiring or sufficiently remaining controlled deadline; this is stage-first evidence and not a competing `504` oracle |
| Payload size becomes conclusively over 2 MiB before expiry | `413 REQUEST_PAYLOAD_TOO_LARGE` | T030–T032 preserve valid or replace absent/invalid correlation, emit no `400`, and prove no database call |
| Deadline expires before payload size is conclusive | `504 REQUEST_TIMEOUT` | T031–T032 controlled slow-body deadline signal; later size result cannot replace 504 |
| Company/correlation/idempotency invalidity becomes conclusive before expiry | Applicable approved `400`, including the exact required/invalid/multiple idempotency-key code | T029–T032 controlled stage-first header vectors; repeated/comma input never selects first |
| Deadline expires before header classification is conclusive | `504 REQUEST_TIMEOUT` | T032 controlled deadline-first header vectors; no entity decode or later work |
| Replay/conflict, validation/calculation, rollback/failure, or commit becomes conclusive before expiry | Approved `200`/`201`/`409`/`422`/`500`/`503` result | T027/T032/T036 controlled stage-first application and persistence vectors |
| Deadline expires while lookup or commit outcome is unresolved | `504 REQUEST_TIMEOUT`, uncertain when commit may be in flight | T027/T032/T036 same-Company/key/content replay resolves state without a duplicate |
| Deadline expires after HTTP response commitment | Existing status/body remains authoritative; telemetry only | T032/T038/T039/T083 prove no 504, second response/database write, mutation, or compensation and record the safe event |

## Cross-Cutting Constitutional Evidence

| Risk | Governing source | Planned evidence | Explicit prohibition |
|------|------------------|------------------|----------------------|
| Flyway and local constraints | Constitution IX and Definition of Done | Empty-database migration and constraint tests using the approved reference baseline | No auto-generation, manual schema, legacy dump, or cross-service foreign key |
| Health and observability | Constitution XIII | `operational-requirements.md` liveness/readiness/log/metric/trace tests | No Company/identity/SRI readiness and no sensitive/high-cardinality labels |
| JVM/native evidence | Constitution III/XII | Mandatory packaged JVM suite; native build plus runtime only if claimed | No native claim from build alone |
| Performance and reactive safety | Constitution V/XIII | Documented reference environment, latency profiles, 50-way concurrency, blocked-thread evidence | No Company latency, cache, blocking wait, or hidden blocking wrapper |
