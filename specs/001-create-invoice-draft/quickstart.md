# Quickstart and Validation Guide: Create Invoice Draft

This guide describes runnable evidence expected after implementation. It does not create source
code, migrations, external infrastructure, or production credentials.

## Prerequisites

- JDK 25 selected by `JAVA_HOME`.
- A Docker- or Podman-compatible container runtime for Quarkus Dev Services.
- The repository Gradle 9.5.1 wrapper.
- `curl` and `jq` for the manual HTTP examples.
- Network access to development/test Keycloak and company-context endpoints, or their approved
  contract-test fixtures.
- No real buyer identification, token, idempotency key, or production fiscal payload in fixtures.

The implementation must align `gradle.properties` to Quarkus `3.33.2.1` LTS before these commands
are considered evidence.

## Artifact References

- Requirements: [`spec.md`](spec.md)
- Technical decisions: [`plan.md`](plan.md) and [`research.md`](research.md)
- Persistence model: [`data-model.md`](data-model.md)
- Public contract: [`contracts/invoice-draft-api.openapi.yaml`](contracts/invoice-draft-api.openapi.yaml)
- Company dependency: [`contracts/company-context-port.md`](contracts/company-context-port.md)

## 1. Run the Complete Automated Suite

```bash
./gradlew clean test
```

Expected evidence:

- Pure domain monetary, identification, tax, payment, and zero-total tests pass.
- PostgreSQL 18 Dev Services starts and Flyway migrates an empty database.
- Hibernate Reactive mapping validation passes without schema generation.
- Keycloak Dev Services token-validation and role tests pass.
- API contract, tenant isolation, rollback, timeout, idempotency concurrency, health, and sensitive
  observability tests pass.

Focused evidence may be run with:

```bash
./gradlew test --tests '*InvoiceCalculatorTest'
./gradlew test --tests '*FlywayMigrationTest'
./gradlew test --tests '*InvoiceDraftIdempotencyConcurrencyTest'
./gradlew test --tests '*InvoiceDraftOidcTest'
./gradlew test --tests '*InvoiceDraftContractTest'
```

## 2. Start the JVM Development Runtime

Configure development values without committing secrets:

```bash
export COMPANY_CONTEXT_URL='http://localhost:9090'
export QUARKUS_OIDC_AUTH_SERVER_URL='http://localhost:8180/realms/tax-document-dev'
export QUARKUS_OIDC_CLIENT_ID='ecuador-tax-document-service'
./gradlew quarkusDev
```

PostgreSQL and Keycloak MAY be supplied by Dev Services. The company-context test endpoint must
implement the outbound contract and contain only synthetic Company/Issuer/emission-point data.

Verify management behavior:

```bash
curl --fail http://localhost:8080/q/health/live
curl --fail http://localhost:8080/q/health/ready
curl --fail 'http://localhost:8080/q/openapi?format=yaml'
```

Expected:

- Liveness is `UP` while the process can execute internal health logic.
- Readiness is `UP` only while PostgreSQL, OIDC, and the configured company destination are usable.
- The generated OpenAPI is compatible with the checked-in contract.

## 3. Obtain a Synthetic Billing-Operator Token

Use a test-only Keycloak user/client and never paste the token into logs or committed files:

```bash
export ACCESS_TOKEN="$({
  curl --silent --fail \
    --request POST \
    --data-urlencode 'grant_type=password' \
    --data-urlencode 'client_id=invoice-draft-test-client' \
    --data-urlencode 'username=billing-operator-a' \
    --data-urlencode 'password=test-only-password' \
    'http://localhost:8180/realms/tax-document-dev/protocol/openid-connect/token'
} | jq --raw-output '.access_token')"
```

The test token must have the configured issuer, the invoice service and company-capability
audiences, approved `azp`, subject, and `billing_operator` client role. Prefer the automated
Keycloak Dev Services tests as the durable claim-validation evidence.

## 4. Create a Positive-Total Draft

The development company/catalog fixtures must publish the synthetic UUIDs used below.

```bash
export COMPANY_ID='11111111-1111-4111-8111-111111111111'
export IDEMPOTENCY_KEY='quickstart-positive-001'
export EMISSION_DATE="$(TZ=America/Guayaquil date +%F)"

curl --silent --fail-with-body \
  --request POST \
  --header "Authorization: Bearer ${ACCESS_TOKEN}" \
  --header "Idempotency-Key: ${IDEMPOTENCY_KEY}" \
  --header 'X-Correlation-ID: 44444444-4444-4444-8444-444444444444' \
  --header 'Content-Type: application/json' \
  --data @- \
  "http://localhost:8080/api/v1/companies/${COMPANY_ID}/invoice-drafts" <<JSON
{
  "issuerId": "22222222-2222-4222-8222-222222222222",
  "emissionPointId": "33333333-3333-4333-8333-333333333333",
  "emissionDate": "${EMISSION_DATE}",
  "buyer": {
    "identificationType": "07",
    "identification": "9999999999999",
    "legalName": "CONSUMIDOR FINAL"
  },
  "lines": [
    {
      "productCode": "SERVICE-001",
      "description": "Synthetic service",
      "quantity": "2",
      "unitPrice": "10.00",
      "discount": "5.00",
      "taxRuleId": "55555555-5555-4555-8555-555555555515"
    }
  ],
  "payments": [
    {
      "paymentMethodId": "66666666-6666-4666-8666-666666666666",
      "amount": "17.25"
    }
  ],
  "additionalInformation": [
    {"name": "ORDER_REFERENCE", "value": "SYNTHETIC-001"}
  ]
}
JSON
```

