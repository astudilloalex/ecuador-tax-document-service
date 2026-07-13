# Operational and Performance Requirements: Create Invoice Draft

## Measurement Boundary

Request latency includes this service's header/body parsing, normalization, fingerprinting, local
catalog/domain validation, calculation, reactive PostgreSQL work, and response serialization.

It excludes gateway/BFF work, authentication, authorization, Company validation, and Company
Service latency because those operations do not exist inside this service. JVM startup, Flyway
migration, and test-fixture setup are not included in request latency.

Before performance evidence is accepted, record:

- CPU and memory limits;
- Java/Quarkus/PostgreSQL versions;
- JVM arguments;
- PostgreSQL placement and measured baseline latency;
- reactive pool size and timeout configuration;
- dataset/catalog size;
- warm-up duration, measurement duration, sample count, and percentile method.

Server-side request duration MUST use a monotonic timer captured at the earliest service request
boundary and stopped when the terminal response is ready for delivery. It MUST NOT use wall-clock
timestamps, `requestCreationInstant`, `createdAt`, or correlation timestamps. Client network,
gateway, and upstream queueing time are outside this server-side measurement.

`requestCreationInstant` is separate functional evidence. The request boundary captures it exactly
once from the approved wall clock and derives the expected emission date once using
`America/Guayaquil`. It MUST be passed through validation and persistence without recapture, MUST
remain fixed if commit crosses midnight, and MUST NOT be replaced by `createdAt`. Equivalent replay
may capture an operational request instant but MUST NOT use it to revalidate or mutate the original
emission date.

Correlation initialization/validation time is included in request duration. Correlation identity
never starts, stops, partitions, or otherwise changes a performance measurement.

## Request Performance Budgets

| Profile | Load | Required outcome |
|---------|------|------------------|
| Typical new draft | At most 10 lines, 2 payments, 5 additional entries; warmed JVM | p95 ≤ 750 ms; p99 ≤ 1.5 s |
| Maximum valid draft | 500 lines, 10 payments, 15 additional entries, body ≤ 2 MiB | p95 ≤ 3 s; p99 ≤ 5 s |
| Equivalent replay | Existing binding and complete aggregate | p95 ≤ 250 ms; p99 ≤ 500 ms |
| Idempotency conflict | Existing binding, different fingerprint | p95 ≤ 250 ms; p99 ≤ 500 ms |
| Same-scope concurrency | 50 concurrent equivalent commands | All terminate within 10 s; exactly one draft and binding; no pool starvation after completion |

The overall request deadline is 10 seconds and the local database write-transaction timeout is 5
seconds. A request larger than 2 MiB is rejected before Company-header evaluation.

The overall deadline begins at the same service boundary as the monotonic request timer. Therefore
body reading, Company-header validation, safe correlation initialization/validation, idempotency
processing, domain work, persistence, and serialization all consume the same 10-second budget.
Failure precedence determines the observable result; it does not reset the timer.

If evidence misses a budget, the plan must be revisited before implementation is considered
complete. The response is not to add a Company cache, application cache, broker, or unapproved
distributed component.

Performance fixtures MUST use the exact published tax-rule and payment-method UUIDs from
`reference-data-baseline.md`. Benchmarks MUST NOT substitute invented identifiers, runtime-generated
rows, or excluded SRI representations. Execution evidence begins only after the later Flyway seed
implements that approved baseline unchanged.

## Capacity and Resource Safety

- Accept at most 500 invoice lines, 10 payments, and 15 additional-information entries.
- Bound request body at 2,097,152 bytes.
- Enforce quantity `numeric(12,6)`, monetary `numeric(17,2)`, and percentage-rate `numeric(5,2)`
  envelopes in representation, calculation, persistence, and response paths. Individually valid
  inputs whose exact intermediate, grouped, payment-sum, or total result overflows the monetary
  maximum MUST fail before persistence with `MONETARY_RANGE_EXCEEDED`.
- Bound reactive connection-pool acquisition and query duration beneath the request deadline.
- Do not synchronously wait for `Uni`, `CompletionStage`, or future results.
- Domain work remains synchronous and bounded; a maximum-payload benchmark must show no event-loop
  blocked-thread warning. If it does not, planning must explicitly isolate the CPU work on a
  bounded executor before implementation continues.
