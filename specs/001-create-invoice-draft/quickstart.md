# Quickstart: Validate Create Invoice Draft

**Feature**: `001-create-invoice-draft`

**Purpose**: Phase 1 validation guide; it does not create implementation tasks

## Approved Reference Inputs

`reference-data-baseline.md` approves catalog version `SRI-OFFLINE-2.32-TARGET-1`. Every command
and fixture MUST copy its identifiers exactly; invented or runtime-generated substitutes are
prohibited. Frequently used validation identifiers are:

```bash
IVA_ZERO_RULE_ID="84cb3f03-574b-54de-9e73-efb8d485476a"
IVA_FIFTEEN_RULE_ID="5b34b038-931c-50e3-a84c-10af272fdcd4"
PAYMENT_WITHOUT_FINANCIAL_SYSTEM_ID="639f2b7e-10a3-5d92-a1a3-28223896f5b5"
```

The reference namespace is `32576bbf-b70d-5c24-98ff-d5f9b48e8826`. The later Flyway seed and
published contract MUST agree byte-for-byte with the complete approved baseline.

## Prerequisites

- `GATE-GOV-001` is released by `astudilloalex`; the mandatory current `$speckit-analyze` gate
  remains before T017, while T017/T018 remain pending and T019 remains blocked until both pass;
- Java 25;
- repository Gradle wrapper;
- PostgreSQL 18.4-compatible database or approved PostgreSQL test container;
- the approved identification-type, IVA-rule, and payment-method baseline;
- no Keycloak, OIDC provider, token, Company Service, Company stub, gateway, or BFF endpoint.

Only PostgreSQL and mandatory local service infrastructure are runtime dependencies.

## 1. Verify the Empty-Database Baseline

After the later implementation supplies the planned migrations, run against an empty PostgreSQL
database:

```bash
./gradlew clean test
./gradlew quarkusBuild
```

Expected evidence:

- Flyway creates the complete schema and every approved local reference row from empty state;
- immutable V3 is followed by pending T017 V5 and T018 proves the exact final ASCII constraints,
  five-migration history, and successful Flyway validation;
- each seeded tax-rule and payment-method UUID matches the published baseline;
- production schema auto-generation is disabled;
- no Company/Issuer/establishment/emission-point master table exists;
- no tenant, subject, authorization, Company-context, or fiscal-snapshot column exists;
- idempotency uniqueness is CompanyId plus key hash;
- all monetary columns use the approved numeric envelopes;
- the packaged JVM artifact is produced.

## 2. Start the JVM Service

Configure PostgreSQL and normal observability settings, then start the supported JVM runtime.
Development validation may use:

```bash
./gradlew quarkusDev
```

Packaged JVM smoke uses:

```bash
java -jar build/quarkus-app/quarkus-run.jar
```

There must be no OIDC, JWT, Keycloak, Company-client, Company-timeout, token-propagation, Company
health, or application-cache configuration.

## 3. Verify Health Boundaries

Expected:

- liveness is `UP` when internal process health is viable;
- readiness is `UP` only after PostgreSQL, migrations, and mandatory approved local catalogs are
  ready;
- stopping PostgreSQL changes readiness to `DOWN` while liveness remains `UP`;
- restoring PostgreSQL restores readiness;
- readiness uses the same datasource as draft persistence and performs no destructive operation;
- no health request calls Company Service, an identity provider, gateway/BFF, or SRI.

## 4. Prepare Runtime-Safe Inputs

The operation is `POST /api/v1/invoice-drafts`; CompanyId appears only in the header. CompanyId and
`emissionPointId` are opaque external UUIDs and are not seeded reference-data identifiers. The tax
rule and payment method are different: they MUST use approved published/seeded UUIDs.

Derive the manual-test date dynamically in the required civil timezone immediately before building
the request:

```bash
ECUADOR_DATE="$(TZ=America/Guayaquil date +%F)"
```

