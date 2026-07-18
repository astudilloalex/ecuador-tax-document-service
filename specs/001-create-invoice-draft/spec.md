# Feature Specification: Create Invoice Draft

**Created**: 2026-07-12

**Status**: Approved functional specification; implementation remains subject to the current
project quality and analysis gates.

**Input**: Create and save a complete Company-scoped electronic invoice draft for review before
any fiscal identifier allocation or SRI interaction.

## Clarifications

### Session 2026-07-12

- Q: What numeric envelope governs quantities, prices, money, and rates? → A: Quantity and unit
  price allow at most six fractional digits and range from `0` through `999999.999999`, with
  quantity strictly greater than zero. Every monetary input and result ranges from `0.00` through
  `999999999999999.99`. Rates range from `0.00` through `100.00`. Any input, intermediate,
  rounded, grouped, payment-sum, or invoice-total overflow returns `BUSINESS_VALIDATION_FAILED`
  with violation `MONETARY_RANGE_EXCEEDED` and saves no draft.
- Q: Which date and timestamps govern creation? → A: The accepted emission date is the Ecuadorian
  civil date determined once at the initial service boundary, before the request body is consumed.
  Crossing midnight later does not change it. A successful new draft returns equal UTC `createdAt`
  and `updatedAt` values belonging
  to that saved draft. Equivalent replay returns both original values unchanged. A failed
  all-or-nothing save exposes no created resource or timestamps.
- Q: What reference-data baseline is approved? → A: Identification types, IVA rules, and payment
  methods use the approved `SRI-OFFLINE-2.32-TARGET-1` baseline. Approved tax-rule and
  payment-method identifiers are stable values published to clients; the service never invents a
  replacement identifier while processing a request.
- Q: How is `X-Correlation-Id` handled? → A: Each response has a safe correlation identifier. An
  absent value produces a safe UUID, one valid value is preserved, and blank, repeated,
  over-length, or unsafe input is never echoed. When correlation validation governs, invalid input
  returns `INVALID_REQUEST` with a safe replacement. Correlation validation follows Company
  validation, precedes idempotency-key validation, and never affects idempotency.
- Q: What is the definitive Company context? → A: Every request supplies exactly one nonblank,
  syntactically valid non-nil Company UUID in `X-Company-Id`. It is normalized to canonical
  lowercase hyphenated form. The service performs no authentication, authorization, tenant, or
  Company lookup and records no Company, Issuer, establishment, or emission-point master-data
  snapshot. Company context is forbidden in the path, query, and request body. Idempotency is
  scoped by the normalized Company UUID plus key.
- Q: Which tax scope and per-line cardinality apply? → A: Each line has exactly one active IVA tax
  treatment effective on the emission date: configured percentage-rate IVA, IVA 0%, not subject
  to IVA, or exempt from IVA. The three zero-tax treatments remain distinct. Other or simultaneous
  taxes are unsupported.
- Q: Which buyer identification types are supported? → A: The versioned SRI rules effective on the
  emission date govern RUC (`04`), Ecuadorian identity card (`05`), passport (`06`), final consumer
  (`07`), and foreign identification (`08`). No unapproved checksum, legacy rule, online registry
  check, or buyer-name registry verification is performed.
- Q: May a valid draft total `0.00`? → A: Yes. It remains subject to every line and tax rule and
  requires exactly one active payment method with amount `0.00`. A positive-total draft requires
  every payment amount to be greater than `0.00`.
- Q: How do duplicate and concurrent creation requests behave? → A: Every request requires a
  caller-generated idempotency key scoped by normalized Company UUID plus key. Equivalent retries
  return the original saved draft; different content conflicts; concurrent equivalent requests
  create exactly one draft; rejected or completely reversed saves do not bind the key.
- Q: May the client supply calculated monetary fields? → A: No. Every request containing a
  service-calculated field is rejected even when the supplied value matches the calculated value.
- Q: What collection limits apply? → A: At most 500 invoice lines, 8 payments, and 15
  additional-information entries.
- Q: What text limits apply? → A: Product codes allow 25 characters; descriptions, buyer names,
  addresses, and additional-information names and values allow 300 Unicode code points; email
  allows 254; telephone allows 20; and idempotency keys allow 128. Exact normalization,
  uniqueness, whitespace, and prohibited-character rules are defined below.
- Q: What Company-to-Issuer relationship is validated? → A: None. Company and emission-point
  identifiers are opaque inputs. Fiscal relationships belong to a separately approved issuance
  workflow.
- Q: How long does an idempotency association remain valid? → A: For the lifetime of the created
  draft; it has no time-based expiration.
- Q: Which collection order affects idempotency? → A: Invoice-line order is significant. Payment
  and additional-information ordering are representation-only differences.
- Q: May a payment method appear more than once? → A: No. A payment method appears at most once
  per draft.
- Q: How does the 10-second deadline interact with validation precedence? → A: One deadline begins
  before body consumption and applies throughout processing. A result wins only when conclusively
  determined before expiry; otherwise `REQUEST_TIMEOUT` wins. Once a result is selected, a later
  signal cannot replace it. An unresolved save is reconciled by same-scope replay, and expiry after
  a response has been sent cannot produce another response.

### Session 2026-07-15

- Q: What exact contract governs `Idempotency-Key`? → A: It is a mandatory single-valued request
  header. After HTTP parsing there must be exactly one field value. Leading and trailing ASCII SP
  (`U+0020`) and HTAB (`U+0009`) are trimmed once; internal characters and case are unchanged. The
  normalized value governs idempotency. Missing, invalid, and multiple or ambiguous values use the
  three stable errors in FR-027. Repeated and comma-combined values are never accepted by choosing
  the first.
- Q: May CompanyId appear in a response? → A: Company identifiers are forbidden in request bodies
  and input schemas. Authoritative Company context comes only from `X-Company-Id`. Canonical
  `companyId` appears in the response because the approved contract requires it; request data can
  never override the header context.
- Q: Which operations are Company-scoped? → A: Every operation involving a draft or its
  idempotency association enforces the authoritative Company context and prevents cross-Company
  access. Global VAT, payment-method, identification-type, and other immutable SRI reference
  catalogs are shared and are not Company-owned.
- Q: What executable repertoire replaces “alphanumeric”? → A: `productCode` uses case-sensitive
  ASCII `^[A-Za-z0-9]{1,25}$`; passport (`06`) and foreign identification (`08`) use
  case-sensitive ASCII `^[A-Za-z0-9]{1,20}$`. Each permits one leading/trailing ASCII SP/HTAB trim
  and no other transformation.
- Q: What policy governs general human-readable text? → A: Each applicable value is normalized to
  NFC once; leading and trailing `U+0020` are trimmed; Unicode categories `Cc`, `Cf`, `Cs`, `Co`,
  and `Cn`, plus `U+2028` and `U+2029`, are rejected; only `U+0020` is accepted as whitespace in
  canonical single-line text. Internal punctuation, internal spaces, and display case are
  preserved. Length is counted in Unicode code points after normalization and trimming.
- Q: Which date governs payment-method validity? → A: The invoice `emissionDate`. A method is valid
  only when it exists, is active, `effectiveFrom <= emissionDate`, and `effectiveTo` is absent or
  `emissionDate <= effectiveTo`. Both finite boundaries are inclusive.
- Q: Which outcome wins at the deadline? → A: Exactly one response is returned. A conclusively
  determined result before expiry wins; otherwise timeout wins. A late result cannot replace an
  already returned response. HTTP statuses and the approved error envelope reflect only the
  selected outcome.
- Q: What are creation timestamp semantics? → A: On successful creation, `createdAt` and
  `updatedAt` are equal UTC timestamps recorded for that draft. Equivalent replay returns the
  original values. Failed saves produce no created resource.

### Session 2026-07-16

- Q: What is the observable text-normalization boundary? → A: Text is normalized deterministically
  according to the approved Unicode policy before business comparison, duplicate detection, and
  storage. Equivalent Unicode forms produce the same normalized result. No applicable value is
  normalized more than once, and accepted display values retain the specified punctuation, spaces,
  and case.
- Q: What becomes externally visible after creation? → A: A valid request produces one complete
  saved draft and its final response. No provisional or partially saved draft is visible. The
  response contains the final identifier, required business values, and creation timestamps.
- Q: What are replay timestamp semantics? → A: Initial `createdAt` and `updatedAt` are identical.
  Replay returns both originally recorded values unchanged and produces no new timestamps. A failed
  all-or-nothing save exposes neither value as a created resource.
- Q: What limit applies after `canonicalName` transformation? → A: After NFC normalization,
  approved trimming, internal `U+0020` collapse, and deterministic locale-independent lowercase
  canonicalization, the result may contain at most 300 Unicode code points. It is never truncated.
  Overflow returns `CANONICAL_NAME_TOO_LONG` with the original field, maximum `300`, counting unit
  `UNICODE_CODE_POINTS`, and stage `CANONICALIZATION`. Approved vectors include `U+0130` lowercase
  expansion.

### Session 2026-07-17

- Q: Which operation begins Stage 6? → A: The service first trims, validates, and canonicalizes
  `emissionPointId`. General business-text normalization begins only after that identifier succeeds;
  therefore an emission-point failure always precedes any general-text failure.
- Q: How are calculated request properties distinguished from ordinary unknown properties? → A: A
  well-formed request object is inspected against the exhaustive calculated-property paths defined
  below. Presence of any such path returns `PROHIBITED_CALCULATED_FIELD`, regardless of its value or
  JSON type, and takes precedence over ordinary unknown/prohibited properties and other Stage-5
  structural failures. Malformed JSON cannot be inspected and returns `INVALID_REQUEST`.
- Q: What response representation is used for `emissionPointId`? → A: Every successful new or replay
  response contains the same canonical lowercase-hyphenated UUID representation required for the
  saved draft.
- Q: Which buyer-email syntax is accepted? → A: After the approved general-text normalization, one
  ASCII dot-atom address with a 1–64-character local part, DNS-style domain labels, at least one
  domain dot, and no more than 254 total code points is accepted. Case is preserved and comparison
  is case-sensitive. Quoted local parts, comments, domain literals, internationalized addresses,
  whitespace, and multiple-address forms are rejected with safe violation `EMAIL_INVALID`.

## Scope and Evidence *(mandatory)*

### Bounded Outcome

An internal billing client can create one complete invoice draft scoped by the opaque Company UUID
supplied in `X-Company-Id` and immediately review its emission-point reference, buyer, lines,
taxes, payments, calculated totals, final identifier, and timestamps before fiscal issuance.

