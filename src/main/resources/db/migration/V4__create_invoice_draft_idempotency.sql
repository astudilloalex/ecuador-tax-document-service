CREATE TABLE invoice_draft_idempotency (
    company_id uuid NOT NULL,
    idempotency_key_hash bytea NOT NULL,
    request_fingerprint bytea NOT NULL,
    normalization_version smallint NOT NULL,
    invoice_draft_id uuid NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT pk_invoice_draft_idempotency
        PRIMARY KEY (company_id, idempotency_key_hash),
    CONSTRAINT uq_invoice_draft_idempotency_draft UNIQUE (invoice_draft_id),
    CONSTRAINT fk_invoice_draft_idempotency_company_draft
        FOREIGN KEY (company_id, invoice_draft_id)
        REFERENCES invoice_draft (company_id, id),
    CONSTRAINT ck_invoice_draft_idempotency_company_id
        CHECK (company_id <> '00000000-0000-0000-0000-000000000000'::uuid),
    CONSTRAINT ck_invoice_draft_idempotency_key_hash
        CHECK (octet_length(idempotency_key_hash) = 32),
    CONSTRAINT ck_invoice_draft_idempotency_fingerprint
        CHECK (octet_length(request_fingerprint) = 32),
    CONSTRAINT ck_invoice_draft_idempotency_normalization_version
        CHECK (normalization_version > 0),
    CONSTRAINT ck_invoice_draft_idempotency_draft_id
        CHECK (invoice_draft_id <> '00000000-0000-0000-0000-000000000000'::uuid)
);

CREATE INDEX ix_invoice_draft_idempotency_company_draft
    ON invoice_draft_idempotency (company_id, invoice_draft_id);
