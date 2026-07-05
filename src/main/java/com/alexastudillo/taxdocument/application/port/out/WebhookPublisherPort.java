package com.alexastudillo.taxdocument.application.port.out;

import com.alexastudillo.taxdocument.application.issuance.AuditEventName;
import com.alexastudillo.taxdocument.domain.taxdocument.AccessKey;

public interface WebhookPublisherPort {
    PublicationResult publish(AuditEventName eventName, AccessKey accessKey, String auditCorrelationId);

    record PublicationResult(String correlationId, boolean accepted) {
    }
}
