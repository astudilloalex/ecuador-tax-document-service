# Outbound Contract: Company Context Port

**Consumer**: Create Invoice Draft use case

**Authority**: Company capability

**Purpose**: Resolve current caller access and, separately, evaluate the requested
Company/Issuer/emission-point combination before any local draft transaction begins. Keeping those
results separate allows an authorized replay to return its immutable original draft even when
Issuer or emission-point data changed after creation.

The Company bounded context is the sole source of truth for current Company, Issuer,
establishment, and emission-point master data. This contract transfers a request-bounded fiscal
context; it does not transfer master-data ownership to the Tax Document Service.

## Application Port

The application boundary is transport-independent and conceptually exposes:

```text
resolveAuthorizedInvoiceContext(
  authenticatedSubject,
  requestedCompanyId,
  requestedIssuerId,
  requestedEmissionPointId,
  correlationId
) -> Uni<CompanyContextResolution>
```

`CompanyContextResolution` has exactly these terminal outcomes:

- `Authorized(CompanyAuthorizationScope, CreationContextEvaluation)`
- `Inaccessible`
- `Unavailable`
- `TimedOut`
- `InvalidAuthoritativeResponse`

`CreationContextEvaluation` is either:

- `Eligible(CompanyContextSnapshot)` for a logically new creation command; or
- `Ineligible` with no foreign or inactive resource details.

The port MUST NOT expose whether an inaccessible Company, Issuer, or emission point exists. It MUST
NOT persist, cache, or become authoritative for company data. It MUST NOT create an invoice
idempotency binding.

The adapter and provider MUST NOT share a database, schema, repository, persistence entity,
cross-service foreign key, or database transaction. No response may be written to a Company cache,
replica table, materialized view, or background synchronization stream. Only an eligible new
creation copies the approved fields into the document-owned immutable fiscal snapshot.

## Required Authorization Scope

Every `Authorized` response contains:

| Field | Required | Rules |
|-------|----------|-------|
| `tenantId` | Conditional | Present when Company belongs to a broader tenant; absent only when Company is the tenant boundary |
| `companyId` | Yes | Equals requested Company; Company is active and the current subject is authorized |
| `authorizationVersion` | Yes | Stable Company-access decision version/ETag |
| `resolvedAt` | Yes | Unambiguous instant |

These fields are sufficient to scope an idempotency lookup. They remain available when the current
Issuer/emission-point combination is `Ineligible`.

## Eligible Creation Snapshot

An `Eligible` creation evaluation additionally contains:

| Field | Required | Rules |
|-------|----------|-------|
| `contextVersion` | Yes | Stable response version/ETag used in the draft snapshot |
| `issuerId` | Yes | Equals requested Issuer and is the Company's single active Issuer |
| `ruc` | Yes | Registered 13-digit Issuer RUC |
| `legalName` | Yes | Registered authoritative legal name |
| `tradeName` | No | Registered value when applicable |
| `headOfficeAddress` | Yes | Registered fiscal address |
| `accountingRequired` | Yes | Registered fiscal attribute |
| `specialTaxpayerCode` | No | Registered fiscal attribute when applicable |
| `withholdingAgentCode` | No | Registered fiscal attribute when applicable |
| `regimeCode` | No | Registered effective regime when applicable |
| `emissionPointId` | Yes | Equals requested emission point and is active |
| `establishmentCode` | Yes | Exactly three registered digits |
| `emissionPointCode` | Yes | Exactly three registered digits |
| `establishmentAddress` | Yes | Registered address |

An inactive, absent, cross-Company, or non-single Issuer/emission-point combination is
`Ineligible`, not an invalid adapter response. A contradictory or malformed response is
`InvalidAuthoritativeResponse`; the adapter never repairs or infers authoritative values.

The application MUST first use the authorization scope to check an existing local binding. If the
binding exists, it compares normalized command content and returns the original draft or an
idempotency conflict without requiring `Eligible` and without revalidating mutable creation data.
If no binding exists, `Eligible` is mandatory before fiscal/catalog validation and persistence.
The replay response uses the original persisted document snapshot and MUST NOT replace it with
fields from this response.

## Selected REST Adapter Contract

The production infrastructure adapter uses non-blocking Quarkus REST Client against the configured
company-capability base URL:

