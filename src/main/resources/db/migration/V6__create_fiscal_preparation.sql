CREATE TABLE official_sequence_baseline (
    id uuid NOT NULL,
    company_id uuid NOT NULL,
    issuer_reference varchar(128) NOT NULL,
    establishment_reference varchar(128) NOT NULL,
    emission_point_id uuid NOT NULL,
    establishment_code varchar(3) NOT NULL,
    emission_point_code varchar(3) NOT NULL,
    document_type_code varchar(2) NOT NULL,
    last_allocated integer NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT pk_official_sequence_baseline PRIMARY KEY (id),
    CONSTRAINT uq_official_sequence_baseline_company_id UNIQUE (company_id, id),
    CONSTRAINT uq_official_sequence_baseline_scope UNIQUE (
        issuer_reference, establishment_reference, emission_point_id,
        establishment_code, emission_point_code, document_type_code
    ),
    CONSTRAINT ck_official_sequence_baseline_ids CHECK (
        id <> '00000000-0000-0000-0000-000000000000'::uuid
        AND company_id <> '00000000-0000-0000-0000-000000000000'::uuid
        AND emission_point_id <> '00000000-0000-0000-0000-000000000000'::uuid
    ),
    CONSTRAINT ck_official_sequence_baseline_scope CHECK (
        btrim(issuer_reference) <> ''
        AND issuer_reference !~ '[[:cntrl:]]'
        AND btrim(establishment_reference) <> ''
        AND establishment_reference !~ '[[:cntrl:]]'
        AND establishment_code ~ '^[0-9]{3}$'
        AND emission_point_code ~ '^[0-9]{3}$'
        AND document_type_code = '01'
    ),
    CONSTRAINT ck_official_sequence_baseline_value CHECK (
        last_allocated BETWEEN 0 AND 999999999
    ),
    CONSTRAINT ck_official_sequence_baseline_timestamps CHECK (updated_at >= created_at)
);

CREATE FUNCTION sri_modulo11_verification_digit(value text) RETURNS integer
LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE AS $function$
    WITH weighted AS (
        SELECT SUM(
            (ascii(substring(value FROM position FOR 1)) - ascii('0'))
            * (2 + ((length(value) - position) % 6))
        ) AS total
        FROM generate_series(1, length(value)) AS position
    ), raw_digit AS (
        SELECT 11 - (total % 11) AS value FROM weighted
    )
    SELECT CASE value WHEN 11 THEN 0 WHEN 10 THEN 1 ELSE value END FROM raw_digit;
$function$;

