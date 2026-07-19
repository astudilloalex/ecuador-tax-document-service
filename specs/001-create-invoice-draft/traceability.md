# Traceability Matrix: Create Invoice Draft

This matrix maps every approved functional requirement, domain rule, and measurable outcome to
design evidence and an observable verification target. It does not create implementation tasks.

**Reference-data status**: Complete. `reference-data-baseline.md` approves 5 buyer rows, 6 IVA
rows, and 8 payment rows under `SRI-OFFLINE-2.32-TARGET-1`, using deterministic UUIDv5 namespace
`32576bbf-b70d-5c24-98ff-d5f9b48e8826`. Constitution v2.0.1 is the current PATCH-amended text.
The T001–T016 retrospective and D1–D3 dispositions are approved by `astudilloalex`, and
`GATE-GOV-001` is released. The later request-time and request-contract findings were remediated,
the follow-up analysis cleared implementation, T017/T018 fulfilled their approved corrective
assignments, and the final T019–T101 implementation/validation sequence is complete.

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
| `GOV-001` / `GATE-GOV-001` | Completed retrospective, approved D1–D3 dispositions, and mandatory corrective controls | `governance-retrospective-review.md`; `governance-owner-approval.md`; `governance-nonconformity.md`; commits `1289871`, `8bdd548`, `5e5452a` | `RELEASED`; approved by `astudilloalex`; historical non-conformity retained; corrective addendum and later documentary remediation complete |
| `T017` | Own `ascii-validation-vectors.json`, validate its staged request/storage structure, and create intentional red PostgreSQL/Flyway evidence against immutable V3; create no migration | `FR-007`, `FR-010`, `FR-020`; `DR-014`; `SC-002`, `SC-010`, `SC-016`, `SC-022`; C2/D2/D3 | Complete — authoritative fixture and intentional V3 failure evidence preserved; V3 remains immutable |
| `T018` | Depend on T017; create V5 and make T017 PostgreSQL/Flyway evidence green using stored/probe fixture values; no production-Java equivalence | `FR-007`, `FR-010`, `FR-020`; `DR-014`; `SC-002`, `SC-010`, `SC-016`, `SC-022`; C2/D2/D3 | Complete — V5 is the sole corrective migration; empty-db and V3→V5 PostgreSQL evidence is green |
| `T013` | Versioned reference-catalog structures, evidence/validity fields, stable-identifier columns, and constraints | `FR-045`, `FR-046`, `FR-047`; `SC-031`, `SC-032`; `reference-data-baseline.md`; V1 retrospective evidence | Historically complete; retrospective result `CONFORMING`; owner disposition approved |
| `T014` | Exact approved baseline rows, fixed UUIDs, exclusion of unsupported rows, and verification queries | `FR-045`, `FR-046`, `FR-047`; `SC-031`, `SC-032`; `reference-data-baseline.md`; V2 retrospective evidence | Historically complete; retrospective result `CONFORMING`; owner disposition approved |

The requirement references above match the catalog structure/seed responsibilities: FR-045
governs required row metadata, FR-046 governs fixed published UUID storage/seeding, FR-047 governs
no invented or unsupported rows, SC-031 requires a fully evidenced baseline, and SC-032 requires
stable UUID and no-runtime-generation evidence.

## Functional Requirements

