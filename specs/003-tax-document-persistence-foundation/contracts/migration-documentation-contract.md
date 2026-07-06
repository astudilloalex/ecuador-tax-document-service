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
- Primary key, foreign key, important index, relationship, and delete/update
  restriction summaries for each target table
- Temporal mappings for `issue_date` and `authorized_at`, including database
  type and timezone/precision rules
- Legacy-to-target naming notes that explain approved English target names and
  keep Spanish legacy names limited to migration documentation
- PFV references for deferred compatibility views, XML paths, audit
  persistence, production data migration, rollback/repair workflows, and
  lifecycle correction behavior
- PFV reference for deferred auto-numbering policy

## Classification Rules

| Artifact | Classification |
|----------|----------------|
| Target tables | Target database object |
| Target columns | Target database object |
| Legacy compatibility views | Legacy compatibility concept, deferred |
| XML path persistence | Pending Functional Validation |
| Audit table | Pending Functional Validation unless plan later includes it |
| Auto-numbering policy | Pending Functional Validation / future specification |
| Production data migration | Pending Functional Validation / future data migration specification |
| Migration rollback and repair workflows | Pending Functional Validation / future operations or migration specification |
| Archive, purge, delete, and production correction workflows | Pending Functional Validation / future lifecycle, retention, or operations specification |

## Deferred PFV References

Migration documentation must reference the following deferred PFVs when
describing excluded persistence or migration behavior:

| PFV | Deferred Topic |
|-----|----------------|
| PFV-PER-001 | Automatic sequence increment behavior beyond requested-value reservation. |
| PFV-PER-002 | Legacy compatibility views. |
| PFV-PER-003 | Historical XML path storage. |
| PFV-PER-004 | Audit persistence. |
| PFV-PER-005 | Auto-numbering policy. |
| PFV-PER-006 | Migration failure handling, rollback playbooks, and persisted data repair workflows. |
| PFV-PER-007 | Archive, purge, delete, production correction, and lifecycle correction workflows. |
| PFV-PER-008 | Production data migration. |

## Forbidden Documentation Outcomes

- Migration mappings stored only in feature-local files.
- Spanish legacy names represented as target schema names.
- Compatibility exceptions without reason, scope, owner, expiration condition,
  and safer alternative.

## Traceability

- Spec: `FR-015`, `FR-021`, `FR-022`, `NR-005`, `SC-009`, `SC-011`
- Constitution: Specification-Governed Migration
- Architecture: Durable documentation locations
