# Feature Specification: Create Invoice Draft

**Feature Branch**: `6-ft-1`

**Created**: 2026-07-12

**Status**: Draft

**Input**: User description: "Create and persist a complete, tenant-isolated electronic invoice
draft for review before any fiscal identifier allocation or SRI interaction."

## Scope and Evidence *(mandatory)*

### Bounded Outcome

An authenticated billing operator can create one complete invoice draft for an active issuer and
emission point within the operator's effective tenant. The operator receives the persisted buyer,
line, tax, payment, and system-calculated total information for review without creating an issued
electronic invoice or triggering any fiscal side effect.

### In Scope

- Create a new invoice draft for one tenant-owned issuer and one active emission point.
- Capture buyer identity and optional contact information.
- Capture one or more invoice lines, their applicable active tax rules, one or more payments, and
  optional additional information.
- Calculate line amounts, tax bases, tax amounts, grouped tax totals, discounts, and invoice totals.
- Validate tenant ownership, active reference data, buyer identification, monetary boundaries, and
  payment reconciliation before persistence.
- Persist the complete draft with internal status `DRAFT` and return its captured and calculated
  information, identifier, and timestamps.

### Exclusions and Non-Goals

- Updating, deleting, or cancelling an invoice draft.
- Submitting an invoice for SRI reception or authorization.
- Reserving or assigning an official invoice sequential number.
- Generating an SRI access key or numeric code.
- Generating, validating, storing, or signing invoice XML.
- Communicating with the SRI or observing an SRI authorization result.
- Requiring, loading, validating, or managing a digital certificate.
- Generating a PDF or RIDE representation.
- Sending email, webhook, queue, or other notification messages.
- Creating credit notes, debit notes, withholding documents, delivery notes, or any other tax
  document type.
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
  tenant enforcement. The constitution and this specification govern: the effective tenant scope
  is mandatory for issuer, emission-point, and draft access.
- The legacy invoice flow reserved a sequence, generated an access key and XML, signed it, called
  the SRI, and then persisted results. This target feature explicitly stops before all those
  activities.
- Official SRI electronic-document statuses do not include `DRAFT`. Here, `DRAFT` is an internal
  target-domain state and MUST NOT be presented as an SRI status, issued invoice, or tax-valid
  electronic document.

**Terminology Mapping**: The approved terms `Invoice Draft`, `Invoice`, `Issuer`, `Emission Point`,
`Buyer`, `Invoice Line`, `Tax Category`, `Tax Rate`, `Tax Rule`, `Payment Method`, `Payment`, and
`Additional Information` are recorded in `docs/migration/terminology-mapping.md`. Exact official
terms such as `RUC`, SRI catalog codes, `Access Key`, and `Official Sequential Number` retain their
mapped scope.

### Pending Functional Validation

- **PFV-001 — Monetary precision and rounding pipeline**: Confirm the permitted input scale and
  precision for quantity, unit price, discount, payment amount, and tax rate; the rounding mode;
  and whether gross, net, line tax, grouped tax, and grand totals are rounded per line or only after
  aggregation. — **Evidence needed**: Accounting approval reconciled with SRI Technical Sheet
  v2.32 and the selected invoice schema. — **Blocks**: Complete DR-010 test vectors and boundary
  acceptance criteria.
- **PFV-002 — Emission-date window**: Confirm how far in the past or future a draft emission date
  may be relative to the Ecuadorian civil date at creation. — **Evidence needed**: Product and SRI
  functional approval. — **Blocks**: Date-range acceptance scenarios; impossible dates are already
  prohibited.
- **PFV-003 — Buyer identification authority**: Confirm the allowed active buyer identification
  types and the authoritative validation rule for each, including RUC variants that the SRI warns
  may not have an algorithm, passport, foreign identification, and final-consumer identification.
  — **Evidence needed**: Versioned SRI catalog plus functional approval. — **Blocks**: Exhaustive
  buyer-identification acceptance vectors.
- **PFV-004 — Tax scope and per-line cardinality**: Confirm which tax categories this feature
  supports and whether an invoice line may require one or multiple simultaneous tax rules.
  — **Evidence needed**: Accounting approval against SRI catalogs and invoice rules. — **Blocks**:
  Final tax-rule cardinality and unsupported-tax behavior.
- **PFV-005 — Zero-value drafts and payments**: Confirm whether a draft with a zero net or grand
  total is permitted and, if it is, whether its required payment may have amount zero.
  — **Evidence needed**: Accounting and product approval. — **Blocks**: Zero-value boundary
  scenarios and payment-amount lower bounds.
- **PFV-006 — Duplicate create commands**: Confirm whether retrying the same logical create command
  must return the original draft or create a separate draft, and identify the caller-visible
  idempotency key if deduplication is required. — **Evidence needed**: Product and API-owner
  approval. — **Blocks**: Duplicate and retry semantics.
