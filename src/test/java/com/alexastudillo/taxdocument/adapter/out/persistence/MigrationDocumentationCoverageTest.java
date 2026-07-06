package com.alexastudillo.taxdocument.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MigrationDocumentationCoverageTest {
    private static final Path MIGRATION_DOC = Path.of("docs/migration/legacy-to-target-terminology.md");
    private static final List<String> REQUIRED_TERMS = List.of(
            "issuers",
            "establishments",
            "issuing_points",
            "issuance_sequences",
            "tax_documents",
            "issuer_id",
            "establishment_id",
            "issuing_point_id",
            "access_key",
            "document_type",
            "sequence_number",
            "issue_date",
            "authorized_at",
            "PFV-PER-002",
            "PFV-PER-003",
            "PFV-PER-004",
            "PFV-PER-005",
            "PFV-PER-006",
            "PFV-PER-007",
            "PFV-PER-008",
            "Target database object");

    @Test
    void migrationDocumentationCoversTargetPersistenceObjectsAndDeferredPfvs() throws IOException {
        String documentation = Files.readString(MIGRATION_DOC);

        for (String term : REQUIRED_TERMS) {
            assertTrue(documentation.contains(term), "missing migration documentation term " + term);
        }
    }
}
