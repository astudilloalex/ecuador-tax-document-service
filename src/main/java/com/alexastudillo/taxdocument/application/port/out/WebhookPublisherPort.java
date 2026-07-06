package com.alexastudillo.taxdocument.application.port.out;

import com.alexastudillo.taxdocument.application.issuance.AuditEventName;
import com.alexastudillo.taxdocument.domain.taxdocument.AccessKey;
import io.smallrye.mutiny.Uni;

public interface WebhookPublisherPort {
    Uni<PublicationResult> publish(AuditEventName eventName, AccessKey accessKey, String auditCorrelationId);

    record PublicationResult(String correlationId, boolean accepted) {
    }
}
