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

- [x] CHK001 Does every artifact define `emissionPointId` normalization and validation as the first
  Stage-6 operation, before general business-text normalization begins? [Completeness, Spec §FR-004,
  Spec §FR-041, Plan §Authoritative Creation Sequence]
- [x] CHK002 Is Stage 5 exhaustive about malformed JSON, unsupported representations, unknown
  properties, recognized calculated properties, and missing or non-string `emissionPointId`
  outcomes? [Completeness, Spec §FR-004/FR-041, Error Catalog §Failure Precedence]
- [x] CHK003 Is Stage 6 exhaustive about blank-after-trim, malformed, nil, and valid canonical
  `emissionPointId` values, including the safe violation metadata for every rejected value?
  [Completeness, Spec §FR-004, Error Catalog §Emission-Point Violation]
- [x] CHK004 Does the documented transition from Stage 5 to Stage 6 make clear that only a decoded
  string reaches Stage 6 and that no general-text rule can overtake emission-point validation?
  [Clarity, Spec §FR-041, Contract §CreateInvoiceDraftRequest]
- [x] CHK005 Are the effects of an emission-point failure stated for idempotency lookup,
  fingerprinting, business validation, calculation, save behavior, and response selection?
  [Completeness, Spec §FR-004/FR-041, Error Catalog §Emission-Point Violation]
- [x] CHK006 Is the exact order among all general-text operations defined only after successful
  emission-point validation, without any phrase such as “at the beginning of Stage 6” assigning
  both operations the same precedence? [Ambiguity, Plan §Executable Text-Repertoire Design]

## Calculated-Field Classification

- [x] CHK007 Is the set of client-prohibited calculated monetary property names exhaustive at every
  request nesting level where they can occur? [Completeness, Spec §FR-012/FR-025, Error Catalog
  §Client-Supplied Calculated Fields]
- [x] CHK008 Are recognized calculated properties distinguished objectively from all other unknown
  properties so that `PROHIBITED_CALCULATED_FIELD` and `INVALID_REQUEST` cannot depend on parser or
  mapper accident? [Clarity, Spec §FR-025/FR-041, Error Catalog §Stable Error Catalog]
- [x] CHK009 Is the classification rule defined for a calculated property whose value has the wrong
  JSON type, is `null`, or equals the value the service would calculate? [Edge Case, Spec §SC-014,
  Error Catalog §Client-Supplied Calculated Fields]
- [x] CHK010 Is there one deterministic priority when the same body contains both a recognized
  calculated property and a different unknown or prohibited non-calculated property? [Gap, Spec
  §FR-041 Stage 5]
- [x] CHK011 Is there one deterministic priority when multiple recognized calculated properties
  occur at different nesting depths or collection positions? [Gap, Spec §FR-041 Stage 5]
- [x] CHK012 Are field location, stable top-level code, safe message, correlation behavior, and
  absence-of-state semantics defined for every calculated-field rejection? [Completeness, Spec
  §FR-025, Spec §SC-014, Contract §ProblemDetails]
- [x] CHK013 Does the plan assign one explicit transport-classification responsibility without
  allowing generic unknown-property handling to bypass the required calculated-field result?
  [Consistency, Plan §API and Error Contract, Tasks §T033/T079/T087]

## Canonical Emission-Point Contract

- [x] CHK014 Is canonical `emissionPointId` defined everywhere as the lowercase hyphenated,
  non-nil UUID produced after the one permitted surrounding ASCII SP/HTAB trim? [Clarity, Spec
  §FR-004/FR-022, Spec §SC-022]
- [x] CHK015 Does the success-response schema express the exact canonical lowercase-hyphenated UUID
  repertoire rather than relying only on a broad UUID format annotation? [Gap, Contract
  §InvoiceDraftResponse.emissionPointId]
- [x] CHK016 Are request and response schema responsibilities explicitly different: the request
  accepts decoded strings that Stage 6 may trim, while the response permits only the canonical
  stored representation? [Consistency, Contract §CreateInvoiceDraftRequest.emissionPointId,
  Contract §InvoiceDraftResponse.emissionPointId]
- [x] CHK017 Are valid examples provided for uppercase input, surrounding SP input, surrounding HTAB
  input, and already canonical input, each with its exact canonical response value? [Coverage, Spec
  §Acceptance Scenario 34, Spec §SC-022]
- [x] CHK018 Are invalid examples provided for empty, trim-to-empty, malformed, nil, internal-space,
  internal-HTAB, braced, and non-hyphenated emission-point values with their precise Stage-5 or
  Stage-6 outcome? [Edge Case, Spec §FR-004/FR-041]
- [x] CHK019 Is canonical emission-point preservation stated for new creation and equivalent replay,
  including the absence of re-normalization or a changed response representation? [Completeness,
  Spec §FR-022/FR-029, Spec §SC-012/SC-022]
