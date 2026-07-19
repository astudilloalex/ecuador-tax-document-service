# Feature Specification: Prepare Invoice for Fiscal Issuance

**Created**: 2026-07-18

**Status**: Specification complete; ready for planning

**Input**: Prepare one existing Company-owned Invoice Draft for fiscal issuance by attaching one
immutable authoritative Fiscal Context Snapshot, allocating its next Official Sequential Number,
and generating its SRI Access Key exactly once before XML generation.

## Clarifications

### Session 2026-07-18

- Q: What date window permits the first Fiscal Preparation relative to Ecuador's civil date? → A:
  Same Ecuador date only. The Invoice Draft emission date must equal the Ecuador civil date fixed at
  initial request entry. A prior or future date is stale or ineligible, is never modified, and
  returns `EMISSION_DATE_STALE` before external fiscal-context resolution or sequence allocation.

## Scope and Evidence *(mandatory)*

### Bounded Outcome

An internal billing client prepares one existing eligible Company-owned Invoice Draft exactly once.
The completed outcome is one immutable Fiscal Preparation linked to that draft and containing the
authoritative Fiscal Context Snapshot, the next Official Sequential Number for the applicable
invoice sequence scope, one system-generated Numeric Code, and one valid 49-digit SRI Access Key.
The Invoice Draft and all of its commercial content remain unchanged.

### In Scope

- One directly observable preparation operation for one existing Invoice Draft and invoice document
  type only.
- Exactly one mandatory Company context supplied as a non-nil UUID in `X-Company-Id`.
- Company ownership enforcement for every Invoice Draft, Fiscal Preparation, and Official Sequence
  Baseline read or mutation.
- Revalidation of preparation eligibility and the unmodified Invoice Draft emission date under
  Ecuador civil-date rules.
- Read-only resolution of current, authoritative fiscal context through the approved external
  Company fiscal-context boundary.
- One minimal immutable Fiscal Context Snapshot containing only the fiscal values and source
  evidence required for invoice issuance and traceability.
- Automatic, concurrency-safe allocation from an existing controlled Official Sequence Baseline
  for the exact Issuer, Establishment, Emission Point, and invoice document-type scope.
- One system-generated eight-digit Numeric Code and one validated 49-digit SRI Access Key under the
  current official SRI technical rules.
- Natural idempotency and recovery by normalized Company Identifier plus Invoice Draft identifier.
- All-or-nothing local persistence of the Fiscal Preparation, Fiscal Context Snapshot, Official
  Sequential Number allocation, Numeric Code, Access Key, and baseline advancement.
- Stable caller-safe errors, one safe correlation identifier per outcome, and deterministic retry
  behavior.

### Exclusions and Non-Goals

- XML generation or XML schema validation.
- Certificate or PKCS#12 management.
- XML signing.
- SRI reception, authorization, consultation, or any other SRI communication.
- SRI retry, resubmission, or reconciliation.
- RIDE or PDF generation.
- Email, webhook, queue, event, notification, or other delivery.
- Manual Official Sequential Number assignment or Official Sequence Baseline administration.
- Any Invoice Draft modification, recalculation, replacement, deletion, cancellation, or status
  mutation.
- Any tax document other than Invoice.
- Cancellation, reversal, release, reassignment, or reuse of an allocated Official Sequential
  Number, Numeric Code, or Access Key.
- Legacy API, payload, database, status, asynchronous, or algorithm compatibility.
- Authentication, authorization, Keycloak, OpenID Connect, OAuth, JWT, API keys, user sessions,
  principals, roles, permissions, or user-to-Company entitlement decisions.
- Company administration, Company master-data ownership, or a local Company master-data copy.
- Issuer, Establishment, or Emission Point administration, ownership of their master data, or a
  complete local replica of the external fiscal model.
- Caller-supplied fiscal context, Official Sequential Number, Numeric Code, check digit, Access Key,
  environment, emission type, or official establishment or emission-point code.

### Authority and Evidence

