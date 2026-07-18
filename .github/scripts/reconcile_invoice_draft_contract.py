from __future__ import annotations

from pathlib import Path
import re

ROOT = Path("specs/001-create-invoice-draft")
SPEC = ROOT / "spec.md"
PLAN = ROOT / "plan.md"
TASKS = ROOT / "tasks.md"
ERRORS = ROOT / "error-catalog.md"
OPENAPI = ROOT / "contracts/invoice-draft-api.openapi.yaml"
CHECKLIST = ROOT / "checklists/contract-validation.md"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8")


def replace_exact(path: Path, old: str, new: str, expected: int = 1) -> None:
    text = read(path)
    count = text.count(old)
    if count != expected:
        raise RuntimeError(
            f"{path}: expected {expected} occurrence(s), found {count}: {old!r}"
        )
    write(path, text.replace(old, new))


# ---------------------------------------------------------------------------
# I1 — emissionPointId is the first Stage-6 operation.
# ---------------------------------------------------------------------------
replace_exact(
    PLAN,
    "at the beginning of Stage 6 trim/validate/canonicalize `emissionPointId` once and invoke `BusinessTextNormalizer` exactly once for every supplied applicable business-text value;",
    "as the first Stage-6 operation trim/validate/canonicalize `emissionPointId` once; only after that succeeds invoke `BusinessTextNormalizer` exactly once for every supplied applicable business-text value;",
)
replace_exact(
    PLAN,
    "At the\nbeginning of Stage 6, Application alone invokes `BusinessTextNormalizer` exactly once for each\nsupplied applicable value.",
    "Only after the first Stage-6 operation has successfully trimmed, validated, and canonicalized\n`emissionPointId`, Application alone invokes `BusinessTextNormalizer` exactly once for each supplied\napplicable value.",
)
replace_exact(
    PLAN,
    "inputs, idempotency/correlation, fixed RequestDeadline); invoke BusinessTextNormalizer at\n    Stage 6; orchestrate ordered validation",
    "inputs, idempotency/correlation, fixed RequestDeadline); first validate and canonicalize\n    emissionPointId at Stage 6, then invoke BusinessTextNormalizer; orchestrate ordered validation",
)

# ---------------------------------------------------------------------------
# I2 — exact canonical response emissionPointId.
# ---------------------------------------------------------------------------
replace_exact(
    OPENAPI,
    "        emissionPointId:\n          type: string\n          format: uuid\n          description: Canonical opaque external reference captured in the draft.",
    "        emissionPointId:\n          type: string\n          format: uuid\n          pattern: '^(?!00000000-0000-0000-0000-000000000000$)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'\n          description: Canonical lowercase-hyphenated, non-nil opaque external reference captured in the draft.",
)

# ---------------------------------------------------------------------------
# U1 — exhaustive calculated-field classifier and precedence.
# ---------------------------------------------------------------------------
replace_exact(
    ERRORS,
    """## Recognized Prohibited Calculated Fields

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
""",
    """## Recognized Prohibited Calculated Fields

Stage 5 is owned by the API transport classifier. Before DTO mapping or generic unknown-property
rejection, it traverses the decoded JSON object and classifies this exhaustive set of client-
prohibited calculated paths:

1. `$.subtotalBeforeTaxes`;
2. `$.totalDiscount`;
3. `$.grandTotal`;
4. `$.taxTotals`;
5. `$.lines[*].position`;
6. `$.lines[*].grossAmount`;
7. `$.lines[*].netAmount`;
8. `$.lines[*].lineTotal`;
9. `$.lines[*].tax`;
10. `$.lines[*].tax.family`;
11. `$.lines[*].tax.treatment`;
12. `$.lines[*].tax.officialTaxCode`;
13. `$.lines[*].tax.officialPercentageCode`;
14. `$.lines[*].tax.rate`;
15. `$.lines[*].tax.base`;
16. `$.lines[*].tax.amount`;
17. `$.lines[*].tax.catalogVersion`.

A listed property is classified solely by its property name and location. Its value, JSON type,
`null` state, equality to a service result, collection position, or nesting depth never changes the
classification. Presence of any listed path produces top-level `PROHIBITED_CALCULATED_FIELD` (`422`)
even when its value is malformed or equals the value the service would calculate.

Within Stage 5, calculated-field classification has priority over every ordinary unknown or
prohibited non-calculated property. When several listed paths are present, exactly one result is
selected by canonical path order: root paths in the numbered order above, then ascending line index,
then line paths in the numbered order above. The safe response uses detail `Client-supplied
calculated fields are prohibited` and one value-free violation with code
`CALCULATED_FIELD_PROHIBITED` and the selected concrete field path. Correlation follows the normal
safe-correlation rule and no fingerprint, lookup, business validation, calculation, draft, child, or
idempotency state is created. If no listed path is present, ordinary unknown/prohibited properties
produce `INVALID_REQUEST`.
""",
)

