package com.alexastudillo.taxdocument.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PersistenceSchemaMigrationTest {
    private static final Path MIGRATION =
            Path.of("src/main/resources/db/migration/V1__create_tax_document_persistence_foundation.sql");

    @Test
    void migrationCreatesOnlyApprovedEnglishTables() throws IOException {
        String sql = migrationSql();

        assertTrue(sql.contains("CREATE TABLE issuers"));
        assertTrue(sql.contains("CREATE TABLE establishments"));
        assertTrue(sql.contains("CREATE TABLE issuing_points"));
        assertTrue(sql.contains("CREATE TABLE issuance_sequences"));
        assertTrue(sql.contains("CREATE TABLE tax_documents"));
        assertFalse(sql.contains("CREATE TABLE tax_document_audit_events"));
    }

    @Test
    void migrationUsesCanonicalDocumentTypeValuesAndNoSriNumericCodes() throws IOException {
        String sql = migrationSql();

        assertTrue(sql.contains("'INVOICE'"));
        assertTrue(sql.contains("'CREDIT_NOTE'"));
        assertTrue(sql.contains("'DEBIT_NOTE'"));
        assertTrue(sql.contains("'WAYBILL'"));
        assertTrue(sql.contains("'WITHHOLDING'"));
        assertFalse(sql.contains("'01'"));
        assertFalse(sql.contains("'04'"));
        assertFalse(sql.contains("'05'"));
        assertFalse(sql.contains("'06'"));
        assertFalse(sql.contains("'07'"));
    }

    @Test
    void migrationDefinesRequiredConstraintsAndIndexes() throws IOException {
        String sql = migrationSql();

        assertTrue(sql.contains("CONSTRAINT uk_tax_documents_access_key"));
        assertTrue(sql.contains("CONSTRAINT uk_tax_documents_issuance_identity"));
        assertTrue(sql.contains("CONSTRAINT uk_issuance_sequences_identity"));
        assertTrue(sql.contains("ON UPDATE RESTRICT ON DELETE RESTRICT"));
        assertTrue(sql.contains("CREATE INDEX idx_tax_documents_access_key"));
        assertTrue(sql.contains("CREATE INDEX idx_tax_documents_issuance_identity"));
    }

    private static String migrationSql() throws IOException {
        return Files.readString(MIGRATION);
    }
}
