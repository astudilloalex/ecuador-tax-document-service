# Legacy-To-Target Terminology Mapping

**Status**: Active

**Created**: 2026-07-12

**Authority**: `.specify/memory/constitution.md` v1.1.0

This file is the canonical mapping from historical or official source terminology to target
English terminology. A target name in this table is approved only for the scope described in its
notes. API and database names remain planning decisions unless their classification says
otherwise.

| Historical or official term | Canonical target term | Classification | Scope and notes |
|-----------------------------|-----------------------|----------------|-----------------|
| `factura` | Invoice | Target Domain | The commercial and fiscal invoice concept; this does not imply a legacy API or database contract. |
| `borrador de factura` | Invoice Draft | Target Domain | An internal, pre-issuance record with no official fiscal identifier or SRI status. |
| `comprobante` | Tax Document | Target Domain | Generic target concept for a document governed by Ecuadorian tax rules. |
| `empresa` | Company | Target Domain | Externally owned legal entity and sole document-ownership reference; current Company master data remains authoritative only in the Company bounded context. |
| `emisor` | Issuer | Target Domain | Externally owned fiscal profile belonging to exactly one Company; its values may be preserved only as immutable document evidence. |
| `establecimiento` | Establishment | Target Domain | Externally owned Issuer subdivision; the Tax Document Service preserves only the establishment values used in a document snapshot. |
| `razón social` | Legal Name | Target Domain | Registered legal identity; it is not client-overridable on an invoice draft. |
| `RUC` | RUC | Target Domain | Legally defined Ecuadorian identifier that remains exact. |
| `punto de emisión` | Emission Point | Target Domain | Externally owned active Issuer point selected for future fiscal issuance; document snapshots do not become current master data. |
| `datos fiscales utilizados` | Fiscal Context Snapshot | Target Domain | Immutable document-owned historical evidence of the Issuer, Establishment, and Emission Point values actually used by a tax document; never a Company master-data replica. |
| `comprador` | Buyer | Target Domain | Recipient of the goods or services represented by an invoice. |
| `detalle de factura` | Invoice Line | Target Domain | One priced product or service entry in an invoice draft. |
| `impuesto` | Tax Category | Target Domain | Governing tax type or code; exact SRI catalog values remain official terms. |
| `tarifa de impuesto` | Tax Rate | Target Domain | Percentage or rate associated with an effective tax rule. |
| `regla tributaria` | Tax Rule | Target Domain | Effective combination of tax category, rate, validity, and calculation behavior. |
| `forma de pago` | Payment Method | Target Domain | Active catalog method selected for a draft payment. |
| `pago` | Payment | Target Domain | Amount allocated to one payment method for an invoice draft. |
| `información adicional` | Additional Information | Target Domain | Optional named information captured for later review. |
| `clave de idempotencia` | Idempotency Key | Target API | Caller-generated key scoped to the effective Company and tenant where applicable for deduplicating invoice-draft creation commands. |
| `clave de acceso` | Access Key | SRI Adapter Only | Official 49-digit SRI identifier; allocation is excluded from invoice-draft creation. |
| `secuencial` | Official Sequential Number | SRI Adapter Only | Official invoice sequence allocated only by a later fiscal issuance capability. |

## Feature References

- `specs/001-create-invoice-draft/spec.md`
