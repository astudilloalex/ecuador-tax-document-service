# Data Model: Prepare Invoice for Fiscal Issuance

Canonical Target Domain terms follow `docs/migration/terminology-mapping.md`: Fiscal Preparation,
Fiscal Context Snapshot, Official Sequence Baseline, Official Sequential Number, Numeric Code,
Access Key, and Fiscal Source Evidence. Feature 001 correctly excluded these values from Invoice
Draft creation; this model introduces them only for Feature 002 and does not change Invoice Draft
ownership or history.

**Feature**: `002-prepare-invoice-issuance`  
**Constitution**: v2.0.1  
**Sources**: `spec.md`, `research.md`, SRI Offline Technical Sheet v2.33, and the completed
Feature 001 data model

## Modeling Boundaries

- `Fiscal Preparation` is the immutable aggregate created here. Its embedded
  `Fiscal Context Snapshot`, Official Sequential Number, Numeric Code, and Access Key are one
  indivisible committed result.
- `Invoice Draft` remains the existing Feature 001 aggregate. Feature 002 reads only its
  Company-owned identity, lifecycle, opaque Emission Point reference, and emission date and never
  writes its commercial graph, totals, or timestamps.
- `Official Sequence Baseline` is separate mutable controlled numbering state. This feature may
  advance an existing valid row only as part of a successful preparation transaction; it may not
  create, seed, repair, administer, reset, cancel, or reuse one.
- `Fiscal Context Snapshot` is a logical domain value embedded in the preparation persistence row.
  It is not an Issuer, Establishment, Emission Point, or Company master aggregate.
- All new domain/application/API packages are JSpecify null-marked. Required references exclude
  null. Legitimate absence is an `Optional` or a dedicated domain state; nullable persistence/JSON
  values exist only at mappers and are converted immediately.
- Domain types are framework-free. HTTP DTOs, Mutiny, JSON annotations, Panache models, SQL
  exceptions, and REST Client types remain outside `domain`.

## Existing Aggregate Projection: Invoice Draft Preparation View

This transport-neutral Application projection is loaded using both Company Identifier and Invoice
Draft Identifier.

| Field | Type | Required | Rule |
|-------|------|----------|------|
| `invoiceDraftId` | UUID | Yes | Non-nil existing Feature 001 identifier |
| `companyId` | existing `CompanyId` | Yes | Exact authoritative request scope |
| `emissionPointId` | UUID | Yes | Existing opaque reference; never resolved locally or changed |
| `emissionDate` | `LocalDate` | Yes | Existing Ecuador civil date; never corrected or converted through an instant |
| `status` | lifecycle value | Yes | Must be exactly the existing preparable `DRAFT` state |
| `fiscalPreparationState` | sealed state | Yes | `ABSENT`, `COMMITTED`, or `INCONSISTENT`; never an undocumented null |

The projection excludes buyer, lines, tax totals, payments, additional information, and monetary
values because this feature neither recalculates nor mutates them. The persistence query still
locks the complete root row during first-commit arbitration.

## Aggregate Root: Fiscal Preparation

Logical persistence name: `fiscal_preparation`.

| Field | Domain type | PostgreSQL type | Required | Rule |
|-------|-------------|-----------------|----------|------|
| `id` | UUID | `uuid` | Yes | Non-nil local immutable preparation identifier |
| `companyId` | `CompanyId` | `uuid` | Yes | Non-nil Company ownership partition; omitted from API response |
| `invoiceDraftId` | UUID | `uuid` | Yes | Company-consistent reference to the unchanged Invoice Draft |
| `officialSequenceBaselineId` | UUID | `uuid` | Yes | Exact controlled baseline used by this transaction |
| `emissionDate` | `LocalDate` | `date` | Yes | Exact unchanged draft date used in the Access Key |
| `fiscalContextSnapshot` | `FiscalContextSnapshot` | flattened columns below | Yes | One complete immutable snapshot, never a raw provider payload |
| `officialSequentialNumber` | `OfficialSequentialNumber` | `varchar(9)` | Yes | ASCII digits `000000001`–`999999999`; leading zeros retained |
| `numericCode` | `NumericCode` | `varchar(8)` | Yes | Eight ASCII digits, including valid `00000000` |
| `accessKey` | `AccessKey` | `varchar(49)` | Yes | Exact decimal SRI v2.33 key; every component and DV validated |
| `createdAt` | `Instant` | `timestamptz` | Yes | One immutable unambiguous instant captured in the successful local transaction |