The shell value is only test input. The service captures `requestCreationInstant` exactly once at
the earliest request boundary before body consumption and derives the authoritative expected date
in `America/Guayaquil`; it does not use the test shell's clock or recalculate the date later.

Before a successful create test, verify the selected exact baseline UUIDs exist in the
Flyway-seeded database. Stop rather than substituting a sample value when either row is unavailable.

The valid request must contain:

- one valid mixed-case `X-Company-Id` to prove canonical lowercase storage/response;
- `Idempotency-Key: invoice-draft-e2e-001`;
- one valid `X-Correlation-Id`, such as `billing-e2e-001`, for the preservation vector;
- one opaque non-nil UUID `emissionPointId`; use surrounding ASCII SP/HTAB and mixed-case hex in
  one accepted vector to prove Stage-6 trim and canonicalization;
- `emissionDate` equal to `$ECUADOR_DATE`;
- final-consumer buyer data only when the approved identification baseline and threshold support it;
- `taxRuleId: 5b34b038-931c-50e3-a84c-10af272fdcd4` for the 15% mathematical vector, or another
  exact approved tax-rule UUID appropriate to the upstream-selected transaction;
- `paymentMethodId: 639f2b7e-10a3-5d92-a1a3-28223896f5b5` for official payment code `01`, or
  another exact approved payment UUID;
- payment amount exactly equal to the service-calculated grand total.

Expected new result:

- HTTP `201`;
- the valid supplied `X-Correlation-Id` is returned unchanged;
- `Idempotency-Replayed: false`;
- response `companyId` is canonical lowercase hyphenated UUID text;
- status is `DRAFT` and currency is exactly `USD`;
- the response includes every captured and calculated field required by `FR-022`;
- response `createdAt` and `updatedAt` are equal to the exact immutable UTC Instant captured by
  T076's single clock invocation inside the active persistence transaction after business
  validation and immediately before root persistence; neither is a physical commit timestamp;
  T063, API, Domain, and mappers neither supply nor overwrite them;
- the successful path crossed
  `persist(InvoiceDraftCandidate) -> Uni<PersistedInvoiceDraft>` with final Application-allocated
  local identifiers, no timestamp in the candidate, and no identifier replacement in persistence;
- no Issuer/establishment/emission fiscal snapshot exists;
- no sequence, access key, XML, signature, certificate, SRI, PDF, queue, or notification effect
  occurs.

## 5. Verify Company Header Validation

Execute each case and inspect API results and database row counts:

| Case | Expected outcome | Expected state |
|------|------------------|----------------|
| Missing header | `400 COMPANY_CONTEXT_REQUIRED` | No draft/children/binding |
| ASCII SP/HTAB-only header | `400 COMPANY_CONTEXT_REQUIRED` | No state |
| Malformed UUID | `400 COMPANY_CONTEXT_INVALID` | No state |
| Nil UUID | `400 COMPANY_CONTEXT_INVALID` | No state |
| Multiple header values | `400 COMPANY_CONTEXT_INVALID` | No state |
| Mixed-case valid UUID | Continues when later rules pass | Canonical lowercase CompanyId stored/returned |
| Externally unknown valid UUID | Continues when later rules pass | No external lookup; supplied Company partition used |

No test expects Company existence, active state, fiscal eligibility, caller entitlement, tenant
ownership, or emission-point ownership validation.

Emission-point validation has its own deterministic boundary:

| `emissionPointId` case | Expected outcome | Expected state |
|------------------------|------------------|----------------|
| Property missing or JSON value is not a string | `400 INVALID_REQUEST` at Stage 5 | No state |
| Decoded string is blank/trim-to-empty, malformed UUID, or nil UUID | `422 BUSINESS_VALIDATION_FAILED` with value-free `EMISSION_POINT_INVALID`, field `emissionPointId`, stage `NORMALIZATION` | No lookup/state |
| Valid UUID surrounded by ASCII SP/HTAB | Continue after one trim; persist/return canonical lowercase hyphenated UUID | Normal create/replay rules |

The Stage-6 emission-point check precedes general business-text normalization. API forwards every
decoded string unchanged and never echoes an invalid value in a failure.

