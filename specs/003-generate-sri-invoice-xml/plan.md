# Implementation Plan: Generate Standard SRI Invoice XML

**Branch**: `003-generate-sri-invoice-xml` feature context (workspace Git branch: `10-ft-3`) |
**Date**: 2026-07-19 | **Spec**: `specs/003-generate-sri-invoice-xml/spec.md`

**Input**: Completed Feature 003 specification and clarification session 2026-07-19 (four answers
recorded in `spec.md`).

## Summary

Add one `invoicexml` capability exposing a synchronous Company-scoped POST for one prepared Invoice
Draft. It loads the complete persisted Draft, exact committed Fiscal Preparation, new immutable
per-trigger standard-profile evidence, and any existing artifact. A matching artifact wins before
profile, clock, or XML work. Otherwise, application ports invoke a deterministic Java 25 StAX SRI
adapter and secure JAXP validator on a dedicated bounded CPU executor, validating the exact final
UTF-8 bytes against the pinned SRI Invoice XSD `1.1.0` and its strict offline W3C dependency
closure. A short Hibernate Reactive/PostgreSQL transaction locks the Company-scoped draft,
rechecks the winner/source, and commits one append-only `bytea` artifact with database-verified
SHA-256 and byte length. Natural uniqueness and reconciliation make retries and 100 concurrent
requests converge on the same bytes. No signing, certificate, SRI call, sequence allocation,
current fiscal lookup/date evaluation, draft/preparation mutation, delivery, or lifecycle status is
introduced.

## Technical Context

**Language/Version**: Java 25

**Framework**: Quarkus `3.33.2.1`, retaining the repository's exact LTS/BOM baseline; no upgrade is
needed for JDK StAX/JAXP, reactive persistence, or Java 25

**Reactive Model**: Mutiny for asynchronous API/application/persistence flows; request database
work stays on its captured Vert.x context, while bounded infrastructure adapters isolate only CPU
work

**Persistence**: Hibernate Reactive with Panache; entities, Company predicates, row locks,
transactions, SQLSTATE/constraint classification, and commit reconciliation remain in
`infrastructure`

**Database**: PostgreSQL 18.4 in verification and supported PostgreSQL 18 minors in deployment;
retain the existing reactive pool maximum of 20

**Schema Migration**: Flyway only; add immutable
`V7__create_unsigned_sri_invoice_xml_artifact.sql` after V6, with no data seed or evidence backfill

**Caller Security**: None inside this repository. Authentication, authorization, Keycloak, JWT,
OIDC, API keys, principals, roles, permissions, service authentication, `Authorization`, `401`, and
`403` remain out of scope.

**Testing**: JUnit 5, Quarkus JUnit, REST Assured, Quarkus Vert.x/Hibernate Reactive support, the
existing PostgreSQL 18.4 test resource, exact SRI/W3C resource hashes, official-rule synthetic XML
fixtures, StAX mapping/golden-byte tests, secure JAXP/XSD/XXE tests, application ordering and zero-
call spies, Company-scoping/API/OpenAPI tests, Flyway/constraint/direct-mutation tests, rollback and
lost-commit-acknowledgement tests, 100-request concurrency, sensitive-data/architecture/health
tests, and packaged JVM smoke/performance evidence. Signature, PKCS#12, SOAP/SRI, PDF, queue, event,
notification, and security tests prove absence only.

**Target Execution**: Mandatory packaged JVM execution on Linux with Java 25 and PostgreSQL 18.x

**Native Compatibility**: Deferred and unclaimed. A later claim requires an actual Mandrel/GraalVM
25 build and runtime proof for the complete JAXP/catalog dependency graph, classpath resource
inclusion, StAX, SHA-256, Hibernate Reactive, and HTTP scenarios. JVM remains the accepted target.

**External Integrations**: None at Feature 003 runtime. Validation uses only immutable local SRI/W3C
resources. Feature 002 must evolve its existing `authoritative-fiscal-context` consumer contract to
proposed version `2.0.0` and persist complete profile evidence before this operation; Feature 003
does not call that provider. There is no SRI SOAP/REST, certificate, signing, filesystem/object
storage, rendering, broker, notification, Company, or identity adapter.

**Company Context Boundary**: Require exactly one `X-Company-Id`, trim only surrounding ASCII
SP/HTAB, parse a syntactically valid non-nil UUID, canonicalize it, and map it to existing immutable
`CompanyId`. Carry it explicitly through every source/artifact query, lock, insert, reconciliation,
and composite FK. There is no Company lookup/client/port, local Company row, authentication,
authorization, cache, replication, shared persistence, cross-service FK/transaction, token/session
source, request-body/path/query Company value, or success-response Company property.

**Performance Goals**: Every conclusive request finishes inside the fixed ten-second overall
deadline. In a warmed packaged JVM with PostgreSQL 18.4, one maximum valid bounded source generates,
validates, persists, Base64-encodes, and returns without an event-loop blocking signal; 100
same-draft first requests produce one 201, 99 successful 200 responses, one artifact, and pool/
executor recovery inside the same deadline. Worker parallelism never exceeds four or available
processors, the queue never exceeds 100, and decoded XML never exceeds 2 MiB.

**Constraints**: No request body/query/Idempotency-Key; one draft per request; 10-second monotonic
deadline; source read ceiling 4 seconds, lock ceiling 3 seconds, write transaction ceiling 5
seconds, each clamped to remaining time; CPU parallelism `max(1,min(4,availableProcessors))`; queue
100; exact compact UTF-8 bytes; decoded XML cap 2,097,152; schema resources verified at startup;
one clock invocation only for a confirmed winner; fixed draft-first lock order; no cache; no worker
side effect; no automatic regeneration/reinsert after possible commit.

