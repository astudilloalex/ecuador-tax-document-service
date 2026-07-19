CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

INSERT INTO buyer_identification_type_catalog (
    official_code,
    official_label,
    display_name,
    validation_strategy,
    validation_rule_version,
    source_valid_from,
    source_valid_to,
    target_valid_from,
    target_valid_to,
    active,
    catalog_version,
    official_source_uri,
    official_source_locator
) VALUES
    (
        '04', 'RUC', 'RUC', 'FORMAT_ONLY_NUMERIC_13', 'SRI-OFFLINE-2.32-TARGET-1',
        NULL, NULL, DATE '2026-07-12', NULL, true, 'SRI-OFFLINE-2.32-TARGET-1',
        'https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf',
        'SRI-FT-2.32 Tables 5-6; SRI-FE-CURRENT RUC algorithm notice'
    ),
    (
        '05', 'CÉDULA', 'Ecuadorian identity card', 'FORMAT_ONLY_NUMERIC_10',
        'SRI-OFFLINE-2.32-TARGET-1', NULL, NULL, DATE '2026-07-12', NULL, true,
        'SRI-OFFLINE-2.32-TARGET-1',
        'https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf',
        'SRI-FT-2.32 Tables 5-6'
    ),
    (
        '06', 'PASAPORTE', 'Passport', 'FORMAT_ONLY_ALPHANUMERIC_1_TO_20',
        'SRI-OFFLINE-2.32-TARGET-1', NULL, NULL, DATE '2026-07-12', NULL, true,
        'SRI-OFFLINE-2.32-TARGET-1',
        'https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf',
        'SRI-FT-2.32 Table 6 and invoice identificacionComprador, pp. 65-66'
    ),
    (
        '07', 'VENTA A CONSUMIDOR FINAL', 'Final consumer sale', 'FINAL_CONSUMER_EXACT',
        'SRI-OFFLINE-2.32-TARGET-1', NULL, NULL, DATE '2026-07-12', NULL, true,
        'SRI-OFFLINE-2.32-TARGET-1',
        'https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf',
        'SRI-FT-2.32 Table 6 note and section 9.10, p. 27'
    ),
    (
        '08', 'IDENTIFICACIÓN DEL EXTERIOR', 'Foreign identification',
        'FORMAT_ONLY_ALPHANUMERIC_1_TO_20', 'SRI-OFFLINE-2.32-TARGET-1',
        NULL, NULL, DATE '2026-07-12', NULL, true, 'SRI-OFFLINE-2.32-TARGET-1',
        'https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf',
        'SRI-FT-2.32 Table 6 note and invoice identificacionComprador, pp. 65-66'
    );

INSERT INTO iva_tax_rule_catalog (
    id,
    family,
    official_tax_code,
    official_percentage_code,
    official_label,
    display_name,
    treatment,
    rate,
    source_valid_from,
    source_valid_to,
    target_valid_from,
    target_valid_to,
    active,
    catalog_version,
    official_source_uri,
    official_source_locator
) VALUES
    (
        '84cb3f03-574b-54de-9e73-efb8d485476a', 'IVA', '2', '0', '0%', 'IVA 0%',
        'ZERO_RATE', 0.00, NULL, NULL, DATE '2026-07-12', NULL, true,
        'SRI-OFFLINE-2.32-TARGET-1', 'https://www.sri.gob.ec/impuesto-al-valor-agregado-iva',
        'SRI-FT-2.32 Table 17, p. 28; SRI-IVA-CURRENT rate guidance'
    ),
    (
        '2b31de9b-20f2-50c7-aeff-fed9babfe112', 'IVA', '2', '5', '5%', 'IVA 5%',
        'PERCENTAGE_RATE', 5.00, DATE '2024-04-01', NULL, DATE '2026-07-12', NULL, true,
        'SRI-OFFLINE-2.32-TARGET-1',
        'https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar?id=6b8588f2-a4bf-44bb-ac40-085391ba2aed&nombre=NAC-DGERCGC24-00000013.pdf',
        'SRI-FT-2.32 Table 17; SRI-RES-24-13 Art. 1 and Final Provision'
    ),
    (
        '3aa0fb56-17ad-5310-a10c-64c1f6dbe2fb', 'IVA', '2', '10', '13%', 'IVA 13%',
        'PERCENTAGE_RATE', 13.00, NULL, NULL, DATE '2026-07-12', NULL, true,
        'SRI-OFFLINE-2.32-TARGET-1', 'https://www.sri.gob.ec/impuesto-al-valor-agregado-iva',
        'SRI-FT-2.32 Table 17, p. 28; SRI-IVA-CURRENT rate guidance'
    ),
    (
        '5b34b038-931c-50e3-a84c-10af272fdcd4', 'IVA', '2', '4', '15%', 'IVA 15%',
        'PERCENTAGE_RATE', 15.00, DATE '2025-12-26', NULL, DATE '2026-07-12', NULL, true,
        'SRI-OFFLINE-2.32-TARGET-1',
        'https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar?id=236482f4-6125-42fd-b073-62c99d08233d&nombre=NAC-DGECCGC25-00000006.pdf',
        'SRI-FT-2.32 Table 17; SRI-CIR-25-06 section 2; SRI-CIR-26-05 section 2'
    ),
    (
        'a70a77f5-1176-5b0b-a539-74ead416a3ff', 'IVA', '2', '6', 'No Objeto de Impuesto',
        'Not subject to IVA', 'NOT_SUBJECT', 0.00, NULL, NULL, DATE '2026-07-12', NULL, true,
        'SRI-OFFLINE-2.32-TARGET-1', 'https://www.sri.gob.ec/impuesto-al-valor-agregado-iva',
        'SRI-FT-2.32 Table 17, p. 28; SRI-IVA-CURRENT rate guidance'
    ),
    (
        'a7eeaf77-dbdc-5f99-9bdd-d783c072a7de', 'IVA', '2', '7', 'Exento de IVA',
        'Exempt from IVA', 'EXEMPT', 0.00, NULL, NULL, DATE '2026-07-12', NULL, true,
        'SRI-OFFLINE-2.32-TARGET-1', 'https://www.sri.gob.ec/impuesto-al-valor-agregado-iva',
        'SRI-FT-2.32 Table 17, p. 28; SRI-IVA-CURRENT rate guidance'
    );

