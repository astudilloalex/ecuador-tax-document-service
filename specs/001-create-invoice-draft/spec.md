# Feature Specification: Create Invoice Draft

**Feature Branch**: `6-ft-1`

**Created**: 2026-07-12

**Status**: Approved for Task Generation

**Input**: User description: "Create and persist a complete Company-scoped electronic invoice draft
for review before any fiscal identifier allocation or SRI interaction."

**Approval Note**: Constitution v2.0.0 is approved on `main` and `origin/main`; the requirements
checklist is complete; the reference-data baseline is approved; no material Pending Functional
Validation remains; and this feature may proceed to task generation. Post-generation analysis
remediation aligns the positive-payment capacity with the eight approved unique methods and
clarifies that IVA category is the rule's immutable family rather than a separate entity.

## Clarifications

### Session 2026-07-12

- Q: What numeric envelope governs quantities, prices, money, and rates? → A: Quantity and unit
  price use at most six fractional digits and range from `0` through `999999.999999`, with quantity
  strictly greater than zero; every monetary value/result ranges from `0.00` through
  `999999999999999.99`; rates range from `0.00` through `100.00`. Persistence uses
  `numeric(12,6)`, `numeric(17,2)`, and `numeric(5,2)` respectively. Any input, intermediate,
  rounded, grouped, payment-sum, or invoice-total overflow produces `BUSINESS_VALIDATION_FAILED`
  with violation `MONETARY_RANGE_EXCEEDED` before persistence.
- Q: Which instant determines the accepted Ecuador emission date? → A: Capture
  `requestCreationInstant` once at the request boundary and derive the expected date in
  `America/Guayaquil`. That date remains fixed across midnight and commit; `createdAt` is the
  confirmed commit instant, and equivalent replay returns the original draft without current-date
  revalidation.
- Q: What reference-data baseline is required before tasks? → A: Identification types, IVA rules,
  and payment methods use the approved `SRI-OFFLINE-2.32-TARGET-1` baseline in
  `reference-data-baseline.md`. Tax-rule and payment-method identifiers are deterministic UUIDv5
  values published by the target contract and later seeded by Flyway, never startup-generated.
- Q: How is `X-Correlation-Id` initialized and validated? → A: Initialize correlation at the HTTP
  boundary; generate a safe UUID when absent, preserve one valid supplied identifier, and replace
  blank, repeated, over-length, or unsafe input with a safe UUID for the `INVALID_REQUEST` response
  without echoing the invalid value. Correlation validation follows Company validation and precedes
  idempotency-key validation; correlation never affects idempotency.
- Q: What is the definitive Company context, identity, Company-dependency, and draft-snapshot model?
  → A: Every request supplies exactly one nonblank, syntactically valid non-nil Company UUID in
  `X-Company-Id`; the service normalizes it to canonical lowercase hyphenated form, performs no
  authentication, authorization, tenant, or Company lookup, calls no Company capability, and
  stores no Company/Issuer/establishment/emission-point snapshot. CompanyId is prohibited from the
  path, query, and request body; idempotency is scoped by normalized CompanyId plus key; fiscal
  context is deferred to a later issuance specification.
- Q: Which tax scope and per-line tax cardinality should invoice drafts support? → A: The IVA tax
  family only, with exactly one active, emission-date-effective tax treatment per line: configured
  percentage-rate IVA, IVA 0%, not subject to IVA, or exempt from IVA. The three zero-tax
  treatments remain distinct; all other or simultaneous taxes are unsupported.
- Q: Which buyer identification types and validation authority should draft creation support? → A:
  Apply the versioned SRI rules effective on the emission date for RUC (`04`), Ecuadorian identity
  card (`05`), passport (`06`), final consumer (`07`), and foreign identification (`08`), with no
  invented or legacy rules and no online registry or buyer-name verification.
- Q: May a valid invoice draft have a rounded grand total of `0.00`? → A: Yes. Zero-value drafts
  remain subject to all line and tax rules and require exactly one active payment method with
  amount `0.00`; positive-total drafts require every payment amount to be greater than `0.00`.
- Q: How must duplicate and concurrent draft-creation commands behave? → A: Every command requires
  a caller-generated idempotency key scoped by normalized Company UUID plus key. Equivalent retries
  return the original committed draft; different content conflicts; concurrent equivalent commands
  create exactly one draft; failed or rolled-back commands do not bind the key.
- Q: How should draft creation handle client-supplied calculated monetary fields? → A: Reject every
  request containing a system-calculated input field; such values are never ignored, compared, or
  persisted.
- Q: What maximum collection counts should one draft allow? → A: At most 500 invoice lines, 8
  payments, and 15 additional-information entries.
- Q: What text-boundary policy should draft inputs use? → A: SRI-aligned bounded text: 25-character
  product codes; 300-character descriptions, buyer names, addresses, and additional-information
  names/values; 254-character email; 20-character telephone; and 128-character idempotency keys,
  with explicit format, trimming, uniqueness, and control-character rules.
- Q: What Company-to-Issuer relationship does invoice-draft creation validate? → A: None. The
  service treats Company and emission-point identifiers as opaque inputs; authoritative fiscal
  relationships are outside draft creation and belong to a separately approved issuance workflow.
- Q: How long must a successful company-scoped idempotency binding remain valid? → A: For the
  lifetime of the created draft; the binding must not expire on a time-based schedule.
- Q: Which collection order should affect idempotency equivalence? → A: Invoice-line order only.
  Payment and additional-information ordering are representation-only differences.
- Q: May the same payment method appear more than once in a positive-total draft? → A: No. Each
  active payment method may appear at most once per draft.

## Scope and Evidence *(mandatory)*

### Bounded Outcome

An internal billing client can create one complete invoice draft scoped by the opaque Company UUID
supplied in `X-Company-Id` and can review the persisted emission-point reference, buyer, line, tax,
payment, and system-calculated total information without resolving fiscal master data, creating an
issued electronic invoice, or triggering any SRI side effect.

### In Scope

- Receive Company context exclusively through `X-Company-Id`, receive idempotency through
  `Idempotency-Key`, and create a new invoice draft through `POST /invoice-drafts` under the
  project's API base and version prefix.
- Create the draft for one canonicalized Company UUID and one client-supplied opaque
  emission-point external identifier.
- Capture buyer identity and optional contact information.
- Capture one or more invoice lines, exactly one applicable active IVA tax treatment per line, one
  or more payments, and optional additional information.
- Calculate line amounts, tax bases, tax amounts, grouped tax totals, discounts, and invoice totals.
- Validate the Company header contract, active tax-document reference data, buyer identification,
  monetary boundaries, and payment reconciliation before persistence.
- Persist the complete draft with internal status `DRAFT` and return its captured and calculated
  information, identifier, and timestamps.
- Scope ownership, repository access, mutations, and idempotency by the normalized Company UUID.
- Reject CompanyId in the resource path, query string, or request body; strict request-body
  validation treats it as an unknown or prohibited property.

### Exclusions and Non-Goals

- Updating, deleting, or cancelling an invoice draft.
- Submitting an invoice for SRI reception or authorization.
- Reserving or assigning an official invoice sequential number.
- Generating an SRI access key or numeric code.
- Generating, validating, storing, or signing invoice XML.
- Communicating with the SRI or observing an SRI authorization result.
- Performing an online SRI registry existence check for a buyer or verifying the buyer name against
  an external registry.
- Requiring, loading, validating, or managing a digital certificate.
- Generating a PDF or RIDE representation.
- Sending email, webhook, queue, or other notification messages.
- Creating credit notes, debit notes, withholding documents, delivery notes, or any other tax
  document type.
- Applying ICE, IRBPNR, any non-IVA tax, or multiple simultaneous taxes to one invoice line.
- Applying a deemed taxable base or special gratuitous-transfer treatment not defined by this
  specification.
- Managing loyalty-point balances, authorizing point redemption, or validating loyalty accounts.
- Owning, administering, searching, exposing, caching, or replicating current Company, Issuer,
  establishment, or emission-point master data.
- Calling the Company capability or validating Company existence, state, fiscal eligibility,
  tenant membership, caller entitlement, or Company/Issuer/establishment/emission-point
  relationships.
- Authentication, authorization, user or permission management, tenant authorization, API gateway,
  or BFF responsibilities.
- Persisting Company-context versions or observation timestamps, Issuer fiscal snapshots,
  establishment snapshots, or emission-point fiscal snapshots during draft creation.
- Preserving any legacy NestJS route, payload, response, table, status, authentication mechanism,
  asynchronous behavior, or operational behavior.
- Defining the later transition from `DRAFT` to a fiscally issued document.

### Authority and Evidence

