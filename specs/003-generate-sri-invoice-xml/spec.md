# Feature Specification: Generate Standard SRI Invoice XML

**Feature Branch**: Not created; no `before_specify` branch hook is configured

**Created**: 2026-07-19

**Status**: Draft

**Input**: Generate, validate, persist, and expose exactly one immutable unsigned SRI Invoice XML
for one Company-owned Invoice Draft that already has one committed Fiscal Preparation.

## Clarifications

### Session 2026-07-19

- Q: How must the committed Fiscal Preparation prove standard Invoice XML `1.1.0` eligibility? →
  A: It must record the selected profile plus a separate assessment for every specialized-profile
  trigger governed by the referenced SRI rule version. Each assessment is conclusively
  `APPLIES`, `DOES_NOT_APPLY`, or `INDETERMINATE`. Standard generation is eligible only when every
  governed trigger is present and `DOES_NOT_APPLY`; `APPLIES` is unsupported, while a missing or
  `INDETERMINATE` assessment is undetermined.
- Q: What success representation should the operation return? → A: Return an
  `application/json` metadata envelope whose `xmlContentBase64` field is the canonical padded
  Base64 encoding of the exact validated and persisted UTF-8 XML bytes. Digest and byte length
  describe the decoded XML bytes, not the Base64 text or JSON envelope.
- Q: How should callers distinguish first creation from replay? → A: The request that commits
  the one artifact returns `201 Created`; an existing-artifact replay or concurrent follower
  returns `200 OK`. Both return the same artifact representation values, with no replay flag or
  mutable lifecycle status in the body.
- Q: Which byte-level formatting contract should first-generation XML use? → A: Use compact XML:
  the declaration is immediately followed by the root, no indentation or inter-element formatting
  whitespace is added, and the closing `</factura>` is the final byte sequence with no trailing
  newline. Whitespace persisted inside business text remains unchanged.

## Scope and Evidence *(mandatory)*

### Bounded Outcome

An upstream billing workflow can target one fiscally prepared, Company-owned Invoice Draft and
obtain one immutable unsigned SRI Invoice XML artifact for the ordinary domestic Invoice profile,
schema version `1.1.0`. The artifact is generated only from the complete persisted Invoice Draft
and its committed Fiscal Preparation, passes the exact official SRI Invoice XSD before it is
committed, and can later serve as the exact immutable input to signing without reconstructing its
commercial or fiscal identity. A later signed derivative may add the required signature while this
unsigned source artifact remains unchanged.

The outcome fixes XML representation only. It does not sign, submit, receive, authorize, issue,
deliver, replace, or mutate the Invoice Draft or its Fiscal Preparation.

### In Scope

- One synchronous operation targeting one existing Invoice Draft by its local identifier.
- Exactly one mandatory Company context supplied as a non-nil UUID in `X-Company-Id`.
- Natural request equivalence defined only by normalized Company Identifier plus Invoice Draft
  identifier, with no caller-generated idempotency key.
- Company-scoped retrieval of a complete Invoice Draft, its one committed Fiscal Preparation, and
  any previously committed Unsigned SRI Invoice XML Artifact.
- Explicit committed eligibility evidence for the ordinary domestic Invoice XML profile
  `STANDARD_DOMESTIC_INVOICE_1_1_0`, including one conclusive result for every specialized-profile
  trigger governed by the referenced SRI rule version, and a fail-closed decision when a mandatory
  specialized profile or field may apply.
- Deterministic generation of the official SRI Invoice XML `1.1.0` representation exclusively from
  persisted commercial, calculated, fiscal-identity, and Fiscal Context Snapshot values.
- Official conditional representation of Special Taxpayer, Withholding Agent, supported RIMPE
  Contributor, and Large Contributor designations when their complete committed evidence applies.
- Validation of the complete unsigned XML against the exact official SRI Invoice XSD `1.1.0`
  before artifact creation.
- All-or-nothing persistence of the exact validated UTF-8 byte sequence, schema version, source
  relationships, integrity evidence, and creation instant.
- Concurrency-safe creation of at most one artifact for one Company and Invoice Draft.
- Exact replay of the committed artifact without XML reconstruction, schema revalidation, current
  fiscal-data access, reference-catalog access, or current-date evaluation.
- Stable, caller-safe Problem Details, safe correlation, deadline behavior, and sensitive-data
  protections.

### Exclusions and Non-Goals

- Export invoices or any export-specific field, trade term, port, country, freight, insurance, or
  customs structure.
- Reimbursements and reimbursement-detail structures.
- Subsidies or subsidy-specific pricing and legend structures.
- Third-party charges or invoice versions `2.0.0` and `2.1.0`.
- An Invoice replacing a delivery guide or carrying an unsupported delivery-guide requirement.
- Fuel-specific, presumptive-withholding, newspaper or magazine, plastic-bag, automatic IVA-refund,
  or other specialized structures not explicitly supported by this profile.
- Commercial-transport auxiliary codes or vehicle-plate requirements.
- Construction-material auxiliary-code requirements.
- Fiscal-machine brand, model, or serial-number extensions.
- Negotiable-invoice requirements or any other mandatory specialized extension for which the
  committed source lacks complete approved fields.
- RIMPE Popular Business while the pinned official Invoice XSD `1.1.0` rejects its mandatory v2.33
  legend; this classification fails closed rather than weakening or modifying the official schema.
- Credit Notes, Debit Notes, Withholding Tax Documents, Delivery Guides, Purchase Settlements, or
  any tax document other than Invoice code `01`.
- Certificate, PKCS#12, certificate-password, or private-key management or access.
- XML digital signature, signature placeholders, signature namespaces, or signed-document storage.
- SRI reception, authorization, consultation, retry, resubmission, reconciliation, cancellation,
  or any other SRI communication.
- RIDE or PDF generation.
- Email, webhook, event, queue, notification, customer delivery, or file-system export.
- Invoice Draft creation, update, recalculation, deletion, cancellation, or lifecycle mutation.
- Fiscal Preparation creation, revalidation, replacement, mutation, sequence allocation, Numeric
  Code generation, or Access Key generation.
- Authentication, authorization, Keycloak, OpenID Connect, OAuth, JWT, API keys, user sessions,
  roles, permissions, or Company entitlement decisions.
- Company, Issuer, Establishment, or Emission Point master-data ownership, administration,
  replication, lookup, or caching.
- Legacy API, payload, response, table, file-system, XML-layout, or status compatibility.
- A new application cache or intermediary artifact cache.

### Authority and Evidence