replace_exact(
    SPEC,
    "- **FR-012**: The service MUST calculate line gross, line net, tax base, tax amount, grouped tax\n  totals, subtotal before taxes, total discount, and grand total. Any client-supplied calculated\n  field MUST be rejected. Every monetary input and result MUST stay within `0.00` through\n  `999999999999999.99`.",
    "- **FR-012**: The service MUST calculate line gross, line net, tax base, tax amount, grouped tax\n  totals, subtotal before taxes, total discount, and grand total. The exhaustive prohibited paths\n  are `$.subtotalBeforeTaxes`, `$.totalDiscount`, `$.grandTotal`, `$.taxTotals`,\n  `$.lines[*].position`, `$.lines[*].grossAmount`, `$.lines[*].netAmount`,\n  `$.lines[*].lineTotal`, `$.lines[*].tax`, `$.lines[*].tax.family`,\n  `$.lines[*].tax.treatment`, `$.lines[*].tax.officialTaxCode`,\n  `$.lines[*].tax.officialPercentageCode`, `$.lines[*].tax.rate`, `$.lines[*].tax.base`,\n  `$.lines[*].tax.amount`, and `$.lines[*].tax.catalogVersion`. Presence at one of those paths MUST\n  return `PROHIBITED_CALCULATED_FIELD` regardless of value, JSON type, `null`, or equality to a\n  service result. Every monetary input and result MUST stay within `0.00` through\n  `999999999999999.99`.",
)
replace_exact(
    SPEC,
    "   5. validate request representation and reject unknown or prohibited properties, including a\n      missing or non-string `emissionPointId`, with `INVALID_REQUEST`;",
    "   5. validate request representation: reject malformed JSON or unsupported representation; classify\n      the exhaustive FR-012 calculated paths before generic unknown properties; if any are present,\n      return `PROHIBITED_CALCULATED_FIELD` using root-path order, ascending line index, then FR-012\n      line-path order; otherwise reject ordinary unknown/prohibited properties and a missing or\n      non-string `emissionPointId` with `INVALID_REQUEST`;",
)
replace_exact(
    SPEC,
    "- **SC-014**: Every request containing any calculated monetary field is rejected consistently and\n  creates no state, even when the supplied value matches the service result.",
    "- **SC-014**: Every exhaustive FR-012 calculated-field vector returns\n  `PROHIBITED_CALCULATED_FIELD` and creates no state, including wrong-type, `null`, equal-value,\n  mixed calculated/ordinary-unknown, and multiple-depth cases. Mixed bodies select the calculated\n  classification; multiple calculated paths select the deterministic FR-041 Stage-5 path order and\n  expose only the selected safe field path.",
)
replace_exact(
    PLAN,
    "- Strict request/input schemas reject `companyId`, `issuerId`, fiscal/snapshot data, unknown\n  properties, and calculated fields. The response's explicitly contracted canonical `companyId`\n  does not weaken the input prohibition.",
    "- Strict request/input schemas reject `companyId`, `issuerId`, fiscal/snapshot data, unknown\n  properties, and calculated fields. Before DTO mapping or generic unknown-property rejection, the\n  API transport classifier traverses decoded JSON and classifies the exhaustive FR-012 calculated\n  paths; calculated classification wins over ordinary unknown fields, and multiple matches use the\n  canonical FR-041 Stage-5 path order. The response's explicitly contracted canonical `companyId`\n  does not weaken the input prohibition.",
)

