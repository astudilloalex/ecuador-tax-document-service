# Quickstart: Validate Create Invoice Draft

**Feature**: `001-create-invoice-draft`
**Purpose**: Phase 1 validation guide; it does not create implementation tasks

## Prerequisites

- Java 25;
- repository Gradle wrapper;
- PostgreSQL 18.4-compatible database or approved PostgreSQL test container;
- no Keycloak, OIDC provider, token, Company Service, Company stub, gateway, or BFF endpoint.

Only PostgreSQL and mandatory local service infrastructure are needed.

## 1. Verify the Empty-Database Baseline

Against an empty PostgreSQL database, run the future implementation's migration and test suites:

```bash
./gradlew clean test
./gradlew quarkusBuild
```

Expected evidence:

- Flyway creates the complete schema and local reference data from empty state;
- production schema auto-generation is disabled;
- no Company/Issuer/establishment/emission-point master table exists;
- no tenant, subject, authorization, Company-context, or fiscal-snapshot column exists;
- idempotency uniqueness is CompanyId plus key hash;
- the packaged JVM artifact is produced.

## 2. Start the JVM Service

Configure the PostgreSQL datasource and normal observability settings, then start the supported JVM
runtime. Development validation may use:

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

- liveness `UP` when the process is viable;
- readiness `UP` after PostgreSQL and local migration/catalog initialization are ready;
- stopping PostgreSQL changes readiness to `DOWN` while liveness remains `UP`;
- restoring PostgreSQL restores readiness;
- no health request calls Company Service, an identity provider, gateway/BFF, or SRI.

## 4. Create a Valid Draft

The effective operation is `POST /api/v1/invoice-drafts`; CompanyId appears only in the header.

```bash
curl --request POST 'http://localhost:8080/api/v1/invoice-drafts' \
  --header 'Content-Type: application/json' \
  --header 'X-Company-Id: A0B1C2D3-E4F5-4678-9ABC-DEF012345678' \
  --header 'Idempotency-Key: invoice-draft-example-001' \
  --header 'X-Correlation-Id: billing-e2e-001' \
  --data '{
    "emissionPointId": "33333333-3333-4333-8333-333333333333",
    "emissionDate": "2026-07-12",
    "buyer": {
      "identificationType": "07",
      "identification": "9999999999999",
      "legalName": "CONSUMIDOR FINAL"
    },
    "lines": [
      {
        "productCode": "SERVICE01",
        "description": "Billing service",
        "quantity": "2",
        "unitPrice": "10.000000",
        "discount": "5.00",
        "taxRuleId": "44444444-4444-4444-8444-444444444444"
      }
    ],
    "payments": [
      {
        "paymentMethodId": "55555555-5555-4555-8555-555555555555",
        "amount": "17.25"
      }
    ],
    "additionalInformation": []
  }'
```

Expected new result:

- HTTP `201`;
- `X-Correlation-Id: billing-e2e-001` returned;
- `Idempotency-Replayed: false`;
- response `companyId` is `a0b1c2d3-e4f5-4678-9abc-def012345678`;
- status `DRAFT` and currency `USD`;
- gross `20.00`, net `15.00`, IVA `2.25`, grand total `17.25`;
- no Issuer/establishment/emission fiscal snapshot;
- no sequence, access key, XML, signature, certificate, SRI, PDF, queue, or notification effect.

Omit `X-Correlation-Id` and repeat with a new idempotency key. Expected: a generated correlation
identifier is returned in the response header and body problem contract when applicable.

## 5. Verify Company Header Validation

Execute each case and inspect both API results and database row counts:

| Case | Expected code | Expected state |
|------|---------------|----------------|
| Missing header | `400 COMPANY_CONTEXT_REQUIRED` | No draft/children/binding |
| Whitespace-only header | `400 COMPANY_CONTEXT_REQUIRED` | No state |
| Malformed UUID | `400 COMPANY_CONTEXT_INVALID` | No state |
| Nil UUID | `400 COMPANY_CONTEXT_INVALID` | No state |
| Multiple header values | `400 COMPANY_CONTEXT_INVALID` | No state |
| Mixed-case valid UUID | `201` when local rules pass | Canonical lowercase CompanyId stored/returned |
| Externally unknown valid UUID | `201` when local rules pass | No external lookup; supplied Company partition used |

