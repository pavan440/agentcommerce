package com.agentcommerce.domain.identity;

import java.time.Instant;
import java.util.UUID;

public record DeviceTokenResponse(
    UUID id,
    String platform,
    boolean isActive,
    Instant lastUsedAt,
    Instant createdAt
) {
}
