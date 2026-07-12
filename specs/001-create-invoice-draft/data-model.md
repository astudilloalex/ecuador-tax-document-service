# Data Model: Create Invoice Draft

**Feature**: `001-create-invoice-draft`

**Database**: PostgreSQL 18.4

**Schema authority**: Flyway versioned migrations only

## Modeling Rules

- Domain types are pure Java records/classes/value objects and use `BigDecimal`, `LocalDate`,
  `Instant`, and `UUID`. They contain no Panache, Hibernate, JSON, OIDC, or Mutiny types.
- Panache entities are infrastructure persistence models. API DTOs, application commands/results,
  domain objects, and persistence entities have explicit mappers.
- Fiscal decimals use unconstrained PostgreSQL `numeric` plus named constraints and application
  validation. No floating-point column is permitted.
- Emission date uses PostgreSQL `date`; audit instants use `timestamptz(6)`.
- Company, Issuer, and emission-point identifiers reference an external authoritative capability,
  so the draft stores verified identifiers and creation-time snapshots rather than local foreign
  keys to copied authority tables.
- Versioned identification, tax, and payment catalogs are local read-only reference tables. They
  are created and evolved only through Flyway; this feature has no catalog-management operation.
- Child collections use relational tables so ownership, uniqueness, sign, and row-local monetary
  invariants are database-enforced. Aggregate sums/counts are domain invariants and are tested
  against PostgreSQL; they are not implemented with cross-row `CHECK` constraints or triggers.

## Aggregate Overview

```text
InvoiceDraft 1 ─── 1 IdempotencyBinding
     │
     ├── 1..500 InvoiceLine
     ├── 1..N TaxTotal (derived groups)
     ├── 1..10 Payment (unique payment method)
     └── 0..15 AdditionalInformation (unique trimmed name)

InvoiceLine N ─── 1 TaxRuleCatalogEntry
Payment N ─── 1 PaymentMethodCatalogEntry
Buyer type ─── 1 effective IdentificationRuleCatalogEntry
```

The local catalog foreign keys point to immutable version rows. External Company-context values
are captured as snapshots because a later replay must return the original draft even when mutable
company data has changed.

## Entity: InvoiceDraft

**Purpose**: Persist the complete internal pre-issuance header, ownership, buyer snapshot, totals,
and audit context. It has no fiscal sequence, access key, authorization number, XML, signature, or
certificate reference.