## 6. Verify Correlation Initialization and Precedence

| Correlation input | Expected outcome |
|-------------------|------------------|
| Header absent | Generate one safe UUID and return it on the terminal response |
| One value satisfying the 1–64 ASCII-character safe grammar | Trim surrounding ASCII SP/HTAB once, preserve, and return normalized value |
| Blank value | `400 INVALID_REQUEST`; return a generated safe UUID; never echo input; no state |
| Repeated values | `400 INVALID_REQUEST`; return a generated safe UUID; never echo input; no state |
| 65-character value | `400 INVALID_REQUEST`; return a generated safe UUID; never echo input; no state |
| Value containing a space, slash, control, or non-ASCII character | `400 INVALID_REQUEST`; return a generated safe UUID; never echo input; no state |

Combined-failure vectors MUST prove:

- invalid Company plus invalid correlation returns the applicable Company error, not the
  correlation error, while using a safe replacement UUID;
- a body conclusively detected over 2 MiB before deadline expiry returns
  `REQUEST_PAYLOAD_TOO_LARGE` before Company evaluation with a generated correlation UUID when
  absent, the preserved value when valid, or a safe replacement UUID when invalid; correlation
  invalidity never replaces the 413 outcome with `400`, and no normal body deserialization,
  idempotency/reference lookup, validation, calculation, or database operation follows;
- deadline expiry before payload-size classification is conclusive returns `REQUEST_TIMEOUT`, and a
  later over-limit observation does not replace that `504`;
- invalid correlation plus invalid idempotency key returns the correlation `INVALID_REQUEST`;
- changing, omitting, or invalidating correlation never changes idempotency equivalence.

Run the payload-size vectors both with `Content-Length` and with chunked transfer. When over-limit
classification wins the deadline race, the Quarkus HTTP upload limit must produce the feature's
`413` Problem Details and safe correlation before REST entity decoding. For bodies within the limit,
the single pre-entity request gate must evaluate Company, correlation, and idempotency headers before
Jackson/Bean Validation evaluates the body.
Malformed JSON or an unknown property combined with an earlier invalid header must therefore return
the earlier header outcome.

### Idempotency-Key header matrix

The header is mandatory and must yield exactly one field value after HTTP parsing. The API trims
only leading/trailing ASCII SP/HTAB once, preserves internal characters and case, and validates the
normalized value against
`^[\x21-\x2B\x2D-\x7E](?:[\x20-\x2B\x2D-\x7E]{0,126}[\x21-\x2B\x2D-\x7E])?$`.

| Input | Expected outcome |
|-------|------------------|
| Missing header | `400 IDEMPOTENCY_KEY_REQUIRED`; no lookup/state |
| One blank or SP/HTAB-only value | `400 IDEMPOTENCY_KEY_INVALID`; no lookup/state |
| One normalized 129-character value | `400 IDEMPOTENCY_KEY_INVALID`; no lookup/state |
| One value with a control, non-ASCII character, or other non-comma grammar failure | `400 IDEMPOTENCY_KEY_INVALID`; no lookup/state |
| Repeated header fields | `400 IDEMPOTENCY_KEY_MULTIPLE`; first value is never selected; no lookup/state |
| Parser-produced multiple values | `400 IDEMPOTENCY_KEY_MULTIPLE`; no lookup/state |
| Any comma-containing/comma-combined value such as `key-a,key-b` | `400 IDEMPOTENCY_KEY_MULTIPLE`; no lookup/state |
| `  Draft Key 42  ` using only surrounding SP/HTAB | Continue with normalized case-sensitive `Draft Key 42`; use identical value for lookup/hash/persistence |

Domain tests MUST NOT reproduce this matrix. They receive already normalized and validated values
and know nothing about HTTP headers, cardinality, trimming adapters, or HTTP error responses.

## 7. Verify Strict Request Fields

Add each prohibited property separately:

