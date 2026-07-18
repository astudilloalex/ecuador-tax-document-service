-- V3 is immutable. Replace only the two constraints whose definitions did not enforce the
-- approved explicit ASCII repertoires. PostgreSQL executes these DDL statements transactionally,
-- so a failure leaves the V3/V4 schema unchanged.

ALTER TABLE invoice_draft
    DROP CONSTRAINT ck_invoice_draft_buyer_identification;

ALTER TABLE invoice_draft
    ADD CONSTRAINT ck_invoice_draft_buyer_identification
    CHECK (
        (buyer_identification_type_code = '04'
            AND buyer_identification ~ '^[0-9]{13}$')
        OR (buyer_identification_type_code = '05'
            AND buyer_identification ~ '^[0-9]{10}$')
        OR (buyer_identification_type_code IN ('06', '08')
            AND buyer_identification ~ '^[A-Za-z0-9]{1,20}$')
        OR (buyer_identification_type_code = '07'
            AND buyer_identification = '9999999999999')
    );

ALTER TABLE invoice_line
    DROP CONSTRAINT ck_invoice_line_product_code;

ALTER TABLE invoice_line
    ADD CONSTRAINT ck_invoice_line_product_code
    CHECK (product_code ~ '^[A-Za-z0-9]{1,25}$');