| Field | Java type | PostgreSQL type | Required | Rules |
|-------|-----------|-----------------|----------|-------|
| `draftId` | `UUID` | `uuid` | Yes | Application-generated UUID v4; primary key; unrelated to fiscal identifiers |
| `tenantId` | `UUID` | `uuid` | No | Null only when Company is itself the tenant boundary |
| `companyId` | `UUID` | `uuid` | Yes | Verified authoritative Company identifier |
| `companyContextVersion` | `String` | `varchar(64)` | Yes | Version/ETag from the authoritative snapshot |
| `issuerId` | `UUID` | `uuid` | Yes | The Company's single verified Issuer |
| `issuerRuc` | `String` | `varchar(13)` | Yes | Verified creation-time RUC snapshot |
| `issuerLegalName` | `String` | `varchar(300)` | Yes | Trimmed registered legal name |
| `issuerTradeName` | `String` | `varchar(300)` | No | Trimmed registered trade name |
| `issuerHeadOfficeAddress` | `String` | `varchar(300)` | Yes | Registered fiscal address snapshot |
| `issuerAccountingRequired` | `boolean` | `boolean` | Yes | Registered fiscal attribute |
| `issuerSpecialTaxpayerCode` | `String` | `varchar(32)` | No | Registered fiscal attribute, when applicable |
| `issuerWithholdingAgentCode` | `String` | `varchar(32)` | No | Registered fiscal attribute, when applicable |
| `issuerRegimeCode` | `String` | `varchar(32)` | No | Versioned registered regime, when applicable |
| `emissionPointId` | `UUID` | `uuid` | Yes | Verified active emission point |
| `establishmentCode` | `String` | `char(3)` | Yes | Registered establishment code snapshot |
| `emissionPointCode` | `String` | `char(3)` | Yes | Registered emission-point code snapshot |
| `establishmentAddress` | `String` | `varchar(300)` | Yes | Registered address snapshot |
| `emissionDate` | `LocalDate` | `date` | Yes | Must equal Ecuador civil date derived from `createdAt` |
| `status` | `InvoiceDraftStatus` | `varchar(16)` | Yes | Exactly `DRAFT`; named check constraint |
| `currency` | `CurrencyCode` | `char(3)` | Yes | Exactly `USD`; named check constraint |
| `buyerIdentificationType` | `BuyerIdentificationType` | `char(2)` | Yes | `04`, `05`, `06`, `07`, or `08`; effective catalog row required |
| `buyerIdentification` | `String` | `varchar(20)` | Yes | Trimmed; validated by effective type-specific rule; sensitive |
| `buyerLegalName` | `String` | `varchar(300)` | Yes | Trimmed, nonblank; exact `CONSUMIDOR FINAL` for type `07` |
| `buyerAddress` | `String` | `varchar(300)` | No | Trimmed and nonblank when supplied; sensitive |
| `buyerEmail` | `String` | `varchar(254)` | No | One syntactically valid address; sensitive |
| `buyerTelephone` | `String` | `varchar(20)` | No | 7–15 digits and approved formatting; sensitive |
| `subtotalBeforeTaxes` | `BigDecimal` | `numeric` | Yes | Scale 2, non-negative, total digits <= 14 |
| `totalDiscount` | `BigDecimal` | `numeric` | Yes | Scale 2, non-negative, total digits <= 14 |
| `grandTotal` | `BigDecimal` | `numeric` | Yes | Scale 2, non-negative, total digits <= 14 |
| `createdAt` | `Instant` | `timestamptz(6)` | Yes | One application clock instant |
| `lastModifiedAt` | `Instant` | `timestamptz(6)` | Yes | Equals `createdAt` on creation; never earlier |
| `createdBySubject` | `String` | `varchar(255)` | Yes | Authenticated OIDC `sub`; sensitive audit value |
| `createdCorrelationId` | `UUID` | `uuid` | Yes | Caller-visible correlation for the creation attempt |

**Database constraints and indexes**:

- Primary key: `invoice_draft(draft_id)`.
- Check status is `DRAFT`, currency is `USD`, grand/subtotal/discount are non-negative scale-2
  values within 14 total digits, and `last_modified_at >= created_at`.
- Check buyer and snapshot text is nonblank and contains no control character where required.
- Index `(tenant_id, company_id, created_at)` and `(company_id, issuer_id, created_at)` for scoped
  operations; every repository query still includes effective authorization scope.
- No local foreign key is created for Company/Issuer/emission point; the application port
  validation and persisted snapshot are mandatory.

## Entity: InvoiceLine

**Purpose**: Persist one ordered commercial line, its selected immutable catalog rule, and every
system-calculated line amount.

| Field | Java type | PostgreSQL type | Required | Rules |
|-------|-----------|-----------------|----------|-------|
| `draftId` | `UUID` | `uuid` | Yes | FK to `invoice_draft`, cascade only with future approved draft deletion |
| `position` | `int` | `smallint` | Yes | 1 through 500; preserves business-significant order |
| `productCode` | `String` | `varchar(25)` | Yes | Trimmed SRI-valid alphanumeric text |
| `description` | `String` | `varchar(300)` | Yes | Trimmed, nonblank, no control characters |
| `quantity` | `BigDecimal` | `numeric` | Yes | Positive; <=18 total digits and <=6 fractional digits |
| `unitPrice` | `BigDecimal` | `numeric` | Yes | Non-negative; <=18 total digits and <=6 fractional digits |
| `discount` | `BigDecimal` | `numeric` | Yes | Scale 2; non-negative; <=14 total digits |
| `grossAmount` | `BigDecimal` | `numeric` | Yes | System-calculated scale 2; non-negative; <=14 total digits |
| `netAmount` | `BigDecimal` | `numeric` | Yes | System-calculated scale 2; non-negative; discount <= gross |
| `taxRuleId` | `UUID` | `uuid` | Yes | FK to immutable `tax_rule_catalog` row |
| `taxCatalogVersion` | `String` | `varchar(64)` | Yes | Snapshot for review/evidence |
| `taxTreatmentCode` | `TaxTreatment` | `varchar(32)` | Yes | `PERCENTAGE_RATE`, `ZERO_RATE`, `NOT_SUBJECT`, or `EXEMPT` |
| `officialTaxCode` | `String` | `varchar(8)` | Yes | Exact versioned catalog value |
| `officialPercentageCode` | `String` | `varchar(8)` | Yes | Exact versioned catalog value; distinct zero treatments remain distinct |
| `taxRate` | `BigDecimal` | `numeric` | Yes | Percentage points 0–100, <=2 fractional digits |
| `taxBase` | `BigDecimal` | `numeric` | Yes | Equals rounded net for this feature; scale 2 |
| `taxAmount` | `BigDecimal` | `numeric` | Yes | System-calculated scale 2; `0.00` for the three zero-tax treatments |

