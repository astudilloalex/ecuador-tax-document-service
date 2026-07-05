# Research: Canonical Terminology and Architecture Boundaries

## Decision: Treat this enabler as documentation-only

**Rationale**: The specification explicitly excludes Quarkus code, REST
endpoints, database migrations, persistence entities, SRI clients, runtime
business behavior, and legacy refactoring. The safest plan is to generate
governance documents and validation contracts only.

**Alternatives considered**:

- Create starter Java packages: rejected because implementation code is out of
  scope and would violate the specification.
- Create REST or database contracts: rejected because no runtime interfaces are
  part of this enabler.

## Decision: Split durable outputs by artifact category

**Rationale**: Architecture rules and migration mappings serve different review
workflows. Architecture rules belong under `docs/architecture`; legacy-to-target
terminology mappings belong under `docs/migration`.

**Alternatives considered**:

- Keep the feature spec as the only source of truth: rejected because future
  feature authors need durable repository documentation.
- Put all content into a single architecture document: rejected because mapping
  decisions are migration records, not only architecture rules.

## Decision: Seed mappings with approved glossary terms

**Rationale**: The initial mapping must be useful immediately without pretending
  the entire legacy vocabulary has been exhaustively analyzed. Approved baseline
  terms are included now; newly discovered terms are registered as Pending
  Naming Decisions or Pending Functional Validation.

**Alternatives considered**:

- Exhaustive legacy vocabulary analysis before any business planning: rejected
  because it would block migration progress and exceed this enabler's scope.
- Mapping rules only with no initial entries: rejected because future specs need
  a concrete baseline.

## Decision: Define artifact-specific term rendering

**Rationale**: Canonical terms need predictable representation across different
artifact types. The plan uses lowercase package segments, PascalCase class
names, camelCase fields and methods, lowercase snake_case database objects, and
kebab-case URL path segments.

**Alternatives considered**:

- One exact spelling everywhere: rejected because each artifact type has a
  different conventional naming form.
- Per-feature casing decisions: rejected because it increases inconsistency and
  review burden.

## Decision: Gate Pending Naming Decisions before task generation

**Rationale**: Planning may identify unresolved names, but task generation must
  not create implementation work for names that have not been resolved and
  recorded in the feature plan and `docs/migration`.

**Alternatives considered**:

- Resolve all names before planning: rejected because research during planning
  can legitimately discover additional terms.
- Resolve during implementation: rejected because it permits inconsistent code,
  APIs, and database names.

## Decision: Gate Pending Functional Validation before affected tasks

**Rationale**: Unverified legacy behavior must not be encoded into tasks as if
  it were validated target behavior. Affected work must be resolved, explicitly
  excluded, or deferred before task generation.

**Alternatives considered**:

- Resolve all behavior before planning: rejected because this enabler does not
  implement business workflows.
- Resolve during implementation or testing: rejected because it risks building
  incorrect behavior and rework.

## Decision: Use document contracts instead of runtime contracts

**Rationale**: This feature exposes governance artifacts to future spec authors,
  planners, and reviewers. Contract files define the required document shape and
  validation expectations for those artifacts.

**Alternatives considered**:

- OpenAPI contracts: rejected because no REST endpoints are created.
- Database schema contracts: rejected because no target schema is created.

## Decision: Defer build-tool reconciliation

**Rationale**: The constitution and target project require Maven, but the
current repository scaffold contains Gradle files. This enabler only defines
governance documentation and must not alter build files.

**Alternatives considered**:

- Convert the project scaffold to Maven in this feature: rejected because build
  migration is outside the specified scope and would require separate planning.