**Scale/Scope**: One ordinary domestic Invoice XML `1.1.0` per Company-owned prepared Invoice
Draft; existing Feature 001 limits of 500 lines, eight payments, and 15 Additional Information
entries apply. Large Contributor leaves at most 14 persisted Additional Information entries.
Batch generation, other profiles/documents, and mutable XML lifecycle are outside scope.

**Sensitive Data**: XML bytes, Access Key, Numeric Code, RUC, buyer identification, names,
addresses, descriptions, and Additional Information are fiscal/personal payload. Store XML only in
managed PostgreSQL `bytea`, expose it only in the explicit no-store success envelope, and inherit
the Invoice record's approved TLS, database encryption-at-rest, encrypted-backup/restore,
retention, and disposal controls owned by `Platform Operations Owner`. Errors, parser messages,
logs, metrics, traces, health, audit signals, and fixtures derived from production contain none of
those values or the digest. No certificate lifecycle, custom application encryption/key system,
delete API, filesystem copy, or independent retention scheduler is added.

### Null safety scope

Every new concrete Java type is directly annotated `@NullMarked`, matching current repository
practice; existing `package-info.java` files remain package declarations rather than being used to
transitively mark unaudited code. `byte[]` inputs/outputs are defensively copied. Error Prone,
NullAway, JSpecify, and warning-as-error behavior remain enabled with no new broad suppression or
exclusion.

## Source and Terminology Evidence