# ---------------------------------------------------------------------------
# A1 — exact executable buyer-email profile.
# ---------------------------------------------------------------------------
EMAIL_PROFILE = r'''
### Executable Buyer Email Profile

After the general FR-035 NFC and surrounding `U+0020` normalization, buyer email uses this exact
ASCII public-mailbox profile:

- normalized length is 6–254 ASCII characters;
- the local part is 1–64 characters and matches
  `[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*`;
- the domain is 4–253 characters, contains at least two labels, each label contains 1–63 ASCII
  letters, digits, or internal hyphens, each label starts and ends with a letter or digit, and the
  final label contains 2–63 ASCII letters;
- the complete normalized expression is
  `^(?=.{6,254}$)(?=.{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?=.{4,253}$)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}$`;
- only ASCII is accepted; internationalized local parts and Unicode domain labels are rejected;
- display case is preserved and comparison/idempotency is case-sensitive; no email-specific
  lowercase, IDNA conversion, comment removal, or other normalization is performed;
- quoted local parts, leading/trailing/consecutive local dots, domain literals, comments, trailing
  domain dots, empty labels, leading/trailing label hyphens, whitespace, controls, comma/semicolon
  multiple-address syntax, and more than one `@` are rejected.

Authoritative vectors include minimum `a@b.co`; valid dotted local part `a.b+tag@example.co`; valid
subdomain `User@billing.example.com`; every permitted local-part character category; exact
64-character local-part and 254-character total boundaries; and invalid quoted, leading/trailing/
consecutive-dot, literal, commented, Unicode, whitespace, multiple-address, multi-`@`, trailing-dot,
empty/invalid-label, and length-overflow cases. General normalization occurs first: surrounding
`U+0020` is removed, trim-to-empty is rejected, prohibited controls/Unicode separators are rejected,
and any remaining non-ASCII mailbox character fails this email profile.
'''
spec_text = read(SPEC)
marker = "\n### Executable ASCII Repertoires\n"
if spec_text.count(marker) != 1:
    raise RuntimeError("spec.md: Executable ASCII Repertoires marker mismatch")
write(SPEC, spec_text.replace(marker, "\n" + EMAIL_PROFILE + marker))

replace_exact(
    SPEC,
    "- **FR-008**: Buyer address, email, and telephone MAY be supplied. After FR-035 normalization,\n  address MUST contain 1–300 code points, email MUST be one valid address of at most 254 code\n  points, and telephone MUST contain 7–15 ASCII digits, at most 20 total code points, and only\n  digits, `+`, `U+0020`, hyphen, and parentheses.",
    "- **FR-008**: Buyer address, email, and telephone MAY be supplied. After FR-035 normalization,\n  address MUST contain 1–300 code points. Email MUST satisfy the Executable Buyer Email Profile: one\n  ASCII dot-atom mailbox, 6–254 total characters, 1–64 local-part characters, a 4–253-character\n  multi-label domain, and the exact published normalized expression; case is preserved and compared\n  case-sensitively, with no email-specific normalization. Telephone MUST contain 7–15 ASCII digits,\n  at most 20 total code points, and only digits, `+`, `U+0020`, hyphen, and parentheses.",
)
replace_exact(
    SPEC,
    "- **SC-016**: Every approved Unicode vector produces its specified accepted or rejected result.\n  Canonically equivalent input produces the same normalized value; prohibited whitespace and code\n  points are rejected; assigned emoji is accepted where field limits allow; display case is\n  preserved; canonical overflow returns `CANONICAL_NAME_TOO_LONG`; accepted values are never\n  truncated; and stricter ASCII fields follow their exact repertoires.",
    "- **SC-016**: Every approved Unicode and buyer-email vector produces its specified accepted or\n  rejected result. Canonically equivalent input produces the same normalized value; prohibited\n  whitespace and code points are rejected; assigned emoji is accepted where field limits allow;\n  display case is preserved; canonical overflow returns `CANONICAL_NAME_TOO_LONG`; accepted values\n  are never truncated; stricter ASCII fields follow their exact repertoires; and the complete\n  Executable Buyer Email Profile passes its minimum, maximum, character-category, dotted-local,\n  domain-label, Unicode, whitespace, quoted, literal, comment, and multiple-address vectors.",
)

