# Phase 1 Data Model: Generate Standard SRI Invoice XML

**Feature**: `003-generate-sri-invoice-xml`  
**Date**: 2026-07-19  
**Database baseline**: PostgreSQL 18.4 through Flyway only

## Model boundary

Feature 003 adds one immutable result and one missing piece of immutable fiscal evidence. It does
not create another Invoice aggregate, fiscal-context replica, schema registry, signing state, or
SRI lifecycle model.

| Model | Ownership | Persistence | Role |
|-------|-----------|-------------|------|
| Invoice Draft | Existing Feature 001 Company-owned aggregate | Existing `invoice_draft` graph | Exclusive commercial, monetary, line, tax, payment, text, currency, and emission-date source |
| Fiscal Preparation | Existing Feature 002 Company-owned aggregate | Existing `fiscal_preparation` | Exclusive committed fiscal identity and Fiscal Context Snapshot source |
| Standard Invoice XML Profile Evidence | New immutable value owned by Fiscal Preparation | New explicit nullable columns on `fiscal_preparation` | Proves or fails closed on ordinary-profile eligibility |
| Unsigned SRI Invoice XML Artifact | New Company-owned aggregate result | New `unsigned_sri_invoice_xml_artifact` | Exact validated unsigned bytes and integrity/source metadata |
| Official Invoice Schema Descriptor | Immutable infrastructure configuration | Versioned classpath resources, not a database row | Verifies the exact trusted validation graph |

Company UUIDs are external ownership references. There is no Company table, Company foreign key,
Company lookup, authentication model, or authorization model.

## Existing source: Invoice Draft

The complete persisted Feature 001 aggregate is loaded; no commercial projection may omit a value
that is represented in XML.

### Required source components

- immutable `companyId`, `invoiceDraftId`, and `emissionDate`;
- `USD` currency and persisted subtotal before taxes, total discount, grouped tax totals, grand
  total, and payment reference total;
- Buyer identification type/code, Legal Name, identification, and optional address;
- all Invoice Lines, their persisted positions, product codes, descriptions, quantity, unit price,
  discount, net amount, and one persisted IVA selection/result each;
- all grouped Tax Totals with persisted official tax code, official percentage code, rate, base,
  amount, treatment, and catalog version;
- all Payments with stable payment-method UUID, official method code, and amount;
- all Additional Information entries with persisted position, name, and value.

### Source order

| Collection | Deterministic order |
|------------|---------------------|
| Invoice Lines | ascending persisted `position` |
| Grouped Tax Totals | ascending Feature 001 `TaxTotal.groupKey()` tuple: treatment, official tax code, official percentage code, plain rate, catalog version |
| Payments | ascending official payment code, then canonical lowercase stable payment-method UUID text |
| Additional Information | ascending persisted `position`; mandatory Large Contributor entry follows all persisted entries |

Database row order is never an input. Feature 003 neither recalculates values nor consults current
catalogs. Flyway V7 adds prepared-source mutation guards over `invoice_draft`, `invoice_line`,
`invoice_line_tax`, `invoice_tax_total`, `invoice_payment`, and
`invoice_additional_information`: once the corresponding Fiscal Preparation exists, a direct
insert, update, delete, reparent, or root mutation is rejected. This protects the exact prepared
commercial source without adding a draft update capability or changing historical rows.

## Existing source: Fiscal Preparation

The following existing committed values are loaded without regeneration or normalization:

- `companyId`, `fiscalPreparationId`, and exact `invoiceDraftId` relationship;
- Fiscal Context Snapshot: environment, emission type, Legal Name, optional Commercial Name, RUC,
  Head Office Address, Establishment Address/codes, Emission Point code, accounting-required
  boolean, and conditional designation evidence;
- unchanged Invoice emission date;
- Official Sequential Number, Numeric Code, Access Key, and exact Access Key components;
- Fiscal Source Evidence, technical rule identifier/date, source revision/effective interval, and
  observation instant;
- Standard Invoice XML Profile Evidence when it was committed by the evolved Feature 002 flow.

Generation rechecks all committed relationship and Access Key component invariants. It does not
call the Fiscal Context provider and does not update a legacy preparation.

## StandardInvoiceXmlProfileEvidence

This immutable value belongs inside the Fiscal Context Snapshot/Fiscal Preparation domain. The
proposed provider consumer contract is
`specs/003-generate-sri-invoice-xml/contracts/authoritative-fiscal-context-v2.openapi.yaml`.

