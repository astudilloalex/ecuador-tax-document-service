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

The shell value is only test input. The service captures `requestCreationInstant` exactly once and
derives the authoritative expected date in `America/Guayaquil`; it does not use the test shell's
clock or recalculate the date at commit.

Before a successful create test, verify the selected exact baseline UUIDs exist in the
Flyway-seeded database. Stop rather than substituting a sample value when either row is unavailable.

The valid request must contain:

- one valid mixed-case `X-Company-Id` to prove canonical lowercase storage/response;
- `Idempotency-Key: invoice-draft-e2e-001`;
- one valid `X-Correlation-Id`, such as `billing-e2e-001`, for the preservation vector;
- one opaque non-nil UUID `emissionPointId`;
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
- no Issuer/establishment/emission fiscal snapshot exists;
- no sequence, access key, XML, signature, certificate, SRI, PDF, queue, or notification effect
  occurs.

## 5. Verify Company Header Validation

Execute each case and inspect API results and database row counts:

| Case | Expected outcome | Expected state |
|------|------------------|----------------|
| Missing header | `400 COMPANY_CONTEXT_REQUIRED` | No draft/children/binding |
| Whitespace-only header | `400 COMPANY_CONTEXT_REQUIRED` | No state |
| Malformed UUID | `400 COMPANY_CONTEXT_INVALID` | No state |
| Nil UUID | `400 COMPANY_CONTEXT_INVALID` | No state |
| Multiple header values | `400 COMPANY_CONTEXT_INVALID` | No state |
| Mixed-case valid UUID | Continues when later rules pass | Canonical lowercase CompanyId stored/returned |
| Externally unknown valid UUID | Continues when later rules pass | No external lookup; supplied Company partition used |

No test expects Company existence, active state, fiscal eligibility, caller entitlement, tenant
ownership, or emission-point ownership validation.

## 6. Verify Correlation Initialization and Precedence

| Correlation input | Expected outcome |
|-------------------|------------------|
| Header absent | Generate one safe UUID and return it on the terminal response |
| One value satisfying the 1–64 character safe grammar | Trim, preserve, and return it unchanged |
| Blank value | `400 INVALID_REQUEST`; return a generated safe UUID; never echo input; no state |
| Repeated values | `400 INVALID_REQUEST`; return a generated safe UUID; never echo input; no state |
| 65-character value | `400 INVALID_REQUEST`; return a generated safe UUID; never echo input; no state |
| Value containing a space, slash, control, or non-ASCII character | `400 INVALID_REQUEST`; return a generated safe UUID; never echo input; no state |

Combined-failure vectors MUST prove:

- invalid Company plus invalid correlation returns the applicable Company error, not the
  correlation error, while using a safe replacement UUID;
- a body over 2 MiB plus invalid Company/correlation returns
  `REQUEST_PAYLOAD_TOO_LARGE`, still with a safe correlation UUID;
- invalid correlation plus invalid idempotency key returns the correlation `INVALID_REQUEST`;
- changing, omitting, or invalidating correlation never changes idempotency equivalence.

Run the payload-size vectors both with `Content-Length` and with chunked transfer. The Quarkus HTTP
upload limit must produce the feature's `413` Problem Details and safe correlation before REST
entity decoding. For bodies within the limit, the single pre-entity request gate must evaluate
Company, correlation, and idempotency headers before Jackson/Bean Validation evaluates the body.
Malformed JSON or an unknown property combined with an earlier invalid header must therefore return
the earlier header outcome.

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

A Company path or query value cannot substitute for the required header. OpenAPI must contain no
Company path/query/body parameter.

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
9. Advance the test clock to a later Ecuadorian date and replay. Expect the original draft and
   emission date without current-date revalidation.

Database inspection must show only key/fingerprint hashes and normalization version in the binding;
no raw key, correlation value, CompanyId duplication in the fingerprint, or normalized buyer
request is stored.

## 9. Verify Rollback, Availability, and Timeout

Inject a failure after each planned persistence phase. Expected: zero root, child, and binding rows
from every pre-commit failure.

| Failure | Expected outcome |
|---------|------------------|
| PostgreSQL unavailable before commit | `503 PERSISTENCE_UNAVAILABLE` |
| Overall 10-second deadline exceeded | `504 REQUEST_TIMEOUT` |
| Unexpected safe failure | `500 INTERNAL_ERROR` |

Every response returns a safe `X-Correlation-Id` and safe Problem Details. Retry unavailable or
timeout cases with the same Company/key/content. If commit actually completed before response loss,
the retry must return the original draft.

## 10. Verify Fiscal, Monetary, and Boundary Vectors

Using the exact approved baseline identifiers, validate at least:

- `2 × 10.00 − 5.00` with tax rule
  `5b34b038-931c-50e3-a84c-10af272fdcd4`, yielding gross `20.00`, net `15.00`, IVA `2.25`, and
  contribution `17.25`; this is a mathematical vector, not a claim of universal 15% applicability;
- all four supported IVA treatments and separate zero-tax grouping;
- unsupported ICE/IRBPNR/multiple-tax rejection;
- all supported buyer identification types and final-consumer threshold;
- zero-value draft with IVA 0% rule `84cb3f03-574b-54de-9e73-efb8d485476a` only when that
  treatment is appropriate, and exactly one `0.00` payment using
  `639f2b7e-10a3-5d92-a1a3-28223896f5b5`;
- caller selection of the appropriate published tax rule without service-side product
  classification, including the 5% construction-material applicability boundary;
- payment mismatch and duplicate payment method;
- exactly 8 distinct approved positive payment methods accepted when amounts reconcile, and 9
  payments rejected before persistence;
- current date derived from the single `requestCreationInstant`, past/future/impossible rejection,
  a commit crossing Guayaquil midnight, and later-date replay;
- text and collection maxima plus maximum-plus-one rejection;
- a body exactly `2,097,152` bytes proceeding to the next validation stage;
- a larger body returning `413 REQUEST_PAYLOAD_TOO_LARGE` before Company evaluation.

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
- no Company/SRI/security outbound span or invocation;
- no Company, tenant, subject, role, authorization, or fiscal-snapshot persistence;
- no application cache.

## 12. Record Operational and Performance Evidence

Run every profile in `operational-requirements.md` with the approved seeded reference baseline.
Record environment, warm-up, sample count, percentiles, resource usage, event-loop
blocked-thread evidence, and pool state. Company Service latency must not appear in the workload or
budget.

Verify that the one captured `requestCreationInstant` is functional date evidence, not the latency
timer or commit timestamp. Verify separately that correlation validation/generation is included in
request duration but correlation values never become metric labels or idempotency content.

## 13. JVM and Optional Native Evidence

The packaged JVM smoke suite is mandatory and covers migration, startup, OpenAPI, health, create,
normalization, date capture, monetary envelopes, replay, conflict, rollback, unavailable/timeout,
and correlation using the exact approved seeds. Its OpenAPI check fetches `/q/openapi` from the
running service, resolves it, compares it semantically with the canonical contract, and verifies
header-only Company context plus the absence of security, Authorization, `401`, and `403` content.

Native support is optional. If claimed, record both build and runtime evidence for the same critical
paths using the approved seeds. Otherwise document native as deferred or unsupported with evidence
while retaining the JVM deployment baseline.
