# Legacy-To-Target Terminology Mapping

**Status**: Active

**Created**: 2026-07-12

**Last verified**: 2026-07-19

**Authority**: `.specify/memory/constitution.md` v2.0.1

This file is the canonical mapping from historical or official source terminology to target
English terminology. A target name in this table is approved only for the scope described in its
notes. API and database names remain planning decisions unless their classification says
otherwise.

| Historical or official term | Canonical target term | Classification | Scope and notes |
|-----------------------------|-----------------------|----------------|-----------------|
| `factura` | Invoice | Target Domain | The commercial and fiscal invoice concept; this does not imply a legacy API or database contract. |
| `borrador de factura` | Invoice Draft | Target Domain | An internal, pre-issuance record with no official fiscal identifier or SRI status. |
| `comprobante` | Tax Document | Target Domain | Generic target concept for a document governed by Ecuadorian tax rules. |
| `empresa` | Company | Target Domain | Externally owned legal entity. This service accepts only its opaque UUID through `X-Company-Id`; that value scopes owned tax-document data but is not authentication, authorization, or locally owned Company master data. |
| `identificador de empresa` | Company Identifier | Target API | Mandatory single non-nil UUID supplied only through `X-Company-Id`; forbidden in request bodies/input schemas; permitted in a response only when explicitly contracted. It scopes Company-owned aggregate/record/binding operations, never immutable global SRI catalogs by default. |
| `emisor` | Issuer | Target Domain | Externally owned fiscal profile. Feature 001 does not resolve or snapshot Issuer data; Feature 002 records only the authoritative Issuer facts required in one immutable Fiscal Context Snapshot and does not own Issuer master data. |
| `establecimiento` | Establishment | Target Domain | Externally owned Issuer subdivision. Feature 001 neither resolves nor snapshots it; Feature 002 records only the authoritative Establishment facts required for one preparation and does not own Establishment master data. |
| `razón social` | Legal Name | Target Domain | Registered legal identity. It is outside Create Invoice Draft input and belongs to later authoritative fiscal-context resolution. |
| `RUC` | RUC | Target Domain | Legally defined Ecuadorian identifier that remains exact. |
| `punto de emisión` | Emission Point | Target Domain | Feature 001 stores only an opaque external identifier and performs no fiscal-relationship validation. Feature 002 resolves the authoritative facts needed for preparation without owning or administering Emission Point master data. |
| — (no governing legacy term) | Fiscal Preparation | Target Domain | Immutable pre-XML fiscal identity assigned exactly once to one Company-owned Invoice Draft. It owns the Fiscal Context Snapshot, Official Sequential Number, Numeric Code, Access Key, Fiscal Source Evidence, and creation instant. |
| `datos fiscales utilizados` | Fiscal Context Snapshot | Target Domain | Immutable value containing only the authoritative fiscal facts and Fiscal Source Evidence used for one Fiscal Preparation; it is not Company, Issuer, Establishment, or Emission Point master data. Feature 001 correctly excluded it from Invoice Draft creation. |
| `comprador` | Buyer | Target Domain | Recipient of the goods or services represented by an invoice. |
| `tipo de identificación` | Buyer Identification Type | Target Domain | Uses official SRI codes `04`–`08` as canonical identifiers and the approved validation strategies in `SRI-OFFLINE-2.32-TARGET-1`; no target UUID is introduced. |
| `detalle de factura` | Invoice Line | Target Domain | One priced product or service entry in an invoice draft. |
| `impuesto` | Tax Category | Target Domain | Governing tax type or code; exact SRI catalog values remain official terms. |
| `tarifa de impuesto` | Tax Rate | Target Domain | Percentage or rate associated with an effective tax rule. |
| `regla tributaria` | Tax Rule | Target Domain | Effective combination of tax category, rate, validity, and calculation behavior; the initial IVA rules use immutable deterministic UUIDv5 mappings from the approved reference-data baseline. |
| `forma de pago` | Payment Method | Target Domain | Existing active catalog method effective inclusively on invoice `emissionDate`; the initial Table 24 methods use immutable deterministic UUIDv5 mappings from the approved reference-data baseline. |
| `pago` | Payment | Target Domain | Amount allocated to one payment method for an invoice draft. |
| `información adicional` | Additional Information | Target Domain | Optional named information captured for later review. |
| `clave de idempotencia` | Idempotency Key | Target API | Caller-generated key scoped exactly by normalized Company UUID plus key for deduplicating invoice-draft creation commands. |
| — (no governing legacy term) | Official Sequence Baseline | Target Domain | Controlled mutable domain and persistence aggregate used only to allocate Official Sequential Numbers transactionally. Feature 002 may use an existing provisioned baseline but does not administer it. |
| `secuencial` | Official Sequential Number | Target Domain | Immutable value assigned to one Fiscal Preparation. Its external representation remains the official nine-digit SRI representation; Feature 001 correctly excluded allocation from Invoice Draft creation. |
| `código numérico` | Numeric Code | Target Domain | Immutable system-generated eight-digit Access Key component assigned once to a Fiscal Preparation and never supplied by the caller. |
| `clave de acceso` | Access Key | Target Domain | Immutable 49-digit fiscal identity generated and validated under SRI Offline Technical Sheet v2.33. Feature 001 correctly excluded it from Invoice Draft creation. |
| — (no governing legacy term) | Fiscal Source Evidence | Target Domain | Immutable value recording source authority, source revision, applicable effective interval, and observation instant for the authoritative facts used by one Fiscal Preparation. |
| — (no governing legacy term) | Standard Invoice XML Profile Evidence | Target Domain | Immutable Fiscal Preparation evidence selecting `STANDARD_DOMESTIC_INVOICE_1_1_0`, identifying its governed trigger set, and recording one conclusive or indeterminate assessment for every specialized-profile trigger. |
| — (no governing legacy term) | Specialized Profile Trigger Assessment | Target Domain | One `APPLIES`, `DOES_NOT_APPLY`, or `INDETERMINATE` decision for a named trigger governed by the committed standard-profile evidence. It is never inferred during XML generation. |
| — (no governing legacy term) | Unsigned SRI Invoice XML Artifact | Target Domain | The one immutable, schema-valid, unsigned UTF-8 Invoice XML byte sequence for a prepared Invoice Draft, together with stable source identity, integrity evidence, and creation instant. It is not signed, submitted, received, authorized, or issued. |
| — (no governing legacy term) | XML Integrity Evidence | Target Domain | The fixed `SHA-256` algorithm identifier, lowercase hexadecimal digest, and byte length of the exact validated and persisted unsigned XML bytes. |
| `factura_V1.1.0.xsd` | Official Invoice Schema | SRI Adapter Only | The exact SRI Invoice XSD `1.1.0` and its pinned authoritative offline dependency closure. It is validation authority, not a domain aggregate or current runtime catalog. |
| `factura`, `infoTributaria`, `infoFactura`, `detalles`, `detalle`, `impuestos`, `pagos`, `infoAdicional`, `campoAdicional` | Exact official SRI XML element names | SRI Adapter Only | These case-sensitive official tags remain unchanged only inside the SRI XML adapter and schema fixtures; they do not authorize Spanish production names elsewhere. |