| ID | Design and contract evidence | Acceptance / success evidence | Planned verification |
|----|------------------------------|-------------------------------|----------------------|
| `FR-001` | OpenAPI `X-Company-Id`; `error-catalog.md` Company errors | Scenarios 1, 6, 47, 48; `SC-006`; non-deadline outcomes require conclusive classification before expiry | T031/T034 presence, blank, repeated, malformed, nil, safe-correlation, and zero-state tests prove stage-first `400`; T034 separately proves deadline-first `504` and that a selected `400` is not replaced |
| `FR-002` | OpenAPI operation/header/request-input/response; `plan.md` API boundary | Scenarios 1, 10, 34, 47, 48; `SC-024`, `SC-025` | Contract tests prove Company identifiers absent from request bodies/input/path/query, authoritative header-only input, and explicitly required canonical response CompanyId |
| `FR-003` | `plan.md` Company boundary; no Company port contract | Scenarios 35, 41, 43; `SC-023` | Valid externally unknown UUID succeeds; architecture test proves zero lookup |
| `FR-004` | OpenAPI raw `emissionPointId` plus Stage-5 representation, `x-application-stage-6` stable-failure metadata, and exact lowercase-hyphenated response pattern; `error-catalog.md`; `data-model.md` root | Scenarios 1, 34; `SC-007`, `SC-022` | T030 proves the missing/non-string `INVALID_REQUEST` boundary, no wire preemption, and exact response regex; T033 proves unchanged API forwarding; T029 proves first-in-Stage-6 one-time SP/HTAB trim, canonicalization, and safe `BUSINESS_VALIDATION_FAILED` / `EMISSION_POINT_INVALID` rejection for blank/malformed/nil values before any general-text normalization, lookup, or state; T080/T034 prove new/replay response values |
| `FR-005` | Strict OpenAPI request schema; `error-catalog.md` | Scenarios 10, 44; `SC-022`, `SC-025` | Reject Issuer/establishment/emission fiscal and snapshot properties |
| `FR-006` | `plan.md` earliest-boundary request-clock mapping; `data-model.md` temporal model | Scenarios 26, 27, 51, 52; `SC-013`, `SC-030` | T085 captures one `requestCreationInstant` before body consumption; T034 proves body-crossing-midnight behavior and no later read; Guayaquil conversion/replay remain separate from transactional `createdAt`/`updatedAt` |
| `FR-007` | Approved identification baseline, exact normalized ASCII rule, and staged fixture design | Scenarios 7, 9, 13–16, 53; `SC-010`, `SC-031` | T017 owns raw/normalized/stored/probe vectors and red V3 evidence; T018 creates V5/green PostgreSQL evidence; T030 validates OpenAPI metadata; T045 validates production Java with `applicationNormalizedValue`; code 06/08 uses `^[A-Za-z0-9]{1,20}$` |
| `FR-008` | OpenAPI general Unicode ownership policy plus exact post-normalization buyer-email ASCII dot-atom profile; `data-model.md` buyer fields; authoritative T020 fixture; `error-catalog.md` value-free `EMAIL_INVALID` | Scenarios 31–33, 69; `SC-016` | T033 uses/forwards `rawValue` without email validation; T029 performs the one Stage-6 normalization after emission-point success; T026/T045 exercise accepted and rejected stage-appropriate email outcomes; T036 persists only accepted values; T030 verifies exact metadata; vectors cover case, grammar, forbidden forms, and 64/63/254 boundaries |
| `FR-009` | OpenAPI line cardinality; `data-model.md` aggregate | Scenarios 4, 29, 30; `SC-015` | 1/500 accepted; 0/501 rejected without state |
| `FR-010` | OpenAPI line numeric/text bounds; `data-model.md` decimals and exact product repertoire | Scenarios 2, 3, 31, 32, 49, 50; `SC-004`, `SC-016`, `SC-029` | T017 owns staged product vectors/red V3 evidence; T018 creates V5/green PostgreSQL evidence; T030 uses raw/normalized contract expectations; T050 validates production Java with `applicationNormalizedValue`; product regex is `^[A-Za-z0-9]{1,25}$`; description uses T020 Unicode fixture |
| `FR-011` | OpenAPI `taxRuleId`; approved IVA baseline and upstream-selection boundary | Scenarios 9, 12, 40, 53, 54; `SC-009`, `SC-021`, `SC-031`, `SC-032` | Active/effective immutable `family=IVA` rule selection, zero parent-category entity or automatic classification, direct code/rate and multiple-tax rejection |
| `FR-012` | Exhaustive case-sensitive Stage-5 calculated-path table in `spec.md`; OpenAPI `x-stage-5-property-classification`; calculation model; error catalog | Scenarios 2, 28, 49, 50; `SC-003`, `SC-004`, `SC-014`, `SC-029` | T030 verifies contract metadata; T033 exercises every exact path/subtree, null/wrong-type/equal-value and calculated-plus-ordinary failures; T034 proves HTTP precedence; T079 owns the production pre-binding classifier; every match returns one value-free `PROHIBITED_CALCULATED_FIELD` without `violations` |
| `FR-013` | Payment schema/model, emissionDate-effective reference lookup, and local uniqueness | Scenarios 5, 17–19, 29, 30, 39, 50, 53, 54, 62–68; `SC-011`, `SC-020`, `SC-029`, `SC-032` | T035/T058/T075 prove `(paymentMethodId, emissionDate)`, inclusive boundaries, open end, activity/effectivity combinations, positive/zero/cardinality/duplicate/range |
| `FR-014` | Calculation/reconciliation design | Scenarios 5, 17–19, 50; `SC-003`, `SC-011`, `SC-029` | Exact two-decimal equality and payment-sum overflow tests |
| `FR-015` | OpenAPI Unicode additional-information schema; persisted Java `canonicalName`; authoritative T020 Unicode fixture | Scenarios 29–33, 69, 70; `SC-015`, `SC-016` | T029 applies exact NFC → U+0020 trim/collapse → Locale.ROOT lowercase → 1–300-code-point validation using raw/canonical expectations; T026 gets accepted domain input; T036 gets stored/probe values; cover 0/15/16, `U+0130`, `CANONICAL_NAME_TOO_LONG`, no truncation, and canonical uniqueness |
| `FR-016` | `data-model.md` fixed `USD`; OpenAPI response currency | Scenarios 1, 2, 17; `SC-001`, `SC-007` | Every created/replayed representation and persisted root reports exactly `USD` |
| `FR-017` | Draft state model; OpenAPI `DRAFT` enum | Scenarios 1, 8; `SC-001`, `SC-005`, `SC-007` | Persist/return only internal `DRAFT`; never expose it as SRI status |
| `FR-018` | Local draft identifier model and Application-owned `DraftIdentifierGenerator` boundary | Scenarios 1, 8, 71; `SC-001`, `SC-005` | Application allocates final root/child IDs before candidate construction; persistence preserves them; absence of sequence/access-key/authorization-number fields |
| `FR-019` | `data-model.md` `timestamptz`; T076-only transactional clock design | Scenarios 1, 51, 52, 61, 71; `SC-001`, `SC-007`, `SC-012`, `SC-013`, `SC-030` | T076 calls once after validation/immediately before persistence and assigns the same Instant to `createdAt`/`updatedAt`; T063/API/Domain/mappers never call/supply/overwrite; replay/rollback; no placeholder/physical commit timestamp/post-commit query/`track_commit_timestamp` |
| `FR-020` | `persistence-design.md` candidate/result port and single reactive transaction | Scenarios 1, 11, 22, 46, 71; `SC-001`, `SC-002`, `SC-012`, `SC-028` | Timestamp-free `InvoiceDraftCandidate` → `Uni<PersistedInvoiceDraft>`; real PostgreSQL aggregate-plus-binding commit, equal initial timestamps, injected confirmed rollback, and unresolved-commit replay tests |
| `FR-021` | Persistence failure and deadline model; local foreign keys | Confirmed pre-commit rejection and rollback scenarios; `SC-002`, `SC-028`, `SC-029` | Row-count assertions only after confirmed pre-persistence/rollback outcomes; uncertain/post-commit vectors assert replay and no duplicate instead |
| `FR-022` | OpenAPI created/replay response; `data-model.md` aggregate | Scenarios 1, 2, 17, 34; `SC-001`, `SC-007`, `SC-022` | Field-by-field response/persistence contract including CompanyId, USD, totals and timestamps; no snapshots |
| `FR-023` | `plan.md` exclusions and negative architecture boundary | Scenarios 8, 41, 43; `SC-005`, `SC-008`, `SC-023` | Dependency, trace, persistence, and side-effect inspection proves zero fiscal/SRI action |
| `FR-024` | Repository port contract and `persistence-design.md` aggregate/binding scoping | Scenarios 20–25, 34; `SC-012`, `SC-017` | Every aggregate/binding query or mutation enforces authoritative CompanyId; cross-Company tests; global catalogs prove no Company filter/column |
| `FR-025` | `error-catalog.md`; observability redaction rules | All failure scenarios; `SC-002`, `SC-006`, `SC-028`, `SC-033` | Stable English codes/messages and sensitive/internal-data leakage tests |
| `FR-026` | OpenAPI correlation header; shared API correlation classifier; `operational-requirements.md` | Scenarios 55–58; `SC-033` | Absent/valid/blank/repeated/65-char/unsafe vectors plus oversized bodies with absent, valid, and invalid correlation prove safe 413 propagation without stage-3 takeover |
| `FR-027` | OpenAPI mandatory single-value header; three stable errors; `idempotency-design.md` | Scenarios 20–25, 32, 59–61; `SC-012`, `SC-016`, `SC-018` | T030/T033/T082 prove missing/blank/whitespace/repeated/parser-multiple/comma/over-length/grammar behavior, one-time SP/HTAB trim, no first-value selection, and identical normalized lookup/hash/persistence value; no domain HTTP tests |
| `FR-028` | Identification validation boundary and approved FORMAT_ONLY strategies | Scenarios 7, 13–16; `SC-010` | Exact syntax/length/special-value vectors; zero checksum, online registry, or name lookup |
| `FR-029` | `idempotency-design.md` canonical content version 1 | Scenarios 20, 21, 25, 37, 38, 52, 56, 69, 70; `SC-012`, `SC-016`, `SC-019`, `SC-030`, `SC-033` | Golden fingerprints use only the one accepted Application-normalized/canonical representation and cover property/order/text/decimal/correlation/Company exclusions plus canonical overflow before fingerprinting |
| `FR-030` | Binding schema and `UNIQUE (company_id, idempotency_key_hash)` | Scenarios 20–25, 36; `SC-012`, `SC-018` | New/replay/conflict/lifetime and atomic binding tests |
| `FR-031` | PostgreSQL uniqueness arbitration design | Scenarios 22, 25; `SC-012`, `SC-017` | 50-way same-scope concurrency and cross-Company independence |
| `FR-032` | Transaction and retry design | Scenarios 11, 23, 24, 46; `SC-002`, `SC-012`, `SC-028` | Pre-commit rollback leaves no binding; post-commit response loss replays |
| `FR-033` | Replay flow in `idempotency-design.md` | Scenarios 20, 24, 36, 42, 52, 71; `SC-018`, `SC-023`, `SC-030` | Local scoped persisted-result lookup only; original ID/`createdAt`/`updatedAt`; no clock, identifier allocation, canonical rebuild, new aggregate, recalc/date/Company/auth/external refresh |
| `FR-034` | Local binding repository in persistence design | Scenarios 20–25, 43; `SC-012`, `SC-023` | Architecture evidence proves no delegated idempotency service |
| `FR-035` | Exact Application-owned NFC/U+0020/prohibited-category/code-point policy, canonicalName design, and T020 fixture | Scenarios 31–33, 37, 69, 70; `SC-016`, `SC-019` | T033 API raw handoff; T029 one Application normalization; T026 accepted normalized domain inputs only; T036 stored/probe defenses only; T030 metadata; shared fixture covers accented/decomposed, spaces, tab/CR/LF/NBSP/U+2028/U+2029/Cf, emoji So, case, `U+0130`, boundaries, and no truncation |
| `FR-036` | Negative architecture inventory | Scenarios 35, 41–43; `SC-023` | Source/dependency/config/schema/health inspection proves no Company dependency |
| `FR-037` | Aggregate/persistence ownership model with one immutable normalized Company UUID; repository scope is authoritative in `FR-024` | Scenarios 1, 20–25, 34; `SC-017`, `SC-022` | Root immutability and local child ownership; FR-024 tests enforce aggregate/idempotency Company scope and immutable-global-catalog exclusion without duplicating the repository rule here |
| `FR-038` | Draft-vs-issuance model exclusions | Scenarios 8, 41, 42, 44; `SC-005`, `SC-022`, `SC-023`, `SC-025` | Schema/response/dependency inspection proves no snapshot or issuance data |
| `FR-039` | OpenAPI and build/config negative boundary | Scenario 43; `SC-023`, `SC-024` | Zero security scheme/requirement/Auth/401/403/dependency/config tests |
| `FR-040` | Clean Architecture mapping and candidate/result port in `plan.md` | Scenarios 34, 43, 69, 71; `SC-001`, `SC-016`, `SC-017`, `SC-023` | API maps Company/decoded text only; Application normalizes and allocates IDs; Domain receives normalized values; persistence accepts timestamp-free candidate/returns committed result; dependency tests reject HTTP/security context below API |
| `FR-041` | Executable Stage-5 representation and exhaustive calculated-property boundary; Application-owned Stage 6 ordered as emission-point validation then business-text normalization; Stage 10/11A/ordered 11B; API-exclusive deadline race/HTTP mapping; neutral application/repositories | Scenarios 3, 5, 15, 28, 34, 45–46, 57–61, 69–71; `SC-001`, `SC-012`, `SC-014`, `SC-016`, `SC-022`, `SC-026`, `SC-033` | T030/T033 prove missing/non-string emission-point representation and calculated-over-ordinary precedence at Stage 5; T029/T034 prove Stage-6 `EMISSION_POINT_INVALID` precedes every normalizer invocation, general text, and lookup; T026/T045/T063 prove Stage-10 email grammar; remaining stage separation/order and one normalizer invocation/value; T085 accepts one terminal outcome, then dependent T087 maps only that accepted neutral outcome to HTTP; late results cannot reopen arbitration |
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
| `DR-011` | Calculation ownership and exhaustive strict input classification | Scenarios 2, 5, 28, 40; `SC-003`, `SC-014`, `SC-021` | Caller supplies commercial/payment inputs only; T030/T033/T034/T079 prove every exact calculated path/subtree is rejected before ordinary Stage-5 errors regardless of value/type and never reconciled |
| `DR-012` | Separate earliest-boundary request-date and T076-only transaction timestamp semantics | Scenarios 26, 27, 51, 52, 61, 71; `SC-001`, `SC-012`, `SC-013`, `SC-030` | T085 owns one request Instant before body consumption and the fixed Guayaquil date; T076 owns the sole once-only persistence clock at the pre-persist point; same persistence Instant is assigned to `createdAt`/`updatedAt`; T063/resource/mapper make no clock call; values persist/return/replay; rollback exposes none; no physical timestamp |
| `DR-013` | Validation/error/transaction and emissionDate-effective catalog boundaries | Scenarios 3–7, 9, 12–16, 27, 39, 40, 50, 62–68; `SC-002`, `SC-009`, `SC-010`, `SC-020`, `SC-029` | Invalid combinations reject; payment existence/activity/inclusive dates use emissionDate only; no server/request/transaction/createdAt clock |
| `DR-014` | Approved executable buyer FORMAT_ONLY strategies | Scenarios 7, 13, 14, 16; `SC-010` | RUC 13 ASCII digits, Cédula 10 ASCII digits, passport/foreign case-sensitive ASCII `^[A-Za-z0-9]{1,20}$` valid/invalid/normalization vectors; no checksum or invented rules |
| `DR-015` | Final-consumer rule | Scenario 15; `SC-010` | Exact code/value/name and effective USD threshold boundaries |
| `DR-016` | Zero-value and tax-treatment rules | Scenarios 17–19; `SC-011` | Zero lines/totals/payment accepted only in approved shape; no automatic IVA 0% |
| `DR-017` | Durable idempotency binding | Scenarios 20–24, 36; `SC-012`, `SC-018` | Commit-only binding, conflict, response-loss replay, no time expiry |
| `DR-018` | Company+key scope | Scenarios 20–25; `SC-012`, `SC-017` | Same/different Company tests; neither header nor key treated as credential |
| `DR-019` | Exact Application-owned general Unicode normalization/canonicalName plus T020-owned authoritative fixture and stricter field rules | Scenarios 31–33, 37, 61, 69, 70; `SC-012`, `SC-016`, `SC-019` | T033 raw API handoff; T029 one Stage-6 invocation/value only after emission-point success; T026 accepted Domain input; T045 production buyer/email validation; T036 stored-value defenses only; one NFC pass, prohibited categories/separators, U+0020 only, display/canonical code-point lengths, display case, emoji So, Java Locale.ROOT persisted canonicalName, U+0130 expansion/`CANONICAL_NAME_TOO_LONG`; exact case-sensitive email dot-atom/ASCII/bounds with `EMAIL_INVALID`; strict ASCII and API-only Idempotency-Key remain separate |
| `DR-020` | Root Company ownership and local foreign keys | Scenarios 1, 22, 25, 34; `SC-001`, `SC-017`, `SC-022` | Exactly one immutable CompanyId and zero cross-draft/cross-Company child mixing |
| `DR-021` | Canonical collection ordering | Scenarios 37, 38; `SC-019` | Lines order-sensitive; payments/additional information order-insensitive |
| `DR-022` | Payment uniqueness constraint | Scenario 39; `SC-020` | One payment-method identity per draft at domain and PostgreSQL levels |
| `DR-023` | Opaque emission-point data model and stable Stage-6 violation contract | Scenarios 1, 34, 35, 41, 44; `SC-001`, `SC-007`, `SC-022`, `SC-025` | Store/return canonical opaque ID only; blank/malformed/nil decoded strings return value-free `EMISSION_POINT_INVALID` and no state; no ownership/state lookup or fiscal representation |
| `DR-024` | API correlation mapping and observability design | Scenarios 55–58; `SC-033` | Generate/preserve/replace safely, never echo invalid input, exclude from fingerprint |