INSERT INTO payment_method_catalog (
    id,
    official_code,
    official_label,
    display_name,
    source_valid_from,
    source_valid_to,
    target_valid_from,
    target_valid_to,
    active,
    catalog_version,
    official_source_uri,
    official_source_locator
) VALUES
    (
        '639f2b7e-10a3-5d92-a1a3-28223896f5b5', '01',
        'SIN UTILIZACION DEL SISTEMA FINANCIERO', 'Without use of the financial system',
        DATE '2013-01-01', NULL, DATE '2026-07-12', NULL, true, 'SRI-OFFLINE-2.32-TARGET-1',
        'https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf',
        'SRI-FT-2.32 Table 24, p. 79'
    ),
    (
        'daad9ac7-6a55-5df6-8a9e-60012c5d261b', '15', 'COMPENSACIÓN DE DEUDAS',
        'Debt compensation', DATE '2013-01-01', NULL, DATE '2026-07-12', NULL, true,
        'SRI-OFFLINE-2.32-TARGET-1',
        'https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf',
        'SRI-FT-2.32 Table 24, p. 79'
    ),
    (
        'cbf7e764-0ef5-5422-965e-fe08eaa49772', '16', 'TARJETA DE DÉBITO', 'Debit card',
        DATE '2016-06-01', NULL, DATE '2026-07-12', NULL, true, 'SRI-OFFLINE-2.32-TARGET-1',
        'https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf',
        'SRI-FT-2.32 Table 24, p. 79'
    ),
    (
        '8b626780-39fb-5c72-b1e2-8453df01b79a', '17', 'DINERO ELECTRÓNICO',
        'Electronic money', DATE '2016-06-01', NULL, DATE '2026-07-12', NULL, true,
        'SRI-OFFLINE-2.32-TARGET-1',
        'https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf',
        'SRI-FT-2.32 Table 24, p. 79'
    ),
    (
        '65eee3f8-1c46-5749-8101-6e6d50d08a69', '18', 'TARJETA PREPAGO', 'Prepaid card',
        DATE '2016-06-01', NULL, DATE '2026-07-12', NULL, true, 'SRI-OFFLINE-2.32-TARGET-1',
        'https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf',
        'SRI-FT-2.32 Table 24, p. 79'
    ),
    (
        '178f5fd1-038b-577f-bac3-21c49ce6d1f2', '19', 'TARJETA DE CRÉDITO', 'Credit card',
        DATE '2016-06-01', NULL, DATE '2026-07-12', NULL, true, 'SRI-OFFLINE-2.32-TARGET-1',
        'https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf',
        'SRI-FT-2.32 Table 24, p. 79'
    ),
    (
        '953df84c-d41c-5e72-b975-9d02c45ee656', '20',
        'OTROS CON UTILIZACIÓN DEL SISTEMA FINANCIERO', 'Other with use of the financial system',
        DATE '2016-06-01', NULL, DATE '2026-07-12', NULL, true, 'SRI-OFFLINE-2.32-TARGET-1',
        'https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf',
        'SRI-FT-2.32 Table 24, p. 79'
    ),
    (
        'f2bc801e-c241-5df8-99f8-ceb9ee870d05', '21', 'ENDOSO DE TÍTULOS',
        'Endorsement of securities', DATE '2016-06-01', NULL, DATE '2026-07-12', NULL, true,
        'SRI-OFFLINE-2.32-TARGET-1',
        'https://www.sri.gob.ec/o/sri-portlet-biblioteca-alfresco-internet/descargar/29562323-2e76-42f5-abb6-cb7ac542c3c6/FICHA%20TE%CC%81CNICA%20COMPROBANTES%20ELECTRO%CC%81NICOS%20ESQUEMA%20OFFLINE%20Versio%CC%81n%202.32.pdf',
        'SRI-FT-2.32 Table 24, p. 79'
    );