| Field | Domain type | PostgreSQL mapping | Rule |
|-------|-------------|--------------------|------|
| `profile` | fixed enum/value | `standard_invoice_xml_profile varchar(64)` | Exactly `STANDARD_DOMESTIC_INVOICE_1_1_0` |
| `technicalRuleId` | existing evidence value | existing `technical_rule_id varchar(64)` | Exactly `SRI-OFFLINE-2.33` for this trigger set |
| `technicalRuleModifiedOn` | existing `LocalDate` | existing `technical_rule_modified_on date` | Exactly `2026-07-13` |
| `governedTriggerSetId` | fixed versioned value | `invoice_profile_trigger_set_id varchar(64)` | Exactly `SRI-OFFLINE-2.33-INVOICE-1.1.0-TRIGGERS-V1` |
| each trigger assessment | `ProfileTriggerAssessment` | one nullable `varchar(20)` column per trigger | Exactly `APPLIES`, `DOES_NOT_APPLY`, or `INDETERMINATE` |

### Governed trigger fields

The value has one explicit field for every trigger; it is not a generic map or JSON object.

1. `export`
2. `reimbursement`
3. `subsidy`
4. `thirdPartyCharge`
5. `deliveryGuideReplacement`
6. `fuelSale`
7. `presumptiveWithholding`
8. `commercialTransport`
9. `constructionMaterial`
10. `fiscalMachine`
11. `negotiableInvoice`
12. `plasticBagDelivery`
13. `automaticIvaRefund`
14. `otherMandatoryExtension`

`fuelSale` covers the Annex 12/16 fuel-specific plate/product obligations;
`presumptiveWithholding` separately covers any mandatory presumptive-withholding structure,
including the petroleum and newspaper/magazine cases referenced by Annex 3; and
`plasticBagDelivery` covers Annex 18. Therefore, no known v2.33 exclusion is hidden inside the
catch-all assessment.

`SpecializedInvoiceProfileTrigger` is the exhaustive domain enum for those names.
`StandardInvoiceXmlProfileEvidence.assessment(trigger)` is an exhaustive switch over its explicit
fields, not map lookup.

The fourteen assessment columns are, in the same order:

```text
profile_export_assessment
profile_reimbursement_assessment
profile_subsidy_assessment
profile_third_party_charge_assessment
profile_delivery_guide_replacement_assessment
profile_fuel_sale_assessment
profile_presumptive_withholding_assessment
profile_commercial_transport_assessment
profile_construction_material_assessment
profile_fiscal_machine_assessment
profile_negotiable_invoice_assessment
profile_plastic_bag_delivery_assessment
profile_automatic_iva_refund_assessment
profile_other_mandatory_extension_assessment
```

Each is `varchar(20)` and uses the exact domain enum names.

### Persistence completeness

V7 adds all sixteen new profile columns (profile, trigger-set identifier, and fourteen explicit
assessments) as nullable because pre-V7 preparations cannot be
rewritten. A database check permits only:

- all sixteen columns null: historical generic evidence, classified as undetermined; or
- all sixteen columns non-null, exact profile/set constants, and every assessment in the three
  allowed values.

A new Feature 002 commit writes the complete set atomically. Null is never used as a trigger
assessment in a complete value; `INDETERMINATE` is explicit. Existing `technical_rule_id`,
`technical_rule_modified_on`, effective interval, and source revision remain part of the governing
evidence and must match the v2 response.

### Eligibility decision

| Persisted evidence | Result before XML work |
|--------------------|------------------------|
| Profile columns all null, partial, wrong profile/set/rule, missing trigger, ineffective evidence, or any `INDETERMINATE` | `INVOICE_XML_PROFILE_UNDETERMINED` |
| Any assessment is `APPLIES` | `INVOICE_XML_PROFILE_UNSUPPORTED` |
| RIMPE classification is `POPULAR_BUSINESS` | `INVOICE_XML_PROFILE_UNSUPPORTED`, irrespective of trigger values |
| Exact profile/set/rule and all fourteen assessments are `DOES_NOT_APPLY` | Eligible for mandatory source validation |

Special Taxpayer, Withholding Agent, RIMPE Contributor, and Large Contributor remain conditional
attributes of an otherwise ordinary Invoice. They are not specialized-trigger assessments.

## InvoiceXmlGenerationSource

This application value is a detached immutable snapshot used only during one first-generation
attempt. It contains:

- canonical Company, Invoice Draft, and Fiscal Preparation identities;
- the complete Invoice Draft domain data listed above;
- the complete Fiscal Preparation and profile evidence listed above;
- no current date, request date, correlation identifier, caller fiscal value, entity, JSON model,
  XML tags, schema object, or persistence session.

The source loader returns a sealed outcome rather than null:

- `ExistingArtifact(sourceRelationships, artifact)`;
- `EligibleSource(InvoiceXmlGenerationSource)`;
- `DraftNotFound`;
- `FiscalPreparationRequired`;
- `SourceInconsistent`.