**Keys and constraints**:

- Primary key `(draft_id, position)`; line order cannot be changed by idempotency normalization.
- FK `draft_id` to draft and `tax_rule_id` to the immutable catalog version row.
- Named checks for position, signs, scale/total digits, `discount <= gross_amount`, and
  `net_amount = gross_amount - discount` at approved scale.
- Named treatment checks ensure zero treatments have tax `0.00`; aggregate fiscal calculations
  remain domain-authoritative and are verified by integration tests.

## Entity: TaxTotal

**Purpose**: Persist the system-calculated group for one distinct treatment code and applicable
rate.

| Field | Java type | PostgreSQL type | Required | Rules |
|-------|-----------|-----------------|----------|-------|
| `draftId` | `UUID` | `uuid` | Yes | FK to draft |
| `officialTaxCode` | `String` | `varchar(8)` | Yes | Versioned code |
| `officialPercentageCode` | `String` | `varchar(8)` | Yes | Keeps zero-rate/not-subject/exempt groups distinct |
| `taxTreatmentCode` | `TaxTreatment` | `varchar(32)` | Yes | Approved IVA treatment |
| `taxRate` | `BigDecimal` | `numeric` | Yes | Percentage points, <=2 fractional digits |
| `aggregateTaxBase` | `BigDecimal` | `numeric` | Yes | Sum of rounded line bases; scale 2, non-negative |
| `aggregateTaxAmount` | `BigDecimal` | `numeric` | Yes | Sum of rounded line taxes; scale 2, non-negative |
| `catalogVersion` | `String` | `varchar(64)` | Yes | Official/configuration evidence |

Primary key/unique group:
`(draft_id, official_tax_code, official_percentage_code, tax_treatment_code, tax_rate)`.

## Entity: Payment

**Purpose**: Persist one caller allocation to one active payment method.

| Field | Java type | PostgreSQL type | Required | Rules |
|-------|-----------|-----------------|----------|-------|
| `draftId` | `UUID` | `uuid` | Yes | FK to draft |
| `paymentMethodId` | `UUID` | `uuid` | Yes | FK to immutable `payment_method_catalog` row |
| `officialCode` | `String` | `varchar(8)` | Yes | Creation-time catalog code |
| `catalogVersion` | `String` | `varchar(64)` | Yes | Creation-time catalog version |
| `amount` | `BigDecimal` | `numeric` | Yes | Exactly scale 2, <=14 total digits; positive unless zero-total draft |

Primary key `(draft_id, payment_method_id)` enforces at most one entry per method. The exact payment
sum and the special single `0.00` payment rule are aggregate invariants validated before the
transaction and covered by PostgreSQL integration tests.

## Entity: AdditionalInformation

| Field | Java type | PostgreSQL type | Required | Rules |
|-------|-----------|-----------------|----------|-------|
| `draftId` | `UUID` | `uuid` | Yes | FK to draft |
| `name` | `String` | `varchar(300)` | Yes | Trimmed, nonblank, no control characters |
| `value` | `String` | `varchar(300)` | Yes | Trimmed, nonblank, no control characters; sensitive when caller uses it for fiscal/personal data |

