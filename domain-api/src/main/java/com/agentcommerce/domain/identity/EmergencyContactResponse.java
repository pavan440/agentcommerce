package com.agentcommerce.domain.identity;

import java.time.Instant;
import java.util.UUID;

public record EmergencyContactResponse(
    UUID id,
    String contactName,
    String relationship,
    String phone,
    Instant createdAt
) {
}