## Success Criteria

| ID | Requirements and design traced | Observable evidence |
|----|--------------------------------|---------------------|
| `SC-001` | `FR-016`–`FR-023`, `FR-030`, `FR-037`, `FR-041`; candidate/result, aggregate, response, idempotency, and transaction designs | Under the non-deadline precondition, every logically new valid vector produces one timestamp-free candidate with Application-owned IDs, atomically commits exactly one complete USD `DRAFT` plus children/binding, returns one persisted result with equal initial timestamps and the approved `201`/every `FR-022` field, and has no SRI side effect; deadline-first uses `504 REQUEST_TIMEOUT` instead |
| `SC-002` | `FR-001`, `FR-020`, `FR-021`, `FR-032`, `FR-042`–`FR-044` | Row-count assertions show zero root/child/binding state only for confirmed pre-commit rejection or complete rollback; uncertain/post-commit vectors assert same-scope replay and no duplicate |
| `SC-003` | `FR-012`, `FR-014`; `DR-002`–`DR-010` | Golden calculations identical across repetitions, persistence, packaged JVM, and claimed native runtime |
| `SC-004` | `DR-002`–`DR-005`, `DR-009`, `DR-010` | `2 × 10.00 − 5.00` at 15% always yields `20.00/15.00/2.25/17.25` |
| `SC-005` | `FR-017`, `FR-018`, `FR-023`, `FR-038` | Dependency/trace/storage audit records zero issuance/SRI/PDF/notification effects |
| `SC-006` | `FR-001`, `FR-002`, `FR-025`, `FR-026`, `FR-041` | Under the non-deadline precondition, the complete Company-header matrix conclusively selects the applicable safe correlated `400` and zero state before expiry; controlled races prove deadline-first `504` and that a stage-first Company `400` remains authoritative |
| `SC-007` | `FR-004`, `FR-016`–`FR-019`, `FR-022`; `DR-020`, `DR-023` | Accepted/replayed representation lets client review CompanyId, exact lowercase-hyphenated opaque emissionPointId, buyer, lines, taxes, payments, USD totals, DRAFT and timestamps without issuance |
| `SC-008` | `FR-017`, `FR-023`, `FR-038` | Acceptance suite runs without update/delete/issuance/XML/signature/SRI/PDF/notification facilities |
| `SC-009` | `FR-010`, `FR-011`; `DR-001`, `DR-005`, `DR-008` | Exactly one effective IVA rule per accepted line; unsupported/multiple rejected without state |
| `SC-010` | `FR-007`, `FR-028`; `DR-001`, `DR-014`, `DR-015` | All approved type vectors pass/fail from official rules with zero online lookup |
| `SC-011` | `FR-013`, `FR-014`; `DR-016` | Valid zero-total draft has one `0.00` payment; all other zero-payment shapes reject |
| `SC-012` | `FR-024`, `FR-027`–`FR-034`, `FR-043`; `DR-017`, `DR-018` | Exact required/invalid/multiple header matrix and normalized-value identity; new/replay/conflict/rollback/unresolved/50-way concurrency commits at most one per Company+key scope; replay returns original identifier, `createdAt`, and `updatedAt` without new aggregate/clock/canonical rebuild |
| `SC-013` | `FR-006`, `FR-019`; `DR-012` | Request-date/midnight vectors plus T076 as sole once-only transaction clock owner assigning one Instant to both timestamps; T063 no invocation/overwrite; no physical commit timestamp |
| `SC-014` | `FR-012`; `DR-011` | Every exact calculated path/subtree is detected on the decoded object before binding and rejected with one value-free `PROHIBITED_CALCULATED_FIELD`, even when null, wrong type, equal to computed value, repeated, or combined with an ordinary Stage-5 failure |
| `SC-015` | `FR-009`, `FR-013`, `FR-015` | Exact 500-line/8-distinct-payment/15-additional maxima accepted; maxima+1 reject without state |
| `SC-016` | `FR-008`, `FR-010`, `FR-015`, `FR-035`; `DR-019` | Independent layer-specific suites consume the same authoritative fixture while selecting the stage-appropriate value and responsibility for that layer; this does not mean every layer validates the same literal or performs the same transformation. Evidence covers unchanged API handoff, emission-point validation before exactly one Application normalizer invocation/value, accepted-only Domain input, defensive-only PostgreSQL probes, exact staged ASCII/Unicode/canonicalName vectors, U+0130 150/151 boundaries, exact case-sensitive email dot-atom/ASCII/64/63/254 behavior with `EMAIL_INVALID`, stable overflow, and no truncation; header evidence remains in SC-012 |
| `SC-017` | `FR-024`, `FR-037`; `DR-020` | One immutable canonical CompanyId and local child ownership; zero cross-Company mixing |
| `SC-018` | `FR-030`, `FR-033`; `DR-017` | Equivalent replay loads and returns the original persisted representation for draft lifetime—same identifier, `createdAt`, and `updatedAt`—with no elapsed-time expiry, clock call, canonical rebuild, or new aggregate |
| `SC-019` | `FR-029`; `DR-019`, `DR-021` | Payment/additional reorder replays; line reorder conflicts; no duplicate draft |
| `SC-020` | `FR-013`; `DR-013`, `DR-022` | Unique payment methods plus emissionDate existence/activity/inclusive/open-ended effectivity vectors; duplicate/invalid reference rejects |
| `SC-021` | `FR-011`; `DR-001`, `DR-011` | Tax code/rate comes only from selected approved rule; caller-supplied code/rate rejects |
| `SC-022` | `FR-004`, `FR-005`, `FR-037`, `FR-038`; `DR-020`, `DR-023` | Exactly CompanyId plus canonical lowercase-hyphenated opaque emissionPointId and zero Company/fiscal snapshot fields; approved vectors prove Stage-5 `INVALID_REQUEST` versus Stage-6 `EMISSION_POINT_INVALID`, exact response pattern, replay preservation, and zero invalid state |
| `SC-023` | `FR-003`, `FR-023`, `FR-033`, `FR-036`, `FR-039`, `FR-040` | Create/replay traces and architecture show zero Company/auth/cache/replication behavior |
| `SC-024` | `FR-001`–`FR-003`, `FR-022`, `FR-039` | Static-copy tests plus packaged `/q/openapi` equality prove Company identifiers absent from request/input/path/query, authoritative header-only input, explicitly required response CompanyId, and zero security/Auth/401/403 constructs |
| `SC-025` | `FR-002`, `FR-005`, `FR-012` | Strict body tests reject Company/Issuer/fiscal snapshot and calculated fields with zero state |
| `SC-026` | `FR-041`; exact Stage 10/11A/11B and API-only arbiter design | Pairwise/multi-failure suite proves stage separation/order; API alone races Uni, selects/maps one HTTP result, and discards late outcomes; no HTTP semantics below API |
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
| `AS-003` | `FR-010`, `FR-041`; `DR-003`, `DR-004` | Stage 10 accepts format; 11A calculates gross; ordered 11B rejects discount greater than gross |
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
| `AS-015` | `FR-007`, `FR-041`; `DR-015` | Stage 10 type/value/name then Stage 11B calculated USD 50.00 final-consumer boundary |
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
| `AS-026` | `FR-006`; `DR-012` | Current Ecuador date accepts from the one instant captured before body consumption |
| `AS-027` | `FR-006`; `DR-012`, `DR-013` | Past/future/impossible date rejects without normalization |
| `AS-028` | `FR-012`; `DR-011` | Every exact calculated path/subtree rejects before ordinary Stage-5 errors regardless of value/type; mixed and multiple matches still yield one value-free `PROHIBITED_CALCULATED_FIELD` without `violations` |
| `AS-029` | `FR-009`, `FR-013`, `FR-015` | 500 lines, 8 distinct positive payments, and 15 additional entries accept |
| `AS-030` | `FR-009`, `FR-013`, `FR-015` | 501 lines, 9 payments, or 16 additional entries reject |
| `AS-031` | `FR-008`, `FR-010`, `FR-015`, `FR-035` | Exact text limits accept |
| `AS-032` | `FR-008`, `FR-010`, `FR-015`, `FR-027`, `FR-035` | Over-limit/malformed text rejects; over-length Idempotency-Key returns `IDEMPOTENCY_KEY_INVALID` |
| `AS-033` | `FR-008`, `FR-035`; `DR-019` | Blank/control/duplicate canonical text rejects; buyer-email dot-atom, forbidden-form, ASCII, case and 64/63/254 vectors produce exact `EMAIL_INVALID` outcomes after general-text normalization |
| `AS-034` | `FR-002`, `FR-004`, `FR-037`, `FR-040`, `FR-041` | Header maps to stored/returned CompanyId; valid surrounding emission-point SP/HTAB is trimmed and canonicalized while remaining opaque; blank/malformed/nil decoded strings return safe Stage-6 `EMISSION_POINT_INVALID` and no state |
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
| `AS-051` | `FR-006`, `FR-019`; `DR-012` | Pre-midnight request retains derived date; one separate in-transaction Instant is assigned to both creation timestamps, not treated as physical commit time |
| `AS-052` | `FR-019`, `FR-033`; `DR-012`, `DR-017` | Later-date replay returns original emission date, identifier, `createdAt`, and `updatedAt` without another clock call |
| `AS-053` | `FR-045`, `FR-047`; `DR-001` | Readiness audit blocks any future incomplete reference row; current supported rows all pass |
| `AS-054` | `FR-046`; `DR-001` | Published UUIDs select approved rows and are never startup-generated |
| `AS-055` | `FR-026`; `DR-024` | Missing correlation generates safe UUID |
| `AS-056` | `FR-026`, `FR-029`; `DR-024` | One valid correlation is preserved and excluded from equivalence |
| `AS-057` | `FR-026`, `FR-041`; `DR-024` | Invalid correlation is replaced, never echoed, and returns `INVALID_REQUEST` |
| `AS-058` | `FR-026`, `FR-041`; `DR-024` | Company error precedes correlation error while safe replacement is returned |
| `AS-059` | `FR-027`, `FR-041` | Missing key returns `IDEMPOTENCY_KEY_REQUIRED`; one blank/whitespace/over-length/non-comma grammar-invalid value returns `IDEMPOTENCY_KEY_INVALID`; zero lookup/state |
| `AS-060` | `FR-027`, `FR-041` | Repeated/parser-multiple or comma-containing/comma-combined key returns `IDEMPOTENCY_KEY_MULTIPLE`; no first selection or lookup/state |
| `AS-061` | `FR-019`, `FR-027`, `FR-033`; `DR-012`, `DR-017` | One valid key is SP/HTAB-trimmed once and used identically for lookup/hash/persistence; replay returns original identifier and both timestamps |
| `AS-062` | `FR-013`; `DR-013` | Active method effective exactly on `effectiveFrom` accepts |
| `AS-063` | `FR-013`; `DR-013` | Active method effective exactly on finite `effectiveTo` accepts |
| `AS-064` | `FR-013`; `DR-013` | `emissionDate` before `effectiveFrom` rejects |
| `AS-065` | `FR-013`; `DR-013` | `emissionDate` after finite `effectiveTo` rejects |
| `AS-066` | `FR-013`; `DR-013` | Active open-ended method effective on `emissionDate` accepts |
| `AS-067` | `FR-013`; `DR-013` | Inactive but temporally effective method rejects |
| `AS-068` | `FR-013`; `DR-013` | Active but temporally ineffective method rejects without consulting another clock/date |
| `AS-069` | `FR-008`, `FR-015`, `FR-035`, `FR-040`, `FR-041`; `DR-019` | API forwards decoded business text unchanged; Application validates/canonicalizes emissionPointId before invoking `BusinessTextNormalizer` once per supplied applicable value; Domain/Infrastructure invoke it zero times, and the normalized email reaches Stage 10 unchanged |
| `AS-070` | `FR-015`, `FR-029`, `FR-035`; `DR-019` | `U+0130` lowercase expansion accepts 150 occurrences at 300 canonical code points and rejects 151 at 302 with `CANONICAL_NAME_TOO_LONG` before fingerprint/persistence and without truncation |
| `AS-071` | `FR-018`–`FR-020`, `FR-033`, `FR-040`, `FR-041`; `DR-012`, `DR-017` | Application builds a timestamp-free candidate with final local IDs; T076 opens/joins one reactive transaction, calls the clock once, persists equal timestamps atomically, and returns the committed result; rollback/replay expose no fabricated or regenerated values |