| Authority | Source and exact version/date or path | Relevance to this feature |
|-----------|---------------------------------------|---------------------------|
| Applicable Ecuadorian legislation | [Regulation for Sales, Withholding, and Complementary Documents, consolidated through Executive Decree 99, Official Register 467, 2023-12-29](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/9fb49475-f058-49a1-b08a-f31bf4deb074/Reglamento_Comprobantes_Venta_RetencionYDC_29122023.pdf), especially articles 18, 19, 26, 41, 42, 49, and 50; [current SRI electronic-invoicing legal index](https://www.sri.gob.ec/facturacion-electronica), reviewed 2026-07-18 | Establishes Invoice content, Issuer responsibility, Establishment/Emission Point numbering, nine-digit sequential representation, consecutive issuance, retention, and non-reuse principles. Later transmission and cancellation rules do not expand this pre-XML feature. |
| Official SRI technical documentation | [Technical Sheet for Offline Electronic Tax Documents, v2.33, updated July 2026, modification dated 2026-07-13](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/5a547488-80f3-4966-a2a4-841f2e951986/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.33.pdf), sections 5.2-5.5 and Tables 1-4 | Governs the unique 49-digit Access Key, its exact fields and widths, invoice code `01`, environment codes `1` and `2`, normal emission code `1`, nine-digit sequential, eight-digit Numeric Code, and Modulo 11 Verification Digit. |
| Project constitution | `.specify/memory/constitution.md` v2.0.1, approved 2026-07-16 | Governs official-source precedence, Company context, external fiscal boundaries, all-or-nothing local outcomes, idempotency, sensitive data, and feature scope. |
| Approved target baseline | `specs/001-create-invoice-draft/spec.md`, created 2026-07-12 and approved through 2026-07-17 clarifications | Defines the Company-owned Invoice Draft, immutable Company Identifier, opaque `emissionPointId`, preserved commercial data and totals, Ecuador emission-date semantics, and the absence of fiscal identity at draft creation. |
| Canonical terminology | `docs/migration/terminology-mapping.md`, last verified 2026-07-18 | Governs Invoice, Invoice Draft, Company, Issuer, Establishment, Emission Point, Fiscal Context Snapshot, Access Key, and Official Sequential Number. |
| Legacy evidence | `docs/legacy/source-baseline.md` (`PROVISIONAL`, inspected 2026-07-12); `docs/legacy/as-is/05-business-rules.md` BR-003 through BR-005 and BR-014; `docs/legacy/as-is/06-validation-rules.md` VR-003 through VR-005 and VR-021 through VR-023; `docs/legacy/as-is/07-process-flows.md` DF-002 and DF-003; `docs/legacy/as-is/13-technical-debt.md` RISK-015, RISK-019, and RISK-023; `docs/legacy/as-is/14-pending-functional-validation.md` PFV-020, PFV-022, PFV-023, PFV-030, PFV-032, and PFV-033 | Historical observations and candidate failure/test vectors only. The provisional baseline and every legacy behavior remain non-authoritative. |

**Source Conflicts**:

- Legacy BR-003 allowed the caller to provide environment and emission type. This specification
  requires them to come from authoritative fiscal context or the governing SRI rule and forbids
  caller override.
- Legacy BR-004 and VR-005 allowed a caller-supplied Official Sequential Number and permitted the
  manual branch to bypass an active Emission Point. This specification prohibits manual assignment
  and requires a valid existing baseline for the exact fiscal scope.
- Legacy BR-005 generated a new random Numeric Code whenever the issuance flow ran again. This
  specification generates the Numeric Code once per Fiscal Preparation and returns the committed
  result on every equivalent retry.
- Legacy BR-014 reserved sequence state separately from later failure-prone work, and RISK-019
  observed that retries could allocate a new sequence and Access Key. This specification makes all
  local preparation state and baseline advancement one indivisible outcome.
- Legacy VR-023 checked only Access Key shape at some entry points and exposed an internal failure.
  This specification requires complete composition and Modulo 11 validation with caller-safe errors.
- Legacy PFV-022 observed that editable request fiscal data populated the invoice while local Issuer
  data served other decisions. This specification accepts neither as fiscal authority and uses an
  approved external fiscal source.
- Feature 001 cites SRI technical sheet v2.32, modified 2025-10-08. The current SRI publication is
  v2.33, modified 2026-07-13; v2.33 governs this feature. Its v2.33 change concerns transport-sector
  plate information and does not change the Access Key rules used here.

**Terminology Mapping**: `factura` → Invoice; `borrador de factura` → Invoice Draft; `empresa` →
Company; `emisor` → Issuer; `establecimiento` → Establishment; `punto de emisión` → Emission Point;
`datos fiscales utilizados` → Fiscal Context Snapshot; `secuencial` → Official Sequential Number;
`clave de acceso` → Access Key. Exact SRI codes and official field semantics remain unchanged where
required by the official technical sheet. Fiscal Preparation, Official Sequence Baseline, Numeric
Code, and Verification Digit are English canonical terms for this specification and require
terminology-map registration before planning completes.

### Pending Functional Validation

None. The Product Owner resolved first-preparation emission-date eligibility on 2026-07-18: only an
Invoice Draft whose emission date equals the Ecuador civil date fixed at request entry is eligible.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Prepare an Invoice Exactly Once (Priority: P1)

As an internal billing client, I prepare one existing eligible Company-owned Invoice Draft so that
later XML generation can use one immutable authoritative fiscal snapshot, one official sequential
identity, and one valid SRI Access Key without duplicating fiscal identity on retries.

**Why this priority**: Fiscal identity must be fixed before XML generation, and the smallest safe
outcome is one complete, immutable, recoverable preparation for one draft. Any partial or duplicate
outcome risks an incorrect invoice identity or an unusable sequential number.

**Independent Test**: Start with one eligible Company-owned Invoice Draft, a complete authoritative
fiscal context, and a valid existing Official Sequence Baseline. Prepare it and verify that exactly
one linked immutable Fiscal Preparation is returned with the expected snapshot evidence, next
nine-digit sequence, one eight-digit Numeric Code, and a valid 49-digit invoice Access Key; repeat
and race the same request and verify that every success returns that same preparation while the
Invoice Draft and all excluded systems remain unchanged.

**Acceptance Scenarios**:

1. **Given** one eligible Company-owned Invoice Draft with no fiscal identity, a complete supported
   authoritative fiscal context effective for its unchanged emission date, and a valid baseline
   whose next sequence is `000000123`, **when** the client prepares the draft with exactly one valid
   `X-Company-Id`, **then** exactly one immutable Fiscal Preparation is committed and returned with
   that snapshot, sequence `000000123`, one eight-digit Numeric Code, and one valid 49-digit Access
   Key.
2. **Given** a committed Fiscal Preparation, **when** the same Company and Invoice Draft are submitted
   again with the same or a different correlation identifier, **then** the original preparation is
   returned without fiscal-context resolution, baseline access, new timestamps, or any new fiscal
   identity.
3. **Given** no committed preparation, **when** 100 equivalent requests for the same Company and
   Invoice Draft execute concurrently, **then** exactly one preparation, one sequence, one Numeric
   Code, and one Access Key are committed and every successful request returns that same result.
4. **Given** 100 eligible drafts in the same valid sequence scope and no induced failure, **when**
   they are prepared concurrently, **then** each draft receives one distinct sequence and Access Key,
   and the allocated sequence set contains exactly the next 100 values from the baseline.
5. **Given** a missing, repeated, blank, malformed, or nil `X-Company-Id`, **when** preparation is
   requested, **then** the appropriate stable Company-context error is returned and no Company-owned
   read, external fiscal read, sequence allocation, or preparation write occurs.
6. **Given** a draft identifier owned by another Company, **when** preparation is requested under a
   different valid Company header, **then** the same safe not-found outcome used for an absent draft
   is returned and no information or state crosses Company scope.
7. **Given** an absent Invoice Draft, an Invoice Draft that is not in the preparable lifecycle state,
   or a draft with inconsistent prior fiscal identity but no valid linked preparation, **when**
   preparation is requested, **then** the stable not-found or non-preparable error applies and no
   fiscal context is obtained or sequence considered.
8. **Given** an otherwise eligible draft whose emission date is before or after the Ecuador civil
   date fixed at request entry, **when** preparation is first requested, **then**
   `EMISSION_DATE_STALE` is returned, the original date is not changed, and no external fiscal read
   or local preparation state is created.
9. **Given** a draft whose emission date equals the Ecuador civil date fixed at request entry,
   **when** processing crosses midnight in `America/Guayaquil`, **then** it remains date-eligible and
   neither the draft date nor the eligibility decision changes mid-request.
10. **Given** the approved fiscal-context source is unavailable or fails its bounded response time,
    **when** the failure becomes conclusive before the overall deadline, **then**
    `FISCAL_CONTEXT_UNAVAILABLE` is returned before any sequence is allocated.
11. **Given** the source returns missing, incomplete, unsupported, internally inconsistent,
    ineffective, ineligible, or Company/draft-inconsistent fiscal context, **when** preparation is
    requested, **then** the corresponding stable fiscal-context error is returned before sequence
    allocation and the raw source response is not exposed.
12. **Given** a source response whose Emission Point reference differs from the Invoice Draft's exact
    selected opaque reference, **when** the response is validated, **then**
    `FISCAL_CONTEXT_INCONSISTENT` is returned and no local preparation state changes.
13. **Given** no baseline exists for the exact Issuer, Establishment, Emission Point, and invoice
    document-type scope, **when** preparation is requested, **then**
    `OFFICIAL_SEQUENCE_BASELINE_MISSING` is returned and no baseline is initialized.
14. **Given** a baseline has an invalid scope, value, representation, state, or next-value relation,
    **when** preparation is requested, **then** `OFFICIAL_SEQUENCE_BASELINE_INVALID` is returned and
    neither it nor any preparation state changes.
15. **Given** a baseline has no value after `999999999`, **when** another draft is prepared, **then**
    `OFFICIAL_SEQUENCE_EXHAUSTED` is returned and no identity or preparation is created.
16. **Given** a candidate Access Key whose component, width, invoice code, environment, emission
    type, or Verification Digit fails the v2.33 rules, **when** final pre-commit validation runs,
    **then** `ACCESS_KEY_INVALID` is returned and the candidate sequence is not consumed.
17. **Given** any confirmed local persistence failure before commit or a confirmed complete
    rollback, **when** preparation fails, **then** `PERSISTENCE_FAILURE` is returned and there is no
    snapshot, preparation, sequence advancement, Numeric Code, or Access Key.
18. **Given** one preparation commits but its response is lost or fails, **when** the client retries
    with the same Company and Invoice Draft, **then** the committed preparation is returned and no
    second sequence or Access Key is generated.
19. **Given** the overall deadline expires conclusively before local commit, **when** no earlier
    result won, **then** `REQUEST_TIMEOUT` is returned and no local preparation state or sequence
    advancement remains.
20. **Given** deadline expiry or a persistence interruption leaves the commit outcome genuinely
    unknown, **when** an error can be returned, **then** `PREPARATION_OUTCOME_UNKNOWN` makes no
    zero-state claim and a same-scope retry resolves to the one committed result or safely attempts
    one new preparation if none committed.
21. **Given** a successful or failed preparation, **when** side effects are inspected, **then** there
    is no Invoice Draft mutation, XML, certificate access, signing, SRI call, RIDE/PDF, event, queue,
    notification, or local Company/Issuer/Establishment/Emission Point master-data change.
22. **Given** a caller attempts to provide fiscal context, environment, emission type, Official
    Sequential Number, Numeric Code, Verification Digit, Access Key, or another idempotency key as
    editable preparation content, **when** the request is classified, **then** it is rejected or the
    non-contract input is not accepted, and it cannot influence the outcome.
23. **Given** authoritative fiscal master data changes after a preparation commits, **when** the
    committed preparation is retrieved or retried, **then** the original snapshot, source version,
    observation evidence, sequence, Numeric Code, and Access Key remain unchanged.
24. **Given** a successful preparation, **when** the linked Invoice Draft is compared before and
    after, **then** its commercial data, calculations, totals, emission-point reference, emission
    date, Company ownership reference, and timestamps are byte-for-byte or value-for-value
    unchanged according to their established representations.

---

### Edge Cases

- A valid draft already has a preparation but is now outside the first-preparation date window or
  the fiscal source is unavailable: replay returns the committed preparation before time-sensitive
  validation or external access.
- Two Companies use the same Invoice Draft UUID, external Issuer reference, or correlation value:
  every read, mutation, natural-idempotency decision, and baseline access still uses the
  authoritative Company context and cannot cross ownership scope.
- The source changes between two concurrent observations: only the source version used by the one
  committed preparation becomes authoritative for that draft; a losing equivalent request returns
  it rather than committing another version.
- Fiscal context is structurally complete but not effective on the Invoice Draft emission date, is
  not eligible at observation time, maps to another opaque Emission Point reference, lacks invoice
  support, or contradicts itself: it fails before sequence allocation.
- The next sequence is `000000001` or `999999999`: both boundary values are representable when an
  existing valid baseline supplies them; no missing baseline is auto-initialized and nothing follows
  `999999999`.
- A generated Numeric Code begins with one or more zeroes: leading zeroes are retained so the value
  always has exactly eight digits.
- Modulo 11 produces an intermediate result of `11` or `10`: the Verification Digit is respectively
  `0` or `1`, exactly as SRI v2.33 specifies.
- The Access Key is 49 digits and its check digit is valid but a component differs from the committed
  snapshot or allocated sequence: the key is invalid and nothing commits.
- A sequence candidate is selected but Access Key validation or persistence fails: the candidate is
  not consumed unless the corresponding complete Fiscal Preparation commits.
- The draft has an orphan fiscal identity, two preparations, or a preparation whose Company/draft
  link is inconsistent: fail closed as non-preparable or persistence inconsistency; never repair,
  replace, cancel, or allocate another identity in this feature.
- An invalid correlation identifier accompanies valid Company context: it cannot change equivalence,
  Company scope, sequence selection, or the committed result and is never echoed unsafely.
- Errors, logs, traces, and metrics must not include RUC, names, addresses, raw fiscal-source
  payloads, complete Access Keys, sequence baselines, internal persistence details, or stack traces.

## Requirements *(mandatory)*

### Functional Requirements

#### Preparation and Invoice Draft Boundary

- **FR-001**: The service MUST provide one directly observable operation that prepares exactly one
  existing Invoice Draft for fiscal issuance and returns the resulting Fiscal Preparation.
- **FR-002**: The operation MUST accept no editable business content other than the identity of the
  Invoice Draft being prepared; authoritative Company context MUST come only from `X-Company-Id`,
  and fiscal identity values MUST be derived by the service.
- **FR-003**: The Invoice Draft MUST exist under the authoritative Company context and MUST remain in
  the preparable lifecycle state established by Feature 001.
- **FR-004**: A first preparation MUST require the Invoice Draft to have no Fiscal Preparation,
  Official Sequential Number, Numeric Code, Access Key, or other prior fiscal identity.
- **FR-005**: A valid existing linked Fiscal Preparation MUST be treated as an equivalent replay,
  not as a prior-identity failure.
- **FR-006**: The service MUST preserve the Invoice Draft's Company ownership reference, opaque
  Emission Point reference, emission date, buyer, lines, tax selections and amounts, payments,
  additional information, calculated totals, and timestamps without recalculation or mutation.
- **FR-007**: On first preparation, the unchanged Invoice Draft emission date MUST equal the Ecuador
  civil date fixed at initial request entry; a prior or future date MUST return
  `EMISSION_DATE_STALE` without being replaced, normalized, or advanced.
- **FR-008**: Fiscal Preparation identity, its link to the Invoice Draft, all fiscal snapshot fields,
  Official Sequential Number, Numeric Code, Access Key, source evidence, and creation timestamp MUST
  be immutable after commit.
- **FR-009**: Exactly zero or one Fiscal Preparation MAY exist for one Company and Invoice Draft; no
  operation in this feature may update, delete, cancel, reverse, or replace it.

#### Company Context and Ownership

- **FR-010**: The operation MUST require exactly one nonblank, syntactically valid, non-nil UUID in
  `X-Company-Id` and MAY normalize it only to canonical lowercase hyphenated representation.
- **FR-011**: Missing Company context MUST return `COMPANY_CONTEXT_REQUIRED`; repeated, blank,
  malformed, or nil context MUST return `COMPANY_CONTEXT_INVALID`.
- **FR-012**: The Company Identifier MUST NOT be accepted from a request body, input schema,
  resource path, query string, authentication token, or user-session context.
- **FR-013**: Every Invoice Draft, Fiscal Preparation, Fiscal Context Snapshot, and Official Sequence
  Baseline read or mutation MUST enforce the authoritative normalized Company Identifier.
- **FR-014**: A draft absent from the authoritative Company scope, including a draft that exists
  under another Company, MUST return `INVOICE_DRAFT_NOT_FOUND` without revealing cross-Company
  existence or data.
- **FR-015**: Company scoping MUST NOT be described or implemented as authentication,
  authorization, proof of entitlement, tenant security, or validation that a caller may use a
  Company.
- **FR-016**: The service MUST NOT implement Company lookup, Company status validation, local
  Company master data, or Company administration; fiscal-context validation at the approved
  external boundary is a preparation business precondition and not caller authorization.
- **FR-017**: A Company Identifier MUST appear in the preparation response only if a later approved
  contract clarification explicitly requires it; request data can never override header context.

#### Authoritative Fiscal Context and Snapshot

- **FR-018**: For a first preparation, fiscal data MUST be obtained through the approved external
  Company bounded-context fiscal capability that is authoritative for current Company-to-Issuer,
  Establishment, Emission Point, and issuance eligibility information.
- **FR-019**: The fiscal-context interaction MUST be read-only and MUST use the authoritative Company
  Identifier, the Invoice Draft's exact opaque Emission Point reference and emission date, and
  invoice document type as selection context; it MUST NOT accept editable fiscal values from the
  preparation caller.
- **FR-020**: The source MUST affirm one unambiguous Issuer, Establishment, and Emission Point
  combination associated with the supplied Company business context, matching the draft's exact
  opaque Emission Point reference, effective for the unchanged emission date, eligible at the
  observation time, and supported for invoice issuance in the returned environment.
- **FR-021**: Missing, unavailable, incomplete, invalid, unsupported, ineffective, ineligible,
  ambiguous, or internally inconsistent fiscal context MUST fail before Official Sequential Number
  allocation.
- **FR-022**: External unavailability, connection failure, or external deadline expiry that becomes
  conclusive before the overall request deadline MUST return `FISCAL_CONTEXT_UNAVAILABLE`.
- **FR-023**: A structurally or semantically invalid source result MUST return
  `FISCAL_CONTEXT_INVALID`; an unsupported environment, issuance mode, or invoice capability MUST
  return `FISCAL_CONTEXT_UNSUPPORTED`; contradictory or Company/draft-inconsistent values MUST
  return `FISCAL_CONTEXT_INCONSISTENT`.
- **FR-024**: No fiscal-context failure MUST be translated into Company authentication,
  authorization, `COMPANY_NOT_FOUND`, `COMPANY_INACTIVE`, `COMPANY_NOT_AUTHORIZED`, or
  `ISSUER_COMPANY_MISMATCH` behavior.
- **FR-025**: A successful first preparation MUST persist one Fiscal Context Snapshot containing,
  at minimum: authoritative external Issuer reference; Issuer RUC; Legal Name; optional Commercial
  Name when authoritative and applicable; registered Head Office Address; applicable accounting,
  special-taxpayer, withholding-agent, RIMPE, and large-contributor designations with their required
  resolution identifiers when applicable; authoritative external Establishment reference and
  three-digit Establishment Code; Establishment Address; the draft's exact opaque Emission Point
  reference and authoritative three-digit Emission Point Code; SRI Environment Code; invoice
  Document Type Code `01`; normal Emission Type Code `1`; and the evidence fields in FR-026.
- **FR-026**: Snapshot evidence MUST include a nonblank source authority identifier, the immutable
  source record or revision identifier used for the decision, the source effective date or interval
  applicable to the Invoice Draft emission date, and the observation time as an unambiguous instant.
- **FR-027**: Every required source field MUST retain its authoritative value and exact SRI-required
  representation; Issuer RUC MUST be 13 digits, Establishment Code and Emission Point Code MUST each
  be exactly three digits, Environment Code MUST be `1` or `2`, Document Type Code MUST be `01`, and
  Emission Type Code MUST be `1`.
- **FR-028**: Conditional fiscal designations and resolution identifiers MUST be persisted together
  when applicable and absent together when the authoritative context says they do not apply; a
  partial pair MUST be invalid.
- **FR-029**: The service MUST persist no complete Company, Issuer, Establishment, or Emission Point
  master-data model, editable administrative attributes, status history, authentication data,
  contact directory, or unrelated external fields.
- **FR-030**: After commit, later source changes, unavailability, or different observations MUST NOT
  mutate or replace the Fiscal Context Snapshot used by that Invoice Draft.
- **FR-031**: A replay of an existing preparation MUST return the persisted snapshot and MUST NOT
  call or revalidate the external fiscal source.
- **FR-032**: Raw source requests, raw responses, source credentials, and internal source errors MUST
  NOT be persisted as part of the snapshot or exposed to the caller.
- **FR-033**: External fiscal context MUST be completely validated before any Official Sequential
  Number candidate is selected.

#### Official Sequential Number

- **FR-034**: The Official Sequential Number MUST be allocated automatically from an existing
  controlled Official Sequence Baseline for the exact authoritative Issuer, Establishment,
  Emission Point, and invoice Document Type Code `01` scope.
- **FR-035**: The sequence business scope in FR-034 MUST use stable authoritative external
  references plus the official three-digit Establishment and Emission Point codes; every baseline
  read or mutation remains additionally constrained by the Company ownership rule in FR-013.
- **FR-036**: The caller MUST NOT provide, choose, reserve, replace, or override the Official
  Sequential Number or any baseline value.
- **FR-037**: A valid baseline MUST already identify one next allocatable integer in the inclusive
  range `1` through `999999999` or a definitive exhausted state and MUST be consistent with its exact
  scope and prior committed allocations.
- **FR-038**: An absent baseline MUST return `OFFICIAL_SEQUENCE_BASELINE_MISSING`; the service MUST
  NOT create or initialize a baseline or assume that its next value is `000000001`.
- **FR-039**: An invalid, ambiguous, duplicated, out-of-range, regressed, or scope-inconsistent
  baseline MUST return `OFFICIAL_SEQUENCE_BASELINE_INVALID` and fail closed.
- **FR-040**: A baseline with no next value after `999999999` MUST return
  `OFFICIAL_SEQUENCE_EXHAUSTED`; the service MUST NOT wrap, widen, reset, or reuse the sequence.
- **FR-041**: The allocated Official Sequential Number MUST be represented as exactly nine decimal
  digits with leading zeroes retained.
- **FR-042**: Concurrent allocations in one sequence scope MUST be unique and strictly advance the
  committed baseline once per committed Fiscal Preparation; a confirmed local failure MUST not
  consume a number, and an allocated number MUST never be canceled, reversed, or reused by this
  feature.

#### Numeric Code and SRI Access Key

- **FR-043**: The service MUST generate one Numeric Code automatically and exactly once for a new
  Fiscal Preparation; the caller MUST NOT provide or override it.
- **FR-044**: The Numeric Code MUST contain exactly eight decimal digits, including retained leading
  zeroes. Its service-controlled generation policy MAY be selected by the Issuer as permitted by SRI
  v2.33, but the selected policy MUST NOT change the committed value or natural replay equivalence.
- **FR-045**: The service MUST generate one Access Key automatically and exactly once for a new
  Fiscal Preparation and MUST validate it completely before commit.
- **FR-046**: The Access Key MUST contain exactly 49 decimal digits composed in this exact order:
  Invoice Draft emission date as `ddMMyyyy` (8), invoice Document Type Code `01` (2), Issuer RUC
  (13), Environment Code (1), Establishment Code (3), Emission Point Code (3), Official Sequential
  Number (9), Numeric Code (8), normal Emission Type Code `1` (1), and Verification Digit (1).
- **FR-047**: The six-digit series portion of the Access Key MUST equal the authoritative
  three-digit Establishment Code immediately followed by the authoritative three-digit Emission
  Point Code.
- **FR-048**: The Verification Digit MUST be calculated over the first 48 Access Key digits using
  SRI v2.33 Modulo 11: apply weights `2` through `7` cyclically from right to left, sum the weighted
  digits, subtract the sum modulo `11` from `11`, map result `11` to `0` and result `10` to `1`, and
  otherwise use the result digit.
- **FR-049**: Final validation MUST confirm the Access Key's exact length, decimal-only content,
  Modulo 11 digit, and equality of every parsed component to the Invoice Draft, Fiscal Context
  Snapshot, allocated Official Sequential Number, and generated Numeric Code.
- **FR-050**: Invoice Draft emission date conversion into the Access Key MUST preserve the same
  Ecuador civil date and MUST NOT apply a timezone shift, current-date replacement, or silent date
  correction.
- **FR-051**: An invalid candidate Access Key or any generation/validation inconsistency MUST return
  `ACCESS_KEY_INVALID`, expose no candidate value, and leave the sequence unconsumed.
- **FR-052**: Access Key uniqueness MUST be enforced across committed Fiscal Preparations, in
  addition to Official Sequential Number uniqueness within each exact sequence scope.
- **FR-053**: Legacy algorithms and values MAY supply candidate test vectors only after every
  component and expected Verification Digit is independently validated against SRI v2.33.

#### Natural Idempotency, Concurrency, and Atomicity

- **FR-054**: Natural idempotency identity MUST be exactly the normalized Company Identifier plus
  Invoice Draft identifier; the operation MUST NOT require, interpret, persist, or bind another
  caller-generated idempotency key.
- **FR-055**: Correlation identifiers, request timing, transport metadata, and repeated delivery MUST
  NOT affect natural equivalence or fiscal identity.
- **FR-056**: Equivalent replays MUST return the original committed Fiscal Preparation, including
  its original snapshot, evidence, sequence, Numeric Code, Access Key, and creation timestamp.
- **FR-057**: Existing-preparation lookup MUST occur after Company and correlation validation but
  before current emission-date eligibility, external fiscal-context access, or baseline access.
- **FR-058**: Concurrent equivalent first-preparation requests MUST commit at most one Fiscal
  Preparation; losing equivalent requests MUST resolve to and return the winner rather than allocate
  another identity.
- **FR-059**: A response delivery failure after commit MUST be recoverable by the same natural replay
  without new fiscal context resolution, sequence advancement, Numeric Code, or Access Key.
- **FR-060**: The Fiscal Preparation, its Invoice Draft link, Fiscal Context Snapshot, Official
  Sequential Number allocation, Numeric Code, Access Key, and corresponding baseline advancement
  MUST form one all-or-nothing local outcome.
- **FR-061**: Every confirmed pre-commit failure and every confirmed complete rollback MUST leave
  zero Fiscal Preparation, Fiscal Context Snapshot, baseline advancement, committed Numeric Code,
  committed Access Key, or partial link.
- **FR-062**: A sequence candidate MUST not be considered consumed unless the complete corresponding
  Fiscal Preparation commits.
- **FR-063**: No external call or external side effect may be represented as part of the local atomic
  outcome; the read-only fiscal-context result MUST be obtained and validated before the local
  commit attempt.
- **FR-064**: When commit outcome is genuinely unknown, the service MUST make no zero-state claim;
  it MUST return `PREPARATION_OUTCOME_UNKNOWN` when a response is possible and require recovery
  through the same natural replay.

#### Errors, Deadlines, and Caller-Safe Observability

- **FR-065**: Every failure MUST use the project's stable machine-readable error contract and one
  safe correlation identifier without exposing sensitive fiscal data, raw external content,
  internal topology, stack traces, persistence statements, or implementation details.
- **FR-066**: A non-preparable lifecycle state, orphan fiscal identity, duplicate preparation state,
  or inconsistent preparation link MUST return `INVOICE_DRAFT_NOT_PREPARABLE`, except that one valid
  linked preparation is an idempotent success under FR-005.
- **FR-067**: The stable error catalog for this feature MUST include all codes in the following
  table with the stated state guarantee.

| Error code | Required meaning | Local state guarantee |
|------------|------------------|-----------------------|
| `COMPANY_CONTEXT_REQUIRED` | The one required Company header is missing. | No Company-owned read or write. |
| `COMPANY_CONTEXT_INVALID` | Company header is repeated, blank, malformed, or nil. | No Company-owned read or write. |
| `INVALID_REQUEST` | Preparation representation contains prohibited editable business content or is structurally invalid. | No fiscal context, sequence, or preparation state. |
| `INVOICE_DRAFT_NOT_FOUND` | No draft exists in the authoritative Company scope. | No fiscal context, sequence, or preparation state. |
| `INVOICE_DRAFT_NOT_PREPARABLE` | The draft lifecycle or prior fiscal state cannot be prepared safely. | No new fiscal context, sequence, or preparation state. |
| `EMISSION_DATE_STALE` | The unchanged draft emission date is outside the approved first-preparation window. | No fiscal context, sequence, or preparation state. |
| `FISCAL_CONTEXT_UNAVAILABLE` | The approved external source could not return a conclusive result in its bounded time. | No sequence or preparation state. |
| `FISCAL_CONTEXT_INVALID` | Required fiscal context is missing, incomplete, ineffective, or invalid. | No sequence or preparation state. |
| `FISCAL_CONTEXT_UNSUPPORTED` | Returned environment, emission mode, or invoice capability is unsupported. | No sequence or preparation state. |
| `FISCAL_CONTEXT_INCONSISTENT` | Fiscal values contradict each other or the Company/draft selection context. | No sequence or preparation state. |
| `OFFICIAL_SEQUENCE_BASELINE_MISSING` | No controlled baseline exists for the exact fiscal scope. | No baseline creation or preparation state. |
| `OFFICIAL_SEQUENCE_BASELINE_INVALID` | The baseline or its exact scope is unsafe or invalid. | No baseline or preparation change. |
| `OFFICIAL_SEQUENCE_EXHAUSTED` | No nine-digit sequential remains after the last valid value. | No baseline or preparation change. |
| `ACCESS_KEY_INVALID` | Access Key generation or complete official validation failed. | No baseline advancement or preparation state. |
| `REQUEST_TIMEOUT` | The overall deadline won before an outcome became conclusive. | Zero state only when no commit began or rollback is confirmed; otherwise no zero-state claim. |
| `PERSISTENCE_FAILURE` | Local failure conclusively left no committed preparation. | No baseline advancement or preparation state. |
| `PREPARATION_OUTCOME_UNKNOWN` | Commit outcome cannot be determined safely. | No zero-state claim; natural replay is required. |

- **FR-068**: One overall 10-second deadline MUST begin at initial request entry and govern Company
  validation, draft and replay lookup, date eligibility, external fiscal-context resolution,
  baseline access, Access Key validation, local commit, and result selection.
- **FR-069**: The Ecuador civil date used for first-preparation eligibility MUST be fixed once at
  initial request entry before any later processing can cross midnight.
- **FR-070**: A result conclusively selected before overall expiry MUST win; otherwise
  `REQUEST_TIMEOUT` MUST win. A late result MUST NOT replace an already selected outcome or produce
  a second response.
- **FR-071**: A conclusive external fiscal-context failure before overall expiry MUST use the
  applicable fiscal-context error; unresolved external work at overall expiry MUST use
  `REQUEST_TIMEOUT`.
- **FR-072**: `REQUEST_TIMEOUT` MUST claim zero local state only when commit did not begin or a full
  rollback is confirmed; if a commit may have succeeded, the caller MUST be directed to natural
  replay without allocating a replacement identity.
- **FR-073**: An absent correlation identifier MUST produce a safe UUID; one valid identifier MAY be
  preserved; an invalid, unsafe, repeated, blank, or over-length value MUST never be echoed and MUST
  be replaced safely without changing preparation equivalence.
- **FR-074**: This preparation is synchronous from the caller's perspective and MUST NOT return an
  opaque job identifier, provisional Fiscal Preparation, or asynchronous status requiring later
  polling.
- **FR-075**: Every response and operational signal MUST avoid complete Access Keys, RUC, Legal Name,
  addresses, raw fiscal-source data, baseline values, and other fiscal payloads except where the
  successful preparation representation explicitly requires the data for the internal billing
  client.

#### Excluded Side Effects

- **FR-076**: The operation MUST perform zero XML generation or validation, certificate or PKCS#12
  access, signing, SRI communication, SRI retry, SRI reconciliation, RIDE/PDF generation, delivery,
  queueing, events, webhooks, email, or notifications.
- **FR-077**: The operation MUST perform zero Company, Issuer, Establishment, or Emission Point
  administration and MUST create no local master-data aggregate, complete replica, or editable
  master record for those concepts.
- **FR-078**: The operation MUST expose no manual sequence or baseline administration capability and
  MUST NOT accept a caller-generated sequence or baseline command.
- **FR-079**: The operation MUST support only Invoice with Document Type Code `01`; every other tax
  document type is outside the contract and MUST NOT allocate sequence state.
- **FR-080**: No legacy route, payload, database shape, status, authentication behavior, queue flow,
  or error representation is required or implied.
- **FR-081**: Any implementation of this feature MUST preserve the project's null-safety guarantees,
  treat every null-safety or related compilation warning as a defect, and correct the underlying
  cause before completion rather than ignore it or introduce a broad warning suppression.

### Domain Rules and Invariants

- **DR-001**: One Company-owned Invoice Draft has at most one immutable Fiscal Preparation, and one
  Fiscal Preparation belongs to exactly one Company-owned Invoice Draft.
- **DR-002**: Fiscal Context Snapshot values and source evidence are historical issuance evidence;
  once committed they never follow later external master-data changes.
- **DR-003**: Official Sequential Number, Numeric Code, and Access Key are one indivisible fiscal
  identity; none may exist as a committed partial result for this feature.
- **DR-004**: Invoice Draft emission date and its eligibility MUST use the `America/Guayaquil` civil
  date fixed at request entry and permit only exact date equality for first preparation; observation
  and creation timestamps MUST be unambiguous instants.
- **DR-005**: The Official Sequential Number range is `000000001` through `999999999`, inclusive,
  and uniqueness applies within the exact authoritative Issuer, Establishment, Emission Point, and
  invoice document-type scope.
- **DR-006**: Access Key format and validation MUST follow SRI Offline Technical Sheet v2.33,
  modified 2026-07-13, sections 5.2-5.5 and Tables 1-4.
- **DR-007**: Access Key invoice Document Type Code is `01`; normal Emission Type Code is `1`; SRI
  Environment Code is exactly `1` for testing or `2` for production as returned by authoritative
  fiscal context.
- **DR-008**: The first 48 Access Key digits are authoritative fiscal data plus generated identity;
  the 49th digit is their Modulo 11 Verification Digit and cannot be independently supplied.
- **DR-009**: No monetary value is recalculated, rounded, normalized, or changed by preparation; the
  complete commercial and calculated Invoice Draft from Feature 001 remains authoritative for later
  XML generation.
- **DR-010**: Company ownership is business partitioning metadata, not authentication or
  authorization, and is mandatory on every local owned-data operation.
- **DR-011**: Required absence and optionality MUST be modeled explicitly; no implementation may
  rely on an undocumented null value or weaken an existing non-null contract.

### Key Entities

- **Invoice Draft**: Existing Company-owned pre-issuance record from Feature 001. It supplies the
  immutable Company ownership reference, opaque Emission Point reference, Ecuador emission date,
  commercial content, calculated totals, and draft identifier; this feature never mutates it.
- **Fiscal Preparation**: The one immutable, Company-owned result linked to one Invoice Draft. Its
  natural identity is Company plus Invoice Draft, and it owns one Fiscal Context Snapshot, one
  Official Sequential Number, one Numeric Code, one Access Key, and one creation instant.
- **Fiscal Context Snapshot**: Minimal immutable evidence of the authoritative Issuer,
  Establishment, Emission Point, SRI environment, applicable fiscal designations, source revision,
  effective period, and observation instant actually used to create fiscal identity. It is not a
  complete or editable master-data replica.
- **Official Sequence Baseline**: Existing controlled Company-owned fiscal numbering state for one
  exact Issuer, Establishment, Emission Point, and invoice document-type scope. It can yield one next
  nine-digit Official Sequential Number or report a closed/exhausted state; its administration is
  outside this feature.
- **Official Sequential Number**: Immutable nine-digit number allocated automatically from the
  exact valid baseline and committed only with its corresponding Fiscal Preparation.
- **Numeric Code**: Immutable system-generated eight-digit SRI Access Key component selected under
  an Issuer-controlled generation policy and never supplied by the caller.
- **Access Key**: Immutable 49-digit SRI invoice identity composed and validated under v2.33 from
  the Invoice Draft emission date, authoritative fiscal context, allocated sequence, Numeric Code,
  normal emission type, and Modulo 11 Verification Digit.
- **Fiscal Source Evidence**: Source authority identifier, immutable revision identifier, applicable
  effective date or interval, and observation instant proving which authoritative fiscal values
  were used.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every valid first-preparation request whose result becomes conclusive within the
  10-second deadline returns exactly one complete Fiscal Preparation; no provisional or partial
  result is externally visible.
- **SC-002**: In 100% of successful preparations, exactly one immutable Fiscal Context Snapshot is
  linked to the target draft and contains every required fiscal field plus source authority,
  immutable revision, effective-date evidence, and observation instant; later source changes alter
  zero committed snapshot fields.
- **SC-003**: Across 100 concurrent successful preparations in one sequence scope, 100 distinct
  nine-digit Official Sequential Numbers and 100 distinct Access Keys are committed, covering
  exactly the next 100 baseline values with no duplicates or locally caused gaps.
- **SC-004**: Across 100 concurrent equivalent requests for one Company and Invoice Draft, exactly
  one Fiscal Preparation, one sequence, one Numeric Code, and one Access Key are committed, and all
  successful results identify that same preparation.
- **SC-005**: Every official and approved edge vector produces a 49-digit decimal Access Key whose
  date, `01` document type, 13-digit RUC, environment, three-digit Establishment, three-digit
  Emission Point, nine-digit sequence, eight-digit Numeric Code, emission type `1`, and Modulo 11
  digit are all correct; every one-component or check-digit mutation is rejected before commit.
- **SC-006**: Every missing, repeated, blank, malformed, and nil Company-header vector produces the
  specified stable error and zero owned-data access; every cross-Company draft vector returns the
  safe not-found outcome and exposes zero data from the other Company.
- **SC-007**: Equivalent replay after success, including replay after response loss, source outage,
  date-window expiry, or a changed correlation identifier, returns the original preparation in
  100% of cases and produces zero new external fiscal reads, sequence changes, Numeric Codes, Access
  Keys, or timestamps.
- **SC-008**: Every missing, unavailable, incomplete, invalid, unsupported, ineffective,
  ineligible, or inconsistent fiscal-context vector produces its stable error before sequence
  allocation, with zero local fiscal state and zero raw-source disclosure.
- **SC-009**: Every absent, invalid, and exhausted baseline vector fails closed with its specified
  stable error; none initializes at `000000001`, wraps after `999999999`, changes the baseline, or
  creates preparation state.
- **SC-010**: Every confirmed pre-commit failure and confirmed rollback vector leaves zero Fiscal
  Preparation, snapshot, baseline advancement, Numeric Code, Access Key, or partial link; every
  unknown-outcome vector makes no zero-state claim and is conclusively resolved by natural replay.
- **SC-011**: Every first-preparation emission-date boundary vector uses the one Ecuador date fixed
  at request entry: exact equality is accepted, every prior or future date returns
  `EMISSION_DATE_STALE`, and zero stale dates are silently changed or prepared.
- **SC-012**: In 100% of successful and failed preparation vectors, the Invoice Draft's commercial
  values, calculated amounts and totals, Emission Point reference, emission date, ownership
  reference, and timestamps remain unchanged.
- **SC-013**: Every successful or failed preparation performs zero XML, certificate, signing, SRI,
  PDF/RIDE, queue, event, notification, administration, and local fiscal-master-data replication
  side effects.
- **SC-014**: Every error response contains one stable machine-readable code and safe correlation
  identifier while exposing zero RUCs, names, addresses, complete Access Keys, raw external
  payloads, stack traces, persistence statements, or internal endpoints.
- **SC-015**: All acceptance scenarios are independently verifiable without XML generation,
  signing, SRI connectivity, notification delivery, draft modification, other tax document types,
  authentication, authorization, or administrative capabilities.
- **SC-016**: Approved project quality checks report zero unresolved null-safety or related
  compilation warnings for Feature 002 changes, and zero new warning suppressions substitute for
  correcting their causes.

## Assumptions and Dependencies

- **Assumption**: Feature 001 is the approved source of Invoice Draft ownership, lifecycle,
  commercial data, calculated totals, opaque Emission Point reference, and Ecuador emission date;
  preparation neither reaccepts nor recalculates them. — **Basis**:
  `specs/001-create-invoice-draft/spec.md`.
- **Assumption**: Preparation is a synchronous internal billing operation with one final result, not
  an asynchronous job. — **Basis**: The bounded outcome ends before every external fiscal issuance
  side effect and requires retry recovery by Company plus Invoice Draft.
- **Assumption**: Before reaching this internal service, an upstream component has performed any
  required platform authentication, user authorization, and trusted-header replacement; this
  service does not verify those actions. — **Basis**: Constitution v2.0.1 Principles VII and XVI.
- **Assumption**: The Numeric Code may use any documented service-controlled eight-digit policy
  selected for the Issuer because SRI v2.33 assigns that algorithm to the Issuer; sequence and Access
  Key correctness do not depend on a caller choosing it. — **Basis**: SRI v2.33 section 5.2.
- **Assumption**: Current SRI v2.33 governs Access Key generation for this feature; its 2026-07-13
  change does not alter Access Key composition. — **Basis**: v2.33 revision history and sections
  5.2-5.5.
- **Dependency**: The approved external Company fiscal-context capability must provide one
  authoritative, versioned, effective, observable fiscal result within a bounded time and must not
  require this service to own or administer Company, Issuer, Establishment, or Emission Point master
  data.
- **Dependency**: A controlled Official Sequence Baseline for the exact fiscal scope must already
  exist and be valid; its creation and administration occur outside this feature.
- **Dependency**: The IANA `America/Guayaquil` timezone definition governs Ecuador civil-date
  determination, while source observation and preparation creation times are unambiguous instants.
- **Assumption**: First preparation is eligible only on exact equality between the Invoice Draft
  emission date and the Ecuador civil date fixed at initial request entry; no backdated or future
  exception applies. — **Basis**: Product Owner clarification on 2026-07-18.
- **Dependency**: Later implementation work must retain the project's warning-free null-safety
  policy. — **Basis**: Explicit project-owner direction recorded 2026-07-18 and the existing
  warning-as-error quality baseline.