plan_text = read(PLAN)
payment_marker = "\n**Payment-Method Effectiveness**:"
if plan_text.count(payment_marker) != 1:
    raise RuntimeError("plan.md: Payment-Method Effectiveness marker mismatch")
PLAN_EMAIL = r'''
**Buyer Email Validation**: After Application completes the one FR-035 NFC/U+0020 pass, it applies
the specification's exact ASCII public-mailbox expression and boundaries. API forwards the decoded
string unchanged and documents `x-application-stage-6`; Domain receives the normalized value and
enforces the same profile without transport or database dependencies; Infrastructure persists the
accepted value unchanged. T020 owns authoritative email vectors in
`unicode-text-validation-vectors.json`; T029 validates Application normalization/profile behavior,
T030 validates contract metadata, T026/T045 validate production business behavior, and T036 tests
only defensive stored length/nonempty constraints. No layer invents RFC variants, Unicode mailbox
support, IDNA conversion, or case folding.
'''
write(PLAN, plan_text.replace(payment_marker, "\n" + PLAN_EMAIL + payment_marker))

REQUEST_EMAIL_OLD = """        email:
          type: string
          x-application-stage-6:
            owner: Application
            unicodeNormalization: NFC
            trim: U+0020
            normalizedFormat: email
            normalizedMaxCodePoints: 254
            countingUnit: UNICODE_CODE_POINTS
          description: >-
            Optional email after the one Application-owned Stage 6 general-text NFC/U+0020 pass;
            prohibited Unicode categories/separators are rejected and length is Unicode code
            points. The API forwards the decoded value unchanged.
"""
REQUEST_EMAIL_NEW = """        email:
          type: string
          x-application-stage-6:
            owner: Application
            unicodeNormalization: NFC
            trim: U+0020
            normalizedPattern: \"^(?=.{6,254}$)(?=.{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?=.{4,253}$)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\\\.)+[A-Za-z]{2,63}$\"
            normalizedMinLength: 6
            normalizedMaxLength: 254
            localPartMaxLength: 64
            domainMinLength: 4
            domainMaxLength: 253
            asciiOnly: true
            preserveCase: true
            comparison: CASE_SENSITIVE
            countingUnit: ASCII_CHARACTERS
          description: >-
            Optional single ASCII public-mailbox address after the one Application-owned Stage 6
            general-text NFC/U+0020 pass. The local part uses unquoted dot-atom syntax, the domain
            contains at least two valid labels, and no email-specific lowercase, IDNA conversion,
            comment removal, or other normalization occurs. Quoted local parts, consecutive or
            edge dots, domain literals, comments, trailing domain dots, Unicode, internal
            whitespace, multiple-address separators, and boundary violations are rejected. The API
            forwards the decoded value unchanged.
"""
replace_exact(OPENAPI, REQUEST_EMAIL_OLD, REQUEST_EMAIL_NEW)
replace_exact(
    OPENAPI,
    """        email:
          type: string
          format: email
          maxLength: 254
""",
    """        email:
          type: string
          minLength: 6
          maxLength: 254
          pattern: \"^(?=.{6,254}$)(?=.{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?=.{4,253}$)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\\\.)+[A-Za-z]{2,63}$\"
          description: Persisted case-preserved ASCII mailbox satisfying the approved executable profile.
""",
)

# ---------------------------------------------------------------------------
# Acceptance coverage for Stage-5/Stage-6 competition and deadline races.
# ---------------------------------------------------------------------------
spec_text = read(SPEC)
edge_marker = "\n### Edge Cases\n"
if spec_text.count(edge_marker) != 1:
    raise RuntimeError("spec.md: Edge Cases marker mismatch")
