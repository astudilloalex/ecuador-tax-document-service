# Error Catalog: Create Invoice Draft

**Contract media type**: `application/problem+json`

Every error response uses safe English text and contains:

- stable machine-readable `code`;
- HTTP `status`;
- safe `title` and `detail`;
- request `instance`;
- `correlationId` equal to the returned `X-Correlation-Id` header;
- optional safe violations containing codes and field paths, never rejected values.

Correlation is initialized at the request boundary for every terminal outcome. An absent
`X-Correlation-Id` produces a generated safe UUID. Exactly one supplied value is trimmed and
preserved only when it contains 1 to 64 characters, begins with an ASCII letter or digit, and
contains only ASCII letters, digits, `.`, `_`, `:`, or `-`. A blank, repeated, over-length, or
otherwise unsafe value is never echoed; it is replaced with a safe UUID even when an earlier
payload-size or Company-context outcome governs the response.

No response exposes buyer data, raw idempotency keys, fingerprints, SQL, database names, internal
paths, stack traces, tokens, certificate data, Company lookup results, or external master data.

## Stable Outcomes

| Code | HTTP | When returned | Retry/action | Persistence guarantee |
|------|------|---------------|--------------|-----------------------|
| `COMPANY_CONTEXT_REQUIRED` | 400 | `X-Company-Id` missing, blank after trimming, or without a usable value | Supply exactly one value | No draft, children, or binding |
| `COMPANY_CONTEXT_INVALID` | 400 | Company header repeated/ambiguous, malformed UUID, or nil UUID | Supply one non-nil UUID | No draft, children, or binding |
| `IDEMPOTENCY_KEY_REQUIRED` | 400 | `Idempotency-Key` is missing after HTTP header parsing | Supply exactly one header-field value | No lookup, draft, children, or binding |
| `IDEMPOTENCY_KEY_INVALID` | 400 | Exactly one value is blank/whitespace-only after one ASCII SP/HTAB trim, longer than 128 normalized characters, or fails the approved non-comma ASCII grammar | Supply one valid normalized value | No lookup, draft, children, or binding |
| `IDEMPOTENCY_KEY_MULTIPLE` | 400 | Repeated fields, a parser result with multiple values, or any comma-containing/comma-combined ambiguous value is present | Supply exactly one unambiguous field value; no first value is selected | No lookup, draft, children, or binding |
| `INVALID_REQUEST` | 400 | Blank, repeated, over-length, or unsafe correlation input; malformed JSON; unsupported media representation; or unknown/prohibited non-calculated property such as request `companyId` or `issuerId` | Correct representation; invalid correlation input is replaced and never echoed | No draft, children, or binding |
| `PROHIBITED_CALCULATED_FIELD` | 422 | A recognized system-calculated field is present anywhere in the create body | Remove every calculated field | No draft, children, or binding |
| `IDEMPOTENCY_CONFLICT` | 409 | Same Company/key binding exists with a different fingerprint/version outcome | Use original content or a new key | Existing draft unchanged; no new draft |
| `REQUEST_PAYLOAD_TOO_LARGE` | 413 | Body is conclusively observed to exceed `2,097,152` bytes before deadline expiry | Reduce request size | Rejected before Company evaluation; valid correlation is preserved, absent/invalid correlation receives a safe generated replacement without emitting `400`; no later processing, database operation, or state |
| `BUSINESS_VALIDATION_FAILED` | 422 | Buyer, requestCreationInstant-derived emission date, lines, IVA selection, catalogs, text, collections, calculation, discount, zero-value, payment, or numeric-envelope rule fails; any quantity, unit-price, monetary, percentage-rate, exact-intermediate, rounded, grouped, payment-sum, or invoice-total overflow includes violation `MONETARY_RANGE_EXCEEDED` | Correct business content | No draft, children, or binding |
| `PERSISTENCE_UNAVAILABLE` | 503 | Local PostgreSQL is unavailable, or its configured operation timeout/failure becomes conclusive while shared request-deadline budget remains | Retry same Company/key/content; `Retry-After` MAY be returned | No partial state when pre-commit or complete rollback is confirmed |
| `REQUEST_TIMEOUT` | 504 | The one earliest-boundary monotonic 10-second request deadline expires before the current FR-041 stage or persistence outcome becomes conclusive | Retry same Company/key/content | No state when expiry is confirmed before persistence; no zero-state claim when commit is unresolved; replay resolves uncertain or completed commit state |
| `INTERNAL_ERROR` | 500 | Unexpected unclassified service failure | Follow operational guidance; same key prevents duplication | No known partial state; committed binding remains authoritative |

