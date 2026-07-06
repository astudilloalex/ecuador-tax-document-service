package com.alexastudillo.taxdocument.application.port.out;

import com.alexastudillo.taxdocument.domain.taxdocument.AccessKey;
import com.alexastudillo.taxdocument.domain.taxdocument.AuthorizationNumber;
import com.alexastudillo.taxdocument.domain.taxdocument.AuthorizationState;
import com.alexastudillo.taxdocument.domain.taxdocument.AuthorizedAt;
import com.alexastudillo.taxdocument.domain.taxdocument.TaxDocument;
import io.smallrye.mutiny.Uni;
import java.util.Optional;

public interface SriAuthorizationPort {
    Uni<ReceptionResult> submitForReception(TaxDocument taxDocument);

    Uni<AuthorizationResult> requestAuthorization(AccessKey accessKey);

    record ReceptionResult(AccessKey accessKey, AuthorizationState authorizationState, String correlationId) {
    }

    record AuthorizationResult(
            AccessKey accessKey,
            AuthorizationState authorizationState,
            Optional<AuthorizationNumber> authorizationNumber,
            Optional<AuthorizedAt> authorizedAt,
            String correlationId) {
        public AuthorizationResult {
            authorizationNumber = authorizationNumber == null ? Optional.empty() : authorizationNumber;
            authorizedAt = authorizedAt == null ? Optional.empty() : authorizedAt;
        }
    }
}
