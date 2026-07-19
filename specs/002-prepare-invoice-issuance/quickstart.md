# Quickstart: Prepare Invoice for Fiscal Issuance

This guide is the planned verification path for Feature 002. It does not provision production
Company/Issuer master data, execute the production Official Sequence Baseline runbook, generate
XML, sign documents, or contact SRI.

## Prerequisites

- JDK 25
- repository Gradle wrapper
- Podman or Docker capable of running the pinned PostgreSQL 18.4 test service
- one test-only HTTP fixture implementing approved logical capability
  `authoritative-fiscal-context` consumer contract
  `contracts/authoritative-fiscal-context.openapi.yaml` version `1.0.0`
- Feature 001 migrations V1–V5 and the new Feature 002 migration applied by Flyway

The integration fixture may insert controlled Official Sequence Baseline rows directly for test
setup. That fixture is not a production administration API, migration seed, runtime initializer, or
evidence that a missing production baseline may be created automatically.

## Configuration Boundaries

Implementation introduces configuration in the Feature 002 namespace for:

- the authoritative fiscal-context base URL;
- one-second connection and two-second response limits, each clamped to the remaining request
  deadline;
- local persistence/lock limits derived from the remaining ten-second request budget;
- no automatic external retry;
- no authentication, token, Company lookup, SRI endpoint, certificate, XML, queue, or notification
  configuration.

Test configuration points the fiscal-context client at a lightweight local Vert.x fixture and the
database at the exact PostgreSQL 18.4 test service. Readiness and the business adapter must use the
same configured PostgreSQL destination. No readiness call is made to a Company-specific fiscal
resolution route.

The outbound contract's `.invalid` server URL is a deliberately non-routable planning placeholder.
It is never a runtime endpoint: the provider base URL comes only from environment configuration and
must not be hardcoded.

## Approved Deployment Prerequisites

These responsibilities are approved; this guide does not claim their production evidence already
exists.

### Fiscal-context provider

- Accountable role: `Fiscal Context Provider Owner`.
- Approved capability and consumer contract: `authoritative-fiscal-context`,
  `contracts/authoritative-fiscal-context.openapi.yaml` version `1.0.0`.
- Implementation and automated acceptance may use the approved contract fixture.
- Production deployment is blocked until a concrete provider destination and accountable
  operational owner are registered in deployment configuration.
- Provider implementation, master-data administration, deployment, and any local Company, Issuer,
  Establishment, or Emission Point master-data copy remain outside Feature 002.

### Official Sequence Baseline provisioning

- Accountable role: `Database Operations Owner`.
- Production baselines are created outside Feature 002 by a controlled, reviewed, auditable
  SQL/runbook procedure that validates Company ownership, the exact Issuer/Establishment/Emission
  Point/document-type scope and official codes, and initial `lastAllocated`.
- Evidence must identify requester, approver, execution time, scope, and resulting baseline
  identifier without exposing sensitive values in general telemetry.
- A fiscal scope is not production-ready for its first preparation until that evidence is approved.
- Feature 002 exposes no administration and performs no missing-row creation, Flyway seed,
  upsert-on-missing, reset, decrement, repair, wrap, or reuse. Test fixtures remain the only
  permitted setup path in this guide.

### Sensitive-data platform controls

- Accountable role: `Platform Operations Owner`.
- Required target-environment release evidence comprises TLS-enabled service and PostgreSQL
  connections; approved PostgreSQL encryption at rest; encrypted backup policy/handling and a
  successful restore; an approved Invoice-record retention policy applicable to Fiscal Preparation;
  and confirmation that Fiscal Preparation is retained and disposed of with its related Invoice.
- Feature 002 adds no custom application database encryption, key management, deletion API, or
  separate retention scheduler. Absence of a deletion API does not override platform retention.
- Raw provider requests, responses, credentials, and internal errors are not persisted and therefore
  are absent from backups.

## Quality and Null-Safety Gate

Run the complete formatting, compilation, nullness, warning-as-error, unit, API, migration, and
PostgreSQL suite:

```bash
./gradlew spotlessCheck test
```

The build must report:

- zero `javac -Xlint:all -Werror` warnings;
- zero NullAway/JSpecify errors or generic-inference warnings for all new/modified Feature 002 and
  extracted shared packages;
- zero new `@SuppressWarnings`, broad null-analysis exclusions, or `@NullUnmarked` escapes used in
  place of fixes;
- successful empty-database migration and V5-to-Feature-002 migration on PostgreSQL 18.4;
- no Feature 001 regression.

When the local container engine exposes the Podman socket explicitly, use the repository's approved
environment form:

```bash
DOCKER_HOST=unix:///run/user/1000/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true ./gradlew spotlessCheck test
```

Do not run multiple Gradle builds concurrently against the same workspace/cache.

## Focused Evidence

Run the Feature 002 test families:

```bash
./gradlew test --tests '*fiscalpreparation*'
```

