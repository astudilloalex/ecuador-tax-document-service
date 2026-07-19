# Quickstart: Prepare Invoice for Fiscal Issuance

This guide is the planned verification path for Feature 002. It does not provision production
Company/Issuer master data, administer sequence baselines, generate XML, sign documents, or contact
SRI.

## Prerequisites

- JDK 25
- repository Gradle wrapper
- Podman or Docker capable of running the pinned PostgreSQL 18.4 test service
- one test-only HTTP fixture implementing
  `contracts/authoritative-fiscal-context.openapi.yaml`
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
- `contracts/authoritative-fiscal-context.openapi.yaml` — required read-only provider behavior.

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