Required aggregate constraints:

- primary key on `id` and non-nil UUID checks for all UUIDs;
- `UNIQUE (company_id, invoice_draft_id)` as the natural replay arbiter;
- composite local foreign key `(company_id, invoice_draft_id)` to
  `invoice_draft(company_id, id)`;
- Company-consistent reference to the exact baseline;
- `UNIQUE (issuer_reference, establishment_reference, emission_point_id,
  establishment_code, emission_point_code, document_type_code, official_sequential_number)`;
- global `UNIQUE (access_key)`;
- exact ASCII-digit width, fixed-code, value-range, effective-period, and optional-pair checks;
- an append-only database guard rejects update or delete of a committed preparation. Hibernate
  immutable mapping and insert/read-only repositories are defense in depth, not substitutes for
  the database guard;
- no correlation identifier, caller idempotency key, raw provider payload, credential, XML,
  certificate, SRI state, or notification state.

There is no provisional preparation row and no `PREPARING` state. Nonexistence transitions directly
to one committed immutable row at transaction commit.

## Embedded Value: Fiscal Context Snapshot

The logical snapshot fields are flattened into `fiscal_preparation` so they cannot commit
independently from fiscal identity.

### Authoritative references and SRI header facts

| Field | Domain representation | PostgreSQL representation | Required | Rule |
|-------|-----------------------|---------------------------|----------|------|
| `issuerReference` | opaque external reference | `varchar(128)` | Yes | Stable, nonblank, exact source value; no local Issuer aggregate |
| `issuerRuc` | `IssuerRuc` | `varchar(13)` | Yes | Exactly 13 ASCII digits; source equality; no invented generic checksum |
| `legalName` | validated fiscal text | `varchar(300)` | Yes | Exact authoritative Legal Name |
| `commercialName` | `Optional<FiscalText>` | nullable `varchar(300)` | No | Exact value when authoritative and applicable |
| `headOfficeAddress` | validated fiscal text | `varchar(300)` | Yes | Exact registered Head Office Address |
| `establishmentReference` | opaque external reference | `varchar(128)` | Yes | Stable, nonblank, exact source value |
| `establishmentCode` | `EstablishmentCode` | `varchar(3)` | Yes | Exactly three ASCII digits |
| `establishmentAddress` | validated fiscal text | `varchar(300)` | Yes | Exact authoritative Establishment Address |
| `emissionPointId` | UUID | `uuid` | Yes | Exactly the Invoice Draft's opaque Emission Point reference |
| `emissionPointCode` | `EmissionPointCode` | `varchar(3)` | Yes | Exactly three ASCII digits |
| `environmentCode` | enum/value | `varchar(1)` | Yes | Exactly `1` testing or `2` production |
| `documentTypeCode` | constant value | `varchar(2)` | Yes | Exactly invoice code `01` |
| `emissionTypeCode` | constant value | `varchar(1)` | Yes | Exactly normal emission `1` |

Fiscal text is copied from the validated authoritative response; it is never edited by the caller
or normalized into a different legal value. Contract length/character safety is validated before
commit. Sensitive values may appear in the explicitly contracted successful response, but never in
errors or telemetry.

### Conditional fiscal designations