## Recognized Prohibited Calculated Fields

The API maintains a stable set derived from the response/calculation model, including at least:

- line gross amount;
- line net amount;
- line tax base;
- line tax amount;
- line total;
- grouped tax totals;
- subtotal before taxes;
- total discount;
- grand total;
- server-derived tax code or rate when supplied instead of `taxRuleId`.

Presence produces `PROHIBITED_CALCULATED_FIELD` even when the supplied value equals the service
calculation. Other unknown fields produce `INVALID_REQUEST`.

## Idempotency Header Classification

After HTTP parsing, exactly one `Idempotency-Key` field value is mandatory. The API trims only
leading/trailing ASCII SP (`U+0020`) and HTAB (`U+0009`) once, preserves internal characters and
case, and validates the normalized value against
`^[\x21-\x2B\x2D-\x7E](?:[\x20-\x2B\x2D-\x7E]{0,126}[\x21-\x2B\x2D-\x7E])?$`.
The normalized value is used unchanged for lookup, hashing, and persistence.

Classification is deterministic: absence is `IDEMPOTENCY_KEY_REQUIRED`; a single blank,
whitespace-only, over-length, control, non-ASCII, or other non-comma grammar failure is
`IDEMPOTENCY_KEY_INVALID`; repeated/parser-multiple or any comma-containing/comma-combined value is
`IDEMPOTENCY_KEY_MULTIPLE`. The comma classification prevents an HTTP implementation from silently
choosing the first combined value. The raw rejected value never appears in the response.

## Numeric Envelope Violation

`BUSINESS_VALIDATION_FAILED` MUST contain violation code `MONETARY_RANGE_EXCEEDED` when a
syntactically numeric value or calculation falls outside any of these inclusive envelopes:

- quantity: greater than `0` through `999999.999999`, with at most six fractional digits;
- unit price: `0` through `999999.999999`, with at most six fractional digits;
- every monetary input, exact intermediate, rounded result, grouped amount, payment sum, and
  invoice total: `0.00` through `999999999999999.99`, represented at scale two where stored or
  returned;
- percentage rate: `0.00` through `100.00`, represented at scale two.

The service checks these limits before persistence and never reports an envelope breach as a
persistence error. It does not round, clamp, truncate, or wrap an out-of-range value into range.

## Emission-Date Evaluation

The request boundary captures `requestCreationInstant` exactly once and derives the expected
date-only value using `America/Guayaquil`. That date remains fixed through validation and commit,
including a midnight crossing. Separately, `createdAt` is the UTC `java.time.Instant` captured
exactly once inside the persistence transaction after all business validations succeed and
immediately before the new draft is persisted. The same immutable value is persisted and returned
only after commit confirmation; rollback never exposes it and replay returns the original. It is
not a physical PostgreSQL commit timestamp, is not queried or reconstructed after commit, and does
not require `track_commit_timestamp`. It does not determine the accepted emission date.
`requestCreationInstant` is operational application input, not an API request-body property.

## Failure Precedence

FR-041 orders stage outcomes that become conclusive before deadline expiry:

1. `REQUEST_PAYLOAD_TOO_LARGE`;
2. `COMPANY_CONTEXT_REQUIRED` or `COMPANY_CONTEXT_INVALID`;
3. `INVALID_REQUEST` for `X-Correlation-Id` validation when Company context is valid;
4. `IDEMPOTENCY_KEY_REQUIRED`, `IDEMPOTENCY_KEY_INVALID`, or
   `IDEMPOTENCY_KEY_MULTIPLE` for idempotency-header presence, parsed cardinality, one-time ASCII
   SP/HTAB trim, or normalized grammar;