## Stable Error Coverage

| Error code | Governing requirements | Acceptance / planned evidence |
|------------|------------------------|-------------------------------|
| `COMPANY_CONTEXT_REQUIRED` | `FR-001`, `FR-002`, `FR-025` | `AS-006`; safe 400 and zero state |
| `COMPANY_CONTEXT_INVALID` | `FR-001`, `FR-002`, `FR-025` | `AS-047`, `AS-048`; safe 400 and zero state |
| `IDEMPOTENCY_KEY_REQUIRED` | `FR-027`, `FR-041` | `AS-059`; safe 400, no lookup/state |
| `IDEMPOTENCY_KEY_INVALID` | `FR-027`, `FR-041` | `AS-032`, `AS-059`; safe 400 for one invalid normalized value, no lookup/state |
| `IDEMPOTENCY_KEY_MULTIPLE` | `FR-027`, `FR-041` | `AS-060`; safe 400 for repeated/parser-multiple/comma input, no first selection or lookup/state |
| `INVALID_REQUEST` | `FR-002`, `FR-004`, `FR-005`, `FR-026`, `FR-041` | `AS-010`, `AS-034`, `AS-044`, `AS-057`; safe 400/correlation, including missing/non-string `emissionPointId` representation |
| `PROHIBITED_CALCULATED_FIELD` | `FR-012`, `FR-025`; `DR-011` | `AS-028`; every exact path/subtree, null/wrong-type/equal-value, multiple, and calculated-plus-ordinary vector returns safe value-free 422 without `violations` and with zero state |
| `IDEMPOTENCY_CONFLICT` | `FR-030`, `FR-031` | `AS-021`, `AS-038`; safe 409, original unchanged |
| `REQUEST_PAYLOAD_TOO_LARGE` | `FR-026`, `FR-041`, `FR-042` | `AS-045`; over-limit-first safe 413 before Company validation, valid correlation preserved, absent/invalid replaced, no correlation 400 or DB work |
| `BUSINESS_VALIDATION_FAILED` | `FR-004`, `FR-007`–`FR-015`, `FR-028`, `FR-044` | `AS-034`, business rejection scenarios, and `AS-050`; safe 422 and zero state |
| `EMISSION_POINT_INVALID` | `FR-004`, `FR-041`; `DR-023` | `AS-034`; nested value-free violation under `BUSINESS_VALIDATION_FAILED` for blank-after-trim, malformed, or nil decoded strings before general text or lookup |
| `EMAIL_INVALID` | `FR-008`, `FR-041`; `DR-019` | `AS-033`; nested value-free violation under `BUSINESS_VALIDATION_FAILED` for a post-normalization buyer email outside the exact case-sensitive ASCII dot-atom and 64/63/254 bounds |
| `MONETARY_RANGE_EXCEEDED` | `FR-044`; `DR-010` | `AS-050`; nested violation under `BUSINESS_VALIDATION_FAILED` |
| `PERSISTENCE_UNAVAILABLE` | `FR-020`, `FR-021`, `FR-043` | `AS-011`, `AS-046`; safe 503 for unavailable/configured-operation-timeout-first outcomes while request budget remains, then retry same scope |
| `REQUEST_TIMEOUT` | `FR-041`, `FR-043` | `AS-024`, `AS-045`, `AS-046`; cross-cutting deadline-first 504, aggregate/reference remaining-budget propagation, timer cancellation, unresolved-commit replay, and post-response-commit telemetry-only behavior |
| `INTERNAL_ERROR` | `FR-025`, `FR-043` | `AS-046`; safe 500 without internal/sensitive data |