- **PFV-007 — Collection and text limits**: Confirm maximum invoice-line, payment, and additional
  information counts and the permitted lengths/formats for product codes, descriptions, contact
  fields, and additional-information names and values. — **Evidence needed**: Product limits
  reconciled with SRI Technical Sheet v2.32. — **Blocks**: Maximum-size and over-limit scenarios.
- **PFV-008 — Client-supplied calculated fields**: Confirm whether a target request containing
  client-calculated gross, net, tax, or total fields is rejected as invalid or accepted while those
  fields are ignored. — **Evidence needed**: API-owner approval. — **Blocks**: Exact observable
  behavior for attempted calculated-value injection; such values never become authoritative.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create and Review an Invoice Draft (Priority: P1)

As an authenticated billing operator, I create an invoice draft for an issuer in my tenant so I
can review validated commercial, tax, payment, and calculated total information before any fiscal
issuance occurs.

**Why this priority**: It is the smallest independently valuable billing outcome. It captures and
validates invoice intent while isolating later fiscal numbering, XML, signature, and SRI risks.

**Independent Test**: Supply registered tenant-owned issuer and emission-point references, valid
buyer data, one or more valid lines, active tax and payment references, and matching payment
amounts. The returned persisted draft can be reviewed in full, while sequence, access-key, XML,
certificate, notification, and SRI evidence remain absent.

**Acceptance Scenarios**:

1. **Given** an authenticated billing operator, an active issuer in the operator's tenant, an
   active emission point belonging to that issuer, valid buyer data, at least one valid line, and
   payments equal to the calculated grand total, **when** the operator creates the draft, **then**
   exactly one complete draft is persisted with status `DRAFT`, a unique draft identifier,
   calculated amounts, and creation and last-modification timestamps.
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

### Edge Cases

- An impossible calendar date MUST be rejected rather than normalized; the permitted past/future
  date window remains PFV-002.
- Quantity zero or negative, negative unit price, and negative discount MUST be rejected.
- A discount equal to gross amount produces a zero net line; whether the resulting zero-value draft
  and payment are permitted remains PFV-005.
- A tax rule MUST be active, its parent tax category MUST be active, and its effective period MUST
  include the emission date. Tax cardinality and supported categories remain PFV-004.
- If reference data becomes invalid before the draft is committed, the operation MUST reject the
  request or commit a draft based on one consistent validated reference-data state; it MUST NOT
  persist a partially validated mix.
- Multiple payments that differ from the rounded grand total by any non-zero amount MUST be
  rejected. The rounding sequence used before comparison remains PFV-001.
- Repeated or concurrent equivalent create commands MUST follow the behavior resolved by PFV-006;
  they MUST NOT create a partial or cross-tenant draft.
- Client-supplied calculated values MUST never affect persisted calculations; reject-versus-ignore
  behavior remains PFV-008.
- Buyer identifiers and contact data MUST NOT appear in caller-safe errors, logs, metrics, or
  traces as a consequence of validation failure.
- Values exceeding approved collection or text limits MUST be rejected without partial
  persistence once PFV-007 establishes those limits.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Only an authenticated principal authorized as a billing operator MUST be able to
  create an invoice draft.
- **FR-002**: The service MUST derive the effective tenant from authenticated authorization context
  and MUST NOT trust a tenant identifier supplied only by the request.
- **FR-003**: The selected issuer MUST exist, be active, and belong to the effective tenant. A
  foreign-tenant issuer MUST be indistinguishable from an inaccessible issuer to the caller.
- **FR-004**: The selected emission point MUST exist, be active, and belong to the selected issuer.
- **FR-005**: Issuer legal identity and fiscal information MUST be obtained from the registered
  issuer record and captured as the draft's creation-time issuer information. A caller MUST NOT
  override those values.
- **FR-006**: The emission date MUST be a real Ecuadorian civil date and MUST NOT be silently
  normalized. Its allowed past/future window MUST follow PFV-002 when resolved.
- **FR-007**: Buyer legal name, active identification type, and identification value MUST be
  present. The identification MUST satisfy the approved type-specific rule from PFV-003.
- **FR-008**: Buyer address, email, and telephone MAY be captured as optional contact information;
  none is mandatory for this feature unless PFV-007 establishes a stricter rule.
- **FR-009**: A draft MUST contain at least one invoice line.
- **FR-010**: Every invoice line MUST contain a product or service code, description, quantity
  greater than zero, unit price greater than or equal to zero, absolute discount greater than or
  equal to zero, and the tax-rule selection required by PFV-004.
- **FR-011**: Each selected tax rule, its tax category, and its rate MUST be active and applicable
  on the draft emission date.
- **FR-012**: The service MUST calculate line gross amount, line net amount, tax base, tax amount,
  grouped tax totals, subtotal before taxes, total discount, and grand total. Client-calculated
  values MUST NOT be authoritative and MUST follow the reject-or-ignore outcome from PFV-008.
- **FR-013**: A draft MUST contain at least one payment. Every payment MUST select an active payment
  method and state an exact decimal amount subject to PFV-005.
- **FR-014**: The sum of payment amounts MUST equal the system-calculated grand total after the
  approved rounding pipeline is applied.
