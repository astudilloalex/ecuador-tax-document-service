package com.alexastudillo.taxdocument.application.port.out;

import com.alexastudillo.taxdocument.application.issuance.IssuanceRequest;

public interface TaxDocumentQueuePort {
    QueueAcceptance enqueueIssuance(IssuanceRequest issuanceRequest);

    record QueueAcceptance(String correlationId, boolean accepted) {
    }
}
