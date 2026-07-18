# Contract and Validation Requirements Checklist: Create Invoice Draft

**Purpose**: Validate that validation order, canonical response representation, calculated-field
classification, and buyer-email semantics are complete, unambiguous, consistent, measurable, and
traceable before implementation continues

**Created**: 2026-07-17

**Feature**: [spec.md](../spec.md)

**Audience/Timing**: Feature owner and specification reviewer; formal pre-implementation
requirements-quality gate following the latest cross-artifact analysis

**Note**: This checklist tests the quality of written requirements and contract artifacts. It does
not test implementation behavior or task completion. An item may be marked complete only when the
cited artifacts provide objective textual evidence.

## Validation-Order Completeness

- [ ] CHK001 Does every artifact define `emissionPointId` normalization and validation as the first
  Stage-6 operation, before general business-text normalization begins? [Completeness, Spec §FR-004,
  Spec §FR-041, Plan §Authoritative Creation Sequence]
- [ ] CHK002 Is Stage 5 exhaustive about malformed JSON, unsupported representations, unknown
  properties, recognized calculated properties, and missing or non-string `emissionPointId`
  outcomes? [Completeness, Spec §FR-004/FR-041, Error Catalog §Failure Precedence]
- [ ] CHK003 Is Stage 6 exhaustive about blank-after-trim, malformed, nil, and valid canonical
  `emissionPointId` values, including the safe violation metadata for every rejected value?
  [Completeness, Spec §FR-004, Error Catalog §Emission-Point Violation]
- [ ] CHK004 Does the documented transition from Stage 5 to Stage 6 make clear that only a decoded
  string reaches Stage 6 and that no general-text rule can overtake emission-point validation?
  [Clarity, Spec §FR-041, Contract §CreateInvoiceDraftRequest]
- [ ] CHK005 Are the effects of an emission-point failure stated for idempotency lookup,
  fingerprinting, business validation, calculation, save behavior, and response selection?
  [Completeness, Spec §FR-004/FR-041, Error Catalog §Emission-Point Violation]
- [ ] CHK006 Is the exact order among all general-text operations defined only after successful
  emission-point validation, without any phrase such as “at the beginning of Stage 6” assigning
  both operations the same precedence? [Ambiguity, Plan §Executable Text-Repertoire Design]

## Calculated-Field Classification

- [ ] CHK007 Is the set of client-prohibited calculated monetary property names exhaustive at every
  request nesting level where they can occur? [Completeness, Spec §FR-012/FR-025, Error Catalog
  §Client-Supplied Calculated Fields]
- [ ] CHK008 Are recognized calculated properties distinguished objectively from all other unknown
  properties so that `PROHIBITED_CALCULATED_FIELD` and `INVALID_REQUEST` cannot depend on parser or
  mapper accident? [Clarity, Spec §FR-025/FR-041, Error Catalog §Stable Error Catalog]
- [ ] CHK009 Is the classification rule defined for a calculated property whose value has the wrong
  JSON type, is `null`, or equals the value the service would calculate? [Edge Case, Spec §SC-014,
  Error Catalog §Client-Supplied Calculated Fields]
- [ ] CHK010 Is there one deterministic priority when the same body contains both a recognized
  calculated property and a different unknown or prohibited non-calculated property? [Gap, Spec
  §FR-041 Stage 5]
- [ ] CHK011 Is there one deterministic priority when multiple recognized calculated properties
  occur at different nesting depths or collection positions? [Gap, Spec §FR-041 Stage 5]
- [ ] CHK012 Are field location, stable top-level code, safe message, correlation behavior, and
  absence-of-state semantics defined for every calculated-field rejection? [Completeness, Spec
  §FR-025, Spec §SC-014, Contract §ProblemDetails]
- [ ] CHK013 Does the plan assign one explicit transport-classification responsibility without
  allowing generic unknown-property handling to bypass the required calculated-field result?
  [Consistency, Plan §API and Error Contract, Tasks §T033/T079/T087]

## Canonical Emission-Point Contract

- [ ] CHK014 Is canonical `emissionPointId` defined everywhere as the lowercase hyphenated,
  non-nil UUID produced after the one permitted surrounding ASCII SP/HTAB trim? [Clarity, Spec
  §FR-004/FR-022, Spec §SC-022]
- [ ] CHK015 Does the success-response schema express the exact canonical lowercase-hyphenated UUID
  repertoire rather than relying only on a broad UUID format annotation? [Gap, Contract
  §InvoiceDraftResponse.emissionPointId]
- [ ] CHK016 Are request and response schema responsibilities explicitly different: the request
  accepts decoded strings that Stage 6 may trim, while the response permits only the canonical
  stored representation? [Consistency, Contract §CreateInvoiceDraftRequest.emissionPointId,
  Contract §InvoiceDraftResponse.emissionPointId]
