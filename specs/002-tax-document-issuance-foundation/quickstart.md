# Quickstart: Tax Document Issuance Foundation

This quickstart validates the feature after planning and again after future
task execution. It does not require REST endpoints, databases, SRI services,
queues, webhooks, or migrations.

## Prerequisites

- Repository root: `ecuador-tax-document-service`
- Active feature directory:
  `specs/002-tax-document-issuance-foundation/`
- Gradle wrapper available at `./gradlew`

## Planning Artifact Checks

Run from the repository root:

```bash
test -f specs/002-tax-document-issuance-foundation/spec.md
test -f specs/002-tax-document-issuance-foundation/plan.md
test -f specs/002-tax-document-issuance-foundation/research.md
test -f specs/002-tax-document-issuance-foundation/data-model.md
test -f specs/002-tax-document-issuance-foundation/quickstart.md
test -f specs/002-tax-document-issuance-foundation/contracts/application-port-contracts.md
test -f specs/002-tax-document-issuance-foundation/contracts/future-use-case-contract.md
test -f specs/002-tax-document-issuance-foundation/contracts/migration-mapping-update-contract.md
test -f specs/002-tax-document-issuance-foundation/contracts/source-boundary-contract.md
```

Expected outcome: every command exits successfully.

## Scope Guard Checks

Run from the repository root after implementation tasks are completed:

```bash
find src/main/java/com/alexastudillo/taxdocument -type f | sort
find src/test/java/com/alexastudillo/taxdocument -type f | sort
```

Expected outcome:

- Feature source appears only under `domain/` and `application/`.
- No feature source appears under `adapter/` or `bootstrap/`.
- No REST resources, REST DTOs, JPA entities, Panache entities, migrations, SRI
  DTOs, SRI clients, queue adapters, webhook adapters, or bootstrap wiring are
  introduced by this feature.

## Terminology Checks

Run from the repository root after implementation tasks are completed:

```bash
rg -n "comprobante|factura|retencion|guia|emisor|secuencial|claveAcceso|numeroAutorizacion" \
  src/main/java/com/alexastudillo/taxdocument/domain \
  src/main/java/com/alexastudillo/taxdocument/application \
  src/test/java/com/alexastudillo/taxdocument/domain \
  src/test/java/com/alexastudillo/taxdocument/application
```

Expected outcome: no matches in target domain/application source or tests.
Spanish terms may appear only in migration documentation or AS-IS evidence.

## Test Execution

Run after implementation tasks create the domain/application foundation:

```bash
./gradlew test
```

Expected outcome:

- Domain tests run without Quarkus.
- Application tests run without PostgreSQL, Redis, filesystem access, SRI
  services, queues, webhooks, or external HTTP services.
- Tests cover access key validation, document type support, state transitions,
  authorized immutability, local voiding rejection, retry eligibility candidate
  behavior, idempotency keys, audit event names, and port boundary signatures.

## Durable Documentation Checks

Run after documentation update tasks are completed:

```bash
rg -n "PENDIENTE|EN_PROCESO|AUTORIZADO|ANULADO|VOIDED|IN_PROGRESS" docs/migration/legacy-to-target-terminology.md
rg -n "documentState|authorizationState|issuanceRequest|issuanceResult|issuanceMode" docs/architecture/canonical-terminology.md docs/migration/legacy-to-target-terminology.md
```

Expected outcome:

- Legacy state mappings are recorded in `docs/migration/legacy-to-target-terminology.md`.
- Foundation canonical terms are recorded or referenced in durable
  architecture/migration documentation.
- Pending Functional Validations from this feature are visible in the durable
  migration mapping document or explicitly deferred by the plan/tasks.

## Validation Steps

Before handoff, complete the validation checks in this quickstart in order:

1. Planning artifact checks.
2. Scope guard checks.
3. Terminology checks.
4. Test execution.
5. Durable documentation checks.

Validation evidence should identify commands run, results, and any deferred
Pending Functional Validation items. Validation must confirm that this feature
stayed within domain/application scope and did not introduce REST, persistence,
SRI, queue, webhook, database migration, or bootstrap artifacts.

## Post-Implementation Handoff

After validation, prepare a pull request summary for reviewer handoff. The
summary should include completed task groups, generated or modified files,
validation results, deferred Pending Functional Validation items, and explicit
confirmation that the feature stayed within domain/application scope.

If a GitHub pull request is created, use the currently authenticated GitHub
account. Do not claim or require `codex-bot` as the pull request author unless
the GitHub CLI or remote environment is authenticated as `codex-bot`. Assign or
request review from `codex-bot` only when that GitHub user exists and is
available in the repository.

## Review Gate

Before running `$speckit-tasks`, confirm:

- `research.md` has no unresolved clarification markers.
- `plan.md` Constitution Check is PASS before and after design.
- Contracts define application ports, future use case rules, migration updates,
  and source boundaries.
- No task should be generated for REST, persistence adapters, SRI adapters,
  database migrations, queues, webhooks, or bootstrap wiring.
