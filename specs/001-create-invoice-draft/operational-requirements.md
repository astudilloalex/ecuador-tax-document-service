# Operational and Performance Requirements: Create Invoice Draft

## Measurement Boundary

Request latency includes this service's API header/body decoding and Application Stage-6
normalization/fingerprinting, local
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
boundary and normally stopped when the response ends, so response serialization is included. It
MUST NOT use wall-clock timestamps, `requestCreationInstant`, `createdAt`, `updatedAt`, or correlation
timestamps. Client network, gateway, and upstream queueing time are outside this server-side
measurement.

`requestCreationInstant` is separate functional evidence. The request boundary captures it exactly
once from the approved wall clock and derives the expected emission date once using
`America/Guayaquil`. It MUST be passed through validation and persistence without recapture, MUST
remain fixed if commit crosses midnight, and MUST NOT be replaced by `createdAt` or `updatedAt`. Equivalent replay
may capture an operational request instant but MUST NOT use it to revalidate or mutate the original
emission date.

`createdAt` and `updatedAt` are separate functional persistence evidence: one UTC
`java.time.Instant` is obtained by T076 from the injectable deterministic persistence clock exactly
once inside its active transaction, after all business validations succeed and immediately before
root persistence, then assigned to both fields. T063, API, Domain, and mappers never invoke this
clock or supply/replace/overwrite either value. The equal immutable values are persisted, returned
after commit confirmation, and returned on replay; rollback exposes neither. They are not
PostgreSQL physical commit timestamps, are never queried or reconstructed after commit, and do not
require `track_commit_timestamp`. Neither is a latency measurement source. Replay loads the
original persisted representation and performs no clock call, identifier allocation, persisted
canonical-value rebuild, or aggregate creation.

Correlation initialization/validation time is included in request duration. Correlation identity
never starts, stops, partitions, or otherwise changes a performance measurement.

## Request Performance Budgets

| Profile | Load | Required outcome |
|---------|------|------------------|
| Typical new draft | At most 10 lines, 2 payments, 5 additional entries; warmed JVM | p95 ≤ 750 ms; p99 ≤ 1.5 s |
| Maximum valid draft | 500 lines, 8 payments, 15 additional entries, body ≤ 2 MiB | p95 ≤ 3 s; p99 ≤ 5 s |
| Equivalent replay | Existing binding and complete aggregate | p95 ≤ 250 ms; p99 ≤ 500 ms |
| Idempotency conflict | Existing binding, different fingerprint | p95 ≤ 250 ms; p99 ≤ 500 ms |
| Same-scope concurrency | 50 concurrent equivalent commands | All terminate within 10 s; exactly one draft and binding; no pool starvation after completion |

The overall request deadline is 10 seconds and the local database write-transaction timeout is 5
seconds. A request conclusively detected larger than 2 MiB before deadline expiry is rejected before
Company-header evaluation; deadline-first slow-body processing returns `REQUEST_TIMEOUT` instead.

The overall deadline begins at the same service boundary as the monotonic request timer. Therefore
body reading, Company-header validation, safe correlation initialization/validation, idempotency
processing, domain work, persistence, and serialization all contribute elapsed time against the
same 10-second boundary. Failure precedence does not reset the timer. FR-041 orders only outcomes
that become conclusive; deadline arbitration applies before and during every stage.

One earliest-ordered API routing handler owns exactly one transport-independent monotonic request
deadline for each matched `POST /api/v1/invoice-drafts` attempt. It starts the deadline before body
consumption, stores the deadline and safe correlation state only in request-local API state, arms one
non-blocking timer, and keeps that timer active through response serialization. No later stage may
restart or extend the deadline, and the timer MUST be cancelled when the terminal response ends.

The API transports the same fixed neutral deadline context into the application command only for
cooperative cancellation and remaining-budget checks. Application orchestration checks
the remaining budget before each asynchronous stage and passes an explicit remaining `Duration`
derived from that one deadline to every local aggregate and reference-data repository invocation.
No port depends on HTTP or Quarkus request objects. Each reactive database adapter computes its
effective operation timeout as the minimum of its configured database-operation timeout and the
supplied remaining request budget, applies that bound to pool acquisition and every reactive query
subscription, and starts no database work when the remainder is zero or negative. A write
transaction is additionally bounded by the lesser of that effective budget and five seconds. If
the configured database-operation timeout is the smaller bound and fires while request budget still
remains, the adapter returns a neutral persistence-unavailable failure; exhausted budget returns a
neutral deadline/uncertain outcome. No application or repository component maps HTTP.

