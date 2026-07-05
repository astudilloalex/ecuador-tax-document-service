# Canonical Terminology

## Purpose

This document defines the approved English canonical terms and
artifact-specific format rules used by target specifications, code, APIs,
database objects, events, tests, and documentation for
`ecuador-tax-document-service`.

Target names must be business-oriented English names, not literal translations.
Legacy Spanish names are migration inputs, not target names. Official SRI
contract names remain exact only in SRI adapter, fixture, compatibility,
migration, or mapping artifacts.

## Artifact-Specific Format Rules

Canonical terms must be rendered by artifact type:

| Artifact Type | Required Format | Example From `taxDocument` |
|---------------|-----------------|----------------------------|
| Package segments | lowercase | `taxdocument` |
| Class names | PascalCase | `TaxDocument` |
| DTO class names | PascalCase | `TaxDocumentResponse` |
| Fields | camelCase | `taxDocument` |
| Methods | camelCase | `issueTaxDocument` |
| Database objects | lowercase snake_case | `tax_documents` |
| URL path segments | kebab-case | `tax-documents` |
| Event type names | PascalCase | `TaxDocumentAuthorized` |
| Test class names | PascalCase with a Test suffix | `TaxDocumentTest` |
| Documentation file names | lowercase kebab-case | `tax-document.md` |

Package segments use lowercase. Class names and DTO class names use PascalCase.
Fields and methods use camelCase. Database objects use lowercase snake_case. URL
path segments use kebab-case. Event type names use PascalCase. Test class names
use PascalCase with a Test suffix. Documentation file names use lowercase
kebab-case.

## Approved Canonical Terms

| Legacy Term | Canonical Term | Class Form | Field/Method Form | Database Form | URL Form | Event/Test Stem | Documentation File |
|-------------|----------------|------------|-------------------|---------------|----------|-----------------|--------------------|
| comprobante | taxDocument | `TaxDocument` | `taxDocument` | `tax_document` | `tax-document` | `TaxDocument` | `tax-document.md` |
| factura | invoice | `Invoice` | `invoice` | `invoice` | `invoice` | `Invoice` | `invoice.md` |
| nota credito | creditNote | `CreditNote` | `creditNote` | `credit_note` | `credit-note` | `CreditNote` | `credit-note.md` |
| nota debito | debitNote | `DebitNote` | `debitNote` | `debit_note` | `debit-note` | `DebitNote` | `debit-note.md` |
| retencion | withholding | `Withholding` | `withholding` | `withholding` | `withholding` | `Withholding` | `withholding.md` |
| guia remision | waybill | `Waybill` | `waybill` | `waybill` | `waybill` | `Waybill` | `waybill.md` |
| emisor | issuer | `Issuer` | `issuer` | `issuer` | `issuer` | `Issuer` | `issuer.md` |
| comprador | buyer | `Buyer` | `buyer` | `buyer` | `buyer` | `Buyer` | `buyer.md` |
| receptor | recipient | `Recipient` | `recipient` | `recipient` | `recipient` | `Recipient` | `recipient.md` |
| clave acceso | accessKey | `AccessKey` | `accessKey` | `access_key` | `access-key` | `AccessKey` | `access-key.md` |
| numero autorizacion | authorizationNumber | `AuthorizationNumber` | `authorizationNumber` | `authorization_number` | `authorization-number` | `AuthorizationNumber` | `authorization-number.md` |
| fecha emision | issueDate | `IssueDate` | `issueDate` | `issue_date` | `issue-date` | `IssueDate` | `issue-date.md` |
| fecha autorizacion | authorizedAt | `AuthorizedAt` | `authorizedAt` | `authorized_at` | `authorized-at` | `AuthorizedAt` | `authorized-at.md` |
| razon social | legalName | `LegalName` | `legalName` | `legal_name` | `legal-name` | `LegalName` | `legal-name.md` |
| nombre comercial | tradeName | `TradeName` | `tradeName` | `trade_name` | `trade-name` | `TradeName` | `trade-name.md` |
| establecimiento | establishment | `Establishment` | `establishment` | `establishment` | `establishment` | `Establishment` | `establishment.md` |
| punto emision | issuingPoint | `IssuingPoint` | `issuingPoint` | `issuing_point` | `issuing-point` | `IssuingPoint` | `issuing-point.md` |
| secuencial | sequenceNumber | `SequenceNumber` | `sequenceNumber` | `sequence_number` | `sequence-number` | `SequenceNumber` | `sequence-number.md` |

Plural resource names must use natural English forms. Examples:
`tax_documents`, `invoice_lines`, `tax_document_taxes`, `issuers`,
`establishments`, `issuing_points`, `issuance_sequences`,
`webhook_subscriptions`, and `webhook_delivery_attempts`.

## Forbidden Generic Names

The following generic names are forbidden for target business behavior:

- `DocumentService`
- `SriService`
- `ProcessService`
- `Manager`
- `Helper`
- `Util`

Business behavior must instead be expressed with explicit use case names such as
`IssueInvoiceUseCase`, `RetrySriAuthorizationUseCase`, or
`DeliverWebhookUseCase`.

Static utility classes must not contain business logic. Generic names may appear
only in non-business technical contexts when they are precise, bounded, and do
not hide a business operation.

## Pending Naming Decision Rule

New unclear terms must be registered as Pending Naming Decisions in
`docs/migration/legacy-to-target-terminology.md`.

Pending Naming Decisions may be recorded during specification or planning, but
affected task generation must not proceed until the final English term is
recorded in the feature plan and in
`docs/migration/legacy-to-target-terminology.md`. If the affected work is not
ready for a final name, the work must be explicitly excluded or deferred.

## Traceability

Each approved term in this document traces to the project constitution and to
feature `001-canonical-terminology-boundaries`.

Future specifications must cite the governing requirement identifier or this
document when introducing or using target names. Future task lists must use
`T###` identifiers and cite governing `FR-###`, `AR-###`, `NR-###`, `TR-###`,
`SC-###`, or contract sections.

Pending terms must trace to a Pending Naming Decision in
`docs/migration/legacy-to-target-terminology.md`.
