package com.agentcommerce.domain.identity;

import java.time.Instant;
import java.util.UUID;

public record ConsentResponse(
    UUID id,
    String consentType,
    String policyVersion,
    boolean granted,
    Instant grantedAt,
    Instant revokedAt,
    Instant createdAt
) {
}
