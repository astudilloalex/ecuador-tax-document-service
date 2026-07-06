package com.alexastudillo.taxdocument.application.port.out;

import com.alexastudillo.taxdocument.application.issuance.AuditEventName;
import java.time.Instant;
import java.util.Map;

public interface AuditLogPort {
    void append(AuditEventName eventName, String auditCorrelationId, Instant occurredAt, Map<String, String> safeMetadata);
}