The API adapter alone races the application `Uni` against expiry, atomically accepts exactly one
terminal result, discards late application/database completion, prevents a second response, and
then maps HTTP status/Problem Details. If expiry becomes terminal first, only API maps correlated
`REQUEST_TIMEOUT` (`504`); if an application outcome is accepted first, later expiry cannot replace
it. Company `400` and all other HTTP mappings likewise occur only after API arbitration. In
particular:

- payload size conclusively over 2 MiB first selects `413`; deadline-first slow-body processing
  selects `504`;
- Company, correlation, or idempotency-header invalidity conclusively detected first selects its
  `400`; deadline-first classification selects `504`;
- replay, conflict, validation, or calculation conclusively completed first selects its approved
  result; deadline-first completion selects `504`;
- confirmed rollback/failure or confirmed commit before expiry selects its approved result;
  deadline expiry while commit outcome is unresolved selects `504` and an uncertain outcome.

Business instrumentation distinguishes Stage 10 calculation-independent validation, Stage 11A
calculation, and Stage 11B calculated-value validation in the exact specification order. Metrics
must not imply a Stage 11B check happened during Stage 10.

Confirmed expiry before persistence begins and confirmed complete rollback leave zero state. An
unresolved or successful commit makes no zero-state claim and MUST NOT trigger compensation or
deletion; equivalent replay with the same CompanyId, key, and content reconciles authoritative
state. A generic HTTP connection, idle, or body timeout is not sufficient as the sole
implementation of this end-to-end deadline.

Before HTTP response commitment, the atomic selection rules above govern. Once the response is
committed, deadline expiry is telemetry-only: status and headers are unchanged, no alternative
timeout response, second body, or error envelope is written, no database or domain mutation starts,
and no committed draft or binding is compensated or deleted. Existing response serialization is
allowed to continue to normal completion. The service records one safe
`request_deadline_exceeded_after_response_commit` event/counter and then cancels the timer on
response end.

If evidence misses a budget, the plan must be revisited before implementation is considered
complete. The response is not to add a Company cache, application cache, broker, or unapproved
distributed component.

Performance fixtures MUST use the exact published tax-rule and payment-method UUIDs from
`reference-data-baseline.md`. Benchmarks MUST NOT substitute invented identifiers, runtime-generated
rows, or excluded SRI representations. Execution evidence begins only after the later Flyway seed
implements that approved baseline unchanged.

## Capacity and Resource Safety

- Accept at most 500 invoice lines, 8 payments, and 15 additional-information entries.
- Bound request body at 2,097,152 bytes.
- Enforce quantity `numeric(12,6)`, monetary `numeric(17,2)`, and percentage-rate `numeric(5,2)`
  envelopes in representation, calculation, persistence, and response paths. Individually valid
  inputs whose exact intermediate, grouped, payment-sum, or total result overflows the monetary
  maximum MUST fail before persistence with `MONETARY_RANGE_EXCEEDED`.
- Bound reactive connection-pool acquisition and query duration beneath the request deadline.
- Bound buyer, IVA, and payment reference-data pool acquisition and every reactive lookup by the
  minimum of configured database-operation timeout and supplied remaining request budget; do not
  subscribe to database work when that budget is exhausted.
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
- on a conclusively selected payload-limit result, correlation classification exists solely to
  preserve one valid identifier or generate a safe replacement for absent/invalid input; it never
  emits the normal correlation-validation `400` and no database operation follows;
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
- database outcome category;
- for `request_deadline_exceeded_after_response_commit`, the already selected response status and
  elapsed duration; CompanyId and draftId only when already available and permitted by the existing
  structured audit policy.

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
- `request_deadline_exceeded_after_response_commit` count without high-cardinality labels;
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
- operationally significant rollback;
- request deadline exceeded after HTTP response commitment, containing only correlation identifier,
  operation name, already selected response status, elapsed duration, and optionally an already
  available CompanyId/draftId under the existing audit policy.

Buyer identification/contact, full request/response payloads, raw idempotency keys, normalized
fingerprint inputs, SQL, stack traces, internal paths, tokens, and certificates are prohibited from
every audit event, including the post-response-commit deadline event.

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
- controlled, no-sleep deadline races for payload-size, headers, replay/conflict, validation,
  calculation, known persistence outcome, unresolved commit, and post-response-commit expiry;
- reference-data timeout clamping with each side of the minimum, exhausted-before-invocation, and
  expiry-during-lookup vectors;
- sensitive-data and high-cardinality-label checks;
- the performance profiles above.

Native execution is optional. If claimed, both build and runtime smoke must cover reflection and
resource-sensitive DTO serialization, validation, Panache mapping, Flyway resources, OpenAPI,
health, and the critical create/replay/conflict paths. A build alone is insufficient. JVM remains
acceptable when native evidence fails or complexity is unjustified.
