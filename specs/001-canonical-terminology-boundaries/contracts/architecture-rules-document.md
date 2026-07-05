# Contract: Architecture Rules Document

## Target Artifact

`docs/architecture/backend-clean-architecture.md`

## Purpose

Define mandatory Clean Architecture boundaries and naming rules for all future
backend specifications, plans, tasks, and code.

## Required Sections

1. `# Backend Clean Architecture`
2. `## Dependency Direction`
3. `## Layer Responsibilities`
4. `## Use Case-Centered Design`
5. `## Ports and Adapters`
6. `## DTO Separation`
7. `## Validation Separation`
8. `## SRI Contract Isolation`
9. `## Naming Rules`
10. `## Forbidden Practices`
11. `## Compliance Checklist`

## Required Content

- State dependency direction as `adapter -> application -> domain`,
  `bootstrap -> adapter`, and `bootstrap -> application`.
- State that domain must not depend on Quarkus, Hibernate, Panache, REST,
  JAX-RS, JSON serialization, PostgreSQL, Redis, filesystems, SOAP, XML, SRI
  clients, HTTP clients, external APIs, or messaging infrastructure.
- Define responsibilities for `domain`, `application`, `adapter.in.rest`,
  `adapter.out.persistence`, `adapter.out.sri`, `adapter.out.storage`,
  `adapter.out.queue`, `adapter.out.webhook`, and `bootstrap`.
- Require business operations to enter through explicit application use cases.
- Forbid generic business behavior names such as `DocumentService`,
  `SriService`, `ProcessService`, `Manager`, `Helper`, and `Util`.
- Define artifact-specific rendering:
  - package segments: lowercase
  - class names: PascalCase
  - fields and methods: camelCase
  - database objects: lowercase snake_case
  - URL path segments: kebab-case
- Include the required DTO mapping flow.
- Include a compliance checklist usable during spec, plan, task, and
  implementation review.

## Acceptance Checks

- A reviewer can identify the owning layer for a future business rule.
- A reviewer can reject business logic in REST resources, repositories, SRI
  adapters, or bootstrap configuration.
- A reviewer can derive valid package, class, field, method, database, and URL
  names from a canonical term.
- The document contains no target-domain Spanish legacy names except when
  discussing forbidden or compatibility examples.