- No unbounded retry loop exists. The service performs no automatic Company or SRI retry.

## Availability and Health

**Liveness** reports only process/internal health. It remains `UP` when PostgreSQL is unavailable.

**Readiness** is `UP` only when:

- the same PostgreSQL destination used for business persistence is reachable through a bounded,
  read-only check;
- required Flyway migrations and mandatory approved local reference-catalog initialization
  completed, including exact agreement between published and seeded stable UUIDs.

Readiness performs no destructive action and no Company, gateway/BFF, identity-provider, SRI,
certificate, storage, queue, or notification check.

## Correlation and Structured Logs

Correlation is initialized at the HTTP boundary for every request:

- absent input generates one safe UUID;
- one valid, trimmed 1–64 character value using the approved safe ASCII grammar is preserved;
- blank, repeated, over-length, or unsafe input is never echoed, produces a safe replacement UUID,
  and returns `INVALID_REQUEST` when correlation validation governs;
- payload-size or Company-context failures retain their higher precedence, but still use a safe
  generated identifier when supplied correlation cannot safely be preserved;
- the safe correlation value is returned in `X-Correlation-Id` and propagated through logs/traces;
- correlation is transport evidence and is excluded from the request fingerprint and Company/key
  idempotency scope.

Allowed structured fields include:

- correlation identifier;
- operation name;
- outcome and stable error code;
- HTTP status;
- new/replay/conflict indicator;
- duration;
- whether correlation was generated or accepted, using a bounded boolean/category rather than the
  value as a metric dimension;
- bounded line/payment/additional-information counts;
- database outcome category.

Buyer identification/contact, full request/response payloads, raw idempotency keys, normalized
fingerprint inputs, SQL, stack traces, internal paths, tokens, and certificates are prohibited.
CompanyId and draftId may appear only in approved structured audit/correlation fields and never as
metric labels.

## Metrics

Required bounded metrics:

- request count and duration by operation/outcome/status;
- payload-size rejection count;
- Company-header required/invalid counts;
- correlation generated/accepted/invalid counts using bounded outcome labels;
- business-validation failure count by bounded violation code;
- monetary-range-exceeded count;
- persistence duration, unavailable, timeout, and rollback counts;
- idempotency new/replay/conflict counts;
- liveness/readiness state.

Labels must be low-cardinality. CompanyId, draftId, correlationId, idempotency keys/fingerprints,
buyer fields, product codes, and error details are prohibited labels.

## Tracing

The expected trace path is API → application use case → reactive PostgreSQL persistence. No
Company Service, identity, SRI, XML, certificate, queue, or notification span should exist.

Trace attributes follow the same sensitive/high-cardinality restrictions as metrics and logs.
The request span uses monotonic duration; `requestCreationInstant` MAY appear only as a safe event
attribute when needed to prove midnight behavior and MUST NOT replace the trace clock. Invalid
correlation input MUST never appear in a span.

## Audit Events

Plan safe events for:

- new draft committed;
- equivalent replay returned;
- idempotency conflict;
- persistence unavailable/timeout;
- operationally significant rollback.

The durable audit destination and retention are platform operational decisions outside this
feature unless a later approved requirement makes them document data. Audit failure must not be
represented as fiscal issuance failure because no issuance occurs.

## JVM and Native Evidence

Mandatory JVM evidence:

- empty-database migration;
- packaged JVM boot;
- OpenAPI and health;
- create, normalization, replay, conflict, rollback, unavailable, timeout, and correlation paths;
- single-capture `requestCreationInstant`, Guayaquil midnight, later-date replay, and representative
  fiscal/monetary maximum/overflow vectors;
- absent/valid/invalid/combined-precedence correlation vectors;
- sensitive-data and high-cardinality-label checks;
- the performance profiles above.

Native execution is optional. If claimed, both build and runtime smoke must cover reflection and
resource-sensitive DTO serialization, validation, Panache mapping, Flyway resources, OpenAPI,
health, and the critical create/replay/conflict paths. A build alone is insufficient. JVM remains
acceptable when native evidence fails or complexity is unjustified.