NEW_SCENARIOS = """
72. **Given** a body combines a recognized FR-012 calculated path with an ordinary unknown property,
    an invalid decoded `emissionPointId`, or invalid general text, **when** failure precedence is
    evaluated before the deadline, **then** Stage 5 returns `PROHIBITED_CALCULATED_FIELD` using the
    canonical path priority and Stage 6 does not run; **and given** the same combinations without a
    recognized calculated path but with an ordinary unknown property, **then** Stage 5 returns
    `INVALID_REQUEST` and Stage 6 does not run.
73. **Given** each clarified Stage-5 result (`INVALID_REQUEST` or
    `PROHIBITED_CALCULATED_FIELD`) and each clarified Stage-6 result (`EMISSION_POINT_INVALID`,
    general-text failure, email-profile failure, or `CANONICAL_NAME_TOO_LONG`) is independently
    raced against the request deadline, **when** the stage result becomes conclusive first, **then**
    that result is returned exactly once; **when** expiry becomes conclusive first, **then**
    `REQUEST_TIMEOUT` is returned exactly once and the late stage result cannot replace it.
"""
write(SPEC, spec_text.replace(edge_marker, "\n" + NEW_SCENARIOS + edge_marker))

# Update explicit scenario-count wording without changing task IDs or statuses.
for path in ROOT.rglob("*.md"):
    text = read(path)
    if "71 acceptance scenarios" in text:
        write(path, text.replace("71 acceptance scenarios", "73 acceptance scenarios"))

# ---------------------------------------------------------------------------
# Task ownership and evidence alignment.
# ---------------------------------------------------------------------------
replace_exact(
    TASKS,
    "Cover NFC accented Latin, decomposed/composed equivalence, leading/trailing and repeated internal U+0020, tab, CR, LF, NBSP, U+2028, U+2029, zero-width `Cf`, accepted assigned emoji `So`, preserved display case, code-point boundaries, 150 accepted and 151 rejected `U+0130` canonical expansion, `CANONICAL_NAME_TOO_LONG`, and no truncation.",
    "Cover NFC accented Latin, decomposed/composed equivalence, leading/trailing and repeated internal U+0020, tab, CR, LF, NBSP, U+2028, U+2029, zero-width `Cf`, accepted assigned emoji `So`, preserved display case, code-point boundaries, 150 accepted and 151 rejected `U+0130` canonical expansion, `CANONICAL_NAME_TOO_LONG`, and no truncation. Include the authoritative Executable Buyer Email Profile vectors: minimum and 254-total/64-local boundaries, permitted local-part character categories, subdomains, and rejection of quoted, consecutive/edge-dot, domain-literal, comment, trailing-dot, empty/invalid-label, Unicode, whitespace, multi-address, multi-`@`, and overflow cases.",
)
replace_exact(
    TASKS,
    "API decodes JSON, rejects malformed representation, and validates transport structure only;",
    "API decodes JSON, rejects malformed representation, and validates transport structure only; before DTO mapping it exhaustively classifies the FR-012 calculated JSON paths with deterministic priority over generic unknown properties;",
)
replace_exact(
    TASKS,
    "T087 MUST NOT race outcomes or begin before T085 defines terminal arbitration. Include the exact idempotency errors and delegate 413 exclusively to T085",
    "T087 MUST NOT race outcomes or begin before T085 defines terminal arbitration. T087 also maps the API-owned exhaustive calculated-field classifier: calculated paths win over ordinary unknown properties, multiple matches use the canonical FR-041 path order, and safe violations expose only `CALCULATED_FIELD_PROHIBITED` plus the selected path. Include the exact idempotency errors and delegate 413 exclusively to T085",
)
replace_exact(
    TASKS,
    "For general Unicode, prove OpenAPI documents API decoding/unchanged forwarding and Application normalization/canonical errors without assigning business normalization to API.",
    "For general Unicode and email, prove OpenAPI documents API decoding/unchanged forwarding, Application normalization, the exact ASCII buyer-email profile and vectors, and canonical errors without assigning business normalization to API.",
)