- `companyId`;
- `issuerId`;
- Issuer RUC/legal/trade/address/fiscal fields;
- establishment or emission-point fiscal snapshot fields;
- line gross/net/tax base/tax amount/line total;
- grouped taxes, subtotal, discount total, or grand total;
- direct tax code/rate instead of `taxRuleId`.

Expected:

- unknown Company/Issuer/snapshot fields produce `INVALID_REQUEST`;
- recognized calculated fields produce `PROHIBITED_CALCULATED_FIELD`;
- no state is persisted.

A Company path or query value cannot substitute for the required header. Company identifiers are
forbidden in request bodies and input schemas. OpenAPI must contain no Company path/query input,
while canonical response `companyId` remains required by the feature contract.

## 8. Verify Idempotency

1. Repeat the original command with the same Company/key and equivalent content. Expect `200`,
   `Idempotency-Replayed: true`, and the original draft.
2. Change business content with the same Company/key. Expect `409 IDEMPOTENCY_CONFLICT` and no
   mutation.
3. Use the same key and content with a different valid Company UUID. Expect an independent `201`
   draft and binding.
4. Reorder JSON properties, payments, or additional information only. Expect equivalent replay.
5. Reorder invoice lines. Expect conflict because line order is business-significant.
6. Change only `X-Correlation-Id`. Expect equivalent replay.
7. Submit at least 50 concurrent equivalent commands in one Company/key scope. Expect exactly one
   committed draft and binding; all successful outcomes identify that draft.
8. Simulate response loss after commit and retry equivalent content. Expect the original draft.
9. Advance the test clocks to a later Ecuadorian date/Instant and replay. Expect the original draft
   identifier, emission date, `createdAt`, and `updatedAt` without current-date revalidation,
   identifier allocation, clock invocation, persisted-canonical-value rebuild, or another
   aggregate. The retry's single Stage-6 pass is used only for fingerprint comparison.

Database inspection must show only key/fingerprint hashes and normalization version in the binding;
no raw key, correlation value, CompanyId duplication in the fingerprint, or normalized buyer
request is stored.

## 9. Verify Rollback, Availability, and Timeout

Inject a failure after each planned persistence phase. Expect zero root, child, and binding rows
only when rejection before persistence or complete rollback is confirmed. An unresolved commit
outcome makes no zero-state claim.

| Failure | Expected outcome |
|---------|------------------|
| PostgreSQL unavailable before commit | `503 PERSISTENCE_UNAVAILABLE` |
| Configured database-operation timeout fires while request budget remains | `503 PERSISTENCE_UNAVAILABLE` |
| Overall deadline expires before the current outcome is conclusive | `504 REQUEST_TIMEOUT` |
| Unexpected safe failure | `500 INTERNAL_ERROR` |

Every response returns a safe `X-Correlation-Id` and safe Problem Details. Retry unavailable or
timeout cases with the same Company/key/content. If commit actually completed before response loss,
the retry must return the original draft.

Using a controllable deadline signal rather than real sleeps, prove that one monotonic 10-second
timer starts at the earliest matched route before body consumption, is never restarted, remains
armed through serialization, and is cancelled when the response ends. Prove the API adapter alone
races the application `Uni`, atomically accepts exactly one terminal result, discards late
application/database results, and maps Company `400`, timeout `504`, and every other HTTP outcome
only afterward. Deadline-first processing returns correlated `REQUEST_TIMEOUT`; stage-first remains
selected. Application, domain, and repositories expose only transport-neutral outcomes and no HTTP
status, exception, envelope, or arbiter. Cover payload-size, headers, replay/conflict,
validation/calculation, confirmed persistence, and unresolved-commit races.

Application and both aggregate and reference-data repository probes must observe a decreasing
remaining `Duration`. Reference lookups clamp every pool/query subscription to the minimum of the
configured database-operation timeout and remainder, exercise both sides of that minimum, and
start no database operation for a zero/negative remainder. A write transaction is clamped to the
lesser of the remainder and five seconds. An unresolved or completed commit is recovered by
equivalent same-scope replay, never compensating deletion.