| Field | Domain representation | Persistence representation | Rule |
|-------|-----------------------|----------------------------|------|
| `accountingRequired` | boolean | `boolean NOT NULL` | Exact authoritative accounting obligation; no resolution exists |
| `specialTaxpayer` | `Optional<ResolutionDesignation>` | nullable resolution `varchar(64)` | Presence means applicable; resolution is nonblank and stored with it |
| `withholdingAgent` | `Optional<ResolutionDesignation>` | nullable resolution `varchar(8)` | Presence means applicable; exact SRI numeric resolution representation, including no invented left padding |
| `rimpeClassification` | enum | `varchar(32) NOT NULL` | `NONE`, `RIMPE_CONTRIBUTOR`, or `POPULAR_BUSINESS`; no resolution is invented |
| `largeContributor` | `Optional<LargeContributorDesignation>` | paired nullable resolution/legend columns | The authoritative resolution and required legend are both present together under consumer contract v1.0.0 or both absent |

Optional objects, not independent booleans and strings, enforce applicability and pairing in the
domain. A designation for which the governing source requires a resolution identifier or paired
evidence is persisted atomically with that evidence when applicable and absent with it when not
applicable. Corresponding database checks reject any partial required pair. Accounting Required and
RIMPE Classification never acquire an invented resolution identifier. Later XML work derives only
the exact legend behavior defined by its approved schema/version; this feature records the current
authoritative classification/evidence but generates no XML.

### Fiscal Source Evidence and governing rule

| Field | Type | PostgreSQL type | Required | Rule |
|-------|------|-----------------|----------|------|
| `technicalRuleId` | string/value | `varchar(64)` | Yes | Exactly `SRI-OFFLINE-2.33` for this feature |
| `technicalRuleModifiedOn` | `LocalDate` | `date` | Yes | Exactly `2026-07-13` |
| `numericCodePolicyId` | string/value | `varchar(64)` | Yes | Exactly `SECURE_RANDOM_8_V1` initially |
| `sourceAuthority` | string/value | `varchar(128)` | Yes | Nonblank approved authority identifier |
| `sourceRevision` | string/value | `varchar(128)` | Yes | Immutable source record/revision used for the decision |
| `effectiveFrom` | `LocalDate` | `date` | Yes | Inclusive Ecuador civil-date start |
| `effectiveThrough` | `Optional<LocalDate>` | nullable `date` | No | Inclusive end; absence means open-ended |
| `observedAt` | `Instant` | `timestamptz` | Yes | Unambiguous provider observation time |

Rules:

- `effectiveThrough`, when present, is not before `effectiveFrom`.
- The unchanged Invoice Draft emission date lies inside the inclusive/open-ended interval.
- Observation time and source revision are evidence, not request equivalence inputs.
- Later provider changes never update these fields.
- The provider's eligibility/capability assertion is validated as a precondition. Mutable provider
  status history is not replicated into the snapshot.

## Value Objects and Exact Invariants

### OfficialSequenceScope

Contains Issuer reference, Establishment reference, Emission Point UUID, three-digit Establishment
Code, three-digit Emission Point Code, and Document Type Code `01`. Company is mandatory ownership
and query context but is not another numbering dimension; environment likewise is not part of the
legislation/specification's sequence scope. All string values are exact, nonblank, bounded values.

### OfficialSequentialNumber

- integer domain range `1..999999999`;
- canonical external/persistence representation is exactly nine ASCII decimal digits;
- `000000000`, signs, spaces, separators, and more than nine digits are invalid;
- parsing and formatting are locale-independent.

### NumericCode

- exactly eight ASCII digits;
- generated by policy `SECURE_RANDOM_8_V1` from uniform range `0..99,999,999`;
- leading zeros and `00000000` are valid;
- cannot be constructed from caller input.

### AccessKey

- exactly 49 ASCII digits;
- parsed component widths are `8+2+13+1+3+3+9+8+1+1`;
- component equality is checked against emission date, snapshot, sequence, and Numeric Code;
- Verification Digit is recomputed with SRI v2.33 Modulo 11;
- the value object has no setter, update, replacement, or partial state.

## Controlled Aggregate: Official Sequence Baseline

Logical persistence name: `official_sequence_baseline`.

