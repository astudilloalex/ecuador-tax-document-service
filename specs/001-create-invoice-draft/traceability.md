# Traceability Matrix: Create Invoice Draft

This matrix maps every approved functional requirement, domain rule, and measurable outcome to
design evidence and an observable verification target. It does not create implementation tasks.

**Reference-data status**: Complete. `reference-data-baseline.md` approves 5 buyer rows, 6 IVA
rows, and 8 payment rows under `SRI-OFFLINE-2.32-TARGET-1`, using deterministic UUIDv5 namespace
`32576bbf-b70d-5c24-98ff-d5f9b48e8826`. Constitution v2.0.0 is approved on `main`, and the
reconciled specification and plan contain no active governance blocker.

## Functional Requirements

| ID | Design and contract evidence | Acceptance / success evidence | Planned verification |
|----|------------------------------|-------------------------------|----------------------|
| `FR-001` | OpenAPI `X-Company-Id`; `error-catalog.md` Company errors | Scenarios 1, 6, 47, 48; `SC-006` | API presence, blank, repeated, malformed, nil, safe-correlation, and zero-state tests |
| `FR-002` | OpenAPI operation/header/body; `plan.md` API boundary | Scenarios 1, 10, 34, 47, 48; `SC-024`, `SC-025` | Contract test proves canonical UUID and no Company path/query/body field |
| `FR-003` | `plan.md` Company boundary; no Company port contract | Scenarios 35, 41, 43; `SC-023` | Valid externally unknown UUID succeeds; architecture test proves zero lookup |
| `FR-004` | OpenAPI `emissionPointId`; `data-model.md` root | Scenarios 1, 34; `SC-007`, `SC-022` | UUID syntax/canonicalization, persistence, response, and no ownership lookup |
| `FR-005` | Strict OpenAPI request schema; `error-catalog.md` | Scenarios 10, 44; `SC-022`, `SC-025` | Reject Issuer/establishment/emission fiscal and snapshot properties |
| `FR-006` | `plan.md` clock mapping; `data-model.md` temporal model | Scenarios 26, 27, 51, 52; `SC-013`, `SC-030` | One captured `requestCreationInstant`, Guayaquil conversion, midnight and replay tests |
| `FR-007` | Approved identification baseline in `reference-data-baseline.md` | Scenarios 7, 9, 13–16, 53; `SC-010`, `SC-031` | Exact FORMAT_ONLY and final-consumer vectors for all five approved codes |
| `FR-008` | OpenAPI buyer/contact limits; `data-model.md` buyer fields | Scenarios 31–33; `SC-016` | Address/email/telephone boundary and invalid-format tests |
| `FR-009` | OpenAPI line cardinality; `data-model.md` aggregate | Scenarios 4, 29, 30; `SC-015` | 1/500 accepted; 0/501 rejected without state |
| `FR-010` | OpenAPI line numeric/text bounds; `data-model.md` decimals | Scenarios 2, 3, 31, 32, 49, 50; `SC-004`, `SC-016`, `SC-029` | Quantity/price/discount precision, range, text, and overflow vectors |
| `FR-011` | OpenAPI `taxRuleId`; approved IVA baseline and upstream-selection boundary | Scenarios 9, 12, 40, 53, 54; `SC-009`, `SC-021`, `SC-031`, `SC-032` | Active/effective immutable `family=IVA` rule selection, zero parent-category entity or automatic classification, direct code/rate and multiple-tax rejection |
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
| `FR-028` | Identification validation boundary and approved FORMAT_ONLY strategies | Scenarios 7, 13–16; `SC-010` | Exact syntax/length/special-value vectors; zero checksum, online registry, or name lookup |
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
| `FR-041` | `error-catalog.md` precedence; ordered HTTP upload handler/pre-entity gate/application flow | Scenarios 45, 57, 58; `SC-026`, `SC-033` | Content-Length/chunked and combined-failure tables cover all 12 ordered stages, deferred entity decoding, and safe correlation initialization |
| `FR-042` | Quarkus HTTP upload limit/failure handler; OpenAPI 413 | Scenario 45; `SC-002`, `SC-027` | Exact 2 MiB proceeds; every larger Content-Length or chunked body returns feature Problem Details before Company/correlation validation with safe correlation |
| `FR-043` | Persistence/error/timeout design | Scenarios 24, 46; `SC-028` | 503/504/500 injection, zero pre-commit state, and post-commit replay recovery |
| `FR-044` | OpenAPI decimal constraints; domain/data numeric envelopes | Scenarios 49, 50; `SC-029` | API/domain/intermediate/group/payment/persistence/response boundary and overflow vectors |
| `FR-045` | Approved `reference-data-baseline.md` row tables and source register | Scenario 53; `SC-031` | Audit proves every supported row has official facts, target decisions, validity, activity, version, source, and approval |
| `FR-046` | Published UUIDv5 namespace/names; OpenAPI and quickstart exact IDs | Scenario 54; `SC-032` | Independently recalculate all 14 UUIDs; later contract-to-seed equality and no runtime generation/catalog query |
| `FR-047` | Baseline excluded-row table and governance rules | Scenario 53; `SC-031`, `SC-032` | Unsupported rows omitted; any later unevidenced row blocks its own introduction |

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
| `DR-012` | Single request clock and temporal model | Scenarios 26, 27, 51, 52; `SC-013`, `SC-030` | One-instant Guayaquil conversion, midnight commit, commit timestamp, later replay |
| `DR-013` | Validation/error/transaction boundaries | Scenarios 3–7, 9, 12–16, 27, 39, 40, 50; `SC-002`, `SC-009`, `SC-010`, `SC-029` | Impossible/inconsistent/inactive/unsupported/local-relation failures reject without normalization or state |
| `DR-014` | Approved buyer FORMAT_ONLY strategies | Scenarios 7, 13, 14, 16; `SC-010` | RUC 13-digit, Cédula 10-digit, passport/foreign 1–20 alphanumeric vectors; no checksum or invented rules |
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
| `SC-015` | `FR-009`, `FR-013`, `FR-015` | Exact 500-line/8-distinct-payment/15-additional maxima accepted; maxima+1 reject without state |
| `SC-016` | `FR-008`, `FR-010`, `FR-015`, `FR-027`, `FR-035`; `DR-019` | Exact text boundaries accepted; invalid/over/blank/control/contact/duplicate vectors reject |
| `SC-017` | `FR-024`, `FR-037`; `DR-020` | One immutable canonical CompanyId and local child ownership; zero cross-Company mixing |
| `SC-018` | `FR-030`, `FR-033`; `DR-017` | Equivalent replay returns original for draft lifetime with no elapsed-time expiry |
| `SC-019` | `FR-029`; `DR-019`, `DR-021` | Payment/additional reorder replays; line reorder conflicts; no duplicate draft |
| `SC-020` | `FR-013`; `DR-022` | Accepted drafts have unique payment methods; duplicate method rejects with zero state |
| `SC-021` | `FR-011`; `DR-001`, `DR-011` | Tax code/rate comes only from selected approved rule; caller-supplied code/rate rejects |
| `SC-022` | `FR-004`, `FR-005`, `FR-037`, `FR-038`; `DR-020`, `DR-023` | Exactly CompanyId+opaque emissionPointId and zero Company/fiscal snapshot fields |
| `SC-023` | `FR-003`, `FR-023`, `FR-033`, `FR-036`, `FR-039`, `FR-040` | Create/replay traces and architecture show zero Company/auth/cache/replication behavior |
| `SC-024` | `FR-001`–`FR-003`, `FR-039` | Static-copy tests plus packaged `/q/openapi` semantic equality prove header-only Company context and zero security/Auth/401/403 constructs in the served contract |
| `SC-025` | `FR-002`, `FR-005`, `FR-012` | Strict body tests reject Company/Issuer/fiscal snapshot and calculated fields with zero state |
| `SC-026` | `FR-041`; ordered HTTP gate design | Pairwise/multi-failure suite proves earliest outcome, pre-entity header ordering, and no execution of later stages |
| `SC-027` | `FR-042` | Content-Length and chunked exact byte-boundary tests: ≤2 MiB continues; >2 MiB returns correlated feature 413 before Company evaluation |
| `SC-028` | `FR-032`, `FR-043` | 503/504/500 pre-commit failures leave zero state; post-commit loss replays original |
| `SC-029` | `FR-010`, `FR-012`–`FR-014`, `FR-044`; `DR-010` | Same numeric envelope at API/domain/intermediate/group/payment/database/response; every breach gives `MONETARY_RANGE_EXCEEDED` |
| `SC-030` | `FR-006`, `FR-033`; `DR-012`, `DR-017` | Later-date equivalent replay returns original date without validation or mutation |
| `SC-031` | `FR-045`, `FR-047`; `DR-001` | Baseline audit records zero unverified supported rows and explicitly omits unsupported rows |
| `SC-032` | `FR-046`, `FR-047`; `DR-001` | Published UUIDs recalculate exactly; later contract UUIDs equal Flyway seed UUIDs; zero startup generation/catalog-query operations |
| `SC-033` | `FR-026`, `FR-029`, `FR-041`; `DR-024` | Correlation absent/valid/invalid/combined-failure vectors plus fingerprint invariance |

