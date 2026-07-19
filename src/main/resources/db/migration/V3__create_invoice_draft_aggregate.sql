CREATE TABLE invoice_draft (
    id uuid NOT NULL,
    company_id uuid NOT NULL,
    emission_point_id uuid NOT NULL,
    emission_date date NOT NULL,
    buyer_identification_type_code varchar(2) NOT NULL,
    buyer_identification_catalog_version varchar(64) NOT NULL,
    buyer_identification varchar(20) NOT NULL,
    buyer_legal_name varchar(300) NOT NULL,
    buyer_address varchar(300),
    buyer_email varchar(254),
    buyer_telephone varchar(20),
    status varchar(16) NOT NULL,
    currency char(3) NOT NULL,
    subtotal_before_taxes numeric(17,2) NOT NULL,
    total_discount numeric(17,2) NOT NULL,
    grand_total numeric(17,2) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT pk_invoice_draft PRIMARY KEY (id),
    CONSTRAINT uq_invoice_draft_company_id UNIQUE (company_id, id),
    CONSTRAINT fk_invoice_draft_buyer_catalog
        FOREIGN KEY (buyer_identification_type_code, buyer_identification_catalog_version)
        REFERENCES buyer_identification_type_catalog (official_code, catalog_version),
    CONSTRAINT ck_invoice_draft_id
        CHECK (id <> '00000000-0000-0000-0000-000000000000'::uuid),
    CONSTRAINT ck_invoice_draft_company_id
        CHECK (company_id <> '00000000-0000-0000-0000-000000000000'::uuid),
    CONSTRAINT ck_invoice_draft_emission_point_id
        CHECK (emission_point_id <> '00000000-0000-0000-0000-000000000000'::uuid),
    CONSTRAINT ck_invoice_draft_buyer_identification
        CHECK (btrim(buyer_identification) <> ''),
    CONSTRAINT ck_invoice_draft_buyer_legal_name
        CHECK (btrim(buyer_legal_name) <> '' AND buyer_legal_name !~ '[[:cntrl:]]'),
    CONSTRAINT ck_invoice_draft_buyer_address
        CHECK (buyer_address IS NULL
            OR (btrim(buyer_address) <> '' AND buyer_address !~ '[[:cntrl:]]')),
    CONSTRAINT ck_invoice_draft_status CHECK (status = 'DRAFT'),
    CONSTRAINT ck_invoice_draft_currency CHECK (currency = 'USD'),
    CONSTRAINT ck_invoice_draft_totals
        CHECK (subtotal_before_taxes >= 0.00
            AND total_discount >= 0.00
            AND grand_total >= 0.00),
    CONSTRAINT ck_invoice_draft_timestamps CHECK (updated_at >= created_at)
);

CREATE TABLE invoice_line (
    id uuid NOT NULL,
    invoice_draft_id uuid NOT NULL,
    position integer NOT NULL,
    product_code varchar(25) NOT NULL,
    description varchar(300) NOT NULL,
    quantity numeric(12,6) NOT NULL,
    unit_price numeric(12,6) NOT NULL,
    discount numeric(17,2) NOT NULL,
    gross_amount numeric(17,2) NOT NULL,
    net_amount numeric(17,2) NOT NULL,
    line_total numeric(17,2) NOT NULL,
    CONSTRAINT pk_invoice_line PRIMARY KEY (id),
    CONSTRAINT fk_invoice_line_draft
        FOREIGN KEY (invoice_draft_id) REFERENCES invoice_draft (id),
    CONSTRAINT uq_invoice_line_position UNIQUE (invoice_draft_id, position),
    CONSTRAINT ck_invoice_line_id
        CHECK (id <> '00000000-0000-0000-0000-000000000000'::uuid),
    CONSTRAINT ck_invoice_line_position CHECK (position BETWEEN 1 AND 500),
    CONSTRAINT ck_invoice_line_product_code
        CHECK (product_code ~ '^[[:alnum:]]+$'),
    CONSTRAINT ck_invoice_line_description
        CHECK (btrim(description) <> '' AND description !~ '[[:cntrl:]]'),
    CONSTRAINT ck_invoice_line_quantity CHECK (quantity BETWEEN 0.000001 AND 999999.999999),
    CONSTRAINT ck_invoice_line_unit_price CHECK (unit_price BETWEEN 0.000000 AND 999999.999999),
    CONSTRAINT ck_invoice_line_amounts
        CHECK (discount >= 0.00
            AND gross_amount >= 0.00
            AND net_amount >= 0.00
            AND line_total >= 0.00
            AND discount <= gross_amount)
);

CREATE TABLE invoice_line_tax (
    id uuid NOT NULL,
    invoice_line_id uuid NOT NULL,
    tax_rule_id uuid NOT NULL,
    family varchar(16) NOT NULL,
    treatment varchar(32) NOT NULL,
    official_tax_code varchar(8) NOT NULL,
    official_percentage_code varchar(8) NOT NULL,
    rate numeric(5,2) NOT NULL,
    tax_base numeric(17,2) NOT NULL,
    tax_amount numeric(17,2) NOT NULL,
    catalog_version varchar(64) NOT NULL,
    CONSTRAINT pk_invoice_line_tax PRIMARY KEY (id),
    CONSTRAINT fk_invoice_line_tax_line
        FOREIGN KEY (invoice_line_id) REFERENCES invoice_line (id),
    CONSTRAINT fk_invoice_line_tax_catalog
        FOREIGN KEY (tax_rule_id, catalog_version)
        REFERENCES iva_tax_rule_catalog (id, catalog_version),
    CONSTRAINT uq_invoice_line_tax_line UNIQUE (invoice_line_id),
    CONSTRAINT ck_invoice_line_tax_id
        CHECK (id <> '00000000-0000-0000-0000-000000000000'::uuid),
    CONSTRAINT ck_invoice_line_tax_family CHECK (family = 'IVA'),
    CONSTRAINT ck_invoice_line_tax_treatment
        CHECK (treatment IN ('PERCENTAGE_RATE', 'ZERO_RATE', 'NOT_SUBJECT', 'EXEMPT')),
    CONSTRAINT ck_invoice_line_tax_rate
        CHECK (rate BETWEEN 0.00 AND 100.00
            AND ((treatment = 'PERCENTAGE_RATE' AND rate > 0.00)
                OR (treatment <> 'PERCENTAGE_RATE' AND rate = 0.00))),
    CONSTRAINT ck_invoice_line_tax_amounts CHECK (tax_base >= 0.00 AND tax_amount >= 0.00)
);