## Feature References

- `specs/001-create-invoice-draft/spec.md`
- `specs/002-prepare-invoice-issuance/spec.md`
- `specs/003-generate-sri-invoice-xml/spec.md`

## Feature 001 Historical Classification Verification — 2026-07-18

The completed Create Invoice Draft implementation, OpenAPI contract, migrations, source packages,
tests, and final Spec Kit artifacts were checked against the terminology approved for Feature 001.
All target names remain in English or are approved exact official terms such as RUC and SRI
identifiers. At Feature 001 completion, the fiscal-identity concepts were recognized only within
the scopes and exclusions then recorded; the Feature 002 registration below does not rewrite that
historical result.

Feature 001 correctly introduced none of the fiscal-identity values registered for Feature 002. In
particular, Company remains an opaque externally owned context, global SRI catalogs remain unscoped
by Company, and Fiscal Context Snapshot, Official Sequence Baseline, Official Sequential Number,
Numeric Code, Access Key, and Fiscal Source Evidence remain excluded from Invoice Draft creation.
This preserves the approved Feature 001 scope and does not rewrite its history.

## Feature 002 Target-Domain Registration — 2026-07-18

Feature 002 introduces Fiscal Preparation, Fiscal Context Snapshot, Official Sequence Baseline,
Official Sequential Number, Numeric Code, Access Key, and Fiscal Source Evidence as the canonical
target-domain concepts defined above. Exact SRI acronyms, codes, digit widths, and official field
semantics remain unchanged where SRI v2.33 governs them. This registration authorizes only the
bounded pre-XML Fiscal Preparation outcome; it does not authorize master-data administration,
baseline administration, XML generation, signing, SRI communication, or another excluded side
effect.

## Feature 003 Target-Domain Registration — 2026-07-19

Feature 003 introduces Standard Invoice XML Profile Evidence, Specialized Profile Trigger
Assessment, Unsigned SRI Invoice XML Artifact, XML Integrity Evidence, and Official Invoice Schema
within the classifications above. Exact official SRI XML tags remain confined to the SRI adapter.
The artifact is the immutable unsigned input intended for a separately approved signing feature;
its existence never means that an Invoice is signed, submitted, received, authorized, fiscally
issued, or legally valid as an authorized electronic document.