- [x] CHK020 Do the contract narrative, response schema, plan, and contract-evidence task all require
  the same canonical representation with no weaker artifact? [Consistency, Plan §API and Error
  Contract, Tasks §T030/T080/T089]

## Buyer-Email Semantics

- [x] CHK021 Is “one valid email address” replaced or supplemented by one executable grammar or an
  exact named standard and profile whose version and deviations are stated? [Ambiguity, Spec
  §FR-008]
- [x] CHK022 Does the email rule define the accepted character repertoire for local and domain
  parts, including whether non-ASCII characters are accepted? [Completeness, Spec §FR-008/FR-035]
- [x] CHK023 Does the email rule define case preservation, comparison sensitivity, and whether any
  normalization beyond the approved general-text policy is allowed? [Completeness, Spec
  §FR-008/FR-035, Spec §DR-019]
- [x] CHK024 Are the total 254-code-point limit and any local-part, domain-label, separator, or
  minimum-length boundaries explicit and mutually consistent? [Clarity, Spec §FR-008, Contract
  §CreateBuyer.email]
- [x] CHK025 Are quoted local parts, consecutive dots, leading or trailing dots, domain literals,
  subdomains, trailing domain dots, comments, and multiple-address separators each assigned an
  explicit accepted or rejected result? [Edge Case, Gap]
- [x] CHK026 Are whitespace, control, prohibited Unicode, decomposed/composed Unicode, and
  trim-to-empty interactions assigned unambiguous normalization and email-validation outcomes?
  [Edge Case, Spec §FR-035, Spec §Executable General Text Policy and Vectors]
- [x] CHK027 Are valid and invalid email examples sufficient to exercise every accepted character
  category, every prohibited form, and the exact lower and upper boundaries? [Coverage, Spec
  §Acceptance Scenario 33, Spec §SC-016]
- [x] CHK028 Do the specification, contract metadata, design rules, acceptance scenarios, and
  validation-vector plan all use the same email grammar and limits? [Consistency, Spec §FR-008,
  Contract §CreateBuyer.email, Plan §Unicode Validation Fixture]

## Acceptance, Precedence, and Traceability

- [x] CHK029 Is there an acceptance scenario for each pairwise competition between a Stage-5
  calculated/unknown-property failure and a Stage-6 emission-point or general-text failure?
  [Coverage, Spec §FR-041, Spec §SC-026]
- [x] CHK030 Is there an acceptance scenario showing how deadline expiry competes with each newly
  clarified Stage-5 and Stage-6 result while still producing exactly one terminal response?
  [Coverage, Spec §FR-041–FR-043, Spec §SC-026–SC-028]
- [x] CHK031 Are SC-014, SC-016, SC-022, and SC-026 measurable against explicit input/output vectors
  rather than depending on undefined notions such as “recognized,” “valid email,” or “canonical
  UUID”? [Measurability, Spec §Measurable Outcomes]
- [x] CHK032 Does every clarified obligation map to an acceptance scenario, contract element,
  planning responsibility, and pending task without assigning business normalization to the API?
  [Traceability, Spec §FR-004/FR-008/FR-025/FR-041, Tasks §T029/T030/T033/T054/T063/T079/T087]
- [x] CHK033 Are the four latest analysis findings—Stage-6 invocation order, response UUID
  canonicality, calculated-field classification, and exact email semantics—each closed by
  normative text rather than commentary or implementation assumption? [Gap, Cross-Artifact
  Analysis I1/I2/U1/A1]
- [x] CHK034 After reconciliation, are there no conflicting or weaker statements in `spec.md`,
  `plan.md`, `tasks.md`, `error-catalog.md`, or the OpenAPI contract for these four topics?
  [Consistency, Conflict]

## Evaluation Evidence