Primary key `(draft_id, name)` enforces exact case-sensitive uniqueness after trimming. Entry order
is not persisted as business state and is ignored by idempotency comparison.

## Entity: IdempotencyBinding

**Purpose**: Atomically bind one approved scope/key to one draft and its exact normalized creation
content for the draft lifetime.

| Field | Java type | PostgreSQL type | Required | Rules |
|-------|-----------|-----------------|----------|-------|
| `tenantId` | `UUID` | `uuid` | No | Null only for Company-as-tenant scope |
| `companyId` | `UUID` | `uuid` | Yes | Effective authorized Company |
| `idempotencyKey` | `String` | `varchar(128)` | Yes | Trimmed printable ASCII, 1–128; sensitive and never logged |
| `draftId` | `UUID` | `uuid` | Yes | Pre-generated draft ID; unique FK to draft |
| `normalizationVersion` | `short` | `smallint` | Yes | Initial value `1`; permits future compatible comparison logic |
| `normalizedContent` | infrastructure JSON model | `jsonb` | Yes | Exact normalized business content; sensitive; no GIN index |
| `createdAt` | `Instant` | `timestamptz(6)` | Yes | Same creation instant as draft |

**Constraints**:

- `UNIQUE NULLS NOT DISTINCT (tenant_id, company_id, idempotency_key)` is the concurrency arbiter.
- `UNIQUE (draft_id)` ensures exactly one binding per draft.
- Binding-to-draft FK is `DEFERRABLE INITIALLY DEFERRED`, allowing the claim to precede aggregate
  insert while preventing a binding-only commit.
- Scope uniqueness is non-deferrable so `ON CONFLICT` can arbitrate immediately.
- Binding is not updated or time-expired. A future approved draft deletion must delete its binding
  in the same transaction.

### Normalized Content v1

```text
normalizationVersion
tenantId (when applicable)
companyId
issuerId
emissionPointId
emissionDate
buyer (trimmed canonical fields)
lines[] (original business order, canonical decimal strings and taxRuleId)
payments[] (sorted by paymentMethodId)
additionalInformation[] (sorted by canonical name)
```

Excluded: idempotency key, correlation ID, transport JSON property order, calculated output fields,
and mutable company snapshot attributes. PostgreSQL `jsonb` equality compares the normalized
structure exactly. Equivalent replay never updates the binding or original draft.

## Reference Entity: IdentificationRuleCatalog

| Field | Type | Rules |
|-------|------|-------|
| `code` | `char(2)` | `04` RUC, `05` identity card, `06` passport, `07` final consumer, `08` foreign identification |
| `effectiveFrom` / `effectiveTo` | `date` | Inclusive start, exclusive optional end; emission date must fall in period |
| `active` | `boolean` | Must be true at creation |
| `validationStrategy` | `varchar(32)` | Approved English strategy identifier, not a caller value |
| `minimumLength` / `maximumLength` | `smallint` | Versioned SRI bounds |
| `formatRule` | `varchar(255)` | Versioned representation rule; no invented checksum |
| `finalConsumerValue` | `varchar(20)` | `9999999999999` only for code `07` |
| `finalConsumerName` | `varchar(300)` | `CONSUMIDOR FINAL` only for code `07` |
| `finalConsumerMaximum` | `numeric` | Scale 2; USD `50.00` for SRI v2.32 |
| `sourceVersion` | `varchar(64)` | Official evidence version |

Primary key `(code, effective_from)`. Flyway catalog tests prove exactly one applicable active row
per supported code/date in the shipped baseline.

## Reference Entity: TaxRuleCatalog

| Field | Type | Rules |
|-------|------|-------|
| `taxRuleId` | `uuid` | Immutable primary key selected by caller |
| `family` | `varchar(16)` | Exactly `IVA` for this feature |
| `treatmentCode` | `varchar(32)` | Four approved target treatments |
| `officialTaxCode` / `officialPercentageCode` | `varchar(8)` | Versioned SRI values; never caller-supplied rates/codes |
| `rate` | `numeric` | Percentage points, 0–100, <=2 fractional digits |
| `effectiveFrom` / `effectiveTo` | `date` | Emission date applicability |
| `active` | `boolean` | Must be true |
| `sourceVersion` | `varchar(64)` | Official/configuration evidence |