No test expects Company existence, active state, fiscal eligibility, caller entitlement, tenant
ownership, or emission-point ownership validation.

## 6. Verify Strict Request Fields

Add each prohibited property separately to the request:

- `companyId`;
- `issuerId`;
- Issuer RUC/legal/trade/address/fiscal fields;
- establishment or emission-point fiscal snapshot fields;
- line gross/net/tax base/tax amount/line total;
- grouped taxes, subtotal, discount total, or grand total;
- direct tax code/rate instead of `taxRuleId`.

Expected:

- unknown Company/Issuer/snapshot fields → `INVALID_REQUEST`;
- recognized calculated fields → `PROHIBITED_CALCULATED_FIELD`;
- no state is persisted.

A Company path or query value cannot substitute for the required header. The OpenAPI document must
contain no Company path/query/body parameter.

## 7. Verify Idempotency

1. Repeat the original command with the same Company/key and equivalent content. Expect `200`,
   `Idempotency-Replayed: true`, and the original draft.
2. Change business content with the same Company/key. Expect `409 IDEMPOTENCY_CONFLICT` and no
   mutation.
3. Use the same key and content with a different valid Company UUID. Expect an independent `201`
   draft and binding.
4. Reorder JSON properties, payments, or additional information only. Expect equivalent replay.
5. Reorder invoice lines. Expect conflict because line order is business-significant.
6. Submit at least 50 concurrent equivalent commands in one Company/key scope. Expect exactly one
   committed draft and binding; all successful outcomes identify that draft.
7. Simulate response loss after commit and retry the equivalent command. Expect the original draft.

Database inspection must show only key/fingerprint hashes and normalization version in the binding;
no raw key or normalized buyer request is stored.

## 8. Verify Rollback, Availability, and Timeout

Inject a failure after each planned persistence phase. Expected: zero root, child, and binding rows
from the failed transaction.

| Failure | Expected outcome |
|---------|------------------|
| PostgreSQL unavailable before commit | `503 PERSISTENCE_UNAVAILABLE` |
| Overall 10-second deadline exceeded | `504 REQUEST_TIMEOUT` |
| Unexpected safe failure | `500 INTERNAL_ERROR` |

Every response returns `X-Correlation-Id` and safe Problem Details. Retry unavailable/timeout cases
with the same Company/key/content.

## 9. Verify Fiscal and Boundary Vectors

Validate at least:

- the `2 × 10.00 − 5.00` at 15% calculation vector;
- all four supported IVA treatments and separate zero-tax grouping;
- unsupported ICE/IRBPNR/multiple-tax rejection;
- all supported buyer identification types and final-consumer threshold;
- zero-value draft with exactly one `0.00` payment;
- payment mismatch and duplicate payment method;
- current Ecuador date and past/future/impossible rejection;
- text and collection maximums plus maximum-plus-one rejection;
- body exactly 2,097,152 bytes proceeds to later validation;
- body larger than 2,097,152 bytes returns `413 REQUEST_PAYLOAD_TOO_LARGE` before Company checks.

## 10. Verify the Negative Architecture Boundary

Static and runtime evidence must show:

- no `CompanyContextPort`, Company client/adapter/stub/table/cache/readiness/timeout/retry;
- no authentication/security dependency, scheme, requirement, Authorization header, `401`, or
  `403`;
- no HTTP request/header/security/thread-local/Gateway object in application or domain;
- no Company/SRI/security outbound span or invocation;
- no Company, tenant, subject, role, authorization, or fiscal-snapshot persistence;
- no application cache.

## 11. Record Performance Evidence

Run the profiles in `operational-requirements.md` and record environment, warm-up, sample count,
percentiles, resource usage, and pool state. Company Service latency must not appear in the workload
or budget.

## 12. JVM and Optional Native Evidence

The packaged JVM smoke suite is mandatory and covers migration, startup, OpenAPI, health, create,
normalization, replay, conflict, rollback, unavailable/timeout, and correlation.

Native support is optional. If claimed, record both build and runtime evidence for the same critical
paths. Otherwise document native as deferred or unsupported with evidence while retaining the JVM
deployment baseline.
