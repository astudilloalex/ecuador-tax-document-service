# Error Catalog: Create Invoice Draft

**Contract media type**: `application/problem+json`

Every error response uses safe English text and contains:

- stable machine-readable `code`;
- HTTP `status`;
- safe `title` and `detail`;
- request `instance`;
- `correlationId` equal to the returned `X-Correlation-Id` header;
- optional safe violations containing codes and field paths, never rejected values.

No response exposes buyer data, raw idempotency keys, fingerprints, SQL, database names, internal
paths, stack traces, tokens, certificate data, Company lookup results, or external master data.

## Stable Outcomes

| Code | HTTP | When returned | Retry/action | Persistence guarantee |
|------|------|---------------|--------------|-----------------------|
| `COMPANY_CONTEXT_REQUIRED` | 400 | `X-Company-Id` missing, blank after trimming, or without a usable value | Supply exactly one value | No draft, children, or binding |
| `COMPANY_CONTEXT_INVALID` | 400 | Company header repeated/ambiguous, malformed UUID, or nil UUID | Supply one non-nil UUID | No draft, children, or binding |
| `INVALID_REQUEST` | 400 | Invalid idempotency/correlation syntax, malformed JSON, unsupported media representation, or unknown/prohibited non-calculated property such as `companyId` or `issuerId` | Correct representation | No draft, children, or binding |
| `PROHIBITED_CALCULATED_FIELD` | 422 | A recognized system-calculated field is present anywhere in the create body | Remove every calculated field | No draft, children, or binding |
| `IDEMPOTENCY_CONFLICT` | 409 | Same Company/key binding exists with a different fingerprint/version outcome | Use original content or a new key | Existing draft unchanged; no new draft |
| `REQUEST_PAYLOAD_TOO_LARGE` | 413 | Body exceeds `2,097,152` bytes | Reduce request size | Rejected before Company evaluation; no state |
| `BUSINESS_VALIDATION_FAILED` | 422 | Buyer, date, lines, IVA selection, catalogs, text, collections, calculation, discount, zero-value, or payment rule fails | Correct business content | No draft, children, or binding |
| `PERSISTENCE_UNAVAILABLE` | 503 | Local PostgreSQL is unavailable or cannot start/continue a transaction before commit | Retry same Company/key/content; `Retry-After` MAY be returned | No partial state when pre-commit is confirmed |
| `REQUEST_TIMEOUT` | 504 | The 10-second request deadline is exhausted | Retry same Company/key/content | Replay resolves a commit that completed before response loss |
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

## Failure Precedence

When more than one failure is present, return the first applicable outcome:

1. `REQUEST_PAYLOAD_TOO_LARGE`;
2. `COMPANY_CONTEXT_REQUIRED` or `COMPANY_CONTEXT_INVALID`;
3. `INVALID_REQUEST` for idempotency-key syntax;
4. `INVALID_REQUEST` or `PROHIBITED_CALCULATED_FIELD` for representation/properties;
5. normalization failure as `INVALID_REQUEST` when representation caused it;
6. local binding lookup infrastructure outcome;
7. equivalent binding success (`200`, not an error);
8. `IDEMPOTENCY_CONFLICT`;
9. `BUSINESS_VALIDATION_FAILED`;
10. calculation failure as `BUSINESS_VALIDATION_FAILED` when input-driven;
11. persistence outcome (`PERSISTENCE_UNAVAILABLE`, `REQUEST_TIMEOUT`, or `INTERNAL_ERROR`).

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