## Acceptance Scenario Coverage

| Scenario | Primary trace | Planned observable evidence |
|----------|---------------|-----------------------------|
| `AS-001` | `FR-001`, `FR-020`, `FR-022`, `FR-037` | Valid header and content commit one complete draft |
| `AS-002` | `FR-012`; `DR-002`–`DR-005` | 15% mathematical vector yields 20.00/15.00/2.25/17.25 without universal-rate claim |
| `AS-003` | `FR-010`; `DR-003`, `DR-004` | Discount greater than gross rejects with zero state |
| `AS-004` | `FR-009` | Empty line collection rejects with zero state |
| `AS-005` | `FR-014` | Payment mismatch rejects with zero state |
| `AS-006` | `FR-001`, `FR-025` | Missing/blank Company header returns `COMPANY_CONTEXT_REQUIRED` |
| `AS-007` | `FR-007`, `FR-028`; `DR-014` | Invalid approved buyer format rejects |
| `AS-008` | `FR-023` | Successful create has zero fiscal/SRI/notification side effects |
| `AS-009` | `FR-007`, `FR-011`, `FR-013` | Inactive or ineffective reference rejects |
| `AS-010` | `FR-002`, `FR-005` | Body `companyId` rejects as prohibited/unknown |
| `AS-011` | `FR-020`, `FR-021` | Injected persistence failure rolls back aggregate and binding |
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
| `AS-024` | `FR-032`, `FR-033`, `FR-043` | Response loss after commit reconciles by replay |
| `AS-025` | `FR-027`, `FR-031` | Same key is independent across Companies |
| `AS-026` | `FR-006`; `DR-012` | Current Ecuador date accepts from one request instant |
| `AS-027` | `FR-006`; `DR-012`, `DR-013` | Past/future/impossible date rejects without normalization |
| `AS-028` | `FR-012`; `DR-011` | Every supplied calculated field rejects |
| `AS-029` | `FR-009`, `FR-013`, `FR-015` | 500 lines, 8 distinct positive payments, and 15 additional entries accept |
| `AS-030` | `FR-009`, `FR-013`, `FR-015` | 501 lines, 9 payments, or 16 additional entries reject |
| `AS-031` | `FR-008`, `FR-010`, `FR-015`, `FR-035` | Exact text limits accept |
| `AS-032` | `FR-008`, `FR-010`, `FR-015`, `FR-027`, `FR-035` | Over-limit and malformed text/key reject |
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
| `AS-045` | `FR-041`, `FR-042` | Payload >2 MiB wins precedence and leaves zero state |
| `AS-046` | `FR-043` | Persistence timeout/unexpected failure is safe and atomic |
| `AS-047` | `FR-001`, `FR-002` | Malformed/nil Company header returns `COMPANY_CONTEXT_INVALID` |
| `AS-048` | `FR-001`, `FR-002` | Multiple Company values return `COMPANY_CONTEXT_INVALID` |
| `AS-049` | `FR-010`, `FR-044`; `DR-010` | Quantity/price/money maxima accept when totals remain in range |
| `AS-050` | `FR-044`; `DR-010` | Any input/intermediate/group/total overflow returns `MONETARY_RANGE_EXCEEDED` |
| `AS-051` | `FR-006`, `FR-019`; `DR-012` | Pre-midnight request retains derived date across commit |
| `AS-052` | `FR-033`; `DR-012`, `DR-017` | Later-date replay returns original emission date |
| `AS-053` | `FR-045`, `FR-047`; `DR-001` | Readiness audit blocks any future incomplete reference row; current supported rows all pass |
| `AS-054` | `FR-046`; `DR-001` | Published UUIDs select approved rows and are never startup-generated |
| `AS-055` | `FR-026`; `DR-024` | Missing correlation generates safe UUID |
| `AS-056` | `FR-026`, `FR-029`; `DR-024` | One valid correlation is preserved and excluded from equivalence |
| `AS-057` | `FR-026`, `FR-041`; `DR-024` | Invalid correlation is replaced, never echoed, and returns `INVALID_REQUEST` |
| `AS-058` | `FR-026`, `FR-041`; `DR-024` | Company error precedes correlation error while safe replacement is returned |

