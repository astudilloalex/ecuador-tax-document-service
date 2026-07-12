# Feature Specification: Create Invoice Draft

**Feature Branch**: `6-ft-1`

**Created**: 2026-07-12

**Status**: Draft

**Input**: User description: "Create and persist a complete, tenant-isolated electronic invoice
draft for review before any fiscal identifier allocation or SRI interaction."

## Clarifications

### Session 2026-07-12

- Q: Which monetary precision and rounding policy should govern invoice drafts? → A: Line-level
  `HALF_UP` with six-decimal quantity/unit-price inputs, two-decimal tax-rate precision,
  two-decimal discounts/payments, rejected excess precision, rounded line values, and totals
  aggregated from rounded line values.
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
  a caller-generated idempotency key scoped to the authenticated effective company and tenant when
  applicable. Equivalent retries return the original committed draft; different content conflicts;
  concurrent equivalent commands create exactly one draft; failed or rolled-back commands do not
  bind the key.
- Q: What emission-date window should draft creation allow? → A: The emission date must equal the
  current Ecuadorian civil date at creation. Fiscal dates remain date-only values; creation and
  last-modification timestamps remain unambiguous instants.
- Q: How should draft creation handle client-supplied calculated monetary fields? → A: Reject every
  request containing a system-calculated input field; such values are never ignored, compared, or
  persisted.
- Q: What maximum collection counts should one draft allow? → A: At most 500 invoice lines, 10
  payments, and 15 additional-information entries.
- Q: What text-boundary policy should draft inputs use? → A: SRI-aligned bounded text: 25-character
  product codes; 300-character descriptions, buyer names, addresses, and additional-information
  names/values; 254-character email; 20-character telephone; and 128-character idempotency keys,
  with explicit format, trimming, uniqueness, and control-character rules.
- Q: What is the Company-to-Issuer cardinality for invoice drafts? → A: One Company has exactly one
  Issuer fiscal profile, and each Issuer belongs to exactly one Company. Establishments and
  emission points remain beneath the Issuer.
- Q: How long must a successful company-scoped idempotency binding remain valid? → A: For the
  lifetime of the created draft; the binding must not expire on a time-based schedule.
- Q: Which collection order should affect idempotency equivalence? → A: Invoice-line order only.
  Payment and additional-information ordering are representation-only differences.
- Q: May the same payment method appear more than once in a positive-total draft? → A: No. Each
  active payment method may appear at most once per draft.

## Scope and Evidence *(mandatory)*

### Bounded Outcome

An authenticated billing operator can create one complete invoice draft for an active company,
issuer, and emission point within the operator's effective tenant where applicable. The operator
receives the persisted buyer, line, tax, payment, and system-calculated total information for review
without creating an issued electronic invoice or triggering any fiscal side effect.

### In Scope

- Create a new invoice draft for one authorized active company, its single authoritative issuer
  fiscal profile, and one active emission point belonging to that issuer.
- Capture buyer identity and optional contact information.
- Capture one or more invoice lines, exactly one applicable active IVA tax treatment per line, one
  or more payments, and optional additional information.
- Calculate line amounts, tax bases, tax amounts, grouped tax totals, discounts, and invoice totals.
- Validate company authorization, tenant ownership where applicable, active reference data, buyer
  identification, monetary boundaries, and payment reconciliation before persistence.
- Persist the complete draft with internal status `DRAFT` and return its captured and calculated
  information, identifier, and timestamps.

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
- Preserving any legacy NestJS route, payload, response, table, status, authentication mechanism,
  asynchronous behavior, or operational behavior.
- Defining the later transition from `DRAFT` to a fiscally issued document.

### Authority and Evidence

