# Contract: Legacy-To-Target Mapping Document

## Target Artifact

`docs/migration/legacy-to-target-terminology.md`

## Purpose

Provide the migration source of truth for approved canonical terms, legacy
Spanish term mappings, SRI adapter-only terms, pending naming decisions, pending
functional validations, and compatibility exceptions.

## Required Sections

1. `# Legacy-To-Target Terminology Mapping`
2. `## Classification Rules`
3. `## Approved Baseline Mappings`
4. `## SRI Adapter-Only Terms`
5. `## Pending Naming Decisions`
6. `## Pending Functional Validations`
7. `## Compatibility Exceptions`
8. `## Resolution Gates`

## Required Classification Values

- Target domain concept
- Target API field
- Target database object
- SRI adapter-only concept
- Legacy compatibility concept
- Migration-only concept
- Deprecated concept
- Pending Naming Decision
- Pending Functional Validation

## Required Baseline Mapping Rows

| Legacy Term | Target Term | Classification | Decision Status |
|-------------|-------------|----------------|-----------------|
| comprobante | taxDocument | Target domain concept | Decided |
| factura | invoice | Target domain concept | Decided |
| nota credito | creditNote | Target domain concept | Decided |
| nota debito | debitNote | Target domain concept | Decided |
| retencion | withholding | Target domain concept | Decided |
| guia remision | waybill | Target domain concept | Decided |
| emisor | issuer | Target domain concept | Decided |
| comprador | buyer | Target domain concept | Decided |
| receptor | recipient | Target domain concept | Decided |
| clave acceso | accessKey | Target domain concept | Decided |
| numero autorizacion | authorizationNumber | Target domain concept | Decided |
| fecha emision | issueDate | Target API field | Decided |
| fecha autorizacion | authorizedAt | Target API field | Decided |
| razon social | legalName | Target API field | Decided |
| nombre comercial | tradeName | Target API field | Decided |
| establecimiento | establishment | Target domain concept | Decided |
| punto emision | issuingPoint | Target domain concept | Decided |
| secuencial | sequenceNumber | Target domain concept | Decided |

## Pending Decision Rules

- New unmapped legacy terms must be registered as Pending Naming Decisions.
- New unverified legacy behavior must be registered as Pending Functional
  Validation.
- Pending Naming Decisions must resolve before affected task generation, with
  the final decision recorded in the feature plan and this mapping document.
- Pending Functional Validations must resolve before affected task generation,
  or the affected work must be excluded or deferred.

## Acceptance Checks

- Every row has exactly one classification.
- No pending row appears without a resolution gate.
- No Spanish legacy term is marked as allowed in target domain, application,
  API, or persistence artifacts unless it is an explicit compatibility
  exception.
- The mapping document can be extended by future feature plans without changing
  its structure.
