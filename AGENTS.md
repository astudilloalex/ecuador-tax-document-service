# Agent Instructions

## Project Architecture

This backend project must follow Clean Architecture.

Before creating or modifying backend code, always read and follow:

- `.specify/memory/constitution.md`

## Non-Negotiable Rules

- Do not put business logic in REST resources.
- Do not put business logic in persistence adapters.
- Do not expose persistence entities through REST APIs.
- Do not make the domain layer depend on Quarkus, Hibernate, Panache, REST, PostgreSQL, Redis, filesystems, SOAP, XML, or external APIs.
- Use application use cases as the entry point for business operations.
- Use ports for external dependencies.
- Use adapters to implement ports.
- Keep SRI-specific XML, SOAP, and Spanish contract names isolated in the outbound SRI adapter.
- Use English names for target code, APIs, DTOs, database objects, and documentation.
- Preserve SRI official names only inside SRI mappers, legacy compatibility adapters, or migration scripts.

## Spec Kit Workflow

For every feature:

1. Start with the specification.
2. Generate or update the plan.
3. Generate tasks.
4. Implement only what is covered by tasks.
5. Validate Clean Architecture boundaries before completion.