| Item | Objective evidence reviewed on 2026-07-17 |
|------|-------------------------------------------|
| CHK001 | FR-041 Stage 6, plan creation sequence, OpenAPI `x-business-text-processing`, and T029/T063 all place emission-point validation before general-text normalization. |
| CHK002 | FR-041 Stage 5 and OpenAPI `x-stage-5-property-classification` enumerate malformed/unsupported/non-object, calculated, ordinary unknown/prohibited, missing, wrong-type, and emission-point representation outcomes. |
| CHK003 | The spec emission-point matrix and error catalog enumerate accepted canonicalization plus empty, SP/HTAB trim-to-empty, malformed, nil, internal-whitespace, braced, and non-hyphenated failures with safe metadata. |
| CHK004 | FR-004, the request schema, and the error catalog state that only a decoded JSON string reaches Stage 6; missing/non-string forms terminate in Stage 5. |
| CHK005 | The emission-point matrix and error catalog explicitly suppress normalization, fingerprinting, idempotency/reference lookup, business validation, calculation, and save and require zero created state. |
| CHK006 | FR-041, plan text-repertoire design, and T029 define emission-point processing first and the general-text operation order only after it succeeds. |
| CHK007 | Spec `Executable Stage-5 Request Property Classification`, error catalog, OpenAPI metadata, and T030/T033/T079 contain the same exhaustive path set and subtree rules. |
| CHK008 | Those sections define a pre-binding whole-object scan, calculated-path precedence, and ordinary `INVALID_REQUEST` only when no calculated path matches. |
| CHK009 | The normative classifier ignores supplied value/type and explicitly covers `null`, wrong type, and a value equal to the calculated result. |
| CHK010 | Acceptance scenario 28 and the validation-competition matrix select `PROHIBITED_CALCULATED_FIELD` over every ordinary unknown/prohibited Stage-5 fact. |
| CHK011 | The classifier requires one top-level outcome with omitted `violations` when multiple paths occur at any depth or position. |
| CHK012 | The error catalog fixes code/status/title/detail/instance/correlation/no-state behavior and intentionally omits calculated field locations and values. |
| CHK013 | Plan API contract ownership and T033/T079 assign classification exclusively to API; T087 only maps the already accepted terminal result. |
| CHK014 | FR-004/FR-022, the emission matrix, OpenAPI metadata, and response schema use one SP/HTAB trim and one lowercase-hyphenated non-nil UUID result. |
| CHK015 | `InvoiceDraftResponse.emissionPointId` requires the exact lowercase-hyphenated regex in addition to UUID format. |
| CHK016 | The request schema accepts decoded strings without UUID facets; Stage-6 metadata owns normalization; the response schema enforces the canonical pattern. |
| CHK017 | The spec matrix and OpenAPI `validExamples` bind already-canonical, uppercase, surrounding-SP, and surrounding-HTAB inputs to the exact same canonical response. |
| CHK018 | The spec matrix and OpenAPI `invalidExamples` bind missing/non-string to Stage 5 and every listed invalid decoded string to Stage 6. |
| CHK019 | FR-022/FR-029, acceptance scenarios 20/34, and SC-012/SC-022 require the saved canonical value on creation and unchanged replay. |
| CHK020 | Contract narrative, response regex, plan API section, and T030/T034/T080 require the identical canonical representation. |
| CHK021 | FR-008 and `Executable Buyer Email Profile` publish one exact regular expression rather than an undefined “valid email” rule. |
| CHK022 | The profile defines the full ASCII local repertoire, dot placement, DNS-label repertoire, and rejects internationalized addresses. |
| CHK023 | FR-008/FR-035 and the contract require preserved case, case-sensitive comparison, one general-text pass, and no email-specific normalization. |
| CHK024 | The profile fixes local 1–64, label 1–63, at least one domain dot, exactly one `@`, nonempty atoms, and total 1–254 boundaries. |
| CHK025 | The email vector table explicitly covers quoted forms, all local-dot positions, literals, subdomains, trailing domain dot, comments, and multiple addresses. |
| CHK026 | The table and fixture plan distinguish Stage-6 whitespace/control/Unicode rejection from decomposed-to-NFC non-ASCII Stage-10 `EMAIL_INVALID` and trim-to-empty rejection. |
| CHK027 | Explicit vectors cover the one-character minima, every permitted punctuation character, valid upper bounds, each plus-one rejection, and all prohibited forms. |
| CHK028 | FR-008, DR-019, acceptance scenario 33, OpenAPI normalized metadata, plan fixture semantics, and T020/T026/T030/T045 use the same regex, limits, case, and stage split. |
| CHK029 | The acceptance validation-competition matrix enumerates calculated and ordinary-unknown Stage-5 facts against emission-point and general-text Stage-6 failures. |
| CHK030 | Every row in that matrix states both pre-deadline and deadline-first outcomes; T034 requires controlled evidence for each clarified Stage-5/6 result plus Stage 10 and Stage 11B. |
| CHK031 | SC-014/SC-016/SC-022/SC-026 now resolve to the calculated-path set, explicit emission/email vectors, canonical patterns, and the pairwise/deadline matrix. |
| CHK032 | Scenarios 28/33/34/45/46, OpenAPI extensions, plan responsibilities, and T029/T030/T033/T034/T054/T063/T079/T087 provide complete boundary-preserving traceability. |
| CHK033 | The reconciled normative artifacts close invocation order, response UUID canonicality, calculated-field classification, and exact email semantics with executable matrices and metadata. |
| CHK034 | A new cross-artifact review of spec, plan, tasks, error catalog, and OpenAPI found one consistent rule set for all four topics and no weaker competing statement. |

## Notes

- `[x]` means the cited requirements and contract artifacts objectively satisfy the item; it does
  not mean an implementation or task was tested.
- Record unresolved evidence below the affected item and link it to the exact artifact section.
- CHK033 and CHK034 were marked complete only after the 2026-07-17 cross-artifact review recorded
  above found no active issue in the four scoped areas.