```http
POST /internal/v1/invoice-draft-context/resolve
Authorization: Bearer <incoming approved token>
X-Correlation-ID: <correlation UUID>
Content-Type: application/json
Accept: application/json
```

Request:

```json
{
  "companyId": "11111111-1111-4111-8111-111111111111",
  "issuerId": "22222222-2222-4222-8222-222222222222",
  "emissionPointId": "33333333-3333-4333-8333-333333333333"
}
```

The authenticated subject is taken from the propagated, already validated bearer token. It is not
accepted as an independently caller-editable JSON field.

Authorized and eligible response (`200`):

```json
{
  "authorization": {
    "tenantId": "00000000-0000-4000-8000-000000000001",
    "companyId": "11111111-1111-4111-8111-111111111111",
    "authorizationVersion": "company-access-v19",
    "resolvedAt": "2026-07-12T15:30:00Z"
  },
  "creationContext": {
    "status": "ELIGIBLE",
    "contextVersion": "company-context-v42",
    "issuer": {
      "issuerId": "22222222-2222-4222-8222-222222222222",
      "ruc": "1790012344001",
      "legalName": "EXAMPLE COMPANY S.A.",
      "tradeName": "EXAMPLE",
      "headOfficeAddress": "Registered fiscal address",
      "accountingRequired": true,
      "specialTaxpayerCode": null,
      "withholdingAgentCode": null,
      "regimeCode": null
    },
    "emissionPoint": {
      "emissionPointId": "33333333-3333-4333-8333-333333333333",
      "establishmentCode": "001",
      "emissionPointCode": "001",
      "establishmentAddress": "Registered establishment address"
    }
  }
}
```

An authorized request whose current Issuer/emission-point combination cannot be used for a new
draft returns the same authorization object and `"creationContext": {"status": "INELIGIBLE"}`.
No existence, ownership, or active-state detail is included.

When Company is itself the tenant boundary, `tenantId` is absent rather than copied from the
untrusted request.

## Failure Mapping

| Provider outcome | Port outcome | Invoice API outcome | Persistence effect |
|------------------|--------------|---------------------|--------------------|
| `200` authorized + eligible | `Authorized(..., Eligible)` | Check binding; validate new command only when unbound | No local transaction yet |
| `200` authorized + ineligible | `Authorized(..., Ineligible)` | Replay/conflict when bound; otherwise concealed `404 RESOURCE_NOT_ACCESSIBLE` | No new binding or draft |
| `401`/`403`/`404` or concealed inaccessible Company result | `Inaccessible` | Concealed `404 RESOURCE_NOT_ACCESSIBLE` | No binding or draft |
| Connect failure, `502`, `503` | `Unavailable` | `503 DEPENDENCY_UNAVAILABLE` | No binding or draft |
| Connect/read/overall timeout | `TimedOut` | `504 DEPENDENCY_TIMEOUT` | No binding or draft |
| `200` malformed/contradictory response or other unexpected status | `InvalidAuthoritativeResponse` | Safe `503 DEPENDENCY_INVALID_RESPONSE` | No binding or draft |

The adapter does not automatically retry. The caller retries the complete command with the same
idempotency key.

## Time and Resource Bounds

- Connect timeout: 1 second.
- Response/read timeout: 3 seconds.
- Overall company validation budget: 5 seconds.
- Maximum successful response: 64 KiB.
- Automatic retries: zero.
- Cancellation propagates to the REST call and cannot create local data.

## Security and Observability

- Use TLS in production and propagate the bearer token only to the allow-listed company base URL.
- Propagate only a token that contains the configured company-capability audience; otherwise fail
  closed before the request.
- The company endpoint independently validates that audience and its authorization requirements;
  network placement alone is not authentication.
- Never log or trace the token, response body, fiscal values, buyer data, or requested identifiers
  as metric labels.
- Propagate `X-Correlation-ID` and W3C trace context separately.
- Record only bounded dependency name, outcome category, status class, and latency.
- Readiness calls a read-only company health operation on the same configured base URL. It does not
  call this authorization operation with fabricated user data.

## Contract Evidence

Required adapter tests cover valid broader-tenant and Company-as-boundary scopes, eligible and
ineligible creation evaluations, authorized replay after mutable Issuer/emission-point changes,
wrong returned IDs, malformed fiscal fields, concealed Company access, every failure mapping,
exact timeout budgets, cancellation, token/correlation propagation, sensitive-data redaction, and
same-destination readiness configuration.
