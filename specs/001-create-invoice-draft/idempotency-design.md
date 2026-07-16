# Idempotency Design: Create Invoice Draft

**Scope**: canonical CompanyId + API-normalized `Idempotency-Key`
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

Every binding lookup, binding create/read, winner re-read, or aggregate load in this flow includes
and enforces authoritative CompanyId. This does not apply Company scope to global VAT,
payment-method, identification-type, or other immutable SRI reference catalogs.

## Transport Header Contract

`Idempotency-Key` is a mandatory single-valued request header owned by the API adapter. Exactly one
field value must remain after HTTP header parsing. The adapter trims leading/trailing ASCII SP
(`U+0020`) and HTAB (`U+0009`) once, changes no internal character or case, and validates the
normalized 1–128-character value against
`^[\x21-\x2B\x2D-\x7E](?:[\x20-\x2B\x2D-\x7E]{0,126}[\x21-\x2B\x2D-\x7E])?$`.

- missing → `IDEMPOTENCY_KEY_REQUIRED`;
- one blank/whitespace-only, over-length, control, non-ASCII, or other non-comma invalid value →
  `IDEMPOTENCY_KEY_INVALID`;
- repeated fields, parser-produced multiple values, or any comma-containing/comma-combined
  ambiguous value → `IDEMPOTENCY_KEY_MULTIPLE`.

No first value is selected. Only the normalized value is passed below the API for lookup/hash use;
the domain has no HTTP, header-name, trimming, cardinality, or HTTP-error responsibility.

## Key Hash

Version 1 hashes the UTF-8 bytes of the API-normalized, validated key with SHA-256 and explicit domain
separation:

```text
SHA-256("invoice-draft-idempotency-key:v1\n" || normalizedKeyUtf8)
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
| General display text | NFC once; reject `Cc`/`Cf`/`Cs`/`Co`/`Cn`, `U+2028`/`U+2029`, tabs/CR/LF/NBSP/non-U+0020 separators; trim surrounding `U+0020`; preserve internal punctuation/U+0020/case; count code points |
| Quantity/unit price | Numerically canonical plain decimal; equivalent trailing-zero forms compare equally |
| Money/payment | Exact validated two-decimal representation |
| Tax rule selection | Canonical tax-rule UUID only; derived code/rate excluded |
| Optional contact value | Explicit absent marker distinct from a present value; JSON `null` is rejected unless the request schema explicitly permits it |
| Lines | Preserve original line order |
| Payments | Sort by canonical payment-method UUID, then amount |
| Additional information | Persist/use canonical name from NFC → surrounding `U+0020` trim → collapse U+0020 runs → Java `Locale.ROOT` lowercase; sort by that persisted value then normalized display value; never use database-locale recomputation |
| Empty optional collections | Normalize absent and empty to one approved empty representation |
| Object properties | Emit in a fixed field order independent of JSON input order |

The canonical byte representation is length-delimited so concatenated values cannot be
ambiguous. The exact normalization vectors become committed test fixtures before production code.

## Failure Precedence and Replay

The numbered sequence orders application outcomes. The API adapter alone races the application
`Uni` against the monotonic deadline, accepts exactly one terminal result, discards late outcomes,
and maps HTTP/Problem Details. Application/repositories return neutral values and never arbitrate or
map `504`.

1. Enforce request payload size.
2. Validate and canonicalize Company header.
3. Initialize and validate correlation: preserve one valid value, generate a UUID when absent, or
   generate a safe replacement and return `INVALID_REQUEST` without echoing invalid input.
4. In the API adapter, enforce idempotency-key presence/cardinality, normalize once, validate with
   the three stable error classifications, and hash only the accepted normalized key.
5. Validate request representation and prohibited properties.
6. Normalize business content and compute fingerprint.
7. Look up binding by CompanyId plus key hash.
8. If fingerprint/version match, load by CompanyId plus draftId and return the original result.
9. If they differ, return `IDEMPOTENCY_CONFLICT`.
10. Only an unbound command proceeds to Stage 10 calculation-independent validation, then Stage 11A
    calculation, then exact ordered Stage 11B calculated-value validation, then write.

Correlation initialization still provides a safe response identifier for a higher-precedence
payload-size or Company-context failure. On a selected 413 path, classification solely preserves
one valid value or generates a safe replacement for absent/invalid input; it never emits the normal
correlation-validation `400` and no binding/reference lookup or other database operation follows.
Correlation values never affect the key hash, request fingerprint, binding scope, replay result, or
conflict decision.

If expiry occurs before persistence begins, no binding exists. If expiry occurs while commit status
is unresolved, the client-facing outcome is uncertain and no zero-state claim is made. The client
retries the same CompanyId, key, and equivalent content: a committed binding returns the original
  draft; otherwise normal creation may proceed under the same unique boundary. The API discards a
late database/application result after terminal selection, and no correctly committed or possibly
committed draft is compensated or deleted because its response was not received.

Replay does not call Company Service, validate current Company/Issuer/emission-point state,
authenticate, authorize, recalculate, or mutate the original draft. It returns the originally
persisted immutable `createdAt`, never a replay-time or reconstructed timestamp.

## Concurrent Creation

The unique database boundary arbitrates concurrent requests. One transaction commits the draft
and binding. Every loser rolls back its tentative aggregate, re-reads the committed binding in the
same Company scope, and returns replay or conflict based on fingerprint/version.

No application cache, distributed lock, message broker, Company service, or time-based key expiry
participates.

## Retry Guidance

- Validation errors do not bind the key; correct the request and retry.
- `PERSISTENCE_UNAVAILABLE` and `REQUEST_TIMEOUT` are retried with the same Company, key, and
  content. For an unresolved commit, this replay is the required authoritative-state resolution,
  not evidence that the original request persisted zero rows.
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
- deadline-first unresolved-commit replay proves at most one draft without a zero-state assumption;
- response-commit deadline expiry changes no selected status or persisted state and triggers no
  compensation.
- missing, blank, whitespace-only, repeated, parser-multiple, comma-combined, over-length, and
  grammar-invalid headers produce their exact stable errors before any lookup;
- one accepted SP/HTAB-trimmed key uses identical normalized bytes for lookup, hashing, and
  persistence, without any domain HTTP dependency.