CREATE TABLE fiscal_preparation (
    id uuid NOT NULL,
    company_id uuid NOT NULL,
    invoice_draft_id uuid NOT NULL,
    official_sequence_baseline_id uuid NOT NULL,
    emission_date date NOT NULL,
    issuer_reference varchar(128) NOT NULL,
    issuer_ruc varchar(13) NOT NULL,
    legal_name varchar(300) NOT NULL,
    commercial_name varchar(300),
    head_office_address varchar(300) NOT NULL,
    accounting_required boolean NOT NULL,
    special_taxpayer_resolution varchar(64),
    withholding_agent_resolution varchar(8),
    rimpe_classification varchar(32) NOT NULL,
    large_contributor_resolution varchar(64),
    large_contributor_legend varchar(300),
    establishment_reference varchar(128) NOT NULL,
    establishment_code varchar(3) NOT NULL,
    establishment_address varchar(300) NOT NULL,
    emission_point_id uuid NOT NULL,
    emission_point_code varchar(3) NOT NULL,
    environment_code varchar(1) NOT NULL,
    document_type_code varchar(2) NOT NULL,
    emission_type_code varchar(1) NOT NULL,
    source_authority varchar(128) NOT NULL,
    source_revision varchar(128) NOT NULL,
    source_effective_from date NOT NULL,
    source_effective_through date,
    source_observed_at timestamptz NOT NULL,
    technical_rule_id varchar(64) NOT NULL,
    technical_rule_modified_on date NOT NULL,
    numeric_code_policy_id varchar(64) NOT NULL,
    official_sequential_number varchar(9) NOT NULL,
    numeric_code varchar(8) NOT NULL,
    access_key varchar(49) NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT pk_fiscal_preparation PRIMARY KEY (id),
    CONSTRAINT uq_fiscal_preparation_company_draft UNIQUE (company_id, invoice_draft_id),
    CONSTRAINT uq_fiscal_preparation_scoped_sequential UNIQUE (
        official_sequence_baseline_id, official_sequential_number
    ),
    CONSTRAINT uq_fiscal_preparation_access_key UNIQUE (access_key),
    CONSTRAINT fk_fiscal_preparation_company_draft FOREIGN KEY (company_id, invoice_draft_id)
        REFERENCES invoice_draft (company_id, id),
    CONSTRAINT fk_fiscal_preparation_company_baseline
        FOREIGN KEY (company_id, official_sequence_baseline_id)
        REFERENCES official_sequence_baseline (company_id, id),
    CONSTRAINT ck_fiscal_preparation_ids CHECK (
        id <> '00000000-0000-0000-0000-000000000000'::uuid
        AND company_id <> '00000000-0000-0000-0000-000000000000'::uuid
        AND invoice_draft_id <> '00000000-0000-0000-0000-000000000000'::uuid
        AND official_sequence_baseline_id <> '00000000-0000-0000-0000-000000000000'::uuid
        AND emission_point_id <> '00000000-0000-0000-0000-000000000000'::uuid
    ),
    CONSTRAINT ck_fiscal_preparation_fiscal_text CHECK (
        btrim(issuer_reference) <> '' AND issuer_reference !~ '[[:cntrl:]]'
        AND issuer_ruc ~ '^[0-9]{13}$'
        AND btrim(legal_name) <> '' AND legal_name !~ '[[:cntrl:]]'
        AND (commercial_name IS NULL
             OR (btrim(commercial_name) <> '' AND commercial_name !~ '[[:cntrl:]]'))
        AND btrim(head_office_address) <> '' AND head_office_address !~ '[[:cntrl:]]'
        AND btrim(establishment_reference) <> ''
        AND establishment_reference !~ '[[:cntrl:]]'
        AND btrim(establishment_address) <> ''
        AND establishment_address !~ '[[:cntrl:]]'
    ),
    CONSTRAINT ck_fiscal_preparation_designation_pairs CHECK (
        rimpe_classification IN ('NONE', 'RIMPE_CONTRIBUTOR', 'POPULAR_BUSINESS')
        AND (special_taxpayer_resolution IS NULL
             OR (btrim(special_taxpayer_resolution) <> ''
                 AND special_taxpayer_resolution !~ '[[:cntrl:]]'))
        AND (withholding_agent_resolution IS NULL
             OR withholding_agent_resolution ~ '^(0|[1-9][0-9]{0,7})$')
        AND ((large_contributor_resolution IS NULL AND large_contributor_legend IS NULL)
             OR (btrim(large_contributor_resolution) <> ''
                 AND large_contributor_resolution !~ '[[:cntrl:]]'
                 AND btrim(large_contributor_legend) <> ''
                 AND large_contributor_legend !~ '[[:cntrl:]]'))
    ),
    CONSTRAINT ck_fiscal_preparation_codes CHECK (
        establishment_code ~ '^[0-9]{3}$'
        AND emission_point_code ~ '^[0-9]{3}$'
        AND environment_code IN ('1', '2')
        AND document_type_code = '01'
        AND emission_type_code = '1'
        AND official_sequential_number ~ '^[0-9]{9}$'
        AND official_sequential_number <> '000000000'
        AND numeric_code ~ '^[0-9]{8}$'
    ),
    CONSTRAINT ck_fiscal_preparation_source_evidence CHECK (
        btrim(source_authority) <> '' AND source_authority !~ '[[:cntrl:]]'
        AND btrim(source_revision) <> '' AND source_revision !~ '[[:cntrl:]]'
        AND (source_effective_through IS NULL
             OR source_effective_through >= source_effective_from)
        AND emission_date >= source_effective_from
        AND (source_effective_through IS NULL OR emission_date <= source_effective_through)
        AND technical_rule_id = 'SRI-OFFLINE-2.33'
        AND technical_rule_modified_on = DATE '2026-07-13'
        AND numeric_code_policy_id = 'SECURE_RANDOM_8_V1'
    ),
    CONSTRAINT ck_fiscal_preparation_access_key CHECK (
        access_key ~ '^[0-9]{49}$'
        AND substring(access_key FROM 1 FOR 8) = to_char(emission_date, 'DDMMYYYY')
        AND substring(access_key FROM 9 FOR 2) = document_type_code
        AND substring(access_key FROM 11 FOR 13) = issuer_ruc
        AND substring(access_key FROM 24 FOR 1) = environment_code
        AND substring(access_key FROM 25 FOR 3) = establishment_code
        AND substring(access_key FROM 28 FOR 3) = emission_point_code
        AND substring(access_key FROM 31 FOR 9) = official_sequential_number
        AND substring(access_key FROM 40 FOR 8) = numeric_code
        AND substring(access_key FROM 48 FOR 1) = emission_type_code
        AND substring(access_key FROM 49 FOR 1)
            = sri_modulo11_verification_digit(substring(access_key FROM 1 FOR 48))::text
    )
);

CREATE FUNCTION reject_fiscal_preparation_change() RETURNS trigger
LANGUAGE plpgsql AS $function$
BEGIN
    RAISE EXCEPTION 'Fiscal Preparation is append-only';
END;
$function$;

CREATE TRIGGER trg_fiscal_preparation_append_only
BEFORE UPDATE OR DELETE ON fiscal_preparation
FOR EACH ROW EXECUTE FUNCTION reject_fiscal_preparation_change();

CREATE FUNCTION guard_official_sequence_baseline_change() RETURNS trigger
LANGUAGE plpgsql AS $function$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Official Sequence Baseline is not removable';
    END IF;
    IF NEW.id <> OLD.id
        OR NEW.company_id <> OLD.company_id
        OR NEW.issuer_reference <> OLD.issuer_reference
        OR NEW.establishment_reference <> OLD.establishment_reference
        OR NEW.emission_point_id <> OLD.emission_point_id
        OR NEW.establishment_code <> OLD.establishment_code
        OR NEW.emission_point_code <> OLD.emission_point_code
        OR NEW.document_type_code <> OLD.document_type_code
        OR NEW.created_at <> OLD.created_at
        OR NEW.last_allocated <> OLD.last_allocated + 1
        OR NEW.updated_at < OLD.updated_at THEN
        RAISE EXCEPTION 'Official Sequence Baseline change is not an allocation step';
    END IF;
    IF NOT EXISTS (
        SELECT 1
          FROM fiscal_preparation
         WHERE company_id = NEW.company_id
           AND official_sequence_baseline_id = NEW.id
           AND official_sequential_number = lpad(NEW.last_allocated::text, 9, '0')
    ) THEN
        RAISE EXCEPTION 'Official Sequence Baseline advancement lacks its Fiscal Preparation';
    END IF;
    RETURN NEW;
END;
$function$;

CREATE TRIGGER trg_official_sequence_baseline_guard
BEFORE UPDATE OR DELETE ON official_sequence_baseline
FOR EACH ROW EXECUTE FUNCTION guard_official_sequence_baseline_change();
