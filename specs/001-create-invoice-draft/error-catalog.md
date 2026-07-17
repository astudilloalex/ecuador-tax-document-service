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
`X-Correlation-Id` produces a generated safe UUID. Exactly one supplied value has surrounding
ASCII SP/HTAB trimmed once and is preserved only when it contains 1 to 64 ASCII characters, begins
with an ASCII letter or digit, and
contains only ASCII letters, digits, `.`, `_`, `:`, or `-`. A blank, repeated, over-length, or
otherwise unsafe value is never echoed; it is replaced with a safe UUID even when an earlier
payload-size or Company-context outcome governs the response.

No response exposes buyer data, raw idempotency keys, fingerprints, SQL, database names, internal
paths, stack traces, tokens, certificate data, Company lookup results, or external master data.

## Stable Outcomes

| Code | HTTP | When returned | Retry/action | Persistence guarantee |
|------|------|---------------|--------------|-----------------------|
| `COMPANY_CONTEXT_REQUIRED` | 400 | `X-Company-Id` missing or blank after one surrounding ASCII SP/HTAB trim | Supply exactly one value | No draft, children, or binding |
| `COMPANY_CONTEXT_INVALID` | 400 | Company header repeated/ambiguous, malformed UUID, or nil UUID | Supply one non-nil UUID | No draft, children, or binding |
| `IDEMPOTENCY_KEY_REQUIRED` | 400 | `Idempotency-Key` is missing after HTTP header parsing | Supply exactly one header-field value | No lookup, draft, children, or binding |
| `IDEMPOTENCY_KEY_INVALID` | 400 | Exactly one value is blank/whitespace-only after one ASCII SP/HTAB trim, longer than 128 normalized characters, or fails the approved non-comma ASCII grammar | Supply one valid normalized value | No lookup, draft, children, or binding |
| `IDEMPOTENCY_KEY_MULTIPLE` | 400 | Repeated fields, a parser result with multiple values, or any comma-containing/comma-combined ambiguous value is present | Supply exactly one unambiguous field value; no first value is selected | No lookup, draft, children, or binding |
| `INVALID_REQUEST` | 400 | Blank, repeated, over-length, or unsafe correlation input; malformed JSON; unsupported media representation; or unknown/prohibited non-calculated property such as request `companyId` or `issuerId` | Correct representation; invalid correlation input is replaced and never echoed | No draft, children, or binding |
| `PROHIBITED_CALCULATED_FIELD` | 422 | A recognized system-calculated field is present anywhere in the create body | Remove every calculated field | No draft, children, or binding |
| `IDEMPOTENCY_CONFLICT` | 409 | Same Company/key binding exists with a different fingerprint/version outcome | Use original content or a new key | Existing draft unchanged; no new draft |
| `REQUEST_PAYLOAD_TOO_LARGE` | 413 | Body is conclusively observed to exceed `2,097,152` bytes before deadline expiry | Reduce request size | Rejected before Company evaluation; valid correlation is preserved, absent/invalid correlation receives a safe generated replacement without emitting `400`; no later processing, database operation, or state |
| `BUSINESS_VALIDATION_FAILED` | 422 | Application-owned Stage 6 text/canonical validation, Stage 10 independent buyer/line/catalog/text/payment-reference rules (including payment activity/effectiveness on emissionDate), or ordered Stage 11B calculated-value rules fail; nested stable violations include `CANONICAL_NAME_TOO_LONG` and `MONETARY_RANGE_EXCEEDED` | Correct business content | No fingerprint lookup, draft, children, or binding when rejected in Stage 6; otherwise no draft, children, or binding |
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

## Canonical-Name Length Violation

`CANONICAL_NAME_TOO_LONG` is a stable nested violation under top-level
`BUSINESS_VALIDATION_FAILED` (`422`). It is selected by Application during FR-041 Stage 6 after one
per-value normalizer invocation performs one NFC pass and one surrounding `U+0020` trim, validates
the display value, then reuses that intermediate value for consecutive internal `U+0020` collapse
and Java `Locale.ROOT` lowercase without repeating NFC or trim. Application then counts the derived
value in Unicode code points and rejects more than 300. It never truncates the result.

The safe violation contains:

- `code`: `CANONICAL_NAME_TOO_LONG`;
- `field`: the original request field, such as `additionalInformation[0].name`;
- `maximum`: `300`;
- `countingUnit`: `UNICODE_CODE_POINTS`;
- `validationStage`: `CANONICALIZATION`.