## Deadline Arbitration and Terminal-Outcome Coverage

All winners below are selected by the API adapter's exclusive application-`Uni`/deadline race.
Application and repositories return neutral outcomes; only API emits one HTTP response and discards
late results.

| Controlled race | Required winner | Focused evidence |
|-----------------|-----------------|------------------|
| Non-deadline acceptance scenario | Tested stage conclusively selects its described terminal outcome before expiry | T034 configures a non-expiring or sufficiently remaining controlled deadline; this is stage-first evidence and not a competing `504` oracle |
| Payload size becomes conclusively over 2 MiB before expiry | `413 REQUEST_PAYLOAD_TOO_LARGE` | T032–T034 preserve valid or replace absent/invalid correlation, emit no `400`, and prove no database call |
| Deadline expires before payload size is conclusive | `504 REQUEST_TIMEOUT` | T033–T034 controlled slow-body deadline signal; later size result cannot replace 504 |
| Company/correlation/idempotency invalidity becomes conclusive before expiry | Applicable approved `400`, including the exact required/invalid/multiple idempotency-key code | T031–T034 controlled stage-first header vectors; repeated/comma input never selects first |
| Deadline expires before header classification is conclusive | `504 REQUEST_TIMEOUT` | T034 controlled deadline-first header vectors; no entity decode or later work |
| Replay/conflict, Stage 10/11A/11B outcome, rollback/failure, or commit is accepted before expiry | API maps approved `200`/`201`/`409`/`422`/`500`/`503` result | T019/T021/T022/T029/T034/T038/T052/T063 prove stage separation/order and neutral outcomes before API mapping |
| Deadline expires while lookup or commit outcome is unresolved | `504 REQUEST_TIMEOUT`, uncertain when commit may be in flight | T029/T034/T038 same-Company/key/content replay resolves state without a duplicate |
| Deadline expires after HTTP response commitment | Existing status/body remains authoritative; telemetry only | T034/T040/T041/T085/T088 prove no 504, second response/database write, mutation, or compensation and record the safe event |