DO $verification$
DECLARE
    reference_namespace constant uuid := '32576bbf-b70d-5c24-98ff-d5f9b48e8826';
BEGIN
    IF (SELECT count(*) FROM buyer_identification_type_catalog) <> 5
        OR (SELECT count(*) FROM iva_tax_rule_catalog) <> 6
        OR (SELECT count(*) FROM payment_method_catalog) <> 8 THEN
        RAISE EXCEPTION 'The invoice-draft reference baseline must contain exactly 5 buyer, 6 IVA, and 8 payment rows';
    END IF;

    IF EXISTS (
        (SELECT official_code, official_label, display_name, validation_strategy,
                source_valid_from, source_valid_to
         FROM buyer_identification_type_catalog)
        EXCEPT
        (VALUES
            ('04'::varchar, 'RUC'::varchar, 'RUC'::varchar,
             'FORMAT_ONLY_NUMERIC_13'::varchar, NULL::date, NULL::date),
            ('05', 'CÉDULA', 'Ecuadorian identity card',
             'FORMAT_ONLY_NUMERIC_10', NULL, NULL),
            ('06', 'PASAPORTE', 'Passport',
             'FORMAT_ONLY_ALPHANUMERIC_1_TO_20', NULL, NULL),
            ('07', 'VENTA A CONSUMIDOR FINAL', 'Final consumer sale',
             'FINAL_CONSUMER_EXACT', NULL, NULL),
            ('08', 'IDENTIFICACIÓN DEL EXTERIOR', 'Foreign identification',
             'FORMAT_ONLY_ALPHANUMERIC_1_TO_20', NULL, NULL))
    ) THEN
        RAISE EXCEPTION 'The buyer-identification baseline contains an unapproved mapping';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM iva_tax_rule_catalog
        WHERE id <> uuid_generate_v5(
            reference_namespace,
            'tax-rule|SRI-OFFLINE-2.32|'
                || official_tax_code || '|' || official_percentage_code || '|'
                || to_char(rate, 'FM990.00') || '|' || treatment)
    ) THEN
        RAISE EXCEPTION 'An IVA tax-rule UUID does not match the approved UUIDv5 derivation';
    END IF;

    IF EXISTS (
        (SELECT official_tax_code, official_percentage_code, official_label, display_name,
                rate, treatment, source_valid_from, source_valid_to
         FROM iva_tax_rule_catalog)
        EXCEPT
        (VALUES
            ('2'::varchar, '0'::varchar, '0%'::varchar, 'IVA 0%'::varchar,
             0.00::numeric, 'ZERO_RATE'::varchar, NULL::date, NULL::date),
            ('2', '5', '5%', 'IVA 5%', 5.00, 'PERCENTAGE_RATE', DATE '2024-04-01', NULL),
            ('2', '10', '13%', 'IVA 13%', 13.00, 'PERCENTAGE_RATE', NULL, NULL),
            ('2', '4', '15%', 'IVA 15%', 15.00, 'PERCENTAGE_RATE', DATE '2025-12-26', NULL),
            ('2', '6', 'No Objeto de Impuesto', 'Not subject to IVA',
             0.00, 'NOT_SUBJECT', NULL, NULL),
            ('2', '7', 'Exento de IVA', 'Exempt from IVA',
             0.00, 'EXEMPT', NULL, NULL))
    ) THEN
        RAISE EXCEPTION 'The IVA baseline contains an unapproved natural mapping';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM payment_method_catalog
        WHERE id <> uuid_generate_v5(
            reference_namespace,
            'payment-method|SRI-OFFLINE-2.32|' || official_code)
    ) THEN
        RAISE EXCEPTION 'A payment-method UUID does not match the approved UUIDv5 derivation';
    END IF;

    IF EXISTS (
        (SELECT official_code, official_label, display_name, source_valid_from, source_valid_to
         FROM payment_method_catalog)
        EXCEPT
        (VALUES
            ('01'::varchar, 'SIN UTILIZACION DEL SISTEMA FINANCIERO'::varchar,
             'Without use of the financial system'::varchar, DATE '2013-01-01', NULL::date),
            ('15', 'COMPENSACIÓN DE DEUDAS', 'Debt compensation', DATE '2013-01-01', NULL),
            ('16', 'TARJETA DE DÉBITO', 'Debit card', DATE '2016-06-01', NULL),
            ('17', 'DINERO ELECTRÓNICO', 'Electronic money', DATE '2016-06-01', NULL),
            ('18', 'TARJETA PREPAGO', 'Prepaid card', DATE '2016-06-01', NULL),
            ('19', 'TARJETA DE CRÉDITO', 'Credit card', DATE '2016-06-01', NULL),
            ('20', 'OTROS CON UTILIZACIÓN DEL SISTEMA FINANCIERO',
             'Other with use of the financial system', DATE '2016-06-01', NULL),
            ('21', 'ENDOSO DE TÍTULOS', 'Endorsement of securities', DATE '2016-06-01', NULL))
    ) THEN
        RAISE EXCEPTION 'The payment-method baseline contains an unapproved mapping';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM buyer_identification_type_catalog left_type
        JOIN buyer_identification_type_catalog right_type
          ON left_type.ctid < right_type.ctid
         AND left_type.official_code = right_type.official_code
         AND left_type.active AND right_type.active
         AND daterange(left_type.target_valid_from,
                       COALESCE(left_type.target_valid_to, 'infinity'::date), '[]')
             && daterange(right_type.target_valid_from,
                          COALESCE(right_type.target_valid_to, 'infinity'::date), '[]')
    ) OR EXISTS (
        SELECT 1
        FROM iva_tax_rule_catalog left_rule
        JOIN iva_tax_rule_catalog right_rule
          ON left_rule.ctid < right_rule.ctid
         AND left_rule.official_tax_code = right_rule.official_tax_code
         AND left_rule.official_percentage_code = right_rule.official_percentage_code
         AND left_rule.active AND right_rule.active
         AND daterange(left_rule.target_valid_from,
                       COALESCE(left_rule.target_valid_to, 'infinity'::date), '[]')
             && daterange(right_rule.target_valid_from,
                          COALESCE(right_rule.target_valid_to, 'infinity'::date), '[]')
    ) OR EXISTS (
        SELECT 1
        FROM payment_method_catalog left_method
        JOIN payment_method_catalog right_method
          ON left_method.ctid < right_method.ctid
         AND left_method.official_code = right_method.official_code
         AND left_method.active AND right_method.active
         AND daterange(left_method.target_valid_from,
                       COALESCE(left_method.target_valid_to, 'infinity'::date), '[]')
             && daterange(right_method.target_valid_from,
                          COALESCE(right_method.target_valid_to, 'infinity'::date), '[]')
    ) THEN
        RAISE EXCEPTION 'Active reference-data target validity intervals overlap';
    END IF;

    IF EXISTS (
        SELECT 1 FROM (
            SELECT validation_rule_version AS version, target_valid_from, target_valid_to,
                   active, catalog_version, official_source_uri, official_source_locator
            FROM buyer_identification_type_catalog
            UNION ALL
            SELECT catalog_version, target_valid_from, target_valid_to,
                   active, catalog_version, official_source_uri, official_source_locator
            FROM iva_tax_rule_catalog
            UNION ALL
            SELECT catalog_version, target_valid_from, target_valid_to,
                   active, catalog_version, official_source_uri, official_source_locator
            FROM payment_method_catalog
        ) approved
        WHERE version <> 'SRI-OFFLINE-2.32-TARGET-1'
           OR catalog_version <> 'SRI-OFFLINE-2.32-TARGET-1'
           OR target_valid_from <> DATE '2026-07-12'
           OR target_valid_to IS NOT NULL
           OR NOT active
           OR btrim(official_source_uri) = ''
           OR btrim(official_source_locator) = ''
    ) THEN
        RAISE EXCEPTION 'Reference-data approval, evidence, version, or target validity metadata is incomplete';
    END IF;
END
$verification$;