| Authority | Source and version/path | Relevance to this feature |
|-----------|-------------------------|---------------------------|
| Applicable Ecuadorian legislation | [Regulation for Sales, Withholding, and Complementary Documents, SRI consolidated copy dated 2023-12-29](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/9fb49475-f058-49a1-b08a-f31bf4deb074/Reglamento_Comprobantes_Venta_RetencionYDC_29122023.pdf), together with [later amendments listed by the SRI](https://www.sri.gob.ec/facturacion-electronica) | Establishes invoices as sales documents and governs issuer responsibility and required invoice information. This feature creates only an internal pre-issuance draft. |
| Official SRI technical documentation | [Electronic Tax Documents Offline Scheme Technical Sheet v2.32, updated 2025-10-08](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf) and the [SRI electronic invoicing page](https://www.sri.gob.ec/facturacion-electronica), which lists invoice XSD/XML versions 1.0.0 through 2.1.0 updated in 2022-02 | Governs official catalogs, invoice fields, tax and payment representations, and the later generation/authorization process. XML generation and XSD selection are excluded here. |
| Project constitution | `.specify/memory/constitution.md` v1.0.0 | Governs authority, target-first scope, terminology, fiscal arithmetic, tenant isolation, atomic persistence, testing, and workflow. |
| Approved target requirements | This specification and its future clarification session | Governs the internal `DRAFT` behavior and the explicit absence of fiscal side effects. |
| Legacy evidence | `docs/legacy/as-is/04-data-model.md`; `docs/legacy/as-is/05-business-rules.md`; `docs/legacy/as-is/06-validation-rules.md`; `docs/legacy/as-is/07-process-flows.md`; `docs/legacy/as-is/10-security-access-control.md`; `docs/legacy/as-is/14-pending-functional-validation.md` | Supplies historical scenarios for invoice graphs, calculations, validation gaps, persistence, side effects, and tenant risks. It is not target authority. |

**Source Conflicts**:

- Legacy evidence BR-002, BR-006, BR-007, VR-009, VR-020, and legacy PFV-021 shows that historical
  processing accepted tax rates, bases, values, and payment totals from the client. The approved
  target requirement governs: the service calculates and reconciles these values.
- Legacy evidence BR-001, SEC-002, VR-025, and legacy PFV-036 documents selection by RUC with incomplete
  tenant enforcement. The constitution and this specification govern: the authorized effective
  company and its tenant scope where applicable are mandatory for issuer, emission-point, and draft
  access.
- The legacy invoice flow reserved a sequence, generated an access key and XML, signed it, called
  the SRI, and then persisted results. This target feature explicitly stops before all those
  activities.
- Official SRI electronic-document statuses do not include `DRAFT`. Here, `DRAFT` is an internal
  target-domain state and MUST NOT be presented as an SRI status, issued invoice, or tax-valid
  electronic document.

**Terminology Mapping**: The approved terms `Invoice Draft`, `Invoice`, `Company`, `Issuer`,
`Emission Point`, `Buyer`, `Invoice Line`, `Tax Category`, `Tax Rate`, `Tax Rule`, `Payment Method`,
`Payment`, `Additional Information`, and `Idempotency Key` are recorded in
`docs/migration/terminology-mapping.md`. Exact official terms such as `RUC`, SRI catalog codes,
`Access Key`, and `Official Sequential Number` retain their mapped scope.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create and Review an Invoice Draft (Priority: P1)

As an authenticated billing operator, I create an invoice draft for an authorized company and an
issuer in that company's tenant scope so I can review validated commercial, tax, payment, and
calculated total information before any fiscal issuance occurs.

**Why this priority**: It is the smallest independently valuable billing outcome. It captures and
validates invoice intent while isolating later fiscal numbering, XML, signature, and SRI risks.

**Independent Test**: Supply an authorized active company, an issuer belonging to that company, an
emission point, valid buyer data, one or more valid lines, active tax and payment references, and
matching payment amounts. The returned persisted draft can be reviewed in full, while sequence,
access-key, XML, certificate, notification, and SRI evidence remain absent.

**Acceptance Scenarios**:

1. **Given** an authenticated billing operator authorized for an active company, an active issuer
   belonging to that company and its tenant where applicable, an active emission point belonging to
   that issuer, valid buyer data, at least one valid line, and payments equal to the calculated grand
   total, **when** the operator creates the draft, **then** exactly one complete draft is persisted
   with status `DRAFT`, a unique draft identifier, calculated amounts, and creation and
   last-modification timestamps.
2. **Given** a line with quantity `2`, unit price `10.00`, discount `5.00`, and an applicable
   percentage tax rate of `15%`, **when** the draft is calculated, **then** gross amount is `20.00`,
   net amount is `15.00`, tax is `2.25`, and that line contributes `17.25` to the grand total.
3. **Given** a line with gross amount `20.00` and discount `21.00`, **when** draft creation is
   attempted, **then** a business validation error is returned and no draft or child data is
   persisted.
4. **Given** otherwise valid data with no invoice lines, **when** draft creation is attempted,
   **then** the request is rejected and no draft is persisted.
5. **Given** a calculated grand total of `100.00` and payments totaling `90.00`, **when** draft
   creation is attempted, **then** a payment-total validation error is returned and no draft is
   persisted.
6. **Given** an operator in Tenant A and an issuer in Tenant B, **when** draft creation is
   attempted, **then** access is denied, no Tenant B data is exposed, and no draft is persisted.
7. **Given** buyer identification that fails the approved rule for its active identification type,
   **when** draft creation is attempted, **then** a buyer-identification validation error is
   returned and no draft is persisted.
8. **Given** a valid draft request, **when** creation succeeds, **then** no official sequential
   number is reserved, no access key is generated, no XML is generated or signed, no certificate
   is required, no notification is sent, and no SRI communication occurs.
9. **Given** an inactive issuer, emission point, buyer identification type, tax rule, tax category,
   or payment method, **when** draft creation is attempted, **then** the request is rejected and no
   draft is persisted.
10. **Given** a request that attempts to override registered issuer legal identity, **when** draft
    creation is attempted, **then** the override is not accepted and no unregistered issuer legal
    identity is persisted or returned as authoritative.
11. **Given** a persistence failure after validation begins, **when** draft creation cannot finish,
    **then** the operator receives a safe failure outcome and no partial draft, line, tax, payment,
    or additional-information data remains persisted.
12. **Given** a line selects ICE, IRBPNR, another non-IVA tax, no tax treatment, or more than one
    simultaneous tax treatment, **when** draft creation is attempted, **then** the request is
    rejected as unsupported and no draft is persisted.
13. **Given** a buyer identification uses code `04`, `05`, `06`, or `08`, **when** draft creation is
    attempted, **then** it is accepted only when it satisfies the applicable versioned SRI syntax,
    length, and any officially applicable checksum effective on the emission date.
14. **Given** a RUC class for which the applicable SRI rules define no checksum, **when** its value
    satisfies the official RUC format, **then** the draft is not rejected for failing a generic or
    locally assumed checksum.
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
20. **Given** a successfully committed creation command, **when** the same authorized company,
    scoped idempotency key, and semantically equivalent normalized business content are submitted
    again, **then** the original draft is returned and no new draft is created or modified.
21. **Given** a scoped idempotency key already bound by a successful command, **when** the same
    authorized company and key are submitted with different business content, **then** a stable
    idempotency conflict error is returned and no draft is created or modified.
22. **Given** concurrent equivalent commands for the same authorized company and idempotency key,
    **when** they are processed, **then** exactly one draft is committed and every successful
    outcome resolves to that same persisted draft.
23. **Given** a command fails business validation, company resolution or authorization, or a
    persistence operation that fully rolls back, **when** the key is retried, **then** the prior
    failure has not bound the scoped idempotency key.
24. **Given** a draft committed successfully but its response was not delivered, **when** the same
    authorized company, key, and equivalent content are retried, **then** the committed binding
    returns the existing draft without executing the original creation operation again.
25. **Given** the same idempotency key is used for two independently authorized companies, **when**
    each company submits a valid command, **then** the keys are independent and neither company can
    observe or deduplicate against the other's draft.
26. **Given** an emission date equal to the current Ecuadorian civil date at creation, **when** all
    other draft data is valid, **then** the emission date is accepted as a date-only value.
27. **Given** an emission date before or after the current Ecuadorian civil date at creation,
    **when** draft creation is attempted, **then** the request is rejected and no draft is
    persisted.
28. **Given** a creation request containing any client-supplied line gross amount, line net amount,
    tax base, tax amount, grouped tax total, subtotal before taxes, total discount, or grand total,
    **when** draft creation is attempted, **then** a stable calculated-field validation error is
    returned and no draft or child data is persisted.
29. **Given** otherwise valid data containing exactly 500 invoice lines, 10 positive payments, and
    15 additional-information entries, **when** the payment sum matches the positive grand total,
    **then** the collection counts are accepted.
30. **Given** a request containing more than 500 invoice lines, more than 10 payments, or more than
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
34. **Given** an authorized active company with its single active Issuer fiscal profile and an
    active emission point beneath that Issuer, **when** otherwise valid draft creation is attempted,
    **then** the draft is associated with that Company and Issuer.
35. **Given** a request references an Issuer that is not the effective Company's single Issuer,
    **when** draft creation is attempted, **then** the request is rejected without exposing the
    referenced Issuer and no draft is persisted.
36. **Given** a draft and its scoped idempotency binding still exist, regardless of elapsed time,
    **when** an authorized equivalent retry uses that key, **then** the original draft is returned
    and no new draft is created.
37. **Given** an authorized retry uses the same scoped key and business content but reorders only
    payments or additional-information entries, **when** idempotency equivalence is evaluated,
    **then** the original draft is returned and no new draft is created.
38. **Given** an authorized retry uses the same scoped key but changes the invoice-line order,
    **when** idempotency equivalence is evaluated, **then** an idempotency conflict is returned and
    no draft is created or modified.
39. **Given** a positive-total draft contains two or more payments selecting the same payment
    method, **when** draft creation is attempted, **then** a stable duplicate-payment-method error
    is returned and no draft or child data is persisted.
40. **Given** a line supplies a tax code or rate instead of selecting the applicable effective tax
    rule, **when** draft creation is attempted, **then** the request is rejected and no draft or
    child data is persisted.

### Edge Cases

- An impossible calendar date and any date other than the current Ecuadorian civil date at creation
  MUST be rejected rather than normalized.
- Quantity zero or negative, negative unit price, and negative discount MUST be rejected.
- A discount equal to gross amount MAY produce a zero net line, and all lines MAY produce a rounded
  grand total of `0.00`; neither outcome alone is a validation failure.
- Quantity or unit-price input with more than six decimal places, or discount/payment input not
  expressed to two decimal places, MUST be rejected; input precision MUST NOT be silently rounded.
- A caller MUST select an effective tax rule and MUST NOT supply a tax code or rate as line input.
  A catalog tax rule whose configured rate exceeds two decimal places MUST be rejected as invalid
  reference data rather than rounded.
- Every line MUST select exactly one tax rule from the IVA tax family. The rule and its parent tax
  category MUST be active, and the rule's effective period MUST include the emission date.
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
- Multiple payments that differ from the rounded grand total by any non-zero amount MUST be
  rejected. Payment comparison MUST use exact two-decimal values after line-level `HALF_UP`
  calculation and aggregation.
- A payment method MUST NOT appear in more than one payment entry within the same draft.
- Representation-only differences, including JSON property ordering and the ordering of payments
  or additional-information entries, MUST NOT make otherwise equivalent creation commands
  different for idempotency purposes. Invoice-line order is business-significant and MUST be
  preserved.
- A retry of a committed command MUST still enforce current caller authorization, MUST NOT expose a
  draft outside the authorized company and tenant scope, and MUST NOT repeat creation even if
  mutable company information has changed.
- A successful idempotency binding MUST NOT expire because a time interval elapsed while its draft
  still exists.
- A Company with no Issuer or more than one Issuer violates the required cardinality and MUST NOT
  be accepted for draft creation. An Issuer associated with another Company MUST remain
  indistinguishable from an inaccessible Issuer.
- A request containing any system-calculated monetary field MUST be rejected. The supplied value
  MUST NOT be ignored, compared with a calculated result, or persisted.
- Identification code `05` MUST satisfy the official Ecuadorian identity-card numeric format and
  check-digit rule effective on the emission date; no alternative algorithm is permitted.
- Identification code `04` MUST satisfy the official RUC format and an official checksum only when
  the applicable SRI rule defines one for that RUC class. A generic locally assumed checksum MUST
  NOT be applied to a class for which the SRI defines no checksum.
- Identification codes `06` and `08` MUST satisfy only the format and length defined by the
  applicable SRI catalog or invoice schema; no checksum or country-specific rule may be invented.
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

- **FR-001**: Only an authenticated principal authorized as a billing operator MUST be able to
  create an invoice draft.
- **FR-002**: The service MUST resolve the effective company, and the effective tenant when the
  company belongs to a broader tenant, from authenticated and authorized business context. A
  company or tenant identifier supplied by the request MUST NOT be trusted without verifying the
  caller's authorization for that company.
- **FR-003**: The effective company MUST exist and be active. The selected issuer MUST exist, be
  active, be the effective Company's single authoritative Issuer fiscal profile, and belong to its
  effective tenant where applicable. A Company with no Issuer or more than one Issuer MUST be
  rejected. A foreign-company or foreign-tenant Issuer MUST be indistinguishable from an
  inaccessible Issuer to the caller.
- **FR-004**: The selected emission point MUST exist, be active, and belong to the selected issuer.
- **FR-005**: Issuer legal identity and fiscal information MUST be obtained from the registered
  issuer record and captured as the draft's creation-time issuer information. A caller MUST NOT
  override those values.
- **FR-006**: The emission date MUST be a real date equal to the current Ecuadorian civil date at
  creation and MUST be represented without a time-of-day component. A past, future, or impossible
  date MUST be rejected rather than normalized.
- **FR-007**: Buyer legal name, identification value, and exactly one supported identification type
  MUST be present. Supported types are RUC (`04`), Ecuadorian identity card (`05`), passport (`06`),
  final consumer (`07`), and foreign identification (`08`). The selected type MUST be active and
  effective on the draft emission date, and the identification MUST satisfy the versioned SRI
  type-specific rules applicable on that date.
- **FR-008**: Buyer address, email, and telephone MAY be captured as optional contact information.
  When present, an address MUST contain 1 to 300 characters, an email MUST be one syntactically
  valid address of no more than 254 characters, and a telephone MUST contain no more than 20
  characters, exactly 7 to 15 digits, and only digits, `+`, spaces, hyphens, and parentheses.
- **FR-009**: A draft MUST contain between 1 and 500 invoice lines, inclusive.
- **FR-010**: Every invoice line MUST contain a product or service code, description, quantity
  greater than zero, unit price greater than or equal to zero, absolute discount greater than or
  equal to zero, and exactly one selected IVA tax rule. The code MUST contain 1 to 25 SRI-valid
  alphanumeric characters and the description MUST contain 1 to 300 characters.
- **FR-011**: Each selected IVA tax rule and its tax category MUST be active, and the rule's
  effective period MUST include the draft emission date. Supported rules MUST represent configured
  percentage-rate IVA, IVA 0%, not subject to IVA, or exempt from IVA. Any other tax or multiple
  simultaneous taxes on one line MUST be rejected as unsupported. The caller MUST select the tax
  rule and MUST NOT supply a tax code or rate as line input.
- **FR-012**: The service MUST calculate line gross amount, line net amount, tax base, tax amount,
  grouped tax totals, subtotal before taxes, total discount, and grand total. A creation request
  containing any of those system-calculated fields MUST be rejected with a stable validation error
  and MUST NOT persist any draft data.
- **FR-013**: A draft MUST contain at least one payment and every payment MUST select an active
  payment method. When the rounded grand total is greater than `0.00`, every payment amount MUST be
  greater than `0.00` and the draft MUST contain no more than 10 payments. When the rounded grand
  total is `0.00`, the draft MUST contain exactly one payment with amount `0.00`. A selected payment
  method MUST appear at most once within the draft.
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
  The last-modification timestamp MUST NOT precede the creation timestamp.
- **FR-020**: The complete draft, its invoice lines, tax selections and calculated amounts,
  payments, and additional information MUST be persisted as one all-or-nothing outcome.
- **FR-021**: A validation, authorization, reference-data, or persistence failure MUST leave no
  partial draft or child data.
- **FR-022**: A successful response MUST include the draft identifier, `DRAFT` status, captured
  issuer and buyer information, emission date, lines, payments, additional information, every
  calculated line amount, grouped tax totals, invoice totals, and both timestamps.
- **FR-023**: Draft creation MUST NOT reserve an official sequential number; generate an SRI access
  key, XML, signature, PDF, or RIDE; load a certificate; call the SRI; publish an asynchronous
  integration job or notification event; or send a notification. This prohibition does not exclude
  sanitized audit records required by the constitution.
- **FR-024**: Cross-company or cross-tenant denial MUST NOT expose whether the requested company,
  issuer, emission point, draft, or related data exists outside the caller's effective scope.
- **FR-025**: Validation and failure outcomes MUST use stable machine-readable English error
  categories and safe English messages. They MUST NOT expose buyer identification, issuer secrets,
  internal paths, persistence errors, stack traces, or other sensitive implementation details.
- **FR-026**: Every creation attempt MUST carry or receive a correlation identifier that can be
  used to correlate its safe operational records.
- **FR-027**: Every creation command MUST provide a non-blank caller-generated idempotency key. When
  a company belongs to a broader tenant, the effective idempotency scope MUST be tenant identifier,
  company identifier, and key. When the company itself is the tenant boundary, the scope MAY omit
  a separate tenant identifier and MUST contain the company identifier and key. After trimming, the
  key MUST contain 1 to 128 printable ASCII characters.
- **FR-028**: Buyer validation MUST validate only the approved type-specific syntax, length, exact
  special values, and officially applicable checksum rules. Draft creation MUST NOT perform an
  online SRI registry existence check or verify the supplied buyer name against an external
  registry.
- **FR-029**: Idempotency equivalence MUST include company, issuer, emission point, emission date,
  buyer information, invoice lines, tax treatments, payments, and additional information affecting
  the resulting draft. Representation-only differences, including JSON property ordering, MUST NOT
  make semantically equivalent normalized business content different. Invoice-line order MUST be
  included in business-content comparison. Payment and additional-information ordering MUST be
  ignored while their entries and values remain part of the comparison.
- **FR-030**: The first creation command that commits successfully MUST bind its scoped idempotency
  key to the effective tenant when applicable, effective company, created draft, and normalized
  business content as one atomic committed outcome. An equivalent retry MUST return the original
  draft without creating or modifying another draft. A retry with different business content MUST
  return a stable idempotency conflict error and MUST NOT create or modify a draft. The binding MUST
  remain valid for the lifetime of the created draft and MUST NOT use time-based expiration.
- **FR-031**: Concurrent equivalent commands in the same idempotency scope MUST create exactly one
  draft and resolve to the same persisted result. The same key MAY be used independently by
  different companies and MUST NOT deduplicate across company boundaries.
- **FR-032**: Business validation, company resolution, company authorization, and fully rolled-back
  persistence failures MUST NOT bind the idempotency key. A successful commit MUST remain bound
  after any response timeout, connection loss, or response-delivery failure.
- **FR-033**: A retry of a committed command MUST enforce current caller authorization before
  returning the existing draft. It MUST NOT re-execute the original creation operation because
  mutable company information changed, bypass authorization, expose another company's or tenant's
  draft, or treat the idempotency key as an authentication credential.
- **FR-034**: The invoice-draft capability MUST own the idempotency binding. The company capability
  remains authoritative for company existence, active status, tenant ownership, and caller access
  and MUST NOT be responsible for invoice-draft idempotency bindings.
- **FR-035**: Buyer legal name MUST contain 1 to 300 characters. Leading and trailing whitespace
  MUST be removed from every caller-supplied text value before validation, persistence, and
  idempotency-equivalence evaluation. A required or supplied optional text value that is blank after
  trimming, or any text containing a control character, MUST be rejected without persistence.

### Domain Rules and Invariants

- **DR-001**: Official SRI Technical Sheet v2.32 and the active, versioned target catalogs MUST
  govern supported identification, tax, and payment codes. Tax codes and rates MUST come from
  versioned, effective-dated catalogs and MUST NOT be hard-coded. Legacy enums and catalog rows
  MUST NOT become authoritative.
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
  and invoice totals MUST use exact decimal arithmetic; binary floating-point arithmetic is
  prohibited. Quantity and unit price MUST allow no more than six decimal places. Catalog tax rates
  expressed in percentage points MUST allow no more than two decimal places. Discounts and payments
  MUST be expressed to two decimal places. Excess input precision MUST be rejected rather than
  rounded. For each line, the service
  MUST calculate the exact gross amount and round it to scale two using `HALF_UP`; subtract the
  two-decimal discount and round net amount to scale two using `HALF_UP`; use that rounded net as
  the tax base; and round each calculated tax amount to scale two using `HALF_UP`. Subtotal before
  taxes, total discount, grouped tax bases, grouped tax amounts, and grand total MUST be sums of
  those rounded line values and MUST be represented at scale two. Payment reconciliation MUST
  compare the exact sum of two-decimal payment amounts with the two-decimal grand total.
- **DR-011**: The service is responsible for calculation and payment reconciliation. The caller is
  responsible only for commercial inputs and payment allocations. Caller-supplied calculated
  fields are prohibited and MUST NOT be ignored, reconciled, or persisted.
- **DR-012**: The emission date MUST use date-only Ecuadorian civil-date semantics and equal the
  Ecuadorian civil date at the creation instant. Creation and last-modification timestamps MUST be
  unambiguous instants.
- **DR-013**: Impossible dates, inconsistent totals, inactive or temporally inapplicable catalog
  combinations, unsupported identification types, and unauthorized ownership combinations MUST
  be rejected without normalization or partial persistence.
- **DR-014**: An Ecuadorian identity card (`05`) MUST satisfy the official numeric format and
  check-digit rule. A RUC (`04`) MUST satisfy the official RUC format and an official checksum only
  when one applies to its RUC class. Passport (`06`) and foreign identification (`08`) MUST satisfy
  only the format and length defined by the applicable SRI catalog or invoice schema. No format,
  checksum, country-specific rule, or legacy algorithm may be invented.
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
  business content is an idempotency conflict. The binding MUST exist for as long as the draft
  exists and MUST NOT expire because of elapsed time.
- **DR-018**: Idempotency scope and authorization scope MUST remain aligned to the effective
  company and effective tenant where applicable. An idempotency key MUST NOT grant access or reveal
  whether a binding exists outside that scope.
- **DR-019**: The trimmed text values governed by FR-008, FR-010, FR-015, FR-027, and FR-035 are the
  canonical values used for persistence and idempotency equivalence. Lengths MUST be measured after
  trimming and before persistence.
- **DR-020**: A Company MUST have exactly one Issuer fiscal profile, and an Issuer MUST belong to
  exactly one Company. Establishments and emission points MUST remain subordinate to that Issuer.
- **DR-021**: Invoice-line order is part of a creation command's normalized business content.
  Payment and additional-information collections are order-insensitive for idempotency equivalence.
- **DR-022**: Payments MUST be unique by payment-method identity within one draft.

### Key Entities

- **Invoice Draft**: Internal pre-issuance record identified by a unique draft identifier, owned by
  one company, its tenant where applicable, and one issuer, fixed to USD and `DRAFT`, and containing
  captured commercial inputs, calculated totals, and audit timestamps. It has no official fiscal
  identity.
- **Company**: Effective legal entity for which the draft is created. It exists within an
  authorization boundary, MAY itself be the tenant boundary or belong to a broader tenant, and has
  exactly one Issuer fiscal profile.
- **Issuer**: The single active fiscal profile belonging to exactly one Company. Its registered
  fiscal data is authoritative for the draft and cannot be overridden by the operator.
- **Emission Point**: Active point belonging to the selected issuer and intended for later fiscal
  issuance without reserving a sequence during draft creation.
- **Buyer**: Named recipient with an active identification type, validated identification value,
  and optional contact information. Final consumer is the exact SRI-defined special identity and
  name rather than a registry-verified person.
- **Invoice Line**: One product or service entry with code, description, positive quantity,
  non-negative unit price and discount, exactly one applicable IVA tax rule, and calculated
  amounts.
- **Tax Rule**: Versioned, effective rule combining a tax category, rate, applicability period, and
  calculation behavior. It represents configured percentage-rate IVA, IVA 0%, not subject to IVA,
  or exempt from IVA and is captured for review at draft creation.
- **Tax Total**: System-calculated aggregate tax base and amount for one versioned tax-treatment-code
  and applicable-rate group.
- **Payment**: Exact decimal amount assigned to one active payment method. Positive-total drafts
  contain only positive payments; zero-total drafts contain exactly one `0.00` payment. In every
  case, payment amounts reconcile exactly to the rounded grand total, and a payment method appears
  at most once.
- **Additional Information**: Optional named textual entry captured for later review. A draft has
  at most 15 entries; each trimmed name and value has 1 to 300 characters, and trimmed names are
  unique within the draft.
- **Idempotency Binding**: Company-scoped association among a caller-generated key, effective tenant
  where applicable, normalized creation-command business content, and the successfully committed
  invoice draft. Its lifetime equals the draft's lifetime and has no time-based expiration.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In the approved acceptance suite, 100% of logically new valid create commands persist
  exactly one complete invoice draft and return all fields required by FR-022.
- **SC-002**: In the approved validation and failure suite, 100% of rejected requests leave zero
  draft and child records.
- **SC-003**: The same commercial inputs, emission date, and catalog-rule versions always produce
  identical line amounts, grouped taxes, payment comparison, and invoice totals.
- **SC-004**: The calculation vector `2 × 10.00 − 5.00` with a `15%` percentage tax produces gross
  `20.00`, net `15.00`, tax `2.25`, and line contribution `17.25` in every supported runtime.
- **SC-005**: Every successful or failed draft-creation test records zero official sequential
  reservations, access keys, XML artifacts, signature operations, certificate reads, SRI calls,
  asynchronous integration jobs, notification events, PDFs, and notifications.
- **SC-006**: All cross-company and cross-tenant test attempts are denied, expose zero foreign-scope
  fields, and persist zero data.
- **SC-007**: All accepted drafts expose enough persisted captured and calculated information for a
  billing operator to review issuer, emission point, buyer, lines, taxes, payments, totals, status,
  and timestamps without invoking any later authorization capability.
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
  expose no cross-company or cross-tenant data.
- **SC-013**: Every accepted draft uses the current Ecuadorian civil date at creation as its
  date-only emission date, and every past, future, or impossible date is rejected without persisted
  draft data.
- **SC-014**: Every request containing one or more system-calculated monetary fields is rejected
  with the same stable error category and persists no draft or child data, regardless of whether a
  supplied value matches the system's calculation.
- **SC-015**: Drafts at the limits of 500 invoice lines, 10 positive payments, and 15
  additional-information entries are accepted when otherwise valid, and every request exceeding
  any limit is rejected without persisted draft data.
- **SC-016**: Every text field at its approved length and format boundary is accepted when otherwise
  valid, and every over-limit, blank-after-trimming, control-character, invalid-contact, or
  duplicate-additional-name vector is rejected without persisted draft data.
- **SC-017**: Every accepted draft is associated with one effective Company and that Company's
  single Issuer; every missing, multiple, foreign-company, or foreign-tenant Issuer case is rejected
  without persisted draft data or foreign-scope disclosure.
- **SC-018**: Every authorized equivalent retry returns the original draft while that draft exists,
  regardless of elapsed time, and no time-based idempotency expiration permits a duplicate draft.
- **SC-019**: Reordering only payments or additional-information entries preserves idempotency
  equivalence, while changing invoice-line order produces a conflict for an already bound scoped
  key; neither case creates a duplicate draft.
- **SC-020**: Every accepted draft contains at most one payment per payment method, and every
  duplicate-payment-method request is rejected without persisted draft data.
- **SC-021**: Every accepted line derives its tax code and rate from its selected effective catalog
  rule, and every request supplying a line tax code or rate is rejected without persisted draft
  data.

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
- **Dependency**: Authentication and authorization context must provide the billing-operator grant
  and the authorized effective company and tenant scope without trusting request-only identifiers.
- **Dependency**: The company capability must provide authoritative company existence, active
  status, tenant ownership, caller-access decisions, and the single Company-to-Issuer association;
  invoice-draft idempotency remains owned by this feature.
- **Dependency**: Registered issuer and emission-point capabilities must provide company and tenant
  ownership, hierarchy, active state, and authoritative issuer fiscal information.
- **Dependency**: Versioned identification, tax-category, tax-rule, and payment-method catalogs must
  provide active/effective reference data aligned with the official sources cited above.
- **Dependency**: `docs/migration/terminology-mapping.md` is authoritative for the English target
  terms used by this feature.
