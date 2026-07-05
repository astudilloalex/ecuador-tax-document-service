# Contract: Source Boundary

## Target Consumers

Task generation and implementation review for
`002-tax-document-issuance-foundation`.

## Purpose

Constrain source files created by this feature to the allowed Clean Architecture
layers and prevent early adapter or bootstrap implementation.

## Allowed Source Areas

```text
src/main/java/com/alexastudillo/taxdocument/domain/
src/main/java/com/alexastudillo/taxdocument/application/
src/test/java/com/alexastudillo/taxdocument/domain/
src/test/java/com/alexastudillo/taxdocument/application/
```

## Forbidden Source Areas For This Feature

```text
src/main/java/com/alexastudillo/taxdocument/adapter/
src/main/java/com/alexastudillo/taxdocument/bootstrap/
src/test/java/com/alexastudillo/taxdocument/adapter/
```

## Forbidden Runtime Artifacts

This feature must not create:

- REST resources.
- REST DTOs.
- JPA entities.
- Panache entities.
- Database migrations.
- Persistence adapter implementations.
- SRI XML/SOAP DTOs.
- SRI clients.
- XML generation or signing implementation.
- Filesystem or object-storage adapters.
- Queue adapters.
- Webhook adapters.
- Quarkus bootstrap wiring.

## Dependency Rules

- Domain source depends only on domain source and Java standard types.
- Application source may depend on domain source and application ports/models.
- Application source must not depend on adapter implementations or framework
  types.
- Tests for domain/application behavior must not require Quarkus or real
  infrastructure.

## Acceptance Checks

- `domain` source contains no imports from Quarkus, Hibernate, Panache, JAX-RS,
  JSON serialization frameworks, PostgreSQL, Redis, XML/SOAP clients,
  filesystem APIs for business behavior, HTTP clients, queue SDKs, or storage
  SDKs.
- `application` source contains no adapter implementation imports and no
  framework-specific types in port signatures.
- No Spanish legacy names appear in target domain/application package, class,
  method, field, or test names.
- All future tasks cite this contract when creating source files.

## Traceability

- Spec `AR-001` through `AR-008`, `FR-013`, `FR-014`
- Constitution Principle I: Clean Architecture First
- Constitution Principle II: Mandatory Layer Responsibilities
- Constitution Principle IV: Ports and Adapters for External Dependencies
- Constitution Principle V: SRI Contract Isolation
- Constitution Principle VI: English Canonical Terminology
- Constitution Principle VII: DTO and Validation Separation
- Constitution Principle IX: Layered Testing Requirements
