# Governance Corrective-Assignment Addendum

**Feature identifier**: `001-create-invoice-draft`

**Addendum status**: `APPROVED`

**Approval decision**: `APPROVED_CORRECTIVE_ASSIGNMENT_ADDENDUM`

**Final disposition**: `T017_RED_EVIDENCE_T018_V5_IMPLEMENTATION`

**Current implementation permission**: `PENDING_SUCCESSFUL_ANALYSIS`

**Approver**: `astudilloalex`

**Constitutional capacity**: `Constitutional Governance Owner`

**Feature capacity**: `Owner of 001-create-invoice-draft`

**Same-person declaration**: `astudilloalex` explicitly exercises both approval capacities.

**Identity verification method**: `Explicit declaration by the repository owner`

**Approval basis**: `Explicit repository-owner declaration`

**Approved at (UTC)**: `2026-07-17T00:12:28Z`

**Reviewed baseline commit**: `2b72fbdd72aa701101ee232bf2d60caadc9cdca7`

**approvalCommit**: `TO_BE_RECORDED_AFTER_COMMIT`

**Retrospective review**:
[`governance-retrospective-review.md`](governance-retrospective-review.md)

**Retrospective SHA-256**:
`540f840a1903840a19566675b6935c2591a903218b05a3ebfef4c805cdae0063`

No GitHub CLI identity, Git configuration identity, commit signature, cryptographic signature, SSH
identity, or external authentication evidence is required or asserted by this documentary
decision. The explicit repository-owner declaration above is the approval authority.

## Purpose and preserved governance

The original retrospective approval assigned T017 to creation of the corrective V5 migration and
T018 to validation of V5. A subsequent `$speckit-analyze` report identified a sequencing defect:
the authoritative vectors and executable evidence exposing D2/D3 should exist before the
corrective migration is implemented.

This addendum formally approves only the revised execution assignment described below. It does
not rewrite the historical approval as though that assignment had always existed. It does not
change the retrospective findings, D1, D2, D3, Constitution 2.0.1, the historical completion of
T001–T016, V3 immutability, or the released governance disposition of `GATE-GOV-001`.

The retrospective-review file remains byte-for-byte unchanged at the SHA-256 recorded above. Its
approved findings remain authoritative. This addendum changes only which pending task performs
each mandatory D2/D3 corrective action and the order in which those actions execute.

## Previous approved assignment

The approval recorded on 2026-07-16 assigned:

- T017 to create `V5__tighten_invoice_draft_ascii_constraints.sql`.
- T018 to validate V5 through PostgreSQL, Flyway, and cross-layer evidence.

That assignment remains part of the historical approval record. It is superseded prospectively,
and only for corrective execution sequencing, by this approved addendum.

## Revised approved assignment

### T017 — Red evidence and authoritative validation vectors

T017 owns:

- creation of the authoritative shared validation fixture;
- preparation of PostgreSQL/Flyway assertions demonstrating the known V3 mismatch;
- validation of fixture integrity;
- optional validation of the approved literal regular expressions using a standalone Java
  `Pattern` as fixture/literal-regex evidence only;
- no production Java-validator equivalence;
- no creation or modification of V5; and
- no modification of V3.

Before T018, the expected result is intentional red evidence: assertions exposing D2/D3 fail
against V3 for approved invalid vectors, while unrelated existing behavior remains stable. T017 is
not complete unless the shared fixture is valid and the failures specifically demonstrate the
known insufficient or locale-dependent V3 barriers.

### T018 — V5 implementation and green persistence evidence

T018 depends on completed T017 and owns:

- creation of `src/main/resources/db/migration/V5__tighten_invoice_draft_ascii_constraints.sql`;
- preservation of V3 without modification;
- replacement of only the locale-dependent or insufficient affected constraints;
- successful V3-to-V5 Flyway upgrade;
- successful Flyway validation;
- passing PostgreSQL/Flyway assertions introduced by T017;
- updated migration-inventory assertions;
- updated reference-data assertions affected by the new migration; and
- proof that locale-dependent POSIX classes and equivalent ambiguous expressions are absent from
  the final constraints.

T018 establishes PostgreSQL and Flyway behavior. It does not test or claim equivalence with future
production Java validators.

## Distributed cross-layer equivalence

The approved responsibility distribution is:

- T017/T018: PostgreSQL and Flyway behavior using the authoritative fixture;
- T030: OpenAPI contract validation using the same fixture;
- T045: production buyer-identification Java validation using the same fixture; and
- T050: production product-code and text-rule Java validation using the same fixture.

Each layer-specific suite independently consumes the same authoritative fixture and asserts the
stored expected result for every applicable vector. The complete set of those independent results
establishes cross-layer equivalence.

Domain tests must not directly depend on PostgreSQL, Flyway, OpenAPI parser infrastructure, or HTTP
transport infrastructure. T017 may use standalone Java `Pattern` only to validate fixture parsing
or the approved literal expression; it must not claim to exercise T045 or T050 production code.

## Mandatory sequencing and implementation gate

1. T017 remains mandatory and pending.
2. T018 remains mandatory and pending and cannot begin before T017 completes with the approved red
   evidence.
3. T019 remains pending and cannot begin before T017 and T018 complete successfully.
4. Failure of T017 or T018 blocks T019 and every later business implementation task.
5. No corrective task may be skipped or represented as completed by this approval.
6. `GATE-GOV-001` retains its recorded governance status, but the current implementation permission
   is `PENDING_SUCCESSFUL_ANALYSIS` because the latest analysis contains a CRITICAL finding about
   the now-superseded assignment.
7. T017 is not authorized to start until a new `$speckit-analyze` confirms that the CRITICAL finding
   has been removed from the reconciled artifacts.

## Approval

`astudilloalex`, acting as both `Constitutional Governance Owner` and
`Owner of 001-create-invoice-draft`, approves this corrective-assignment addendum on the explicit
repository-owner declaration recorded above.

**Decision**: `APPROVED_CORRECTIVE_ASSIGNMENT_ADDENDUM`

**Disposition**: `T017_RED_EVIDENCE_T018_V5_IMPLEMENTATION`

