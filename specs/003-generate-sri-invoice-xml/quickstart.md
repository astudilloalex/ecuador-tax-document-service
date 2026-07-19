# Quickstart Validation Guide: Generate Standard SRI Invoice XML

This guide validates Feature 003 after implementation. It does not authorize production data,
provider deployment, signing, SRI communication, or another tax-document profile.

## 1. Prerequisites

- Linux with Java 25.
- Podman available through `unix:///run/user/1000/podman/podman.sock`.
- Repository dependencies resolvable by Gradle.
- PostgreSQL 18.4 test image available to the existing test resource.
- Synthetic, non-production Invoice Draft and Fiscal Preparation fixtures.
- For an eligible first-generation fixture, complete profile evidence conforming to
  `specs/003-generate-sri-invoice-xml/contracts/authoritative-fiscal-context-v2.openapi.yaml`:
  exact profile/rule/trigger-set identifiers and all fourteen assessments set to
  `DOES_NOT_APPLY`.

The provider contract requires approval by the Fiscal Context Provider Owner and Feature 002
contract owner before first-generation acceptance. Tests may use a contract fixture; Feature 003
itself must never call that provider.

## 2. Verify the pinned schema contract

From the repository root:

```bash
sha256sum \
  specs/003-generate-sri-invoice-xml/contracts/sri/invoice/1.1.0/factura_V1.1.0.xsd \
  specs/003-generate-sri-invoice-xml/contracts/sri/invoice/1.1.0/xmldsig-core-schema.xsd \
  specs/003-generate-sri-invoice-xml/contracts/sri/invoice/1.1.0/XMLSchema.dtd \
  specs/003-generate-sri-invoice-xml/contracts/sri/invoice/1.1.0/datatypes.dtd

wc -c specs/003-generate-sri-invoice-xml/contracts/sri/invoice/1.1.0/{factura_V1.1.0.xsd,xmldsig-core-schema.xsd,XMLSchema.dtd,datatypes.dtd}
```

Expected values:

| Resource | Bytes | SHA-256 |
|----------|------:|--------|
| `factura_V1.1.0.xsd` | 36,356 | `62db9bf0ecceb00ef2b7ed136e59224815e5e5e33c77efc6a0552001e052eb8b` |
| `xmldsig-core-schema.xsd` | 10,293 | `35cf8197da812c85e40d57891b35c94187569ed474a2dac813ce5090dafcd35c` |
| `XMLSchema.dtd` | 16,075 | `2032ead9fd47a61b22fe56aa02be1840bd9bb9015b0c0d3f1e8aac75dd91c3b9` |
| `datatypes.dtd` | 6,357 | `6946432ca7af2e9584f91b48564111fd2c73c8debbbcd9a0e3f5ddd382eeb51c` |

Implementation tests must also verify byte-identical copies under
`src/main/resources/sri/invoice/1.1.0/`, strict catalog resolution, no network/filesystem fallback,
and validator unavailability on any mismatch.

## 3. Run static, contract, and PostgreSQL evidence

Run one Gradle build at a time:

```bash
DOCKER_HOST=unix:///run/user/1000/podman/podman.sock \
TESTCONTAINERS_RYUK_DISABLED=true \
QUARKUS_HTTP_TEST_PORT=0 \
./gradlew spotlessCheck test
```

Focused Feature 003 evidence may be rerun with:

```bash
DOCKER_HOST=unix:///run/user/1000/podman/podman.sock \
TESTCONTAINERS_RYUK_DISABLED=true \
QUARKUS_HTTP_TEST_PORT=0 \
./gradlew test --tests '*invoicexml*'
```

Expected results:

- all prior Feature 001/002 tests remain green;
- Flyway creates V1 through V7 from empty PostgreSQL 18.4 and upgrades V6 to V7 without modifying
  previous checksums or backfilling existing preparations;
- OpenAPI semantics preserve existing operations and add only the approved Feature 003 operation;
- Clean Architecture and exclusion tests permit official XML names only in the SRI adapter and
  schema fixtures;
- direct database mutation, cross-Company linkage, digest mismatch, partial profile evidence, and
  duplicate artifact probes fail at the expected constraint/guard.

## 4. Validate first generation

Use a synthetic prepared Invoice fixture containing:

- exact same Company/draft/preparation relationships;
- complete ordinary-profile evidence with all fourteen `DOES_NOT_APPLY` decisions;
- persisted USD currency, grouped IVA totals, payments, and calculated totals;
- optional designation variants, including separate fixtures for present/absent Special Taxpayer,
  Withholding Agent, RIMPE Contributor, and Large Contributor;
- XML metacharacters and approved Unicode in synthetic business text;
- quantity/unit-price fixtures with two and six fractional digits.

Call the contract operation with exactly one Company header and no body:

```text
POST /api/v1/invoice-drafts/{invoiceDraftId}/unsigned-sri-invoice-xml
X-Company-Id: {synthetic-company-uuid}
X-Correlation-Id: feature003-validation
```

Expected first response:

- HTTP `201 Created`;
- `Content-Type: application/json` and `Cache-Control: no-store`;
- one safe `X-Correlation-Id`;
- artifact, draft, and exact preparation IDs; schema version `1.1.0`; `SHA-256`; lowercase
  64-hex digest; byte length; original creation instant; canonical padded `xmlContentBase64`;
- no Company ID, replay flag, lifecycle status, signature, SRI status, or authorization field.

Decode only the synthetic response in a protected temporary test location and assert:

1. decoded length equals `integrityEvidence.byteLength` and is at most 2,097,152;
2. independent SHA-256 equals `integrityEvidence.digest`;
3. bytes begin exactly with `<?xml version="1.0" encoding="UTF-8"?><factura
   id="comprobante" version="1.1.0">`;