| Authority | Source and exact version/date or path | Relevance to this feature |
|-----------|---------------------------------------|---------------------------|
| Applicable Ecuadorian legislation | [Regulation for Sales, Withholding, and Complementary Documents, consolidated through Executive Decree 99, Official Register 467, 2023-12-29](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/9fb49475-f058-49a1-b08a-f31bf4deb074/Reglamento_Comprobantes_Venta_RetencionYDC_29122023.pdf), especially Invoice-content, numbering, electronic-document, retention, and issuer-responsibility provisions; [current SRI electronic-invoicing legal index](https://www.sri.gob.ec/facturacion-electronica), reviewed 2026-07-19 | Establishes the legal Invoice content and distinguishes an unsigned pre-submission artifact from a signed, authorized electronic tax document. |
| Official SRI technical documentation | [Technical Sheet for Offline Electronic Tax Documents, v2.33, updated July 2026 and modified 2026-07-13](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/5a547488-80f3-4966-a2a4-841f2e951986/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.33.pdf), section 5.1, Annex 3 pages 65–68, and applicable Annexes 11, 12, 13, 16, and 18–25 | Governs XSD conformance, Invoice XML `1.1.0`, official elements and ordering, two-to-six-decimal quantity and unit price, conditional designations, and specialized-profile requirements. |
| Official SRI schema artifact | [SRI Invoice XML and XSD bundle, published February 2022](https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/05546998-6f29-4870-be3b-62650f312a6c/XML%20y%20XSD%20Factura.zip), specifically the official Invoice schema version `1.1.0` | Governs machine-verifiable structure, sequence, cardinality, lexical constraints, and root attributes. The XSD imports `xmldsig-core-schema.xsd`, but the SRI ZIP does not contain that dependency; planning must pin its exact authoritative offline dependency chain and record every file's path, byte length, and cryptographic digest. |
| Project constitution | `.specify/memory/constitution.md` v2.0.1, approved 2026-07-16 | Governs official-source precedence, English terminology, Company context, fiscal correctness, sensitive data, all-or-nothing outcomes, port-bound XML work, execution safety, and no-cache scope. |
| Approved target source contract | `specs/001-create-invoice-draft/spec.md` and `src/main/resources/META-INF/openapi.yaml`, Invoice Draft contract as of 2026-07-19 | Defines the complete persisted Company-owned Invoice Draft, buyer, ordered lines, IVA selections, grouped totals, payments, additional information, exact decimal values, USD currency, and immutable dates and timestamps. |
| Approved target fiscal contract | `specs/002-prepare-invoice-issuance/spec.md` and `specs/002-prepare-invoice-issuance/contracts/authoritative-fiscal-context.openapi.yaml` v1.0.0 | Defines the immutable Fiscal Preparation, Fiscal Context Snapshot, Official Sequential Number, Numeric Code, Access Key, fiscal designations, and source evidence. Its generic invoice-eligibility assertion is not sufficient by itself for this feature's standard XML profile. |
| Canonical terminology | `docs/migration/terminology-mapping.md`, last verified 2026-07-18 | Governs Invoice, Invoice Draft, Company, Fiscal Preparation, Fiscal Context Snapshot, Official Sequential Number, Numeric Code, Access Key, and related English target terms. Exact official SRI XML names remain SRI-adapter-only terms. |
| Legacy evidence | `docs/legacy/source-baseline.md` (`PROVISIONAL`); `docs/legacy/as-is/05-business-rules.md`; `docs/legacy/as-is/06-validation-rules.md`; `docs/legacy/as-is/07-process-flows.md`; `docs/legacy/as-is/13-technical-debt.md`; `docs/legacy/as-is/14-pending-functional-validation.md` | Historical XML examples and failure candidates only. No legacy generator, XML shape, storage behavior, or validation shortcut is authoritative. |

**Source Conflicts**:

- The legacy implementation generated XML as part of a broader issuance flow and may regenerate
  identifiers or content on retry. This feature commits one unsigned artifact after Fiscal
  Preparation and returns that same artifact on every equivalent replay.
- Legacy XML and validation behavior is historical evidence. The official v2.33 Technical Sheet
  and exact official Invoice XSD `1.1.0` govern every target element, order, cardinality, lexical
  rule, and conditional field.
- Feature 001 used the approved v2.32 catalog baseline for draft-time IVA and payment selection.
  This feature uses the exact official codes and amounts persisted with that draft and does not
  query or substitute a current catalog. V2.33 governs XML shape and profile eligibility.
- Feature 002 contract v1.0.0 establishes generic `invoiceIssuanceEligible` evidence, but it does
  not prove ordinary-profile eligibility or explicitly rule out every mandatory specialized
  extension. Generic evidence remains sufficient for Feature 002's pre-XML outcome but is
  insufficient for Feature 003. Generation requires additional immutable standard-profile
  evidence committed before this operation; an older or generic preparation fails closed without
  being mutated.
- The official XSD bundle is published separately from Technical Sheet v2.33. The exact Invoice
  XSD `1.1.0` governs structural validation, while v2.33 governs current conditional obligations
  and specialized-profile eligibility. Both must pass; neither may be weakened to match the other.
- Technical Sheet v2.33 Annex 22 requires `CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE` for
  RIMPE Popular Business, while the pinned official `factura_V1.1.0.xsd` accepts only
  `CONTRIBUYENTE RÉGIMEN RIMPE`. Because this feature requires both exact-XSD validity and
  fail-closed handling, `POPULAR_BUSINESS` is schema-incompatible and returns
  `INVOICE_XML_PROFILE_UNSUPPORTED` before generation. The schema is not edited, relaxed, or
  replaced.
- The official SRI ZIP omits the relative `xmldsig-core-schema.xsd` imported by
  `factura_V1.1.0.xsd`. The implementation plan must pin the dated authoritative W3C XML Signature
  schema and its DTD dependency chain as immutable offline resources, resolve them locally, and
  prohibit runtime network resolution.
- The official schema permits optional fields that the current Invoice Draft does not own. This
  feature omits an optional field when it has no persisted authoritative source and rejects the
  request when that field is mandatory for the applicable profile.

**Terminology Mapping**: `factura` → Invoice; `borrador de factura` → Invoice Draft; `empresa` →
Company; `secuencial` → Official Sequential Number; `código numérico` → Numeric Code; `clave de
acceso` → Access Key. This feature establishes **Unsigned SRI Invoice XML Artifact** as the English
target term for the immutable, schema-valid, unsigned byte sequence and its integrity evidence.
Exact element names such as `factura`, `infoTributaria`, `infoFactura`, `detalles`, and
`infoAdicional` are official SRI Adapter Only terms and are not target API, domain, or persistence
names outside that boundary.

### Pending Functional Validation

None. The business decision is explicit: only the ordinary domestic Invoice XML `1.1.0` profile is
supported; eligibility must be affirmatively established in committed fiscal evidence; generic,
missing, indeterminate, or specialized-profile evidence fails closed. The current Feature 002
contract gap is a blocking prerequisite described under Assumptions and Dependencies, not license
for generation-time inference. Exact XSD file provenance and adapter choices are planning evidence,
not unresolved functional behavior.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate One Valid Unsigned Invoice XML (Priority: P1)

As an upstream billing workflow, I generate the unsigned XML for one fiscally prepared Invoice
Draft so that the exact commercial and fiscal document can be digitally signed later without
reconstructing its identity or values.

**Why this priority**: A schema-valid unsigned Invoice is the smallest independently useful result
between fiscal preparation and signing. Any incomplete, nonconforming, or regenerated result can
invalidate the later signature or fiscal identity.

**Independent Test**: Start with one complete Company-owned Invoice Draft, one consistent committed
Fiscal Preparation, and affirmative standard-profile eligibility evidence. Request generation and
verify that exactly one artifact is returned, decoding `xmlContentBase64` yields UTF-8 content with
root version `1.1.0`, every represented source value matches the persisted source, it contains no
signature, it passes the exact official XSD, and its stored digest and byte length match the decoded
content.

**Acceptance Scenarios**:

1. **Given** one valid prepared ordinary domestic Invoice with complete standard-profile evidence
   and no existing XML artifact, **when** generation is requested with exactly one valid
   `X-Company-Id`, **then** one unsigned UTF-8 Invoice XML `1.1.0` is generated, validated, committed,
   and returned as canonical padded Base64 in a JSON envelope with its stable identity, source
   identities, integrity evidence, and creation instant using `201 Created`; decoding the content
   yields the required compact byte representation.
2. **Given** the valid source uses environment, Issuer, Establishment, Emission Point, sequence,
   Numeric Code, Access Key, emission date, buyer, lines, tax groups, payments, and totals with
   boundary-valid values, **when** XML is generated, **then** every represented value is mapped from
   the persisted source exactly as specified and no current or caller-supplied replacement is used.
3. **Given** quantity or unit price with two through six significant fractional digits, **when** XML
   is generated, **then** its decimal representation is plain, deterministic, within the official
   two-to-six-decimal envelope, and numerically equal to the persisted value.
4. **Given** accepted names, descriptions, addresses, or additional information containing XML
   metacharacters and supported Unicode, **when** XML is generated and parsed, **then** the parsed
   text equals the persisted text exactly and the byte representation contains one correct XML
   escaping pass with no truncation, replacement, or double escaping.
5. **Given** the committed snapshot marks accounting as required or not required, **when** XML is
   generated, **then** `obligadoContabilidad` contains `SI` or `NO`, respectively, while the
   committed boolean remains unchanged.
6. **Given** the draft currency is `USD`, **when** XML is generated, **then** only the SRI XML
   `moneda` value is `DOLAR`; the draft and artifact metadata do not redefine the persisted
   currency.
7. **Given** the draft does not support tips, **when** XML is generated, **then** `propina` is
   exactly `0.00`, `importeTotal` remains the persisted grand total, and payments remain unchanged.
8. **Given** applicable complete Special Taxpayer, Withholding Agent, supported RIMPE Contributor,
   or Large Contributor evidence, **when** XML is generated, **then** each required official field
   or legend is present in its official location and representation using only committed evidence.
9. **Given** none of those conditional designations applies, **when** XML is generated, **then** the
   corresponding optional elements are absent rather than populated with invented empty, false, or
   placeholder values.
10. **Given** a generated document that violates any official XSD rule, **when** validation runs,
    **then** the request fails closed, no XML content or sensitive validation value is disclosed,
    and no artifact or partial artifact is committed.

---

### User Story 2 - Replay the Exact Committed Artifact (Priority: P1)

As an upstream billing workflow recovering from a retry, timeout, or response loss, I retrieve the
original XML artifact for the same Company and Invoice Draft so that later signing always uses the
same bytes and fiscal identity.

**Why this priority**: Safe replay is inseparable from immutable fiscal identity. Rebuilding on a
retry could change whitespace, escaping, timestamps, catalog interpretation, or conditional
content even when visible amounts appear equal.

**Independent Test**: Commit one artifact, then repeat and race the equivalent request while the
fiscal-context provider and reference catalogs are unavailable and the system date has changed.
Verify that every successful response has the same artifact identifier, source identifiers,
schema version, Base64 content that decodes to byte-equivalent XML, digest, byte length, and creation
instant, with no generation, schema validation, external read, current-date read, or new state.

**Acceptance Scenarios**:

1. **Given** one committed Unsigned SRI Invoice XML Artifact, **when** the same Company and Invoice
   Draft request generation again, **then** `200 OK` returns the original artifact representation
   and exact bytes without rebuilding or revalidating XML and without creating a timestamp or
   artifact.
2. **Given** an existing artifact and later changes or outages in the fiscal-context provider,
   current reference catalogs, official publication site, or system date, **when** replay is
   requested, **then** the original artifact is returned unchanged and none of those sources is
   accessed.
3. **Given** no committed artifact, **when** 100 equivalent requests for the same Company and
   Invoice Draft run concurrently, **then** exactly one committing request returns `201 Created`,
   every other successful request returns `200 OK`, and all return the same artifact identity,
   content, integrity evidence, and timestamp.
4. **Given** the first response is lost after a successful commit, **when** the caller repeats the
   same natural request, **then** the committed artifact is recovered without another artifact,
   XML build, XSD validation, or idempotency key.
5. **Given** an unresolved commit outcome, **when** the caller repeats the same Company and Invoice
   Draft request, **then** replay resolves whether the one artifact exists and never creates a
   replacement for a possible winner.

---

### User Story 3 - Reject Unsafe or Unsupported Generation (Priority: P1)

As the accountable billing and fiscal workflow, I receive a stable, non-sensitive failure when the
source, ownership, profile, generated XML, or commit cannot be trusted so that an invalid or
specialized Invoice is never mistaken for a standard unsigned artifact.

**Why this priority**: Failing closed protects the legal identity and prevents an incomplete
standard XML from bypassing a mandatory specialized extension.

**Independent Test**: Exercise all Company-header, ownership, missing-preparation, relationship,
profile, mandatory-field, XSD, timeout, and persistence-failure vectors. Verify the stable Problem
Details outcome, zero prohibited disclosure, the stated state guarantee, no draft or preparation
mutation, and zero excluded side effects.

**Acceptance Scenarios**:

1. **Given** a missing, repeated, blank, malformed, or nil Company header, **when** generation is
   requested, **then** the stable Company-context error is returned before any Company-owned read
   and no state is created.
2. **Given** an absent draft or a draft owned by another Company, **when** generation is requested,
   **then** the same safe not-found outcome is returned without revealing cross-Company existence
   or data.
3. **Given** a draft without exactly one committed Fiscal Preparation, **when** generation is
   requested, **then** generation is rejected and no XML is built or stored.
4. **Given** mismatched Company, draft, preparation, snapshot, sequence, Access Key, or source
   relationships, **when** generation is requested, **then** a stable inconsistency outcome is
   returned before artifact creation.
5. **Given** only generic invoice eligibility, missing profile evidence, or an indeterminate
   mandatory-extension decision, **when** generation is requested, **then** the standard profile is
   not inferred and no artifact is created.
6. **Given** committed evidence that an export, reimbursement, subsidy, transport, construction,
   fuel, presumptive-withholding, plastic-bag, fiscal-machine, delivery-guide replacement, or other
   unsupported mandatory profile applies,
   **when** generation is requested, **then** the unsupported-profile outcome is returned before XML
   generation.
7. **Given** a mandatory ordinary-profile value is absent, incomplete, out of the XSD envelope, or
   cannot be represented without changing persisted meaning, **when** generation is requested,
   **then** the source-invalid outcome is returned and no artifact is created.
8. **Given** generation, validation, timeout, or persistence failure, **when** the operation ends,
   **then** no certificate, signature, SRI call, sequence allocation, draft mutation, PDF, event,
   queue, notification, or delivery side effect occurs.

### Edge Cases

- Missing, repeated, comma-combined, blank, whitespace-only, malformed, and nil
  `X-Company-Id` values fail before Company-owned reads; accepted UUIDs are canonicalized only for
  Company scope.
- A missing, malformed, or nil Invoice Draft identifier, a non-empty request body, a Company
  identifier outside `X-Company-Id`, any fiscal/XML/calculated value supplied by the caller, or any
  query input is rejected as invalid request content and never affects natural equivalence.
- An artifact found only under another Company is indistinguishable from an absent draft or
  artifact under the authoritative Company scope.
- Zero or multiple Fiscal Preparations, a preparation linked to another draft or Company, a
  snapshot inconsistent with preparation identity, or an Access Key inconsistent with committed
  components fails before XML generation.
- A preparation containing only Feature 002's generic invoice eligibility does not become
  standard-profile eligible by age, absence of known fields, successful prior preparation, or
  caller assertion.
- Positive standard-profile evidence must contain a separate assessment for every v2.33 mandatory
  specialized trigger applicable to invoices. Every assessment must be `DOES_NOT_APPLY`; an absent
  or `INDETERMINATE` assessment is not equivalent to `DOES_NOT_APPLY`, and `APPLIES` establishes an
  unsupported profile.
- A Special Taxpayer or Withholding Agent designation without its required resolution, a RIMPE
  value outside the committed enumeration, or a Large Contributor designation missing either its
  required legend or resolution fails closed.
- `POPULAR_BUSINESS` returns `INVOICE_XML_PROFILE_UNSUPPORTED` before XML generation because its
  mandatory v2.33 legend cannot pass the pinned unmodified official Invoice XSD `1.1.0`.
- If 15 persisted Additional Information entries already consume the official cardinality and a
  mandatory Large Contributor entry also applies, generation fails; no entry is dropped,
  overwritten, merged, or truncated.
- Optional buyer email and telephone are not synthesized as Additional Information. They appear in
  XML only if the draft already contains approved Additional Information entries for them; buyer
  address uses its dedicated official field when present.
- Quantity and unit price use two to six fractional digits without scientific notation. Money and
  tax amounts use exactly two fractional digits. A persisted numeric value outside the official
  lexical or magnitude envelope fails rather than being rounded, clamped, widened, or truncated.
- Persisted line order is preserved. Grouped tax totals, payments, and additional information use
  their defined deterministic order even if persistence returns records in another physical order.
- Empty optional collections do not create empty wrapper elements unless the official XSD requires
  the wrapper. Required collections never serialize empty.
- Element text and attribute values containing `&`, `<`, `>`, quotes, apostrophes, accented text,
  or supported supplementary Unicode are escaped once and round-trip exactly. Invalid XML 1.0
  characters cannot be silently replaced.
- `propina` remains `0.00` for a zero-total or positive-total draft and never changes
  `importeTotal` or payment reconciliation.
- A missing, unreadable, substituted, or digest-mismatched official XSD resource is validator
  unavailability, not successful validation and not a source-data error.
- Schema validation may identify sensitive content internally, but the returned problem, log,
  metric, and trace contain no XML excerpt, element value, Access Key, RUC, buyer data, or address.
- Deadline expiry before a conclusive result returns one timeout outcome. A late completion cannot
  replace it, create a second response, delete a possible winner, or trigger compensation.
- If commit may have succeeded, timeout or persistence failure makes no zero-state claim and directs
  natural replay. A confirmed pre-commit failure or complete rollback leaves no artifact.
- A replay after schema publication changes, source-data changes outside this service, or a later
  unsupported-profile determination still returns the original committed artifact; replay does
  not reinterpret historical source evidence.

## Requirements *(mandatory)*

### Functional Requirements

#### Operation, Input, and Company Scope

- **FR-001**: The service MUST provide one synchronous operation that targets one existing Invoice
  Draft and either returns its one committed Unsigned SRI Invoice XML Artifact or commits and
  returns that artifact as one final outcome.
- **FR-002**: The operation MUST require exactly one nonblank, syntactically valid, non-nil UUID in
  `X-Company-Id` and MAY normalize it only to canonical lowercase hyphenated representation.
- **FR-003**: A missing Company header MUST return `COMPANY_CONTEXT_REQUIRED`; a repeated, blank,
  malformed, comma-combined, or nil value MUST return `COMPANY_CONTEXT_INVALID`.
- **FR-004**: Company context MUST NOT be accepted from a request body, path, query, authentication
  token, user session, or any source other than the approved header.
- **FR-005**: The only caller-selected business resource input MUST be one non-nil Invoice Draft
  identifier. The operation MUST accept no request body and no query parameter.
- **FR-006**: The caller MUST NOT supply or override Company, Issuer, fiscal-context, sequence,
  Numeric Code, Access Key, emission date, currency, calculated amount, tax result, payment result,
  XML field, schema version, profile, artifact identity, digest, or timestamp data in the request
  body, resource path, query string, or any other input location.
- **FR-007**: A malformed or nil Invoice Draft identifier, non-empty body, prohibited input, or
  structurally unsupported request representation MUST return `INVALID_REQUEST` and create no
  artifact.
- **FR-008**: Natural equivalence MUST be exactly the canonical Company Identifier plus Invoice
  Draft identifier. No `Idempotency-Key`, request fingerprint, caller-generated command identifier,
  or correlation identifier may be required or included in equivalence.
- **FR-009**: Every read or mutation involving an Invoice Draft, Fiscal Preparation, Fiscal Context
  Snapshot, or Unsigned SRI Invoice XML Artifact MUST enforce the authoritative Company Identifier.
- **FR-010**: An absent Invoice Draft and a draft owned by another Company MUST both return
  `INVOICE_DRAFT_NOT_FOUND` without revealing cross-Company existence or content.
- **FR-011**: Company scoping MUST remain business partitioning and MUST NOT perform or claim
  authentication, authorization, Company existence validation, Company status validation, or
  caller entitlement validation.
- **FR-012**: The success representation MUST omit Company Identifier unless a later approved
  contract explicitly requires it; request data can never override header context.
- **FR-013**: An absent correlation identifier MUST produce a safe identifier; one valid identifier
  MAY be preserved; repeated, blank, unsafe, or over-length input MUST never be echoed and MUST be
  replaced safely without changing natural equivalence.

#### Replay Precedence and Persisted Sources

- **FR-014**: After request and Company validation, the service MUST use Company-scoped reads to
  load the complete persisted Invoice Draft, its exactly one committed Fiscal Preparation, and any
  existing artifact. Artifact replay selection MUST occur before profile reevaluation, XML
  generation, XSD validation, provider access, catalog access, or current-date access.
- **FR-015**: An existing artifact whose Company, Invoice Draft, and Fiscal Preparation identities
  match the loaded immutable relationships MUST be returned as an equivalent replay with its
  original artifact identity, exact XML bytes encoded under the success contract, schema version,
  source identities, integrity evidence, and creation instant unchanged.
- **FR-016**: Replay MAY verify only persisted ownership and source-identity relationships. It MUST
  NOT revalidate source business values or profile eligibility; rebuild, normalize,
  parse-and-reserialize, schema-validate, or update the XML; generate a new digest or timestamp;
  load current fiscal context or reference catalogs; evaluate current system date; or mutate any
  source or artifact.
- **FR-017**: When no artifact exists, the complete loaded Invoice Draft and Fiscal Preparation MUST
  be used as the exclusive generation sources after the preconditions in FR-018 through FR-030
  pass.
- **FR-018**: A draft without one committed Fiscal Preparation MUST return
  `FISCAL_PREPARATION_REQUIRED`; a draft with more than one candidate preparation or partial fiscal
  identity MUST return `INVOICE_XML_SOURCE_INCONSISTENT`.
- **FR-019**: The Invoice Draft, Fiscal Preparation, Fiscal Context Snapshot, Official Sequential
  Number, Numeric Code, and Access Key MUST all belong to the same authoritative Company and exact
  Invoice Draft and MUST satisfy their committed one-to-one relationships.
- **FR-020**: A missing, contradictory, cross-linked, duplicated, partial, or otherwise inconsistent
  persisted relationship MUST return `INVOICE_XML_SOURCE_INCONSISTENT` before XML generation.
- **FR-021**: First generation MUST use only persisted Invoice Draft and Fiscal Preparation values.
  It MUST perform zero calls to the authoritative Fiscal Context provider, Company service, current
  reference catalogs, SRI services, or current system date.
- **FR-022**: The operation MUST preserve the committed Official Sequential Number, Numeric Code,
  Access Key, emission date, Fiscal Context Snapshot, source evidence, commercial values, calculated
  values, and collection membership without regeneration, recalculation, normalization, or
  substitution.
- **FR-023**: This feature MUST NOT mutate the Invoice Draft, Fiscal Preparation, Fiscal Context
  Snapshot, sequence baseline, Numeric Code, Access Key, source evidence, or their timestamps.

#### Standard Profile Eligibility

- **FR-024**: First generation MUST require immutable committed evidence that explicitly declares
  the exact profile `STANDARD_DOMESTIC_INVOICE_1_1_0` and contains a separate assessment for every
  specialized-profile trigger governed by the evidence's referenced SRI rule version. Each
  assessment MUST be exactly `APPLIES`, `DOES_NOT_APPLY`, or `INDETERMINATE`; standard generation
  is eligible only when every governed trigger is present and `DOES_NOT_APPLY`.
- **FR-025**: A generic invoice-issuance eligibility boolean, successful Fiscal Preparation,
  absence of specialized fields, default value, caller assertion, or inference from commercial
  text MUST NOT satisfy FR-024.
- **FR-026**: Missing, generic, incomplete, not effective for the persisted emission date, or
  indeterminate profile evidence, including an absent governed-trigger assessment or any
  `INDETERMINATE` result, MUST return `INVOICE_XML_PROFILE_UNDETERMINED` before XML generation and
  create no artifact.
- **FR-027**: An `APPLIES` assessment for export, reimbursement, subsidy, third-party charge,
  replacement of a delivery guide, fuel-specific case, presumptive-withholding case,
  commercial-transport requirement, construction-material requirement, fiscal-machine requirement,
  negotiable-invoice requirement, plastic-bag requirement, automatic IVA-refund requirement, or
  any other unsupported mandatory extension
  governed by the referenced rule version, and a committed RIMPE classification of
  `POPULAR_BUSINESS`, MUST return `INVOICE_XML_PROFILE_UNSUPPORTED` before XML generation.
- **FR-028**: The standard-profile evidence MUST be part of immutable fiscal evidence committed
  before this operation and MUST identify the governing SRI technical rule version, its complete
  governed-trigger set, the separate assessment for each trigger, applicable effective evidence,
  and source revision. Generation MUST NOT add, collapse, infer, or repair that evidence.
- **FR-029**: Special Taxpayer, Withholding Agent, RIMPE Contributor, and Large Contributor
  designations are supported conditional attributes of the ordinary profile and MUST NOT by
  themselves classify an otherwise eligible Invoice as an unsupported specialized profile. RIMPE
  Popular Business is the explicit exception in FR-027 because its mandatory v2.33 representation
  is incompatible with the pinned official Invoice XSD `1.1.0`.
- **FR-030**: Every mandatory value for the eligible standard profile MUST already exist in the
  persisted sources with a representation that can satisfy official rules without changing its
  business meaning. Missing, incomplete, or unrepresentable mandatory source data MUST return
  `INVOICE_XML_SOURCE_INVALID`.

#### Official XML Representation

- **FR-031**: The generated byte sequence MUST be XML `1.0`, encoded as UTF-8 without a byte-order
  mark, and begin with the exact declaration `<?xml version="1.0" encoding="UTF-8"?>`. The first
  byte of the root start tag MUST immediately follow that declaration with no intervening byte.
- **FR-032**: The root MUST be exactly `<factura id="comprobante" version="1.1.0">` in no namespace,
  and the complete document MUST contain no digital-signature element, signature namespace,
  certificate data, processing instruction other than the XML declaration, or application-added
  comment.
- **FR-033**: The document MUST use the exact official SRI element names, nesting, sequence,
  cardinality, optionality, lexical formats, and root attributes defined jointly by the official
  Invoice XSD `1.1.0` and applicable v2.33 rules.
- **FR-034**: The root children emitted by this profile MUST appear in this order:
  `infoTributaria`, `infoFactura`, `detalles`, and optional `infoAdicional`. Unsupported root
  sections, including `retenciones`, MUST be absent.
- **FR-035**: Within `infoTributaria`, present elements MUST appear in this official order:
  `ambiente`, `tipoEmision`, `razonSocial`, optional `nombreComercial`, `ruc`, `claveAcceso`,
  `codDoc`, `estab`, `ptoEmi`, `secuencial`, `dirMatriz`, optional `agenteRetencion`, and optional
  `contribuyenteRimpe`.
- **FR-036**: `ambiente`, `tipoEmision`, `razonSocial`, `nombreComercial`, `ruc`, `claveAcceso`,
  `codDoc`, `estab`, `ptoEmi`, `secuencial`, and `dirMatriz` MUST map respectively from the committed
  environment code, emission type, Legal Name, optional Commercial Name, Issuer RUC, Access Key,
  document type code `01`, Establishment Code, Emission Point Code, Official Sequential Number, and
  registered Head Office Address.
- **FR-037**: Within `infoFactura`, present elements MUST appear in this official order:
  `fechaEmision`, `dirEstablecimiento`, optional `contribuyenteEspecial`,
  `obligadoContabilidad`, `tipoIdentificacionComprador`, `razonSocialComprador`,
  `identificacionComprador`, optional `direccionComprador`, `totalSinImpuestos`,
  `totalDescuento`, `totalConImpuestos`, `propina`, `importeTotal`, `moneda`, and `pagos`.
- **FR-038**: `fechaEmision` MUST represent the unchanged persisted Ecuador civil date as
  `dd/MM/yyyy`; it MUST NOT use the request date, artifact creation date, current date, or a
  timezone conversion that changes the civil date.
- **FR-039**: `dirEstablecimiento`, buyer identification type, buyer Legal Name, buyer
  identification, and optional buyer address MUST map from the committed Establishment Address and
  persisted buyer values without external lookup or text rewriting.
- **FR-040**: `totalSinImpuestos`, `totalDescuento`, and `importeTotal` MUST equal the persisted
  subtotal before taxes, total discount, and grand total, respectively.
- **FR-041**: `totalConImpuestos` MUST contain one `totalImpuesto` for every persisted grouped tax
  total and no other group. Each entry MUST use its persisted official tax code, official percentage
  code, tax base, and tax amount in the official element order and representation.
- **FR-042**: `propina` MUST be exactly `0.00`. It MUST NOT be added to, subtracted from, or otherwise
  change persisted grand total, grouped taxes, line values, or payment amounts.
- **FR-043**: Persisted currency `USD` MUST map to `moneda` value `DOLAR` only inside the SRI XML
  representation. No persisted or returned non-XML currency value may be changed to `DOLAR`.
- **FR-044**: Persisted `accountingRequired=true` MUST map to `obligadoContabilidad` value `SI` and
  `false` MUST map to `NO`; the boolean itself MUST remain unchanged.
- **FR-045**: `detalles` MUST contain exactly one `detalle` for each persisted Invoice Line in
  ascending persisted line position and no generated, omitted, merged, or reordered line.
- **FR-046**: Each `detalle` MUST map `codigoPrincipal`, `descripcion`, `cantidad`,
  `precioUnitario`, `descuento`, and `precioTotalSinImpuesto` from product code, description,
  quantity, unit price, discount, and line net amount, respectively. `codigoAuxiliar` and
  `detallesAdicionales` MUST be absent for this profile because the persisted source does not own
  approved values for them.
- **FR-047**: Each `detalle/impuestos` MUST contain exactly the persisted IVA tax selection for that
  line. Its `codigo`, `codigoPorcentaje`, `tarifa`, `baseImponible`, and `valor` MUST equal the
  persisted official tax code, official percentage code, rate, tax base, and tax amount.
- **FR-048**: `pagos` MUST contain exactly one `pago` for every persisted Payment, ordered
  deterministically by official payment code and then stable payment-method identity. Each entry
  MUST map `formaPago` and `total` from the persisted official code and amount; unsupported or
  absent payment-term fields MUST be omitted.
- **FR-049**: Optional `infoAdicional` MUST contain each persisted Additional Information entry as
  one `campoAdicional` whose `nombre` attribute and element text equal the persisted name and value,
  in stable persisted position order.
- **FR-050**: Buyer email and telephone MUST NOT be synthesized into `infoAdicional`. They may appear
  only when an approved persisted Additional Information entry already represents them.
- **FR-051**: When a complete Large Contributor designation applies, `infoAdicional` MUST also
  contain the official mandatory Large Contributor entry after persisted entries, using only the
  committed required legend and resolution identifier in the exact v2.33 representation. An
  incomplete pair or exhausted official cardinality MUST return `INVOICE_XML_SOURCE_INVALID`.
- **FR-052**: A complete Special Taxpayer designation MUST map its committed resolution identifier
  to `contribuyenteEspecial`; otherwise that element MUST be absent.
- **FR-053**: A complete Withholding Agent designation MUST map its committed resolution identifier
  to `agenteRetencion` in the exact official numeric representation; otherwise that element MUST be
  absent.
- **FR-054**: RIMPE classification `RIMPE_CONTRIBUTOR` MUST map to exact text
  `CONTRIBUYENTE RÉGIMEN RIMPE`; `NONE` MUST omit `contribuyenteRimpe`;
  `POPULAR_BUSINESS` MUST fail under FR-027 before XML generation and MUST NOT emit an altered,
  omitted, or schema-invalid substitute legend.
- **FR-055**: An optional XML element MUST be omitted when its authoritative persisted source is
  absent. The generator MUST NOT use empty elements, placeholders, defaults, current values, or
  inferred values unless this specification explicitly requires the fixed value.

#### Decimal, Text, and Deterministic Byte Rules

- **FR-056**: All numeric XML values MUST use plain base-10 notation with `.` as decimal separator,
  no grouping separator, no sign for non-negative values, no exponent, and no loss of numeric
  equality.
- **FR-057**: Quantity and unit price MUST serialize with at least two and at most six fractional
  digits. Trailing zeroes beyond the minimum two MUST be removed; significant persisted fractional
  digits MUST be preserved; no value may be rounded to enter the envelope.
- **FR-058**: Money, tax base, tax amount, discount, payment, tip, subtotal, and grand-total values
  MUST serialize with exactly two fractional digits and remain numerically identical to their
  persisted two-decimal values.
- **FR-059**: IVA rate values MUST serialize deterministically with exactly two fractional digits
  when the official element is emitted and MUST equal the persisted rate.
- **FR-060**: Every accepted business or fiscal text value MUST be XML-escaped exactly once in
  element text or attribute context as applicable. Parsing the generated XML MUST recover the exact
  persisted Unicode value without truncation, silent replacement, normalization, case conversion,
  whitespace rewriting, or double escaping.
- **FR-061**: A persisted character not representable in XML `1.0`, a text value outside the exact
  XSD constraints, or a value that would require lossy conversion MUST return
  `INVOICE_XML_SOURCE_INVALID`; the generator MUST NOT remove or replace the value.
- **FR-062**: The first-generation byte sequence MUST be deterministic for the same complete
  persisted sources and schema/profile rules. Physical database row order, locale, process
  timezone, current date, and correlation identifier MUST NOT change it.
- **FR-063**: The XML MUST be compact: the generator MUST add no indentation, line break, or other
  formatting whitespace between elements; the closing `</factura>` MUST be the final byte sequence
  with no trailing newline or other byte. Whitespace inside business text MUST appear exactly when
  and as persisted, and every such byte MUST be included in the integrity digest.

#### Schema Validation, Artifact Persistence, and Concurrency

- **FR-064**: The completed unsigned byte sequence MUST be validated against the exact official SRI
  Invoice XSD `1.1.0`, including its exact dependencies, before any artifact is committed.
- **FR-065**: The selected official XSD distribution URL, retrieval date, exact repository resource
  path, file byte length, and SHA-256 digest MUST be recorded as validation provenance before
  implementation acceptance. A different or modified schema MUST not be substituted silently.
- **FR-066**: Validation MUST cover the final exact byte sequence intended for persistence, not an
  intermediate object model, partial tree, normalized copy, or post-persistence reconstruction.
- **FR-067**: Any XSD violation MUST return `INVOICE_XML_SCHEMA_INVALID`, disclose no XML or
  sensitive value, and commit no artifact, digest, source link, or creation timestamp.
- **FR-068**: A missing, unreadable, untrusted, dependency-incomplete, or provenance-mismatched XSD
  resource MUST return `INVOICE_XML_VALIDATOR_UNAVAILABLE` and MUST never be treated as successful
  validation.
- **FR-069**: A successful first request MUST atomically persist one immutable Unsigned SRI Invoice
  XML Artifact containing the exact validated UTF-8 bytes, schema version `1.1.0`, one opaque
  artifact identifier, the authoritative Company ownership reference, source Invoice Draft
  identity, source Fiscal Preparation identity, integrity evidence, and one creation instant.
- **FR-070**: Integrity evidence MUST contain algorithm identifier `SHA-256`, the lowercase
  64-hexadecimal SHA-256 digest of the exact persisted UTF-8 bytes, and the exact byte length.
- **FR-071**: The digest and byte length MUST be computed from the same final byte sequence that was
  schema-validated and committed. They MUST NOT cover a decoded string, reserialized document,
  signed derivative, compressed form, or transport envelope.
- **FR-072**: One Company-owned Invoice Draft MUST have at most one Unsigned SRI Invoice XML
  Artifact, and one artifact MUST reference exactly one Invoice Draft and that draft's exact one
  Fiscal Preparation.
- **FR-073**: Artifact content, schema version, identifier, Company ownership, source relationships,
  digest, byte length, and creation instant MUST be immutable after commit; this feature MUST expose
  no update, replace, delete, or regenerate behavior.
- **FR-074**: Concurrent equivalent first-generation requests MUST converge on one committed
  artifact. Exactly the committing contender MUST return `201 Created`; every other successful
  contender MUST return `200 OK`; all MUST return the committed winner with identical artifact
  representation values.
- **FR-075**: A failed generation or confirmed complete rollback MUST leave no artifact or partial
  artifact. A commit whose outcome cannot be determined safely MUST return
  `INVOICE_XML_OUTCOME_UNKNOWN`, make no zero-state claim, and direct natural replay.
- **FR-076**: The successful representation MUST be an `application/json` metadata envelope that
  exposes the artifact identifier, Invoice Draft identifier, Fiscal Preparation identifier, schema
  version, SHA-256 algorithm and digest, byte length, and original creation instant. Its
  `xmlContentBase64` field MUST be the canonical RFC 4648 standard-alphabet Base64 encoding with
  required padding of the exact validated and persisted UTF-8 XML bytes. The decoded bytes, not the
  Base64 characters or JSON envelope, are the XML content covered by the digest and byte length.
  The representation MUST not expose a mutable status that implies signing, submission, receipt,
  authorization, or issuance.
- **FR-077**: The request that successfully commits the artifact MUST return `201 Created`. A
  request that returns an artifact already committed before its result selection, including a
  concurrent follower, MUST return `200 OK`. Both outcomes MUST return the same artifact
  representation values; the body MUST contain no replay indicator or mutable lifecycle status.
  Every successful representation MUST be marked non-cacheable.

#### Deadline, Errors, Privacy, and Side Effects

- **FR-078**: One overall 10-second deadline MUST begin at initial request entry and govern input
  validation, Company-scoped lookup, source loading, profile checks, generation, XSD validation,
  commit, and result selection.
- **FR-079**: A result conclusively selected before overall expiry MUST win; otherwise
  `REQUEST_TIMEOUT` MUST win. A late result MUST NOT replace an already selected result or produce
  a second response.
- **FR-080**: `REQUEST_TIMEOUT` MUST claim zero artifact state only when commit did not begin or a
  complete rollback is confirmed. If commit may have succeeded, the outcome MUST direct natural
  replay without generating a replacement.
- **FR-081**: Every failure MUST use the service-wide Problem Details representation with one stable
  machine-readable code, safe English detail, and safe correlation identifier.
- **FR-082**: No error detail, violation, log, metric label, trace, health signal, or exception
  exposed outside the protected artifact response may contain XML content or excerpts, Access Key,
  Numeric Code, RUC, buyer identification, names, addresses, product descriptions, Additional
  Information, schema-invalid values, or other fiscal payload values.
- **FR-083**: Operational evidence MAY identify only safe opaque artifact, draft, preparation, and
  correlation identifiers, outcome code, schema-version identifier, duration, and non-sensitive
  counts; it MUST NOT use a digest as a public correlation token or metric label.
- **FR-084**: Successful XML content is sensitive fiscal payload and MUST be exposed only in the
  explicit success representation and retained artifact, never in examples containing real data or
  general telemetry.
- **FR-085**: The operation MUST perform zero certificate or PKCS#12 access, signature work, SRI
  communication, sequence allocation, Numeric Code generation, Access Key generation, draft or
  preparation mutation, RIDE/PDF generation, event publication, queueing, email, webhook,
  notification, or customer delivery.
- **FR-086**: A successful outcome MUST NOT mark or describe the Invoice, Invoice Draft, Fiscal
  Preparation, or artifact as signed, submitted, received, authorized, rejected by SRI, fiscally
  issued, or legally valid as an authorized electronic document.
- **FR-087**: The operation MUST create no Company, Issuer, Establishment, or Emission Point master
  data, replica, lookup cache, or administration capability and MUST introduce no application cache.
- **FR-088**: XML generation and schema validation MUST remain independently replaceable governed
  capabilities at the application boundary, and their CPU-intensive work MUST preserve the
  service's governed request-execution safety. Detailed technical choices belong to planning.

### Stable Error Catalog

| Error code | Required meaning | State and disclosure guarantee |
|------------|------------------|--------------------------------|
| `COMPANY_CONTEXT_REQUIRED` | The one required Company header is missing. | No Company-owned read or write. |
| `COMPANY_CONTEXT_INVALID` | Company header is repeated, blank, malformed, comma-combined, or nil. | No Company-owned read or write. |
| `INVALID_REQUEST` | Draft identifier or request representation is malformed or contains prohibited input. | No source use, XML work, or artifact state. |
| `INVOICE_DRAFT_NOT_FOUND` | No draft exists in the authoritative Company scope. | No cross-Company disclosure and no artifact state. |
| `FISCAL_PREPARATION_REQUIRED` | The draft lacks exactly one committed Fiscal Preparation. | No XML work or artifact state. |
| `INVOICE_XML_SOURCE_INCONSISTENT` | Persisted ownership, identity, or relationship invariants contradict one another. | No XML work or artifact state; no sensitive values disclosed. |
| `INVOICE_XML_PROFILE_UNDETERMINED` | Explicit ordinary-profile eligibility or mandatory-extension evidence is missing, generic, incomplete, or indeterminate. | No XML work or artifact state. |
| `INVOICE_XML_PROFILE_UNSUPPORTED` | A mandatory specialized profile or field outside this feature applies. | No XML work or artifact state. |
| `INVOICE_XML_SOURCE_INVALID` | A required persisted value is absent or cannot be represented under official rules without changing meaning. | No committed artifact and no rejected value disclosed. |
| `INVOICE_XML_GENERATION_FAILED` | The governed XML generator failed without producing a trustworthy final document. | No committed artifact and no XML excerpt disclosed. |
| `INVOICE_XML_SCHEMA_INVALID` | The completed unsigned XML violates the exact official Invoice XSD `1.1.0`. | No artifact, digest, source link, or XML disclosure. |
| `INVOICE_XML_VALIDATOR_UNAVAILABLE` | The exact trusted XSD or validation capability is unavailable or provenance-invalid. | Validation never bypassed; no artifact. |
| `PERSISTENCE_FAILURE` | Local failure conclusively left no committed artifact. | No artifact; safe retry is permitted. |
| `INVOICE_XML_OUTCOME_UNKNOWN` | Artifact commit outcome cannot be determined safely. | No zero-state claim; natural replay is required. |
| `REQUEST_TIMEOUT` | The overall deadline won before a result became conclusive. | Zero-state claim only when known; otherwise natural replay. |
| `INTERNAL_ERROR` | An unexpected safely classified service failure occurred. | No internal or fiscal payload disclosure. |

### Domain Rules and Invariants

- **DR-001**: One Company-owned Invoice Draft has at most one immutable Unsigned SRI Invoice XML
  Artifact, and that artifact belongs to exactly one committed Fiscal Preparation of the same
  Company and draft.
- **DR-002**: The artifact is an unsigned pre-signing representation. It is not a signed,
  submitted, received, authorized, or fiscally issued Invoice.
- **DR-003**: The Invoice Draft is the sole commercial and monetary source; the Fiscal Preparation
  is the sole fiscal-identity and Fiscal Context Snapshot source; neither source is recalculated or
  refreshed during generation.
- **DR-004**: Standard-profile eligibility is affirmative evidence. Unknown, generic, or absent
  eligibility is ineligible for generation even when the ordinary XML could happen to pass the
  XSD.
- **DR-005**: XSD validity is necessary but not sufficient: applicable v2.33 conditional and
  profile rules MUST also be satisfied before commit.
- **DR-006**: Invoice line order is the persisted business-significant position order. Grouped tax
  total order is the canonical Feature 001 tax-group order. Payment order is official code then
  stable method identity. Additional Information order is persisted position followed by the
  mandatory Large Contributor entry when applicable.
- **DR-007**: Every XML money value uses the persisted exact two-decimal value. Quantity and unit
  price retain exact numeric value with two to six fractional digits. No XML mapping owns a new
  monetary calculation or rounding decision.
- **DR-008**: `propina=0.00` is a required XML constant for the unsupported-tip profile and does not
  participate in the persisted grand total calculation.
- **DR-009**: `USD` → `DOLAR`, boolean → `SI`/`NO`, Ecuador civil date → `dd/MM/yyyy`, and supported
  RIMPE Contributor → exact official legend are SRI-representation mappings only; source values
  remain unchanged. Popular Business has no schema-valid mapping under the pinned official XSD and
  fails before generation.
- **DR-010**: Text escaping changes XML syntax bytes but not the parsed business value. Escaping is
  neither business-text normalization nor permission to alter unsupported characters.
- **DR-011**: The artifact digest identifies exact unsigned bytes, not semantic XML equivalence.
  The compact layout is part of those bytes. Any whitespace, declaration, escaping, ordering, or
  byte change produces a different digest and is prohibited after commit.
- **DR-012**: Artifact creation time is one unambiguous instant recorded only for a successful first
  commit. It is not an Invoice emission date, SRI receipt time, authorization time, or issuance
  time.
- **DR-013**: Company ownership is mandatory business partitioning metadata on every owned-data
  operation and is not authentication or authorization.
- **DR-014**: Required absence and optionality are explicit. No undocumented null, empty element,
  inferred default, or placeholder may stand in for missing official data.
- **DR-015**: Natural replay, not regeneration or compensation, is the only recovery mechanism for
  a possibly committed artifact.

### Official Source-to-XML Mapping Summary

| XML location | Official field | Persisted source or fixed rule |
|--------------|----------------|--------------------------------|
| `/factura/@id` | `id` | Fixed `comprobante` |
| `/factura/@version` | `version` | Fixed `1.1.0` |
| `infoTributaria` | `ambiente`, `tipoEmision` | Fiscal Context Snapshot environment and emission-type codes |
| `infoTributaria` | `razonSocial`, optional `nombreComercial`, `ruc`, `dirMatriz` | Committed Legal Name, optional Commercial Name, Issuer RUC, and Head Office Address |
| `infoTributaria` | `claveAcceso`, `codDoc`, `estab`, `ptoEmi`, `secuencial` | Committed Access Key, `01`, Establishment Code, Emission Point Code, and Official Sequential Number |
| `infoTributaria` | optional `agenteRetencion`, optional `contribuyenteRimpe` | Complete Withholding Agent evidence and exact supported RIMPE Contributor legend mapping; Popular Business fails before generation |
| `infoFactura` | `fechaEmision`, `dirEstablecimiento` | Persisted emission date formatted `dd/MM/yyyy`; committed Establishment Address |
| `infoFactura` | optional `contribuyenteEspecial`, `obligadoContabilidad` | Complete Special Taxpayer resolution; committed boolean mapped to `SI`/`NO` |
| `infoFactura` | buyer fields | Persisted buyer identification type, Legal Name, identification, and optional address |
| `infoFactura` | `totalSinImpuestos`, `totalDescuento`, `totalConImpuestos` | Persisted subtotal, discount total, and every grouped tax total |
| `infoFactura` | `propina`, `importeTotal`, `moneda` | Fixed `0.00`, persisted grand total, and `USD` mapped to `DOLAR` |
| `infoFactura/pagos` | each `pago` | Every persisted payment's official method code and amount |
| `detalles` | each `detalle` | Every persisted line in position order, including product, quantity, price, discount, net, and IVA result |
| `infoAdicional` | each `campoAdicional` | Persisted Additional Information plus mandatory committed Large Contributor evidence when applicable |

### Key Entities

- **Invoice Draft**: Existing immutable Company-owned commercial source from Feature 001. It owns
  the buyer, ordered lines, persisted IVA selections and results, grouped tax totals, payments,
  Additional Information, USD currency, emission date, calculated totals, and local identifier.
- **Fiscal Preparation**: Existing immutable Company-owned fiscal source from Feature 002, linked
  one-to-one to the Invoice Draft. It owns the Fiscal Context Snapshot, Official Sequential Number,
  Numeric Code, Access Key, source evidence, and creation instant. This feature requires additional
  committed standard-profile eligibility evidence and never mutates the preparation.
- **Standard Invoice XML Profile Evidence**: Immutable evidence committed before generation that
  identifies ordinary domestic Invoice XML `1.1.0`, the governing SRI rule version and its complete
  specialized-trigger set, and one `APPLIES`, `DOES_NOT_APPLY`, or `INDETERMINATE` assessment per
  trigger. It establishes eligibility only when every governed trigger is present and
  `DOES_NOT_APPLY`.
- **Unsigned SRI Invoice XML Artifact**: One immutable Company-owned result identified naturally by
  Company plus Invoice Draft and also carrying one opaque artifact identifier. It contains exact
  schema-valid UTF-8 bytes, schema version, source Invoice Draft and Fiscal Preparation identities,
  SHA-256 digest, byte length, and creation instant. It has no mutable lifecycle status.
- **XML Integrity Evidence**: Immutable `SHA-256` algorithm identifier, lowercase 64-hexadecimal
  digest, and byte length calculated over the exact validated and persisted unsigned bytes.
- **Official Invoice Schema**: Exact versioned SRI Invoice XSD `1.1.0` plus dependencies and recorded
  provenance used only to validate first-generation output; it is not current reference data and
  is not consulted during artifact replay.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every valid first-generation request that becomes conclusive within the service-wide
  10-second deadline returns exactly one complete artifact using `201 Created`; no provisional,
  partial, or post-deadline second result is externally visible.
- **SC-002**: One hundred percent of supported-profile acceptance fixtures pass the exact official
  Invoice XSD `1.1.0` and applicable v2.33 rules before persistence; every one-rule mutation fails
  before artifact creation.
- **SC-003**: In 100% of successful artifacts, every represented fiscal identifier equals its
  committed Fiscal Preparation value and every represented commercial amount, tax value, payment,
  date, and text equals its persisted Invoice Draft or Fiscal Context Snapshot source after only
  the explicitly approved SRI representation mapping.
- **SC-004**: Across 100 concurrent equivalent first-generation requests for one Company and
  Invoice Draft, exactly one artifact is committed, exactly one outcome is `201 Created`, every
  other successful outcome is `200 OK`, and all return one artifact identifier, one byte sequence,
  one digest, one byte length, and one creation instant.
- **SC-005**: Every equivalent replay returns identical Base64 content that decodes to byte-for-byte
  identical XML using `200 OK`, plus identical identity, schema version, source links, integrity
  evidence, and timestamp, while performing zero XML builds, XSD validations, provider calls,
  current-catalog reads, current-date reads, or writes.
- **SC-006**: For every successful artifact, decoding `xmlContentBase64` and independently hashing
  the decoded bytes with SHA-256 produces the stored 64-hexadecimal digest; independently counting
  those decoded bytes produces the stored byte length; the declaration is adjacent to the root,
  no formatting whitespace occurs between elements, and `</factura>` is the final byte sequence.
- **SC-007**: Every missing, repeated, blank, malformed, comma-combined, and nil Company-header
  vector produces its specified stable error and zero Company-owned access; every cross-Company
  draft vector returns the safe not-found result and exposes zero other-Company data.
- **SC-008**: Every absent, duplicate, partial, cross-linked, or inconsistent Fiscal Preparation
  vector produces its specified failure before XML generation and leaves zero artifact state.
- **SC-009**: Every generic, missing, incomplete, absent-trigger, or `INDETERMINATE`
  profile-evidence vector returns `INVOICE_XML_PROFILE_UNDETERMINED`; every governed trigger marked
  `APPLIES` and every `POPULAR_BUSINESS` classification returns
  `INVOICE_XML_PROFILE_UNSUPPORTED`; none builds or stores XML.
- **SC-010**: Every XML metacharacter and approved Unicode fixture round-trips to its exact persisted
  text after parsing, with zero truncated, silently replaced, normalized, or double-escaped values.
- **SC-011**: Every numeric fixture uses plain official representation: quantity and unit price have
  two to six fractional digits, money has exactly two, all parsed values equal persisted values,
  and zero values are rounded or widened to gain XSD validity.
- **SC-012**: Every XSD-invalid, source-invalid, validator-unavailable, confirmed persistence-failure,
  and confirmed rollback vector leaves zero committed artifact, digest, link, or creation instant;
  every unknown commit outcome makes no zero-state claim and is resolved by natural replay.
- **SC-013**: In 100% of success and failure vectors, the Invoice Draft, Fiscal Preparation, Fiscal
  Context Snapshot, sequence baseline, Official Sequential Number, Numeric Code, Access Key, and
  their timestamps remain unchanged.
- **SC-014**: Every success and failure performs zero certificate, signing, SRI, sequence-allocation,
  current-fiscal-context, current-catalog, PDF/RIDE, queue, event, email, webhook, notification, and
  customer-delivery side effects.
- **SC-015**: Every error response and operational signal contains zero XML excerpts, complete
  Access Keys, Numeric Codes, RUCs, buyer identifiers, names, addresses, descriptions, Additional
  Information values, schema-invalid values, stack traces, persistence statements, or internal
  endpoints.
- **SC-016**: One hundred percent of success representations describe an unsigned artifact and zero
  outcomes mark the Invoice as signed, submitted, received, authorized, fiscally issued, or legally
  valid as an authorized electronic document.
- **SC-017**: All acceptance scenarios are verifiable without certificate access, digital signing,
  SRI connectivity, draft mutation, Fiscal Preparation mutation, another tax-document type,
  authentication, authorization, Company master-data ownership, or an application cache.

## Assumptions and Dependencies

- **Assumption**: Feature 001 is the approved, complete, immutable source of Company ownership,
  buyer, line order, commercial text, IVA codes and values, payments, Additional Information,
  emission date, USD currency, and calculated totals. — **Basis**:
  `specs/001-create-invoice-draft/spec.md` and the source-of-truth contract under
  `src/main/resources/META-INF`.
- **Assumption**: Feature 002 is the approved immutable source of Fiscal Context Snapshot,
  Official Sequential Number, Numeric Code, Access Key, and source evidence, and each target draft
  has zero or one preparation. — **Basis**: `specs/002-prepare-invoice-issuance/spec.md`.
- **Blocking contract dependency**: Before first-generation implementation can be accepted, the
  accountable Fiscal Context Provider Owner and Feature 002 contract owner must approve a
  versioned contract evolution that identifies the exact standard Invoice XML profile, defines the
  complete specialized-trigger set for the referenced SRI rule version, and returns a separate
  `APPLIES`, `DOES_NOT_APPLY`, or `INDETERMINATE` assessment for every trigger. Feature 002 must
  persist that immutable decision with its governing rule/effective evidence. Existing generic
  `invoiceIssuanceEligible=true` is not migrated, inferred, or rewritten by Feature 003. A
  preparation lacking the new evidence remains readable but returns
  `INVOICE_XML_PROFILE_UNDETERMINED` for first generation.
- **Dependency**: The official SRI Invoice XML and XSD bundle published February 2022 supplies the
  exact `1.1.0` schema artifact but omits its declared XML Signature dependency; SRI Technical Sheet
  v2.33, modified 2026-07-13, supplies current conditional and specialized-profile rules. Planning
  must preserve an approved repository copy of the exact schema, pin the dated authoritative W3C
  dependency chain as offline resources, and record every source URL, retrieval date, file path,
  byte length, and SHA-256 digest. Production validation must not depend on SRI or W3C site
  availability.
- **Assumption**: Optional buyer email and telephone are not automatically invented as
  `campoAdicional` values; only persisted Additional Information and mandatory committed Large
  Contributor evidence populate `infoAdicional`. — **Basis**: The ordinary official schema has no
  dedicated buyer email or telephone element, and the feature must not invent optional XML fields.
- **Assumption**: Payment term and unit fields, auxiliary product codes, line additional details,
  delivery-guide references, and other optional structures are omitted because the current Invoice
  Draft owns no approved values for them. Their future support requires a separately approved
  feature or clarification. — **Basis**: Features 001 and 002 persisted contracts and this feature's
  exclusions.
- **Dependency**: The platform retention and protected-storage policy applicable to the Invoice
  record also governs the sensitive unsigned XML artifact, integrity evidence, and backups. The
  accountable Platform Operations Owner must provide approved encryption-at-rest, encrypted-backup,
  restore, retention, and disposal evidence. This feature does not add a deletion operation,
  custom key-management system, or independent retention scheduler.
- **Dependency**: The project constitution's mandated technology baseline, Clean Architecture
  boundaries, application-port isolation for XML and schema validation, non-event-loop execution
  of CPU-intensive work, JVM execution, native-compatibility evaluation, null-safety, and no-cache
  rules govern implementation planning. This specification selects no replacement stack or
  architecture.
- **Assumption**: The success caller is an internal upstream billing workflow permitted by external
  platform controls to receive sensitive fiscal payload; this service validates Company syntax and
  ownership scope but does not authenticate or authorize that caller. — **Basis**: Constitution
  v2.0.1 Principles VII and XVI.
- **Readiness statement**: The observable XML, validation, immutability, concurrency, replay,
  failure, and privacy behavior is fully specified. Implementation planning may proceed only with
  the blocking profile-evidence contract dependency explicitly scheduled and without expanding
  this feature into fiscal-context resolution, signing, SRI communication, or another XML profile.