Replay still loads/verifies the complete Company-scoped identities required by FR-014 through
FR-016, but it does not reconstruct this value into XML or re-evaluate business/profile fields.

## ValidatedUnsignedSriInvoiceXml

This transient immutable value exists only after the generator and validator both succeed.

| Field | Type | Rule |
|-------|------|------|
| `schemaVersion` | fixed value | Exactly `1.1.0` |
| `content` | defensive-copy `byte[]` | UTF-8, no BOM, nonempty, at most 2,097,152 bytes, exact compact document |
| `integrityEvidence` | `XmlIntegrityEvidence` | Computed over this exact array after successful XSD validation |

The generator yields candidate bytes; only the schema-validation application port can produce a
`ValidatedUnsignedSriInvoiceXml`. No partial or invalid value is persistable. The same bytes are
passed to validation, SHA-256, persistence, and success encoding. No string decode/re-encode or XML
reserialization occurs between those stages.

## XmlIntegrityEvidence

| Field | Type | Rule |
|-------|------|------|
| `algorithm` | fixed enum/value | Exactly `SHA-256` |
| `digestHex` | immutable ASCII string | Exactly 64 lowercase hexadecimal characters |
| `byteLength` | positive integer | Exactly the content byte count; `1..2,097,152` |

The domain exposes lowercase hexadecimal evidence. Infrastructure maps it reversibly to the
32-byte `content_sha256` database column; the API returns `digestHex`. The digest is not a public
correlation identifier and never appears in telemetry.

## UnsignedSriInvoiceXmlArtifact

The artifact is the single committed aggregate result.

| Field | Domain type | PostgreSQL type | Required | Invariant |
|-------|-------------|-----------------|----------|-----------|
| `id` | non-nil UUID | `uuid` | Yes | Opaque artifact identity generated only for the winner |
| `companyId` | existing `CompanyId` | `uuid` | Yes | Authoritative ownership scope; omitted from success response |
| `invoiceDraftId` | non-nil UUID | `uuid` | Yes | Exact source draft |
| `fiscalPreparationId` | non-nil UUID | `uuid` | Yes | Exact source preparation for the same Company/draft |
| `schemaVersion` | fixed value | `varchar(8)` | Yes | Exactly `1.1.0` |
| `xmlContent` | defensive-copy `byte[]` | `bytea` | Yes | Exact validated unsigned bytes, `1..2,097,152` |
| `integrityEvidence.algorithm` | fixed value | `varchar(16)` | Yes | Exactly `SHA-256` |
| `integrityEvidence.digestHex` | lowercase hex | `content_sha256 bytea` | Yes | Exactly 32 bytes and equals PostgreSQL `sha256(xml_content)` |
| `integrityEvidence.byteLength` | integer | `byte_length integer` | Yes | Equals `octet_length(xml_content)` |
| `createdAt` | `Instant` | `timestamptz` | Yes | One persistence instant obtained only by the confirmed winner |

The artifact has no status, signed content, signature, certificate, SRI response, authorization
number/date, retry count, filesystem path, delivery state, mutable version, `updatedAt`, or deletion
timestamp.

### Database keys and constraints

Flyway V7 creates `unsigned_sri_invoice_xml_artifact` with:

- primary key `id` and non-nil checks for every UUID;
- `UNIQUE (company_id, invoice_draft_id)` as natural exactly-one arbiter;
- `UNIQUE (fiscal_preparation_id)` so one preparation cannot back multiple artifacts;
- existing Company-scoped FK `(company_id, invoice_draft_id)` to `invoice_draft`;
- new unique key `(company_id, invoice_draft_id, id)` on `fiscal_preparation` and artifact composite
  FK `(company_id, invoice_draft_id, fiscal_preparation_id)` to that exact preparation;
- checks for fixed schema/algorithm, content nonemptiness/cap, 32-byte digest, database SHA-256
  equality, and database byte-length equality;
- `BEFORE UPDATE OR DELETE` trigger that always rejects changes.

There is no Company master-data FK and no provisional artifact row.

## OfficialInvoiceSchemaDescriptor

This infrastructure-only immutable descriptor is initialized from
`src/main/resources/sri/invoice/1.1.0/`.

| Property | Rule |
|----------|------|
| Schema | `factura_V1.1.0.xsd`, exact 36,356 bytes and pinned digest |
| Dependency closure | Exact W3C XSD and two DTD files with pinned lengths/digests |
| Resolver | Versioned local OASIS catalog in strict mode |
| External access | Empty allowed protocol set for DTD/schema; no network/filesystem fallback |
| Runtime state | `AVAILABLE(compiledSchema)` or `UNAVAILABLE(safeReasonCode)` |

This is trusted static validation configuration, not an application cache. `Schema` is shared;
each validation creates a new non-shared `Validator`. No raw parser error is retained in a caller
response, metric label, or health data field.