| Field | Domain type | PostgreSQL type | Required | Rule |
|-------|-------------|-----------------|----------|------|
| `id` | UUID | `uuid` | Yes | Non-nil controlled baseline identifier |
| `companyId` | `CompanyId` | `uuid` | Yes | Company-owned numbering scope |
| `scope` | `OfficialSequenceScope` | flattened exact-scope columns | Yes | Immutable after provisioning |
| `lastAllocated` | integer value | `integer` | Yes | `0..999999999`; `0` is valid only on an explicitly provisioned row |
| `createdAt` | `Instant` | `timestamptz` | Yes | Provisioning execution instant correlated to external operational evidence; administration is outside this feature |
| `updatedAt` | `Instant` | `timestamptz` | Yes | Changes only with a successful allocation |

Required constraints:

- primary key on `id`, `UNIQUE (company_id, id)`, and non-nil UUID checks;
- one row per exact fiscal scope globally:
  `UNIQUE (issuer_reference, establishment_reference, emission_point_id,
  establishment_code, emission_point_code, document_type_code)`;
- Company remains required on the row, every query/mutation, and the preparation/baseline composite
  reference. A duplicate exact scope under another Company is not a separate sequence; it is
  invalid/ambiguous and the global unique constraint prevents it;
- fixed `01`, exact three-digit code checks, nonblank bounded references, and
  `last_allocated BETWEEN 0 AND 999999999`;
- scope fields and `createdAt` are immutable; the allocation operation may change only
  `lastAllocated` by exactly one and `updatedAt` in the same transaction that inserts its
  preparation;
- no Flyway seed row, runtime default row, upsert-on-missing behavior, reset, decrement, wrap, or
  administration route.

### Approved production provisioning responsibility

The accountable role is `Database Operations Owner`. Production rows are created outside Feature
002 through a controlled, reviewed, auditable SQL/runbook procedure. Before creation, that procedure
validates Company ownership, the exact Issuer/Establishment/Emission Point/document-type scope and
official codes, and initial `lastAllocated`. The external audit record identifies requester,
approver, execution time, exact scope, and resulting baseline identifier without exposing sensitive
values in general telemetry. The baseline identifier, immutable scope, and `createdAt` in this model
correlate the row to that evidence; Feature 002 does not replicate requester/approver administration
data into its domain model.

Production readiness for a fiscal scope requires approved provisioning evidence before its first
preparation request. Tests may create controlled fixture rows only. Feature 002 may read, lock,
validate, and increment an existing row in the same successful Fiscal Preparation transaction; it
cannot create a missing row, seed one through Flyway, upsert on missing, reset, decrement, repair,
wrap, reuse, or expose baseline administration.

Derivation:

```text
lastAllocated < 999999999  => next = lastAllocated + 1
lastAllocated = 999999999  => EXHAUSTED; no next value
missing row                => BASELINE_MISSING; never infer lastAllocated = 0
```

This non-null representation makes exhaustion explicit in the domain while avoiding a nullable
next-number sentinel. PostgreSQL sequences/identity columns are forbidden for this business number
because their allocation is not rolled back.

## Application Port Models

### FiscalContextResolution

Transport-neutral result from `FiscalContextPort`. It mirrors only the consumer contract fields,
not REST or JSON types. Application validates structure, exact selection equality, effective dates,
eligibility, supported codes, and conditional designation completeness before constructing a
domain snapshot. Adapter/provider failures are typed outcomes, not nullable results.

### FiscalPreparationCommitIntent

Contains Company/draft identity, the prevalidated immutable snapshot, the fixed request-entry date
context, and remaining deadline. It contains no proposed sequence, Numeric Code, Verification
Digit, Access Key, creation time, HTTP DTO, or persistence entity. Those winning values are selected
only after the draft and baseline locks.

### FiscalPreparationLookup

Sealed alternatives:

- `Existing(FiscalPreparation)`;
- `EligibleDraft(InvoiceDraftPreparationView)`;
- `NotFound`;
- `NotPreparable`.