CREATE INDEX ix_invoice_line_tax_catalog
    ON invoice_line_tax (tax_rule_id, catalog_version);

CREATE TABLE invoice_tax_total (
    id uuid NOT NULL,
    invoice_draft_id uuid NOT NULL,
    family varchar(16) NOT NULL,
    treatment varchar(32) NOT NULL,
    official_tax_code varchar(8) NOT NULL,
    official_percentage_code varchar(8) NOT NULL,
    rate numeric(5,2) NOT NULL,
    tax_base numeric(17,2) NOT NULL,
    tax_amount numeric(17,2) NOT NULL,
    catalog_version varchar(64) NOT NULL,
    CONSTRAINT pk_invoice_tax_total PRIMARY KEY (id),
    CONSTRAINT fk_invoice_tax_total_draft
        FOREIGN KEY (invoice_draft_id) REFERENCES invoice_draft (id),
    CONSTRAINT uq_invoice_tax_total_group
        UNIQUE (
            invoice_draft_id,
            treatment,
            official_tax_code,
            official_percentage_code,
            rate,
            catalog_version
        ),
    CONSTRAINT ck_invoice_tax_total_id
        CHECK (id <> '00000000-0000-0000-0000-000000000000'::uuid),
    CONSTRAINT ck_invoice_tax_total_family CHECK (family = 'IVA'),
    CONSTRAINT ck_invoice_tax_total_treatment
        CHECK (treatment IN ('PERCENTAGE_RATE', 'ZERO_RATE', 'NOT_SUBJECT', 'EXEMPT')),
    CONSTRAINT ck_invoice_tax_total_rate
        CHECK (rate BETWEEN 0.00 AND 100.00
            AND ((treatment = 'PERCENTAGE_RATE' AND rate > 0.00)
                OR (treatment <> 'PERCENTAGE_RATE' AND rate = 0.00))),
    CONSTRAINT ck_invoice_tax_total_amounts CHECK (tax_base >= 0.00 AND tax_amount >= 0.00)
);

CREATE TABLE invoice_payment (
    id uuid NOT NULL,
    invoice_draft_id uuid NOT NULL,
    payment_method_id uuid NOT NULL,
    official_code varchar(8) NOT NULL,
    name varchar(100) NOT NULL,
    amount numeric(17,2) NOT NULL,
    catalog_version varchar(64) NOT NULL,
    CONSTRAINT pk_invoice_payment PRIMARY KEY (id),
    CONSTRAINT fk_invoice_payment_draft
        FOREIGN KEY (invoice_draft_id) REFERENCES invoice_draft (id),
    CONSTRAINT fk_invoice_payment_catalog
        FOREIGN KEY (payment_method_id, catalog_version)
        REFERENCES payment_method_catalog (id, catalog_version),
    CONSTRAINT uq_invoice_payment_method UNIQUE (invoice_draft_id, payment_method_id),
    CONSTRAINT ck_invoice_payment_id
        CHECK (id <> '00000000-0000-0000-0000-000000000000'::uuid),
    CONSTRAINT ck_invoice_payment_code CHECK (btrim(official_code) <> ''),
    CONSTRAINT ck_invoice_payment_name CHECK (btrim(name) <> ''),
    CONSTRAINT ck_invoice_payment_amount CHECK (amount >= 0.00)
);

CREATE INDEX ix_invoice_payment_catalog
    ON invoice_payment (payment_method_id, catalog_version);

CREATE TABLE invoice_additional_information (
    id uuid NOT NULL,
    invoice_draft_id uuid NOT NULL,
    position integer NOT NULL,
    name varchar(300) NOT NULL,
    canonical_name varchar(300) NOT NULL,
    value varchar(300) NOT NULL,
    CONSTRAINT pk_invoice_additional_information PRIMARY KEY (id),
    CONSTRAINT fk_invoice_additional_information_draft
        FOREIGN KEY (invoice_draft_id) REFERENCES invoice_draft (id),
    CONSTRAINT uq_invoice_additional_information_name
        UNIQUE (invoice_draft_id, canonical_name),
    CONSTRAINT uq_invoice_additional_information_position
        UNIQUE (invoice_draft_id, position),
    CONSTRAINT ck_invoice_additional_information_id
        CHECK (id <> '00000000-0000-0000-0000-000000000000'::uuid),
    CONSTRAINT ck_invoice_additional_information_position CHECK (position BETWEEN 1 AND 15),
    CONSTRAINT ck_invoice_additional_information_name
        CHECK (btrim(name) <> '' AND name !~ '[[:cntrl:]]'),
    CONSTRAINT ck_invoice_additional_information_canonical_name
        CHECK (btrim(canonical_name) <> '' AND canonical_name !~ '[[:cntrl:]]'),
    CONSTRAINT ck_invoice_additional_information_value
        CHECK (btrim(value) <> '' AND value !~ '[[:cntrl:]]')
);