### In Scope

- Create an invoice draft through `POST /api/v1/invoice-drafts`.
- Receive Company context exclusively through exactly one `X-Company-Id` value and idempotency
  through exactly one `Idempotency-Key` value.
- Capture one opaque emission-point external identifier, buyer identity, optional contact data,
  one or more invoice lines, one IVA treatment per line, payments, and optional additional
  information.
- Calculate line amounts, discounts, tax bases, tax amounts, grouped tax totals, and invoice totals.
- Validate approved reference data, buyer identification, numeric boundaries, text rules, payment
  effectiveness, and payment reconciliation before saving.
- Save and return one complete draft with status `DRAFT`, currency USD, its final identifier,
  captured values, calculated values, and timestamps.
- Enforce authoritative Company context for every draft and idempotency operation while keeping
  immutable global SRI reference catalogs shared and outside Company ownership.
- Reject Company identifiers in request bodies, paths, and queries. Return canonical `companyId`
  only as required by the approved response contract.

### Exclusions and Non-Goals

- Updating, deleting, cancelling, or fiscally issuing an invoice draft.
- Reserving or assigning an official invoice sequential number or generating an SRI access key.
- Generating, validating, storing, or signing XML; managing certificates; generating PDF or RIDE.
- Communicating with the SRI or observing an SRI reception or authorization result.
- Sending email, webhook, queue, notification, or other integration messages.
- Creating another tax-document type or applying ICE, IRBPNR, non-IVA tax, or simultaneous taxes.
- Applying a deemed taxable base or special gratuitous-transfer treatment not defined here.
- Performing online buyer registry checks or buyer-name verification.
- Managing loyalty points or redemption.
- Owning, administering, searching, exposing, caching, or replicating Company, Issuer,
  establishment, or emission-point master data.
- Validating Company existence, state, fiscal eligibility, caller entitlement, or fiscal
  relationships among Company, Issuer, establishment, and emission point.
- Authentication, authorization, user, role, permission, tenant-authorization, gateway, or BFF
  responsibilities inside this service.
- Saving Company-context versions, observation timestamps, or fiscal master-data snapshots.
- Preserving an unapproved legacy route, payload, response, state, or behavior.
- Defining the later transition from `DRAFT` to a fiscally issued document.

### Authority and Evidence