No alternative contains null.

### FiscalPreparationCommitResult

Sealed alternatives:

- `Created(FiscalPreparation)`;
- `Replay(FiscalPreparation)`.

The Infrastructure adapter returns the committed domain/application representation, never a
Panache entity. Business failures and commit-knowledge failures use typed exceptions/outcomes with
the stable catalog.

## Transaction and Lock State Transitions

### First preparation

```text
ABSENT
  -> preflight Company-scoped draft read
  -> fixed-date eligibility
  -> authoritative fiscal resolution and validation
  -> begin transaction
  -> lock Company+draft
  -> recheck winner and draft invariants
  -> lock exact baseline
  -> derive next from lastAllocated
  -> generate Numeric Code and complete Access Key
  -> validate all components
  -> increment baseline + insert complete preparation
  -> flush
  -> commit
  -> COMMITTED
```

No row between `ABSENT` and `COMMITTED` is externally visible. On confirmed rollback, baseline and
preparation both return to their prior state and the same numeric candidate remains available.

### Natural replay

```text
COMMITTED -> Company+draft read -> return the same immutable preparation
```

Replay performs no date recheck, provider call, baseline lock/read, identifier generation, or
timestamp generation.

### Concurrent equivalent requests

All contenders may complete a read-only provider resolution after the same initial miss. Draft row
locking serializes their commit phase. The winner commits once; each later locker observes and
returns the winner before touching the baseline. Exactly one sequence/Numeric Code/Access Key is
created.

### Concurrent different drafts in one scope

Draft locks do not conflict. The exact baseline row serializes allocations. Successful commits
cover consecutive values in baseline-lock acquisition order. A failed transaction rolls back its
increment, allowing the next waiter to use that same candidate; local failure creates no gap.

## Persistence Failure and Outcome Knowledge

- Failure before transaction start or after confirmed rollback may make the exact zero-state claim
  required by the stable business/persistence error.
- Explicit flush is used to surface constraint failures before commit, but flush success is not
  commit success.
- After commit initiation, connection loss, cancellation, deadline, shutdown, SQLSTATE `08007` or
  `40003`, or another condition without confirmed rollback is an unknown outcome.
- Reconciliation runs only after the failed transaction ends and uses Company plus Invoice Draft.
  A complete preparation becomes replay. An inconclusive read returns
  `PREPARATION_OUTCOME_UNKNOWN`; it does not trigger another direct allocation.
- Constraint name, not exception text, distinguishes the natural-key winner from Access Key or
  scoped-sequence integrity failures.
- SQLSTATE `23502`, `23503`, `23505`, and `23514` are interpreted with the named constraint;
  `40P01`/`40001` count as conclusively aborted only after rollback is confirmed; class `08`,
  `08007`, `40003`, `57014`, and server-shutdown failures are conservatively classified by commit
  phase and reconciliation evidence.

## Immutability and Retention

- API, Application, and Infrastructure expose no update/delete method for Fiscal Preparation.
- Domain values are immutable; persistence is append-only; a PostgreSQL guard rejects direct
  update/delete attempts.
- Baseline mutation cannot update any committed preparation or snapshot.
- No cancellation, reversal, sequential reuse, or deletion capability is introduced.
- The accountable `Platform Operations Owner` owns TLS-enabled service and PostgreSQL connections,
  approved PostgreSQL encryption at rest, encrypted backup handling, successful restore evidence,
  and the approved Invoice-record retention policy applicable to this row.
- Release evidence must confirm the target-environment TLS connections, approved at-rest control,
  encrypted backup policy and successful restoration, applicable Invoice-record retention policy,
  and retention/disposal of Fiscal Preparation with its related Invoice record.
- No custom application database encryption, key management, independent purge, deletion API, or
  retention scheduler is introduced. The absence of a deletion API does not override platform
  retention/disposal.
- Raw provider requests, responses, credentials, and internal errors are excluded from both primary
  storage and backups because they are never persisted.