- **FR-015**: Optional additional information MUST consist of named textual entries subject to the
  approved PFV-007 limits.
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
- **FR-024**: Cross-tenant denial MUST NOT expose whether the requested issuer, emission point, or
  related data exists outside the caller's effective scope.
- **FR-025**: Validation and failure outcomes MUST use stable machine-readable English error
  categories and safe English messages. They MUST NOT expose buyer identification, issuer secrets,
  internal paths, persistence errors, stack traces, or other sensitive implementation details.
- **FR-026**: Every creation attempt MUST carry or receive a correlation identifier that can be
  used to correlate its safe operational records.
- **FR-027**: Duplicate or retried logical create commands MUST follow the idempotency behavior
  approved through PFV-006.

### Domain Rules and Invariants

- **DR-001**: Official SRI Technical Sheet v2.32 and the active, versioned target catalogs MUST
  govern supported identification, tax, and payment codes. Legacy enums and catalog rows MUST NOT
  become authoritative.
- **DR-002**: `gross amount = quantity × unit price` for each invoice line.
- **DR-003**: `net amount = gross amount − absolute discount` for each invoice line.
- **DR-004**: A line discount MUST NOT exceed its gross amount.
- **DR-005**: For a percentage-based tax rule, the tax base MUST be the line net amount and
  `tax amount = tax base × tax rate`. Any non-percentage tax rule requires explicit approval under
  PFV-004 before inclusion.
- **DR-006**: `subtotal before taxes = sum of all line net amounts`.
- **DR-007**: `total discount = sum of all line absolute discounts`.
- **DR-008**: Tax totals MUST be grouped by tax category code and tax rate and MUST include the
  aggregate tax base and aggregate tax amount for each group.
- **DR-009**: `grand total = subtotal before taxes + sum of all calculated tax amounts`.
- **DR-010**: Quantity, unit price, discount, tax rates, tax bases, tax amounts, payment amounts,
  and invoice totals MUST use exact decimal arithmetic. Binary floating-point arithmetic is
  prohibited. Final monetary amounts MUST have scale two in USD; permitted input/intermediate
  precision, rounding mode, and rounding sequence remain PFV-001.
- **DR-011**: The service is responsible for calculation and payment reconciliation. The caller is
  responsible only for commercial inputs and payment allocations.
- **DR-012**: The emission date MUST use Ecuadorian civil-date semantics. Creation and
  last-modification timestamps MUST be unambiguous instants.
- **DR-013**: Impossible dates, inconsistent totals, inactive or temporally inapplicable catalog
  combinations, unsupported identification types, and unauthorized ownership combinations MUST
  be rejected without normalization or partial persistence.

### Key Entities

- **Invoice Draft**: Internal pre-issuance record identified by a unique draft identifier, owned by
  one tenant and issuer, fixed to USD and `DRAFT`, and containing captured commercial inputs,
  calculated totals, and audit timestamps. It has no official fiscal identity.
- **Issuer**: Active tenant-owned registered legal identity whose fiscal data is authoritative for
  the draft and cannot be overridden by the operator.
- **Emission Point**: Active point belonging to the selected issuer and intended for later fiscal
  issuance without reserving a sequence during draft creation.
- **Buyer**: Named recipient with an active identification type, validated identification value,
  and optional contact information.
- **Invoice Line**: One product or service entry with code, description, positive quantity,
  non-negative unit price and discount, applicable tax rule or rules, and calculated amounts.
- **Tax Rule**: Versioned, effective rule combining a tax category, rate, applicability period, and
  calculation behavior. It is captured for review at draft creation.
- **Tax Total**: System-calculated aggregate tax base and amount for one tax-category-code and
  tax-rate group.
- **Payment**: Exact decimal amount assigned to one active payment method; all payments reconcile
  to the rounded grand total.
- **Additional Information**: Optional named textual entry captured for later review and subject to
  approved limits.

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
- **SC-006**: All cross-tenant test attempts are denied, expose zero foreign-tenant fields, and
  persist zero data.
- **SC-007**: All accepted drafts expose enough persisted captured and calculated information for a
  billing operator to review issuer, emission point, buyer, lines, taxes, payments, totals, status,
  and timestamps without invoking any later authorization capability.
- **SC-008**: The feature's acceptance scenarios can be validated independently of draft update,
  deletion, fiscal numbering, XML, signature, SRI authorization, PDF, and notification features.

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
  and effective tenant scope without trusting request-only tenant identifiers.
- **Dependency**: Registered issuer and emission-point capabilities must provide tenant ownership,
  hierarchy, active state, and authoritative issuer fiscal information.
- **Dependency**: Versioned identification, tax-category, tax-rule, and payment-method catalogs must
  provide active/effective reference data aligned with the official sources cited above.
- **Dependency**: `docs/migration/terminology-mapping.md` is authoritative for the English target
  terms used by this feature.
- **Dependency**: PFV-001 through PFV-008 require resolution or explicit non-blocking disposition
  through `$speckit-clarify` before technical planning.