## Cross-Cutting Constitutional Evidence

| Risk | Governing source | Planned evidence | Explicit prohibition |
|------|------------------|------------------|----------------------|
| Flyway and local constraints | Constitution IX and Definition of Done | Empty-database migration and constraint tests using the approved reference baseline | No auto-generation, manual schema, legacy dump, or cross-service foreign key |
| Health and observability | Constitution XIII | `operational-requirements.md` liveness/readiness/log/metric/trace tests | No Company/identity/SRI readiness and no sensitive/high-cardinality labels |
| JVM/native evidence | Constitution III/XII | Mandatory packaged JVM suite; native build plus runtime only if claimed | No native claim from build alone |
| Performance and reactive safety | Constitution V/XIII | Documented reference environment, latency profiles, 50-way concurrency, blocked-thread evidence | No Company latency, cache, blocking wait, or hidden blocking wrapper |

## Final Implementation Reconciliation — 2026-07-18

The final source, migration, contract, and test inventory was reconciled against every identifier
range in this matrix. No approved identifier is unmapped and no implementation evidence requires a
new requirement or acceptance scenario.

| Reconciled range or boundary | Final implementation evidence | Status |
|------------------------------|-------------------------------|--------|
| `FR-001`–`FR-047` | API header/body gates, Application orchestration, pure Domain rules, reactive repositories, V1–V5 migrations, exact catalogs, health/telemetry, static and served OpenAPI, and packaged JVM suites | Complete |
| `DR-001`–`DR-024` | Baseline/catalog tests, monetary/domain vectors, emission-date payment lookups, normalization and fingerprint fixtures, Company-scoped persistence, deadline budgets, and timestamp/replay tests | Complete |
| `SC-001`–`SC-033` | Full Gradle suite, migration suite, packaged JVM smoke, performance profiles, architecture/sensitive-data checks, and dynamic `America/Guayaquil` evidence | Complete |
| `AS-001`–`AS-071` | Scenario mappings above are exercised by the focused API/Application/Domain/Infrastructure suites and the packaged create/replay/conflict/failure/date/correlation/maximum paths | Complete |
| Stable errors | Contract and API suites cover every catalogued top-level code; domain violations remain value-free where required, and sensitive/internal values are absent | Complete |
| Governance gate/addendum | `GATE-GOV-001` remains released; T017 red fixture/evidence and T018 V5/green persistence fulfill the corrective assignment without rewriting the historical non-conformity | Complete |
| Shared ASCII equivalence | T017/T018 use stored/probe values, T030 uses raw/contract metadata, and T045/T050 use Application-normalized values from the same `ascii-validation-vectors.json` | Complete |
| Normalization and candidate/result ownership | API performs representation classification only; Application owns emission-point and Unicode normalization plus final IDs; Domain is framework-free; persistence accepts a timestamp-free candidate and returns the committed result | Complete |
| Timestamp ownership | The earliest API boundary owns request time; T076 alone invokes the persistence clock once and assigns equal values; replay returns originals without another clock or identifier allocation | Complete |
| Deadline ownership | One pre-body API timer and atomic terminal arbitrate HTTP; Application/repositories receive neutral remaining budgets; configured-timeout and deadline-timeout ownership remain distinct; post-commit expiry is telemetry-only | Complete |
| Prohibited boundaries | Architecture, dependency/config, schema, OpenAPI, readiness/trace, and runtime evidence show no auth/identity, Company dependency/cache/snapshot, Company-scoped global catalog, SRI/fiscal side effect, HTTP below API, or sensitive metric label | Complete |

Executed closure evidence is recorded in `quickstart.md` and
`operational-requirements.md`. `spotlessCheck test` passed 70 tests, the empty/V3→V5 migration
suite passed 8 tests against PostgreSQL 18.4, and both packaged JVM smoke and performance suites
passed. Native runtime remains optional, deferred, and explicitly unclaimed in `plan.md`.