5. `INVALID_REQUEST` or `PROHIBITED_CALCULATED_FIELD` for representation/properties;
6. normalization failure as `INVALID_REQUEST` when representation caused it;
7. local Company-scoped binding lookup infrastructure outcome;
8. equivalent binding success (`200`, not an error);
9. `IDEMPOTENCY_CONFLICT`;
10. `BUSINESS_VALIDATION_FAILED` for buyer, line, tax-selection, payment, text, collection, date,
    and numeric-envelope validation, including `MONETARY_RANGE_EXCEEDED`;
11. calculation failure as `BUSINESS_VALIDATION_FAILED` when input-driven, including any exact,
    rounded, grouped, payment-sum, or invoice-total overflow;
12. persistence outcome (`PERSISTENCE_UNAVAILABLE` or `INTERNAL_ERROR`) or successful commit.

Correlation initialization always produces a safe response identifier. When payload size is
conclusively over limit first, `REQUEST_PAYLOAD_TOO_LARGE` remains terminal: the 413 handler
classifies correlation only to preserve one valid value or generate a safe UUID for absent/invalid
input, never echoes invalid input, and never emits the normal stage-3 `INVALID_REQUEST`. It does not
continue into deserialization, idempotency or reference-data lookup, validation, calculation, or
persistence. Company-context failure selected first likewise governs while using a safe correlation
value. Correlation does not affect idempotency equivalence.

### Cross-Cutting Deadline Arbitration

`REQUEST_TIMEOUT` is a cross-cutting terminal result, not a numbered validation or persistence
stage. One deadline begins before body consumption from a monotonic elapsed-time source, is never
restarted, and applies before and during every FR-041 stage. A stage outcome wins only when it is
conclusively determined before expiry. If expiry occurs first, `REQUEST_TIMEOUT` wins; a later
stage completion cannot replace it. Once any terminal outcome is conclusively selected before
expiry, a later deadline signal cannot replace that outcome.

Deterministic races are:

- body size known over limit before expiry → `413`; expiry before size is known → `504`;
- Company, correlation, or idempotency-header invalidity known before expiry → approved `400`;
  expiry before classification → `504`;
- replay or conflict known before expiry → `200` or `409`; expiry before lookup resolution → `504`;
- business validation or calculation result known before expiry → approved `422`; expiry first →
  `504`;
- confirmed rollback, unavailable, or unexpected persistence failure known before expiry → the
  approved persistence result; confirmed commit before expiry → the selected success result;
  expiry while commit remains unresolved → `504` with uncertain state.

A database completion arriving after expiry never changes the selected HTTP outcome. Confirmed
pre-persistence expiry or complete rollback leaves no state; unresolved commit status makes no
zero-state claim and is reconciled by retrying the same CompanyId, idempotency key, and equivalent
content. No timeout or response loss authorizes deletion of a committed or possibly committed
draft.

The non-blocking deadline timer remains active through response serialization and is cancelled on
response end. After the HTTP response is committed, expiry is telemetry-only: it does not change
status, write a second body or error envelope, start a database write, mutate domain state, or
compensate committed data; existing serialization may complete normally.

No authentication, authorization, tenant, Company lookup, Issuer lookup, or emission-point
ownership outcome participates.

## Explicitly Prohibited Outcomes

This feature does not define:

- `COMPANY_NOT_FOUND`;
- `COMPANY_INACTIVE`;
- `COMPANY_NOT_FISCALLY_ELIGIBLE`;
- `COMPANY_NOT_AUTHORIZED`;
- `COMPANY_CONTEXT_UNAVAILABLE`;
- `COMPANY_CONTEXT_TIMEOUT`;
- `COMPANY_ISSUER_CONFIGURATION_INVALID`;
- `EMISSION_POINT_COMPANY_MISMATCH`;
- `AUTHENTICATION_REQUIRED`;
- `PERMISSION_DENIED`;
- HTTP `401` or `403`.