After HTTP response commitment, trigger deadline expiry and verify the chosen status/body is not
rewritten, no `504` or second response is emitted, no domain/database mutation or compensation
occurs, existing serialization may complete, and only the safe
`request_deadline_exceeded_after_response_commit` event/counter is recorded without buyer PII,
request body, raw idempotency key, or token.

## 10. Verify Fiscal, Monetary, and Boundary Vectors

Using the exact approved baseline identifiers, validate at least:

- Stage 10 only evaluates calculation-independent structure/catalog/text/basic amount rules;
  Stage 11A calculates gross/discount/base/tax/line/subtotal/invoice/payment-reference values; and
  Stage 11B evaluates exact order: range/overflow, discount-over-gross, final-consumer total limit,
  total-dependent payment shape/positivity, then payment reconciliation. Use competing-failure
  vectors that would expose any different order;

- `2 × 10.00 − 5.00` with tax rule
  `5b34b038-931c-50e3-a84c-10af272fdcd4`, yielding gross `20.00`, net `15.00`, IVA `2.25`, and
  contribution `17.25`; this is a mathematical vector, not a claim of universal 15% applicability;
- all four supported IVA treatments and separate zero-tax grouping;
- unsupported ICE/IRBPNR/multiple-tax rejection;
- all supported buyer identification types and final-consumer threshold;
- passport/foreign-identification case-sensitive ASCII `^[A-Za-z0-9]{1,20}$`: accept `A1234567`
  and `EC9Z`; reject `A-123`, `A 123`, `Á123`, empty, and 21 characters after the one permitted
  surrounding SP/HTAB trim;
- product-code case-sensitive ASCII `^[A-Za-z0-9]{1,25}$`: accept `ABC123` and `sku9`; reject
  `ABC-123`, `ABC 123`, `ÁBC1`, empty, and 26 characters after the one permitted trim;
- zero-value draft with IVA 0% rule `84cb3f03-574b-54de-9e73-efb8d485476a` only when that
  treatment is appropriate, and exactly one `0.00` payment using
  `639f2b7e-10a3-5d92-a1a3-28223896f5b5`;
- caller selection of the appropriate published tax rule without service-side product
  classification, including the 5% construction-material applicability boundary;
- payment mismatch and duplicate payment method;
- payment-method lookup passes `(paymentMethodId, emissionDate)` and accepts exactly-on
  `effectiveFrom`, exactly-on finite `effectiveTo`, and open-ended active rows; rejects before-start,
  after-end, inactive-but-temporally-effective, and active-but-ineffective rows without consulting
  server current date, request arrival, transaction time, or `createdAt`;
- exactly 8 distinct approved positive payment methods accepted when amounts reconcile, and 9
  payments rejected before persistence;
- current date derived from the single earliest-boundary `requestCreationInstant`,
  past/future/impossible rejection, body consumption or commit crossing Guayaquil midnight, and
  later-date replay;
- general human-readable text vectors shared across layers: prove API forwards decoded business
  text unchanged, Application invokes `BusinessTextNormalizer` exactly once per supplied applicable
  value at Stage 6, and Domain/Infrastructure invoke it zero times; cover NFC accented Latin;
  decomposed/composed equivalence; surrounding and repeated internal `U+0020`; tab, newline, NBSP,
  `U+2028`, `U+2029`, and zero-width `Cf` rejection; assigned emoji `So` acceptance when field
  format/length permits; case preservation; Unicode-code-point maximum/max+1. Verify
  `canonicalName` is NFC → surrounding U+0020 trim → collapse U+0020 runs → Java `Locale.ROOT`
  lowercase → code-point count, is never truncated, is rejected over 300 with
  `CANONICAL_NAME_TOO_LONG` before fingerprint/persistence, is persisted when accepted, and is not
  recalculated by PostgreSQL locale. Include 150 occurrences of `U+0130` as exactly 300 canonical
  code points and 151 occurrences as 302/rejected;
