package com.agentcommerce.domain.identity;

import java.util.Set;
import java.util.UUID;

public record CustomerProfileResponse(
    UUID id,
    String email,
    String phone,
    String displayName,
    String locale,
    String timezone,
    Set<String> roles
) {
}

