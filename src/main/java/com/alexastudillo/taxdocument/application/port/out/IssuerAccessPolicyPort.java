package com.alexastudillo.taxdocument.application.port.out;

import com.alexastudillo.taxdocument.domain.taxdocument.Establishment;
import com.alexastudillo.taxdocument.domain.taxdocument.Issuer;
import com.alexastudillo.taxdocument.domain.taxdocument.IssuingPoint;
import io.smallrye.mutiny.Uni;
import java.util.Optional;

public interface IssuerAccessPolicyPort {
    Uni<AccessDecision> canIssue(
            String actorId,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint);

    Uni<AccessDecision> canInspect(
            String actorId,
            Issuer issuer,
            Establishment establishment,
            IssuingPoint issuingPoint);

    record AccessDecision(boolean allowed, Optional<String> reason) {
        public AccessDecision {
            reason = reason == null ? Optional.empty() : reason;
        }
    }
}