| Authority | Approved source | Relevance |
|-----------|-----------------|-----------|
| Ecuadorian legislation | [Regulation for Sales, Withholding, and Complementary Documents, consolidated 2023-12-29](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/9fb49475-f058-49a1-b08a-f31bf4deb074/Reglamento_Comprobantes_Venta_RetencionYDC_29122023.pdf) and [later SRI amendments](https://www.sri.gob.ec/facturacion-electronica) | Establishes invoice obligations and issuer responsibility. This feature creates only an internal pre-issuance draft. |
| Official SRI technical documentation | [Electronic Tax Documents Offline Scheme Technical Sheet v2.32, updated 2025-10-08](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf) and the [SRI electronic invoicing page](https://www.sri.gob.ec/facturacion-electronica) | Governs official catalogs, invoice fields, tax representations, payments, and later issuance. |
| Project governance | Project Constitution v2.0.1 | Governs Company context, feature boundaries, official-rule authority, and all-or-nothing outcomes. |
| Approved target requirements | This specification and its clarification decisions | Governs observable `DRAFT` behavior and exclusions. |
| Legacy evidence | Approved legacy behavior, validation, process, and risk records | Supplies historical scenarios only and is not target authority. |

**Source conflicts**:

- Legacy behavior accepted tax values and totals from clients. This feature calculates and
  reconciles them.
- Legacy access behavior used RUC-based selection. This feature uses one opaque Company UUID from
  `X-Company-Id` without authentication, authorization, tenant resolution, or Company lookup.
- The legacy flow performed fiscal issuance. This feature stops before sequence allocation, access
  key, XML, signing, certificates, SRI communication, and authorization.
- `DRAFT` is an internal business state, not an official SRI state or tax-valid document.
- Official evidence supports multiple IVA rates with applicability conditions. No rate is treated
  as universal; the upstream billing workflow selects the approved rule appropriate to the
  product or operation.

**Approved reference data**:

- Baseline `SRI-OFFLINE-2.32-TARGET-1` contains five buyer-identification types, six IVA rules, and
  eight payment methods.
- Buyer-identification types use official codes `04` through `08`. Tax and payment references use
  the exact stable UUIDs published in the approved baseline.
- RUC, identity card, passport, and foreign identification use the approved `FORMAT_ONLY`
  strategies because no additional checksum rule was approved for this draft feature.
- The target adoption date is 2026-07-12 and is project catalog metadata, not an SRI legal-origin
  date.

## Scenario Timing Assumption

Unless a scenario explicitly tests deadline behavior, it assumes the described result becomes
conclusive before the request deadline. If expiry occurs first, FR-041 selects `REQUEST_TIMEOUT`.
Once a result is selected, a later event cannot replace it.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create and Review an Invoice Draft (Priority: P1)

As an internal billing client, I want to create an invoice draft under a supplied Company context,
so that buyer, commercial, tax, payment, and calculated information can be reviewed before fiscal
issuance.

**Why this priority**: It is the smallest independently valuable billing outcome and isolates later
fiscal numbering, XML, signature, and SRI risks.

**Independent Test**: Submit one valid request with the required headers and approved business
data. Exactly one complete `DRAFT` is saved and returned for review, while every excluded fiscal,
Company-master-data, identity, and external side effect remains absent.

**Acceptance Scenarios**:

1. **Given** exactly one valid non-nil Company UUID in `X-Company-Id`, one opaque emission-point
   identifier, valid buyer data, at least one valid line, and reconciling payments, **when** creation
   succeeds, **then** exactly one complete `DRAFT` is saved and returned with canonical Company UUID,
   a unique draft identifier, calculated amounts, and equal UTC `createdAt` and `updatedAt` values.
2. **Given** quantity `2`, unit price `10.00`, discount `5.00`, and the approved IVA 15% rule used
   only as a mathematical vector, **when** values are calculated, **then** gross is `20.00`, net is
   `15.00`, tax is `2.25`, and line total is `17.25`; the scenario does not declare 15% universal.
3. **Given** gross `20.00` and discount `21.00`, **when** validation reaches calculated-value rules,
   **then** the discount format is accepted before calculation, gross is calculated, discount over
   gross is rejected in the approved order, and no draft is saved.
4. **Given** otherwise valid data with no lines, **when** creation is attempted, **then** the request
   is rejected and no draft is saved.
5. **Given** calculated total `100.00` and payments totaling `90.00`, **when** creation is attempted,
   **then** payment reconciliation fails and no draft is saved.
6. **Given** `X-Company-Id` is missing or blank after one surrounding ASCII SP/HTAB trim, **when**
   creation is attempted, **then** `COMPANY_CONTEXT_REQUIRED` is returned safely with correlation
   and no draft or idempotency association is saved.
7. **Given** buyer identification fails the approved rule for its selected active type, **when**
   creation is attempted, **then** buyer validation fails and no draft is saved.
8. **Given** a valid request, **when** creation succeeds, **then** no sequence, access key, XML,
   signature, certificate operation, notification, or SRI communication occurs.
9. **Given** an inactive buyer identification type or IVA rule, or an inactive or ineffective
   payment method on `emissionDate`, **when** creation is attempted, **then** it is rejected and no
   draft is saved.
10. **Given** the request body supplies `companyId`, **when** request-body validation runs, **then**
    the property is rejected as unknown or prohibited and no draft or idempotency association is
    saved.
11. **Given** a failure occurs while saving any part of the draft or idempotency association,
    **when** the all-or-nothing save is confirmed failed, **then** a safe failure is returned and no
    partial draft, line, tax, payment, additional information, or association remains.
12. **Given** a line selects ICE, IRBPNR, another non-IVA tax, no tax treatment, or simultaneous tax
    treatments, **when** creation is attempted, **then** it is rejected and no draft is saved.
13. **Given** identification type `04`, `05`, `06`, or `08`, **when** creation is attempted, **then**
    it is accepted only when the value satisfies the approved `FORMAT_ONLY` rule; `06` and `08` use
    `^[A-Za-z0-9]{1,20}$` after one surrounding SP/HTAB trim.
14. **Given** a syntactically valid RUC or Ecuadorian identity-card value, **when** creation is
    attempted, **then** it is not rejected by an unapproved checksum or legacy algorithm.
15. **Given** type `07`, value `9999999999999`, name `CONSUMIDOR FINAL`, and calculated total at or
    below the effective SRI threshold, **when** creation is attempted, **then** final consumer is
    valid; any mismatch or total above the threshold is rejected. Type, value, and name are checked
    before calculation; the total limit is checked only after calculation.
16. **Given** an unknown, inactive, not-yet-effective, expired, or unresolved identification type,
    **when** creation is attempted, **then** it is rejected and no draft is saved.
17. **Given** valid lines, an explicit applicable tax treatment per line, total `0.00`, and exactly
    one effective payment method with amount `0.00`, **when** creation is attempted, **then** the
    zero-value draft is accepted and saved.
18. **Given** total `0.00`, **when** there is no payment, more than one payment, or a nonzero payment,
    **then** the request is rejected and no draft is saved.
19. **Given** total greater than `0.00`, **when** any payment is `0.00`, **then** the request is
    rejected and no draft is saved.
20. **Given** a successfully saved request, **when** the same Company, key, and equivalent normalized
    business content are submitted again, **then** the original draft and both original timestamps
    are returned unchanged, no new timestamps are produced, and no draft is created or modified.
21. **Given** a scoped key already associated with a successful request, **when** the same Company
    and key are submitted with different business content, **then** `IDEMPOTENCY_CONFLICT` is
    returned and no draft is created or modified.
22. **Given** concurrent equivalent requests in the same scope, **when** they are processed, **then**
    exactly one draft is saved and every successful result identifies that same draft.
23. **Given** header validation, business validation, or a confirmed all-or-nothing save fails,
    **when** the key is retried, **then** the prior failure has not associated the key with a draft.
24. **Given** a draft was saved but its response was not delivered, **when** the same Company, key,
    and equivalent content are retried, **then** the existing draft is returned without repeating
    creation or contacting a Company capability.
25. **Given** the same key is used with two Company UUIDs, **when** both requests are otherwise valid,
    **then** their scopes are independent and neither Company deduplicates against the other.
26. **Given** `emissionDate` equals the Ecuadorian civil date fixed when the request first reached
    the service, before its body was consumed, **when** all other data is valid, **then** the date is
    accepted.
27. **Given** `emissionDate` differs from that fixed Ecuadorian civil date, **when** creation is
    attempted, **then** it is rejected and no draft is saved.
28. **Given** a well-formed request contains any exhaustive recognized calculated-property path,
    including one whose value is null, incorrectly typed, or equal to the service result, **when**
    creation is attempted, **then** `PROHIBITED_CALCULATED_FIELD` is returned and no draft is saved;
    **and given** the same request also contains ordinary unknown/prohibited properties or another
    Stage-5 structural failure, **then** the calculated-field result retains precedence.
29. **Given** exactly 500 lines, 8 positive payments, and 15 additional-information entries, **when**
    all other rules pass, **then** those boundary counts are accepted.
30. **Given** more than 500 lines, 8 payments, or 15 additional-information entries, **when**
    creation is attempted, **then** it is rejected and no draft is saved.
31. **Given** text values at their approved normalized maxima, **when** creation is attempted,
    **then** general text is accepted after the approved NFC, `U+0020` trim, prohibited-code-point,
    and code-point-length rules; stricter ASCII fields use their own one-time SP/HTAB trim and exact
    expressions; an additional-information name is accepted only when its derived canonical value
    also remains within 300 code points.
32. **Given** a product code, description, buyer name, contact field, additional-information name or
    value, or idempotency key exceeds its maximum, **when** creation is attempted, **then** the
    applicable stable error is returned—`IDEMPOTENCY_KEY_INVALID` for that header—and no state is
    created.
33. **Given** required general text becomes empty after trimming, contains a prohibited code point
    or whitespace, has invalid telephone form, violates the exact buyer-email profile, or duplicates
    an additional-information canonical name, **when** creation is attempted, **then** it is rejected
    and no draft is saved; an email-profile failure returns `BUSINESS_VALIDATION_FAILED` with safe
    value-free violation `EMAIL_INVALID` for `buyer.email`.
34. **Given** valid Company and emission-point identifiers, including an emission-point UUID with
    surrounding ASCII SP or HTAB, **when** creation succeeds, **then** the draft is owned by the
    immutable Company UUID and retains the trimmed canonical emission-point identifier without
    resolving fiscal master data; **and given** a decoded emission-point string that becomes blank,
    is malformed, or is the nil UUID, **when** Stage 6 evaluates it, **then** the service returns
    `BUSINESS_VALIDATION_FAILED` (`422`) with safe violation `EMISSION_POINT_INVALID` and saves no
    state.
35. **Given** a syntactically valid Company UUID is unknown, inactive, ineligible, or unauthorized
    elsewhere, **when** the request passes local draft rules, **then** no external lookup occurs and
    the draft is created under that UUID.
36. **Given** a draft and its scoped idempotency association still exist, regardless of elapsed
    time, **when** an equivalent retry occurs, **then** the original draft is returned.
37. **Given** an equivalent retry reorders only payments or additional information, **when**
    equivalence is evaluated, **then** the original draft is returned.
38. **Given** a retry changes invoice-line order, **when** equivalence is evaluated, **then** an
    idempotency conflict is returned and no draft is modified.
39. **Given** a positive-total draft repeats a payment method, **when** creation is attempted,
    **then** a stable duplicate-payment-method error is returned and no draft is saved.
40. **Given** the caller selects an approved IVA rule effective on the emission date, **when**
    creation is attempted, **then** that rule is used without product classification; a line tax
    code or rate supplied instead of `taxRuleId` is rejected.
41. **Given** a valid new Company-scoped request, **when** creation succeeds, **then** it saves no
    Company-context version or fiscal snapshot and performs no fiscal-issuance side effect.
42. **Given** an existing draft and equivalent request, **when** related master data changes
    elsewhere, **then** replay returns the original draft without external lookup or mutation.
43. **Given** the service boundary is reviewed, **then** this feature has no Company master-data,
    identity, authorization, tenant, cache, or external Company availability requirement.
44. **Given** the body supplies `issuerId`, Issuer attributes, establishment fiscal data, or an
    emission-point fiscal snapshot, **when** body validation runs, **then** each property is rejected
    and no state is created.
45. **Given** the body is conclusively larger than `2 MiB` before deadline expiry, **when** it is
    received, **then** `REQUEST_PAYLOAD_TOO_LARGE` (`413`) wins before Company validation, correlation
    remains safe, no later processing occurs, and no state is created; if the deadline becomes
    conclusive first, `REQUEST_TIMEOUT` wins.
46. **Given** saving is unavailable, times out, or fails and a complete reversal is confirmed before
    the deadline, **when** processing ends, **then** the approved safe error is returned and no state
    remains; if deadline expiry occurs while the save outcome is unresolved, `REQUEST_TIMEOUT`
    makes no zero-state claim and same-scope replay resolves the result without duplication.
47. **Given** `X-Company-Id` is malformed or nil, **when** creation is attempted, **then**
    `COMPANY_CONTEXT_INVALID` is returned safely with correlation and no state is created.
48. **Given** multiple `X-Company-Id` values, **when** creation is attempted, **then**
    `COMPANY_CONTEXT_INVALID` rejects the ambiguity and no state is created.
49. **Given** quantity `0.000001` or `999999.999999`, unit price `0` or `999999.999999`, and all
    resulting money remains in range, **when** the draft is calculated, **then** those inclusive
    boundaries are accepted without precision loss.
50. **Given** any approved numeric range is exceeded by input, calculation, grouping, payment sum,
    or total, **when** creation is attempted, **then** `BUSINESS_VALIDATION_FAILED` contains
    `MONETARY_RANGE_EXCEEDED` and no state is created.
51. **Given** the request first reaches the service immediately before midnight in
    `America/Guayaquil` and uses the date fixed before body consumption, **when** body consumption or
    later completion crosses midnight, **then** that date remains accepted and the successful
    response returns equal UTC `createdAt` and `updatedAt` values for the saved draft.
52. **Given** a saved draft is replayed on a later Ecuadorian date, **when** the same scope and
    equivalent content are used, **then** the original draft and emission date are returned without
    current-date revalidation.
53. **Given** a proposed identification, IVA, or payment reference lacks official evidence or an
    approved mapping, **when** support is considered, **then** it remains Pending Functional
    Validation and is not accepted by this feature.
54. **Given** the approved baseline publishes stable `taxRuleId` and `paymentMethodId` UUIDs,
    **when** a client supplies them, **then** the applicable entries are selected without a catalog
    query operation and no replacement identifier is assigned during the request.
55. **Given** `X-Correlation-Id` is absent and Company context is valid, **when** processing begins,
    **then** a safe UUID correlation identifier is generated and returned.
56. **Given** exactly one safe `X-Correlation-Id` satisfies the grammar, **when** processing begins,
    **then** it is preserved and returned unchanged.
57. **Given** `X-Correlation-Id` is blank, repeated, over 64 characters, or unsafe, **when** its
    validation governs, **then** the value is not echoed, a safe replacement is returned with
    `INVALID_REQUEST`, and no state is created.
58. **Given** Company context and correlation are both invalid, **when** precedence is evaluated,
    **then** the Company error wins and a safe replacement correlation identifier is returned.
59. **Given** `Idempotency-Key` is missing, **when** its validation governs, **then**
    `IDEMPOTENCY_KEY_REQUIRED` is returned; one blank, trim-to-empty, over-length, non-ASCII,
    control-containing, or grammar-invalid value returns `IDEMPOTENCY_KEY_INVALID`. No state is
    created.
60. **Given** repeated, parser-multiple, or comma-combined `Idempotency-Key` values, **when** header
    validation governs, **then** `IDEMPOTENCY_KEY_MULTIPLE` is returned, no first value is selected,
    and no lookup or state change occurs.
61. **Given** one key with surrounding SP/HTAB and valid internal characters, **when** it is
    accepted, **then** surrounding whitespace is trimmed once, internal characters and case are
    preserved, and the same normalized value governs the idempotency association; replay returns
    the original timestamps unchanged.
62. **Given** `effectiveFrom` equals `emissionDate`, **when** a payment method exists and is active,
    **then** it is accepted.
63. **Given** finite `effectiveTo` equals `emissionDate`, **when** a payment method exists and is
    active, **then** it is accepted.
64. **Given** `effectiveFrom` is after `emissionDate`, **when** payment validity is evaluated,
    **then** the method is rejected as not yet effective.
65. **Given** finite `effectiveTo` is before `emissionDate`, **when** payment validity is evaluated,
    **then** the method is rejected as expired.
66. **Given** an active payment method has no `effectiveTo` and begins on or before `emissionDate`,
    **when** validity is evaluated, **then** it is accepted as open-ended.
67. **Given** an inactive payment method is temporally effective on `emissionDate`, **when**
    validity is evaluated, **then** it is rejected.
68. **Given** an active payment method is temporally ineffective on `emissionDate`, **when**
    validity is evaluated, **then** it is rejected without substituting current date, request time,
    or draft creation time.
69. **Given** decodable business text, **when** text processing occurs, **then** each applicable
    value is transformed exactly once according to the approved Unicode policy before comparison
    and storage, and equivalent Unicode forms produce the same normalized result.
70. **Given** an additional-information display name contains no more than 300 code points but its
    deterministic locale-independent lowercase canonical value exceeds 300—for example 300
    occurrences of `U+0130`—**when** canonicalization occurs, **then** the request is rejected
    without truncation using `BUSINESS_VALIDATION_FAILED` and `CANONICAL_NAME_TOO_LONG`, including
    the original field, maximum, counting unit, and stage.
71. **Given** validation and calculation succeed, **when** the complete save succeeds, **then** one
    complete draft and its idempotency association are saved together and the response returns the
    final identifier, business values, and equal timestamps; a confirmed failed save exposes no
    provisional draft, association, or timestamps.

#### Acceptance Evidence Matrix — Emission-Point Representation

The following rows are explicit subcases of acceptance scenario 34 and SC-022. `Raw value` is the
decoded request string; `\t` denotes one ASCII HTAB. Every accepted value produces the same
canonical response representation on new creation and equivalent replay.

| Case | Raw value or representation | Governing stage | Observable outcome |
|------|-----------------------------|-----------------|--------------------|
| Already canonical | `123e4567-e89b-12d3-a456-426614174000` | Stage 6 | Accepted as `123e4567-e89b-12d3-a456-426614174000` |
| Uppercase input | `123E4567-E89B-12D3-A456-426614174000` | Stage 6 | Accepted as `123e4567-e89b-12d3-a456-426614174000` |
| Surrounding ASCII SP | ` 123e4567-e89b-12d3-a456-426614174000 ` | Stage 6 | Accepted as `123e4567-e89b-12d3-a456-426614174000` |
| Surrounding ASCII HTAB | `\t123E4567-E89B-12D3-A456-426614174000\t` | Stage 6 | Accepted as `123e4567-e89b-12d3-a456-426614174000` |
| Missing property | property absent | Stage 5 | `INVALID_REQUEST` (`400`) |
| Non-string property | JSON number, object, array, boolean, or `null` | Stage 5 | `INVALID_REQUEST` (`400`) |
| Empty string | `""` | Stage 6 | `BUSINESS_VALIDATION_FAILED` (`422`) / `EMISSION_POINT_INVALID` |
| Trim-to-empty SP | `"   "` | Stage 6 | `BUSINESS_VALIDATION_FAILED` (`422`) / `EMISSION_POINT_INVALID` |
| Trim-to-empty HTAB | `"\t\t"` | Stage 6 | `BUSINESS_VALIDATION_FAILED` (`422`) / `EMISSION_POINT_INVALID` |
| Malformed UUID | `not-a-uuid` | Stage 6 | `BUSINESS_VALIDATION_FAILED` (`422`) / `EMISSION_POINT_INVALID` |
| Nil UUID | `00000000-0000-0000-0000-000000000000` | Stage 6 | `BUSINESS_VALIDATION_FAILED` (`422`) / `EMISSION_POINT_INVALID` |
| Internal ASCII SP | `123e4567-e89b-12d3-a456-4266141740 00` | Stage 6 | `BUSINESS_VALIDATION_FAILED` (`422`) / `EMISSION_POINT_INVALID` |
| Internal ASCII HTAB | `123e4567-e89b-12d3-a456-4266141740\t00` | Stage 6 | `BUSINESS_VALIDATION_FAILED` (`422`) / `EMISSION_POINT_INVALID` |
| Braced UUID | `{123e4567-e89b-12d3-a456-426614174000}` | Stage 6 | `BUSINESS_VALIDATION_FAILED` (`422`) / `EMISSION_POINT_INVALID` |
| Non-hyphenated UUID | `123e4567e89b12d3a456426614174000` | Stage 6 | `BUSINESS_VALIDATION_FAILED` (`422`) / `EMISSION_POINT_INVALID` |

Every Stage-6 emission-point rejection is safe and correlated, identifies only
`field=emissionPointId` and `validationStage=NORMALIZATION`, exposes no rejected value, and occurs
before general-text normalization, fingerprinting, idempotency lookup, business validation,
calculation, or save. It therefore creates no draft or idempotency association.

#### Acceptance Evidence Matrix — Validation and Deadline Competition

The following rows are explicit subcases of acceptance scenarios 28, 33, 34, 45, and 46 and make
FR-041/SC-026 pairwise precedence executable. In every row exactly one safe correlated response is
returned; a later outcome is discarded and cannot replace it.

| Competing request facts | Outcome when classification/validation is conclusive before the deadline | Outcome when deadline is conclusive first |
|-------------------------|------------------------------------------------------------------------|-------------------------------------------|
| Recognized calculated path plus ordinary unknown/prohibited property | `PROHIBITED_CALCULATED_FIELD` (`422`) | `REQUEST_TIMEOUT` (`504`) |
| Recognized calculated path plus missing/non-string or invalid-string `emissionPointId` | `PROHIBITED_CALCULATED_FIELD` (`422`) | `REQUEST_TIMEOUT` (`504`) |
| Recognized calculated path plus an additional-information name that would produce Stage-6 `CANONICAL_NAME_TOO_LONG` | `PROHIBITED_CALCULATED_FIELD` (`422`) | `REQUEST_TIMEOUT` (`504`) |
| Ordinary unknown/prohibited property plus invalid-string `emissionPointId` | `INVALID_REQUEST` (`400`) | `REQUEST_TIMEOUT` (`504`) |
| Ordinary unknown/prohibited property plus an additional-information name that would produce Stage-6 `CANONICAL_NAME_TOO_LONG` | `INVALID_REQUEST` (`400`) | `REQUEST_TIMEOUT` (`504`) |
| Valid Stage 5 plus invalid `emissionPointId` and a name that would produce `CANONICAL_NAME_TOO_LONG` | `BUSINESS_VALIDATION_FAILED` (`422`) / `EMISSION_POINT_INVALID` | `REQUEST_TIMEOUT` (`504`) |
| Valid emission point plus Stage-6 `CANONICAL_NAME_TOO_LONG` and a normalized email that would fail Stage 10 | `BUSINESS_VALIDATION_FAILED` (`422`) / `CANONICAL_NAME_TOO_LONG`; Stage 10 does not run | `REQUEST_TIMEOUT` (`504`) |
| Valid Stage 6 plus normalized buyer email outside the approved grammar | `BUSINESS_VALIDATION_FAILED` (`422`) / `EMAIL_INVALID` | `REQUEST_TIMEOUT` (`504`) |
| Valid independent rules plus a Stage-11B calculated-value violation | The first FR-041 Stage-11B violation | `REQUEST_TIMEOUT` (`504`) |

Malformed JSON remains `INVALID_REQUEST` because its properties cannot be classified. Once any
Stage-5 or Stage-6 result in the table is selected, no fingerprint, idempotency lookup, later
validation, calculation, or save occurs, so no state is created.

### Edge Cases

- A syntactically valid noncanonical Company UUID is normalized to lowercase hyphenated form. Nil
  UUID is rejected.
- FR-041 governs competing failures. Payload size precedes Company validation; Company validation
  precedes correlation; correlation precedes idempotency and body validation; equivalent replay
  precedes current business revalidation. Deadline expiry first produces `REQUEST_TIMEOUT`.
- Correlation always yields a safe response identifier and never echoes unsafe input.
- Impossible dates and dates different from the one Ecuadorian date fixed at the initial service
  boundary before body consumption are rejected. Crossing midnight later does not change that date;
  replay does not reevaluate it.
- Quantity zero, negative, or over `999999.999999`; unit price negative or over that limit; and
  negative discount are rejected.
- Every monetary input and result stays within `0.00` through `999999999999999.99`; values are
  never saturated, truncated, wrapped, or silently reduced.
- Discount equal to gross may produce zero net, and all lines may produce total `0.00`.
- Quantity or unit price with more than six decimals and discount or payment without exactly two
  decimals are rejected rather than rounded.
- A caller selects an effective tax rule and cannot supply tax code or rate. An approved rate with
  more than two decimals is invalid reference data rather than rounded.
- Every line has exactly one active IVA rule effective on emission date. IVA 0%, not subject to IVA,
  and exempt remain separate even when their calculated tax is zero.
- Unsupported or simultaneous taxes and special deemed-base or gratuitous-transfer treatment are
  rejected.
- A zero-price or zero-net line keeps its explicitly selected tax treatment.
- Reference data used by one accepted draft is internally consistent; a partially validated mix is
  never saved.
- An unverified reference entry remains Pending Functional Validation and cannot be accepted.
- Payment comparison uses exact two-decimal amounts after the approved calculation and rounding.
- A payment method cannot repeat within a draft.
- JSON property order and payment or additional-information order do not change equivalence;
  invoice-line order does.
- Replay is limited to the normalized Company and key scope and performs no entitlement check.
- Valid Company UUIDs are accepted opaquely without Company-state validation.
- Emission-point identifier is retained opaquely without fiscal master-data resolution. A missing
  property or non-string JSON representation fails Stage 5 with `INVALID_REQUEST`; a decoded string
  that becomes blank after one surrounding ASCII SP/HTAB trim, is malformed, or is the nil UUID
  fails first in Stage 6 with `BUSINESS_VALIDATION_FAILED` and safe violation
  `EMISSION_POINT_INVALID`.
- A successful idempotency association does not expire while its draft exists.
- Missing, repeated, blank, malformed, or nil Company context is rejected before creation. No body,
  path, query, token, or session value may substitute for the header.
- The exhaustive calculated-property paths are rejected rather than ignored or compared. For a
  well-formed object, their presence wins over ordinary unknown/prohibited properties, missing
  properties, and other representation failures in the same Stage-5 classification. Malformed JSON
  remains `INVALID_REQUEST` because no property classification is possible.
- Identification `05` is exactly 10 ASCII digits; `04` is exactly 13 ASCII digits. No checksum is
  applied.
- Identification `06` and `08` match `^[A-Za-z0-9]{1,20}$` after one SP/HTAB trim. Valid examples:
  `A1234567`, `EC9Z`. Invalid: `A-123`, `A 123`, `Á123`, empty, or 21 characters.
- `productCode` matches `^[A-Za-z0-9]{1,25}$` after one SP/HTAB trim. Valid: `ABC123`, `sku9`.
  Invalid: `ABC-123`, `ABC 123`, `ÁBC1`, empty, or 26 characters.
- Buyer email uses the exact post-normalization ASCII dot-atom profile below. Surrounding `U+0020`
  may be removed only by FR-035; internal whitespace, non-ASCII text, multiple addresses, comments,
  quoted local parts, and domain literals remain invalid. Case is preserved and compared exactly.
- Identification `07` requires value `9999999999999`, name `CONSUMIDOR FINAL`, and total no greater
  than USD `50.00` under SRI Technical Sheet v2.32.
- Buyer identifiers and contact data never appear in safe errors or operational observations.
- A draft with 501 lines, 9 payments, or 16 additional-information entries is rejected without
  partial state.
- General text follows FR-035 and DR-019; stricter ASCII fields follow their own rules. Required or
  supplied optional text empty after normalization is rejected.
- A display name within 300 code points can still fail if its canonical value exceeds 300. It is
  rejected with `CANONICAL_NAME_TOO_LONG` and never truncated.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Every create request MUST contain exactly one `X-Company-Id` value. One surrounding
  ASCII SP/HTAB trim MUST be applied. The result MUST be a nonblank, syntactically valid, non-nil
  UUID. Missing or blank MUST return `COMPANY_CONTEXT_REQUIRED`; repeated, malformed, or nil MUST
  return `COMPANY_CONTEXT_INVALID`. Either result MUST be safe, correlated, and create no state.
- **FR-002**: Accepted Company UUID text MUST be normalized to canonical lowercase hyphenated form.
  Company context MUST come only from `X-Company-Id`; it MUST NOT be accepted from a request body,
  path, query, token, or session. Body `companyId` MUST be rejected. Canonical `companyId` MAY appear
  in the response as required by FR-022. The operation MUST be `POST /api/v1/invoice-drafts`.
- **FR-003**: Accepted Company UUIDs MUST be treated as opaque context. The service MUST NOT validate
  Company existence, state, eligibility, tenant ownership, entitlement, or fiscal relationships.
  It MUST NOT define `COMPANY_NOT_FOUND`, `COMPANY_INACTIVE`,
  `COMPANY_NOT_FISCALLY_ELIGIBLE`, `COMPANY_NOT_AUTHORIZED`,
  `COMPANY_CONTEXT_UNAVAILABLE`, `COMPANY_CONTEXT_TIMEOUT`,
  `COMPANY_ISSUER_CONFIGURATION_INVALID`, `EMISSION_POINT_COMPANY_MISMATCH`, authentication or
  authorization failures, `401`, or `403`.
- **FR-004**: A request MUST contain one emission-point external identifier that, after one
  surrounding ASCII SP/HTAB trim, is a nonblank, non-nil UUID. It MUST be normalized to canonical
  lowercase hyphenated form, retained as an opaque reference, and MUST NOT trigger relationship or
  status validation. A missing property or non-string JSON representation MUST fail Stage 5 with
  `INVALID_REQUEST`. A decoded string that is blank after the approved trim, malformed, or the nil
  UUID MUST fail as the first Stage 6 check with `BUSINESS_VALIDATION_FAILED` (`422`) and nested safe
  violation `EMISSION_POINT_INVALID`; the violation MUST identify `emissionPointId` and the
  normalization stage without exposing the rejected value.
- **FR-005**: Request bodies MUST NOT contain Issuer, establishment, emission-point fiscal, Company
  version, observation-time, or fiscal-snapshot data. Such properties MUST be rejected and MUST NOT
  be saved or returned as master-data snapshots.
- **FR-006**: The accepted `emissionDate` MUST equal the `America/Guayaquil` civil date determined
  once at the initial service boundary before the request body is consumed. Impossible or different
  dates MUST be rejected. The expected date MUST remain fixed if body consumption or later
  processing crosses midnight.
- **FR-007**: Buyer legal name, identification value, and exactly one supported type MUST be present.
  Supported types are `04`, `05`, `06`, `07`, and `08`. The selected type MUST be active and
  effective on `emissionDate`; the value MUST follow the approved type rule. Types `06` and `08`
  MUST match case-sensitive ASCII `^[A-Za-z0-9]{1,20}$` after one surrounding SP/HTAB trim.
- **FR-008**: Buyer address, email, and telephone MAY be supplied. After FR-035 normalization,
  address MUST contain 1–300 code points; email MUST satisfy the exact case-sensitive ASCII
  dot-atom profile in **Executable Buyer Email Profile**, including a 1–64-character local part,
  1–63-character DNS-style domain labels, at least one domain dot, and at most 254 total code points;
  and telephone MUST contain 7–15 ASCII digits, at most 20 total code points, and only digits, `+`,
  `U+0020`, hyphen, and parentheses. Email case MUST be preserved, no email-specific normalization
  or case folding is allowed, and failure MUST return `BUSINESS_VALIDATION_FAILED` (`422`) with safe
  value-free violation `EMAIL_INVALID` identifying `buyer.email`.
- **FR-009**: A draft MUST contain 1–500 invoice lines.
- **FR-010**: Each line MUST contain a product code, description, quantity greater than zero and at
  most `999999.999999`, unit price from `0` through `999999.999999`, absolute discount from `0.00`
  through `999999999999999.99`, and exactly one IVA rule. Quantity and unit price allow at most six
  decimals. Product code MUST match case-sensitive ASCII `^[A-Za-z0-9]{1,25}$` after one
  surrounding SP/HTAB trim. Description follows FR-035 and contains 1–300 code points.
- **FR-011**: Each selected tax rule MUST be active, belong to IVA, and be effective on
  `emissionDate`. Supported treatments are configured percentage-rate IVA, IVA 0%, not subject to
  IVA, and exempt. Other or simultaneous taxes MUST be rejected. The caller supplies only
  `taxRuleId`; tax code, rate, and product classification MUST NOT be supplied or inferred here.
- **FR-012**: The service MUST calculate line gross, line net, tax base, tax amount, grouped tax
  totals, subtotal before taxes, total discount, grand total, and line total. The caller MUST NOT
  supply the exhaustive paths in **Executable Stage-5 Request Property Classification**. Presence of
  any recognized path MUST return `PROHIBITED_CALCULATED_FIELD` even when its value is null, has the
  wrong JSON type, or matches the service result. For a well-formed request object, this result MUST
  take precedence over ordinary unknown/prohibited properties and every other Stage-5 structural
  failure; the response MUST expose no rejected value and MUST omit field-level violations. Every
  monetary input and result MUST stay within `0.00` through `999999999999999.99`.
- **FR-013**: A draft MUST contain at least one payment. Each payment method MUST exist, be active,
  and satisfy `effectiveFrom <= emissionDate` and (`effectiveTo` absent or
  `emissionDate <= effectiveTo`). No other date may substitute. Positive-total drafts allow at
  most 8 unique payment methods with each amount greater than `0.00`; zero-total drafts require
  exactly one payment of `0.00`.
- **FR-014**: The exact sum of two-decimal payment amounts MUST equal the calculated two-decimal
  grand total after DR-010.
- **FR-015**: A draft MAY contain at most 15 additional-information entries. Each display name and
  value follows FR-035 and contains 1–300 code points. Names MUST be unique by DR-019
  `canonicalName`, which MUST contain 1–300 code points. Overflow MUST return
  `BUSINESS_VALIDATION_FAILED` with `CANONICAL_NAME_TOO_LONG`; no value may be truncated.
- **FR-016**: Currency MUST be USD.
- **FR-017**: A successfully created record MUST have status `DRAFT`, which MUST NOT be represented
  as an official SRI state or fiscal issuance evidence.
- **FR-018**: A successful draft MUST receive a unique identifier unrelated to official sequence,
  access key, or authorization number.
- **FR-019**: A successful new draft MUST return unambiguous UTC `createdAt` and `updatedAt` values
  that are identical and belong to the saved draft. A confirmed failed save MUST expose no created
  resource. Equivalent replay MUST return both originally recorded values unchanged and MUST NOT
  produce new timestamps. Neither timestamp determines `emissionDate`.
- **FR-020**: The draft, lines, taxes, payments, additional information, and idempotency association
  MUST be saved completely or not at all. After validation and calculation, the service MUST save
  one complete draft and return its final identifier, business values, and timestamps. No
  provisional or partial draft may be externally visible.
- **FR-021**: Header, body, business, reference-data, or deadline rejection and every confirmed
  completely reversed save MUST leave no draft or idempotency association. No zero-state claim may
  be made while a save outcome is unresolved or after a successful save; FR-032, FR-033, and
  FR-043 govern those cases.
- **FR-022**: A successful response MUST include identifier, `DRAFT`, canonical `companyId`, opaque
  emission-point identifier in FR-004's canonical lowercase-hyphenated UUID form, buyer, emission
  date, lines, payments, additional information,
  calculated line amounts, grouped taxes, totals, `createdAt`, and `updatedAt`. It MUST include no
  Company, Issuer, establishment, or emission-point master-data snapshot.
- **FR-023**: Creation MUST NOT reserve a sequence; generate access key, XML, signature, PDF, or
  RIDE; read a certificate; call the SRI; create an integration or notification job; or send a
  notification.
- **FR-024**: Every operation involving a draft or its idempotency association MUST enforce the
  authoritative Company context and prevent cross-Company access, reuse, conflict, or mutation.
  This is business ownership, not authentication or authorization. Global VAT, payment-method,
  identification-type, and other immutable SRI catalogs are shared and not Company-owned.
- **FR-025**: Failures MUST use stable machine-readable English codes and safe English messages.
  They MUST NOT expose buyer identification, secrets, internal paths, storage details, stack
  traces, or other sensitive implementation information.
- **FR-026**: Every response MUST have a safe correlation identifier. An absent
  `X-Correlation-Id` MUST generate a UUID. One supplied value MUST be trimmed once and preserved
  only when it has 1–64 ASCII characters, begins with a letter or digit, and contains only letters,
  digits, `.`, `_`, `:`, or `-`. Invalid input MUST never be echoed; when its validation governs,
  it returns `INVALID_REQUEST` with a safe replacement. Payload-size rejection keeps its earlier
  result while still returning safe correlation.
- **FR-027**: `Idempotency-Key` MUST be mandatory and single-valued after HTTP parsing. One
  surrounding SP/HTAB trim MUST be applied without altering internal characters or case. The
  result MUST contain 1–128 case-sensitive ASCII characters matching
  `^[\x21-\x2B\x2D-\x7E](?:[\x20-\x2B\x2D-\x7E]{0,126}[\x21-\x2B\x2D-\x7E])?$`.
  Missing MUST return `IDEMPOTENCY_KEY_REQUIRED`; one blank, over-length, or grammar-invalid value
  MUST return `IDEMPOTENCY_KEY_INVALID`; repeated, parser-multiple, or comma-containing input MUST
  return `IDEMPOTENCY_KEY_MULTIPLE`. No first value may be selected. The same normalized value MUST
  govern the idempotency association, scoped with canonical Company UUID.
- **FR-028**: Buyer validation MUST use only the approved type syntax, length, and special values.
  No checksum, online SRI lookup, or external buyer-name verification may be added to this feature.
- **FR-029**: Idempotency content equivalence MUST include emission point, emission date, buyer,
  ordered lines, tax treatments, payments, and additional information. Company determines scope but
  is not duplicated in compared content. Idempotency key, correlation, and other headers do not
  affect content equivalence. JSON property, payment, and additional-information ordering are
  insignificant; line ordering is significant.
- **FR-030**: The first successful save MUST associate canonical Company, normalized key, normalized
  business content, and the created draft as one all-or-nothing outcome. Equivalent retry MUST
  return the original draft; different content MUST return `IDEMPOTENCY_CONFLICT`. The association
  MUST last as long as the draft and MUST NOT expire by time.
- **FR-031**: Concurrent equivalent requests in the same Company-and-key scope MUST create exactly
  one draft and resolve to it. The same key MAY be used independently by different Companies.
- **FR-032**: Header, business, or reference rejection and every confirmed completely reversed save
  MUST NOT associate the key. A successful save MUST remain associated after response timeout,
  connection loss, or response-delivery failure.
- **FR-033**: Equivalent replay MUST return the original draft only within the current
  Company-and-key scope. It MUST NOT repeat creation, call Company capabilities, authenticate,
  authorize, refresh external data, revalidate emission date against the current date, modify either
  timestamp, or produce new timestamps.
- **FR-034**: The invoice-draft feature MUST own the idempotency association and MUST NOT delegate it
  to a Company capability, gateway, BFF, or another service.
- **FR-035**: Unless a stricter field rule applies, every human-readable single-line value MUST be
  interpreted as Unicode, normalized to NFC exactly once, trimmed only of surrounding `U+0020`,
  and rejected when it contains category `Cc`, `Cf`, `Cs`, `Co`, or `Cn`, `U+2028`, `U+2029`, tab,
  CR, LF, NBSP, or any whitespace other than `U+0020`. Internal punctuation and `U+0020` runs and
  display case MUST be preserved. Length MUST be counted in code points after normalization and
  trimming. Comparison is case-sensitive unless specified otherwise. Empty required or supplied
  optional text MUST be rejected. Assigned emoji such as `U+1F600` category `So` is accepted when
  the field's format and length permit it. Stricter ASCII fields follow their exact rules.
- **FR-036**: Creation and replay MUST make no Company Service call and MUST have no Company
  availability, readiness, status, eligibility, cache, master-data, shared-storage, or cross-service
  relationship dependency or related failure, timeout, or retry outcome.
- **FR-037**: A draft MUST retain the canonical Company UUID as its only Company ownership
  reference, immutable after creation. All draft and idempotency operations MUST comply with
  FR-024. Child values belong through the draft and do not represent Company master data. Shared
  immutable SRI catalogs remain outside Company scope.
- **FR-038**: Creation MUST NOT resolve or save Company master data, context versions, observation
  timestamps, or Issuer, establishment, or emission-point fiscal snapshots. Fiscal eligibility,
  sequence, access key, XML, signing, and SRI submission belong to later approved work.
- **FR-039**: The service MUST NOT require or interpret identity protocols, access tokens, API
  keys, sessions, authenticated principals, users, roles, permissions, or tenant authorization.
  The contract MUST define no security scheme, Authorization header, `401`, or `403` response.
- **FR-040**: The accepted `X-Company-Id` value MUST become the authoritative Company context for
  all subsequent draft behavior. No other request context, security context, caller identity, or
  external Company client may influence or replace it.
- **FR-041**: The create operation MUST have one 10-second request deadline beginning before body
  consumption. Observable processing MUST follow this exact precedence:
  1. enforce request body size;
  2. validate `X-Company-Id` presence, cardinality, trim, UUID syntax, and non-nil value;
  3. validate `X-Correlation-Id`;
  4. validate `Idempotency-Key` presence, cardinality, trim, grammar, and stable errors;
  5. decode one JSON object; malformed JSON, an unsupported/non-object representation, ordinary
     unknown/prohibited properties, and missing or incorrectly typed required properties use
     `INVALID_REQUEST`, while any exhaustive recognized calculated-property path uses
     `PROHIBITED_CALCULATED_FIELD` and, for a well-formed object, takes precedence over all other
     outcomes within this stage;
  6. first trim and validate `emissionPointId` according to FR-004, returning
     `BUSINESS_VALIDATION_FAILED` with `EMISSION_POINT_INVALID` when invalid; then normalize
     applicable business text according to FR-035 and DR-019, including canonical length;
  7. resolve the local Company-scoped idempotency association;
  8. return the original draft for equivalent content;
  9. return `IDEMPOTENCY_CONFLICT` for different content;
  10. validate rules independent of calculated amounts: buyer, identification, required lines,
      cardinalities, product code, description, numeric input form, tax and payment reference
      existence/activity/effectiveness, payment structure, text, and collection rules;
  11. calculate line and invoice monetary values, then validate calculated-value rules in this
      exact order: range or overflow by line then tax group then invoice; discount over gross by
      line; final-consumer total limit; total-dependent payment shape and positivity; exact payment
      reconciliation; then any later approved calculated-value rule at its explicitly assigned
      position; and
  12. save the complete draft and idempotency association and return the successful response.

  A rule dependent on calculated values MUST NOT run before calculation. Within one priority item,
  lowest line position and then canonical tax-group order govern. Payload, Company, correlation,
  and idempotency precedence MUST remain unchanged. A result conclusively determined before the
  deadline wins; otherwise `REQUEST_TIMEOUT` (`504`) wins. Exactly one response is returned, and a
  late outcome cannot replace it. An unresolved save makes no zero-state claim and is reconciled by
  same-scope replay.
- **FR-042**: A body conclusively larger than `2 MiB` (`2,097,152` bytes) before deadline expiry
  MUST return `REQUEST_PAYLOAD_TOO_LARGE` (`413`) before Company validation. Correlation MUST remain
  safe. No later validation, calculation, reference lookup, or state change may occur. If deadline
  expiry becomes conclusive first, FR-041 selects `REQUEST_TIMEOUT`.
- **FR-043**: Local save unavailability or its shorter configured timeout conclusively determined
  while deadline budget remains MUST return `PERSISTENCE_UNAVAILABLE`; an unexpected classified
  service failure MUST return `INTERNAL_ERROR`. Both responses MUST be safe and correlated. Zero
  state may be claimed only for a pre-save failure or confirmed complete reversal. Deadline expiry
  while save outcome is unresolved MUST return `REQUEST_TIMEOUT`, make no zero-state claim, and be
  recoverable by same-scope replay. A successful save survives response timeout or delivery
  failure. A late result MUST NOT replace the selected response or trigger deletion or compensation.
- **FR-044**: Any quantity, price, money, or percentage outside the approved envelope MUST return
  `BUSINESS_VALIDATION_FAILED` with `MONETARY_RANGE_EXCEEDED` before saving. The same limits MUST
  govern request values, calculations, grouped results, payment reconciliation, saved values,
  responses, and acceptance vectors. Values MUST NOT be rounded, clamped, truncated, wrapped, or
  exposed as a storage error to bypass the rule.
- **FR-045**: Supported identification types, IVA rules, and payment methods MUST come only from the
  approved versioned reference baseline. Every supported entry MUST retain official code and label,
  canonical English name, treatment or validation strategy, rate when applicable, validity,
  activity, catalog version, official source, and approval status.
- **FR-046**: Every approved tax rule and payment method MUST use its published stable fixed UUID.
  Those identifiers MUST remain unchanged across equivalent executions. This feature MUST NOT
  expose a catalog-query operation; callers use identifiers published by the approved contract.
- **FR-047**: No tax rate, payment code, identification rule, validity period, official mapping, or
  stable identifier may be invented. Unsupported entries MUST remain unavailable. Any later
  unverified entry MUST remain Pending Functional Validation until official evidence and target
  mapping are approved.

### Domain Rules and Invariants

- **DR-001**: Official SRI Technical Sheet v2.32 and the approved versioned baseline govern
  identification types, tax rules, and payment methods. Each approved entry retains official code,
  display name, treatment or validation strategy, rate when applicable, validity, activity,
  catalog version, exact official source, and stable published identifier. Legacy or unresolved
  entries are not authoritative.
- **DR-002**: `gross amount = quantity × unit price` for each line.
- **DR-003**: `net amount = gross amount − absolute discount` for each line.
- **DR-004**: Discount MUST NOT exceed gross amount.
- **DR-005**: For percentage-rate IVA, tax base is line net amount and
  `tax amount = tax base × (tax rate ÷ 100)`, where `15.00` means 15%. IVA 0%, not subject to IVA,
  and exempt use line net as base and `0.00` tax.
- **DR-006**: `subtotal before taxes = sum of rounded line net amounts`.
- **DR-007**: `total discount = sum of two-decimal line discounts`.
- **DR-008**: Tax totals MUST be grouped by versioned treatment code and applicable rate and include
  summed tax base and tax amount. The three zero-tax treatments remain separate groups.
- **DR-009**: `grand total = subtotal before taxes + sum of calculated tax amounts`.
- **DR-010**: All arithmetic MUST use exact decimal values. Quantity is greater than `0` and at most
  `999999.999999`; unit price is `0` through that maximum; both allow at most six decimals. Rates
  are `0.00` through `100.00` with at most two decimals. Discounts and payments use exactly two
  decimals. Every monetary value and result is `0.00` through `999999999999999.99`. Excess input
  precision is rejected. For each line, exact gross is rounded to two decimals using `HALF_UP`; the
  two-decimal discount is subtracted and net is rounded `HALF_UP`; rounded net is tax base; each tax
  is rounded `HALF_UP`. Aggregates sum those rounded values and use two decimals. Payment comparison
  uses the exact sum of two-decimal amounts. Overflow returns `MONETARY_RANGE_EXCEEDED`.
- **DR-011**: The service calculates and reconciles; the caller supplies only commercial inputs and
  payment allocations. The exhaustive calculated-property paths are rejected through the
  deterministic Stage-5 classification in FR-012 and FR-041; no recognized path is ignored,
  compared with a calculated result, or downgraded to an ordinary unknown-property outcome.
- **DR-012**: The expected emission date is the one `America/Guayaquil` civil date fixed at the
  initial service boundary before body consumption and remains unchanged across midnight. A
  successful new draft returns equal UTC
  `createdAt` and `updatedAt` values. Equivalent replay returns the original emission date and both
  original timestamps unchanged. A confirmed failed save exposes no created resource or timestamps.
- **DR-013**: Impossible dates, inconsistent totals, inactive or ineffective references,
  unsupported identification, and invalid draft relationships MUST be rejected without partial
  state. Payment effectiveness uses only `emissionDate` with inclusive boundaries.
- **DR-014**: RUC (`04`) uses `FORMAT_ONLY_NUMERIC_13` and is exactly 13 ASCII digits; identity
  card (`05`) uses `FORMAT_ONLY_NUMERIC_10` and is exactly 10 ASCII digits. Passport (`06`) and
  foreign identification (`08`) use `FORMAT_ONLY_ALPHANUMERIC_1_TO_20`, defined as case-sensitive ASCII
  `^[A-Za-z0-9]{1,20}$` after one surrounding SP/HTAB trim. No internal transformation,
  punctuation, space, diacritic, broader Unicode, checksum, registry, legacy, or invented
  country-specific rule is accepted. Valid examples: `A1234567`, `EC9Z`; invalid: `A-123`,
  `A 123`, `Á123`, empty, and 21 characters.
- **DR-015**: Final consumer (`07`) requires value `9999999999999`, name exactly
  `CONSUMIDOR FINAL`, and calculated total no greater than USD `50.00` under SRI Technical Sheet
  v2.32.
- **DR-016**: Line net and invoice total MAY be `0.00`; all amounts remain non-negative. Zero-price
  or zero-net lines keep the explicitly selected tax treatment. Deemed bases and special
  gratuitous-transfer treatments are unsupported.
- **DR-017**: A key becomes associated only with a successfully saved draft. Equivalent normalized
  content returns that draft; different content returns `IDEMPOTENCY_CONFLICT`. The association
  lasts as long as the draft and has no time-based expiry.
- **DR-018**: Idempotency scope is exactly canonical Company UUID plus normalized key. Neither value
  authenticates, authorizes, or proves entitlement.
- **DR-019**: General display text is normalized once to NFC, trimmed only of surrounding `U+0020`,
  checked against FR-035, and otherwise preserves internal punctuation, internal `U+0020` runs,
  and case. Code-point length is counted afterward. The normalized display value governs storage
  and idempotency comparison. For additional-information names, `canonicalName` is derived by:
  NFC; surrounding `U+0020` trim; collapse each internal `U+0020` run to one; deterministic
  locale-independent lowercase conversion; code-point count; rejection over 300 without
  truncation. Accepted canonical values govern name uniqueness and applicable idempotency
  comparison. Overflow returns `CANONICAL_NAME_TOO_LONG` with the original field, maximum `300`,
  counting unit `UNICODE_CODE_POINTS`, and stage `CANONICALIZATION`. ASCII fields retain their
  stricter rules. Buyer email then applies the exact ASCII profile below without case folding,
  additional trimming, or any other email-specific normalization.
- **DR-020**: Every draft has one immutable canonical Company UUID. Lines, payments, tax selections,
  tax totals, and additional information belong only to that draft; cross-Company mixing is
  prohibited.
- **DR-021**: Invoice-line order is significant for idempotency. Payment and
  additional-information order is insignificant.
- **DR-022**: Payments are unique by payment-method identity within a draft.
- **DR-023**: The opaque emission-point identifier is unverified input for later processing and is
  stored and returned only after the approved surrounding ASCII SP/HTAB trim, nonblank/non-nil UUID
  validation, and canonical lowercase hyphenated conversion. Invalid decoded strings produce safe
  `EMISSION_POINT_INVALID` evidence and no state. The identifier is not an Issuer, establishment,
  emission-point, or Company master-data snapshot or proof of a validated relationship.
- **DR-024**: An absent correlation value produces a safe UUID; one valid supplied value is
  preserved; invalid input is not echoed and produces a safe replacement. Correlation never affects
  idempotency equivalence.

### Executable ASCII Repertoires

| Field | Accepted repertoire and length | Comparison | Normalization | Valid examples | Invalid examples |
|-------|--------------------------------|------------|---------------|----------------|------------------|
| `productCode` | ASCII `^[A-Za-z0-9]{1,25}$` | Case-sensitive | Trim surrounding ASCII SP/HTAB once; change nothing else | `ABC123`, `sku9` | `ABC-123`, `ABC 123`, `ÁBC1`, empty, 26 characters |
| Buyer type `04` | ASCII `^[0-9]{13}$` | Exact | None beyond the approved surrounding-field handling | `1790012345001` | letters, spaces, punctuation, 12 or 14 digits |
| Buyer type `05` | ASCII `^[0-9]{10}$` | Exact | None beyond the approved surrounding-field handling | `0912345678` | letters, spaces, punctuation, 9 or 11 digits |
| Buyer type `06` or `08` | ASCII `^[A-Za-z0-9]{1,20}$` | Case-sensitive | Trim surrounding ASCII SP/HTAB once; change nothing else | `A1234567`, `EC9Z` | `A-123`, `A 123`, `Á123`, empty, 21 characters |

Surrounding SP or HTAB is accepted only because it is removed before validation. Internal SP or
HTAB remains invalid. Trim-to-empty and over-maximum normalized values are invalid. Directly stored
representations containing surrounding or internal whitespace are not approved representations.

### Executable Stage-5 Request Property Classification

For the templates below, `{i}` is any zero-based invoice-line array position. A property is
recognized by its exact case-sensitive path. The set is exhaustive:

| Recognized calculated path | Classification scope |
|----------------------------|----------------------|
| `/taxTotals` | The property and every descendant, regardless of supplied JSON type |
| `/subtotalBeforeTaxes` | Exact top-level property |
| `/totalDiscount` | Exact top-level property |
| `/grandTotal` | Exact top-level property |
| `/lines/{i}/grossAmount` | Exact line property |
| `/lines/{i}/netAmount` | Exact line property |
| `/lines/{i}/lineTotal` | Exact line property |
| `/lines/{i}/tax` | The property and every descendant, regardless of supplied JSON type |
| `/lines/{i}/taxBase` | Exact direct line property |
| `/lines/{i}/taxAmount` | Exact direct line property |
| `/lines/{i}/taxCode` | Exact direct line property |
| `/lines/{i}/taxRate` | Exact direct line property |
| `/lines/{i}/officialTaxCode` | Exact direct line property |
| `/lines/{i}/officialPercentageCode` | Exact direct line property |
| `/lines/{i}/rate` | Exact direct line property |

Stage 5 first determines whether the representation is a decodable JSON object. Malformed JSON,
unsupported media, or a non-object representation returns `INVALID_REQUEST`. For a decoded object,
the entire object is inspected before ordinary schema binding: if any recognized path exists,
`PROHIBITED_CALCULATED_FIELD` is returned even when its value is null, wrongly typed, or equal to a
service result. That result precedes ordinary unknown/prohibited properties, missing required
properties, and property-type errors. Multiple recognized paths produce the same single top-level
outcome. Its `violations` member is omitted and no supplied value is exposed. Only when no recognized
path exists may ordinary unknown/property/type classification produce `INVALID_REQUEST`.

### Executable General Text Policy and Vectors

| Vector | Observable normalized or canonical result | Accepted | Reason |
|--------|-------------------------------------------|----------|--------|
| `José Álvarez` in NFC | unchanged | Yes | Assigned letters and `U+0020` are allowed |
| `Jose\u0301` | `José` | Yes | Canonically equivalent input has the same NFC result |
| `  Acme  Uno  ` | display `Acme  Uno`; canonical `acme uno` | Yes | Surrounding spaces trim; display keeps internal run; canonical collapses it |
| `Acme\tUno` | none | No | Tab is prohibited |
| `Acme\rUno` or `Acme\nUno` | none | No | CR and LF are prohibited |
| `Acme\u00A0Uno` | none | No | NBSP is not `U+0020` |
| `Acme\u200BUno` | none | No | Zero-width category `Cf` is prohibited |
| `Acme\u2028Uno` or `Acme\u2029Uno` | none | No | Line and paragraph separators are prohibited |
| `Happy 😀` | unchanged | Yes when field limits allow | Assigned category `So` is not prohibited |
| `Acme` versus `ACME` display | preserved and distinct | Yes | Display comparison is case-sensitive; canonical forms are both `acme` |
| 150 occurrences of `U+0130` | canonical length 300 | Yes | Lowercase expansion reaches the inclusive maximum |
| 151 occurrences of `U+0130` | canonical length 302 | No | Returns `CANONICAL_NAME_TOO_LONG` without truncation |
| Exactly a field maximum | unchanged | Yes | Maximum is inclusive |
| Field maximum plus one code point | none | No | Code-point maximum exceeded |
| Empty or only `U+0020` | empty | No when required or supplied | Empty after approved trim |

### Executable Buyer Email Profile

After the one FR-035 general-text pass, a supplied buyer email MUST match this exact ASCII pattern:

```regex
^(?=.{1,254}$)(?=[^@]{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$
```

The accepted local part contains 1–64 ASCII letters, digits, or ``!#$%&'*+/=?^_`{|}~-``, with `.`
only between nonempty atoms. The domain contains at least two dot-separated labels; every label is
1–63 ASCII letters/digits/hyphens, begins and ends with a letter or digit, and the complete address
contains at most 254 ASCII code points. Exactly one `@` is present. Case is preserved and comparison
is case-sensitive. No additional normalization, lowercase conversion, or truncation occurs.

| Vector | Accepted | Observable result |
|--------|----------|-------------------|
| `buyer@example.com` | Yes | Preserved unchanged |
| `a@b.c` | Yes | Inclusive minimum: one-character local part and one-character domain labels |
| ``a!#$%&'*+/=?^_`{|}~-z@example.com`` | Yes | Every permitted local-part punctuation character is accepted inside one nonempty atom |
| ` O'Connor+tax@sub.example.ec ` | Yes | FR-035 produces `O'Connor+tax@sub.example.ec` |
| `Buyer@Example.COM` | Yes | Case preserved; distinct from a differently cased value |
| `@b.c`, `a@.c`, or `a@b..c` | No | Empty local part or domain label violates the inclusive minimum |
| `.buyer@example.com`, `buyer.@example.com`, or `buyer..name@example.com` | No | `EMAIL_INVALID` |
| `buyer@example`, `buyer@example.com.`, `buyer@-example.com`, or `buyer@example-.com` | No | `EMAIL_INVALID` |
| `"buyer"@example.com`, `buyer@[127.0.0.1]`, or `buyer(comment)@example.com` | No | `EMAIL_INVALID` |
| `buyer name@example.com` | No | Internal ASCII SP survives normalization and produces `EMAIL_INVALID` |
| Tab, CR, LF, NBSP, `U+2028`, `U+2029`, or zero-width `Cf` in the address | No | Rejected during Stage 6 before the email grammar runs |
| Empty or only surrounding `U+0020` | No | Rejected during Stage 6 as supplied optional text empty after normalization |
| `bu\u0301yer@example.com` | No | NFC produces non-ASCII `búyer@example.com`, then Stage 10 returns `EMAIL_INVALID` |
| `búyer@example.com` or `buyer@exámple.com` | No | `EMAIL_INVALID`; the email profile is ASCII |
| `buyer@example.com,other@example.com` | No | `EMAIL_INVALID`; multiple addresses are forbidden |
| Local part of 64 characters, one 63-character domain label, or total length 254 | Yes when the remaining grammar holds | Inclusive upper boundaries |
| Local part of 65 characters, one 64-character domain label, or total length 255 | No | `EMAIL_INVALID` |

An email-profile failure occurs during independent business validation after normalization and
returns top-level `BUSINESS_VALIDATION_FAILED` with exactly one safe violation: `code` is
`EMAIL_INVALID`, `field` is `buyer.email`, and the rejected value is absent.

## Key Entities

- **Invoice Draft**: Company-owned pre-issuance record with status `DRAFT`, USD currency, opaque
  emission-point reference, buyer, ordered lines, taxes, payments, additional information, totals,
  final identifier, and creation timestamps.
- **Buyer**: Recipient name, one approved identification type and value, and optional contact data.
- **Invoice Line**: Ordered product or service entry with code, description, quantity, unit price,
  discount, one IVA rule, and calculated amounts.
- **Tax Selection**: One approved active IVA rule effective on `emissionDate` and selected for one
  line.
- **Tax Total**: Calculated tax base and amount for one treatment-code and rate group.
- **Payment**: Exact two-decimal amount assigned to one approved effective payment method.
- **Additional Information**: Optional display name and value, with deterministic canonical name
  used for uniqueness.
- **Approved Identification Type**: Versioned SRI-backed buyer-identification rule.
- **Approved IVA Rule**: Versioned, effective, stable-identifier rule for one supported IVA
  treatment and calculation behavior.
- **Approved Payment Method**: Versioned, effective, stable-identifier payment reference.
- **Idempotency Association**: Company-and-key-scoped association between normalized request
  content and the successfully saved draft, retained for the draft lifetime.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every logically new valid request whose success becomes conclusive before deadline
  creates exactly one complete draft and idempotency association and returns `201` with every
  FR-022 field, including equal timestamps; no SRI issuance side effect occurs. Deadline first
  returns `REQUEST_TIMEOUT`.
- **SC-002**: Every confirmed rejection or completely reversed save leaves zero draft and
  idempotency state and leaves the key reusable. Unresolved or post-save delivery outcomes make no
  zero-state claim and are resolved by replay without duplication.
- **SC-003**: Identical commercial inputs, emission date, and reference versions always produce
  identical line values, grouped taxes, payment comparison, and totals.
- **SC-004**: Vector `2 × 10.00 − 5.00` with approved IVA 15% produces gross `20.00`, net `15.00`,
  tax `2.25`, and line total `17.25` without declaring 15% universal.
- **SC-005**: Every success and failure produces zero sequence reservations, access keys, XML,
  signatures, certificate reads, SRI calls, integration jobs, notifications, PDFs, and RIDE.
- **SC-006**: Every missing, repeated, blank, malformed, or nil Company-header vector returns the
  correct `400` Company error when conclusive before deadline, safe correlation, and zero created
  state. Deadline first returns `504`; an earlier Company result is not replaced.
- **SC-007**: Every accepted draft exposes enough captured and calculated information for review
  without fiscal issuance; initial timestamps are equal.
- **SC-008**: All acceptance scenarios can be validated independently of update, deletion, fiscal
  numbering, XML, signing, SRI authorization, PDF, and notification features.
- **SC-009**: Every accepted line has exactly one active effective IVA rule; unsupported or
  simultaneous taxes are rejected without created state.
- **SC-010**: Every accepted buyer identification satisfies its approved effective type rule;
  invalid or unsupported values are rejected without online registry calls or created state.
- **SC-011**: Every valid zero-total vector creates one complete draft with exactly one `0.00`
  payment; every invalid zero-total payment combination creates no state.
- **SC-012**: Every idempotency and concurrency vector creates at most one draft per scope and
  equivalent content. The complete header matrix returns the three FR-027 errors deterministically,
  never chooses a first ambiguous value, and uses the same one-time-trimmed value for the
  association. Replay returns the original draft and timestamps.
- **SC-013**: Every accepted new draft uses the one Ecuadorian date fixed at the initial service
  boundary before body consumption and retains it when body consumption or later work crosses
  midnight. Every successful creation returns equal UTC `createdAt` and
  `updatedAt`; equivalent replay returns those values unchanged; failed saves expose no created
  draft. No physical save-completion timestamp is part of the contract.
- **SC-014**: Every exhaustive recognized calculated-property path returns
  `PROHIBITED_CALCULATED_FIELD` and creates no state, including null, wrongly typed, equal-result,
  nested, and multiple-path vectors. For every well-formed mixed request, that result wins over
  ordinary unknown/prohibited properties and other Stage-5 structural failures; malformed JSON
  remains `INVALID_REQUEST` because it cannot be classified by property.
- **SC-015**: Valid drafts at 500 lines, 8 positive payments, and 15 additional-information entries
  are accepted; any request above a limit is rejected with no state.
- **SC-016**: Every approved Unicode vector produces its specified accepted or rejected result.
  Canonically equivalent input produces the same normalized value; prohibited whitespace and code
  points are rejected; assigned emoji is accepted where field limits allow; display case is
  preserved; canonical overflow returns `CANONICAL_NAME_TOO_LONG`; accepted values are never
  truncated; stricter ASCII fields follow their exact repertoires; and every buyer-email vector
  follows the exact post-normalization ASCII profile, preserves case, enforces 64/63/254 boundaries,
  and returns value-free `EMAIL_INVALID` when its email grammar fails.
- **SC-017**: Every accepted draft retains exactly one immutable canonical Company UUID, every child
  belongs to that draft, and no cross-Company child mixing occurs.
- **SC-018**: Every equivalent replay returns the original draft while it exists regardless of
  elapsed time, and no time-based expiration creates a duplicate.
- **SC-019**: Reordering only payments or additional information preserves equivalence; changing
  line order conflicts; neither creates a duplicate.
- **SC-020**: Every accepted draft has at most one payment per method. Payment vectors prove
  existence, activity, inclusive lower and upper boundaries, open-ended validity, and before/after
  rejection using only `emissionDate`.
- **SC-021**: Every accepted line derives tax code and rate from its selected approved rule; every
  request supplying those calculated/reference values directly is rejected with no state.
- **SC-022**: Every accepted draft has one external Company ownership identifier, one canonical
  opaque emission-point identifier, and zero fiscal master-data snapshots or observation metadata.
  Every approved emission-point vector yields the deterministic Stage 5 representation outcome or
  Stage 6 `EMISSION_POINT_INVALID` outcome, and an invalid vector saves no state.
- **SC-023**: Every create and replay vector makes zero Company Service calls, identity or
  authorization decisions, Company availability checks, Company cache access, or master-data
  replication.
- **SC-024**: The published contract forbids Company input in bodies, paths, and queries, requires
  exactly one UUID `X-Company-Id`, returns canonical `companyId`, and defines no security scheme,
  Authorization input, `401`, or `403`.
- **SC-025**: Every body containing `companyId`, `issuerId`, or Issuer, establishment, or
  emission-point fiscal attributes is rejected and creates no state.
- **SC-026**: Every controlled precedence and deadline vector returns exactly one deterministic
  response. Rules independent of calculated amounts precede calculation; calculated rules follow
  the exact FR-041 order; the first conclusive result before deadline wins; otherwise timeout wins;
  late outcomes never replace an already returned response.
- **SC-027**: A body of at most `2,097,152` bytes proceeds to the next applicable validation; a
  larger body conclusively detected first returns correlated `413` before Company validation and
  creates no state. Deadline-first slow-body vectors return `504`.
- **SC-028**: Confirmed pre-save or completely reversed failures leave zero state. Unresolved or
  post-save response failures make no zero-state claim and replay recovers the one authoritative
  draft. Expiry after response delivery produces no second response or compensation.
- **SC-029**: Requests, calculations, grouped results, payment sums, saved values, and responses all
  enforce the same numeric envelopes. Every breach returns `MONETARY_RANGE_EXCEEDED` and creates no
  state.
- **SC-030**: Equivalent replay on a later Ecuadorian date returns the original draft and emission
  date unchanged.
- **SC-031**: Every supported identification, IVA, and payment entry is approved, versioned, and
  traceable to official evidence. Unsupported or unverified entries are unavailable.
- **SC-032**: Every accepted `taxRuleId` and `paymentMethodId` resolves to exactly one approved
  active and effective entry; unsupported, inactive, or ineffective identifiers are rejected;
  stable identifiers remain unchanged across equivalent executions; no catalog query is exposed.
- **SC-033**: Correlation vectors generate a UUID when absent, preserve one valid value, never echo
  invalid input, and return a safe replacement when needed. Combined failures follow FR-041, and
  changing correlation never changes idempotency equivalence.

## Feature Definition of Done

The feature is complete only when:

- Every required acceptance scenario and measurable success criterion passes.
- Every accepted request creates and returns exactly one complete draft with all required values.
- Every rejected request and every confirmed failed all-or-nothing save leaves no unintended state.
- Company context comes only from `X-Company-Id`, cross-Company access is prevented, and shared SRI
  reference catalogs remain outside Company ownership.
- Equivalent retries, conflicts, concurrency, unresolved outcomes, and replay satisfy the approved
  idempotency behavior.
- Text, numeric, date, tax, payment, reference, error, deadline, and timestamp outcomes match this
  specification.
- No excluded identity, Company-master-data, fiscal-issuance, SRI, certificate, XML, PDF, or
  notification behavior occurs.
- Every supported reference entry remains approved, versioned, stable, and traceable to official
  evidence.

## Assumptions and Dependencies

- **Assumption**: Buyer address, email, and telephone are optional; buyer name and identification
  are mandatory.
- **Assumption**: `DRAFT` has no fiscal validity.
- **Assumption**: Creation is a directly observable request/response operation, not a later SRI
  command.
- **Assumption**: Before reaching this internal service, an upstream component has performed any
  required authentication, authorization, Company validation, and trusted-header replacement. This
  service does not verify those upstream actions.
- **Assumption**: Any process that reaches this service can submit any syntactically valid non-nil
  Company UUID; the service accepts it without entitlement or Company-state verification.
- **Dependency**: Draft creation has no runtime dependency on Company Service, identity,
  authorization, gateway, BFF, or Company master data.
- **Dependency**: Identification, tax, and payment references use the exact approved
  `SRI-OFFLINE-2.32-TARGET-1` entries and stable identifiers.
- **Dependency**: Callers know approved tax-rule and payment-method identifiers through the
  published integration contract because this feature exposes no catalog query.
- **Dependency**: The IANA `America/Guayaquil` definition governs conversion of request-entry time
  to the expected emission date.
- **Dependency**: Approved English terminology governs the feature's canonical names.
