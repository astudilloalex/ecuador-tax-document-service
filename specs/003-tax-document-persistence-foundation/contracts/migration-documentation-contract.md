# Contract: Migration Documentation Updates

## Purpose

Define durable documentation updates required when this persistence foundation
is implemented.

## Required Durable Location

Update `docs/migration/legacy-to-target-terminology.md` with all target
database objects introduced by this feature.

## Required Entries

The documentation must include:

- `issuers`
- `establishments`
- `issuing_points`
- `issuance_sequences`
- `tax_documents`
- All target columns introduced by the migration
- Uniqueness constraints that define `access_key`, issuance identity, and
  sequence reservation behavior
- PFV references for deferred compatibility views, XML paths, and audit
  persistence

## Classification Rules

| Artifact | Classification |
|----------|----------------|
| Target tables | Target database object |
| Target columns | Target database object |
| Legacy compatibility views | Legacy compatibility concept, deferred |
| XML path persistence | Pending Functional Validation |
| Audit table | Pending Functional Validation unless plan later includes it |

## Forbidden Documentation Outcomes

- Migration mappings stored only in feature-local files.
- Spanish legacy names represented as target schema names.
- Compatibility exceptions without reason, scope, owner, expiration condition,
  and safer alternative.

## Traceability

- Spec: `FR-015`, `NR-005`, `SC-009`
- Constitution: Specification-Governed Migration
- Architecture: Durable documentation locations