4. `</factura>` is the final byte sequence with no trailing newline;
5. the exact official local XSD graph validates the decoded bytes;
6. parsed business/fiscal values equal persisted sources after only the approved adapter mappings;
7. no signature namespace/element or unsupported root section exists.

Do not print the decoded XML, digest, Access Key, RUC, buyer data, names, or addresses to CI logs.

## 5. Validate exact replay

Repeat the same Company-plus-draft request after the artifact commits. Make the provider/current
catalog test doubles unavailable and configure the clock spy to fail if current date or a new
persistence instant is requested.

Expected replay:

- HTTP `200 OK`;
- every body value, decoded byte, digest, byte length, source ID, and `createdAt` matches the first
  response;
- zero generator, XSD validator, Fiscal Context provider, current-catalog, current-date,
  persistence-clock, and write invocations;
- no new artifact row or other state.

A process restart followed by the same replay must produce the same result from PostgreSQL.

## 6. Validate 100-request convergence

Run the packaged performance scenario for 100 simultaneous equivalent first requests:

```bash
DOCKER_HOST=unix:///run/user/1000/podman/podman.sock \
TESTCONTAINERS_RYUK_DISABLED=true \
QUARKUS_HTTP_TEST_PORT=0 \
./gradlew quarkusIntTest --tests '*InvoiceXmlJvmPerformanceIT'
```

Required observation in a warmed JVM:

- all conclusive successes complete within the single ten-second request budget;
- exactly one response is `201`; every other successful response is `200`;
- PostgreSQL contains exactly one artifact for the Company/draft and one for the preparation;
- all responses identify the same artifact/content/digest/length/creation instant;
- worker concurrency never exceeds the configured maximum, queued work never exceeds 100, the
  reactive pool recovers, and no blocked-event-loop signal appears.

Also test a uniqueness race from independent Vert.x contexts so correctness is not attributed only
to one process-local executor.

## 7. Validate fail-closed outcomes

| Scenario | Expected code | Required state/evidence |
|----------|---------------|-------------------------|
| Missing/repeated/blank/malformed/nil Company header | `COMPANY_CONTEXT_REQUIRED` or `COMPANY_CONTEXT_INVALID` | Zero Company-owned access |
| Malformed/nil draft, body, query, `Idempotency-Key`, or prohibited input | `INVALID_REQUEST` | Zero source/XML/artifact work |
| Missing or cross-Company draft | `INVOICE_DRAFT_NOT_FOUND` | Indistinguishable and non-disclosing |
| No preparation | `FISCAL_PREPARATION_REQUIRED` | Zero XML work/artifact |
| Duplicate/partial/cross-linked relationship | `INVOICE_XML_SOURCE_INCONSISTENT` | Zero XML work/artifact |
| Legacy/generic/partial/wrong/indeterminate profile evidence | `INVOICE_XML_PROFILE_UNDETERMINED` | Zero XML work/artifact |
| Each trigger `APPLIES` or RIMPE Popular Business | `INVOICE_XML_PROFILE_UNSUPPORTED` | Zero XML work/artifact |
| Missing/unrepresentable mandatory source | `INVOICE_XML_SOURCE_INVALID` | No lossy conversion/artifact |
| Generated one-rule XSD violation | `INVOICE_XML_SCHEMA_INVALID` | No artifact/digest/link/timestamp and no XML disclosure |
| Missing/mutated schema dependency/catalog | `INVOICE_XML_VALIDATOR_UNAVAILABLE` | Readiness down; validation never bypassed |
| Confirmed database rollback | `PERSISTENCE_FAILURE` | Zero artifact; safe natural retry |
| Lost commit acknowledgement with inconclusive reconciliation | `INVOICE_XML_OUTCOME_UNKNOWN` | No zero-state claim; same natural request required |
| Deadline before conclusive selection | `REQUEST_TIMEOUT` | One terminal response; possible commit is not denied |

Every error uses `application/problem+json`, a stable code, safe English detail, safe correlation,
and `Cache-Control: no-store`. Assert that all synthetic sensitive sentinel values are absent from
the body, logs, traces, metrics, and health output.

## 8. Run mandatory packaged JVM evidence

```bash
DOCKER_HOST=unix:///run/user/1000/podman/podman.sock \
TESTCONTAINERS_RYUK_DISABLED=true \
QUARKUS_HTTP_TEST_PORT=0 \
./gradlew quarkusBuild

DOCKER_HOST=unix:///run/user/1000/podman/podman.sock \
TESTCONTAINERS_RYUK_DISABLED=true \
QUARKUS_HTTP_TEST_PORT=0 \
./gradlew quarkusIntTest --tests '*InvoiceXmlJvmSmokeIT'
```

The smoke suite must prove exact packaged-resource hashes, strict offline schema compilation,
valid/invalid XSD behavior, CPU work off the event loop, exact PostgreSQL byte/digest round trip,
first/replay HTTP semantics, restart replay, safe failures, and zero excluded side effects.

Native support remains unclaimed. A future native claim requires a real Mandrel/GraalVM 25 build
with the complete schema resource graph and the same smoke/performance/security results; a JVM
result is not native evidence.

## 9. Release gates

Before production release, record:

- approval and deployed support for authoritative fiscal-context consumer contract `2.0.0` by the
  Fiscal Context Provider Owner and Feature 002 contract owner;
- Platform Operations approval for TLS, PostgreSQL encryption at rest, encrypted backups,
  successful restore, Invoice-record retention, and linked XML-artifact disposal;
- exact runtime schema hashes matching this planning contract;
- passed empty/V6-upgrade PostgreSQL 18.4 migrations and mandatory packaged JVM suites;
- no unresolved constitutional deviation or Pending Functional Validation.

The release evidence must not include real XML or fiscal/personal payload values.
