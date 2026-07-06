package com.alexastudillo.taxdocument.application.port.out;

import com.alexastudillo.taxdocument.application.issuance.IssuanceRequest;
import io.smallrye.mutiny.Uni;

public interface TaxDocumentQueuePort {
    Uni<QueueAcceptance> enqueueIssuance(IssuanceRequest issuanceRequest);

    record QueueAcceptance(String correlationId, boolean accepted) {
    }
}