## Source-to-XML adapter invariants

Official XML names are confined to the SRI adapter. The full mapping is normative in `spec.md`; the
model adds these representation checks before writing:

- Special Taxpayer resolution must satisfy the exact Invoice XSD 3–13 alphanumeric envelope;
- Withholding Agent resolution must already be `0` or one to eight digits without a leading zero;
- `RIMPE_CONTRIBUTOR` maps only to `CONTRIBUYENTE RÉGIMEN RIMPE`; `NONE` omits the element;
- a Large Contributor requires both committed values, exact `nombre` text `Gran Contribuyente`,
  and its committed resolution as element content; at most 14 persisted Additional Information
  entries may precede it because the combined XSD maximum is 15;
- all required core text must satisfy exact XSD/code-point constraints without alteration;
- `USD` maps to `DOLAR`, accounting boolean maps to `SI`/`NO`, emission date formats as
  `dd/MM/yyyy`, and `propina` is fixed `0.00` only in the adapter;
- XML contains no namespace on `factura`, no signature namespace/element, no unsupported root
  section, and no empty placeholder optional element.

## Lifecycle and transitions

The persistent artifact has one state only: `COMMITTED`.

```text
ABSENT
  ├─ source/profile/generation/schema/deadline failure ──> ABSENT
  ├─ validated candidate loses concurrency race ─────────> COMMITTED WINNER (return existing)
  └─ validated candidate commits atomically ─────────────> COMMITTED WINNER

COMMITTED WINNER
  └─ equivalent request ─────────────────────────────────> SAME COMMITTED WINNER
```

`ValidatedUnsignedSriInvoiceXml` is an in-memory value, not a persistent intermediate state. There
is no `GENERATING`, `VALID`, `FAILED`, `SIGNED`, `SUBMITTED`, `RECEIVED`, `AUTHORIZED`, `ISSUED`, or
`DELETED` artifact state.

## Transaction and concurrency model

1. Validate transport/Company input and start the one monotonic deadline at earliest API entry.
2. Company-scoped read loads the complete draft, exact preparation, profile evidence, and possible
   artifact.
3. If a relationship-consistent artifact exists, return it; perform no profile/XML/schema/clock
   work and no write.
4. Validate relationships, profile evidence, and source representability.
5. Generate and schema-validate a bounded candidate on the CPU executor outside a database
   transaction; compute integrity and the candidate's canonical Base64 from the exact bytes. A
   first-generation representation-capacity failure therefore occurs before commit.
6. Return to the captured Vert.x context and open one short reactive transaction.
7. Lock the Company-scoped Invoice Draft first, then recheck exact preparation/artifact identities.
8. If the artifact now exists, discard the candidate, Base64-encode the persisted winner's bytes,
   and return that winner; do not assume candidate bytes can stand in for an already committed row.
9. Otherwise obtain one artifact UUID and one persistence `Instant`, insert/flush the artifact, and
   commit; the confirmed committer reuses its precomputed canonical Base64 for the same byte array.
10. A confirmed own commit returns `Created`; a found winner returns `Replay`. A uniqueness race
    rolls back and reads the winner through a fresh Company-scoped session.
11. If commit acknowledgement is uncertain, a fresh reconciliation lookup returns the winner when
    visible or `INVOICE_XML_OUTCOME_UNKNOWN`; it never regenerates or inserts again.

All successful paths return the database-loaded committed representation. Exactly one confirmed
committer returns HTTP 201; existing/reconciled/follower outcomes return HTTP 200.

## Validation ownership

| Boundary | Responsibilities |
|----------|------------------|
| API | Header cardinality/UUID, path UUID, no body/query/prohibited input, safe correlation, JSON/Base64 response mapping, HTTP status |
| Application | Company-scoped use-case ordering, replay precedence, source/profile classification, deadline, port orchestration, commit knowledge |
| Domain | Immutable values, exact profile completeness, integrity shape, artifact identity/source invariants, defensive copies |
| Infrastructure | Company predicates/FKs, prepared-source/artifact mutation guards, deterministic StAX tags/order, secure XSD graph, bounded executor, PostgreSQL outcome classification |

## Sensitive-data and retention model

`xml_content` is sensitive fiscal payload and inherits the Invoice record's approved PostgreSQL
encryption-at-rest, encrypted-backup, restore, retention, and disposal controls. Only the explicit
success response exposes it as Base64. No XML, source value, digest, or validation excerpt enters
logs, errors, metrics, traces, health output, test fixtures derived from production, or schema
fixtures. Platform Operations evidence is a release prerequisite. Feature 003 adds no delete API,
retention scheduler, custom encryption/key system, certificate field, or filesystem copy.