- [ ] CHK017 Are valid examples provided for uppercase input, surrounding SP input, surrounding HTAB
  input, and already canonical input, each with its exact canonical response value? [Coverage, Spec
  §Acceptance Scenario 34, Spec §SC-022]
- [ ] CHK018 Are invalid examples provided for empty, trim-to-empty, malformed, nil, internal-space,
  internal-HTAB, braced, and non-hyphenated emission-point values with their precise Stage-5 or
  Stage-6 outcome? [Edge Case, Spec §FR-004/FR-041]
- [ ] CHK019 Is canonical emission-point preservation stated for new creation and equivalent replay,
  including the absence of re-normalization or a changed response representation? [Completeness,
  Spec §FR-022/FR-029, Spec §SC-012/SC-022]
- [ ] CHK020 Do the contract narrative, response schema, plan, and contract-evidence task all require
  the same canonical representation with no weaker artifact? [Consistency, Plan §API and Error
  Contract, Tasks §T030/T080/T089]

## Buyer-Email Semantics

- [ ] CHK021 Is “one valid email address” replaced or supplemented by one executable grammar or an
  exact named standard and profile whose version and deviations are stated? [Ambiguity, Spec
  §FR-008]
- [ ] CHK022 Does the email rule define the accepted character repertoire for local and domain
  parts, including whether non-ASCII characters are accepted? [Completeness, Spec §FR-008/FR-035]
- [ ] CHK023 Does the email rule define case preservation, comparison sensitivity, and whether any
  normalization beyond the approved general-text policy is allowed? [Completeness, Spec
  §FR-008/FR-035, Spec §DR-019]
- [ ] CHK024 Are the total 254-code-point limit and any local-part, domain-label, separator, or
  minimum-length boundaries explicit and mutually consistent? [Clarity, Spec §FR-008, Contract
  §CreateBuyer.email]
- [ ] CHK025 Are quoted local parts, consecutive dots, leading or trailing dots, domain literals,
  subdomains, trailing domain dots, comments, and multiple-address separators each assigned an
  explicit accepted or rejected result? [Edge Case, Gap]
- [ ] CHK026 Are whitespace, control, prohibited Unicode, decomposed/composed Unicode, and
  trim-to-empty interactions assigned unambiguous normalization and email-validation outcomes?
  [Edge Case, Spec §FR-035, Spec §Executable General Text Policy and Vectors]
- [ ] CHK027 Are valid and invalid email examples sufficient to exercise every accepted character
  category, every prohibited form, and the exact lower and upper boundaries? [Coverage, Spec
  §Acceptance Scenario 33, Spec §SC-016]
- [ ] CHK028 Do the specification, contract metadata, design rules, acceptance scenarios, and
  validation-vector plan all use the same email grammar and limits? [Consistency, Spec §FR-008,
  Contract §CreateBuyer.email, Plan §Unicode Validation Fixture]

## Acceptance, Precedence, and Traceability

- [ ] CHK029 Is there an acceptance scenario for each pairwise competition between a Stage-5
  calculated/unknown-property failure and a Stage-6 emission-point or general-text failure?
  [Coverage, Spec §FR-041, Spec §SC-026]
- [ ] CHK030 Is there an acceptance scenario showing how deadline expiry competes with each newly
  clarified Stage-5 and Stage-6 result while still producing exactly one terminal response?
  [Coverage, Spec §FR-041–FR-043, Spec §SC-026–SC-028]
- [ ] CHK031 Are SC-014, SC-016, SC-022, and SC-026 measurable against explicit input/output vectors
  rather than depending on undefined notions such as “recognized,” “valid email,” or “canonical
  UUID”? [Measurability, Spec §Measurable Outcomes]
- [ ] CHK032 Does every clarified obligation map to an acceptance scenario, contract element,
  planning responsibility, and pending task without assigning business normalization to the API?
  [Traceability, Spec §FR-004/FR-008/FR-025/FR-041, Tasks §T029/T030/T033/T054/T063/T079/T087]
- [ ] CHK033 Are the four latest analysis findings—Stage-6 invocation order, response UUID
  canonicality, calculated-field classification, and exact email semantics—each closed by
  normative text rather than commentary or implementation assumption? [Gap, Cross-Artifact
  Analysis I1/I2/U1/A1]
- [ ] CHK034 After reconciliation, are there no conflicting or weaker statements in `spec.md`,
  `plan.md`, `tasks.md`, `error-catalog.md`, or the OpenAPI contract for these four topics?
  [Consistency, Conflict]

## Notes

- `[x]` means the cited requirements and contract artifacts objectively satisfy the item; it does
  not mean an implementation or task was tested.
- Record unresolved evidence below the affected item and link it to the exact artifact section.
- Do not mark CHK033 or CHK034 complete until a new cross-artifact review finds no active issue in
  the four scoped areas.
