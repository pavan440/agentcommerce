package com.agentcommerce.domain.identity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CustomerProfileResponse(
    UUID id,
    String email,
    String phone,
    String profileImageUrl,
    String displayName,
    String locale,
    String timezone,
    List<String> dietaryRestrictions,
    List<String> allergens,
    String substitutionMode,
    boolean autoOrderEnabled,
    long autoOrderMaxAmountMinor,
    BigDecimal defaultTipPercentage,
    long version,
    Set<String> roles
) {
}
