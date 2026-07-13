# Quickstart: Create Invoice Draft

**Feature**: `001-create-invoice-draft`
**Constitution**: v2.0.0

This is verification guidance for the later implementation phase. It does not authorize source
code creation during the constitution amendment.

## Prerequisites

- Java 25;
- the repository Gradle wrapper;
- PostgreSQL 18-compatible test/runtime access;
- no Keycloak, OIDC provider, token, Company Service, gateway, or BFF endpoint.

## Build and Test

```bash
./gradlew clean test
./gradlew quarkusBuild
```

The required suite must prove domain calculations, local catalog rules, Company-header validation,
Company ownership scoping, idempotency concurrency, atomic rollback, empty-database Flyway
migration, API contract, sensitive-data safety, health behavior, and packaged JVM execution.

## Runtime Configuration

Configure only the application’s PostgreSQL datasource and normal observability settings. There
must be no configuration for OIDC, Keycloak, JWT, Company clients, Company endpoints, token
propagation, Company timeouts, Company health, or Company caches.

Start the JVM application with the supported project command after PostgreSQL is available:

```bash
./gradlew quarkusDev
```

Readiness may depend on the configured PostgreSQL destination. It must not report or invoke a
Company or identity-provider destination.

## Create a Draft

The Company context is supplied only by the mandatory header. It is not supplied in path, query,
body, token, or session.

```bash
curl --request POST 'http://localhost:8080/api/v1/invoice-drafts' \
  --header 'Content-Type: application/json' \
  --header 'X-Company-Id: 11111111-1111-4111-8111-111111111111' \
  --header 'Idempotency-Key: invoice-draft-example-001' \
  --header 'X-Correlation-ID: 22222222-2222-4222-8222-222222222222' \
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

Expected new-command result:

- `201`;
- normalized `companyId` and opaque `emissionPointId` in the response;
- status `DRAFT`;
- gross `20.00`, discount `5.00`, net `15.00`, IVA `2.25`, grand total `17.25`;
- no Issuer, establishment, emission-point fiscal, or Company snapshot;
- no official sequence, access key, XML, signature, certificate, SRI call, or notification.

The service does not check whether the Company exists, is active, is fiscally eligible, owns the
emission point, or may be used by the caller.

## Verify Company Header Failures

Exercise all of these cases:

- missing or blank `X-Company-Id` → `400` with `COMPANY_CONTEXT_REQUIRED`;
- repeated, malformed, or nil UUID header → `400` with `COMPANY_CONTEXT_INVALID`;
- a Company UUID only in path/query/body → still `COMPANY_CONTEXT_REQUIRED`;
- syntactically valid non-nil UUID for a nonexistent or unauthorized Company → processed normally
  as opaque business context, subject only to local draft validation.

Every rejected header case leaves no draft, child row, or idempotency binding.

## Verify Idempotency

1. Repeat the same request with the same Company header and key. Expect `200`, the original draft,
   and no new rows.
2. Change business content while retaining the Company and key. Expect `409` with the stable
   idempotency-conflict code and no mutation.
3. Use the same key with a different valid Company UUID. Expect an independent command scope.
4. Submit at least 50 concurrent equivalent commands for one Company/key. Expect exactly one draft
   and one binding; all successful callers resolve to the same draft.
5. Simulate response loss after commit and retry. Expect the original draft.

Correlation values and representation-only property order do not affect equivalence. Company UUID
is part of the scope and not duplicated inside the business-content fingerprint.

## Verify Zero-Value and Rejection Paths

- A legitimate zero-total draft with at least one line and exactly one `0.00` payment is accepted.
- Zero-valued lines still require an explicitly selected effective IVA treatment.
- Discounts above gross, payment mismatch, unsupported tax, invalid buyer identity, invalid date,
  collection overflow, calculated input fields, or persistence failure produce no partial data.

## Verify the Negative Architecture Boundary

Evidence must show:

- no security scheme, security requirement, Authorization header, `401`, or `403` in OpenAPI;
- no Keycloak/OIDC/OAuth/JWT/API-key dependency or configuration;
- no Company port/client/repository/entity/table/cache/replica/adapter;
- no direct or shared Company database access, foreign key, or transaction;
- no Company failure, timeout, availability, or readiness behavior;
- no Company-context version/observation time or fiscal master-data snapshot in the draft.

## JVM and Optional Native Evidence

The packaged JVM runtime must pass create, replay, conflict, rollback, PostgreSQL/Flyway, contract,
and health smoke scenarios.

Native execution is optional. If claimed, record the native build and runtime results for the same
critical scenarios. If native support is unsupported or deferred, record the evidence and retain
the accepted JVM deployment.
