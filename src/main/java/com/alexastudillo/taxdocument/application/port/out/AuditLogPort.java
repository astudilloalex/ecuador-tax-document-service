package com.alexastudillo.taxdocument.application.port.out;

import com.alexastudillo.taxdocument.application.issuance.AuditEventName;
import io.smallrye.mutiny.Uni;
import java.time.Instant;
import java.util.Map;

public interface AuditLogPort {
    Uni<Void> append(
            AuditEventName eventName,
            String auditCorrelationId,
            Instant occurredAt,
            Map<String, String> safeMetadata);
}
