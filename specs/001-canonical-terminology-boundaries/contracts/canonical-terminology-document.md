# Contract: Canonical Terminology Document

## Target Artifact

`docs/architecture/canonical-terminology.md`

## Purpose

Define the approved English canonical terms and artifact-specific format rules
used by target specifications, code, APIs, database objects, events, tests, and
documentation.

## Required Sections

1. `# Canonical Terminology`
2. `## Purpose`
3. `## Artifact-Specific Format Rules`
4. `## Approved Canonical Terms`
5. `## Forbidden Generic Names`
6. `## Pending Naming Decision Rule`
7. `## Traceability`

## Required Format Rules

- Package segments use lowercase.
- Class names and DTO class names use PascalCase.
- Fields and methods use camelCase.
- Database objects use lowercase snake_case.
- URL path segments use kebab-case.
- Event type names use PascalCase.
- Test class names use PascalCase with a `Test` suffix.
- Documentation file names use lowercase kebab-case.

## Required Baseline Terms

| Legacy Term | Canonical Term |
|-------------|----------------|
| comprobante | taxDocument |
| factura | invoice |
| nota credito | creditNote |
| nota debito | debitNote |
| retencion | withholding |
| guia remision | waybill |
| emisor | issuer |
| comprador | buyer |
| receptor | recipient |
| clave acceso | accessKey |
| numero autorizacion | authorizationNumber |
| fecha emision | issueDate |
| fecha autorizacion | authorizedAt |
| razon social | legalName |
| nombre comercial | tradeName |
| establecimiento | establishment |
| punto emision | issuingPoint |
| secuencial | sequenceNumber |

## Required Traceability

- Each approved term must trace to the constitution or a feature plan decision.
- Each pending term must trace to a Pending Naming Decision in
  `docs/migration/legacy-to-target-terminology.md`.
- Future tasks that introduce or use target names must cite the governing
  requirement identifier or this contract section.

## Acceptance Checks

- A reviewer can derive package, class, DTO class, field, method, database,
  URL, event, test, and documentation file forms for each approved term.
- A reviewer can identify generic names that are forbidden for business
  behavior.
- A reviewer can determine where unresolved terms must be recorded before
  affected task generation.