| Authority | Applicable source and version/path | Requirements or decisions governed |
|-----------|------------------------------------|-------------------------------------|
| Ecuadorian legislation and official SRI documentation | [Consolidated Regulation for Sales, Withholding, and Complementary Documents through 2023-12-29](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/9fb49475-f058-49a1-b08a-f31bf4deb074/Reglamento_Comprobantes_Venta_RetencionYDC_29122023.pdf); [SRI Technical Sheet v2.33, updated July 2026 and modified 2026-07-13](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/5a547488-80f3-4966-a2a4-841f2e951986/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.33.pdf), Annex 3 and applicable Annexes 11–25 | Invoice obligations, XML order/fields, decimals, conditional designations, profiles, unsigned versus authorized document |
| Official SRI schema | [SRI Invoice XML/XSD ZIP, published February 2022](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/05546998-6f29-4870-be3b-62650f312a6c/XML%20y%20XSD%20Factura.zip), retrieved 2026-07-19; exact `factura_V1.1.0.xsd` in `contracts/sri/invoice/1.1.0/` | Root, sequence, cardinality, lexical constraints; ZIP/resource hashes recorded in `research.md` and schema README |
| W3C XML Signature dependency | [XML Signature Recommendation dated 2002-02-12 schema](https://www.w3.org/TR/2002/REC-xmldsig-core-20020212/xmldsig-core-schema.xsd), `XMLSchema.dtd`, and `datatypes.dtd`, retrieved 2026-07-19; exact bytes in `contracts/sri/invoice/1.1.0/` | Complete unchanged offline XSD dependency closure and notices |
| Constitution | `.specify/memory/constitution.md` v2.0.1, approved 2026-07-16 | Official-source precedence, English terms, Clean Architecture, reactive/Company/sensitive/persistence/runtime gates |
| Approved specification and clarifications | `specs/003-generate-sri-invoice-xml/spec.md`, FR-001–FR-088, DR-001–DR-015, SC-001–SC-017; clarification session 2026-07-19 | Bounded target behavior, evidence profile, Base64 envelope, 201/200 semantics, compact bytes |
| Approved source features | Feature 001 spec/contracts/implementation and V1–V5; Feature 002 spec/contracts/implementation and V6 | Existing complete Invoice Draft and immutable Fiscal Preparation sources; generic Feature 002 profile gap |
| Architecture decisions | No ADR exists; `research.md`, `data-model.md`, and the two OpenAPI contracts record Feature 003 decisions | JDK XML stack, worker boundary, V7, profile contract, API, commit recovery, native deferral |
| Canonical terminology | `docs/migration/terminology-mapping.md`, verified 2026-07-19 | English Feature 003 terms and official SRI Adapter Only tags |
| Legacy evidence | `docs/legacy/source-baseline.md` and cited `docs/legacy/as-is/` analysis listed in `spec.md` | Historical discovery/negative vectors only; no target generator, API, schema, status, or storage compatibility |

**Source Conflicts and Resolutions**:

1. Technical Sheet v2.33 Annex 22 requires a Popular Business legend rejected by the pinned SRI
   Invoice XSD `1.1.0`. Both sources remain authoritative; `POPULAR_BUSINESS` is unsupported and
   fails before XML. The schema and source classification are not altered.
2. The SRI ZIP omits its relative XML Signature import. The unchanged dated W3C Recommendation XSD
   and its two DTD dependencies are pinned byte-for-byte, resolved strictly offline, and verified
   by length/digest. No network fallback or import removal is allowed.
3. The XSD marks root attributes, `propina`, `pagos`, and `codigoPrincipal` optional while Technical
   Sheet Annex 3 requires them. The stricter joint rule applies; XSD validity alone is insufficient.
4. The SRI ZIP example uses placeholder/local-schema values and specialized branches. It is
   evidence about schema shape only, not a target golden document. Synthetic approved-profile
   fixtures are generated from persisted target values.
5. Feature 002 contract `1.0.0` has only generic eligibility. Proposed contract `2.0.0` adds every
   named trigger assessment; old preparations remain unchanged and return profile undetermined.
6. Legacy behavior may generate/sign/store in one issuance flow or regenerate on retry. Target
   Feature 003 commits one unsigned artifact and naturally replays it with zero external/lifecycle
   side effects.

**Pending Functional Validation**: None. Popular Business is explicitly unsupported; all other
profile, mapping, byte, error, and replay choices are resolved. Provider-contract approval,
protected-storage evidence, and optional future native proof are accountable release/evidence gates,
not unresolved business behavior.

**Terminology Mapping Impact**: `docs/migration/terminology-mapping.md` now registers Standard
Invoice XML Profile Evidence, Specialized Profile Trigger Assessment, Unsigned SRI Invoice XML
Artifact, XML Integrity Evidence, and Official Invoice Schema. Exact `factura`, `infoTributaria`,
`infoFactura`, `detalles`, `detalle`, `impuestos`, `pagos`, `infoAdicional`, and
`campoAdicional` remain SRI Adapter Only.

## Approved architecture and release dependencies

**Profile evidence contract**: Proposed consumer contract
`contracts/authoritative-fiscal-context-v2.openapi.yaml` is version `2.0.0`, accountable to the
`Fiscal Context Provider Owner` and Feature 002 contract owner. It adds the fixed standard profile,
technical rule/version, exhaustive trigger-set ID, and fourteen explicit assessments. Feature 002
must validate and atomically persist the complete evidence. Tests may use a contract fixture, but
Feature 003 first-generation acceptance is blocked until both owners approve the contract and the
deployed Feature 002 path writes it. Existing rows remain all-null profile evidence and fail closed.

**Schema provenance**: Exact planning bytes and hashes in `contracts/sri/invoice/1.1.0/` are
approved input to implementation. Runtime copies must be byte-identical at
`src/main/resources/sri/invoice/1.1.0/`; tests and readiness fail closed on any mismatch. This is a
local immutable technical dependency, not a runtime external service or cache.

**Sensitive-data platform controls**: `Platform Operations Owner` owns target TLS, PostgreSQL
encryption at rest, encrypted backup, successful restore evidence, the Invoice-record retention
policy, and linked artifact disposal. Target-environment release is blocked until that evidence is
approved. Feature 003 adds no custom crypto, deletion behavior, or scheduler.

## Constitution Check

*GATE: Every row passed before research and was re-evaluated after Phase 1 design.*

| Gate | Pre-Research evidence | Post-Design evidence |
|------|-----------------------|----------------------|
| Greenfield scope: one bounded outcome; no implicit legacy compatibility | PASS — one prepared draft becomes zero-or-one unsigned artifact; legacy is evidence only | PASS — one `invoicexml` capability/route/table; no legacy API, status, filesystem, or payload compatibility |
| Authority: official sources are versioned; conflicts and Pending Functional Validation are recorded | PASS — v2.33 and exact XSD identified; missing import/conflicts declared for research | PASS — four-resource closure/hashes, joint-rule policy, and Popular Business resolution recorded; no PFV |
| Language: target names are English and terminology mapping decisions are respected | PASS — spec uses approved English except official XML tags | PASS — mapping updated 2026-07-19; official tags confined to SRI adapter/contracts |
| Baseline: required technologies are fixed; Quarkus version research is identified pre-research and resolved with justification post-design | PASS — repository baseline and XML/native questions identified | PASS — Java 25, Quarkus 3.33.2.1, PostgreSQL 18.4 retained; JDK XML chosen; no unrelated upgrade |
| Architecture: `api`, `application`, `domain`, and `infrastructure` dependencies and mappings comply | PASS — generator/validator ports required | PASS — capability-local packages, independent ports, explicit DTO/domain/entity mappings, infrastructure XML names |
| Domain purity: no framework, transport, persistence, JSON, security, or Mutiny types in `domain` | PASS — immutable Java values planned | PASS — artifact/profile/integrity values are synchronous Java-only; byte arrays copied |
| Reactive safety: every blocking or CPU-intensive operation is isolated, bounded, timed out, and testable | PASS — XML/XSD work identified as CPU-sensitive | PASS — max-four/queue-100 executor isolates generation, validation, hashing, Base64; DB returns to Vert.x; startup compilation isolated |
| Fiscal correctness: official rules, `BigDecimal` policies, time semantics, and invalid-data behavior are explicit | PASS — persisted values/no recalculation required | PASS — joint XSD/v2.33 mapping, exact decimals/text/date/conditional rules and fail-closed classifications defined |
| Internal caller boundary: no authentication, authorization, identity, token, security scheme, `401`, or `403` behavior is introduced | PASS — explicitly excluded | PASS — inbound and proposed provider contracts use `security: []`; no security code/outcome |
| Company boundary: Company context uses the mandatory single `X-Company-Id` UUID header; Company identifiers are absent from request bodies/input schemas and appear in responses only when explicitly contracted; Company-owned aggregate/persistence/idempotency operations enforce the UUID while immutable global SRI catalogs remain outside automatic Company scope; no Company lookup, snapshot, shared persistence, cache, or replication exists | PASS — exact header/natural key approved | PASS — Company on every owned read/lock/FK/write/reconcile; absent from body/path/query/response; no Company master/cache |
| Sensitive data: storage, encryption, redaction, retention, and certificate lifecycle are defined before implementation | PASS — XML/fiscal values identified; certificate excluded | PASS — PostgreSQL-only protected storage, no-store response, platform owner/evidence, redaction and no-certificate lifecycle defined |
| Persistence: Flyway-only evolution, immutable migrations, database invariants, and empty-database tests are defined | PASS — exact immutable artifact required | PASS — V7 profile/source guards/artifact constraints, `bytea` digest/length checks, empty/V6-upgrade tests; no backfill |
| Boundary consistency: states, retries, idempotency, duplicates, timeouts, recovery, reconciliation, and terminal outcomes are defined | PASS — natural replay and unknown commit required | PASS — one-state artifact, draft-first lock, uniqueness, confirmed/replay outcomes, possible-commit reconciliation, no regeneration |
| API and async quality: DTO separation, validation ownership, stable errors, correlation, and result observation are defined | PASS — synchronous final envelope and stable catalog approved | PASS — standalone OpenAPI, 201/200 body equality, Problem Details, safe correlation, no hidden async job |
| External adapters: ports, endpoint configuration, sanitized observability, resilience, and contract evidence are defined | PASS — local XML ports and Feature 002 evidence dependency identified | PASS — independent StAX/JAXP ports, strict local resources; Feature 003 has zero external calls; provider v2 belongs to Feature 002 |
| Testing: acceptance scenarios and applicable risk-based tests are identified before production tasks | PASS — SC-001–SC-017 and 100 concurrency defined | PASS — domain/application/adapter/PostgreSQL/API/security/JVM/native-evaluation paths cover every SC |
| Operations: meaningful liveness/readiness, structured logs, auditing, and destination-consistent health checks are defined | PASS — schema/database readiness and redaction required | PASS — process liveness; same-PostgreSQL/Flyway V7 plus local-validator readiness; bounded safe signals/audit, no external probe |
| Simplicity: every dependency, abstraction, process, store, and distributed interaction is justified | PASS — no cache/broker/external call approved | PASS — JDK XML, one table, one bounded executor, narrow ports; no background process/new store/distributed interaction |
| Runtime evidence: JVM verification is mandatory and native status has an evidence path | PASS — JVM required and native evaluation declared | PASS — packaged JVM scenarios/commands defined; native explicitly deferred pending actual complete-resource proof |

## Reactive and Resource Boundary Design

| Operation | Infrastructure adapter and application port | Blocking/CPU classification | Execution context | Timeout and resource bound | Required concurrency/failure evidence |
|-----------|---------------------------------------------|-----------------------------|-------------------|----------------------------|---------------------------------------|
| Schema resource read, provenance verification, and `Schema` compilation | `JaxpSriInvoiceXsdValidator` implementing `SriInvoiceSchemaValidatorPort` | Startup classpath I/O plus CPU-intensive schema compilation | Quarkus startup initialization thread, never a request event loop | One compilation; four pinned resources totaling 69,081 bytes; strict catalog/no network; unavailable state on failure | exact hash/length, missing/mutated/misresolved resource, network/XXE denial, readiness down, existing liveness up |
| Complete source/artifact read | `InvoiceXmlArtifactRepositoryAdapter` implementing `InvoiceXmlGenerationSourceStore` | Non-blocking database I/O | Captured Vert.x/Hibernate Reactive context | min(4-second ceiling, remaining deadline); pool max 20 | cross-Company, missing/partial/corrupt relationships, DB timeout/outage, pool recovery |
| Deterministic StAX generation | `StaxSriInvoiceXmlAdapter` implementing `SriInvoiceXmlGeneratorPort` | CPU-intensive, bounded in-memory output | Named bounded `ManagedExecutor`; clear unrelated context | active `max(1,min(4,processors))`; queue 100; exact 2-MiB cap; remaining deadline | max fixture, 100 contention, executor rejection, timeout/late discard, no event-loop signal, exact bytes |
| Exact JAXP schema validation and SHA-256 | `JaxpSriInvoiceXsdValidator` / `SriInvoiceSchemaValidatorPort`; JDK digest inside validated-result adapter | CPU-intensive | Same bounded executor; fresh `Validator` per invocation | queue/active bounds above; exact final bytes; remaining deadline; no external access | valid/invalid/security vectors, 100 concurrent validators, no shared-validator race, sanitized failure |
| Success Base64 encoding on create and replay | `JdkInvoiceXmlContentEncoder` implementing `InvoiceXmlContentEncoderPort` | CPU-intensive up to 2 MiB input/2.8 MiB output | Same bounded executor, then return to captured Vert.x context | queue/active bounds above; remaining deadline | canonical padding, decoded equality, replay zero XML/XSD calls, 100 replay responses, late discard |
| Artifact lock/recheck/insert/flush/commit/reconcile | `InvoiceXmlArtifactRepositoryAdapter` implementing `InvoiceXmlArtifactStore` | Non-blocking database I/O | Captured Vert.x/Hibernate Reactive context only | lock max 3s; transaction max 5s; operation max 4s; all clamped to remaining; pool 20 | one winner/100 followers, direct uniqueness, confirmed rollback, lost COMMIT ack, fresh reconciliation, deadlock absence |

No request worker holds a database session, connection, or row lock. A Mutiny timeout cannot prove
the underlying JAXP thread stopped; worker work is pure, bounded, side-effect-free, and every late
result is discarded before persistence. A first-generation candidate is Base64-encoded before its
commit, so an encoder failure leaves no artifact; an existing/follower path always encodes the
persisted winner rather than substituting uncommitted candidate bytes.

## Company Context and Sensitive-Data Design

**Internal Caller Boundary**: The service does not authenticate/authorize callers, interpret
identity, validate Company existence/status/entitlement, or define security schemes,
`Authorization`, `401`, or `403`. External platform controls are assumed to permit the upstream
billing caller to receive the success payload.

**Company Header Contract**: `X-Company-Id` must be present exactly once after header list
inspection, nonblank after ASCII SP/HTAB edge trim, syntactically UUID, and non-nil. Canonical
lowercase/hyphen form becomes `CompanyId`. Missing is `COMPANY_CONTEXT_REQUIRED`; repeated,
comma-combined, blank, malformed, or nil is `COMPANY_CONTEXT_INVALID`. Company is prohibited in
path/query/body/input schemas/tokens/sessions and omitted from success. Invalid transport input is
rejected before Company-owned reads.

**Company Ownership Scoping**: Every draft/preparation/profile/artifact query, draft lock, insert,
unique lookup, and reconciliation uses `company_id` plus the local resource identity. Composite
FKs prove artifact-to-preparation-to-draft ownership. Natural equivalence is only Company plus
Invoice Draft; correlation and prohibited idempotency headers are irrelevant. The local official
schema bundle is immutable global technical authority and has no Company column/predicate.

**Company Master-Data Boundary**: No Company/Issuer/Establishment/Emission Point lookup, port,
client, repository, table, cache, replication, shared database, cross-service FK/transaction,
readiness call, or generation-time snapshot exists. Feature 003 reads the already committed Fiscal
Context Snapshot only.

**Sensitive Data and Certificate Lifecycle**: Exact XML bytes and all represented fiscal/personal
values stay in managed PostgreSQL and the explicit response. Platform Operations owns TLS,
database/backup encryption, restore, retention, and disposal; release requires its evidence.
Application-level custom encryption, keys, rotation, deletion, backup, or retention scheduling is
not added. Certificates/passwords/signing keys are never accessed, stored, logged, or probed, so no
certificate rotation/expiration/revocation lifecycle applies. Every schema/generation/persistence
failure is fail-closed and redacted.

## Data and External Consistency Design

| Boundary or logical command | Intermediate states | Retry and idempotency | Duplicate handling | Timeout | Failure recovery and reconciliation | Observable terminal outcomes |
|-----------------------------|---------------------|-----------------------|--------------------|---------|-------------------------------------|------------------------------|
| Generate or replay unsigned artifact | No persistent provisional state; detached source and validated candidate are request-local; persistent state is absent or committed | Unlimited natural replay by Company+draft; no Idempotency-Key; replay never regenerates | Initial lookup/recheck and DB `UNIQUE(company_id, invoice_draft_id)` select one winner; follower discards candidate | One 10s deadline across all stages | Confirmed precommit/rollback leaves absent; possible commit triggers one fresh Company+draft reconciliation; never reinsert/regenerate; inconclusive becomes outcome unknown | 201 confirmed committer; 200 existing/follower/reconciled winner; stable 4xx/5xx; timeout/unknown makes no false zero-state claim |
| Local schema initialization | `AVAILABLE(compiled Schema)` or `UNAVAILABLE(safe reason class)`; no generated artifact state | Reinitialized only with process lifecycle/deployment; never substitute/fetch at request time | One immutable compiled descriptor per process; each request gets fresh Validator | Startup bounded by ordinary service startup; request immediately fails when unavailable | Readiness down and `INVOICE_XML_VALIDATOR_UNAVAILABLE`; operator restores exact packaged resources/deploys; no network fallback | Readiness up/down and safe 503 only; liveness remains process-only |
| Feature 002 profile-evidence acquisition (deployment dependency, not a Feature 003 runtime call) | Provider v2 response is validated and either complete Fiscal Preparation commits or none | Feature 002 rules govern provider use; Feature 003 never retries/calls it | One preparation remains natural winner; new evidence is atomic with that preparation | Feature 002's existing provider/deadline contract | Old/generic rows remain unchanged and Feature 003 returns profile undetermined; approval/deployment is required for new eligible preparations | Complete persisted evidence, Feature 002 failure, or Feature 003 profile-undetermined/unsupported; no inference |
| PostgreSQL protected storage lifecycle | Committed artifact only; backup/restore copies follow platform policy | Database/platform recovery policy, never application regeneration | DB constraints preserve one row; restored state is verified by digest/length and natural replay | Deployment/operations policy, outside request deadline | Platform Operations owns encrypted backup, restore, retention and disposal evidence; no app-level deletion/compensation | Artifact retained/restored/disposed with Invoice policy; release blocked without evidence |

There is no database/external SRI, filesystem, object-storage, queue, webhook, or provider atomicity
claim in Feature 003.

## Native Compatibility Evaluation

| Risk area | Applicable? | Build evidence | Runtime evidence | Decision and consequences |
|-----------|-------------|----------------|------------------|---------------------------|
| SOAP clients | No | Architecture/dependency test must prove no SOAP client | Success/failure smoke proves zero SRI traffic | Not applicable to Feature 003; adding one requires a separate feature |
| XML generation and schema validation | Yes | Mandatory JVM `quarkusBuild`; future native build must include `sri/invoice/1.1.0/**` and compile unchanged schema graph | Packaged JVM valid/invalid/hash tests mandatory; future native must pass identical HTTP/adapter/security fixtures | JVM supported; native deferred/unclaimed until actual evidence |
| XML digital signatures | No | Static test rejects signature dependencies/code and generated signature namespace | Fixture inspection proves no `ds:Signature` | Not applicable; W3C schema dependency does not authorize signing |
| PKCS#12 certificate handling | No | Static dependency/import/resource checks | Zero certificate-access probes in all scenarios | Not applicable and excluded |
| Cryptographic providers | Yes, JDK SHA-256 digest only | JVM build plus known digest vectors; future native build must retain provider | Exact bytes independently hash to stored/returned digest in JVM; repeat in future native | JVM supported; native digest claim deferred with overall native status |
| Reflection and resource loading | Yes | JVM package verifies all four resource hashes/catalog; future native resource-inclusion config/build | JVM strict offline resolution and readiness tests; future native must pass same missing/mutated/valid/invalid cases | JVM supported; native deferred because classpath/catalog behavior is unproven |

## Project Structure

### Documentation (this feature)

```text
specs/003-generate-sri-invoice-xml/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── checklists/requirements.md
└── contracts/
    ├── unsigned-sri-invoice-xml-api.openapi.yaml
    ├── authoritative-fiscal-context-v2.openapi.yaml
    └── sri/invoice/1.1.0/
        ├── README.md
        ├── catalog.xml
        ├── factura_V1.1.0.xsd
        ├── xmldsig-core-schema.xsd
        ├── XMLSchema.dtd
        └── datatypes.dtd
```

`tasks.md` is intentionally absent from this planning command and belongs to a later
`$speckit-tasks` invocation.

### Source Code (repository root)

```text
src/main/java/com/alexastudillo/taxdocument/
├── api/invoicexml/
│   ├── InvoiceXmlResource.java
│   ├── InvoiceXmlRequestBoundary.java
│   ├── InvoiceXmlRequestDeadlineHandler.java
│   ├── InvoiceXmlApiMapper.java
│   ├── InvoiceXmlExceptionMapper.java
│   ├── UnsignedSriInvoiceXmlArtifactResponse.java
│   └── telemetry/
├── application/invoicexml/
│   ├── GenerateUnsignedSriInvoiceXmlCommand.java
│   ├── GenerateUnsignedSriInvoiceXmlResult.java
│   ├── GenerateUnsignedSriInvoiceXmlUseCase.java
│   ├── GenerateUnsignedSriInvoiceXmlService.java
│   ├── InvoiceXmlGenerationSource.java
│   ├── InvoiceXmlGenerationSourceStore.java
│   ├── InvoiceXmlArtifactStore.java
│   ├── SriInvoiceXmlGeneratorPort.java
│   ├── SriInvoiceSchemaValidatorPort.java
│   ├── InvoiceXmlContentEncoderPort.java
│   └── InvoiceXmlFailure.java
├── application/fiscalpreparation/       # evolved v2 provider/profile validation and mapping
├── domain/fiscalpreparation/
│   ├── StandardInvoiceXmlProfileEvidence.java
│   ├── SpecializedInvoiceProfileTrigger.java
│   └── ProfileTriggerAssessment.java
├── domain/invoicexml/
│   ├── UnsignedSriInvoiceXmlArtifact.java
│   ├── ValidatedUnsignedSriInvoiceXml.java
│   └── XmlIntegrityEvidence.java
└── infrastructure/invoicexml/
    ├── StaxSriInvoiceXmlAdapter.java
    ├── JaxpSriInvoiceXsdValidator.java
    ├── JdkInvoiceXmlContentEncoder.java
    ├── InvoiceXmlWorkerExecutor.java
    ├── InvoiceXmlArtifactRepositoryAdapter.java
    ├── InvoiceXmlPersistenceMapper.java
    ├── UnsignedSriInvoiceXmlArtifactEntity.java
    ├── InvoiceXmlCommitReconciler.java
    └── PostgreSqlInvoiceXmlCommitOutcomeClassifier.java

src/main/java/com/alexastudillo/taxdocument/infrastructure/fiscalpreparation/
└── ...                                  # evolved v2 DTO/entity/persistence mapping; no Feature 003 provider call

src/main/resources/
├── META-INF/openapi.yaml
├── application.properties
├── db/migration/V7__create_unsigned_sri_invoice_xml_artifact.sql
└── sri/invoice/1.1.0/
    ├── catalog.xml
    ├── factura_V1.1.0.xsd
    ├── xmldsig-core-schema.xsd
    ├── XMLSchema.dtd
    └── datatypes.dtd

src/test/java/com/alexastudillo/taxdocument/
├── api/invoicexml/
├── application/invoicexml/
├── domain/invoicexml/
├── domain/fiscalpreparation/
├── infrastructure/invoicexml/
├── architecture/
└── support/invoicexml/

src/integrationTest/java/com/alexastudillo/taxdocument/runtime/invoicexml/
├── InvoiceXmlJvmSmokeIT.java
└── InvoiceXmlJvmPerformanceIT.java
```

**Structure Decision**: `invoicexml` names the bounded artifact capability without implying
issuance. API owns HTTP validation/status/Base64 DTO mapping. Application owns replay precedence,
profile/source decisions, deadline, and independent XML/validator/encoder/store ports. Domain owns
framework-free immutable profile, content, integrity, and artifact invariants. Infrastructure alone
owns exact official SRI tags, StAX/JAXP/catalog, worker mechanics, Panache/PostgreSQL, locks,
constraints, and reconciliation. Profile evidence remains in `domain/fiscalpreparation` because it
is committed evidence owned by that aggregate, not an XML-generated fact. Shared existing Company,
correlation, deadline, clock, persistence-budget, Problem Details, and commit-knowledge patterns are
reused narrowly; the current date-capturing Feature 002 boundary is not reused. No generic
repository hierarchy, XML framework, schema registry, cache, or shared domain kernel is added.

Existing guards are narrowed rather than removed: `CleanArchitectureTest` allows official XML only
inside `invoicexml` infrastructure; `FiscalPreparationBoundaryTest` still forbids generation there
while permitting the evidence value; Feature 002 OpenAPI absence checks allow exactly the approved
new route; service readiness advances from Flyway V6 to V7.

## Test and Operational Evidence Plan

| Requirement/risk | Test level and environment | Planned path | Observable behavior or invariant | Failure/boundary cases |
|------------------|----------------------------|--------------|----------------------------------|------------------------|
| FR-024–030 / SC-009 profile evidence | Pure domain/JUnit and proposed-contract parser | `domain/fiscalpreparation/StandardInvoiceXmlProfileEvidenceTest.java`; `api/invoicexml/InvoiceXmlOpenApiContractTest.java` | Exact profile/set/rule and all 14 explicit assessments; all-no applies eligible | legacy/all-null, partial, wrong set/rule, missing, indeterminate, each APPLIES, Popular Business |
| FR-069–073 integrity/immutability | Pure domain/JUnit | `domain/invoicexml/UnsignedSriInvoiceXmlArtifactTest.java`; `XmlIntegrityEvidenceTest.java` | defensive copies; fixed schema/algorithm; exact lowercase digest/length/source identity | null/nil IDs, invalid digest/length, content mutation, lifecycle/status absence |
| FR-031–055 mapping | Adapter/JUnit with synthetic persisted sources | `infrastructure/invoicexml/StaxSriInvoiceXmlAdapterTest.java` | Exact declaration/root/order/cardinality; every fiscal/commercial value; optional designation mapping | each optional absent/present, Large Contributor 14/15 boundary, unsupported section/signature absence |
| FR-056–063 deterministic bytes | Adapter/golden vectors | same adapter test plus `src/test/resources/invoicexml/` | exact compact bytes, Feature 001 ordering, two/six decimal boundaries, XML escaping round-trip | locale/timezone/row-order changes, exponent, rounding, invalid code point/surrogate, double escape, trailing newline |
| FR-064–068 schema provenance/security | JAXP adapter/JUnit | `infrastructure/invoicexml/JaxpSriInvoiceXsdValidatorTest.java` | exact four hashes/lengths; strict offline compilation; valid passes/invalid fails; no payload leak | missing/mutated resource/import/DTD/catalog, XXE/SSRF/file attempts, shared-validator race, sanitized SAX error |
| FR-014–023 first generation/replay | Application/JUnit with port spies/fixed clock | `application/invoicexml/GenerateUnsignedSriInvoiceXmlUseCaseTest.java` | source-only first build; replay relationship check first; zero provider/catalog/date/XML/XSD/write on replay; one winner persistence clock | missing draft/preparation, inconsistent link/key, external fakes throwing, source invalid, clock-call counts |
| FR-001–013 / FR-076–081 API | Quarkus API/REST Assured | `api/invoicexml/InvoiceXmlResourceTest.java`; `InvoiceXmlExceptionMapperTest.java`; `InvoiceXmlRequestDeadlineHandlerTest.java` | exact header/path/no-body contract, 201/200, equal representation values and byte-identical decoded XML, canonical Base64, no-store, safe Problem Details/correlation, and every stable code/status mapping | missing/repeated/comma/blank/malformed/nil Company, path nil, body/query/Idempotency-Key, deadline race, every error mapping |
| API source of truth | Semantic OpenAPI test | `api/invoicexml/InvoiceXmlOpenApiContractTest.java` and existing Feature 001/002 regressions | standalone contract merges into `META-INF/openapi.yaml` without changing prior routes; explicit `security: []` is preserved while security schemes and non-empty requirements, Company input, and replay flags remain absent | Authorization, 401/403, security scheme or non-empty security requirement, raw XML media type, status field, response Company, missing errors/headers |
| V7 profile/source/artifact invariants | Empty/V6-upgrade PostgreSQL 18.4 | `infrastructure/invoicexml/InvoiceXmlArtifactMigrationTest.java` | V1–V6 checksums unchanged; profile all-null/all-complete; prepared-source guards; exact table/FKs/uniques/hash/length/append-only | partial/bad enum, source insert/update/delete, cross-Company/preparation FK, duplicate draft/prep, digest/content/length mismatch, >2 MiB, update/delete artifact |
| Repository mapping and replay | PostgreSQL 18.4 reactive integration | `infrastructure/invoicexml/InvoiceXmlArtifactRepositoryAdapterTest.java` | complete deterministic hydration, exact `bytea` round trip, Company predicates, draft-first lock and committed result | physical row reorder, corrupt relationship, other Company, timeouts, unavailable DB, defensive-copy boundary |
| FR-074 / SC-004 concurrency | PostgreSQL 18.4 + bounded executor | `infrastructure/invoicexml/InvoiceXmlArtifactConcurrencyTest.java` | 100 equivalent requests -> exactly one row/201 and 99 successful 200; identical ID/bytes/hash/time; pool/executor recover | cross-process uniqueness race, queue boundary, same draft across Company, no deadlock/blocked event loop |
| FR-075/079–080 commit uncertainty | PostgreSQL transport-fault integration | `infrastructure/invoicexml/InvoiceXmlArtifactRollbackTest.java` | confirmed rollback leaves zero; lost COMMIT ack reconciles existing or returns unknown; no second insert/generation | fault before insert/flush/commit, dropped acknowledgement, reconciliation timeout/unavailable, late worker result |
| FR-078–080 resource deadline | Quarkus/Mutiny controlled worker and database delays | request deadline test plus JVM performance test | one earliest monotonic budget; stage ceilings clamped; one terminal response; late pure result discarded | exhausted before submit/read/commit, queue delay, JAXP ignores interrupt, timeout after possible commit |
| FR-082–087 / SC-013–017 privacy/exclusions | API/telemetry/static architecture | `infrastructure/invoicexml/SensitiveInvoiceXmlDataExposureTest.java`; `architecture/InvoiceXmlBoundaryTest.java` | no sensitive response outside success or operational signal; no excluded dependency/route/entity/side effect; sources unchanged | parser/SQL exceptions, all sensitive sentinel values, digest labels, signature/cert/SRI/PDF/queue/event/security/cache imports |
| Readiness/liveness | Quarkus health + PG18.4 | existing `ServiceHealthTest.java` plus validator tests | liveness process-only; readiness same PostgreSQL/Flyway V7 and local exact schema; no Company/provider/SRI/current-catalog probe | DB down, migration missing, schema mismatch, official sites offline, validator unavailable |
| SC-001–006 JVM runtime | Packaged JVM + PG18.4 | `runtime/invoicexml/InvoiceXmlJvmSmokeIT.java` | first/replay exact API, independent decode/hash/XSD verification, resource hashes, zero replay calls, persisted round trip | valid/invalid fixture, provider/SRI sites unavailable, restart/replay, no event-loop execution |
| SC-004 performance/resources | Warmed packaged JVM + PG18.4 | `runtime/invoicexml/InvoiceXmlJvmPerformanceIT.java` | max bounded source and 100 contention/replay finish within 10s; one winner; no blocked-thread signal; pool/executor recovery | queue saturation, DB lock/statement limits, 2-MiB guard, late results, heap/resource observation |
| Native evaluation | Optional future Mandrel/GraalVM 25 build/runtime | native profile of the two runtime tests | complete resource graph inclusion and identical secure XML/API behavior | missing resource/reflection/catalog/provider differences; JVM remains accepted and native unclaimed |

**Liveness and Readiness**: Liveness remains process/event-loop viability only. Readiness uses the
same configured PostgreSQL destination as business persistence, confirms Flyway V7/catalog rows,
and incorporates the local schema descriptor's verified/compiled availability. It never contacts
SRI, W3C, Fiscal Context provider, Company, current catalogs, or a representative business row.
Schema failure makes readiness down and the endpoint fail closed; other already-committed replay
data remains unchanged.

**Structured Observability**: Preserve/generate one safe correlation ID. Record bounded outcome
code, created/replay classification, schema identifier, duration/stage, worker rejection/timeout,
commit-knowledge class, safe opaque artifact/draft/preparation IDs, and non-sensitive counts only.
No Company UUID, digest, XML, fiscal identifier/value, parser detail, SQL, path, catalog URL, or
endpoint becomes a log field, metric label, trace attribute, or health datum.

**Audit Events**: Emit sanitized structured operational audit signals for artifact committed,
replay observed, confirmed rollback, and unknown commit outcome, containing only safe correlation,
opaque local IDs, schema identifier, outcome, and technical instant. This is not event publication,
a queue, a new audit table, or fiscal payload storage. There are no certificate/signature/SRI
events.

**External Destination Consistency**: Flyway/JDBC, Hibernate Reactive, readiness, and tests resolve
the same PostgreSQL database; JDBC remains migration-only. The schema catalog resolves only the
same packaged resources used by validation and has no remote destination. Feature 003 has no
runtime external business adapter or health destination.

## Complexity Tracking

| Addition | Requirement creating the need | Simpler alternatives considered | Why insufficient | Testing and operational consequences |
|----------|-------------------------------|--------------------------------|------------------|--------------------------------------|
| Exact four-file SRI/W3C schema closure plus local catalog | FR-064–068 | SRI XSD alone; runtime fetch; edit import/DOCTYPE | SRI archive omits a required import; fetching is nondeterministic; editing is not exact-XSD validation | Pin hashes/lengths/notices, strict offline/XXE tests, readiness signal, native resource risk |
| Independent generator and validator application ports | FR-064/066/088; Constitution XI | Call StAX/JAXP from use case; one combined port | Official names/technology must remain infrastructure-only and capabilities independently replaceable; combined port obscures exact-byte validation boundary | Port fakes/application ordering plus adapter contract/security tests |
| Narrow Base64 content-encoder port | FR-076 and 2-MiB response | Encode on event loop; persist Base64 | Event-loop CPU is prohibited; persisted Base64 duplicates sensitive state and creates mismatch risk | Same bounded executor, canonical decode/hash and replay-load tests |
| Named bounded worker executor and direct context-propagation extension | FR-088; Constitution V | Event loop, `@Blocking` endpoint, common pool, virtual threads | XML/JAXP/Base64 are CPU-bound; whole-endpoint blocking breaks reactive-session context; unbounded pools lack capacity control | max-four/queue-100 configuration, rejection/timeout/late-result/100-load tests, thread-context assertions |
| New complete Company-scoped source/artifact store | FR-014–023 | Reuse private idempotency-coupled Draft loader; query current catalogs | Existing port cannot load preparation/artifact and does not guarantee required order; catalogs/current values are forbidden | Hydration/order/Company/corruption tests and explicit detached mapping |
| Proposed provider contract v2 and sixteen flattened preparation columns | FR-024–029 | Generic boolean; JSON/map; infer/backfill | Cannot prove exhaustive trigger absence or DB completeness; historical rows lack evidence | Owner approval/deployment gate, provider/Feature 002 regression tests, all-null/all-complete migration constraints |
| Prepared-source mutation guards in V7 | FR-022–023 and exact artifact/source identity | Repository convention only; source fingerprint after mutation | Direct writes could change prepared commercial values after Fiscal Preparation; a fingerprint detects but does not prevent contract violation | Direct insert/update/delete tests across all six source tables; no new update API |
| Append-only artifact table with `bytea`, database SHA-256, composite source FKs | FR-069–075 | Filesystem/object store; text/Base64; digest/path only | External storage creates distributed commit; text/encoded copies weaken exact bytes; digest alone cannot replay/sign later | Empty/upgrade migration, direct constraints, backup/retention, exact round trip, one-winner concurrency |
| Commit tracker/classifier/reconciler for artifact | FR-075/079–080 | Treat every exception as rollback; blind retry/delete | Lost acknowledgement can hide a committed immutable winner | PostgreSQL transport-fault and natural replay tests; safe unknown outcome; no compensation |

There is no new XML library, background process, cache, broker, distributed lock, filesystem/object
store, schema registry, Company/Issuer master store, signature store, caller idempotency table, or
runtime external interaction. Sharing one compiled immutable `Schema` is trusted static
configuration, not an application cache.

Constitution deviations: None.

| Deviated principle and rule | Scope | Justification | Approval record | Expiration or remediation condition |
|-----------------------------|-------|---------------|-----------------|-------------------------------------|
| None | None | All pre-research and post-design gates pass without deviation | Not applicable | Not applicable |
