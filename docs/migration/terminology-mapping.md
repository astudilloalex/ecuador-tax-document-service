# Legacy-To-Target Terminology Mapping

**Status**: Active

**Created**: 2026-07-12

**Authority**: `.specify/memory/constitution.md` v2.0.0

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
| `identificador de empresa` | Company Identifier | Target API | Mandatory single non-nil UUID supplied only through `X-Company-Id` for Company-scoped operations and normalized to an application-level ownership reference. |
| `emisor` | Issuer | Target Domain | Externally owned fiscal profile. Create Invoice Draft does not resolve or snapshot Issuer data; a later fiscal-issuance specification must define any immutable Issuer evidence. |
| `establecimiento` | Establishment | Target Domain | Externally owned Issuer subdivision. Create Invoice Draft neither resolves nor snapshots it. |
| `razón social` | Legal Name | Target Domain | Registered legal identity. It is outside Create Invoice Draft input and belongs to later authoritative fiscal-context resolution. |
| `RUC` | RUC | Target Domain | Legally defined Ecuadorian identifier that remains exact. |
| `punto de emisión` | Emission Point | Target Domain | Create Invoice Draft stores only an opaque external identifier selected for later processing and performs no Company, Issuer, establishment, status, or fiscal-relationship validation. |
| `datos fiscales utilizados` | Fiscal Context Snapshot | Target Domain | Reserved for a later separately approved fiscal-issuance specification. Create Invoice Draft MUST NOT resolve or persist this snapshot. |
| `comprador` | Buyer | Target Domain | Recipient of the goods or services represented by an invoice. |
| `tipo de identificación` | Buyer Identification Type | Target Domain | Uses official SRI codes `04`–`08` as canonical identifiers and the approved validation strategies in `SRI-OFFLINE-2.32-TARGET-1`; no target UUID is introduced. |
| `detalle de factura` | Invoice Line | Target Domain | One priced product or service entry in an invoice draft. |
| `impuesto` | Tax Category | Target Domain | Governing tax type or code; exact SRI catalog values remain official terms. |
| `tarifa de impuesto` | Tax Rate | Target Domain | Percentage or rate associated with an effective tax rule. |
| `regla tributaria` | Tax Rule | Target Domain | Effective combination of tax category, rate, validity, and calculation behavior; the initial IVA rules use immutable deterministic UUIDv5 mappings from the approved reference-data baseline. |
| `forma de pago` | Payment Method | Target Domain | Active catalog method selected for a draft payment; the initial Table 24 methods use immutable deterministic UUIDv5 mappings from the approved reference-data baseline. |
| `pago` | Payment | Target Domain | Amount allocated to one payment method for an invoice draft. |
| `información adicional` | Additional Information | Target Domain | Optional named information captured for later review. |
| `clave de idempotencia` | Idempotency Key | Target API | Caller-generated key scoped exactly by normalized Company UUID plus key for deduplicating invoice-draft creation commands. |
| `clave de acceso` | Access Key | SRI Adapter Only | Official 49-digit SRI identifier; allocation is excluded from invoice-draft creation. |
| `secuencial` | Official Sequential Number | SRI Adapter Only | Official invoice sequence allocated only by a later fiscal issuance capability. |

## Feature References

- `specs/001-create-invoice-draft/spec.md`