Expected first response:

- HTTP `201 Created`.
- `X-Correlation-ID` and `Idempotency-Replayed: false` headers.
- Status `DRAFT`, currency `USD`, a non-fiscal UUID, and original timestamps.
- Line gross `20.00`, net/tax base `15.00`, tax `2.25`, and line total `17.25`.
- Subtotal `15.00`, discount `5.00`, and grand total `17.25`.
- No sequence, access key, XML, signature, certificate, SRI, PDF, queue, webhook, or notification
  evidence.

## 5. Verify Idempotent Replay and Conflict

Repeat the exact command with the same scoped key.

Expected replay:

- HTTP `200 OK` and `Idempotency-Replayed: true`.
- Original draft identifier, content, and timestamps.
- No additional draft or child rows.

Reorder only payments/additional-information entries in a multi-entry fixture: the result remains a
replay. Change invoice-line order or any business content while keeping the same key: expect HTTP
`409` with `code: IDEMPOTENCY_CONFLICT` and no mutation.

After the first commit, change a mutable Issuer or emission-point attribute in the company fixture
while preserving the caller's current Company access. The equivalent retry must still return the
original persisted snapshot. Revoking current Company access must instead conceal the replay.

Persistence and architecture evidence must also show that the draft stores only the external
Company identifier as its ownership reference; the tenant scope remains outside the draft
aggregate; and no Company master-data table, cross-service foreign key/repository/transaction,
cache, materialized view, or background replication exists.

The committed-concurrency test must send equivalent requests simultaneously and prove one `201`,
replay outcomes resolving to the same draft, and exactly one aggregate in PostgreSQL.

## 6. Verify a Zero-Total Draft

Use one valid line with unit price `0.00`, an explicitly selected applicable tax rule, and exactly
one payment of `0.00`.

Expected:

- HTTP `201` on first commit.
- One line, one selected tax treatment, one `0.00` payment, and grand total `0.00`.
- A zero-priced line is not automatically converted to IVA 0%.

Then send zero total with no payment, two payments, or a non-zero payment. Each request returns
`422`, creates no binding, and leaves no partial rows.

## 7. Verify Validation and Sensitive Errors

Run contract vectors for:

- calculated gross/net/tax/total request fields (`400`);
- caller-supplied tax code/rate (`400`);
- past/future/impossible emission date (`422`);
- invalid buyer identification and final-consumer total above `50.00` (`422`);
- unsupported ICE/IRBPNR/multiple tax treatments (`422`);
- excess precision, mismatched payments, duplicate payment method, collection/text overflow (`422`);
- same idempotency key with different content (`409`).

Every RFC 9457 response must contain a stable English code and correlation UUID but no rejected
buyer value, token, key, SQL detail, stack trace, filesystem path, or raw dependency response.

## 8. Verify Authentication and Ownership

Automated tests must cover wrong signature, expiration, issuer, audience, `azp`, and missing token
(`401`), plus missing `billing_operator` (`403`).

With a valid Tenant A token, request a Company, Issuer, or emission point from Tenant B. Expected:

- Same concealed `404` as an inaccessible identifier.
- No Tenant B field in body, logs, metrics, or traces.
- No idempotency binding or draft.

Revoke Company access after a successful create and replay the command. The service must recheck
current authorization and conceal the original draft.

## 9. Verify Dependency and Health Failures

- Stop PostgreSQL: readiness becomes `DOWN`; liveness remains `UP`; create cannot partially commit.
- Stop the company endpoint: readiness becomes `DOWN`; create returns `503`; no binding exists.
- Delay the company endpoint beyond three seconds: create returns `504`; no automatic retry or
  binding occurs.
- Restore each exact configured destination: readiness returns `UP` and an idempotent caller retry
  can proceed.

## 10. Package and Run JVM Evidence

```bash
./gradlew clean build
./gradlew quarkusIntTest
java -jar build/quarkus-app/quarkus-run.jar
```

The packaged JVM integration suite must prove create, replay, conflict, OIDC, PostgreSQL/Flyway,
company adapter, safe errors, and health behavior. JVM evidence is mandatory for completion.

## 11. Native Candidate Evidence

Native compatibility is not claimed by this plan. To claim it later, run both build and runtime
evidence:

```bash
./gradlew clean build \
  -Dquarkus.native.enabled=true \
  -Dquarkus.native.container-build=true
./gradlew quarkusIntTest -Dquarkus.native.enabled=true
```

The native process must pass create/replay/conflict, real PostgreSQL, OIDC/JWK, company REST,
Flyway-resource, JSON mapping, and health smoke tests. If it does not, record native as deferred or
unsupported with evidence; JVM deployment remains acceptable.
