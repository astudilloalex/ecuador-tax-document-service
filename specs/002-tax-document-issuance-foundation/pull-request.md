# Pull Request Summary: Tax Document Issuance Foundation

## Branches

- Source branch: `3-ft-2`
- Target integration branch: `main`

## PR Creation Status

Prepared only. The GitHub CLI is not installed in this environment, so no
authenticated GitHub account was available and no pull request was created from
this session. Do not claim `codex-bot` as the pull request author unless the
environment used to create the PR is authenticated as `codex-bot`.

## Summary

Implements the common tax document issuance foundation for the target Quarkus
backend while staying inside Clean Architecture domain and application
boundaries.

## Completed Task Groups

- Setup package boundary documentation.
- Domain exception and application error primitives.
- Common domain model for document type, access key, issuer context, sequence
  number, issue date, authorization number, authorization timestamp, issuance
  mode, and tax document aggregate identity.
- Common document state, authorization state, lifecycle transitions,
  authorized-document immutability, local voiding, and retry candidate policy.
- Application issuance request/result models, canonical audit event names, and
  outbound application ports.
- Durable terminology and migration documentation updates.
- Quickstart validation evidence.

## Generated Or Modified Files

- `src/main/java/com/alexastudillo/taxdocument/domain/`
- `src/main/java/com/alexastudillo/taxdocument/application/`
- `src/test/java/com/alexastudillo/taxdocument/domain/`
- `src/test/java/com/alexastudillo/taxdocument/application/`
- `docs/architecture/canonical-terminology.md`
- `docs/migration/legacy-to-target-terminology.md`
- `specs/002-tax-document-issuance-foundation/quickstart.md`
- `specs/002-tax-document-issuance-foundation/tasks.md`
- `.gitignore`

## Validation Results

- `./gradlew test`: passed.
- Source scope guard: passed. Feature source exists only under domain and
  application packages.
- Terminology check: passed. No forbidden Spanish legacy names appear in target
  domain/application source or tests.
- Forbidden import check: passed. Domain/application source contains no Quarkus,
  Hibernate, Panache, JAX-RS, PostgreSQL, Redis, SOAP, HTTP client, adapter,
  filesystem, or storage imports.
- Durable documentation check: passed.

## Deferred Pending Functional Validations

- PFV-ISS-001: runtime issuance mode default.
- PFV-ISS-002: legacy route compatibility.
- PFV-ISS-003: synchronization scheduling.
- PFV-ISS-004: retry policy and signed-XML precondition.
- PFV-ISS-005: post-authorization corrections.

## Scope Confirmation

This feature stayed within domain/application scope. It did not create REST
endpoints, REST DTOs, persistence adapters, JPA or Panache entities, SRI
XML/SOAP adapters, SRI clients, database migrations, queue adapters, webhook
adapters, or Quarkus bootstrap wiring.
