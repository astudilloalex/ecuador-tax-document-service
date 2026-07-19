# Phase 0 Research: Generate Standard SRI Invoice XML

**Feature**: `003-generate-sri-invoice-xml`  
**Completed**: 2026-07-19  
**Status**: Complete; all planning questions are resolved

## R-001 — Governing XML authority

**Decision**: Apply the exact SRI Invoice XSD `1.1.0` together with the stricter applicable rules
in the SRI Offline Electronic Tax Documents Technical Sheet v2.33. The XSD governs machine
structure and lexical envelopes; v2.33 governs current mandatory fields, conditional
designations, and specialized-profile eligibility. Both must pass.

**Rationale**: The XSD leaves root attributes and several Invoice fields optional even though
Annex 3 requires them. Conversely, current conditional obligations are not fully represented by
the February 2022 schema package. Treating either source alone as complete would accept a document
that violates the other official source.

**Alternatives considered**:

- XSD-only validation: rejected because it would omit stricter v2.33 obligations.
- Technical-Sheet-only validation or a transcribed schema: rejected because it loses exact,
  machine-verifiable sequence and lexical constraints.
- Legacy generator behavior: rejected as historical evidence only.

Primary sources: [SRI electronic invoicing](https://www.sri.gob.ec/facturacion-electronica),
[Technical Sheet v2.33](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/5a547488-80f3-4966-a2a4-841f2e951986/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.33.pdf),
and the [SRI Invoice bundle](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/05546998-6f29-4870-be3b-62650f312a6c/XML%20y%20XSD%20Factura.zip).

## R-002 — RIMPE authority conflict

**Decision**: Support `RIMPE_CONTRIBUTOR` with exact text
`CONTRIBUYENTE RÉGIMEN RIMPE`. Classify `POPULAR_BUSINESS` as unsupported and return
`INVOICE_XML_PROFILE_UNSUPPORTED` before generation.

**Rationale**: Annex 22 requires `CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE` for Popular
Business, while the pinned `factura_V1.1.0.xsd` permits only the contributor literal. No official
matching replacement XSD was established. The feature promises exact-XSD validity and cannot edit,
relax, or silently bypass the official schema.

**Alternatives considered**:

- Emit the v2.33 Popular Business text: rejected because the exact XSD fails it.
- Emit the shorter contributor text or omit the element: rejected because either changes the
  committed legal classification.
- Patch the XSD: rejected because validation would no longer use the exact official artifact.

## R-003 — Exact offline schema closure

**Decision**: Vendor four unchanged authority resources under the versioned runtime path
`src/main/resources/sri/invoice/1.1.0/`, copied byte-for-byte from
`specs/003-generate-sri-invoice-xml/contracts/sri/invoice/1.1.0/`. Resolve them through a strict
local XML catalog with no network or filesystem fallback.

| Resource | Bytes | SHA-256 |
|----------|------:|--------|
| `factura_V1.1.0.xsd` | 36,356 | `62db9bf0ecceb00ef2b7ed136e59224815e5e5e33c77efc6a0552001e052eb8b` |
| `xmldsig-core-schema.xsd` | 10,293 | `35cf8197da812c85e40d57891b35c94187569ed474a2dac813ce5090dafcd35c` |
| `XMLSchema.dtd` | 16,075 | `2032ead9fd47a61b22fe56aa02be1840bd9bb9015b0c0d3f1e8aac75dd91c3b9` |
| `datatypes.dtd` | 6,357 | `6946432ca7af2e9584f91b48564111fd2c73c8debbbcd9a0e3f5ddd382eeb51c` |

The SRI ZIP itself is 24,515 bytes with SHA-256
`ba1ff0c4e329fe759c3f88dc75f2975780b315b6eb3d0069071b77c1f26fec03`.

**Rationale**: The SRI XSD imports `xmldsig-core-schema.xsd`, but the SRI ZIP omits it. The dated
W3C XML Signature Recommendation schema declares `XMLSchema.dtd`, which references
`datatypes.dtd`. All four are necessary to compile the unchanged schema graph offline.

**Alternatives considered**:

- Runtime SRI/W3C download: rejected because generation must remain deterministic and offline.
- Drop the XML Signature import or DOCTYPE: rejected because it modifies authority bytes.
- Use the mutable current W3C URL: rejected in favor of the dated 2002-02-12 Recommendation.
- Ignore unresolved imports: rejected because that is not complete-schema validation.

## R-004 — Deterministic XML serialization

**Decision**: Use Java 25 StAX (`XMLOutputFactory` and `XMLStreamWriter`) with a
`ByteArrayOutputStream`. Write the exact ASCII declaration
`<?xml version="1.0" encoding="UTF-8"?>` directly, then write the unformatted document body in
UTF-8. Write root attributes in `id`, `version` order; use `writeCharacters`/`writeAttribute` for
one escaping pass; finish with `</factura>` and no trailing byte.

**Rationale**: StAX provides controlled official ordering and safe context-aware escaping without
an annotated object graph, reflection, provider-specific pretty printing, or string
concatenation. Direct declaration output avoids provider variation in quote style or encoding
case. The final `byte[]` can be validated, hashed, persisted, and returned without reserialization.

**Alternatives considered**:

- JAXB: rejected because annotations/reflection and implicit ordering add complexity with no
  benefit for one fixed schema adapter.
- DOM: rejected because it allocates a second tree and serializers may alter declaration or
  whitespace.
- Hand-built XML strings: rejected because correct escaping and context handling are too fragile.
- Pretty printing: rejected because compact byte layout is contractual.

Java authority: [XMLStreamWriter](https://docs.oracle.com/en/java/javase/25/docs/api/java.xml/javax/xml/stream/XMLStreamWriter.html).

## R-005 — Decimal and text representation

**Decision**: Serialize only persisted `BigDecimal` values with `toPlainString()` after checking
the required persisted scale. Money, rates, bases, taxes, discounts, and payments emit exactly two
fractional digits. Quantity and unit price remove only insignificant trailing zeroes beyond the
minimum two and must remain within two through six fractional digits without rounding. Reject
invalid XML 1.0 code points, unpaired surrogates, LF-prohibited or over-length fields before the
writer; never sanitize, normalize, truncate, pre-escape, or replace text.

**Rationale**: XSD `fractionDigits` is a maximum, not a lexical trailing-zero mandate. The
specification supplies the deterministic lexical rule. Validation before serialization prevents a
provider from performing silent replacement, while StAX ensures parsed text equals persisted text.

**Alternatives considered**:

- `DecimalFormat`, locale formatting, or floating point: rejected because locale and binary
  rounding can change value.
- Round to six decimals: rejected because generation may not change persisted meaning.
- Strip unsupported characters: rejected as lossy fiscal-data mutation.

## R-006 — Secure XSD validation

**Decision**: Use Java 25 JAXP `SchemaFactory.newDefaultInstance()`. Verify the length and digest of
all four embedded resources, configure `FEATURE_SECURE_PROCESSING=true`, set
`ACCESS_EXTERNAL_DTD=""` and `ACCESS_EXTERNAL_SCHEMA=""`, and use the catalog with strict
resolution. Compile one immutable `Schema` during startup, create one fresh `Validator` per
request, reapply external-access restrictions to it, and validate the exact candidate `byte[]`.
Sanitize every SAX error rather than returning parser messages.

**Rationale**: `Schema` is thread-safe and shareable; `Validator` is neither thread-safe nor
reentrant. A startup-compiled schema is immutable trusted configuration, not a cache of business
or generated data. Strict closed resolution prevents XXE, SSRF, local-file reads, and schema
substitution. A missing resource, mapping, or digest yields an unavailable validator, never a
fallback validator.

**Alternatives considered**:

- Compile on each request: rejected as duplicate CPU work and a deadline risk.
- Share `Validator`: rejected as unsafe under concurrency.
- Enable external resolution: rejected as a security and determinism violation.
- Expose `SAXParseException`: rejected because messages can contain sensitive payload values.

Java authorities: [Schema](https://docs.oracle.com/en/java/javase/25/docs/api/java.xml/javax/xml/validation/Schema.html),
[Validator](https://docs.oracle.com/en/java/javase/25/docs/api/java.xml/javax/xml/validation/Validator.html),
[JAXP security](https://docs.oracle.com/en/java/javase/25/security/java-api-xml-processing-jaxp-security-guide.html),
and [XML catalogs](https://docs.oracle.com/en/java/javase/25/core/use-catalog-xml-processors.html).

## R-007 — Standard-profile evidence evolution

**Decision**: Define proposed provider consumer contract `2.0.0` in
`contracts/authoritative-fiscal-context-v2.openapi.yaml`. It requires profile
`STANDARD_DOMESTIC_INVOICE_1_1_0`, trigger-set identifier
`SRI-OFFLINE-2.33-INVOICE-1.1.0-TRIGGERS-V1`, and all fourteen named trigger assessments. Each is
`APPLIES`, `DOES_NOT_APPLY`, or `INDETERMINATE`. Persist the evidence as explicit nullable columns
on `fiscal_preparation`: all columns null means a legacy/generic preparation; otherwise every
column must be present. Do not update or infer existing rows.

**Rationale**: Feature 002's `invoiceIssuanceEligible=true` cannot prove absence of mandatory
specialized structures. Explicit named fields make completeness reviewable in Java, OpenAPI, and
PostgreSQL. Flattening keeps evidence atomic with the append-only Fiscal Preparation and follows
the existing snapshot model.

**Alternatives considered**:

- Treat generic eligibility as sufficient: rejected by FR-024 through FR-028.
- Generic map or JSONB: rejected because required trigger completeness and database constraints
  would be weaker.
- Backfill old preparations: rejected because their historical evidence does not prove a fact
  that was never observed and they are append-only.
- Resolve eligibility during Feature 003: rejected because generation must make no provider or
  current-rule call.

Approval by the Fiscal Context Provider Owner and Feature 002 contract owner remains a blocking
implementation-acceptance prerequisite; the design does not claim that approval has occurred.

## R-008 — Complete persisted source and deterministic ordering

**Decision**: Add a Company-scoped `InvoiceXmlGenerationSource` port that hydrates a detached,
immutable view of the complete Invoice Draft, Fiscal Preparation, profile evidence, and possible
artifact. First generation orders lines by persisted position; grouped tax totals by Feature 001's
canonical key (`treatment|officialTaxCode|officialPercentageCode|rate.toPlainString()|catalogVersion`);
payments by official code then canonical lowercase payment-method UUID text; and Additional Information by persisted
position, followed by the Large Contributor entry.

**Rationale**: Existing persistence mapping does not guarantee tax-total or payment iteration
order. Explicit sorting prevents physical row order from changing exact XML bytes. A new port is
needed because the existing Invoice Draft repository load is private and coupled to create-command
idempotency.

**Alternatives considered**:

- Reuse database iteration order: rejected as nondeterministic.
- Recalculate tax totals: rejected because Feature 001 persisted values are authoritative.
- Query current catalogs: rejected because persisted official codes are the source for this
  operation.

## R-009 — CPU isolation and bounded capacity

**Decision**: Keep Company-scoped Hibernate Reactive reads and transactions on their Vert.x
context. Offload only StAX generation, JAXP validation, SHA-256 work, and up-to-2-MiB Base64 response
encoding to one named bounded `ManagedExecutor` through infrastructure adapters. Bound active work
to `max(1, min(4, Runtime.availableProcessors()))`, queued work to 100, clear unneeded propagated
thread contexts, and explicitly resume the captured Vert.x context before another reactive
session. Add `quarkus-smallrye-context-propagation` as a direct dependency if its API is used.

**Rationale**: Quarkus REST `Uni` endpoints normally begin on an event-loop thread, while Hibernate
Reactive sessions are Vert.x-context bound. Marking the entire endpoint `@Blocking` would move
reactive persistence to the wrong execution model. Active and queue bounds cover the required
100-request contention case without unbounded CPU or memory use.

Executor rejection before a first-generation candidate is ready maps to
`INVOICE_XML_GENERATION_FAILED`; rejection while encoding an already persisted replay maps to the
safe `INTERNAL_ERROR`; expiration still maps to `REQUEST_TIMEOUT`. None changes artifact state or
leaks payload.

**Alternatives considered**:

- Run generation/validation or Base64 encoding on the event loop: rejected as CPU-intensive.
- Mark the whole resource blocking: rejected because Hibernate Reactive work must stay on its
  Vert.x context.
- Virtual threads: rejected because these tasks are CPU-bound, not blocking-I/O waits.
- Unbounded common pool: rejected because resource exhaustion would be uncontrolled.
- In-process single-flight map: rejected as cache-like process-local coordination that does not
  establish cross-instance uniqueness.

Primary references: [Quarkus REST execution model](https://quarkus.io/guides/rest),
[Quarkus context propagation](https://quarkus.io/guides/context-propagation), and
[Hibernate Reactive Panache](https://quarkus.io/guides/hibernate-reactive-panache).

## R-010 — Content and memory bound

**Decision**: Cap decoded XML bytes at 2,097,152. Generate into one bounded byte array, validate and
hash that exact array, persist it as PostgreSQL `bytea`, and Base64-encode only at the HTTP
boundary. Domain and application values defensively copy every mutable array.

**Rationale**: Feature 001 already bounds source cardinalities to 500 lines, eight payments, and 15
Additional Information entries. Two MiB leaves ample room for every valid bounded source while
placing a deterministic ceiling on worker and response memory. The maximum fixture must prove it
fits; an overflow fails closed before persistence.

**Alternatives considered**:

- Unbounded buffers: rejected because concurrency could exhaust memory.
- Persist Base64: rejected because it duplicates sensitive data, increases storage by roughly one
  third, and creates a second consistency invariant.
- PostgreSQL large objects or filesystem paths: rejected because `bytea` is transactional and the
  artifact is bounded.

## R-011 — Immutable persistence and database baseline

**Decision**: Retain the established PostgreSQL 18.4 baseline and add immutable Flyway migration
`V7__create_unsigned_sri_invoice_xml_artifact.sql`. It extends new Fiscal Preparations with the
all-or-none profile evidence, hardens all existing Invoice Draft aggregate tables against update
or delete, and creates `unsigned_sri_invoice_xml_artifact` with `bytea` content, 32-byte SHA-256,
byte length, source identities, and creation instant. PostgreSQL checks recompute
`sha256(xml_content)`, verify `octet_length`, enforce the 2-MiB cap, fixed schema/algorithm, composite
Company/source foreign keys, natural uniqueness, and append-only behavior.

**Rationale**: The artifact must survive response loss and be byte-exact for later signing.
PostgreSQL 18 provides transactional `bytea` storage and the core `sha256(bytea)` function. Draft
immutability guards close the direct-write gap on the commercial source on which exact generation
depends. Existing V1 through V6 migrations remain unchanged.

**Alternatives considered**:

- Text XML: rejected because database/client encoding conversions could obscure the exact-byte
  contract.
- Filesystem/object storage: rejected because it creates a distributed commit and reconciliation
  problem for a bounded artifact.
- Store only a digest/path: rejected because replay and signing require the exact content.
- JSONB profile evidence: rejected under R-007.
- Depend on application-only immutability: rejected because critical source/artifact invariants
  should hold for direct database writes too.

PostgreSQL authorities: [binary data](https://www.postgresql.org/docs/18/datatype-binary.html) and
[binary string functions](https://www.postgresql.org/docs/18/functions-binarystring.html).

## R-012 — Concurrency and commit uncertainty

**Decision**: Use Company-plus-draft natural uniqueness and a short pessimistically locked reactive
transaction. Read and detach the source without locks, generate/validate outside the transaction,
then lock the Company-scoped Invoice Draft first, recheck the artifact and exact source
relationships, and either return the winner or insert once. Database uniqueness is the final
cross-process arbiter. Follow Feature 002's commit tracker/classifier/reconciler: after a possibly
committed failure, perform one fresh Company-plus-draft lookup and never regenerate or reinsert.

**Rationale**: This avoids holding a row lock or database connection during CPU work while still
ensuring exactly one durable winner. A fixed lock order avoids deadlock. Explicit commit knowledge
prevents false zero-state claims after lost commit acknowledgement.

**Alternatives considered**:

- Caller `Idempotency-Key`: rejected because natural equivalence is already Company plus draft.
- Hold a transaction during generation: rejected because it wastes connections and extends locks.
- PostgreSQL advisory locks or process-local locks: rejected as extra coordination without stronger
  invariants.
- Retry insert after ambiguous commit: rejected because it can create a replacement or misreport
  the winner.

## R-013 — Replay precedence and time semantics

**Decision**: Validate request shape and Company context, load the Company-scoped persisted
relationships, and return a matching existing artifact before profile or XML work. Replay invokes
no request-time/current-date clock and no persistence clock. For a winning first creation, invoke
`RequestClock.persistenceTime()` exactly once only after locking and confirming that no artifact
exists. Carry the existing monotonic `RequestDeadline` from earliest API entry through every
stage; never start a new ten-second budget.

**Rationale**: Artifact creation time is a technical `Instant`, not Invoice emission date or
current fiscal context. Replay must be stable through date, provider, catalog, or schema-site
changes. The existing generic `RequestContext.capture()` is unsuitable because it obtains a current
Ecuador date at every request.

**Alternatives considered**:

- Reuse Feature 002 request boundary unchanged: rejected because it evaluates current date.
- Create timestamp before contention: rejected because losing candidates must not create or expose
  a creation instant.
- Rebuild on replay: rejected because it can alter exact bytes and fiscal identity.

## R-014 — API contract and outcome mapping

**Decision**: Expose
`POST /api/v1/invoice-drafts/{invoiceDraftId}/unsigned-sri-invoice-xml` with no request body, query,
Company path field, or idempotency key. Return `201 Created` only for the confirmed committer and
`200 OK` for an existing artifact or follower. Both bodies use the same JSON schema and canonical
padded RFC 4648 `xmlContentBase64`; no replay or lifecycle flag is present. Use existing Problem
Details conventions with the specification's stable codes and `Cache-Control: no-store`.

**Rationale**: Status alone distinguishes creation from replay without changing immutable artifact
data. Base64 makes exact arbitrary UTF-8 bytes safe inside JSON, while the digest and length remain
defined over decoded XML bytes.

**Alternatives considered**:

- Return raw XML: rejected by the clarification selecting a metadata envelope.
- Return both raw and Base64 content: rejected as duplicate representations.
- Add replay header/field: rejected by the clarification.
- Add GET/status resource: rejected because the bounded synchronous operation already returns or
  naturally replays its terminal artifact.

## R-015 — Deadline and late work

**Decision**: Use the existing ten-second monotonic deadline. Before source reads, worker
submission, worker completion, commit, and reconciliation, compute remaining time and apply the
minimum of remaining time and the stage ceiling. A Mutiny timeout bounds queue plus worker
observation, but timed-out JAXP work is side-effect-free and its late result is discarded. Database
statement, lock, and transaction budgets use the remaining deadline. After commit might have
started, reconcile or return an outcome that makes no zero-state claim.

**Rationale**: Java XML processing cannot be safely force-stopped by a reactive timeout. Keeping
worker tasks pure and commits separate makes late completion harmless. A single deadline prevents
stage-by-stage timeout multiplication.

**Alternatives considered**:

- Per-stage independent ten-second timeouts: rejected because total latency becomes unbounded.
- Thread interruption as correctness control: rejected because JAXP need not terminate promptly.
- Compensating delete after timeout: rejected because the artifact is immutable and a winner may
  already exist.

## R-016 — Quarkus and dependency baseline

**Decision**: Keep repository Quarkus `3.33.2.1`, Java 25, Mutiny 3.1.1, Hibernate Reactive
3.2.11.Final, and PostgreSQL 18.4 test baseline. Use JDK `java.xml`, `MessageDigest`, and `Base64`;
add no XML library. Add only a direct `quarkus-smallrye-context-propagation` dependency if the
bounded `ManagedExecutor` API is used.

**Rationale**: Quarkus 3.33 is an LTS line already integrated here, and Java 25 support exists in
the selected line. Upgrading would add unrelated risk. JDK APIs satisfy the exact fixed XML use
case and reduce native/reflection complexity.

**Alternatives considered**:

- Upgrade Quarkus: rejected because no Feature 003 need requires it.
- Add JAXB, Saxon, Xerces, or another XML stack: rejected because JAXP/StAX are sufficient.
- Rely silently on transitive context propagation: rejected if its API is referenced directly.

Primary sources: [Quarkus releases](https://quarkus.io/releases/),
[Quarkus 3.33](https://quarkus.io/blog/quarkus-3-33-released/), and
[Quarkus 3.31 Java 25 support](https://quarkus.io/blog/quarkus-3-31-released/).

## R-017 — Health, observability, and sensitive storage

**Decision**: Liveness remains process-only. Readiness requires the existing PostgreSQL/catalog
checks, successful Flyway V7, and successful local schema provenance verification/compilation; it
does not call SRI, W3C, Company, current catalogs, or the Fiscal Context provider. Return validator
unavailable if the endpoint is reached while local validation is unavailable. Logs, metrics,
traces, and audit signals use only safe opaque IDs, bounded counts, duration, schema identifier,
and outcome code. The artifact uses the platform's approved database encryption-at-rest,
encrypted-backup, restore, retention, and disposal controls; release remains blocked on Platform
Operations evidence. No certificate lifecycle applies.

**Rationale**: The schema closure and PostgreSQL are the only runtime dependencies of this
feature. XML, Access Key, RUC, buyer, name, address, and other fiscal values are sensitive. A new
storage service, audit table, delete route, retention process, or custom crypto system would exceed
scope.

**Alternatives considered**:

- Probe official sites in readiness: rejected because runtime validation is deliberately offline.
- Log digest or XML for correlation: rejected because both are sensitive/high-cardinality.
- Add custom encryption or deletion scheduler: rejected because platform controls own those
  concerns and the artifact is contractually immutable.

## R-018 — JVM and native status

**Decision**: JVM execution on Linux is mandatory and must be proven with packaged-JAR tests that
compile the offline schema, validate valid/invalid fixtures, confirm worker isolation, and round
trip exact PostgreSQL bytes/digests. Native compatibility is deferred and unclaimed until a
Mandrel/GraalVM 25 build embeds `sri/invoice/1.1.0/**` and passes the same catalog, JAXP, SHA-256,
valid/invalid, and HTTP scenarios.

**Rationale**: Classpath resource inclusion and XML catalog URL handling in native images require
actual build/runtime evidence. The constitution explicitly permits JVM deployment rather than
weakening secure XML behavior for native compatibility.

**Alternatives considered**:

- Claim native support from JVM tests: rejected because resource inclusion and JAXP behavior can
  differ.
- Modify or simplify the schema for native: rejected because authority bytes are immutable.
- Declare native unsupported permanently: rejected because a concrete evidence path is available.

Primary references: [Quarkus native resource guidance](https://quarkus.io/guides/writing-native-applications-tips)
and [building native images](https://quarkus.io/guides/building-native-image).

## Research closure

All material planning questions are resolved. Two non-functional release prerequisites remain
explicit rather than inferred: approval of the provider/Feature 002 contract evolution and
Platform Operations evidence for protected storage and retention. Native support remains deferred;
JVM support is mandatory. There is no Pending Functional Validation item.
