# Idempotency Design: Create Invoice Draft

**Scope**: canonical CompanyId + trimmed `Idempotency-Key`
**Initial normalization version**: `1`

## Binding Contract

The local binding contains at least:

- `company_id`;
- `idempotency_key_hash`;
- `request_fingerprint`;
- `normalization_version`;
- `invoice_draft_id`;
- `created_at`.

The mandatory uniqueness boundary is:

```text
UNIQUE (company_id, idempotency_key_hash)
```

The same raw key may be used independently by different Companies. Changing
`X-Company-Id` changes the binding scope even when the key and business content are unchanged.

## Key Hash

Version 1 hashes the UTF-8 bytes of the trimmed, validated key with SHA-256 and explicit domain
separation:

```text
SHA-256("invoice-draft-idempotency-key:v1\n" || trimmedKeyUtf8)
```

The persisted value is exactly 32 bytes. The raw key is not persisted, logged, traced, or exposed
in an error. The key-hash algorithm must remain stable for the lifetime of existing bindings; an
algorithm change requires an approved migration/backfill strategy that cannot bypass uniqueness.

## Request Fingerprint

Version 1 hashes canonical normalized client-controlled business content with independent domain
separation:

```text
SHA-256("invoice-draft-request:v1\n" || canonicalBusinessContentUtf8)
```

Included content:

- canonical emission-point UUID;
- emission date;
- normalized buyer fields;
- ordered invoice lines and selected tax-rule UUIDs;
- payments and amounts;
- additional-information names and values.

Excluded content:

- CompanyId, because it is the explicit binding scope;
- `Idempotency-Key` and its hash;
- `X-Correlation-Id`;
- every HTTP header and transport field;
- `requestCreationInstant`, because it is request-boundary evidence rather than client-controlled
  business content;
- JSON property ordering;
- server-calculated amounts, which are prohibited request inputs;
- timestamps, generated identifiers, catalog-derived codes/rates, and other server results.

Only the 32-byte fingerprint and normalization version are persisted. The complete normalized
request is not stored because it contains buyer personal data and a cryptographic fingerprint is
sufficient for approved equivalence.

## Version 1 Canonicalization

| Content | Canonical rule |
|---------|----------------|
| UUID | Lowercase hyphenated representation |
| Date | ISO `YYYY-MM-DD` |
| Text | Trimmed validated Unicode value; control characters rejected before fingerprinting |
| Quantity/unit price | Numerically canonical plain decimal; equivalent trailing-zero forms compare equally |
| Money/payment | Exact validated two-decimal representation |
| Tax rule selection | Canonical tax-rule UUID only; derived code/rate excluded |
| Optional contact value | Explicit absent marker distinct from a present value; JSON `null` is rejected unless the request schema explicitly permits it |
| Lines | Preserve original line order |
| Payments | Sort by canonical payment-method UUID, then amount |
| Additional information | Sort by canonical name, then value |
| Empty optional collections | Normalize absent and empty to one approved empty representation |
| Object properties | Emit in a fixed field order independent of JSON input order |

The canonical byte representation is length-delimited so concatenated values cannot be
ambiguous. The exact normalization vectors become committed test fixtures before production code.

## Failure Precedence and Replay

1. Enforce request payload size.
2. Validate and canonicalize Company header.
3. Initialize and validate correlation: preserve one valid value, generate a UUID when absent, or
   generate a safe replacement and return `INVALID_REQUEST` without echoing invalid input.
4. Validate and hash idempotency key.
5. Validate request representation and prohibited properties.
6. Normalize business content and compute fingerprint.
7. Look up binding by CompanyId plus key hash.
8. If fingerprint/version match, load by CompanyId plus draftId and return the original result.
9. If they differ, return `IDEMPOTENCY_CONFLICT`.
10. Only an unbound command proceeds to current catalog/domain validation, calculation, and write.

Correlation initialization still provides a safe response identifier for a higher-precedence
payload-size or Company-context failure. Correlation values never affect the key hash, request
fingerprint, binding scope, replay result, or conflict decision.

Replay does not call Company Service, validate current Company/Issuer/emission-point state,
authenticate, authorize, recalculate, or mutate the original draft.

## Concurrent Creation

The unique database boundary arbitrates concurrent requests. One transaction commits the draft
and binding. Every loser rolls back its tentative aggregate, re-reads the committed binding in the
same Company scope, and returns replay or conflict based on fingerprint/version.

No application cache, distributed lock, message broker, Company service, or time-based key expiry
participates.

## Retry Guidance

- Validation errors do not bind the key; correct the request and retry.
- `PERSISTENCE_UNAVAILABLE` and `REQUEST_TIMEOUT` are retried with the same Company, key, and
  content.
- Response loss after commit is recovered by the same equivalent replay.
- `IDEMPOTENCY_CONFLICT` is not automatically retried; use the original content or a new key for a
  distinct command.
- A successful binding remains for the draft lifetime and does not expire by elapsed time.

## Evidence

- published normalization vectors, including decimal/property/collection-order equivalence;
- CompanyId excluded from fingerprint but included in lookup/uniqueness;
- correlation and transport metadata excluded;
- no raw key or normalized request in database/logs/errors;
- equivalent replay returns identical persisted result;
- different content conflicts;
- same key/content across Companies creates independent bindings;
- 50 simultaneous equivalent requests create one draft;
- response-loss replay and pre-commit rollback behavior.