Checks reject non-IVA family, negative/excess-precision rates, and non-zero rates for `ZERO_RATE`,
`NOT_SUBJECT`, or `EXEMPT`. ICE, IRBPNR, other families, deemed bases, and simultaneous line taxes
have no selectable rule for this feature.

## Reference Entity: PaymentMethodCatalog

| Field | Type | Rules |
|-------|------|-------|
| `paymentMethodId` | `uuid` | Immutable primary key selected by caller |
| `officialCode` | `varchar(8)` | Versioned SRI code |
| `name` | `varchar(100)` | Canonical English name; official code remains exact |
| `effectiveFrom` / `effectiveTo` | `date` | Emission date applicability |
| `active` | `boolean` | Must be true |
| `sourceVersion` | `varchar(64)` | Official evidence version |

## External Values: Company Authorization and Creation Context

These are application values returned by `CompanyContextPort`, not local authority tables.

```text
CompanyAuthorizationScope:
tenantId?; companyId; companyActive; callerAuthorized; authorizationVersion; resolvedAt

CreationContextEvaluation:
Eligible(CompanyContextSnapshot) | Ineligible

CompanyContextSnapshot when eligible:
contextVersion;
issuerId; issuerActive; RUC; legal/trade names; fiscal address and attributes;
emissionPointId; emissionPointActive; establishment/emission codes and address
```

The current authorization scope is required before any binding lookup. An eligible creation
snapshot is required only when no binding already exists; its necessary fiscal values are copied
into the new draft. An authorized equivalent replay returns the original snapshot even when the
current creation evaluation is ineligible. No remote call is made during the local database
transaction. A failure or unavailable authorization result leaves no idempotency binding.

## Validation Ownership

| Layer | Responsibilities |
|-------|------------------|
| API transport | Required fields/headers, unknown calculated/code/rate fields, decimal lexical patterns, UUID/date representation, collection maxima, text length and syntax |
| Application | OIDC subject/role handoff, Company scope, authoritative context and catalog resolution, current Ecuador date, idempotency normalization, dependency outcomes |
| Domain | Buyer invariants, line/tax/payment/additional invariants, exact arithmetic and rounding, final-consumer threshold, aggregate counts/sums, zero-total rules |
| Infrastructure | External response validity, database mapping, row constraints, scoped uniqueness, transaction rollback, timeout mapping |

## Create and Replay Lifecycle

1. Capture one `Instant`, derive current `America/Guayaquil` `LocalDate`, and canonicalize
   transport text/decimals into normalized business content.
2. Authenticate/authorize the role and resolve the authoritative current Company/tenant scope plus
   the separate current creation-context evaluation.
3. Check for a binding in that authorized scope. Equal normalized content returns the immutable
   original graph; different content conflicts. This fast path does not revalidate mutable
   Issuer/emission-point/catalog data.
4. For an unbound command, require eligible creation context, load effective local catalog
   versions, construct pure domain values, validate all rules, calculate lines/groups/totals, and
   reconcile payments.
5. Enter one reactive PostgreSQL transaction and try to claim the scoped key.
6. If the claim wins, insert draft and children; deferred FK validates binding at commit.
7. If the claim loses, load the committed binding in the same scope. Equal normalized content returns
   the original graph; different content returns conflict. Neither path changes timestamps/data.
8. Any cancellation, constraint violation, or persistence failure rolls back every local row.
9. Return `201` for the new commit or `200` for replay, with the original persisted graph.

## State Transitions

```text
No persisted row ──successful atomic create──> DRAFT
No persisted row ──validation/dependency/persistence failure──> No persisted row
DRAFT ──equivalent replay──> DRAFT (no mutation)
DRAFT ──same key/different content──> DRAFT (conflict, no mutation)
```

Update, deletion, cancellation, fiscal numbering, and authorization transitions are excluded.