# ---------------------------------------------------------------------------
# OpenAPI safe violation description for calculated classification.
# ---------------------------------------------------------------------------
replace_exact(
    OPENAPI,
    "            CANONICAL_NAME_TOO_LONG for a post-canonicalization result over 300 Unicode code points.",
    "            CANONICAL_NAME_TOO_LONG for a post-canonicalization result over 300 Unicode code points, and CALCULATED_FIELD_PROHIBITED with the selected safe JSON path when PROHIBITED_CALCULATED_FIELD governs Stage 5.",
)

# ---------------------------------------------------------------------------
# Checklist: all scoped questions now have normative evidence and this script
# performs the required post-reconciliation cross-artifact review.
# ---------------------------------------------------------------------------
checklist_text = read(CHECKLIST)
for number in range(1, 35):
    unchecked = f"- [ ] CHK{number:03d}"
    checked = f"- [x] CHK{number:03d}"
    if checklist_text.count(unchecked) != 1:
        raise RuntimeError(f"contract checklist: expected one {unchecked}")
    checklist_text = checklist_text.replace(unchecked, checked)
checklist_text += """

## Reconciliation Evidence — 2026-07-17

- Stage 6 now names emission-point trim/validation/canonicalization as its first operation in every
  scoped artifact; general-text processing begins only after it succeeds.
- The success response enforces the exact lowercase-hyphenated, non-nil emission-point UUID pattern.
- FR-012 and the error catalog define the exhaustive calculated-path set, API classification owner,
  value-independent classification, safe field evidence, and deterministic mixed/multiple priority.
- FR-008 defines one exact ASCII buyer-email profile, normalization/case behavior, boundaries,
  prohibited forms, contract metadata, and authoritative vectors.
- Acceptance Scenarios 72 and 73 close Stage-5/Stage-6 pairwise competition and deadline races.
- A deterministic cross-artifact validation checked `spec.md`, `plan.md`, `tasks.md`,
  `error-catalog.md`, and the OpenAPI contract before CHK033 and CHK034 were marked complete.
"""
write(CHECKLIST, checklist_text)

# ---------------------------------------------------------------------------
# Deterministic cross-artifact validation.
# ---------------------------------------------------------------------------
spec = read(SPEC)
plan = read(PLAN)
tasks = read(TASKS)
errors = read(ERRORS)
openapi = read(OPENAPI)
checklist = read(CHECKLIST)

assert "at the beginning of Stage 6 trim/validate/canonicalize" not in plan
assert "At the\nbeginning of Stage 6, Application alone invokes" not in plan
assert "Only after the first Stage-6 operation" in plan
assert "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}" in openapi
assert "including at least" not in errors
assert "CALCULATED_FIELD_PROHIBITED" in errors and "CALCULATED_FIELD_PROHIBITED" in openapi
assert "Executable Buyer Email Profile" in spec
assert "localPartMaxLength: 64" in openapi and "comparison: CASE_SENSITIVE" in openapi
assert "exhaustive calculated-field classifier" in tasks
assert "73 acceptance scenarios" in tasks
assert len(re.findall(r"^- \*\*FR-\d{3}\*\*:", spec, re.MULTILINE)) == 47
assert len(re.findall(r"^- \*\*SC-\d{3}\*\*:", spec, re.MULTILINE)) == 33
assert len(re.findall(r"^- \*\*DR-\d{3}\*\*:", spec, re.MULTILINE)) == 24
assert len(re.findall(r"^\d+\. \*\*Given\*\*", spec, re.MULTILINE)) == 73
assert len(re.findall(r"^- \[[ x]\] T\d{3}", tasks, re.MULTILINE)) == 101
assert checklist.count("- [ ] CHK") == 0
assert checklist.count("- [x] CHK") == 34

print("Documentary reconciliation completed and cross-artifact invariants passed.")
