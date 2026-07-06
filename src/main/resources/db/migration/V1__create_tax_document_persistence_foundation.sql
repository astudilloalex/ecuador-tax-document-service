CREATE TABLE issuers (
    issuer_id varchar(64) PRIMARY KEY,
    legal_identifier varchar(32) NOT NULL,
    legal_name varchar(255),
    trade_name varchar(255),
    created_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_issuers_legal_identifier ON issuers (legal_identifier);

CREATE TABLE establishments (
    establishment_id varchar(64) PRIMARY KEY,
    issuer_id varchar(64) NOT NULL,
    establishment_code varchar(16) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_establishments_issuer
        FOREIGN KEY (issuer_id) REFERENCES issuers (issuer_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT uk_establishments_issuer_code
        UNIQUE (issuer_id, establishment_code)
);

CREATE INDEX idx_establishments_issuer_id ON establishments (issuer_id);

CREATE TABLE issuing_points (
    issuing_point_id varchar(64) PRIMARY KEY,
    establishment_id varchar(64) NOT NULL,
    issuing_point_code varchar(16) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_issuing_points_establishment
        FOREIGN KEY (establishment_id) REFERENCES establishments (establishment_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT uk_issuing_points_establishment_code
        UNIQUE (establishment_id, issuing_point_code)
);

CREATE INDEX idx_issuing_points_establishment_id ON issuing_points (establishment_id);

CREATE TABLE issuance_sequences (
    issuance_sequence_id bigserial PRIMARY KEY,
    issuer_id varchar(64) NOT NULL,
    establishment_id varchar(64) NOT NULL,
    issuing_point_id varchar(64) NOT NULL,
    document_type varchar(32) NOT NULL,
    sequence_number varchar(20) NOT NULL,
    reserved_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_issuance_sequences_issuer
        FOREIGN KEY (issuer_id) REFERENCES issuers (issuer_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_issuance_sequences_establishment
        FOREIGN KEY (establishment_id) REFERENCES establishments (establishment_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_issuance_sequences_issuing_point
        FOREIGN KEY (issuing_point_id) REFERENCES issuing_points (issuing_point_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT uk_issuance_sequences_identity
        UNIQUE (issuer_id, establishment_id, issuing_point_id, document_type, sequence_number),
    CONSTRAINT ck_issuance_sequences_document_type
        CHECK (document_type IN ('INVOICE', 'CREDIT_NOTE', 'DEBIT_NOTE', 'WAYBILL', 'WITHHOLDING')),
    CONSTRAINT ck_issuance_sequences_sequence_number
        CHECK (sequence_number ~ '^[0-9]+$')
);

CREATE INDEX idx_issuance_sequences_identity
    ON issuance_sequences (issuer_id, establishment_id, issuing_point_id, document_type, sequence_number);

CREATE TABLE tax_documents (
    tax_document_id bigserial PRIMARY KEY,
    access_key varchar(49) NOT NULL,
    issuer_id varchar(64) NOT NULL,
    establishment_id varchar(64) NOT NULL,
    issuing_point_id varchar(64) NOT NULL,
    document_type varchar(32) NOT NULL,
    sequence_number varchar(20) NOT NULL,
    issue_date date NOT NULL,
    document_state varchar(32) NOT NULL,
    authorization_state varchar(32) NOT NULL,
    authorization_number varchar(128),
    authorized_at timestamp(6) with time zone,
    issuance_mode varchar(32) NOT NULL,
    external_request_id varchar(128),
    created_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tax_documents_issuer
        FOREIGN KEY (issuer_id) REFERENCES issuers (issuer_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_tax_documents_establishment
        FOREIGN KEY (establishment_id) REFERENCES establishments (establishment_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_tax_documents_issuing_point
        FOREIGN KEY (issuing_point_id) REFERENCES issuing_points (issuing_point_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT uk_tax_documents_access_key
        UNIQUE (access_key),
    CONSTRAINT uk_tax_documents_issuance_identity
        UNIQUE (issuer_id, document_type, establishment_id, issuing_point_id, sequence_number),
    CONSTRAINT ck_tax_documents_access_key
        CHECK (access_key ~ '^[0-9]{49}$'),
    CONSTRAINT ck_tax_documents_document_type
        CHECK (document_type IN ('INVOICE', 'CREDIT_NOTE', 'DEBIT_NOTE', 'WAYBILL', 'WITHHOLDING')),
    CONSTRAINT ck_tax_documents_sequence_number
        CHECK (sequence_number ~ '^[0-9]+$'),
    CONSTRAINT ck_tax_documents_document_state
        CHECK (document_state IN ('PENDING', 'IN_PROGRESS', 'RECEIVED', 'AUTHORIZED', 'NOT_AUTHORIZED', 'RETURNED', 'REJECTED', 'IRRECOVERABLE', 'VOIDED')),
    CONSTRAINT ck_tax_documents_authorization_state
        CHECK (authorization_state IN ('NOT_SUBMITTED', 'SUBMITTED', 'RECEIVED', 'AUTHORIZED', 'NOT_AUTHORIZED', 'RETURNED', 'REJECTED')),
    CONSTRAINT ck_tax_documents_issuance_mode
        CHECK (issuance_mode IN ('SYNCHRONOUS', 'ASYNCHRONOUS'))
);

CREATE INDEX idx_tax_documents_access_key ON tax_documents (access_key);

CREATE INDEX idx_tax_documents_issuance_identity
    ON tax_documents (issuer_id, document_type, establishment_id, issuing_point_id, sequence_number);
