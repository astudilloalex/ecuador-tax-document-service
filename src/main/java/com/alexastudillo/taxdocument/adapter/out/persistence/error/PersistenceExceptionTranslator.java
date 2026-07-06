package com.alexastudillo.taxdocument.adapter.out.persistence.error;

import com.alexastudillo.taxdocument.application.error.PersistenceFailure;
import com.alexastudillo.taxdocument.application.error.PersistenceFailureCategory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;

@ApplicationScoped
public class PersistenceExceptionTranslator {
    private static final String GENERIC_MESSAGE = "Persistence operation failed";

    public PersistenceFailure duplicateAccessKey() {
        return failure(PersistenceFailureCategory.DUPLICATE_ACCESS_KEY_CONFLICT, "Duplicate access key");
    }

    public PersistenceFailure duplicateIssuanceIdentity() {
        return failure(PersistenceFailureCategory.DUPLICATE_ISSUANCE_IDENTITY_CONFLICT, "Duplicate issuance identity");
    }

    public PersistenceFailure unavailableSequenceReservation() {
        return failure(
                PersistenceFailureCategory.UNAVAILABLE_SEQUENCE_RESERVATION_CONFLICT,
                "Sequence reservation is unavailable");
    }

    public PersistenceFailure invalidPersistedTaxDocumentState() {
        return failure(
                PersistenceFailureCategory.INVALID_PERSISTED_TAX_DOCUMENT_STATE,
                "Persisted tax document state is invalid");
    }

    public PersistenceFailure invalidPersistenceRelationship() {
        return failure(
                PersistenceFailureCategory.INVALID_PERSISTENCE_RELATIONSHIP,
                "Persisted relationship is invalid");
    }

    public PersistenceFailure transactionFailure() {
        return failure(
                PersistenceFailureCategory.PERSISTENCE_TRANSACTION_FAILURE,
                "Persistence transaction failed");
    }

    public PersistenceFailure translate(Throwable failure) {
        if (failure instanceof PersistenceFailure persistenceFailure) {
            return persistenceFailure;
        }

        String constraintName = constraintName(failure);
        if (constraintName != null) {
            return translateConstraint(constraintName);
        }

        if (failure instanceof PersistenceException || containsClassName(failure, "PersistenceException")) {
            return failure(PersistenceFailureCategory.GENERIC_PERSISTENCE_FAILURE, GENERIC_MESSAGE);
        }

        return failure(PersistenceFailureCategory.GENERIC_PERSISTENCE_FAILURE, GENERIC_MESSAGE);
    }

    public PersistenceFailure translateConstraint(String constraintName) {
        String normalized = constraintName == null ? "" : constraintName.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "uk_tax_documents_access_key" -> duplicateAccessKey();
            case "uk_tax_documents_issuance_identity" -> duplicateIssuanceIdentity();
            case "uk_issuance_sequences_identity" -> unavailableSequenceReservation();
            case "fk_establishments_issuer",
                    "fk_issuing_points_establishment",
                    "fk_issuance_sequences_issuer",
                    "fk_issuance_sequences_establishment",
                    "fk_issuance_sequences_issuing_point",
                    "fk_tax_documents_issuer",
                    "fk_tax_documents_establishment",
                    "fk_tax_documents_issuing_point" -> invalidPersistenceRelationship();
            case "ck_tax_documents_document_type",
                    "ck_tax_documents_document_state",
                    "ck_tax_documents_authorization_state",
                    "ck_tax_documents_issuance_mode" -> invalidPersistedTaxDocumentState();
            default -> failure(PersistenceFailureCategory.GENERIC_PERSISTENCE_FAILURE, GENERIC_MESSAGE);
        };
    }

    private PersistenceFailure failure(PersistenceFailureCategory category, String message) {
        return new PersistenceFailure(category, message);
    }

    private static String constraintName(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolationException) {
                return constraintViolationException.getConstraintName();
            }
            current = current.getCause();
        }
        return null;
    }

    private static boolean containsClassName(Throwable failure, String className) {
        Throwable current = failure;
        while (current != null) {
            if (current.getClass().getName().contains(className)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