## Stable Error Coverage

| Error code | Governing requirements | Acceptance / planned evidence |
|------------|------------------------|-------------------------------|
| `COMPANY_CONTEXT_REQUIRED` | `FR-001`, `FR-002`, `FR-025` | `AS-006`; safe 400 and zero state |
| `COMPANY_CONTEXT_INVALID` | `FR-001`, `FR-002`, `FR-025` | `AS-047`, `AS-048`; safe 400 and zero state |
| `INVALID_REQUEST` | `FR-002`, `FR-005`, `FR-026`, `FR-027`, `FR-041` | `AS-010`, `AS-032`, `AS-044`, `AS-057`; safe 400/correlation |
| `PROHIBITED_CALCULATED_FIELD` | `FR-012`, `FR-025`; `DR-011` | `AS-028`; safe 422 and zero state |
| `IDEMPOTENCY_CONFLICT` | `FR-030`, `FR-031` | `AS-021`, `AS-038`; safe 409, original unchanged |
| `REQUEST_PAYLOAD_TOO_LARGE` | `FR-041`, `FR-042` | `AS-045`; safe 413 before Company validation |
| `BUSINESS_VALIDATION_FAILED` | `FR-007`–`FR-015`, `FR-028`, `FR-044` | Business rejection scenarios and `AS-050`; safe 422 and zero state |
| `MONETARY_RANGE_EXCEEDED` | `FR-044`; `DR-010` | `AS-050`; nested violation under `BUSINESS_VALIDATION_FAILED` |
| `PERSISTENCE_UNAVAILABLE` | `FR-020`, `FR-021`, `FR-043` | `AS-011`, `AS-046`; safe 503 and retry same scope |
| `REQUEST_TIMEOUT` | `FR-032`, `FR-043` | `AS-024`, `AS-046`; safe 504 and replay recovery |
| `INTERNAL_ERROR` | `FR-025`, `FR-043` | `AS-046`; safe 500 without internal/sensitive data |

## Cross-Cutting Constitutional Evidence

| Risk | Governing source | Planned evidence | Explicit prohibition |
|------|------------------|------------------|----------------------|
| Flyway and local constraints | Constitution IX and Definition of Done | Empty-database migration and constraint tests using the approved reference baseline | No auto-generation, manual schema, legacy dump, or cross-service foreign key |
| Health and observability | Constitution XIII | `operational-requirements.md` liveness/readiness/log/metric/trace tests | No Company/identity/SRI readiness and no sensitive/high-cardinality labels |
| JVM/native evidence | Constitution III/XII | Mandatory packaged JVM suite; native build plus runtime only if claimed | No native claim from build alone |
| Performance and reactive safety | Constitution V/XIII | Documented reference environment, latency profiles, 50-way concurrency, blocked-thread evidence | No Company latency, cache, blocking wait, or hidden blocking wrapper |
