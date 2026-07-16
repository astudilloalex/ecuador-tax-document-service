CREATE TABLE buyer_identification_type_catalog (
    official_code varchar(2) NOT NULL,
    official_label varchar(100) NOT NULL,
    display_name varchar(100) NOT NULL,
    validation_strategy varchar(64) NOT NULL,
    validation_rule_version varchar(64) NOT NULL,
    source_valid_from date,
    source_valid_to date,
    target_valid_from date NOT NULL,
    target_valid_to date,
    active boolean NOT NULL,
    catalog_version varchar(64) NOT NULL,
    official_source_uri text NOT NULL,
    official_source_locator varchar(128) NOT NULL,
    CONSTRAINT pk_buyer_identification_type_catalog
        PRIMARY KEY (official_code, catalog_version),
    CONSTRAINT ck_buyer_identification_type_code
        CHECK (official_code ~ '^[0-9]{2}$'),
    CONSTRAINT ck_buyer_identification_type_names
        CHECK (btrim(official_label) <> '' AND btrim(display_name) <> ''),
    CONSTRAINT ck_buyer_identification_type_source_validity
        CHECK (source_valid_to IS NULL
            OR (source_valid_from IS NOT NULL AND source_valid_to >= source_valid_from)),
    CONSTRAINT ck_buyer_identification_type_target_validity
        CHECK (target_valid_to IS NULL OR target_valid_to >= target_valid_from),
    CONSTRAINT ck_buyer_identification_type_active_metadata
        CHECK (NOT active OR (
            btrim(validation_strategy) <> ''
            AND btrim(validation_rule_version) <> ''
            AND btrim(catalog_version) <> ''
            AND btrim(official_source_uri) <> ''
            AND btrim(official_source_locator) <> ''))
);

CREATE TABLE iva_tax_rule_catalog (
    id uuid NOT NULL,
    family varchar(16) NOT NULL,
    official_tax_code varchar(8) NOT NULL,
    official_percentage_code varchar(8) NOT NULL,
    official_label varchar(100) NOT NULL,
    display_name varchar(100) NOT NULL,
    treatment varchar(32) NOT NULL,
    rate numeric(5,2) NOT NULL,
    source_valid_from date,
    source_valid_to date,
    target_valid_from date NOT NULL,
    target_valid_to date,
    active boolean NOT NULL,
    catalog_version varchar(64) NOT NULL,
    official_source_uri text NOT NULL,
    official_source_locator varchar(128) NOT NULL,
    CONSTRAINT pk_iva_tax_rule_catalog PRIMARY KEY (id, catalog_version),
    CONSTRAINT uq_iva_tax_rule_catalog_natural_version
        UNIQUE (official_tax_code, official_percentage_code, target_valid_from, catalog_version),
    CONSTRAINT ck_iva_tax_rule_catalog_id
        CHECK (id <> '00000000-0000-0000-0000-000000000000'::uuid),
    CONSTRAINT ck_iva_tax_rule_catalog_family CHECK (family = 'IVA'),
    CONSTRAINT ck_iva_tax_rule_catalog_treatment
        CHECK (treatment IN ('PERCENTAGE_RATE', 'ZERO_RATE', 'NOT_SUBJECT', 'EXEMPT')),
    CONSTRAINT ck_iva_tax_rule_catalog_rate
        CHECK (rate BETWEEN 0.00 AND 100.00
            AND ((treatment = 'PERCENTAGE_RATE' AND rate > 0.00)
                OR (treatment <> 'PERCENTAGE_RATE' AND rate = 0.00))),
    CONSTRAINT ck_iva_tax_rule_catalog_names
        CHECK (btrim(official_tax_code) <> ''
            AND btrim(official_percentage_code) <> ''
            AND btrim(official_label) <> ''
            AND btrim(display_name) <> ''),
    CONSTRAINT ck_iva_tax_rule_catalog_source_validity
        CHECK (source_valid_to IS NULL
            OR (source_valid_from IS NOT NULL AND source_valid_to >= source_valid_from)),
    CONSTRAINT ck_iva_tax_rule_catalog_target_validity
        CHECK (target_valid_to IS NULL OR target_valid_to >= target_valid_from),
    CONSTRAINT ck_iva_tax_rule_catalog_active_metadata
        CHECK (NOT active OR (
            btrim(catalog_version) <> ''
            AND btrim(official_source_uri) <> ''
            AND btrim(official_source_locator) <> ''))
);

CREATE TABLE payment_method_catalog (
    id uuid NOT NULL,
    official_code varchar(8) NOT NULL,
    official_label varchar(160) NOT NULL,
    display_name varchar(100) NOT NULL,
    source_valid_from date NOT NULL,
    source_valid_to date,
    target_valid_from date NOT NULL,
    target_valid_to date,
    active boolean NOT NULL,
    catalog_version varchar(64) NOT NULL,
    official_source_uri text NOT NULL,
    official_source_locator varchar(128) NOT NULL,
    CONSTRAINT pk_payment_method_catalog PRIMARY KEY (id, catalog_version),
    CONSTRAINT uq_payment_method_catalog_natural_version
        UNIQUE (official_code, target_valid_from, catalog_version),
    CONSTRAINT ck_payment_method_catalog_id
        CHECK (id <> '00000000-0000-0000-0000-000000000000'::uuid),
    CONSTRAINT ck_payment_method_catalog_names
        CHECK (btrim(official_code) <> ''
            AND btrim(official_label) <> ''
            AND btrim(display_name) <> ''),
    CONSTRAINT ck_payment_method_catalog_source_validity
        CHECK (source_valid_to IS NULL OR source_valid_to >= source_valid_from),
    CONSTRAINT ck_payment_method_catalog_target_validity
        CHECK (target_valid_to IS NULL OR target_valid_to >= target_valid_from),
    CONSTRAINT ck_payment_method_catalog_active_metadata
        CHECK (NOT active OR (
            btrim(catalog_version) <> ''
            AND btrim(official_source_uri) <> ''
            AND btrim(official_source_locator) <> ''))
);
