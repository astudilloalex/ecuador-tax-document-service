package com.alexastudillo.taxdocument.application.issuance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alexastudillo.taxdocument.domain.taxdocument.AccessKey;
import com.alexastudillo.taxdocument.domain.taxdocument.AuthorizationNumber;
import com.alexastudillo.taxdocument.domain.taxdocument.AuthorizationState;
import com.alexastudillo.taxdocument.domain.taxdocument.AuthorizedAt;
import com.alexastudillo.taxdocument.domain.taxdocument.DocumentState;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IssuanceResultTest {
    @Test
    void exposesApplicationResultWithoutAdapterModels() {
        AccessKey accessKey = new AccessKey("1234567890123456789012345678901234567890123456789");
        IssuanceError error = new IssuanceError(IssuanceError.Code.RETRY_INELIGIBLE, "Retry is not eligible");

        IssuanceResult result = new IssuanceResult(
                accessKey,
                DocumentState.AUTHORIZED,
                AuthorizationState.AUTHORIZED,
                new AuthorizationNumber("authorization-1"),
                new AuthorizedAt(Instant.parse("2026-07-05T12:00:00Z")),
                IssuanceResult.Outcome.COMPLETED,
                "audit-correlation-1",
                List.of(error));

        assertEquals(accessKey, result.accessKey());
        assertTrue(result.authorizationNumber().isPresent());
        assertTrue(result.authorizedAt().isPresent());
        assertEquals(IssuanceResult.Outcome.COMPLETED, result.outcome());
        assertEquals(List.of(error), result.errors());
    }
}
