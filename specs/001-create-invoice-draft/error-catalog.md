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
| `INVALID_REQUEST` | 400 | Invalid idempotency syntax; blank, repeated, over-length, or unsafe correlation input; malformed JSON; unsupported media representation; or unknown/prohibited non-calculated property such as `companyId` or `issuerId` | Correct representation; invalid correlation input is replaced and never echoed | No draft, children, or binding |
| `PROHIBITED_CALCULATED_FIELD` | 422 | A recognized system-calculated field is present anywhere in the create body | Remove every calculated field | No draft, children, or binding |
| `IDEMPOTENCY_CONFLICT` | 409 | Same Company/key binding exists with a different fingerprint/version outcome | Use original content or a new key | Existing draft unchanged; no new draft |
| `REQUEST_PAYLOAD_TOO_LARGE` | 413 | Body exceeds `2,097,152` bytes | Reduce request size | Rejected before Company evaluation; no state |
| `BUSINESS_VALIDATION_FAILED` | 422 | Buyer, requestCreationInstant-derived emission date, lines, IVA selection, catalogs, text, collections, calculation, discount, zero-value, payment, or numeric-envelope rule fails; any quantity, unit-price, monetary, percentage-rate, exact-intermediate, rounded, grouped, payment-sum, or invoice-total overflow includes violation `MONETARY_RANGE_EXCEEDED` | Correct business content | No draft, children, or binding |
| `PERSISTENCE_UNAVAILABLE` | 503 | Local PostgreSQL is unavailable or cannot start/continue a transaction before commit | Retry same Company/key/content; `Retry-After` MAY be returned | No partial state when pre-commit is confirmed |
| `REQUEST_TIMEOUT` | 504 | The one earliest-boundary monotonic 10-second request deadline is exhausted before an uncommitted terminal response wins | Retry same Company/key/content | Confirmed pre-commit failure leaves no state; replay resolves an uncertain or completed commit after response loss |
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
including a midnight crossing. `createdAt` is the confirmed commit instant and does not determine
the accepted emission date. An equivalent replay returns the original draft without current-date
revalidation. `requestCreationInstant` is operational application input, not an API request-body
property.

## Failure Precedence

When more than one failure is present, return the first applicable outcome:

1. `REQUEST_PAYLOAD_TOO_LARGE`;
2. `COMPANY_CONTEXT_REQUIRED` or `COMPANY_CONTEXT_INVALID`;
3. `INVALID_REQUEST` for `X-Correlation-Id` validation when Company context is valid;
4. `INVALID_REQUEST` for idempotency-key syntax;
5. `INVALID_REQUEST` or `PROHIBITED_CALCULATED_FIELD` for representation/properties;
6. normalization failure as `INVALID_REQUEST` when representation caused it;
7. local Company-scoped binding lookup infrastructure outcome;
8. equivalent binding success (`200`, not an error);
9. `IDEMPOTENCY_CONFLICT`;
10. `BUSINESS_VALIDATION_FAILED` for buyer, line, tax-selection, payment, text, collection, date,
    and numeric-envelope validation, including `MONETARY_RANGE_EXCEEDED`;
11. calculation failure as `BUSINESS_VALIDATION_FAILED` when input-driven, including any exact,
    rounded, grouped, payment-sum, or invoice-total overflow;
12. persistence outcome (`PERSISTENCE_UNAVAILABLE`, `REQUEST_TIMEOUT`, or `INTERNAL_ERROR`).

Correlation initialization always produces a safe response identifier. If payload size or Company
context fails before correlation validation, that earlier code governs, but invalid correlation
input is still never echoed and the response uses a safe replacement UUID. Correlation does not
affect idempotency equivalence.

The deadline begins before body consumption and is not restarted by any precedence stage. Its
non-blocking timer remains active through response serialization and is cancelled on response end.
Application and persistence work receive the same deadline's remaining budget. Deadline expiry does
not overwrite an earlier payload-size or Company outcome and never authorizes deletion when commit
status is uncertain or successful.

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
