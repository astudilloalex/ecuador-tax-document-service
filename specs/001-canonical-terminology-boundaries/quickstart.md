# Quickstart: Validate Canonical Terminology and Architecture Boundaries

This guide validates the documentation artifacts produced by this enabler after
implementation tasks are complete. It does not run the application or require
external infrastructure.

## Prerequisites

- Repository checkout at the project root.
- Feature artifacts available under `specs/001-canonical-terminology-boundaries/`.
- Implementation tasks have created the target documentation files.

## Expected Target Files

```text
docs/architecture/backend-clean-architecture.md
docs/architecture/canonical-terminology.md
docs/migration/legacy-to-target-terminology.md
```

## Validation Steps

### 1. Verify target documents exist

```bash
test -f docs/architecture/backend-clean-architecture.md
test -f docs/architecture/canonical-terminology.md
test -f docs/migration/legacy-to-target-terminology.md
```

Expected outcome: all commands exit successfully.

### 2. Verify architecture boundary sections

```bash
rg -n "Dependency Direction|Layer Responsibilities|Ports and Adapters|DTO Separation|SRI Contract Isolation|Forbidden Practices" docs/architecture
```

Expected outcome: all required architecture sections are present.

### 3. Verify baseline mapping terms

```bash
rg -n "comprobante|taxDocument|factura|invoice|clave acceso|accessKey|secuencial|sequenceNumber" docs/migration/legacy-to-target-terminology.md
```

Expected outcome: approved baseline legacy and target terms are present in the
mapping document.

### 4. Verify pending decision gates

```bash
rg -n "Pending Naming Decision|Pending Functional Validation|before task generation|excluded or deferred" docs/migration/legacy-to-target-terminology.md
```

Expected outcome: the mapping document states how unresolved naming and
functional behavior block, exclude, or defer affected task generation.

### 5. Verify target naming renderings

```bash
rg -n "PascalCase|camelCase|snake_case|kebab-case|lowercase" docs/architecture
```

Expected outcome: artifact-specific rendering rules are documented for packages,
classes, fields, methods, database objects, and URL path segments.

### 6. Verify no runtime artifacts were created for this enabler

```bash
find src/main/java src/test/java -type f 2>/dev/null | rg "taxdocument" || true
```

Expected outcome: no new Java source or test files are required by this feature.

## Review Checklist

- Future feature authors can locate architecture rules under `docs/architecture`.
- Future feature authors can locate legacy-to-target mappings under
  `docs/migration`.
- Approved baseline terms are mapped and classified.
- New legacy terms have a documented pending decision path.
- SRI contract terms are isolated to allowed artifacts.
- Pending Naming Decisions and Pending Functional Validations cannot proceed
  into affected tasks unresolved.