| Authority | Source and version/path | Relevance to this feature |
|-----------|-------------------------|---------------------------|
| Applicable Ecuadorian legislation | [Regulation for Sales, Withholding, and Complementary Documents, SRI consolidated copy dated 2023-12-29](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/9fb49475-f058-49a1-b08a-f31bf4deb074/Reglamento_Comprobantes_Venta_RetencionYDC_29122023.pdf), together with [later amendments listed by the SRI](https://www.sri.gob.ec/facturacion-electronica) | Establishes invoices as sales documents and governs issuer responsibility and required invoice information. This feature creates only an internal pre-issuance draft. |
| Official SRI technical documentation | [Electronic Tax Documents Offline Scheme Technical Sheet v2.32, updated 2025-10-08](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf) and the [SRI electronic invoicing page](https://www.sri.gob.ec/facturacion-electronica), which lists invoice XSD/XML versions 1.0.0 through 2.1.0 updated in 2022-02 | Governs official catalogs, invoice fields, tax and payment representations, and the later generation/authorization process. XML generation and XSD selection are excluded here. |
| Project constitution | `.specify/memory/constitution.md` v2.0.0 | Governs authority, target-first scope, `X-Company-Id`, caller-agnostic Company scoping, Company master-data exclusion, fiscal-stage separation, atomic persistence, testing, and workflow. |
| Approved target requirements | This specification and its recorded clarification decisions | Governs the internal `DRAFT` behavior and the explicit absence of fiscal side effects. |
| Legacy evidence | `docs/legacy/as-is/04-data-model.md`; `docs/legacy/as-is/05-business-rules.md`; `docs/legacy/as-is/06-validation-rules.md`; `docs/legacy/as-is/07-process-flows.md`; `docs/legacy/as-is/10-security-access-control.md`; `docs/legacy/as-is/14-pending-functional-validation.md` | Supplies historical scenarios for invoice graphs, calculations, validation gaps, persistence, side effects, and tenant risks. It is not target authority. |

**Source Conflicts**:

- Legacy evidence BR-002, BR-006, BR-007, VR-009, VR-020, and legacy PFV-021 shows that historical
  processing accepted tax rates, bases, values, and payment totals from the client. The approved
  target requirement governs: the service calculates and reconciles these values.
- Legacy evidence BR-001, SEC-002, VR-025, and legacy PFV-036 documents RUC-based selection and
  application-level access behavior. The constitution and this specification govern: one opaque
  Company UUID from `X-Company-Id` scopes the draft without authentication, authorization, tenant
  resolution, or Company lookup.
- The legacy invoice flow reserved a sequence, generated an access key and XML, signed it, called
  the SRI, and then persisted results. This target feature explicitly stops before all those
  activities.
- Official SRI electronic-document statuses do not include `DRAFT`. Here, `DRAFT` is an internal
  target-domain state and MUST NOT be presented as an SRI status, issued invoice, or tax-valid
  electronic document.
- The current SRI IVA guidance observed on 2026-07-12 identifies 13% for general goods and services,
  while Circular NAC-DGECCGC25-00000006 and Circular NAC-DGECCGC26-00000005 provide official 15%
  applicability evidence. The approved target baseline retains both representation rules with
  explicit applicability notes. Neither is labeled universal, and the upstream billing workflow,
  not this feature, selects the rule appropriate to the product or transaction.

**Approved Reference Data**:

- `reference-data-baseline.md` approves five buyer-identification types, six IVA tax rules, and
  eight payment methods under catalog version `SRI-OFFLINE-2.32-TARGET-1`.
- Buyer-identification types use official codes `04` through `08`. Tax and payment references use
  the exact deterministic UUIDv5 values published in that baseline.
- Buyer validation is deliberately `FORMAT_ONLY` for RUC, Ecuadorian identity card, passport, and
  foreign identification because no exact governing checksum algorithm was established from the
  approved primary sources for this draft feature. This is an approved scope boundary, not an
  inferred fiscal algorithm or hidden implementation task.
- The target adoption date is 2026-07-12. It is target-service catalog metadata and MUST NOT be
  attributed to the SRI as a legal-origin date.

**Terminology Mapping**: The approved terms `Invoice Draft`, `Invoice`, `Company`, `Issuer`,
`Establishment`, `Emission Point`, `Fiscal Context Snapshot`, `Buyer`, `Invoice Line`, `Tax
Category`, `Tax Rate`, `Tax Rule`, `Payment Method`, `Payment`, `Additional Information`, and
`Idempotency Key` are recorded in
`docs/migration/terminology-mapping.md`. Exact official terms such as `RUC`, SRI catalog codes,
`Access Key`, and `Official Sequential Number` retain their mapped scope.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create and Review an Invoice Draft (Priority: P1)

As an internal billing client, I want to create an invoice draft under a supplied Company context,
so that commercial, buyer, tax, payment, and calculated information can be reviewed before fiscal
issuance.

The internal billing client has no identity, role, permission, tenant, or authentication state
inside this feature.

**Why this priority**: It is the smallest independently valuable billing outcome. It captures and
validates invoice intent while isolating later fiscal numbering, XML, signature, and SRI risks.

**Independent Test**: Supply one valid `X-Company-Id`, an opaque emission-point identifier, valid
buyer data, one or more valid lines, active tax and payment references, and matching payment
amounts. The returned persisted draft can be reviewed in full, while Company/Issuer fiscal
snapshots, sequence, access-key, XML, certificate, notification, and SRI evidence remain absent.

**Acceptance Scenarios**:

1. **Given** exactly one syntactically valid non-nil Company UUID in `X-Company-Id`, one opaque
   emission-point identifier, valid buyer data, at least one valid line, and payments equal to the
   calculated grand total, **when** the internal billing client creates the draft, **then** exactly
   one complete draft is persisted with status `DRAFT`, the normalized Company UUID, a unique draft identifier,
   calculated amounts, and creation and last-modification timestamps.
2. **Given** a line with quantity `2`, unit price `10.00`, discount `5.00`, and the approved IVA
   15% rule selected solely as a mathematical rounding vector, **when** the draft is calculated,
   **then** gross amount is `20.00`, net amount is `15.00`, tax is `2.25`, and that line contributes
   `17.25` to the grand total; this scenario does not state that 15% is universally applicable.
3. **Given** a line with gross amount `20.00` and discount `21.00`, **when** draft creation is
   attempted, **then** a business validation error is returned and no draft or child data is
   persisted.
4. **Given** otherwise valid data with no invoice lines, **when** draft creation is attempted,
   **then** the request is rejected and no draft is persisted.
5. **Given** a calculated grand total of `100.00` and payments totaling `90.00`, **when** draft
   creation is attempted, **then** a payment-total validation error is returned and no draft is
   persisted.
6. **Given** `X-Company-Id` is missing or contains no usable value after trimming, **when** draft
   creation is attempted, **then** `COMPANY_CONTEXT_REQUIRED` is returned in a safe English error
   response with a correlation identifier and no draft, child, or idempotency binding is persisted.
7. **Given** buyer identification that fails the approved rule for its active identification type,
   **when** draft creation is attempted, **then** a buyer-identification validation error is
   returned and no draft is persisted.
8. **Given** a valid draft request, **when** creation succeeds, **then** no official sequential
   number is reserved, no access key is generated, no XML is generated or signed, no certificate
   is required, no notification is sent, and no SRI communication occurs.
9. **Given** an inactive buyer identification type, IVA tax rule, or payment method,
   **when** draft creation is attempted, **then** the request is rejected and no draft is persisted.
10. **Given** a request body supplies `companyId`, **when** strict
    request-body validation is performed, **then** the property is rejected as unknown or
    prohibited and no draft, child, or idempotency binding is persisted.
11. **Given** a persistence failure after validation begins, **when** draft creation cannot finish,
    **then** the internal billing client receives a safe failure outcome and no partial draft, line,
    tax, payment,
    or additional-information data remains persisted.
12. **Given** a line selects ICE, IRBPNR, another non-IVA tax, no tax treatment, or more than one
    simultaneous tax treatment, **when** draft creation is attempted, **then** the request is
    rejected as unsupported and no draft is persisted.
13. **Given** a buyer identification uses code `04`, `05`, `06`, or `08`, **when** draft creation is
    attempted, **then** it is accepted only when it satisfies that row's approved `FORMAT_ONLY`
    syntax and length in `SRI-OFFLINE-2.32-TARGET-1`.
14. **Given** a syntactically valid RUC or Ecuadorian identity-card value, **when** draft creation is
    attempted, **then** the draft is not rejected by any checksum or legacy validation algorithm,
    because checksum validation is outside this draft feature.
15. **Given** identification type `07`, value `9999999999999`, buyer name `CONSUMIDOR FINAL`, and a
    grand total at or below the effective SRI final-consumer threshold, **when** draft creation is
    attempted, **then** the final-consumer identification is valid; **and given** any mismatch in
    that type, value, or name, or a grand total above the threshold, **then** the request is rejected
    and no draft is persisted.
16. **Given** an unknown, inactive, not-yet-effective, expired, or unresolved identification type,
    **when** draft creation is attempted, **then** the request is rejected and no draft is
    persisted.
17. **Given** at least one valid line, explicitly selected applicable tax treatment on every line,
    a rounded grand total of `0.00`, and exactly one active payment method with amount `0.00`,
    **when** draft creation is attempted, **then** the complete zero-value draft is accepted and
    persisted.
18. **Given** a rounded grand total of `0.00`, **when** the request contains no payment, more than
    one payment, or a payment amount other than `0.00`, **then** the request is rejected and no
    draft is persisted.
19. **Given** a rounded grand total greater than `0.00`, **when** any payment amount is `0.00`,
    **then** the request is rejected and no draft is persisted.
20. **Given** a successfully committed creation command, **when** the same Company UUID,
    idempotency key, and semantically equivalent normalized business content are submitted
    again through the current `X-Company-Id` header, **then** the original draft is returned and no
    new draft is created or modified.
21. **Given** a scoped idempotency key already bound by a successful command, **when** the same
    Company UUID and key are submitted with different business content, **then** a stable
    `IDEMPOTENCY_CONFLICT` error is returned and no draft is created or modified.
22. **Given** concurrent equivalent commands for the same Company UUID and idempotency key,
    **when** they are processed, **then** exactly one draft is committed and every successful
    outcome resolves to that same persisted draft.
23. **Given** a command fails Company-header validation, business validation, or a persistence
    operation that fully rolls back, **when** the key is retried, **then** the prior failure has not
    bound the scoped idempotency key.
24. **Given** a draft committed successfully but its response was not delivered, **when** the same
    Company UUID, key, and equivalent content are retried, **then** the committed binding returns
    the existing draft without calling a Company capability or executing creation again.
25. **Given** the same idempotency key is used with two different Company UUIDs, **when** each
    command has equivalent business content and is otherwise valid, **then** the scopes are
    independent, separate drafts MAY be created, and neither Company deduplicates against the
    other's draft.
26. **Given** an emission date equal to the Ecuadorian civil date derived from the one captured
    `requestCreationInstant`, **when** all other draft data is valid, **then** the emission date is
    accepted as a date-only value.
27. **Given** an emission date before or after the Ecuadorian civil date derived from the one
    captured `requestCreationInstant`, **when** draft creation is attempted, **then** the request is
    rejected and no draft is persisted.
28. **Given** a creation request containing any client-supplied line gross amount, line net amount,
    tax base, tax amount, grouped tax total, subtotal before taxes, total discount, or grand total,
    **when** draft creation is attempted, **then** a stable calculated-field validation error is
    returned and no draft or child data is persisted.
29. **Given** otherwise valid data containing exactly 500 invoice lines, 8 positive payments, and
    15 additional-information entries, **when** the payment sum matches the positive grand total,
    **then** the collection counts are accepted.
30. **Given** a request containing more than 500 invoice lines, more than 8 payments, or more than
    15 additional-information entries, **when** draft creation is attempted, **then** the request is
    rejected and no draft or child data is persisted.
31. **Given** otherwise valid text values exactly at their approved maximum lengths and formats,
    **when** draft creation is attempted, **then** the text values are accepted after required
    leading and trailing whitespace removal.
32. **Given** a product code, description, buyer name, contact field, additional-information name or
    value, or idempotency key exceeding its maximum length, **when** draft creation is attempted,
    **then** a stable text-validation error is returned and no draft or child data is persisted.
33. **Given** a required text value that is blank after trimming, text containing a control
    character, an invalid email or telephone, or a duplicate additional-information name after
    trimming, **when** draft creation is attempted, **then** the request is rejected and no draft or
    child data is persisted.
34. **Given** a valid Company header and opaque emission-point identifier, **when** otherwise valid
    draft creation succeeds, **then** the draft is owned by that immutable Company UUID and retains
    the emission-point identifier without resolving Company fiscal context.
35. **Given** a syntactically valid Company UUID that is nonexistent, inactive, ineligible, or not
    permitted for the reachable process, **when** the request is otherwise valid, **then** this
    service performs no external lookup, does not reject those external conditions, and creates the
    draft under that Company UUID when all local draft rules pass.
36. **Given** a draft and its scoped idempotency binding still exist, regardless of elapsed time,
    **when** an equivalent retry uses that Company UUID and key, **then** the original draft is returned
    and no new draft is created.
37. **Given** a retry uses the same scoped key and business content but reorders only
    payments or additional-information entries, **when** idempotency equivalence is evaluated,
    **then** the original draft is returned and no new draft is created.
38. **Given** a retry uses the same scoped key but changes the invoice-line order,
    **when** idempotency equivalence is evaluated, **then** an idempotency conflict is returned and
    no draft is created or modified.
39. **Given** a positive-total draft contains two or more payments selecting the same payment
    method, **when** draft creation is attempted, **then** a stable duplicate-payment-method error
    is returned and no draft or child data is persisted.
40. **Given** the upstream billing workflow selects an approved active tax rule effective on the
    emission date, **when** draft creation is attempted, **then** the service uses that rule without
    classifying the product or deciding its legal tax eligibility; **and given** a line supplies a
    tax code or rate instead of a `taxRuleId`, **then** the request is rejected and no draft or child
    data is persisted.
41. **Given** a valid logically new Company-scoped command, **when** creation is attempted, **then**
    the service uses the supplied normalized Company UUID without calling a Company capability and
    persists no Company-context version, observation time, or fiscal master-data snapshot; it also
    reserves no official sequence, generates no access key, XML, signature, or certificate
    operation, and performs no SRI communication.
42. **Given** an existing committed draft and equivalent Company-scoped command, **when** Company,
    Issuer, establishment, or emission-point master data has changed elsewhere, **then** replay
    returns the original draft without a Company call or mutation.
43. **Given** the Create Invoice Draft boundary is reviewed, **then** it contains no Company client,
    port, repository, entity, table, cache, replication, direct database access, dependency health
    check, authentication, authorization, tenant, user, role, or permission requirement.
44. **Given** a request supplies `issuerId`, Issuer RUC, legal name, trade name, address, another
    Issuer fiscal attribute, establishment fiscal data, or an emission-point fiscal snapshot,
    **when** strict request-body validation is performed, **then** every such property is rejected as
    unknown or prohibited and no draft, child, or idempotency binding is persisted.
45. **Given** the request body exceeds `2 MiB` (`2,097,152` bytes), **when** the create operation
    receives it, **then** the request is rejected with a stable safe payload-size error before
    Company-header evaluation and no draft, child, or idempotency binding is persisted.
46. **Given** local persistence times out or fails unexpectedly before a successful commit,
    **when** draft creation terminates, **then** a stable safe error with a correlation identifier is
    returned and no draft, child, or idempotency binding remains persisted.
47. **Given** `X-Company-Id` is malformed or is the nil UUID, **when** draft creation is attempted,
    **then** `COMPANY_CONTEXT_INVALID` is returned in a safe English error response with a
    correlation identifier and no draft, child, or idempotency binding is persisted.
48. **Given** multiple `X-Company-Id` values are supplied, **when** draft creation is attempted,
    **then** `COMPANY_CONTEXT_INVALID` rejects the ambiguous context in a safe English error response
    with a correlation identifier and no draft, child, or idempotency binding is persisted.
49. **Given** quantity is `0.000001` or `999999.999999`, unit price is `0` or
    `999999.999999`, and every resulting money value remains within the approved monetary range,
    **when** the draft is validated and calculated, **then** those inclusive boundary values are
    accepted without precision loss.
50. **Given** any quantity or unit price exceeds its limit, or any monetary input, exact
    intermediate result, rounded line result, grouped amount, payment sum, subtotal, discount total,
    tax amount, or grand total falls outside `0.00` through `999999999999999.99`, **when** draft
    creation is attempted, **then** `BUSINESS_VALIDATION_FAILED` contains violation code
    `MONETARY_RANGE_EXCEEDED` and no draft, child, or idempotency binding is persisted.
51. **Given** `requestCreationInstant` is captured immediately before midnight in
    `America/Guayaquil` and the supplied emission date equals the date derived from that instant,
    **when** validation succeeds before midnight but commit completes after midnight, **then** the
    original derived emission date remains accepted and `createdAt` records the later confirmed
    commit instant.
52. **Given** a committed draft is replayed on a later Ecuadorian civil date with the same Company,
    key, and equivalent content, **when** the binding is resolved, **then** the original draft and
    emission date are returned without revalidating the emission date against the replay date.
53. **Given** any identification-type, IVA-rule, or payment-method baseline row lacks verified
    official evidence or an approved target mapping, **when** pre-task readiness is evaluated,
    **then** the row remains Pending Functional Validation and `$speckit-tasks` is blocked.
54. **Given** an approved baseline publishes stable fixed `taxRuleId` and `paymentMethodId` UUIDs,
    **when** a client submits those identifiers, **then** the applicable effective rows can be
    selected without a catalog-query endpoint and no identifier is generated at application
    startup.
55. **Given** `X-Correlation-Id` is absent and Company context is valid, **when** request processing
    begins, **then** one safe UUID correlation identifier is generated and returned on the terminal
    response.
56. **Given** exactly one safe supplied `X-Correlation-Id` satisfies the approved character and
    length rules, **when** request processing begins, **then** that identifier is preserved and
    returned unchanged.
57. **Given** `X-Correlation-Id` is blank, repeated, longer than 64 characters, or contains an
    unsafe character, and Company context is valid, **when** correlation validation is reached,
    **then** the invalid value is never echoed, a safe replacement UUID is returned with
    `INVALID_REQUEST`, and no draft, child, or idempotency binding is persisted.
58. **Given** both Company context and the supplied correlation identifier are invalid, **when**
    failure precedence is evaluated, **then** the applicable Company-context error is returned,
    correlation input is not echoed, and a safe replacement UUID correlates the error response.

### Edge Cases

- Uppercase or noncanonical but syntactically valid UUID text in the single `X-Company-Id` value
  MUST be normalized to canonical lowercase hyphenated form before ownership, persistence,
  response, or idempotency scoping. Nil UUID text MUST be rejected rather than normalized into an
  accepted CompanyId.
- Failure evaluation MUST follow FR-041. In particular, an oversized payload MUST be rejected
  before Company-header evaluation; invalid Company context MUST take precedence over invalid
  correlation; correlation validation MUST take precedence over idempotency-key and body-field
  validation; and an existing equivalent local binding MUST return the original result before
  current business rules are reevaluated.
- Correlation initialization MUST always produce a safe response identifier. If both Company
  context and correlation input are invalid, the Company error governs, the unsafe correlation is
  not echoed, and the replacement identifier correlates the response.
- An impossible calendar date and an emission date different from the date derived once from
  `requestCreationInstant` in `America/Guayaquil` MUST be rejected rather than normalized. Crossing
  midnight after that instant MUST NOT change the expected date, and replay MUST NOT reevaluate it.
- Quantity zero, negative, or greater than `999999.999999`; unit price negative or greater than
  `999999.999999`; and negative discount MUST be rejected.
- Every money-bearing input, intermediate result, rounded line result, grouped amount, payment sum,
  subtotal, discount total, tax amount, and grand total MUST remain within `0.00` through
  `999999999999999.99`. A range failure MUST NOT be saturated, truncated, wrapped, or deferred to a
  persistence error.
- A discount equal to gross amount MAY produce a zero net line, and all lines MAY produce a rounded
  grand total of `0.00`; neither outcome alone is a validation failure.
- Quantity or unit-price input with more than six decimal places, or discount/payment input not
  expressed to two decimal places, MUST be rejected; input precision MUST NOT be silently rounded.
- A caller MUST select an effective tax rule and MUST NOT supply a tax code or rate as line input.
  A catalog tax rule whose configured rate exceeds two decimal places MUST be rejected as invalid
  reference data rather than rounded.
- Every line MUST select exactly one active tax rule whose immutable family is `IVA`, and the
  rule's effective period MUST include the emission date. There is no separate parent tax-category
  lifecycle or reference entity in this feature.
- Configured percentage-rate IVA, IVA 0%, not subject to IVA, and exempt from IVA MUST be supported.
  IVA 0%, not subject to IVA, and exempt from IVA MUST remain distinct treatments and tax-total
  groups even though each produces a zero tax amount.
- ICE, IRBPNR, every other non-IVA tax, and every line requiring multiple simultaneous taxes MUST be
  rejected as unsupported.
- A zero-priced or zero-net line MUST retain its explicitly selected applicable tax treatment and
  MUST NOT be classified automatically as IVA 0%. A transaction requiring a deemed taxable base or
  special gratuitous-transfer treatment MUST be rejected as unsupported.
- If reference data becomes invalid before the draft is committed, the operation MUST reject the
  request or commit a draft based on one consistent validated reference-data state; it MUST NOT
  persist a partially validated mix.
- A reference row without verified official evidence and an approved target mapping MUST remain
  Pending Functional Validation. Placeholder or randomly generated startup UUIDs MUST NOT be used
  to bypass the pre-task baseline gate.
- Multiple payments that differ from the rounded grand total by any non-zero amount MUST be
  rejected. Payment comparison MUST use exact two-decimal values after line-level `HALF_UP`
  calculation and aggregation.
- A payment method MUST NOT appear in more than one payment entry within the same draft.
- Representation-only differences, including JSON property ordering and the ordering of payments
  or additional-information entries, MUST NOT make otherwise equivalent creation commands
  different for idempotency purposes. Invoice-line order is business-significant and MUST be
  preserved.
- A committed replay MUST be scoped by the normalized `X-Company-Id` value and idempotency key and
  MUST return only the original draft in that exact scope. The service MUST NOT perform caller
  authorization or contact a Company capability during the replay.
- A syntactically valid Company UUID MUST be accepted as opaque business context without checking
  Company existence, status, fiscal eligibility, tenant ownership, or caller entitlement.
- The selected emission-point external identifier MUST be stored as opaque draft input. Draft
  creation MUST NOT resolve an Issuer, establishment, emission point, or fiscal snapshot.
- A successful idempotency binding MUST NOT expire because a time interval elapsed while its draft
  still exists.
- A missing, repeated, blank, malformed, or nil `X-Company-Id` value MUST be rejected before draft
  persistence or idempotency binding. Company identifiers in a path, query, body, token, or session
  MUST NOT substitute for the required header.
- A syntactically valid non-nil Company UUID MUST remain acceptable even when an external Company
  system would consider it unknown, inactive, nonexistent, ineligible, or unauthorized; no external
  check exists in this feature.
- A request containing any system-calculated monetary field MUST be rejected. The supplied value
  MUST NOT be ignored, compared with a calculated result, or persisted.
- Identification code `05` MUST contain exactly 10 ASCII digits under the approved
  `FORMAT_ONLY_NUMERIC_10` strategy. Identification code `04` MUST contain exactly 13 ASCII digits
  under `FORMAT_ONLY_NUMERIC_13`. This feature MUST NOT apply a checksum, legacy validation
  algorithm, or online existence check to either type.
- Identification codes `06` and `08` MUST contain 1 to 20 alphanumeric characters under
  `FORMAT_ONLY_ALPHANUMERIC_1_TO_20`; no checksum or country-specific rule may be invented. Code
  `08` retains the SRI requirement that the value is the identifier issued by the tax authority of
  the buyer's fiscal-residence country.
- Identification code `07` MUST use value `9999999999999` and buyer name `CONSUMIDOR FINAL`, and the
  rounded invoice grand total MUST NOT exceed the final-consumer threshold effective on the
  emission date. Under SRI Technical Sheet v2.32, that threshold is USD `50.00`.
- A draft MUST NOT be rejected solely because one or all line net amounts, tax amounts, the valid
  zero payment amount, or the rounded grand total are `0.00`.
- Buyer identifiers and contact data MUST NOT appear in caller-safe errors, logs, metrics, or
  traces as a consequence of validation failure.
- A draft with 501 invoice lines, 11 payments, or 16 additional-information entries MUST be
  rejected without partial persistence.
- Leading and trailing whitespace MUST be removed from text before length, format, nonblank,
  uniqueness, persistence, and idempotency-equivalence evaluation. A required or supplied optional
  text value that is empty afterward MUST be rejected.
- Control characters MUST be rejected in every caller-supplied text field.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Every create request MUST contain exactly one `X-Company-Id` HTTP request-header value.
  The value MUST be nonblank, syntactically valid as one UUID, and different from the nil UUID. A
  missing or blank value MUST produce `COMPANY_CONTEXT_REQUIRED`; a repeated, malformed, or nil
  value MUST produce `COMPANY_CONTEXT_INVALID`. Either result MUST use a safe English error response,
  include or return a correlation identifier, and leave no draft, child data, or idempotency
  binding.
- **FR-002**: The API boundary MUST validate only the presence, single-value cardinality, UUID
  syntax, and non-nil value of `X-Company-Id` and MUST normalize every accepted UUID to canonical
  lowercase hyphenated form. The Company identifier MUST NOT appear in the resource path, query
  string, request body, authentication token, or user-session context. `companyId` in the request
  body MUST be rejected as an unknown or prohibited property under strict unknown-property
  validation. The create resource operation MUST be `POST /invoice-drafts`, subject only to the
  project's API base and version prefix.
- **FR-003**: The service MUST treat an accepted Company UUID as opaque business-context metadata.
  It MUST NOT determine Company existence, active state, fiscal eligibility, tenant ownership,
  caller entitlement, or Company-to-Issuer, establishment, or emission-point relationships. It
  MUST NOT define `COMPANY_NOT_FOUND`, `COMPANY_INACTIVE`,
  `COMPANY_NOT_FISCALLY_ELIGIBLE`, `COMPANY_NOT_AUTHORIZED`,
  `COMPANY_CONTEXT_UNAVAILABLE`, `COMPANY_CONTEXT_TIMEOUT`,
  `COMPANY_ISSUER_CONFIGURATION_INVALID`, `EMISSION_POINT_COMPANY_MISMATCH`, authentication
  failures, authorization failures, `401` responses, or `403` responses for draft creation. A
  syntactically valid non-nil UUID MUST NOT be rejected because it is unknown, inactive, or
  nonexistent in an external Company system.
- **FR-004**: The create command MUST include one emission-point external identifier as business
  input. It MUST be nonblank after trimming, syntactically valid as a non-nil UUID, and normalized
  to canonical lowercase hyphenated form. The service MUST persist it as an opaque external
  reference, but MUST NOT resolve or validate its Company, Issuer, establishment, active, or
  effective relationship.
- **FR-005**: A caller MUST NOT provide Issuer fiscal data, establishment fiscal data,
  emission-point fiscal data, Company-context versions, observation timestamps, or fiscal
  snapshots. This prohibition includes `issuerId`, Issuer RUC, legal name, trade name, address, and
  every other Issuer fiscal attribute. Strict request-body validation MUST reject these properties
  as unknown or prohibited. Draft creation MUST neither resolve nor persist those values.
- **FR-006**: The request boundary MUST capture `requestCreationInstant` exactly once. The expected
  emission date MUST be derived from that instant in the IANA time zone `America/Guayaquil` and
  represented without a time-of-day component. A different or impossible date MUST be rejected
  rather than normalized. The derived date MUST remain fixed even when validation or commit crosses
  midnight.
- **FR-007**: Buyer legal name, identification value, and exactly one supported identification type
  MUST be present. Supported types are RUC (`04`), Ecuadorian identity card (`05`), passport (`06`),
  final consumer (`07`), and foreign identification (`08`). The selected type MUST be active and
  effective on the draft emission date, and the identification MUST satisfy the approved
  type-specific strategy in `SRI-OFFLINE-2.32-TARGET-1`.
- **FR-008**: Buyer address, email, and telephone MAY be captured as optional contact information.
  When present, an address MUST contain 1 to 300 characters, an email MUST be one syntactically
  valid address of no more than 254 characters, and a telephone MUST contain no more than 20
  characters, exactly 7 to 15 digits, and only digits, `+`, spaces, hyphens, and parentheses.
- **FR-009**: A draft MUST contain between 1 and 500 invoice lines, inclusive.
- **FR-010**: Every invoice line MUST contain a product or service code, description, quantity
  greater than zero and no greater than `999999.999999`, unit price from `0` through
  `999999.999999`, absolute discount from `0.00` through `999999999999999.99`, and exactly one
  selected IVA tax rule. Quantity and unit price MUST contain no more than six fractional digits.
  The code MUST contain 1 to 25 SRI-valid alphanumeric characters and the description MUST contain
  1 to 300 characters.
- **FR-011**: Each selected tax rule MUST be active, MUST have immutable family `IVA`, and its
  effective period MUST include the draft emission date. Supported rules MUST represent configured
  percentage-rate IVA, IVA 0%, not subject to IVA, or exempt from IVA. Any other tax or multiple
  simultaneous taxes on one line MUST be rejected as unsupported. The caller MUST select the tax
  rule and MUST NOT supply a tax code or rate as line input. The service MUST NOT infer product
  classification or decide whether a product legally qualifies for construction-material or other
  special treatment; the upstream billing workflow is responsible for selecting the appropriate
  published rule.
- **FR-012**: The service MUST calculate line gross amount, line net amount, tax base, tax amount,
  grouped tax totals, subtotal before taxes, total discount, and grand total. A creation request
  containing any of those system-calculated fields MUST be rejected with a stable validation error
  and MUST NOT persist any draft data. Every monetary input, exact intermediate result, rounded line
  result, grouped amount, payment sum, subtotal, discount total, tax amount, and grand total MUST
  remain within `0.00` through `999999999999999.99` inclusive.
- **FR-013**: A draft MUST contain at least one payment and every payment MUST select an active
  payment method. When the rounded grand total is greater than `0.00`, every payment amount MUST be
  greater than `0.00` and no greater than `999999999999999.99`, and the draft MUST contain no more
  than 8 payments. When the rounded grand total is `0.00`, the draft MUST contain exactly one
  payment with amount `0.00`. A selected payment method MUST appear at most once within the draft.
- **FR-014**: The exact sum of two-decimal payment amounts MUST equal the system-calculated
  two-decimal grand total after the DR-010 rounding pipeline is applied.
- **FR-015**: Optional additional information MUST contain no more than 15 named textual entries.
  Each entry's name and value MUST contain 1 to 300 characters. Names MUST be unique within the
  draft after trimming; two names with identical trimmed characters are duplicates.
- **FR-016**: The invoice currency MUST be USD.
- **FR-017**: A successfully created record MUST have internal target status `DRAFT`. That status
  MUST NOT be represented as an official SRI status or evidence of fiscal issuance.
- **FR-018**: A successfully created draft MUST receive a unique draft identifier unrelated to an
  official sequential number, access key, or authorization number.
- **FR-019**: Creation and last-modification timestamps MUST be returned as unambiguous instants.
  `createdAt` MUST represent the confirmed commit instant and MUST NOT be used to derive the
  accepted emission date. The last-modification timestamp MUST NOT precede `createdAt`.
- **FR-020**: The complete draft, its invoice lines, tax selections and calculated amounts,
  payments, and additional information MUST be persisted as one all-or-nothing outcome.
- **FR-021**: A request-contract, business-validation, reference-data, or persistence failure MUST
  leave no partial draft, child data, or idempotency binding.
- **FR-022**: A successful response MUST include the draft identifier, `DRAFT` status, captured
  normalized Company identifier, opaque emission-point identifier, buyer information, emission
  date, lines, payments, additional information, every calculated line amount, grouped tax totals,
  invoice totals, and both timestamps. It MUST NOT include a Company, Issuer, establishment, or
  emission-point master-data snapshot.
- **FR-023**: Draft creation MUST NOT reserve an official sequential number; generate an SRI access
  key, XML, signature, PDF, or RIDE; load a certificate; call the SRI; publish an asynchronous
  integration job or notification event; or send a notification. This prohibition does not exclude
  sanitized audit records required by the constitution.
- **FR-024**: Every repository query, mutation, and idempotency lookup for this feature MUST be
  scoped by the normalized Company UUID. This scoping is a business ownership-partitioning rule
  and MUST NOT be described as authentication, caller authorization, or proof of entitlement.
- **FR-025**: Validation and failure outcomes MUST use stable machine-readable English error
  categories and safe English messages. They MUST NOT expose buyer identification, issuer secrets,
  internal paths, persistence errors, stack traces, or other sensitive implementation details.
- **FR-026**: Correlation MUST be initialized at the request boundary for every creation attempt.
  When `X-Correlation-Id` is absent, the service MUST generate a safe UUID. One supplied value MUST
  be trimmed and preserved only when it contains 1 to 64 characters, begins with an ASCII letter or
  digit, and contains only ASCII letters, digits, `.`, `_`, `:`, or `-`. A blank, repeated,
  over-length, or otherwise unsafe supplied value MUST NOT be echoed; the service MUST generate a
  safe replacement UUID and return `INVALID_REQUEST` when correlation validation is the governing
  outcome. The safe correlation identifier MUST be returned and used for operational correlation.
- **FR-027**: Every creation command MUST provide a non-blank caller-generated idempotency key. Its
  scope MUST be the normalized Company UUID plus the key, with no tenant component. After trimming,
  the key MUST contain 1 to 128 printable ASCII characters. Changing the normalized Company UUID
  MUST change the idempotency scope even when the key and normalized business content are unchanged.
- **FR-028**: Buyer validation MUST validate only the approved type-specific syntax, length, and
  exact special values in `SRI-OFFLINE-2.32-TARGET-1`. Draft creation MUST NOT apply a RUC or
  Ecuadorian identity-card checksum, perform an online SRI registry existence check, or verify the
  supplied buyer name against an external registry. Checksum and registry verification are outside
  this feature and MUST NOT be treated as deferred implementation work.
- **FR-029**: Idempotency equivalence MUST include emission-point identifier, emission date, buyer
  information, invoice lines, tax treatments, payments, and additional information affecting the
  resulting draft. Company identifier MUST participate in the scope and MUST NOT be duplicated in
  the normalized request-content fingerprint. Idempotency keys, correlation identifiers, and all
  other transport-only headers MUST NOT affect that fingerprint. Representation-only differences,
  including JSON property ordering, MUST NOT make semantically equivalent normalized business
  content different. Invoice-line order MUST be included in business-content comparison. Payment
  and additional-information ordering MUST be ignored while their entries and values remain part
  of the comparison.
- **FR-030**: The first creation command that commits successfully MUST atomically bind the
  normalized Company UUID, a hash of the idempotency key, the created draft, and the normalized
  business content. The persistence uniqueness boundary MUST be `company_id +
  idempotency_key_hash`. An equivalent retry MUST return the original draft without creating or
  modifying another draft. A retry with different business content MUST return the stable
  `IDEMPOTENCY_CONFLICT` error and MUST NOT create or modify a draft. The binding MUST remain valid
  for the lifetime of the created draft and MUST NOT use time-based expiration.
- **FR-031**: Concurrent equivalent commands in the same idempotency scope MUST create exactly one
  draft and resolve to the same persisted result. The same key MAY be used independently by
  different companies and MUST NOT deduplicate across company boundaries.
- **FR-032**: Header-contract failure, business validation, reference-data failure, and every fully
  rolled-back persistence failure MUST NOT bind the idempotency key. A successful commit MUST
  remain bound after any response timeout, connection loss, or response-delivery failure.
- **FR-033**: A committed equivalent replay MUST return the original persisted draft in the same
  Company-and-key scope selected by the CompanyId in the current `X-Company-Id` header. It MUST
  resolve only the local Company-scoped binding and MUST NOT repeat creation, call a Company
  capability, authenticate or authorize the client, validate current Company, Issuer, or
  emission-point state, refresh external data, or apply current Company data. An idempotency key
  MUST NOT be treated as an authentication or authorization credential. Replay MUST NOT revalidate
  the original emission date against the current Ecuadorian civil date.
- **FR-034**: The invoice-draft capability MUST own its idempotency binding and MUST NOT delegate
  that responsibility to a Company capability, gateway, BFF, or other external service.
- **FR-035**: Buyer legal name MUST contain 1 to 300 characters. Leading and trailing whitespace
  MUST be removed from every caller-supplied text value before validation, persistence, and
  idempotency-equivalence evaluation. A required or supplied optional text value that is blank after
  trimming, or any text containing a control character, MUST be rejected without persistence.
- **FR-036**: Create Invoice Draft MUST NOT call a Company Service or introduce a Company-context
  port, client, repository, entity, validation or authorization adapter, eligibility lookup, status
  cache, master-data replica, direct Company-database access, cross-service foreign key, shared
  repository, or cross-service transaction. A Company dependency failure, timeout, retry,
  availability check, or readiness check MUST NOT exist for this feature.
- **FR-037**: An invoice draft MUST store the normalized Company UUID as its only Company ownership
  reference. The value MUST be immutable after creation and MUST be used to scope ownership,
  repository queries, mutations, and idempotency. Child data MUST belong through local draft
  aggregate relationships rather than independently representing Company master data.
- **FR-038**: Create Invoice Draft MUST NOT resolve or persist Company master-data snapshots,
  Company-context versions or observation timestamps, Issuer fiscal snapshots, establishment
  snapshots, or emission-point fiscal snapshots. Those concerns, together with fiscal eligibility,
  sequencing, access-key generation, XML, signing, and SRI submission, MUST be defined only by a
  separately approved fiscal-issuance specification.
- **FR-039**: The service MUST NOT implement, require, parse, propagate, persist, or interpret
  Keycloak, OpenID Connect, OAuth, JWTs, access tokens, API keys, user sessions, authenticated
  principals, user identifiers, roles, permissions, tenant authorization, user-to-Company
  authorization, or application-level service authentication. Its API contract MUST define no
  security scheme, security requirement, Authorization header, `401` response, or `403` response.
- **FR-040**: The API adapter MUST map the accepted `X-Company-Id` value to an application-level
  Company identifier. HTTP headers MUST NOT enter the domain model, and application or domain logic
  MUST NOT depend on HTTP request objects, security contexts, thread-local request context, gateway
  implementations, or Company Service clients.
- **FR-041**: Observable failure evaluation MUST use this precedence: (1) request payload-size
  enforcement; (2) `X-Company-Id` presence, cardinality, trimming, UUID syntax, and nil validation;
  (3) `X-Correlation-Id` validation; (4) `Idempotency-Key` syntax validation; (5) request
  representation and unknown or prohibited property validation; (6) normalized business-content
  generation; (7) local Company-scoped idempotency lookup; (8) equivalent binding returns the
  original persisted draft; (9) a binding with different content returns `IDEMPOTENCY_CONFLICT`;
  (10) buyer, line, tax-selection, payment, text, and collection validation; (11) monetary and tax
  calculation; and (12) atomic aggregate and idempotency-binding persistence. Correlation
  initialization MUST still produce a safe identifier for an earlier payload-size or
  Company-context error, but correlation invalidity MUST NOT replace that higher-precedence error.
  No authentication, authorization, tenant resolution, Company lookup, Issuer lookup, or
  emission-point ownership validation step may be inserted.
- **FR-042**: The create operation MUST reject a request body larger than `2 MiB` (`2,097,152`
  bytes) with a stable safe English payload-size error before Company-header evaluation. The error
  MUST include or return a correlation identifier and MUST leave no draft, child data, or
  idempotency binding.
- **FR-043**: A local persistence timeout or unexpected failure before successful commit MUST
  return a stable safe English error with a correlation identifier and MUST leave no draft, child
  data, or idempotency binding. A timeout or response-delivery failure after successful commit MUST
  preserve the binding and be recoverable through the replay behavior in FR-032 and FR-033.
- **FR-044**: Any value outside the approved quantity, unit-price, monetary, or percentage-rate
  envelope MUST return `BUSINESS_VALIDATION_FAILED` with violation code
  `MONETARY_RANGE_EXCEEDED` before persistence. The same limits MUST govern API schemas and
  representations, validation, exact calculation, rounded/grouped results, payment reconciliation,
  persistence, responses, and acceptance vectors. An out-of-range value MUST NOT be silently
  rounded, clamped, truncated, wrapped, or exposed as a persistence error.
- **FR-045**: Before `$speckit-tasks`, the supported identification types, IVA tax rules, and payment
  methods MUST use the approved versioned baseline in `reference-data-baseline.md`. Every baseline
  row MUST retain official code, exact official label, canonical English target name, treatment or
  validation strategy, rate when applicable, source and target validity, active state, catalog
  version, exact official source, and approval status.
- **FR-046**: Every approved tax rule and payment method MUST use a stable fixed UUID published by
  the target integration contract and derived by UUIDv5 from namespace
  `32576bbf-b70d-5c24-98ff-d5f9b48e8826` using the exact names in
  `reference-data-baseline.md`. The authoritative Flyway baseline created later MUST seed those
  values unchanged. UUIDs MUST NOT be generated at application startup. This feature MUST NOT add
  a catalog query operation; clients are expected to know the published identifiers through the
  integration contract.
- **FR-047**: No tax rate, payment code, identification rule, validity period, official mapping, or
  baseline UUID may be invented. Unsupported Table 17 rows MUST be omitted from the initial seed
  plan rather than seeded inactive. Any later unverified row MUST remain Pending Functional
  Validation and MUST block its introduction until official evidence and target mapping are
  approved.

### Domain Rules and Invariants

- **DR-001**: Official SRI Technical Sheet v2.32 and approved, versioned, effective-dated target
  baselines MUST govern supported identification types, tax rules, and payment methods. Each
  baseline row MUST retain the official code, display name, treatment or validation strategy, rate
  when applicable, validity interval, active state, catalog version, and exact official source.
  Tax and payment references MUST use approved stable fixed UUIDs published by the target service
  contract and supplied by authoritative Flyway migration data; they MUST NOT be generated at
  startup.
  Legacy enums and catalog rows MUST NOT become authoritative, and no unresolved mapping may be
  treated as approved reference data.
- **DR-002**: `gross amount = quantity × unit price` for each invoice line.
- **DR-003**: `net amount = gross amount − absolute discount` for each invoice line.
- **DR-004**: A line discount MUST NOT exceed its gross amount.
- **DR-005**: For a configured percentage-rate IVA rule, the tax base MUST be the line net amount
  and the configured rate MUST be expressed in percentage points, where `15.00` means `15%` and
  `tax amount = tax base × (tax rate ÷ 100)`. For IVA 0%, not subject to IVA, and exempt from IVA,
  the tax base MUST be the line net amount and the tax amount MUST be `0.00`.
- **DR-006**: `subtotal before taxes = sum of all line net amounts`.
- **DR-007**: `total discount = sum of all line absolute discounts`.
- **DR-008**: Tax totals MUST be grouped by the versioned tax-treatment code and applicable rate and
  MUST include the aggregate tax base and aggregate tax amount for each group. IVA 0%, not subject
  to IVA, and exempt from IVA MUST remain separate groups even when their rates or calculated tax
  amounts are equal.
- **DR-009**: `grand total = subtotal before taxes + sum of all calculated tax amounts`.
- **DR-010**: Quantity, unit price, discount, tax rates, tax bases, tax amounts, payment amounts,
  and invoice totals MUST use exact `BigDecimal` arithmetic; binary floating-point arithmetic is
  prohibited. Quantity MUST be greater than `0` and no greater than `999999.999999`; unit price
  MUST be from `0` through `999999.999999`; both MUST allow no more than six decimal places and
  target `numeric(12,6)` persistence. Catalog percentage rates MUST be from `0.00` through `100.00`,
  allow no more than two decimal places, and target `numeric(5,2)` persistence. Discounts and
  payments MUST be expressed to two decimal places. Every monetary input, exact intermediate,
  rounded line value, grouped value, payment sum, subtotal, discount total, tax amount, and grand
  total MUST be from `0.00` through `999999999999999.99` and target `numeric(17,2)` persistence.
  Excess input precision MUST be rejected rather than rounded. For each line, the service MUST
  calculate the exact gross amount before rounding it to scale two using `HALF_UP`; subtract the
  two-decimal discount and round net amount to scale two using `HALF_UP`; use that rounded net as
  the tax base; and round each calculated tax amount to scale two using `HALF_UP`. Subtotal before
  taxes, total discount, grouped tax bases, grouped tax amounts, and grand total MUST be sums of
  those rounded line values and MUST be represented at scale two. Payment reconciliation MUST
  compare the exact sum of two-decimal payment amounts with the two-decimal grand total. Every
  range check MUST occur before persistence and any violation MUST produce
  `BUSINESS_VALIDATION_FAILED` with violation code `MONETARY_RANGE_EXCEEDED`.
- **DR-011**: The service is responsible for calculation and payment reconciliation. The caller is
  responsible only for commercial inputs and payment allocations. Caller-supplied calculated
  fields are prohibited and MUST NOT be ignored, reconciled, or persisted.
- **DR-012**: The request boundary MUST capture `requestCreationInstant` exactly once and derive the
  expected date-only Ecuadorian civil date using `America/Guayaquil`. That expected date MUST remain
  fixed through validation and commit, including when commit crosses midnight. `createdAt` MUST be
  the confirmed commit instant and MUST NOT determine the accepted emission date. Creation and
  last-modification timestamps MUST be unambiguous instants. An equivalent replay MUST return the
  original emission date without comparing it with the current Ecuadorian date.
- **DR-013**: Impossible dates, inconsistent totals, inactive or temporally inapplicable catalog
  combinations, unsupported identification types, and invalid local aggregate relationships MUST
  be rejected without normalization or partial persistence.
- **DR-014**: RUC (`04`) MUST use `FORMAT_ONLY_NUMERIC_13`; Ecuadorian identity card (`05`) MUST use
  `FORMAT_ONLY_NUMERIC_10`; passport (`06`) and foreign identification (`08`) MUST use
  `FORMAT_ONLY_ALPHANUMERIC_1_TO_20`. Code `08` MUST represent the identifier issued by the tax
  authority of the buyer's fiscal-residence country. No checksum, country-specific rule, registry
  lookup, or legacy algorithm may be applied in this feature.
- **DR-015**: Final consumer (`07`) is valid only with identification value `9999999999999`, buyer
  name exactly `CONSUMIDOR FINAL`, and a rounded grand total not exceeding the SRI final-consumer
  threshold effective on the emission date. Under SRI Technical Sheet v2.32, the threshold MUST be
  USD `50.00`.
- **DR-016**: A line net amount and the rounded invoice grand total MAY be `0.00`; all monetary
  amounts MUST remain non-negative. A zero-priced or zero-net line MUST use the tax treatment
  explicitly selected for the product or transaction and MUST NOT default to IVA 0% because of its
  amount. Deemed taxable bases and special gratuitous-transfer treatments are unsupported.
- **DR-017**: A scoped idempotency key becomes bound only by a successful atomic commit. Once
  bound, equivalent normalized business content identifies the original draft and different
  business content produces `IDEMPOTENCY_CONFLICT`. The binding MUST exist for as long as the draft
  exists and MUST NOT expire because of elapsed time.
- **DR-018**: Idempotency scope MUST be exactly normalized Company UUID plus idempotency key. A
  Company UUID is business context, and neither it nor an idempotency key authenticates a caller,
  authorizes access, or proves entitlement.
- **DR-019**: The trimmed text values governed by FR-008, FR-010, FR-015, FR-027, and FR-035 are the
  canonical values used for persistence and idempotency equivalence. Lengths MUST be measured after
  trimming and before persistence.
- **DR-020**: Every draft MUST have exactly one immutable normalized Company UUID. Every line,
  payment, tax selection, tax total, and additional-information entry MUST belong to that draft;
  accidental mixing of child data between Company-scoped drafts is prohibited.
- **DR-021**: Invoice-line order is part of a creation command's normalized business content.
  Payment and additional-information collections are order-insensitive for idempotency equivalence.
- **DR-022**: Payments MUST be unique by payment-method identity within one draft.
- **DR-023**: The opaque emission-point identifier captured by a draft is unverified commercial
  input for later processing. It MUST NOT be represented as an Issuer, establishment, emission
  point, Company master-data snapshot, or proof of a validated external relationship.
- **DR-024**: Correlation initialization belongs to the request boundary. An absent correlation
  value MUST produce a generated safe UUID; one valid supplied value MUST be preserved; and an
  invalid supplied value MUST never be echoed and MUST produce a safe replacement UUID. Correlation
  values are transport evidence and MUST NOT participate in idempotency equivalence.

### Key Entities

- **Invoice Draft**: Internal pre-issuance record identified by a unique draft identifier and owned
  through one external Company identifier. It is fixed to USD and `DRAFT`, contains captured
  emission-point reference, emission date, buyer data, lines, selected tax-rule references,
  calculated monetary and tax values, payments, additional information, creation and modification
  timestamps, and Company-scoped idempotency association. It has no fiscal-context snapshot or
  official fiscal identity.
- **Company Identifier**: Immutable, normalized, opaque external UUID received through
  `X-Company-Id`. It partitions draft ownership, repository operations, and idempotency, but is not
  locally owned Company master data or proof of caller entitlement.
- **Emission-Point Identifier**: Opaque external identifier selected as draft input and retained for
  later processing. Draft creation does not resolve its Company, Issuer, establishment, status, or
  fiscal configuration.
- **Buyer**: Named recipient with an active identification type, validated identification value,
  and optional contact information. Final consumer is the exact SRI-defined special identity and
  name rather than a registry-verified person.
- **Invoice Line**: One product or service entry with code, description, positive quantity,
  non-negative unit price and discount, exactly one applicable IVA tax rule, and calculated
  amounts.
- **Tax Rule**: Approved, versioned, effective rule identified by a stable published UUID and
  combining an official code, immutable `IVA` family, treatment, rate, applicability period, active state,
  catalog version, official source, and calculation behavior. It represents configured
  percentage-rate IVA, IVA 0%, not subject to IVA, or exempt from IVA and is captured for review at
  draft creation.
- **Tax Total**: System-calculated aggregate tax base and amount for one versioned tax-treatment-code
  and applicable-rate group.
- **Payment Method**: Approved, versioned, effective reference identified by a stable published UUID
  and retaining its official code, display name, validity interval, active state, catalog version,
  and official source. No catalog-query operation is included in this feature.
- **Payment**: Exact decimal amount assigned to one active payment method. Positive-total drafts
  contain only positive payments; zero-total drafts contain exactly one `0.00` payment. In every
  case, payment amounts reconcile exactly to the rounded grand total, and a payment method appears
  at most once.
- **Additional Information**: Optional named textual entry captured for later review. A draft has
  at most 15 entries; each trimmed name and value has 1 to 300 characters, and trimmed names are
  unique within the draft.
- **Idempotency Binding**: Company-scoped association among a hash of a caller-generated key,
  normalized creation-command business content, and the successfully committed invoice draft. Its
  uniqueness boundary is Company identifier plus key hash; its lifetime equals the draft's lifetime
  and has no time-based expiration.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In the approved acceptance suite, 100% of logically new valid create commands persist
  exactly one complete invoice draft and return all fields required by FR-022.
- **SC-002**: In the approved validation and failure suite, 100% of rejected requests leave zero
  draft, child, and idempotency-binding records.
- **SC-003**: The same commercial inputs, emission date, and catalog-rule versions always produce
  identical line amounts, grouped taxes, payment comparison, and invoice totals.
- **SC-004**: The mathematical calculation vector `2 × 10.00 − 5.00` with the approved IVA 15%
  rule produces gross `20.00`, net `15.00`, tax `2.25`, and line contribution `17.25` in every
  supported runtime without asserting universal 15% applicability.
- **SC-005**: Every successful or failed draft-creation test records zero official sequential
  reservations, access keys, XML artifacts, signature operations, certificate reads, SRI calls,
  asynchronous integration jobs, notification events, PDFs, and notifications.
- **SC-006**: Every tested missing, repeated, blank, malformed, or nil `X-Company-Id` is rejected with
  `COMPANY_CONTEXT_REQUIRED` or `COMPANY_CONTEXT_INVALID` as applicable and leaves zero draft,
  child, or idempotency records.
- **SC-007**: All accepted drafts expose enough persisted captured and calculated information for a
  billing client to review Company identifier, opaque emission-point identifier, buyer, lines,
  taxes, payments, totals, status, and timestamps without invoking any fiscal-issuance capability.
- **SC-008**: The feature's acceptance scenarios can be validated independently of draft update,
  deletion, fiscal numbering, XML, signature, SRI authorization, PDF, and notification features.
- **SC-009**: Every accepted invoice line has exactly one active IVA tax rule effective on the
  emission date, and every unsupported or simultaneous tax selection is rejected without persisted
  draft data.
- **SC-010**: All accepted buyer identifications satisfy the versioned type-specific SRI rule
  effective on the emission date, and all invalid or unsupported identifications are rejected
  without online registry calls or persisted draft data.
- **SC-011**: Every otherwise valid zero-total acceptance vector persists one complete draft with
  exactly one `0.00` payment, while every invalid zero-total payment combination persists no draft
  data.
- **SC-012**: Every idempotency acceptance and concurrency vector commits at most one draft for one
  scoped key and equivalent content; conflict and failure vectors create or modify no draft and
  every lookup remains scoped by Company UUID plus idempotency key.
- **SC-013**: Every accepted new draft uses the date derived from the one captured
  `requestCreationInstant` in `America/Guayaquil`; all midnight-boundary vectors retain that date
  through commit, and every different or impossible date is rejected without persisted draft data.
- **SC-014**: Every request containing one or more system-calculated monetary fields is rejected
  with the same stable error category and persists no draft or child data, regardless of whether a
  supplied value matches the system's calculation.
- **SC-015**: Drafts at the limits of 500 invoice lines, 8 positive payments, and 15
  additional-information entries are accepted when otherwise valid, and every request exceeding
  any limit is rejected without persisted draft data.
- **SC-016**: Every text field at its approved length and format boundary is accepted when otherwise
  valid, and every over-limit, blank-after-trimming, control-character, invalid-contact, or
  duplicate-additional-name vector is rejected without persisted draft data.
- **SC-017**: Every accepted draft stores exactly one normalized immutable Company UUID and every
  persisted child belongs through that draft; test vectors produce zero accidental cross-Company
  child-data mixing.
- **SC-018**: Every equivalent replay in the same Company-and-key scope returns the original draft
  while that draft exists, regardless of elapsed time, and no time-based idempotency expiration
  permits a duplicate draft.
- **SC-019**: Reordering only payments or additional-information entries preserves idempotency
  equivalence, while changing invoice-line order produces a conflict for an already bound scoped
  key; neither case creates a duplicate draft.
- **SC-020**: Every accepted draft contains at most one payment per payment method, and every
  duplicate-payment-method request is rejected without persisted draft data.
- **SC-021**: Every accepted line derives its tax code and rate from its selected effective catalog
  rule, and every request supplying a line tax code or rate is rejected without persisted draft
  data.
- **SC-022**: Every accepted draft contains exactly one external Company ownership identifier and
  one opaque emission-point identifier, and contains zero Company-context versions, observation
  timestamps, Issuer snapshots, establishment snapshots, or emission-point fiscal snapshots.
- **SC-023**: Every create and replay acceptance vector performs zero Company Service calls,
  authentication or authorization processing, Company availability checks, Company cache access,
  or Company master-data replication.
- **SC-024**: The published API contract defines `X-Company-Id` only as a required single UUID
  request header, defines no Company path/query/body field, and contains zero security schemes,
  security requirements, Authorization headers, `401` responses, or `403` responses.
- **SC-025**: Every request body containing `companyId`, `issuerId`, or an Issuer, establishment, or
  emission-point fiscal attribute is rejected as unknown or prohibited and leaves zero draft,
  child, and idempotency-binding records.
- **SC-026**: Every failure-precedence test produces the outcome of the earliest applicable FR-041
  step; later validation, calculation, lookup, or persistence behavior is not executed after that
  terminal outcome.
- **SC-027**: Every request body of `2,097,152` bytes or less proceeds to the next applicable
  validation step, while every larger body is rejected before Company-header evaluation and leaves
  zero draft, child, and idempotency-binding records.
- **SC-028**: Every simulated local persistence timeout or unexpected pre-commit failure leaves zero
  draft, child, and idempotency-binding records and returns a correlated safe error; every simulated
  post-commit response failure remains recoverable as the original draft by equivalent replay.
- **SC-029**: Boundary review and vectors demonstrate that API schemas and inputs, exact
  intermediate calculations, rounded and grouped results, payment sums, persistence values, and
  response values all enforce the same quantity, unit-price, monetary, and percentage-rate
  envelopes; every breach returns `BUSINESS_VALIDATION_FAILED` with `MONETARY_RANGE_EXCEEDED` and
  persists no state.
- **SC-030**: Every equivalent replay on a later Ecuadorian date returns the original draft and
  emission date without current-date revalidation or mutation.
- **SC-031**: Pre-task review records zero unverified rows across the identification-type, IVA-rule,
  and payment-method baselines before `$speckit-tasks` may proceed; each approved row contains all
  evidence and metadata required by FR-045.
- **SC-032**: Every accepted `taxRuleId` and `paymentMethodId` is one published stable UUID from the
  approved baseline; zero reference identifiers are randomly generated at startup, and this feature
  exposes zero catalog-query operations.
- **SC-033**: Correlation acceptance vectors generate a safe UUID when the header is absent, preserve
  every single valid supplied identifier, never echo invalid input, and return a safe replacement
  UUID with `INVALID_REQUEST` when correlation validation governs. Combined-failure vectors follow
  FR-041, and changing correlation never changes idempotency equivalence.

## Assumptions and Dependencies

- **Assumption**: Buyer address, email, and telephone are optional because the approved business
  rules identify only buyer name and identification as mandatory. — **Basis**: User-provided
  Business Rule 4.
- **Assumption**: `DRAFT` is an internal target-domain status with no tax validity. — **Basis**:
  Explicit user objective and out-of-scope fiscal side effects, reconciled with SRI Technical Sheet
  v2.32.
- **Assumption**: Draft creation is a directly observable create operation, not an asynchronous SRI
  command. — **Basis**: The requested response contains the newly persisted draft and every SRI,
  queue, and notification interaction is excluded.
- **Assumption**: Before a request reaches this service, an upstream Gateway or BFF has performed
  any required authentication, authorization, Company validation, and Company-context resolution.
  It is expected to strip every externally supplied `X-Company-Id` and inject exactly one canonical
  validated value. The Tax Document Service does not verify those actions. This is an accepted
  integration assumption, not a guarantee enforced by this feature. — **Basis**: Approved
  Constitution v2.0.0 Company-context boundary.
- **Assumption**: Any process capable of reaching this internal API can submit any syntactically
  valid non-nil Company UUID. The service accepts that UUID without entitlement or Company-state
  verification. — **Basis**: Approved Constitution v2.0.0 trust boundary.
- **Dependency**: Draft creation has no Company Service, authentication, authorization, gateway,
  BFF, or Company master-data runtime dependency. Only the required Company header contract is
  visible at this service boundary.
- **Dependency**: Versioned identification, tax-rule, and payment-method catalogs use the approved
  `SRI-OFFLINE-2.32-TARGET-1` rows and exact identifiers in `reference-data-baseline.md`. The later
  Flyway seed task MUST reproduce that baseline without generating or substituting identifiers.
- **Dependency**: The target integration contract publishes the stable fixed UUIDs for approved tax
  rules and payment methods. Callers know those identifiers through that contract because Create
  Invoice Draft provides no catalog-query operation. The authoritative Flyway migration baseline
  supplies the same identifiers and MUST NOT substitute startup-generated values.
- **Dependency**: The IANA time-zone definition for `America/Guayaquil` governs conversion of the
  single captured `requestCreationInstant` to the expected emission date.
- **Dependency**: `docs/migration/terminology-mapping.md` is authoritative for the English target
  terms used by this feature.