The focused suite must prove:

1. exact SRI v2.33 48+1 composition and independent Modulo 11 calculation;
2. the official positive vectors, synthetic raw `11→0` and `10→1` vectors, every component
   mutation, and the invalid SRI page-64 printed sample;
3. exact nine-digit sequence and eight-digit Numeric Code representations, including leading
   zeros and valid `00000000`;
4. exact fixed Ecuador request-entry date behavior across midnight;
5. all fiscal-context validation failures occur before baseline access;
6. missing, invalid, and exhausted baselines fail closed without creation/reset/wrap;
7. 100 equivalent requests commit one preparation and return the same winner;
8. 100 different drafts in one scope commit the exact next 100 values without duplicates or
   locally caused gaps;
9. every forced rollback leaves the baseline and preparation state unchanged;
10. response loss and commit uncertainty reconcile only by Company plus Invoice Draft;
11. cross-Company lookups return the safe not-found result;
12. replays make no provider, baseline, generator, clock, XML, certificate, SRI, queue, or
    notification call.

## Contract Verification

Feature 002 owns these planning contracts:

- `contracts/fiscal-preparation-api.openapi.yaml` — new inbound operation;
- `contracts/authoritative-fiscal-context.openapi.yaml` version `1.0.0` — approved contract-first,
  read-only provider behavior for logical capability `authoritative-fiscal-context`, accountable to
  `Fiscal Context Provider Owner`.

Implementation merges the new operation into
`src/main/resources/META-INF/openapi.yaml`, which remains the runtime API source of truth. The
completed Feature 001 standalone contract stays unchanged and is protected by semantic regression
tests rather than whole-file equality after the second path is added.

Contract tests must verify:

- no `security`, security schemes, `Authorization`, `401`, or `403` contract;
- exactly one required `X-Company-Id`, no Company body/query/path/response property, and no
  `Idempotency-Key`;
- no request body and runtime rejection of non-empty content;
- `201` new and `200` replay with the same response schema;
- complete stable error catalog and status mapping;
- outbound capability identifier, contract version, accountable role, configurable base URL, and
  non-routable `.invalid` placeholder semantics;
- `accountingRequired` and `rimpeClassification` accept no resolution identifier; applicable
  Special Taxpayer and Withholding Agent values require their authoritative resolution; applicable
  Large Contributor requires its complete resolution/legend pair and rejects partial evidence;
- no XML, certificate, SRI, PDF/RIDE, event, queue, notification, or administration route.

## JVM Runtime Evidence

Build the mandatory JVM artifact:

```bash
./gradlew quarkusBuild
```

Then run the planned packaged JVM smoke and concurrency integration tests against PostgreSQL 18.4
and the fiscal provider fixture:

```bash
./gradlew quarkusIntTest --tests '*FiscalPreparationJvmSmokeIT'
./gradlew quarkusIntTest --tests '*FiscalPreparationJvmPerformanceIT'
```

The runtime evidence must include:

- first preparation with a pre-provisioned baseline;
- identical replay after provider outage and after a simulated response loss;
- a provider timeout producing `FISCAL_CONTEXT_UNAVAILABLE` before sequence access;
- a PostgreSQL rollback fault producing no local consumption;
- a commit-knowledge fault producing either a reconciled replay or
  `PREPARATION_OUTCOME_UNKNOWN`, never a second preparation;
- event-loop/blocking inspection and recovery of the reactive pool after concurrent load;
- sanitized logs, metrics, and traces with no fiscal payloads or high-cardinality sensitive labels.

Packaged JVM evidence against fixtures does not establish production provider registration,
production baseline provisioning, or target-environment platform controls; the deployment evidence
above remains a separate release gate.

## Native Status

Native compatibility is deferred and unclaimed. No native command is an acceptance prerequisite
unless the implementation elects to claim native support and supplies both build and runtime
evidence for REST Client JSON mapping, Hibernate Reactive persistence, configuration, reflection,
and resource loading. JVM support remains mandatory regardless.

## Independent Acceptance Setup

For the smallest end-to-end scenario:

1. create one Feature 001 Invoice Draft under Company A with emission date equal to the Ecuador
   civil date fixed at request entry;
2. configure the fiscal fixture to return one complete, supported, effective context for that exact
   Company, Emission Point, date, and document type `01`;
3. test-provision one valid baseline whose `lastAllocated` is `122` for the exact fiscal scope;
4. call `POST /api/v1/invoice-drafts/{invoiceDraftId}/fiscal-preparation` with exactly one valid
   `X-Company-Id` and no body;
5. verify `201`, sequential `000000123`, one eight-digit Numeric Code, one valid 49-digit Access
   Key, complete immutable source evidence, and an unchanged Invoice Draft;
6. call the same path again with a different correlation identifier and the fiscal fixture offline;
7. verify `200`, byte/value-equivalent persisted fiscal data, replay header `true`, and no baseline
   or provider activity.