It never includes the rejected display or canonical value. A display name may pass its own
1–300-code-point boundary and still fail this derived boundary: 150 occurrences of `U+0130`
lowercase to 300 code points (`i` plus `U+0307`) and are accepted; 151 lowercase to 302 and are
rejected. Rejection occurs before fingerprinting, idempotency lookup, Domain entry, or persistence,
and creates no state.

## Emission-Date Evaluation

The request boundary captures `requestCreationInstant` exactly once and derives the expected
date-only value using `America/Guayaquil`. That date remains fixed through validation and commit,
including a midnight crossing. Application passes a timestamp-free `InvoiceDraftCandidate` to the
persistence port. Separately, the persistence adapter captures one UTC `java.time.Instant` exactly
once inside the active transaction after all business validations succeed and immediately before
the new draft is persisted. The adapter assigns that same immutable value to `createdAt` and
`updatedAt`, persists both atomically, and returns them through `PersistedInvoiceDraft` only after
commit confirmation. Rollback exposes neither; replay returns both original values without another
clock invocation or mutation. Neither is a physical PostgreSQL commit timestamp, neither is queried
or reconstructed after commit, and neither requires `track_commit_timestamp` or determines the
accepted emission date.
`requestCreationInstant` is operational application input, not an API request-body property.
T076 is the sole persistence-clock invocation owner and calls it once at that point; T063 neither
invokes that clock nor supplies, replaces, or overwrites either timestamp.

## Failure Precedence

FR-041 orders stage outcomes that become conclusive before deadline expiry:

1. `REQUEST_PAYLOAD_TOO_LARGE`;
2. `COMPANY_CONTEXT_REQUIRED` or `COMPANY_CONTEXT_INVALID`;
3. `INVALID_REQUEST` for `X-Correlation-Id` validation when Company context is valid;
4. `IDEMPOTENCY_KEY_REQUIRED`, `IDEMPOTENCY_KEY_INVALID`, or
   `IDEMPOTENCY_KEY_MULTIPLE` for idempotency-header presence, parsed cardinality, one-time ASCII
   SP/HTAB trim, or normalized grammar;
5. API-owned `INVALID_REQUEST` or `PROHIBITED_CALCULATED_FIELD` for malformed
   representation/unknown or prohibited properties; API forwards decoded business text unchanged;
6. Application-owned business-text normalization/canonicalization, with exactly one normalizer
   invocation for each supplied applicable value and zero for an absent optional value. Business
   validation failures use `BUSINESS_VALIDATION_FAILED`; post-lowercase canonical overflow uses
   nested `CANONICAL_NAME_TOO_LONG` with its required safe metadata;
7. local Company-scoped binding lookup infrastructure outcome;
8. equivalent binding success (`200`, not an error);
9. `IDEMPOTENCY_CONFLICT`;
10. Stage 10 independent validation only: buyer/identification syntax; line/cardinality;
    product/description; quantity/unit-price sign/scale; tax existence/applicability; payment-method
    existence/activity/inclusive effectiveness on `emissionDate`; payment structure/basic amount;
    text/catalog/structural rules;
11. Stage 11A calculates monetary values, then Stage 11B returns
    `BUSINESS_VALIDATION_FAILED` in exact order: (a) calculated range/overflow by line/tax-group/
    aggregate order; (b) discount-over-gross by line; (c) final-consumer calculated-total limit;
    (d) total-dependent payment shape/positivity; (e) exact payment reconciliation;
12. persistence outcome (`PERSISTENCE_UNAVAILABLE` or `INTERNAL_ERROR`) or successful commit.

Correlation initialization always produces a safe response identifier. When payload size is
conclusively over limit first, `REQUEST_PAYLOAD_TOO_LARGE` remains terminal: the 413 handler
classifies correlation only to preserve one valid value or generate a safe UUID for absent/invalid
input, never echoes invalid input, and never emits the normal stage-3 `INVALID_REQUEST`. It does not
continue into deserialization, idempotency or reference-data lookup, validation, calculation, or
persistence. Company-context failure selected first likewise governs while using a safe correlation
value. Correlation does not affect idempotency equivalence.

### Cross-Cutting Deadline Arbitration

`REQUEST_TIMEOUT` is cross-cutting, not a numbered stage. The API adapter alone owns the monotonic
deadline race, accepts exactly one terminal result, prevents a second response, and selects/maps
HTTP status/envelope. Application and repositories return only neutral typed outcomes. The API
races the application `Uni`; if expiry is accepted first it maps `504`, otherwise it maps the
accepted application outcome. Late application/database completion is discarded and cannot replace
or add a response. Company `400` and all other HTTP mappings occur only after this arbitration.

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