- text and collection maxima plus maximum-plus-one rejection;
- a body exactly `2,097,152` bytes proceeding to the next validation stage;
- a larger body conclusively detected before deadline expiry returning
  `413 REQUEST_PAYLOAD_TOO_LARGE` before Company evaluation, plus the deadline-first `504` vector.

Numeric-envelope evidence MUST cover all of the following without persistence errors or silent
rounding/clamping:

| Vector | Expected outcome |
|--------|------------------|
| Quantity `0.000001` and `999999.999999` | Accepted when all resulting money remains in range |
| Unit price `0` and `999999.999999` | Accepted when all resulting money remains in range |
| Quantity `0`, negative, `1000000.000000`, or more than six fractional digits | Business rejection; no state |
| Unit price negative, `1000000.000000`, or more than six fractional digits | Business rejection; no state |
| Money `0.00` and `999999999999999.99` | Accepted where the applicable business rule permits |
| Money `1000000000000000.00` or negative | `BUSINESS_VALIDATION_FAILED` with `MONETARY_RANGE_EXCEEDED`; no state |
| Individually valid quantity and price whose exact product exceeds the money maximum | Same range error before persistence |
| Individually valid lines whose subtotal, grouped tax, payment sum, or grand total overflows | Same range error before persistence |
| Approved catalog rate `0.00` or `100.00` | Accepted for the applicable approved treatment |
| Catalog rate below `0.00`, above `100.00`, or with excess precision | Baseline/reference rejection; never rounded or seeded |

## 11. Verify the Negative Architecture Boundary

Static and runtime evidence must show:

- no `CompanyContextPort`, Company client/adapter/stub/table/cache/readiness/timeout/retry;
- no authentication/security dependency, scheme, requirement, Authorization header, `401`, or
  `403`;
- no HTTP request/header/security/thread-local/Gateway object in application or domain;
- no NFC normalization, business trim, space collapse, lowercase conversion, or `canonicalName`
  derivation in API, Domain, Infrastructure, or persistence mappers; Application Stage 6 is the
  sole owner;
- the persistence port accepts only timestamp-free `InvoiceDraftCandidate` and returns
  `Uni<PersistedInvoiceDraft>`; no HTTP type, Panache entity, placeholder timestamp, or HTTP error
  crosses it;
- Application alone allocates final local draft/child identifiers, while T076 alone invokes the
  transactional clock and assigns one Instant to both `createdAt` and `updatedAt`;
- no Company/SRI/security outbound span or invocation;
- no Company, tenant, subject, role, authorization, or fiscal-snapshot persistence;
- authoritative CompanyId on every aggregate/binding query or mutation, with no `company_id`
  column or Company filter on global VAT/payment/identification/SRI reference catalogs;
- no application cache.

## 12. Record Operational and Performance Evidence

Run every profile in `operational-requirements.md` with the approved seeded reference baseline.
Record environment, warm-up, sample count, percentiles, resource usage, event-loop
blocked-thread evidence, aggregate/reference budget clamping, controlled deadline arbitration,
post-response-commit telemetry-only expiry, and pool state. Company Service latency must not appear
in the workload or budget.

Verify that the one captured `requestCreationInstant` is functional date evidence, not the latency
timer or commit timestamp. Verify separately that correlation validation/generation is included in
request duration but correlation values never become metric labels or idempotency content.

## 13. JVM and Optional Native Evidence

The packaged JVM smoke suite is mandatory and covers migration, startup, OpenAPI, health, create,
normalization, date capture, monetary envelopes, replay, conflict, rollback, unavailable/timeout,
and correlation using the exact approved seeds. Its OpenAPI check fetches `/q/openapi` from the
running service, resolves it, compares it semantically with the canonical contract, and verifies
header-only Company input, explicitly contracted response CompanyId, plus the absence of security,
Authorization, `401`, and `403` content.

Native support is optional. If claimed, record both build and runtime evidence for the same critical
paths using the approved seeds. Otherwise document native as deferred or unsupported with evidence
while retaining the JVM deployment baseline.
